package com.baatcheat;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.baatcheat.model.User;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class InterestSearchFragment extends Fragment {
    public final String TAG="INTEREST MATCHING ";
    public static double currentRangeOfQuery=100;
    public static String errorMessage="We need the current location to find people nearby!";

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

        final EditText searchBar=getActivity().findViewById(R.id.searchBar);
        Button findPeople=getActivity().findViewById(R.id.findPeople);

        //set up recycler:
        final RecyclerView currentRecycler=getActivity().findViewById(R.id.availableUsers);
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
                View view = getActivity().findViewById(R.id.findPeople);

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
        new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }


    public void getLocationPermission(){
        if (ContextCompat.checkSelfPermission(getActivity(), ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted

            ActivityCompat.requestPermissions(getActivity(),new String[]{ACCESS_FINE_LOCATION}, 1);
        }
    }

    public void getLocationAndFindPeople(final RecyclerView currentRecycler, final List<String>interestListToSearch){

        //activate location permissions.
        LocationInterface l=new LocationInterface();
        l.getLocationData(getActivity().getApplicationContext(), new MyCallBack() {
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
                            currentRecycler.addOnItemTouchListener(new RecyclerTouchListener(Objects.requireNonNull(getActivity()).getApplicationContext(), currentRecycler,new RecyclerTouchListener.ClickListener() {
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
        MaterialTextView errorTextView = Objects.requireNonNull(getActivity()).findViewById(R.id.error_textview);
        if(visible)
            errorTextView.setVisibility(View.VISIBLE);
        else
            errorTextView.setVisibility(View.GONE);
    }

}
