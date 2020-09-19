package com.baatcheat;

import android.Manifest;
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
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.baatcheat.model.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import android.util.Base64;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.Response;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.app.Activity.RESULT_OK;

public class InterestSearchFragment extends Fragment {
    public final String TAG="INTEREST MATCHING ";
    static double currentRangeOfQuery=100;
    private final static String errorMessage="We need the current location to find people nearby!";

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

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public InterestSearchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_interest_search, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final EditText searchBar= requireActivity().findViewById(R.id.searchBar);
        Button findPeople= requireActivity().findViewById(R.id.findPeople);
        FloatingActionButton scanPeople = requireActivity().findViewById(R.id.scan_face_button);

        //set up recycler:
        final RecyclerView currentRecycler=requireActivity().findViewById(R.id.availableUsers);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        currentRecycler.setLayoutManager(llm);

        //requesting location permissions if not granted
        //LocationInterface l1=new LocationInterface();
        getLocationPermission();

        findPeople.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showErrorTextView(false);
                String searchBarValue = searchBar.getText().toString();
                if (searchBarValue.isEmpty()) {
                    searchBar.setError(getString(R.string.empty_error));
                } else {
                    List<String> cleanedUpList = cleanUpString(searchBarValue);
                    //now replace all synonyms if any are found, with the respective topic name
                    FirestoreInterface fs = new FirestoreInterface();
                    fs.recommendationSearch(cleanedUpList, new MyCallBack() {
                        @Override
                        public void onCallBack(String[] topicsAndSynonyms) {

                        }

                        @Override
                        public void onLocationCallback(double[] locationData) {

                        }

                        @Override
                        public void onMatchedUsersCallback(List<User> listOfMatchedUsers) {

                        }

                        @Override
                        public void onReplacementListCallback(List<String> replacementList) {
                            StringBuilder x = new StringBuilder();
                            for (String y : replacementList) {
                                x.append(y).append(",");
                            }
                            Log.d(TAG, "ReplacementList is" + x.toString());
                            getLocationAndFindPeople(currentRecycler,replacementList);
                        }
                    });
                }
            }
        });

        //FaceRecognitionStuff:
        scanPeople.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if we have write permission
                int permission = ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

                if (permission != PackageManager.PERMISSION_GRANTED) {
                    // We don't have permission so prompt the user
                    ActivityCompat.requestPermissions(
                            requireActivity(),
                            PERMISSIONS_STORAGE,
                            REQUEST_EXTERNAL_STORAGE
                    );
                }

                takePicture();
            }
        });

        requestQueue = Volley.newRequestQueue(requireContext());  // This setups up a new request queue which we will need to make HTTP requests.

    }

    private List<String> cleanUpString(String searchBarValue) {

        searchBarValue=searchBarValue.toLowerCase();
        //remove any extra spaces between words, between commas and at the start/end of this string also, remove any weird characters
        searchBarValue=searchBarValue.replaceAll("[^,a-zA-Z0-9\\s]",",");
        searchBarValue=searchBarValue.replaceAll("\\s\\s+"," ");//replace >1 space

        Log.d(TAG,"searchbarvalue IS"+searchBarValue);

        String[] arrayOfInterests=searchBarValue.split(",");
        arrayOfInterests[0]=arrayOfInterests[0].replaceAll("^\\s+","");
        arrayOfInterests[arrayOfInterests.length-1]=arrayOfInterests[arrayOfInterests.length-1].replaceAll("\\s+$","");
        int k;
        StringBuilder x= new StringBuilder();
        List<String> finishedList=new ArrayList<>();
        for(k=0;k<arrayOfInterests.length;k++){
            arrayOfInterests[k]=arrayOfInterests[k].replaceAll("^\\s+","");
            arrayOfInterests[k]=arrayOfInterests[k].replaceAll("\\s+$","");
            if(!arrayOfInterests[k].equals("")){
                x.append(arrayOfInterests[k]).append(",");
                finishedList.add(arrayOfInterests[k]);
            }

        }
        Log.d(TAG,"VAL IS"+x.toString());
        //we now have a list which is without any extra spaces etc.
        //now we need to cross-check with all existing terms already in our database of interests.
        //if a word exists as synonym, replace with interestName
        return finishedList;

    }


