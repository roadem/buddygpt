package com.robotique.aevaweb.buddygpt.activities;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.GazePosition;
import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddy.utils.events.EventItem;
import com.bfr.buddy.utils.values.FloatingWidgetVisibility;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;
import com.google.android.exoplayer2.C;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.robotique.aevaweb.buddygpt.R;
import com.robotique.aevaweb.buddygpt.application.BuddyGPTApplication;
import com.robotique.aevaweb.buddygpt.chatbotresponse.ResponseFromTeamGPT;
import com.robotique.aevaweb.buddygpt.models.Langue;
import com.robotique.aevaweb.buddygpt.models.Replica;
import com.robotique.aevaweb.buddygpt.models.Session;
import com.robotique.aevaweb.buddygpt.models.Setting;
import com.robotique.aevaweb.buddygpt.utilis.MailSender;
import com.robotique.aevaweb.buddygpt.adapters.ReplicaListAdapter;
import com.robotique.aevaweb.buddygpt.observers.IDBObserver;
import com.robotique.aevaweb.buddygpt.utilis.ITTSCallbacks;
import com.robotique.aevaweb.buddygpt.utilis.OnMailReadyListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class ChatWindow extends BuddyActivity implements IDBObserver {
    private static final String TAG = "BuddyGPT_ChatWindow";

    private BuddyGPTApplication buddyGPTApplication;
    private View decorView;

    private boolean onSdkReadyIsAlreadyCalledOnce = false;
    private boolean isListeningFreeSpeech = false;
    private boolean isWaitingForResponse = false;


    private String fullResponse="";
    private Setting settingClass;
    private ArrayList<Replica> listRep=new ArrayList();
    private ArrayList<Replica> listRepGlobale=new ArrayList();
    private ReplicaListAdapter adapter;

    //timers
    private CountDownTimer timerEcoute;
    private CountDownTimer responseTimeout;

    //views
    private RelativeLayout popupAddMail;
    private LinearLayout popupAddMailContent;
    private RelativeLayout parent_chat;
    private ImageView micro_btn;
    private RecyclerView recyclerView;
    private ScrollView scrollView;
    private TextView textEmail;
    private EditText editTextEmail;
    int click=1;
    boolean startlisten=true;
    MailSender smtpService;
    private ResponseFromTeamGPT responseFromTeamGPT;
    private String langueFr = "Français";
    private String langueEn = "Anglais";
    private String langueEs = "Espagnol";
    private String langueDe = "Allemand";
    private Handler handlerTTSError = new Handler();
    private Runnable runnableTTSError;
    private String configFile ="BuddyGPT.properties";
    private String header ="header";
    private String entete ="entete";
    private String cabecera ="Cabecera";
    private String kopfzeile ="Kopfzeile";
    private String openAIKey = "openAI_API_Key";
    private String addDestinationMail="";
    private String addDestinationMailEditText="";
    private boolean isClickedBtnCloseChat=false;
    String[] newSessionText = new String[1];
    String[] responseText = new String[1];
    String[] qstText = new String[1];
    private Handler handlerPauseTime = new Handler();
    private Runnable runnablePauseTime;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_window);

        Log.d(TAG," --- onCreate() ---");

        buddyGPTApplication = (BuddyGPTApplication) getApplicationContext();
        buddyGPTApplication.setInitSharedpreferences(false);
        buddyGPTApplication.hideSystemUI(this);
        decorView=getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if(visibility==0){
                    decorView.setSystemUiVisibility(buddyGPTApplication.hideSystemUI(ChatWindow.this));
                }
            }
        });
        if(responseFromTeamGPT != null){
            responseFromTeamGPT.reset();
        }
        responseFromTeamGPT=new ResponseFromTeamGPT(buddyGPTApplication);
        //init views
        popupAddMail = findViewById(R.id.popup_add_mail);
        parent_chat = findViewById( R.id.parent_chat );
        micro_btn = findViewById( R.id.micro_btn );
        scrollView=findViewById(R.id.scrollview);
        recyclerView=findViewById(R.id.chatRecyclerView);
        editTextEmail = findViewById(R.id.editTextEmail);
        textEmail = findViewById(R.id.popup_add_mail_textView);
        popupAddMailContent = findViewById(R.id.popup_add_mail_linearLayout);
        setAddMailDestinationText();
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG," --- onResume() ---");
        buddyGPTApplication.registerObserver(this);
        buddyGPTApplication.hideSystemUI(this);
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (responseTimeout!=null) responseTimeout.cancel();
//        if(buddyGPTApplication.getChatGptStreamMode() != null){
//            buddyGPTApplication.getChatGptStreamMode().reset();
//        }
//        if(buddyGPTApplication.getCustomGPTStreamMode() != null){
//            buddyGPTApplication.getCustomGPTStreamMode().reset();
//        }
        if(handlerTTSError!=null && runnableTTSError!=null){
            handlerTTSError.removeCallbacks(runnableTTSError);
            handlerTTSError.removeCallbacksAndMessages(null);
        }
        onSdkReadyIsAlreadyCalledOnce = false;
        isWaitingForResponse = false;
        startlisten=true;
        listRep=new ArrayList();
        //listRepGlobale=new ArrayList();
        buddyGPTApplication.stopTTS();
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
        }
        catch (Exception e){
            Log.e(TAG,"BuddySDK Exception  "+e);
        }
        stopListeningFreeSpeech();
        buddyGPTApplication.removeObserver(this);
    }
    @Override
    protected void onDestroy() {

        if (!buddyGPTApplication.getInitSharedpreferences()){
            buddyGPTApplication.setparam("firstLaunch","true");
            buddyGPTApplication.notifyObservers("ChatDestroy");

        }

            if(buddyGPTApplication.getDialog() != null && buddyGPTApplication.getDialog().isShowing()) buddyGPTApplication.getDialog().dismiss();

        Log.d(TAG," --- onDestroy() ---");
        super.onDestroy();
    }
    /**
     * ------------------ Register to the SDK callbacks ---------------------
     */

    @Override
    public void onSDKReady() {
        Log.w(TAG, "onSDKReady");
        if(!onSdkReadyIsAlreadyCalledOnce){
            //initialisation du visage de Buddy
            BuddySDK.UI.setFaceEnergy(1.0f);
            BuddySDK.UI.setFacePositivity(1.0f);
            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
            BuddySDK.UI.lookAt(GazePosition.CENTER, true);
            BuddySDK.UI.stopListenAnimation();
            BuddySDK.UI.setViewAsFace(parent_chat);
            BuddySDK.UI.setMenuWidgetVisibility(FloatingWidgetVisibility.ALWAYS);
            BuddySDK.UI.setCloseWidgetVisibility(FloatingWidgetVisibility.ALWAYS);

            //buddyGPTApplication.setTTSLanguage();

            init();
        }
        onSdkReadyIsAlreadyCalledOnce = true;
    }

    @Override
    public void onEvent(EventItem iEvent) {
        Log.w(TAG, "onEvent : "+iEvent.toString());
    }
    /**
     * Initialisations
     */
    private void init(){

        click=1;


        settingClass = new Setting();
        settingClass.setDuration(buddyGPTApplication.getparam("listening_duration"));
        settingClass.setAttempt(buddyGPTApplication.getparam("listening_attempt"));
        settingClass.setLangue(buddyGPTApplication.getLangue().getNom());
        settingClass.setVolume(buddyGPTApplication.getparam("speak_volume"));
        settingClass.setSwitchVisibility(buddyGPTApplication.getparam("switch_visibility"));
        refreshSTTLangue();

        popupAddMail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Vérifier si le popup_add_mail est visible et si le clic est en dehors de celui-ci
                if (popupAddMail.getVisibility() == View.VISIBLE) {
                    MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);
                    if (!isViewInsideBounds(popupAddMailContent, (int) event.getRawX(), (int) event.getRawY())) {
                        // Si le clic est en dehors, rendre le popup invisible
                        popupAddMail.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
        popupAddMailContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ne rien faire pour empêcher la propagation du clic aux éléments enfants du popup
            }
        });

        adapter = new ReplicaListAdapter(buddyGPTApplication,initDataset());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        editTextEmail.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);


        editTextEmail.setText(buddyGPTApplication.getparam("Mail_Destination"));


        editTextEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Method left empty intentionally because no specific action is needed for this update.
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {


                buddyGPTApplication.setparam("Mail_Destination",charSequence.toString());
            }
            @Override
            public void afterTextChanged(Editable editable) {
                // Method left empty intentionally because no specific action is needed for this update.
            }
        });

        editTextEmail.setOnFocusChangeListener((v,hasFocus) -> {
            if (hasFocus) {
                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
                // color black + opacity 50%
            } else {
                buddyGPTApplication.hideSystemUI(ChatWindow.this);
                if(editTextEmail.getText().toString().trim().isEmpty()){
                    buddyGPTApplication.setparam("Mail_Destination",buddyGPTApplication.getparam("Email"));
                    editTextEmail.setText(buddyGPTApplication.getparam("Mail_Destination"));
                }
            }
        });

        scroll();

    }
    // Vérifie si les coordonnées de l'événement sont à l'intérieur de la vue spécifiée
    private boolean isViewInsideBounds(View view, int x, int y) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];
        return !(x < viewX || x > viewX + view.getWidth() || y < viewY || y > viewY + view.getHeight());
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }
    private void refreshSTTLangue() {
        buddyGPTApplication.refresh(new Gson().fromJson(buddyGPTApplication.getparam(settingClass.getLangue()), Langue.class).getLanguageCode(),this);
    }
    /**
     * Récupération des questions/réponses
     */
    private Replica[] initDataset() {
        // Initialiser la liste des répliques


        // Charger les données JSON depuis les ressources
        String jsonString = buddyGPTApplication.getparam("messages");

        Log.i(TAG, "initDataset: messages "+jsonString);
        try {
            // Analyser le JSON
            JSONArray messagesArray = new JSONArray(jsonString);

            for (int i = 0; i < messagesArray.length(); i++) {
                JSONObject messageObject = messagesArray.getJSONObject(i);

                Replica replica = new Replica();
                if (messageObject.has("Question") ) {
                    replica.setType("Question");
                    replica.setValue(messageObject.getString("Question"));
                }
                if (messageObject.has("Response") ) {
                    replica.setType("Response");
                    replica.setValue(messageObject.getString("Response").split(";SPLIT;")[0]);
                    replica.setDuration(messageObject.getString("Response").split(";SPLIT;")[1]);
                }
                if (messageObject.has("Session") ){
                    replica.setType("Session");
                    replica.setValue(messageObject.getString("Session"));
                }
                    // Ajouter les questions et réponses comme des objets Replica
                listRepGlobale.add(replica);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            // Gérer les erreurs de parsing JSON
        }

        // Convertir la liste en tableau
        Replica[] mDataset = new Replica[listRepGlobale.size()];
        mDataset = listRepGlobale.toArray(mDataset);

        // Retourner le dataset
        return mDataset;
    }


    /**
     * Mettre à jour la liste des messages
     */
    private void updateChat() {
        Replica[] mDataset = new Replica[listRepGlobale.size()];
        for (int i = 0; i < listRepGlobale.size(); i++) {
            mDataset[i] = listRepGlobale.get(i);
        }
        adapter.setData(mDataset); // Update adapter data
        adapter.notifyDataSetChanged(); // Ensure the changes are reflected immediately
        scroll(); // Scroll to the latest message
    }

    /**
     * Gestion du scroll automatique
     */
    private void scroll(){
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                recyclerView.smoothScrollToPosition(adapter.getItemCount()+5);
            }
        });
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    /**
     * Gestion du clic sur l'icone Micro
     */
    public void onClickMicro(View view) {
        if (responseTimeout!=null) responseTimeout.cancel();
        if(buddyGPTApplication.getResponseFromTeamGPT()!=null)
            buddyGPTApplication.getResponseFromTeamGPT().reset();

        if(handlerTTSError!=null && runnableTTSError!=null){
            handlerTTSError.removeCallbacks(runnableTTSError);
            handlerTTSError.removeCallbacksAndMessages(null);
        }
        if(buddyGPTApplication.getparam("INVALID_TEAMGPT_DEVICE_ID").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")){
            Log.i("TAG", "run: notifyObservers INVALID_TEAMGPT_DEVICE_ID 3");
            buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_DEVICE_ID");
        }
        else if(buddyGPTApplication.getparam("INVALID_TEAMGPT_KEY").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")){
            Log.i("TAG", "run: notifyObservers INVALID_TEAMGPT_KEY 3");
            buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
        }
        else if(buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")){
            Log.i("TAG", "run: notifyObservers TEAMGPT_KEY EMPTY 3");
            if (buddyGPTApplication.getLangue().getNom().equals("Anglais")){
                buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_en));
            }
            else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_fr));
            }
            else if (buddyGPTApplication.getLangue().getNom().equals("Espagnol")) {
                buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_es));
            }
            else if (buddyGPTApplication.getLangue().getNom().equals("Allemand")){
                buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_de));
            }
            else{
                buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                        .translate(getString(R.string.toast_teamgpt_key_indispo_en))
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String translatedText) {
                                buddyGPTApplication.showToast(translatedText);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_en));
                            }
                        });
            }
        } else{
            if (click==1){
                click=2;
                if(!isListeningFreeSpeech && !isWaitingForResponse){
                    buddyGPTApplication.stopTTS();
                    startlisten=true;
                    startListeningFreeSpeech(buddyGPTApplication.getListeningDuration());
                }

            }
            else if (click==2){
                Log.e("MEHDII","buddyGPTApplication.getAppIsListeningToTheQuestion()----------------------"+ buddyGPTApplication.getAppIsListeningToTheQuestion());
                if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Android")
                        || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Cerence")
                        || !buddyGPTApplication.getAppIsListeningToTheQuestion() ) {
                    click = 1;
                    isListeningFreeSpeech = false;
                    isWaitingForResponse = false;
                    startlisten = false;
                    buddyGPTApplication.stopTTS();
                    buddyGPTApplication.setStoredResponse("");
                    try {
                        BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                    } catch (Exception e) {
                        Log.e(TAG, "BuddySDK Exception  " + e);
                    }
                    stopListeningFreeSpeech();
                }
                else {
                    buddyGPTApplication.setLed("neutral");
                    micro_btn.setImageResource(R.drawable.micro_off);
                    buddyGPTApplication.setAppIsListeningToTheQuestion(false);
                    Log.e("MEHDII","buddyGPTApplication.traitementAudio---------------------------------------");
                    buddyGPTApplication.traitementAudio(false);
                }

            }
        }


    }
    private void setAddMailDestinationText(){
        if (buddyGPTApplication.getLangue().getNom().equals(langueEn)) {
            textEmail.setText(buddyGPTApplication.getString(R.string.destination_mail_texte));
            editTextEmail.setHint(buddyGPTApplication.getString(R.string.destination_mail_Edittexte));
        }
        else if (buddyGPTApplication.getLangue().getNom().equals(langueFr)){
            textEmail.setText(buddyGPTApplication.getString(R.string.destination_mail_texte_fr));
            editTextEmail.setHint(buddyGPTApplication.getString(R.string.destination_mail_Edittexte_fr));
        }else {
            buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getString(R.string.destination_mail_texte)).addOnSuccessListener(new OnSuccessListener<String>() {
                @Override
                public void onSuccess(String translatedText) {

                    textEmail.setText(translatedText);
                }

            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG,"translatedText exception  "+e);
                    textEmail.setText(buddyGPTApplication.getString(R.string.destination_mail_texte));
                }
            });

            buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getString(R.string.destination_mail_Edittexte)).addOnSuccessListener(new OnSuccessListener<String>() {
                @Override
                public void onSuccess(String translatedText) {
                    editTextEmail.setHint(translatedText);

                }

            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG,"translatedText exception  "+e);
                    editTextEmail.setHint(buddyGPTApplication.getString(R.string.destination_mail_Edittexte));
                }
            });
        }
    }
    /**
     * Gestion du clic sur l'icone ClearCoonversation
     */
    public void onClickClearConversation(View view){
        buddyGPTApplication.listSessionClear();
        buddyGPTApplication.setparam("messages","[]");
        listRep.clear();
        listRepGlobale.clear();
        adapter = new ReplicaListAdapter(buddyGPTApplication,initDataset());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);



    }
    /**
     * Gestion du clic sur l'icone Send
     */
    public void onClickSend(View view){
        if(listRepGlobale.isEmpty()){
            if (buddyGPTApplication.getLangue().getNom().equals("Anglais")){
                buddyGPTApplication.showToast(getString(R.string.no_message_to_send_en));
            }
            else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                buddyGPTApplication.showToast(getString(R.string.no_message_to_send_fr));
            }
            else{
                buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                        .translate(getString(R.string.no_message_to_send_en))
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String translatedText) {
                                buddyGPTApplication.showToast(translatedText);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                buddyGPTApplication.showToast(getString(R.string.no_message_to_send_en));
                            }
                        });
            }
        }
        else
            popupAddMail.setVisibility(View.VISIBLE);

    }
    /**
     * Gestion du clic sur l'icone Send depuis le popUP
     */
    public void onClickSendFromPopup(View view){

            if (!buddyGPTApplication.getparam("Mail_Destination").trim().isEmpty()){
                if(buddyGPTApplication.getLangue().getNom().equals(langueEn)) {
                    writeMail(new OnMailReadyListener() {
                        @Override
                        public void onMailReady(String emailContent) {
                            smtpService = new MailSender(ChatWindow.this,emailContent, buddyGPTApplication.getparam("Mail_Destination"), buddyGPTApplication.getparam("Mail_Subject_en"));
                            smtpService.execute();
                        }
                    });

                }
                else if(buddyGPTApplication.getLangue().getNom().equals(langueFr)){
                    writeMail(new OnMailReadyListener() {
                        @Override
                        public void onMailReady(String emailContent) {

                    smtpService = new MailSender(ChatWindow.this,emailContent, buddyGPTApplication.getparam("Mail_Destination"), buddyGPTApplication.getparam("Mail_Subject_fr"));
                    smtpService.execute();


                    }
                });

            }else{
                    final Activity activity = ChatWindow.this;
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getparam("Mail_Subject_en")).addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String translatedText) {
                            writeMail(new OnMailReadyListener() {
                                @Override
                                public void onMailReady(String emailContent) {
                                    smtpService = new MailSender(activity,emailContent, buddyGPTApplication.getparam("Mail_Destination"), translatedText);
                                    smtpService.execute();
                                }
                            });

                        }

                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG,"translatedText exception  "+e);
                        }
                    });
                }
                popupAddMail.setVisibility(View.INVISIBLE);

            }else {
                if(buddyGPTApplication.getLangue().getNom().equals(langueEn)) {
                    Toast.makeText(buddyGPTApplication, buddyGPTApplication.getString(R.string.add_mail_toast_en),Toast.LENGTH_LONG).show();
                }
                else if(buddyGPTApplication.getLangue().getNom().equals(langueFr)){
                    Toast.makeText(buddyGPTApplication, buddyGPTApplication.getString(R.string.add_mail_toast_fr),Toast.LENGTH_LONG).show();
                }
                else if(buddyGPTApplication.getLangue().getNom().equals(langueEs)){
                    Toast.makeText(buddyGPTApplication, buddyGPTApplication.getString(R.string.add_mail_toast_es),Toast.LENGTH_LONG).show();
                }
                else if(buddyGPTApplication.getLangue().getNom().equals(langueDe)){
                    Toast.makeText(buddyGPTApplication, buddyGPTApplication.getString(R.string.add_mail_toast_de),Toast.LENGTH_LONG).show();
                }
                else{
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getString(R.string.add_mail_toast_en)).addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String translatedText) {
                            Toast.makeText(buddyGPTApplication, translatedText, Toast.LENGTH_LONG).show();
                        }

                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG,"translatedText exception  "+e);
                        }
                    });
                }
            }

    }
    public void writeMail(OnMailReadyListener listener) {
        String langue = buddyGPTApplication.getLangue().getNom();

        if (langue.equals("Anglais")) {
            listener.onMailReady(buildEmailContent(
                    new String[]{"_____________________ New Session _____________________"},
                    new String[]{"Response"},
                    new String[]{"Question"}
            ));
        } else if (langue.equals("Français")) {
            listener.onMailReady(buildEmailContent(
                    new String[]{"_____________________ Nouvelle Session _____________________"},
                    new String[]{"Réponse"},
                    new String[]{"Question"}
            ));
        } else {
            translateTexts(listener);
        }
    }

    private void translateTexts(OnMailReadyListener listener) {


        buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                .translate("New Session")
                .addOnSuccessListener(translatedText -> {
                    newSessionText[0] = "_____________________ " + translatedText + " _____________________";
                    translateNext("Response", responseText, () ->
                            translateNext("Question", qstText, () ->
                                    listener.onMailReady(buildEmailContent(new String[]{newSessionText[0]}, new String[]{responseText[0]}, new String[]{qstText[0]}))
                            )
                    );
                })
                .addOnFailureListener(e -> handleTranslationFailure(listener, newSessionText, responseText, qstText));
    }

    private void translateNext(String text, String[] output, Runnable onSuccess) {
        buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                .translate(text)
                .addOnSuccessListener(translatedText -> {
                    output[0] = translatedText;
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Échec de la traduction de '" + text + "'", e);
                    output[0] = text;
                    onSuccess.run();
                });
    }

    private void handleTranslationFailure(OnMailReadyListener listener, String[] newSessionText, String[] responseText, String[] qstText) {
        Log.e(TAG, "Échec de la traduction de 'New Session'");
        newSessionText[0] = "_____________________ New Session _____________________";
        translateNext("Response", responseText, () ->
                translateNext("Question", qstText, () ->
                        listener.onMailReady(buildEmailContent(new String[]{newSessionText[0]}, new String[]{responseText[0]}, new String[]{qstText[0]}))
                )
        );
    }

    private String buildEmailContent(String[] newSessionText, String[] responseText, String[] qstText) {
        String firstLine;
        Log.i(TAG, "buildEmailContent: "+newSessionText[0]+"******"+responseText[0]+"******"+qstText[0]+"******");
        if (!buddyGPTApplication.getparam("SelectedChatbot").equalsIgnoreCase("") ||
                !buddyGPTApplication.getparam("NomCompte").equalsIgnoreCase("")) {
            firstLine = buddyGPTApplication.getparam("NomCompte") + " " +
                    buddyGPTApplication.getparam("SelectedChatbot") + " " +
                    buddyGPTApplication.getModel() + "<br>";
        } else {
            firstLine = "_<br>";
        }

        StringBuilder question = new StringBuilder(firstLine);
        for (Replica replica : listRepGlobale) {
            if (replica.getType().equalsIgnoreCase("Session")) {
                question.append("<br>").append(newSessionText[0])
                        .append("<br>").append(replica.getValue());
            } else if (replica.getType().equalsIgnoreCase("Response")) {
                question.append("<br>").append(responseText[0])  // Correction ici
                        .append(" : ").append(replica.getValue().split(";SPLIT;")[0])
                        .append(" (").append(replica.getDuration()).append(")");
            } else if (replica.getType().equalsIgnoreCase("Question")) {
                question.append("<br>").append(qstText[0])  // Correction ici
                        .append(" : ").append(replica.getValue());

            } else {
                question.append("<br>").append(replica.getType()).append(" : ").append(replica.getValue());
            }
        }
        Log.i(TAG, "buildEmailContent: test"+question);
        return question.toString();
    }


    /**
     * Fermeture de la fenetre de discussion
     */
    public void btnCloseChat(View view) {
        isClickedBtnCloseChat=true;
        buddyGPTApplication.stopTTS();

        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
        }
        catch (Exception e){
            Log.e(TAG,"BuddySDK Exception  "+e);
        }
        //stopListeningFreeSpeech();

        Intent intent = new Intent(ChatWindow.this,MainActivity.class);
        intent.putExtra("fromChatWindow", "true");
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }

    /**
     * ------------------------------------------ Gestion de notifications --------------------------
     */

    @Override
    public void update(String message) throws IOException {
        if(message != null){

            if (message.contains("CANCEL_RESPONSE_TIMEOUT")) {
                if (responseTimeout!=null) responseTimeout.cancel();
            }

            if (message.contains("MODE_STREAM_SPEAK;SPLIT;")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (message.split(";SPLIT;").length > 1){
                            String phraseToPronounce = message.split(";SPLIT;")[1];
                            speak(phraseToPronounce, "nothealysa");
                        }
                    }
                });
            }

            if(message.contains("STTQuestion_success")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        buddyGPTApplication.setAppIsListeningToTheQuestion(false);
                        isWaitingForResponse = true;

                        stopListeningFreeSpeech();

                        String detectedSTTMessage = message.split(";")[1].replaceAll("' ","'");


                        buddyGPTApplication.setQuestionNumber(buddyGPTApplication.getCurrentQuestionNubmer()+1);
                        //buddyGPTApplication.setQuestionTime(System.currentTimeMillis());
                        String time =new SimpleDateFormat("HH:mm:ss").format(new Date());
                        Replica question=new Replica();
                        question.setType("Question");
                        question.setTime(time);
                        question.setValue(detectedSTTMessage);
                        listRep.add(question);
                        listRepGlobale.add(question);
                        updateChat();
                        buddyGPTApplication.setActivityClosed(false);

                        if(buddyGPTApplication.getResponseFromTeamGPT()!=null)
                            buddyGPTApplication.getResponseFromTeamGPT().reset();

                        if(buddyGPTApplication.getResponseFromTeamGPT()==null)
                            buddyGPTApplication.setResponseFromTeamGPT(new ResponseFromTeamGPT(buddyGPTApplication));
                        //if(buddyGPTApplication.getparam("Stream_mode").equalsIgnoreCase("true")){
                        buddyGPTApplication.getResponseFromTeamGPT().sendPutRequestStream(detectedSTTMessage, buddyGPTApplication.getQuestionNumber());
//                            }else
//                                buddyGPTApplication.getResponseFromTeamGPT().sendPutRequestNStream(detectedSTTMessage, buddyGPTApplication.getQuestionNumber());

                        if (
                                ( Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds",configFile))!=0 )
                                        && (
                                        (
                                                buddyGPTApplication.getCurrentLanguage().equals("en")
                                                        && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en",configFile).trim().isEmpty()
                                        )
                                                ||
                                                (
                                                        buddyGPTApplication.getCurrentLanguage().equals("fr")
                                                                && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_fr",configFile).trim().isEmpty()
                                                )
                                                ||
                                                (
                                                        buddyGPTApplication.getCurrentLanguage().equals("es")
                                                                && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_es",configFile).trim().isEmpty()
                                                )
                                                ||
                                                (
                                                        buddyGPTApplication.getCurrentLanguage().equals("de")
                                                                && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_de",configFile).trim().isEmpty()
                                                )
                                                ||(
                                                !buddyGPTApplication.getCurrentLanguage().equals("en")
                                                        && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en",configFile).trim().isEmpty()
                                        )

                                )
                        ) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    buddyGPTApplication.setAnswerHasExceededTimeOut(false);
                                    responseTimeout = new CountDownTimer(Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds", configFile)) * 1000, 1000) {
                                        @Override
                                        public void onTick(long l) {

                                        }

                                        @Override
                                        public void onFinish() {
                                            if (!buddyGPTApplication.isAlreadyGetAnswer()) {
                                                buddyGPTApplication.setAnswerHasExceededTimeOut(true);
                                                buddyGPTApplication.setTimeoutExpired(true);
                                                if (buddyGPTApplication.getCurrentLanguage().equals("en")) {
                                                    String[] message_Timeout_NotRespected_en = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en",configFile).split("/");
                                                    int randomNumber_message_Timeout_NotRespected_en = new Random().nextInt(message_Timeout_NotRespected_en.length);
                                                    speak(message_Timeout_NotRespected_en[randomNumber_message_Timeout_NotRespected_en],"timeOutExpired");
                                                }
                                                else if (buddyGPTApplication.getCurrentLanguage().equals("fr")) {
                                                    String[] message_Timeout_NotRespected_fr = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_fr",configFile).split("/");
                                                    int randomNumber_message_Timeout_NotRespected_fr = new Random().nextInt(message_Timeout_NotRespected_fr.length);
                                                    speak(message_Timeout_NotRespected_fr[randomNumber_message_Timeout_NotRespected_fr],"timeOutExpired");
                                                }
                                                else if (buddyGPTApplication.getCurrentLanguage().equals("es")) {
                                                    String[] message_Timeout_NotRespected_es = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_es",configFile).split("/");
                                                    int randomNumber_message_Timeout_NotRespected_es = new Random().nextInt(message_Timeout_NotRespected_es.length);
                                                    speak(message_Timeout_NotRespected_es[randomNumber_message_Timeout_NotRespected_es],"timeOutExpired");
                                                }
                                                else if (buddyGPTApplication.getCurrentLanguage().equals("de")) {
                                                    String[] message_Timeout_NotRespected_de = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_de",configFile).split("/");
                                                    int randomNumber_message_Timeout_NotRespected_de = new Random().nextInt(message_Timeout_NotRespected_de.length);
                                                    speak(message_Timeout_NotRespected_de[randomNumber_message_Timeout_NotRespected_de],"timeOutExpired");
                                                }
                                                else {
                                                    String[] message_Timeout_NotRespected_en = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en","BuddyGPT.properties").split("/");
                                                    int randomNumber_message_Timeout_NotRespected_en = new Random().nextInt(message_Timeout_NotRespected_en.length);
                                                    buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(message_Timeout_NotRespected_en[randomNumber_message_Timeout_NotRespected_en]).addOnSuccessListener(new OnSuccessListener<String>() {
                                                        @Override
                                                        public void onSuccess(String translatedText) {

                                                            speak(translatedText,"timeOutExpired");
                                                        }

                                                    }).addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Log.e(TAG,"translatedText exception  "+e);
                                                        }
                                                    });

                                                }



                                            }
                                        }
                                    };

                                    responseTimeout.start();
                                }
                            });
                        }
                    }
                });
            }


            else if (message.contains("TTS_success")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isWaitingForResponse = false;
                        if (startlisten==true) {
                            //startListeningFreeSpeech(buddyGPTApplication.getListeningAttempt() * 5);
                            buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getListeningAttempt()-1);
                            startCycle();
                        }
                        //creationFile.updateFile(buddyGPTApplication.getFileupdate(),settingClass, buddyGPTApplication);
                    }
                });
            }

            else if (message.contains("TTS_error") || message.contains("TTS_exception")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                        String text = message.split(";")[1];

                        Log.w(TAG,"TTS_ERROR:"+text);

                        buddyGPTApplication.playUsingReadSpeakerCaseError(text, new ITTSCallbacks() {
                            @Override
                            public void onSuccess(String s) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                        }
                                        catch (Exception e){
                                            Log.e(TAG,"BuddySDK Exception  "+e);
                                        }
//                                        if (buddyGPTApplication.getparam("Mode_Stream").equals("true") && buddyGPTApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") ) {
//                                            Log.w("MODE_STREAM","TTS ERROR");
//                                            buddyGPTApplication.getChatGptStreamMode().isReadyToSpeak = true;
//                                        }
//                                        else if(buddyGPTApplication.getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT")){
//                                            buddyGPTApplication.getCustomGPTStreamMode().isReadyToSpeak = true;
//                                        }
                                        //else{
                                            isWaitingForResponse = false;
                                            if (startlisten==true) {
                                                //startListeningFreeSpeech(buddyGPTApplication.getListeningAttempt() * 5);
                                                buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getListeningAttempt()-1);
                                                startCycle();
                                            }
                                            //creationFile.updateFile(buddyGPTApplication.getFileupdate(),settingClass, buddyGPTApplication);
                                       // }
                                    }
                                });
                            }

                            @Override
                            public void onError(String s) {
                                int textLength = text.length();// Calculate the length of the pronounced text
                                int delayTime = (textLength / 20) * 1000; // 1 second for every 20 characters
                                if(buddyGPTApplication.getparam("TTS").equalsIgnoreCase("ReadSpeaker") && (buddyGPTApplication.getCurrentLanguage().equals("en") || buddyGPTApplication.getCurrentLanguage().equals("fr")) && buddyGPTApplication.getUsingReadSpeaker() ){
                                    delayTime = 0;
                                }
                                handlerTTSError.postDelayed(runnableTTSError = new Runnable() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                                }
                                                catch (Exception e){
                                                    Log.e(TAG,"BuddySDK Exception  "+e);
                                                }
//                                                if (buddyGPTApplication.getparam("Mode_Stream").equals("true") && buddyGPTApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && buddyGPTApplication.getChatGptStreamMode() != null) {
//                                                    Log.w("MODE_STREAM","TTS ERROR");
//                                                    buddyGPTApplication.getChatGptStreamMode().isReadyToSpeak = true;
//                                                }
//                                                else if(buddyGPTApplication.getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && buddyGPTApplication.getCustomGPTStreamMode() != null){
//                                                    buddyGPTApplication.getCustomGPTStreamMode().isReadyToSpeak = true;
//                                                }
//                                                else{
                                                    isWaitingForResponse = false;
                                                    if (startlisten==true) {
                                                        //startListeningFreeSpeech(buddyGPTApplication.getListeningAttempt() * 5);
                                                        buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getListeningAttempt()-1);
                                                        startCycle();
                                                    }
                                                    //creationFile.updateFile(buddyGPTApplication.getFileupdate(),settingClass, buddyGPTApplication);
                                                //}
                                            }
                                        });
                                    }
                                },delayTime);
                            }
                        });

                    }
                });
            }

            else if(message.contains("CHATBOTS_RETURN")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String action = message.split(";SPLIT;")[1];
                        String value =  message.split(";SPLIT;")[2];
                        if(action.equals("translation_question") && !value.equals("OPERATION_FAILED")){
                            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                            Replica question = new Replica();
                            question.setType("Question traduction");
                            question.setTime(time);
                            question.setValue(value);
                            listRep.add(question);
                            listRepGlobale.add(question);
                            updateChat();
                        }
                        else if(action.equals("speak")){
                            if (message.split(";SPLIT;").length>3) {
                                int numberOfQuestion = Integer.parseInt(message.split(";SPLIT;")[3]);

                                if (numberOfQuestion == buddyGPTApplication.getQuestionNumber()) {
                                    if (!buddyGPTApplication.isTimeoutExpired()) {
                                        speak(value, "nothealysa");
                                    } else {
                                        buddyGPTApplication.setStoredResponse(value);
                                    }
                                }
                            }
                        }
                    }
                });
            }


            else if (message.contains("conversationFinished google assistant responce")){

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isWaitingForResponse = false;
                        //startListeningFreeSpeech(buddyGPTApplication.getListeningAttempt()*5);
                        buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getListeningAttempt()-1);
                        startCycle();
                        //creationFile.updateFile(buddyGPTApplication.getFileupdate(),settingClass, buddyGPTApplication);
                    }
                });
            }
            else if(message.contains("main destroy")){
                buddyGPTApplication.setFileCreate(false);
                buddyGPTApplication.setparam("firstLaunch","false");
            }
            else if (message.contains("getResponseF;SPLIT;chatbot;SPLIT;response google complete")){
                buddyGPTApplication.notifyObservers("play google response");
            }
            else if (message.contains("playStoredResponse")){
                if (!buddyGPTApplication.getStoredResponse().equals("")){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            speak(buddyGPTApplication.getStoredResponse(),"storedResponse");
                        }
                    });

                }
            }
            else if (message.contains("makeBuddyFaceNeutral")){
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
            }
            else if (message.contains("mailSend")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(buddyGPTApplication.getLangue().getNom().equals(langueEn)) {
                            if (!buddyGPTApplication.getParamFromFile("Message_mail_send_en",configFile).trim().equals("")){
                                Toast.makeText(buddyGPTApplication, buddyGPTApplication.getParamFromFile("Message_mail_send_en",configFile),Toast.LENGTH_LONG).show();
                            }

                        }
                        else if(buddyGPTApplication.getLangue().getNom().equals(langueFr)){
                            if (!buddyGPTApplication.getParamFromFile("Message_mail_send_fr",configFile).trim().equals("")) {
                                Toast.makeText(buddyGPTApplication, buddyGPTApplication.getParamFromFile("Message_mail_send_fr", configFile), Toast.LENGTH_LONG).show();
                            }
                        }

                        else{
                            if (!buddyGPTApplication.getParamFromFile("Message_mail_send_en",configFile).trim().equals("")) {
                                buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getParamFromFile("Message_mail_send_en", configFile)).addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String translatedText) {
                                        Toast.makeText(buddyGPTApplication, translatedText, Toast.LENGTH_LONG).show();
                                    }

                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(TAG, "translatedText exception  " + e);
                                    }
                                });
                            }
                        }
                    }
                });
            }
            else if (message.contains("Session_ID_Changed")){
                Replica session = new Replica();
                session.setType("Session");
                session.setValue(buddyGPTApplication.getparam("SelectedChatbot")+" - "+ buddyGPTApplication.getModel());
                listRepGlobale.add(session);
                Replica[] mDataset = listRepGlobale.toArray(new Replica[0]);
                adapter.setData(mDataset);
                scroll();
            }
            if (message.contains("INVALID_TEAMGPT_KEY")){
                buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY","TRUE");
                if (buddyGPTApplication.getLangue().getNom().equals("Anglais")) {
                    buddyGPTApplication.showInputDialog(ChatWindow.this, buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                } else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                    buddyGPTApplication.showInputDialog(ChatWindow.this, buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_fr), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_fr));
                } else if (buddyGPTApplication.getLangue().getNom().equals("Espagnol")) {
                    buddyGPTApplication.showInputDialog(ChatWindow.this, buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_es), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_es));
                } else if (buddyGPTApplication.getLangue().getNom().equals("Allemand")) {
                    buddyGPTApplication.showInputDialog(ChatWindow.this, buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_de), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_de));
                }
                else {
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                            .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    buddyGPTApplication.showInputDialog(ChatWindow.this, translatedText,"Attention !");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    buddyGPTApplication.showInputDialog(ChatWindow.this, buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                                }
                            });
                }


            }
            if (message.contains("INVALID_TEAMGPT_DEVICE_ID")){
                buddyGPTApplication.setparam("INVALID_TEAMGPT_DEVICE_ID","TRUE");
                if (buddyGPTApplication.getLangue().getNom().equals("Anglais")) {
                    buddyGPTApplication.showInputDialog(ChatWindow.this, buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                } else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                    buddyGPTApplication.showInputDialog(ChatWindow.this, buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_fr), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_fr));
                }
                else {
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                            .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    buddyGPTApplication.showInputDialog(ChatWindow.this, translatedText,"Attention !");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    buddyGPTApplication.showInputDialog(ChatWindow.this, buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                                }
                            });
                }


            }
            if (message.contains("Session_ID_ERROR")){

                if (buddyGPTApplication.getLangue().getNom().equals("Anglais")) {
                    buddyGPTApplication.showInputDialog2(ChatWindow.this, buddyGPTApplication.getString(R.string.toast_teamgpt_params_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                } else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                    buddyGPTApplication.showInputDialog2(ChatWindow.this, buddyGPTApplication.getString(R.string.toast_teamgpt_params_invalid_fr), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_fr));
                } else {
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                            .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_params_invalid_en))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    buddyGPTApplication.showInputDialog2(ChatWindow.this, translatedText,"Attention !");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    buddyGPTApplication.showInputDialog2(ChatWindow.this, buddyGPTApplication.getString(R.string.toast_teamgpt_params_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                                }
                            });
                }


            }
            else if (message.contains("ErrorSending")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(buddyGPTApplication.getLangue().getNom().equals(langueEn)) {
                            Toast.makeText(buddyGPTApplication, buddyGPTApplication.getString(R.string.error_mail_toast_en),Toast.LENGTH_LONG).show();
                        }
                        else if(buddyGPTApplication.getLangue().getNom().equals(langueFr)){
                            Toast.makeText(buddyGPTApplication, buddyGPTApplication.getString(R.string.error_mail_toast_fr),Toast.LENGTH_LONG).show();
                        }
                        else if(buddyGPTApplication.getLangue().getNom().equals(langueEs)){
                            Toast.makeText(buddyGPTApplication, buddyGPTApplication.getString(R.string.error_mail_toast_es),Toast.LENGTH_LONG).show();
                        }
                        else if(buddyGPTApplication.getLangue().getNom().equals(langueDe)){
                            Toast.makeText(buddyGPTApplication, buddyGPTApplication.getString(R.string.error_mail_toast_de),Toast.LENGTH_LONG).show();
                        }
                        else{
                            buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getString(R.string.error_mail_toast_en)).addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    Toast.makeText(buddyGPTApplication, translatedText, Toast.LENGTH_LONG).show();
                                }

                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG,"translatedText exception  "+e);
                                }
                            });
                        }
                    }
                });

            }
            else if (message.contains("changeDetected")){
                int speakVolume = buddyGPTApplication.getVolume();
                int max = buddyGPTApplication.getMaxVolume();
                int defaultVolume = buddyGPTApplication.getClosestInt((double) (speakVolume * 100) / max);
                Log.e("FCH","volumeMedia  "+String.valueOf(defaultVolume));
                buddyGPTApplication.setparam("speak_volume", String.valueOf(defaultVolume));
            }
            else if (message.contains("restartListeningHotword")){
                click=1;
                isListeningFreeSpeech=false;
                isWaitingForResponse=false;
                startlisten=false;
                buddyGPTApplication.stopTTS();
                buddyGPTApplication.setStoredResponse("");
                try {
                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                }
                catch (Exception e){
                    Log.e(TAG,"BuddySDK Exception  "+e);
                }
                stopListeningFreeSpeech();
            }
            else if (message.contains("end of cycle")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        buddyGPTApplication.setLed("neutral");
                        micro_btn.setImageResource(R.drawable.micro_off);
                    }
                });

            }
            else if (message.contains("restartNewCycle")){
                runnablePauseTime =new Runnable() {
                    @Override
                    public void run() {
                        // startCycle(settingClass, listRep, nameActivity, adapter, cancelTheTimer);
                        startNextCycle();
                        Log.e("ARR","startNextCycle  after handler ");
                    }
                };
                handlerPauseTime.postDelayed(runnablePauseTime,1000);

            }
            else if (message.contains("Obtain audio transcription after the listening time has elapsed")){
                String shouldRestartNewCycle = message.split(";SPLIT;")[1];
                Log.e("ARR","Obtain audio transcription after the listening time has elapsed "+shouldRestartNewCycle);
                micro_btn.setImageResource(R.drawable.micro_off);
                buddyGPTApplication.setLed("neutral");
                buddyGPTApplication.setAppIsListeningToTheQuestion(false);
                if (shouldRestartNewCycle.equals("true")) {
                    buddyGPTApplication.traitementAudio(true);
                }else {
                    buddyGPTApplication.traitementAudio(false);
                }

            }
        }
    }
    /**
     * ------------------------------------------ STT  -------------------------------------------
     */

    private void startListeningFreeSpeech(int duration) {

        isListeningFreeSpeech = true;
        buddyGPTApplication.setOpenaialreadySwitchEmotion(false);
        buddyGPTApplication.setAppIsListeningToTheQuestion(true);

        Log.d(TAG," --- startListeningFreeSpeech("+duration+") ---");


            if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Android")){
                buddyGPTApplication.startListeningQuestion(this);
            }else if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Cerence")){
                if (buddyGPTApplication.getCurrentLanguage().equals("fr") || buddyGPTApplication.getCurrentLanguage().equals("en")){
                    buddyGPTApplication.startListeningCerence(this);
                }
                else{
                    buddyGPTApplication.startListeningQuestion(this);
                }
            }



        if(timerEcoute != null) timerEcoute.cancel();
        timerEcoute = new CountDownTimer(duration * 1000L,1000) {
            @Override
            public void onTick(long l) {
                Log.d(TAG, "timerEcoute onTick");
            }
            @Override
            public void onFinish() {

                if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Android") || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Cerence")){
                    Log.i(TAG, "timerEcoute onFinish");
                    stopListeningFreeSpeech();
                    click=1;
                }
                else{
                    buddyGPTApplication.notifyObservers("Obtain audio transcription after the listening time has elapsed;SPLIT;false");
                }
            }
        };
        timerEcoute.start();

        micro_btn.setImageResource(R.drawable.micro_on);

    }
    private void startCycle(){
        isListeningFreeSpeech = true;
        buddyGPTApplication.setOpenaialreadySwitchEmotion(false);
        buddyGPTApplication.setAppIsListeningToTheQuestion(true);

            if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Android")){
                buddyGPTApplication.startListeningQuestion(this);

            }else if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Cerence")){
                if (buddyGPTApplication.getCurrentLanguage().equals("fr") || buddyGPTApplication.getCurrentLanguage().equals("en")){
                    buddyGPTApplication.startListeningCerence(this);
                }
                else{
                    buddyGPTApplication.startListeningQuestion(this);
                }
            }



        if(timerEcoute != null) timerEcoute.cancel();
        timerEcoute = new CountDownTimer(buddyGPTApplication.getListeningDuration() * 1000L,1000) {
            @Override
            public void onTick(long l) {
                Log.d(TAG, "timerEcoute onTick");
            }
            @Override
            public void onFinish() {
                Log.i(TAG, "timerEcoute onFinish");
                if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Android") || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Cerence")){
                    buddyGPTApplication.notifyObservers("end of cycle");
                    runnablePauseTime =new Runnable() {
                        @Override
                        public void run() {
                            // startCycle(settingClass, listRep, nameActivity, adapter, cancelTheTimer);
                            startNextCycle();
                            Log.e("ARR","startNextCycle  after handler ");
                        }
                    };
                    handlerPauseTime.postDelayed(runnablePauseTime,1000);
                }
                else{
                    buddyGPTApplication.notifyObservers("Obtain audio transcription after the listening time has elapsed;SPLIT;true");
                }
            }
        };
        timerEcoute.start();

        micro_btn.setImageResource(R.drawable.micro_on);
    }
    public void startNextCycle() {
        // Si nous avons encore des tentatives restantes
        Log.e("ARR","startNextCycle  remainingattempts= "+ buddyGPTApplication.getRemainingAttempts());
        if (buddyGPTApplication.getRemainingAttempts() > 0) {
            buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getRemainingAttempts()-1);
            startCycle();

            Log.e("ARR","startNextCycle  after handler ");
        } else {
            stopListeningFreeSpeech();
            click=1;
            // Si toutes les tentatives ont été épuisées, vous pouvez faire quelque chose ici si nécessaire
        }
    }
    private void stopListeningFreeSpeech() {

        isListeningFreeSpeech = false;

        Log.d(TAG," --- stopListeningFreeSpeech() ---");

        if (timerEcoute!=null) timerEcoute.cancel();
        buddyGPTApplication.stopListening(this);
        buddyGPTApplication.setLed("neutral");
        micro_btn.setImageResource(R.drawable.micro_off);
    }


    /**
     * ------------------------------------------ TTS  -------------------------------------------
     */

    private void speak(final String texte,String type) {
        Log.d(TAG," --- speak("+texte+") ---");
        if (responseTimeout!=null) responseTimeout.cancel();
        if (type.equals("nothealysa") || type.equals("storedResponse")) {

            buddyGPTApplication.setAlreadyGetAnswer(true);
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());

                if (!listRep.isEmpty()) {
                    //--> this function is called right after a question : we should create a new Replica for the response
                    Replica reponse = new Replica();
                    reponse.setValue(texte);
                    reponse.setTime(time);
                    long responseTime = buddyGPTApplication.getResponseTime() - buddyGPTApplication.getQuestionTime();
                    DecimalFormat df = new DecimalFormat("#,###");
                    String formattedTime= df.format(responseTime);
                    reponse.setType("Response");
                    reponse.setDuration(formattedTime + " ms");

                    listRep.add(reponse);
                    Session session = new Session(new ArrayList<>(listRep));
                    buddyGPTApplication.getListSession().add(session);
                    listRep.clear();
                    listRepGlobale.add(reponse);
                    Replica[] mDataset = listRepGlobale.toArray(new Replica[0]);
                    adapter.setData(mDataset);
                    scroll();
                }
                else{
                    //---> this function is called after finishing pronouncing a phrase from the response : we should add the new phrase to the already existing Replica
                    Replica lastReplica = listRepGlobale.get(listRepGlobale.size() - 1);
                    if (lastReplica.getType().equals("Response")) {

                        if(responseFromTeamGPT != null){
                            if(!responseFromTeamGPT .isError)lastReplica.setValue(lastReplica.getValue() + texte);
                        }
                        else lastReplica.setValue(texte);

                    }
                    Replica[] mDataset = listRepGlobale.toArray(new Replica[0]);
                    adapter.setData(mDataset);
                    scroll();
                }


            buddyGPTApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL, type);
        }
        else if (type.equals("timeOutExpired")){
            buddyGPTApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL,type);
        }
    }


    /**
     *   -------------------------------  Gestion d'affichage des barres du systemUI  ----------------------------------------------
     */

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            buddyGPTApplication.hideSystemUI(this);
        }
    }

}