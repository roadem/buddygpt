package com.robotique.aevaweb.buddygpt.activities;

import static com.bfr.buddysdk.BuddySDK.USB;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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
import com.robotique.aevaweb.buddygpt.fragments.ChatFragment;
import com.robotique.aevaweb.buddygpt.fragments.MainFragment;
import com.robotique.aevaweb.buddygpt.fragments.SettingsFragment;
import com.robotique.aevaweb.buddygpt.observers.IDBObserver;
import com.robotique.aevaweb.buddygpt.utilis.CustomToast;
import com.robotique.aevaweb.buddygpt.utilis.WifiBroadcastReceiver;
import com.robotique.aevaweb.buddygpt.utilis.tracking.PoseTracking;

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
    private boolean onSdkReadyIsAlreadyCalledOnce = false;
    private PoseTracking poseTracking;
    private ExecutorService backgroundExecutor;
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
        buddyGPTApplication.setparam("session_id", "");
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);

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
        buddyGPTApplication.stopTTS();
        buddyGPTApplication.setActivityClosed(true);
        CustomToast.getInstance().hideToast();
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
        } catch (Exception e) {
            Log.e(TAG, "BuddySDK Exception  " + e);
        }
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
        if(!onSdkReadyIsAlreadyCalledOnce){
            //initialisation du visage de Buddy
            BuddySDK.UI.setFaceEnergy(1.0f);
            BuddySDK.UI.setFacePositivity(1.0f);
            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
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
                    checkSelfPermission(REQUESTED_PERMISSIONS[3], PERMISSION_REQ_ID)

            ){
                init();
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
            Log.i(TAG, "mainactivity initOrMajOrNone "+message.split(";SPLIT;")[1] );

            buddyGPTApplication.setNotYet(false);

            runOnUiThread(() -> {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

                if (current instanceof SettingsFragment) {
                    Log.i(TAG, "update: properties file done but SettingsFragment is active -> skip replace");
                    return;
                }
                if (current instanceof ChatFragment) {
                    Log.i(TAG, "update: properties file done but ChatFragment is active -> skip replace");
                    return;
                }
                if (!isFinishing() && !getSupportFragmentManager().isStateSaved()) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, MainFragment.newInstance(initOrMajOrNone))
                            .commit();
                } else {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, MainFragment.newInstance(initOrMajOrNone))
                            .commitAllowingStateLoss();
                }
            });
        }


    }

    /**
     * ----------------- Utils ---------------------------
     */

    private void init() {
        Log.e(TAG, "init() ");
        initOrMajOrNone = buddyGPTApplication.createPropertiesFile();
        Log.i(TAG, "init: isFirstLaunch " + isFirstLaunch);




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
        init();
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