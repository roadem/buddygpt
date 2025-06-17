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

    private Task biTask = null;

    // private static instance variable to hold the singleton instance
    private static BIPlayer instance = new BIPlayer();

    // private constructor to prevent instantiation of the class
    private BIPlayer() {}

    // public static method to retrieve the singleton instance
    public static BIPlayer getInstance() {
        // Check if the instance is already created
        if(instance == null) {
            // synchronize the block to ensure only one thread can execute at a time
            synchronized (BIPlayer.class) {
                // check again if the instance is already created
                    // create the singleton instance
                    instance = new BIPlayer();
                }
        }
        // return the singleton instance
        return instance;
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
