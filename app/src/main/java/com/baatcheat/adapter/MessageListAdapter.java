package com.baatcheat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.paris.Paris;
import com.baatcheat.R;
import com.baatcheat.model.Message;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class MessageListAdapter extends RecyclerView.Adapter<MessageListAdapter.MessageViewHolder> {

    private ArrayList<Message> messageList;

    public MessageListAdapter(ArrayList<Message> messageList){
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_list_item, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message currentMessage = messageList.get(position);
        holder.messageTextView.setText(currentMessage.getMessage());
        if(currentMessage.getSenderID().equals(FirebaseAuth.getInstance().getUid())) {
            Paris.style(holder.messageTextView).apply(R.style.Widget_MyApp_TextView_Messaging_Sent);
            Paris.style(holder.messageContainerCardView).apply(R.style.Widget_MyApp_MaterialCardView_Sent);
        }
        else{
            Paris.style(holder.messageTextView).apply(R.style.Widget_MyApp_TextView_Messaging_Received);
            Paris.style(holder.messageContainerCardView).apply(R.style.Widget_MyApp_MaterialCardView_Received);
        }

    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {

        MaterialTextView messageTextView;
        MaterialCardView messageContainerCardView;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.message_textview);
            messageContainerCardView = itemView.findViewById(R.id.message_container_cardview);
        }
    }
}
