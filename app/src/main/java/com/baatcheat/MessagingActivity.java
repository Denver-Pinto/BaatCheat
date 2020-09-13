package com.baatcheat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.baatcheat.adapter.MessageListAdapter;
import com.baatcheat.model.Message;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

public class MessagingActivity extends AppCompatActivity {

    private static final String TAG = "Custom: Messaging";

    private String senderID, receiverID, receiverName;
    private Uri receiverImageURI;
    private ArrayList<Message> messageList;
    MessageListAdapter messageListAdapter;

    //Views:
    TextInputEditText messagingEditText;
    RecyclerView messagesRecyclerView;
    public ImageButton arButton, userProfileButton;
    MaterialTextView nameTextView;

    final String arAppPackage="com.anxit.baatcheat.ARFlags";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);

        receiverID = "";
        if(getIntent().hasExtra("UserID"))
            receiverID = getIntent().getStringExtra("UserID");

        receiverName = "";
        if(getIntent().hasExtra("userName"))
            receiverName = getIntent().getStringExtra("userName");

        receiverImageURI = null;
        if(getIntent().hasExtra("userImageURI"))
            receiverImageURI = Uri.parse(getIntent().getStringExtra("userImageURI"));

        senderID = FirebaseAuth.getInstance().getUid();

        //Initializing views:
        messagingEditText = findViewById(R.id.messaging_edit_text);
        userProfileButton = findViewById(R.id.user_profile);
        nameTextView = findViewById(R.id.name_textview);
        nameTextView.setText(receiverName);
        if(receiverImageURI!=null){
            FirebaseStorageInterface storageInterface = new FirebaseStorageInterface();
            storageInterface.displayImage(receiverImageURI, userProfileButton);
        }

        arButton=findViewById(R.id.arButton);
        arButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirestoreInterface fs=new FirestoreInterface();
                fs.openArApplication(getApplicationContext(),arAppPackage,receiverID);
                //startNewActivity(getApplicationContext(),arAppPackage,);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        initializeRecyclerView();

        DatabaseReference messageDatabase = FirebaseDatabase.getInstance().getReference()
                .child("messages").child(getMessagingID());

         messageDatabase.addChildEventListener(new ChildEventListener() {
             @Override
             public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                 messageList.add(dataSnapshot.getValue(Message.class));
             }

             @Override
             public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
             }

             @Override
             public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
             }

             @Override
             public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
             }

             @Override
             public void onCancelled(@NonNull DatabaseError databaseError) {
             }
         });
    }

    private void initializeRecyclerView(){
        messageList = new ArrayList<>();

        messagesRecyclerView = findViewById(R.id.messages_recycler_view);
        messagesRecyclerView.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager =  new LinearLayoutManager(this);
        messagesRecyclerView.setLayoutManager(linearLayoutManager);

        messageListAdapter = new MessageListAdapter(messageList);
        messagesRecyclerView.setAdapter(messageListAdapter);
    }

    public void sendClicked(View view){
        String message = Objects.requireNonNull(messagingEditText.getText()).toString();
        if(!"".equals(message))
            addMessage(message);
        messagingEditText.setText("");
    }

    private void addMessage(String message){
        Message newMessage = new Message(message, senderID, receiverID, new Date());
        DatabaseReference messagingDatabase = FirebaseDatabase.getInstance().getReference().child("messages");
        messagingDatabase.child(getMessagingID()).child(Long.toString(newMessage.getTimeStamp().getTime()))
                .setValue(newMessage);
    }

    private String getMessagingID(){
        if(senderID.compareTo(receiverID)<0)
            return senderID+"-"+receiverID;
        else
            return receiverID+"-"+senderID;
    }

    public void profileClicked(View view){
        Intent toDisplayProfile = new Intent(this, DisplayProfile.class);
        toDisplayProfile.putExtra("UserID", receiverID);
        startActivity(toDisplayProfile);
    }
}
