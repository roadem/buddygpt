package com.robotique.aevaweb.buddygpt.activities;

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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bfr.buddy.ui.shared.FaceTouchData;
import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.GazePosition;
import com.bfr.buddy.ui.shared.IUIFaceTouchCallback;
import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddy.utils.events.EventItem;
import com.bfr.buddy.utils.values.FloatingWidgetVisibility;
import com.bfr.buddy.vision.shared.IVisionRsp;
import com.bfr.buddysdk.BuddyCompatActivity;
import com.bfr.buddysdk.BuddySDK;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.exoplayer2.ui.PlayerView;
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
import com.robotique.aevaweb.buddygpt.utilis.IBehaviourCallBack;
import com.robotique.aevaweb.buddygpt.utilis.WifiBroadcastReceiver;
import com.robotique.aevaweb.buddygpt.utilis.BIPlayer;
import com.robotique.aevaweb.buddygpt.utilis.CustomToast;
import com.robotique.aevaweb.buddygpt.utilis.IMLKitDownloadCallback;
import com.robotique.aevaweb.buddygpt.utilis.ITTSCallbacks;
import com.robotique.aevaweb.buddygpt.utilis.tracking.MainViewModel;
import com.robotique.aevaweb.buddygpt.utilis.tracking.OverlayView;
import com.robotique.aevaweb.buddygpt.utilis.tracking.PoseLandmarkerHelper;
import com.robotique.aevaweb.buddygpt.utilis.tracking.PoseTracking;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BuddyCompatActivity implements IDBObserver {

    private static final String TAG = "BuddyGPT_MainActivity";
    private static final String TAG_TRACKING = "BuddyGPT_TRACKING_INFO";
    private static final String TAG_TRACKING_DEBUG = "BuddyGPT_TRACKING_DEBUG";
    private static final String[] REQUESTED_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE

    };
    private static final int PERMISSION_REQ_ID = 22;
    private BuddyGPTApplication buddyGPTApplication;
    private View decorView;

    //views
    private RelativeLayout buddy_texte_qst_lyt;
    private RelativeLayout buddy_texte_resp_lyt;
    private RelativeLayout lyt_open_menu_settings;
    private RelativeLayout lyt_open_menu_chat;
    private RelativeLayout view_face;
    private RelativeLayout launch_view;
    private RelativeLayout reGroup;
    private RelativeLayout preViewViewLyt;
    private RelativeLayout photo_timer_bg_rlyt;
    private RelativeLayout photo_timer_rlyt;
    private FrameLayout preview_container;
    private OverlayView overlay;
    private PreviewView previewView;
    private PreviewView previewViewphoto;
    private TextView photo_timer_txtView;
    private TextView buddy_texte_qst;
    private TextView buddy_texte_resp;
    private ImageView noNetwork;
    private ProgressBar downloadingBar;
    private ImageView photo_timer_imgview;
    private ImageView bi_imageView;
    private PlayerView bi_videoView;


    private double TRACKING_WELCOME_TEMPERATURE;

    private long lastVisibleTime = 0L;          // Track the time when person was last seen
    private long lastVisibleTime_saved = 0L;    // Track the time when person was last seen and do not reset it when re-track (useful for invitation check)
    private long firstVisibleTime = 0L;         // Track the time when person started being visible
    private long visibleDuration = 0L;          // How long a person remained visible
    private long lastLookingAtCameraTime = 0L;  // Track the time when the person last looked at the camera
    private long totalTimeLookingAtCamera = 0L; // Total time spent looking at the camera
    private long startLookingAtCameraTime = 0L; // Track the start time of the current interval when the person is looking directly at the camera

    private Float[] res = {(float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0,(float) 0, (float) 0, (float) 0};
    private float initLang=190F;
    private float degx,degy,x0,x2,x5,y0,y5,y2,lang,dLeft,eog,Eod, dRight;

    private int path= R.string.path;
    private int pathLog=R.string.pathConfig;
    private int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private int TRACKING_DELAY_NO_WATCH;
    private int TRACKING_DELAY_NO_TRACK;
    private int TRACKING_DELAY_START_LISTEN;
    private int TRACKING_DELAY_STOP_LISTEN;
    private int TRACKING_REGARD_CENTER;
    private int TRACKING_DELAY_WELCOME;
    private int TRACKING_DURATION_WELCOME;
    private int TRACKING_WELCOME_MAX_TOKEN;
    private int TRACKING_TIMEOUT;

    private String TRACKING_WELCOME_FR;
    private String TRACKING_WELCOME_EN;
    private String TRACKING_WELCOME_PROMPT_FR;
    private String TRACKING_WELCOME_PROMPT_EN;
    private String TRACKING_WELCOME_MODEL;
    private String TRACKING_WATCH;
    private String directionRegardNez= "";
    private String langueFr = "Français";
    private String langueEn = "Anglais";
    private String langueEs = "Espagnol";
    private String langueDe = "Allemand";
    private String header ="header";
    private String entete ="entete";
    private String cabecera ="Cabecera";
    private String kopfzeile ="Kopfzeile";
    private String openAIKey = "openAI_API_Key";
    private String info_toast = "";
    private String gptResponse;

    private boolean onSdkReadyIsAlreadyCalledOnce = false;
    private boolean isListeningFreeSpeech = false;
    private Boolean mlKitIsDownloading = false;
    private boolean english_is_downloaded = false;
    private boolean french_is_downloaded = false;
    private boolean languageToEnglish_is_downloaded = false;
    private boolean isCMDLangue = false;
    private boolean isSpeaking = false;
    private Boolean gptSend=false;
    private boolean isFirstLaunch = true; // Used to init TeamGPT params only once
    private boolean regarde_camera=false;
    private boolean direction=false;
    private boolean deFace=false;
    private boolean isPersonDetected = false;
    private boolean wasPersonDetected = false;
    private boolean personIsVisible = false;
    private boolean sendInvitationPending = false;
    private boolean isProcessingReTrack = false;
    private boolean isTrackingAlreadyInitialised = false;
    private boolean isReTrack = false;
    private boolean isFirstInvitaion = false;

    private CountDownTimer timerEcoute;
    private CountDownTimer responseTimeout;
    private CountDownTimer timerDownloading;
    private CountDownTimer timerPhoto;

    private static final Random random = new Random();
    private WifiBroadcastReceiver wifiBroadCastReceiver = new WifiBroadcastReceiver();
    private ArrayList<Replica> listRep=new ArrayList();
    private AudioManager amanager;
    private Dialog dialog;

    private Setting settingClass;
    //private Commande commande;
    private PoseTracking poseTracking;
    private ExecutorService backgroundExecutor;
    private CameraSelector cameraSelector;
    private MainViewModel viewModel;
    private PoseLandmarkerHelper poseLandmarkerHelper;
    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private ImageAnalysis imageAnalyzer;
    private Camera camera;
    private AnimationDrawable animationTimerPhoto;
    private ImageCapture imageCapture;
    private String initOrMajOrNone="";

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
                    case "languageToEnglish":
                        languageToEnglish_is_downloaded = true;
                        break;
                }
                if (english_is_downloaded && french_is_downloaded && languageToEnglish_is_downloaded) {

                    handlerProgressBar.removeCallbacksAndMessages(null);
                    handlerProgressBar.removeCallbacks(runnableProgressBar);
                    launch_view.setVisibility(View.INVISIBLE);
                    switch (initOrMajOrNone) {
                        case "INIT":
                            if (buddyGPTApplication.getCurrentLanguage().equals("en")) {
                                info_toast = getString(R.string.toast_config_file_init_en);
                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                            }
                            else if (buddyGPTApplication.getCurrentLanguage().equals("fr")){
                                info_toast = getString(R.string.toast_config_file_init_fr);
                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                            }
                            else if (buddyGPTApplication.getCurrentLanguage().equals("de")) {
                                info_toast = getString(R.string.toast_config_file_init_de);
                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                            }
                            else if (buddyGPTApplication.getCurrentLanguage().equals("es")) {
                                info_toast = getString(R.string.toast_config_file_init_es);
                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                            }
                            else{
                                buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                                        .translate(getString(R.string.toast_config_file_init_en))
                                        .addOnSuccessListener(new OnSuccessListener<String>() {
                                            @Override
                                            public void onSuccess(String translatedText) {
                                                info_toast = translatedText;
                                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                info_toast = getString(R.string.toast_config_file_init_en);
                                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                                            }
                                        });
                            }

                            break;
                        case "MAJ":
                            //traduire l'info du configFile :
                            if (buddyGPTApplication.getCurrentLanguage().equals("en")) {
                                info_toast = getString(R.string.toast_config_file_maj_en);
                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                            }
                            else if (buddyGPTApplication.getCurrentLanguage().equals("fr")){
                                info_toast = getString(R.string.toast_config_file_maj_fr);
                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                            }
                            else if (buddyGPTApplication.getCurrentLanguage().equals("de")) {
                                info_toast = getString(R.string.toast_config_file_maj_de);
                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                            }
                            else if (buddyGPTApplication.getCurrentLanguage().equals("es")) {
                                info_toast = getString(R.string.toast_config_file_maj_es);
                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                            }
                            else{
                                buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                                        .translate(getString(R.string.toast_config_file_maj_en))
                                        .addOnSuccessListener(new OnSuccessListener<String>() {
                                            @Override
                                            public void onSuccess(String translatedText) {
                                                info_toast = translatedText;
                                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                info_toast = getString(R.string.toast_config_file_maj_en);
                                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                                            }
                                        });
                            }

                            break;
                        case "NONE":
                            break;
                    }
                    mlKitIsDownloading = false;
//                    if (isCMDLangue){
//                        isCMDLangue = false;
//                        commande.translate("CMD_LANGUE", new Commande.ITranslationCallback() {
//                            @Override
//                            public void onTranslated(String translatedText) {
//                                String verifyMessage = commande.verifyCmdMessages(translatedText);
//                                if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
//                                    buddyGPTApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
//                                }
//                            }
//                        });
//                    }
//                    else
                        if(!Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation"))){
                        buddyGPTApplication.startListeningHotwor(MainActivity.this);
                        reGroup.setTranslationY(1000);
                    }
                    else if(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation"))){
                        if( !Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Auto_Listen"))){
                            buddyGPTApplication.startListeningHotwor(MainActivity.this);
                        }
                        isReTrack = false;
                        //initTracking();
                    }
                }
            }
            else{
                mlKitIsDownloading = true;
                french_is_downloaded = false;
                english_is_downloaded = false;
                languageToEnglish_is_downloaded =false;
                buddyGPTApplication.downloadModel(imlKitDownloadCallback,new Gson().fromJson(buddyGPTApplication.getparam(settingClass.getLangue()), Langue.class).getLanguageCode().split("-")[0].trim());
                handlerProgressBar.postDelayed(runnableProgressBar,500);
            }
        }
    };
    private Handler handlerProgressBar = new Handler(Looper.getMainLooper());
    private Runnable runnableProgressBar = new Runnable() {
        @Override
        public void run() {
            buddyGPTApplication.stopTTS();
            stopListeningFreeSpeech();
            try {
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                BuddySDK.UI.stopListenAnimation();
            } catch (Exception e) {
                Log.e(TAG, "BuddySDK Exception  " + e);
            }
            launch_view.setVisibility(View.VISIBLE);
            timerDownloading = new CountDownTimer((long) Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds", "BuddyGPT.properties")) * 1000, 1000) {
                @Override
                public void onTick(long l) {
                    Log.e(TAG,"onTick mlKitIsDownloading");
                }
                @Override
                public void onFinish() {
                    Log.e(TAG,"on Finish mlKitIsDownloading");
                    if (mlKitIsDownloading){
                        if (buddyGPTApplication.getLangue().getNom().equals(langueEn)) {
                            Toast.makeText(MainActivity.this, R.string.mlkit_model_is_downloading_en, Toast.LENGTH_SHORT).show();
                        }
                        else if (buddyGPTApplication.getLangue().getNom().equals(langueFr)){
                            Toast.makeText(MainActivity.this, R.string.mlkit_model_is_downloading_fr, Toast.LENGTH_SHORT).show();
                        }
                        else if (buddyGPTApplication.getLangue().getNom().equals(langueEs)){
                            Toast.makeText(MainActivity.this, R.string.mlkit_model_is_downloading_es, Toast.LENGTH_SHORT).show();
                        }
                        else if (buddyGPTApplication.getLangue().getNom().equals(langueDe)){
                            Toast.makeText(MainActivity.this, R.string.mlkit_model_is_downloading_de, Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Toast.makeText(MainActivity.this, R.string.mlkit_model_is_downloading_en, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            };
            timerDownloading.start();
        }
    };
    private Handler handlerForSensor;
    private Runnable runnableForSensor;
    private Handler handlerTTSError = new Handler();
    private Runnable runnableTTSError;
    private Handler handlerPauseTime = new Handler();
    private Runnable runnablePauseTime;
    private Handler handler = new Handler();
    private Runnable runnable;
    private boolean isGetParametersCalled=false;

    /**
     * ------------------ App LifeCycle ---------------------
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG," --- onCreate() ---");
        buddyGPTApplication = (BuddyGPTApplication) getApplicationContext();
        buddyGPTApplication.setInitSharedpreferences(true);
        buddyGPTApplication.hideSystemUI(this);
        decorView=getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if(visibility==0){
                    decorView.setSystemUiVisibility(buddyGPTApplication.hideSystemUI(MainActivity.this));
                }
            }
        });

        //init views
        buddy_texte_qst = findViewById( R.id.buddy_texte_qst );
        buddy_texte_qst_lyt = findViewById( R.id.buddy_texte_qst_lyt );
        buddy_texte_resp = findViewById( R.id.buddy_texte_resp );
        buddy_texte_resp_lyt = findViewById( R.id.buddy_texte_resp_lyt );
        lyt_open_menu_settings = findViewById( R.id.lyt_open_menu_settings );
        lyt_open_menu_chat = findViewById( R.id.lyt_open_menu_chat );
        view_face = findViewById(R.id.view_face);
        launch_view = findViewById(R.id.launch_view);
        noNetwork = findViewById(R.id.noNetwork);
        bi_videoView = findViewById(R.id.bi_videoView);
        bi_imageView = findViewById(R.id.bi_imageView);
        downloadingBar = findViewById(R.id.progressBar_MLKitDownload);
        preview_container = findViewById(R.id.preview_container);
        reGroup = findViewById(R.id.reGroup);
        overlay = findViewById(R.id.overlay);
        previewView = findViewById(R.id.view_finder);
//        photo_timer_bg_rlyt= findViewById(R.id.photo_timer_bg_rlyt);
//        photo_timer_rlyt = findViewById(R.id.photo_timer_rlyt);
//        photo_timer_imgview = findViewById(R.id.photo_timer_imgview);
//        photo_timer_txtView = findViewById(R.id.photo_timer_txtView);

        amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);

        Intent myIntent = getIntent();
        isFirstLaunch = true;
        if (myIntent != null) {
            if (myIntent.hasExtra("fromSettings")) {
                String fromSettings = myIntent.getStringExtra("fromSettings");
                if (fromSettings != null && fromSettings.equals("true")) {
                    isFirstLaunch= false;
                    Log.i(TAG_TRACKING,"is back from Settings");
                }
            }
            else if(myIntent.hasExtra("fromChatWindow")){
                String fromChatWindow = myIntent.getStringExtra("fromChatWindow");
                if (fromChatWindow != null && fromChatWindow.equals("true")) {
                    isFirstLaunch= false;
                    Log.i(TAG_TRACKING,"is back from ChatWindow");
                }
            }
        }
        else {
            buddyGPTApplication.setSpeaking(false);
            buddyGPTApplication.setNotYet(true);
           buddyGPTApplication.setActivityClosed(false);
            buddyGPTApplication.setStartRecording(false);
            buddyGPTApplication.setUsingEmotions(false);
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
            buddyGPTApplication.setStop_TTS_ReadSpeaker(false);
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
            Log.i(TAG_TRACKING,"First launch of application");
        }

        /**
         * init animated drawables for timer
         */
        animationTimerPhoto = new AnimationDrawable();
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0001), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0002), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0003), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0004), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0005), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0006), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0007), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0008), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0009), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0010), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0011), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0012), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0013), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0014), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0015), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0016), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0017), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0018), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0019), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0020), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0021), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0022), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0023), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0024), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0025), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0026), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0027), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0028), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0029), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0030), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0031), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0032), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0033), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0034), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0035), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0036), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0037), 1000/37);
        buddyGPTApplication.hideSystemUI(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG," --- onResume() ---");

        buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        buddyGPTApplication.hideSystemUI(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG," --- onPause() ---");
        if (responseTimeout!=null) responseTimeout.cancel();
        if(handlerTTSError!=null && runnableTTSError!=null){
            handlerTTSError.removeCallbacks(runnableTTSError);
            handlerTTSError.removeCallbacksAndMessages(null);
        }
        if(buddyGPTApplication.getResponseFromTeamGPT()!=null)
            buddyGPTApplication.getResponseFromTeamGPT().reset();
        if (handlerForSensor != null && runnableForSensor != null) {

            handlerForSensor.removeCallbacksAndMessages(null);
            handlerForSensor.removeCallbacks(runnableForSensor);

        }
        if(handler!=null && runnable!=null){
            handler.removeCallbacks(runnable);
            handler.removeCallbacksAndMessages(null);
        }
        onSdkReadyIsAlreadyCalledOnce = false;
        isListeningFreeSpeech = false;
        listRep=new ArrayList();
        gptSend=false;
        buddyGPTApplication.removeObserver(this);
        buddyGPTApplication.stopTTS();
        buddyGPTApplication.setActivityClosed(true);
        stopListeningFreeSpeech();
        CustomToast.getInstance().hideToast();
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
            BuddySDK.UI.removeFaceTouchListener(iuiFaceTouchCallback);
        }
        catch (Exception e){
            Log.e(TAG,"BuddySDK Exception  "+e);
        }
        if(cameraProvider != null) cameraProvider.unbindAll();
       // isFirstLaunch= false;
    }

    @Override
    protected void onDestroy() {

        Log.d(TAG," --- onDestroy() ---");
        //closeImage();
        try {
            unregisterReceiver(wifiBroadCastReceiver);
        }catch(IllegalArgumentException e) {
            Log.i(TAG,"---unregisterReceiver wifiBroadcast:: IllegalArgumentException---"+e.getMessage());
        }
        buddyGPTApplication.setparam("firstLaunch","true");
        if(buddyGPTApplication.getDialog() != null && buddyGPTApplication.getDialog().isShowing()) buddyGPTApplication.getDialog().dismiss();
        buddyGPTApplication.setFileCreate(true);
        buddyGPTApplication.notifyObservers("main destroy");
        handlerCheckPersonDetection.removeCallbacks(runnableCheckPersonDetection);
        handlerCheckPersonDetection.removeCallbacksAndMessages(null);
        if(poseTracking != null) poseTracking.stopMovingAndCancelRunnables();
        if(backgroundExecutor != null) backgroundExecutor.shutdownNow();
//        try{
//            if(!BuddySDK.Actuators.getLeftWheelStatus().toUpperCase().contains("DISABLE") || !BuddySDK.Actuators.getRightWheelStatus().toUpperCase().contains("DISABLE")) {
//                BuddySDK.USB.enableWheels(false, iUsbCommadRspTracking);
//            }
//            if(!BuddySDK.Actuators.getYesStatus().toUpperCase().contains("DISABLE")) {
//                BuddySDK.USB.enableYesMove(false, iUsbCommadRspTracking);
//            }
//            if(!BuddySDK.Actuators.getNoStatus().toUpperCase().contains("DISABLE")) {
//                BuddySDK.USB.enableNoMove(false, iUsbCommadRspTracking);
//            }
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
        super.onDestroy();
    }

    /**
     * ------------------ Register to the SDK callbacks ---------------------
     */

    public void onSDKReady() {
        Log.w(TAG, "onSDKReady");
        if(!onSdkReadyIsAlreadyCalledOnce){
            //initialisation du visage de Buddy
            BuddySDK.UI.setFaceEnergy(1.0f);
            BuddySDK.UI.setFacePositivity(1.0f);
            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
            BuddySDK.UI.lookAt(GazePosition.CENTER, true);
            BuddySDK.UI.stopListenAnimation();
            BuddySDK.UI.setViewAsFace(view_face);
            BuddySDK.UI.setMenuWidgetVisibility(FloatingWidgetVisibility.ALWAYS);
            BuddySDK.UI.setCloseWidgetVisibility(FloatingWidgetVisibility.ALWAYS);

            //Désactiver le trigger Companion de la bouche et de OK BUDDY
            BuddySDK.Companion.raiseEvent("disableOkBuddy");
            BuddySDK.Companion.raiseEvent("disableOnMouth");


            BuddySDK.Vision.stopCamera(0, new IVisionRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    Log.i(TAG_TRACKING, "stopCamera(0) onSuccess : " + s);
                }
                @Override
                public void onFailed(String s) throws RemoteException {
                    Log.e(TAG_TRACKING, "stopCamera(0) onFailed : " + s);
                }
            });

            if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[3], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[4], PERMISSION_REQ_ID)

            ){
                try {
                    init();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        onSdkReadyIsAlreadyCalledOnce = true;
    }

    public void onEvent(EventItem iEvent){
        Log.w(TAG, "onEvent : "+iEvent.toString());
    }

//    private final IUsbCommadRsp iUsbCommadRspBI = new IUsbCommadRsp.Stub(){
//        @Override
//        public void onSuccess(String s) throws RemoteException {
//            Log.e("DEBUG_BI","detectBI onSuccess");
//            runnableForSensor= new Runnable() {
//                public void run() {
//                   // detectBI();
//                    handlerForSensor.postDelayed(this, 100);
//                }
//            };
//            handlerForSensor.post(runnableForSensor);
//        }
//        @Override
//        public void onFailed(String s) throws RemoteException {
//            Log.e("DEBUG_BI","detectBI onFailed");
//        }
//    };

//    private final IUsbCommadRsp iUsbCommadRspTracking = new IUsbCommadRsp.Stub(){
//        @Override
//        public void onSuccess(String s) throws RemoteException {}
//        @Override
//        public void onFailed(String s) throws RemoteException {}
//    };

    private final IUIFaceTouchCallback iuiFaceTouchCallback = new IUIFaceTouchCallback.Stub() {
        @Override
        public void onTouch(FaceTouchData faceTouchData) throws RemoteException {

            //Right_Eyebrow
            if(faceTouchData.getX() < 460 &&  faceTouchData.getY() < 250){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Integer.parseInt(buddyGPTApplication.getparam("speak_volume"))>0){
                            int curr = Integer.parseInt(buddyGPTApplication.getparam("speak_volume")) - 10;
                            if (curr<0){
                                curr=0;
                            }
                            buddyGPTApplication.setVolume(curr);
                            buddyGPTApplication.setparam("speak_volume", String.valueOf(curr));
                            buddyGPTApplication.setSpeakVolume(curr);
                            settingClass.setVolume(String.valueOf(curr));
                            Log.d(TAG, "RIGHT_EYE update volume : " + curr);
                        }
                        else{
                            Log.d(TAG, "Volume MIN");
                        }
                    }
                });
            }

            //Left_Eyebrow
            else if(faceTouchData.getX() > 720 &&  faceTouchData.getY() < 250){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Integer.parseInt(buddyGPTApplication.getparam("speak_volume"))<100){
                            int curr = Integer.parseInt(buddyGPTApplication.getparam("speak_volume")) + 10;
                            if (curr>100){
                                curr=100;
                            }
                            buddyGPTApplication.setVolume(curr);
                            buddyGPTApplication.setparam("speak_volume", String.valueOf(curr));
                            buddyGPTApplication.setSpeakVolume(curr);
                            settingClass.setVolume(String.valueOf(curr));
                            Log.d(TAG, "LEFT_EYE update volume : " + curr);
                        }
                        else{
                            Log.d(TAG, "Volume MAX");
                        }
                    }
                });
            }

            //eyes
            else if (faceTouchData.getY() > 250 && faceTouchData.getY() < 568){
                Log.e("FCHH","click1");
                if (buddyGPTApplication.getparam("Stimulis").equals("true")) {
                    Log.e("FCHH","click");
                    if (!buddyGPTApplication.getAppIsCurrentlyDealingWithTheQuestion() && !mlKitIsDownloading) {
//                        if (faceTouchData.getX()>200 && faceTouchData.getX()<565){
//                           if (buddyGPTApplication.getParamFromFile("touchLeftEye_Behavior", "BuddyGPT.properties")!=null && !buddyGPTApplication.getParamFromFile("touchLeftEye_Behavior", "BuddyGPT.properties").trim().equalsIgnoreCase("")) {
///                               executeBI("touchLeftEye_Behavior");
//                            }
//                        }
//                        else if (faceTouchData.getX()>773 && faceTouchData.getX()<1120){
//                            if (buddyGPTApplication.getParamFromFile("touchRightEye_Behavior", "BuddyGPT.properties")!=null && !buddyGPTApplication.getParamFromFile("touchRightEye_Behavior", "BuddyGPT.properties").trim().equalsIgnoreCase("")) {
//                                executeBI("touchRightEye_Behavior");
//                            }
//                        }
//                        else {
//                            if (buddyGPTApplication.getParamFromFile("touchFace_Behavior", "BuddyGPT.properties")!=null && !buddyGPTApplication.getParamFromFile("touchFace_Behavior", "BuddyGPT.properties").trim().equalsIgnoreCase("")) {
//                                executeBI("touchFace_Behavior");
//                            }
//                        }
                    }
                }
            }

            //Mouth
            else if(faceTouchData.getX() > 400 && faceTouchData.getX() < 820 &&  faceTouchData.getY() > 568){
                runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Mouth touched1");
                        isSpeaking =false;
                        if(iInvitationCallback != null) iInvitationCallback.onEnd("INVITATION_END");
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
                        if (!buddyGPTApplication.getSpeaking() && !mlKitIsDownloading){
                            Log.d(TAG, "Mouth touched2");
                            if(buddyGPTApplication.getparam("INVALID_TEAMGPT_KEY").equalsIgnoreCase("TRUE") && !buddyGPTApplication.getparam("TeamGPT_Key").equalsIgnoreCase("")){
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

                        }
                        else if (buddyGPTApplication.getSpeaking() && !mlKitIsDownloading) {
                            Log.d(TAG, "Mouth touched3 STT  "+buddyGPTApplication.getparam("STT")+" AUTRE "+buddyGPTApplication.getAppIsListeningToTheQuestion());
                            if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Android")
                                    || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Cerence")
                                    || !buddyGPTApplication.getAppIsListeningToTheQuestion())
                            {
                                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                                buddyGPTApplication.setStartRecording(false);
                                buddyGPTApplication.setSpeaking(false);
                                buddyGPTApplication.setActivityClosed(true);
                                isListeningFreeSpeech = false;
                                buddyGPTApplication.stopTTS();
                                buddyGPTApplication.setStoredResponse("");
                                if (buddy_texte_qst_lyt != null && buddy_texte_resp_lyt != null && buddy_texte_qst != null && buddy_texte_resp != null) {
                                    buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                                    buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                                    buddy_texte_qst.setMovementMethod(null);
                                    buddy_texte_resp.setMovementMethod(null);
                                }
                                lyt_open_menu_settings.setVisibility(View.VISIBLE);
                                lyt_open_menu_chat.setVisibility(View.VISIBLE);
                                try {
                                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }
                                buddyGPTApplication.notifyObservers("end of timer");
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
                                buddyGPTApplication.traitementAudio(false);
                            }
                        }
                    }
                });
            }
        }
        @Override
        public void onRelease(FaceTouchData faceTouchData) throws RemoteException {}
    };


    /**
     * ----------------- Gestion de notifications ---------------------------
     */
    @Override
    public void update(String message) throws IOException {
        if (message != null) {

            if (message.contains("CANCEL_RESPONSE_TIMEOUT")) {
                if (responseTimeout!=null) responseTimeout.cancel();
            }

            else if (message.contains("MODE_STREAM_TEXT;SPLIT;")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (message.split(";SPLIT;").length > 1) {
                            String response = message.split(";SPLIT;")[1];
                            //translate Response Title and show the response :
                            if (buddyGPTApplication.getCurrentLanguage().equals("en")) {
                                showStream("Response",response);
                            }
                            else if (buddyGPTApplication.getCurrentLanguage().equals("fr")){
                                showStream("Réponse",response);
                            }
                            else if (buddyGPTApplication.getCurrentLanguage().equals("de")) {
                                showStream("Antwort",response);
                            }
                            else if (buddyGPTApplication.getCurrentLanguage().equals("es")) {
                                showStream("Respuesta",response);
                            }
                            else{
                                buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate("Response")
                                        .addOnSuccessListener(new OnSuccessListener<String>() {
                                            @Override
                                            public void onSuccess(String translatedText) {
                                                showStream(translatedText,response);
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                showStream("Response",response);
                                            }
                                        });
                            }
                        }
                    }
                });
            }

            else if (message.contains("MODE_STREAM_SPEAK;SPLIT;")){
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

            else if (message.contains("STTHotword_success")) {
                if(buddyGPTApplication.getparam("INVALID_TEAMGPT_KEY").equalsIgnoreCase("TRUE")){
                    Log.i("TAG", "run: notifyObservers INVALID_TEAMGPT_KEY 4");
                    buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
                }else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            buddyGPTApplication.setSpeaking(true);
                            isListeningFreeSpeech = true;
                            buddyGPTApplication.setActivityClosed(false);
                            buddyGPTApplication.setStartRecording(true);
                            startListeningFreeSpeech(buddyGPTApplication.getListeningDuration());
                        }
                    });
                }

            }

            else if (message.contains("STTQuestion_success")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopListeningFreeSpeech();
                        buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(true);
                        SystemClock.sleep(200);
                        buddyGPTApplication.setAppIsListeningToTheQuestion(false);
                        String detectedSTTMessage = message.split(";")[1].replaceAll("' ", "'");
                        Log.i("HOU", "HOU run: "+ detectedSTTMessage);
                        if (!buddyGPTApplication.isActivityClosed()) {
                            buddyGPTApplication.setQuestionNumber(buddyGPTApplication.getQuestionNumber()+1);
                            buddyGPTApplication.setQuestionTime(System.currentTimeMillis());
                            BuddySDK.UI.setFacialExpression(FacialExpression.THINKING,1);
                            if (settingClass.getSwitchVisibility().equals("true")) {
                                if (buddyGPTApplication.getCurrentLanguage().equals("en")) {
                                    buddy_texte_qst.setText(String.format("I heard :  %s ", detectedSTTMessage));
                                }
                                else if (settingClass.getLangue().equals(langueFr)) {
                                    buddy_texte_qst.setText(String.format("J'ai entendu :  %s ", detectedSTTMessage));
                                }
                                else if (settingClass.getLangue().equals(langueEs)) {
                                    buddy_texte_qst.setText(String.format("He oído :  %s ", detectedSTTMessage));
                                }
                                else if (settingClass.getLangue().equals(langueDe)) {
                                    buddy_texte_qst.setText(String.format("Ich habe gehört :  %s ", detectedSTTMessage));
                                }
                                else {
                                    buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate("I heard ").addOnSuccessListener(new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String translatedText) {

                                            buddy_texte_qst.setText(String.format(translatedText+" :  %s ", detectedSTTMessage));
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e(TAG,"translatedText exception  "+e);
                                        }
                                    });
                                }
                                buddy_texte_qst_lyt.setVisibility(View.VISIBLE);
                                buddy_texte_qst.setMovementMethod(new ScrollingMovementMethod());
                                buddy_texte_qst.scrollTo(0, 0);
                                lyt_open_menu_settings.setVisibility(View.INVISIBLE);
                                lyt_open_menu_chat.setVisibility(View.INVISIBLE);
                            }
                            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                            Replica question = new Replica();
                            question.setType("Question");
                            question.setTime(time);
                            question.setValue(detectedSTTMessage);
                            listRep.add(question);
                            if(buddyGPTApplication.getResponseFromTeamGPT()!=null)
                                buddyGPTApplication.getResponseFromTeamGPT().reset();

                            if(buddyGPTApplication.getResponseFromTeamGPT()==null)
                                buddyGPTApplication.setResponseFromTeamGPT(new ResponseFromTeamGPT(buddyGPTApplication));
                            //if(buddyGPTApplication.getparam("Stream_mode").equalsIgnoreCase("true")){
                            buddyGPTApplication.getResponseFromTeamGPT().sendPutRequestStream(detectedSTTMessage, buddyGPTApplication.getQuestionNumber());
