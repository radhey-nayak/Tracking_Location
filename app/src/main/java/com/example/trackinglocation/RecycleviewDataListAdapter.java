package com.example.trackinglocation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;

import java.security.Key;
import java.util.ArrayList;

public class RecycleviewDataListAdapter extends RecyclerView.Adapter
        <RecycleviewDataListAdapter.DataHolder> {
    TrackingHistory trackingHistory;
    ArrayList<String> keyList;
    ArrayList<String> valueList;

    public RecycleviewDataListAdapter(TrackingHistory trackinghistory,
                                      ArrayList<String> keylist, ArrayList<String> valuelist) {
        trackingHistory = trackinghistory;
        keyList = keylist;
        valueList = valuelist;
    }

    @NonNull
    @Override
    public DataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(trackingHistory);
        View view = inflater.inflate(R.layout.design, parent, false);

        return new DataHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DataHolder holder, int position) {

        String key = keyList.get(position);
        String value = valueList.get(position);
        holder.itemName.setText(key);
        holder.listItem.setOnClickListener(view -> {

            Intent i1 = new Intent(trackingHistory, Map.class);
            i1.putExtra("ExtraIntentType", "History");
            //i1.putExtra("Key", key);
            i1.putExtra("value", value);
            trackingHistory.startActivity(i1);
        });

    }

    @Override
    public int getItemCount() {
        return keyList.size();
    }

    public static class DataHolder extends RecyclerView.ViewHolder {

        CardView listItem;
        TextView itemName;

        public DataHolder(@NonNull View itemView) {
            super(itemView);

            listItem = itemView.findViewById(R.id.list_item);
            itemName = itemView.findViewById(R.id.item_name);
        }
    }
}
