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

import com.bfr.buddy.utils.events.EventItem;
import com.bfr.buddy.utils.values.FloatingWidgetVisibility;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;
import com.google.gson.Gson;
import com.robotique.aevaweb.buddygpt.R;
import com.robotique.aevaweb.buddygpt.adapters.LangueSpinnerAdapter;
import com.robotique.aevaweb.buddygpt.adapters.SttSpinnerAdapter;
import com.robotique.aevaweb.buddygpt.adapters.TtsSpinnerAdapter;
import com.robotique.aevaweb.buddygpt.application.BuddyGPTApplication;
import com.robotique.aevaweb.buddygpt.chatbotresponse.ResponseFromTeamGPT;
import com.robotique.aevaweb.buddygpt.models.Langue;
import com.robotique.aevaweb.buddygpt.models.Setting;
import com.robotique.aevaweb.buddygpt.models.SttModel;
import com.robotique.aevaweb.buddygpt.models.TtsModel;
import com.robotique.aevaweb.buddygpt.observers.IDBObserver;
import com.robotique.aevaweb.buddygpt.utilis.IMLKitDownloadCallback;
import com.robotique.aevaweb.buddygpt.utilis.LanguageDetailsChecker;
import com.robotique.aevaweb.buddygpt.utilis.WifiBroadcastReceiver;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BuddyActivity implements IDBObserver {
    private static final String TAG = "BuddyGPT_SettingsActivity";
    private static final String french = "Français";
    private static final String english = "Anglais";
    private static final String spanish = "Espagnol";
    private static final String deutsch = "Allemand";
    private static final String speakVolume = "speak_volume";
    private static final String visibilityString = "switch_visibility";
    private static final String emotionString = "switch_emotion";
    private static final String detectionLanguageString = "Detection_de_langue";
    private static final String langueFR = "Français";
    private static final String langueEN = "Anglais";
    private static final String langueES = "Espagnol";
    private static final String langueDE = "Allemand";
    private static final String header = "Header";
    private static final String entete = "Entete";
    private static final String cabecera = "Cabecera";
    private static final String kopfzeile = "Kopfzeile";
    private static final String teamGPT_Key = "TeamGPT_Key";
    private final WifiBroadcastReceiver wifiBroadCastReceiver = new WifiBroadcastReceiver();
    private final Handler handlerProgressBar = new Handler(Looper.getMainLooper());
    private BuddyGPTApplication buddyGPTApplication;
    private View decorView;
    private RelativeLayout launch_view;
    private ImageView noNetwork;
    private ProgressBar downloadingBar;
    private LangueSpinnerAdapter langueSpinnerAdapter;
    private SttSpinnerAdapter sttSpinnerAdapter;
    private TtsSpinnerAdapter ttsSpinnerAdapter;
    private LinearLayout menuOptionSttLyt;
    private LinearLayout menuOptionTtsLyt;
    private LinearLayout menuOptionChatbotLyt;
    private TextView menuTitle;
    private TextView menu_option_langue_textView;
    private TextView menu_option_stt_textView;
    private TextView menu_option_tts_textView;
    private TextView menu_option_chatbot_textView;
    private TextView menu_option_volume_textView;
    private TextView menu_option_affichage_textView;
    private TextView menu_option_emotion_textView;
    private TextView menu_option_detectLanguage_textView;
    private TextView menu_header_textView;
    private TextView menu_apiKey_textView;
    private TextView menu_name_textView;
    private Spinner menu_option_langue_spinner;
    private Spinner menu_option_stt_spinner;
    private Spinner menu_option_tts_spinner;
    private TextView menu_option_chatbot_spinner;
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
    private ResponseFromTeamGPT responseFromTeamGPT;
    private String teamGPTKeyValue = "";
    private Boolean modelDownloading = false;
    private boolean english_is_downloaded = false;
    private boolean french_is_downloaded = false;
    private boolean isCalledOnce = false; // focus changed
    private int chosenLanguagePos = -1;
    private int chosenSTTPos = -1;
    private int chosenTTSPos = -1;
    private CountDownTimer timerEcoute;
    private final Runnable runnableProgressBar = new Runnable() {
        @Override
        public void run() {
            launch_view.setVisibility(View.VISIBLE);
            timerEcoute = new CountDownTimer((long) Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds", "BuddyGPT.properties")) * 1000, 1000) {
                @Override
                public void onTick(long l) {
                    Log.e("MRAA", "onTick response");
                    // Method left empty intentionally because no action needed on each tick.
                }

                @Override
                public void onFinish() {
                    Log.e("MIDO", "onfinish timer mlkit");
                    if (Boolean.TRUE.equals(modelDownloading)) {
                        if (buddyGPTApplication.getLangue().getNom().equals(langueEN)) {
                            Toast.makeText(SettingsActivity.this, R.string.mlkit_model_is_downloading_en, Toast.LENGTH_SHORT).show();
                        } else if (buddyGPTApplication.getLangue().getNom().equals(langueFR)) {
                            Toast.makeText(SettingsActivity.this, R.string.mlkit_model_is_downloading_fr, Toast.LENGTH_SHORT).show();
                        } else if (buddyGPTApplication.getLangue().getNom().equals(langueES)) {
                            Toast.makeText(SettingsActivity.this, R.string.mlkit_model_is_downloading_es, Toast.LENGTH_SHORT).show();
                        } else if (buddyGPTApplication.getLangue().getNom().equals(langueDE)) {
                            Toast.makeText(SettingsActivity.this, R.string.mlkit_model_is_downloading_de, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("MIDO", "onfinish affichage toast else");
                            Toast.makeText(SettingsActivity.this, R.string.mlkit_model_is_downloading_en, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            };

            timerEcoute.start();
        }
    };
    private LanguageDetailsChecker languageDetailsChecker;

    public static void avoidSpinnerDropdownFocus(Spinner spinner) {
        try {
            Field listPopupField = Spinner.class.getDeclaredField("mPopup");
            Object listPopup = listPopupField.get(spinner);
            if (listPopup instanceof ListPopupWindow) {
                Field popupField = ListPopupWindow.class.getDeclaredField("mPopup");
                Object popup = popupField.get(listPopup);
                if (popup instanceof PopupWindow) {
                    ((PopupWindow) popup).setFocusable(false);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Switch switchLanguageDetection;
        Switch switchEmotion;
        Switch switchVisibility;
        LinearLayout popupLanguageListContent;
        RelativeLayout popupLanguageList;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Log.d(TAG, " --- onCreate() ---");

        buddyGPTApplication = (BuddyGPTApplication) getApplicationContext();
        buddyGPTApplication.hideSystemUI(this);
        buddyGPTApplication.setInitSharedpreferences(false);
        decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            if (menu_apiKey_editText != null && visibility == 0 && !menu_apiKey_editText.hasFocus()) {
                decorView.setSystemUiVisibility(buddyGPTApplication.hideSystemUI(SettingsActivity.this));
            }
        });


        menuTitle = findViewById(R.id.menu_title);
        popupLanguageList = findViewById(R.id.popup_Languages_List);
        popupLanguageListContent = findViewById(R.id.popup_Languages_List_linearLayout);
        menuOptionSttLyt = findViewById(R.id.menu_option_stt_lyt);
        menuOptionTtsLyt = findViewById(R.id.menu_option_tts_lyt);
        menuOptionChatbotLyt = findViewById(R.id.menu_option_chatbot_lyt);

        menu_option_langue_textView = findViewById(R.id.menu_option_langue_textView);
        menu_option_chatbot_textView = findViewById(R.id.menu_option_chatbot_textView);
        menu_option_stt_textView = findViewById(R.id.menu_option_stt_textView);
        menu_option_tts_textView = findViewById(R.id.menu_option_tts_textView);

        menu_option_volume_textView = findViewById(R.id.menu_option_volume_textView);
        menu_option_affichage_textView = findViewById(R.id.menu_option_affichage_textView);
        menu_option_emotion_textView = findViewById(R.id.menu_option_emotion_textView);
        menu_option_detectLanguage_textView = findViewById(R.id.menu_option_language_detection_textView);
        menu_apiKey_textView = findViewById(R.id.api_key_txt);
        menu_name_textView = findViewById(R.id.name_txt);
        menu_header_textView = findViewById(R.id.header_txt);
        menu_option_langue_spinner = findViewById(R.id.menu_option_langue_spinner);
        menu_option_stt_spinner = findViewById(R.id.menu_option_stt_spinner);
        menu_option_tts_spinner = findViewById(R.id.menu_option_tts_spinner);
        menu_option_chatbot_spinner = findViewById(R.id.menu_option_chatbot_spinner);
        menu_apiKey_editText = findViewById(R.id.api_key_editText);
        menu_nameText = findViewById(R.id.user_name);
        copyRight = findViewById(R.id.copyright_texte);
        identifiers = findViewById(R.id.identifiers_texte);
        menu_header_editText = findViewById(R.id.header_editText);

        volume_seekbar = findViewById(R.id.volume_seekbar);
        volume_seekbar_value = findViewById(R.id.volume_seekbar_value);
        switchVisibility = findViewById(R.id.switchVisibility);
        switchEmotion = findViewById(R.id.switchEmotion);
        switchLanguageDetection = findViewById(R.id.switchLanguageDetection);
        launch_view = findViewById(R.id.launch_view);
        noNetwork = findViewById(R.id.noNetwork);
        downloadingBar = findViewById(R.id.progressBar_MLKitDownload);
        set = new Setting();
        setting = new Setting();
        buddyGPTApplication.registerObserver(this);
        wifiBroadCastReceiver.setAct(getApplicationContext());
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(wifiBroadCastReceiver, intentFilter);
        wifiBroadCastReceiver.forceCheckConnexState(getApplicationContext());

        /**
         *  Gestion de l'api key
         */
        if (!buddyGPTApplication.getparam(teamGPT_Key).equalsIgnoreCase(""))
            teamGPTKeyValue = buddyGPTApplication.getparam(teamGPT_Key);
        handlerApiKey();


        /**
         *  Gestion des chatbots
         */

        menuOptionChatbotLyt.setVisibility(View.VISIBLE);

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
        switchVisibility.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) -> {
            buddyGPTApplication.setSwitchVisibility(String.valueOf(b));
            buddyGPTApplication.setparam(visibilityString, String.valueOf(b));
            set.setSwitchVisibility(String.valueOf(b));

        });

        /**
         *  Gestion de l'affichage des émotions
         */

        switchEmotion.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam(emotionString)));
        set.setSwitchEmotion(buddyGPTApplication.getparam(emotionString));
        setting.setSwitchEmotion(buddyGPTApplication.getparam(emotionString));
        buddyGPTApplication.setSwitchEmotion(buddyGPTApplication.getparam(emotionString));
        switchEmotion.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) -> {
            buddyGPTApplication.setSwitchEmotion(String.valueOf(b));
            buddyGPTApplication.setparam(emotionString, String.valueOf(b));
            set.setSwitchEmotion(String.valueOf(b));
        });
        /**
         *  Gestion de la detection des langues
         */

        switchLanguageDetection.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam(detectionLanguageString)));
        set.setSwitchLanguageDetection(buddyGPTApplication.getparam(detectionLanguageString));
        setting.setSwitchLanguageDetection(buddyGPTApplication.getparam(detectionLanguageString));
        buddyGPTApplication.setSwitchdetectLanguage(buddyGPTApplication.getparam(detectionLanguageString));
        switchLanguageDetection.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) -> {
            buddyGPTApplication.setSwitchdetectLanguage(String.valueOf(b));
            buddyGPTApplication.setparam(detectionLanguageString, String.valueOf(b));
            set.setSwitchLanguageDetection(String.valueOf(b));
        });

        if (responseFromTeamGPT != null) {
            responseFromTeamGPT.reset();
        }
        responseFromTeamGPT = new ResponseFromTeamGPT(buddyGPTApplication);
        if (buddyGPTApplication.getResponseFromTeamGPT() != null)
            buddyGPTApplication.getResponseFromTeamGPT().reset();
        buddyGPTApplication.setResponseFromTeamGPT(responseFromTeamGPT);

        /**
         *  Gestion de l'entete
         */
        handlerHeader();


        /**
         *  Gestion du choix STT
         */
        if (buddyGPTApplication.getparam("STT-TeamGPT").equalsIgnoreCase("local"))
            menuOptionSttLyt.setVisibility(View.VISIBLE);
        if (buddyGPTApplication.getparam("TTS-TeamGPT").equalsIgnoreCase("local"))
            menuOptionTtsLyt.setVisibility(View.VISIBLE);

        handlerSTT();
        handlerTTS();
        handlerSupport();
        handlerNameAndEmail();
        /**
         * Gestion Tracking
         */

        popupLanguageList.setOnClickListener(v -> {
            // Vérifier si le popup_add_mail est visible et si le clic est en dehors de celui-ci
            if (popupLanguageList.getVisibility() == View.VISIBLE) {
                MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);
                if (!isViewInsideBounds(popupLanguageListContent, (int) event.getRawX(), (int) event.getRawY())) {
                    // Si le clic est en dehors, rendre le popup invisible
                    popupLanguageList.setVisibility(View.INVISIBLE);
                }
            }
        });
        popupLanguageListContent.setOnClickListener(v -> {
            // Ne rien faire pour empêcher la propagation du clic aux éléments enfants du popup
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

    private void handlerLangue() {

        langues = new ArrayList<>();

        List<String> langueDisponible = buddyGPTApplication.getDisponibleLangue();
        for (int i = 1; i < langueDisponible.size(); i++) {

            langues.add(new Gson().fromJson(buddyGPTApplication.getparam(langueDisponible.get(i - 1)), Langue.class));
            i++;
        }
        if (langues.isEmpty()) {
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
            if (index == langues.size() - 1) {
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
                for (Langue langue : langues) {
                    langue.setChosen(false);
                    if (langue.equals(parent.getSelectedItem())) {
                        langue.setChosen(true);
                        buddyGPTApplication.setLangue(langue);
                        set.setLangue(langue.getNom());
                        modelDownloading = true;
                        buddyGPTApplication.downloadModel(imlKitDownloadCallback, buddyGPTApplication.getLangue().getLanguageCode().split("-")[0].trim());
                        handlerProgressBar.postDelayed(runnableProgressBar, 500);

                    }
                    if (langue.getNom().equals(langueFR)) {
                        buddyGPTApplication.setparam(french, new Gson().toJson(langue));
                    } else if (langue.getNom().equals(langueEN)) {
                        buddyGPTApplication.setparam(english, new Gson().toJson(langue));
                    } else if (langue.getNom().equals(langueES)) {
                        buddyGPTApplication.setparam(spanish, new Gson().toJson(langue));
                    } else if (langue.getNom().equals(langueDE)) {
                        buddyGPTApplication.setparam(deutsch, new Gson().toJson(langue));
                    } else {
                        buddyGPTApplication.setparam(langue.getNom(), new Gson().toJson(langue));
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

    private void handlerChatbot() {


        Log.i(TAG, "handlerChatbot: HOU" + buddyGPTApplication.getparam("SelectedChatbot"));

        menu_option_chatbot_spinner.setText(buddyGPTApplication.getparam("SelectedChatbot") + " " + buddyGPTApplication.getModel());//
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
                // document why this method is empty
            }
        });
    }

    private void handlerTTS() {

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
                //  why this method is empty
            }
        });
    }

    private void handlerNameAndEmail() {

        Log.i(TAG, "handlerName: HOU" + buddyGPTApplication.getparam("NomCompte"));
        menu_nameText.setText(buddyGPTApplication.getparam("NomCompte") + " " + buddyGPTApplication.getparam("Email"));

    }

    private void handlerSupport() {

        Log.i(TAG, "handlerSupport: HOU" + buddyGPTApplication.getparam("email_support"));
        copyRight.setText(getString(R.string.copyright) + " / " + buddyGPTApplication.getparam("email_support"));
        identifiers.setText(buddyGPTApplication.getparam("IdCompte") + " / " + buddyGPTApplication.getparam("IMEI_ID_Device"));
    }

    private void handlerSpeakVolume() {
        volume_seekbar_value.setText(buddyGPTApplication.getparam(speakVolume) + "%");
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

    private void handlerHeader() {
        menu_header_editText.setFocusable(false);
        menu_header_editText.setClickable(false);

        if (buddyGPTApplication.getLangue().getNom().equals(english)) {

            menu_header_editText.setText(buddyGPTApplication.getparam(header));

        } else if (buddyGPTApplication.getLangue().getNom().equals(french)) {
            Log.i(TAG, "handlerHeader: HOU 2" + buddyGPTApplication.getparam(entete));
            menu_header_editText.setText(buddyGPTApplication.getparam(entete));

        } else {
            Log.i(TAG, "handlerHeader: HOU 3" + buddyGPTApplication.getparam(header));
            translateAndSetTextView(0, menu_header_editText, buddyGPTApplication.getparam(header));

        }
    }

    /**
     * handlerApiKey
     */
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
                isCalledOnce = false;
                buddyGPTApplication.setparam(teamGPT_Key, charSequence.toString());
                set.setApiKey(buddyGPTApplication.getparam(teamGPT_Key));
                setting.setApiKey(buddyGPTApplication.getparam(teamGPT_Key));
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // document why this method is empty
            }
        });

        menu_apiKey_editText.setOnFocusChangeListener((v, hasFocus) -> {
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
                if (!isCalledOnce) {
                    isCalledOnce = true;
                    buddyGPTApplication.hideSystemUI(SettingsActivity.this);
                    buddyGPTApplication.setparam(teamGPT_Key, menu_apiKey_editText.getText().toString());
                    Log.i("TAG", "run: menu_apiKey_editText" + menu_apiKey_editText.getText().toString());

                    if (menu_apiKey_editText.getText().toString().equals("")) {
                        Log.i("TAG", "run: getParameters");
                        buddyGPTApplication.resetSharedPreferences();
                        refresh(0);
                    } else {
                        Log.i("TAG", "run: getParameters else");
                        if (buddyGPTApplication.getparam("Mail_Destination").equalsIgnoreCase(buddyGPTApplication.getparam("Email"))) {
                            buddyGPTApplication.setparam("Mail_Destination", "");
                        }
                        if (buddyGPTApplication.getResponseFromTeamGPT() != null) {
                            Log.w("BuddyGPT", "buddyGPTApplication.getResponseFromTeamGPT()!=null ");
                            buddyGPTApplication.getResponseFromTeamGPT().getParameters();
                        }
                        refresh(1);
                    }

                }
            }
        });

        menu_apiKey_editText.setOnEditorActionListener((textView, i, keyEvent) -> {

            if (!isCalledOnce) {
                isCalledOnce = true;
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
        });
    }

    private void refresh(int state) {

        runOnUiThread(() -> {
            if (state == 1) {// refresh with new values
                buddyGPTApplication.setparam("session_id", "");
                if (buddyGPTApplication.getLangue().getNom().equals(english)) {
                    menu_header_editText.setText(buddyGPTApplication.getparam(header));
                } else if (buddyGPTApplication.getLangue().getNom().equals(french)) {
                    menu_header_editText.setText(buddyGPTApplication.getparam(entete));
                } else if (buddyGPTApplication.getLangue().getNom().equals(spanish)) {
                    menu_header_editText.setText(buddyGPTApplication.getparam(cabecera));
                } else if (buddyGPTApplication.getLangue().getNom().equals(deutsch)) {
                    menu_header_editText.setText(buddyGPTApplication.getparam(kopfzeile));
                }
                if (buddyGPTApplication.getparam("STT-TeamGPT").equalsIgnoreCase("local"))
                    menuOptionSttLyt.setVisibility(View.VISIBLE);
                else if (buddyGPTApplication.getparam("STT-TeamGPT").equalsIgnoreCase(""))
                    menuOptionSttLyt.setVisibility(View.GONE);
                if (buddyGPTApplication.getparam("TTS-TeamGPT").equalsIgnoreCase("local"))
                    menuOptionTtsLyt.setVisibility(View.VISIBLE);
                else if (buddyGPTApplication.getparam("TTS-TeamGPT").equalsIgnoreCase(""))
                    menuOptionTtsLyt.setVisibility(View.GONE);

                menu_option_chatbot_spinner.setText(buddyGPTApplication.getparam("SelectedChatbot") + " " + buddyGPTApplication.getModel());
                menu_nameText.setText(buddyGPTApplication.getparam("NomCompte") + " " + buddyGPTApplication.getparam("Email"));
                if (buddyGPTApplication.getparam("Mail_Destination").equalsIgnoreCase(""))
                    buddyGPTApplication.setparam("Mail_Destination", buddyGPTApplication.getparam("Email"));

                copyRight.setText(getString(R.string.copyright) + " / " + buddyGPTApplication.getparam("email_support"));
                identifiers.setText(buddyGPTApplication.getparam("IdCompte") + " / " + buddyGPTApplication.getparam("IMEI_ID_Device"));
            } else {// refresh with null
                menu_header_editText.setText("");
                menuOptionSttLyt.setVisibility(View.GONE);
                menuOptionTtsLyt.setVisibility(View.GONE);
                menu_option_chatbot_spinner.setText("");
                menu_nameText.setText("");
                copyRight.setText(getString(R.string.copyright) + " / _");
                identifiers.setText("_ / _");

            }

        });

    }

    public void setLanguageText() {
        if (buddyGPTApplication.getLangue().getNom().equals("Anglais")) {
            menuTitle.setText(R.string.menu_title_en);
            menu_option_langue_textView.setText(R.string.menu_option_langue_en);
            menu_option_tts_textView.setText(R.string.menu_option_tts_en);
            menu_option_stt_textView.setText(R.string.menu_option_stt_en);
            menu_option_chatbot_textView.setText(R.string.menu_option_chatbot_en);
            menu_option_volume_textView.setText(R.string.menu_option_volume_en);
            menu_option_affichage_textView.setText(R.string.menu_option_affichage_en);
            menu_option_emotion_textView.setText(R.string.menu_option_emotion_en);
            menu_option_detectLanguage_textView.setText(R.string.menu_option_detectionLanguage_en);
            menu_apiKey_textView.setText(R.string.menu_api_key_en);
            menu_name_textView.setText(R.string.menu_name_en);
            menu_header_textView.setText(R.string.menu_header_en);
            menu_header_editText.setText(buddyGPTApplication.getparam(header));
        } else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
            menuTitle.setText(R.string.menu_title_fr);

            menu_option_langue_textView.setText(R.string.menu_option_langue_fr);
            menu_option_stt_textView.setText(R.string.menu_option_stt_fr);
            menu_option_tts_textView.setText(R.string.menu_option_tts_fr);
            menu_option_chatbot_textView.setText(R.string.menu_option_chatbot_fr);
            menu_option_volume_textView.setText(R.string.menu_option_volume_fr);
            menu_option_affichage_textView.setText(R.string.menu_option_affichage_fr);
            menu_option_emotion_textView.setText(R.string.menu_option_emotion_fr);
            menu_option_detectLanguage_textView.setText(R.string.menu_option_detectionLanguage_fr);
            menu_apiKey_textView.setText(R.string.menu_api_key_fr);
            menu_name_textView.setText(R.string.menu_name_fr);
            menu_header_textView.setText(R.string.menu_header_fr);

            menu_header_editText.setText(buddyGPTApplication.getparam(entete));


        } else {
            if (Boolean.FALSE.equals(modelDownloading)) {
                translateAndSetTextView(R.string.menu_title_en, menuTitle, "");
                translateAndSetTextView(R.string.menu_option_langue_en, menu_option_langue_textView, "");
                translateAndSetTextView(R.string.menu_option_stt_en, menu_option_stt_textView, "");
                translateAndSetTextView(R.string.menu_option_tts_en, menu_option_tts_textView, "");
                translateAndSetTextView(R.string.menu_option_chatbot_en, menu_option_chatbot_textView, "");
                translateAndSetTextView(R.string.menu_option_volume_en, menu_option_volume_textView, "");
                translateAndSetTextView(R.string.menu_option_affichage_en, menu_option_affichage_textView, "");
                translateAndSetTextView(R.string.menu_option_emotion_en, menu_option_emotion_textView, "");
                translateAndSetTextView(R.string.menu_option_detectionLanguage_en, menu_option_detectLanguage_textView, "");
                translateAndSetTextView(R.string.menu_api_key_en, menu_apiKey_textView, "");
                translateAndSetTextView(R.string.menu_name_en, menu_name_textView, "");
                translateAndSetTextView(R.string.menu_header_en, menu_header_textView, "");
                translateAndSetTextView(0, menu_header_editText, buddyGPTApplication.getparam(header));


            }
            modelDownloading = true;

        }
    }

    private void translateAndSetTextView(int stringResId, final View view, String texteAtraduire) {
        String text = "";
        if (stringResId != 0) {
            text = getResources().getString(stringResId);
        } else {
            text = texteAtraduire;
        }
        buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(text)
                .addOnSuccessListener(translatedText -> {
                    if (view instanceof EditText) {
                        ((EditText) view).setHint(translatedText);
                        if (stringResId == 0) {
                            ((EditText) view).setText(translatedText);

                            buddyGPTApplication.setparam(buddyGPTApplication.getLangue().getNom() + "entete", translatedText);
                            set.setHeader(buddyGPTApplication.getparam(buddyGPTApplication.getLangue().getNom() + "entete"));

                        }
                    } else if (view instanceof TextView) {
                        ((TextView) view).setText(translatedText);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "translatedText exception  " + e));
    }    private final IMLKitDownloadCallback imlKitDownloadCallback = new IMLKitDownloadCallback() {
        @Override
        public void onDownloadEnd(boolean success, String englishOrFrench) {
            if (success) {
                if (englishOrFrench.equals("english")) {
                    english_is_downloaded = true;
                } else if (englishOrFrench.equals("french")) {
                    french_is_downloaded = true;
                }
                if (english_is_downloaded && french_is_downloaded) {
                    handlerProgressBar.removeCallbacksAndMessages(null);
                    handlerProgressBar.removeCallbacks(runnableProgressBar);

                    launch_view.setVisibility(View.INVISIBLE);
                    modelDownloading = false;
                    setLanguageText();
                    langueSpinnerAdapter.notifyDataSetChanged();
                }
            } else {
                french_is_downloaded = false;
                english_is_downloaded = false;
                buddyGPTApplication.downloadModel(imlKitDownloadCallback, buddyGPTApplication.getLangue().getLanguageCode().split("-")[0].trim());
                handlerProgressBar.postDelayed(runnableProgressBar, 500);
            }

        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, " --- onResume() ---");

        buddyGPTApplication.hideSystemUI(this);
        buddyGPTApplication.setVolume(Integer.parseInt(buddyGPTApplication.getparam("speak_volume")), AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    @Override
    protected void onDestroy() {
        if (Boolean.FALSE.equals(buddyGPTApplication.getInitSharedpreferences())) {
            buddyGPTApplication.setparam("firstLaunch", "true");
            buddyGPTApplication.notifyObservers("ChatDestroy");
        }

        if (buddyGPTApplication.getDialog() != null && buddyGPTApplication.getDialog().isShowing())
            buddyGPTApplication.getDialog().dismiss();


        try {
            unregisterReceiver(wifiBroadCastReceiver);
        } catch (IllegalArgumentException e) {
            Log.i(TAG, "---unregisterReceiver wifiBroadcast:: IllegalArgumentException---" + e.getMessage());
        }
        buddyGPTApplication.removeObserver(this);
        Log.d(TAG, " --- onDestroy() ---");
        super.onDestroy();
    }

    @Override
    public void update(String message) throws IOException {

        if (message != null) {

            if (message.contains("main destroy")) {
                buddyGPTApplication.setFileCreate(false);
                buddyGPTApplication.setparam("firstLaunch", "false");
            }
            if (message.contains("isConnected")) {
                runOnUiThread(() -> {

                    downloadingBar.setVisibility(View.VISIBLE);
                    noNetwork.setVisibility(View.GONE);

                });
            }
            if (message.contains("isNotConnected")) {
                runOnUiThread(() -> {

                    downloadingBar.setVisibility(View.GONE);
                    noNetwork.setVisibility(View.VISIBLE);

                });
            }
            if (message.contains("changeDetected")) {
                int speakVolume = buddyGPTApplication.getVolume();
                int max = buddyGPTApplication.getMaxVolume();
                int defaultVolume = buddyGPTApplication.getClosestInt((double) (speakVolume * 100) / max);
                Log.e("FCH", "volumeMedia  " + defaultVolume);
                buddyGPTApplication.setparam("speak_volume", String.valueOf(defaultVolume));
                buddyGPTApplication.setVolume(defaultVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                volume_seekbar_value.setText(defaultVolume + " %");
                set.setVolume(Integer.toString(defaultVolume));
                volume_seekbar.setProgress(defaultVolume);
            }
            if (message.contains("INVALID_TEAMGPT_KEY")) {
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
                            .addOnSuccessListener(translatedText -> buddyGPTApplication.showInputDialog(SettingsActivity.this, translatedText, "Attention !"))
                            .addOnFailureListener(e -> buddyGPTApplication.showInputDialog(SettingsActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en)));
                }
            }
            if (message.contains("INVALID_TEAMGPT_DEVICE_ID")) {
                refresh(0);
                Log.i(TAG, "afterTextChanged: invalid");
                if (buddyGPTApplication.getLangue().getNom().equals("Anglais")) {

                    buddyGPTApplication.showInputDialog(SettingsActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                } else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                    buddyGPTApplication.showInputDialog(SettingsActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_fr), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_fr));
                } else {
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                            .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en))
                            .addOnSuccessListener(translatedText -> buddyGPTApplication.showInputDialog(SettingsActivity.this, translatedText, "Attention !"))
                            .addOnFailureListener(e -> buddyGPTApplication.showInputDialog(SettingsActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en)));
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
        Log.w(TAG, "onEvent : " + iEvent.toString());
    }

    public void btnCloseSettings(View view) {
        boolean isClickedBtnCloseSettings = false;
        isClickedBtnCloseSettings = true;
        buddyGPTApplication.setSetting(set);
        buddyGPTApplication.setFileCreate(true);


        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        intent.putExtra("fromSettings", "true");
        finish();
        startActivity(intent);
        overridePendingTransition(0, 0);

    }

    public void btnAfficheLanguageList(View view) {
        Intent detailsIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        detailsIntent.setPackage("com.google.android.googlequicksearchbox");
        sendOrderedBroadcast(detailsIntent, null, languageDetailsChecker, null, Activity.RESULT_OK, null, null);
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    /**
     * -------------------------------  Gestion d'affichage des barres du systemUI  ----------------------------------------------
     */

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            buddyGPTApplication.hideSystemUI(this);
        }
    }


}