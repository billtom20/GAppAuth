package org.gappauth.sdk;

import static net.openid.appauth.AuthorizationException.EXTRA_EXCEPTION;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class BridgeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bridge);

        Intent result = new Intent();
        if (getIntent() != null) {
            result.putExtra(EXTRA_EXCEPTION, getIntent().getStringExtra(EXTRA_EXCEPTION));
        }

        setResult(RESULT_OK, result);
        finish();
    }
}