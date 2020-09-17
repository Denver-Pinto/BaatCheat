package com.baatcheat;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.baatcheat.model.Card;
import com.baatcheat.model.Interest;
import com.baatcheat.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.myhexaville.smartimagepicker.ImagePicker;
import com.myhexaville.smartimagepicker.OnImagePickedListener;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.baatcheat.Homepage.currentUser;
import static com.baatcheat.Homepage.currentUserId;

public class BuildProfile extends AppCompatActivity {

    public static Map<String, TableRow> cardNameToTableRow=new HashMap<>();//card Name to id of the TableRow containing it's card

    //only do it as a single map with cardname and card
    public static Map<String,Card>cardNameToCard=new HashMap<>();

    public static Map<String,InterestAdapter> adapMap=new HashMap<>();//cardName to interests associated

    public static Map<String, CheckBox> cardCheckBoxMap=new HashMap<>();//cardName to each checkbox inside the card
    public static Map<String, CardView> cardMap=new HashMap<>();//cardName to @id of each card

    public TableLayout whereToAddNewCards;

    private static final String TAG = "C: Build Profile";
    ImageView userProfilePic;
    ImagePicker imagePicker;



    @Override
    protected void onDestroy() {
        super.onDestroy();
        cardNameToCard.clear();
        adapMap.clear();
        cardNameToTableRow.clear();
        cardMap.clear();
        cardCheckBoxMap.clear();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_build_profile);

        //Initializing Views:
        userProfilePic=findViewById(R.id.profilePicture);
        final EditText nameEntry=findViewById(R.id.nameEntry);
        final EditText bioEntry=findViewById(R.id.bioEntry);
        final Button updateAll=findViewById(R.id.updateAll);
        whereToAddNewCards=findViewById(R.id.table);
        final FloatingActionButton cardAdder= findViewById(R.id.add_card_button);
        final Button cardDeleter=findViewById(R.id.cardDeleter);
        final Button storeButton=findViewById(R.id.testButton);

        if(nameEntry.getText().toString().isEmpty()){
            Log.d(TAG,"WE ARE ADDING THE TEXT HERE as "+currentUser.getUserName());
            Log.d(TAG,"STATUS OF USER: "+currentUser.toString());
            nameEntry.setText(currentUser.getUserName());
        }
        if(bioEntry.getText().toString().isEmpty()){
            bioEntry.setText(currentUser.getBio());
        }

        if(currentUser.getUserImageURI()!=null){
            Log.i(TAG,"URI: "+currentUser.getUserImageURI().toString());
            FirebaseStorageInterface storageInterface=new FirebaseStorageInterface();
            storageInterface.displayImage(currentUser.getUserImageURI(), userProfilePic);
        }

        imagePicker = new ImagePicker(this, /* activity non null*/
                null, /* fragment nullable*/
                new OnImagePickedListener() {
                    @Override
                    public void onImagePicked(Uri imageUri) {/*on image picked */
                        //we got an imageURI
                        //now we need to upload it to storage and get it from there.
                        userProfilePic.setImageURI(imageUri);
                        //currentUser.setUserImage(imageUri);
                        FirebaseStorageInterface st=new FirebaseStorageInterface();
                        st.addImage(imageUri, currentUserId,new FirestoreDataInterface() {
                            @Override
                            public void onFirestoreCallBack(Set<User> usersAlreadyFound) {
                            }

                            @Override
                            public void onUriObtained(Uri obtainedUri) {
                                currentUser.setUserImageURI(obtainedUri);
                            }

                            @Override
                            public void onUserCallback(User retrievedUser) {

                            }
                        });
                    }
                })
                .setWithImageCrop(
                        1 ,/*aspect ratio x*/
                        1 /*aspect ratio y*/);

