package org.gappauth.sdk.api;

import static net.openid.appauth.AuthorizationException.EXTRA_EXCEPTION;
import static net.openid.appauth.AuthorizationException.GeneralErrors.NETWORK_ERROR;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenResponse;
import net.openid.appauthdemo.AuthStateManager;
import net.openid.appauthdemo.Configuration;
import net.openid.appauthdemo.TokenActivity;

import org.gappauth.sdk.BridgeActivity;
import org.gappauth.sdk.entity.GSignInAccount;
import org.gappauth.sdk.entity.GSignInOptions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

public class GAppAuth {

    private static final String TAG = GAppAuth.class.getSimpleName();
    private static final Exception UNKNOWN_ERROR = new Exception("Unknown Error");

    private final Activity mActivity;
    private final GSignInOptions mOptions;

    private AuthorizationService mAuthService;
    private AuthStateManager mAuthStateManager;
    private Configuration mConfiguration;

    private AuthorizationServiceConfiguration mServiceConfiguration;
    private GSignInAccount.Builder mAccountBuilder;

    private final OnSignOutListener initListener = new OnSignOutListener() {
        @Override
        public void onSuccess() {
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            Log.i(TAG, "initListener onFailure", e);
        }
    };

    public GAppAuth(@NonNull Activity activity, @NonNull GSignInOptions options) {
        this.mActivity = activity;
        this.mOptions = options;

        init(initListener);
    }

    private void init(OnSignOutListener listener) {
        mAuthStateManager = AuthStateManager.getInstance(mActivity);
        mConfiguration = Configuration.getInstance(mActivity);

        if (mAuthStateManager.getCurrent().isAuthorized()
                && !mConfiguration.hasConfigurationChanged()) {
            Log.d(TAG, "User is already authenticated, proceeding to token activity");
            listener.onFailure(new Exception("User is already authenticated, proceeding to token activity"));
            return;
        }

        if (mConfiguration.hasConfigurationChanged()) {
            // discard any existing authorization state due to the change of configuration
            Log.i(TAG, "Configuration change detected, discarding old state");
            mAuthStateManager.replace(new AuthState());
            mConfiguration.acceptConfiguration();
        }

        AuthorizationServiceConfiguration.fetchFromUrl(
                mConfiguration.getDiscoveryUri(),
                new AuthorizationServiceConfiguration.RetrieveConfigurationCallback() {
                    @Override
                    public void onFetchConfigurationCompleted(@Nullable AuthorizationServiceConfiguration serviceConfiguration, @Nullable AuthorizationException ex) {
                        if (serviceConfiguration == null) {
                            Log.d(TAG, "onFetchConfigurationCompleted", ex);
                            listener.onFailure(ex != null ? ex : UNKNOWN_ERROR);
                            return;
                        }
                        mAuthStateManager.replace(new AuthState(serviceConfiguration));
                        mServiceConfiguration = serviceConfiguration;
                        listener.onSuccess();
                    }
                });
    }

    public void signIn(int requestCode) {
        mAuthService = new AuthorizationService(mActivity);

        if (mAuthStateManager.getCurrent().isAuthorized()
                && !mConfiguration.hasConfigurationChanged()) {
            Log.i(TAG, "User is already authenticated, proceeding to token activity");
            mActivity.startActivityForResult(new Intent(mActivity, BridgeActivity.class), requestCode);
            return;
        }

        if (mServiceConfiguration == null) {
            Log.i(TAG, NETWORK_ERROR.toString());
            Intent intent = new Intent(mActivity, BridgeActivity.class);
            intent.putExtra(EXTRA_EXCEPTION, NETWORK_ERROR.toJsonString());
            mActivity.startActivityForResult(intent, requestCode);
            init(initListener);
            return;
        }

        Log.d(TAG, mConfiguration.getClientId() + ", " + mOptions.getScopes());
        AuthorizationRequest.Builder authRequestBuilder = new AuthorizationRequest.Builder(
                mServiceConfiguration,
                mConfiguration.getClientId(),
                mOptions.getResponseType(),
                mConfiguration.getRedirectUri());

        AuthorizationRequest authRequest = authRequestBuilder.setScopes(mOptions.getScopes()).build();

        Intent authIntent = mAuthService.getAuthorizationRequestIntent(authRequest);
        mActivity.startActivityForResult(authIntent, requestCode);
    }

    public void signOut(OnSignOutListener listener){
        // discard the authorization and token state, but retain the configuration and
        // dynamic client registration (if applicable), to save from retrieving them again.
        AuthState currentState = mAuthStateManager.getCurrent();
        AuthState clearedState =
                new AuthState(currentState.getAuthorizationServiceConfiguration());
        if (currentState.getLastRegistrationResponse() != null) {
            clearedState.update(currentState.getLastRegistrationResponse());
        }
        mAuthStateManager.replace(clearedState);
        init(listener);
    }

    public void parseAuthRequestFromIntent(Intent data, OnSignInListener listener){
        mAccountBuilder = new GSignInAccount.Builder();

        if (mAuthStateManager.getCurrent().isAuthorized()) {
            Log.i(TAG, "User is already authenticated, proceeding to token activity");
            AuthorizationResponse lastResp = mAuthStateManager.getCurrent().getLastAuthorizationResponse();
            if (lastResp!=null){
                mAccountBuilder.setServerAuthCode(lastResp.authorizationCode);
            }

            mAuthStateManager.getCurrent().performActionWithFreshTokens(mAuthService, new AuthState.AuthStateAction() {
                @Override
                public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException ex) {
                    fetchUserInfo(accessToken, ex, listener);
                }
            });
            return;
        }

        AuthorizationResponse resp = AuthorizationResponse.fromIntent(data);
        AuthorizationException ex = AuthorizationException.fromIntent(data);
        if (resp == null && ex == null) {
            listener.onFailure(UNKNOWN_ERROR);
            return;
        }

        mAuthStateManager.updateAfterAuthorization(resp, ex);

        if (resp == null){
            Log.d(TAG, ex.toString());
            listener.onFailure(ex);
            return;
        }
        mAccountBuilder.setServerAuthCode(resp.authorizationCode);

        mAuthService.performTokenRequest(resp.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {
            @Override
            public void onTokenRequestCompleted(@Nullable TokenResponse response, @Nullable AuthorizationException ex) {

            }
        });
    }

    public interface OnSignInListener {
        void onSuccess(@NonNull GSignInAccount account);

        void onFailure(@NonNull Exception e);
    }

    public interface OnSignOutListener {
        void onSuccess();

        void onFailure(@NonNull Exception e);
    }
}
