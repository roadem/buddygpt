package com.robotique.aevaweb.buddygpt.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bfr.buddy.utils.events.EventItem;
import com.bfr.buddy.utils.values.FloatingWidgetVisibility;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.robotique.aevaweb.buddygpt.R;
import com.robotique.aevaweb.buddygpt.application.BuddyGPTApplication;
import com.robotique.aevaweb.buddygpt.chatbotresponse.ResponseFromTeamGPT;
import com.robotique.aevaweb.buddygpt.models.Langue;
import com.robotique.aevaweb.buddygpt.models.Setting;
import com.robotique.aevaweb.buddygpt.models.SttModel;
import com.robotique.aevaweb.buddygpt.models.TtsModel;
import com.robotique.aevaweb.buddygpt.utilis.LanguageDetailsChecker;
import com.robotique.aevaweb.buddygpt.utilis.WifiBroadcastReceiver;
import com.robotique.aevaweb.buddygpt.adapters.LangueSpinnerAdapter;
import com.robotique.aevaweb.buddygpt.adapters.SttSpinnerAdapter;
import com.robotique.aevaweb.buddygpt.adapters.TtsSpinnerAdapter;
import com.robotique.aevaweb.buddygpt.observers.IDBObserver;
import com.robotique.aevaweb.buddygpt.utilis.IMLKitDownloadCallback;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BuddyActivity implements IDBObserver {
    private static final String TAG = "BuddyGPT_SettingsActivity";

    private BuddyGPTApplication buddyGPTApplication;
    private View decorView;
    private RelativeLayout launch_view;
    private ImageView noNetwork;
    private ProgressBar downloadingBar;

    private LangueSpinnerAdapter langueSpinnerAdapter;
    private SttSpinnerAdapter sttSpinnerAdapter;
    private TtsSpinnerAdapter ttsSpinnerAdapter;
    private LinearLayout menu_option_tracking_camera_display_lyt;
    private LinearLayout menu_option_tracking_head_lyt;
    private LinearLayout menu_option_tracking_body_lyt;
    private LinearLayout menu_option_tracking_auto_listen_lyt;
    private LinearLayout menu_option_tracking_invitation_lyt;

    private LinearLayout menu_option_stt_lyt;
    private LinearLayout menu_option_tts_lyt;
    private LinearLayout menu_option_chatbot_lyt;
    //private LinearLayout menu_option_chatbotmodel_lyt;

    private TextView menu_title;


    private TextView menu_option_langue_textView;
    private TextView menu_option_stt_textView;
    private TextView menu_option_tts_textView;
    private TextView menu_option_chatbot_textView;
    //private TextView menu_option_chatbotmodel_textView;

    private TextView menu_option_volume_textView;
    private TextView menu_option_affichage_textView;
    private TextView menu_option_emotion_textView;
    private TextView menu_option_detectLanguage_textView;
    private TextView menu_option_mode_stream_textView;
    private TextView menu_header_textView;
    private TextView menu_apiKey_textView;
    private TextView menu_name_textView;


    private TextView menu_option_tracking_activation_textView;
    private TextView menu_option_tracking_camera_display_textView;
    private TextView menu_option_tracking_head_textView;
    private TextView menu_option_tracking_body_textView;
    private TextView menu_option_tracking_auto_listen_textView;
    private TextView menu_option_tracking_invitation_textView;
    private TextView menu_option_tracking_invitation_chatGpt_textView;


    private Spinner menu_option_langue_spinner;

    private Spinner menu_option_stt_spinner;
    private Spinner menu_option_tts_spinner;
    private TextView menu_option_chatbot_spinner;
    private TextView menu_option_chatbotmodel_spinner;

    private TextView menu_header_editText;
    private EditText menu_apiKey_editText;

    private TextView menu_nameText;
    private TextView copyRight;
    private TextView identifiers;



    private TextView volume_seekbar_value;
    private SeekBar volume_seekbar;

    private Setting set;
    private Setting setting;
    private List<Langue> langues;

    private Switch switchVisibility;
    private Switch switchEmotion;
    private Switch switchLanguageDetection;
    //private Switch switchModeStream;
//    private Switch switchCommande;
//    private Switch switchBIDisplay;
//    private Switch switchTrackingActivation;
//    private Switch switchTrackingCameraDisplay;
//    private Switch switchTrackingHead;
//    private Switch switchTrackingBody;
//    private Switch switchTrackingAutoListen;
//    private Switch switchTrackingInvitation;

    private String french= "Français";
    private String english = "Anglais";
    private String spanish = "Espagnol";
    private String deutsch = "Allemand";
    private String listeningDuration = "listening_duration";
    private String listeningAttempt = "listening_attempt";
    private String speakVolume ="speak_volume";
    private String visibilityString = "switch_visibility";
    private String emotionString = "switch_emotion";
    private String detectionLanguageString = "Detection_de_langue";
    private ResponseFromTeamGPT responseFromTeamGPT;
//    private String modeStreamString = "Stream_mode";
//    private String commandeString = "Commands";
    private String langueFR ="Français";
    private String langueEN ="Anglais";
    private String langueES ="Espagnol";
    private String langueDE ="Allemand";
    private String header ="Header";
    private String entete ="Entete";
    private String cabecera ="Cabecera";
    private String kopfzeile ="Kopfzeile";
    private String openAIKey = "openAI_API_Key";
    private String teamGPT_Key = "TeamGPT_Key";
    private String teamGPT_Key_value = "";
    private Boolean modelDownloading = false;
    private boolean english_is_downloaded = false;
    private boolean french_is_downloaded = false;
    private WifiBroadcastReceiver wifiBroadCastReceiver = new WifiBroadcastReceiver();
    private boolean isCalledOnce= false; // focus changed
    private int chosenLanguagePos = -1;
    private int chosenSTTPos = -1;
    private int chosenTTSPos = -1;
    private int chosenChatBotPos = -1;
    private CountDownTimer timerEcoute;
    private LanguageDetailsChecker languageDetailsChecker;
    private RelativeLayout popupLanguageList;
    private LinearLayout popupLanguageListContent;
    private ImageView dollar_icon;
    private boolean isClickedBtnCloseSettings=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Log.d(TAG," --- onCreate() ---");

        buddyGPTApplication = (BuddyGPTApplication) getApplicationContext();
        buddyGPTApplication.hideSystemUI(this);
        buddyGPTApplication.setInitSharedpreferences(false);
        decorView=getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if(menu_apiKey_editText != null && visibility==0 && !menu_apiKey_editText.hasFocus()){
                    decorView.setSystemUiVisibility(buddyGPTApplication.hideSystemUI(SettingsActivity.this));
                }
            }
        });


        menu_title = findViewById(R.id.menu_title);

