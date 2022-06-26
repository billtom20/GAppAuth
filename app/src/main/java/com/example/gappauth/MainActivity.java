package com.example.gappauth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.gappauth.sdk.api.GAppAuth;
import org.gappauth.sdk.entity.GSignInAccount;
import org.gappauth.sdk.entity.GSignInOptions;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int RC_AUTH = 100;

    private TextView log;
    private GAppAuth appAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        log = findViewById(R.id.log);

        GSignInOptions signInOptions = new GSignInOptions.Builder(GSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode("")
                .requestEmail()
                .build();
        appAuth = new GAppAuth(this, signInOptions);

        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appAuth.signIn(RC_AUTH);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_AUTH) {
            appAuth.parseAuthResultFromIntent(data, new GAppAuth.OnSignInListener() {
                @Override
                public void onSuccess(@NonNull GSignInAccount account) {
                    log.append(account + "\r\n");
                }

                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "onFailure", e);
                    log.append("signIn fail" + "\r\n");
                }
            });
        }
    }

    public void signOut(View view) {
        appAuth.signOut(new GAppAuth.OnSignOutListener() {
            @Override
            public void onSuccess() {
                log.append("signOut success\r\n");
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "onFailure", e);
                log.append("signOut fail\r\n");
            }
        });
    }
}