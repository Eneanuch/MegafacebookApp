package ru.eneanuch.megafacebookapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import ru.eneanuch.megafacebookapp.adapters.UserAdapter;
import ru.eneanuch.megafacebookapp.databinding.ActivityUserBinding;
import ru.eneanuch.megafacebookapp.listeners.UserListener;
import ru.eneanuch.megafacebookapp.models.UserModel;
import ru.eneanuch.megafacebookapp.other.PreferenceFunctions;
import ru.eneanuch.megafacebookapp.other.Variables;

public class UserActivity extends BaseActivity implements UserListener {

    private ActivityUserBinding binding;
    private PreferenceFunctions preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceFunctions(getApplicationContext());
        setListeners();
        getUsers();
    }

    private void setListeners() {

        binding.backImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void getUsers() {
        UserListener some = this;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Variables.USERS_TABLE)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        String currentUserId = preferenceManager.getString(Variables.USER_ID);
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<UserModel> users = new ArrayList<>();
                            for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                                if (currentUserId.equals(queryDocumentSnapshot.getId())) {
                                    continue;
                                }
                                UserModel user = new UserModel();
                                user.name = queryDocumentSnapshot.getString(Variables.USER_NAME);
                                user.email = queryDocumentSnapshot.getString(Variables.USER_EMAIL);
                                user.image = queryDocumentSnapshot.getString(Variables.USER_IMAGE);
                                user.token = queryDocumentSnapshot.getString(Variables.TOKEN);
                                user.id = queryDocumentSnapshot.getId();
                                users.add(user);
                            }
                            if (users.size() > 0) {
                                UserAdapter userAdapter = new UserAdapter(users, some);
                                binding.recyclerView.setAdapter(userAdapter);
                                binding.recyclerView.setVisibility(View.VISIBLE);
                            } else {
                                showError();
                            }
                        } else {
                            showError();
                        }
                    }
                });
    }

    private void showError() {
        Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUserClicked(UserModel user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Variables.USER, user);
        startActivity(intent);
        finish();
    }
}