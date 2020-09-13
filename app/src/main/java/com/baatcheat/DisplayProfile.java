package com.baatcheat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.baatcheat.model.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Set;

public class DisplayProfile extends AppCompatActivity {
    public TableLayout whereToAddNewCards;

    String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_profile);
        FloatingActionButton messageUser=findViewById(R.id.messageUser);
        whereToAddNewCards=findViewById(R.id.tableToShowCards);
        final TextView nameValue=findViewById(R.id.nameValue);
        final TextView bioValue=findViewById(R.id.description);
        final ImageView profilePicture=findViewById(R.id.profilePicture);

        userID = "";
        if(getIntent().hasExtra("UserID"))
            userID = getIntent().getStringExtra("UserID");

        //retrieving given user
        final FirestoreInterface fs=new FirestoreInterface();
        fs.getUser(userID, new FirestoreDataInterface() {
            @Override
            public void onFirestoreCallBack(Set<User> usersAlreadyFound) {

            }

            @Override
            public void onUriObtained(Uri obtainedUri) {

            }

            @Override
            public void onUserCallback(User foundUser) {
                // we now have a user- lets display his details
                nameValue.setText(foundUser.getUserName());
                //bioValue.setText(foundUser.getEmail());
                if(foundUser.getUserImageURI()!=null){
                    //profilePicture.setImageURI(foundUser.getUserImageURI());
                    FirebaseStorageInterface storageInterface=new FirebaseStorageInterface();
                    storageInterface.displayImage(foundUser.getUserImageURI(),profilePicture);
                }
                if(foundUser.getBio()!=null){
                    bioValue.setText(foundUser.getBio());
                }
                else{
                    bioValue.setText(R.string.no_bio);
                }
                //retrieveal needs some improvement- need to check that out
                fs.retrieveCardsToDisplay(whereToAddNewCards,getApplicationContext(),DisplayProfile.this,foundUser);
            }
        });

        messageUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent toMessaging = new Intent(DisplayProfile.this, MessagingActivity.class);
                toMessaging.putExtra("UserID", userID);
                startActivity(toMessaging);
            }
        });
    }
}
