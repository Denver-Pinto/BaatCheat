package com.baatcheat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.baatcheat.adapter.UserListAdapter;
import com.baatcheat.model.User;
import com.baatcheat.utility.RecyclerViewDecoration;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

import static com.baatcheat.Homepage.currentUser;

public class MessagingHomepage extends AppCompatActivity
    implements UserListAdapter.ItemClickListener {

    private static final String TAG = "Custom: Messaging";

    ArrayList<User> userData;
    UserListAdapter userListAdapter;

    //Views:
    RecyclerView userRecyclerView;
    private DrawerLayout messagingDrawerLayout;
    private MaterialToolbar messagingToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging_homepage);

        userData = new ArrayList<>();
        initializeRecyclerView();
        fetchUserData();

        messagingDrawerLayout = findViewById(R.id.messaging_drawer_layout);
        messagingToolbar = findViewById(R.id.messaging_toolbar);
        ImageButton userProfilePicture = findViewById(R.id.user_profile_picture);

        //AppBar:
        setSupportActionBar(messagingToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, messagingDrawerLayout,
                messagingToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        messagingDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView mainNavigationView = findViewById(R.id.navigation_view_messaging);
        mainNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.messaging_menu_item:
                        messagingDrawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.homepage_menu_item:
                        Intent toMessagingActivity = new Intent(MessagingHomepage.this, Homepage.class);
                        startActivity(toMessagingActivity);
                        return true;
                    case R.id.settings_menu_item:
                        Intent toSettings = new Intent(MessagingHomepage.this, SettingsActivity.class);
                        startActivity(toSettings);
                        return true;
                    default:
                        return false;
                }
            }
        });
        mainNavigationView.setCheckedItem(R.id.messaging_menu_item);

        if (currentUser.getUserImageURI() != null) {
            FirebaseStorageInterface storageInterface = new FirebaseStorageInterface();
            storageInterface.displayImage(currentUser.getUserImageURI(), userProfilePicture);
        }
    }

    @Override
    public void onBackPressed() {
        if(messagingDrawerLayout.isDrawerOpen(GravityCompat.START))
            messagingDrawerLayout.closeDrawer(GravityCompat.START);
        else {
            super.onBackPressed();
        }
    }

    private void fetchUserData(){

        final DatabaseReference userListDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        userListDatabase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                String name ="";
                for(DataSnapshot childSnapshot: dataSnapshot.getChildren()){
                    if("name".equals(childSnapshot.getKey()))
                        name = childSnapshot.getValue(String.class);
                }
                final User user = new User(name);
                user.setUserID(dataSnapshot.getKey());
                Log.i(TAG, "onChildAdded: "+user);

                if(!user.getUserID().equals(FirebaseAuth.getInstance().getUid())){
                    FirestoreInterface userDatabase = new FirestoreInterface();
                    userDatabase.getUser(user.getUserID(), new FirestoreDataInterface() {
                        @Override
                        public void onFirestoreCallBack(Set<User> usersAlreadyFound) {
                        }

                        @Override
                        public void onUriObtained(Uri obtainedUri) {
                        }

                        @Override
                        public void onUserCallback(User retrievedUser) throws IOException {
                            if(retrievedUser.getUserImageURI()!=null) {
                                user.setUserImageURI(retrievedUser.getUserImageURI());
                            }
                            userData.add(user);
                            userListAdapter.notifyDataSetChanged();
                        }
                    });
                }
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
        userRecyclerView = findViewById(R.id.user_recycler_view);
        userRecyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        userRecyclerView.setLayoutManager(layoutManager);

        userListAdapter = new UserListAdapter(userData, this);
        userRecyclerView.setAdapter(userListAdapter);

        userRecyclerView.addItemDecoration(new RecyclerViewDecoration());
    }

    @Override
    public void onItemClicked(int itemPosition) {
        Intent toMessagingActivity = new Intent(this, MessagingActivity.class);
        toMessagingActivity.putExtra("UserID", userData.get(itemPosition).getUserID());
        toMessagingActivity.putExtra("userName", userData.get(itemPosition).getUserName());
        if(userData.get(itemPosition).getUserImageURI()!=null)
            toMessagingActivity.putExtra("userImageURI", userData.get(itemPosition).getUserImageURI().toString());
        startActivity(toMessagingActivity);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_button:
                signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void signOut(){
        FirebaseAuth authenticator = FirebaseAuth.getInstance();
        FirebaseUser currentUser = authenticator.getCurrentUser();
        if(currentUser==null){
            Log.d(TAG, "signOut: "+"How can this happen?");
        }
        else{
            Homepage.currentUser = null;
            authenticator.signOut();
            Intent toLoginActivity = new Intent(this, LoginActivity.class);
            startActivity(toLoginActivity);
        }
    }

    public void profileClicked(View view){
        Intent buildProfile = new Intent(this, BuildProfile.class);
        startActivity(buildProfile);
    }
}