//        menu_option_listening_duration_textView = findViewById(R.id.menu_option_listening_duration_textView);
//        menu_option_listening_attempt_textView = findViewById(R.id.menu_option_listening_attempt_textView);
        popupLanguageList= findViewById(R.id.popup_Languages_List);
        popupLanguageListContent= findViewById(R.id.popup_Languages_List_linearLayout);
       // affichage_Languages_List_lyt = findViewById(R.id.Android_STT_language_lyt);
        menu_option_stt_lyt = findViewById(R.id.menu_option_stt_lyt);
        menu_option_tts_lyt = findViewById(R.id.menu_option_tts_lyt);
        menu_option_chatbot_lyt = findViewById(R.id.menu_option_chatbot_lyt);
        //menu_option_chatbotmodel_lyt = findViewById(R.id.menu_option_chatbotmodel_lyt);

        menu_option_langue_textView = findViewById(R.id.menu_option_langue_textView);
        menu_option_chatbot_textView = findViewById(R.id.menu_option_chatbot_textView);
        //menu_option_chatbotmodel_textView = findViewById(R.id.menu_option_chatbotmodel_textView);
        menu_option_stt_textView =findViewById(R.id.menu_option_stt_textView);
        menu_option_tts_textView =findViewById(R.id.menu_option_tts_textView);

        menu_option_volume_textView = findViewById(R.id.menu_option_volume_textView);
        menu_option_affichage_textView = findViewById(R.id.menu_option_affichage_textView);
        menu_option_emotion_textView = findViewById(R.id.menu_option_emotion_textView);
        menu_option_detectLanguage_textView = findViewById(R.id.menu_option_language_detection_textView);
       // menu_option_mode_stream_textView = findViewById(R.id.menu_option_mode_stream_textView);
        menu_apiKey_textView = findViewById(R.id.api_key_txt);
        menu_name_textView = findViewById(R.id.name_txt);
        menu_header_textView = findViewById(R.id.header_txt);
        menu_option_langue_spinner = findViewById(R.id.menu_option_langue_spinner);
        menu_option_stt_spinner = findViewById(R.id.menu_option_stt_spinner);
        menu_option_tts_spinner = findViewById(R.id.menu_option_tts_spinner);
        menu_option_chatbot_spinner = findViewById(R.id.menu_option_chatbot_spinner);
        //menu_option_chatbotmodel_spinner = findViewById(R.id.menu_option_chatbotmodel_spinner);

//        menu_option_listening_duration_editText = findViewById(R.id.menu_option_listening_duration_editText);
//        menu_option_listening_attempt_editText = findViewById(R.id.menu_option_listening_attempt_editText);
        menu_apiKey_editText = findViewById(R.id.api_key_editText);

        menu_nameText = findViewById(R.id.user_name);
        copyRight = findViewById(R.id.copyright_texte);
        identifiers = findViewById(R.id.identifiers_texte);
        menu_header_editText = findViewById(R.id.header_editText);

        volume_seekbar=findViewById(R.id.volume_seekbar);
        volume_seekbar_value=findViewById(R.id.volume_seekbar_value);
        switchVisibility=findViewById(R.id.switchVisibility);
        switchEmotion = findViewById(R.id.switchEmotion);
        //switchBIDisplay = findViewById(R.id.switchBI);
        switchLanguageDetection = findViewById(R.id.switchLanguageDetection);
        //switchModeStream = findViewById(R.id.switchModeStream);
        launch_view = findViewById(R.id.launch_view);
        noNetwork = findViewById(R.id.noNetwork);
        downloadingBar = findViewById(R.id.progressBar_MLKitDownload);

//        menu_option_tracking_camera_display_lyt = findViewById(R.id.menu_option_tracking_camera_display_lyt);
//        menu_option_tracking_head_lyt = findViewById(R.id.menu_option_tracking_head_lyt);
//        menu_option_tracking_body_lyt = findViewById(R.id.menu_option_tracking_body_lyt);
//        menu_option_tracking_auto_listen_lyt = findViewById(R.id.menu_option_tracking_auto_listen_lyt);
//        menu_option_tracking_invitation_lyt = findViewById(R.id.menu_option_tracking_invitation_lyt);
//        //menu_option_tracking_invitation_chatGpt_lyt = findViewById(R.id.menu_option_tracking_invitation_chatGpt_lyt);
//        menu_option_tracking_activation_textView = findViewById(R.id.menu_option_tracking_activation_textView);
//        menu_option_tracking_camera_display_textView = findViewById(R.id.menu_option_tracking_camera_display_textView);
//        menu_option_tracking_head_textView = findViewById(R.id.menu_option_tracking_head_textView);
//        menu_option_tracking_body_textView = findViewById(R.id.menu_option_tracking_body_textView);
//        menu_option_tracking_auto_listen_textView = findViewById(R.id.menu_option_tracking_auto_listen_textView);
//        menu_option_tracking_invitation_textView = findViewById(R.id.menu_option_tracking_invitation_textView);
//        //menu_option_tracking_invitation_chatGpt_textView = findViewById(R.id.menu_option_tracking_invitation_chatGpt_textView);
//        switchTrackingActivation = findViewById(R.id.switchTrackingActivation);
//        switchTrackingCameraDisplay = findViewById(R.id.switchTrackingCameraDisplay);
//        switchTrackingHead = findViewById(R.id.switchTrackingHead);
//        switchTrackingBody = findViewById(R.id.switchTrackingBody);
//        switchTrackingAutoListen = findViewById(R.id.switchTrackingAutoListen);
//        switchTrackingInvitation = findViewById(R.id.switchTrackingInvitation);
        //switchTrackingInvitationChatGpt = findViewById(R.id.switchTrackingInvitationChatGpt);
        //dollar_icon = findViewById(R.id.dollar_icon);

        set=new Setting();
        setting=new Setting();
        buddyGPTApplication.registerObserver(this);
        wifiBroadCastReceiver.setAct(getApplicationContext());
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(wifiBroadCastReceiver, intentFilter);
        wifiBroadCastReceiver.forceCheckConnexState(getApplicationContext());


        //ImageView blue_mic_lyt = findViewById(R.id.blue_mic_lyt);

       // String blueMic_Disponibility = buddyGPTApplication.getParamFromFile("BlueMic_Disponibility", "BuddyGPT.properties");

//        if(blueMic_Disponibility != null && blueMic_Disponibility.trim().equalsIgnoreCase("Yes")){
//            blue_mic_lyt.setVisibility(View.VISIBLE);
//        }
//        else{
//            blue_mic_lyt.setVisibility(View.INVISIBLE);
//        }


        /**
         *  Gestion du choix de la durée d'écoute (secondes)
         */

       // handlerListeningDuration();
        /**
         *  Gestion du choix de du nombre de tentatives
         */

        //handlerListeningAttempt();
        /**
         *  Gestion de l'api key
         */
        if(!buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase(""))
            teamGPT_Key_value= buddyGPTApplication.getparam("TeamGPT_Key");
        handlerApiKey();



        /**
         *  Gestion des chatbots
         */

        menu_option_chatbot_lyt.setVisibility(View.VISIBLE);
        //menu_option_chatbotmodel_lyt.setVisibility(View.VISIBLE);

        handlerChatbot();

        /**
         *  Gestion du seekbar de volume de parole
         */


        handlerSpeakVolume();
        /**
         *  Gestion de la liste déroulante pour le choix de langue [ Français | Anglais ]
         */

        handlerLangue();

        /**
         *  Gestion de l'affichage des paroles
         */

        switchVisibility.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam(visibilityString)));
        set.setSwitchVisibility(buddyGPTApplication.getparam(visibilityString));
        setting.setSwitchVisibility(buddyGPTApplication.getparam(visibilityString));
        buddyGPTApplication.setSwitchVisibility(buddyGPTApplication.getparam(visibilityString));
        switchVisibility.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            buddyGPTApplication.setSwitchVisibility(String.valueOf(b));
            buddyGPTApplication.setparam(visibilityString,String.valueOf(b));
            set.setSwitchVisibility(String.valueOf(b));

        });
        /**
         *  Gestion de la lecture des BI
         */

