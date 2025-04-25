package com.robotique.aevaweb.buddygpt.utilis;

import android.app.Activity;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bfr.buddy.ui.shared.FacialEvent;
import com.bfr.buddy.ui.shared.IUIFaceAnimationCallback;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddysdk.services.companion.Task;
import com.bfr.buddysdk.services.companion.TaskCallback;
import com.google.android.exoplayer2.ui.PlayerView;
import com.robotique.aevaweb.buddygpt.R;
import com.robotique.aevaweb.buddygpt.application.BuddyGPTApplication;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;


public class BIPlayer {

    private static final String TAG = "BuddyGPT_BIPlayer";

    private BuddyGPTApplication buddyGPTApplication;
    private Task biTask = null;
    private Boolean biContainsEyesAnimation = false;

    // private static instance variable to hold the singleton instance
    private static BIPlayer INSTANCE = new BIPlayer();

    // private constructor to prevent instantiation of the class
    private BIPlayer() {}

    // public static method to retrieve the singleton instance
    public static BIPlayer getInstance() {
        // Check if the instance is already created
        if(INSTANCE == null) {
            // synchronize the block to ensure only one thread can execute at a time
            synchronized (BIPlayer.class) {
                // check again if the instance is already created
                if (INSTANCE == null) {
                    // create the singleton instance
                    INSTANCE = new BIPlayer();
                }
            }
        }
        // return the singleton instance
        return INSTANCE;
    }

    public void playBI(Activity activity, String behaviour,IBehaviourCallBack callBack){

        this.buddyGPTApplication = (BuddyGPTApplication) activity.getApplicationContext();

        ImageView bi_imageView = activity.findViewById(R.id.bi_imageView);
        PlayerView bi_videoView = activity.findViewById(R.id.bi_videoView);

        if(behaviour != null && !behaviour.isEmpty()){
            try {

                //vérifier si le BI contient une animation des yeux pour contourner le problème suivant :
                // le BI peut s'arrêter avant la fin de toutes les instructions (exemple : appel du stop() au milieu de l'execution),
                // dans ce cas il est possible que les yeux restent fermés ...
                // il faut appeler OPEN_EYES à la fin de l'execution du bi
                String stringToSearch = "EYES";
                try (Stream<String> stream = Files.lines(Paths.get("/storage/emulated/0/BI/Behaviour/"+behaviour))) {
                    Optional<String> lineHavingTarget = stream.filter(l -> l.contains(stringToSearch)).findFirst();
                    biContainsEyesAnimation = lineHavingTarget.isPresent();

                } catch (Exception e) {
                    Log.e(TAG, "Exception pendant la vérification du fichier BI: " + e);
                }

                biTask = BuddySDK.Companion.createBITask(behaviour, bi_videoView, bi_imageView,false);
                biTask.start(new TaskCallback() {
                    @Override
                    public void onStarted() {
                        Log.d(TAG, "[BI][TASK] onStarted");
                    }
                    @Override
                    public void onSuccess(@NonNull String s) {
                        Log.d(TAG, "[BI][TASK] onSuccess : "+s);
                        biTask = null;
                        if(biContainsEyesAnimation) {
                            BuddySDK.UI.playFacialEvent(FacialEvent.OPEN_EYES, 1, new IUIFaceAnimationCallback.Stub() {
                                @Override
                                public void onAnimationEnd(String s, String s1) throws RemoteException {
                                    if(callBack != null) callBack.onEnd(true,"SUCCESS");
                                }
                            });
                        }
                        else if(callBack != null) callBack.onEnd(true,"SUCCESS");
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "[BI][TASK] onCancel");
                        biTask = null;
                        if(biContainsEyesAnimation) {
                            BuddySDK.UI.playFacialEvent(FacialEvent.OPEN_EYES, 1, new IUIFaceAnimationCallback.Stub() {
                                @Override
                                public void onAnimationEnd(String s, String s1) throws RemoteException {
                                    if(callBack != null) callBack.onEnd(true,"SUCCESS");
                                }
                            });
                        }
                        else if(callBack != null) callBack.onEnd(true,"SUCCESS");

                    }

                    @Override
                    public void onError(@NonNull String s) {
                        Log.e(TAG, "[BI][TASK] onError : "+s);
                        biTask = null;
                        if(callBack != null) callBack.onEnd(false,"ERROR_TASK");

                    }

                    @Override
                    public void onIntermediateResult(@NonNull String s) {
                        Log.d(TAG, "[BI][TASK] onIntermediateResult :" + s);
                        if(callBack != null) callBack.onRun(s);
                    }
                });
            }
            catch (Exception e){
                Log.e(TAG, "Exception lors de la lecture du BI : " + e);
                biTask = null;
                if(callBack != null) callBack.onEnd(false,"ERROR_EXCEPTION");
            }
        }
        else{
            Log.w(TAG, "Fichier BI introuvable");
            biTask = null;
            if(callBack != null) callBack.onEnd(false, "ERROR_FILE_NOT_FOUND");
        }
    }
    public void stopBehaviour(){
        Log.i(TAG, "stopBI");
        if(biTask != null){
            try{
                biTask.stop();
                biTask = null;
            }
            catch (Exception e){
                Log.e(TAG, "Exception lors de l'arrêt de lecture du BI : " + e);
            }
        }
    }
}
