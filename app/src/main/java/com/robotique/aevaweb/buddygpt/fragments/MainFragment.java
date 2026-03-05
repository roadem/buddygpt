package com.robotique.aevaweb.buddygpt.fragments;


import static com.robotique.aevaweb.buddygpt.utilis.tracking.PoseTracking.TAG_TRACKING_DEBUG;
import static java.lang.String.format;
import static java.lang.String.valueOf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
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
import com.robotique.aevaweb.buddygpt.utilis.CustomToast;
import com.robotique.aevaweb.buddygpt.utilis.IMLKitDownloadCallback;
import com.robotique.aevaweb.buddygpt.utilis.ITTSCallbacks;
import com.robotique.aevaweb.buddygpt.utilis.ResponseCallback;
import com.robotique.aevaweb.buddygpt.utilis.tracking.MainViewModel;
import com.robotique.aevaweb.buddygpt.utilis.tracking.OverlayView;
import com.robotique.aevaweb.buddygpt.utilis.tracking.PoseLandmarkerHelper;
import com.robotique.aevaweb.buddygpt.utilis.tracking.PoseTracking;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    private static final String WHISPER_STT = "openai";
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
    private boolean isWaitingForResponse = false;
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
    private String directionRegardNez= "";

    // When true, indicates that a server-sent "BuddyFace_Neutral" should be
    // applied after the current TTS playback finishes instead of immediately.
    private boolean pendingNeutralEmotion = false;
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
                        buddyGPTApplication.startListeningHotwor(getActivity());
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
                Log.i("BBB", "⏱️ Posting runnableProgressBar with 500ms delay (ML Kit download)");
                handlerProgressBar.postDelayed(runnableProgressBar, 500);
                timerDownloading.start();
            }

        }
    };
    private final Runnable runnableProgressBar = new Runnable() {
        @Override
        public void run() {
            Log.i("BBB", "⚠️ runnableProgressBar executing - about to reset face to NEUTRAL");
            buddyGPTApplication.stopTTS();
            stopListeningFreeSpeech();
            try {
                Log.i("BBB", "⚠️ runnableProgressBar calling setFacialExpression(NEUTRAL)");
                Log.i("🔍_NEUTRAL_HUNT", "════════════════════════════════════════════════════════");
                Log.i("🔍_NEUTRAL_HUNT", "🔍 setFacialExpression(NEUTRAL) called from: MainFragment.runnableProgressBar");
                Log.i("🔍_NEUTRAL_HUNT", "🔍 Current active emotion: " + BuddyGPTApplication.currentActiveEmotion);
                StackTraceElement[] st = Thread.currentThread().getStackTrace();
                for (int i = 1; i < Math.min(6, st.length); i++) {
                    Log.i("🔍_NEUTRAL_HUNT", "   └─ [" + i + "] " + st[i].getClassName() + "." + st[i].getMethodName() + ":" + st[i].getLineNumber());
                }
                Log.i("🔍_NEUTRAL_HUNT", "════════════════════════════════════════════════════════");
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                BuddySDK.UI.stopListenAnimation();
                Log.i("BBB", "✓ runnableProgressBar face reset to NEUTRAL");
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
                        if (buddyGPTApplication.getparam("INVALID_TEAMGPT_DEVICE_ID").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
                            Log.i("TAG", "run: notifyObservers INVALID_TEAMGPT_DEVICE_ID 3");
                            buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_DEVICE_ID");
                        }
                        else if (buddyGPTApplication.getparam("INVALID_TEAMGPT_KEY").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
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
                        } else if (buddyGPTApplication.getparam("EnvInProgress").equalsIgnoreCase("true")) {
                            Log.i("TAG", "run: notifyObservers EnvInProgress");
                            if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_EN)) {
                                buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_en));
                            } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_FR)) {
                                buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_fr));
                            } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_ES)) {
                                buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_es));
                            } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_DE)) {
                                buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_de));
                            } else {
                                buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                                        .translate(getString(R.string.toast_teamgpt_EnvProgress_en))
                                        .addOnSuccessListener(translatedText -> buddyGPTApplication.showToast(translatedText))
                                        .addOnFailureListener(e -> buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_en)));
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
        buddyGPTApplication = (BuddyGPTApplication) getActivity().getApplicationContext();
        buddyGPTApplication.hideSystemUI(getActivity());
        configureSystemUI();
        buddyGPTApplication.registerObserver(this);
        // restore initOrMajOrNone from arguments to survive process recreation
        if (savedInstanceState != null && savedInstanceState.containsKey("initMode")) {
                 initOrMajOrNone = savedInstanceState.getString("initMode", "");
             } else if (getArguments() != null && getArguments().containsKey("initMode")) {
                 initOrMajOrNone = getArguments().getString("initMode", "");
             }
         Log.i(TAG, "onCreate: restored initOrMajOrNone=" + initOrMajOrNone);

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
        lytOpenMenuSettings.setOnClickListener(v -> btnOpenFragment("settingsFragment"));
        lytOpenMenuChat.setOnClickListener(v -> btnOpenFragment("chatFragment"));
        // Initialiser le spinner
        lytSpinner = view.findViewById(R.id.lyt_spinner);
        lytSpinner.setVisibility(View.GONE);

        if (buddyGPTApplication.isFirstLaunch()) {

            Log.i(TAG, "onCreateView: isFirstLaunch=false;");
            initTeamGPTSettings();
            buddyGPTApplication.setFirstLaunch(false);
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

        if(cameraProvider != null) cameraProvider.unbindAll();
        Log.i(TAG, "onCreateView: --------------end");
        return view;


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

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, " --- onCreate() ---");
    }
    @Override
    public void onDestroyView() {
        Log.d(TAG, " --- onDestroyView() ---");
        if (responseTimeout != null) responseTimeout.cancel();
        if (handlerTTSError != null && runnableTTSError != null) {
            handlerTTSError.removeCallbacks(runnableTTSError);
            handlerTTSError.removeCallbacksAndMessages(null);
        }
        if (buddyGPTApplication.getResponseFromTeamGPT() != null)
            buddyGPTApplication.getResponseFromTeamGPT().reset();
        if (handlerPauseTime != null && runnablePauseTime != null) {

            handlerPauseTime.removeCallbacksAndMessages(null);
            handlerPauseTime.removeCallbacks(runnablePauseTime);

        }
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
            handler.removeCallbacksAndMessages(null);
        }
        isListeningFreeSpeech = false;
        buddyGPTApplication.removeObserver(this);
        buddyGPTApplication.stopTTS();
        buddyGPTApplication.setActivityClosed(true);
        stopListeningFreeSpeech();
        CustomToast.getInstance().hideToast();
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
            BuddySDK.UI.removeFaceTouchListener(iuiFaceTouchCallback);
        } catch (Exception e) {
            Log.e(TAG, "BuddySDK Exception  " + e);
        }
        if (cameraProvider != null) cameraProvider.unbindAll();
        BuddySDK.UI.removeFaceTouchListener(iuiFaceTouchCallback);
        buddyGPTApplication.setparam("firstLaunch", "true");
        if (buddyGPTApplication.getDialog() != null && buddyGPTApplication.getDialog().isShowing())
            buddyGPTApplication.getDialog().dismiss();
        buddyGPTApplication.setFileCreate(true);
        buddyGPTApplication.setFirstLaunch(true);
        buddyGPTApplication.stopListening(getActivity());

        buddyGPTApplication.notifyObservers("main destroy");
        if (poseTracking != null) poseTracking.stopMovingAndCancelRunnables();
        if (backgroundExecutor != null) backgroundExecutor.shutdownNow();
        downloadingBar.setVisibility(View.GONE);
        // Annule le timeout du spinner si actif
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
        if(cameraProvider != null) cameraProvider.unbindAll();
        buddyGPTApplication.removeObserver(this);
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (handlerCheckPersonDetection != null) handlerCheckPersonDetection.removeCallbacksAndMessages(null);
        if (handlerPauseTime != null) handlerPauseTime.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "--- MainFragment.onResume() ---");

        //  Réactiver l'activity
        if (buddyGPTApplication != null) {
            buddyGPTApplication.setActivityClosed(false);
            Log.i(TAG, "onResume: Activity is ACTIVE again");

            //  Redémarrer TOUS les services (hotword, audio, VAD, etc.)
            Log.i(TAG, "onResume: Calling buddyGPTApplication.resume()");
            buddyGPTApplication.setAppIsListeningToTheQuestion(false);
            buddyGPTApplication.startListeningHotwor(getActivity());
            if (BuddySDK.UI != null) {
                BuddySDK.UI.addFaceTouchListener(iuiFaceTouchCallback);
            } else {
                Log.e(TAG, "BuddySDK.UI is null, cannot add face touch listener");
            }
        }
    }

    /**
     * Navigation vers la page des paramètres avec nettoyage de l'état
     */
    public void btnOpenFragment(String fragment) {
        // Arrêter toutes les activités en cours
        try {
            // Arrêt de l'écoute et du TTS
            buddyGPTApplication.stopTTS();
            buddyGPTApplication.setStartRecording(false);
            buddyGPTApplication.setSpeaking(false);
            buddyGPTApplication.setAppIsListeningToTheQuestion(false);
            isListeningFreeSpeech = false;
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
                handler.removeCallbacks(runnable);
            }
            if (handlerCheckPersonDetection != null) {
                handlerCheckPersonDetection.removeCallbacksAndMessages(null);
                handlerCheckPersonDetection.removeCallbacks(runnableCheckPersonDetection);
            }
            if (handler != null && runnable != null) {
                handler.removeCallbacks(runnable);
                handler.removeCallbacksAndMessages(null);
            }
            if (handlerPauseTime != null) {
                handlerPauseTime.removeCallbacksAndMessages(null);
                handlerPauseTime.removeCallbacks(runnablePauseTime);
            }
            // Reset UI state
            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
            BuddySDK.UI.stopListenAnimation();
            buddyGPTApplication.setLed("Neutral");

            // Reset application state
            buddyGPTApplication.setActivityClosed(true);
            buddyGPTApplication.setStoredResponse("");

            // Masquer les bulles de texte
            if (buddyTexteQstLyt != null) buddyTexteQstLyt.setVisibility(View.INVISIBLE);
            if (buddyTexteRespLyt != null) buddyTexteRespLyt.setVisibility(View.INVISIBLE);
            if (buddyTexteQst != null) buddyTexteQst.setMovementMethod(null);
            if (buddyTexteResp != null) buddyTexteResp.setMovementMethod(null);

            // Rendre visibles les boutons de menu
            if (lytOpenMenuSettings != null) lytOpenMenuSettings.setVisibility(View.VISIBLE);
            if (lytOpenMenuChat != null) lytOpenMenuChat.setVisibility(View.VISIBLE);
            stopListeningFreeSpeech();

            // S'assurer que le traitement audio est arrêté
            if (Boolean.TRUE.equals(buddyGPTApplication.getAppIsListeningToTheQuestion())) {
                buddyGPTApplication.traitementAudio();
            }

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du nettoyage avant navigation", e);
        }

        // Navigation vers Settings uniquement si l'Activity est valide
        if (getActivity() == null || !isAdded()) {
            Log.e(TAG, "Navigation impossible : Activity null ou Fragment détaché");
            return;
        }

        try {
            if(fragment.equalsIgnoreCase("settingsFragment")){
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new SettingsFragment())
                        .commitAllowingStateLoss();

                getActivity().overridePendingTransition(0, 0);
            }
            else if(fragment.equalsIgnoreCase("chatFragment")){
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ChatFragment())
                        .commitAllowingStateLoss();

                getActivity().overridePendingTransition(0, 0);
            }
            // Effectuer la transition

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la navigation vers Settings", e);
        }
    }

    private void showStream(String responseTitle, String response) {
        if (!buddyGPTApplication.isActivityClosed()) {
            if (response != null && !response.trim().isEmpty()) {
                String preview = response.length() > 200 ? response.substring(0, 200) + "…" : response;
                Log.i("FFF", "Response received (stream) -> visage NEUTRAL, len=" + response.length() + " title=" + responseTitle + " text=\"" + preview + "\"");
                Log.i("_NEUTRAL_HUNT", "════════════════════════════════════════════════════════");
                Log.i("_NEUTRAL_HUNT", " setFacialExpression(NEUTRAL) called from: MainFragment.showStream() - STREAMING");
                Log.i("_NEUTRAL_HUNT", " Current active emotion: " + BuddyGPTApplication.currentActiveEmotion);
                StackTraceElement[] st = Thread.currentThread().getStackTrace();
                for (int i = 1; i < Math.min(6, st.length); i++) {
                    Log.i("🔍_NEUTRAL_HUNT", "   └─ [" + i + "] " + st[i].getClassName() + "." + st[i].getMethodName() + ":" + st[i].getLineNumber());
                }
                Log.i("🔍_NEUTRAL_HUNT", "════════════════════════════════════════════════════════");
                // If an emotion is currently active, SKIP switching to NEUTRAL to avoid visual flicker.
                if (BuddyGPTApplication.currentActiveEmotion == null) {
                    BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                    isWaitingForResponse = false;
                } else {
                    Log.i("BBB", "⏭ Skipping setFacialExpression(NEUTRAL) because active emotion: " + BuddyGPTApplication.currentActiveEmotion);
                    // Keep the current emotion displayed during audio playback; no restoration needed.
                    isWaitingForResponse = false;
                }
            } else {
                Log.i("FFF", "Response empty (stream) -> keep THINKING");
            }
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
            Log.i("BBB", "→ runnableCheckPersonDetection EXECUTING (tracking detection logic)");
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
                            if (buddyGPTApplication.getparam("INVALID_TEAMGPT_DEVICE_ID").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
                                Log.i("TAG", "run: notifyObservers INVALID_TEAMGPT_DEVICE_ID 3");
                                buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_DEVICE_ID");
                            }
                            else if (buddyGPTApplication.getparam("INVALID_TEAMGPT_KEY").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")) {
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
                            }else if (buddyGPTApplication.getparam("EnvInProgress").equalsIgnoreCase("true")) {
                                Log.i("TAG", "run: notifyObservers EnvInProgress");
                                if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_EN)) {
                                    buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_en));
                                } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_FR)) {
                                    buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_fr));
                                } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_ES)) {
                                    buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_es));
                                } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_DE)) {
                                    buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_de));
                                } else {
                                    buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                                            .translate(getString(R.string.toast_teamgpt_EnvProgress_en))
                                            .addOnSuccessListener(translatedText -> buddyGPTApplication.showToast(translatedText))
                                            .addOnFailureListener(e -> buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_en)));
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
        getActivity().runOnUiThread(() -> {
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
        buddyGPTApplication.setAlreadyChatting(false);
        buddyGPTApplication.stopListening(getActivity());

        Log.i("FZE", "STT non local flux: startListeningFreeSpeech duration=" + duration + " STT=" + buddyGPTApplication.getparam("STT"));
        Log.d(TAG, " --- startListeningFreeSpeech(" + duration + ") ---");
        Log.d(TAG, " --- startListeningFreeSpeech( STT" + buddyGPTApplication.getparam("STT") + ") ---");

        buddyGPTApplication.startListeningSTTForQuestion(getActivity());

        Log.i(TAG, "startListeningFreeSpeech: after cerence or android");
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
                    if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(ANDROID_STT) || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(CERENCE_STT)|| buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(GOOGLE_STT)|| buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(WHISPER_STT)) {
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
        Log.i("FZE", "STT non local flux: stopListeningFreeSpeech STT=" + buddyGPTApplication.getparam("STT"));
        Log.d(TAG, " --- stopListeningFreeSpeech() ---");
        if (timerEcoute != null) timerEcoute.cancel();
        buddyGPTApplication.stopListening(getActivity());
    }

    private void startCycle() {
        Log.e(TAG, "startCycle  after handler ");
        isListeningFreeSpeech = true;
        buddyGPTApplication.setMessageError(false);
        buddyGPTApplication.setAlreadyChatting(false);
        buddyGPTApplication.startListeningSTTForQuestion(getActivity());

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
                    if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(ANDROID_STT) || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(CERENCE_STT)|| buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(GOOGLE_STT)|| buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(WHISPER_STT)) {
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
        Log.i("BBB", "🔊 speak() CALLED with type='" + type + "' texte length=" + (texte == null ? "null" : texte.length()));
        isSpeaking = true;
        getActivity().runOnUiThread(() -> {
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            buddyTexteRespLyt.setTranslationY(0);
            if (responseTimeout != null) responseTimeout.cancel();

            if (!buddyGPTApplication.isActivityClosed()) {
                    if (type.equals("nothealysa") || type.equals("storedResponse")) {
                    if (texte != null && !texte.trim().isEmpty()) {
                        String preview = texte.length() > 200 ? texte.substring(0, 200) + "…" : texte;
                        Log.i("FFF", "Response received (speak) -> visage NEUTRAL, len=" + texte.length() + " text=\"" + preview + "\"");
                        // Preserve the current facial emotion during TTS when emotion switching is enabled in settings.
                        if (settingClass == null || !"true".equals(settingClass.getSwitchEmotion())) {
                            Log.i("BBB", " speak() is resetting face to NEUTRAL (SwitchEmotion=" + (settingClass == null ? "null" : settingClass.getSwitchEmotion()) + ")");
                            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                            Log.i("BBB", "✓ Face reset to NEUTRAL in speak()");
                        } else {
                            Log.i("BBB", "✓ speak() preserving emotion - SwitchEmotion=true");
                        }
                        isWaitingForResponse = false;
                    } else {
                        Log.i("FFF", "Response empty (speak) -> keep THINKING");
                    }
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


                    Log.i("BBB", "→ CALLING speakTTS for type='" + type + "' with texte length=" + texte.length());
                    buddyGPTApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL, type);
                    Log.i("BBB", "← speakTTS returned for type='" + type + "'");
                } else if (type.equals("timeOutExpired")) {
                    Log.i("BBB", "→ CALLING speakTTS for timeOutExpired");
                    buddyGPTApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL, type);
                    Log.i("BBB", "← speakTTS returned for timeOutExpired");
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
                    } else if (buddyGPTApplication.getparam("EnvInProgress").equalsIgnoreCase("true")) {
                        Log.i("TAG", "run: notifyObservers EnvInProgress");
                        if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_EN)) {
                            buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_en));
                        } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_FR)) {
                            buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_fr));
                        } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_ES)) {
                            buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_es));
                        } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_DE)) {
                            buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_de));
                        } else {
                            buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                                    .translate(getString(R.string.toast_teamgpt_EnvProgress_en))
                                    .addOnSuccessListener(translatedText -> buddyGPTApplication.showToast(translatedText))
                                    .addOnFailureListener(e -> buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_en)));
                        }
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
                    Log.i(TAG, "update: STTHotword_success else ");
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
                String[] parts = message.split(";SPLIT;");
                Log.i(TAG, "update: PARTS 1 "+parts[1]);
                if (parts.length > 1 && !parts[1].equals("NONE")) {
                    Log.i(TAG, "Result: " + parts[1]);
                    getActivity().runOnUiThread(() -> {
                        stopListeningFreeSpeech();
                        buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(true);
                        SystemClock.sleep(200);
                        buddyGPTApplication.setAppIsListeningToTheQuestion(false);
                        String detectedSTTMessage = parts[1].replaceAll("' ", "'");
                        Log.i("HOU", "HOU run: " + detectedSTTMessage);
                        if (!buddyGPTApplication.isActivityClosed()) {
                            buddyGPTApplication.setQuestionNumber(buddyGPTApplication.getQuestionNumber() + 1);
                            isWaitingForResponse = true;
                            buddyGPTApplication.setAlreadyGetAnswer(false);
                            BuddySDK.UI.setFacialExpression(FacialExpression.THINKING, 1);
                            handler.postDelayed(() -> {
                                if (!buddyGPTApplication.isAlreadyGetAnswer()) {
                                    BuddySDK.UI.setFacialExpression(FacialExpression.THINKING, 1);
                                }
                            }, 250);
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
                                                            if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(ANDROID_STT)
                                                                    || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase(CERENCE_STT)
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
                }else {
                   getActivity().runOnUiThread(() -> {
                       stopListeningFreeSpeech();
                       Log.i("FFF", "STT non local: envoi audio -> visage THINKING");
                       isWaitingForResponse = true;
                       buddyGPTApplication.setAlreadyGetAnswer(false);
                       BuddySDK.UI.setFacialExpression(FacialExpression.THINKING, 1);
                       handler.postDelayed(() -> {
                           if (!buddyGPTApplication.isAlreadyGetAnswer()) {
                               BuddySDK.UI.setFacialExpression(FacialExpression.THINKING, 1);
                           }
                       }, 250);
                       BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                       buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(true);
                       SystemClock.sleep(200);
                       buddyGPTApplication.setAppIsListeningToTheQuestion(false);
                       logLargeString(TAG, "update: message google or whisper : "+message.split(";SPLIT;")[2]);
                       Log.i("FZE", "STT non local flux: audio capturé, envoi POST au serveur");
                       if (buddyGPTApplication.getResponseFromTeamGPT() != null)
                           buddyGPTApplication.getResponseFromTeamGPT().reset();
                       if (buddyGPTApplication.getResponseFromTeamGPT() == null)
                           buddyGPTApplication.setResponseFromTeamGPT(new ResponseFromTeamGPT(buddyGPTApplication));
                       buddyGPTApplication.getResponseFromTeamGPT().sendPutRequestStream(null,message.split(";SPLIT;")[2]);

                   });
                }

            }
            if(message.contains("AUDIO_TEXT_INPUT;")){
                String[] parts = message.split(";SPLIT;");
                Log.i(TAG, "update: AUDIO_TEXT_INPUT PARTS 1 "+parts[1]);
                if (!parts[1].equals("NONE")) {
                    Log.i(TAG, "Result: " + parts[1]);
                    getActivity().runOnUiThread(() -> {
                        buddyGPTApplication.setQuestionNumber(buddyGPTApplication.getQuestionNumber() + 1);
                        BuddySDK.UI.setFacialExpression(FacialExpression.THINKING, 1);
                        isWaitingForResponse = true;
                        stopListeningFreeSpeech();
                        buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(true);
                        SystemClock.sleep(200);
                        buddyGPTApplication.setAppIsListeningToTheQuestion(false);
                        String detectedSTTMessage = parts[1].replaceAll("' ", "'");
                        Log.i("HOU", "HOU run: " + detectedSTTMessage);
                        if (!buddyGPTApplication.isActivityClosed()) {
                            buddyGPTApplication.setQuestionNumber(buddyGPTApplication.getQuestionNumber() + 1);
                            BuddySDK.UI.setFacialExpression(FacialExpression.THINKING, 1);
                        }
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
                    });
                }
            }

            if (message.contains("TTS_success")) {
                getActivity().runOnUiThread(() -> {
                    Log.e(TAG, " TTS_success");
                    BuddyGPTApplication.logAndResetLabialExpression("MainFragment.update() - TTS_success handler (line 2062)");
                    buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                    buddyTexteQstLyt.setVisibility(View.INVISIBLE);
                    buddyTexteRespLyt.setVisibility(View.INVISIBLE);
                    buddyTexteQst.setMovementMethod(null);
                    buddyTexteResp.setMovementMethod(null);
                    lytOpenMenuSettings.setVisibility(View.VISIBLE);
                    lytOpenMenuChat.setVisibility(View.VISIBLE);
                    isSpeaking = false;
                    // If a neutral emotion was deferred while speaking, apply it now
                    if (pendingNeutralEmotion) {
                        Log.i("BBB", "TTS_success: applying deferred neutral emotion now");
                        Log.i("_NEUTRAL_HUNT", "═══ CALLING setAnimation(BuddyFace_Neutral) from Emotion_Change handler ═══");
                        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                        for (int i = 1; i < Math.min(5, stackTrace.length); i++) {
                            Log.i("_NEUTRAL_HUNT", "  [" + i + "] " + stackTrace[i].getClassName() + "." + stackTrace[i].getMethodName() + ":" + stackTrace[i].getLineNumber());
                        }
                        buddyGPTApplication.setAnimation("BuddyFace_Neutral");
                        pendingNeutralEmotion = false;
                    }
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
                String emo = message.split(";SPLIT;")[1];
                Log.i("BBB", "Emotion_Change received: " + emo + " | isSpeaking=" + isSpeaking);
                // If server asks to set neutral while we're speaking, defer it until
                // TTS finishes so the emotion stays visible during playback.
                if ("BuddyFace_Neutral".equalsIgnoreCase(emo) && isSpeaking) {
                    pendingNeutralEmotion = true;
                    Log.i("BBB", "Deferred BuddyFace_Neutral until end of speech");
                } else {
                    if (pendingNeutralEmotion) {
                        Log.i("BBB", "Clearing pending neutral (received non-neutral while pending): " + emo);
                    }
                    pendingNeutralEmotion = false;
                    Log.i("BBB", "Applying emotion immediately: " + emo);
                    buddyGPTApplication.setAnimation(emo);
                    Log.i("BBB", "Animation request sent for: " + emo);
                }
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
                    Log.i("_NEUTRAL_HUNT", "════════════════════════════════════════════════════════");
                    Log.i("_NEUTRAL_HUNT", " setFacialExpression(NEUTRAL) called from: MainFragment.update() - 'end of cycle' message");
                    Log.i("_NEUTRAL_HUNT", " Current active emotion: " + BuddyGPTApplication.currentActiveEmotion);
                    StackTraceElement[] st = Thread.currentThread().getStackTrace();
                    for (int i = 1; i < Math.min(6, st.length); i++) {
                        Log.i("_NEUTRAL_HUNT", "   └─ [" + i + "] " + st[i].getClassName() + "." + st[i].getMethodName() + ":" + st[i].getLineNumber());
                    }
                    Log.i("_NEUTRAL_HUNT", "════════════════════════════════════════════════════════");
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

    public void initTeamGPTSettings() {

        //  Charger les paramètres de base depuis config.json ─────
        buddyGPTApplication.setparam("TeamGPT_Base_url",
                buddyGPTApplication.getParamFromFile("TeamGPT_Base_url", configFile));

        buddyGPTApplication.setparam("TeamGPT_ApiEndpoint_Env",
                buddyGPTApplication.getParamFromFile("TeamGPT_ApiEndpoint_Env", configFile));
        buddyGPTApplication.setparam("TeamGPT_ApiEndpoint_Params",
                buddyGPTApplication.getParamFromFile("TeamGPT_ApiEndpoint_Params", configFile));
        buddyGPTApplication.setparam("TeamGPT_ApiEndpoint_Response",
                buddyGPTApplication.getParamFromFile("TeamGPT_ApiEndpoint_Response", configFile));

        String idDevice = buddyGPTApplication.getParamFromFile("TeamGPT_ID_Device", configFile);
        buddyGPTApplication.setparam("TeamGPT_ID_Device", idDevice.isEmpty() ? "0" : idDevice);

        Log.i(TAG, "initTeamGPTSettings: TeamGPT_ID_Device: " + idDevice);


        //  Préparer ResponseFromTeamGPT ─────
        ResponseFromTeamGPT rft = buddyGPTApplication.getResponseFromTeamGPT();
        if (rft == null) {
            rft = new ResponseFromTeamGPT(buddyGPTApplication);
            buddyGPTApplication.setResponseFromTeamGPT(rft);
        }

        String savedKey = buddyGPTApplication.getparam("TeamGPT_Key");
        String fileKey  = buddyGPTApplication.getParamFromFile("TeamGPT_Key", configFile);


        //  CASE 1 : pas de clé en RAM ⇒ essayer la clé du fichier ─────
        if (savedKey.isEmpty()) {

            // cacher le spinner au début
            lytSpinner = view.findViewById(R.id.lyt_spinner);
            if (lytSpinner != null) lytSpinner.setVisibility(View.GONE);

            buddyGPTApplication.setparam("TeamGPT_Key", fileKey);

            if (!fileKey.isEmpty()) {

                // On appelle getParameters avec la clé venant du fichier
                callGetParameters(rft);

            } else {
                // ─────────── Clé inexistante nulle part ⇒ afficher toast ───────────
                showMissingKeyToast();
            }

        }

        // CASE 2 : clé déjà dans SharedPreferences ─────
        else {
            callGetParameters(rft);
        }


        //  First Launch : recharger header + entete ─────
        if (buddyGPTApplication.getparam("firstLaunch").equals("true")) {

            if (buddyGPTApplication.getparam(header).isEmpty()) {
                buddyGPTApplication.setparam(header,
                        buddyGPTApplication.getParamFromFile(header, configFile));
            }

            if (buddyGPTApplication.getparam(entete).isEmpty()) {
                buddyGPTApplication.setparam(entete,
                        buddyGPTApplication.getParamFromFile(entete, configFile));
            }

            buddyGPTApplication.setparam("firstLaunch", "false");
        }
    }

    private void callGetParameters(ResponseFromTeamGPT rft) {

        // Stop timeout si actif
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }

        lytSpinner.setVisibility(View.VISIBLE);

        rft.getEnvironnement(new ResponseCallback() {

            @Override
            public void onSuccess() {
                getActivity().runOnUiThread(() -> {
                    Log.i(TAG, "getEnvironnement SUCCESS");

                    removeTimeout();
                    lytSpinner.setVisibility(View.GONE);
                });
            }

            @Override
            public void onFailure() {
                getActivity().runOnUiThread(() -> {
                    Log.e(TAG, "getEnvironnement FAILURE");

                    removeTimeout();
                    lytSpinner.setVisibility(View.VISIBLE);

                    // Notifier l’application que l’environnement est en erreur
                    buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
                });
            }
        });
    }
    private void removeTimeout() {
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
    }
    private void showMissingKeyToast() {

        String lang = buddyGPTApplication.getLangue().getNom();

        if (lang.equals("Anglais")) {
            buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_en));
            return;
        }

        if (lang.equals("Français")) {
            buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_fr));
            return;
        }

        // langue autre → traduction automatique
        buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                .translate(getString(R.string.toast_teamgpt_key_indispo_en))
                .addOnSuccessListener(text -> buddyGPTApplication.showToast(text))
                .addOnFailureListener(e -> buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_en)));
    }

    private void checkSpeakingAndSetNeutral() {
        Log.i("🔍_NEUTRAL_HUNT", "→ checkSpeakingAndSetNeutral() called, isSpeaking=" + isSpeaking);
        handler.postDelayed(() -> {
            if(isSpeaking) checkSpeakingAndSetNeutral();
            else {
                Log.i("🔍_NEUTRAL_HUNT", "════════════════════════════════════════════════════════");
                Log.i("🔍_NEUTRAL_HUNT", "🔍 setFacialExpression(NEUTRAL) called from: MainFragment.checkSpeakingAndSetNeutral()");
                Log.i("🔍_NEUTRAL_HUNT", "🔍 Current active emotion: " + BuddyGPTApplication.currentActiveEmotion);
                Log.i("🔍_NEUTRAL_HUNT", "🔍 isSpeaking=false, so resetting face");
                StackTraceElement[] st = Thread.currentThread().getStackTrace();
                for (int i = 1; i < Math.min(6, st.length); i++) {
                    Log.i("🔍_NEUTRAL_HUNT", "   └─ [" + i + "] " + st[i].getClassName() + "." + st[i].getMethodName() + ":" + st[i].getLineNumber());
                }
                Log.i("🔍_NEUTRAL_HUNT", "════════════════════════════════════════════════════════");
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
                                    .addOnSuccessListener(translatedText -> buddyGPTApplication.showToast(translatedText))
                                    .addOnFailureListener(e -> buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_key_indispo_en)));
                        }
                    } else if (buddyGPTApplication.getparam("EnvInProgress").equalsIgnoreCase("true")) {
                        Log.i("TAG", "run: notifyObservers EnvInProgress");
                        if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_EN)) {
                            buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_en));
                        } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_FR)) {
                            buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_fr));
                        } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_ES)) {
                            buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_es));
                        } else if (buddyGPTApplication.getLangue().getNom().equals(LANGUE_DE)) {
                            buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_de));
                        } else {
                            buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                                    .translate(getString(R.string.toast_teamgpt_EnvProgress_en))
                                    .addOnSuccessListener(translatedText -> buddyGPTApplication.showToast(translatedText))
                                    .addOnFailureListener(e -> buddyGPTApplication.showToast(getString(R.string.toast_teamgpt_EnvProgress_en)));
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
    private void logLargeString(String TAG,String str) {
        if(str.length() > 3000) {
            Log.i(TAG, str.substring(0, 3000));
            logLargeString(TAG,str.substring(3000));
        } else {
            Log.i(TAG, str);
        }
    }

}