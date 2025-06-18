package com.robotique.aevaweb.buddygpt.adapters;

import android.graphics.Color;
import android.graphics.text.LineBreaker;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.robotique.aevaweb.buddygpt.R;
import com.robotique.aevaweb.buddygpt.application.BuddyGPTApplication;
import com.robotique.aevaweb.buddygpt.models.Replica;

public class ReplicaListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Replica[] mDataset;
    int itemSend = 1;
    int itemReceive = 2;
    int itemSession = 3;
    private final BuddyGPTApplication buddyGPTApplication;

    public ReplicaListAdapter(BuddyGPTApplication buddyGPTApplication, Replica[] dataSet) {
        mDataset = dataSet;
        this.buddyGPTApplication = buddyGPTApplication;
    }

    public void setData(Replica[] newdata) {
        mDataset = newdata;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 1) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_send, parent, false);
            return new SentViewHolder(view, this.buddyGPTApplication);
        } else if (viewType == 2) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_receive, parent, false);
            return new ReceiveViewHolder(view, this.buddyGPTApplication);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_session, parent, false);
            return new SessionViewHolder(view, this.buddyGPTApplication);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder.getClass() == SentViewHolder.class) {
            ((SentViewHolder) holder).sentmessage.setText(mDataset[position].getValue());

        } else if (holder.getClass() == ReceiveViewHolder.class) {

            String message = mDataset[position].getValue().trim();
            String duration = "(" + mDataset[position].getDuration() + ")";

            // Fusionner les deux textes avec un format HTML
            SpannableString spannable = new SpannableString(message + " " + duration);
            spannable.setSpan(new ForegroundColorSpan(Color.BLACK), message.length() + 1, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new RelativeSizeSpan(0.7f), message.length() + 1, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ((ReceiveViewHolder) holder).receivemessage.setText(spannable);
        } else if (holder.getClass() == SessionViewHolder.class) {

            if (buddyGPTApplication.getLangue().getNom().equals("Anglais")) {
                ((SessionViewHolder) holder).txtSession.setText("_______________________" + buddyGPTApplication.getString(R.string.toast_teamgpt_session_en) + "_______________________");
            } else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                ((SessionViewHolder) holder).txtSession.setText("_______________________" + buddyGPTApplication.getString(R.string.toast_teamgpt_session_fr) + "_______________________");
            } else {
                buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                        .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_session_en))
                        .addOnSuccessListener(translatedText -> ((SessionViewHolder) holder).txtSession.setText("_______________________" + translatedText + "_______________________"))
                        .addOnFailureListener(e -> ((SessionViewHolder) holder).txtSession.setText("_______________________" + buddyGPTApplication.getString(R.string.toast_teamgpt_session_en) + "_______________________"));
            }
        }

    }

    @Override
    public int getItemViewType(int position) {

        if (mDataset[position].getType().equals("Question")) {
            return itemSend;
        }
        if (mDataset[position].getType().equals("Response")) {
            return itemReceive;
        } else {
            return itemSession;
        }
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }

    public static class SentViewHolder extends RecyclerView.ViewHolder {

        private final TextView sentmessage;

        public SentViewHolder(View itemView, BuddyGPTApplication buddyGPTApplication) {
            super(itemView);
            sentmessage = itemView.findViewById(R.id.txt_sent_message);
            sentmessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, buddyGPTApplication.getTextSizeBullesPX());

        }

        public TextView getTextView() {
            return sentmessage;
        }
    }

    public static class ReceiveViewHolder extends RecyclerView.ViewHolder {

        private final TextView receivemessage;

        public ReceiveViewHolder(View itemView, BuddyGPTApplication buddyGPTApplication) {
            super(itemView);
            receivemessage = itemView.findViewById(R.id.txt_receive_message);
            receivemessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, buddyGPTApplication.getTextSizeBullesPX());
            receivemessage.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD);
        }

        public TextView getTextView() {
            return receivemessage;
        }
    }

    public static class SessionViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtSession;


        public SessionViewHolder(View itemView, BuddyGPTApplication buddyGPTApplication) {
            super(itemView);
            txtSession = itemView.findViewById(R.id.txt_session);
            txtSession.setTextSize(TypedValue.COMPLEX_UNIT_PX, buddyGPTApplication.getTextSizeBullesPX());

        }

        public TextView getTextView() {
            return txtSession;
        }
    }

}
