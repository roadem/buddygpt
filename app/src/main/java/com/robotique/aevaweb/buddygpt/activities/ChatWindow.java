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
import android.text.TextUtils;
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

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.GazePosition;
import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddy.utils.events.EventItem;
import com.bfr.buddy.utils.values.FloatingWidgetVisibility;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;
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
    private static final String NOTHEALYSA = "nothealysa";
    private static final String SESSION_TYPE = "Session";
    private static final String INVALID_DEVICE_ID = "INVALID_TEAMGPT_DEVICE_ID";
    private static final String INVALID_KEY = "INVALID_TEAMGPT_KEY";
    private static final String TEAMGPT_KEY = "TeamGPT_Key";
    private static final String ANDROID_STT = "Android";
    private static final String CERENCE_STT = "Cerence";
    private static final String NEUTRAL = "neutral";
    private static final String HOUR_PATTERN = "HH:mm:ss";
    private static final String SELECTED_CHATBOT = "SelectedChatbot";
    private BuddyGPTApplication buddyGPTApplication;
    private final Random random = new Random();
    private boolean onSdkReadyIsAlreadyCalledOnce = false;
    private boolean isListeningFreeSpeech = false;
    private boolean isWaitingForResponse = false;


    private ArrayList<Replica> listRep=new ArrayList<>();
    private final ArrayList<Replica> listRepGlobale=new ArrayList<>();
    private ReplicaListAdapter adapter;

    //timers
    private CountDownTimer timerEcoute;
    private CountDownTimer responseTimeout;

    //views
    private RelativeLayout popupAddMail;
    private LinearLayout popupAddMailContent;
    private RelativeLayout lytCloseMenuChat;
    private RelativeLayout parentChat;
    private ImageView microBtn;
    private ImageView sendBtn;
    private ImageView sendBtn2;
    private ImageView btnClearConversation;
    private RecyclerView recyclerView;
    private ScrollView scrollView;
    private TextView textEmail;
    private EditText editTextEmail;
    int click=1;
    boolean startlisten=true;
    MailSender smtpService;
    private ResponseFromTeamGPT responseFromTeamGPT;
    private static final String langueFr = "Français";
    private static final String langueEn = "Anglais";
    private static final String langueEs = "Espagnol";
    private static final String langueDe = "Allemand";
    private static final Handler handlerTTSError = new Handler();
    private Runnable runnableTTSError;
    private static final String configFile ="BuddyGPT.properties";
    private boolean isClickedBtnCloseChat=false;
    String[] newSessionText = new String[1];
    String[] responseText = new String[1];
    String[] qstText = new String[1];
    private static final Handler handlerPauseTime = new Handler();
    private Runnable runnablePauseTime;

    //-----------------------------Cycle de vie de l'activité--------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_window);
        Log.d(TAG, " --- onCreate() ---");

        initializeApplication();
        configureSystemUI();
        initializeResponseHandler();
        initializeViews();
        setupClickListeners();
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

        // Annule le timer de délai de réponse s'il est actif
        if (responseTimeout != null) responseTimeout.cancel();

        // Supprime les callbacks liés aux erreurs TTS s'ils existent
        if (runnableTTSError != null) {
            handlerTTSError.removeCallbacks(runnableTTSError);
            handlerTTSError.removeCallbacksAndMessages(null); // Supprime tous les messages restants
        }

        // Réinitialise l’état de démarrage du SDK et l'attente de réponse
        onSdkReadyIsAlreadyCalledOnce = false;
        isWaitingForResponse = false;

        // Réinitialise l'état d'écoute vocale et vide la liste des réponses
        startlisten = true;
        listRep = new ArrayList<>();

        // Arrête la synthèse vocale (TTS)
        buddyGPTApplication.stopTTS();

        // Tente de remettre l'expression faciale du robot à un état neutre
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
        } catch (Exception e) {
            Log.e(TAG, BUDDY_SDK_EXCEPTION + e);
        }

        // Arrête l'écoute vocale (reconnaissance libre)
        stopListeningFreeSpeech();

        // Retire cette activité des observateurs de l'application
        buddyGPTApplication.removeObserver(this);
    }

    @Override
    protected void onDestroy() {
        // Vérifie si les préférences initiales ne sont pas définies
        if (Boolean.FALSE.equals(buddyGPTApplication.getInitSharedpreferences())) {
            buddyGPTApplication.setparam("firstLaunch", "true");
            buddyGPTApplication.notifyObservers("ChatDestroy");
        }
            if(buddyGPTApplication.getDialog() != null && buddyGPTApplication.getDialog().isShowing()) buddyGPTApplication.getDialog().dismiss();

        Log.d(TAG, " --- onDestroy() ---");

        // Appel à la méthode parent pour libérer les ressources système
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

    // -------------------------------------Initialisation---------------------------------------

    private void initializeApplication() {
        buddyGPTApplication = (BuddyGPTApplication) getApplicationContext();
        buddyGPTApplication.setInitSharedpreferences(false);
    }

    private void configureSystemUI() {
        int uiFlags = buddyGPTApplication.hideSystemUI(this);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiFlags);
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            if (visibility == View.SYSTEM_UI_FLAG_VISIBLE)
                decorView.setSystemUiVisibility(uiFlags);
        });
    }

    private void initializeResponseHandler() {
        if (responseFromTeamGPT != null) {
            responseFromTeamGPT = null; // libère explicitement avant recréation (évite reset inutile)
        }
        responseFromTeamGPT = new ResponseFromTeamGPT(buddyGPTApplication);
    }

    private void initializeViews() {
        popupAddMail = findViewById(R.id.popup_add_mail);
        parentChat = findViewById(R.id.parent_chat);
        microBtn = findViewById(R.id.micro_btn);
        sendBtn = findViewById(R.id.send_btn);
        sendBtn2 = findViewById(R.id.send_btn2);
        btnClearConversation = findViewById(R.id.clear_btn);
        scrollView = findViewById(R.id.scrollview);
        lytCloseMenuChat = findViewById(R.id.lyt_close_menu_chat);
        recyclerView = findViewById(R.id.chatRecyclerView);
        editTextEmail = findViewById(R.id.editTextEmail);
        textEmail = findViewById(R.id.popup_add_mail_textView);
        popupAddMailContent = findViewById(R.id.popup_add_mail_linearLayout);
        setAddMailDestinationText();
    }

    private void setupClickListeners() {
        microBtn.setOnClickListener(v -> onClickMicro());
        sendBtn.setOnClickListener(v -> onClickSend());
        sendBtn2.setOnClickListener(v -> onClickSendFromPopup());
        btnClearConversation.setOnClickListener(v -> onClickClearConversation());
        lytCloseMenuChat.setOnClickListener(v -> btnCloseChat());
    }

    private void init() {
        click = 1;

        // Initialisation des paramètres utilisateur
        initSettings();

        // Gestion des popups d'ajout d'email
        initPopupHandlers();

        // Initialisation du champ email
        initEmailField();

        // Initialisation de la RecyclerView
        adapter = new ReplicaListAdapter(buddyGPTApplication, initDataset());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Activation du scroll automatique
        scroll();
    }

    /** Initialise les paramètres depuis le stockage local. */
    private void initSettings() {
        Setting settingClass;
        settingClass = new Setting();
        settingClass.setDuration(buddyGPTApplication.getparam("listening_duration"));
        settingClass.setAttempt(buddyGPTApplication.getparam("listening_attempt"));
        settingClass.setLangue(buddyGPTApplication.getLangue().getNom());
        settingClass.setVolume(buddyGPTApplication.getparam("speak_volume"));
        settingClass.setSwitchVisibility(buddyGPTApplication.getparam("switch_visibility"));
        buddyGPTApplication.refresh(new Gson().fromJson(buddyGPTApplication.getparam(settingClass.getLangue()), Langue.class).getLanguageCode(),this);
    }

    /** Gère l'affichage et la fermeture du popup d'ajout d'email. */
    private void initPopupHandlers() {
        popupAddMail.setOnClickListener(v -> {
            if (popupAddMail.getVisibility() == View.VISIBLE) {
                popupAddMail.setVisibility(View.INVISIBLE); // simplification
            }
        });

        popupAddMailContent.setOnClickListener(v -> {
            // Empêche la propagation du clic
        });
    }

    /** Initialise le champ de saisie de l'email et ses écouteurs. */
    private void initEmailField() {
        editTextEmail.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);
        editTextEmail.setText(buddyGPTApplication.getparam(MAIL_DESTINATION_KEY));

        editTextEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                buddyGPTApplication.setparam(MAIL_DESTINATION_KEY, charSequence.toString());
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Nothing to do
                }
            @Override public void afterTextChanged(Editable s) {
                //nothing to do
            }
        });

        editTextEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                enableImmersiveMode();
            } else {
                buddyGPTApplication.hideSystemUI(ChatWindow.this);
                if (editTextEmail.getText().toString().trim().isEmpty()) {
                    String email = buddyGPTApplication.getparam("Email");
                    buddyGPTApplication.setparam(MAIL_DESTINATION_KEY, email);
                    editTextEmail.setText(email);
                }
            }
        });
    }

    /**
     *  Active le mode immersif (plein écran sans barre système).
     */
    private void enableImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    /**
     * Récupération des questions/réponses depuis les préférences partagées au format JSON.
     * Transforme chaque paire question/réponse en objet Replica et les ajoute à listRepGlobale.
     *
     * @return Tableau de Replica contenant toutes les entrées extraites.
     */
    private Replica[] initDataset() {

        // Récupérer la chaîne JSON des messages
        String jsonString = buddyGPTApplication.getparam("messages");

        if (TextUtils.isEmpty(jsonString)) {
            Log.w(TAG, "initDataset: chaîne JSON vide ou nulle.");
            return new Replica[0]; // Retourner un tableau vide en cas de données absentes
        }

        try {
            JSONArray messagesArray = new JSONArray(jsonString);

            for (int i = 0; i < messagesArray.length(); i++) {
                JSONObject messageObject = messagesArray.getJSONObject(i);
                Replica replica = new Replica();

                // Gestion du type QUESTION
                if (messageObject.has(KEY_QUESTION)) {
                    replica.setType(KEY_QUESTION);
                    replica.setValue(messageObject.getString(KEY_QUESTION));
                }

                // Gestion du type RESPONSE
                if (messageObject.has(KEY_RESPONSE)) {
                    String[] responseParts = messageObject.getString(KEY_RESPONSE).split(SPLITER);
                    if (responseParts.length >= 2) {
                        replica.setType(KEY_RESPONSE);
                        replica.setValue(responseParts[0]);
                        replica.setDuration(responseParts[1]);
                    } else {
                        Log.w(TAG, "initDataset: format de réponse inattendu à l'index " + i);
                    }
                }

                // Gestion du type SESSION_TYPE
                if (messageObject.has(SESSION_TYPE)) {
                    replica.setType(SESSION_TYPE);
                    replica.setValue(messageObject.getString(SESSION_TYPE));
                }

                // Ajouter la réplique à la liste globale
                listRepGlobale.add(replica);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Erreur lors du parsing JSON dans initDataset()", e);
            return new Replica[0]; // Éviter de continuer avec des données corrompues
        }

        // Convertir la liste en tableau et retourner
        return listRepGlobale.toArray(new Replica[0]);
    }
// -------------------------------Gestion des événements UI--------------------------

    /**
     * Initialisation des composants de l'activité : paramètres, interface, écouteurs.
     */






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
    public void onClickMicro() {
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
                && !buddyGPTApplication.getparam(TEAMGPT_KEY).equalsIgnoreCase("");
    }

    private void notifyInvalidTeamGPTDeviceId() {
        Log.i("TAG", "run: notifyObservers INVALID_TEAMGPT_DEVICE_ID 3");
        buddyGPTApplication.notifyObservers(INVALID_DEVICE_ID);
    }

    private boolean isInvalidTeamGPTKey() {
        return buddyGPTApplication.getparam(INVALID_KEY).equalsIgnoreCase("TRUE")
                && !buddyGPTApplication.getparam(TEAMGPT_KEY).equalsIgnoreCase("");
    }

    private void notifyInvalidTeamGPTKey() {
        Log.i("TAG", "run: notifyObservers INVALID_TEAMGPT_KEY 3");
        buddyGPTApplication.notifyObservers(INVALID_KEY);
    }

    private boolean isTeamGPTKeyEmpty() {
        return buddyGPTApplication.getparam(TEAMGPT_KEY).equalsIgnoreCase("");
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
        buddyGPTApplication.traitementAudio();
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
                        Log.e(TAG, "translatedText exception  __setAddMailDestinationText__" + e);
                        textEmail.setText(buddyGPTApplication.getString(R.string.destination_mail_texte));
                    });

            buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getString(R.string.destination_mail_Edittexte))
                    .addOnSuccessListener(translatedText -> editTextEmail.setHint(translatedText))
                            .addOnFailureListener(e -> {
                    Log.e(TAG,"translatedText exception  __setAddMailDestinationText__"+e);
                    editTextEmail.setHint(buddyGPTApplication.getString(R.string.destination_mail_Edittexte));
            });
        }
    }
    /**
     * Gestion du clic sur l'icone ClearCoonversation
     */
    public void onClickClearConversation(){
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
    public void onClickSend(){
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
    public void onClickSendFromPopup(){

            if (!buddyGPTApplication.getparam(MAIL_DESTINATION_KEY).trim().isEmpty()){
                if(buddyGPTApplication.getLangue().getNom().equals(langueEn))
                    writeMail( emailContent -> {
                            smtpService = new MailSender(ChatWindow.this,emailContent, buddyGPTApplication.getparam(MAIL_DESTINATION_KEY), buddyGPTApplication.getparam("Mail_Subject_en"));
                            smtpService.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                    });


                else if(buddyGPTApplication.getLangue().getNom().equals(langueFr)){
                    writeMail(emailContent -> {

                    smtpService = new MailSender(ChatWindow.this,emailContent, buddyGPTApplication.getparam(MAIL_DESTINATION_KEY), buddyGPTApplication.getparam("Mail_Subject_fr"));
                        smtpService.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                });

            }else{
                    final Activity activity = ChatWindow.this;
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getparam("Mail_Subject_en"))
                            .addOnSuccessListener( translatedText ->
                            writeMail(emailContent -> {
                                    smtpService = new MailSender(activity,emailContent, buddyGPTApplication.getparam(MAIL_DESTINATION_KEY), translatedText);
                                smtpService.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            })

                    ).addOnFailureListener( e ->
                            Log.e(TAG,"translatedText exception __onClickSendFromPopup__"+e)
                    );
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
                            .addOnSuccessListener(translatedText -> Toast.makeText(buddyGPTApplication, translatedText, Toast.LENGTH_LONG).show()).addOnFailureListener(e -> Log.e(TAG,"translatedText exception __onClickSendFromPopup__ "+e));
                }
            }

    }

    //----------------------Gestion du chat et mail--------------------------


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
        Log.i(TAG, "buildEmailContent: "+newSessionText[0]+" - "+responseText[0]+" - "+qstText[0]);
        if (!buddyGPTApplication.getparam(SELECTED_CHATBOT).equalsIgnoreCase("") ||
                !buddyGPTApplication.getparam("NomCompte").equalsIgnoreCase("")) {
            firstLine = buddyGPTApplication.getparam("NomCompte") + " " +
                    buddyGPTApplication.getparam(SELECTED_CHATBOT) + " " +
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
    public void btnCloseChat() {
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
            overridePendingTransition(0, 0);
        }

    }

    /**
     * ------------------------------------------ Gestion de notifications --------------------------
     */

    public void update(String message) {
        if(message != null){

            if (message.contains("CANCEL_RESPONSE_TIMEOUT") && responseTimeout != null) responseTimeout.cancel();


            if (message.contains("MODE_STREAM_SPEAK;SPLIT;")) {
                runOnUiThread(() -> {
                    if (message.split(SPLITER).length > 1) {
                        String phraseToPronounce = message.split(SPLITER)[1];
                        speak(phraseToPronounce, NOTHEALYSA);
                    }
                });
            }

            if (message.contains("STTQuestion_success")) {
                runOnUiThread(() -> {
                    buddyGPTApplication.setAppIsListeningToTheQuestion(false);
                    isWaitingForResponse = true;

                    stopListeningFreeSpeech();

                    String detectedSTTMessage = message.split(";")[1].replace("' ","'");

                    buddyGPTApplication.setQuestionNumber(buddyGPTApplication.getCurrentQuestionNubmer() + 1);
                    String time = new SimpleDateFormat(HOUR_PATTERN).format(new Date());
                    Replica question = new Replica();
                    question.setType(KEY_QUESTION);
                    question.setTime(time);
                    question.setValue(detectedSTTMessage);
                    listRep.add(question);
                    listRepGlobale.add(question);
                    updateChat();
                    buddyGPTApplication.setActivityClosed(false);

                    if (buddyGPTApplication.getResponseFromTeamGPT() != null)
                        buddyGPTApplication.getResponseFromTeamGPT().reset();

                    if (buddyGPTApplication.getResponseFromTeamGPT() == null)
                        buddyGPTApplication.setResponseFromTeamGPT(new ResponseFromTeamGPT(buddyGPTApplication));

                    buddyGPTApplication.getResponseFromTeamGPT().sendPutRequestStream(detectedSTTMessage, buddyGPTApplication.getQuestionNumber());

                    if ((Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds", configFile)) != 0)
                            && ((
                            buddyGPTApplication.getCurrentLanguage().equals("en")
                                    && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en", configFile).trim().isEmpty()
                    )
                            || (
                            buddyGPTApplication.getCurrentLanguage().equals("fr")
                                    && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_fr", configFile).trim().isEmpty()
                    )
                            || (
                            buddyGPTApplication.getCurrentLanguage().equals("es")
                                    && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_es", configFile).trim().isEmpty()
                    )
                            || (
                            buddyGPTApplication.getCurrentLanguage().equals("de")
                                    && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_de", configFile).trim().isEmpty()
                    )
                            || (
                            !buddyGPTApplication.getCurrentLanguage().equals("en")
                                    && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en", configFile).trim().isEmpty()
                    )
                    )) {
                        runOnUiThread(() -> {
                            buddyGPTApplication.setAnswerHasExceededTimeOut(false);
                            responseTimeout = new CountDownTimer((long) Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds", configFile)) * 1000, 1000) {
                                @Override
                                public void onTick(long l) {
                                    // on tick code
                                }

                                @Override
                                public void onFinish() {
                                    if (!buddyGPTApplication.isAlreadyGetAnswer()) {
                                        buddyGPTApplication.setAnswerHasExceededTimeOut(true);
                                        buddyGPTApplication.setTimeoutExpired(true);
                                        String messageToSpeak = null;
                                        switch (buddyGPTApplication.getCurrentLanguage()) {
                                            case "en":
                                                messageToSpeak = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en", configFile);
                                                break;
                                            case "fr":
                                                messageToSpeak = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_fr", configFile);
                                                break;
                                            case "es":
                                                messageToSpeak = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_es", configFile);
                                                break;
                                            case "de":
                                                messageToSpeak = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_de", configFile);
                                                break;
                                            default:
                                                messageToSpeak = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en", configFile);
                                                break;
                                        }
                                        String[] messages = messageToSpeak.split("/");
                                        int randomNumber = random.nextInt(messages.length);
                                        speak(messages[randomNumber], "timeOutExpired");
                                    }
                                }
                            };
                            responseTimeout.start();
                        });
                    }
                });
            }

            else if (message.contains("TTS_success")) {
                runOnUiThread(() -> {
                    isWaitingForResponse = false;
                    if (startlisten) {
                        buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getListeningAttempt() - 1);
                        startCycle();
                    }
                });
            }

            else if (message.contains("TTS_error") || message.contains("TTS_exception")) {
                runOnUiThread(() -> {
                    String text = message.split(";")[1];

                    Log.w(TAG, "TTS_ERROR:" + text);

                    buddyGPTApplication.playUsingReadSpeakerCaseError(text, new ITTSCallbacks() {
                        @Override
                        public void onSuccess(String s) {
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

                        @Override
                        public void onError(String s) {
                            int textLength = text.length();
                            int delayTime = (textLength / 20) * 1000; // 1 second for every 20 characters
                            if (buddyGPTApplication.getparam("TTS").equalsIgnoreCase("ReadSpeaker") &&
                                    (buddyGPTApplication.getCurrentLanguage().equals("en") || buddyGPTApplication.getCurrentLanguage().equals("fr")) &&
                                    Boolean.TRUE.equals(buddyGPTApplication.getUsingReadSpeaker())) {
                                delayTime = 0;
                            }
                            handlerTTSError.postDelayed(runnableTTSError = () -> runOnUiThread(() -> {
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
                            }), delayTime);
                        }
                    });
                });
            }




            else if (message.contains("main destroy")) {
                buddyGPTApplication.setFileCreate(false);
                buddyGPTApplication.setparam("firstLaunch", "false");
            }
            else if (message.contains("getResponseF;SPLIT;chatbot;SPLIT;response google complete")) {
                buddyGPTApplication.notifyObservers("play google response");
            }
            else if (message.contains("playStoredResponse")) {
                if (!buddyGPTApplication.getStoredResponse().equals("")) {
                    runOnUiThread(() -> speak(buddyGPTApplication.getStoredResponse(), "storedResponse"));
                }
            }
            else if (message.contains("makeBuddyFaceNeutral")) {
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
            }
            else if (message.contains("mailSend")) {
                runOnUiThread(() -> {
                    if (buddyGPTApplication.getLangue().getNom().equals(langueEn) && !buddyGPTApplication.getParamFromFile("Message_mail_send_en", configFile).trim().isEmpty()) {
                            speak(buddyGPTApplication.getParamFromFile("Message_mail_send_en", configFile), "mailSent");
                        }

                });
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
        timerEcoute = new CountDownTimer(duration * 1000L,1000) {
            @Override
            public void onTick(long l) {
                Log.d(TAG, "timerEcoute onTick");
            }
            @Override
            public void onFinish() {

                if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(ANDROID_STT) || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(CERENCE_STT)){
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

        if (type.equals(NOTHEALYSA) || type.equals("storedResponse")) {
            handleResponseSpeak(texte);
        } else if (type.equals("timeOutExpired")) {
            buddyGPTApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL, type);
        }
    }

    private void handleResponseSpeak(String texte) {
        buddyGPTApplication.setAlreadyGetAnswer(true);
        String time = new SimpleDateFormat(HOUR_PATTERN).format(new Date());

        if (!listRep.isEmpty()) {
            createNewResponseReplica(texte, time);
        } else {
            updateExistingResponseReplica(texte);
        }

        buddyGPTApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL, NOTHEALYSA);
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