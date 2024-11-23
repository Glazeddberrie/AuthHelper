package com.glazeddberrie.authhelper;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.glazeddberrie.authhelper.Model.Password;
import com.glazeddberrie.authhelper.Model.PasswordAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class ManageActivity extends AppCompatActivity {
    private ListView listView;
    private TextView lbl_userGreetings;
    private String userEmail;
    private Button btn_createEntry;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.manage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        listView = findViewById(R.id.listView);
        lbl_userGreetings = findViewById(R.id.lbl_userGreetings);
        btn_createEntry = findViewById(R.id.btn_createEntry);
        userEmail = getIntent().getStringExtra("email");
        if (userEmail != null) {
            fetchUsernameForEmail(userEmail);
        } else {
            lbl_userGreetings.setText("Unknown error, try again later");
        }

        fetchPasswordsForUser(userEmail);

        btn_createEntry.setOnClickListener(view -> {
            Intent intent = new Intent(ManageActivity.this, CreateEntryActivity.class);
            intent.putExtra("email", userEmail);
            startActivity(intent);
            finish();

        });
    }
    private void fetchPasswordsForUser(String email) {
        db.collection("passwords")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            ArrayList<Password> passwordsList = new ArrayList<>();

                            for (DocumentSnapshot document : querySnapshot) {
                                String serviceName = document.getString("service");
                                String userAssignedName = document.getString("name");

                                if (serviceName != null && userAssignedName != null) {
                                    passwordsList.add(new Password(serviceName, userAssignedName));
                                }
                            }

                            PasswordAdapter adapter = new PasswordAdapter(ManageActivity.this, passwordsList);
                            listView.setAdapter(adapter);

                            listView.setOnItemClickListener((parent, view, position, id) -> {
                                Password selectedPassword = passwordsList.get(position);

                                Intent intent = new Intent(ManageActivity.this, PasswordActivity.class);

                                intent.putExtra("service_name", selectedPassword.getServiceName());
                                intent.putExtra("user_assigned_name", selectedPassword.getUserAssignedName());
                                intent.putExtra("email", email);

                                startActivity(intent);
                            });
                        } else {
                            Toast.makeText(this, "No passwords found for this email", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Error fetching passwords", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void fetchUsernameForEmail(String email) {
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String username = document.getString("username");

                        if (username != null) {
                            lbl_userGreetings.setText("Welcome " + username);
                        } else {
                            lbl_userGreetings.setText("Welcome (username not found)");
                        }
                    } else {
                        lbl_userGreetings.setText("No user found for this email");
                    }
                })
                .addOnFailureListener(e -> {
                    lbl_userGreetings.setText("Error retrieving username");
                    Toast.makeText(ManageActivity.this, "Error fetching username: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}