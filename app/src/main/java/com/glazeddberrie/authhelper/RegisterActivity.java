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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private EditText txt_emailReg, txt_passReg, txt_usernameReg, txt_passRegValid;
    private TextView txt_errorTextReg;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        txt_emailReg = findViewById(R.id.txt_servName);
        txt_passReg = findViewById(R.id.txt_passServ);
        txt_usernameReg = findViewById(R.id.txt_servDesc);
        txt_passRegValid = findViewById(R.id.txt_passServValid);
        txt_errorTextReg = findViewById(R.id.txt_errorTextEntry);
        Button btn_signUpReg = findViewById(R.id.btn_createServ);

        btn_signUpReg.setOnClickListener(view -> registerUser());
    }
    private void registerUser() {
        String email = txt_emailReg.getText().toString().trim();
        String password = txt_passReg.getText().toString().trim();
        String confirmPassword = txt_passRegValid.getText().toString().trim();
        String username = txt_usernameReg.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword) || TextUtils.isEmpty(username)) {
            showError("Fill the blank fields.");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();
                            String salt = generateSalt(uid);

                            String encryptionKey = generateKey(uid, salt);

                            Map<String, Object> user = new HashMap<>();
                            user.put("username", username);
                            user.put("email", email);
                            user.put("salt", salt);
                            user.put("encryptionKey", encryptionKey);


                            db.collection("users")
                                    .document(firebaseUser.getUid())
                                    .set(user)
                                    .addOnCompleteListener(dbTask -> {
                                        if (dbTask.isSuccessful()) {
                                            Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();

                                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            showError("Failed to save user data.");
                                        }
                                    });
                        }
                    } else {
                        showError("Registration failed. Try again.");
                    }
                });
    }

    private String generateSalt(String userUID) {
        if (userUID.length() > 16) {
            String salt = userUID.substring(16) + "marmalade";
            return salt;
        } else {
            return userUID + "marmalade";
        }
    }

    private String generateKey(String userUID, String salt) {
        String combined = userUID + salt;
        return combined.substring(0, Math.min(combined.length(), 16));
    }

    private void showError(String message) {
        txt_errorTextReg.setText(message);
        txt_errorTextReg.setVisibility(View.VISIBLE);
    }
}
