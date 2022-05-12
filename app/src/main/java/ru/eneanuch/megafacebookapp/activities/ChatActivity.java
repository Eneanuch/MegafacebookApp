package ru.eneanuch.megafacebookapp.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.errorprone.annotations.Var;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.eneanuch.megafacebookapp.adapters.MessageAdapter;
import ru.eneanuch.megafacebookapp.databinding.ActivityChatBinding;
import ru.eneanuch.megafacebookapp.models.MessageModel;
import ru.eneanuch.megafacebookapp.models.UserModel;
import ru.eneanuch.megafacebookapp.network.ApiClient;
import ru.eneanuch.megafacebookapp.network.ApiService;
import ru.eneanuch.megafacebookapp.other.PreferenceFunctions;
import ru.eneanuch.megafacebookapp.other.Variables;

public class ChatActivity extends BaseActivity {

    private ActivityChatBinding binding;
    private UserModel receiverUser;
    private List<MessageModel> messages;
    private MessageAdapter messageAdapter;
    private PreferenceFunctions preferenceFunctions;
    private FirebaseFirestore db;
    private String conversionId;
    private Boolean isReceiverOnline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverData();
        init();
        listenerMessages();
    }

    private void init() {
        preferenceFunctions = new PreferenceFunctions(getApplicationContext());
        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(
                messages,
                getBitmapFromEncodedString(receiverUser.image),
                preferenceFunctions.getString(Variables.USER_ID));
        binding.chatRecycleView.setAdapter(messageAdapter);
        db = FirebaseFirestore.getInstance();
    }

    private void sendMessage() {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Variables.SENDER_ID, preferenceFunctions.getString(Variables.USER_ID));
        message.put(Variables.RECEIVER_ID, receiverUser.id);
        message.put(Variables.MESSAGE, binding.inputMessage.getText().toString());
        message.put(Variables.DATETIME, new Date());
        db.collection(Variables.MESSAGES_TABLE).add(message);
        if (conversionId != null) {
            updateConversion(binding.inputMessage.getText().toString());
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Variables.SENDER_ID, preferenceFunctions.getString(Variables.USER_ID));
            conversion.put(Variables.SENDER_NAME, preferenceFunctions.getString(Variables.USER_NAME));
            conversion.put(Variables.SENDER_IMAGE, preferenceFunctions.getString(Variables.USER_IMAGE));
            conversion.put(Variables.RECEIVER_ID, receiverUser.id);
            conversion.put(Variables.RECEIVER_NAME, receiverUser.name);
            conversion.put(Variables.RECEIVER_IMAGE, receiverUser.image);
            conversion.put(Variables.LAST_MESSAGE, binding.inputMessage.getText().toString());
            conversion.put(Variables.DATETIME, new Date());
            addConversion(conversion);
        }
        if (!isReceiverOnline) {
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.token);

                JSONObject data = new JSONObject();
                data.put(Variables.USER_ID, preferenceFunctions.getString(Variables.USER_ID));
                data.put(Variables.USER_NAME, preferenceFunctions.getString(Variables.USER_NAME));
                data.put(Variables.TOKEN, preferenceFunctions.getString(Variables.TOKEN));
                data.put(Variables.MESSAGE, binding.inputMessage.getText().toString());

                JSONObject body = new JSONObject();
                body.put(Variables.MSG_DATA, data);
                body.put(Variables.MSG_REGISTER_IDS, tokens);

                sendNotification(body.toString());
            } catch (Exception exception) {
                messageToast(exception.getMessage());
            }
        }
        binding.inputMessage.setText("");
    }

    private void messageToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String msg) {
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Variables.getMsgHeaders(),
                msg
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    try {
                        if (response.body() != null) {
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray result = responseJson.getJSONArray("results");
                            if (responseJson.getInt("failure") == 1) {
                                JSONObject error = (JSONObject) result.get(0);
                                messageToast(error.getString("error"));
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    messageToast("Уведомление отправлено");
                } else {
                    messageToast("Error " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                messageToast(t.getMessage());
            }
        });
    }

    private void listenerMessages() {
        db.collection(Variables.MESSAGES_TABLE)
                .whereEqualTo(Variables.SENDER_ID, preferenceFunctions.getString(Variables.USER_ID))
                .whereEqualTo(Variables.RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);
        db.collection(Variables.MESSAGES_TABLE)
                .whereEqualTo(Variables.SENDER_ID, receiverUser.id)
                .whereEqualTo(Variables.RECEIVER_ID, preferenceFunctions.getString(Variables.USER_ID))
                .addSnapshotListener(eventListener);
    }

    private void listenerOnlineReceiver() {
        db.collection(Variables.USERS_TABLE).document(
                receiverUser.id
        ).addSnapshotListener(ChatActivity.this, (value, error) -> {
           if (error != null) {
               return;
           }
           if (value != null) {
               if (value.getLong(Variables.USER_ONLINE) != null) {
                   int online = Objects.requireNonNull(
                        value.getLong(Variables.USER_ONLINE)
                   ).intValue();
                   isReceiverOnline = online == 1;
               }
               receiverUser.token = value.getString(Variables.TOKEN);
               if (receiverUser.image == null) {
                   receiverUser.image = value.getString(Variables.USER_IMAGE);
                   messageAdapter.setReceiverProfileImage(getBitmapFromEncodedString(receiverUser.image));
                   messageAdapter.notifyItemRangeChanged(0, messages.size());
               }
           }
           if (isReceiverOnline) {
               binding.online.setVisibility(View.VISIBLE);
           } else {
               binding.online.setVisibility(View.GONE);
           }
        });
    }

    private final EventListener<QuerySnapshot> eventListener = ((value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = messages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    MessageModel messageModel = new MessageModel();
                    messageModel.senderId = documentChange.getDocument().getString(Variables.SENDER_ID);
                    messageModel.receiverId = documentChange.getDocument().getString(Variables.RECEIVER_ID);
                    messageModel.message = documentChange.getDocument().getString(Variables.MESSAGE);
                    messageModel.dateTime = getNormalDate(documentChange.getDocument().getDate(Variables.DATETIME));
                    messageModel.date = documentChange.getDocument().getDate(Variables.DATETIME);
                    messages.add(messageModel);
                }
            }
            messages.sort((obj1, obj2) -> obj1.date.compareTo(obj2.date));
            if (count == 0) {
                messageAdapter.notifyDataSetChanged();
            } else {
                messageAdapter.notifyItemRangeInserted(messages.size(), messages.size());
                binding.chatRecycleView.smoothScrollToPosition(messages.size() - 1);
            }

        }
        if (conversionId == null) {
            checkForConversion();
        }
    });

    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }

    private void loadReceiverData() {
        receiverUser = (UserModel) getIntent().getSerializableExtra(Variables.USER);
        binding.nickname.setText(receiverUser.name);
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        binding.layoutSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });
    }

    private String getNormalDate(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversion(HashMap<String, Object> conversion) {
        db.collection(Variables.CONVERSATIONS_TABLE)
                .add(conversion)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        conversionId = documentReference.getId();
                    }
                });
    }

    private void updateConversion(String message) {
        DocumentReference documentReference =
                db.collection(Variables.CONVERSATIONS_TABLE).document(conversionId);
        documentReference.update(
                Variables.LAST_MESSAGE, message,
                Variables.DATETIME, new Date()
        );
    }

    private void checkForConversion() {
        if (messages.size() != 0) {
            checkFromConversionRemotely(
                    preferenceFunctions.getString(Variables.USER_ID),
                    receiverUser.id
            );
            checkFromConversionRemotely(
                    receiverUser.id,
                    preferenceFunctions.getString(Variables.USER_ID)
            );
        }
    }

    private void checkFromConversionRemotely(String senderId, String receiverId) {
        db.collection(Variables.CONVERSATIONS_TABLE)
                .whereEqualTo(Variables.SENDER_ID, senderId)
                .whereEqualTo(Variables.RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);
    }


    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = new OnCompleteListener<QuerySnapshot>() {
        @Override
        public void onComplete(@NonNull Task<QuerySnapshot> task) {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                conversionId = documentSnapshot.getId();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenerOnlineReceiver();
    }
}