//        switchBIDisplay.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam("Stimulis")));
//        set.setSwitchBIDisplay(buddyGPTApplication.getparam("Stimulis"));
//        setting.setSwitchBIDisplay(buddyGPTApplication.getparam("Stimulis"));
//        buddyGPTApplication.setSwitchBIDisplay(buddyGPTApplication.getparam("Stimulis"));
//        switchBIDisplay.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
//            buddyGPTApplication.setSwitchBIDisplay(String.valueOf(b));
//            buddyGPTApplication.setparam("Stimulis",String.valueOf(b));
//            if (buddyGPTApplication.getparam("Stimulis").equals("true")){
//                Log.e("MRARA","disnable Raise event Stimilus");
//                BuddySDK.Companion.raiseEvent("disableRightEye");
//                BuddySDK.Companion.raiseEvent("disableLeftEye");
//                BuddySDK.Companion.raiseEvent("disableHeadSensors");
//                BuddySDK.Companion.raiseEvent("disableBodySensors");
//            }else {
//                if (buddyGPTApplication.getParamFromFile("use_companion_when_stimulis_disabled","BuddyGPT.properties").trim().equalsIgnoreCase("Yes")){
//                    Log.e("MRARA","enable Raise event Yes");
//                    BuddySDK.Companion.raiseEvent("enableRightEye");
//                    BuddySDK.Companion.raiseEvent("enableLeftEye");
//                    BuddySDK.Companion.raiseEvent("enableHeadSensors");
//                    BuddySDK.Companion.raiseEvent("enableBodySensors");
//                    BuddySDK.Companion.raiseEvent("disableOnMouth");
//                }else {
//                    Log.e("MRARA","disable Raise event NO");
//                    BuddySDK.Companion.raiseEvent("disableRightEye");
//                    BuddySDK.Companion.raiseEvent("disableLeftEye");
//                    BuddySDK.Companion.raiseEvent("disableHeadSensors");
//                    BuddySDK.Companion.raiseEvent("disableBodySensors");
//                }
        //    }
//            set.setSwitchBIDisplay(String.valueOf(b));
//
//        });
        /**
         *  Gestion de l'affichage des émotions
         */

        switchEmotion.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam(emotionString)));
        set.setSwitchEmotion(buddyGPTApplication.getparam(emotionString));
        setting.setSwitchEmotion(buddyGPTApplication.getparam(emotionString));
        buddyGPTApplication.setSwitchEmotion(buddyGPTApplication.getparam(emotionString));
        switchEmotion.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            buddyGPTApplication.setSwitchEmotion(String.valueOf(b));
            buddyGPTApplication.setparam(emotionString,String.valueOf(b));
            set.setSwitchEmotion(String.valueOf(b));
        });
        /**
         *  Gestion de la detection des langues
         */

        switchLanguageDetection.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam(detectionLanguageString)));
        set.setSwitchLanguageDetection(buddyGPTApplication.getparam(detectionLanguageString));
        setting.setSwitchLanguageDetection(buddyGPTApplication.getparam(detectionLanguageString));
        buddyGPTApplication.setSwitchdetectLanguage(buddyGPTApplication.getparam(detectionLanguageString));
        switchLanguageDetection.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            buddyGPTApplication.setSwitchdetectLanguage(String.valueOf(b));
            buddyGPTApplication.setparam(detectionLanguageString,String.valueOf(b));
            set.setSwitchLanguageDetection(String.valueOf(b));
        });


        /**
         *  Gestion du switch mode stream
         */
