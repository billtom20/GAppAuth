package org.gappauth.sdk.entity;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class GSignInAccount {

    private final String jsonResult;
    private final String id;
    private final String email;
    private final String displayName;
    private final String givenName;
    private final Uri photoUrl;
    private final String serverAuthCode;

    private GSignInAccount(String jsonResult, String id, String email, String displayName, String givenName, Uri photoUrl, String serverAuthCode) {
        this.jsonResult = jsonResult;
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.givenName = givenName;
        this.photoUrl = photoUrl;
        this.serverAuthCode = serverAuthCode;
    }

    public String getJsonResult() {
        return jsonResult;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getGivenName() {
        return givenName;
    }

    public Uri getPhotoUrl() {
        return photoUrl;
    }

    public String getServerAuthCode() {
        return serverAuthCode;
    }

    @NonNull
    @Override
    public String toString() {
        return "GSignInAccount{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", givenName='" + givenName + '\'' +
                ", photoUrl=" + photoUrl +
                ", serverAuthCode='" + serverAuthCode + '\'' +
                '}';
    }

    public static final class Builder {
        private static final String TAG = Builder.class.getSimpleName();

        private String jsonResult;
        private String id;
        private String email;
        private String displayName;
        private String givenName;
        private Uri photoUrl;
        private String serverAuthCode;

        @NonNull
        public GSignInAccount.Builder fromJson(String jsonStr) {
            this.jsonResult = jsonStr;
            try {
                JSONObject jsonObject = new JSONObject(jsonStr);
                this.id = jsonObject.optString("sub", "");
                this.email = jsonObject.optString("email", "");
                this.displayName = jsonObject.optString("name", "");
                this.givenName = jsonObject.optString("given_name", "");
                this.photoUrl = Uri.parse(jsonObject.optString("picture", ""));
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse userinfo response", e);
            }
            return this;
        }

        @NonNull
        public GSignInAccount.Builder setServerAuthCode(String serverAuthCode) {
            this.serverAuthCode = serverAuthCode;
            return this;
        }

        @NonNull
        public GSignInAccount build() {
            if (this.jsonResult == null) {
                throw new IllegalArgumentException("No User Info");
            }
            return new GSignInAccount(this.jsonResult, this.id, this.email, this.displayName,
                    this.givenName, this.photoUrl, this.serverAuthCode);
        }
    }
}
