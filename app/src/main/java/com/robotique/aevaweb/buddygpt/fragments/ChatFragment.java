package com.robotique.aevaweb.buddygpt.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.LabialExpression;
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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment implements IDBObserver {


    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public ChatFragment() {
        // Required empty public constructor
    }

    private static final String TAG = "BuddyGPT_ChatFragment";
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
    private static final String langueFr = "Français";
    private static final String langueEn = "Anglais";
    private static final String langueEs = "Espagnol";
    private static final String langueDe = "Allemand";
    private static final Handler handlerTTSError = new Handler();
    private static final String configFile = "BuddyGPT.properties";
    private static final Handler handlerPauseTime = new Handler();
    private final Random random = new Random();
    private final ArrayList<Replica> listRepGlobale = new ArrayList<>();
    int click = 1;
    boolean startlisten = true;
    MailSender smtpService;
    String[] newSessionText = new String[1];
    String[] responseText = new String[1];
    String[] qstText = new String[1];
    private BuddyGPTApplication buddyGPTApplication;
    private boolean isListeningFreeSpeech = false;
    private boolean isWaitingForResponse = false;
    private ArrayList<Replica> listRep = new ArrayList<>();
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
    private ResponseFromTeamGPT responseFromTeamGPT;
    private Runnable runnableTTSError;
    private boolean isClickedBtnCloseChat = false;
    private Runnable runnablePauseTime;


    /**
     * fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ChatFragment.
     */
    public static ChatFragment newInstance(String param1, String param2) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        buddyGPTApplication = (BuddyGPTApplication) getActivity().getApplicationContext();
        buddyGPTApplication.registerObserver(this);
        buddyGPTApplication.hideSystemUI(getActivity());
        initializeApplication();
        configureSystemUI();
        initializeResponseHandler();
        initializeViews(view);
        setupClickListeners();
        init();
        return view;
    }

    @Override
    public void onDestroyView() {
        // Annule le timer de délai de réponse s'il est actif
        if (responseTimeout != null) responseTimeout.cancel();

        // Supprime les callbacks liés aux erreurs TTS s'ils existent
        if (runnableTTSError != null) {
            handlerTTSError.removeCallbacks(runnableTTSError);
            handlerTTSError.removeCallbacksAndMessages(null); // Supprime tous les messages restants
        }

        // Réinitialise l’état de démarrage du SDK et l'attente de réponse
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
        // Vérifie si les préférences initiales ne sont pas définies
        if (Boolean.FALSE.equals(buddyGPTApplication.getInitSharedpreferences())) {
            buddyGPTApplication.setparam("firstLaunch", "true");
            buddyGPTApplication.notifyObservers("ChatDestroy");
        }
        if (buddyGPTApplication.getDialog() != null && buddyGPTApplication.getDialog().isShowing())
            buddyGPTApplication.getDialog().dismiss();

        super.onDestroyView();
    }

    // -------------------------------------Initialisation---------------------------------------

    private void initializeApplication() {
        buddyGPTApplication = (BuddyGPTApplication) getActivity().getApplicationContext();
        buddyGPTApplication.setInitSharedpreferences(false);
    }

    private void configureSystemUI() {
        int uiFlags = buddyGPTApplication.hideSystemUI(getActivity());
        View decorView = getActivity().getWindow().getDecorView();
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

    private void initializeViews(View view) {
        popupAddMail = view.findViewById(R.id.popup_add_mail);
        parentChat = view.findViewById(R.id.parent_chat);
        microBtn = view.findViewById(R.id.micro_btn);
        sendBtn = view.findViewById(R.id.send_btn);
        sendBtn2 = view.findViewById(R.id.send_btn2);
        btnClearConversation = view.findViewById(R.id.clear_btn);
        scrollView = view.findViewById(R.id.scrollview);
        lytCloseMenuChat = view.findViewById(R.id.lyt_close_menu_chat);
        recyclerView = view.findViewById(R.id.chatRecyclerView);
        editTextEmail = view.findViewById(R.id.editTextEmail);
        textEmail = view.findViewById(R.id.popup_add_mail_textView);
        popupAddMailContent = view.findViewById(R.id.popup_add_mail_linearLayout);
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
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        recyclerView.setAdapter(adapter);

        // Activation du scroll automatique
        scroll();
    }


    /**
     * Initialise les paramètres depuis le stockage local.
     */
    private void initSettings() {
        Setting settingClass;
        settingClass = new Setting();
        settingClass.setDuration(buddyGPTApplication.getparam("listening_duration"));
        settingClass.setAttempt(buddyGPTApplication.getparam("listening_attempt"));
        settingClass.setLangue(buddyGPTApplication.getLangue().getNom());
        settingClass.setVolume(buddyGPTApplication.getparam("speak_volume"));
        settingClass.setSwitchVisibility(buddyGPTApplication.getparam("switch_visibility"));
        buddyGPTApplication.refresh(new Gson().fromJson(buddyGPTApplication.getparam(settingClass.getLangue()), Langue.class).getLanguageCode(), getActivity());
    }

    /**
     * Gère l'affichage et la fermeture du popup d'ajout d'email.
     */
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

    /**
     * Initialise le champ de saisie de l'email et ses écouteurs.
     */
    private void initEmailField() {
        editTextEmail.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);
        editTextEmail.setText(buddyGPTApplication.getparam(MAIL_DESTINATION_KEY));

        editTextEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                buddyGPTApplication.setparam(MAIL_DESTINATION_KEY, charSequence.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Nothing to do
            }

            @Override
            public void afterTextChanged(Editable s) {
                //nothing to do
            }
        });

        editTextEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                enableImmersiveMode();
            } else {
                buddyGPTApplication.hideSystemUI(getActivity());
                if (editTextEmail.getText().toString().trim().isEmpty()) {
                    String email = buddyGPTApplication.getparam("Email");
                    buddyGPTApplication.setparam(MAIL_DESTINATION_KEY, email);
                    editTextEmail.setText(email);
                }
            }
        });
    }

    /**
     * Active le mode immersif (plein écran sans barre système).
     */
    private void enableImmersiveMode() {
        getActivity().getWindow().getDecorView().setSystemUiVisibility(
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

                // Ajouter les questions et réponses comme des objets Replica
                if (!listRepGlobale.isEmpty() && replica.getType().equalsIgnoreCase(SESSION_TYPE)) {
                    listRepGlobale.add(listRepGlobale.size() - 1, replica);
                } else
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
    private void scroll() {
        recyclerView.post(() -> recyclerView.smoothScrollToPosition(adapter.getItemCount() + 5));
        scrollView.post(() -> scrollView.fullScroll(android.view.View.FOCUS_DOWN));

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
        } else if (isEnvError()) {
            Log.i(TAG, "run: notifyObservers ENV_ERROR 3");
            buddyGPTApplication.notifyObservers("ENV_ERROR");
        } else if (isTeamGPTKeyEmpty()) {
            handleEmptyTeamGPTKey();
        } else {
            handleMicroClick();
        }
    }

    private void resetTimeoutsAndHandlers() {
        if (responseTimeout != null) responseTimeout.cancel();
        if (buddyGPTApplication.getResponseFromTeamGPT() != null)
            buddyGPTApplication.getResponseFromTeamGPT().reset();
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

    private boolean isEnvError() {
        return buddyGPTApplication.getparam("ENV_ERROR").equalsIgnoreCase("TRUE")
                && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("");
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

    private void setAddMailDestinationText() {
        if (buddyGPTApplication.getLangue().getNom().equals(langueEn)) {
            textEmail.setText(buddyGPTApplication.getString(R.string.destination_mail_texte));
            editTextEmail.setHint(buddyGPTApplication.getString(R.string.destination_mail_Edittexte));
        } else if (buddyGPTApplication.getLangue().getNom().equals(langueFr)) {
            textEmail.setText(buddyGPTApplication.getString(R.string.destination_mail_texte_fr));
            editTextEmail.setHint(buddyGPTApplication.getString(R.string.destination_mail_Edittexte_fr));
        } else {
            buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getString(R.string.destination_mail_texte))
                    .addOnSuccessListener(translatedText -> textEmail.setText(translatedText))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "translatedText exception  __setAddMailDestinationText__" + e);
                        textEmail.setText(buddyGPTApplication.getString(R.string.destination_mail_texte));
                    });

            buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getString(R.string.destination_mail_Edittexte))
                    .addOnSuccessListener(translatedText -> editTextEmail.setHint(translatedText))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "translatedText exception  __setAddMailDestinationText__" + e);
                        editTextEmail.setHint(buddyGPTApplication.getString(R.string.destination_mail_Edittexte));
                    });
        }
    }

    /**
     * Gestion du clic sur l'icone ClearCoonversation
     */
    public void onClickClearConversation() {
        buddyGPTApplication.listSessionClear();
        buddyGPTApplication.setparam("messages", "[]");
        listRep.clear();
        listRepGlobale.clear();
        adapter = new ReplicaListAdapter(buddyGPTApplication, initDataset());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);


    }

    private void showTranslatedToast(Integer resIdEn, Integer resIdFr, Integer resIdEs, Integer resIdDe) {
        String langue = buddyGPTApplication.getLangue().getNom();
        String message = null;

        try {
            if (langue.equals(langueEn) && resIdEn != null) {
                message = getString(resIdEn);
            } else if (langue.equals(langueFr) && resIdFr != null) {
                message = getString(resIdFr);
            } else if (langue.equals(langueEs) && resIdEs != null) {
                message = getString(resIdEs);
            } else if (langue.equals(langueDe) && resIdDe != null) {
                message = getString(resIdDe);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération de la ressource string", e);
        }

        if (!TextUtils.isEmpty(message)) {
            buddyGPTApplication.showToast(message);
        } else {
            // Fallback: traduction dynamique ou message anglais par défaut
            String fallback = getString(resIdEn != null ? resIdEn : R.string.app_name);
            buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                    .translate(fallback)
                    .addOnSuccessListener(translatedText -> buddyGPTApplication.showToast(translatedText))
                    .addOnFailureListener(e -> buddyGPTApplication.showToast(fallback));
        }
    }

    /**
     * Gestion du clic sur l'icone Send
     */
    public void onClickSend() {
        if (listRepGlobale.isEmpty()) {
            showTranslatedToast(
                    R.string.no_message_to_send_en,
                    R.string.no_message_to_send_fr,
                    null,
                    null
            );
        } else
            popupAddMail.setVisibility(View.VISIBLE);

    }

    /**
     * Gestion du clic sur l'icone Send depuis le popUP
     */
    @SuppressLint("SuspiciousIndentation")
    public void onClickSendFromPopup() {

        if (!buddyGPTApplication.getparam(MAIL_DESTINATION_KEY).trim().isEmpty()) {
            if (buddyGPTApplication.getLangue().getNom().equals(langueEn))
                writeMail(emailContent -> {
                    smtpService = new MailSender(getActivity(), emailContent, buddyGPTApplication.getparam(MAIL_DESTINATION_KEY), buddyGPTApplication.getparam("Mail_Subject_en"));
                    smtpService.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                });


            else if (buddyGPTApplication.getLangue().getNom().equals(langueFr)) {
                writeMail(emailContent -> {

                    smtpService = new MailSender(getActivity(), emailContent, buddyGPTApplication.getparam(MAIL_DESTINATION_KEY), buddyGPTApplication.getparam("Mail_Subject_fr"));
                    smtpService.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                });

            } else {
                final Activity activity = getActivity();
                buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getparam("Mail_Subject_en"))
                        .addOnSuccessListener(translatedText ->
                                writeMail(emailContent -> {
                                    smtpService = new MailSender(activity, emailContent, buddyGPTApplication.getparam(MAIL_DESTINATION_KEY), translatedText);
                                    smtpService.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                })

                        ).addOnFailureListener(e ->
                                Log.e(TAG, "translatedText exception __onClickSendFromPopup__" + e)
                        );
            }
            popupAddMail.setVisibility(View.INVISIBLE);

        } else {
            if (buddyGPTApplication.getLangue().getNom().equals(langueEn)) {
                Toast.makeText(buddyGPTApplication, buddyGPTApplication.getString(R.string.add_mail_toast_en), Toast.LENGTH_LONG).show();
            } else if (buddyGPTApplication.getLangue().getNom().equals(langueFr)) {
                Toast.makeText(buddyGPTApplication, buddyGPTApplication.getString(R.string.add_mail_toast_fr), Toast.LENGTH_LONG).show();
            } else if (buddyGPTApplication.getLangue().getNom().equals(langueEs)) {
                Toast.makeText(buddyGPTApplication, buddyGPTApplication.getString(R.string.add_mail_toast_es), Toast.LENGTH_LONG).show();
            } else if (buddyGPTApplication.getLangue().getNom().equals(langueDe)) {
                Toast.makeText(buddyGPTApplication, buddyGPTApplication.getString(R.string.add_mail_toast_de), Toast.LENGTH_LONG).show();
            } else {
                buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getString(R.string.add_mail_toast_en))
                        .addOnSuccessListener(translatedText -> Toast.makeText(buddyGPTApplication, translatedText, Toast.LENGTH_LONG).show()).addOnFailureListener(e -> Log.e(TAG, "translatedText exception __onClickSendFromPopup__ " + e));
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
        Log.i(TAG, "buildEmailContent: " + newSessionText[0] + " - " + responseText[0] + " - " + qstText[0]);
        if (!buddyGPTApplication.getparam(SELECTED_CHATBOT).equalsIgnoreCase("") ||
                !buddyGPTApplication.getparam("NomCompte").equalsIgnoreCase("")) {
            firstLine = buddyGPTApplication.getparam("NomCompte") + " " +
                    buddyGPTApplication.getparam(SELECTED_CHATBOT) + " " +
                    buddyGPTApplication.getparam("chatbotModel") + "<br>";
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
        Log.i(TAG, "buildEmailContent: test" + question);
        return question.toString();
    }

    /**
     * Fermeture de la fenetre de discussion
     */
    public void btnCloseChat() {
        if (!isClickedBtnCloseChat) {
            isClickedBtnCloseChat = true;
            buddyGPTApplication.stopTTS();

            try {
                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
            } catch (Exception e) {
                Log.e(TAG, BUDDY_SDK_EXCEPTION + e);
            }

            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MainFragment())
                    .commit();
            getActivity().overridePendingTransition(0, 0);
        }

    }

    /**
     * ------------------------------------------ Gestion de notifications --------------------------
     */

    @Override
    public void update(String message) {
        if (message != null) {

            if (message.contains("CANCEL_RESPONSE_TIMEOUT") && responseTimeout != null) {
                responseTimeout.cancel();
            }
            if (message.contains("MODE_STREAM_SPEAK;SPLIT;")) {
                Log.i("HOU", "update: MODE_STREAM_SPEAK");
                getActivity().runOnUiThread(() -> {
                    if (message.split(";SPLIT;").length > 1) {
                        String phraseToPronounce = message.split(";SPLIT;")[1];
                        speak(phraseToPronounce, "nothealysa");
                    }
                });
            }
            if (message.contains("STTQuestion_success")) {
                getActivity().runOnUiThread(() -> {
                    buddyGPTApplication.setAppIsListeningToTheQuestion(false);
                    isWaitingForResponse = true;

                    stopListeningFreeSpeech();

                    String detectedSTTMessage = message.split(";")[1].replaceAll("' ", "'");


                    buddyGPTApplication.setQuestionNumber(buddyGPTApplication.getCurrentQuestionNubmer() + 1);
                    String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                    Replica question = new Replica();
                    question.setType("Question");
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
                    buddyGPTApplication.getResponseFromTeamGPT().sendPutRequestStream(detectedSTTMessage);
                    if (
                            (Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds", configFile)) != 0)
                                    && (
                                    (
                                            buddyGPTApplication.getCurrentLanguage().equals("en")
                                                    && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en", configFile).trim().isEmpty()
                                    )
                                            ||
                                            (
                                                    buddyGPTApplication.getCurrentLanguage().equals("fr")
                                                            && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_fr", configFile).trim().isEmpty()
                                            )
                                            ||
                                            (
                                                    buddyGPTApplication.getCurrentLanguage().equals("es")
                                                            && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_es", configFile).trim().isEmpty()
                                            )
                                            ||
                                            (
                                                    buddyGPTApplication.getCurrentLanguage().equals("de")
                                                            && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_de", configFile).trim().isEmpty()
                                            )
                                            || (
                                            !buddyGPTApplication.getCurrentLanguage().equals("en")
                                                    && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en", configFile).trim().isEmpty()
                                    )

                            )
                    ) {
                        getActivity().runOnUiThread(() -> {
                            buddyGPTApplication.setAnswerHasExceededTimeOut(false);
                            responseTimeout = new CountDownTimer(Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds", configFile)) * 1000, 1000) {
                                @Override
                                public void onTick(long l) {
                                    Log.i(TAG, "onTick: ");
                                }

                                @Override
                                public void onFinish() {
                                    if (!buddyGPTApplication.isAlreadyGetAnswer()) {
                                        buddyGPTApplication.setAnswerHasExceededTimeOut(true);
                                        buddyGPTApplication.setTimeoutExpired(true);

                                        handlerPauseTime.postDelayed(runnablePauseTime = () -> {
                                            if (handlerPauseTime != null && runnablePauseTime != null) {
                                                handlerPauseTime.removeCallbacks(runnablePauseTime);
                                                handlerPauseTime.removeCallbacksAndMessages(null);
                                            }
                                            if (responseTimeout != null)
                                                responseTimeout.cancel();
                                            if (buddyGPTApplication.getResponseFromTeamGPT() != null)
                                                buddyGPTApplication.getResponseFromTeamGPT().reset();
                                            if (handlerTTSError != null && runnableTTSError != null) {
                                                handlerTTSError.removeCallbacks(runnableTTSError);
                                                handlerTTSError.removeCallbacksAndMessages(null);
                                            }
                                            if (Boolean.FALSE.equals(buddyGPTApplication.getSpeaking())) {
                                                Log.d(TAG, "Mouth touched2");
                                                if (buddyGPTApplication.getparam("INVALID_TEAMGPT_DEVICE_ID").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
                                                    Log.i(TAG, "run: notifyObservers INVALID_TEAMGPT_DEVICE_ID 3");
                                                    buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_DEVICE_ID");
                                                } else if (buddyGPTApplication.getparam("INVALID_TEAMGPT_KEY").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
                                                    Log.i(TAG, "run: notifyObservers INVALID_TEAMGPT_KEY 3");
                                                    buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
                                                } else if (buddyGPTApplication.getparam("ENV_ERROR").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
                                                    Log.i(TAG, "run: notifyObservers ENV_ERROR 3");
                                                    buddyGPTApplication.notifyObservers("ENV_ERROR");
                                                } else if (buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
                                                    Log.i(TAG, "run: notifyObservers TEAMGPT_KEY EMPTY 3");
                                                    if (buddyGPTApplication.getLangue().getNom().equals("Anglais")) {
                                                        buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_en));
                                                    } else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                                                        buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_fr));
                                                    } else if (buddyGPTApplication.getLangue().getNom().equals("Espagnol")) {
                                                        buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_es));
                                                    } else if (buddyGPTApplication.getLangue().getNom().equals("Allemand")) {
                                                        buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_de));
                                                    } else {
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
                                                } else {
                                                    buddyGPTApplication.setStartRecording(true);
                                                    buddyGPTApplication.setSpeaking(true);
                                                    if (!isListeningFreeSpeech) {
                                                        isListeningFreeSpeech = true;
                                                        buddyGPTApplication.setActivityClosed(false);
                                                        startListeningFreeSpeech(buddyGPTApplication.getListeningDuration());
                                                    }
                                                }

                                            } else if (Boolean.TRUE.equals(buddyGPTApplication.getSpeaking())) {
                                                Log.d(TAG, "Mouth touched3 STT  " + buddyGPTApplication.getparam("STT") + " AUTRE " + buddyGPTApplication.getAppIsListeningToTheQuestion());
                                                if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Android")
                                                        || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Cerence")
                                                        || Boolean.TRUE.equals(!buddyGPTApplication.getAppIsListeningToTheQuestion())) {
                                                    BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                                                    buddyGPTApplication.setStartRecording(false);
                                                    buddyGPTApplication.setActivityClosed(true);
                                                    isListeningFreeSpeech = false;
                                                    buddyGPTApplication.setStoredResponse("");
                                                    try {
                                                        BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                                    } catch (Exception e) {
                                                        Log.e(TAG, "BuddySDK Exception  " + e);
                                                    }
                                                    buddyGPTApplication.notifyObservers("end of timer");
                                                } else {
                                                    buddyGPTApplication.setLed("Neutral");
                                                    BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                                                    try {
                                                        BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                                                        BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                                        BuddySDK.UI.stopListenAnimation();
                                                    } catch (Exception e) {
                                                        Log.e(TAG, "BuddySDK Exception  " + e);
                                                    }
                                                    BuddySDK.UI.stopListenAnimation();
                                                    buddyGPTApplication.setAppIsListeningToTheQuestion(false);
                                                    buddyGPTApplication.traitementAudio();
                                                }
                                            }
                                        }, Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds", "BuddyGPT.properties")) * 1000);

                                        if (buddyGPTApplication.getCurrentLanguage().equals("en")) {
                                            String[] message_Timeout_NotRespected_en = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en", configFile).split("/");
                                            int randomNumber_message_Timeout_NotRespected_en = random.nextInt(message_Timeout_NotRespected_en.length);
                                            speak(message_Timeout_NotRespected_en[randomNumber_message_Timeout_NotRespected_en], "timeOutExpired");
                                        } else if (buddyGPTApplication.getCurrentLanguage().equals("fr")) {
                                            String[] message_Timeout_NotRespected_fr = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_fr", configFile).split("/");
                                            int randomNumber_message_Timeout_NotRespected_fr = random.nextInt(message_Timeout_NotRespected_fr.length);
                                            speak(message_Timeout_NotRespected_fr[randomNumber_message_Timeout_NotRespected_fr], "timeOutExpired");
                                        } else if (buddyGPTApplication.getCurrentLanguage().equals("es")) {
                                            String[] message_Timeout_NotRespected_es = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_es", configFile).split("/");
                                            int randomNumber_message_Timeout_NotRespected_es = random.nextInt(message_Timeout_NotRespected_es.length);
                                            speak(message_Timeout_NotRespected_es[randomNumber_message_Timeout_NotRespected_es], "timeOutExpired");
                                        } else if (buddyGPTApplication.getCurrentLanguage().equals("de")) {
                                            String[] message_Timeout_NotRespected_de = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_de", configFile).split("/");
                                            int randomNumber_message_Timeout_NotRespected_de = random.nextInt(message_Timeout_NotRespected_de.length);
                                            speak(message_Timeout_NotRespected_de[randomNumber_message_Timeout_NotRespected_de], "timeOutExpired");
                                        } else {
                                            String[] message_Timeout_NotRespected_en = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en", "BuddyGPT.properties").split("/");
                                            int randomNumber_message_Timeout_NotRespected_en = random.nextInt(message_Timeout_NotRespected_en.length);
                                            buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(message_Timeout_NotRespected_en[randomNumber_message_Timeout_NotRespected_en]).addOnSuccessListener(new OnSuccessListener<String>() {
                                                @Override
                                                public void onSuccess(String translatedText) {

                                                    speak(translatedText, "timeOutExpired");
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
                            };

                            responseTimeout.start();
                        });
                    }
                });
            }
            if (message.contains("TTS_success")) {
                getActivity().runOnUiThread(() -> {
                    isWaitingForResponse = false;
                    if (startlisten) {
                        buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getListeningAttempt() - 1);
                        startCycle();
                    }
                });
            }
            if (message.contains("TTS_error") || message.contains("TTS_exception")) {
                getActivity().runOnUiThread(() -> {


                    String text = message.split(";")[1];

                    Log.w(TAG, "TTS_ERROR:" + text);

                    buddyGPTApplication.playUsingReadSpeakerCaseError(text, new ITTSCallbacks() {
                        @Override
                        public void onSuccess(String s) {
                            getActivity().runOnUiThread(() -> {
                                try {
                                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
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
                            int textLength = text.length();// Calculate the length of the pronounced text
                            int delayTime = (textLength / 20) * 1000; // 1 second for every 20 characters
                            if (buddyGPTApplication.getparam("TTS").equalsIgnoreCase("ReadSpeaker") && (buddyGPTApplication.getCurrentLanguage().equals("en") || buddyGPTApplication.getCurrentLanguage().equals("fr")) && Boolean.TRUE.equals(buddyGPTApplication.getUsingReadSpeaker())) {
                                delayTime = 0;
                            }
                            handlerTTSError.postDelayed(runnableTTSError = () -> getActivity().runOnUiThread(() -> {
                                try {
                                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
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
            if (message.contains("main destroy")) {
                buddyGPTApplication.setFileCreate(false);
                buddyGPTApplication.setparam("firstLaunch", "false");
            }
            if (message.contains("playStoredResponse") && !buddyGPTApplication.getStoredResponse().equals("")) {
                    getActivity().runOnUiThread(() -> speak(buddyGPTApplication.getStoredResponse(), "storedResponse"));

            }
            if (message.contains("mailSend")) {
                getActivity().runOnUiThread(() -> {
                    if (buddyGPTApplication.getLangue().getNom().equals(langueEn)) {
                        if (!buddyGPTApplication.getParamFromFile("Message_mail_send_en", configFile).trim().equals("")) {
                            Toast.makeText(buddyGPTApplication, buddyGPTApplication.getParamFromFile("Message_mail_send_en", configFile), Toast.LENGTH_LONG).show();
                        }

                    } else if (buddyGPTApplication.getLangue().getNom().equals(langueFr)) {
                        if (!buddyGPTApplication.getParamFromFile("Message_mail_send_fr", configFile).trim().equals("")) {
                            Toast.makeText(buddyGPTApplication, buddyGPTApplication.getParamFromFile("Message_mail_send_fr", configFile), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        if (!buddyGPTApplication.getParamFromFile("Message_mail_send_en", configFile).trim().equals("")) {
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
                });
            }
            if (message.contains("Session_ID_Changed")) {
                Replica session = new Replica();
                session.setType("Session");
                session.setValue(buddyGPTApplication.getparam("SelectedChatbot") + " - " + buddyGPTApplication.getparam("chatbotModel"));
                listRepGlobale.add(listRepGlobale.size() - 1, session);
                Replica[] mDataset = listRepGlobale.toArray(new Replica[0]);
                adapter.setData(mDataset);
                scroll();
            }
            if (message.contains("INVALID_TEAMGPT_KEY")) {
                buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "TRUE");
                if (buddyGPTApplication.getLangue().getNom().equals("Anglais")) {
                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                } else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_fr), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_fr));
                } else if (buddyGPTApplication.getLangue().getNom().equals("Espagnol")) {
                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_es), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_es));
                } else if (buddyGPTApplication.getLangue().getNom().equals("Allemand")) {
                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_de), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_de));
                } else {
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                            .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    buddyGPTApplication.showInputDialog(getActivity(), translatedText, "Attention !");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                                }
                            });
                }


            }
            if (message.contains("INVALID_TEAMGPT_DEVICE_ID")) {
                buddyGPTApplication.setparam("INVALID_TEAMGPT_DEVICE_ID", "TRUE");
                if (buddyGPTApplication.getLangue().getNom().equals("Anglais")) {
                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                } else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_fr), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_fr));
                } else {
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                            .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    buddyGPTApplication.showInputDialog(getActivity(), translatedText, "Attention !");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                                }
                            });
                }


            }
            if (message.contains("Session_ID_ERROR")) {

                if (buddyGPTApplication.getLangue().getNom().equals("Anglais")) {
                    buddyGPTApplication.showInputDialog2(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_params_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                } else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                    buddyGPTApplication.showInputDialog2(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_params_invalid_fr), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_fr));
                } else {
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                            .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_params_invalid_en))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    buddyGPTApplication.showInputDialog2(getActivity(), translatedText, "Attention !");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    buddyGPTApplication.showInputDialog2(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_params_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                                }
                            });
                }


            }
            if (message.contains("ErrorSending")) {
                getActivity().runOnUiThread(() -> {
                    if (buddyGPTApplication.getLangue().getNom().equals(langueEn)) {
                        Toast.makeText(buddyGPTApplication, buddyGPTApplication.getString(R.string.error_mail_toast_en), Toast.LENGTH_LONG).show();
                    } else if (buddyGPTApplication.getLangue().getNom().equals(langueFr)) {
                        Toast.makeText(buddyGPTApplication, buddyGPTApplication.getString(R.string.error_mail_toast_fr), Toast.LENGTH_LONG).show();
                    } else if (buddyGPTApplication.getLangue().getNom().equals(langueEs)) {
                        Toast.makeText(buddyGPTApplication, buddyGPTApplication.getString(R.string.error_mail_toast_es), Toast.LENGTH_LONG).show();
                    } else if (buddyGPTApplication.getLangue().getNom().equals(langueDe)) {
                        Toast.makeText(buddyGPTApplication, buddyGPTApplication.getString(R.string.error_mail_toast_de), Toast.LENGTH_LONG).show();
                    } else {
                        buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getString(R.string.error_mail_toast_en)).addOnSuccessListener(new OnSuccessListener<String>() {
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
                });

            }
            if (message.contains("changeDetected")) {
                int speakVolume = buddyGPTApplication.getVolume();
                int max = buddyGPTApplication.getMaxVolume();
                int defaultVolume = buddyGPTApplication.getClosestInt((double) (speakVolume * 100) / max);
                Log.e("FCH", "volumeMedia  " + defaultVolume);
                buddyGPTApplication.setparam("speak_volume", String.valueOf(defaultVolume));
            }
            if (message.contains("ENV_ERROR")){
                buddyGPTApplication.setparam("ENV_ERROR","TRUE");
                if (buddyGPTApplication.getLangue().getNom().equals("Anglais")) {
                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_env_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                } else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_env_invalid_fr), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_fr));
                } else {
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                            .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_env_invalid_en))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    buddyGPTApplication.showInputDialog(getActivity(), translatedText,"Attention !");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_env_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                                }
                            });
                }


            }
            if (message.contains("end of cycle")) {
                getActivity().runOnUiThread(() -> {
                    buddyGPTApplication.setLed("neutral");
                    microBtn.setImageResource(R.drawable.micro_off);
                });

            }
            if (message.contains("Obtain audio transcription after the listening time has elapsed")) {
                String shouldRestartNewCycle = message.split(";SPLIT;")[1];
                Log.e("ARR", "Obtain audio transcription after the listening time has elapsed " + shouldRestartNewCycle);
                microBtn.setImageResource(R.drawable.micro_off);
                buddyGPTApplication.setLed("neutral");
                buddyGPTApplication.setAppIsListeningToTheQuestion(false);
                buddyGPTApplication.traitementAudio();

            }
        }
    }

    /**
     * ------------------------------------------ STT  -------------------------------------------
     */

    private void startListeningFreeSpeech(int duration) {
        Log.d(TAG, " --- startListeningFreeSpeech(" + duration + ") ---");
        isListeningFreeSpeech = true;
        buddyGPTApplication.setAppIsListeningToTheQuestion(true);


        if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(ANDROID_STT)) {
            buddyGPTApplication.startListeningQuestion(getActivity());
        } else if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(CERENCE_STT)) {
            if (buddyGPTApplication.getCurrentLanguage().equals("fr") || buddyGPTApplication.getCurrentLanguage().equals("en")) {
                buddyGPTApplication.startListeningCerence(getActivity());
            } else {
                buddyGPTApplication.startListeningQuestion(getActivity());
            }
        }


        if (timerEcoute != null) timerEcoute.cancel();
        timerEcoute = new CountDownTimer(duration * 1000, 1000) {
            @Override
            public void onTick(long l) {
                Log.d(TAG, "timerEcoute onTick");
            }

            @Override
            public void onFinish() {

                if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(ANDROID_STT) || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(CERENCE_STT)) {
                    Log.i(TAG, "timerEcoute onFinish");
                    stopListeningFreeSpeech();
                    click = 1;
                } else {
                    buddyGPTApplication.notifyObservers("Obtain audio transcription after the listening time has elapsed;SPLIT;false");
                }
            }
        };
        timerEcoute.start();

        microBtn.setImageResource(R.drawable.micro_on);

    }

    private void startCycle() {
        isListeningFreeSpeech = true;
        buddyGPTApplication.setAppIsListeningToTheQuestion(true);

        if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(ANDROID_STT)) {
            buddyGPTApplication.startListeningQuestion(getActivity());

        } else if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(CERENCE_STT)) {
            if (buddyGPTApplication.getCurrentLanguage().equals("fr") || buddyGPTApplication.getCurrentLanguage().equals("en")) {
                buddyGPTApplication.startListeningCerence(getActivity());
            } else {
                buddyGPTApplication.startListeningQuestion(getActivity());
            }
        }


        if (timerEcoute != null) timerEcoute.cancel();
        timerEcoute = new CountDownTimer(buddyGPTApplication.getListeningDuration() * 1000, 1000) {
            @Override
            public void onTick(long l) {
                Log.d(TAG, "timerEcoute onTick");
            }

            @Override
            public void onFinish() {
                Log.i(TAG, "timerEcoute onFinish");
                if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(ANDROID_STT) || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(CERENCE_STT)) {
                    buddyGPTApplication.notifyObservers("end of cycle");
                    runnablePauseTime = () -> startNextCycle();
                    handlerPauseTime.postDelayed(runnablePauseTime, 1000);
                } else {
                    buddyGPTApplication.notifyObservers("Obtain audio transcription after the listening time has elapsed;SPLIT;true");
                }
            }
        };
        timerEcoute.start();

        microBtn.setImageResource(R.drawable.micro_on);
    }

    public void startNextCycle() {
        // Si nous avons encore des tentatives restantes
        Log.e("ARR", "startNextCycle  remainingattempts= " + buddyGPTApplication.getRemainingAttempts());
        if (buddyGPTApplication.getRemainingAttempts() > 0) {
            buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getRemainingAttempts() - 1);
            startCycle();

            Log.e("ARR", "startNextCycle  after handler ");
        } else {
            stopListeningFreeSpeech();
            click = 1;
            // Si toutes les tentatives ont été épuisées, vous pouvez faire quelque chose ici si nécessaire
        }
    }

    private void stopListeningFreeSpeech() {

        isListeningFreeSpeech = false;

        Log.d(TAG, " --- stopListeningFreeSpeech() ---");

        if (timerEcoute != null) timerEcoute.cancel();
        buddyGPTApplication.stopListening(getActivity());
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
        if(!texte.isEmpty())
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
}