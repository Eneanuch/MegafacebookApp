package ru.eneanuch.megafacebookapp.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.errorprone.annotations.Var;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import ru.eneanuch.megafacebookapp.other.PreferenceFunctions;
import ru.eneanuch.megafacebookapp.other.Variables;

public class BaseActivity extends AppCompatActivity {

    private DocumentReference documentReference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceFunctions preferenceFunctions = new PreferenceFunctions(getApplicationContext());
        FirebaseFirestore db=  FirebaseFirestore.getInstance();
        documentReference = db.collection(Variables.USERS_TABLE)
                .document(preferenceFunctions.getString(Variables.USER_ID));
    }

    @Override
    protected void onPause() {
        super.onPause();
        documentReference.update(Variables.USER_ONLINE, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        documentReference.update(Variables.USER_ONLINE, 1);

    }
}
