package com.baatcheat.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interest {

    public Interest(String name, List<String> synonyms, String cardName){
        this.name=name;
        this.synonyms=new ArrayList<>(synonyms);
        this.cardName=cardName;
    }



    public Interest(String name, String cardName) {
        this.cardName=cardName;
        this.name=name;
        //this.synonyms=new ArrayList<>(synonyms);

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }
    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
    }
    public Map<String,Object> convertToMap(){
        Map<String,Object>m=new HashMap<>();
        m.put("name",this.name);
        m.put("synonyms",this.synonyms);
        m.put("cardName",this.cardName);
        return m;
    }

    public void updateSynonyms(String[] topicsAndSynonyms){
        for(String x:topicsAndSynonyms){
            if(!this.synonyms.contains(x)){
                this.synonyms.add(x);
            }
        }
    }


    public Interest(Map<String,Object>map){
        this.name=(String)map.get("name");
        this.synonyms=(ArrayList<String>)map.get("synonyms");
        this.cardName=(String)map.get("cardName");

    }

    public boolean doIHaveYourSynonym(String word){
        return synonyms.contains(word);
    }

    public String name;
    public List<String> synonyms;
    public String cardName;
}