//                                }else
//                                    buddyGPTApplication.getResponseFromTeamGPT().sendPutRequestNStream(detectedSTTMessage, buddyGPTApplication.getQuestionNumber());

                            if (
                                    ( Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds","BuddyGPT.properties"))!=0 )
                                            && (
                                            (
                                                    buddyGPTApplication.getCurrentLanguage().equals("en")
                                                            && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en","BuddyGPT.properties").trim().isEmpty()
                                            )
                                                    ||
                                                    (
                                                            buddyGPTApplication.getCurrentLanguage().equals("fr")
                                                                    && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_fr","BuddyGPT.properties").trim().isEmpty()
                                                    )
                                                    ||
                                                    (
                                                            buddyGPTApplication.getCurrentLanguage().equals("es")
                                                                    && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_es","BuddyGPT.properties").trim().isEmpty()
                                                    )
                                                    ||
                                                    (
                                                            buddyGPTApplication.getCurrentLanguage().equals("de")
                                                                    && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_de","BuddyGPT.properties").trim().isEmpty()
                                                    )
                                                    ||(
                                                    !buddyGPTApplication.getCurrentLanguage().equals("en")
                                                            && !buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en","BuddyGPT.properties").trim().isEmpty()
                                            )

                                    )
                            ) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        buddyGPTApplication.setAnswerHasExceededTimeOut(false);
                                        responseTimeout = new CountDownTimer(Integer.parseInt(buddyGPTApplication.getParamFromFile("Response_Timeout_in_seconds", "BuddyGPT.properties")) * 1000, 1000) {
                                            @Override
                                            public void onTick(long l) {}
                                            @Override
                                            public void onFinish() {
                                                if (buddyGPTApplication.isAlreadyGetAnswer()) {
                                                    Log.e(TAG, "app get the answer on time");
                                                } else {
                                                    buddyGPTApplication.setAnswerHasExceededTimeOut(true);
                                                    buddyGPTApplication.setTimeoutExpired(true);
                                                    if (!buddyGPTApplication.isOpenaialreadySwitchEmotion()) {
                                                        BuddySDK.UI.setFacialExpression(FacialExpression.TIRED,1);
                                                    }
                                                    if (buddyGPTApplication.getCurrentLanguage().equals("en")) {
                                                        String[] message_Timeout_NotRespected_en = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_en","BuddyGPT.properties").split("/");
                                                        int randomNumber_message_Timeout_NotRespected_en = new Random().nextInt(message_Timeout_NotRespected_en.length);
                                                        speak(message_Timeout_NotRespected_en[randomNumber_message_Timeout_NotRespected_en],"timeOutExpired");
                                                    }
                                                    else if (buddyGPTApplication.getCurrentLanguage().equals("fr")){
                                                        String[] message_Timeout_NotRespected_fr = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_fr","BuddyGPT.properties").split("/");
                                                        int randomNumber_message_Timeout_NotRespected_fr = new Random().nextInt(message_Timeout_NotRespected_fr.length);
                                                        speak(message_Timeout_NotRespected_fr[randomNumber_message_Timeout_NotRespected_fr],"timeOutExpired");
                                                    }
                                                    else if (buddyGPTApplication.getCurrentLanguage().equals("es")){
                                                        String[] message_Timeout_NotRespected_es = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_es","BuddyGPT.properties").split("/");
                                                        int randomNumber_message_Timeout_NotRespected_es = new Random().nextInt(message_Timeout_NotRespected_es.length);
                                                        speak(message_Timeout_NotRespected_es[randomNumber_message_Timeout_NotRespected_es],"timeOutExpired");
                                                    }
                                                    else if (buddyGPTApplication.getCurrentLanguage().equals("de")){
                                                        String[] message_Timeout_NotRespected_de = buddyGPTApplication.getParamFromFile("Message_Timeout_NotRespected_de","BuddyGPT.properties").split("/");
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
                    }
                });
            }

            else if (message.contains("TTS_success")) {
                runOnUiThread(() ->{
                    Log.e(TAG," TTS_success");
                    buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                    buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                    buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                    buddy_texte_qst.setMovementMethod(null);
                    buddy_texte_resp.setMovementMethod(null);
                    lyt_open_menu_settings.setVisibility(View.VISIBLE);
                    lyt_open_menu_chat.setVisibility(View.VISIBLE);
                    isSpeaking =false;
                    if(iInvitationCallback != null) iInvitationCallback.onEnd("INVITATION_END");
                });
                if(handler!=null && runnable!=null){
                    handler.removeCallbacks(runnable);
                    handler.removeCallbacksAndMessages(null);
                }
                 runnable =new Runnable() {
                    @Override
                    public void run() {
                        if (!buddyGPTApplication.getStoredResponse().equals("")){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    speak(buddyGPTApplication.getStoredResponse(),"storedResponse");
                                }
                            });
                        }
                        else {
                            if (buddyGPTApplication.getStartRecording()) {
                                Log.e(TAG,"startCycle TTS_success 2");
                                buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getListeningAttempt()-1);
                                startCycle();
                            }
                        }
                    }
                };
                handler.postDelayed(runnable,500);
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
                                        buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                                        try {
                                            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                        }
                                        catch (Exception e){
                                            Log.e(TAG,"BuddySDK Exception  "+e);
                                        }
                                        if (buddyGPTApplication.getparam("Stream_mode").equals("true") ){
                                            if(buddyGPTApplication.getResponseFromTeamGPT()!=null)
                                                buddyGPTApplication.getResponseFromTeamGPT().isReadyToSpeak=true;
                                        }
                                        else{
//                                        if (buddyGPTApplication.getparam("Mode_Stream").equals("true") && buddyGPTApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && buddyGPTApplication.getChatGptStreamMode() != null) {
//                                            Log.w("MODE_STREAM","TTS ERROR");
//                                            buddyGPTApplication.getChatGptStreamMode().isReadyToSpeak = true;
//                                        }
//                                        else if(buddyGPTApplication.getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && buddyGPTApplication.getCustomGPTStreamMode() != null){
//                                            buddyGPTApplication.getCustomGPTStreamMode().isReadyToSpeak = true;
//                                        }
//                                        else{
                                            buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                                            buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                                            buddy_texte_qst.setMovementMethod(null);
                                            buddy_texte_resp.setMovementMethod(null);
                                            lyt_open_menu_settings.setVisibility(View.VISIBLE);
                                            lyt_open_menu_chat.setVisibility(View.VISIBLE);
                                            isSpeaking =false;
                                            if(iInvitationCallback != null) iInvitationCallback.onEnd("INVITATION_END");
                                            if(handler!=null && runnable!=null){
                                                handler.removeCallbacks(runnable);
                                                handler.removeCallbacksAndMessages(null);
                                            }
                                            runnable =new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (!buddyGPTApplication.getStoredResponse().equals("")){
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                speak(buddyGPTApplication.getStoredResponse(),"storedResponse");
                                                            }
                                                        });
                                                    }
                                                    else {
                                                        if (buddyGPTApplication.getStartRecording()) {
                                                            buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getListeningAttempt()-1);
                                                            startCycle();
                                                        }
                                                    }
                                                }
                                            };
                                            handler.postDelayed(runnable,500);
                                        }
                                    }
                                });
                            }
                            @Override
                            public void onError(String s) {
                                int textLength = text.length();// Calculate the length of the pronounced text
                                int delayTime = (textLength / 20) * 1000; // 1 second for every 20 characters
                                if (delayTime==0){
                                    delayTime=1500;
                                }
                                if(buddyGPTApplication.getparam("TTS").equalsIgnoreCase("ReadSpeaker") && (buddyGPTApplication.getCurrentLanguage().equals("en") || buddyGPTApplication.getCurrentLanguage().equals("fr")) && buddyGPTApplication.getUsingReadSpeaker() ){
                                    delayTime = 0;
                                    Log.e(TAG,"set 0 delay time  "+delayTime);
                                }
                                handlerTTSError.postDelayed(runnableTTSError = new Runnable() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                                                try {
                                                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                                }
                                                catch (Exception e){
                                                    Log.e(TAG,"BuddySDK Exception  "+e);
                                                }
                                                if (buddyGPTApplication.getparam("Stream_mode").equals("true") ){
                                                    if(buddyGPTApplication.getResponseFromTeamGPT()!=null)
                                                        buddyGPTApplication.getResponseFromTeamGPT().isReadyToSpeak=true;
                                                }
                                                else{

//                                                if (buddyGPTApplication.getparam("Mode_Stream").equals("true") && buddyGPTApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && buddyGPTApplication.getChatGptStreamMode() != null) {
//                                                    Log.w("MODE_STREAM","TTS ERROR");
//                                                    buddyGPTApplication.getChatGptStreamMode().isReadyToSpeak = true;
//                                                }
//                                                else if(buddyGPTApplication.getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && buddyGPTApplication.getCustomGPTStreamMode() != null){
//                                                    buddyGPTApplication.getCustomGPTStreamMode().isReadyToSpeak = true;
//                                                }
//                                                else{
                                                    buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                                                    buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                                                    buddy_texte_qst.setMovementMethod(null);
                                                    buddy_texte_resp.setMovementMethod(null);
                                                    lyt_open_menu_settings.setVisibility(View.VISIBLE);
                                                    lyt_open_menu_chat.setVisibility(View.VISIBLE);
                                                    isSpeaking =false;
                                                    if(iInvitationCallback != null) iInvitationCallback.onEnd("INVITATION_END");
                                                    if(handler!=null && runnable!=null){
                                                        handler.removeCallbacks(runnable);
                                                        handler.removeCallbacksAndMessages(null);
                                                    }
                                                    runnable =new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (!buddyGPTApplication.getStoredResponse().equals("")){
                                                                runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        speak(buddyGPTApplication.getStoredResponse(),"storedResponse");
                                                                    }
                                                                });
                                                            }
                                                            else {
                                                                if (buddyGPTApplication.getStartRecording()) {
                                                                    buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getListeningAttempt()-1);
                                                                    startCycle();
                                                                }
                                                            }
                                                        }
                                                    };
                                                    handler.postDelayed(runnable,500);
                                                }
                                            }
                                        });
                                    }
                                },delayTime);
                            }
                        });
                    }
                });
            }

            else if (message.contains("CHATBOTS_RETURN")) {
                runOnUiThread(() ->{
                    String action = message.split(";SPLIT;")[1];
                    Log.i(TAG,"action : "+action);
                    String value = message.split(";SPLIT;")[2];
                    Log.i(TAG,"value : "+value);
                    if (action.equals("speak")) {
                        if (message.split(";SPLIT;").length>3){
                            int numberOfQuestion = Integer.parseInt(message.split(";SPLIT;")[3]);
                            if(numberOfQuestion== buddyGPTApplication.getQuestionNumber()) {
                                if(message.split(";SPLIT;").length>4 ){
                                    if (message.split(";SPLIT;")[4].equals("onError")){
                                        buddyGPTApplication.setMessageError(true);
                                        if (!buddyGPTApplication.isOpenaialreadySwitchEmotion()) {
                                            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                        }
                                    }
                                }
                                if (!buddyGPTApplication.isTimeoutExpired()) {
                                    speak(value, "nothealysa");
                                } else {
                                    buddyGPTApplication.setStoredResponse(value);
                                }
                            }
                        }
                    }
                    else if (action.equals("INVITATION")) {
                        Log.d(TAG_TRACKING, "ChatGPT Invitation: " + value);
                        speak(value, "INVITATION");
                    }
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
                buddyGPTApplication.startListeningHotwor(this);
            }

            else if (message.contains("end of cycle")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
                        BuddySDK.UI.stopListenAnimation();
                        buddyGPTApplication.setLed("neutral");
                    }
                });
            }

            else if (message.contains("restartNewCycle")){
                runnablePauseTime =new Runnable() {
                    @Override
                    public void run() {
                        startNextCycle();
                        Log.e(TAG,"startNextCycle  after handler ");
                    }
                };
                handlerPauseTime.postDelayed(runnablePauseTime,1000);
            }

            else if (message.contains("Obtain audio transcription after the listening time has elapsed")){
                String shouldRestartNewCycle = message.split(";SPLIT;")[1];
                Log.e(TAG,"Obtain audio transcription after the listening time has elapsed "+shouldRestartNewCycle);
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
                BuddySDK.UI.stopListenAnimation();
                buddyGPTApplication.setLed("neutral");
                buddyGPTApplication.setAppIsListeningToTheQuestion(false);
                if (shouldRestartNewCycle.equals("true")) {
                    buddyGPTApplication.traitementAudio(true);
                }
                else {
                    buddyGPTApplication.traitementAudio(false);
                }
            }

            else if (message.contains("restartListeningHotword")){
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                buddyGPTApplication.setStartRecording(false);
                buddyGPTApplication.setSpeaking(false);
                buddyGPTApplication.setActivityClosed(true);
                isListeningFreeSpeech = false;
                buddyGPTApplication.stopTTS();
                buddyGPTApplication.setStoredResponse("");
                if (buddy_texte_qst_lyt != null && buddy_texte_resp_lyt != null && buddy_texte_qst != null && buddy_texte_resp != null) {
                    buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                    buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                    buddy_texte_qst.setMovementMethod(null);
                    buddy_texte_resp.setMovementMethod(null);
                }
                lyt_open_menu_settings.setVisibility(View.VISIBLE);
                lyt_open_menu_chat.setVisibility(View.VISIBLE);
                try {
                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                } catch (Exception e) {
                    Log.e(TAG, "BuddySDK Exception  " + e);
                }
                buddyGPTApplication.notifyObservers("end of timer");
            }
            if (message.contains("INVALID_TEAMGPT_KEY")){
                buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY","TRUE");
                     if (buddyGPTApplication.getLangue().getNom().equals("Anglais")) {
                         buddyGPTApplication.showInputDialog(MainActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                     } else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                         buddyGPTApplication.showInputDialog(MainActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_fr), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_fr));
                     } else if (buddyGPTApplication.getLangue().getNom().equals("Espagnol")) {
                         buddyGPTApplication.showInputDialog(MainActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_es), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_es));
                     } else if (buddyGPTApplication.getLangue().getNom().equals("Allemand")) {
                         buddyGPTApplication.showInputDialog(MainActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_de), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_de));
                     }
                     else {
                         buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                                 .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en))
                                 .addOnSuccessListener(new OnSuccessListener<String>() {
                                     @Override
                                     public void onSuccess(String translatedText) {
                                         buddyGPTApplication.showInputDialog(MainActivity.this, translatedText,"Attention !");
                                     }
                                 })
                                 .addOnFailureListener(new OnFailureListener() {
                                     @Override
                                     public void onFailure(@NonNull Exception e) {
                                         buddyGPTApplication.showInputDialog(MainActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                                     }
                                 });
                     }


            }
            if (message.contains("Session_ID_ERROR")){

                if (buddyGPTApplication.getLangue().getNom().equals("Anglais")) {
                    buddyGPTApplication.showInputDialog2(MainActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_params_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                } else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                    buddyGPTApplication.showInputDialog2(MainActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_params_invalid_fr), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_fr));
                } else {
                         buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                                 .translate(buddyGPTApplication.getString(R.string.toast_teamgpt_key_invalid_en))
                                 .addOnSuccessListener(new OnSuccessListener<String>() {
                                     @Override
                                     public void onSuccess(String translatedText) {
                                         buddyGPTApplication.showInputDialog2(MainActivity.this, translatedText,"Attention !");
                                     }
                                 })
                                 .addOnFailureListener(new OnFailureListener() {
                                     @Override
                                     public void onFailure(@NonNull Exception e) {
                                         buddyGPTApplication.showInputDialog2(MainActivity.this, buddyGPTApplication.getString(R.string.toast_teamgpt_params_invalid_en), buddyGPTApplication.getString(R.string.toast_teamgpt_invalid_en));
                                     }
                                 });
                     }


            }
            else if (message.contains("getResponseF")) {
                if (message.split(";SPLIT;")[1].equalsIgnoreCase("gpt")) {
                    gptResponse = message.split(";SPLIT;")[2];
                    gptSend = true;
                }
                if (gptSend ) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (Integer.parseInt(message.split(";SPLIT;")[3])== buddyGPTApplication.getQuestionNumber()) {
                                if (!buddyGPTApplication.getAnswerHasExceededTimeOut()){
                                    setAnimation(gptResponse);
                                }
                                buddyGPTApplication.setOpenaialreadySwitchEmotion(true);
                            }
                            gptSend = false;
                        }
                    });
                }
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

            else if (message.contains("playEmotion")){
                if (!buddyGPTApplication.getCurrentEmotion().equals("")){
                    if (!buddyGPTApplication.getAnswerHasExceededTimeOut()){
                        setAnimation(buddyGPTApplication.getCurrentEmotion());
                    }
                    buddyGPTApplication.setOpenaialreadySwitchEmotion(true);
                }
            }

            else if (message.contains("ChatDestroy")){
                buddyGPTApplication.setparam("firstLaunch","false");
            }

            else if (message.contains("isConnected")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        downloadingBar.setVisibility(View.VISIBLE);
                        noNetwork.setVisibility(View.GONE);
                    }
                });
            }

            else if (message.contains("isNotConnected")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        downloadingBar.setVisibility(View.GONE);
                        noNetwork.setVisibility(View.VISIBLE);
                    }
                });
            }

            else if (message.contains("changeDetected")){
                int speakVolume = buddyGPTApplication.getVolume();
                int max = buddyGPTApplication.getMaxVolume();
                int defaultVolume = buddyGPTApplication.getClosestInt((double) (speakVolume * 100) / max);
                Log.e(TAG,"volumeMedia  "+String.valueOf(defaultVolume));
                buddyGPTApplication.setparam("speak_volume", String.valueOf(defaultVolume));
            }

            else if (message.contains( "commandResponse" )){
                if(message.split( ";SPLIT;" )[1].equals("CANCEL")){
                    if(!isSpeaking){
                        if (responseTimeout!=null) responseTimeout.cancel();
                        if (!buddyGPTApplication.getUsingEmotions()){
                            Log.d(TAG,"FacialExpression NEUTRAL");
                            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
                        }
                        buddyGPTApplication.notifyObservers("TTS_success");
                    }
                }
                else if(message.split( ";SPLIT;" )[1].equals("CHANGE_LANGUE")){
                    isCMDLangue = true;
                    settingClass.setLangue(buddyGPTApplication.getLangue().getNom());
                    mlKitIsDownloading = true;
                    buddyGPTApplication.downloadModel(imlKitDownloadCallback, buddyGPTApplication.getLangue().getLanguageCode().split("-")[0].trim());
                    handlerProgressBar.postDelayed(runnableProgressBar,500);
                }
                else{
                    if(!isSpeaking)
                    {
                        stopListeningFreeSpeech();
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                            BuddySDK.UI.stopListenAnimation();
                        } catch (Exception e) {
                            Log.e(TAG, "BuddySDK Exception  " + e);
                        }
                        speak(message.split( ";SPLIT;" )[1], "commande");
                    }
                    else
                        buddyGPTApplication.setStoredResponse( message.split( ";SPLIT;" )[1] );
                }
            }


