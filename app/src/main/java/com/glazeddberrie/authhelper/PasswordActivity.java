package com.glazeddberrie.authhelper;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class PasswordActivity extends AppCompatActivity {
    private TextView txt_userAssignedName, txt_serviceName;
    private EditText txt_showPass;
    private Button btn_showPass, btn_editPass, btn_delPass;
    private FirebaseFirestore db;
    private String userEmail, serviceName, userAssignedName, decryptedPassword;

    private boolean isPasswordVisible = false;
    private boolean isEditingPassword = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.password), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        serviceName = getIntent().getStringExtra("service_name");
        userAssignedName = getIntent().getStringExtra("user_assigned_name");
        userEmail = getIntent().getStringExtra("email");

        txt_userAssignedName = findViewById(R.id.txt_userAssignedName);
        txt_serviceName = findViewById(R.id.txt_serviceName);
        txt_showPass = findViewById(R.id.txt_showPass);
        btn_showPass = findViewById(R.id.btn_showPass);
        btn_editPass = findViewById(R.id.btn_editPass);
        btn_delPass = findViewById(R.id.btn_delPass);
        txt_serviceName.setText(serviceName);
        txt_userAssignedName.setText(userAssignedName);

        fetchPassword();

        btn_showPass.setOnClickListener(v -> togglePasswordVisibility());
        btn_editPass.setOnClickListener(v -> toggleEditMode());
        System.out.println(userEmail + " " + serviceName + " " + userAssignedName);
        btn_delPass.setOnClickListener(v -> deletePasswordByEmailNameDescription(userEmail, serviceName, userAssignedName));
    }

    private void fetchPassword() {
        db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                            String encryptionKey = documentSnapshot.getString("encryptionKey");

                            if (encryptionKey != null) {
                                fetchEncryptedPassword(encryptionKey);
                                break;
                            } else {
                                Toast.makeText(this, "Encryption key not found for the user.", Toast.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        Toast.makeText(this, "User not found for the given email.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error retrieving encryption key: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void fetchEncryptedPassword(String encryptionKey) {
        db.collection("passwords")
                .whereEqualTo("email", userEmail)
                .whereEqualTo("service", serviceName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                            String encryptedPassword = documentSnapshot.getString("password");

                            if (encryptedPassword != null) {
                                try {
                                    decryptedPassword = decrypt(encryptedPassword, encryptionKey);

                                    txt_showPass.setText(decryptedPassword);

                                    Toast.makeText(this, "Password retrieved and decrypted successfully.", Toast.LENGTH_SHORT).show();

                                    break;
                                } catch (Exception e) {
                                    Toast.makeText(this, "Decryption failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(this, "Encrypted password not found.", Toast.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        Toast.makeText(this, "Password entry not found for the given email and service.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error retrieving encrypted password: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    private String decrypt(String data, String secretKey) throws Exception {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length > 16) {
            keyBytes = Arrays.copyOfRange(keyBytes, 0, 16);
        } else if (keyBytes.length < 16) {
            keyBytes = Arrays.copyOf(keyBytes, 16);
        }
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decodedBytes = Base64.decode(data, Base64.DEFAULT);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    private String encrypt(String data, String secretKey) throws Exception {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length > 16) {
            keyBytes = Arrays.copyOfRange(keyBytes, 0, 16);
        } else if (keyBytes.length < 16) {
            keyBytes = Arrays.copyOf(keyBytes, 16);
        }
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
    }

    private void encryptAndUpdatePassword() {
        String newPassword = txt_showPass.getText().toString();

        if (newPassword.isEmpty()) {
            Toast.makeText(this, "Password cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        retrieveEncryptionKeyAndUpdatePassword(newPassword);
    }

    private void retrieveEncryptionKeyAndUpdatePassword(final String newPassword) {
        db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        String encryptionKey = documentSnapshot.getString("encryptionKey");

                        if (encryptionKey != null) {
                            try {
                                String encryptedPassword = encrypt(newPassword, encryptionKey);

                                Map<String, Object> passwordUpdate = new HashMap<>();
                                passwordUpdate.put("password", encryptedPassword);

                                db.collection("passwords")
                                        .whereEqualTo("email", userEmail)
                                        .whereEqualTo("service", serviceName)
                                        .get()
                                        .addOnSuccessListener(passwordQueryDocumentSnapshots -> {
                                            if (!passwordQueryDocumentSnapshots.isEmpty()) {
                                                DocumentSnapshot passwordDocSnapshot = passwordQueryDocumentSnapshots.getDocuments().get(0);
                                                String documentId = passwordDocSnapshot.getId();

                                                db.collection("passwords").document(documentId).update(passwordUpdate)
                                                        .addOnSuccessListener(aVoid -> {
                                                            Toast.makeText(this, "Password updated successfully.", Toast.LENGTH_SHORT).show();
                                                            fetchPassword();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(this, "Error updating password: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                        });
                                            } else {
                                                Toast.makeText(this, "Password entry not found for the given email and service.", Toast.LENGTH_LONG).show();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Error retrieving password entry: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });

                            } catch (Exception e) {
                                Toast.makeText(this, "Encryption failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(this, "Encryption key not found.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "User entry not found for the given email.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error retrieving encryption key: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            txt_showPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btn_showPass.setText("Show Password");
            btn_editPass.setEnabled(true);
        } else {
            txt_showPass.setInputType(InputType.TYPE_CLASS_TEXT);
            btn_showPass.setText("Hide Password");
            btn_editPass.setEnabled(false);
        }
        isPasswordVisible = !isPasswordVisible;
    }


    private void toggleEditMode() {
        if (isEditingPassword) {
            txt_showPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            txt_showPass.setEnabled(false);
            encryptAndUpdatePassword();
            btn_editPass.setText("Edit Password");
            btn_showPass.setEnabled(true);
        } else {
            btn_showPass.setEnabled(false);
            txt_showPass.setEnabled(true);
            txt_showPass.setInputType(InputType.TYPE_CLASS_TEXT);
            btn_editPass.setText("Save Password");
        }
        isEditingPassword = !isEditingPassword;
    }

    private void deletePasswordByEmailNameDescription(final String email, final String service, final String name) {
        // Create a confirmation dialog
        new AlertDialog.Builder(PasswordActivity.this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this password?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Proceed with the deletion if the user confirms
                    db.collection("passwords")
                            .whereEqualTo("email", email)  // Match the email
                            .whereEqualTo("service", service)    // Match the name
                            .whereEqualTo("name", name)  // Match the description
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    // Loop through the documents and delete the matching password entry
                                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                                        String passwordDocumentId = documentSnapshot.getId();

                                        // Delete the matching password document
                                        db.collection("passwords").document(passwordDocumentId)
                                                .delete()
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(PasswordActivity.this, "Password deleted successfully.", Toast.LENGTH_SHORT).show();
                                                    Intent intent = new Intent(PasswordActivity.this, ManageActivity.class);
                                                    intent.putExtra("email", email);
                                                    startActivity(intent);
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(PasswordActivity.this, "Error deleting password: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                });
                                    }
                                } else {
                                    Toast.makeText(PasswordActivity.this, "Password not found for the given email, name, and description.", Toast.LENGTH_LONG).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(PasswordActivity.this, "Error retrieving passwords: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Dismiss the dialog if the user cancels the action
                    dialog.dismiss();
                })
                .show();
    }
}