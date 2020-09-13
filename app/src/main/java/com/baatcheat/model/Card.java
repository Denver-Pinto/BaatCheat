package com.baatcheat.model;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baatcheat.BuildProfile;
import com.baatcheat.InterestAdapter;
import com.baatcheat.R;
import com.baatcheat.RecyclerTouchListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class Card {
    public String getuserEmailId() {
        return userEmailId;
    }
    public void setuserEmailId(String userDocId) {
        this.userEmailId = userDocId;
    }
    public String getCardName() {
        return cardName;
    }
    public void setCardName(String topicName) {
        this.cardName = topicName;
    }
    public List<Interest> getStoredInterests() {
        return storedInterests;
    }
    public void setStoredInterests(List<Interest> storedInterests) {
        this.storedInterests = storedInterests;
    }
    public boolean getVisible() {
        return visible;
    }
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Card(String userEmailId, String cardName, List<Interest>storedInterests, boolean visible){
        this.userEmailId=userEmailId;
        this.cardName=cardName;
        this.storedInterests=storedInterests;
        this.visible=visible;
    }

//create a card with layout Data and data handlers
    public void addNewCard(TableLayout t, Context context,Context otherTypeOfContext){
        //physically producing a card
        //t is the layout we will put it inside
        t.setColumnStretchable(0,true);
        t.setColumnStretchable(1,true);
        TableRow tr= new TableRow(context);
        CardView c=new CardView(context);
        int idForThisRow= View.generateViewId();
        tr.setId(idForThisRow);
        //1st value to return
        BuildProfile.cardNameToTableRow.put(this.cardName,tr);
        if(this.visible)
            c.setCardBackgroundColor(Color.rgb(3, 218, 198));
        else
            c.setCardBackgroundColor(0x7B000000);

        c.layout(5,TableRow.LayoutParams.MATCH_PARENT,5,TableRow.LayoutParams.MATCH_PARENT);

        TextView txt=new TextView(context);
        txt.setText(this.cardName);
        txt.layout(TableRow.LayoutParams.MATCH_PARENT,20,TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.MATCH_PARENT);

        //after text, make checkbox
        CheckBox checkBox=new CheckBox(context);
        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,60);
        checkBox.setLayoutParams(parms);
        checkBox.setText(R.string.make_invisible);
        int generatedId=View.generateViewId();
        checkBox.setId(generatedId);
        checkBox.setChecked(!this.visible);
        //2nd return value
        BuildProfile.cardCheckBoxMap.put(this.cardName,checkBox);
        //Toast.makeText(context,"CURRENT CHECKBOX IS "+generatedId+" CARDNAME:"+this.cardName,Toast.LENGTH_LONG).show();

        //now we go inside each card:
        RelativeLayout rel=new RelativeLayout(context);
        rel.layout(TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.MATCH_PARENT);

        RecyclerView recyc=new RecyclerView(context);
        recyc.layout(TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.MATCH_PARENT);
        recyc.setVerticalScrollBarEnabled(true);

        //manage this recycler
        RecyclerView.LayoutManager mLayoutManagerNew = new LinearLayoutManager(context);
        InterestAdapter adap=new InterestAdapter(this.storedInterests);
        BuildProfile.adapMap.put(cardName,adap);
        recyc.setLayoutManager(mLayoutManagerNew);
        recyc.setItemAnimator(new DefaultItemAnimator());
        recyc.setAdapter(adap);

        //need to do something about handleData!
        BuildProfile.cardNameToCard.put(this.cardName,this);
        handleCardData(recyc,adap,this.storedInterests,this.cardName,context,otherTypeOfContext);

        //attach recyc to rel
        rel.addView(recyc);
        //attach rel to card
        //Linear Layout to place the subject and list value in
        LinearLayout lin=new LinearLayout(context);
        lin.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
        lin.setOrientation(LinearLayout.VERTICAL);
        lin.addView(txt);
        lin.addView(checkBox);
        lin.addView(rel);

        c.addView(lin);
        c.setPadding(5,5,5,5);

        BuildProfile.cardMap.put(this.cardName,c);
        tr.addView(c);
        tr.setPadding(5,5,5,5);
        t.addView(tr);
    }


    //generates a new card with defaults
    public void handleCardData(final RecyclerView r, final InterestAdapter a, final List<Interest> l, final String cardName, final Context context, final Context otherTypeOfContext){

        //first enttry
        Interest currentInterest = new Interest("ADD ONE MORE",cardName);
        l.add(currentInterest);
        a.notifyDataSetChanged();

        r.addOnItemTouchListener(new RecyclerTouchListener(context, r, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Interest currentInterest = l.get(position);
                Log.d("CARD","YOU CLICKED ON POSN "+l.get(position).getName());
                List<Interest>currentList= Objects.requireNonNull(BuildProfile.cardNameToCard.get(cardName)).getStoredInterests();
                StringBuilder vals= new StringBuilder();
                assert currentList != null;
                for(Interest x:currentList){
                    vals.append(x.getName()).append(",");
                }
                //Toast.makeText(context,"Current list is"+vals,Toast.LENGTH_LONG).show();
                if(currentInterest.getName().equals("ADD ONE MORE")){
                    //making a new one:
                    //https://mkyong.com/android/android-prompt-user-input-dialog-example/
                    // get prompts.xml view
                    LayoutInflater li = LayoutInflater.from(otherTypeOfContext);
                    View promptsView = li.inflate(R.layout.prompts, null);
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                            new ContextThemeWrapper(otherTypeOfContext, R.style.myDialog)
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
                                            Interest m;
                                            if(userInput.getText().toString().equals("")||userInput.getText().toString()==null){
                                                m=new Interest("",cardName);
                                            }
                                            else{
                                                m=new Interest(userInput.getText().toString().toLowerCase(),cardName);
                                            }
                                            int pos= l.size();
                                            l.add(m);
                                            a.notifyItemInserted(pos);
                                            r.setAdapter(a);
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
            }

            @Override
            public void onLongClick(View view, int position) {
                Interest currentInterest = l.get(position);
                if(!currentInterest.getName().equals("ADD ONE MORE")){
                    l.remove(position);
                    a.notifyItemChanged(position);
                }
                if(l.isEmpty()){
                    l.add(new Interest("ADD ONE MORE",cardName));
                    a.notifyItemInserted(0);
                }
                r.setAdapter(a);
                //Toast.makeText(context, currentInterest.getName() + " is selected!", Toast.LENGTH_SHORT).show();
            }
        }));
    }

    public Map<String,Object> convertToMap(){
        Map<String,Object> m=new HashMap<>();
        m.put("userEmailId",this.userEmailId);
        if(!this.storedInterests.isEmpty())
            m.put("storedInterests",this.storedInterests);
        else
            m.put("storedInterests",new ArrayList<String>(Collections.singletonList("")));
        m.put("cardName",this.cardName);
        m.put("visible",this.visible);
        return m;
    }

    private String userEmailId;
    private String cardName;
    private List<Interest> storedInterests;
    private boolean visible;

    //display profile:

    //create a card with layout Data and data handlers
    public void addNewCardToView(TableLayout t, Context context,Context otherTypeOfContext){
        //physically producing a card
        //t is the layout we will put it inside
        t.setColumnStretchable(0,true);
        t.setColumnStretchable(1,true);
        TableRow tr= new TableRow(context);
        CardView c=new CardView(context);
        int idForThisRow= View.generateViewId();
        tr.setId(idForThisRow);
        //1st value to return
        //Main3Activity.cardNameToTableRow.put(this.cardName,tr);
        //random color:
        if(this.visible) {
            Random rnd = new Random();
            int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
            c.setCardBackgroundColor(color);
        }
        else {
            c.setCardBackgroundColor(0x7B000000);
        }
        c.layout(5,TableRow.LayoutParams.MATCH_PARENT,5,TableRow.LayoutParams.MATCH_PARENT);

        TextView txt=new TextView(context);
        txt.setText(this.cardName);
        txt.layout(TableRow.LayoutParams.MATCH_PARENT,20,TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.MATCH_PARENT);

        //after text, make checkbox
        CheckBox checkBox=new CheckBox(context);
        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,60);
        checkBox.setLayoutParams(parms);
        checkBox.setText("make Invisible");
        int generatedId=View.generateViewId();
        checkBox.setId(generatedId);
        checkBox.setChecked(!this.visible);
        //2nd return value
        //Main3Activity.cardCheckBoxMap.put(this.cardName,checkBox);
        //Toast.makeText(context,"CURRENT CHECKBOX IS "+generatedId+" CARDNAME:"+this.cardName,Toast.LENGTH_LONG).show();

        //now we go inside each card:
        RelativeLayout rel=new RelativeLayout(context);
        rel.layout(TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.MATCH_PARENT);

        RecyclerView recyc=new RecyclerView(context);
        recyc.layout(TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.MATCH_PARENT);
        recyc.setVerticalScrollBarEnabled(true);

        //manage this recycler
        RecyclerView.LayoutManager mLayoutManagerNew = new LinearLayoutManager(context);
        InterestAdapter adap=new InterestAdapter(this.storedInterests);
        //Main3Activity.adapMap.put(cardName,adap);
        recyc.setLayoutManager(mLayoutManagerNew);
        recyc.setItemAnimator(new DefaultItemAnimator());
        recyc.setAdapter(adap);

        //need to do something about handleData!
        //Main3Activity.cardNameToCard.put(this.cardName,this);
        //Main3Activity.listMap.put(cardName,this.storedInterests);
        //handleCardData(recyc,adap,this.storedInterests,this.cardName,context,otherTypeOfContext);

        //attach recyc to rel
        rel.addView(recyc);
        //attach rel to card
        //Linear Layout to place the subject and list value in
        LinearLayout lin=new LinearLayout(context);
        lin.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
        lin.setOrientation(LinearLayout.VERTICAL);
        lin.addView(txt);
        lin.addView(checkBox);
        lin.addView(rel);

        c.addView(lin);
        c.setPadding(5,5,5,5);

        //Main3Activity.cardMap.put(this.cardName,c);
        tr.addView(c);
        tr.setPadding(5,5,5,5);
        t.addView(tr);
    }

}