//            else if (message.contains("takePicture")){
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        startCameraForCommand();
//                    }
//                });
//            }

            else if(message.contains("STOP_TRACKING")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation"))){
                            stopTracking();
                        }
                    }
                });
            }

            else if(message.contains("RESTART_TRACKING")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation"))){
                            isReTrack = true;
                            //initTracking();
                        }
                    }
                });
            }
        }
    }


    /**
     * ----------------- Utils ---------------------------
     */

    private void init() throws IOException {
        Log.e(TAG,"init() ");
        String imei= getIMEI();

        buddyGPTApplication.setImeiRobot(imei);
        BuddySDK.UI.addFaceTouchListener(iuiFaceTouchCallback);
//        if (buddyGPTApplication.getparam("Stimulis").equals("true")){
//            handlerForSensor = new Handler();
//            BuddySDK.USB.enableSensorModule(true,iUsbCommadRspBI);
//        }

        buddyGPTApplication.registerObserver(this);

        buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
        buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
        buddy_texte_qst.setMovementMethod(null);
        buddy_texte_resp.setMovementMethod(null);
        lyt_open_menu_settings.setVisibility(View.VISIBLE);
        lyt_open_menu_chat.setVisibility(View.VISIBLE);

//        commande = new Commande( this );

        //init config file
       // buddyGPTApplication.pushFiles("teamchat.json","storage/emulated/0/Configs/Users/Default/Companion/Domains");
        initOrMajOrNone = buddyGPTApplication.createPropertiesFile();
        Log.i(TAG, "init: isFirstLaunch "+isFirstLaunch);
        if(isFirstLaunch) {
            buddyGPTApplication.initTeamGPTSettings();
            buddyGPTApplication.setparam("session_id","");
        }
    }

    private void getData(){

        buddyGPTApplication.initTTSAndroid();
        //buddyGPTApplication.initTTSGoogleCoud();

        //init Settings
        settingClass=new Setting();
        settingClass.setDuration(buddyGPTApplication.getparam("listening_duration"));
        settingClass.setAttempt(buddyGPTApplication.getparam("listening_attempt"));
        settingClass.setChatbot(buddyGPTApplication.getparam("SelectedChatbot"));
        settingClass.setLangue(buddyGPTApplication.getLangue().getNom());
        settingClass.setVolume(buddyGPTApplication.getparam("speak_volume"));
        settingClass.setSwitchVisibility(buddyGPTApplication.getparam("switch_visibility"));
        settingClass.setSwitchEmotion(buddyGPTApplication.getparam("switch_emotion"));
        Log.i(TAG, settingClass.toString());
//        if (buddyGPTApplication.getparam("Stimulis").equals("true")){
//            Log.e("MRA","disnable Raise event Stimilus");
//            BuddySDK.Companion.raiseEvent("disableRightEye");
//            BuddySDK.Companion.raiseEvent("disableLeftEye");
//            BuddySDK.Companion.raiseEvent("disableHeadSensors");
//            BuddySDK.Companion.raiseEvent("disableBodySensors");
//        }else {
//            if (buddyGPTApplication.getParamFromFile("use_companion_when_stimulis_disabled","BuddyGPT.properties").trim().equalsIgnoreCase("Yes")){
//                Log.e("MRARA","enable Raise event Yes");
//                BuddySDK.Companion.raiseEvent("enableRightEye");
//                BuddySDK.Companion.raiseEvent("enableLeftEye");
//                BuddySDK.Companion.raiseEvent("enableHeadSensors");
//                BuddySDK.Companion.raiseEvent("enableBodySensors");
//                BuddySDK.Companion.raiseEvent("disableOnMouth");
//            }else {
//                Log.e("MRARA","disable Raise event NO");
//                BuddySDK.Companion.raiseEvent("disableRightEye");
//                BuddySDK.Companion.raiseEvent("disableLeftEye");
//                BuddySDK.Companion.raiseEvent("disableHeadSensors");
//                BuddySDK.Companion.raiseEvent("disableBodySensors");
          //  }
      //  }

        wifiBroadCastReceiver.setAct(getApplicationContext());
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(wifiBroadCastReceiver, intentFilter);
        wifiBroadCastReceiver.forceCheckConnexState(getApplicationContext());

        refreshSTTLangue();

        //set volume
        buddyGPTApplication.setVolume(Integer.parseInt(buddyGPTApplication.getparam("speak_volume")));

        //create Log file
        if (buddyGPTApplication.getFileCreate()) {
            buddyGPTApplication.listSessionClear();
            listRep.clear();
            buddyGPTApplication.setFileCreate(false);
        }

        //init chatbots
        /*if(responseFromTeamGPT != null){
            responseFromTeamGPT.reset();
        }*/
      //  responseFromChatbot = new ResponseFromTeamGPT(buddyGPTApplication);


        //start hotword listening
        mlKitIsDownloading = true;
        buddyGPTApplication.downloadModel(imlKitDownloadCallback,new Gson().fromJson(buddyGPTApplication.getparam(settingClass.getLangue()), Langue.class).getLanguageCode().split("-")[0].trim());
        handlerProgressBar.postDelayed(runnableProgressBar,500);

        buddyGPTApplication.setActivityClosed(false);

    }

    private void showStream(String responseTitle,String response) {
        if (!buddyGPTApplication.isActivityClosed()) {
            buddy_texte_resp.setText(String.format(responseTitle + " :  %s ", response));
            buddy_texte_resp_lyt.setVisibility(View.VISIBLE);
            buddy_texte_resp.setMovementMethod(new ScrollingMovementMethod());
            // Scroll to the end
            buddy_texte_resp.post(new Runnable() {
                @Override
                public void run() {
                    int scrollAmount = buddy_texte_resp.getLayout().getLineTop(buddy_texte_resp.getLineCount())
                            - buddy_texte_resp.getHeight() + buddy_texte_resp.getLineHeight();
                    if (scrollAmount > 0) {
                        buddy_texte_resp.scrollTo(0, scrollAmount);
                    } else {
                        buddy_texte_resp.scrollTo(0, 0);
                    }
                }
            });
        }
    }

    private void afficherPopupAvecBitmap(String imagePath) {
        if(dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_layout);
        ImageView imageView = dialog.findViewById(R.id.imageView);
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        imageView.setImageBitmap(bitmap);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.horizontalMargin = 0;
        layoutParams.verticalMargin = 0;
        dialog.getWindow().setAttributes(layoutParams);
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.show();
    }



    private void hideTimer(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                photo_timer_rlyt.setVisibility(View.GONE);
                photo_timer_bg_rlyt.setVisibility(View.GONE);
                photo_timer_txtView.setText("");
            }
        });

    }

    private void detectBI(){
        boolean head_top_touched = BuddySDK.Sensors.HeadTouchSensors().Top().isTouched();
        boolean head_left_touched = BuddySDK.Sensors.HeadTouchSensors().Left().isTouched();
        boolean head_right_touched = BuddySDK.Sensors.HeadTouchSensors().Right().isTouched();
        boolean body_torso_touched = BuddySDK.Sensors.BodyTouchSensors().Torso().isTouched();
        boolean body_left_touched = BuddySDK.Sensors.BodyTouchSensors().LeftShoulder().isTouched();
        boolean body_right_touched = BuddySDK.Sensors.BodyTouchSensors().RightShoulder().isTouched();
        Log.i("DEBUG_BI","detectBI : "+head_top_touched+"   -   "+head_left_touched+"   -   "+head_right_touched+"   -   "+body_torso_touched+"   -   "+body_left_touched+"   -   "+body_right_touched);
        if (!buddyGPTApplication.getAppIsCurrentlyDealingWithTheQuestion() && !mlKitIsDownloading){
//            if (head_top_touched){
//                if (buddyGPTApplication.getParamFromFile("touchCenterHead_Behavior", "BuddyGPT.properties")!=null && !buddyGPTApplication.getParamFromFile("touchCenterHead_Behavior", "BuddyGPT.properties").trim().equalsIgnoreCase("")){
//                    executeBI("touchCenterHead_Behavior");
//                }
//            }
//            else if (head_left_touched){
//                if (buddyGPTApplication.getParamFromFile("touchLeftHead_Behavior", "BuddyGPT.properties")!=null && !buddyGPTApplication.getParamFromFile("touchLeftHead_Behavior", "BuddyGPT.properties").trim().equalsIgnoreCase("")){
//                    executeBI("touchLeftHead_Behavior");
//                }
//            }
//            else if (head_right_touched){
//                if (buddyGPTApplication.getParamFromFile("touchRightHead_Behavior", "BuddyGPT.properties")!=null && !buddyGPTApplication.getParamFromFile("touchRightHead_Behavior", "BuddyGPT.properties").trim().equalsIgnoreCase("")){
//                    executeBI("touchRightHead_Behavior");
//                }
//            }
//            else if (body_torso_touched){
//                if (buddyGPTApplication.getParamFromFile("touchHeart_Behavior", "BuddyGPT.properties")!=null && !buddyGPTApplication.getParamFromFile("touchHeart_Behavior", "BuddyGPT.properties").trim().equalsIgnoreCase("")){
//                    executeBI("touchHeart_Behavior");
//                }
//            }
//            else if (body_left_touched){
//                if (buddyGPTApplication.getParamFromFile("touchLeftShoulder_Behavior", "BuddyGPT.properties")!=null && !buddyGPTApplication.getParamFromFile("touchLeftShoulder_Behavior", "BuddyGPT.properties").trim().equalsIgnoreCase("")){
//                    executeBI("touchLeftShoulder_Behavior");
//                }
//            }
//            else if (body_right_touched){
//                if (buddyGPTApplication.getParamFromFile("touchRightShoulder_Behavior", "BuddyGPT.properties")!=null && !buddyGPTApplication.getParamFromFile("touchRightShoulder_Behavior", "BuddyGPT.properties").trim().equalsIgnoreCase("")){
//                    executeBI("touchRightShoulder_Behavior");
//                }
//            }
        }
    }

    private String getTheRightBINameFromConfigFile(String propertyName){
        String xmlBehaviour;
        if (!propertyName.equals("") && buddyGPTApplication.getParamFromFile(propertyName, "BuddyGPT.properties")!=null) {
            StringTokenizer st = new StringTokenizer(buddyGPTApplication.getParamFromFile(propertyName, "BuddyGPT.properties"), "/", false);
            List<String> listBehaviour = new ArrayList<>();
            while (st.hasMoreTokens()) {
                String result = st.nextToken();
                listBehaviour.add(result.toLowerCase());
            }
            if (listBehaviour.size() > 0) {
                xmlBehaviour = listBehaviour.get(new Random().nextInt(listBehaviour.size())).trim();
            } else {
                xmlBehaviour = "";
            }
        }
        else xmlBehaviour = "";
        Log.i("DEBUG_BI","xmlBehaviour récupéré depuis le fichier de configuration  "+xmlBehaviour);
        String dossierExterne = getString(R.string.path) + "/BI/Behaviour";
        if (!xmlBehaviour.contains(".xml") && !xmlBehaviour.equals("")){
            List<String> nomsFichiers = getFilenamesForCategory(dossierExterne, xmlBehaviour);
            if (!nomsFichiers.isEmpty()) {
                xmlBehaviour = nomsFichiers.get(new Random().nextInt(nomsFichiers.size()));
                Log.i("DEBUG_BI","Nom du fichier choisi : " + xmlBehaviour);
            } else {
                Log.i("DEBUG_BI","Aucun fichier trouvé pour la catégorie : " + xmlBehaviour);
            }
            Log.i("DEBUG_BI","Nom du fichier selon la  catégorie choisi : " + xmlBehaviour);
            return xmlBehaviour;
        }
        else if (xmlBehaviour.equals("")) {
            Log.i("DEBUG_BI", "Le fichier n'existe pas.");
            return "";
        }
        else {
            Log.i("DEBUG_BI", "xmlBehaviour : "+xmlBehaviour);
            return xmlBehaviour;
        }
    }

    private static List<String> getFilenamesForCategory(String dossier, String categorieRecherchee) {
        List<String> nomsFichiers = new ArrayList<>();
        File dossierBehaviours = new File(dossier);
        File[] fichiers = dossierBehaviours.listFiles();
        if (fichiers != null) {
            for (File fichier : fichiers) {
                if (fichier.isFile() && fichier.getName().toLowerCase().endsWith(".xml")) {
                    if (fichier.getName().toLowerCase().contains(categorieRecherchee.toLowerCase())) {
                        nomsFichiers.add(fichier.getName());
                    }
                }
            }
        }
        return nomsFichiers;
    }

    private void executeBI(String behaviour){
        Log.i("DEBUG_BI", "executeBI : "+behaviour);
        buddyGPTApplication.setBIExecution(true);
        mlKitIsDownloading = true;
        stopListeningFreeSpeech();
        BuddySDK.UI.stopListenAnimation();
        BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
        BIPlayer.getInstance().playBI(this, getTheRightBINameFromConfigFile(behaviour), new IBehaviourCallBack() {
            @Override
            public void onEnd(boolean hasAborted, String reason) {
                Log.e("DEBUG_BI","on END BI execution");
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
                BuddySDK.UI.lookAt(GazePosition.CENTER, true);
                mlKitIsDownloading = false;
                buddyGPTApplication.setBIExecution(false);
                buddyGPTApplication.notifyObservers("end of timer");
            }
            @Override
            public void onRun(String s) {}
        });
    }

    private void refreshSTTLangue() {
        buddyGPTApplication.refresh(new Gson().fromJson(buddyGPTApplication.getparam(settingClass.getLangue()), Langue.class).getLanguageCode(),this);
    }

    private void setAnimation(String emotion){
        buddyGPTApplication.setCurrentEmotion("");
        if (buddyGPTApplication.separator(buddyGPTApplication.getParamFromFile("BuddyFace_Happy", "BuddyGPT.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.HAPPY,1);
        }
        else if (buddyGPTApplication.separator(buddyGPTApplication.getParamFromFile("BuddyFace_Thinking", "BuddyGPT.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.THINKING,1);
        }
        else if (buddyGPTApplication.separator(buddyGPTApplication.getParamFromFile("BuddyFace_Sick", "BuddyGPT.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.SICK,1);
        }
        else if (buddyGPTApplication.separator(buddyGPTApplication.getParamFromFile("BuddyFace_Love", "BuddyGPT.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.LOVE,1);
        }
        else if (buddyGPTApplication.separator(buddyGPTApplication.getParamFromFile("BuddyFace_Tired", "BuddyGPT.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED,1);
        }
        else if (buddyGPTApplication.separator(buddyGPTApplication.getParamFromFile("BuddyFace_Listening", "BuddyGPT.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.LISTENING,1);
        }
        else if (buddyGPTApplication.separator(buddyGPTApplication.getParamFromFile("BuddyFace_Surprised", "BuddyGPT.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.SURPRISED,1);
        }
        else if (buddyGPTApplication.separator(buddyGPTApplication.getParamFromFile("BuddyFace_Grumpy", "BuddyGPT.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.GRUMPY,1);
        }
        else if (buddyGPTApplication.separator(buddyGPTApplication.getParamFromFile("BuddyFace_Scared", "BuddyGPT.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.SCARED,1);
        }
        else if (buddyGPTApplication.separator(buddyGPTApplication.getParamFromFile("BuddyFace_Angry", "BuddyGPT.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.ANGRY,1);
        }
        else if (buddyGPTApplication.separator(buddyGPTApplication.getParamFromFile("BuddyFace_Sad", "BuddyGPT.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.SAD,1);
        }
        else{
            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
        }
    }

    public void btnOpenSettings(View view) {
        if ( !mlKitIsDownloading){
            Intent intent = new Intent(MainActivity.this,SettingsActivity.class);
            intent.putExtra("activity_name", "main");
            startActivity(intent);
            finish();

            overridePendingTransition(0, 0);
        }else if (buddyGPTApplication.getBIExecution()){
            BIPlayer.getInstance().stopBehaviour();
            Intent intent = new Intent(MainActivity.this,SettingsActivity.class);
            intent.putExtra("activity_name", "main");
            startActivity(intent);
            finish();

            overridePendingTransition(0, 0);
        }
    }

    public void btnOpenChat(View view) {
        if ( !mlKitIsDownloading) {
            Intent intent = new Intent(MainActivity.this, ChatWindow.class);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        }
        else if (buddyGPTApplication.getBIExecution()){
            BIPlayer.getInstance().stopBehaviour();
            Intent intent = new Intent(MainActivity.this, ChatWindow.class);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        }
    }


    public String getIMEI() {
        String imei = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For Android 8.0 and above
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    imei = telephonyManager.getImei();
                }
            }
        } else {
            // For Android versions below 8.0
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                imei = telephonyManager.getDeviceId();
            }
        }
        return imei;
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

        Log.d(TAG," --- startListeningFreeSpeech("+duration+") ---");
        Log.d(TAG," --- startListeningFreeSpeech( STT"+ buddyGPTApplication.getparam("STT")+") ---");


            if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Android")){
                buddyGPTApplication.startListeningQuestion(this);
            }

            else if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Cerence")){
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
                if(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation")) && Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Auto_Listen")) && regarde_camera){
                    Log.i(TAG_TRACKING, "timerEcoute onFinish --> Do not stop listening because tracking auto listen is enabled and user is looking directly at camera --> restart timer");
                    timerEcoute.start();
                }
                else{
                    Log.i(TAG, "timerEcoute onFinish");
                    if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Android") || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Cerence")){
                        buddyGPTApplication.notifyObservers("end of timer");
                    }
                    else{
                        buddyGPTApplication.notifyObservers("Obtain audio transcription after the listening time has elapsed;SPLIT;false");
                    }
                }
            }
        };
        timerEcoute.start();

    }

    private void stopListeningFreeSpeech() {
        isListeningFreeSpeech = false;
        Log.d(TAG," --- stopListeningFreeSpeech() ---");
        if (timerEcoute!=null) timerEcoute.cancel();
        buddyGPTApplication.stopListening(this);
    }

    private void startCycle() {
        Log.e(TAG,"startCycle  after handler ");
        isListeningFreeSpeech = true;
        buddyGPTApplication.setMessageError(false);
        buddyGPTApplication.setOpenaialreadySwitchEmotion(false);
        buddyGPTApplication.setAppIsListeningToTheQuestion(true);
        buddyGPTApplication.setAlreadyChatting(false);

            if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Android")){
                buddyGPTApplication.startListeningQuestion(this);
            }
            else if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Cerence")){
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
                if(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Activation")) && Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Auto_Listen")) && regarde_camera){
                    Log.i(TAG_TRACKING, "timerEcoute onFinish --> Do not stop listening because tracking auto listen is enabled and user is looking directly at camera --> restart timer");
                    timerEcoute.start();
                }
                else{
                    Log.i(TAG, "timerEcoute onFinish");
                    if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Android") || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Cerence")){
                        buddyGPTApplication.notifyObservers("end of cycle");
                        runnablePauseTime =new Runnable() {
                            @Override
                            public void run() {
                                startNextCycle();
                                Log.e(TAG,"startNextCycle  after handler ");
                            }
                        };
                        handlerPauseTime.postDelayed(runnablePauseTime,1000);
                    }
                    else{
                        buddyGPTApplication.notifyObservers("Obtain audio transcription after the listening time has elapsed;SPLIT;true");
                    }
                }
            }
        };
        timerEcoute.start();
    }

    private void startNextCycle() {
        Log.e(TAG,"startNextCycle  remainingattempts= "+ buddyGPTApplication.getRemainingAttempts());
        if (buddyGPTApplication.getRemainingAttempts() > 0) {
            buddyGPTApplication.setRemainingAttempts(buddyGPTApplication.getRemainingAttempts()-1);
            startCycle();
            Log.e(TAG,"startNextCycle  after handler ");
        }
        else {
            buddyGPTApplication.notifyObservers("end of timer");
        }
    }


    /**
     * ------------------------------------------ TTS  -------------------------------------------
     */

    private void speak(final String texte, String type) {
        Log.d(TAG," --- speak("+texte+") ---");
        isSpeaking = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                buddy_texte_resp_lyt.setTranslationY(0);
                if (responseTimeout!=null) responseTimeout.cancel();
                if (!buddyGPTApplication.getUsingEmotions()){
                    Log.d(TAG,"FacialExpression NEUTRAL");
                    BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
                }
                if (!buddyGPTApplication.isActivityClosed()) {
                    if (type.equals("nothealysa") || type.equals("storedResponse")) {
                        buddyGPTApplication.setAlreadyGetAnswer(true);

                            if (settingClass.getSwitchVisibility().equals("true")) {
                                if (buddyGPTApplication.getCurrentLanguage().equals("en")) {
                                    buddy_texte_resp.setText(String.format("Response :  %s ", texte));
                                } else if (buddyGPTApplication.getCurrentLanguage().equals("fr")) {
                                    buddy_texte_resp.setText(String.format("Réponse :  %s ", texte));
                                } else if (buddyGPTApplication.getCurrentLanguage().equals("de")) {
                                    buddy_texte_resp.setText(String.format("Antwort :  %s ", texte));
                                } else if (buddyGPTApplication.getCurrentLanguage().equals("es")) {
                                    buddy_texte_resp.setText(String.format("Respuesta :  %s ", texte));
                                } else {
                                    buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate("Response").addOnSuccessListener(new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String translatedText) {
                                            buddy_texte_resp.setText(String.format(translatedText + " :  %s ", texte));
                                        }

                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e(TAG, "translatedText exception  " + e);
                                        }
                                    });

                                }
                                if (type.equals("storedResponse")) {
                                    lyt_open_menu_settings.setVisibility(View.INVISIBLE);
                                    lyt_open_menu_chat.setVisibility(View.INVISIBLE);
                                    if (buddy_texte_qst_lyt.getVisibility() != View.VISIBLE)
                                        buddy_texte_resp_lyt.setTranslationY(-155);
                                    else buddy_texte_resp_lyt.setTranslationY(0);
                                }
                                buddy_texte_resp_lyt.setVisibility(View.VISIBLE);
                                buddy_texte_resp.setMovementMethod(new ScrollingMovementMethod());
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
                                }
                                else{
                                    //---> this function is called after finishing pronouncing a phrase from the response : we should add the new phrase to the already existing Replica
                                    ArrayList<Session> listSessions = buddyGPTApplication.getListSession();
                                    ArrayList<Replica> lastSession = listSessions.get(listSessions.size() - 1).getSession();
                                    Replica lastReplica = lastSession.get(lastSession.size() - 1);
                                    if (lastReplica.getType().equals("Response")) {
                                                lastReplica.setValue(lastReplica.getValue() + texte);
                                        }
                                    else
                                        lastReplica.setValue(texte);

                                    }
                                }
                                if (buddyGPTApplication.getparam("Stream_mode").equalsIgnoreCase("true"))
                                    buddy_texte_resp.scrollTo(0, 0);
                            else{

                                // Scroll to the end
                                buddy_texte_resp.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        int scrollAmount = buddy_texte_resp.getLayout().getLineTop(buddy_texte_resp.getLineCount())
                                                - buddy_texte_resp.getHeight() + buddy_texte_resp.getLineHeight();
                                        if (scrollAmount > 0) {
                                            buddy_texte_resp.scrollTo(0, scrollAmount);
                                        } else {
                                            buddy_texte_resp.scrollTo(0, 0);
                                        }
                                    }
                                });
                            }



