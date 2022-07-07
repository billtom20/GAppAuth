package org.gappauth.sdk.entity;

import androidx.annotation.NonNull;

import net.openid.appauth.ResponseTypeValues;

import org.gappauth.sdk.BuildConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GSignInOptions {
    private final String clientId;
    private final List<String> scopes;
    private final String responseType;

    @NonNull
    public static final List<String> DEFAULT_SIGN_IN = new ArrayList<String>() {
        {
            add("profile");
            add("openid");
        }
    };

    @NonNull
    public static final List<String> DEFAULT_GAMES_SIGN_IN = new ArrayList<String>() {
        {
            add("https://www.googleapis.com/auth/games_lite");
        }
    };

    private GSignInOptions(String clientId, List<String> scopes, String responseType) {
        this.clientId = clientId;
        this.scopes = scopes;
        this.responseType = responseType;
    }

    @NonNull
    public String getClientId() {
        return clientId;
    }

    public String getScopes() {
        StringBuffer stringBuffer = new StringBuffer();
        for (String scope : scopes) {
            stringBuffer.append(scope).append(" ");
        }
        return stringBuffer.toString().trim();
    }

    @NonNull
    public String getResponseType() {
        return responseType;
    }

    public static final class Builder {
        private final Set<String> scopes;

        @NonNull
        public GSignInOptions.Builder requestEmail() {
            this.scopes.add("email");
            return this;
        }

        @NonNull
        public GSignInOptions.Builder requestId() {
            this.scopes.add("openid");
            return this;
        }

        @NonNull
        public GSignInOptions.Builder requestProfile() {
            this.scopes.add("profile");
            return this;
        }

        @NonNull
        public GSignInOptions.Builder requestScopes(@NonNull String scope, @NonNull String... scopes) {
            this.scopes.add(scope);
            this.scopes.addAll(Arrays.asList(scopes));
            return this;
        }

        @NonNull
        public GSignInOptions build() {
            ArrayList<String> var2 = new ArrayList<>(this.scopes);
            return new GSignInOptions(BuildConfig.GOOGLE_CLIENT_ID, var2, ResponseTypeValues.CODE);
        }

        public Builder() {
            this.scopes = new HashSet<>();
        }

        public Builder(@NonNull List<String> googleSignInOptions) {
            this();
            scopes.addAll(googleSignInOptions);
        }
    }
}
