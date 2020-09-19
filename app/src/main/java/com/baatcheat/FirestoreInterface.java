package com.baatcheat;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.baatcheat.model.Card;
import com.baatcheat.model.Interest;
import com.baatcheat.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.baatcheat.BuildProfile.adapMap;
import static com.baatcheat.BuildProfile.cardNameToCard;
import static com.baatcheat.Homepage.currentUser;
import static com.baatcheat.Homepage.currentUserId;

class FirestoreInterface {
    private static final String TAG = "FIRESSTOREINTERFACE:";
    //replace given list with list containing interest keywords
    void interestSearch(final List<String> userInterests, final MyCallBack myCallBack){
        FirebaseFirestore db= FirebaseFirestore.getInstance();
        if(!currentUser.interests.isEmpty())
        db.collection("interest")
                .whereArrayContainsAny("synonyms",userInterests)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List <String>replacementList=new ArrayList<>();
                        for(QueryDocumentSnapshot document:queryDocumentSnapshots){
                            Interest i=new Interest(document.getData());
                            for(String name:userInterests) {
                                if(i.doIHaveYourSynonym(name)){
                                    replacementList.add(i.getName());
                                }
                            }
                        }
                        //we now have a replacementList! and we pass it
                        myCallBack.onReplacementListCallback(replacementList);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG,"ERROR INTERESTSEARCH"+e);
                    }
                });
    }

    //replace given list with list containing interest keywords
    void recommendationSearch(final List<String> userInterests, final MyCallBack myCallBack){
        FirebaseFirestore db= FirebaseFirestore.getInstance();
        db.collection("interest")
                .whereArrayContainsAny("synonyms",userInterests)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List <String>replacementList=new ArrayList<>();
                        for(QueryDocumentSnapshot document:queryDocumentSnapshots){
                            Interest i=new Interest(document.getData());
                            for(String name:userInterests) {
                                if(i.doIHaveYourSynonym(name)){
                                    replacementList.add(i.getName());
                                }
                            }
                        }
                        //we now have a replacementList! and we pass it
                        myCallBack.onReplacementListCallback(replacementList);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG,"ERROR INTERESTSEARCH"+e);
                    }
                });
    }


    //searching properly within interests
    void searchForSynonyms(final String[] topicAndSynonyms, final String cardName, final String userId, final User u){
        //now we clean up the user just in case he already has some pre stored interests
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("interest")
                .whereArrayContainsAny("synonyms", Arrays.asList(topicAndSynonyms))
                .whereEqualTo("cardName",cardName)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            List<Interest> interestList = new ArrayList<>();
                            String lastDocumentID="";
                            for(QueryDocumentSnapshot document: Objects.requireNonNull(task.getResult())){
                                Map<String,Object> mapOfDocument=document.getData();
                                String documentString= Objects.requireNonNull(mapOfDocument.get("name")).toString();
                                List<String> documentSynonyms=(List<String>)mapOfDocument.get("synonyms");
                                String documentCardName= Objects.requireNonNull(mapOfDocument.get("cardName")).toString();
                                assert documentSynonyms != null;
                                Log.d(TAG,documentCardName+","+documentSynonyms.toString()+","+documentString);
                                Interest i=new Interest(documentString,documentSynonyms,documentCardName);
                                interestList.add(i);
                                lastDocumentID=document.getId();
                            }
                            if(interestList.isEmpty()){
                                Log.d(TAG,"There is no common interest so we add "+topicAndSynonyms[0]);
                                //we add a new document into our interests collection with the name, cardName and synonyms here.
                                Interest newInterest= new Interest(topicAndSynonyms[0], Arrays.asList(topicAndSynonyms),cardName);
                                insertNewInterest(newInterest);
                                //remember!! we need to update user as well here!!
                                updateUserInterest(userId,u,topicAndSynonyms[0]);
                            }else {
                                Log.d(TAG, "We have found a common interest! It is:"+interestList.get(0).getName());
                                //take the name of the common interest and store it in the user's list, along with the synonyms of this one which haven't been tracked yet.
                                Interest interestToUpdate=interestList.get(0);
                                interestToUpdate.updateSynonyms(topicAndSynonyms);
                                updateInterestList(lastDocumentID,interestToUpdate);
                                //remember, we need to add the interest to the user here.
                                updateUserInterest(userId,u,interestList.get(0).name);
                            }
                        }else {
                            Log.d(TAG,"ERROR"+task.getException());
                        }
                    }
                });
    }

    //adding a new document with our brand new interest
    private void insertNewInterest(Interest newInterest){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String,Object> m=newInterest.convertToMap();
        db.collection("interest")
                .add(m)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG,"We have published a new Interest in our database");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
    }

    //updating an existing document to contain more synonyms
    private void updateInterestList(String documentID, Interest interestToUpdate){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String,Object>m=interestToUpdate.convertToMap();
        db.collection("interest")
                .document(documentID)
                .set(m)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG,"We have updated an Interest in our database");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
    }

    //updating user on the database
    public  void  updateUser(User u){

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(u.getUserID())
                .set(u.convertToMap())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG,"We have updated the User data.");
                    }
                });
    }

    private void updateUserInterest(String currentUserID, User u, final String interestName){
        //only add an interest if it does not exist already!
        if(!u.interests.contains(interestName))
            u.interests.add(interestName);
        Map<String,Object>m=u.convertToMap();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(currentUserID)
                .set(m)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG,"We have updated an Interest in our user which is "+interestName);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });

    }

    void findPeopleWithCommonInterests(final User currentUser, final String currentUserDocumentID, final List<String> interestList, final double[] locationValues, final double currentRangeOfQuery, final MyCallBack myCallBack){
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        //update current user's location
        currentUser.latitude=locationValues[0];
        currentUser.longitude=locationValues[1];
        //post it to db
        db.collection("users")
                .document(currentUserDocumentID)
                .set(currentUser.convertToMap())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG,"User Location updated");
                        //we are searching by latitude and cross referencing it with data from longitude search
                        searchByLatitude(interestList, locationValues, currentRangeOfQuery, new FirestoreDataInterface() {
                            @Override
                            public void onFirestoreCallBack(Set<User> usersAlreadyFound) {
                                Log.d(TAG,"WE WILL NOW SEARCH BY LONGITUDE");
                                searchByLongitude(currentUser, interestList, locationValues, currentRangeOfQuery,myCallBack,usersAlreadyFound);
                            }

                            @Override
                            public void onUriObtained(Uri obtainedUri) {

                            }

                            @Override
                            public void onUserCallback(User retrievedUser) throws IOException {

                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG,"ERROR:"+e);
                    }
                });


    }

    private void  searchByLatitude(List<String> interestList, double[] locationValues, double currentRangeOfQuery, final FirestoreDataInterface firestoreDataInterface){
        Log.d(TAG,"WE ARE GOING TO SEARCH ARRAY");
        double currentLatitude=locationValues[0];
        final double valueOfOneLatitude=1/110.0;

        //ensure that the interests are all in lowercase
        List<String>lowerCaseInterests=new ArrayList<>();
        for(String i:interestList){
            String x=i.toLowerCase();
            lowerCaseInterests.add(x);
        }
        Log.d(TAG,"lowercase interests are "+lowerCaseInterests.toString());

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        //searching people by latitude:
        db.collection("users")
                .whereArrayContainsAny("interests",lowerCaseInterests)
                .whereLessThanOrEqualTo("latitude", currentLatitude + currentRangeOfQuery * valueOfOneLatitude)
                .whereGreaterThanOrEqualTo("latitude", currentLatitude - currentRangeOfQuery * valueOfOneLatitude)
                .whereEqualTo("searchVisibility",true)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        Set<User> setOfLatUsers=new HashSet<>();
                        if(task.isSuccessful()){
                            for(QueryDocumentSnapshot document: Objects.requireNonNull(task.getResult())){
                                User u=new User(document.getData());
                                u.setUserID(document.getId());
                                setOfLatUsers.add(u);
                                Log.d(TAG,"Current user: "+u.getUserName());
                            }
                            Log.d(TAG,"TOTAL NUMBER OF USERS FOUND:"+setOfLatUsers.size());
                            //now we have our set of users pruned by Latitude.
                            //now we search within longitude
                            firestoreDataInterface.onFirestoreCallBack(setOfLatUsers);
                        }
                        else {
                            Log.d(TAG,"LAT TASK WAS UNSUCCESSFUL");
                        }

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG,"ERROR:"+e);
                    }
                });
    }

    private void  searchByLongitude(final User currentUser, List<String> interestList, double[] locationValues, double currentRangeOfQuery, final MyCallBack myCallBack, final Set<User> usersAlreadyFound){
        Log.d(TAG,"WE ARE GOING TO SEARCH ARRAY");
        double currentLatitude=locationValues[0];
        double currentLongitude=locationValues[1];

        //ensure that the interests are all in lowercase
        List<String>lowerCaseInterests=new ArrayList<>();
        for(String i:interestList){
            String x=i.toLowerCase();
            lowerCaseInterests.add(x);
        }
        Log.d(TAG,"lowercase interests are "+lowerCaseInterests.toString());

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        //searching people by latitude:
        db.collection("users")
                .whereArrayContainsAny("interests",lowerCaseInterests)
                .whereLessThanOrEqualTo("longitude", currentLongitude + currentRangeOfQuery * (1 / valueOfOneLongitude(currentLatitude)))
                .whereGreaterThanOrEqualTo("longitude", currentLongitude - currentRangeOfQuery * (1 / valueOfOneLongitude(currentLatitude)))
                .whereEqualTo("searchVisibility",true)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        Set<User> setOfLongUsers=new HashSet<>();
                        Set<String> idsOfUsers=new HashSet<>();
                        if(task.isSuccessful()){
                            for(QueryDocumentSnapshot document: Objects.requireNonNull(task.getResult())){
                                User u=new User(document.getData());
                                u.setUserID(document.getId());
                                setOfLongUsers.add(u);
                                Log.d(TAG,"Current user: "+u.getUserName());
                            }
                            Log.d(TAG,"TOTAL NUMBER OF USERS FOUND:"+setOfLongUsers.size());
                            //now we have our set of users pruned by Longitude
                            //we now merge this with the set of already achieved users
                            StringBuilder x= new StringBuilder();
                            Iterator<User> i=setOfLongUsers.iterator();
                            List<User> matchedUsers=new ArrayList<>();
                            while (i.hasNext()){
                                User current=i.next();
                                for (User user : usersAlreadyFound) {
                                    if (current.compare(user) && !current.compare(currentUser)) {
                                        //if they are matching,
                                        matchedUsers.add(current);
                                        x.append(current.getUserName()).append(",");

                                    }
                                }
                            }
                            //we will sort the matchedUsers based on the number of common interests
                            List<User>orderedUsers=new ArrayList<>();
                            HashMap<String,Integer> userNameToMatchCount=new HashMap<>();
                            for(User u:matchedUsers){
                                int numberOfMatches=0;
                                for(String currentInterest:u.interests){
                                    if(currentUser.interests.contains(currentInterest)){
                                        numberOfMatches++;
                                    }
                                }
                                userNameToMatchCount.put(u.getUserName(),numberOfMatches);
                            }
                            for(String nam:sortByValue(userNameToMatchCount).keySet()){
                                for(User u:matchedUsers){
                                    if(u.getUserName().equals(nam)){
                                        orderedUsers.add(u);
                                    }
                                }
                            }


                            //we now have a matched users list matchedUsers
                            Log.d(TAG,"OUR RETAINED LIST:"+x.toString());
                            myCallBack.onMatchedUsersCallback(orderedUsers);
                        }
                        else {
                            Log.d(TAG,"LAT TASK WAS UNSUCCESSFUL");
                        }

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG,"ERROR:"+e);
                    }
                });
    }

    private static Map<String, Integer> sortByValue(Map<String, Integer> unsortMap) {

        // 1. Convert Map to List of Map
        List<Map.Entry<String, Integer>> list =
                new LinkedList<Map.Entry<String, Integer>>(unsortMap.entrySet());

        // 2. Sort list with Collections.sort(), provide a custom Comparator
        //    Try switch the o1 o2 position for a different order
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        // 3. Loop the sorted list and put it into a new insertion order Map LinkedHashMap
        Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        /*
        //classic iterator example
        for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext(); ) {
            Map.Entry<String, Integer> entry = it.next();
            sortedMap.put(entry.getKey(), entry.getValue());
        }*/


        return sortedMap;
    }

    private static double valueOfOneLongitude(double latitude){
        double value=Math.cos(latitude * Math.PI / 180);
        //reference:https://www.space.com/17638-how-big-is-earth.html
        double radiusOfEarth = 6356.0;
        value=value*Math.PI/180* radiusOfEarth;
        return value;

    }

    void clearUserInterests(String currentUserId, User currentUser){
        //locally
        currentUser.setInterests(new ArrayList<String>());

        //in firestore
        FirebaseFirestore db=FirebaseFirestore.getInstance();
        db.collection("users")
                .document(currentUserId)
                .set(currentUser.convertToMap())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG,"WE HAVE REMOVED ALL USER INTERESTS AND ARE NOW UPDATNG.");
                    }
                });
    }


    //need to make one which permanently deletes cards as well!
    void deleteCard(final Card card){
        Log.d(TAG,"WE ARE DELETING this guy:"+card.getCardName());
        final FirebaseFirestore db= FirebaseFirestore.getInstance();
        db.collection("cards")
                .whereEqualTo("cardName",card.getCardName())
                .whereEqualTo("userEmailId",currentUser.getEmail())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for(QueryDocumentSnapshot documentSnapshot:queryDocumentSnapshots){

                            db.collection("cards")
                                    .document(documentSnapshot.getId())
                                    .delete()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d(TAG,"WE HAVE SUCCESFULLY DELETED THIS GUY FROM THE DATABASE "+card.getCardName());
                                        }
                                    });
                        }
                    }
                });
    }

    void storeOneCard(final Card c) {
        FirebaseFirestore db= FirebaseFirestore.getInstance();
        db.collection("cards")
                .whereEqualTo("cardName",c.getCardName())
                .whereEqualTo("userEmailId",currentUser.getEmail())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        //there will only be one, if one exists
                        if(queryDocumentSnapshots.size()>0){
                            //it exists in database
                            //so we need only update this card
                            for(QueryDocumentSnapshot document:queryDocumentSnapshots){
                                String documentId=document.getId();
                                updateOneCard(documentId,c);
                            }
                        }
                        else{
                            //we need to add this new card into database
                            addOneCard(c);
                        }
                    }
                });
    }

    private void updateOneCard(String documentId, final Card c){
        FirebaseFirestore db=FirebaseFirestore.getInstance();
        db.collection("cards")
                .document(documentId)
                .set(c)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG,"We have updated the card "+c.getCardName());
                    }
                });
    }

    private void addOneCard(final Card c){
        FirebaseFirestore db= FirebaseFirestore.getInstance();
        db.collection("cards")
                .add(c)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG,"WE HAVE ADDED A NEW CARD "+c.getCardName());
                    }
                });

    }

    void retrieveCards(final TableLayout whereToAddNewCards, final Context applicationContext, final Context main3ActivityContext) {
        FirebaseFirestore db=FirebaseFirestore.getInstance();
        db.collection("cards")
                .whereEqualTo("userEmailId",currentUser.getEmail())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        //these are all cards that exist in db
                        for(QueryDocumentSnapshot documentSnapshot: queryDocumentSnapshots){
                            //for each card we get from the database:

                            if(cardNameToCard.containsKey(documentSnapshot.getData().get("cardName"))){
                                //this card is already visible in our user's view
                                //we need to update the interest list currently in user's view

                                //card in db
                                Map<String,Object> retrievedData=documentSnapshot.getData();

                                //card in user's voiew
                                Card currentCardInView= cardNameToCard.get(retrievedData.get("cardName"));


                                List<Object>objectListRetrieved=(List<Object>) retrievedData.get("storedInterests");
                                List<Interest>interestListRetrieved=new ArrayList<>();
                                assert objectListRetrieved != null;

                                //get all the interests which were stored in database
                                for(Object o:objectListRetrieved){
                                    HashMap<String, Object> m = (HashMap<String, Object>) o;
                                    Interest i = new Interest((String) m.get("name"), new ArrayList<String>(), (String) m.get("cardName"));
                                    interestListRetrieved.add(i);
                                }

                                assert currentCardInView != null;
                                List<Interest>visibleInterests=currentCardInView.getStoredInterests();
                                //debug:
                                for(Interest i:visibleInterests){
                                    Log.d(TAG,"I CAN SEE "+i.getName());
                                }


                                Set <String> retrieved=new HashSet();
                                for(Interest i:interestListRetrieved){
                                    retrieved.add(i.getName());
                                }
                                Set <String> visible=new HashSet();
                                for(Interest i:visibleInterests){
                                    visible.add(i.getName());
                                }

                                retrieved.removeAll(visible);

                                List<Interest>finalListOfInterests=new ArrayList<>();
                                for(String x:retrieved){
                                    finalListOfInterests.add(new Interest(x, (String) retrievedData.get("cardName")));
                                    Log.d(TAG,"OUUR FINAL INTEREST LIST CONTATINS"+x);
                                }


                                //currentCardInView.setStoredInterests(finalInterestsWithoutReplicas);
                                currentCardInView.setStoredInterests(new ArrayList<Interest>());

                                currentCardInView.setStoredInterests(finalListOfInterests);
                                //put this card back in its place and update its interest adapter
                                cardNameToCard.remove((String) retrievedData.get("cardName"));
                                cardNameToCard.put((String) retrievedData.get("cardName"),currentCardInView);
                                Objects.requireNonNull(adapMap.get((String) retrievedData.get("cardName"))).notifyDataSetChanged();
                            }
                            else{
                                //this card does not exist in our user's view
                                //create a new card
                                Map<String,Object> retrievedData=documentSnapshot.getData();
                                List<Object>objectListRetrieved=(List<Object>) retrievedData.get("storedInterests");
                                List<Interest>interestListRetrieved=new ArrayList<>();

                                //get all interests in the online store
                                assert objectListRetrieved != null;
                                for(Object o:objectListRetrieved){
                                    HashMap<String, Object> m = (HashMap<String, Object>) o;
                                    Interest i = new Interest((String) m.get("name"), new ArrayList<String>(), (String) m.get("cardName"));
                                    interestListRetrieved.add(i);
                                }

                                //prune uselesss values
                                List<Interest> copyInterestList= new ArrayList<>();
                                for(Interest i:interestListRetrieved){
                                    if(!i.getName().equals("")&&!i.getName().equals("ADD ONE MORE")){
                                        copyInterestList.add(i);
                                    }
                                }

                                //generate this card in user's view
                                Card c = new Card((String) retrievedData.get("userEmailId"), (String) retrievedData.get("cardName"), copyInterestList, (boolean) retrievedData.get("visible"));
                                c.addNewCard(whereToAddNewCards, applicationContext, main3ActivityContext);
                            }
                        }
                    }
                });
    }


    public void openArApplication(final Context applicationContext, final String arAppPackage, String documentIdOfOtherPerson) {
        FirebaseFirestore db=FirebaseFirestore.getInstance();
        db.collection("users")
                .document(documentIdOfOtherPerson)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        double latitude=(double)documentSnapshot.getData().get("latitude");
                        double longitude=(double)documentSnapshot.getData().get("longitude");
                        String combinedLocationString=latitude+","+longitude;
                        Log.d(TAG,"COMBINED LOCATION STRING OF PERSON IN FRONT:"+combinedLocationString);
                        if((boolean)documentSnapshot.getData().get("searchVisibility"))
                            arInterface.startNewActivity(applicationContext,arAppPackage,combinedLocationString);
                        else
                            Toast.makeText(applicationContext,"Please request user to turn Search Visibility 'ON' in settings to share their position",Toast.LENGTH_LONG).show();
                    }
                });
    }

    void getUser(String documentId, final FirestoreDataInterface firestoreCallBack){
        FirebaseFirestore db=FirebaseFirestore.getInstance();
        db.collection("users")
                .document(documentId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Map <String,Object>retrievedMap=documentSnapshot.getData();
                        User retrievedUser=new User(retrievedMap);
                        try {
                            firestoreCallBack.onUserCallback(retrievedUser);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                });
    }

    public void retrieveCardsToDisplay(final TableLayout whereToAddNewCards, final Context applicationContext, final Context displayProfileContext, User foundUser) {

        FirebaseFirestore db=FirebaseFirestore.getInstance();
        db.collection("cards")
                .whereEqualTo("userEmailId",foundUser.getEmail())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        //these are all cards that exist in db
                        for(QueryDocumentSnapshot documentSnapshot: queryDocumentSnapshots){
                            //this card does not exist in our user's view
                            //create a new card
                            Map<String,Object> retrievedData=documentSnapshot.getData();
                            List<Object>objectListRetrieved=(List<Object>) retrievedData.get("storedInterests");
                            List<Interest>interestListRetrieved=new ArrayList<>();
                            assert objectListRetrieved != null;
                            for(Object o:objectListRetrieved){
                                HashMap<String, Object> m = (HashMap<String, Object>) o;
                                Interest i = new Interest((String) m.get("name"), new ArrayList<String>(), (String) m.get("cardName"));
                                interestListRetrieved.add(i);
                            }
                            List<Interest> copyInterestList= new ArrayList<>();
                            for(Interest i:interestListRetrieved){
                                if(!i.getName().equals("")){
                                    copyInterestList.add(i);
                                }
                            }

                            Card c = new Card((String) retrievedData.get("userEmailId"), (String) retrievedData.get("cardName"), copyInterestList, (boolean) retrievedData.get("visible"));
                            c.addNewCardToView(whereToAddNewCards, applicationContext, displayProfileContext);

                        }
                    }
                });
    }
}