package com.baatcheat.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baatcheat.FirebaseStorageInterface;
import com.baatcheat.R;
import com.baatcheat.model.User;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserViewHolder>{

    final private ArrayList<User> userData;
    private ItemClickListener itemClickListener;

    private static final String TAG = "C: UserListAdapter";

    public UserListAdapter(ArrayList<User> userData, ItemClickListener itemClickListener){
        this.userData = userData;
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_list_item, parent, false);
        return new UserViewHolder(view, itemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.usernameView.setText(userData.get(position).getUserName());
        if(userData.get(position).getUserImageURI()!=null) {
            FirebaseStorageInterface storageInterface = new FirebaseStorageInterface();
            storageInterface.displayImage(userData.get(position).getUserImageURI(), holder.userImageView);
        }
    }

    @Override
    public int getItemCount() {
        return userData.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ImageView userImageView;
        TextView usernameView;
        ItemClickListener itemClickListener;

        public UserViewHolder(@NonNull View itemView, ItemClickListener itemClickListener) {
            super(itemView);

            userImageView = itemView.findViewById(R.id.user_imageview);
            usernameView = itemView.findViewById(R.id.username_textview);
            this.itemClickListener = itemClickListener;

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            itemClickListener.onItemClicked(getAdapterPosition());
        }
    }

    public interface ItemClickListener{
        public  void onItemClicked(int itemPosition);
    }
}
