package com.baatcheat;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.baatcheat.model.Interest;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InterestAdapter extends RecyclerView.Adapter<InterestAdapter.MyViewHolder> {

    private List<Interest> interestList;

    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView interest;
        MyViewHolder(View view) {
            super(view);
                interest = view.findViewById(R.id.interestName);
        }
    }


    public InterestAdapter(List<Interest> interestList) {
        this.interestList = interestList;
    }

    @NotNull
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.interest_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Interest i = interestList.get(position);
        holder.interest.setText(i.getName());
    }

    @Override
    public int getItemCount() {
        return interestList.size();
    }

}
