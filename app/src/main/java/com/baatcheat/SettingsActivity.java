package com.baatcheat;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceFragmentCompat;

import com.baatcheat.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.baatcheat.Homepage.currentUser;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "C: SettingsActivity";

    DrawerLayout settingsDrawerLayout;
    ImageButton userProfilePicture;
    MaterialToolbar settingsToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        settingsDrawerLayout = findViewById(R.id.settings_drawer_layout);
        settingsToolbar = findViewById(R.id.settings_toolbar);
        userProfilePicture = findViewById(R.id.user_profile_picture);

        //AppBar:
        setSupportActionBar(settingsToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, settingsDrawerLayout,
                settingsToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        settingsDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView mainNavigationView = findViewById(R.id.navigation_view_settings);
        mainNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.settings_menu_item:
                        settingsDrawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.messaging_menu_item:
                        Intent toMessagingActivity = new Intent(SettingsActivity.this, MessagingHomepage.class);
                        startActivity(toMessagingActivity);
                        return true;
                    case R.id.homepage_menu_item:
                        Intent toHomepage = new Intent(SettingsActivity.this, Homepage.class);
                        startActivity(toHomepage);
                        return true;
                    default:
                        return false;
                }
            }
        });
        mainNavigationView.setCheckedItem(R.id.settings_menu_item);

        if(currentUser.getUserImageURI()!=null){
            FirebaseStorageInterface storageInterface=new FirebaseStorageInterface();
            storageInterface.displayImage(currentUser.getUserImageURI(), userProfilePicture);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if(currentUser.getUserImageURI()!=null){
            FirebaseStorageInterface storageInterface=new FirebaseStorageInterface();
            storageInterface.displayImage(currentUser.getUserImageURI(), userProfilePicture);
        }
    }

    @Override
    public void onBackPressed() {
        if(settingsDrawerLayout.isDrawerOpen(GravityCompat.START))
            settingsDrawerLayout.closeDrawer(GravityCompat.START);
        else {
            super.onBackPressed();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }

    public void profileClicked(View view){
        Intent buildProfile = new Intent(this, BuildProfile.class);
        startActivity(buildProfile);
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

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    deleteUser();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        }
    };

    private void deleteUser(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;

        Toast.makeText(this, "Hello World", Toast.LENGTH_SHORT).show();

        String userID = user.getUid();

        //FireStore:
        final CollectionReference firestoreUserDatabase = FirebaseFirestore.getInstance().collection("users");
        firestoreUserDatabase.document(userID).delete();

        //RealtimeDatabase:
        final DatabaseReference realtimeUserDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        realtimeUserDatabase.child(userID).removeValue();

        //Auth:
        user.delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User account deleted.");

                            //Delete Cache:
                            try {
                                File dir = SettingsActivity.this.getCacheDir();
                                if(!deleteDir(dir))
                                    Log.e(TAG, "deleteUser: "+"Couldn't delete cache");;
                            } catch (Exception e) { e.printStackTrace();}

                            //Goto LoginActivity:
                            startActivity(new Intent(SettingsActivity.this, LoginActivity.class));

                        }
                        else {
                            Toast.makeText(SettingsActivity.this, "Error. Please try again later", Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            assert children != null;
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    public void deleteUserAccount(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }
}