//data from our request to the user

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0) {
                View view = requireActivity().findViewById(R.id.findPeople);

                boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                if (locationAccepted)
                    Snackbar.make(view, "Permission Granted, Now you can access location data and camera.", Snackbar.LENGTH_LONG).show();
                else {

                    Snackbar.make(view, "Permission Denied, You cannot access location data and camera.", Snackbar.LENGTH_LONG).show();

                    if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
                        showMessageOKCancel(errorMessage,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        getLocationPermission();
                                        requestPermissions(new String[]{ACCESS_FINE_LOCATION},
                                                1);
                                    }
                                });
                    }

                }
            }
        }
    }


    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        //creating an alert to display why we need each permission
        new AlertDialog.Builder(requireActivity())
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void getLocationPermission(){
        if (ContextCompat.checkSelfPermission(requireActivity(), ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted

            ActivityCompat.requestPermissions(requireActivity(),new String[]{ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void getLocationAndFindPeople(final RecyclerView currentRecycler, final List<String> interestListToSearch){

        //activate location permissions.
        LocationInterface l=new LocationInterface();
        l.getLocationData(requireActivity().getApplicationContext(), new MyCallBack() {
            @Override
            public void onCallBack(String[] topicsAndSynonyms) {

            }

            @Override
            public void onLocationCallback(double[] locationData) {
                Log.d(TAG,"WE ARE IN onLocatonCallBack");
                //we have location, now we search people!
                FirestoreInterface f=new FirestoreInterface();
                if(interestListToSearch.isEmpty()){
                    Log.d(TAG,"WE ARE IN if");
                    showErrorTextView(true);
                }
                else{
                    Log.d(TAG,"WE ARE IN else");
                    f.findPeopleWithCommonInterests(Homepage.currentUser, Homepage.currentUserId, interestListToSearch, locationData, currentRangeOfQuery, new MyCallBack() {
                        @Override
                        public void onCallBack(String[] topicsAndSynonyms) {

                        }

                        @Override
                        public void onLocationCallback(double[] locationData) {

                        }

                        @Override
                        public void onMatchedUsersCallback(final List<User> setOfMatchedUsers) {
                            StringBuilder x= new StringBuilder();
                            for(User u:setOfMatchedUsers){
                                x.append(u.getUserName()).append(",");
                            }
                            Log.d(TAG,"MATCHED USERS ARE:"+x);
                            //we now have a working list.
                            //now we just pass this data to the recyclerList

                            showErrorTextView(false);
                            if(setOfMatchedUsers.isEmpty()){
                                showErrorTextView(true);
                            }

                            //managing list
                            UserAdapter u=new UserAdapter(setOfMatchedUsers);
                            u.notifyDataSetChanged();
                            currentRecycler.setAdapter(u);

                            //for each data item in our recycler list, we need to know if clicked!
                            currentRecycler.addOnItemTouchListener(new RecyclerTouchListener(requireActivity().getApplicationContext(), currentRecycler,new RecyclerTouchListener.ClickListener() {
                                @Override
                                public void onClick(View view, int position) {
                                    User u=setOfMatchedUsers.get(position);
                                    Intent toDisplayProfile = new Intent(getContext(), DisplayProfile.class);
                                    toDisplayProfile.putExtra("UserID", u.getUserID());
                                    startActivity(toDisplayProfile);
                                }

                                @Override
                                public void onLongClick(View view, int position) {
                                    //Not needed
                                }
                            }));
                        }

                        @Override
                        public void onReplacementListCallback(List<String> replacementList) {

                        }

                    });
                }

            }

            @Override
            public void onMatchedUsersCallback(List<User> setOfMatchedUsers) {

            }

            @Override
            public void onReplacementListCallback(List<String> replacementList) {

            }

        });
    }

    private void showErrorTextView(boolean visible){
        MaterialTextView errorTextView =  requireActivity().findViewById(R.id.error_textview);
        if(visible)
            errorTextView.setVisibility(View.VISIBLE);
        else
            errorTextView.setVisibility(View.GONE);
    }

    //Face Recognition Stuff:
    private void takePicture() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile;
            try {
                photoFile = createImageFile();
                Uri photoURI = FileProvider.getUriForFile(requireContext(),
                        BuildConfig.APPLICATION_ID + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(getContext(),
                        "Photo file can't be created, please try again",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpeg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                FaceDetector.Builder(getContext()).setTrackingEnabled(false)
                .build();
        if(!faceDetector.isOperational()){
            // new AlertDialog.Builder(v.getContext()).setMessage("Could not set up the face detector!").show();
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
            Toast.makeText(requireContext(), "No Faces found", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(getContext(), "Recognising.. ", Toast.LENGTH_SHORT).show();
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
                        setPredictionTextResponse(response);
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // If there a HTTP error then add a note to our repo list.
                        //setPredictionText("Error while calling REST API");
                        Toast.makeText(getContext(), "Error while calling REST API", Toast.LENGTH_SHORT).show();
                        Log.e("Volley", error.toString());
                    }
                }
            );
        requestQueue.add(arrReq);// Add the request we just defined to our request queue.
    }

    private void setPredictionTextResponse(JSONObject response) {
        JSONArray prob,prob1;
        try {
            prob = response.getJSONArray("predictions");
            prob1 = prob.getJSONArray(0);

            Log.d("prob1 type",prob1.toString());

            getFaceID(prob1);

        } catch (JSONException e) {
            Log.e("Volley", "retrieving predictions JSONArray.");
            Toast.makeText(getContext(), "Error processing server response", Toast.LENGTH_SHORT).show();
        }
    }

    private void getFaceID(JSONArray prob) {
        try {
            for (int i = 0; i < 128; i++) {
                faceID[i] = (double)prob.get(i);
            }
        } catch (JSONException e) {
            Log.e("Volley", "JSONArray to faceid conversion.");
            Toast.makeText(getContext(), "Error processing server response", Toast.LENGTH_SHORT).show();
        }
        getMatch();
        //addPredictionText(Arrays.toString(faceID));
    }

    private void getMatch(){
        result = new HashMap<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("faceid")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                Log.d("TAG", document.getId() + " => " + document.getData());
                                //Log.d("TAG",document.getString("name"));
                                //Log.d("TAG",document.getData().get("faceid").toString());
                                result.put(document.getString("name"),(List<Double>)document.getData().get("faceid"));
                            }
                            Log.d("TAG","Processing Result");
                            processResult();
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private void processResult(){

        double bestdistance = 100;
        Double currentdistance ;
        String match = "";
        for(Map.Entry<String, List<Double>> ee : result.entrySet()) {
            String name = ee.getKey();
            Log.d("TAG",".."+name);
            List<Double> id_list = ee.getValue();
            Log.d("TAG",id_list.toString());
            Double[] id = id_list.toArray(new Double[0]);
            currentdistance = distanceBetween(faceID,id);
            Log.d("distance",currentdistance.toString());
            Log.d("name"," "+name);
            if(currentdistance < bestdistance){
                bestdistance = currentdistance;
                match = name;
            }
        }

        //Goto the profile of matched user
        Intent toDisplayProfile = new Intent(requireContext(), DisplayProfile.class);
        toDisplayProfile.putExtra("UserID", match);
        startActivity(toDisplayProfile);
    }

    private double distanceBetween(Double[] a, Double[] b){
        int i ;
        double sum =0;
        for(i =0;i<a.length;i++)
            sum = sum + Math.pow((b[i]-a[i]),2);
        return Math.sqrt(sum);
    }

    private Bitmap rotateBitmap(Bitmap original) {
        Matrix matrix = new Matrix();
        matrix.preRotate((float) 270);
        Bitmap rotatedBitmap = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
        original.recycle();
        return rotatedBitmap;
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
