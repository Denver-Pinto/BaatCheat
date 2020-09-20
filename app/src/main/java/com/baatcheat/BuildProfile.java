package com.baatcheat;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.baatcheat.model.Card;
import com.baatcheat.model.Interest;
import com.baatcheat.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.myhexaville.smartimagepicker.ImagePicker;
import com.myhexaville.smartimagepicker.OnImagePickedListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
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

    //Face Recognition Stuff:
    static private final int REQUEST_IMAGE_CAPTURE = 1;
    private String currentPhotoPath;
    private Bitmap sourceBitmap, croppedBitmap;
    private SparseArray<Face> faces;
    private String fname;
    private final static String BASE_URL = "http://40.117.95.58:8501/v1/models/estimator_model:predict"; // This is the API base URL
    private Double[] faceID = new Double[128];
    private Map<String, List<Double>> result;
    private RequestQueue requestQueue;  // This is our requests queue to process our HTTP requests.

    //Permissions
    private static final int REQUEST_PERMISSIONS = 1;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

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

        if (requestCode==REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            File imgFile = new  File(currentPhotoPath);
            if(imgFile.exists())            {
                sourceBitmap=rotateBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath()));
                /*comment the next section out if you want to show pictures from camera*/
                /*BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable=true;
                sourceBitmap = BitmapFactory.decodeResource(
                        getApplicationContext().getResources(),
                        R.drawable.abc,
                        options);
                /**/
                faceDetection();
            }
        }
        else{
            //TODO: Figure out the request code for this part.
            imagePicker.handleActivityResult(resultCode,requestCode, data);
            File f = imagePicker.getImageFile();
        }
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

    //FaceRecognitionStuff:
    public void addFaceSample(View view){
        // This setups up a new request queue which we will need to make HTTP requests.
        requestQueue = Volley.newRequestQueue(this);

        // Check if we have permissions
        int storagePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int cameraPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        if (storagePermission != PackageManager.PERMISSION_GRANTED
                || cameraPermission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS,
                    REQUEST_PERMISSIONS
            );

            //Ignore the warning:
            //Check again:
            if (storagePermission != PackageManager.PERMISSION_GRANTED
                    || cameraPermission != PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Face Recognition wouldn't work", Toast.LENGTH_SHORT).show();
            else
                takePicture();
        }
        else
            takePicture();
        requestQueue = Volley.newRequestQueue(this);  // This setups up a new request queue which we will need to make HTTP requests.
    }

    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(this.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile;
            try {
                photoFile = createImageFile();
                Uri photoURI = FileProvider.getUriForFile(this,
                        BuildConfig.APPLICATION_ID + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(this,
                        "Photo file can't be created, please try again",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpeg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private Bitmap rotateBitmap(Bitmap original) {
        Matrix matrix = new Matrix();
        matrix.preRotate((float) 270);
        Bitmap rotatedBitmap = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
        original.recycle();
        return rotatedBitmap;
    }

    private void faceDetection() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable=true;
        Paint myRectPaint = new Paint();
        myRectPaint.setStrokeWidth(5);
        myRectPaint.setColor(Color.RED);
        myRectPaint.setStyle(Paint.Style.STROKE);
        Bitmap drawnBitmap = Bitmap.createBitmap(sourceBitmap.getWidth(), sourceBitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas tempCanvas = new Canvas(drawnBitmap);

        tempCanvas.drawBitmap(sourceBitmap, 0, 0, null);

        FaceDetector faceDetector = new
                FaceDetector.Builder(this).setTrackingEnabled(false)
                .build();
        if(!faceDetector.isOperational()){
            // new AlertDialog.Builder(v.this).setMessage("Could not set up the face detector!").show();
            return;
        }
        Frame frame = new Frame.Builder().setBitmap(sourceBitmap).build();
        faces = faceDetector.detect(frame);

        int maxFaces = faces.size();
        int faceid = maxFaces > 0 ? 0 : -1;

        for(int i=0; i<faces.size(); i++) {
            Face thisFace = faces.valueAt(i);
            float x1 = thisFace.getPosition().x;

            float y1 = thisFace.getPosition().y;
            float x2 = x1 + thisFace.getWidth();
            float y2 = y1 + thisFace.getHeight();
            Log.d("value of x1 is ", String.valueOf((int)x1));
            Log.d("value of y1 is ", String.valueOf((int)y1));
            Log.d("value of x2 is ", String.valueOf((int)x2));
            Log.d("value of y2 is ", String.valueOf((int)y2));
            Log.d("bitmap height", String.valueOf(sourceBitmap.getHeight()));
            Log.d("bitmap width", String.valueOf(sourceBitmap.getWidth()));
            Log.d("value of face height is", String.valueOf(thisFace.getHeight()));
            Log.d("value of face width is ", String.valueOf(thisFace.getWidth()));

            int pad =30;
            tempCanvas.drawRect(Math.max((int)x1-pad,0), Math.max((int)y1-pad,0),Math.min((int)x2+pad,sourceBitmap.getWidth()), Math.min((int)y2+pad,sourceBitmap.getHeight()), myRectPaint);

            croppedBitmap = Bitmap.createBitmap(sourceBitmap, Math.max((int)x1-pad,0), Math.max((int)y1-pad,0),(int)Math.min(thisFace.getWidth()+2*pad,sourceBitmap.getWidth()-(int)x1+pad), (int)Math.min(thisFace.getHeight()+2*pad,sourceBitmap.getHeight()-(int)y1+pad));
            //convertedBitmap = Bitmap.createBitmap(croppedBitmap.getWidth(), croppedBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            String s  = convertBitmapToString(croppedBitmap);
            bitmap2file(croppedBitmap);
            //textView.setText(s);
            string2file(s);
            Log.d("value of CBx is ", String.valueOf(croppedBitmap.getWidth()));
            Log.d("value of CBy is ", String.valueOf(croppedBitmap.getHeight()));

            // tempCanvas.drawOval(new RectF(x1-10, y1-10, x2+10, y2+10), myRectPaint);
/*
            //this next code is for cropping faces
            Bitmap output = Bitmap.createBitmap(sourceBitmap.getWidth(),
                    sourceBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(0XFF000000);
            //destination
            canvas.drawRect(x1-10, y1-10, x2+10, y2+10, paint);

            // Keeps the source pixels that cover the destination pixels,
            // discards the remaining source and destination pixels.
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            //source
            canvas.drawBitmap(sourceBitmap, 0, 0, paint);
            Log.d("value of Hheight is ", String.valueOf(output.getHeight()));
            Log.d("value of Wwidth is ", String.valueOf(output.getWidth()));

            //ends here


            imageView.setImageDrawable(new BitmapDrawable(getResources(),output));

            imageView.setImageBitmap(output);
            */


            // croppedface.setImageBitmap(croppedBitmap);

        }

        if(faceid !=-1)
        {
            faceProcessing();
        }
        else
            Toast.makeText(this, "No Faces found", Toast.LENGTH_SHORT).show();
        faceDetector.release();
    }

    private void string2file(String sBody) {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "Notes");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, fname+".txt");
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(sBody);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String convertBitmapToString(Bitmap bitmap) {
        String encodedImage = "";

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);

        encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);

        return encodedImage;
    }

    private void bitmap2file(Bitmap bitmap) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/req_images");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        fname = "Image-" + n;
        String filename = fname + ".jpg";
        File file = new File(myDir, filename);

        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void faceProcessing() {
        //setPredictionText("Recognizing faces...");
        Toast.makeText(this, "Creating Face ID", Toast.LENGTH_SHORT).show();
        Face thisFace = faces.valueAt(0);
        float x1 = thisFace.getPosition().x;
        float y1 = thisFace.getPosition().y;

        int pad =30;
        croppedBitmap = Bitmap.createBitmap(sourceBitmap, Math.max((int)x1-pad,0), Math.max((int)y1-pad,0),(int)Math.min(thisFace.getWidth()+2*pad,sourceBitmap.getWidth()-(int)x1+pad), (int)Math.min(thisFace.getHeight()+2*pad,sourceBitmap.getHeight()-(int)y1+pad));
        String s  = convertBitmapToString(croppedBitmap);
        JSONObject instance = new JSONObject();
        try {
            JSONObject b64 = new JSONObject();

            b64.put("b64",s);
            JSONObject bytes = new JSONObject();
            bytes.put("bytes", b64);
            JSONArray list = new JSONArray();
            list.put(bytes);

            instance.put("instances", list);
            Log.d("Instance",instance.toString());
        }catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest arrReq = new JsonObjectRequest
                (Request.Method.POST, BASE_URL, instance, new Response.Listener<JSONObject>()  {
                    @Override
                    public void onResponse(JSONObject response) {
                        setPopulateTextResponse(response);
                    }
                },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // If there a HTTP error then add a note to our repo list.
                                //setPredictionText("Error while calling REST API");
                                Toast.makeText(BuildProfile.this, "Error while calling REST API", Toast.LENGTH_SHORT).show();
                                Log.e("Volley", error.toString());
                            }
                        }
                );
        requestQueue.add(arrReq);// Add the request we just defined to our request queue.
    }

    private void setPopulateTextResponse(JSONObject response) {
        JSONArray prob,prob1;
        try {
            prob = response.getJSONArray("predictions");
            prob1 = prob.getJSONArray(0);

            Log.d("prob1 type",prob1.toString());
            //getTopKProbability(prob1);
            getFaceID(prob1);

        } catch (JSONException e) {
            Log.e("Volley", "Error processing JSON-setPopulateTextResponse."); // needs to be a toast
            Toast.makeText(this, "Error processing server response", Toast.LENGTH_SHORT).show();
        }
    }

    private void getFaceID(JSONArray prob) {
        try {
            for (int i = 0; i < 128; i++) {
                faceID[i] = (double)prob.get(i);
            }
        } catch (JSONException e) {
            Log.e("Volley", "JSONArray to faceid conversion.");
            Toast.makeText(this, "Error processing server response", Toast.LENGTH_SHORT).show();
        }
        //addPredictionText(Arrays.toString(faceID));
        addFaceToDB();
    }

    public void addFaceToDB(){
        String name = FirebaseAuth.getInstance().getUid();
        Map<String, Object> docData = new HashMap<>();
        docData.put("name", name);
        docData.put("dateAdded", new Timestamp(new Date()));
        docData.put("faceid", Arrays.asList(faceID));

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        assert name != null;
        db.collection("faceid").document(name)
                .set(docData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("TAG", "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("TAG", "Error writing document", e);
                    }
                });
    }
}