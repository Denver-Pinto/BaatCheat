package com.baatcheat;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.baatcheat.model.Card;
import com.baatcheat.model.Interest;
import com.baatcheat.model.User;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.jacksonandroidnetworking.JacksonParserFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.baatcheat.BuildProfile.cardNameToCard;

class DatamuseQuery {

    private static final String TAG = "C: DatamuseQuery";
    //call this function with name and application context
    //should return string[]
    private void getSynonyms(final String name, Context context, final int limiter, final MyCallBack myCallBack){
        //why use this: apparently better than Retrofit
        //https://github.com/amitshekhariitbhu/Fast-Android-Networking
        //https://medium.com/@filswino/making-rest-calls-download-upload-files-with-one-line-of-code-on-android-no-retrofit-needed-5c0574f41476

        AndroidNetworking.initialize(context);
        // Then set the JacksonParserFactory like below
        AndroidNetworking.setParserFactory(new JacksonParserFactory());
        AndroidNetworking.get("https://api.datamuse.com/words?ml={topicName}")
                .addPathParameter("topicName",name)
                .addQueryParameter("limit","3")
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        StringBuilder finalResponse= new StringBuilder(name);
                        for(int i=0;i<limiter;i++){
                            JSONObject obj;
                            try {
                                obj = response.getJSONObject(i);
                                String word= obj.getString("word");
                                if(!word.equals(name))
                                    finalResponse.append(",").append(word);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        String x=new String(finalResponse);
                        String[] topicsAndSynonyms=x.split(",");
                        myCallBack.onCallBack(topicsAndSynonyms);
                        Log.d(TAG, x);
                    }
                    @Override
                    public void onError(ANError anError) {
                        Log.d(TAG, "WE HAVE A PROBLEM"+ anError);
                    }
                });
    }

    //to put entire list inside(from the keep ui into the collection of interests
    void listUpdater(final String cardName, List<Interest> allInterestsInCard, Context c, int synonymLimiter){

        List<String> interests=new ArrayList<>();
        StringBuilder z= new StringBuilder();
        for(Interest d:allInterestsInCard){
            if(!"ADD ONE MORE".equals(d.getName())) {
                interests.add(d.getName());
                z.append(d.getName()).append(",");
            }
        }
        Log.d(TAG,"listUpdater: "+z.toString());

        //get only the non duplicate list
        List<Object> nonDuplicateList= null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            nonDuplicateList = interests.stream().distinct().collect(Collectors.toList());
        }

        assert nonDuplicateList != null;
        List<String> interestWithoutDuplicates= Lists.transform(nonDuplicateList, Functions.toStringFunction());

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            interestWithoutDuplicates = interests;

        Card currentCard= cardNameToCard.get(cardName);
        StringBuilder v= new StringBuilder();
        assert currentCard != null;
        for(Interest i:currentCard.getStoredInterests()){
            v.append(i.getName()).append(",");
        }
        Log.d(TAG,"INITIALLY The CARD HAD: "+v);

        //currentCard.setStoredInterests(new ArrayList<Interest>());
        //List<Interest>interestList=currentCard.getStoredInterests();
        //update the cards we have stored locally
        for(String x:interestWithoutDuplicates){
            Interest i=new Interest(x,cardName);
            Log.d(TAG,"ADDING: "+x);
            //currentCard.addInterest(i);
        }
        //currentCard.setStoredInterests(interestList);
        //cardNameToCard.remove(cardName);
        //cardNameToCard.put(cardName,currentCard);
        //adapMap.get(cardName).notifyDataSetChanged();

        //uploading all interests to database
        for(String x:interestWithoutDuplicates){
            //for each interest in this card
            getSynonyms(x, c, synonymLimiter, new MyCallBack() {
                @Override
                public void onCallBack(String[] topicsAndSynonyms) {
                    //we now have all the synonyms for the word x
                    FirestoreInterface f= new FirestoreInterface();
                    f.searchForSynonyms(topicsAndSynonyms,cardName, Homepage.currentUserId, Homepage.currentUser);
                }

                @Override
                public void onLocationCallback(double[] locationData) {
                }

                @Override
                public void onMatchedUsersCallback(List<User> setOfMatchedUsers) {
                }

                @Override
                public void onReplacementListCallback(List<String> replacementList) {
                }
            });
        }
    }
}
