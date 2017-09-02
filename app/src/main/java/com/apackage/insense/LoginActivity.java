package com.apackage.insense;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import android.net.Uri;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.apackage.api.ServerConnection;
import com.apackage.api.ServerConnectionListener;
import com.apackage.db.DataBase;
import com.apackage.model.User;
import com.apackage.utils.Constants;

import java.util.Map;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements ServerConnectionListener {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    // Connectors
    private int request_code = 1000;
    private ServerConnection con;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final DataBase db = new DataBase(this);
        //db.close();
        //getApplicationContext().deleteDatabase("USERS");
        //getApplicationContext().deleteDatabase("SETTINGS");

        // Get the app's shared preferences
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mEmailView = (EditText) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });


        Button mRegisterButton = (Button) findViewById(R.id.register_button);
        mRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse(getString(R.string.register_url)));
                startActivity(browserIntent);
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        if(preferences.getBoolean("isLogged", false))
        {
            Intent intent = new Intent(LoginActivity.this,
                    HomeActivity.class);
            Bundle bundle = new Bundle();
            int userId = preferences.getInt("userID", 0);
            if(userId > 0)
            {
                if(db.isActiveUser(userId))
                {
                    User user = db.getActiveUser();
                    con = new ServerConnection(this, getApplicationContext());
                    con.execute(Constants.REQUEST_VALIDATE_TOKEN, user.getRefreshToken());
                }else{
                    showProgress(false);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.remove("userID").remove("isLogged").commit();
                }
            }else{
                showProgress(false);
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove("userID").remove("isLogged").commit();
            }
        }else{
            showProgress(false);
        }
    }

    @Override
    public void onConnectionError() {
        Toast.makeText(this, "Erro de Conexão", Toast.LENGTH_LONG).show();
        showProgress(false);
    }

    @Override
    public void onConnectionSuccess() {

    }

    @Override
    public void onConnectionError(Map<String, String> result) {
        Toast.makeText(this, result.get("error"), Toast.LENGTH_LONG).show();
        showProgress(false);
    }

    @Override
    public void onConnectionSuccess(Map<String, Object> result) {
        User user;
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        Intent intent;
        Bundle bundle;
        bundle = new Bundle();
        switch ((String)result.get("name"))
        {
            case Constants.REQUEST_LOGIN:
                showProgress(false);
                user = (User) result.get("result");
                editor.putInt("userID", user.getId());
                editor.putBoolean("isLogged", true);
                //salvar de forma sincrona
                editor.commit();
                intent = new Intent(LoginActivity.this,
                        HomeActivity.class);
                bundle.putInt("userID", user.getId());
                intent.putExtras(bundle);
                startActivity(intent);
                finish();
                break;
            case Constants.REQUEST_SETTINGS:
                break;
            case Constants.REQUEST_DEVICES:
                break;
            case Constants.REQUEST_REFRESH_TOKEN:
                break;
            case Constants.REQUEST_VALIDATE_TOKEN:
                user = (User) result.get("result");
                editor.putInt("userID", user.getId());
                editor.putBoolean("isLogged", true);
                //salvar de forma sincrona
                editor.commit();
                intent = new Intent(LoginActivity.this,
                        HomeActivity.class);
                bundle.putInt("userID", user.getId());
                intent.putExtras(bundle);
                startActivity(intent);
                finish();
                break;
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        con = new ServerConnection(this, getApplicationContext());

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            con.execute(Constants.REQUEST_LOGIN,email, password);
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 5;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}

