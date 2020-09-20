package com.baatcheat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.baatcheat.model.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Homepage extends AppCompatActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "Custom: Homepage";

    private DrawerLayout homepageDrawerLayout;
    private BottomNavigationView homepageBottomNav;
    private MaterialToolbar homepageToolbar;
    private ImageButton userProfilePicture;

    public static Map<String,?> settingsValues;
    static SharedPreferences sf;
    public static User currentUser;
    public static String currentUserId;

    @Override
    protected void onResume() {
        super.onResume();
        settingsValues = (Map<String, ?>) sf.getAll();
        boolean visibility= sf.getBoolean("visibility", false);
        int rangeOfInterestSearch=(int)sf.getInt("interestRange", 10);
        int rangeOfRecommendationSearch= sf.getInt("recommendationsRange", 10);
        InterestSearchFragment.currentRangeOfQuery=rangeOfInterestSearch*1.0;
        RecommendationsFragment.currentRangeOfQuery=rangeOfRecommendationSearch*1.0;
/*
        if(currentUser==null||currentUserId==null) {
            currentUser = getThisUser();//a temporary set of values until the user's actual details are retrieved
            currentUserId = getThisUserId();

            //getting current user from Database
            FirestoreInterface fs=new FirestoreInterface();
            fs.getUser(currentUserId, new FirestoreDataInterface() {
                @Override
                public void onFirestoreCallBack(Set<User> usersAlreadyFound) {

                }

                @Override
                public void onUriObtained(Uri obtainedUri) {

                }

                @Override
                public void onUserCallback(User retrievedUser) throws IOException {
                    Log.d(TAG,"WE HAVE GOT OUR CURRENT USER's Updated values");
                    currentUser=retrievedUser;

                    //putting image back in place if it exists
                    if(currentUser.getUserImageURI()!=null){
                        Log.d(TAG,"We have got our user's URI");
                        FirebaseStorageInterface storageInterface=new FirebaseStorageInterface();
                        storageInterface.displayImage(currentUser.getUserImageURI(), userProfilePicture);
                    }

                }
            });


        }*/
        if(currentUser!=null) {
            currentUser.setSearchVisibility(visibility);
            if (currentUser.getUserImageURI() != null) {
                FirebaseStorageInterface storageInterface = new FirebaseStorageInterface();
                storageInterface.displayImage(currentUser.getUserImageURI(), userProfilePicture);
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        sf = PreferenceManager.getDefaultSharedPreferences(this);
        settingsValues = (Map<String, ?>) sf.getAll();
        boolean visibility= sf.getBoolean("visibility", false);
        int rangeOfInterestSearch=10;
        int rangeOfRecommendationSearch= sf.getInt("recommendationsRange", 10);
        if(sf!=null){
            rangeOfInterestSearch = (int)sf.getInt("interestRange", 10);

        }
        //Log.d("SETTINGS VALUE","Current value is "+visibility+","+rangeOfInterestSearch+","+rangeOfRecommendationSearch);
        InterestSearchFragment.currentRangeOfQuery=rangeOfInterestSearch*1.0;
        RecommendationsFragment.currentRangeOfQuery=rangeOfRecommendationSearch*1.0;

        //Initializing UI Elements:
        homepageDrawerLayout = findViewById(R.id.homepage_drawer_layout);
        homepageBottomNav = findViewById(R.id.homepage_bottom_nav);
        homepageBottomNav.setOnNavigationItemSelectedListener(this);
        homepageToolbar = findViewById(R.id.homepage_toolbar);
        userProfilePicture = findViewById(R.id.user_profile_picture);

        getSupportFragmentManager().beginTransaction().replace(R.id.homepage_fragment_container,
                new RecommendationsFragment()).commit();

        //AppBar:
        setSupportActionBar(homepageToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, homepageDrawerLayout,
                homepageToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        homepageDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView mainNavigationView = findViewById(R.id.navigation_view_homepage);
        mainNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.homepage_menu_item:
                        homepageDrawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case R.id.messaging_menu_item:
                        Intent toMessagingActivity = new Intent(Homepage.this, MessagingHomepage.class);
                        startActivity(toMessagingActivity);
                        return true;
                    case R.id.settings_menu_item:
                        Intent toSettings = new Intent(Homepage.this, SettingsActivity.class);
                        startActivity(toSettings);
                        return true;
                    default:
                        return false;
                }
            }
        });
        mainNavigationView.setCheckedItem(R.id.homepage_menu_item);

        if(currentUser==null||currentUserId==null) {
            currentUser = getThisUser();//a temporary set of values until the user's actual details are retrieved
            currentUserId = getThisUserId();

            //getting current user from Database
            FirestoreInterface fs=new FirestoreInterface();
            fs.getUser(currentUserId, new FirestoreDataInterface() {
                @Override
                public void onFirestoreCallBack(Set<User> usersAlreadyFound) {

                }

                @Override
                public void onUriObtained(Uri obtainedUri) {

                }

                @Override
                public void onUserCallback(User retrievedUser) throws IOException {
                    Log.d(TAG,"WE HAVE GOT OUR CURRENT USER's Updated values"+retrievedUser.toString());

                    currentUser=retrievedUser;
                    currentUser.setUserID(currentUserId);
                    //currentUser.setEmail();

                    //putting image back in place if it exists
                    if(currentUser.getUserImageURI()!=null){
                        Log.d(TAG,"We have got our user's URI");
                        FirebaseStorageInterface storageInterface=new FirebaseStorageInterface();
                        storageInterface.displayImage(currentUser.getUserImageURI(), userProfilePicture);
                    }

                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if(homepageDrawerLayout.isDrawerOpen(GravityCompat.START))
            homepageDrawerLayout.closeDrawer(GravityCompat.START);
        else {
            Intent exitApp = new Intent(Intent.ACTION_MAIN);
            exitApp.addCategory(Intent.CATEGORY_HOME);
            exitApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(exitApp);
        }
    }

    public void profileClicked(View view){
        Intent buildProfile = new Intent(this, BuildProfile.class);
        startActivity(buildProfile);
    }

    private void signOut(){
        FirebaseAuth authenticator = FirebaseAuth.getInstance();
        FirebaseUser currentUser = authenticator.getCurrentUser();
        if(currentUser==null){
            Log.d(TAG, "signOut: "+"How can this happen?");
        }
        else{
            authenticator.signOut();
            Intent toLoginActivity = new Intent(this, LoginActivity.class);
            startActivity(toLoginActivity);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;

        switch (item.getItemId()){
            case R.id.recommendation_menu:
                selectedFragment = new RecommendationsFragment();
                break;
            case R.id.interest_search_menu:
                selectedFragment = new InterestSearchFragment();
                break;
        }

        if(selectedFragment!=null)
            getSupportFragmentManager().beginTransaction().replace(R.id.homepage_fragment_container,
                    selectedFragment).commit();

        return true;
    }

    public User getThisUser(){
        boolean visibility = true;
        if(sf!=null)
            visibility= sf.getBoolean("visibility", false);
        Log.d("SETTINGS VALUE","Current value is "+visibility);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Log.d(TAG,"Our guy's email is:"+user.getEmail());
        assert user != null;
        return new User(user.getEmail(),user.getDisplayName(),LocationInterface.lastUpdatedLatitude,LocationInterface.lastUpdatedLongitude, new ArrayList<>(Collections.singletonList("")),visibility);
    }

    public String getThisUserId(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        return user.getUid();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}