package com.robotique.aevaweb.buddygpt.fragments;


import static androidx.camera.core.impl.utils.ContextUtil.getApplicationContext;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bfr.buddy.ui.shared.FaceTouchData;
import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.GazePosition;
import com.bfr.buddy.ui.shared.IUIFaceTouchCallback;
import com.bfr.buddy.ui.shared.LabialExpression;

import com.bfr.buddy.utils.values.FloatingWidgetVisibility;
import com.bfr.buddy.vision.shared.IVisionRsp;
import com.bfr.buddysdk.BuddyCompatActivity;
import com.bfr.buddysdk.BuddySDK;
import com.google.gson.Gson;
import com.robotique.aevaweb.buddygpt.R;
import com.robotique.aevaweb.buddygpt.activities.MainActivity;
import com.robotique.aevaweb.buddygpt.application.BuddyGPTApplication;
import com.robotique.aevaweb.buddygpt.chatbotresponse.ResponseFromTeamGPT;
import com.robotique.aevaweb.buddygpt.models.Langue;
import com.robotique.aevaweb.buddygpt.models.Replica;
import com.robotique.aevaweb.buddygpt.models.Session;
import com.robotique.aevaweb.buddygpt.models.Setting;
import com.robotique.aevaweb.buddygpt.observers.IDBObserver;
import com.robotique.aevaweb.buddygpt.utilis.BIPlayer;
import com.robotique.aevaweb.buddygpt.utilis.CustomToast;
import com.robotique.aevaweb.buddygpt.utilis.IMLKitDownloadCallback;
import com.robotique.aevaweb.buddygpt.utilis.ITTSCallbacks;
import com.robotique.aevaweb.buddygpt.utilis.WifiBroadcastReceiver;
import com.robotique.aevaweb.buddygpt.utilis.tracking.PoseTracking;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import static java.lang.String.format;
import static java.lang.String.valueOf;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.robotique.aevaweb.buddygpt.activities.ChatWindow;
import com.robotique.aevaweb.buddygpt.activities.SettingsActivity;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment implements IDBObserver{



    private static final String TAG = "BuddyGPT_MainActivity";
    private static final String TAG_TRACKING = "BuddyGPT_TRACKING_INFO";
    private static final String ANDROID_STT = "Android";
    private static final String CERENCE_STT = "Cerence";
    private static final String[] REQUESTED_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE

    };
    private static final int PERMISSION_REQ_ID = 22;
    private static final String LANGUE_FR = "Français";
    private static final String LANGUE_EN = "Anglais";
    private static final String LANGUE_ES = "Espagnol";
    private static final String LANGUE_DE = "Allemand";
    private static final String configFile = "BuddyGPT.properties";
    private final WifiBroadcastReceiver wifiBroadCastReceiver = new WifiBroadcastReceiver();
    private BuddyGPTApplication buddyGPTApplication;
    private View decorView;
    private final Random random = new Random();
    //views
    private RelativeLayout buddyTexteQstLyt;
    private RelativeLayout buddyTexteRespLyt;
    private RelativeLayout lytOpenMenuSettings;
    private RelativeLayout lytOpenMenuChat;
    private RelativeLayout launchView;
    private RelativeLayout reGroup;
    private TextView buddyTexteQst;
    private TextView buddyTexteResp;
    private ImageView noNetwork;
    private ProgressBar downloadingBar;
    private String infoToast = "";
    private boolean onSdkReadyIsAlreadyCalledOnce = false;
    private boolean isListeningFreeSpeech = false;
    private Boolean mlKitIsDownloading = false;
    private boolean englishIsDownloaded = false;
    private boolean frenchIsDownloaded = false;
    private boolean languageToenglishIsDownloaded = false;
    private boolean isSpeaking = false;
    private boolean isFirstLaunch = true; // Used to init TeamGPT params only once
    private final boolean regardeCamera = false;
    private CountDownTimer timerEcoute;
    private CountDownTimer responseTimeout;
    private ArrayList<Replica> listRep = new ArrayList<>();

    private Setting settingClass;
    private PoseTracking poseTracking;
    private ExecutorService backgroundExecutor;

    private ProcessCameraProvider cameraProvider;
    private String initOrMajOrNone = "";
    private final Handler handlerProgressBar = new Handler(Looper.getMainLooper());
    private final IMLKitDownloadCallback imlKitDownloadCallback = new IMLKitDownloadCallback() {
        @Override
        public void onDownloadEnd(boolean success, String englishOrFrench) {
            if (success) {
                if (englishOrFrench.equals("english")) {
                    englishIsDownloaded = true;
                } else if (englishOrFrench.equals("french")) {
                    frenchIsDownloaded = true;
                } else if (englishOrFrench.equals("languageToEnglish")) {
                    languageToenglishIsDownloaded = true;
                }
                if (englishIsDownloaded && frenchIsDownloaded && languageToenglishIsDownloaded) {

                    handlerProgressBar.removeCallbacksAndMessages(null);
                    handlerProgressBar.removeCallbacks(runnableProgressBar);
                    launchView.setVisibility(View.INVISIBLE);
                    if (initOrMajOrNone.equals("INIT")) {
                        if (buddyGPTApplication.getCurrentLanguage().equals("en")) {
                            infoToast = getString(R.string.toast_config_file_init_en);
                            CustomToast.getInstance().showInfo(getActivity(), infoToast, 2000);
                        } else if (buddyGPTApplication.getCurrentLanguage().equals("fr")) {
                            infoToast = getString(R.string.toast_config_file_init_fr);
                            CustomToast.getInstance().showInfo(getActivity(), infoToast, 2000);
                        } else if (buddyGPTApplication.getCurrentLanguage().equals("de")) {
                            infoToast = getString(R.string.toast_config_file_init_de);
                            CustomToast.getInstance().showInfo(getActivity(), infoToast, 2000);
                        } else if (buddyGPTApplication.getCurrentLanguage().equals("es")) {
                            infoToast = getString(R.string.toast_config_file_init_es);
                            CustomToast.getInstance().showInfo(getActivity(), infoToast, 2000);
                        } else {
                            buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                                    .translate(getString(R.string.toast_config_file_init_en))
                                    .addOnSuccessListener(translatedText -> {
                                        infoToast = translatedText;
                                        CustomToast.getInstance().showInfo(getActivity(), infoToast, 2000);
                                    })
                                    .addOnFailureListener(e -> {
                                        infoToast = getString(R.string.toast_config_file_init_en);
                                        CustomToast.getInstance().showInfo(getActivity(), infoToast, 2000);
                                    });
                        }
                    } else if (initOrMajOrNone.equals("MAJ")) {//traduire l'info du configFile :
                        if (buddyGPTApplication.getCurrentLanguage().equals("en")) {
                            infoToast = getString(R.string.toast_config_file_maj_en);
                            CustomToast.getInstance().showInfo(getActivity(), infoToast, 2000);
                        } else if (buddyGPTApplication.getCurrentLanguage().equals("fr")) {
                            infoToast = getString(R.string.toast_config_file_maj_fr);
                            CustomToast.getInstance().showInfo(getActivity(), infoToast, 2000);
                        } else if (buddyGPTApplication.getCurrentLanguage().equals("de")) {
                            infoToast = getString(R.string.toast_config_file_maj_de);
                            CustomToast.getInstance().showInfo(getActivity(), infoToast, 2000);
                        } else if (buddyGPTApplication.getCurrentLanguage().equals("es")) {
                            infoToast = getString(R.string.toast_config_file_maj_es);
                            CustomToast.getInstance().showInfo(getActivity(), infoToast, 2000);
                        } else {
                            buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                                    .translate(getString(R.string.toast_config_file_maj_en))
                                    .addOnSuccessListener(translatedText -> {
                                        infoToast = translatedText;
                                        CustomToast.getInstance().showInfo(getActivity(), infoToast, 2000);
                                    })
                                    .addOnFailureListener(e -> {
                                        infoToast = getString(R.string.toast_config_file_maj_en);
                                        CustomToast.getInstance().showInfo(getActivity(), infoToast, 2000);
                                    });
                        }
                    } else if (initOrMajOrNone.equals("NONE")) {
                        // COMMENT
                    }
                    mlKitIsDownloading = false;
                    if (!Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation"))) {
                        buddyGPTApplication.startListeningHotwor(getActivity());
                        reGroup.setTranslationY(1000);
                    } else if (Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation")) && !Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Auto_Listen"))) {
                        buddyGPTApplication.startListeningHotwor(getActivity());
                    }
                }
            } else {
                mlKitIsDownloading = true;
                frenchIsDownloaded = false;
                englishIsDownloaded = false;
                languageToenglishIsDownloaded = false;
                buddyGPTApplication.downloadModel(imlKitDownloadCallback, new Gson().fromJson(buddyGPTApplication.getparam(settingClass.getLangue()), Langue.class).getLanguageCode().split("-")[0].trim());
                handlerProgressBar.postDelayed(runnableProgressBar, 500);
            }

        }
    };
    private final Runnable runnableProgressBar = new Runnable() {
        @Override
        public void run() {
            buddyGPTApplication.stopTTS();
            stopListeningFreeSpeech();
            try {
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                BuddySDK.UI.stopListenAnimation();
            } catch (Exception e) {
                Log.e(TAG, "BuddySDK Exception :" + e);
            }
            launchView.setVisibility(View.VISIBLE);
            CountDownTimer timerDownloading = new CountDownTimer((long) Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds", configFile)) * 1000, 1000) {
                @Override
                public void onTick(long l) {
                    Log.e(TAG, "onTick mlKitIsDownloading");
                }

                @Override
                public void onFinish() {
                    Log.e(TAG, "on Finish mlKitIsDownloading");
                    if (Boolean.TRUE.equals(mlKitIsDownloading)) {
                        if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_EN)) {
                            Toast.makeText(getActivity(), R.string.mlkit_model_is_downloading_en, Toast.LENGTH_SHORT).show();
                        } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_FR)) {
                            Toast.makeText(getActivity(), R.string.mlkit_model_is_downloading_fr, Toast.LENGTH_SHORT).show();
                        } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_ES)) {
                            Toast.makeText(getActivity(), R.string.mlkit_model_is_downloading_es, Toast.LENGTH_SHORT).show();
                        } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_DE)) {
                            Toast.makeText(getActivity(), R.string.mlkit_model_is_downloading_de, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), R.string.mlkit_model_is_downloading_en, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            };
            timerDownloading.start();
        }
    };
    private Handler handlerForSensor;
    private Runnable runnableForSensor;
    private final Handler handlerTTSError = new Handler();
    private Runnable runnableTTSError;
    private final Handler handlerPauseTime = new Handler();
    private Runnable runnablePauseTime;
    private final Handler handler = new Handler();
    private Runnable runnable;
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private final IUIFaceTouchCallback iuiFaceTouchCallback = new IUIFaceTouchCallback.Stub() {
        @Override
        public void onTouch(FaceTouchData faceTouchData) {

            //Right_Eyebrow
            if (faceTouchData.getX() < 460 && faceTouchData.getY() < 250) {
                getActivity().runOnUiThread(() -> {
                    if (Integer.parseInt(buddyGPTApplication.getparam("speak_volume")) > 0) {
                        int curr = Integer.parseInt(buddyGPTApplication.getparam("speak_volume")) - 10;
                        if (curr < 0) {
                            curr = 0;
                        }
                        buddyGPTApplication.setVolume(curr, AudioManager.FLAG_SHOW_UI);
                        buddyGPTApplication.setparam("speak_volume", valueOf(curr));
                        buddyGPTApplication.setSpeakVolume(curr);
                        settingClass.setVolume(valueOf(curr));
                        Log.d(TAG, "RIGHT_EYE update volume : " + curr);
                    } else {
                        Log.d(TAG, "Volume MIN");
                    }
                });
            }

            //Left_Eyebrow
            else if (faceTouchData.getX() > 720 && faceTouchData.getY() < 250) {
                getActivity().runOnUiThread(() -> {
                    if (Integer.parseInt(buddyGPTApplication.getparam("speak_volume")) < 100) {
                        int curr = Integer.parseInt(buddyGPTApplication.getparam("speak_volume")) + 10;
                        if (curr > 100) {
                            curr = 100;
                        }
                        buddyGPTApplication.setVolume(curr, AudioManager.FLAG_SHOW_UI);
                        buddyGPTApplication.setparam("speak_volume", valueOf(curr));
                        buddyGPTApplication.setSpeakVolume(curr);
                        settingClass.setVolume(valueOf(curr));
                        Log.d(TAG, "LEFT_EYE update volume : " + curr);
                    } else {
                        Log.d(TAG, "Volume MAX");
                    }
                });
            }

            //eyes
            else if (faceTouchData.getY() > 250 && faceTouchData.getY() < 568) {
                Log.e("FCHH", "click1");
                if (buddyGPTApplication.getparam("Stimulis").equals("true")) {
                    Log.e("FCHH", "click");
                    if (Boolean.TRUE.equals(!buddyGPTApplication.getAppIsCurrentlyDealingWithTheQuestion()) && Boolean.TRUE.equals(!mlKitIsDownloading)) {
                        //
                    }
                }
            }

            //Mouth
            else if (faceTouchData.getX() > 400 && faceTouchData.getX() < 820 && faceTouchData.getY() > 568) {
                getActivity().runOnUiThread(() -> {
                    Log.d(TAG, "Mouth touched1");
                    isSpeaking = false;
                    if (handler != null && runnable != null) {
                        handler.removeCallbacks(runnable);
                        handler.removeCallbacksAndMessages(null);
                    }
                    if (responseTimeout != null) responseTimeout.cancel();
                    if (buddyGPTApplication.getResponseFromTeamGPT() != null)
                        buddyGPTApplication.getResponseFromTeamGPT().reset();
                    if (handlerTTSError != null && runnableTTSError != null) {
                        handlerTTSError.removeCallbacks(runnableTTSError);
                        handlerTTSError.removeCallbacksAndMessages(null);
                    }
                    if (Boolean.TRUE.equals(!buddyGPTApplication.getSpeaking()) && Boolean.TRUE.equals(!mlKitIsDownloading)) {
                        Log.d(TAG, "Mouth touched2");
                        if (buddyGPTApplication.getparam("INVALID_TEAMGPT_DEVICE_ID").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
                            Log.i("TAG", "run: notifyObservers INVALID_TEAMGPT_DEVICE_ID 3");
                            buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_DEVICE_ID");
                        } else if (buddyGPTApplication.getparam("INVALID_TEAMGPT_KEY").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
                            Log.i("TAG", "run: notifyObservers INVALID_TEAMGPT_KEY 3");
                            buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
                        } else if (buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
                            Log.i("TAG", "run: notifyObservers TEAMGPT_KEY EMPTY 3");
                            if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_EN)) {
                                buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_en));
                            } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_FR)) {
                                buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_fr));
                            } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_ES)) {
                                buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_es));
                            } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_DE)) {
                                buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_de));
                            } else {
                                buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                                        .translate(getString(R.string.toast_teamgpt_key_indispo_en))
                                        .addOnSuccessListener(translatedText -> buddyGPTApplication.showToast(translatedText))
                                        .addOnFailureListener(e -> buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_en)));
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

                    } else if (Boolean.TRUE.equals(buddyGPTApplication.getSpeaking()) && Boolean.TRUE.equals(!mlKitIsDownloading)) {
                        Log.d(TAG, "Mouth touched3 STT  " + buddyGPTApplication.getparam("STT") + " AUTRE " + buddyGPTApplication.getAppIsListeningToTheQuestion());
                        if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(ANDROID_STT)
                                || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(CERENCE_STT)
                                || Boolean.TRUE.equals(!buddyGPTApplication.getAppIsListeningToTheQuestion())) {
                            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                            buddyGPTApplication.setStartRecording(false);
                            buddyGPTApplication.setSpeaking(false);
                            buddyGPTApplication.setActivityClosed(true);
                            isListeningFreeSpeech = false;
                            buddyGPTApplication.stopTTS();
                            buddyGPTApplication.setStoredResponse("");
                            if (buddyTexteQstLyt != null && buddyTexteRespLyt != null && buddyTexteQst != null && buddyTexteResp != null) {
                                buddyTexteQstLyt.setVisibility(View.INVISIBLE);
                                buddyTexteRespLyt.setVisibility(View.INVISIBLE);
                                buddyTexteQst.setMovementMethod(null);
                                buddyTexteResp.setMovementMethod(null);
                            }
                            lytOpenMenuSettings.setVisibility(View.VISIBLE);
                            lytOpenMenuChat.setVisibility(View.VISIBLE);
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
                });
            }
        }

        @Override
        public void onRelease(FaceTouchData faceTouchData) {
            // comment
        }
    };
    private String mParam1;
    private String mParam2;

    public MainFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MainFragment.
     */
    public static MainFragment newInstance(String param1, String param2) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, " --- onDestroy() ---");
        BuddySDK.UI.removeFaceTouchListener(iuiFaceTouchCallback);
        buddyGPTApplication.setparam("firstLaunch", "true");
        if (buddyGPTApplication.getDialog() != null && buddyGPTApplication.getDialog().isShowing())
            buddyGPTApplication.getDialog().dismiss();
        buddyGPTApplication.setFileCreate(true);
        buddyGPTApplication.notifyObservers("main destroy");
        if (poseTracking != null) poseTracking.stopMovingAndCancelRunnables();
        if (backgroundExecutor != null) backgroundExecutor.shutdownNow();

        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
      buddyGPTApplication = (BuddyGPTApplication) getActivity().getApplicationContext();
      buddyGPTApplication.setInitSharedpreferences(true);
        if (BuddySDK.UI != null) {
            BuddySDK.UI.addFaceTouchListener(iuiFaceTouchCallback);
        } else {
            Log.e(TAG, "BuddySDK.UI is null, cannot add face touch listener");
        }
      //init views
      buddyTexteQst = view.findViewById(R.id.buddy_texte_qst);
      buddyTexteQstLyt = view.findViewById(R.id.buddy_texte_qst_lyt);
      buddyTexteResp = view.findViewById(R.id.buddy_texte_resp);
      buddyTexteRespLyt = view.findViewById(R.id.buddy_texte_resp_lyt);
      lytOpenMenuSettings = view.findViewById(R.id.lyt_open_menu_settings);
      lytOpenMenuChat = view.findViewById(R.id.lyt_open_menu_chat);
      launchView = view.findViewById(R.id.launch_view);
      noNetwork = view.findViewById(R.id.noNetwork);

      downloadingBar = view.findViewById(R.id.progressBar_MLKitDownload);
      reGroup = view.findViewById(R.id.reGroup);
      lytOpenMenuSettings.setOnClickListener(v -> btnOpenSettingsFragment());
      lytOpenMenuChat.setOnClickListener(v -> btnOpenChatFragment());

      Intent myIntent = getActivity().getIntent();
      isFirstLaunch = true;
      if (myIntent != null) {
          if (myIntent.hasExtra("fromSettings")) {
              String fromSettings = myIntent.getStringExtra("fromSettings");
              if (fromSettings != null && fromSettings.equals("true")) {
                  isFirstLaunch = false;
                  Log.i(TAG_TRACKING, "is back from Settings");
              }
          } else if (myIntent.hasExtra("fromChatWindow")) {
              String fromChatWindow = myIntent.getStringExtra("fromChatWindow");
              if (fromChatWindow != null && fromChatWindow.equals("true")) {
                  isFirstLaunch = false;
                  Log.i(TAG_TRACKING, "is back from ChatWindow");
              }
          }
      }
      else {
          buddyGPTApplication.setSpeaking(false);
          buddyGPTApplication.setNotYet(true);
          buddyGPTApplication.setActivityClosed(false);
          buddyGPTApplication.setStartRecording(false);
          buddyGPTApplication.setQuestionNumber(0);
          buddyGPTApplication.setCurrentQuestionNubmer(0);
          buddyGPTApplication.setAlreadyGetAnswer(false);
          buddyGPTApplication.setOpenaialreadySwitchEmotion(false);
          buddyGPTApplication.setTimeoutExpired(false);
          buddyGPTApplication.setQuestionTime(0);
          buddyGPTApplication.setStoredResponse("");
          buddyGPTApplication.setBuddyFaceisTired(false);
          buddyGPTApplication.setShouldPlayEmotion(false);
          buddyGPTApplication.setCurrentEmotion("");
          buddyGPTApplication.setMessageError(false);
          buddyGPTApplication.setCurrentIndexText(0);
          buddyGPTApplication.setAllTextPronoucedSuccess(true);
          buddyGPTApplication.setStopTTSReadSpeaker(false);
          buddyGPTApplication.setInitSharedpreferences(true);
          buddyGPTApplication.setLanguageDetected("");
          buddyGPTApplication.setAlreadyCalled(false);
          buddyGPTApplication.setRecording(false);
          buddyGPTApplication.setCurrentState("");


          buddyGPTApplication.setStopProcessus(false);
          buddyGPTApplication.setAlReadyHadSpoke(false);

          buddyGPTApplication.setResponseTime(0);
          buddyGPTApplication.setAnswerHasExceededTimeOut(false);
          buddyGPTApplication.setPreviousVolume(Float.valueOf(0));
          buddyGPTApplication.setAppIsListeningToTheQuestion(false);
          buddyGPTApplication.setChosenTTS("");
          buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
          buddyGPTApplication.setBIExecution(false);
          buddyGPTApplication.setAlreadyChatting(false);
          Log.i(TAG_TRACKING, "First launch of application");
      }

      /**
       * init animated drawables for timer
       */
      AnimationDrawable animationTimerPhoto = new AnimationDrawable();
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0001), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0002), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0003), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0004), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0005), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0006), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0007), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0008), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0009), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0010), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0011), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0012), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0013), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0014), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0015), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0016), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0017), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0018), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0019), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0020), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0021), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0022), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0023), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0024), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0025), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0026), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0027), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0028), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0029), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0030), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0031), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0032), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0033), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0034), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0035), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0036), 1000 / 37);
      animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0037), 1000 / 37);
        // Inflate the layout for this fragment
        return view;


    }



        /**
         * ------------------ App LifeCycle ---------------------
         */

        @SuppressLint("UseCompatLoadingForDrawables")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.d(TAG, " --- onCreate() ---");

        }



    public void btnOpenSettingsFragment() {
        if (Boolean.FALSE.equals(mlKitIsDownloading)) {
            if (getActivity() != null && isAdded()) {
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new SettingsFragment())
                        .commit();
            }
            getActivity().overridePendingTransition(0, 0);
        } else if (Boolean.TRUE.equals(buddyGPTApplication.getBIExecution())) {
            BIPlayer.getInstance().stopBehaviour();
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SettingsFragment())
                    .commit();

            getActivity().overridePendingTransition(0, 0);
        }
    }

    public void btnOpenChatFragment() {
        if (Boolean.FALSE.equals(mlKitIsDownloading)) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ChatFragment())
                    .commit();
            getActivity().overridePendingTransition(0, 0);
        } else if (Boolean.TRUE.equals(buddyGPTApplication.getBIExecution())) {
            BIPlayer.getInstance().stopBehaviour();
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ChatFragment())
                    .commit();
            getActivity().overridePendingTransition(0, 0);
        }
    }

        private void showStream(String responseTitle, String response) {
            if (!buddyGPTApplication.isActivityClosed()) {
                buddyTexteResp.setText(String.format("%s : %s", responseTitle, response));
                buddyTexteRespLyt.setVisibility(View.VISIBLE);
                buddyTexteResp.setMovementMethod(new ScrollingMovementMethod());
                // Scroll to the end
                buddyTexteResp.post(() -> {
                    int scrollAmount = buddyTexteResp.getLayout().getLineTop(buddyTexteResp.getLineCount())
                            - buddyTexteResp.getHeight() + buddyTexteResp.getLineHeight();
                    if (scrollAmount > 0) {
                        buddyTexteResp.scrollTo(0, scrollAmount);
                    } else {
                        buddyTexteResp.scrollTo(0, 0);
                    }
                });
            }
        }

        private void refreshSTTLangue() {
            buddyGPTApplication.refresh(new Gson().fromJson(buddyGPTApplication.getparam(settingClass.getLangue()), Langue.class).getLanguageCode(), getActivity());
        }

        /**
         * ------------------------------------------ STT  -------------------------------------------
         */

        private void startListeningFreeSpeech(int duration) {

            isListeningFreeSpeech = true;
            buddyGPTApplication.setMessageError(false);
            buddyGPTApplication.setOpenaialreadySwitchEmotion(false);
            buddyGPTApplication.setAppIsListeningToTheQuestion(true);
            buddyGPTApplication.setAlreadyChatting(false);

            Log.d(TAG, " --- startListeningFreeSpeech(" + duration + ") ---");
            Log.d(TAG, " --- startListeningFreeSpeech( STT" + buddyGPTApplication.getparam("STT") + ") ---");


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
            timerEcoute = new CountDownTimer(duration * 1000L, 1000) {
                @Override
                public void onTick(long l) {
                    Log.d(TAG, "timerEcoute onTick");
                }

                @Override
                public void onFinish() {
                    if (Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation")) && Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Auto_Listen")) && regardeCamera) {
                        Log.i(TAG_TRACKING, "timerEcoute onFinish --> Do not stop listening because tracking auto listen is enabled and user is looking directly at camera --> restart timer");
                        timerEcoute.start();
                    } else {
                        Log.i(TAG, "timerEcoute onFinish");
                        if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(ANDROID_STT) || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(CERENCE_STT)) {
                            buddyGPTApplication.notifyObservers("end of timer");
                        } else {
                            buddyGPTApplication.notifyObservers("Obtain audio transcription after the listening time has elapsed;SPLIT;false");
                        }
                    }
                }
            };
            timerEcoute.start();

        }

        private void stopListeningFreeSpeech() {
            isListeningFreeSpeech = false;
            Log.d(TAG, " --- stopListeningFreeSpeech() ---");
            if (timerEcoute != null) timerEcoute.cancel();
            buddyGPTApplication.stopListening(getActivity());
        }

        private void startCycle() {
            Log.e(TAG, "startCycle  after handler ");
            isListeningFreeSpeech = true;
            buddyGPTApplication.setMessageError(false);
            buddyGPTApplication.setOpenaialreadySwitchEmotion(false);
            buddyGPTApplication.setAppIsListeningToTheQuestion(true);
            buddyGPTApplication.setAlreadyChatting(false);

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
            timerEcoute = new CountDownTimer(buddyGPTApplication.getListeningDuration() * 1000L, 1000) {
                @Override
                public void onTick(long l) {
                    Log.d(TAG, "timerEcoute onTick");
                }

                @Override
                public void onFinish() {
                    if (Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation")) && Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Auto_Listen")) && regardeCamera) {
                        Log.i(TAG_TRACKING, "timerEcoute onFinish --> Do not stop listening because tracking auto listen is enabled and user is looking directly at camera --> restart timer");
                        timerEcoute.start();
                    } else {
                        Log.i(TAG, "timerEcoute onFinish");
                        if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(ANDROID_STT) || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(CERENCE_STT)) {
                            buddyGPTApplication.notifyObservers("end of cycle");
                            runnablePauseTime = () -> {
                                startNextCycle();
                                Log.e(TAG, "startNextCycle  after handler ");
                            };
                            handlerPauseTime.postDelayed(runnablePauseTime, 1000);
                        } else {
                            buddyGPTApplication.notifyObservers("Obtain audio transcription after the listening time has elapsed;SPLIT;true");
                        }
                    }
                }
            };
            timerEcoute.start();
        }

        private void startNextCycle() {
            Log.e(TAG, "startNextCycle  remainingattempts= " + buddyGPTApplication.getRemainingAttempts());
            if (buddyGPTApplication.getRemainingAttempts() > 0) {
                buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getRemainingAttempts() - 1);
                startCycle();
                Log.e(TAG, "startNextCycle  after handler ");
            } else {
                buddyGPTApplication.notifyObservers("end of timer");
            }
        }

        /**
         * ------------------------------------------ TTS  -------------------------------------------
         */

        private void speak(final String texte, String type) {
            Log.d(TAG, " --- speak(" + texte + ") ---");
            isSpeaking = true;
            getActivity().runOnUiThread(() -> {
                String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                buddyTexteRespLyt.setTranslationY(0);
                if (responseTimeout != null) responseTimeout.cancel();

                if (!buddyGPTApplication.isActivityClosed()) {
                    if (type.equals("nothealysa") || type.equals("storedResponse")) {
                        buddyGPTApplication.setAlreadyGetAnswer(true);

                        if (settingClass.getSwitchVisibility().equals("true")) {
                            if (buddyGPTApplication.getCurrentLanguage().equals("en")) {
                                buddyTexteResp.setText(format("Response :  %s ", texte));
                            } else if (buddyGPTApplication.getCurrentLanguage().equals("fr")) {
                                buddyTexteResp.setText(format("Réponse :  %s ", texte));
                            } else if (buddyGPTApplication.getCurrentLanguage().equals("de")) {
                                buddyTexteResp.setText(format("Antwort :  %s ", texte));
                            } else if (buddyGPTApplication.getCurrentLanguage().equals("es")) {
                                buddyTexteResp.setText(format("Respuesta :  %s ", texte));
                            } else {
                                buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate("Response").addOnSuccessListener(translatedText -> buddyTexteResp.setText(format("%s :  %s ", translatedText, texte))).addOnFailureListener(e -> Log.e(TAG, "translatedText exception  " + e));

                            }
                            if (type.equals("storedResponse")) {
                                lytOpenMenuSettings.setVisibility(View.INVISIBLE);
                                lytOpenMenuChat.setVisibility(View.INVISIBLE);
                                if (buddyTexteQstLyt.getVisibility() != View.VISIBLE)
                                    buddyTexteRespLyt.setTranslationY(-155);
                                else buddyTexteRespLyt.setTranslationY(0);
                            }
                            buddyTexteRespLyt.setVisibility(View.VISIBLE);
                            buddyTexteResp.setMovementMethod(new ScrollingMovementMethod());
                            if (!listRep.isEmpty()) {
                                //--> this function is called right after a question : we should create a new Replica for the response
                                Replica reponse = new Replica();
                                reponse.setValue(texte);
                                reponse.setTime(time);
                                long responseTime = buddyGPTApplication.getResponseTime() - buddyGPTApplication.getQuestionTime();
                                DecimalFormat df = new DecimalFormat("#,###");
                                String formattedTime = df.format(responseTime);
                                reponse.setType("Response");
                                reponse.setDuration(formattedTime + " ms");
                                listRep.add(reponse);
                                Session session = new Session(new ArrayList<>(listRep));
                                buddyGPTApplication.getListSession().add(session);
                                listRep.clear();
                            } else {
                                //---> this function is called after finishing pronouncing a phrase from the response : we should add the new phrase to the already existing Replica
                                ArrayList<Session> listSessions = buddyGPTApplication.getListSession();
                                ArrayList<Replica> lastSession = listSessions.get(listSessions.size() - 1).getSession();
                                Replica lastReplica = lastSession.get(lastSession.size() - 1);
                                if (lastReplica.getType().equals("Response")) {
                                    lastReplica.setValue(lastReplica.getValue() + texte);
                                } else
                                    lastReplica.setValue(texte);

                            }
                        }
                        if (buddyGPTApplication.getparam("Stream_mode").equalsIgnoreCase("true"))
                            buddyTexteResp.scrollTo(0, 0);
                        else {

                            // Scroll to the end
                            buddyTexteResp.post(() -> {
                                int scrollAmount = buddyTexteResp.getLayout().getLineTop(buddyTexteResp.getLineCount())
                                        - buddyTexteResp.getHeight() + buddyTexteResp.getLineHeight();
                                if (scrollAmount > 0) {
                                    buddyTexteResp.scrollTo(0, scrollAmount);
                                } else {
                                    buddyTexteResp.scrollTo(0, 0);
                                }
                            });
                        }


                        buddyGPTApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL, type);
                    } else if (type.equals("timeOutExpired")) {
                        buddyGPTApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL, type);
                    }
                }
            });
        }
    private void getData() {

        buddyGPTApplication.initTTSAndroid();

        //init Settings
        settingClass = new Setting();
        settingClass.setDuration(buddyGPTApplication.getparam("listening_duration"));
        settingClass.setAttempt(buddyGPTApplication.getparam("listening_attempt"));
        settingClass.setChatbot(buddyGPTApplication.getparam("SelectedChatbot"));
        settingClass.setLangue(buddyGPTApplication.getLangue().getNom());
        settingClass.setVolume(buddyGPTApplication.getparam("speak_volume"));
        settingClass.setSwitchVisibility(buddyGPTApplication.getparam("switch_visibility"));
        settingClass.setSwitchEmotion(buddyGPTApplication.getparam("switch_emotion"));
        Log.i(TAG, settingClass.toString());


        refreshSTTLangue();

        //set volume
        buddyGPTApplication.setVolume(Integer.parseInt(buddyGPTApplication.getparam("speak_volume")), AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

        //create Log file
        if (Boolean.TRUE.equals(buddyGPTApplication.getFileCreate())) {
            buddyGPTApplication.listSessionClear();
            listRep.clear();
            buddyGPTApplication.setFileCreate(false);
        }


        //start hotword listening
        mlKitIsDownloading = true;
        buddyGPTApplication.downloadModel(imlKitDownloadCallback, new Gson().fromJson(buddyGPTApplication.getparam(settingClass.getLangue()), Langue.class).getLanguageCode().split("-")[0].trim());
        handlerProgressBar.postDelayed(runnableProgressBar, 500);

        buddyGPTApplication.setActivityClosed(false);

    }



    /**
     * ----------------- Gestion de notifications ---------------------------
     */
    @Override
    public void update(String message) {
        if (message != null) {

            if (message.contains("CANCEL_RESPONSE_TIMEOUT")) {
                if (responseTimeout != null) responseTimeout.cancel();
            }
            else if (message.contains("MODE_STREAM_TEXT;SPLIT;")) {
                getActivity().runOnUiThread(() -> {
                    if (message.split(";SPLIT;").length > 1) {
                        String response = message.split(";SPLIT;")[1];
                        //translate Response Title and show the response :
                        if (buddyGPTApplication.getCurrentLanguage().equals("en")) {
                            showStream("Response", response);
                        } else if (buddyGPTApplication.getCurrentLanguage().equals("fr")) {
                            showStream("Réponse", response);
                        } else if (buddyGPTApplication.getCurrentLanguage().equals("de")) {
                            showStream("Antwort", response);
                        } else if (buddyGPTApplication.getCurrentLanguage().equals("es")) {
                            showStream("Respuesta", response);
                        } else {
                            buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate("Response")
                                    .addOnSuccessListener(translatedText -> showStream(translatedText, response)).addOnFailureListener(e -> showStream("Response", response));
                        }
                    }
                });
            }
            else if (message.contains("MODE_STREAM_SPEAK;SPLIT;")) {
                getActivity().runOnUiThread(() -> {
                    if (message.split(";SPLIT;").length > 1) {
                        String phraseToPronounce = message.split(";SPLIT;")[1];
                        speak(phraseToPronounce, "nothealysa");
                    }
                });
            }
            else if (message.contains("STTHotword_success")) {
                if (buddyGPTApplication.getparam("INVALID_TEAMGPT_DEVICE_ID").equalsIgnoreCase("TRUE")) {
                    Log.i("TAG", "run: notifyObservers INVALID_TEAMGPT_DEVICE_ID 4");
                    buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_DEVICE_ID");
                } else if (buddyGPTApplication.getparam("INVALID_TEAMGPT_KEY").equalsIgnoreCase("TRUE")) {
                    Log.i("TAG", "run: notifyObservers INVALID_TEAMGPT_KEY 4");
                    buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
                } else {
                    getActivity().runOnUiThread(() -> {
                        buddyGPTApplication.setSpeaking(true);
                        isListeningFreeSpeech = true;
                        buddyGPTApplication.setActivityClosed(false);
                        if (buddyGPTApplication.getDialog() != null && buddyGPTApplication.getDialog().isShowing())
                            buddyGPTApplication.getDialog().dismiss();
                        buddyGPTApplication.setStartRecording(true);
                        startListeningFreeSpeech(buddyGPTApplication.getListeningDuration());
                    });
                }

            }
            else if (message.contains("STTQuestion_success")) {
                getActivity().runOnUiThread(() -> {
                    stopListeningFreeSpeech();
                    buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(true);
                    SystemClock.sleep(200);
                    buddyGPTApplication.setAppIsListeningToTheQuestion(false);
                    String detectedSTTMessage = message.split(";")[1].replaceAll("' ", "'");
                    Log.i("HOU", "HOU run: " + detectedSTTMessage);
                    if (!buddyGPTApplication.isActivityClosed()) {
                        buddyGPTApplication.setQuestionNumber(buddyGPTApplication.getQuestionNumber() + 1);
                        BuddySDK.UI.setFacialExpression(FacialExpression.THINKING, 1);
                        if (settingClass.getSwitchVisibility().equals("true")) {
                            if (buddyGPTApplication.getCurrentLanguage().equals("en")) {
                                buddyTexteQst.setText(format("I heard :  %s ", detectedSTTMessage));
                            } else if (settingClass.getLangue().equals(LANGUE_FR)) {
                                buddyTexteQst.setText(format("J'ai entendu :  %s ", detectedSTTMessage));
                            } else if (settingClass.getLangue().equals(LANGUE_ES)) {
                                buddyTexteQst.setText(format("He oído :  %s ", detectedSTTMessage));
                            } else if (settingClass.getLangue().equals(LANGUE_DE)) {
                                buddyTexteQst.setText(format("Ich habe gehört :  %s ", detectedSTTMessage));
                            } else {
                                buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate("I heard ").addOnSuccessListener(translatedText -> buddyTexteQst.setText(format(  " %s  :  %s ",translatedText, detectedSTTMessage))).addOnFailureListener(e -> Log.e(TAG, "translatedText exception  " + e));
                            }
                            buddyTexteQstLyt.setVisibility(View.VISIBLE);
                            buddyTexteQst.setMovementMethod(new ScrollingMovementMethod());
                            buddyTexteQst.scrollTo(0, 0);
                            lytOpenMenuSettings.setVisibility(View.INVISIBLE);
                            lytOpenMenuChat.setVisibility(View.INVISIBLE);
                        }
                        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                        Replica question = new Replica();
                        question.setType("Question");
                        question.setTime(time);
                        question.setValue(detectedSTTMessage);
                        listRep.add(question);
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
                                responseTimeout = new CountDownTimer((long) Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds", configFile)) * 1000, 1000) {
                                    @Override
                                    public void onTick(long l) {
                                        // cmnt
                                    }

                                    @Override
                                    public void onFinish() {
                                        if (buddyGPTApplication.isAlreadyGetAnswer()) {
                                            Log.e(TAG, "app get the answer on time");
                                        } else {
                                            buddyGPTApplication.setAnswerHasExceededTimeOut(true);
                                            buddyGPTApplication.setTimeoutExpired(true);
                                            if (!buddyGPTApplication.isOpenaialreadySwitchEmotion()) {
                                                BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                            }
                                            if (buddyGPTApplication.getCurrentLanguage().equals("en")) {
                                                String[] messageTimeoutNotRespectedEn = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en", configFile).split("/");
                                                int randomNumberMessageTimeoutNotRespectedEn = random.nextInt(messageTimeoutNotRespectedEn.length);
                                                speak(messageTimeoutNotRespectedEn[randomNumberMessageTimeoutNotRespectedEn], "timeOutExpired");
                                            } else if (buddyGPTApplication.getCurrentLanguage().equals("fr")) {
                                                String[] messageTimeoutNotRespectedFr = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_fr", configFile).split("/");
                                                int randomNumberMessageTimeoutNotRespectedFr = random.nextInt(messageTimeoutNotRespectedFr.length);
                                                speak(messageTimeoutNotRespectedFr[randomNumberMessageTimeoutNotRespectedFr], "timeOutExpired");
                                            } else if (buddyGPTApplication.getCurrentLanguage().equals("es")) {
                                                String[] messageTimeoutNotRespectedEs = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_es", configFile).split("/");
                                                int randomNumberMessageTimeoutNotRespectedEs = random.nextInt(messageTimeoutNotRespectedEs.length);
                                                speak(messageTimeoutNotRespectedEs[randomNumberMessageTimeoutNotRespectedEs], "timeOutExpired");
                                            } else if (buddyGPTApplication.getCurrentLanguage().equals("de")) {
                                                String[] messageTimeoutNotRespectedDe = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_de", configFile).split("/");
                                                int randomNumberMessageTimeoutNotRespectedDe = random.nextInt(messageTimeoutNotRespectedDe.length);
                                                speak(messageTimeoutNotRespectedDe[randomNumberMessageTimeoutNotRespectedDe], "timeOutExpired");
                                            } else {
                                                String[] messageTimeoutNotRespectedEn = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en", configFile).split("/");
                                                int randomNumberMessageTimeoutNotRespectedEn = random.nextInt(messageTimeoutNotRespectedEn.length);
                                                buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(messageTimeoutNotRespectedEn[randomNumberMessageTimeoutNotRespectedEn]).addOnSuccessListener(translatedText -> speak(translatedText, "timeOutExpired")).addOnFailureListener(e -> Log.e(TAG, "translatedText exception  " + e));

                                            }
                                        }
                                    }
                                };
                                responseTimeout.start();
                            });
                        }
                    }
                });
            }
            else if (message.contains("TTS_success")) {
                getActivity().runOnUiThread(() -> {
                    Log.e(TAG, " TTS_success");
                    buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                    buddyTexteQstLyt.setVisibility(View.INVISIBLE);
                    buddyTexteRespLyt.setVisibility(View.INVISIBLE);
                    buddyTexteQst.setMovementMethod(null);
                    buddyTexteResp.setMovementMethod(null);
                    lytOpenMenuSettings.setVisibility(View.VISIBLE);
                    lytOpenMenuChat.setVisibility(View.VISIBLE);
                    isSpeaking = false;
                });
                if (handler != null && runnable != null) {
                    handler.removeCallbacks(runnable);
                    handler.removeCallbacksAndMessages(null);
                }
                runnable = () -> {
                    if (!buddyGPTApplication.getStoredResponse().isEmpty()) {
                        getActivity().runOnUiThread(() -> speak(buddyGPTApplication.getStoredResponse(), "storedResponse"));
                    } else {
                        if (Boolean.TRUE.equals(buddyGPTApplication.getStartRecording())) {
                            Log.e(TAG, "startCycle TTS_success 2");
                            buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getListeningAttempt() - 1);
                            startCycle();
                        }
                    }
                };
                if (handler != null) {
                    handler.postDelayed(runnable, 500);
                } else {
                    Log.e(TAG, "Handler is null. Unable to post the runnable.");
                }
            }
            else if (message.contains("Emotion_Change")) {
                buddyGPTApplication.setAnimation(message.split(";SPLIT;")[1]);
            }
            else if (message.contains("TTS_error") || message.contains("TTS_exception")) {
                getActivity().runOnUiThread(() -> {
                    String text = message.split(";")[1];
                    Log.w(TAG, "TTS_ERROR:" + text);
                    buddyGPTApplication.playUsingReadSpeakerCaseError(text, new ITTSCallbacks() {
                        @Override
                        public void onSuccess(String s) {
                            getActivity().runOnUiThread(() -> {
                                buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                                try {
                                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }
                                if (buddyGPTApplication.getparam("Stream_mode").equals("true")) {
                                    if (buddyGPTApplication.getResponseFromTeamGPT() != null)
                                        buddyGPTApplication.getResponseFromTeamGPT().isReadyToSpeak = true;
                                } else {
                                    buddyTexteQstLyt.setVisibility(View.INVISIBLE);
                                    buddyTexteRespLyt.setVisibility(View.INVISIBLE);
                                    buddyTexteQst.setMovementMethod(null);
                                    buddyTexteResp.setMovementMethod(null);
                                    lytOpenMenuSettings.setVisibility(View.VISIBLE);
                                    lytOpenMenuChat.setVisibility(View.VISIBLE);
                                    isSpeaking = false;
                                    if (handler != null && runnable != null) {
                                        handler.removeCallbacks(runnable);
                                        handler.removeCallbacksAndMessages(null);
                                    }
                                    runnable = () -> {
                                        if (!buddyGPTApplication.getStoredResponse().equals("")) {
                                            getActivity().runOnUiThread(() -> speak(buddyGPTApplication.getStoredResponse(), "storedResponse"));
                                        } else {
                                            if (Boolean.TRUE.equals(buddyGPTApplication.getStartRecording())) {
                                                buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getListeningAttempt() - 1);
                                                startCycle();
                                            }
                                        }
                                    };
                                    if (handler != null) {
                                        handler.postDelayed(runnable, 500);
                                    } else {
                                        Log.e(TAG, "Handler is null. Unable to post the runnable.");
                                    }
                                }
                            });
                        }

                        @Override
                        public void onError(String s) {
                            int textLength = text.length();// Calculate the length of the pronounced text
                            int delayTime = (textLength / 20) * 1000; // 1 second for every 20 characters
                            if (delayTime == 0) {
                                delayTime = 1500;
                            }
                            if (buddyGPTApplication.getparam("TTS").equalsIgnoreCase("ReadSpeaker") && (buddyGPTApplication.getCurrentLanguage().equals("en") || buddyGPTApplication.getCurrentLanguage().equals("fr")) && Boolean.TRUE.equals(buddyGPTApplication.getUsingReadSpeaker())) {
                                delayTime = 0;
                                Log.e(TAG, "set 0 delay time  " + delayTime);
                            }
                            handlerTTSError.postDelayed(runnableTTSError = () -> getActivity().runOnUiThread(() -> {
                                buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                                try {
                                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }
                                if (buddyGPTApplication.getparam("Stream_mode").equals("true")) {
                                    if (buddyGPTApplication.getResponseFromTeamGPT() != null)
                                        buddyGPTApplication.getResponseFromTeamGPT().isReadyToSpeak = true;
                                } else {

                                    buddyTexteQstLyt.setVisibility(View.INVISIBLE);
                                    buddyTexteRespLyt.setVisibility(View.INVISIBLE);
                                    buddyTexteQst.setMovementMethod(null);
                                    buddyTexteResp.setMovementMethod(null);
                                    lytOpenMenuSettings.setVisibility(View.VISIBLE);
                                    lytOpenMenuChat.setVisibility(View.VISIBLE);
                                    isSpeaking = false;
                                    if (handler != null && runnable != null) {
                                        handler.removeCallbacks(runnable);
                                        handler.removeCallbacksAndMessages(null);
                                    }
                                    runnable = () -> {
                                        if (!buddyGPTApplication.getStoredResponse().equals("")) {
                                            getActivity().runOnUiThread(() -> speak(buddyGPTApplication.getStoredResponse(), "storedResponse"));
                                        } else {
                                            if (Boolean.TRUE.equals(buddyGPTApplication.getStartRecording())) {
                                                buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getListeningAttempt() - 1);
                                                startCycle();
                                            }
                                        }
                                    };
                                    if (handler != null) {
                                        handler.postDelayed(runnable, 500);
                                    } else {
                                        Log.e(TAG, "Handler is null. Unable to post the runnable.");
                                    }
                                }
                            }), delayTime);
                        }
                    });
                });
            }
            else if (message.contains("properties file done")) {
                buddyGPTApplication.setNotYet(false);
                getData();
            }
            else if (message.contains("end of timer")) {
                buddyGPTApplication.setAppIsListeningToTheQuestion(false);
                stopListeningFreeSpeech();
                SystemClock.sleep(200);
                buddyGPTApplication.startListeningHotwor(getActivity());
            }
            else if (message.contains("end of cycle")) {
                getActivity().runOnUiThread(() -> {
                    BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                    BuddySDK.UI.stopListenAnimation();
                    buddyGPTApplication.setLed("neutral");
                });
            }
            else if (message.contains("restartNewCycle")) {
                runnablePauseTime = () -> {
                    startNextCycle();
                    Log.e(TAG, "startNextCycle  after handler ");
                };
                handlerPauseTime.postDelayed(runnablePauseTime, 1000);
            }
            else if (message.contains("Obtain audio transcription after the listening time has elapsed")) {
                String shouldRestartNewCycle = message.split(";SPLIT;")[1];
                Log.e(TAG, "Obtain audio transcription after the listening time has elapsed " + shouldRestartNewCycle);
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                BuddySDK.UI.stopListenAnimation();
                buddyGPTApplication.setLed("neutral");
                buddyGPTApplication.setAppIsListeningToTheQuestion(false);
                buddyGPTApplication.traitementAudio();
            }
            else if (message.contains("restartListeningHotword")) {
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                buddyGPTApplication.setStartRecording(false);
                buddyGPTApplication.setSpeaking(false);
                buddyGPTApplication.setActivityClosed(true);
                isListeningFreeSpeech = false;
                buddyGPTApplication.stopTTS();
                buddyGPTApplication.setStoredResponse("");
                if (buddyTexteQstLyt != null && buddyTexteRespLyt != null && buddyTexteQst != null && buddyTexteResp != null) {
                    buddyTexteQstLyt.setVisibility(View.INVISIBLE);
                    buddyTexteRespLyt.setVisibility(View.INVISIBLE);
                    buddyTexteQst.setMovementMethod(null);
                    buddyTexteResp.setMovementMethod(null);
                }
                lytOpenMenuSettings.setVisibility(View.VISIBLE);
                lytOpenMenuChat.setVisibility(View.VISIBLE);
                try {
                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                } catch (Exception e) {
                    Log.e(TAG, "BuddySDK Exception  " + e);
                }
                buddyGPTApplication.notifyObservers("end of timer");
            }
            if (message.contains("INVALID_TEAMGPT_KEY")) {
                buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "TRUE");
                if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_EN)) {
                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_FR)) {
                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_fr), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_fr));
                } else {
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                            .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en))
                            .addOnSuccessListener(translatedText -> buddyGPTApplication.showInputDialog(getActivity(), translatedText, "Attention !"))
                            .addOnFailureListener(e -> buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en)));
                }


            }
            if (message.contains("INVALID_TEAMGPT_DEVICE_ID")) {
                buddyGPTApplication.setparam("INVALID_TEAMGPT_DEVICE_ID", "TRUE");
                if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_EN)) {
                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_FR)) {
                    buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_fr), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_fr));
                } else {
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                            .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en))
                            .addOnSuccessListener(translatedText -> buddyGPTApplication.showInputDialog(getActivity(), translatedText, "Attention !"))
                            .addOnFailureListener(e -> buddyGPTApplication.showInputDialog(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_id_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en)));
                }


            }
            if (message.contains("Session_ID_ERROR")) {

                if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_EN)) {
                    buddyGPTApplication.showInputDialog2(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_params_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_FR)) {
                    buddyGPTApplication.showInputDialog2(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_params_invalid_fr), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_fr));
                } else {
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                            .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_params_invalid_en))
                            .addOnSuccessListener(translatedText -> buddyGPTApplication.showInputDialog2(getActivity(), translatedText, "Attention !"))
                            .addOnFailureListener(e -> buddyGPTApplication.showInputDialog2(getActivity(), buddyGPTApplication.getString(R.string.toast_teamgpt_params_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en)));
                }


            }
            else if (message.contains("QST_LAYOUT_DISMISSED")) {
                buddyTexteQstLyt.setVisibility(View.INVISIBLE);
                buddyTexteQst.setMovementMethod(null);
                lytOpenMenuSettings.setVisibility(View.VISIBLE);
                lytOpenMenuChat.setVisibility(View.VISIBLE);
            }
            else if (message.contains("playStoredResponse")) {
                if (!buddyGPTApplication.getStoredResponse().equals("")) {
                    getActivity().runOnUiThread(() -> speak(buddyGPTApplication.getStoredResponse(), "storedResponse"));
                }
            }
            else if (message.contains("makeBuddyFaceNeutral")) {
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
            }
            else if (message.contains("ChatDestroy")) {
                buddyGPTApplication.setparam("firstLaunch", "false");
            }
            else if (message.contains("isConnected")) {
                getActivity().runOnUiThread(() -> {
                    downloadingBar.setVisibility(View.VISIBLE);
                    noNetwork.setVisibility(View.GONE);
                });
            }
            else if (message.contains("isNotConnected")) {
                getActivity().runOnUiThread(() -> {
                    downloadingBar.setVisibility(View.GONE);
                    noNetwork.setVisibility(View.VISIBLE);
                });
            }
            else if (message.contains("changeDetected")) {
                int speakVolume = buddyGPTApplication.getVolume();
                int max = buddyGPTApplication.getMaxVolume();
                int defaultVolume = buddyGPTApplication.getClosestInt((double) (speakVolume * 100) / max);
                Log.e(TAG, "volumeMedia  " + defaultVolume);
                buddyGPTApplication.setparam("speak_volume", valueOf(defaultVolume));
            }

        }
    }
}