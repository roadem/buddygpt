package com.robotique.aevaweb.buddygpt.utilis;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;

import com.robotique.aevaweb.buddygpt.application.BuddyGPTApplication;


/*
 * cette classe permet d"écouter les changements du volume
 */
public class SettingsContentObserver extends ContentObserver {
    public Context context;
    public BuddyGPTApplication app;
    public SettingsContentObserver(Handler handler ,Context cntx) {
        super(handler);
        this.context = cntx;
    }



    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        app = (BuddyGPTApplication) context;
        /*
         * Notifier le changement de volume
         */
        app.notifyObservers("changeDetected");
    }

}