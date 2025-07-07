package com.robotique.aevaweb.buddygpt.utilis;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;

import java.util.ArrayList;

public class LanguageDetailsChecker extends BroadcastReceiver {

    private final LanguageDetailsListener listener;

    String TAG= "LanguageDetails";
    public LanguageDetailsChecker(LanguageDetailsListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "BroadcastReceiver onReceive called");
        int resultCode = getResultCode();
        Log.i(TAG, "Result code: " + resultCode);
        if (resultCode == Activity.RESULT_OK) {
            Log.i(TAG, "Result code is RESULT_OK");
            Bundle results = getResultExtras(true);

            if (results != null) {
                Log.i(TAG, "Results are not null");
                String prefLang = results.getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE);
                Log.i(TAG, "Preferred Language: " + prefLang);

                ArrayList<String> allLangs = results.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);
                if (allLangs != null) {
                    listener.onLanguagesReceived(allLangs);
                } else {
                    Log.i(TAG, "Supported Languages: None found");
                }
            } else {
                Log.i(TAG, "No results received");
            }
        } else {
            Log.i(TAG, "Unexpected result code: " + resultCode);
        }
    }

    public interface LanguageDetailsListener {
        void onLanguagesReceived(ArrayList<String> languages);
    }

}