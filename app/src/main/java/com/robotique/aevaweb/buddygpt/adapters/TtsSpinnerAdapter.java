package com.robotique.aevaweb.buddygpt.adapters;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import com.robotique.aevaweb.buddygpt.models.TtsModel;

import java.util.List;

public class TtsSpinnerAdapter extends BaseAdapter{


    private final LayoutInflater layoutInflater;
    private List<TtsModel> ttss;
    private final int listItemLayoutResource;
    private final int itemNameId;
    private final int itemCheckedId;

    public TtsSpinnerAdapter(Context context, int listItemLayoutResource, int itemNameId, int itemCheckedId, List<TtsModel> ttss) {
        this.listItemLayoutResource = listItemLayoutResource;
        this.itemNameId = itemNameId;
        this.itemCheckedId = itemCheckedId;
        this.ttss = ttss;
        this.layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        if (this.ttss == null) {
            return 0;
        }
        return this.ttss.size();
    }

    @Override
    public Object getItem(int position) {
        return this.ttss.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TtsModel graphe = (TtsModel) getItem(position);
        View rowView = this.layoutInflater.inflate(this.listItemLayoutResource, null, true);
        TextView textViewItemName = (TextView) rowView.findViewById(this.itemNameId);
        String nameToDisplay=graphe.getNom();

        textViewItemName.setGravity(Gravity.CENTER_HORIZONTAL);
        textViewItemName.setText(nameToDisplay);
        ImageView checkedIcon = (ImageView) rowView.findViewById(itemCheckedId);
        checkedIcon.setVisibility(View.GONE);
        return rowView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TtsModel graphe = (TtsModel) getItem(position);
        View rowView = this.layoutInflater.inflate(this.listItemLayoutResource, null, true);
        TextView textViewItemName = (TextView) rowView.findViewById(this.itemNameId);
        textViewItemName.setText(graphe.getNom());
        ImageView checkedIcon = (ImageView) rowView.findViewById(itemCheckedId);
        if (graphe.isChosen()) {
            checkedIcon.setVisibility(View.VISIBLE);
        } else {
            checkedIcon.setVisibility(View.INVISIBLE);
        }
        return rowView;
    }
    public void updateDataSet(List<TtsModel> data) {
        this.ttss = data;
        notifyDataSetChanged();
    }
}


