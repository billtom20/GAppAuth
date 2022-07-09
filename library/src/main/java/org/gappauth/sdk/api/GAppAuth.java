package org.gappauth.sdk.api;

import static net.openid.appauth.AuthorizationException.EXTRA_EXCEPTION;
import static net.openid.appauth.AuthorizationException.GeneralErrors.NETWORK_ERROR;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.AuthorizationServiceDiscovery;
import net.openid.appauth.EndSessionRequest;
import net.openid.appauth.RedirectUriReceiverActivity;
import net.openid.appauth.TokenResponse;
import net.openid.appauthdemo.AuthStateManager;
import net.openid.appauthdemo.Configuration;

import org.gappauth.sdk.BridgeActivity;
import org.gappauth.sdk.entity.GSignInAccount;
import org.gappauth.sdk.entity.GSignInOptions;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okio.Okio;

public class GAppAuth {

    private static final String TAG = GAppAuth.class.getSimpleName();
    private static final Exception UNKNOWN_ERROR = new Exception("Unknown Error");
    private static final int END_SESSION_REQUEST_CODE = 911;

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
        String clientId = "invalid";
        try {
            ActivityInfo activityInfo = mActivity.getPackageManager().getActivityInfo(
                    new ComponentName(mActivity, RedirectUriReceiverActivity.class), PackageManager.GET_META_DATA);
            clientId = activityInfo.metaData.getString("gappauth.google.clientid");
        } catch (PackageManager.NameNotFoundException e) {
            listener.onFailure(new Exception("client_id_prefix is invalid", e));
            return;
        }
        mAuthStateManager = AuthStateManager.getInstance(mActivity);
        mConfiguration = Configuration.getInstance(mActivity, clientId);

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

        if (mServiceConfiguration == null) {
            AuthorizationServiceConfiguration serviceConfiguration = new AuthorizationServiceConfiguration(
                    Uri.parse("https://accounts.google.com/o/oauth2/v2/auth") /* auth endpoint */,
                    Uri.parse("https://www.googleapis.com/oauth2/v4/token") /* token endpoint */
            );
            mAuthStateManager.replace(new AuthState(serviceConfiguration));
            mServiceConfiguration = serviceConfiguration;
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
                }, mConfiguration.getConnectionBuilder());
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

    @MainThread
    private void endSession(OnSignOutListener listener) {
        AuthState currentState = mAuthStateManager.getCurrent();
        AuthorizationServiceConfiguration config =
                currentState.getAuthorizationServiceConfiguration();
        if (config.endSessionEndpoint != null) {
            Intent endSessionIntent = mAuthService.getEndSessionRequestIntent(
                    new EndSessionRequest.Builder(config)
                            .setIdTokenHint(currentState.getIdToken())
                            .setPostLogoutRedirectUri(mConfiguration.getEndSessionRedirectUri())
                            .build());
            mActivity.startActivityForResult(endSessionIntent, END_SESSION_REQUEST_CODE);
        } else {
            signOut(listener);
        }
    }

    public void signOut(OnSignOutListener listener) {
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

    public void parseAuthResultFromIntent(Intent data, OnSignInListener listener) {
        mAccountBuilder = new GSignInAccount.Builder();

        if (mAuthStateManager.getCurrent().isAuthorized()) {
            Log.i(TAG, "User is already authenticated, proceeding to token activity");
            AuthorizationResponse lastResp = mAuthStateManager.getCurrent().getLastAuthorizationResponse();
            if (lastResp != null) {
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

        if (resp == null) {
            Log.d(TAG, ex.toString());
            listener.onFailure(ex);
            return;
        }
        mAccountBuilder.setServerAuthCode(resp.authorizationCode);

        mAuthService.performTokenRequest(resp.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {
            @Override
            public void onTokenRequestCompleted(@Nullable TokenResponse response, @Nullable AuthorizationException ex) {
                if (response == null) {
                    Log.d(TAG, "authorization failed, check ex for more details", ex);
                    listener.onFailure(ex != null ? ex : UNKNOWN_ERROR);
                    return;
                }
                // exchange succeeded
                mAuthStateManager.updateAfterTokenResponse(response, ex);
                mAuthStateManager.getCurrent().performActionWithFreshTokens(mAuthService, new AuthState.AuthStateAction() {
                    @Override
                    public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException ex) {
                        fetchUserInfo(accessToken, ex, listener);
                    }
                });
            }
        });
    }

    private void fetchUserInfo(String accessToken, AuthorizationException ex, OnSignInListener listener) {
        Log.d(TAG, "fetchUserInfo");
        if (ex != null) {
            Log.e(TAG, "Token refresh failed when fetching user info");
            listener.onFailure(ex);
            return;
        }

        AuthorizationServiceDiscovery discovery =
                mAuthStateManager.getCurrent()
                        .getAuthorizationServiceConfiguration()
                        .discoveryDoc;

        Uri userInfoEndpoint =
                mConfiguration.getUserInfoEndpointUri() != null
                        ? Uri.parse(mConfiguration.getUserInfoEndpointUri().toString())
                        : Uri.parse(discovery.getUserinfoEndpoint().toString());

        Log.d(TAG, userInfoEndpoint.toString());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                HttpURLConnection conn = mConfiguration.getConnectionBuilder().openConnection(
                        userInfoEndpoint);
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setInstanceFollowRedirects(false);
                String response = Okio.buffer(Okio.source(conn.getInputStream()))
                        .readString(StandardCharsets.UTF_8);
                listener.onSuccess(mAccountBuilder.fromJson(response).build());
            } catch (IOException ioEx) {
                Log.e(TAG, "Network error when querying userinfo endpoint", ioEx);
                listener.onFailure(ioEx);
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
