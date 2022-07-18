package com.example.hidden_treasures.login;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.hidden_treasures.R;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

public class SignUpActivity extends AppCompatActivity {

    public static final String TAG = "SignUpActivity";

    private EditText etUsername;
    private EditText etPassword;
    private Button btnCreateAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);

        setOnClickListeners();
    }

    private void setOnClickListeners() {
        // Create Account button
        btnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString();
                String password = etPassword.getText().toString();

                // create a new parse user
                ParseUser user = new ParseUser();
                user.setUsername(username);
                user.setPassword(password);

                // Save new user to Parse
                user.signUpInBackground(new SignUpCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            Toast.makeText(SignUpActivity.this, "Account created!", Toast.LENGTH_SHORT).show();
                            // redirect to login activity after saving user
                            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                        } else {
                            Toast.makeText(SignUpActivity.this, "Unable to create account", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }
}