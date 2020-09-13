package com.baatcheat.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.net.Uri;

public class User {

    public double latitude;
    public double longitude;
    private String userID, userName, email, bio;
    public List<String> interests;
    private Uri userImageURI;
    private boolean searchVisibility;

    public String getBio() {
        return this.bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Uri getUserImageURI() {
        return userImageURI;
    }

    public void setUserImageURI(Uri userImageURI) {
        this.userImageURI = userImageURI;
    }

    public boolean isSearchVisibility() {
        return this.searchVisibility;
    }

    public void setSearchVisibility(boolean searchVisibility) {
        this.searchVisibility = searchVisibility;
    }

    public void cleanUpInterests() {
        this.interests.clear();
    }

    public User(String userName){
        this.userName = userName;
    }


    public String getUserID() {
        return this.userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getInterests() {
        return this.interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }

    @Override @NonNull
    public String toString() {
        return "User{" +
                "userID='" + this.userID + '\'' +
                ", name='" + this.userName + '\'' +
                ", email='" + this.email + '\'' +
                ", interests=" +
                '}';
    }

    //temporary user setup values
    public User(String email,String userName,double latitude,double longitude,List<String> interests,boolean searchVisibility){
        this.userName=userName;
        this.email=email;
        this.longitude=longitude;
        this.latitude=latitude;
        this.interests=new ArrayList<>(interests);
        this.searchVisibility=searchVisibility;
    }

    public boolean compare(User u){
        if(this.userName.equals(u.getUserName())){
            if(this.email.equals(u.getEmail())){
                return true;
            }
        }
        return false;
    }

    public User(Map<String, Object> data) {
        if(data.get("interests")!=null)
            this.interests = (ArrayList<String>) data.get("interests");
        else
            this.interests = new ArrayList<>();
        this.userName = (String) data.get("userName");
        if(data.get("longitude")!=null)
            this.longitude = (double) data.get("longitude");
        if(data.get("latitude")!=null)
            this.latitude = (double) data.get("latitude");
        this.email = (String) data.get("email");
        if(data.get("searchVisibility")!=null)
            this.searchVisibility=(boolean)data.get("searchVisibility");
        if(data.get("userImageURI")!=null)
            this.userImageURI=Uri.parse((String)data.get("userImageURI"));
        if(data.get("bio")!=null)
            this.bio=(String)data.get("bio");
    }

    public Map<String,Object> convertToMap(){
        Map<String,Object>m=new HashMap<>();
        m.put("userName",this.userName);
        m.put("longitude",this.longitude);
        m.put("latitude",this.latitude);
        m.put("searchVisibility",this.searchVisibility);
        m.put("email",this.email);
        //update bio
        if(this.bio!=null)
            m.put("bio",this.bio);
        //ensure that the interests are all in lowercase
        List<String>interests2=new ArrayList<>();
        if(interests!=null)
            for(String i:this.interests){
                String x=i.toLowerCase();
                interests2.add(x);
            }

        this.interests=interests2;

        m.put("interests",this.interests);
        if(userImageURI!=null)
            m.put("userImageURI",this.userImageURI.toString());//passsing the uri as a string. we have to read the string as well.
        return m;
    }

}
