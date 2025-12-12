package com.robotique.aevaweb.buddygpt.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.GazePosition;
import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddy.utils.events.EventItem;
import com.bfr.buddy.utils.values.FloatingWidgetVisibility;
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

public class MainActivity extends BuddyCompatActivity implements IDBObserver {

    private static final String TAG = "BuddyGPT_MainActivity";
    private static final String TAG_TRACKING = "BuddyGPT_TRACKING_INFO";
    private static final String[] REQUESTED_PERMISSIONS = new String[] {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE
    };
    private static final int PERMISSION_REQ_ID = 22;
    private final WifiBroadcastReceiver wifiBroadCastReceiver = new WifiBroadcastReceiver();
    private BuddyGPTApplication buddyGPTApplication;
    private View decorView;
    private boolean onSdkReadyExecuted = false; //
    private String propertiesStatus = ""; // Renommé initOrMajOrNone pour clarté
    private RelativeLayout viewFace;
    private com.bfr.buddy.vision.shared.IVisionRsp.Stub stopCameraCallback;

    /**
     * ------------------ App LifeCycle ---------------------
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "--- onCreate() ---");

        buddyGPTApplication = (BuddyGPTApplication) getApplicationContext();
        buddyGPTApplication.registerObserver(this);

        // 1. Initialisation de l'UI et de l'état système
        setupSystemUI();

        // 2. Initialisation de l'état global de l'application (Déléguer cette tâche à BuddyGPTApplication)
        initializeApplicationState();

        // 3. Muet les notifications (pour ne pas interférer avec la TTS/STT)
        muteNotifications();
    }

    // Nouvelle méthode pour encapsuler la logique d'initialisation de l'UI système
    private void setupSystemUI() {
        // Cacher la barre système pour une expérience plein écran
        buddyGPTApplication.hideSystemUI(this);
        decorView = getWindow().getDecorView();

        // Gérer la réapparition de la barre système (très bien de le garder)
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            if (visibility == 0) {
                decorView.setSystemUiVisibility(buddyGPTApplication.hideSystemUI(MainActivity.this));
            }
        });
    }

    // Méthode pour encapsuler la réinitialisation de l'état de l'Application
    private void initializeApplicationState() {
        viewFace = findViewById(R.id.view_face);
        buddyGPTApplication.setparam("session_id", "");
        buddyGPTApplication.setparam("TeamGPT_ID_Device", "");
        buddyGPTApplication.setSpeaking(false);
        buddyGPTApplication.setNotYet(true);
        buddyGPTApplication.setActivityClosed(false);
        buddyGPTApplication.setStartRecording(false);
        buddyGPTApplication.setAlreadyGetAnswer(false);
        buddyGPTApplication.setTimeoutExpired(false);
        buddyGPTApplication.setMessageError(false);
        buddyGPTApplication.setInitSharedpreferences(true);
        buddyGPTApplication.setLanguageDetected("");
        buddyGPTApplication.setAnswerHasExceededTimeOut(false);
        buddyGPTApplication.setAppIsListeningToTheQuestion(false);
        buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
        buddyGPTApplication.setBIExecution(false);
        buddyGPTApplication.setAlreadyChatting(false);
        // La réinitialisation des compteurs et temps est souvent faite au début d'une nouvelle session.
        buddyGPTApplication.setQuestionNumber(0);
        buddyGPTApplication.setCurrentQuestionNubmer(0);
        buddyGPTApplication.setQuestionTime(0);
        buddyGPTApplication.setResponseTime(0);
        buddyGPTApplication.setStoredResponse("");

        Log.i(TAG_TRACKING, "First launch of application");
    }

    private void muteNotifications() {
        // Mettre en sourdine le flux de notification (pertinent pour la robotique)
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            am.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "--- onResume() ---");

        // Réactiver l'activity
        buddyGPTApplication.setActivityClosed(false);
        Log.i(TAG, "onResume: Activity is ACTIVE again");

        // Réinitialiser uniquement l'état 'en cours de traitement'
        buddyGPTApplication.setAppIsCurrentlyDealingWithTheQuestion(false);

        // Initialiser ou démarrer Python (très bien de le garder ici)
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        // Assurer que l'UI système est cachée
        buddyGPTApplication.hideSystemUI(this);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "--- onPause() ---");

        // Arrêt des processus pour économiser les ressources et éviter les fuites
        if (buddyGPTApplication.getResponseFromTeamGPT() != null) {
            buddyGPTApplication.getResponseFromTeamGPT().reset();
        }

        buddyGPTApplication.stopTTS();
        buddyGPTApplication.setActivityClosed(true);
        CustomToast.getInstance().hideToast();

        // Réinitialiser l'expression labiale
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
        } catch (Exception e) {
            Log.e(TAG, "BuddySDK Exception during onPause: " + e.getMessage());
        }
        if (buddyGPTApplication != null) {
            buddyGPTApplication.cleanup();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "--- onDestroy() ---");
        Log.w(TAG, "Activity is CLOSING, disabling SDK callbacks");
        // Annuler le callback stopCamera IMMÉDIATEMENT
        if (stopCameraCallback != null) {
            try {
                // Essayer d'annuler le callback en appelant stopCamera avec null
                BuddySDK.Vision.stopCamera(0, null);
                Log.i(TAG_TRACKING, "onDestroy: stopCamera callback cancelled");
            } catch (Exception e) {
                Log.e(TAG, "onDestroy: Error cancelling stopCamera callback: " + e.getMessage());
            }
            stopCameraCallback = null;
        }
        // Marquer que l'activity est fermée AVANT le nettoyage
        buddyGPTApplication.setActivityClosed(true);

        // Nettoyage des Broadcasters
        try {
            unregisterReceiver(wifiBroadCastReceiver);
        } catch (IllegalArgumentException e) {
            Log.i(TAG, "--- unregisterReceiver wifiBroadcast:: IllegalArgumentException ---" + e.getMessage());
        }

        // Nettoyage de l'état de l'application
        buddyGPTApplication.setparam("firstLaunch", "true");
        if (buddyGPTApplication.getDialog() != null && buddyGPTApplication.getDialog().isShowing()) {
            buddyGPTApplication.getDialog().dismiss();
        }
        buddyGPTApplication.setFileCreate(true);
        buddyGPTApplication.notifyObservers("main destroy");
        buddyGPTApplication.removeObserver(this);

        // Cleanup COMPLET
        if (buddyGPTApplication != null) {
            buddyGPTApplication.cleanup();
        }

        super.onDestroy();
        Log.w(TAG, "onDestroy() COMPLETE - Activity is CLOSED");
    }

    @Override
    public void onSDKReady() {
        Log.w(TAG, "onSDKReady "+onSdkReadyExecuted);
        if (!onSdkReadyExecuted) {
            //  GUARD : Ne pas exécuter si l'activity est fermée
            if (buddyGPTApplication.isActivityClosed()) {
                Log.w(TAG, "❌ onSDKReady called but Activity is CLOSED, IGNORING");
                return;
            }
            Log.i(TAG, "onSDKReady: Executing SDK initialization");

            // Configuration de l'état initial du robot (Visuals)
            configureBuddyVisuals();

            // Désactivation des triggers vocaux/visuels standards
            disableBuddyTriggers();
            // Créer un callback qu'on peut stocker et contrôler
            stopCameraCallback = new com.bfr.buddy.vision.shared.IVisionRsp.Stub() {
                @Override
                public void onSuccess(String s) {
                    // ✅ GUARD : Ne pas logger si l'activity est fermée
                    if (!buddyGPTApplication.isActivityClosed()) {
                        Log.i(TAG_TRACKING, "stopCamera(0) onSuccess : " + s);
                    }
                }

                @Override
                public void onFailed(String s) {
                    // ✅ GUARD : Ne pas logger si l'activity est fermée
                    if (!buddyGPTApplication.isActivityClosed()) {
                        Log.e(TAG_TRACKING, "stopCamera(0) onFailed : " + s);
                    }
                }
            };

            // Arrêter la caméra 0 avec le callback stocké
            BuddySDK.Vision.stopCamera(0, stopCameraCallback);

            // Vérification des permissions et initialisation après succès
            if (arePermissionsGranted()) {
                init();
            }

            onSdkReadyExecuted = true;  // ← Marquer comme exécuté
        } else {
            Log.w(TAG, "onSDKReady: Already executed, skipping");
        }
    }

    // configurer l'état visuel initial de Buddy
    private void configureBuddyVisuals() {
        BuddySDK.UI.setViewAsFace(viewFace);
        BuddySDK.UI.setFaceEnergy(1.0f);
        BuddySDK.UI.setFacePositivity(1.0f);
        BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
        BuddySDK.UI.lookAt(GazePosition.CENTER, true);
        BuddySDK.UI.stopListenAnimation();
        BuddySDK.UI.setMenuWidgetVisibility(FloatingWidgetVisibility.ALWAYS);
        BuddySDK.UI.setCloseWidgetVisibility(FloatingWidgetVisibility.ALWAYS);
    }

    // désactiver les triggers standards
    private void disableBuddyTriggers() {
        // Désactiver le trigger Companion de la bouche et de OK BUDDY
        BuddySDK.Companion.raiseEvent("disableOkBuddy");
        BuddySDK.Companion.raiseEvent("disableOnMouth");
    }

    // vérification des permissions (simplification)
    private boolean arePermissionsGranted() {
        for (String permission : REQUESTED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // Si une permission manque, demander toutes les permissions
                ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onEvent(EventItem iEvent) {
        Log.w(TAG, "onEvent : " + iEvent.toString());
        // Pas de changement, c'est un callback standard.
    }

    /**
     * ----------------- Gestion de notifications ---------------------------
     */
    @Override
    public void update(String message) {
        if (message != null && message.startsWith("properties file done")) {
            Log.i(TAG, "update: properties file done");

            // Extraction du statut (init/Maj/None)
            String[] parts = message.split(";SPLIT;");
            if (parts.length > 1) {
                propertiesStatus = parts[1];
            } else {
                propertiesStatus = "";
            }
            Log.i(TAG, "update: propertiesStatus " + propertiesStatus);

            buddyGPTApplication.setNotYet(false);

            // Mise à jour de l'interface utilisateur sur le thread principal
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing()) {
                    // Vérifier si nous devons remplacer le fragment
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

                    if (current instanceof SettingsFragment || current instanceof ChatFragment) {
                        Log.i(TAG, "update: properties file done but specific Fragment is active -> skip replace");
                        return; // Ne pas remplacer si l'utilisateur est dans ces fragments
                    }

                    // Utiliser commitAllowingStateLoss() par défaut pour éviter les crashs
                    // si l'état de l'Activity est déjà sauvegardé.
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, MainFragment.newInstance(propertiesStatus))
                            .commitAllowingStateLoss();
                }
            }, 1000); // Délai réduit à 1 seconde pour un démarrage plus rapide (était 2000 ms)
        }
        // Gérer d'autres messages si nécessaire
    }

    /**
     * ----------------- Utils ---------------------------
     */

    private void init() {
        Log.e(TAG, "init() - Starting configuration file creation/check");
        // Déléguer la création/vérification à l'objet Application.
        propertiesStatus = buddyGPTApplication.createPropertiesFile();
    }

    /**
     * ------------------------------- Gestion des permissions
     * ---------------------------------------------------------------
     */

    // La méthode checkSelfPermission a été fusionnée dans arePermissionsGranted pour la clarté.
    // L'ancienne méthode était redondante avec la nouvelle "arePermissionsGranted".

    // Vérifie si TOUTES les permissions requises ont été accordées.
    private boolean checkAllPermissionsGranted(@NonNull int[] grantResults) {
        if (grantResults.length != REQUESTED_PERMISSIONS.length) {
            return false;
        }
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQ_ID) {
            if (checkAllPermissionsGranted(grantResults)) {
                // Toutes les permissions accordées : procéder à l'initialisation
                init();
            } else {
                // Permissions manquantes : Afficher le Toast et terminer
                this.runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        getString(R.string.permission_required_message), // Utiliser une ressource String
                        Toast.LENGTH_LONG).show());
                finish();
            }
        }
    }

    /**
     * ------------------------------- Gestion d'affichage des barres du systemUI
     * ----------------------------------------------
     */

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            buddyGPTApplication.hideSystemUI(this);
        }
    }
}