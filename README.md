# GAppAuth

GAppAuth is an SDK for applications installed on Android devices like phones, tablets use Google's OAuth 2.0 endpoints to authorize access to Google APIs.

## Usage

1. An OAuth2 client ID for Google Sign In must be created. The
   [quick-start configurator](https://goo.gl/pl2Fu2) can be used to generate this, or it can be
   done directly on the
   [Google Developer Console](https://console.developers.google.com/apis/credentials?project=_).
   Or refer to this [document](https://developers.google.cn/identity/protocols/oauth2/native-app#prerequisites)

2. Add codes below in project build.gradle file.
```java
allprojects {
	repositories {
		...
		maven { url 'https://www.jitpack.io' }
	}
}
```

3. Add codes below in app build.gradle file.
```java
dependencies {
    implementation 'com.github.billtom20:GAppAuth:1.0'
    ...
}
```

API description
1. SignIn
```java
GSignInOptions signInOptions = new GSignInOptions.Builder(GSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode("")
                .requestEmail()
                .build();
GAppAuth appAuth = new GAppAuth(this, signInOptions);

findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        appAuth.signIn(RC_AUTH);
    }
});
```

2. Process SignIn results
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == RC_AUTH) {
        appAuth.parseAuthResultFromIntent(data, new GAppAuth.OnSignInListener() {
            @Override
            public void onSuccess(@NonNull GSignInAccount account) {
                Log.d(TAG, account.toString();
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "onFailure", e);
            }
        });
    }
}
```

3. SignOut
```java
appAuth.signOut(new GAppAuth.OnSignOutListener() {
    @Override
    public void onSuccess() {
        log.append("signOut success\r\n");
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        Log.e(TAG, "onFailure", e);
    }
});
```