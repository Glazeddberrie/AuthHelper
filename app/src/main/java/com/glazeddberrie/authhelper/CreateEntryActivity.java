package com.glazeddberrie.authhelper;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class CreateEntryActivity extends AppCompatActivity {
    private EditText txt_servName, txt_servDesc, txt_passServ, txt_passServValid;
    private TextView txt_errorTextEntry;
    private Button btn_createServ;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_entry);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.create_entry), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        txt_servName = findViewById(R.id.txt_servName);
        txt_servDesc = findViewById(R.id.txt_servDesc);
        txt_passServ = findViewById(R.id.txt_passServ);
        txt_passServValid = findViewById(R.id.txt_passServValid);
        txt_errorTextEntry = findViewById(R.id.txt_errorTextEntry);
        btn_createServ = findViewById(R.id.btn_createServ);

        userEmail = getIntent().getStringExtra("email");

        btn_createServ.setOnClickListener(v -> createPasswordEntry());
    }

    private void createPasswordEntry() {
        String serviceName = txt_servName.getText().toString().trim();
        String serviceDesc = txt_servDesc.getText().toString().trim();
        String password = txt_passServ.getText().toString().trim();
        String confirmPassword = txt_passServValid.getText().toString().trim();
        System.out.println(userEmail);

        if (TextUtils.isEmpty(serviceName) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            txt_errorTextEntry.setText("Please fill in all required fields.");
            txt_errorTextEntry.setEnabled(true);
            return;
        }

        if (!password.equals(confirmPassword)) {
            txt_errorTextEntry.setText("Passwords do not match.");
            txt_errorTextEntry.setEnabled(true);
            return;
        }

        db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String encryptionKey = null;
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                            encryptionKey = documentSnapshot.getString("encryptionKey");
                        }

                        if (encryptionKey != null) {
                            try {
                                String encryptedPassword = encrypt(password, encryptionKey);

                                PasswordEntry entry = new PasswordEntry(serviceName, serviceDesc, encryptedPassword, userEmail);

                                db.collection("passwords")
                                        .add(entry)
                                        .addOnSuccessListener(docRef -> {
                                            Toast.makeText(this, "Password entry created successfully.", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(CreateEntryActivity.this, ManageActivity.class);
                                            intent.putExtra("email", userEmail);
                                            startActivity(intent);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            txt_errorTextEntry.setText("Error saving password: " + e.getMessage());
                                            Toast.makeText(this, "Error saving password: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });

                            } catch (Exception e) {
                                txt_errorTextEntry.setText("Encryption error: " + e.getMessage());
                                Toast.makeText(this, "Encryption failed.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            txt_errorTextEntry.setText("Encryption key not found for user.");
                            Toast.makeText(this, "Encryption key not found.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        txt_errorTextEntry.setText("User not found.");
                        Toast.makeText(this, "User not found.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    txt_errorTextEntry.setText("Error retrieving encryption key: " + e.getMessage());
                    Toast.makeText(this, "Error retrieving encryption key.", Toast.LENGTH_LONG).show();
                });
    }

    private String encrypt(String data, String secretKey) throws Exception {
        SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
    }

    public static class PasswordEntry {
        private String service;
        private String name;
        private String password;
        private String email;

        public PasswordEntry() {
            // Default constructor required for Firestore
        }

        public PasswordEntry(String service, String name, String password, String email) {
            this.service = service;
            this.name = name;
            this.password = password;
            this.email = email;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public String getName() {
            return name;
        }

        public void setName(String serviceDesc) {
            this.name = serviceDesc;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}