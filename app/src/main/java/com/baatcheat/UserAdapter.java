package com.baatcheat;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baatcheat.model.User;

import java.util.List;
import java.util.Random;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserHolder> {
    private List<User> userList;
    UserAdapter(List<User> users){
        this.userList=users;
    }


    @NonNull
    @Override
    public UserHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView= LayoutInflater.
                from(parent.getContext())
                .inflate(R.layout.user_card_layout,parent,false);
        return new UserHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull UserHolder holder, int position) {
        User u=userList.get(position);
        holder.userName.setText(u.getUserName());
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    //viewHolder
    static class UserHolder extends RecyclerView.ViewHolder {
        TextView userName;
        UserHolder(View v){
            super(v);
            userName=v.findViewById(R.id.userName);

            //random color:
            Random rnd = new Random();
            int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
            userName.setBackgroundColor(color);
        }
    }
}
