package com.baatcheat;
//https://stackoverflow.com/questions/50109885/firestore-how-can-read-data-from-outside-void-oncomplete-methods

import com.baatcheat.model.User;

import java.util.List;

public interface MyCallBack {
    void onCallBack(String[] topicsAndSynonyms);
    void onLocationCallback(double[] locationData);
    void onMatchedUsersCallback(List<User> listOfMatchedUsers);
    void onReplacementListCallback(List<String> replacementList);
}
