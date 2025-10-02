package com.robotique.aevaweb.buddygpt.fragments;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

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
import com.robotique.aevaweb.buddygpt.utilis.ResponseCallback;
import com.robotique.aevaweb.buddygpt.utilis.WifiBroadcastReceiver;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment implements IDBObserver {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;
    private static final String TAG = "BuddyGPT_SettingsFragment";
    private static final String EMAIL = "Email";
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
    private RelativeLayout lytCloseMenuSettings;
    private TextView menuTitle;
    private TextView menuOptionLangueTextView;
    private TextView menuOptionSttTextView;
    private TextView menuOptionTtsTextView;
    private TextView menuOptionChatbotTextView;
    private TextView menuOptionVolumeTextView;
    private TextView menuOptionAffichageTextViewiew;
    private TextView menuOptionEmotionTextView;
    private TextView menuOptionDetectLanguageTextView;
    private TextView menuHeaderTextView;
    private TextView menuApiKeyTextView;
    private TextView menuNameTextView;
    private Spinner menuOptionLangueSpinner;
    private Spinner menuOptionSttSpinner;
    private Spinner menuOptionTtsSpinner;
    private TextView menuOptionChatbotSpinner;
    private TextView menuHeaderEditText;
    private EditText menuApiKeyEditText;
    private TextView menuNameText;
    private TextView copyRight;
    private TextView identifiers;
    private TextView volumeSeekbarValue;
    private SeekBar volumeSeekbar;
    private Switch switchTrackingActivation;
    private Switch switchTrackingCameraDisplay;
    private Switch switchTrackingAutoListen;
    private LinearLayout menu_option_tracking_camera_display_lyt;

    private LinearLayout menu_option_tracking_auto_listen_lyt;

    private TextView menu_option_tracking_activation_textView;
    private TextView menu_option_tracking_camera_display_textView;
    private TextView menu_option_tracking_auto_listen_textView;
    private Setting set;
    private Setting setting;
    private List<Langue> langues;
    private ResponseFromTeamGPT responseFromTeamGPT;
    private Boolean modelDownloading = false;
    private boolean englishIsDownloaded = false;
    private boolean french_is_downloaded = false;
    private boolean isCalledOnce = false; // focus changed
    private int chosenLanguagePos = -1;
    private int chosenSTTPos = -1;
    private int chosenTTSPos = -1;
    private CountDownTimer timerEcoute;
    private final Runnable runnableProgressBar = new Runnable() {
        @Override
        public void run() {
            FragmentActivity activity = getActivity();
            if (activity == null || !isAdded()) {
                Log.w(TAG, "runnableProgressBar aborted: fragment not attached");
                return;
            }
            launch_view.setVisibility(View.VISIBLE);
            timerEcoute = new CountDownTimer((long) Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds", "BuddyGPT.properties")) * 1000, 1000) {
                @Override
                public void onTick(long l) {
                    Log.e(TAG, "onTick response");
                    // Method left empty intentionally because no action needed on each tick.
                }

                @Override
                public void onFinish() {
                    Log.e(TAG, "onfinish timer mlkit");
                    if (Boolean.TRUE.equals(modelDownloading)) {
                        Context ctx = (buddyGPTApplication != null) ? buddyGPTApplication : activity;
                        String currentLang = null;
                        if (buddyGPTApplication != null && buddyGPTApplication.getLangue() != null) {
                            currentLang = buddyGPTApplication.getLangue().getNom();
                        }
                        if (langueEN.equals(currentLang)) {
                            Toast.makeText(ctx, ctx.getString(R.string.mlkit_model_is_downloading_en), Toast.LENGTH_SHORT).show();
                        } else if (langueFR.equals(currentLang)) {
                            Toast.makeText(ctx, ctx.getString(R.string.mlkit_model_is_downloading_fr), Toast.LENGTH_SHORT).show();
                        } else if (langueES.equals(currentLang)) {
                            Toast.makeText(ctx, ctx.getString(R.string.mlkit_model_is_downloading_es), Toast.LENGTH_SHORT).show();
                        } else if (langueDE.equals(currentLang)) {
                            Toast.makeText(ctx, ctx.getString(R.string.mlkit_model_is_downloading_de), Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "onfinish affichage toast else");
                            Toast.makeText(ctx, ctx.getString(R.string.mlkit_model_is_downloading_en), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            };

            timerEcoute.start();
        }
    };
    private final IMLKitDownloadCallback imlKitDownloadCallback = new IMLKitDownloadCallback() {
        @Override
        public void onDownloadEnd(boolean success, String englishOrFrench) {
            if (success) {
                // mark the specific language as downloaded
                if ("english".equalsIgnoreCase(englishOrFrench)) {
                    englishIsDownloaded = true;
                } else if ("french".equalsIgnoreCase(englishOrFrench)) {
                    french_is_downloaded = true;
                }

                // Stop progress UI and consider download finished for closing the fragment
                handlerProgressBar.removeCallbacksAndMessages(null);
                handlerProgressBar.removeCallbacks(runnableProgressBar);
                modelDownloading = false;

                FragmentActivity activity = getActivity();
                if (activity != null && isAdded()) {
                    launch_view.setVisibility(View.INVISIBLE);
                    setLanguageText();
                    if (langueSpinnerAdapter != null) {
                        langueSpinnerAdapter.notifyDataSetChanged();
                    }
                } else {
                    Log.w(TAG, "imlKitDownloadCallback: fragment not attached, skipping UI update");
                }
            } else {
                // download failed for that language: reset corresponding flag
                if ("english".equalsIgnoreCase(englishOrFrench)) {
                    englishIsDownloaded = false;
                } else if ("french".equalsIgnoreCase(englishOrFrench)) {
                    french_is_downloaded = false;
                }

                // restart UI/work only if fragment still attached
                if (getActivity() != null && isAdded()) {
                    String languageCode = buddyGPTApplication.getLangue().getLanguageCode().split("-")[0].trim();
                    buddyGPTApplication.downloadModel(imlKitDownloadCallback, languageCode);
                    handlerProgressBar.postDelayed(runnableProgressBar, 500);
                } else {
                    Log.w(TAG, "imlKitDownloadCallback: fragment not attached, not restarting model download UI");
                }
            }
        }
    };

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance(String param1, String param2) {
        SettingsFragment fragment = new SettingsFragment();
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
        View view= inflater.inflate(R.layout.fragment_settings, container, false);

        Switch switchLanguageDetection;
        Switch switchEmotion;
        Switch switchVisibility;
        LinearLayout popupLanguageListContent;
        RelativeLayout popupLanguageList;
        Log.d(TAG, " --- onCreate() ---");

        buddyGPTApplication = (BuddyGPTApplication) getActivity().getApplicationContext();
        buddyGPTApplication.hideSystemUI(getActivity());
        buddyGPTApplication.setInitSharedpreferences(false);



        menuTitle = view.findViewById(R.id.menu_title);
        popupLanguageList = view.findViewById(R.id.popup_Languages_List);
        popupLanguageListContent = view.findViewById(R.id.popup_Languages_List_linearLayout);
        lytCloseMenuSettings = view.findViewById(R.id.lyt_close_menu_settings);
        menuOptionSttLyt = view.findViewById(R.id.menu_option_stt_lyt);
        menuOptionTtsLyt = view.findViewById(R.id.menu_option_tts_lyt);
        menuOptionChatbotLyt = view.findViewById(R.id.menu_option_chatbot_lyt);

        menuOptionLangueTextView= view.findViewById(R.id.menu_option_langue_textView);
        menuOptionChatbotTextView = view.findViewById(R.id.menu_option_chatbot_textView);
        menuOptionSttTextView = view.findViewById(R.id.menu_option_stt_textView);
        menuOptionTtsTextView = view.findViewById(R.id.menu_option_tts_textView);

        menuOptionVolumeTextView = view.findViewById(R.id.menu_option_volume_textView);
        menuOptionAffichageTextViewiew = view.findViewById(R.id.menu_option_affichage_textView);
        menuOptionEmotionTextView = view.findViewById(R.id.menu_option_emotion_textView);
        menuOptionDetectLanguageTextView = view.findViewById(R.id.menu_option_language_detection_textView);
        menuApiKeyTextView = view.findViewById(R.id.api_key_txt);
        menuNameTextView = view.findViewById(R.id.name_txt);
        menuHeaderTextView = view.findViewById(R.id.header_txt);
        menuOptionLangueSpinner = view.findViewById(R.id.menu_option_langue_spinner);
        menuOptionSttSpinner = view.findViewById(R.id.menu_option_stt_spinner);
        menuOptionTtsSpinner = view.findViewById(R.id.menu_option_tts_spinner);
        menuOptionChatbotSpinner = view.findViewById(R.id.menu_option_chatbot_spinner);
        menuApiKeyEditText = view.findViewById(R.id.api_key_editText);
        menuNameText = view.findViewById(R.id.user_name);
        copyRight = view.findViewById(R.id.copyright_texte);
        identifiers = view.findViewById(R.id.identifiers_texte);
        menuHeaderEditText = view.findViewById(R.id.header_editText);

        volumeSeekbar = view.findViewById(R.id.volume_seekbar);
        volumeSeekbarValue = view.findViewById(R.id.volume_seekbar_value);
        switchVisibility = view.findViewById(R.id.switchVisibility);
        switchEmotion = view.findViewById(R.id.switchEmotion);
        switchLanguageDetection = view.findViewById(R.id.switchLanguageDetection);
        launch_view = view.findViewById(R.id.launch_view);
        noNetwork = view.findViewById(R.id.noNetwork);
        downloadingBar = view.findViewById(R.id.progressBar_MLKitDownload);
        menu_option_tracking_camera_display_lyt = view.findViewById(R.id.menu_option_tracking_camera_display_lyt);
        menu_option_tracking_auto_listen_lyt = view.findViewById(R.id.menu_option_tracking_auto_listen_lyt);
        menu_option_tracking_activation_textView = view.findViewById(R.id.menu_option_tracking_activation_textView);
        menu_option_tracking_camera_display_textView = view.findViewById(R.id.menu_option_tracking_camera_display_textView);
        menu_option_tracking_auto_listen_textView = view.findViewById(R.id.menu_option_tracking_auto_listen_textView);
        switchTrackingActivation = view.findViewById(R.id.switchTrackingActivation);
        switchTrackingCameraDisplay = view.findViewById(R.id.switchTrackingCameraDisplay);
        switchTrackingAutoListen = view.findViewById(R.id.switchTrackingAutoListen);

        set = new Setting();
        setting = new Setting();
        buddyGPTApplication.registerObserver(this);
        wifiBroadCastReceiver.setAct(getActivity().getApplicationContext());

        /**
         *  Gestion de l'api key
         */

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
        String can_change_stt = buddyGPTApplication.getParamFromFile("Change_STT", "BuddyGPT.properties");
        if(can_change_stt != null && can_change_stt.trim().equalsIgnoreCase("Yes")){
            if(buddyGPTApplication.getparam("STT-TeamGPT").equalsIgnoreCase("local"))
                menuOptionSttLyt.setVisibility(View.VISIBLE);

        }
        else{
            menuOptionSttLyt.setVisibility(View.GONE);
        }


        handlerSTT();
        handlerTTS();
        handlerSupport();
        handlerNameAndEmail();
        /**
         * Gestion Tracking
         */
        handlerTracking();

        setupClickListeners();
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
        return view;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // stop any pending progress runnable / timers to avoid callbacks after detach
        handlerProgressBar.removeCallbacksAndMessages(null);
        handlerProgressBar.removeCallbacks(runnableProgressBar);
        if (timerEcoute != null) {
            timerEcoute.cancel();
            timerEcoute = null;
        }
        modelDownloading = false;
        buddyGPTApplication.removeObserver(this);
    }

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

    private void setupClickListeners() {
        lytCloseMenuSettings.setOnClickListener(v -> btnCloseSettingsFragment());

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
        initLangues();
        setupSpinnerAdapter();
        selectChosenLanguage();
        setupSpinnerListener();
    }


    private void initLangues() {
        langues = new ArrayList<>();
        List<String> langueDisponible = buddyGPTApplication.getDisponibleLangue();

        for (int i = 1; i < langueDisponible.size(); i += 2) {
            String param = buddyGPTApplication.getparam(langueDisponible.get(i - 1));
            langues.add(new Gson().fromJson(param, Langue.class));
        }

        if (langues.isEmpty()) {
            langues.add(new Gson().fromJson(buddyGPTApplication.getparam(langueFR), Langue.class));
        }
    }


    private void setupSpinnerAdapter() {
        langueSpinnerAdapter = new LangueSpinnerAdapter(
                getActivity().getApplicationContext(),
                R.layout.spinner_item_layout_resource,
                R.id.item_name,
                R.id.checked_item_checked,
                langues
        );
        menuOptionLangueSpinner.setAdapter(langueSpinnerAdapter);
        avoidSpinnerDropdownFocus(menuOptionLangueSpinner);
    }


    private void selectChosenLanguage() {
        for (int index = 0; index < langues.size(); index++) {
            Langue lang = langues.get(index);
            if (lang.isChosen() || index == langues.size() - 1) {
                chosenLanguagePos = index;
                buddyGPTApplication.setLangue(lang);
                setting.setLangue(lang.getNom());
                break;
            }
        }

        menuOptionLangueSpinner.setSelection(chosenLanguagePos);
        menuOptionLangueSpinner.setEnabled(true);
        setLanguageText();
    }


    private void setupSpinnerListener() {
        menuOptionLangueSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Langue selectedLangue = (Langue) parent.getSelectedItem();
                updateChosenLanguage(selectedLangue, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });
    }


    private void updateChosenLanguage(Langue selectedLangue, int position) {
        chosenLanguagePos = position;
        for (Langue lang : langues) {
            lang.setChosen(lang.equals(selectedLangue));
        }

        buddyGPTApplication.setLangue(selectedLangue);
        set.setLangue(selectedLangue.getNom());
        modelDownloading = true;

        String languageCode = selectedLangue.getLanguageCode().split("-")[0].trim();
        buddyGPTApplication.downloadModel(imlKitDownloadCallback, languageCode);

        handlerProgressBar.postDelayed(runnableProgressBar, 500);
        saveLangueParams();
        langueSpinnerAdapter.updateDataSet(langues);
        setLanguageText();
    }


    private void saveLangueParams() {
        for (Langue lang : langues) {
            String json = new Gson().toJson(lang);
            switch (lang.getNom()) {
                case langueFR:
                    buddyGPTApplication.setparam(langueFR, json);
                    break;
                case langueEN:
                    buddyGPTApplication.setparam(langueEN, json);
                    break;
                case langueES:
                    buddyGPTApplication.setparam(langueES, json);
                    break;
                case langueDE:
                    buddyGPTApplication.setparam(langueDE, json);
                    break;
                default:
                    buddyGPTApplication.setparam(lang.getNom(), json);
            }
        }
    }

    private void handlerChatbot() {


        Log.i(TAG, "handlerChatbot: HOU" + buddyGPTApplication.getparam("SelectedChatbot"));

        menuOptionChatbotSpinner.setText(buddyGPTApplication.getparam("SelectedChatbot") + " " + buddyGPTApplication.getparam("chatbotModel"));//
    }

    private void handlerSTT() {


        final List<SttModel> sttList = new ArrayList<>();


        sttList.add(new SttModel(1, "Android", false));
        sttList.add(new SttModel(2, "Cerence", false));


        sttSpinnerAdapter = new SttSpinnerAdapter(getActivity().getApplicationContext(),
                R.layout.spinner_item_layout_resource,
                R.id.item_name,
                R.id.checked_item_checked,
                sttList);
        menuOptionSttSpinner.setAdapter(sttSpinnerAdapter);
        avoidSpinnerDropdownFocus(menuOptionSttSpinner);
        for (int i = 0; i < sttList.size(); i++) {
            if (sttList.get(i).getNom().equalsIgnoreCase(buddyGPTApplication.getparam("STT"))) {
                chosenSTTPos = i;

                break;
            }
        }

        menuOptionSttSpinner.setSelection(chosenSTTPos);
        menuOptionSttSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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


        ttsSpinnerAdapter = new TtsSpinnerAdapter(getActivity().getApplicationContext(),
                R.layout.spinner_item_layout_resource,
                R.id.item_name,
                R.id.checked_item_checked,
                ttsList);
        menuOptionTtsSpinner.setAdapter(ttsSpinnerAdapter);
        avoidSpinnerDropdownFocus(menuOptionTtsSpinner);
        for (int i = 0; i < ttsList.size(); i++) {
            if (ttsList.get(i).getNom().equalsIgnoreCase(buddyGPTApplication.getparam("TTS"))) {
                chosenTTSPos = i;

                break;
            }
        }

        menuOptionTtsSpinner.setSelection(chosenTTSPos);
        menuOptionTtsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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

    private void handlerTracking(){

        //Tracking activation
        if(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation"))){
            menu_option_tracking_camera_display_lyt.setVisibility(View.VISIBLE);
            menu_option_tracking_auto_listen_lyt.setVisibility(View.VISIBLE);

        }
        else{
            menu_option_tracking_camera_display_lyt.setVisibility(View.GONE);
            menu_option_tracking_auto_listen_lyt.setVisibility(View.GONE);
        }
        switchTrackingActivation.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation")));
        switchTrackingActivation.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            buddyGPTApplication.setparam("Tracking_Activation",String.valueOf(b));
            if(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation"))){
                menu_option_tracking_camera_display_lyt.setVisibility(View.VISIBLE);
                menu_option_tracking_auto_listen_lyt.setVisibility(View.VISIBLE);

            }
            else{
                menu_option_tracking_camera_display_lyt.setVisibility(View.GONE);

                menu_option_tracking_auto_listen_lyt.setVisibility(View.GONE);

            }
        });

        //Tracking camera display
        switchTrackingCameraDisplay.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Camera_Display")));
        switchTrackingCameraDisplay.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) -> buddyGPTApplication.setparam("Tracking_Camera_Display",String.valueOf(b)));

        //Tracking auto listen
        switchTrackingAutoListen.setChecked(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Auto_Listen")));
        switchTrackingAutoListen.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) -> buddyGPTApplication.setparam("Tracking_Auto_Listen",String.valueOf(b)));


    }

    private void handlerNameAndEmail() {

        Log.i(TAG, "handlerName: HOU" + buddyGPTApplication.getparam("NomCompte"));
        menuNameText.setText(buddyGPTApplication.getparam("NomCompte") + " " + buddyGPTApplication.getparam(EMAIL));

    }

    private void handlerSupport() {

        Log.i(TAG, "handlerSupport: HOU" + buddyGPTApplication.getparam("email_support"));
        copyRight.setText(getString(R.string.copyright) + " / " + buddyGPTApplication.getparam("email_support"));
        identifiers.setText(buddyGPTApplication.getparam("IdCompte") + " / " + buddyGPTApplication.getparam("IMEI_ID_Device"));
    }

    private void handlerSpeakVolume() {
        volumeSeekbarValue.setText(buddyGPTApplication.getparam(speakVolume) + "%");
        volumeSeekbar.setProgress(Integer.parseInt(buddyGPTApplication.getparam(speakVolume)));
        set.setVolume(buddyGPTApplication.getparam(speakVolume));
        setting.setVolume(buddyGPTApplication.getparam(speakVolume));
        buddyGPTApplication.setSpeakVolume(Integer.parseInt(buddyGPTApplication.getparam(speakVolume)));

        volumeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                buddyGPTApplication.setVolume(progress, AudioManager.FLAG_SHOW_UI);
                volumeSeekbarValue.setText(progress + " %");
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
        menuHeaderEditText.setFocusable(false);
        menuHeaderEditText.setClickable(false);

        if (buddyGPTApplication.getLangue().getNom().equals(langueEN)) {

            menuHeaderEditText.setText(buddyGPTApplication.getparam(header));

        } else if (buddyGPTApplication.getLangue().getNom().equals(langueFR)) {
            Log.i(TAG, "handlerHeader: HOU 2" + buddyGPTApplication.getparam(entete));
            menuHeaderEditText.setText(buddyGPTApplication.getparam(entete));

        } else {
            Log.i(TAG, "handlerHeader: HOU 3" + buddyGPTApplication.getparam(header));
            translateAndSetTextView(0, menuHeaderEditText, buddyGPTApplication.getparam(header));

        }
    }

    /**
     * handlerApiKey
     */
    private void handlerApiKey() {
        menuApiKeyEditText.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);
        menuApiKeyEditText.setText(buddyGPTApplication.getparam(teamGPT_Key));
        set.setApiKey(buddyGPTApplication.getparam(teamGPT_Key));
        setting.setApiKey(buddyGPTApplication.getparam(teamGPT_Key));


        menuApiKeyEditText.addTextChangedListener(new TextWatcher() {
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

        menuApiKeyEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                Log.i(TAG, "handlerApiKey: hasFocus");
                View decorView = getActivity().getWindow().getDecorView();
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
                    buddyGPTApplication.hideSystemUI(getActivity());
                    buddyGPTApplication.setparam(teamGPT_Key, menuApiKeyEditText.getText().toString());
                    Log.i("TAG", "run: menuApiKeyEditText" + menuApiKeyEditText.getText().toString());

                    if (menuApiKeyEditText.getText().toString().equals("")) {
                        Log.i("TAG", "run: getParameters");
                        buddyGPTApplication.resetSharedPreferences();
                        refresh(0);
                    } else {
                        Log.i("TAG", "run: getParameters else");
                        if (buddyGPTApplication.getparam("Mail_Destination").equalsIgnoreCase(buddyGPTApplication.getparam(EMAIL))) {
                            buddyGPTApplication.setparam("Mail_Destination", "");
                        }
                        if (buddyGPTApplication.getResponseFromTeamGPT() != null) {
                            Log.w("BuddyGPT", "buddyGPTApplication.getResponseFromTeamGPT()!=null ");
                            buddyGPTApplication.getResponseFromTeamGPT().getParameters(new ResponseCallback() {
                                @Override
                                public void onSuccess() {
                                    Log.i(TAG, "onSuccess getParameters ");
                                }

                                @Override
                                public void onFailure() {
                                    Log.i(TAG, "onFailure: getParameters");
                                }
                            });                        }
                        refresh(1);
                    }

                }
            }
        });

        menuApiKeyEditText.setOnEditorActionListener((textView, i, keyEvent) -> {

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
                    if (buddyGPTApplication.getparam("Mail_Destination").equalsIgnoreCase(buddyGPTApplication.getparam(EMAIL))) {
                        buddyGPTApplication.setparam("Mail_Destination", "");
                    }
                    if (buddyGPTApplication.getResponseFromTeamGPT() != null)
                        buddyGPTApplication.getResponseFromTeamGPT().getParameters(new ResponseCallback() {
                            @Override
                            public void onSuccess() {
                                Log.i(TAG, "onSuccess getParameters ");
                            }

                            @Override
                            public void onFailure() {
                                Log.i(TAG, "onFailure getParameters: ");
                            }
                        });
                    refresh(1);
                }
            }


            return false;
        });
    }

    private void refresh(int state) {
            FragmentActivity activity = getActivity();
            if (activity == null || !isAdded()) return; // Ajout de la vérification

            activity.runOnUiThread(() -> {
            if (state == 1) {// refresh with new values
                buddyGPTApplication.setparam("session_id", "");
                if (buddyGPTApplication.getLangue().getNom().equals(langueEN)) {
                    menuHeaderEditText.setText(buddyGPTApplication.getparam(header));
                } else if (buddyGPTApplication.getLangue().getNom().equals(langueFR)) {
                    menuHeaderEditText.setText(buddyGPTApplication.getparam(entete));
                } else if (buddyGPTApplication.getLangue().getNom().equals(langueES)) {
                    menuHeaderEditText.setText(buddyGPTApplication.getparam(cabecera));
                } else if (buddyGPTApplication.getLangue().getNom().equals(langueDE)) {
                    menuHeaderEditText.setText(buddyGPTApplication.getparam(kopfzeile));
                }
                if (buddyGPTApplication.getparam("STT-TeamGPT").equalsIgnoreCase("local"))
                    menuOptionSttLyt.setVisibility(View.VISIBLE);
                else if (buddyGPTApplication.getparam("STT-TeamGPT").equalsIgnoreCase(""))
                    menuOptionSttLyt.setVisibility(View.GONE);
                if (buddyGPTApplication.getparam("TTS-TeamGPT").equalsIgnoreCase("local"))
                    menuOptionTtsLyt.setVisibility(View.VISIBLE);
                else if (buddyGPTApplication.getparam("TTS-TeamGPT").equalsIgnoreCase(""))
                    menuOptionTtsLyt.setVisibility(View.GONE);

                menuOptionChatbotSpinner.setText(buddyGPTApplication.getparam("SelectedChatbot") + " " + buddyGPTApplication.getparam("chatbotModel"));
                menuNameText.setText(buddyGPTApplication.getparam("NomCompte") + " " + buddyGPTApplication.getparam(EMAIL));
                if (buddyGPTApplication.getparam("Mail_Destination").equalsIgnoreCase(""))
                    buddyGPTApplication.setparam("Mail_Destination", buddyGPTApplication.getparam(EMAIL));

                copyRight.setText(getString(R.string.copyright) + " / " + buddyGPTApplication.getparam("email_support"));
                identifiers.setText(buddyGPTApplication.getparam("IdCompte") + " / " + buddyGPTApplication.getparam("IMEI_ID_Device"));
            } else {// refresh with null
                menuHeaderEditText.setText("");
                menuOptionSttLyt.setVisibility(View.GONE);
                menuOptionTtsLyt.setVisibility(View.GONE);
                menuOptionChatbotSpinner.setText("");
                menuNameText.setText("");
                copyRight.setText(getString(R.string.copyright) + " / _");
                identifiers.setText("_ / _");

            }

        });

    }

    public void setLanguageText() {
        if (buddyGPTApplication.getLangue().getNom().equals(langueEN)) {
            menuTitle.setText(R.string.menu_title_en);
            menuOptionLangueTextView.setText(R.string.menu_option_langue_en);
            menuOptionTtsTextView.setText(R.string.menu_option_tts_en);
            menuOptionSttTextView.setText(R.string.menu_option_stt_en);
            menuOptionChatbotTextView.setText(R.string.menu_option_chatbot_en);
            menuOptionVolumeTextView.setText(R.string.menu_option_volume_en);
            menuOptionAffichageTextViewiew.setText(R.string.menu_option_affichage_en);
            menuOptionEmotionTextView.setText(R.string.menu_option_emotion_en);
            menuOptionDetectLanguageTextView.setText(R.string.menu_option_detectionLanguage_en);
            menuApiKeyTextView.setText(R.string.menu_api_key_en);
            menuNameTextView.setText(R.string.menu_name_en);
            menuHeaderTextView.setText(R.string.menu_header_en);
            menu_option_tracking_activation_textView.setText(R.string.menu_option_tracking_activation_en);
            menu_option_tracking_camera_display_textView.setText(R.string.menu_option_tracking_camera_display_en);
            menu_option_tracking_auto_listen_textView.setText(R.string.menu_option_tracking_auto_listen_en);
            menuHeaderEditText.setText(buddyGPTApplication.getparam(header));
        } else if (buddyGPTApplication.getLangue().getNom().equals(langueFR)) {
            menuTitle.setText(R.string.menu_title_fr);

            menuOptionLangueTextView.setText(R.string.menu_option_langue_fr);
            menuOptionSttTextView.setText(R.string.menu_option_stt_fr);
            menuOptionTtsTextView.setText(R.string.menu_option_tts_fr);
            menuOptionChatbotTextView.setText(R.string.menu_option_chatbot_fr);
            menuOptionVolumeTextView.setText(R.string.menu_option_volume_fr);
            menuOptionAffichageTextViewiew.setText(R.string.menu_option_affichage_fr);
            menuOptionEmotionTextView.setText(R.string.menu_option_emotion_fr);
            menuOptionDetectLanguageTextView.setText(R.string.menu_option_detectionLanguage_fr);
            menuApiKeyTextView.setText(R.string.menu_api_key_fr);
            menuNameTextView.setText(R.string.menu_name_fr);
            menuHeaderTextView.setText(R.string.menu_header_fr);
            menu_option_tracking_activation_textView.setText(R.string.menu_option_tracking_activation_fr);
            menu_option_tracking_camera_display_textView.setText(R.string.menu_option_tracking_camera_display_fr);
            menu_option_tracking_auto_listen_textView.setText(R.string.menu_option_tracking_auto_listen_fr);
            menuHeaderEditText.setText(buddyGPTApplication.getparam(entete));


        } else {
            if (Boolean.FALSE.equals(modelDownloading)) {
                translateAndSetTextView(R.string.menu_title_en, menuTitle, "");
                translateAndSetTextView(R.string.menu_option_langue_en, menuOptionLangueTextView, "");
                translateAndSetTextView(R.string.menu_option_stt_en, menuOptionSttTextView, "");
                translateAndSetTextView(R.string.menu_option_tts_en, menuOptionTtsTextView, "");
                translateAndSetTextView(R.string.menu_option_chatbot_en, menuOptionChatbotTextView, "");
                translateAndSetTextView(R.string.menu_option_volume_en, menuOptionVolumeTextView, "");
                translateAndSetTextView(R.string.menu_option_affichage_en, menuOptionAffichageTextViewiew, "");
                translateAndSetTextView(R.string.menu_option_emotion_en, menuOptionEmotionTextView, "");
                translateAndSetTextView(R.string.menu_option_detectionLanguage_en, menuOptionDetectLanguageTextView, "");
                translateAndSetTextView(R.string.menu_api_key_en, menuApiKeyTextView, "");
                translateAndSetTextView(R.string.menu_name_en, menuNameTextView, "");
                translateAndSetTextView(R.string.menu_header_en, menuHeaderTextView, "");
                translateAndSetTextView(0, menuHeaderEditText, buddyGPTApplication.getparam(header));
                translateAndSetTextView(R.string.menu_option_tracking_activation_en,menu_option_tracking_activation_textView,"");
                translateAndSetTextView(R.string.menu_option_tracking_camera_display_en,menu_option_tracking_camera_display_textView,"");
                translateAndSetTextView(R.string.menu_option_tracking_auto_listen_en,menu_option_tracking_auto_listen_textView,"");
            }


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
    }


    @Override
    public void update(String message) {

        if (message != null) {

            if (message.contains("main destroy")) {
                buddyGPTApplication.setFileCreate(false);
                buddyGPTApplication.setparam("firstLaunch", "false");
            }
            if (message.contains("isConnected")) {
                getActivity().runOnUiThread(() -> {

                    downloadingBar.setVisibility(View.VISIBLE);
                    noNetwork.setVisibility(View.GONE);

                });
            }
            if (message.contains("isNotConnected")) {
                getActivity().runOnUiThread(() -> {

                    downloadingBar.setVisibility(View.GONE);
                    noNetwork.setVisibility(View.VISIBLE);

                });
            }
            if (message.contains("changeDetected")) {
                int speakVolumeValue = buddyGPTApplication.getVolume();
                int max = buddyGPTApplication.getMaxVolume();
                int defaultVolume = buddyGPTApplication.getClosestInt((double) (speakVolumeValue * 100) / max);
                Log.e(TAG, "volumeMedia  " + defaultVolume);
                buddyGPTApplication.setparam(speakVolume, String.valueOf(defaultVolume));
                buddyGPTApplication.setVolume(defaultVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                volumeSeekbarValue.setText(defaultVolume + " %");
                set.setVolume(Integer.toString(defaultVolume));
                volumeSeekbar.setProgress(defaultVolume);
            }
            if (message.contains("INVALID_TEAMGPT_KEY")) {
                refresh(0);
                Log.i(TAG, "afterTextChanged: invalid");
                if (buddyGPTApplication.getLangue().getNom().equals(langueEN)) {

                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                } else if (buddyGPTApplication.getLangue().getNom().equals(langueFR)) {
                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_fr), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_fr));
                } else if (buddyGPTApplication.getLangue().getNom().equals(langueES)) {
                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_es), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_es));
                } else if (buddyGPTApplication.getLangue().getNom().equals(langueDE)) {
                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_de), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_de));
                } else {
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                            .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en))
                            .addOnSuccessListener(translatedText -> buddyGPTApplication.showInputDialog(getActivity(), translatedText, "Attention !"))
                            .addOnFailureListener(e -> buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en)));
                }
            }
            if (message.contains("INVALID_TEAMGPT_DEVICE_ID")) {
                refresh(0);
                Log.i(TAG, "afterTextChanged: invalid");
                if (buddyGPTApplication.getLangue().getNom().equals(langueEN)) {

                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                } else if (buddyGPTApplication.getLangue().getNom().equals(langueFR)) {
                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_fr), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_fr));
                } else {
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                            .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en))
                            .addOnSuccessListener(translatedText -> buddyGPTApplication.showInputDialog(getActivity(), translatedText, "Attention !"))
                            .addOnFailureListener(e -> buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en)));
                }
            }
            if (message.contains("ENV_ERROR")){

                if (buddyGPTApplication.getLangue().getNom().equals("Anglais")) {
                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_env_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                } else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_env_invalid_fr), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_fr));
                } else {
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                            .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_env_invalid_en))
                            .addOnSuccessListener(translatedText -> buddyGPTApplication.showInputDialog(getActivity(), translatedText,"Attention !"))
                            .addOnFailureListener(e -> buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_env_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en)));
                }

            }
        }
    }

    public void btnCloseSettingsFragment() {

        // If a model is downloading, block closing and inform the user
        if (Boolean.TRUE.equals(modelDownloading)) {
            Context ctx = (buddyGPTApplication != null) ? buddyGPTApplication : getActivity();
            String currentLang = null;
            if (buddyGPTApplication != null && buddyGPTApplication.getLangue() != null) {
                currentLang = buddyGPTApplication.getLangue().getNom();
            }
            String msg;
            if (langueEN.equals(currentLang)) {
                msg = ctx != null ? ctx.getString(R.string.mlkit_model_is_downloading_en) : "Model is downloading";
            } else if (langueFR.equals(currentLang)) {
                msg = ctx != null ? ctx.getString(R.string.mlkit_model_is_downloading_fr) : "Téléchargement en cours";
            } else {
                msg = ctx != null ? ctx.getString(R.string.mlkit_model_is_downloading_en) : "Model is downloading";
            }
            if (ctx != null) Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
            return;
        }
        // Save settings if needed
        buddyGPTApplication.setSetting(set);
        buddyGPTApplication.setFileCreate(true);
        // Replace SettingsFragment with MainFragment (only if still attached)
        if (getActivity() != null && isAdded()) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MainFragment())
                    .commitAllowingStateLoss();
        }
    }

}