package com.baatcheat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baatcheat.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

//Todo: Test and fix bugs - Spinner, verification
//Todo: Forgot password

public class LoginActivity extends AppCompatActivity
    implements TabLayout.OnTabSelectedListener {

    private static final String TAG = "C: LoginActivity";

    private static final int LOGIN_UI = 0;
    private static  final int SIGNUP_UI = 1;
    private static final int VERIFY_UI = 2;
    private static int currentUI;

    //Error codes
    private static final int NO_ERRORS = 0;
    private static final int EMPTY_EMAIL_ERROR = 1;
    private static final int EMAIL_FORMAT_ERROR = 2;
    private static final int EMPTY_PASSWORD_ERROR = 4;
    private static final int PASSWORD_FORMAT_ERROR = 8;
    private static final int EMPTY_NAME_ERROR = 16;
    private static final int OTHER_ERROR = 32;

    //Firebase Authentication instance:
    private FirebaseAuth authenticator;

    //UI Stuff
    TabLayout loginTabLayout;
    TabItem loginTab, signupTab;
    ImageView logoImageView;
    TextInputLayout nameTextInput, emailTextInput, passwordTextInput;
    TextInputEditText nameEditText, emailEditText, passwordEditText;
    TextView loginErrorText, emailVerificationText, verificationErrorText;
    RelativeLayout loginButtonLayout, signupButtonLayout;
    LinearLayout loginLayout, verificationLayout;
    Button signupButton, loginButton;
    ProgressBar loginProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.i(TAG, "onCreate: "+"Logs are working");

        authenticator = FirebaseAuth.getInstance();

        //Initializing UI Stuff
        loginTabLayout = findViewById(R.id.login_tab_layout);
        loginTabLayout.addOnTabSelectedListener(this);
        loginTab = findViewById(R.id.login_tab);
        signupTab = findViewById(R.id.sign_up_tab);
        logoImageView = findViewById(R.id.logo_image_view);
        nameTextInput = findViewById(R.id.name_text_input);
        nameEditText = findViewById(R.id.name_edit_text);
        emailTextInput = findViewById(R.id.email_text_input);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordTextInput = findViewById(R.id.password_text_input);
        passwordEditText  = findViewById(R.id.password_edit_text);
        loginErrorText = findViewById(R.id.login_error_text);
        loginButton = findViewById(R.id.login_button);
        signupButton = findViewById(R.id.sign_up_button);
        loginButtonLayout = findViewById(R.id.login_button_layout);
        signupButtonLayout = findViewById(R.id.signup_button_layout);
        loginLayout = findViewById(R.id.login_layout);
        verificationLayout = findViewById(R.id.verification_layout);
        emailVerificationText = findViewById(R.id.email_verification_text);
        verificationErrorText = findViewById(R.id.verification_error_text);
        loginProgressBar = findViewById(R.id.login_progress_bar);
        loginProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = authenticator.getCurrentUser();
        updateUI(currentUser);
    }

    public void signupUser(View view){
        if(verifyForm(SIGNUP_UI)== NO_ERRORS){
            showProgressBar();
            final String email = Objects.requireNonNull(emailEditText.getText()).toString();
            Log.i(TAG, "signupUser: "+ email);
            final String password = Objects.requireNonNull(passwordEditText.getText()).toString();
            final String name = Objects.requireNonNull(nameEditText.getText()).toString();

            authenticator.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                                Log.i(TAG, "signupUser -> onComplete: "+email);
                                FirebaseUser currentUser = authenticator.getCurrentUser();

                                UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name).build();

                                Objects.requireNonNull(currentUser).updateProfile(profileUpdate)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if(task.isSuccessful()){
                                                    Log.i(TAG, "signupUser -> onComplete: "+"Name set match");
                                                }
                                                //ToDo: Cool random name if name is empty.
                                            }
                                        });

                                addUserToDatabase(currentUser.getUid(), nameEditText.getText().toString());

                                updateUI(currentUser);
                            }
                            else{ //Failed
                                Log.w(TAG, "signupUser -> onComplete: "+"Failed: ", task.getException());
                                setErrors(NO_ERRORS); // Clear existing errors
                                try{
                                    throw Objects.requireNonNull(task.getException());
                                }
                                catch (FirebaseAuthWeakPasswordException exception){
                                    passwordTextInput.setError(getString(R.string.weak_password_error));
                                    passwordTextInput.requestFocus();
                                }
                                catch (FirebaseAuthInvalidCredentialsException exception){
                                    emailTextInput.setError(getString(R.string.email_error));
                                    emailTextInput.requestFocus();
                                }
                                catch (FirebaseAuthUserCollisionException exception){
                                    emailTextInput.setError(getString(R.string.email_exists_error));
                                    emailTextInput.requestFocus();
                                }
                                catch(Exception exception){
                                    loginErrorText.setVisibility(View.VISIBLE);
                                    loginErrorText.setText(getString(R.string.unknown_error));
                                }
                            }
                        }
                    });
            hideProgressBar();
        }
    }

    public void loginClicked(View view){
        loginUser();
    }

    public void loginUser(){
        if(verifyForm(LOGIN_UI)==NO_ERRORS) {
            showProgressBar();

            final String email = Objects.requireNonNull(emailEditText.getText()).toString();
            Log.i(TAG, "loginUser: " + email);
            final String password = Objects.requireNonNull(passwordEditText.getText()).toString();

            authenticator.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                                Log.i(TAG, "loginUser -> onComplete: "+email);
                                FirebaseUser currentUser = authenticator.getCurrentUser();
                                addUserToDatabase(Objects.requireNonNull(currentUser).getUid(), currentUser.getDisplayName());
                                updateUI(currentUser);
                            }
                            else{
                                Log.w(TAG, "loginUser -> onComplete: "+"Failed", task.getException());
                                setErrors(NO_ERRORS);

                                final String errorCode = ((FirebaseAuthException) Objects.requireNonNull(task.getException())).getErrorCode();

                                switch(errorCode){
                                    case "ERROR_USER_NOT_FOUND":
                                        emailTextInput.setError(getString(R.string.email_not_found_error));
                                        emailTextInput.requestFocus();
                                        break;

                                    case "ERROR_WRONG_PASSWORD":
                                        passwordTextInput.setError(getString(R.string.password_error));
                                        passwordTextInput.requestFocus();
                                        break;

                                    case "ERROR_INVALID_CREDENTIAL":
                                        loginErrorText.setVisibility(View.VISIBLE);
                                        loginErrorText.setText(getString(R.string.invalid_credentials_error));
                                        break;

                                    default:
                                        loginErrorText.setVisibility(View.VISIBLE);
                                        loginErrorText.setText(getString(R.string.unknown_error));
                                }
                            }
                        }
                    });

            hideProgressBar();
        }
    }

    public void googleSignIn(View view){
        Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
    }

    private void addUserToDatabase(final String userID, final String name){


        //FireStore:
        //Check if it already exists:
        final CollectionReference firestoreUserDatabase = FirebaseFirestore.getInstance().collection("users");
        firestoreUserDatabase.document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot userDocument = task.getResult();
                    if(userDocument==null || !userDocument.exists()){
                        // Doesn't exist, so adding
                        Log.i(TAG, "addUserToDatabase => onComplete: "+"Adding user to FireStore...");
                         new HashMap<>();/*
                        currentUser.put("name", name);*/
                        User u=new User(name);
                        final Map<String, Object> currentUser =u.convertToMap();
                        firestoreUserDatabase.document(userID).set(currentUser)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.i(TAG, "addUserToDatabase => onSuccess: "+userID+":"+currentUser.toString());
                                    }
                                });
                    }
                }
            } //Kaafi brackets, much wow
        });

        //Realtime Database:
        final DatabaseReference realtimeUserDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        realtimeUserDatabase.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    realtimeUserDatabase.child(userID).child("name").setValue(name);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "addUserToDatabase => onCancelled: "+"Realtime database");
            }
        });
    }

    private void sendVerificationMail(){
        verificationErrorText.setText("");
        final FirebaseUser currentUser = authenticator.getCurrentUser();
        if(currentUser==null)
            return;
        currentUser.sendEmailVerification()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Log.i(TAG, "sendVerificationMail -> onComplete: "+
                                    "Successfully sent to "+currentUser.getEmail());
                            final String message = getString(R.string.verification_mail_sent)+" to "+currentUser.getEmail()
                                    +"\n Verify and Proceed";
                            emailVerificationText.setText(message);
                            verificationErrorText.setText("");
                        }
                        else{
                            Log.e(TAG, "sendVerificationMail -> onComplete: ", task.getException());
                            verificationErrorText.setText(getString(R.string.verification_sending_error));
                        }
                    }
                });
    }

    public void checkVerification(View view){
        authenticator.signOut();

        loginUser();

        authenticator = FirebaseAuth.getInstance();
        final FirebaseUser currentUser = authenticator.getCurrentUser();

        if(currentUser==null)
            return;

        Log.i(TAG, "checkVerification: "+"Verified = "+currentUser.isEmailVerified());
        if(currentUser.isEmailVerified()) {
            updateUI(currentUser);
        }
        else{
            verificationErrorText.setText(getString(R.string.email_verification_error));
        }
    }

    public void resendVerificationMail(View view){
        sendVerificationMail();
    }

    public void changeEmail(View view){
        setupUI(SIGNUP_UI);
    }

    private int verifyForm(final int formType){
        //This only checks the form locally

        emailTextInput.setError(null);
        passwordTextInput.setError(null);
        nameTextInput.setError(null);
        loginErrorText.setText("");
        loginErrorText.setVisibility(View.GONE);

        int verificationCode = 0;

        //noinspection ConstantConditions
        if(emailEditText.getText().length() == 0)
            verificationCode |= EMPTY_EMAIL_ERROR;
        String email = emailEditText.getText().toString();
        if(!email.matches(getString(R.string.email_regex)))
            verificationCode |= EMAIL_FORMAT_ERROR;

        //noinspection ConstantConditions
        if(passwordEditText.getText().length() == 0)
            verificationCode |= EMPTY_PASSWORD_ERROR;
        String password = passwordEditText.getText().toString();
        if(!password.matches(getString(R.string.password_regex)))
            verificationCode |= PASSWORD_FORMAT_ERROR;

        if(formType == SIGNUP_UI){
            //noinspection ConstantConditions
            if(nameEditText.getText().length() == 0)
                verificationCode |= EMPTY_NAME_ERROR;
        }

        setErrors(verificationCode);

        return verificationCode;
    }

    public void resetUserPassword(View view){
        final String email = Objects.requireNonNull(emailEditText.getText()).toString();
        if(email.length()==0){
            setErrors(EMPTY_EMAIL_ERROR);
        }
        else{
            authenticator.sendPasswordResetEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()) {
                                Log.i(TAG, "resetUserPassword -> onComplete: " + email);
                                Toast.makeText(LoginActivity.this, getString(R.string.check_email) ,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void updateUI(FirebaseUser currentUser){
        hideProgressBar();
        if(currentUser != null && currentUser.isEmailVerified()){



            Intent toHomepage = new Intent(this, Homepage.class);
            startActivity(toHomepage);
        }
        else if(currentUser!=null && !currentUser.isEmailVerified()){
            sendVerificationMail();
            setupUI(VERIFY_UI);
        }
        else{
            setupUI(currentUI);
        }
    }

    private  void setupUI(int uiType){
        setErrors(NO_ERRORS);
        if(uiType == LOGIN_UI) {
            verificationLayout.setVisibility(View.GONE);
            loginLayout.setVisibility(View.VISIBLE);
            nameTextInput.setVisibility(View.GONE);
            logoImageView.setVisibility(View.VISIBLE);
            signupButtonLayout.setVisibility(View.GONE);
            loginButtonLayout.setVisibility(View.VISIBLE);
            currentUI = LOGIN_UI;
        }
        else if(uiType == SIGNUP_UI){
            //logoImageView.setVisibility(View.GONE);
            verificationLayout.setVisibility(View.GONE);
            loginLayout.setVisibility(View.VISIBLE);
            nameTextInput.setVisibility(View.VISIBLE);
            loginButtonLayout.setVisibility(View.GONE);
            signupButtonLayout.setVisibility(View.VISIBLE);
            currentUI = SIGNUP_UI;
        }
        else if(uiType == VERIFY_UI){
            verificationErrorText.setText("");
            emailVerificationText.setText("");
            loginLayout.setVisibility(View.GONE);
            verificationLayout.setVisibility(View.VISIBLE);
            verificationErrorText.setText(getString(R.string.email_verification_error));
        }
    }

    private  void setErrors(int errorCode){
        //Clear all existing errors first

        emailTextInput.setError("");
        passwordTextInput.setError("");
        nameTextInput.setError("");
        loginErrorText.setText("");
        loginErrorText.setVisibility(View.GONE);

        if((errorCode&EMAIL_FORMAT_ERROR) != 0)
            emailTextInput.setError(getString(R.string.email_error));
        if((errorCode&EMPTY_EMAIL_ERROR) != 0)
            emailTextInput.setError(getString(R.string.empty_error));
        if((errorCode&PASSWORD_FORMAT_ERROR) != 0)
            passwordTextInput.setError(getString(R.string.password_error));
        if((errorCode&EMPTY_PASSWORD_ERROR) != 0)
            passwordTextInput.setError(getString(R.string.empty_error));
        if((errorCode&EMPTY_NAME_ERROR)!=0)
            nameTextInput.setError(getString(R.string.empty_error));
        if((errorCode&OTHER_ERROR)!=0) {
            loginErrorText.setVisibility(View.VISIBLE);
            loginErrorText.setText(getString(R.string.unknown_error));
        }
    }

    private void showProgressBar(){
        loginProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar(){
        loginProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        final int LOGIN  = 0;
        final int SIGNUP = 1;
        Log.d(TAG, "onTabSelected: "+tab.getPosition());
        switch (tab.getPosition()){
            case LOGIN:
                setupUI(LOGIN_UI);
                break;
            case SIGNUP:
                setupUI(SIGNUP_UI);
                break;
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        //NotNeeded
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        //NotNeeded
    }
}