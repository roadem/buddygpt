package com.robotique.aevaweb.buddygpt.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.AsyncTask;
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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.GazePosition;
import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddy.utils.events.EventItem;
import com.bfr.buddy.utils.values.FloatingWidgetVisibility;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.robotique.aevaweb.buddygpt.R;
import com.robotique.aevaweb.buddygpt.adapters.ReplicaListAdapter;
import com.robotique.aevaweb.buddygpt.application.BuddyGPTApplication;
import com.robotique.aevaweb.buddygpt.chatbotresponse.ResponseFromTeamGPT;
import com.robotique.aevaweb.buddygpt.models.Langue;
import com.robotique.aevaweb.buddygpt.models.Replica;
import com.robotique.aevaweb.buddygpt.models.Session;
import com.robotique.aevaweb.buddygpt.models.Setting;
import com.robotique.aevaweb.buddygpt.observers.IDBObserver;
import com.robotique.aevaweb.buddygpt.utilis.ITTSCallbacks;
import com.robotique.aevaweb.buddygpt.utilis.MailSender;
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
    private static final String BUDDY_SDK_EXCEPTION = "BuddySDK Exception ";
    private static final String MAIL_DESTINATION_KEY = "Mail_Destination";
    private static final String KEY_QUESTION = "Question";
    private static final String KEY_RESPONSE = "Response";
    private static final String SPLITER = ";SPLIT;";
    private static final String SESSION_TYPE = "Session";
    private static final String INVALID_DEVICE_ID = "INVALID_TEAMGPT_DEVICE_ID";
    private static final String INVALID_KEY = "INVALID_TEAMGPT_KEY";
    private static final String TeamGPT_Key = "TeamGPT_Key";
    private static final String ANDROID_STT = "Android";
    private static final String CERENCE_STT = "Cerence";
    private static final String NEUTRAL = "neutral";
    private static final String Selected_Chatbot = "SelectedChatbot";
    private BuddyGPTApplication buddyGPTApplication;
    private View decorView;
    private Random random = new Random();
    private boolean onSdkReadyIsAlreadyCalledOnce = false;
    private boolean isListeningFreeSpeech = false;
    private boolean isWaitingForResponse = false;


    private Setting settingClass;
    private ArrayList<Replica> listRep=new ArrayList<>();
    private ArrayList<Replica> listRepGlobale=new ArrayList<>();
    private ReplicaListAdapter adapter;

    //timers
    private CountDownTimer timerEcoute;
    private CountDownTimer responseTimeout;

    //views
    private RelativeLayout popupAddMail;
    private LinearLayout popupAddMailContent;
    private RelativeLayout parentChat;
    private ImageView microBtn;
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
// todo
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
        decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            if (visibility == 0) {
                decorView.setSystemUiVisibility(buddyGPTApplication.hideSystemUI(ChatWindow.this));
            }
        });
        if(responseFromTeamGPT != null){
            responseFromTeamGPT.reset();
        }
        responseFromTeamGPT=new ResponseFromTeamGPT(buddyGPTApplication);
        //init views
        popupAddMail = findViewById(R.id.popup_add_mail);
        parentChat = findViewById( R.id.parent_chat );
        microBtn = findViewById( R.id.micro_btn );
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
        if(handlerTTSError!=null && runnableTTSError!=null){
            handlerTTSError.removeCallbacks(runnableTTSError);
            handlerTTSError.removeCallbacksAndMessages(null);
        }
        onSdkReadyIsAlreadyCalledOnce = false;
        isWaitingForResponse = false;
        startlisten=true;
        listRep=new ArrayList<Replica>();
        buddyGPTApplication.stopTTS();
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
        }
        catch (Exception e){
            Log.e(TAG,BUDDY_SDK_EXCEPTION+e);
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
            BuddySDK.UI.setViewAsFace(parentChat);
            BuddySDK.UI.setMenuWidgetVisibility(FloatingWidgetVisibility.ALWAYS);
            BuddySDK.UI.setCloseWidgetVisibility(FloatingWidgetVisibility.ALWAYS);

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

        popupAddMail.setOnClickListener(v -> {
            // Vérifier si le popup_add_mail est visible et si le clic est en dehors de celui-ci
            if (popupAddMail.getVisibility() == View.VISIBLE) {
                MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);
                if (!isViewInsideBounds(popupAddMailContent, (int) event.getRawX(), (int) event.getRawY())) {
                    // Si le clic est en dehors, rendre le popup invisible
                    popupAddMail.setVisibility(View.INVISIBLE);
                }
            }
        });
        popupAddMailContent.setOnClickListener(v -> {
            // Ne rien faire pour empêcher la propagation du clic aux éléments enfants du popup
        });


        adapter = new ReplicaListAdapter(buddyGPTApplication,initDataset());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        editTextEmail.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);


        editTextEmail.setText(buddyGPTApplication.getparam(MAIL_DESTINATION_KEY));


        editTextEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Method left empty intentionally because no specific action is needed for this update.
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {


                buddyGPTApplication.setparam(MAIL_DESTINATION_KEY,charSequence.toString());
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
                    buddyGPTApplication.setparam(MAIL_DESTINATION_KEY,buddyGPTApplication.getparam("Email"));
                    editTextEmail.setText(buddyGPTApplication.getparam(MAIL_DESTINATION_KEY));
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
                if (messageObject.has(KEY_QUESTION) ) {
                    replica.setType(KEY_QUESTION);
                    replica.setValue(messageObject.getString(KEY_QUESTION));
                }
                if (messageObject.has(KEY_RESPONSE) ) {
                    replica.setType(KEY_RESPONSE);
                    replica.setValue(messageObject.getString(KEY_RESPONSE).split(SPLITER)[0]);
                    replica.setDuration(messageObject.getString(KEY_RESPONSE).split(SPLITER)[1]);
                }
                if (messageObject.has(SESSION_TYPE) ){
                    replica.setType(SESSION_TYPE);
                    replica.setValue(messageObject.getString(SESSION_TYPE));
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
        recyclerView.post(() -> recyclerView.smoothScrollToPosition(adapter.getItemCount() + 5));
        scrollView.post(() ->  scrollView.fullScroll(android.view.View.FOCUS_DOWN));

    }


    /**
     * Gestion du clic sur l'icone Micro
     */
    public void onClickMicro(View view) {
        resetTimeoutsAndHandlers();

        if (isInvalidTeamGPTDeviceId()) {
            notifyInvalidTeamGPTDeviceId();
        } else if (isInvalidTeamGPTKey()) {
            notifyInvalidTeamGPTKey();
        } else if (isTeamGPTKeyEmpty()) {
            handleEmptyTeamGPTKey();
        } else {
            handleMicroClick();
        }
    }

    private void resetTimeoutsAndHandlers() {
        if (responseTimeout != null) responseTimeout.cancel();
        if (buddyGPTApplication.getResponseFromTeamGPT() != null) buddyGPTApplication.getResponseFromTeamGPT().reset();
        if (handlerTTSError != null && runnableTTSError != null) {
            handlerTTSError.removeCallbacks(runnableTTSError);
            handlerTTSError.removeCallbacksAndMessages(null);
        }
    }

    private boolean isInvalidTeamGPTDeviceId() {
        return buddyGPTApplication.getparam(INVALID_DEVICE_ID).equalsIgnoreCase("TRUE")
                && !buddyGPTApplication.getparam(TeamGPT_Key).equalsIgnoreCase("");
    }

    private void notifyInvalidTeamGPTDeviceId() {
        Log.i("TAG", "run: notifyObservers INVALID_TEAMGPT_DEVICE_ID 3");
        buddyGPTApplication.notifyObservers(INVALID_DEVICE_ID);
    }

    private boolean isInvalidTeamGPTKey() {
        return buddyGPTApplication.getparam(INVALID_KEY).equalsIgnoreCase("TRUE")
                && !buddyGPTApplication.getparam(TeamGPT_Key).equalsIgnoreCase("");
    }

    private void notifyInvalidTeamGPTKey() {
        Log.i("TAG", "run: notifyObservers INVALID_TEAMGPT_KEY 3");
        buddyGPTApplication.notifyObservers(INVALID_KEY);
    }

    private boolean isTeamGPTKeyEmpty() {
        return buddyGPTApplication.getparam(TeamGPT_Key).equalsIgnoreCase("");
    }

    private void handleEmptyTeamGPTKey() {
        Log.i("TAG", "run: notifyObservers TEAMGPT_KEY EMPTY 3");
        String messageKey = getTeamGPTKeyMessageKey();
        buddyGPTApplication.showToast(messageKey);
    }

    private String getTeamGPTKeyMessageKey() {
        String language = buddyGPTApplication.getLangue().getNom();
        if (language.equals(langueEn)) return getString(R.string.toast_teamgpt_key_indispo_en);
        if (language.equals(langueFr)) return getString(R.string.toast_teamgpt_key_indispo_fr);
        if (language.equals(langueEs)) return getString(R.string.toast_teamgpt_key_indispo_es);
        if (language.equals(langueDe)) return getString(R.string.toast_teamgpt_key_indispo_de);
        return translateTeamGPTKeyMessage();
    }

    private String translateTeamGPTKeyMessage() {
        buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                .translate(getString(R.string.toast_teamgpt_key_indispo_en))
                .addOnSuccessListener(translatedText -> buddyGPTApplication.showToast(translatedText))
                .addOnFailureListener(e -> buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_en)));
        return "";
    }

    private void handleMicroClick() {
        if (click == 1) {
            startListening();
        } else if (click == 2) {
            stopListeningOrProcessAudio();
        }
    }

    private void startListening() {
        click = 2;
        if (!isListeningFreeSpeech && !isWaitingForResponse) {
            buddyGPTApplication.stopTTS();
            startlisten = true;
            startListeningFreeSpeech(buddyGPTApplication.getListeningDuration());
        }
    }

    private void stopListeningOrProcessAudio() {
        if (shouldStopListening()) {
            resetListeningState();
        } else {
            processAudio();
        }
    }

    private boolean shouldStopListening() {
        return buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(ANDROID_STT)
                || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(CERENCE_STT)
                || !buddyGPTApplication.getAppIsListeningToTheQuestion();
    }

    private void resetListeningState() {
        click = 1;
        isListeningFreeSpeech = false;
        isWaitingForResponse = false;
        startlisten = false;
        buddyGPTApplication.stopTTS();
        buddyGPTApplication.setStoredResponse("");
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
        } catch (Exception e) {
            Log.e(TAG, BUDDY_SDK_EXCEPTION + e);
        }
        stopListeningFreeSpeech();
    }

    private void processAudio() {
        buddyGPTApplication.setLed(NEUTRAL);
        microBtn.setImageResource(R.drawable.micro_off);
        buddyGPTApplication.setAppIsListeningToTheQuestion(false);
        buddyGPTApplication.traitementAudio(false);
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
            buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getString(R.string.destination_mail_texte))
                    .addOnSuccessListener(translatedText -> textEmail.setText(translatedText))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "translatedText exception  " + e);
                        textEmail.setText(buddyGPTApplication.getString(R.string.destination_mail_texte));
                    });

            buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getString(R.string.destination_mail_Edittexte))
                    .addOnSuccessListener(translatedText -> editTextEmail.setHint(translatedText))
                            .addOnFailureListener(e -> {
                    Log.e(TAG,"translatedText exception  "+e);
                    editTextEmail.setHint(buddyGPTApplication.getString(R.string.destination_mail_Edittexte));
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
            if (buddyGPTApplication.getLangue().getNom().equals(langueEn)){
                buddyGPTApplication.showToast(getString(R.string.no_message_to_send_en));
            }
            else if (buddyGPTApplication.getLangue().getNom().equals(langueFr)) {
                buddyGPTApplication.showToast(getString(R.string.no_message_to_send_fr));
            }
            else{
                buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                        .translate(getString(R.string.no_message_to_send_en))
                        .addOnSuccessListener(translatedText->
                                buddyGPTApplication.showToast(translatedText))
                        .addOnFailureListener( e ->
                                buddyGPTApplication.showToast(getString(R.string.no_message_to_send_en)));
            }
        }
        else
            popupAddMail.setVisibility(View.VISIBLE);

    }
    /**
     * Gestion du clic sur l'icone Send depuis le popUP
     */
    @SuppressLint("SuspiciousIndentation")
    public void onClickSendFromPopup(View view){

            if (!buddyGPTApplication.getparam(MAIL_DESTINATION_KEY).trim().isEmpty()){
                if(buddyGPTApplication.getLangue().getNom().equals(langueEn)) {
                    writeMail( emailContent -> {
                            smtpService = new MailSender(ChatWindow.this,emailContent, buddyGPTApplication.getparam("Mail_Destination"), buddyGPTApplication.getparam("Mail_Subject_en"));
                            smtpService.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                    });

                }
                else if(buddyGPTApplication.getLangue().getNom().equals(langueFr)){
                    writeMail(emailContent -> {

                    smtpService = new MailSender(ChatWindow.this,emailContent, buddyGPTApplication.getparam(MAIL_DESTINATION_KEY), buddyGPTApplication.getparam("Mail_Subject_fr"));
                        smtpService.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                });

            }else{
                    final Activity activity = ChatWindow.this;
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getparam("Mail_Subject_en"))
                            .addOnSuccessListener( translatedText -> {
                            writeMail(emailContent -> {
                                    smtpService = new MailSender(activity,emailContent, buddyGPTApplication.getparam(MAIL_DESTINATION_KEY), translatedText);
                                smtpService.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            });

                    }).addOnFailureListener( e -> {
                            Log.e(TAG,"translatedText exception  "+e);
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
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getString(R.string.add_mail_toast_en))
                            .addOnSuccessListener(translatedText -> {
                            Toast.makeText(buddyGPTApplication, translatedText, Toast.LENGTH_LONG).show();
                    }).addOnFailureListener(e -> {
                            Log.e(TAG,"translatedText exception  "+e);
                    });
                }
            }

    }
    public void writeMail(OnMailReadyListener listener) {
        String langue = buddyGPTApplication.getLangue().getNom();

        if (langue.equals(langueEn)) {
            listener.onMailReady(buildEmailContent(
                    new String[]{"_____________________ New Session _____________________"},
                    new String[]{KEY_RESPONSE},
                    new String[]{KEY_QUESTION}
            ));
        } else if (langue.equals(langueFr)) {
            listener.onMailReady(buildEmailContent(
                    new String[]{"_____________________ Nouvelle Session _____________________"},
                    new String[]{"Réponse"},
                    new String[]{KEY_QUESTION}
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
                    translateNext(KEY_RESPONSE, responseText, () ->
                            translateNext(KEY_QUESTION, qstText, () ->
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
        translateNext(KEY_RESPONSE, responseText, () ->
                translateNext(KEY_QUESTION, qstText, () ->
                        listener.onMailReady(buildEmailContent(new String[]{newSessionText[0]}, new String[]{responseText[0]}, new String[]{qstText[0]}))
                )
        );
    }

    private String buildEmailContent(String[] newSessionText, String[] responseText, String[] qstText) {
        String firstLine;
        Log.i(TAG, "buildEmailContent: "+newSessionText[0]+"******"+responseText[0]+"******"+qstText[0]+"******");
        if (!buddyGPTApplication.getparam(Selected_Chatbot).equalsIgnoreCase("") ||
                !buddyGPTApplication.getparam("NomCompte").equalsIgnoreCase("")) {
            firstLine = buddyGPTApplication.getparam("NomCompte") + " " +
                    buddyGPTApplication.getparam(Selected_Chatbot) + " " +
                    buddyGPTApplication.getModel() + "<br>";
        } else {
            firstLine = "_<br>";
        }

        StringBuilder question = new StringBuilder(firstLine);
        for (Replica replica : listRepGlobale) {
            if (replica.getType().equalsIgnoreCase(SESSION_TYPE)) {
                question.append("<br>").append(newSessionText[0])
                        .append("<br>").append(replica.getValue());
            } else if (replica.getType().equalsIgnoreCase(KEY_RESPONSE)) {
                question.append("<br>").append(responseText[0])  // Correction ici
                        .append(" : ").append(replica.getValue().split(SPLITER)[0])
                        .append(" (").append(replica.getDuration()).append(")");
            } else if (replica.getType().equalsIgnoreCase(KEY_QUESTION)) {
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
        if(!isClickedBtnCloseChat){
            isClickedBtnCloseChat=true;
            buddyGPTApplication.stopTTS();

            try {
                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
            }
            catch (Exception e){
                Log.e(TAG,BUDDY_SDK_EXCEPTION+e);
            }

            Intent intent = new Intent(ChatWindow.this,MainActivity.class);
            intent.putExtra("fromChatWindow", "true");
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);}

    }

    /**
     * ------------------------------------------ Gestion de notifications --------------------------
     */

    @Override
    public void update(String message) throws IOException {
        if (message == null) return;

        if (message.contains("CANCEL_RESPONSE_TIMEOUT")) handleCancelResponseTimeout();
        else if (message.contains("MODE_STREAM_SPEAK;SPLIT;")) handleModeStreamSpeak(message);
        else if (message.contains("STTQuestion_success")) handleSTTQuestionSuccess(message);
        else if (message.contains("TTS_success")) handleTTSSuccess();
        else if (message.contains("TTS_error") || message.contains("TTS_exception")) handleTTSError(message);
        else if (message.contains("CHATBOTS_RETURN")) handleChatbotsReturn(message);
        else if (message.contains("conversationFinished google assistant responce")) handleConversationFinished();
        else if (message.contains("main destroy")) handleMainDestroy();
        else if (message.contains("getResponseF;SPLIT;chatbot;SPLIT;response google complete")) handleGoogleResponseComplete();
        else if (message.contains("playStoredResponse")) handlePlayStoredResponse();
        else if (message.contains("makeBuddyFaceNeutral")) handleMakeBuddyFaceNeutral();
        else if (message.contains("mailSend")) handleMailSend();
        else if (message.contains("Session_ID_Changed")) handleSessionIDChanged();
        else if (message.contains(INVALID_KEY)) handleInvalidKey();
        else if (message.contains(INVALID_DEVICE_ID)) handleInvalidDeviceID();
        else if (message.contains("Session_ID_ERROR")) handleSessionIDError();
        else if (message.contains("ErrorSending")) handleErrorSending();
        else if (message.contains("changeDetected")) handleChangeDetected();
        else if (message.contains("restartListeningHotword")) handleRestartListeningHotword();
        else if (message.contains("end of cycle")) handleEndOfCycle();
        else if (message.contains("restartNewCycle")) handleRestartNewCycle();
        else if (message.contains("Obtain audio transcription after the listening time has elapsed")) handleAudioTranscription(message);
    }

    private void handleCancelResponseTimeout() {
        if (responseTimeout != null) responseTimeout.cancel();
    }

    private void handleModeStreamSpeak(String message) {
        runOnUiThread(() -> {
            String[] parts = message.split(SPLITER);
            if (parts.length > 1) speak(parts[1], "nothealysa");
        });
    }

    private void handleSTTQuestionSuccess(String message) {
        runOnUiThread(() -> {
            buddyGPTApplication.setAppIsListeningToTheQuestion(false);
            isWaitingForResponse = true;
            stopListeningFreeSpeech();

            String detectedSTTMessage = message.split(";")[1].replaceAll("' ", "'");
            addQuestionToChat(detectedSTTMessage);
            sendPutRequest(detectedSTTMessage);
            startResponseTimeout();
        });
    }

    private void addQuestionToChat(String detectedSTTMessage) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        Replica question = new Replica(KEY_QUESTION, time, detectedSTTMessage);
        listRep.add(question);
        listRepGlobale.add(question);
        updateChat();
    }

    private void sendPutRequest(String detectedSTTMessage) {
        if (buddyGPTApplication.getResponseFromTeamGPT() == null) {
            buddyGPTApplication.setResponseFromTeamGPT(new ResponseFromTeamGPT(buddyGPTApplication));
        }
        buddyGPTApplication.getResponseFromTeamGPT().sendPutRequestStream(detectedSTTMessage, buddyGPTApplication.getQuestionNumber());
    }

    private void startResponseTimeout() {
        if (shouldStartResponseTimeout()) {
            runOnUiThread(() -> {
                buddyGPTApplication.setAnswerHasExceededTimeOut(false);
                responseTimeout = new CountDownTimer(getResponseTimeoutDuration(), 1000) {
                    @Override
                    public void onTick(long l) {}

                    @Override
                    public void onFinish() {
                        handleResponseTimeout();
                    }
                };
                responseTimeout.start();
            });
        }
    }

    private boolean shouldStartResponseTimeout() {
        return Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds", configFile)) != 0 &&
                !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en", configFile).trim().isEmpty();
    }

    private long getResponseTimeoutDuration() {
        return (long) Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds", configFile)) * 1000;
    }

    private void handleResponseTimeout() {
        if (!buddyGPTApplication.isAlreadyGetAnswer()) {
            buddyGPTApplication.setAnswerHasExceededTimeOut(true);
            buddyGPTApplication.setTimeoutExpired(true);
            speakTimeoutMessage();
        }
    }

    private void speakTimeoutMessage() {
        String[] messages = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en", configFile).split("/");
        int randomIndex = random.nextInt(messages.length);
        speak(messages[randomIndex], "timeOutExpired");
    }

    private void handleTTSSuccess() {
        runOnUiThread(() -> {
            isWaitingForResponse = false;
            if (startlisten) {
                buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getListeningAttempt() - 1);
                startCycle();
            }
        });
    }

    private void handleTTSError(String message) {
        runOnUiThread(() -> {
            String text = message.split(";")[1];
            buddyGPTApplication.playUsingReadSpeakerCaseError(text, new ITTSCallbacks() {
                @Override
                public void onSuccess(String s) {
                    handleTTSErrorSuccess();
                }

                @Override
                public void onError(String s) {
                    handleTTSErrorFailure(text);
                }
            });
        });
    }

    private void handleTTSErrorSuccess() {
        runOnUiThread(() -> {
            try {
                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
            } catch (Exception e) {
                Log.e(TAG, BUDDY_SDK_EXCEPTION + e);
            }
            isWaitingForResponse = false;
            if (startlisten) {
                buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getListeningAttempt() - 1);
                startCycle();
            }
        });
    }

    private void handleTTSErrorFailure(String text) {
        int delayTime = calculateDelayTime(text);
        handlerTTSError.postDelayed(() -> handleTTSErrorSuccess(), delayTime);
    }

    private int calculateDelayTime(String text) {
        int textLength = text.length();
        return (textLength / 20) * 1000;
    }

    private void handleChatbotsReturn(String message) {
        runOnUiThread(() -> {
            String action = message.split(SPLITER)[1];
            String value = message.split(SPLITER)[2];
            if (action.equals("translation_question") && !value.equals("OPERATION_FAILED")) {
                addTranslatedQuestionToChat(value);
            } else if (action.equals("speak")) {
                handleSpeakAction(message, value);
            }
        });
    }

    private void addTranslatedQuestionToChat(String value) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        Replica question = new Replica("Question traduction", time, value);
        listRep.add(question);
        listRepGlobale.add(question);
        updateChat();
    }

    private void handleSpeakAction(String message, String value) {
        int numberOfQuestion = Integer.parseInt(message.split(SPLITER)[3]);
        if (numberOfQuestion == buddyGPTApplication.getQuestionNumber() && !buddyGPTApplication.isTimeoutExpired()) {
            speak(value, "nothealysa");
        } else {
            buddyGPTApplication.setStoredResponse(value);
        }
    }

    private void handleConversationFinished() {
        runOnUiThread(() -> {
            isWaitingForResponse = false;
            buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getListeningAttempt() - 1);
            startCycle();
        });
    }

    private void handleMainDestroy() {
        buddyGPTApplication.setFileCreate(false);
        buddyGPTApplication.setparam("firstLaunch", "false");
    }

    private void handleGoogleResponseComplete() {
        buddyGPTApplication.notifyObservers("play google response");
    }

    private void handlePlayStoredResponse() {
        if (!buddyGPTApplication.getStoredResponse().isEmpty()) {
            runOnUiThread(() -> speak(buddyGPTApplication.getStoredResponse(), "storedResponse"));
        }
    }

    private void handleMakeBuddyFaceNeutral() {
        BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
    }

    private void handleMailSend() {
        runOnUiThread(() -> {
            String message = buddyGPTApplication.getParamFromFile("Message_mail_send_en", configFile);
            if (!message.trim().isEmpty()) {
                Toast.makeText(buddyGPTApplication, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleSessionIDChanged() {
        Replica session = new Replica(SESSION_TYPE, null, buddyGPTApplication.getparam("SelectedChatbot") + " - " + buddyGPTApplication.getModel());
        listRepGlobale.add(session);
        updateChat();
    }

    private void handleInvalidKey() {
        buddyGPTApplication.setparam(INVALID_KEY, "TRUE");
        showInvalidKeyDialog();
    }

    private void showInvalidKeyDialog() {
        String message = buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en);
        buddyGPTApplication.showInputDialog(ChatWindow.this, message, "Attention!");
    }

    private void handleInvalidDeviceID() {
        buddyGPTApplication.setparam(INVALID_DEVICE_ID, "TRUE");
        showInvalidDeviceIDDialog();
    }

    private void showInvalidDeviceIDDialog() {
        String message = buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en);
        buddyGPTApplication.showInputDialog(ChatWindow.this, message, "Attention!");
    }

    private void handleSessionIDError() {
        String message = buddyGPTApplication.getString(R.string.toast_teamgpt_params_invalid_en);
        buddyGPTApplication.showInputDialog2(ChatWindow.this, message, "Attention!");
    }

    private void handleErrorSending() {
        runOnUiThread(() -> {
            String message = buddyGPTApplication.getString(R.string.error_mail_toast_en);
            Toast.makeText(buddyGPTApplication, message, Toast.LENGTH_LONG).show();
        });
    }

    private void handleChangeDetected() {
        int speakVolume = buddyGPTApplication.getVolume();
        int max = buddyGPTApplication.getMaxVolume();
        int defaultVolume = buddyGPTApplication.getClosestInt((double) (speakVolume * 100) / max);
        buddyGPTApplication.setparam("speak_volume", String.valueOf(defaultVolume));
    }

    private void handleRestartListeningHotword() {
        resetListeningState();
        stopListeningFreeSpeech();
    }

    private void handleEndOfCycle() {
        runOnUiThread(() -> {
            buddyGPTApplication.setLed(NEUTRAL);
            microBtn.setImageResource(R.drawable.micro_off);
        });
    }

    private void handleRestartNewCycle() {
        handlerPauseTime.postDelayed(this::startNextCycle, 1000);
    }

    private void handleAudioTranscription(String message) {
        String shouldRestartNewCycle = message.split(SPLITER)[1];
        buddyGPTApplication.setLed(NEUTRAL);
        microBtn.setImageResource(R.drawable.micro_off);
        buddyGPTApplication.setAppIsListeningToTheQuestion(false);
        buddyGPTApplication.traitementAudio(shouldRestartNewCycle.equals("true"));
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

        microBtn.setImageResource(R.drawable.micro_on);

    }
    private void startCycle(){
        isListeningFreeSpeech = true;
        buddyGPTApplication.setOpenaialreadySwitchEmotion(false);
        buddyGPTApplication.setAppIsListeningToTheQuestion(true);

            if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(ANDROID_STT)){
                buddyGPTApplication.startListeningQuestion(this);

            }else if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(CERENCE_STT)){
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
                if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(ANDROID_STT) || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(CERENCE_STT)){
                    buddyGPTApplication.notifyObservers("end of cycle");
                    runnablePauseTime = () -> startNextCycle();
                    handlerPauseTime.postDelayed(runnablePauseTime,1000);
                }
                else{
                    buddyGPTApplication.notifyObservers("Obtain audio transcription after the listening time has elapsed;SPLIT;true");
                }
            }
        };
        timerEcoute.start();

        microBtn.setImageResource(R.drawable.micro_on);
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
        buddyGPTApplication.setLed(NEUTRAL);
        microBtn.setImageResource(R.drawable.micro_off);
    }


    /**
     * ------------------------------------------ TTS  -------------------------------------------
     */


    private void speak(final String texte, String type) {
        Log.d(TAG, " --- speak(" + texte + ") ---");
        if (responseTimeout != null) responseTimeout.cancel();

        if (type.equals("nothealysa") || type.equals("storedResponse")) {
            handleResponseSpeak(texte);
        } else if (type.equals("timeOutExpired")) {
            buddyGPTApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL, type);
        }
    }

    private void handleResponseSpeak(String texte) {
        buddyGPTApplication.setAlreadyGetAnswer(true);
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());

        if (!listRep.isEmpty()) {
            createNewResponseReplica(texte, time);
        } else {
            updateExistingResponseReplica(texte);
        }

        buddyGPTApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL, "nothealysa");
    }

    private void createNewResponseReplica(String texte, String time) {
        Replica reponse = new Replica();
        reponse.setValue(texte);
        reponse.setTime(time);
        reponse.setType(KEY_RESPONSE);
        reponse.setDuration(formatResponseTime());

        listRep.add(reponse);
        Session session = new Session(new ArrayList<>(listRep));
        buddyGPTApplication.getListSession().add(session);
        listRep.clear();
        listRepGlobale.add(reponse);

        updateAdapterData();
    }

    private void updateExistingResponseReplica(String texte) {
        Replica lastReplica = listRepGlobale.get(listRepGlobale.size() - 1);
        if (lastReplica.getType().equals(KEY_RESPONSE)) {
            if (responseFromTeamGPT != null && !responseFromTeamGPT.isError) {
                lastReplica.setValue(lastReplica.getValue() + texte);
            } else {
                lastReplica.setValue(texte);
            }
        }
        updateAdapterData();
    }

    private String formatResponseTime() {
        long responseTime = buddyGPTApplication.getResponseTime() - buddyGPTApplication.getQuestionTime();
        DecimalFormat df = new DecimalFormat("#,###");
        return df.format(responseTime) + " ms";
    }

    private void updateAdapterData() {
        Replica[] mDataset = listRepGlobale.toArray(new Replica[0]);
        adapter.setData(mDataset);
        scroll();
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