        updateAll.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {

                //This just updates the Username and bio, now.
                //update Entered Fields
                final String name= nameEntry.getText().toString();
                final String bio= bioEntry.getText().toString();

                currentUser.setUserName(name);
                currentUser.setBio(bio);

                FirestoreInterface fs=new FirestoreInterface();
                fs.updateUser(currentUser);

                final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                        .setDisplayName(name).build();
                Objects.requireNonNull(user).updateProfile(profileUpdate)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    Log.i(TAG, "signupUser -> onComplete: "+"Name set match");
                                }
                                //ToDo: Cool random name if name is empty.
                            }
                        });

                //Realtime Database:
                final DatabaseReference realtimeUserDatabase = FirebaseDatabase.getInstance().getReference().child("users");
                realtimeUserDatabase.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        realtimeUserDatabase.child(user.getUid()).child("name").setValue(name);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.d(TAG, "addUserToDatabase => onCancelled: "+"Realtime database");
                    }
                });

            }
        });

        cardAdder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newCardPrompt();
            }
        });

        cardDeleter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteCardPrompt();
            }
        });

        storeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Set<String> currentVisibleCardsKeyset= cardNameToCard.keySet();

                DatamuseQuery q=new DatamuseQuery();
                //every single available card
                Set<String> cardNameSet=new HashSet<>(cardNameToCard.keySet());
                //debug test
                StringBuilder z= new StringBuilder();
                List<String> currentUserInterests=new ArrayList<String>();
                for(String x:cardNameToCard.keySet()){
                    z.append(x).append(":");
                    Card currentCard=cardNameToCard.get(x);
                    assert currentCard != null;
                    for(Interest y:currentCard.getStoredInterests()){
                        z.append(y.getName()).append(",");
                        currentUserInterests.add(y.getName());

                    }
                    z.append("\n");
                }
                currentUser.setInterests(currentUserInterests);
                Log.d("MAIN3","All available cards are "+z.toString());

                //
                Map<String,Card> visibleCards=new HashMap<>();
                //listMap.putAll(listMapToSend);

                for(String i:cardNameSet){
                    Log.d("MAIN3","CURRENT STATUS OF CARDS IS: "+i+":"+ Objects.requireNonNull(cardCheckBoxMap.get(i)).isChecked());

                    if(!Objects.requireNonNull(cardCheckBoxMap.get(i)).isChecked()){
                        //replace this card in the map with something which has changed its visibility
                        Card cardToReplaceStorage=cardNameToCard.get(i);
                        assert cardToReplaceStorage != null;
                        cardToReplaceStorage.setVisible(true);
                        cardNameToCard.remove(i);
                        cardNameToCard.put(i,cardToReplaceStorage);

                        //Both themes are exactly same, so warning doesn't matter.
                        //this card is visible!
                        Objects.requireNonNull(cardMap.get(i)).setCardBackgroundColor(Color.rgb(3,218,198));

                        visibleCards.put(i,cardNameToCard.get(i));
                        Log.d("MAIN3",i+"is visible");
                    }
                    else{
                        //it is invisible, remove anything associated with this i
                        //replace this card in the map with something which has changed its visibility
                        Card cardToReplaceStorage=cardNameToCard.get(i);
                        assert cardToReplaceStorage != null;
                        cardToReplaceStorage.setVisible(false);
                        cardNameToCard.remove(i);
                        cardNameToCard.put(i,cardToReplaceStorage);

                        //making grey in color
                        Objects.requireNonNull(cardMap.get(i)).setCardBackgroundColor(Color.LTGRAY);
                        Log.d("MAIN3",i+"is invisible");
                    }
                }
                //cleaning all the user's interests because now we will replce with only the visible ones
                FirestoreInterface fs2=new FirestoreInterface();
                currentUser.cleanUpInterests();
                fs2.clearUserInterests(currentUserId,currentUser);

                //debug
                StringBuilder display= new StringBuilder();
                for(String x: currentUser.interests){
                    display.append(x).append(",");
                }
                Log.d("MAIN3","CURRENT INNTERESTS OF USRE"+display.toString());


                //for each visible card: update the list in user database and interest database and cardDatabase
                for(String x:visibleCards.keySet()){
                    Log.d("Main3","WE ARE UPDATING INTERESTS LIST FOR CARD NAME:"+x);
                    q.listUpdater(x, new ArrayList<>(Objects.requireNonNull(Objects.requireNonNull(visibleCards.get(x)).getStoredInterests())),getApplicationContext(),3);

                }

                for(String cardName:cardNameToCard.keySet()){
                    //for each card
                    Card c=cardNameToCard.get(cardName);
                    FirestoreInterface fs=new FirestoreInterface();
                    fs.storeOneCard(c);

                }


            }
        });
    }

    private void deleteCardPrompt() {
        //get info
        //https://mkyong.com/android/android-prompt-user-input-dialog-example/
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(BuildProfile.this);
        @SuppressLint("InflateParams") View promptsView = li.inflate(R.layout.prompts, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                new ContextThemeWrapper(BuildProfile.this, R.style.myDialog)
        );
        // set prompts.xml to alertDialog builder
        alertDialogBuilder.setView(promptsView);
        final EditText userInput = promptsView
                .findViewById(R.id.editTextDialogUserInput);
        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                // get user input and set it to result
                                // edit text
                                String cardName=userInput.getText().toString();
                                removeCard(cardName);
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();
    }

    public void removeCard(String cardName){
        //simply hiding a card here
        if(cardNameToTableRow.containsKey(cardName)){
            //remove from database as well
            FirestoreInterface fs=new FirestoreInterface();
            fs.deleteCard(Objects.requireNonNull(cardNameToCard.get(cardName)));

            //removing from current user's view
            TableRow tr=cardNameToTableRow.get(cardName);
            assert tr != null;
            tr.setVisibility(View.GONE);
            cardNameToCard.remove(cardName);
            cardMap.remove(cardName);
            cardCheckBoxMap.remove(cardName);
            cardNameToTableRow.remove(cardName);
        }
    }

    public void newCardPrompt(){
        //get info
        //https://mkyong.com/android/android-prompt-user-input-dialog-example/
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(BuildProfile.this);
        @SuppressLint("InflateParams") View promptsView = li.inflate(R.layout.prompts, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                new ContextThemeWrapper(BuildProfile.this, R.style.myDialog)
        );
        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput =  promptsView
                .findViewById(R.id.editTextDialogUserInput);
        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                // get user input and set it to result
                                // edit text contains the cardName
                                String cardNameInfo=userInput.getText().toString();
                                //addCard(cardNameInfo );
                                Card c=new Card(currentUser.getEmail(),cardNameInfo,new ArrayList<Interest>(),true);
                                c.addNewCard(whereToAddNewCards,getApplicationContext(),BuildProfile.this);

                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();
    }

    public void getUserImage(View view){
        imagePicker.choosePicture(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        imagePicker.handleActivityResult(resultCode,requestCode, data);
        File f = imagePicker.getImageFile();

        //Ignore the "Always false" warning:
        if(f==null)
            Log.d(TAG,"Image Not Selected");
        else
            Log.d(TAG,"Got the image file");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        imagePicker.handlePermission(requestCode, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EditText nameEntry=findViewById(R.id.nameEntry);
        EditText bioEntry=findViewById(R.id.bioEntry);


        FirestoreInterface fs=new FirestoreInterface();
        fs.retrieveCards(whereToAddNewCards,getApplicationContext(),BuildProfile.this);

        if(nameEntry.getText().toString().isEmpty()){
            Log.d(TAG,"WE ARE ADDING THE TEXT HERE as "+currentUser.getUserName());
            nameEntry.setText(currentUser.getUserName());
        }
        if(bioEntry.getText().toString().isEmpty()){
            bioEntry.setText(currentUser.getBio());
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        EditText nameEntry=findViewById(R.id.nameEntry);
        EditText bioEntry=findViewById(R.id.bioEntry);

        FirestoreInterface fs=new FirestoreInterface();
        fs.retrieveCards(whereToAddNewCards,getApplicationContext(),BuildProfile.this);

        if(nameEntry.getText().toString().isEmpty()){
            Log.d(TAG,"WE ARE ADDING THE TEXT HERE as "+currentUser.getUserName());
            nameEntry.setText(currentUser.getUserName());
        }
        if(bioEntry.getText().toString().isEmpty()){
            bioEntry.setText(currentUser.getBio());
        }
    }
}