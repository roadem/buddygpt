package com.robotique.aevaweb.buddygpt.adapters;

import android.graphics.text.LineBreaker;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.robotique.aevaweb.buddygpt.R;
import com.robotique.aevaweb.buddygpt.application.BuddyGPTApplication;
import com.robotique.aevaweb.buddygpt.models.Replica;

public class ReplicaListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Replica[] mDataset;
    int item_send=1;
    int item_receive=2;
    int item_session=3;
    private BuddyGPTApplication buddyGPTApplication;

    public static class SentViewHolder extends RecyclerView.ViewHolder {

        private TextView sentmessage;
        public SentViewHolder(View itemView, BuddyGPTApplication buddyGPTApplication){
            super(itemView);
            sentmessage =itemView.findViewById(R.id.txt_sent_message);
            sentmessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, buddyGPTApplication.getTextSizeBullesPX());

        }
        public TextView getTextView() {
            return sentmessage;
        }
    }
    public static class ReceiveViewHolder extends RecyclerView.ViewHolder {

        private TextView receivemessage;
        private TextView messageDuration;
        private TextView messageConsommation;

        public ReceiveViewHolder(View itemView, BuddyGPTApplication buddyGPTApplication){
            super(itemView);
            receivemessage =itemView.findViewById(R.id.txt_receive_message);
            messageDuration = itemView.findViewById(R.id.txt_response_time);
            receivemessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, buddyGPTApplication.getTextSizeBullesPX());
            messageDuration.setTextSize(10);
            receivemessage.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD);
//            if(buddyGPTApplication.getParamFromFile("show_openAI_prices", "BuddyGPT.properties").trim().equalsIgnoreCase("yes")){
//                messageConsommation = itemView.findViewById(R.id.openai_price);
//                messageConsommation.setTextSize(TypedValue.COMPLEX_UNIT_PX,buddyGPTApplication.getTextSizeBullesPX());
//            }
        }

        public TextView getTextView() {
            return receivemessage;
        }
    }
    public static class SessionViewHolder extends RecyclerView.ViewHolder {

        private TextView txt_session;


        public SessionViewHolder(View itemView, BuddyGPTApplication buddyGPTApplication){
            super(itemView);
            txt_session =itemView.findViewById(R.id.txt_session);
            txt_session.setTextSize(TypedValue.COMPLEX_UNIT_PX, buddyGPTApplication.getTextSizeBullesPX());
//            if(buddyGPTApplication.getParamFromFile("show_openAI_prices", "BuddyGPT.properties").trim().equalsIgnoreCase("yes")){
//                messageConsommation = itemView.findViewById(R.id.openai_price);
//                messageConsommation.setTextSize(TypedValue.COMPLEX_UNIT_PX,buddyGPTApplication.getTextSizeBullesPX());
//            }
        }

        public TextView getTextView() {
            return txt_session;
        }
    }

    public ReplicaListAdapter(BuddyGPTApplication buddyGPTApplication, Replica[] dataSet) {
        mDataset=dataSet;
        this.buddyGPTApplication = buddyGPTApplication;
    }

    public void setData(Replica[] newdata){
        mDataset=newdata;
        notifyDataSetChanged();
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType==1){
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_send, parent, false);
            return new SentViewHolder(view,this.buddyGPTApplication);}
        else if (viewType==2){
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_receive, parent, false);
            return new ReceiveViewHolder(view,this.buddyGPTApplication);
        }else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_session, parent, false);
            return new SessionViewHolder(view,this.buddyGPTApplication);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder.getClass()==SentViewHolder.class){
            ((SentViewHolder) holder).sentmessage.setText(mDataset[position].getValue());

        }
        else if (holder.getClass()==ReceiveViewHolder.class){
            ((ReceiveViewHolder) holder).receivemessage.setText(mDataset[position].getValue().trim());
            ((ReceiveViewHolder) holder).messageDuration.setText("("+mDataset[position].getDuration()+")");
//            if(buddyGPTApplication.getParamFromFile("show_openAI_prices", "BuddyGPT.properties").trim().equalsIgnoreCase("yes")){
//                ((ReceiveViewHolder) holder).messageConsommation.setText(mDataset[position].getPrix());
//            }
        }
        else if (holder.getClass()==SessionViewHolder.class){

            ((SessionViewHolder) holder).txt_session.setText("_______________________"+buddyGPTApplication.getString(R.string.toast_teamgpt_session_en)+"_______________________");
            if (buddyGPTApplication.getLangue().getNom().equals("Anglais")) {
                ((SessionViewHolder) holder).txt_session.setText("_______________________"+buddyGPTApplication.getString(R.string.toast_teamgpt_session_en)+"_______________________");
            }
            else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                ((SessionViewHolder) holder).txt_session.setText("_______________________"+buddyGPTApplication.getString(R.string.toast_teamgpt_session_fr)+"_______________________");
            }
            else {
                buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                        .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_session_en))
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String translatedText) {
                                ((SessionViewHolder) holder).txt_session.setText("_______________________"+buddyGPTApplication.getString(R.string.toast_teamgpt_session_en)+"_______________________");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                ((SessionViewHolder) holder).txt_session.setText("_______________________"+buddyGPTApplication.getString(R.string.toast_teamgpt_session_en)+"_______________________");
                            }
                        });
            }
        }

    }

    @Override
    public int getItemViewType(int position) {

        if (mDataset[position].getType().equals("Question") ){
            return item_send;
        }
        if (mDataset[position].getType().equals("Response") ){
            return item_receive;
        }
        else {
            return item_session;
        }
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }

}
