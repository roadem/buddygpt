package com.robotique.aevaweb.buddygpt.adapters;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.robotique.aevaweb.buddygpt.application.BuddyGPTApplication;
import com.robotique.aevaweb.buddygpt.models.Langue;

import java.util.List;

public class LangueSpinnerAdapter extends BaseAdapter {

    private final LayoutInflater layoutInflater;
    private List<Langue> langues;
    private final int listItemLayoutResource;
    private final int itemNameId;
    private final int itemCheckedId;
    private BuddyGPTApplication buddyGPTApplication;
    private String langueFr = "Français";
    private String langueEn = "Anglais";
    private String langueEs = "Espagnol";
    private String langueDe = "Allemand";
    String translatedLanguageName = "";

    public LangueSpinnerAdapter(Context context, int listItemLayoutResource, int itemNameId, int itemCheckedId, List<Langue> langues) {
        this.listItemLayoutResource = listItemLayoutResource;
        this.itemNameId = itemNameId;
        this.itemCheckedId = itemCheckedId;
        this.langues = langues;
        this.layoutInflater = LayoutInflater.from(context);
        buddyGPTApplication = (BuddyGPTApplication) context;
    }

    @Override
    public int getCount() {
        if (this.langues == null) {
            return 0;
        }
        return this.langues.size();
    }

    @Override
    public Object getItem(int position) {
        return this.langues.get(position);
    }

    @Override
    public long getItemId(int position) {
        Langue langue = (Langue) this.getItem(position);
        return langue.getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Langue langue = (Langue) getItem(position);
        View rowView = this.layoutInflater.inflate(this.listItemLayoutResource, null, true);
        TextView textViewItemName = (TextView) rowView.findViewById(this.itemNameId);
        String nameToDisplay=langue.getNom();
        if(buddyGPTApplication.getLangue().getNom().equals(langueEn)){
            if(nameToDisplay.equals(langueFr)) nameToDisplay = "French";
            if(nameToDisplay.equals(langueEn)) nameToDisplay = "English";
            if(nameToDisplay.equals(langueEs)) nameToDisplay = "Spanish";
            if(nameToDisplay.equals(langueDe)) nameToDisplay = "German";
            textViewItemName.setGravity(Gravity.CENTER_VERTICAL |Gravity.START) ;
            textViewItemName.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) buddyGPTApplication.getBestTextSize()-10);
            textViewItemName.setGravity(Gravity.CENTER_VERTICAL);
            textViewItemName.setText(nameToDisplay);
            ImageView checkedIcon = (ImageView) rowView.findViewById(itemCheckedId);
            checkedIcon.setVisibility(View.GONE);
            return rowView;
        }else if(buddyGPTApplication.getLangue().getNom().equals(langueEs)){
            if(nameToDisplay.equals(langueFr)) nameToDisplay = "Francés";
            if(nameToDisplay.equals(langueEn)) nameToDisplay = "Inglés";
            if(nameToDisplay.equals(langueEs)) nameToDisplay = "Español";
            if(nameToDisplay.equals(langueDe)) nameToDisplay = "Alemán";
            textViewItemName.setGravity(Gravity.CENTER_VERTICAL |Gravity.START) ;
            textViewItemName.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) buddyGPTApplication.getBestTextSize()-10);
            textViewItemName.setGravity(Gravity.CENTER_VERTICAL);
            textViewItemName.setText(nameToDisplay);
            ImageView checkedIcon = (ImageView) rowView.findViewById(itemCheckedId);
            checkedIcon.setVisibility(View.GONE);
            return rowView;
        }else if(buddyGPTApplication.getLangue().getNom().equals(langueDe)){
            if(nameToDisplay.equals(langueFr)) nameToDisplay = "Französisch";
            if(nameToDisplay.equals(langueEn)) nameToDisplay = "Englisch";
            if(nameToDisplay.equals(langueEs)) nameToDisplay = "Spanisch";
            if(nameToDisplay.equals(langueDe)) nameToDisplay = "Deutsch";
            textViewItemName.setGravity(Gravity.CENTER_VERTICAL |Gravity.START) ;
            textViewItemName.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)buddyGPTApplication.getBestTextSize()-10);
            textViewItemName.setGravity(Gravity.CENTER_VERTICAL);
            textViewItemName.setText(nameToDisplay);
            ImageView checkedIcon = (ImageView) rowView.findViewById(itemCheckedId);
            checkedIcon.setVisibility(View.GONE);
            return rowView;
        }else if(buddyGPTApplication.getLangue().getNom().equals(langueFr)){
            if(nameToDisplay.equals(langueFr)) nameToDisplay = "Français";
            if(nameToDisplay.equals(langueEn)) nameToDisplay = "Anglais";
            if(nameToDisplay.equals(langueEs)) nameToDisplay = "Espagnol";
            if(nameToDisplay.equals(langueDe)) nameToDisplay = "Allemand";
            textViewItemName.setGravity(Gravity.CENTER_VERTICAL |Gravity.START) ;
            textViewItemName.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)buddyGPTApplication.getBestTextSize()-10);
            textViewItemName.setGravity(Gravity.CENTER_VERTICAL);
            textViewItemName.setText(nameToDisplay);
            ImageView checkedIcon = (ImageView) rowView.findViewById(itemCheckedId);
            checkedIcon.setVisibility(View.GONE);
            return rowView;
        }else {
            Log.e("MRA","nameToDisplay else"+nameToDisplay);
            buddyGPTApplication.getFrenchLanguageSelectedTranslator().translate(nameToDisplay)
                    .addOnSuccessListener(translatedText -> {
                        Log.e("MRA","nameToDisplay else translatedText"+translatedText);
                        translatedLanguageName = translatedText;
                        updateView(rowView,textViewItemName);
                    })
                    .addOnFailureListener(e -> Log.e("MRA", "translatedText exception  " + e));

        }
        return rowView;
    }
    private void updateView(View rowView,TextView textViewItemName) {
        textViewItemName.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        textViewItemName.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)buddyGPTApplication.getBestTextSize() - 10);
        textViewItemName.setGravity(Gravity.CENTER_VERTICAL);
        textViewItemName.setText(translatedLanguageName);
        ImageView checkedIcon = (ImageView) rowView.findViewById(itemCheckedId);
        checkedIcon.setVisibility(View.GONE);
    }
    public void updateDropDownView(View rowView,TextView textViewItemName,Langue langue){
        textViewItemName.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)buddyGPTApplication.getBestTextSize()-10);
        textViewItemName.setText(translatedLanguageName);
        ImageView checkedIcon = (ImageView) rowView.findViewById(itemCheckedId);
        if (langue.isChosen()) {
            checkedIcon.setVisibility(View.VISIBLE);
        } else {
            checkedIcon.setVisibility(View.INVISIBLE);
        }
    }
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        Langue langue = (Langue) getItem(position);
        View rowView = this.layoutInflater.inflate(this.listItemLayoutResource, null, true);
        TextView textViewItemName = (TextView) rowView.findViewById(this.itemNameId);
        String nameToDisplay=langue.getNom();
        buddyGPTApplication.getFrenchLanguageSelectedTranslator().translate(nameToDisplay)
                .addOnSuccessListener(translatedText -> {
                    Log.e("MRA","nameToDisplay else translatedText"+translatedText);
                    translatedLanguageName = translatedText;
                    updateDropDownView(rowView,textViewItemName,langue);
                })
                .addOnFailureListener(e -> Log.e("MRA", "translatedText exception  " + e));

        return rowView;
    }

    public void updateDataSet(List<Langue> data) {
        this.langues = data;
        notifyDataSetChanged();
    }
}