//                        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
//
//                            Replica reponse = new Replica();
//                            reponse.setValue(texte);
//                            reponse.setTime(time);
//                            long responseTime = buddyGPTApplication.getGetResponseTime()- buddyGPTApplication.getQuestionTime();
//                            DecimalFormat df = new DecimalFormat("#,###");
//                            String formattedTime= df.format(responseTime);
//                            reponse.setType("Response");
//                            reponse.setDuration(formattedTime + " ms");
//                            DecimalFormat decimalFormatter = new DecimalFormat("0.00");
//                            String formattedValue = decimalFormatter.format(Double.parseDouble(buddyGPTApplication.getparam("Total_cons")));
//                            reponse.setPrix(formattedValue+" $");
//                            listRep.add(reponse);
//                            ArrayList<Replica> ll = new ArrayList<>();
//                            for (int t = 0; t < listRep.size(); t++) {
//                                ll.add(listRep.get(t));
//                            }
//
//                            listRep.clear();
////                        }
////                        else{
//                            if (!listRep.isEmpty()) {
//                                //--> this function is called right after a question : we should create a new Replica for the response
//                                Replica reponse = new Replica();
//                                reponse.setValue(texte);
//                                reponse.setTime(time);
//                                long responseTime = buddyGPTApplication.getGetResponseTime() - buddyGPTApplication.getQuestionTime();
//                                DecimalFormat df = new DecimalFormat("#,###");
//                                String formattedTime= df.format(responseTime);
//                                reponse.setType("Response");
//                                reponse.setDuration(formattedTime + " ms");
//                                DecimalFormat decimalFormatter = new DecimalFormat("0.00");
//                                String formattedValue = decimalFormatter.format(Double.parseDouble(buddyGPTApplication.getparam("Total_cons")));
//                                reponse.setPrix(formattedValue+" $");
//                                listRep.add(reponse);
//
//                                listRep.clear();
//                            }
//                            else{
//                                //---> this function is called after finishing pronouncing a phrase from the response : we should add the new phrase to the already existing Replica
//
//
//                            }
//                        }

                        buddyGPTApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL,type);
                    }
                    else if (type.equals("timeOutExpired")){
                        buddyGPTApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL,type);
                    }
