package com.robotique.aevaweb.buddygpt.fragments;


import static androidx.core.app.ActivityCompat.finishAffinity;
import static com.robotique.aevaweb.buddygpt.utilis.tracking.PoseTracking.TAG_TRACKING_DEBUG;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import androidx.annotation.Nullable;
import android.util.Log;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bfr.buddy.ui.shared.FaceTouchData;
import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.IUIFaceTouchCallback;
import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddySDK;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;
import com.robotique.aevaweb.buddygpt.R;
import com.robotique.aevaweb.buddygpt.application.BuddyGPTApplication;
import com.robotique.aevaweb.buddygpt.chatbotresponse.ResponseFromTeamGPT;
import com.robotique.aevaweb.buddygpt.models.Langue;
import com.robotique.aevaweb.buddygpt.models.Replica;
import com.robotique.aevaweb.buddygpt.models.Session;
import com.robotique.aevaweb.buddygpt.models.Setting;
import com.robotique.aevaweb.buddygpt.observers.IDBObserver;
import com.robotique.aevaweb.buddygpt.utilis.AudioUtils;
import com.robotique.aevaweb.buddygpt.utilis.CustomToast;
import com.robotique.aevaweb.buddygpt.utilis.IMLKitDownloadCallback;
import com.robotique.aevaweb.buddygpt.utilis.ITTSCallbacks;
import com.robotique.aevaweb.buddygpt.utilis.ResponseCallback;
import com.robotique.aevaweb.buddygpt.utilis.tracking.MainViewModel;
import com.robotique.aevaweb.buddygpt.utilis.tracking.OverlayView;
import com.robotique.aevaweb.buddygpt.utilis.tracking.PoseLandmarkerHelper;
import com.robotique.aevaweb.buddygpt.utilis.tracking.PoseTracking;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment implements IDBObserver {
    private static final String TAG = "BuddyGPT_MainFragment";
    private static final String TAG_TRACKING = "BuddyGPT_TRACKING_INFO";
    private static final String ANDROID_STT = "Android";
    private static final String CERENCE_STT = "Cerence";
    private static final String GOOGLE_STT = "google";
    private static final String WHISPER_STT = "whisper";
    private static final String LANGUE_FR = "Français";
    private static final String LANGUE_EN = "Anglais";
    private static final String LANGUE_ES = "Espagnol";
    private static final String LANGUE_DE = "Allemand";
    private static final String configFile = "BuddyGPT.properties";
    private BuddyGPTApplication buddyGPTApplication;
    private final Random random = new Random();
    //views
    private RelativeLayout buddyTexteQstLyt;
    private RelativeLayout buddyTexteRespLyt;
    private RelativeLayout lytOpenMenuSettings;
    private RelativeLayout lytOpenMenutest;
    private RelativeLayout lytOpenMenuChat;
    private RelativeLayout launchView;
    private RelativeLayout reGroup;
    private TextView buddyTexteQst;
    private TextView buddyTexteResp;
    private ImageView noNetwork;
    private ProgressBar downloadingBar;
    private RelativeLayout lytSpinner;
    private PreviewView previewView;
    private FrameLayout preview_container;

    private Handler timeoutHandler;
    private Runnable timeoutRunnable;
    private String infoToast = "";
    private boolean isListeningFreeSpeech = false;
    private Boolean mlKitIsDownloading = false;
    private boolean englishIsDownloaded = false;
    private boolean frenchIsDownloaded = false;
    private boolean languageToenglishIsDownloaded = false;
    private boolean isSpeaking = false;
    private boolean isReTrack = false;
    private final boolean regardeCamera = false;
    private CountDownTimer timerEcoute;
    private CountDownTimer responseTimeout;
    private CountDownTimer timerDownloading;
    private static final String header = "header";
    private static final String entete = "entete";
    private OverlayView overlay;

    private int TRACKING_DELAY_START_LISTEN;
    private int TRACKING_DELAY_STOP_LISTEN;
    private String TRACKING_WATCH;
    private CameraSelector cameraSelector;
    private MainViewModel viewModel;
    private int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private boolean isTrackingAlreadyInitialised = false;
    private PoseLandmarkerHelper poseLandmarkerHelper;
    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private ImageAnalysis imageAnalyzer;
    private Camera camera;
    private Float[] res = {(float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0,(float) 0, (float) 0, (float) 0};
    private float initLang=190F;
    private float degx,degy,x0,x2,x5,y0,y5,y2,lang,dLeft,eog,Eod, dRight;

    private long lastVisibleTime = 0L;          // Track the time when person was last seen
    private long lastVisibleTime_saved = 0L;    // Track the time when person was last seen and do not reset it when re-track (useful for invitation check)
    private long firstVisibleTime = 0L;         // Track the time when person started being visible
    private long visibleDuration = 0L;          // How long a person remained visible
    private long lastLookingAtCameraTime = 0L;  // Track the time when the person last looked at the camera
    private long totalTimeLookingAtCamera = 0L; // Total time spent looking at the camera
    private long startLookingAtCameraTime = 0L; // Track the start time of the current interval when the person is looking directly at the camera
    private boolean isPersonDetected = false;
    private boolean wasPersonDetected = false;
    private boolean personIsVisible = false;
    private boolean isProcessingReTrack = false;
    private boolean regarde_camera=false;
    private boolean direction=false;
    private boolean deFace=false;
    private final ArrayList<Replica> listRep = new ArrayList<>();


    private boolean isFirstInvitaion = false;
    private boolean sendInvitationPending = false;
    private String directionRegardNez= "";

    private Setting settingClass;
    private PoseTracking poseTracking;
    private ExecutorService backgroundExecutor;

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
                    if(timerDownloading!=null){
                        timerDownloading.cancel();
                    }
                    Log.i(TAG, "onDownloadEnd: initOrMajOrNone "+initOrMajOrNone );
                    if (initOrMajOrNone.equals("INIT")) {
                        if (buddyGPTApplication.getCurrentLanguage().equals("en")) {
                            infoToast = getString(R.string.toast_config_file_init_en);
                            CustomToast.getInstance().showInfo(getActivity(), infoToast, 2000);
                        }
                        else if (buddyGPTApplication.getCurrentLanguage().equals("fr")) {
                            infoToast = getString(R.string.toast_config_file_init_fr);
                            CustomToast.getInstance().showInfo(getActivity(), infoToast, 2000);
                        }
                        else if (buddyGPTApplication.getCurrentLanguage().equals("de")) {
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
                    }
                    else if (initOrMajOrNone.equals("MAJ")) {//traduire l'info du configFile :
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
                    }
                    else if (initOrMajOrNone.equals("NONE")) {

                        Log.i(TAG, "onDownloadEnd: initOrMajOrNone none "+initOrMajOrNone );

                    }
                    mlKitIsDownloading = false;
                    Log.i(TAG, "onDownloadEnd: value "+Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation")));

                    if (!Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation"))) {
                        buddyGPTApplication.startListeningHotwor(getActivity());
                        Log.i(TAG, "run: teeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeest 1");
                        reGroup.setTranslationY(1000);
                    }
                    else if (Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation")) ) {
                        if (!Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Auto_Listen"))) {
                            buddyGPTApplication.startListeningHotwor(getActivity());
                        }
                        isReTrack = false;
                        Log.i(TAG, "trackingtests 1");
                        initTracking();
                    }

                }
            } else {
                mlKitIsDownloading = true;
                frenchIsDownloaded = false;
                englishIsDownloaded = false;
                languageToenglishIsDownloaded = false;
                buddyGPTApplication.downloadModel(imlKitDownloadCallback, new Gson().fromJson(buddyGPTApplication.getparam(settingClass.getLangue()), Langue.class).getLanguageCode().split("-")[0].trim());
                handlerProgressBar.postDelayed(runnableProgressBar, 500);
                timerDownloading.start();
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
                Log.e(TAG, "click1");
                if (buddyGPTApplication.getparam("Stimulis").equals("true")) {
                    Log.e(TAG, "click");
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
                        if (buddyGPTApplication.getparam("INVALID_TEAMGPT_KEY").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
                            Log.i("TAG", "run: notifyObservers INVALID_TEAMGPT_KEY 3");
                            buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");}
                        else if (buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
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
                        }
                        else if(buddyGPTApplication.getparam("ENV_ERROR").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")){
                            Log.i("TAG", "run: notifyObservers ENV_ERROR 3");
                            buddyGPTApplication.notifyObservers("ENV_ERROR");
                        }
                        else {
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
    private final IUsbCommadRsp iUsbCommadRspTracking = new IUsbCommadRsp.Stub(){
        @Override
        public void onSuccess(String s) {
            Log.i(TAG, "onSuccess: "+s);
        }
        @Override
        public void onFailed(String s) {
            Log.i(TAG, "onFailed: "+s);
        }
    };
    private View view;
    private String mParam1;
    private String mParam2;
    private static final String ARG_INIT_MODE = "initMode";
    public MainFragment() {
        // Required empty public constructor
        Log.i(TAG, "MainFragment: constructeur");
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment MainFragment.
     */
    public static MainFragment newInstance(String param1) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putString("initMode", param1);
        Log.i(TAG, "newInstance: put initMode in args :"+param1);

        fragment.setArguments(args);
        return fragment;
    }

    /**
     * ------------------ App LifeCycle ---------------------
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: --------------");
        view = inflater.inflate(R.layout.fragment_main, container, false);
        // restore initOrMajOrNone from arguments to survive process recreation
        if (savedInstanceState != null && savedInstanceState.containsKey("initMode")) {
                 initOrMajOrNone = savedInstanceState.getString("initMode", "");
             } else if (getArguments() != null && getArguments().containsKey("initMode")) {
                 initOrMajOrNone = getArguments().getString("initMode", "");
             }
         Log.i(TAG, "onCreate: restored initOrMajOrNone=" + initOrMajOrNone);
        buddyGPTApplication = (BuddyGPTApplication) getActivity().getApplicationContext();
        buddyGPTApplication.registerObserver(this);
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
        lytOpenMenutest = view.findViewById(R.id.lyt_open_menu_test);
        lytOpenMenuChat = view.findViewById(R.id.lyt_open_menu_chat);
        launchView = view.findViewById(R.id.launch_view);
        noNetwork = view.findViewById(R.id.noNetwork);
        previewView = view.findViewById(R.id.view_finder);
        downloadingBar = view.findViewById(R.id.progressBar_MLKitDownload);
        reGroup = view.findViewById(R.id.reGroup);
        overlay = view.findViewById(R.id.overlay);
        preview_container = view.findViewById(R.id.preview_container);
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(getActivity()));
        }
        currentTrackingListeningState = StateTrackingListening.PERSON_IS_NOT_VISIBLE_TIMEOUT;
        totalTimeLookingAtCamera = 0L;
        lytOpenMenuSettings.setOnClickListener(v -> btnOpenSettingsFragment());

        lytOpenMenutest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        buddyGPTApplication.getResponseFromTeamGPT().sendPutRequestStream("Bonjour",null); // <-- runs in background
                    }
                }).start();
            }
        });
        lytOpenMenuChat.setOnClickListener(v -> btnOpenChatFragment());
        // Initialiser le spinner
        lytSpinner = view.findViewById(R.id.lyt_spinner);
        lytSpinner.setVisibility(View.GONE);
        if(buddyGPTApplication.getparam("IMEI").equals("")){
            String imei= buddyGPTApplication.getIMEI();
            Log.i(TAG, "init: imei device "+imei);
            buddyGPTApplication.setparam("IMEI", imei);
        }
        else
            Log.i(TAG, "onCreateView: imei robot : "+buddyGPTApplication.getparam("IMEI"));

        if (buddyGPTApplication.isFirstLaunch()) {

            try {
                Log.i(TAG, "onCreateView: isFirstLaunch=false;");
                initTeamGPTSettings();
                buddyGPTApplication.setFirstLaunch(false);
            } catch (IOException e) {

                Log.w(TAG, "onCreateView: IOException "+e.getMessage());
            }
            if (buddyGPTApplication.getparam("IMEI_ID_Device").equals("")) {
                buddyGPTApplication.setparam("IMEI_ID_Device", " _ ");
            }
            if (buddyGPTApplication.getparam("email_support").equals("")) {
                buddyGPTApplication.setparam("email_support", " _ ");
            }
            if (buddyGPTApplication.getparam("IdCompte").equals("")) {
                buddyGPTApplication.setparam("IdCompte", " _ ");
            }
        }
            buddyGPTApplication.setSpeaking(false);
            buddyGPTApplication.setNotYet(true);
            buddyGPTApplication.setActivityClosed(false);
            buddyGPTApplication.setStartRecording(false);
            buddyGPTApplication.setQuestionNumber(0);
            buddyGPTApplication.setCurrentQuestionNubmer(0);
            buddyGPTApplication.setAlreadyGetAnswer(false);
            buddyGPTApplication.setTimeoutExpired(false);
            buddyGPTApplication.setQuestionTime(0);
            buddyGPTApplication.setStoredResponse("");
            buddyGPTApplication.setMessageError(false);
            buddyGPTApplication.setInitSharedpreferences(true);
            buddyGPTApplication.setLanguageDetected("");
            buddyGPTApplication.setResponseTime(0);
            buddyGPTApplication.setAnswerHasExceededTimeOut(false);
            buddyGPTApplication.setAppIsListeningToTheQuestion(false);
            buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
            buddyGPTApplication.setBIExecution(false);
            buddyGPTApplication.setAlreadyChatting(false);
            Log.i(TAG_TRACKING, "First launch of application");

        getData();

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
        Log.i(TAG, "onCreateView: --------------end");
        if(cameraProvider != null) cameraProvider.unbindAll();

        return view;


    }



    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, " --- onCreate() ---");
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, " --- onDestroyView() ---");

        BuddySDK.UI.removeFaceTouchListener(iuiFaceTouchCallback);
        buddyGPTApplication.setparam("firstLaunch", "true");
        if (buddyGPTApplication.getDialog() != null && buddyGPTApplication.getDialog().isShowing())
            buddyGPTApplication.getDialog().dismiss();
        buddyGPTApplication.setFileCreate(true);
        buddyGPTApplication.setFirstLaunch(true);
        buddyGPTApplication.removeObserver(this);
        buddyGPTApplication.notifyObservers("main destroy");
        if (poseTracking != null) poseTracking.stopMovingAndCancelRunnables();
        if (backgroundExecutor != null) backgroundExecutor.shutdownNow();
        downloadingBar.setVisibility(View.GONE);
        // Annule le timeout du spinner si actif
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
        if(cameraProvider != null) cameraProvider.unbindAll();
        super.onDestroyView();
    }

    public void btnOpenSettingsFragment() {
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
        if (Boolean.FALSE.equals(mlKitIsDownloading)) {
            if (getActivity() != null && isAdded()) {
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new SettingsFragment())
                        .commitAllowingStateLoss();
            }
            getActivity().overridePendingTransition(0, 0);
        } else if (Boolean.TRUE.equals(buddyGPTApplication.getBIExecution())) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SettingsFragment())
                    .commitAllowingStateLoss();

            getActivity().overridePendingTransition(0, 0);
        }
    }

    public void btnOpenChatFragment() {
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
        if (Boolean.FALSE.equals(mlKitIsDownloading)) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ChatFragment())
                    .commitAllowingStateLoss();
            getActivity().overridePendingTransition(0, 0);
        } else if (Boolean.TRUE.equals(buddyGPTApplication.getBIExecution())) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ChatFragment())
                    .commitAllowingStateLoss();
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



    /**
     * ------------------------------------------ TRACKING  -------------------------------------------
     */


    private enum StateTrackingListening {
        NONE,
        PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT,
        PERSON_IS_VISIBLE_BUT_IS_NOT_LOOKING_AT_CAMERA_TIMEOUT,
        PERSON_IS_NOT_VISIBLE_TIMEOUT
    }
    private StateTrackingListening currentTrackingListeningState = StateTrackingListening.NONE;

    private Handler handlerCheckPersonDetection = new Handler();
    private Runnable runnableCheckPersonDetection = new Runnable() {
        public void run() {
            Log.i(TAG, "run: runnable");
            long currentTime = System.currentTimeMillis();

            if(isPersonDetected){
                if(!personIsVisible){
                    firstVisibleTime = currentTime;  // Update the time when person started being visible
                }
                personIsVisible =true;
                lastVisibleTime = currentTime;  // Update the time when person was last seen
                lastVisibleTime_saved = currentTime;  // Update the time when person was last seen
                if (regarde_camera) {
                    lastLookingAtCameraTime = currentTime;  // Update the time when the person last looked at the camera

                    // If starting to look at the camera, set the start time
                    if (startLookingAtCameraTime == 0L) {
                        startLookingAtCameraTime = currentTime;
                        totalTimeLookingAtCamera = 0L; // Reset total time when starting to look at the camera
                    }
                    else {
                        // Calculate the interval time looking at the camera
                        totalTimeLookingAtCamera += currentTime - startLookingAtCameraTime;
                        // Update the start time for the next interval calculation
                        startLookingAtCameraTime = currentTime;
                    }
                }
                else {
                    // Reset the start time if not looking at the camera
                    startLookingAtCameraTime = 0L;
                    totalTimeLookingAtCamera = 0L;
                }
            }
            else{
                if(personIsVisible){
                    visibleDuration = currentTime - firstVisibleTime;
                    Log.e(TAG_TRACKING, "Person was detected for " + visibleDuration + " milliseconds");
                }
                personIsVisible =false;
                totalTimeLookingAtCamera = 0L;
                startLookingAtCameraTime = 0L;
            }


            //#region arrêt et lancement d'écoute
            if(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Auto_Listen")) && !isSpeaking){
                /**
                 *  Lorsque personne ne regarde la camera pendant (TRACKING_DELAY_STOP_LISTEN secondes) il arrête d’écouter y compris les hotwords.
                 *  Si une personne regarde la caméra pendant (TRACKING_DELAY_START_LISTEN secondes), il se met à l’écoute sans attendre un hotword.
                 */
                if (isPersonDetected) {
                    if (regarde_camera) {
                        Log.w(TAG_TRACKING_DEBUG, "A person is visible again and is looking directly at the CAMERA");
                        if (totalTimeLookingAtCamera  >= TRACKING_DELAY_START_LISTEN * 1000L && currentTrackingListeningState != StateTrackingListening.PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT) {
                                currentTrackingListeningState = StateTrackingListening.PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT;
                                Log.w(TAG_TRACKING, "A person has been looking directly at the camera for TRACKING_DELAY_START_LISTEN="+TRACKING_DELAY_START_LISTEN+" seconds (or more) --> start listening");
                            if (buddyGPTApplication.getparam("INVALID_TEAMGPT_KEY").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
                                Log.i("TAG", "run: notifyObservers INVALID_TEAMGPT_KEY 3");
                                buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");}
                            else if (buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
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
                            }
                            else if(buddyGPTApplication.getparam("ENV_ERROR").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")){
                                Log.i("TAG", "run: notifyObservers ENV_ERROR 3");
                                buddyGPTApplication.notifyObservers("ENV_ERROR");
                            }
                            else
                                startListeningQuestion();
                            Log.w(TAG_TRACKING, "isFirstInvitaion= "+isFirstInvitaion);
                        }

                    }
                    else {
                        Log.w(TAG_TRACKING_DEBUG, "A person is visible again BUT is not looking at the CAMERA");
                        if (currentTime - lastLookingAtCameraTime >= TRACKING_DELAY_STOP_LISTEN * 1000L && currentTrackingListeningState != StateTrackingListening.PERSON_IS_VISIBLE_BUT_IS_NOT_LOOKING_AT_CAMERA_TIMEOUT) {
                                currentTrackingListeningState = StateTrackingListening.PERSON_IS_VISIBLE_BUT_IS_NOT_LOOKING_AT_CAMERA_TIMEOUT;
                                Log.w(TAG_TRACKING, "No person has been looking directly at the camera for TRACKING_DELAY_STOP_LISTEN="+TRACKING_DELAY_STOP_LISTEN+" seconds (or more) --> stop listening");
                                stopListeningEverything();
                            }

                    }
                }
                else if (currentTime - lastVisibleTime >= TRACKING_DELAY_STOP_LISTEN * 1000L) {
                    Log.w(TAG_TRACKING_DEBUG, "No person is visible");
                    if (currentTrackingListeningState != StateTrackingListening.PERSON_IS_NOT_VISIBLE_TIMEOUT) {
                        currentTrackingListeningState = StateTrackingListening.PERSON_IS_NOT_VISIBLE_TIMEOUT;
                        Log.w(TAG_TRACKING, "No person has been visible for TRACKING_DELAY_STOP_LISTEN="+TRACKING_DELAY_STOP_LISTEN+" seconds (or more) --> stop listening");
                        stopListeningEverything();
                    }
                }
            }
            //#endregion arrêt et lancement d'écoute


        }
    };
    private void startListeningQuestion(){
        Log.d(TAG_TRACKING, "startListeningQuestion()");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    isSpeaking =false;
                    if(handler!=null && runnable!=null){
                        handler.removeCallbacks(runnable);
                        handler.removeCallbacksAndMessages(null);
                    }
                    if (responseTimeout!=null) responseTimeout.cancel();
                    if(buddyGPTApplication.getResponseFromTeamGPT()!=null)
                        buddyGPTApplication.getResponseFromTeamGPT().reset();
                    if(handlerTTSError!=null && runnableTTSError!=null){
                        handlerTTSError.removeCallbacks(runnableTTSError);
                        handlerTTSError.removeCallbacksAndMessages(null);
                    }
                    Log.d(TAG_TRACKING, "startListeningQuestion() if first");
                    if (Boolean.TRUE.equals(!buddyGPTApplication.getSpeaking()) && Boolean.TRUE.equals(!mlKitIsDownloading)){
                        buddyGPTApplication.setStartRecording(true);
                        buddyGPTApplication.setSpeaking(true);
                        if(!isListeningFreeSpeech ) {
                            Log.d(TAG_TRACKING, "startListeningQuestion() if second");
                            isListeningFreeSpeech=true;
                            buddyGPTApplication.setActivityClosed(false);
                            startListeningFreeSpeech(buddyGPTApplication.getListeningDuration());
                        }
                        Log.d(TAG_TRACKING, "startListeningQuestion() isListeningFreeSpeech="+isListeningFreeSpeech);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void stopListeningEverything(){
        Log.d(TAG_TRACKING, "stopListeningEverything()");
        getActivity().runOnUiThread(() -> {
            try{
                isSpeaking =false;
                if(handler!=null && runnable!=null){
                    handler.removeCallbacks(runnable);
                    handler.removeCallbacksAndMessages(null);
                }
                if (responseTimeout!=null) responseTimeout.cancel();
                if(buddyGPTApplication.getResponseFromTeamGPT()!=null)
                    buddyGPTApplication.getResponseFromTeamGPT().reset();
                if(handlerTTSError!=null && runnableTTSError!=null){
                    handlerTTSError.removeCallbacks(runnableTTSError);
                    handlerTTSError.removeCallbacksAndMessages(null);
                }
                if (Boolean.TRUE.equals(buddyGPTApplication.getSpeaking()) && Boolean.TRUE.equals(!mlKitIsDownloading)) {
                    if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Android")
                            || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Cerence")
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
                        buddyGPTApplication.setAppIsListeningToTheQuestion(false);
                        stopListeningFreeSpeech();
                        try {
                            BuddySDK.UI.stopListenAnimation();
                        } catch (Exception e) {
                            Log.e(TAG, "BuddySDK Exception  " + e);
                        }
                    }
                    else{
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
            }
            catch (Exception e){
                Log.e(TAG,"Exception  "+e);
                e.printStackTrace();
            }
        });
    }

    private void initTracking(){
        Log.d(TAG_TRACKING, "initTracking(isReTrack="+isReTrack+")");

        if(!buddyGPTApplication.isFirstLaunch() && !isReTrack){
            try{
                if(BuddySDK.Actuators.getLeftWheelStatus().toUpperCase().contains("DISABLE") || BuddySDK.Actuators.getRightWheelStatus().toUpperCase().contains("DISABLE")) {
                    BuddySDK.USB.enableWheels(true, iUsbCommadRspTracking);
                }
                if(BuddySDK.Actuators.getYesStatus().toUpperCase().contains("DISABLE")) {
                    BuddySDK.USB.enableYesMove(true, iUsbCommadRspTracking);
                }
                if(BuddySDK.Actuators.getNoStatus().toUpperCase().contains("DISABLE")) {
                    BuddySDK.USB.enableNoMove(true, iUsbCommadRspTracking);
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        if(!isReTrack){
            //récupération des paramètres TRACKING du fichier de config:
            TRACKING_WATCH = buddyGPTApplication.getParamFromFile("TRACKING_watch", "BuddyGPT.properties");
            TRACKING_DELAY_START_LISTEN = Integer.parseInt(buddyGPTApplication.getParamFromFile("TRACKING_delay_startlisten", "BuddyGPT.properties"));
            TRACKING_DELAY_STOP_LISTEN = Integer.parseInt(buddyGPTApplication.getParamFromFile("TRACKING_delay_stoplisten", "BuddyGPT.properties"));

        }

        if(buddyGPTApplication.isFirstLaunch() && !isReTrack ){
            isFirstInvitaion = true;
            startTracking();
        }
        else {
            startTracking();
        }
    }

    private void startTracking(){
        Log.d(TAG_TRACKING, "startTracking(isReTrack="+isReTrack+")");

        poseTracking= new PoseTracking();
        if(backgroundExecutor != null) backgroundExecutor.shutdownNow();
        backgroundExecutor = Executors.newSingleThreadExecutor();
        cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        viewModel = new ViewModelProvider(getActivity()).get(MainViewModel.class);
        viewModel.setMinPoseDetectionConfidence(PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE);
        viewModel.setMinPoseTrackingConfidence(PoseLandmarkerHelper.DEFAULT_POSE_TRACKING_CONFIDENCE);
        viewModel.setMinPosePresenceConfidence(PoseLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE);
        viewModel.setDelegate(PoseLandmarkerHelper.DELEGATE_GPU);
        viewModel.set_model(PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL);

        setUpCamera();
    }

    private void setUpCamera() {
        Log.i(TAG_TRACKING,"setUpCamera(isReTrack="+isReTrack+")");
        isTrackingAlreadyInitialised = false;
        previewView.post(() -> {
            try {
                Log.i(TAG, "setUpCamera: "+getActivity());
                cameraProvider = ProcessCameraProvider.getInstance(getActivity()).get();
                if (preview != null) {
                    preview.setSurfaceProvider(null);
                    preview = null;
                }

                if (imageAnalyzer != null) {
                    imageAnalyzer.clearAnalyzer();
                    imageAnalyzer = null;
                }
                preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build();
                imageAnalyzer = new ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();
                imageAnalyzer.setAnalyzer(backgroundExecutor, this::detectPose);
                if(cameraProvider != null) cameraProvider.unbindAll();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                camera = cameraProvider.bindToLifecycle(getActivity(), cameraSelector, preview, imageAnalyzer);
                Log.i(TAG, "Camera bound successfully");
            } catch (Exception e) {
                Log.e(TAG, "Camera binding failed", e);
                e.printStackTrace();
            }
        });
        backgroundExecutor.execute(() -> {
            try {
                Context context = getActivity();
                Log.i(TAG, "setUpCamera: context "+context);
                poseLandmarkerHelper = new PoseLandmarkerHelper(
                        context,
                        RunningMode.LIVE_STREAM,
                        PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE,
                        PoseLandmarkerHelper.DEFAULT_POSE_TRACKING_CONFIDENCE,
                        PoseLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE,
                        PoseLandmarkerHelper.DELEGATE_CPU,
                        PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL,
                        new PoseLandmarkerHelper.LandmarkerListener() {
                            @Override
                            public void onError(String error, int errorCode) {
                                Log.i(TAG, "onError: test "+error);
                            }
                            @Override
                            public void onResults(PoseLandmarkerHelper.ResultBundle resultBundle) {

                                if(!isTrackingAlreadyInitialised){
                                    isTrackingAlreadyInitialised = true;
                                    //initialisations
                                    lastVisibleTime = System.currentTimeMillis();
                                    if(!isReTrack) lastVisibleTime_saved = System.currentTimeMillis(); //do not reset it when re-track (useful for invitation check)
                                    firstVisibleTime = System.currentTimeMillis();
                                    lastLookingAtCameraTime = System.currentTimeMillis();
                                    visibleDuration = 0;
                                    isPersonDetected = false;
                                    personIsVisible = false;
                                    isProcessingReTrack = false;
                                    getActivity().runOnUiThread(() -> {
                                        if (Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Camera_Display"))) {
                                            Log.i(TAG, "run: teeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeest 0");
                                            reGroup.setTranslationY(560);

                                        } else {
                                            Log.i(TAG, "run: teeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeest 2");
                                            reGroup.setTranslationY(1000);
                                        }
                                    });
                                }

                                if(!isProcessingReTrack){

                                    if(dLeft < initLang){
                                        regarde_camera =  direction && deFace && directionRegardNez.equals("CAMERA");
                                    }
                                    else {
                                        regarde_camera =   deFace && directionRegardNez.equals("CAMERA");
                                    }

                                    PoseLandmarkerResult poseLandmarkerResult = resultBundle.results.get(0);
                                    isPersonDetected = PoseLandmarkerHelper.extractLandmarks(poseLandmarkerResult);

                                    Log.i(TAG_TRACKING_DEBUG, "regarde_camera : "+regarde_camera);
                                    Log.i(TAG_TRACKING_DEBUG, "direction : "+direction);
                                    Log.i(TAG_TRACKING_DEBUG, "deFace : "+deFace);
                                    Log.i(TAG_TRACKING_DEBUG, "directionRegardNez : "+directionRegardNez);
                                    Log.i(TAG_TRACKING_DEBUG, "isPersonDetected : "+ isPersonDetected);

                                    res = poseTracking.suivi(poseLandmarkerResult);
                                    eog = res[0];
                                    Eod = res[1];
                                    degx = res[2];
                                    degy = res[3];
                                    x0 = res[4];
                                    x2 = res[5];
                                    x5 = res[6];
                                    y0 = res[7];
                                    y2 = res[8];
                                    y5 = res[9];
                                    lang = res[12];
                                    dLeft = res[10];
                                    dRight = res[11];

                                    if (isPersonDetected != wasPersonDetected) {
                                        if (!isPersonDetected) {
                                            poseTracking.stopMovingAndCancelRunnables();
                                        }
                                        // Update the previous state
                                        wasPersonDetected = isPersonDetected;
                                    }

                                    if(isPersonDetected){
                                        poseTracking.lookAt(degx, degy);

                                        if (Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Body")) || Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Head"))) {
                                            if (TRACKING_WATCH.trim().equalsIgnoreCase("Yes")) {
                                                if (regarde_camera) {
                                                    poseTracking.rotation(degx, Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Body")));
                                                }
                                            }
                                            else {
                                                poseTracking.rotation(degx, Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Body")));
                                            }
                                        }

                                        if (Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Head"))) {
                                            if (TRACKING_WATCH.trim().equalsIgnoreCase("Yes")) {
                                                if (regarde_camera) {
                                                    poseTracking.yesTracking(degy);
                                                }
                                            } else {
                                                poseTracking.yesTracking(degy);
                                            }
                                        }
                                    }

                                    directionRegardNez = poseTracking.directionVisage(x2, y2, x5, y5, x0, y0, lang);

                                    if ((x2 - x5) > 0 ) {
                                        deFace = true;
                                    }
                                    else {
                                        deFace = false;
                                    }
                                    if (eog > Eod * 2) {
                                        direction = false;
                                    }
                                    else {
                                        if (eog * 1.5 < Eod) {
                                            direction = false;
                                        } else {
                                            direction = true;
                                        }
                                    }

                                    getActivity().runOnUiThread(() -> {
                                        if (overlay != null) {
                                            overlay.setResults(resultBundle.results.get(0), resultBundle.inputImageHeight, resultBundle.inputImageWidth, RunningMode.LIVE_STREAM);
                                            overlay.setNbrLandmarks(poseTracking.getLandmarksCamera(poseLandmarkerResult));
                                        }
                                    });

                                    handlerCheckPersonDetection.removeCallbacks(runnableCheckPersonDetection);
                                    handlerCheckPersonDetection.removeCallbacksAndMessages(null);
                                    handlerCheckPersonDetection.post(runnableCheckPersonDetection);
                                }
                                else{
                                    if (Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Camera_Display"))) {
                                        Log.i(TAG, "onResults: Tracking_Camera_Display");
                                        getActivity().runOnUiThread((Runnable) () -> reGroup.setTranslationY(0));
                                    }

                                }
                            }
                        }
                );
            }catch (Exception e){
                Log.i(TAG, "setUpCamera: error catch"+e);
                e.printStackTrace();
            }
        });
    }

    private void detectPose(ImageProxy imageProxy) {
        poseLandmarkerHelper.detectLiveStream(imageProxy);
    }
    /**
     * ------------------------------------------ STT  -------------------------------------------
     */

    private void startListeningFreeSpeech(int duration) {

        isListeningFreeSpeech = true;
        buddyGPTApplication.setMessageError(false);
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
        } else if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(GOOGLE_STT)) {
            buddyGPTApplication.startListeningQuestionWav(getActivity());
        } else if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(WHISPER_STT)) {
            // RECIRDING whisper
        }


        if (timerEcoute != null) timerEcoute.cancel();
        timerEcoute = new CountDownTimer(duration * 1000, 1000) {
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
                    if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(ANDROID_STT) || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(CERENCE_STT)|| buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(GOOGLE_STT)) {
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
        timerEcoute = new CountDownTimer(buddyGPTApplication.getListeningDuration() * 1000, 1000) {
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
                            Log.i(TAG, "speak: HOU " +reponse.toString());
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

        Log.i(TAG, "getData: settingClass");
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

        Log.i(TAG, "getData: settingClass end");


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

    private void refreshSTTLangue() {
        buddyGPTApplication.refresh(new Gson().fromJson(buddyGPTApplication.getparam(settingClass.getLangue()), Langue.class).getLanguageCode(), getActivity());
    }

    /**
     * ----------------- Gestion de notifications ---------------------------
     */
    @Override
    public void update(String message) {
        if (message != null) {

            if (message.contains("CANCEL_RESPONSE_TIMEOUT") && responseTimeout != null) responseTimeout.cancel();
            if (message.contains("MODE_STREAM_TEXT;SPLIT;")) {
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
            if (message.contains("MODE_STREAM_SPEAK;SPLIT;")) {
                getActivity().runOnUiThread(() -> {
                    if (message.split(";SPLIT;").length > 1) {
                        String phraseToPronounce = message.split(";SPLIT;")[1];
                        speak(phraseToPronounce, "nothealysa");
                    }
                });
            }
            if (message.contains("STTHotword_success")) {
                if (buddyGPTApplication.getparam("INVALID_TEAMGPT_KEY").equalsIgnoreCase("TRUE")) {
                    Log.i("TAG", "run: notifyObservers INVALID_TEAMGPT_KEY 4");
                    buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
                }
                else if (buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
                    Log.i("TAG", "run: notifyObservers TEAMGPT_KEY EMPTY 3");
                    if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_EN)) {
                        buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_en));
                    } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_FR)) {
                        buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_fr));
                    } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_ES)) {// tm-EXbMvZ8fILEODuTJTq1alkw96LYku11P1dSi9U5RT6yqhcC
                        buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_es));
                    } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_DE)) {
                        buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_de));
                    } else {
                        buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                                .translate(getString(R.string.toast_teamgpt_key_indispo_en))
                                .addOnSuccessListener(translatedText -> buddyGPTApplication.showToast(translatedText))
                                .addOnFailureListener(e -> buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_en)));
                    }
                }
                else if(buddyGPTApplication.getparam("ENV_ERROR").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")){
                    Log.i("TAG", "run: notifyObservers ENV_ERROR 4");
                    buddyGPTApplication.notifyObservers("ENV_ERROR");
                }
                else {
                    Log.i(TAG, "update: STTHotword_success else");
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
            if (message.contains("STTQuestion_success")) {
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
                                buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate("I heard ").addOnSuccessListener(translatedText -> buddyTexteQst.setText(format(" %s  :  %s ", translatedText, detectedSTTMessage))).addOnFailureListener(e -> Log.e(TAG, "translatedText exception  " + e));
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
                        buddyGPTApplication.getResponseFromTeamGPT().sendPutRequestStream(detectedSTTMessage,null);


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
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                            // Remettre le visage à NEUTRAL après 10 secondes
                                            if(isSpeaking) checkSpeakingAndSetNeutral();
                                            else {
                                                handler.postDelayed(runnable = () -> {
                                                    BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
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
                                                    if (responseTimeout != null)
                                                        responseTimeout.cancel();
                                                    if (buddyGPTApplication.getResponseFromTeamGPT() != null)
                                                        buddyGPTApplication.getResponseFromTeamGPT().reset();
                                                    if (runnableTTSError != null) {
                                                        handlerTTSError.removeCallbacks(runnableTTSError);
                                                        handlerTTSError.removeCallbacksAndMessages(null);
                                                    }
                                                    if (Boolean.TRUE.equals(!buddyGPTApplication.getSpeaking()) && Boolean.TRUE.equals(!mlKitIsDownloading)) {
                                                        Log.d(TAG, "Mouth touched2");
                                                        if (buddyGPTApplication.getparam("INVALID_TEAMGPT_KEY").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
                                                            Log.i("TAG", "run: notifyObservers INVALID_TEAMGPT_KEY 3");
                                                            buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
                                                        } else if (buddyGPTApplication.getparam("ENV_ERROR").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
                                                            Log.i("TAG", "run: notifyObservers ENV_ERROR 3");
                                                            buddyGPTApplication.notifyObservers("ENV_ERROR");
                                                        } else if (buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
                                                            Log.i("TAG", "run: notifyObservers TEAMGPT_KEY EMPTY 3");
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

                                                    } else if (Boolean.TRUE.equals(buddyGPTApplication.getSpeaking()) && Boolean.TRUE.equals(!mlKitIsDownloading)) {
                                                        Log.d(TAG, "Mouth touched3 STT  " + buddyGPTApplication.getparam("STT") + " AUTRE " + buddyGPTApplication.getAppIsListeningToTheQuestion());
                                                        if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Android")
                                                                || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Cerence")
                                                                || Boolean.TRUE.equals(!buddyGPTApplication.getAppIsListeningToTheQuestion())) {
                                                            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                                                            buddyGPTApplication.setStartRecording(false);
                                                            buddyGPTApplication.setActivityClosed(true);
                                                            isListeningFreeSpeech = false;
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
                                                }, Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds", "BuddyGPT.properties")) * 1000);
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
            if (message.contains("TTS_success")) {
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
            if (message.contains("Emotion_Change")) {
                buddyGPTApplication.setAnimation(message.split(";SPLIT;")[1]);
            }
            if (message.contains("TTS_error") || message.contains("TTS_exception")) {
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
            if (message.contains("properties file done")) {
                buddyGPTApplication.setNotYet(false);
                getData();
            }
            if (message.contains("end of timer")) {
                buddyGPTApplication.setAppIsListeningToTheQuestion(false);
                stopListeningFreeSpeech();
                SystemClock.sleep(200);
                buddyGPTApplication.startListeningHotwor(getActivity());
            }
            if (message.contains("end of cycle")) {
                getActivity().runOnUiThread(() -> {
                    BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                    BuddySDK.UI.stopListenAnimation();
                    buddyGPTApplication.setLed("neutral");
                });
            }
            if (message.contains("Obtain audio transcription after the listening time has elapsed")) {
                String shouldRestartNewCycle = message.split(";SPLIT;")[1];
                Log.e(TAG, "Obtain audio transcription after the listening time has elapsed " + shouldRestartNewCycle);
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                BuddySDK.UI.stopListenAnimation();
                buddyGPTApplication.setLed("neutral");
                buddyGPTApplication.setAppIsListeningToTheQuestion(false);
                buddyGPTApplication.traitementAudio();
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
            if (message.contains("QST_LAYOUT_DISMISSED")) {
                buddyTexteQstLyt.setVisibility(View.INVISIBLE);
                buddyTexteQst.setMovementMethod(null);
                lytOpenMenuSettings.setVisibility(View.VISIBLE);
                lytOpenMenuChat.setVisibility(View.VISIBLE);
            }
            if (message.contains("playStoredResponse") && !buddyGPTApplication.getStoredResponse().equals("")) {
                    getActivity().runOnUiThread(() -> speak(buddyGPTApplication.getStoredResponse(), "storedResponse"));
                }
            if (message.contains("ChatDestroy")) {
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
                int speakVolume = buddyGPTApplication.getVolume();
                int max = buddyGPTApplication.getMaxVolume();
                int defaultVolume = buddyGPTApplication.getClosestInt((double) (speakVolume * 100) / max);
                Log.e(TAG, "volumeMedia  " + defaultVolume);
                buddyGPTApplication.setparam("speak_volume", valueOf(defaultVolume));
            }
            if (message.contains("ENV_ERROR")){
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
            if(message.contains("RESTART_TRACKING")){
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(buddyGPTApplication.getparam("Tracking_Activation").contains("yes")){
                            isReTrack = true;
                            Log.i(TAG, "trackingtests 3");
                            initTracking();
                        }
                    }
                });
            }
        }
    }

    public void initTeamGPTSettings() throws IOException {
        buddyGPTApplication.setparam("TeamGPT_url", buddyGPTApplication.getParamFromFile("TeamGPT_url", configFile));
        buddyGPTApplication.setparam("TeamGPT_ApiEndpoint_Params", buddyGPTApplication.getParamFromFile("TeamGPT_ApiEndpoint_Params", configFile));
        buddyGPTApplication.setparam("TeamGPT_ApiEndpoint_Response", buddyGPTApplication.getParamFromFile("TeamGPT_ApiEndpoint_Response", configFile));
        Log.i(TAG, "initTeamGPTSettings: TeamGPT_ID_Device: "+buddyGPTApplication.getParamFromFile("TeamGPT_ID_Device", configFile));
        if (buddyGPTApplication.getParamFromFile("TeamGPT_ID_Device", configFile).equalsIgnoreCase("")){
            buddyGPTApplication.setparam("TeamGPT_ID_Device", buddyGPTApplication.getparam("IMEI"));
        }
        else{
            buddyGPTApplication.setparam("TeamGPT_ID_Device", buddyGPTApplication.getParamFromFile("TeamGPT_ID_Device", configFile));
        }
        ResponseFromTeamGPT rft = buddyGPTApplication.getResponseFromTeamGPT();
        if (rft == null) {
            rft = new ResponseFromTeamGPT(buddyGPTApplication);
            buddyGPTApplication.setResponseFromTeamGPT(rft);
        }
        if (buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
            lytSpinner = view.findViewById(R.id.lyt_spinner);
            if (lytSpinner != null) {
                lytSpinner.setVisibility(View.GONE);
            }
            buddyGPTApplication.setparam("TeamGPT_Key", buddyGPTApplication.getParamFromFile("TeamGPT_Key", configFile));
            Log.i("TAG", "run: getParameters 4");

            if(!buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")){
                // Appeler getParameters
                if (timeoutHandler != null && timeoutRunnable != null) {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                }

                    rft.getParameters(new ResponseCallback() {
                        @Override
                        public void onSuccess() {
                            getActivity().runOnUiThread(() -> {
                                Log.i(TAG, "run: TEST successsss");
                                // Annuler le timeout et cacher le spinner
                                if (timeoutHandler != null && timeoutRunnable != null) {
                                    timeoutHandler.removeCallbacks(timeoutRunnable);
                                }
                                lytSpinner.setVisibility(View.GONE);
                            });
                        }

                        @Override
                        public void onFailure() {
                            getActivity().runOnUiThread(() -> {
                                Log.i(TAG, "run: TEST failuuuure");
                                // Annuler le timeout et afficher un message d'erreur
                                if (timeoutHandler != null && timeoutRunnable != null) {
                                    timeoutHandler.removeCallbacks(timeoutRunnable);
                                }
                                lytSpinner.setVisibility(View.VISIBLE);
                                buddyGPTApplication.notifyObservers("ENV_ERROR");
                            });
                        }
                    });

            }
            else{
                if (buddyGPTApplication.getLangue().getNom().equals("Anglais")){
                    buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_en));
                }
                else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                    buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_fr));
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
            }
        }
        else

            rft.getParameters(new ResponseCallback() {
                @Override
                public void onSuccess() {
                    getActivity().runOnUiThread(() -> {
                        Log.i(TAG, "run: TEST success");
                        // Annuler le timeout et cacher le spinner
                        if (timeoutHandler != null && timeoutRunnable != null) {
                            timeoutHandler.removeCallbacks(timeoutRunnable);
                        }
                        lytSpinner.setVisibility(View.GONE);
                    });
                }

                @Override
                public void onFailure() {
                    getActivity().runOnUiThread(() -> {
                        Log.i(TAG, "run: TEST failuuuure");
                        // Annuler le timeout et afficher un message d'erreur
                        if (timeoutHandler != null && timeoutRunnable != null) {
                            timeoutHandler.removeCallbacks(timeoutRunnable);
                        }
                        lytSpinner.setVisibility(View.VISIBLE);
                        buddyGPTApplication.notifyObservers("ENV_ERROR");
                        buddyGPTApplication.setparam("ENV_ERROR", "TRUE");
                    });
                }
            });
        if (buddyGPTApplication.getparam("firstLaunch").equals("true")) {
            if (buddyGPTApplication.getparam(header).equals("")) {
                buddyGPTApplication.setparam(header, buddyGPTApplication.getParamFromFile(header, configFile));
            }
            if (buddyGPTApplication.getparam(entete).equals("")) {
                buddyGPTApplication.setparam(entete, buddyGPTApplication.getParamFromFile(entete, configFile));
            }
            buddyGPTApplication.setparam("firstLaunch", "false");
        }
        // TeamGPT
    }

    private void checkSpeakingAndSetNeutral() {
        handler.postDelayed(() -> {
            if(isSpeaking) checkSpeakingAndSetNeutral();
            else {
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                buddyTexteQstLyt.setVisibility(View.INVISIBLE);
                buddyTexteRespLyt.setVisibility(View.INVISIBLE);
                buddyTexteQst.setMovementMethod(null);
                buddyTexteResp.setMovementMethod(null);
                lytOpenMenuSettings.setVisibility(View.VISIBLE);
                lytOpenMenuChat.setVisibility(View.VISIBLE);
                if (handler != null && runnable != null) {
                    handler.removeCallbacks(runnable);
                    handler.removeCallbacksAndMessages(null);
                }
                if (responseTimeout != null)
                    responseTimeout.cancel();
                if (buddyGPTApplication.getResponseFromTeamGPT() != null)
                    buddyGPTApplication.getResponseFromTeamGPT().reset();
                if (handlerTTSError != null && runnableTTSError != null) {
                    handlerTTSError.removeCallbacks(runnableTTSError);
                    handlerTTSError.removeCallbacksAndMessages(null);
                }
                if (Boolean.TRUE.equals(!buddyGPTApplication.getSpeaking()) && Boolean.TRUE.equals(!mlKitIsDownloading)) {
                    Log.d(TAG, "Mouth touched2");
                    if (buddyGPTApplication.getparam("INVALID_TEAMGPT_KEY").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
                        Log.i("TAG", "run: notifyObservers INVALID_TEAMGPT_KEY 3");
                        buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
                    } else if (buddyGPTApplication.getparam("ENV_ERROR").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
                        Log.i("TAG", "run: notifyObservers ENV_ERROR 3");
                        buddyGPTApplication.notifyObservers("ENV_ERROR");
                    } else if (buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
                        Log.i("TAG", "run: notifyObservers TEAMGPT_KEY EMPTY 3");
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
                } else if (Boolean.TRUE.equals(buddyGPTApplication.getSpeaking()) && Boolean.TRUE.equals(!mlKitIsDownloading)) {
                    Log.d(TAG, "Mouth touched3 STT  " + buddyGPTApplication.getparam("STT") + " AUTRE " + buddyGPTApplication.getAppIsListeningToTheQuestion());
                    if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Android")
                            || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Cerence")
                            || Boolean.TRUE.equals(!buddyGPTApplication.getAppIsListeningToTheQuestion())) {
                        BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                        buddyGPTApplication.setStartRecording(false);
                        buddyGPTApplication.setActivityClosed(true);
                        isListeningFreeSpeech = false;
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
            }
        }, Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds", "BuddyGPT.properties")) * 1000);
    }
    // Add at the top of your MainFragment


    public void sendPostRequestSafe() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .retryOnConnectionFailure(true)
                        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS) // infinite for streaming
                        .build();

                // Your JSON body
                String jsonInputString = "{"
                        + "\"Audio_input\": \"UklGRgZZAABXQVZFZm10IBAAAAABAAEAgD4AAAB9AAACABAATElTVBoAAABJTkZPSVNGVA4AAABMYXZmNTkuMjcuMTAwAGRhdGHAWAAAAAAAAAAAAAAAAAAAAAAAAAAA//8AAAAAAAAAAAAA//8AAAEAAAAAAAAAAQABAP//AAD//wAAAAAAAAAAAAD/////AAD//wAAAQAAAAAAAAAAAAAAAAABAAEAAAD//wEAAAD/////AAAAAAAAAAD//wAA//8BAP//AAAAAP//AQAAAAAAAgAAAAEAAAABAAAAAQABAAAAAAAAAAAAAQABAAAAAAAAAAAA//8AAP//AQAAAAEAAAD//wAA//8AAP//AAD/////AQD//wAAAQABAAAAAAAAAAAAAAABAAAAAQAAAAEAAAAAAP///v8AAP7/BQAGAAEA+//7/wAAAgD//////v8AAP7//f8BAP3//v//////AAD9//7//f/8//3//P8BAP///f////7/AQADAAIAAwADAAEAAAADAAMAAgD//wEAAAADAAIAAgAEAAQAAwD//wMAAwAEAAMAAwAAAAEA//8AAP///f/9////AAABAAMAAAACAAEAAQADAAMAAwACAAAAAgACAAAAAAD9////AAADAAMA//8AAP//AgAAAAAAAAD//wAAAAD9/wAAAAAAAAAA/v8BAP///v8AAP3//v/+////AAD/////AAD+/wAA/v8AAAEAAAAAAP//AQD//wEAAgAAAAIAAQD//wEAAAABAAEAAQD//wAAAAAAAAEAAAABAAAAAAABAP//AAABAP//AAAAAAQAAgACAP//AAADAAQABAACAAMAAwADAAEAAwABAAMAAgADAAIAAQD//wAAAwACAAIAAwADAAEAAgAFAAIAAQACAAQAAQD9/wMAAQAAAAAA/v8BAAEAAAD/////AAD//wEA///+////AAABAAEAAAAAAAIAAgABAAIAAwABAP//AAD///3/AAAAAAEAAQAAAP3//v8AAAEA/v///wAA////////AQD+/wAAAgABAAEAAAACAP3/AQAAAAEAAQAAAP//AgAAAP//AAAAAAEAAAAAAP//AAD+//7///8AAAAA//8AAAEA/////wAA/////wAAAAABAAAAAQABAAEAAAD//wAAAQABAP//AQABAAAAAAAAAP//AgAAAAAAAAAAAAAA//8AAP////8AAAAAAAAAAAAA/////wAAAAAAAAEAAAAAAP//AAABAAAAAAAAAAAAAAAAAAAA//8AAAAA////////////////////////AAAAAAAA//8AAAAA/////wAAAAD//wAAAAD/////AAAAAP///////////////wAAAAD//wAAAAD//wAAAAD///7//v/+//3//f/9//3//f/+/wAAAQACAAIAAQAAAAAAAAAAAAAAAAABAAEAAwADAAMAAwACAAEAAgADAAQABQAFAAQAAwACAAIAAQABAAEAAgADAAQABgAHAAUAAwADAAIAAQAAAAEAAQAAAAAAAQABAAEAAgACAAEAAgAEAAYABAAEAAQAAQD//wAAAgABAAIABQAFAAIAAwAFAAEA/P8AAAIA/f///wQAAAD8//3/+//8//3/AQD9//b/+f/5//r/+//7//z//f/4//b//f///wEAAwABAAIAAAD/////BAACAAEAAgAEAAQABAAFAAcABAAEAAEABgAHAAQABgAEAAYABAALAA4ABgAHAAQAAwAFAAcADAAOAAkACwAPAA4ADAAUABQACgAOABAADwASABMAGAAXABAAFQATABMAGQAUABcAFwAVABwAGwAaABoAGgAUABQAHQAdAB0AJAAgABoAIQAlACkAJAAmACkAKgApACEAMQA0AC8ANQA4ADEAPwBLAE8AVQBgAGEAagBzAGoAdwBvAHEAdQBxAHcAbgBlAHoAfgB/AIsAcgCSAGgAPwBYAGEANwAAAAwABwDj/wEAbwBLAHcANgEBAYwAjABhAC8AYv8u/xz/X/6n/dr90P6g/mL/9f+5/2T/5f7H/mL+T/7E/vH+3/74/vT+jf5R/kz+Gf4d/nL+ZP5f/qP+s/7Y/vX+Cf8I/9D+u/64/o3+i/6Q/oD+kv5O/vf97/3F/Y79Tf0w/eT8vvyw/Ib8d/xW/Fb8ZPxi/EL8TPyB/JH8hvxx/JP84Pz8/Bn9g/2s/SP+2P6W/1AAvwBxAUcCAgN+AwYEmgQhBZkFCwZ3BuEGRAe2BycIZwjHCDMJhwnRCRkKRQpmCnkKZwpJCvUJqQlLCdMIfQgICLAHRwcKB6YGJwbrBXYFIgXNBGoECwTAA3wDPQMVA88ClAJNAg4CsgFXAfYAhQAQAIz/O/+5/kv+8/1//SD9zPxr/A38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\","
                        + "\"ID_DEVICE\": \"3\","
                        + "\"EMOTION\": true,"
                        + "\"Commandes\": false,"
                        + "\"LANGUE\": \"fr\""
                        + "}";

                RequestBody body = RequestBody.create(
                        jsonInputString,
                        MediaType.get("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url("https://chat.teamgpt.fr/api/get-response")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Accept", "text/event-stream")
                        .addHeader("TeamGPT-key", "tm-EXbMvZ8fILEODuTJTq1alkw96LYku11P1dSi9U5RT6yqhcC")
                        .addHeader("ID_DEVICE", "3")
                        .post(body)
                        .build();

                EventSource.Factory factory = EventSources.createFactory(client);

                factory.newEventSource(request, new EventSourceListener() {
                    @Override
                    public void onOpen(EventSource eventSource, Response response) {
                        Log.d("HTTP", "Connected to SSE stream");
                    }

                    @Override
                    public void onEvent(EventSource eventSource, @Nullable String id,
                                        @Nullable String type, String data) {
                       logLargeString("HTTP", data);

                    }

                    @Override
                    public void onClosed(EventSource eventSource) {
                        Log.d("HTTP", "Stream closed by server");
                    }

                    @Override
                    public void onFailure(EventSource eventSource, @Nullable Throwable t,
                                          @Nullable Response response) {
                        Log.e("HTTP", "SSE error: " + (t != null ? t.getMessage() : "unknown"));
                        if (response != null) {
                            Log.e("HTTP", "Response code: " + response.code());
                        }
                    }
                });

            } catch (Exception e) {
                Log.e("HTTP", "sendPostRequestSafe exception: " + e.getMessage(), e);
            }
        }).start();
    }
    private void logLargeString(String TAG,String str) {
        if(str.length() > 3000) {
            Log.i(TAG, str.substring(0, 3000));
            logLargeString(TAG,str.substring(3000));
        } else {
            Log.i(TAG, str);
        }
    }
    public  void sendPostRequest() throws IOException {
        String urlString = "https://chat.teamgpt.fr/api/get-response";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Configuration de la requête
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("TeamGPT-key", "tm-EXbMvZ8fILEODuTJTq1alkw96LYku11P1dSi9U5RT6yqhcC");
        conn.setRequestProperty("ID_DEVICE", "3");
        conn.setDoOutput(true);


// Corps JSON

        String jsonInputString = "{"
                + "\"Audio_input\": \"SUQzBAAAAAAAI1RTU0UAAAAPAAADTGF2ZjU5LjI3LjEwMAAAAAAAAAAAAAAA//OEwAAAAAAAAAAAAEluZm8AAAAPAAAAIAAADMAAFhYWHh4eJSUlLS0tNDQ0PDw8Q0NDS0tLUlJSUlpaWmFhYWlpaXBwcHh4eICAgIeHh4+Pj4+Wlpaenp6lpaWtra20tLS8vLzDw8PLy8vL0tLS2tra4eHh6enp8PDw+Pj4////AAAAAExhdmM1OS4zNwAAAAAAAAAAAAAAACQDMAAAAAAAAAzANF9hCgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA//NExAALOAImPghGAEQHF+SKkd4sNCxi6QsfjdjutDXcVAiGjbR3KfvTGo3f2v6f6zbPkB38cmtj0gMFoEARVRgYMuJjhc0DIVOKdobFguPComQu4ChU4ZFowUaaU0ma//NExCYQcCowBUMYAKGgUDqVe6fKZTYlmuhHi0oaufQvLrS5b07aTiIAIIIMMBAMCED/nsDgahNR/94dtma4J/9wR0QM6TG4AXAMNQxV5upBgAvAdCBdMBrH/hYmFi4C//NExDcfyxJhm5qYAUIauDpP0/qIeRMg6Cf/+K3ImQcjxqEiND/9TNxc4xgXNASADZYWih9zUUn///+XllorkUJItG6R0rnf////8yJwwOm7JGhF8lURpGLIMRHDF5o2//NExAoVAO5ws9sYAKnzOgACgZqw0jkwICjKNL+Nfe+E0M1DUOw7Y7ZprXdtQo0YCAhS7G2SqfSVcj+GKCFqodNCgNEe3rDQK/2iX+sr3NLDAKV/vK2rBaLdNySMCwig//NExAkUCLatnoJMIj46gBYYcRSMQKLohMfYUTrLQiDx5mW1tH8RJMuBC4gKOEAnQGMuFwIC5Bp8PhgcGXBYIv6D5Nb2Xhio4OT8lp/CzsEIY4g3KsbkjAuIOPjjmMos//NExAsTcTKcAMvMbD/xFFcY6sNweiZoTBND8slEIdxaHw4akjeSocfjPzWlm2douDJbfme9ff4hCD09LFCRuy/f9v////aUcLJccJ3pb6r+TaRIl/GZwcrVfIOFQcFV//NExBAT+q6oAMjK8MigpY76U0QBgoCPi1C02f7cIXAX2x3zQl/L+fW3wz26g3mPcRcJjRYjne/9k////////////5GeMHGLYxHdFCmFEq2S4EoPuj8EXeIIqT7UyRE0//NExBMWKprJnnpEzomYoSlVexKuZWQm66pPvmKM6pR3sdf/9vJoQ4NFsVySC8pmBAR0ROyLe6hbuZ5kV3m3b/////////qApDwZLid6H/dBgQoDRqUCoEoP8CnhhZ70//NExA0UqrbBnnsKkhejfYz8CHssqECR99UhnkvLPth59fTqP1gWYxgfmbC3ocipPx67GZWH6CibqtG1P1fIza9v////////1TMYmowy90lFPGUAQtZAEBURgd5aZCHq//NExA0SYRKyXssKjEHX5geGWNTSVW1i+uoDObNAeJt2TMW5JeftKgqUD/KP7cmvR3oQmPPLIxV4bIrGi5OxR6n////74TmgHqUUyBkA8sMdTKswh/DWM2IGrcvXK6tX//NExBYSYUKpHMLEzPdvFlrFoq69tCFopAJMflVcokxl48c5U2gHyu7rcPPPMNebhmKhqip21LP////3nyEnGLWNCAA4cP3cZ+f6S/DpfQNT1EGP0xhiRnMAVDvqGN3J//NExB8R2rasfMLKVAMzGPsFuc4DMd3Pye/KhqveXl9uXo3b6L//////////1yXKsacdKEkKAtmhrcvAgG5AAJxLToB+Q2ng+RdnyEi6AmDtFe5OLgiHo38Aq6HHA4fT//NExCoQuQK9vpsGbP8i+GMwRGrQ7qAwUa4kjr/////7w+eDjdcDV2VbgAHxkPgEHa7YTE6sdOxaXhuOPDP5HvdxrsqFzmM7K22t1tQQjKBnVyLXQjd/7Hdjun25530a//NExDoSCe7BlniM/7PaTkwAhdk9/6x+AsMjqv4AIQWqPc/kOCS/zX/kf/Op4637CfIUYUVefgQ1iiAkHj9b6zbm33FkRZJA+Kg+XECKwsMicCmiTXPRvIYYKOplGE////NExEQRoTq8AHhYXHr++lIAABAA3PAxARCkrWP7////95dCLUccxb2UZJgopYOG5dpR6GFs8Otn9TE2KSehp0XYvbJCHhDwdbghdtpyC1evmGcTCByxabdnyrrLd1Qt//NExFASOcbeXCgQVagFuOjgmxiSGZhQ9JipwTaJIS5r/wj0hgEF3nprirxgZqagGRIl7EmA6gdmhoPAUdJtELKW0C96XfV+zwVtivsVY2WH+slftzCrckqwdv3AMTnN//NExFoR0JLvFmDMVEkI/MuT0uIkfXOAYtEe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\","
                + "\"ID_DEVICE\": \"3\","
                + "\"EMOTION\": true,"
                + "\"Commandes\": false,"
                + "\"LANGUE\": \"fr\""
                + "}";

        System.out.println("HOU Sending ");

        // Envoi du corps
        try (
                OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Lecture de la réponse
        int status = conn.getResponseCode();
        Reader streamReader = (status > 299) ? new InputStreamReader(conn.getErrorStream(), "utf-8")
                : new InputStreamReader(conn.getInputStream(), "utf-8");

        try (BufferedReader in = new BufferedReader(streamReader)) {
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            // Affichage complet dans les logs
            System.out.println("HOU Status Code: " + status);
            System.out.println("HOU Response: " + response.toString());
        }

        conn.disconnect();
    }
}
