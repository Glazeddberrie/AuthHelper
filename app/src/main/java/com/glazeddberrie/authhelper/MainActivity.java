package com.glazeddberrie.authhelper;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {
    private EditText txt_email, txt_password;
    private TextView txt_errorTextLog;
    private Button btn_logIn, btn_signUp;

    private FirebaseAuth mAuth;
    private Executor executor;
    private BiometricPrompt biometricPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        txt_email = findViewById(R.id.txt_email);
        txt_password = findViewById(R.id.txt_password);
        txt_errorTextLog = findViewById(R.id.txt_errorTextLog);

        btn_logIn = findViewById(R.id.btn_logIn);
        btn_signUp = findViewById(R.id.btn_signUp);

        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(MainActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(MainActivity.this, "Biometric Authentication Succeeded!", Toast.LENGTH_SHORT).show();
                String email = txt_email.getText().toString().trim();
                Intent intent = new Intent(MainActivity.this, ManageActivity.class);
                intent.putExtra("email", email);
                startActivity(intent);
                finish();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(MainActivity.this, "Authentication Failed. Please try again.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(MainActivity.this, "Authentication Error: " + errString, Toast.LENGTH_SHORT).show();
            }
        });

        btn_logIn.setOnClickListener(view -> {
            String email = txt_email.getText().toString().trim();
            String password = txt_password.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                showError("Email cannot be empty.");
                return;
            }

            if (TextUtils.isEmpty(password)) {
                showError("Password cannot be empty.");
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            showBiometricPrompt();
                        } else {
                            showError("Credentials are invalid, try again");
                        }
                    });
        });

        btn_signUp.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void showBiometricPrompt() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Login")
                .setSubtitle("Authenticate using your biometric credentials")
                .setNegativeButtonText("Use password")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void showError(String message) {
        txt_errorTextLog.setText(message);
        txt_errorTextLog.setVisibility(View.VISIBLE);
    }
}