//                    else if (type.equals("commande")) {
//                        buddyGPTApplication.setAlreadyGetAnswer( true );
//                        if (!buddyGPTApplication.getMessageError() && !buddyGPTApplication.getUsingEmotions()) {
//                            BuddySDK.UI.setFacialExpression( FacialExpression.NEUTRAL, 1 );
//                        }
//
//                        if (settingClass.getSwitchVisibility().equals( "true" )) {
//                            if (buddyGPTApplication.getCurrentLanguage().equals( "en" )) {
//                                buddy_texte_resp.setText( String.format( "Response :  %s ", texte ) );
//                            } else if (buddyGPTApplication.getCurrentLanguage().equals( "fr" )) {
//                                buddy_texte_resp.setText( String.format( "Réponse :  %s ", texte ) );
//                            } else if (buddyGPTApplication.getCurrentLanguage().equals( "de" )) {
//                                buddy_texte_resp.setText( String.format( "Antwort :  %s ", texte ) );
//                            } else if (buddyGPTApplication.getCurrentLanguage().equals( "es" )) {
//                                buddy_texte_resp.setText( String.format( "Respuesta :  %s ", texte ) );
//                            } else {
//                                buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate( "Response" ).addOnSuccessListener(new OnSuccessListener<String>() {
//                                    @Override
//                                    public void onSuccess(String translatedText) {
//                                        buddy_texte_resp.setText( String.format( translatedText + " :  %s ", texte ) );
//                                    }
//
//                                } ).addOnFailureListener( new OnFailureListener() {
//                                    @Override
//                                    public void onFailure(@NonNull Exception e) {
//                                        Log.e( TAG, "translatedText exception  " + e );
//                                    }
//                                } );
//
//                            }
//                            lyt_open_menu_settings.setVisibility(View.INVISIBLE);
//                            lyt_open_menu_chat.setVisibility(View.INVISIBLE);
//                            if(buddy_texte_qst_lyt.getVisibility() != View.VISIBLE) buddy_texte_resp_lyt.setTranslationY(-155);
//                            else buddy_texte_resp_lyt.setTranslationY(0);
//                            buddy_texte_resp_lyt.setVisibility(View.VISIBLE);
//                            buddy_texte_resp.setMovementMethod( new ScrollingMovementMethod() );
//                            buddy_texte_resp.scrollTo( 0, 0 );
//                        }
//
//                        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
//                        Replica reponse = new Replica();
//                        reponse.setValue(texte);
//                        reponse.setTime(time);
//                        long responseTime = buddyGPTApplication.getGetResponseTime()- buddyGPTApplication.getQuestionTime();
//                        DecimalFormat df = new DecimalFormat("#,###");
//                        String formattedTime= df.format(responseTime);
//                        reponse.setType("Response");
//                        reponse.setDuration(formattedTime + " ms");
//                        DecimalFormat decimalFormatter = new DecimalFormat("0.00");
//                        String formattedValue = decimalFormatter.format(Double.parseDouble(buddyGPTApplication.getparam("Total_cons")));
//                        reponse.setPrix(formattedValue+" $");
//                        listRep.add(reponse);
//                        ArrayList<Replica> ll = new ArrayList<>();
//                        for (int t = 0; t < listRep.size(); t++) {
//                            ll.add(listRep.get(t));
//                        }
//
//                        listRep.clear();
//
//                        buddyGPTApplication.speakTTS( texte, LabialExpression.SPEAK_NEUTRAL, type );
//                    }
//                    else if(type.equals("INVITATION")){
//                        buddyGPTApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL,type);
//                    }
                }
            }
        });
    }


    /**
     * ----------------------------------------- Tracking ---------------------------------------
     */

    private IInvitationCallback iInvitationCallback;
    public interface IInvitationCallback {
        void onEnd(String s);
    }

    private enum StateTrackingListening {
        NONE,
        PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT,
        PERSON_IS_VISIBLE_BUT_IS_NOT_LOOKING_AT_CAMERA_TIMEOUT,
        PERSON_IS_NOT_VISIBLE_TIMEOUT
    }
    private StateTrackingListening currentTrackingListeningState = StateTrackingListening.NONE;

    private enum StateTrackingWelcome {
        NONE,
        PERSON_IS_NOT_VISIBLE_TIMEOUT,
        PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT
    }
    private StateTrackingWelcome currentTrackingWelcomeState = StateTrackingWelcome.NONE;

    private Handler handlerCheckPersonDetection = new Handler();
    private Runnable runnableCheckPersonDetection = new Runnable() {
        public void run() {

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
                        if (totalTimeLookingAtCamera  >= TRACKING_DELAY_START_LISTEN * 1000L) {
                            if (currentTrackingListeningState != StateTrackingListening.PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT) {
                                currentTrackingListeningState = StateTrackingListening.PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT;
                                Log.w(TAG_TRACKING, "A person has been looking directly at the camera for TRACKING_DELAY_START_LISTEN="+TRACKING_DELAY_START_LISTEN+" seconds (or more) --> start listening");
                                if (!isFirstInvitaion && Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Invitation"))){
                                    startListeningQuestion();
                                }
                                else if (!Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Invitation"))){
                                    startListeningQuestion();
                                }
                                Log.w(TAG_TRACKING, "isFirstInvitaion= "+isFirstInvitaion);
                            }
                        }
                    }
                    else {
                        Log.w(TAG_TRACKING_DEBUG, "A person is visible again BUT is not looking at the CAMERA");
                        if (currentTime - lastLookingAtCameraTime >= TRACKING_DELAY_STOP_LISTEN * 1000L) {
                            if (currentTrackingListeningState != StateTrackingListening.PERSON_IS_VISIBLE_BUT_IS_NOT_LOOKING_AT_CAMERA_TIMEOUT) {
                                currentTrackingListeningState = StateTrackingListening.PERSON_IS_VISIBLE_BUT_IS_NOT_LOOKING_AT_CAMERA_TIMEOUT;
                                Log.w(TAG_TRACKING, "No person has been looking directly at the camera for TRACKING_DELAY_STOP_LISTEN="+TRACKING_DELAY_STOP_LISTEN+" seconds (or more) --> stop listening");
                                stopListeningEverything();
                            }
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


            //#region Invitation
            if(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Invitation"))){
                /**
                 * S’il n’a pas vu de personnes depuis (TRACKING_DELAY_WELCOME minutes)
                 * et qu’il détecte qu'une personne le regarde pendant (TRACKING_DURATION_WELCOME secondes),
                 * alors il prononce une invitation
                 */
                if (!isPersonDetected && currentTime - lastVisibleTime_saved >= TRACKING_DELAY_WELCOME * 60L * 1000L) {
                    if (currentTrackingWelcomeState != StateTrackingWelcome.PERSON_IS_NOT_VISIBLE_TIMEOUT) {
                        currentTrackingWelcomeState = StateTrackingWelcome.PERSON_IS_NOT_VISIBLE_TIMEOUT;
                        Log.w(TAG_TRACKING, "No person has been visible for TRACKING_DELAY_WELCOME="+TRACKING_DELAY_WELCOME+" minutes (or more)");
                        sendInvitationPending = true;
                    }
                }
                if (sendInvitationPending && isPersonDetected && regarde_camera) {
                    if (totalTimeLookingAtCamera >= TRACKING_DURATION_WELCOME * 1000L) {
                        if (currentTrackingWelcomeState != StateTrackingWelcome.PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT) {
                            currentTrackingWelcomeState = StateTrackingWelcome.PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT;
                            Log.w(TAG_TRACKING, "A person has been looking directly at the camera for TRACKING_DURATION_WELCOME="+TRACKING_DURATION_WELCOME+" seconds (or more) --> Invitation");
                            sendInvitationPending = false;
                            if(!buddyGPTApplication.isAlreadyChatting()){
                                stopListeningFreeSpeech();
                                buddyGPTApplication.setStartRecording(false);
                                buddyGPTApplication.setSpeaking(false);
                                try {
                                    BuddySDK.UI.stopListenAnimation();
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }
//                                invitation(null);
                                if (isFirstLaunch && isFirstInvitaion) {
                                    isFirstInvitaion = false;
                                }
                                    invitation(new IInvitationCallback() {
                                        @Override
                                        public void onEnd(String s) {
                                            Log.e(TAG_TRACKING, "Invitation onEnd Callback : "+s);
                                            iInvitationCallback = null;
                                            if(Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Auto_Listen"))){
                                                startListeningQuestion();
                                            }
                                            else{
                                                buddyGPTApplication.setAlreadyChatting(false);
                                                buddyGPTApplication.startListeningHotwor(MainActivity.this);
                                            }

                                        }
                                    });

                            }
                            else{
                                Log.w(TAG_TRACKING, "Do not say invitation because the person is already chatting");
                            }
                        }
                    }
                }
            }
            //#endregion Invitation


            //#region re-tracking, re-centering gaze and head
            if (isPersonDetected && !regarde_camera && currentTime - lastLookingAtCameraTime >= TRACKING_DELAY_NO_WATCH * 1000L) {
                Log.w(TAG_TRACKING, "No person has been looking directly at the camera for TRACKING_DELAY_NO_WATCH=" + TRACKING_DELAY_NO_WATCH + " seconds --> re-tracking + re-centering the gaze and head");
                re_track_and_center_head_and_gaze();
            }
            else if (!isPersonDetected && currentTime - lastVisibleTime >= TRACKING_DELAY_NO_TRACK * 1000L) {
                Log.w(TAG_TRACKING, "No person has been visible for TRACKING_DELAY_NO_TRACK=" + TRACKING_DELAY_NO_TRACK + " seconds --> re-tracking + re-centering the gaze and head");
                re_track_and_center_head_and_gaze();
            }

            if (isPersonDetected && !regarde_camera && currentTime - lastLookingAtCameraTime >= TRACKING_REGARD_CENTER * 1000L) {
                Log.w(TAG_TRACKING, "No person has been looking directly at the camera for TRACKING_REGARD_CENTER=" + TRACKING_REGARD_CENTER + " seconds --> refocus the pupils");
                poseTracking.lookAtCenter();
            }
            //#endregion re-tracking, re-centering gaze and head

            //#region Timer to exit the application
            if ( TRACKING_TIMEOUT!=0 && !isPersonDetected && currentTime - lastVisibleTime_saved >= TRACKING_TIMEOUT * 1000L){
                finishAffinity();
                System.exit(0);
            }

            //#endregion Timer to exit the application

        }
    };

//    private void initTracking(){
//        Log.d(TAG_TRACKING, "initTracking(isReTrack="+isReTrack+")");
//
//        if(!isFirstLaunch && !isReTrack){
//            try{
//                if(BuddySDK.Actuators.getLeftWheelStatus().toUpperCase().contains("DISABLE") || BuddySDK.Actuators.getRightWheelStatus().toUpperCase().contains("DISABLE")) {
//                    BuddySDK.USB.enableWheels(true, iUsbCommadRspTracking);
//                }
//                if(BuddySDK.Actuators.getYesStatus().toUpperCase().contains("DISABLE")) {
//                    BuddySDK.USB.enableYesMove(true, iUsbCommadRspTracking);
//                }
//                if(BuddySDK.Actuators.getNoStatus().toUpperCase().contains("DISABLE")) {
//                    BuddySDK.USB.enableNoMove(true, iUsbCommadRspTracking);
//                }
//            }
//            catch (Exception e){
//                e.printStackTrace();
//            }
//        }
//
//        if(!isReTrack){
//            //récupération des paramètres TRACKING du fichier de config:
//            TRACKING_WATCH = buddyGPTApplication.getParamFromFile("TRACKING_watch", "BuddyGPT.properties");
//            TRACKING_DELAY_NO_WATCH = Integer.parseInt(buddyGPTApplication.getParamFromFile("TRACKING_delay_nowatch", "BuddyGPT.properties"));
//            TRACKING_DELAY_NO_TRACK = Integer.parseInt(buddyGPTApplication.getParamFromFile("TRACKING_delay_notrack", "BuddyGPT.properties"));
//            TRACKING_DELAY_START_LISTEN = Integer.parseInt(buddyGPTApplication.getParamFromFile("TRACKING_delay_startlisten", "BuddyGPT.properties"));
//            TRACKING_DELAY_STOP_LISTEN = Integer.parseInt(buddyGPTApplication.getParamFromFile("TRACKING_delay_stoplisten", "BuddyGPT.properties"));
//            TRACKING_REGARD_CENTER = Integer.parseInt(buddyGPTApplication.getParamFromFile("TRACKING_regard_center", "BuddyGPT.properties"));
//            TRACKING_DELAY_WELCOME = Integer.parseInt(buddyGPTApplication.getParamFromFile("TRACKING_delay_welcome", "BuddyGPT.properties"));
//            TRACKING_DURATION_WELCOME = Integer.parseInt(buddyGPTApplication.getParamFromFile("TRACKING_duration_welcome", "BuddyGPT.properties"));
//            TRACKING_WELCOME_FR = buddyGPTApplication.getParamFromFile("TRACKING_welcome_FR", "BuddyGPT.properties");
//            TRACKING_WELCOME_EN = buddyGPTApplication.getParamFromFile("TRACKING_welcome_EN", "BuddyGPT.properties");
////            TRACKING_WELCOME_MODEL = buddyGPTApplication.getParamFromFile("TRACKING_welcome_model", "BuddyGPT.properties");
////            TRACKING_WELCOME_TEMPERATURE = Double.parseDouble(buddyGPTApplication.getParamFromFile("TRACKING_welcome_temperature", "BuddyGPT.properties"));
////            TRACKING_WELCOME_PROMPT_FR = buddyGPTApplication.getParamFromFile("TRACKING_welcome_prompt_FR", "BuddyGPT.properties");
////            TRACKING_WELCOME_PROMPT_EN = buddyGPTApplication.getParamFromFile("TRACKING_welcome_prompt_EN", "BuddyGPT.properties");
////            TRACKING_WELCOME_MAX_TOKEN = Integer.parseInt(buddyGPTApplication.getParamFromFile("TRACKING_welcome_maxtoken", "BuddyGPT.properties"));
//            try {
//                TRACKING_TIMEOUT=Integer.parseInt(buddyGPTApplication.getParamFromFile("TRACKING_timeout","BuddyGPT.properties"));
//            }
//            catch (Exception e){
//                TRACKING_TIMEOUT=0;
//            }
//
//        }
//
//        if(isFirstLaunch && !isReTrack && Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Invitation"))){
//            sendInvitationPending = true;
//            isFirstInvitaion = true;
//            startTracking();
//        }
//        else {
//            startTracking();
//        }
//    }

    private void startTracking(){
        Log.d(TAG_TRACKING, "startTracking(isReTrack="+isReTrack+")");

        poseTracking= new PoseTracking();
        if(backgroundExecutor != null) backgroundExecutor.shutdownNow();
        backgroundExecutor = Executors.newSingleThreadExecutor();
        cameraSelector = new CameraSelector.Builder().requireLensFacing(cameraFacing).build();
        viewModel = new ViewModelProvider(MainActivity.this).get(MainViewModel.class);
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
                cameraProvider = ProcessCameraProvider.getInstance(this).get();
                preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build();
                imageAnalyzer = new ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();
                imageAnalyzer.setAnalyzer(backgroundExecutor, this::detectPose);
                cameraProvider.unbindAll();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);
                Log.i(TAG, "Camera bound successfully");
            } catch (Exception e) {
                Log.e(TAG, "Camera binding failed", e);
            }
        });
        backgroundExecutor.execute(() -> {
            Context context = this;
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
                        public void onError(String error, int errorCode) {}
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
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Camera_Display"))) {
                                            reGroup.setTranslationY(0);

                                        } else {
                                            reGroup.setTranslationY(1000);
                                        }
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

                                //Log.i(TAG_TRACKING_DEBUG, "regarde_camera : "+regarde_camera);
                                //Log.i(TAG_TRACKING_DEBUG, "direction : "+direction);
                                //Log.i(TAG_TRACKING_DEBUG, "deFace : "+deFace);
                                //Log.i(TAG_TRACKING_DEBUG, "directionRegardNez : "+directionRegardNez);
                                //Log.i(TAG_TRACKING_DEBUG, "isPersonDetected : "+ isPersonDetected);

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
                                    poseTracking.look_at(degx, degy);

                                    if (Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Body")) || Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Head"))) {
                                        if (TRACKING_WATCH.trim().equalsIgnoreCase("Yes")) {
                                            if (regarde_camera) {
                                                poseTracking.Rotation(degx, Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Body")));
                                            }
                                        }
                                        else {
                                            poseTracking.Rotation(degx, Boolean.parseBoolean(buddyGPTApplication.getparam("Tracking_Body")));
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

                                runOnUiThread(() -> {
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
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        reGroup.setTranslationY(0);
                                    }
                                });
                            }
                        }
                    }
            );
        });
    }
    private void detectPose(ImageProxy imageProxy) {
        poseLandmarkerHelper.detectLiveStream(imageProxy);
    }

    private void re_track_and_center_head_and_gaze(){
        Log.d(TAG_TRACKING, "re_track_and_center_head_and_gaze()");
        isProcessingReTrack = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stopTracking();
                poseTracking.lookAtCenter();
                poseTracking.centerHead();
                isReTrack = true;
             //   initTracking();
            }
        });
    }

    private void stopTracking(){
        reGroup.setTranslationY(1000);
        cameraProvider.unbindAll();
        handlerCheckPersonDetection.removeCallbacks(runnableCheckPersonDetection);
        handlerCheckPersonDetection.removeCallbacksAndMessages(null);
        poseTracking.stopMovingAndCancelRunnables();
    }

    private void startListeningQuestion(){
        Log.d(TAG_TRACKING, "startListeningQuestion()");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    isSpeaking =false;
                    if(iInvitationCallback != null) iInvitationCallback.onEnd("INVITATION_END");
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
                    if (!buddyGPTApplication.getSpeaking() && !mlKitIsDownloading){
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
        runOnUiThread( new Runnable() {
            @Override
            public void run() {
                try{
                    isSpeaking =false;
                    if(iInvitationCallback != null) iInvitationCallback.onEnd("INVITATION_END");
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
                    if (buddyGPTApplication.getSpeaking() && !mlKitIsDownloading) {
                        if (buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Android")
                                || buddyGPTApplication.getparam("STT").trim().equalsIgnoreCase("Cerence")
                                || !buddyGPTApplication.getAppIsListeningToTheQuestion()) {
                            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                            buddyGPTApplication.setStartRecording(false);
                            buddyGPTApplication.setSpeaking(false);
                            buddyGPTApplication.setActivityClosed(true);
                            isListeningFreeSpeech = false;
                            buddyGPTApplication.stopTTS();
                            buddyGPTApplication.setStoredResponse("");
                            if (buddy_texte_qst_lyt != null && buddy_texte_resp_lyt != null && buddy_texte_qst != null && buddy_texte_resp != null) {
                                buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                                buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                                buddy_texte_qst.setMovementMethod(null);
                                buddy_texte_resp.setMovementMethod(null);
                            }
                            lyt_open_menu_settings.setVisibility(View.VISIBLE);
                            lyt_open_menu_chat.setVisibility(View.VISIBLE);
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
                            buddyGPTApplication.traitementAudio(false);
                        }
                    }
                }
                catch (Exception e){
                    Log.e(TAG,"Exception  "+e);
                    e.printStackTrace();
                }
            }
        } );
    }

    private void invitation(IInvitationCallback iInvitationCallback){
        Log.d(TAG_TRACKING, "invitation()");
        this.iInvitationCallback = iInvitationCallback;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                    //Get invitation from config File
                    if (buddyGPTApplication.getLangue().getNom().equals("Anglais")) {
                        if(TRACKING_WELCOME_EN != null && !TRACKING_WELCOME_EN.isEmpty()){
                            String[] englishInvitations = TRACKING_WELCOME_EN.substring(1, TRACKING_WELCOME_EN.length() - 1).split("/");
                            String randomInvitationEN = englishInvitations[random.nextInt(englishInvitations.length)];
                            Log.d(TAG_TRACKING, "Random English Invitation: " + randomInvitationEN);
                            buddyGPTApplication.setActivityClosed(false);
                            speak(randomInvitationEN, "INVITATION");
                        }
                        else {
                            if(iInvitationCallback != null) iInvitationCallback.onEnd("ConfigFile do not contain English Invitation");
                        }
                    }
                    else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                        if(TRACKING_WELCOME_FR != null && !TRACKING_WELCOME_FR.isEmpty()){
                            String[] frenchInvitations = TRACKING_WELCOME_FR.substring(1, TRACKING_WELCOME_FR.length() - 1).split("/");
                            String randomInvitationFR = frenchInvitations[random.nextInt(frenchInvitations.length)];
                            Log.d(TAG_TRACKING, "Random French Invitation: " + randomInvitationFR);
                            buddyGPTApplication.setActivityClosed(false);
                            speak(randomInvitationFR, "INVITATION");
                        }
                        else {
                            if(iInvitationCallback != null) iInvitationCallback.onEnd("ConfigFile do not contain French Invitation");
                        }
                    }
                    else {
                        if(TRACKING_WELCOME_EN != null && !TRACKING_WELCOME_EN.isEmpty()){
                            String[] englishInvitations = TRACKING_WELCOME_EN.substring(1, TRACKING_WELCOME_EN.length() - 1).split("/");
                            String randomInvitationEN = englishInvitations[random.nextInt(englishInvitations.length)];
                            buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(randomInvitationEN)
                                    .addOnSuccessListener(new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String translatedText) {
                                            Log.d(TAG_TRACKING, "Translated Invitation: " + translatedText);
                                            buddyGPTApplication.setActivityClosed(false);
                                            speak(translatedText, "INVITATION");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d(TAG_TRACKING, "Translation failed, using English Invitation: " + randomInvitationEN);
                                            buddyGPTApplication.setActivityClosed(false);
                                            speak(randomInvitationEN, "INVITATION");
                                        }
                                    });
                        }
                        else {
                            if(iInvitationCallback != null) iInvitationCallback.onEnd("ConfigFile do not contain English Invitation");
                        }
                    }

            }
        });
    }


    /**
     *   -------------------------------  Gestion des permissions  ---------------------------------------------------------------
     */

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        return true;
    }

    private boolean checkPermission(@NonNull int[] grantResults){
        return grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                grantResults[1] != PackageManager.PERMISSION_GRANTED ||
                grantResults[2] != PackageManager.PERMISSION_GRANTED ||
                grantResults[3] != PackageManager.PERMISSION_GRANTED ||
                grantResults[4] != PackageManager.PERMISSION_GRANTED;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID && checkPermission(grantResults)) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Need permissions " + Manifest.permission.READ_EXTERNAL_STORAGE +
                                    "/" + Manifest.permission.WRITE_EXTERNAL_STORAGE +
                                    "/" + Manifest.permission.RECORD_AUDIO +
                                    "/" + Manifest.permission.CAMERA +
                                    "/" + Manifest.permission.READ_PHONE_STATE
                            , Toast.LENGTH_LONG).show();
                }
            });
            /*
             * Terminer l'activité si l'utilisateur n'a pas activé une autorisation
             */
            finish();
            return;
        }
        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException(e);
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