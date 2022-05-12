package ru.eneanuch.megafacebookapp.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Message;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.errorprone.annotations.Var;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.auth.User;

import java.util.List;
import java.util.Objects;

import ru.eneanuch.megafacebookapp.activities.ChatActivity;
import ru.eneanuch.megafacebookapp.databinding.ItemContainerRecivedMessageBinding;
import ru.eneanuch.megafacebookapp.databinding.UserItemRecentConversionBinding;
import ru.eneanuch.megafacebookapp.listeners.ConversionListener;
import ru.eneanuch.megafacebookapp.models.MessageModel;
import ru.eneanuch.megafacebookapp.models.UserModel;
import ru.eneanuch.megafacebookapp.other.Variables;

public class RecentConversationAdapter extends RecyclerView.Adapter<RecentConversationAdapter.ConversionViewHolder>{

    private final List<MessageModel> messages;
    private final ConversionListener conversionListener;

    public RecentConversationAdapter(List<MessageModel> messages, ConversionListener conversionListener) {
        this.messages = messages;
        this.conversionListener = conversionListener;
    }

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(
                UserItemRecentConversionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversionViewHolder holder, int position) {
        holder.setData(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class ConversionViewHolder extends RecyclerView.ViewHolder {

        UserItemRecentConversionBinding binding;

        public ConversionViewHolder(@NonNull UserItemRecentConversionBinding itemContainerRecentConversionBinding) {
            super(itemContainerRecentConversionBinding.getRoot());
            binding = itemContainerRecentConversionBinding;
        }

        void setData(MessageModel messageModel) {
            binding.userImage.setImageBitmap(getConversationImage(messageModel.conversionImage));
            binding.userName.setText(messageModel.conversionName);
            binding.recentMessage.setText(messageModel.message);
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection(Variables.USERS_TABLE).document(
                    messageModel.conversion
            ).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    boolean isOnline = false;
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (task.getResult().getLong(Variables.USER_ONLINE) != null) {
                            int online = Objects.requireNonNull(
                                    task.getResult().getLong(Variables.USER_ONLINE)
                            ).intValue();
                            isOnline = online == 1;
                        }
                        if (isOnline) {
                            binding.online.setVisibility(View.VISIBLE);
                        } else {
                            binding.online.setVisibility(View.GONE);
                        }
                    }
                }
            });

            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    UserModel user = new UserModel();
                    user.id = messageModel.conversion;
                    user.name = messageModel.conversionName;
                    user.image = messageModel.conversionImage;
                    conversionListener.onConversionClicked(user);
                }
            });

        }

    }

    private Bitmap getConversationImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
