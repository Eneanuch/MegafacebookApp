package ru.eneanuch.megafacebookapp.activities;

import static ru.eneanuch.megafacebookapp.other.Variables.*;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import ru.eneanuch.megafacebookapp.databinding.ActivityAuthorizeBinding;
import ru.eneanuch.megafacebookapp.other.PreferenceFunctions;

public class AuthorizeActivity extends AppCompatActivity {

    private ActivityAuthorizeBinding binding;
    private PreferenceFunctions preferenceFunctions;

    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthorizeBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        preferenceFunctions = new PreferenceFunctions(getApplicationContext());

        if (preferenceFunctions.getBoolean(USER_AUTHORIZED)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }

        setActionListeners();
    }

    private void setActionListeners() {
        binding.createNewAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
            }
        });
        binding.signButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isFieldsValid() && !isLoading) {
                    isLoading = true;
                    authorize();
                }
            }
        });
    }

    private void authorize() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(USERS_TABLE)
                .whereEqualTo(USER_EMAIL, binding.singEmail.getText().toString())
                .whereEqualTo(USER_PASSWORD, binding.signPassword.getText().toString())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() &&
                                task.getResult() != null &&
                                task.getResult().getDocuments().size() > 0) {
                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);

                            preferenceFunctions.put(USER_AUTHORIZED, true);
                            preferenceFunctions.put(USER_ID, documentSnapshot.getId());
                            preferenceFunctions.put(USER_NAME, documentSnapshot.getString(USER_NAME));
                            preferenceFunctions.put(USER_IMAGE, documentSnapshot.getString(USER_IMAGE));

                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                            startActivity(intent);
                        } else {
                            isLoading = false;
                            messageToast("Ошибка");
                        }
                    }
                });

    }

    private void messageToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private Boolean isFieldsValid() {
        if (binding.singEmail.getText().toString().trim().isEmpty()) {
            messageToast("Введите Email");
            return false;
        }
        if (binding.signPassword.getText().toString().trim().isEmpty()) {
            messageToast("Введите парооль");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(binding.singEmail.getText().toString()).matches()) {
            messageToast("Не существует такого Email");
            return false;
        }
        return true;
    }

}