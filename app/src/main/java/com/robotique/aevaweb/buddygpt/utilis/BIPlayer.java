package com.robotique.aevaweb.buddygpt.utilis;

import android.util.Log;

import com.bfr.buddysdk.services.companion.Task;


public class BIPlayer {

    private static final String TAG = "BuddyGPT_BIPlayer";
    // private static instance variable to hold the singleton instance
    private static BIPlayer instance = new BIPlayer();
    private Task biTask = null;

    // private constructor to prevent instantiation of the class
    private BIPlayer() {
    }

    // public static method to retrieve the singleton instance
    public static BIPlayer getInstance() {
        // Check if the instance is already created
        if (instance == null) {
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

    public void stopBehaviour() {
        Log.i(TAG, "stopBI");
        if (biTask != null) {
            try {
                biTask.stop();
                biTask = null;
            } catch (Exception e) {
                Log.e(TAG, "Exception lors de l'arrêt de lecture du BI : " + e);
            }
        }
    }
}
