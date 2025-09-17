package com.robotique.aevaweb.buddygpt.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.GazePosition;
import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddy.utils.events.EventItem;
import com.bfr.buddy.utils.values.FloatingWidgetVisibility;
import com.bfr.buddy.vision.shared.IVisionRsp;
import com.bfr.buddysdk.BuddyCompatActivity;
import com.bfr.buddysdk.BuddySDK;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.robotique.aevaweb.buddygpt.R;
import com.robotique.aevaweb.buddygpt.application.BuddyGPTApplication;
import com.robotique.aevaweb.buddygpt.fragments.MainFragment;
import com.robotique.aevaweb.buddygpt.models.Replica;
import com.robotique.aevaweb.buddygpt.observers.IDBObserver;
import com.robotique.aevaweb.buddygpt.utilis.CustomToast;
import com.robotique.aevaweb.buddygpt.utilis.WifiBroadcastReceiver;
import com.robotique.aevaweb.buddygpt.utilis.tracking.PoseTracking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class MainActivity extends BuddyCompatActivity implements IDBObserver {

    private static final String TAG = "BuddyGPT_MainActivity";
    private static final String TAG_TRACKING = "BuddyGPT_TRACKING_INFO";
    private static final String[] REQUESTED_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE
    };
    private static final int PERMISSION_REQ_ID = 22;
    private final WifiBroadcastReceiver wifiBroadCastReceiver = new WifiBroadcastReceiver();
    private final boolean isFirstLaunch = true; // Used to init TeamGPT params only once
    private final Handler handlerTTSError = new Handler();
    private final Handler handler = new Handler();
    private BuddyGPTApplication buddyGPTApplication;
    private View decorView;
    //views
    private RelativeLayout viewFace;
    private RelativeLayout launchView;
    private RelativeLayout reGroup;
    private ImageView noNetwork;
    private ProgressBar downloadingBar;
    private boolean onSdkReadyIsAlreadyCalledOnce = false;
    private boolean isListeningFreeSpeech = false;
    private CountDownTimer responseTimeout;
    private ArrayList<Replica> listRep = new ArrayList<>();
    private PoseTracking poseTracking;
    private ExecutorService backgroundExecutor;
    private ProcessCameraProvider cameraProvider;
    private String initOrMajOrNone = "";
    private Handler handlerForSensor;
    private Runnable runnableForSensor;
    private Runnable runnableTTSError;
    private Runnable runnable;

    /**
     * ------------------ App LifeCycle ---------------------
     */

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, " --- onCreate() ---");
        buddyGPTApplication = (BuddyGPTApplication) getApplicationContext();
        buddyGPTApplication.registerObserver(this);
        buddyGPTApplication.setInitSharedpreferences(true);
        buddyGPTApplication.hideSystemUI(this);
        decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            if (visibility == 0) {
                decorView.setSystemUiVisibility(buddyGPTApplication.hideSystemUI(MainActivity.this));
            }
        });
        viewFace = findViewById(R.id.view_face);
        launchView = findViewById(R.id.launch_view);
        noNetwork = findViewById(R.id.noNetwork);
        downloadingBar = findViewById(R.id.progressBar_MLKitDownload);
        reGroup = findViewById(R.id.reGroup);

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);

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

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, " --- onResume() ---");
        buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        buddyGPTApplication.hideSystemUI(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, " --- onPause() ---");
        if (responseTimeout != null) responseTimeout.cancel();
        if (handlerTTSError != null && runnableTTSError != null) {
            handlerTTSError.removeCallbacks(runnableTTSError);
            handlerTTSError.removeCallbacksAndMessages(null);
        }
        if (buddyGPTApplication.getResponseFromTeamGPT() != null)
            buddyGPTApplication.getResponseFromTeamGPT().reset();
        if (handlerForSensor != null && runnableForSensor != null) {

            handlerForSensor.removeCallbacksAndMessages(null);
            handlerForSensor.removeCallbacks(runnableForSensor);

        }
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
            handler.removeCallbacksAndMessages(null);
        }
        onSdkReadyIsAlreadyCalledOnce = false;
        isListeningFreeSpeech = false;
        listRep = new ArrayList<>();
        buddyGPTApplication.stopTTS();
        buddyGPTApplication.setActivityClosed(true);
        CustomToast.getInstance().hideToast();
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
        } catch (Exception e) {
            Log.e(TAG, "BuddySDK Exception  " + e);
        }
        if (cameraProvider != null) cameraProvider.unbindAll();
    }

    @Override
    protected void onDestroy() {

        Log.d(TAG, " --- onDestroy() ---");
        try {
            unregisterReceiver(wifiBroadCastReceiver);
        } catch (IllegalArgumentException e) {
            Log.i(TAG, "---unregisterReceiver wifiBroadcast:: IllegalArgumentException---" + e.getMessage());
        }
        buddyGPTApplication.removeObserver(this);
        buddyGPTApplication.setparam("firstLaunch", "true");
        if (buddyGPTApplication.getDialog() != null && buddyGPTApplication.getDialog().isShowing())
            buddyGPTApplication.getDialog().dismiss();
        buddyGPTApplication.setFileCreate(true);
        buddyGPTApplication.notifyObservers("main destroy");
        if (poseTracking != null) poseTracking.stopMovingAndCancelRunnables();
        if (backgroundExecutor != null) backgroundExecutor.shutdownNow();

        super.onDestroy();
    }

    /**
     * ------------------ Register to the SDK callbacks ---------------------
     */
    @Override
    public void onSDKReady() {
        Log.w(TAG, "onSDKReady");
        if (!onSdkReadyIsAlreadyCalledOnce) {
            //initialisation du visage de Buddy
            BuddySDK.UI.setFaceEnergy(1.0f);
            BuddySDK.UI.setFacePositivity(1.0f);
            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
            BuddySDK.UI.lookAt(GazePosition.CENTER, true);
            BuddySDK.UI.stopListenAnimation();
            BuddySDK.UI.setViewAsFace(viewFace);
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

            ) {
                try {
                    init();
                } catch (IOException e) {
                    Log.i(TAG, "onSDKReady: " + e.getMessage());
                }
            }
        }
        onSdkReadyIsAlreadyCalledOnce = true;
    }

    @Override
    public void onEvent(EventItem iEvent) {
        Log.w(TAG, "onEvent : " + iEvent.toString());
    }

    /**
     * ----------------- Gestion de notifications ---------------------------
     */
    @Override
    public void update(String message) {
        if (message != null && message.contains("properties file done")) {
            Log.i(TAG, "update: properties file done");
            buddyGPTApplication.setNotYet(false);
            runOnUiThread(() -> getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MainFragment())
                    .commit());
        }


    }

    /**
     * ----------------- Utils ---------------------------
     */

    private void init() throws IOException {
        Log.e(TAG, "init() ");
        String imei = getIMEI();
        Log.i(TAG, "init: imei device " + imei);
        buddyGPTApplication.setImeiRobot(imei);
        initOrMajOrNone = buddyGPTApplication.createPropertiesFile();
        Log.i(TAG, "init: isFirstLaunch " + isFirstLaunch);
        if (isFirstLaunch) {
            buddyGPTApplication.initTeamGPTSettings();
            buddyGPTApplication.setparam("session_id", "");
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


    }

    public String getIMEI() {
        String imei = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For Android 8.0 and above
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (telephonyManager != null && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                imei = telephonyManager.getImei();
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
     * -------------------------------  Gestion des permissions  ---------------------------------------------------------------
     */

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        return true;
    }

    private boolean checkPermission(@NonNull int[] grantResults) {
        return grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                grantResults[1] != PackageManager.PERMISSION_GRANTED ||
                grantResults[2] != PackageManager.PERMISSION_GRANTED ||
                grantResults[3] != PackageManager.PERMISSION_GRANTED ||
                grantResults[4] != PackageManager.PERMISSION_GRANTED;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) throws RuntimeException {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID && checkPermission(grantResults)) {
            this.runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Need permissions " + Manifest.permission.READ_EXTERNAL_STORAGE +
                            "/" + Manifest.permission.WRITE_EXTERNAL_STORAGE +
                            "/" + Manifest.permission.RECORD_AUDIO +
                            "/" + Manifest.permission.CAMERA +
                            "/" + Manifest.permission.READ_PHONE_STATE
                    , Toast.LENGTH_LONG).show());
            /*
             * Terminer l'activité si l'utilisateur n'a pas activé une autorisation
             */
            finish();
            return;
        }
        try {
            init();
        } catch (IOException e) {
            Log.i(TAG, "onRequestPermissionsResult: " + e);
        }
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