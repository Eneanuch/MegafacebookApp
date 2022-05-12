package ru.eneanuch.megafacebookapp.adapters;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Objects;

import ru.eneanuch.megafacebookapp.databinding.UserItemBinding;
import ru.eneanuch.megafacebookapp.listeners.UserListener;
import ru.eneanuch.megafacebookapp.models.UserModel;
import ru.eneanuch.megafacebookapp.other.Variables;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UsersViewHolder>{

    private final List<UserModel> users;
    private final UserListener userListener;

    public UserAdapter(List<UserModel> users, UserListener userListener) {
        this.users = users;
        this.userListener = userListener;
    }


    @NonNull
    @Override
    public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        UserItemBinding binding = UserItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new UsersViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UsersViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public class UsersViewHolder extends RecyclerView.ViewHolder {

        UserItemBinding binding;

        public UsersViewHolder(UserItemBinding userItemBinding) {
            super(userItemBinding.getRoot());
            binding = userItemBinding;

        }

        @SuppressLint("NotifyDataSetChanged")
        void setUserData(UserModel user) {
            binding.userName.setText(user.name);
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection(Variables.USERS_TABLE).document(
                    user.id
            ).addSnapshotListener(((value, error) -> { notifyDataSetChanged(); }));
            db.collection(Variables.USERS_TABLE).document(
                    user.id
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
            binding.userImage.setImageBitmap(getProfileImages(user.image));
            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    userListener.onUserClicked(user);
                }
            });
        }
    }

    private Bitmap getProfileImages(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
