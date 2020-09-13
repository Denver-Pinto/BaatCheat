package com.baatcheat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;


public class FirebaseStorageInterface {

    final String TAG="FIREBASESTORAGE";
    public void addImage(Uri imageUri, String documentId, final FirestoreDataInterface firestoreDataInterface ) {
        FirebaseStorage storage = FirebaseStorage.getInstance();// Create a storage reference from our app
        StorageReference storageRef = storage.getReference();
        final StorageReference profilePictureRef = storageRef.child("profilePictures/"+documentId);
        UploadTask uploadTask= profilePictureRef.putFile(imageUri);

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG,"UPLOADED IMAGE");
            }
        });


        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                // Continue with the task to get the download URL
                return profilePictureRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    firestoreDataInterface.onUriObtained(downloadUri);

                } else {
                    // Handle failures
                    // ...
                }
            }
        });
    }

    public void displayImage(Uri userImageURI, final ImageView profilePicture)  {
        FirebaseStorage storage = FirebaseStorage.getInstance();// Create a storage reference from our app
// Create a reference to a file from a Google Cloud Storage URI
        StorageReference profilePictureRef = storage.getReferenceFromUrl(String.valueOf(userImageURI));
        final long FIVE_MEGABYTE = 5*1024 * 1024;
        profilePictureRef.getBytes(FIVE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // Data for "images/island.jpg" is returns, use this as needed
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                profilePicture.setImageBitmap(Bitmap.createScaledBitmap(bmp, profilePicture.getWidth(),
                        profilePicture.getHeight(), false));

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });

// ImageView in your Activity

// Download directly from StorageReference using Glide
// (See MyAppGlideModule for Loader registration)

    }
}
