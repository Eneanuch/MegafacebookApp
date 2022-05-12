package ru.eneanuch.megafacebookapp.activities;

import static ru.eneanuch.megafacebookapp.other.Variables.*;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

import ru.eneanuch.megafacebookapp.databinding.ActivityRegisterBinding;
import ru.eneanuch.megafacebookapp.other.PreferenceFunctions;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private PreferenceFunctions preferenceFunctions;
    private String imageEncoded;
    private EditText[] fields;

    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        preferenceFunctions = new PreferenceFunctions(getApplicationContext());

        setActionListeners();
        doFields();
    }

    private void setActionListeners() {
        binding.haveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        binding.signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isFieldsValid() && !isLoading) {
                    isLoading = true;
                    registration();
                }
            }
        });
        binding.regImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getImage.launch(intent);
            }
        });
    }

    private void doFields() {
        fields = new EditText[]{binding.singUpName, binding.singUpEmail,
                binding.signUpPassword, binding.signUpConfirmPassword};
    }

    private void messageToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }


    private void registration() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        HashMap<String, Object> userData = new HashMap<>();

        userData.put(USER_NAME, fields[0].getText().toString());
        userData.put(USER_EMAIL, fields[1].getText().toString());
        userData.put(USER_PASSWORD, fields[2].getText().toString());
        userData.put(USER_IMAGE, imageEncoded);

        db.collection(USERS_TABLE)
                .add(userData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {

                        preferenceFunctions.put(USER_AUTHORIZED, true);
                        preferenceFunctions.put(USER_ID, documentReference.getId());
                        preferenceFunctions.put(USER_NAME, fields[0].getText().toString());
                        preferenceFunctions.put(USER_EMAIL, fields[1].getText().toString());
                        preferenceFunctions.put(USER_IMAGE, imageEncoded);
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                        startActivity(intent);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        messageToast("Ошибка: " + e.getMessage());
                        isLoading = false;
                    }
                });
    }

    private String getImageEncoded(Bitmap bitmap) {
        int imageWidth = IMAGE_WIDTH;
        int imageHeight = bitmap.getHeight() * imageWidth / bitmap.getWidth();
        Bitmap newImage = Bitmap.createScaledBitmap(bitmap, imageWidth, imageHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        newImage.compress(Bitmap.CompressFormat.PNG, IMAGE_QUALITY, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }


    private final ActivityResultLauncher<Intent> getImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        if (result.getData() != null) {
                            Uri uri = result.getData().getData();
                            try {
                                InputStream inputStream = getContentResolver().openInputStream(uri);
                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                binding.regImage.setImageBitmap(bitmap);

                                imageEncoded = getImageEncoded(bitmap);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
    );


    private Boolean isFieldsValid() {
        if (imageEncoded == null) {
            messageToast("Загрузите изображение своего профиля");
            return false;
        }
        for (EditText field: fields) {
            if (field.getText().toString().trim().isEmpty()) {
                messageToast("Заполните поле: " + field.getHint());
                return false;
            }
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(binding.singUpEmail.getText().toString()).matches()) {
            messageToast("Не существует такого Email");
            return false;
        }
        return binding.signUpPassword.getText().toString()
                .equals(binding.signUpConfirmPassword.getText().toString());
    }
}