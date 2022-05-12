package ru.eneanuch.megafacebookapp.activities;

import static ru.eneanuch.megafacebookapp.other.Variables.CONVERSATIONS_TABLE;
import static ru.eneanuch.megafacebookapp.other.Variables.DATETIME;
import static ru.eneanuch.megafacebookapp.other.Variables.LAST_MESSAGE;
import static ru.eneanuch.megafacebookapp.other.Variables.RECEIVER_ID;
import static ru.eneanuch.megafacebookapp.other.Variables.RECEIVER_IMAGE;
import static ru.eneanuch.megafacebookapp.other.Variables.RECEIVER_NAME;
import static ru.eneanuch.megafacebookapp.other.Variables.SENDER_ID;
import static ru.eneanuch.megafacebookapp.other.Variables.SENDER_IMAGE;
import static ru.eneanuch.megafacebookapp.other.Variables.SENDER_NAME;
import static ru.eneanuch.megafacebookapp.other.Variables.TOKEN;
import static ru.eneanuch.megafacebookapp.other.Variables.USER;
import static ru.eneanuch.megafacebookapp.other.Variables.USERS_TABLE;
import static ru.eneanuch.megafacebookapp.other.Variables.USER_ID;
import static ru.eneanuch.megafacebookapp.other.Variables.USER_IMAGE;
import static ru.eneanuch.megafacebookapp.other.Variables.USER_NAME;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import ru.eneanuch.megafacebookapp.adapters.RecentConversationAdapter;
import ru.eneanuch.megafacebookapp.databinding.ActivityMainBinding;
import ru.eneanuch.megafacebookapp.listeners.ConversionListener;
import ru.eneanuch.megafacebookapp.models.MessageModel;
import ru.eneanuch.megafacebookapp.models.UserModel;
import ru.eneanuch.megafacebookapp.other.PreferenceFunctions;
import ru.eneanuch.megafacebookapp.other.Variables;

public class MainActivity extends BaseActivity implements ConversionListener {

    private ActivityMainBinding binding;
    private PreferenceFunctions preferenceFunctions;
    private List<MessageModel> conversations;
    private RecentConversationAdapter conversationAdapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        preferenceFunctions = new PreferenceFunctions(getApplicationContext());

        init();
        getToken();
        loadUserData();
        setActionListeners();
        listenerConversations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        conversationAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        conversationAdapter.notifyDataSetChanged();
    }

    private void init() {
        conversations = new ArrayList<>();
        conversationAdapter = new RecentConversationAdapter(conversations, this);
        binding.conversationsRecycleView.setAdapter(conversationAdapter);
        db = FirebaseFirestore.getInstance();
    }

    private void loadUserData() {
        binding.userName.setText(preferenceFunctions.getString(USER_NAME));
        byte[] bytes = Base64.decode(preferenceFunctions.getString(USER_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.profileImage.setImageBitmap(bitmap);
    }

    private void setActionListeners() {
        binding.logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });
        binding.fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), UserActivity.class));
            }
        });
    }

    private void messageToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void listenerConversations() {
        db.collection(USERS_TABLE)
                .addSnapshotListener(((value, error) -> {
                    conversationAdapter.notifyDataSetChanged();
                }));
        db.collection(CONVERSATIONS_TABLE)
                .whereEqualTo(SENDER_ID, preferenceFunctions.getString(USER_ID))
                .addSnapshotListener(eventListener);
        db.collection(CONVERSATIONS_TABLE)
                .whereEqualTo(RECEIVER_ID, preferenceFunctions.getString(USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = ((value, error) -> {
       if (error != null) {
           return;
       }
       if (value != null) {
           for (DocumentChange documentChange : value.getDocumentChanges()) {
               if (documentChange.getType() == DocumentChange.Type.ADDED) {
                   String senderId = documentChange.getDocument().getString(SENDER_ID);
                   String receiverId = documentChange.getDocument().getString(RECEIVER_ID);
                   MessageModel messageModel = new MessageModel();
                   messageModel.senderId = senderId;
                   messageModel.receiverId = receiverId;
                   if (preferenceFunctions.getString(USER_ID).equals(senderId)) {
                       messageModel.conversionImage = documentChange.getDocument().getString(RECEIVER_IMAGE);
                       messageModel.conversionName = documentChange.getDocument().getString(RECEIVER_NAME);
                       messageModel.conversion = documentChange.getDocument().getString(RECEIVER_ID);
                   } else {
                       messageModel.conversionImage = documentChange.getDocument().getString(SENDER_IMAGE);
                       messageModel.conversionName = documentChange.getDocument().getString(SENDER_NAME);
                       messageModel.conversion = documentChange.getDocument().getString(SENDER_ID);
                   }
                   messageModel.message = documentChange.getDocument().getString(LAST_MESSAGE);
                   messageModel.date = documentChange.getDocument().getDate(DATETIME);
                   conversations.add(messageModel);
               } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                   for (int i = 0; i < conversations.size(); i++) {
                       String senderId = documentChange.getDocument().getString(SENDER_ID);
                       String receiverId = documentChange.getDocument().getString(RECEIVER_ID);
                       if (conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)) {
                           conversations.get(i).message = documentChange.getDocument().getString(LAST_MESSAGE);
                           conversations.get(i).date = documentChange.getDocument().getDate(DATETIME);
                           break;
                       }
                   }
               }
           }
           Collections.sort(conversations, (obj1, obj2) -> obj2.date.compareTo(obj1.date));
           conversationAdapter.notifyDataSetChanged();
           binding.conversationsRecycleView.smoothScrollToPosition(0);
       }
    });

    private void getToken() {
        FirebaseMessaging
                .getInstance()
                .getToken()
                .addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        preferenceFunctions.put(TOKEN, token);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String token_ =  preferenceFunctions.getString(USER_ID);

        if (token_.equals("")) {
            startActivity(new Intent(getApplicationContext(), AuthorizeActivity.class));
        }

        DocumentReference documentReference = db
                .collection(USERS_TABLE)
                .document(
                        token_
                );
        documentReference.update(TOKEN, token)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        messageToast("Обновлен токен для Firebase");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println("Ошибка");
                        System.out.println(e.getMessage());
                        messageToast("Не получилось обновить токен :(");
                    }
                });
    }

    private void logout() {
        messageToast("Выходим...");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentReference = db
                .collection(USERS_TABLE)
                .document(preferenceFunctions.getString(USER_ID)
                );

        HashMap<String, Object> userData = new HashMap<>();
        userData.put(TOKEN, FieldValue.delete());

        documentReference
                .update(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        messageToast("Токен удален");
                        preferenceFunctions.restore();
                        startActivity(new Intent(getApplicationContext(), AuthorizeActivity.class));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println("Ошибка");
                        System.out.println(e.getMessage());
                        messageToast("Ошибка при удалении токена");
                    }
                });
    }

    @Override
    public void onConversionClicked(UserModel user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(USER, user);
        startActivity(intent);
    }
}