//        switchModeStream.setEnabled(false);
//        switchModeStream.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam(modeStreamString)));
//        set.setSwitchModeStream(buddyGPTApplication.getparam(modeStreamString));
//        setting.setSwitchModeStream(buddyGPTApplication.getparam(modeStreamString));
//        buddyGPTApplication.setSwitchModeStream(buddyGPTApplication.getparam(modeStreamString));
//
//        switchModeStream.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
//            buddyGPTApplication.setSwitchModeStream(String.valueOf(b));
//            buddyGPTApplication.setparam(modeStreamString,String.valueOf(b));
//            set.setSwitchModeStream(String.valueOf(b));
//        });
        if(responseFromTeamGPT != null){
            responseFromTeamGPT.reset();
        }
        responseFromTeamGPT=new ResponseFromTeamGPT(buddyGPTApplication);
        if(buddyGPTApplication.getResponseFromTeamGPT()!=null)
            buddyGPTApplication.getResponseFromTeamGPT().reset();
        buddyGPTApplication.setResponseFromTeamGPT(responseFromTeamGPT);
        /**
         *  Gestion du switch commande
         */
        /*switchCommande.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam(commandeString)));
        set.setSwitchCommande(buddyGPTApplication.getparam(commandeString));
        setting.setSwitchCommande(buddyGPTApplication.getparam(commandeString));
        buddyGPTApplication.setSwitchCommande(buddyGPTApplication.getparam(commandeString));
        switchCommande.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            buddyGPTApplication.setSwitchCommande(String.valueOf(b));
            buddyGPTApplication.setparam(commandeString,String.valueOf(b));
            set.setSwitchCommande(String.valueOf(b));
        });

*/
        /**
         *  Gestion de l'entete
         */
        handlerHeader();



        /**
         *  Gestion du choix STT
         */
        if(buddyGPTApplication.getparam("STT-TeamGPT").equalsIgnoreCase("local"))
            menu_option_stt_lyt.setVisibility(View.VISIBLE);
        if(buddyGPTApplication.getparam("TTS-TeamGPT").equalsIgnoreCase("local"))
            menu_option_tts_lyt.setVisibility(View.VISIBLE);

        handlerSTT();
        handlerTTS();
        handlerSupport();
        handlerNameAndEmail();
        /**
         * Gestion Tracking
         */
        //handlerTracking();

        popupLanguageList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Vérifier si le popup_add_mail est visible et si le clic est en dehors de celui-ci
                if (popupLanguageList.getVisibility() == View.VISIBLE) {
                    MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);
                    if (!isViewInsideBounds(popupLanguageListContent, (int) event.getRawX(), (int) event.getRawY())) {
                        // Si le clic est en dehors, rendre le popup invisible
                        popupLanguageList.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
        popupLanguageListContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ne rien faire pour empêcher la propagation du clic aux éléments enfants du popup
            }
        });
    }
    // Vérifie si les coordonnées de l'événement sont à l'intérieur de la vue spécifiée
    private boolean isViewInsideBounds(View view, int x, int y) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];
        return !(x < viewX || x > viewX + view.getWidth() || y < viewY || y > viewY + view.getHeight());
    }
    private void handlerTracking(){

        //Tracking activation
        if(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation"))){
            menu_option_tracking_camera_display_lyt.setVisibility(View.VISIBLE);
            menu_option_tracking_head_lyt.setVisibility(View.VISIBLE);
            menu_option_tracking_body_lyt.setVisibility(View.VISIBLE);
            menu_option_tracking_auto_listen_lyt.setVisibility(View.VISIBLE);
            menu_option_tracking_invitation_lyt.setVisibility(View.VISIBLE);
           // if(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Invitation"))){
//                menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.VISIBLE);
//            }
//            else{
            //    menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.GONE);
           // }
        }
        else{
            menu_option_tracking_camera_display_lyt.setVisibility(View.GONE);
            menu_option_tracking_head_lyt.setVisibility(View.GONE);
            menu_option_tracking_body_lyt.setVisibility(View.GONE);
            menu_option_tracking_auto_listen_lyt.setVisibility(View.GONE);
            menu_option_tracking_invitation_lyt.setVisibility(View.GONE);
           // menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.GONE);
        }
//        switchTrackingActivation.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation")));
//        switchTrackingActivation.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
//            buddyGPTApplication.setparam("Tracking_Activation",String.valueOf(b));
//            if(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation"))){
//                menu_option_tracking_camera_display_lyt.setVisibility(View.VISIBLE);
//                menu_option_tracking_head_lyt.setVisibility(View.VISIBLE);
//                menu_option_tracking_body_lyt.setVisibility(View.VISIBLE);
//                menu_option_tracking_auto_listen_lyt.setVisibility(View.VISIBLE);
////                menu_option_tracking_invitation_lyt.setVisibility(View.VISIBLE);
////                if(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Invitation"))){
////                    menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.VISIBLE);
////                }
////                else{
////                    menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.GONE);
////                }
//            }
//            else{
//                menu_option_tracking_camera_display_lyt.setVisibility(View.GONE);
//                menu_option_tracking_head_lyt.setVisibility(View.GONE);
//                menu_option_tracking_body_lyt.setVisibility(View.GONE);
//                menu_option_tracking_auto_listen_lyt.setVisibility(View.GONE);
//                menu_option_tracking_invitation_lyt.setVisibility(View.GONE);
//               // menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.GONE);
//            }
//        });
//
//        //Tracking camera display
//        switchTrackingCameraDisplay.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Camera_Display")));
//        switchTrackingCameraDisplay.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
//            buddyGPTApplication.setparam("Tracking_Camera_Display",String.valueOf(b));
//        });
//
//        //Tracking head
//        switchTrackingHead.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Head")));
//        switchTrackingHead.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
//            buddyGPTApplication.setparam("Tracking_Head",String.valueOf(b));
//        });
//
//        //Tracking body
//        switchTrackingBody.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Body")));
//        switchTrackingBody.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
//            buddyGPTApplication.setparam("Tracking_Body",String.valueOf(b));
//        });
//
//        //Tracking auto listen
//        switchTrackingAutoListen.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Auto_Listen")));
//        switchTrackingAutoListen.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
//            buddyGPTApplication.setparam("Tracking_Auto_Listen",String.valueOf(b));
//        });
//
//        //Tracking invitation
//        switchTrackingInvitation.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Invitation")));
//        switchTrackingInvitation.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
//            buddyGPTApplication.setparam("Tracking_Invitation",String.valueOf(b));
//            if(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation"))){
//                if(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Invitation"))){
//                //    menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.VISIBLE);
//                }
//                else{
//                   // menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.GONE);
//                }
//            }
//          //  else menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.GONE);
//        });

        //Tracking invitation chatGpt
//        switchTrackingInvitationChatGpt.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Invitation_ChatGpt")));
//        switchTrackingInvitationChatGpt.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
//            buddyGPTApplication.setparam("Tracking_Invitation_ChatGpt",String.valueOf(b));
//        });

    }

    private void handlerLangue() {

        langues = new ArrayList<>();

        List<String> langueDisponible = buddyGPTApplication.getDisponibleLangue();
        for (int i=1;i<langueDisponible.size();i++){

            langues.add(new Gson().fromJson(buddyGPTApplication.getparam(langueDisponible.get(i-1)), Langue.class));
            i++;
        }
        if (langues.isEmpty()){
            langues.add(new Gson().fromJson(buddyGPTApplication.getparam(french), Langue.class));
        }

        langueSpinnerAdapter = new LangueSpinnerAdapter(getApplicationContext(),
                R.layout.spinner_item_layout_resource,
                R.id.item_name,
                R.id.checked_item_checked,
                langues);
        menu_option_langue_spinner.setAdapter(langueSpinnerAdapter);


        avoidSpinnerDropdownFocus(menu_option_langue_spinner);


        //get the position, of the chosen language
        for (int index = 0; index < langues.size(); index++) {
            if (langues.get(index).isChosen()) {
                chosenLanguagePos = index;
                buddyGPTApplication.setLangue(langues.get(index));
                setting.setLangue(langues.get(index).getNom());
                break;
            }
            if ( index == langues.size()-1){
                chosenLanguagePos = index;
                buddyGPTApplication.setLangue(langues.get(index));
                setting.setLangue(langues.get(index).getNom());

            }
        }
        menu_option_langue_spinner.setSelection(chosenLanguagePos);
        menu_option_langue_spinner.setEnabled(true);
        setLanguageText();
        menu_option_langue_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                chosenLanguagePos = position;
                for(Langue langue : langues ) {
                    langue.setChosen(false);
                    if (langue.equals(parent.getSelectedItem())) {
                        langue.setChosen(true);
                        buddyGPTApplication.setLangue(langue);
                        set.setLangue(langue.getNom());
                        modelDownloading = true;
                        buddyGPTApplication.downloadModel(imlKitDownloadCallback, buddyGPTApplication.getLangue().getLanguageCode().split("-")[0].trim());
                        handlerProgressBar.postDelayed(runnableProgressBar,500);

                    }
                    if(langue.getNom().equals(langueFR)){
                        buddyGPTApplication.setparam(french,new Gson().toJson(langue));
                    }
                    else if(langue.getNom().equals(langueEN)){
                        buddyGPTApplication.setparam(english,new Gson().toJson(langue));
                    }
                    else if (langue.getNom().equals(langueES)){
                        buddyGPTApplication.setparam(spanish,new Gson().toJson(langue));
                    }
                    else if (langue.getNom().equals(langueDE)){
                        buddyGPTApplication.setparam(deutsch,new Gson().toJson(langue));
                    }else{
                        buddyGPTApplication.setparam(langue.getNom(),new Gson().toJson(langue));
                    }
                }
                langueSpinnerAdapter.updateDataSet(langues);
                setLanguageText();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Method left empty intentionally because no specific action is needed for this update.
            }
        });
    }
    private void handlerChatbot(){


        Log.i(TAG, "handlerChatbot: HOU"+ buddyGPTApplication.getparam("SelectedChatbot"));

        menu_option_chatbot_spinner.setText(buddyGPTApplication.getparam("SelectedChatbot") +" "+ buddyGPTApplication.getModel());//    if (buddyGPTApplication.getparam("SelectedChatbot").equals("openai"))
//        menu_option_chatbotmodel_spinner.setText(buddyGPTApplication.getparam("Modele_Openai"));
 }
    private void handlerSTT() {


        final List<SttModel> sttList = new ArrayList<>();


        sttList.add(new SttModel(1, "Android", false));
        sttList.add(new SttModel(2, "Cerence", false));


        sttSpinnerAdapter = new SttSpinnerAdapter(getApplicationContext(),
                R.layout.spinner_item_layout_resource,
                R.id.item_name,
                R.id.checked_item_checked,
                sttList);
        menu_option_stt_spinner.setAdapter(sttSpinnerAdapter);
        avoidSpinnerDropdownFocus(menu_option_stt_spinner);
        for (int i = 0; i < sttList.size(); i++) {
            if (sttList.get(i).getNom().equalsIgnoreCase(buddyGPTApplication.getparam("STT"))) {
                chosenSTTPos = i;

                break;
            }
        }

        menu_option_stt_spinner.setSelection(chosenSTTPos);
        menu_option_stt_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                chosenLanguagePos = position;
                for (SttModel stt : sttList) {
                    stt.setChosen(false);
                    if (stt.equals(parent.getSelectedItem())) {
                        stt.setChosen(true);

                        buddyGPTApplication.setparam("STT", stt.getNom());
                    }

                }
                sttSpinnerAdapter.updateDataSet(sttList);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
    private void handlerTTS(){

        final List<TtsModel> ttsList = new ArrayList<>();


        ttsList.add(new TtsModel(1, "ReadSpeaker", false));
        ttsList.add(new TtsModel(2, "Android", false));


        ttsSpinnerAdapter = new TtsSpinnerAdapter(getApplicationContext(),
                R.layout.spinner_item_layout_resource,
                R.id.item_name,
                R.id.checked_item_checked,
                ttsList);
        menu_option_tts_spinner.setAdapter(ttsSpinnerAdapter);
        avoidSpinnerDropdownFocus(menu_option_tts_spinner);
        for (int i = 0; i < ttsList.size(); i++) {
            if (ttsList.get(i).getNom().equalsIgnoreCase(buddyGPTApplication.getparam("TTS"))) {
                chosenTTSPos = i;

                break;
            }
        }

        menu_option_tts_spinner.setSelection(chosenTTSPos);
        menu_option_tts_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                chosenLanguagePos = position;
                for (TtsModel tts : ttsList) {
                    tts.setChosen(false);
                    if (tts.equals(parent.getSelectedItem())) {
                        tts.setChosen(true);

                        buddyGPTApplication.setparam("TTS", tts.getNom());
                    }

                }
                ttsSpinnerAdapter.updateDataSet(ttsList);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
    private void handlerNameAndEmail(){

        Log.i(TAG, "handlerName: HOU"+ buddyGPTApplication.getparam("NomCompte"));
        menu_nameText.setText(buddyGPTApplication.getparam("NomCompte")+" "+buddyGPTApplication.getparam("Email"));

    }
    private void handlerSupport(){

            Log.i(TAG, "handlerSupport: HOU"+ buddyGPTApplication.getparam("email_support"));
            copyRight.setText(getString(R.string.copyright)+" / "+buddyGPTApplication.getparam("email_support"));
            identifiers.setText(buddyGPTApplication.getparam("IdCompte")+" / "+buddyGPTApplication.getparam("IMEI_ID_Device"));
        }

    private void handlerSpeakVolume() {
        volume_seekbar_value.setText(buddyGPTApplication.getparam(speakVolume)+"%");
        volume_seekbar.setProgress(Integer.parseInt(buddyGPTApplication.getparam(speakVolume)));
        set.setVolume(buddyGPTApplication.getparam(speakVolume));
        setting.setVolume(buddyGPTApplication.getparam(speakVolume));
        buddyGPTApplication.setSpeakVolume(Integer.parseInt(buddyGPTApplication.getparam(speakVolume)));

        volume_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                buddyGPTApplication.setVolume(progress, AudioManager.FLAG_SHOW_UI);
                volume_seekbar_value.setText(progress + " %");
                buddyGPTApplication.setparam(speakVolume, Integer.toString(progress));
                set.setVolume(Integer.toString(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Method left empty intentionally because no specific action is needed for this update.
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                // Method left empty intentionally because no specific action is needed for this update.
            }
        });
    }



    private void handlerListeningAttempt() {
        if(buddyGPTApplication.getparam(listeningAttempt).equals("")){
            buddyGPTApplication.setparam(listeningAttempt,"3");
            set.setAttempt(buddyGPTApplication.getparam(listeningAttempt));
        }
        buddyGPTApplication.setListeningAttempt(Integer.parseInt(buddyGPTApplication.getparam(listeningAttempt)));
        set.setAttempt(buddyGPTApplication.getparam(listeningAttempt));
        setting.setAttempt(buddyGPTApplication.getparam(listeningAttempt));

//        menu_option_listening_attempt_editText.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);
//
//        menu_option_listening_attempt_editText.setText(buddyGPTApplication.getparam(listeningAttempt));
//
//        menu_option_listening_attempt_editText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                // Method left empty intentionally because no specific action is needed for this update.
//            }
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if(!charSequence.toString().isEmpty()){
//                    buddyGPTApplication.setparam(listeningAttempt,charSequence.toString());
//                    buddyGPTApplication.setListeningAttempt(Integer.parseInt(buddyGPTApplication.getparam(listeningAttempt)));
//                    set.setAttempt(buddyGPTApplication.getparam(listeningAttempt));
//                }
//            }
//            @Override
//            public void afterTextChanged(Editable editable) {
//                // Method left empty intentionally because no specific action is needed for this update.
//            }
//        });

//        menu_option_listening_attempt_editText.setOnFocusChangeListener((v,hasFocus) -> {
//            if (hasFocus) {
//                View decorView = getWindow().getDecorView();
//                decorView.setSystemUiVisibility(
//                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
//            } else {
//                buddyGPTApplication.hideSystemUI(SettingsActivity.this);
//            }
//        });
    }

    private void handlerListeningDuration() {
        if(buddyGPTApplication.getparam(listeningDuration).equals("")){
            buddyGPTApplication.setparam(listeningDuration,"30");
            set.setDuration(buddyGPTApplication.getparam(listeningDuration));
        }
        buddyGPTApplication.setListeningDuration(Integer.parseInt(buddyGPTApplication.getparam(listeningDuration)));
        set.setDuration(buddyGPTApplication.getparam(listeningDuration));
        setting.setDuration(buddyGPTApplication.getparam(listeningDuration));

//        menu_option_listening_duration_editText.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);
//
//        menu_option_listening_duration_editText.setText(buddyGPTApplication.getparam(listeningDuration));
//
//        menu_option_listening_duration_editText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                // Method left empty intentionally because no specific action is needed for this update.
//            }
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if(!charSequence.toString().isEmpty()){
//                    buddyGPTApplication.setparam(listeningDuration,charSequence.toString());
//                    buddyGPTApplication.setListeningDuration(Integer.parseInt(buddyGPTApplication.getparam("listening_duration")));
//                    set.setDuration(buddyGPTApplication.getparam(listeningDuration));
//                }
//            }
//            @Override
//            public void afterTextChanged(Editable editable) {
//                // Method left empty intentionally because no specific action is needed for this update.
//            }
//        });
//
//        menu_option_listening_duration_editText.setOnFocusChangeListener((v,hasFocus) -> {
//            if (hasFocus) {
//                View decorView = getWindow().getDecorView();
//                decorView.setSystemUiVisibility(
//                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
//            } else {
//                buddyGPTApplication.hideSystemUI(SettingsActivity.this);
//            }
//        });


    }

    private void handlerHeader() {
        menu_header_editText.setFocusable(false);
        menu_header_editText.setClickable(false);

            if (buddyGPTApplication.getLangue().getNom().equals("Anglais")){

                menu_header_editText.setText(buddyGPTApplication.getparam(header));

            }
            else if(buddyGPTApplication.getLangue().getNom().equals("Français")){
                Log.i(TAG, "handlerHeader: HOU 2"+ buddyGPTApplication.getparam(entete));
                menu_header_editText.setText(buddyGPTApplication.getparam(entete));

            }

            else{
                Log.i(TAG, "handlerHeader: HOU 3"+ buddyGPTApplication.getparam(header));
                translateAndSetTextView(0,menu_header_editText,buddyGPTApplication.getparam(header));

            }
    }
    private void handlerApiKey() {
        menu_apiKey_editText.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);
        menu_apiKey_editText.setText(buddyGPTApplication.getparam(teamGPT_Key));
        set.setApiKey(buddyGPTApplication.getparam(teamGPT_Key));
        setting.setApiKey(buddyGPTApplication.getparam(teamGPT_Key));


        menu_apiKey_editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Method left empty intentionally because no specific action is needed for this update.
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    isCalledOnce= false;
                    buddyGPTApplication.setparam(teamGPT_Key, charSequence.toString());
                    set.setApiKey(buddyGPTApplication.getparam(teamGPT_Key));
                    setting.setApiKey(buddyGPTApplication.getparam(teamGPT_Key));
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        menu_apiKey_editText.setOnFocusChangeListener((v,hasFocus) -> {
            if (hasFocus) {
                Log.i(TAG, "handlerApiKey: hasFocus");
                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            } else {
                if(!isCalledOnce){
                    isCalledOnce= true;
                    buddyGPTApplication.hideSystemUI(SettingsActivity.this);
                    buddyGPTApplication.setparam(teamGPT_Key, menu_apiKey_editText.getText().toString());
                    Log.i("TAG", "run: menu_apiKey_editText"+menu_apiKey_editText.getText().toString());

                    if(menu_apiKey_editText.getText().toString().equals("")){
                        Log.i("TAG", "run: getParameters");
                        buddyGPTApplication.resetSharedPreferences();
                        refresh(0);
                    }else{
                        Log.i("TAG", "run: getParameters else");
                        if(buddyGPTApplication.getparam("Mail_Destination").equalsIgnoreCase(buddyGPTApplication.getparam("Email"))){
                            buddyGPTApplication.setparam("Mail_Destination","");
                        }
                        if(buddyGPTApplication.getResponseFromTeamGPT()!=null) {
                            Log.w("BuddyGPT", "buddyGPTApplication.getResponseFromTeamGPT()!=null " );
                            // buddyGPTApplication.getResponseFromTeamGPT().reset();

                            buddyGPTApplication.getResponseFromTeamGPT().getParameters();
                        }
                        refresh(1);
                    }

                }
            }
        });

        menu_apiKey_editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {

                if(!isCalledOnce){
                    isCalledOnce= true;
                    buddyGPTApplication.setparam(teamGPT_Key, textView.getText().toString());
                    Log.i("TAG", "run: getParameters 3" + textView.getText().toString());

                    if (textView.getText().toString().equals("")) {
                        Log.i("TAG", "run: getParameters 31");
                        buddyGPTApplication.resetSharedPreferences();
                        refresh(0);
                    } else {
                        Log.i("TAG", "run: getParameters 32");
                        if (buddyGPTApplication.getparam("Mail_Destination").equalsIgnoreCase(buddyGPTApplication.getparam("Email"))) {
                            buddyGPTApplication.setparam("Mail_Destination", "");
                        }
                        if (buddyGPTApplication.getResponseFromTeamGPT() != null)
                            buddyGPTApplication.getResponseFromTeamGPT().getParameters();
                        refresh(1);
                    }
                }


                return false;
            }
        });
    }


    private void refresh(int state){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (state==1){// refresh with new values
                    buddyGPTApplication.setparam("session_id","");
                    if(buddyGPTApplication.getLangue().getNom().equals("Anglais")){
                        menu_header_editText.setText(buddyGPTApplication.getparam(header));
                    }
                    else if(buddyGPTApplication.getLangue().getNom().equals("Français")){
                        menu_header_editText.setText(buddyGPTApplication.getparam(entete));
                    }
                    else if(buddyGPTApplication.getLangue().getNom().equals("Espagnol")){
                        menu_header_editText.setText(buddyGPTApplication.getparam(cabecera));
                    }
                    else if(buddyGPTApplication.getLangue().getNom().equals("Allemand")){
                        menu_header_editText.setText(buddyGPTApplication.getparam(kopfzeile));
                    }
                    if(buddyGPTApplication.getparam("STT-TeamGPT").equalsIgnoreCase("local"))
                        menu_option_stt_lyt.setVisibility(View.VISIBLE);
                    else if(buddyGPTApplication.getparam("STT-TeamGPT").equalsIgnoreCase(""))
                        menu_option_stt_lyt.setVisibility(View.GONE);
                    if(buddyGPTApplication.getparam("TTS-TeamGPT").equalsIgnoreCase("local"))
                        menu_option_tts_lyt.setVisibility(View.VISIBLE);
                    else if(buddyGPTApplication.getparam("TTS-TeamGPT").equalsIgnoreCase(""))
                        menu_option_tts_lyt.setVisibility(View.GONE);

                    menu_option_chatbot_spinner.setText(buddyGPTApplication.getparam("SelectedChatbot") +" "+ buddyGPTApplication.getModel());
                    // menu_option_chatbotmodel_spinner.setText(buddyGPTApplication.getparam("Modele_Openai"));
                    menu_nameText.setText(buddyGPTApplication.getparam("NomCompte")+" "+buddyGPTApplication.getparam("Email"));
                    if(buddyGPTApplication.getparam("Mail_Destination").equalsIgnoreCase(""))
                        buddyGPTApplication.setparam("Mail_Destination",buddyGPTApplication.getparam("Email"));

                    copyRight.setText(getString(R.string.copyright)+" / "+buddyGPTApplication.getparam("email_support"));
                    identifiers.setText(buddyGPTApplication.getparam("IdCompte")+" / "+buddyGPTApplication.getparam("IMEI_ID_Device"));
                    // switchModeStream.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam(modeStreamString)));
                }else{// refresh with null
                        menu_header_editText.setText("");
                        menu_option_stt_lyt.setVisibility(View.GONE);
                        menu_option_tts_lyt.setVisibility(View.GONE);
                        menu_option_chatbot_spinner.setText("");
                        menu_nameText.setText("");
                        copyRight.setText(getString(R.string.copyright)+" / _");
                        identifiers.setText("_ / _");

                }

            }
        });

    }
    public void setLanguageText(){
        if(buddyGPTApplication.getLangue().getNom().equals("Anglais")){
            menu_title.setText(R.string.menu_title_en);
            menu_option_langue_textView.setText(R.string.menu_option_langue_en);
            menu_option_tts_textView.setText(R.string.menu_option_tts_en);
            menu_option_stt_textView.setText(R.string.menu_option_stt_en);
            menu_option_chatbot_textView.setText(R.string.menu_option_chatbot_en);
            //menu_option_chatbotmodel_textView.setText(R.string.menu_option_chatbotmodel_en);
            menu_option_volume_textView.setText(R.string.menu_option_volume_en);
            menu_option_affichage_textView.setText(R.string.menu_option_affichage_en);
            menu_option_emotion_textView.setText(R.string.menu_option_emotion_en);
            menu_option_detectLanguage_textView.setText(R.string.menu_option_detectionLanguage_en);
            //menu_option_mode_stream_textView.setText(R.string.menu_option_mode_stream_en);
            menu_apiKey_textView.setText(R.string.menu_api_key_en);
            menu_name_textView.setText(R.string.menu_name_en);
            menu_header_textView.setText(R.string.menu_header_en);
//            menu_option_tracking_activation_textView.setText(R.string.menu_option_tracking_activation_en);
//            menu_option_tracking_camera_display_textView.setText(R.string.menu_option_tracking_camera_display_en);
//            menu_option_tracking_head_textView.setText(R.string.menu_option_tracking_head_en);
//            menu_option_tracking_body_textView.setText(R.string.menu_option_tracking_body_en);
//            menu_option_tracking_auto_listen_textView.setText(R.string.menu_option_tracking_auto_listen_en);
//            menu_option_tracking_invitation_textView.setText(R.string.menu_option_tracking_invitation_en);

                menu_header_editText.setText(buddyGPTApplication.getparam(header));


        }
        else if(buddyGPTApplication.getLangue().getNom().equals("Français")){
            menu_title.setText(R.string.menu_title_fr);

            menu_option_langue_textView.setText(R.string.menu_option_langue_fr);
            menu_option_stt_textView.setText(R.string.menu_option_stt_fr);
            menu_option_tts_textView.setText(R.string.menu_option_tts_fr);
            menu_option_chatbot_textView.setText(R.string.menu_option_chatbot_fr);
//            menu_option_chatbotmodel_textView.setText(R.string.menu_option_chatbotmodel_fr);
            menu_option_volume_textView.setText(R.string.menu_option_volume_fr);
            menu_option_affichage_textView.setText(R.string.menu_option_affichage_fr);
            menu_option_emotion_textView.setText(R.string.menu_option_emotion_fr);
            menu_option_detectLanguage_textView.setText(R.string.menu_option_detectionLanguage_fr);
//            menu_option_mode_stream_textView.setText(R.string.menu_option_mode_stream_fr);
            menu_apiKey_textView.setText(R.string.menu_api_key_fr);
            menu_name_textView.setText(R.string.menu_name_fr);
            menu_header_textView.setText(R.string.menu_header_fr);

//            menu_option_tracking_activation_textView.setText(R.string.menu_option_tracking_activation_fr);
//            menu_option_tracking_camera_display_textView.setText(R.string.menu_option_tracking_camera_display_fr);
//            menu_option_tracking_head_textView.setText(R.string.menu_option_tracking_head_fr);
//            menu_option_tracking_body_textView.setText(R.string.menu_option_tracking_body_fr);
//            menu_option_tracking_auto_listen_textView.setText(R.string.menu_option_tracking_auto_listen_fr);
//            menu_option_tracking_invitation_textView.setText(R.string.menu_option_tracking_invitation_fr);
            //menu_option_tracking_invitation_chatGpt_textView.setText(R.string.menu_option_tracking_invitation_chatGpt_fr);

                menu_header_editText.setText(buddyGPTApplication.getparam(entete));


        }
        else {
            if (!modelDownloading){
                translateAndSetTextView(R.string.menu_title_en, menu_title,"");
                //translateAndSetTextView(R.string.menu_option_listening_duration_en,menu_option_listening_duration_textView,"");
                //translateAndSetTextView(R.string.menu_option_commande_en,menu_option_commande_textView,"");
                //translateAndSetTextView(R.string.menu_option_listening_attempt_en,menu_option_listening_attempt_textView,"");
                translateAndSetTextView(R.string.menu_option_langue_en,menu_option_langue_textView,"");
                translateAndSetTextView(R.string.menu_option_stt_en,menu_option_stt_textView,"");
                translateAndSetTextView(R.string.menu_option_tts_en,menu_option_tts_textView,"");
                translateAndSetTextView(R.string.menu_option_chatbot_en,menu_option_chatbot_textView,"");
                //translateAndSetTextView(R.string.menu_option_chatbotmodel_en,menu_option_chatbotmodel_textView,"");
                translateAndSetTextView(R.string.menu_option_volume_en,menu_option_volume_textView,"");
                translateAndSetTextView(R.string.menu_option_affichage_en,menu_option_affichage_textView,"");
                translateAndSetTextView(R.string.menu_option_emotion_en,menu_option_emotion_textView,"");
                translateAndSetTextView(R.string.menu_option_detectionLanguage_en,menu_option_detectLanguage_textView,"");
               // translateAndSetTextView(R.string.menu_option_mode_stream_en,menu_option_mode_stream_textView,"");
                translateAndSetTextView(R.string.menu_api_key_en,menu_apiKey_textView,"");
                translateAndSetTextView(R.string.menu_name_en,menu_name_textView,"");
                translateAndSetTextView(R.string.menu_header_en,menu_header_textView,"");
                translateAndSetTextView(0,menu_header_editText,buddyGPTApplication.getparam(header));
//                translateAndSetTextView(R.string.menu_option_listening_duration_hint_en,menu_option_listening_duration_editText,"");
//                translateAndSetTextView(R.string.menu_option_listening_attempt_hint_en,menu_option_listening_attempt_editText,"");
//                translateAndSetTextView(R.string.menu_option_tracking_activation_en,menu_option_tracking_activation_textView,"");
//                translateAndSetTextView(R.string.menu_option_tracking_camera_display_en,menu_option_tracking_camera_display_textView,"");
//                translateAndSetTextView(R.string.menu_option_tracking_head_en,menu_option_tracking_head_textView,"");
//                translateAndSetTextView(R.string.menu_option_tracking_body_en,menu_option_tracking_body_textView,"");
//                translateAndSetTextView(R.string.menu_option_tracking_auto_listen_en,menu_option_tracking_auto_listen_textView,"");
//                translateAndSetTextView(R.string.menu_option_tracking_invitation_en,menu_option_tracking_invitation_textView,"");
                //translateAndSetTextView(R.string.menu_option_tracking_invitation_chatGpt_en,menu_option_tracking_invitation_chatGpt_textView,"");


            }
            modelDownloading = true;

        }
    }
    private void translateAndSetTextView(int stringResId, final View view,String texteAtraduire) {
        String text="";
        if (stringResId!=0){
            text= getResources().getString(stringResId);
        }
        else {
            text=texteAtraduire;
        }
        buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(text)
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String translatedText) {
                        if (view instanceof EditText) {
                            ((EditText) view).setHint(translatedText);
                            if (stringResId==0) {
                                ((EditText) view).setText(translatedText);

                                    buddyGPTApplication.setparam(buddyGPTApplication.getLangue().getNom()+"entete",translatedText);
                                    set.setHeader(buddyGPTApplication.getparam(buddyGPTApplication.getLangue().getNom()+"entete"));

                            }
                        } else  if (view instanceof TextView) {
                            ((TextView) view).setText(translatedText);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "translatedText exception  " + e);
                    }
                });
    }
    private Handler handlerProgressBar = new Handler(Looper.getMainLooper());
    private Runnable runnableProgressBar = new Runnable() {
        @Override
        public void run() {
            launch_view.setVisibility(View.VISIBLE);
            timerEcoute = new CountDownTimer((long) Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds", "BuddyGPT.properties")) * 1000, 1000) {
                @Override
                public void onTick(long l) {
                    Log.e("MRAA","onTick response");
                    // Method left empty intentionally because no action needed on each tick.
                }

                @Override
                public void onFinish() {
                    Log.e("MIDO","onfinish timer mlkit");
                    if (modelDownloading){
                        if (buddyGPTApplication.getLangue().getNom().equals(langueEN)) {
                            Toast.makeText(SettingsActivity.this, R.string.mlkit_model_is_downloading_en, Toast.LENGTH_SHORT).show();
                        } else if (buddyGPTApplication.getLangue().getNom().equals(langueFR)){
                            Toast.makeText(SettingsActivity.this, R.string.mlkit_model_is_downloading_fr, Toast.LENGTH_SHORT).show();
                        } else if (buddyGPTApplication.getLangue().getNom().equals(langueES)){
                            Toast.makeText(SettingsActivity.this, R.string.mlkit_model_is_downloading_es, Toast.LENGTH_SHORT).show();
                        } else if (buddyGPTApplication.getLangue().getNom().equals(langueDE)){
                            Toast.makeText(SettingsActivity.this, R.string.mlkit_model_is_downloading_de, Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Log.e("MIDO","onfinish affichage toast else");
                            Toast.makeText(SettingsActivity.this, R.string.mlkit_model_is_downloading_en, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            };

            timerEcoute.start();
        }
    };

    private IMLKitDownloadCallback imlKitDownloadCallback = new IMLKitDownloadCallback() {
        @Override
        public void onDownloadEnd(boolean success,String english_or_french) {
            if(success){
                switch (english_or_french) {
                    case "english":
                        english_is_downloaded = true;
                        break;
                    case "french":
                        french_is_downloaded = true;
                        break;
                }
                if (english_is_downloaded && french_is_downloaded) {
                    handlerProgressBar.removeCallbacksAndMessages(null);
                    handlerProgressBar.removeCallbacks(runnableProgressBar);

                    launch_view.setVisibility(View.INVISIBLE);
                    modelDownloading = false;
                    setLanguageText();
                    langueSpinnerAdapter.notifyDataSetChanged();
                }
            }
            else{
                french_is_downloaded = false;
                english_is_downloaded = false;
                buddyGPTApplication.downloadModel(imlKitDownloadCallback, buddyGPTApplication.getLangue().getLanguageCode().split("-")[0].trim());
                handlerProgressBar.postDelayed(runnableProgressBar,500);
            }

        }
    };
    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG," --- onResume() ---");

        buddyGPTApplication.hideSystemUI(this);
        buddyGPTApplication.setVolume(Integer.parseInt(buddyGPTApplication.getparam("speak_volume")), AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    @Override
    protected void onDestroy() {
        if (!buddyGPTApplication.getInitSharedpreferences()){
            buddyGPTApplication.setparam("firstLaunch","true");
            buddyGPTApplication.notifyObservers("ChatDestroy");
        }

        if(buddyGPTApplication.getDialog() != null && buddyGPTApplication.getDialog().isShowing()) buddyGPTApplication.getDialog().dismiss();


        try {
            unregisterReceiver(wifiBroadCastReceiver);
        }catch(IllegalArgumentException e) {
            Log.i(TAG,"---unregisterReceiver wifiBroadcast:: IllegalArgumentException---"+e.getMessage());
        }
        buddyGPTApplication.removeObserver(this);
        Log.d(TAG," --- onDestroy() ---");
        super.onDestroy();
    }

    @Override
    public void update(String message) throws IOException {

        if (message != null) {

            if(message.contains("main destroy")){
                buddyGPTApplication.setFileCreate(false);
                buddyGPTApplication.setparam("firstLaunch","false");
            }
            if (message.contains("isConnected")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        downloadingBar.setVisibility(View.VISIBLE);
                        noNetwork.setVisibility(View.GONE);

                    }
                });
            }
            if (message.contains("isNotConnected")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        downloadingBar.setVisibility(View.GONE);
                        noNetwork.setVisibility(View.VISIBLE);

                    }
                });
            }
            if (message.contains("changeDetected")){
                int speakVolume = buddyGPTApplication.getVolume();
                int max = buddyGPTApplication.getMaxVolume();
                int defaultVolume = buddyGPTApplication.getClosestInt((double) (speakVolume * 100) / max);
                Log.e("FCH","volumeMedia  "+String.valueOf(defaultVolume));
                buddyGPTApplication.setparam("speak_volume", String.valueOf(defaultVolume));
                buddyGPTApplication.setVolume(defaultVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                volume_seekbar_value.setText(defaultVolume + " %");
                set.setVolume(Integer.toString(defaultVolume));
                volume_seekbar.setProgress(defaultVolume);
            }
                if (message.contains("INVALID_TEAMGPT_KEY")){
                    refresh(0);
                    Log.i(TAG, "afterTextChanged: invalid");
                    if (buddyGPTApplication.getLangue().getNom().equals("Anglais")) {

                        buddyGPTApplication.showInputDialog(SettingsActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                    } else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                        buddyGPTApplication.showInputDialog(SettingsActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_fr), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_fr));
                    } else if (buddyGPTApplication.getLangue().getNom().equals("Espagnol")) {
                        buddyGPTApplication.showInputDialog(SettingsActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_es), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_es));
                    } else if (buddyGPTApplication.getLangue().getNom().equals("Allemand")) {
                        buddyGPTApplication.showInputDialog(SettingsActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_de), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_de));
                    } else {
                        buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                                .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en))
                                .addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String translatedText) {
                                        buddyGPTApplication.showInputDialog(SettingsActivity.this, translatedText, "Attention !");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        buddyGPTApplication.showInputDialog(SettingsActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                                    }
                                });
                    }
                }
                if (message.contains("INVALID_TEAMGPT_DEVICE_ID")){
                    refresh(0);
                    Log.i(TAG, "afterTextChanged: invalid");
                    if (buddyGPTApplication.getLangue().getNom().equals("Anglais")) {

                        buddyGPTApplication.showInputDialog(SettingsActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                    } else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                        buddyGPTApplication.showInputDialog(SettingsActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_fr), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_fr));
                    } else {
                        buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                                .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en))
                                .addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String translatedText) {
                                        buddyGPTApplication.showInputDialog(SettingsActivity.this, translatedText, "Attention !");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        buddyGPTApplication.showInputDialog(SettingsActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                                    }
                                });
                    }
                }

        }
    }
    @Override
    public void onSDKReady() {
        BuddySDK.UI.setMenuWidgetVisibility(FloatingWidgetVisibility.ALWAYS);
        BuddySDK.UI.setCloseWidgetVisibility(FloatingWidgetVisibility.ALWAYS);
        Log.w(TAG, "onSDKReady");
    }

    @Override
    public void onEvent(EventItem iEvent) {
        Log.w(TAG, "onEvent : "+iEvent.toString());
    }
    public void btnCloseSettings(View view) {
        isClickedBtnCloseSettings=true;
        buddyGPTApplication.setSetting(set);


        //if(!set.getChatbot().equals(setting.getChatbot())) {
            buddyGPTApplication.setFileCreate(true);
        //}

        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        intent.putExtra("fromSettings", "true");
        finish();
        startActivity(intent);
        overridePendingTransition(0, 0);

    }

    public void btnAfficheLanguageList(View view){
        Intent detailsIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        detailsIntent.setPackage("com.google.android.googlequicksearchbox");
      //  languageDetailsChecker = new LanguageDetailsChecker(SettingsActivity.this);
        sendOrderedBroadcast(detailsIntent, null, languageDetailsChecker, null, Activity.RESULT_OK, null, null);
    }
//    public void btnOpenAi(View view) {
//        Intent intentOpenAiActivity = new Intent(getApplicationContext(), OpenAiActivity.class);
//        startActivity(intentOpenAiActivity);
//        overridePendingTransition(0, 0);
//    }
//    @Override
//    public void onLanguagesReceived(ArrayList<String> languages) {
//        translateTitle(languages);
//    }
    private void translateTitle(ArrayList<String> languages){
        if(buddyGPTApplication.getLangue().getNom().equals("Anglais")){
            showLanguageDialog(languages,getResources().getString(R.string.menu_dialog_supported_languages_Android_STT_en));
        }
        else if(buddyGPTApplication.getLangue().getNom().equals("Français")){
            showLanguageDialog(languages,getResources().getString(R.string.menu_dialog_supported_languages_Android_STT_fr));
        }
        else if(buddyGPTApplication.getLangue().getNom().equals("Espagnol")){
            showLanguageDialog(languages,getResources().getString(R.string.menu_dialog_supported_languages_Android_STT_es));
        }
        else if(buddyGPTApplication.getLangue().getNom().equals("Allemand")){
            showLanguageDialog(languages,getResources().getString(R.string.menu_dialog_supported_languages_Android_STT_de));
        }
        else {
            buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(getResources().getString(R.string.menu_dialog_supported_languages_Android_STT_en))
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String translatedText) {
                            showLanguageDialog(languages,translatedText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "translatedText exception  " + e);
                            showLanguageDialog(languages,getResources().getString(R.string.menu_dialog_supported_languages_Android_STT_en));

                        }
                    });
        }
    }
    private void showLanguageDialog(ArrayList<String> languages,String title) {


//        TextView titleView = findViewById(R.id.dialogTitle);
//        ListView listView = findViewById(R.id.dialogListView);
//
//
//        titleView.setText(title);
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, languages);
//        listView.setAdapter(adapter);
//
//        popupLanguageList.setVisibility(View.VISIBLE);
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

    public static void avoidSpinnerDropdownFocus(Spinner spinner) {
        try {
            Field listPopupField = Spinner.class.getDeclaredField("mPopup");
            listPopupField.setAccessible(true);
            Object listPopup = listPopupField.get(spinner);
            if (listPopup instanceof ListPopupWindow) {
                Field popupField = ListPopupWindow.class.getDeclaredField("mPopup");
                popupField.setAccessible(true);
                Object popup = popupField.get((ListPopupWindow) listPopup);
                if (popup instanceof PopupWindow) {
                    ((PopupWindow) popup).setFocusable(false);
                }
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
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