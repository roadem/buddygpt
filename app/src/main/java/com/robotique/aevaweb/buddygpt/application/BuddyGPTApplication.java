package com.robotique.aevaweb.buddygpt.application;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.bfr.buddy.speech.shared.ISTTCallback;
import com.bfr.buddy.speech.shared.ITTSCallback;
import com.bfr.buddy.speech.shared.STTResult;
import com.bfr.buddy.speech.shared.STTResultsData;
import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddyApplication;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddysdk.services.speech.STTTask;
import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.gson.Gson;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.ibm.icu.text.BreakIterator;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.konovalov.vad.Vad;
import com.konovalov.vad.VadConfig;
import com.konovalov.vad.VadListener;
import com.robotique.aevaweb.buddygpt.R;
import com.robotique.aevaweb.buddygpt.chatbotresponse.ResponseFromTeamGPT;
import com.robotique.aevaweb.buddygpt.models.Langue;
import com.robotique.aevaweb.buddygpt.models.Replica;
import com.robotique.aevaweb.buddygpt.models.Session;
import com.robotique.aevaweb.buddygpt.models.Setting;
import com.robotique.aevaweb.buddygpt.observers.IDBObserver;
import com.robotique.aevaweb.buddygpt.utilis.ConfigurationFile;
import com.robotique.aevaweb.buddygpt.utilis.CustomProperties;
import com.robotique.aevaweb.buddygpt.utilis.IMLKitDownloadCallback;
import com.robotique.aevaweb.buddygpt.utilis.ITTSCallbacks;
import com.robotique.aevaweb.buddygpt.utilis.PcmToWavConverter;
import com.robotique.aevaweb.buddygpt.utilis.SettingsContentObserver;
import com.robotique.aevaweb.buddygpt.utilis.TtsGoogleC;
import static com.google.android.exoplayer2.audio.OpusUtil.SAMPLE_RATE;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringTokenizer;

public class BuddyGPTApplication extends BuddyApplication {
    private static final String TAG = "BuddyGPT_Application";
    private static final String listeningDurationPseudo = "listening_duration";
    private static final String listeningAttemptPseudo = "listening_attempt";
    private static final String speakVolumePseudo = "speak_volume";
    private static final String visibilityString = "switch_visibility";
    private static final String emotionString = "switch_emotion";
    private static final String detectionLanguageString = "Detection_de_langue";
    private static final String configurationFilePseudo = "BuddyGPT.properties";
    private static final String langueFr = "Français";
    private static final String langueEn = "Anglais";
    private static final String langueEs = "Espagnol";
    private static final String langueDe = "Allemand";
    private static final String ANDROID_STT = "Android";
    private static final String CERENCE_STT = "Cerence";
    private static final String GOOGLE_STT = "google";
    private static final String WHISPER_STT = "openai";
    private static final String TAG_STREAMING = "AudioCapture";
    public static String currentActiveEmotion = null; // Tracks the current active emotion for restoration
    
    /**
     * Helper method to log and call setLabialExpression(NO_EXPRESSION) with stack trace
     * This method creates a single tracking point for all NO_EXPRESSION calls
     */
    public static void logAndResetLabialExpression(String callerName) {
        try {
            Log.i("🔍_NEUTRAL_HUNT", "════════════════════════════════════════════════════════");
            Log.i("🔍_NEUTRAL_HUNT", "🔍 setLabialExpression(NO_EXPRESSION) called from: " + callerName);
            Log.i("🔍_NEUTRAL_HUNT", "🔍 Current active emotion: " + currentActiveEmotion);
            
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (int i = 1; i < Math.min(6, stackTrace.length); i++) {
                Log.i("🔍_NEUTRAL_HUNT", "   └─ [" + i + "] " + stackTrace[i].getClassName() + "." + stackTrace[i].getMethodName() + ":" + stackTrace[i].getLineNumber());
            }
            Log.i("🔍_NEUTRAL_HUNT", "════════════════════════════════════════════════════════");
            
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
        } catch (Exception e) {
            Log.e("🔍_NEUTRAL_HUNT", "Error resetting expression: " + e.getMessage());
        }
    }
    
    public final IUsbCommadRsp iUsbLedCommandRsp = new IUsbCommadRsp.Stub() {
        @Override
        public void onSuccess(String success) {
            Log.i(TAG, "Led success : " + success);
        }

        @Override
        public void onFailed(String error) {
            Log.e(TAG, "Led error : " + error);
        }
    };
    private final Handler handler2 = new Handler();
    private final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    private final Intent speechRecognizerIntent2 = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    int max;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    boolean stopTTSReadSpeaker = false;

    boolean isFirstLaunch = true;
    public boolean isFirstLaunch() {
        return isFirstLaunch;
    }

    public void setFirstLaunch(boolean firstLaunch) {
        isFirstLaunch = firstLaunch;
    }
    private SpeechRecognizer speechRecognizer;
    int remainingAttempts;
    private int listeningDuration;
    private int listeningAttempt;
    private int speakVolume;
    private ResponseFromTeamGPT responseFromTeamGPT;
    private Setting setting;
    private Boolean fileCreate = true;
    private ArrayList<Session> listSession = new ArrayList<>();
    private String switchdetectLanguage;
    private String switchVisibility;
    private String switchEmotion;
    private Boolean isSpeaking = false;
    private List<IDBObserver> observers = new ArrayList<>();
    private Boolean notYet = true;
    private int textSizeBullesPX;
    private boolean activityClosed = false;
    private Boolean startRecording = false;
    private ConnectivityManager cm;
    private int questionNumber = 0;
    private int currentQuestionNubmer = 0;
    private boolean alreadyGetAnswer = false;
    private boolean timeoutExpired = false;
    private long questionTime = 0;
    private String storedResponse = "";
    private int bestTextSize = 0;
    private TextToSpeech ttsAndroid;
    private Boolean messageError = false;
    private Langue langue;
    private Dialog dialog;
    private int currentIndexText = 0;
    private boolean allTextPronoucedSuccess = true;
    private STTTask freeSpeechSttTask;
    private Boolean initSharedpreferences = true;
    private Translator englishLanguageSelectedTranslator;
    private Translator frenchLanguageSelectedTranslator;
    private Translator languageSelectedEnglishTranslator;
    private String translatedList = "";
    private String languageDetected = "";
    private Boolean usingReadSpeaker;
    private Vad vad;
    private Boolean endRecordingAudio = false;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private String currentState = "";
    private Boolean stopProcessus = false;
    private Boolean alReadyHadSpoke = false;
    private Activity activityTemp;
    private long responseTime = 0;
    private Boolean answerHasExceededTimeOut = false;
    private Thread thread;
    private Thread thread1;
    private Float previousVolume = Float.valueOf(0);
    private EncodingRegistry registry;
    private Boolean appIsListeningToTheQuestion = false;
    private String toastSttAndroidIndispo;
    private String toastTtsAndroidIndispo;
    private Boolean appIsCurrentlyDealingWithTheQuestion = false;
    private Boolean bIExecution = false;
    private boolean alreadyChatting = false; // pour savoir si BUDDY doit prononcer l'invitation au dialogue ou non
    private Toast mToast;
    private TranscribeTask transcribeTask;
    private volatile boolean stopRecordingSttInProgress = false;
    private Handler retryHotwordHandler;
    private Runnable retryHotwordRunnable;
    private Handler noMatchHandler;          //  Pour les faux positifs/no match (checkTheHotword)
    private Runnable noMatchRunnable;
    public void setAlreadyChatting(boolean alreadyChatting) {
        this.alreadyChatting = alreadyChatting;
    }

    public Boolean getBIExecution() {
        return bIExecution;
    }

    public void setBIExecution(Boolean bIExecution) {
        this.bIExecution = bIExecution;
    }

    public Boolean getAppIsCurrentlyDealingWithTheQuestion() {
        return appIsCurrentlyDealingWithTheQuestion;
    }

    public void setAppIsCurrentlyDealingWithTheQuestion(Boolean appIsCurrentlyDealingWithTheQuestion) {
        this.appIsCurrentlyDealingWithTheQuestion = appIsCurrentlyDealingWithTheQuestion;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }

    public void setRemainingAttempts(int remainingAttempts) {
        this.remainingAttempts = remainingAttempts;
    }

    public Dialog getDialog() {
        return dialog;
    }

    public Boolean getAppIsListeningToTheQuestion() {
        return appIsListeningToTheQuestion;
    }

    public void setAppIsListeningToTheQuestion(Boolean appIsListeningToTheQuestion) {
        this.appIsListeningToTheQuestion = appIsListeningToTheQuestion;
    }

    public Translator getEnglishLanguageSelectedTranslator() {
        return englishLanguageSelectedTranslator;
    }

    public void setEnglishLanguageSelectedTranslator(Translator englishLanguageSelectedTranslator) {
        this.englishLanguageSelectedTranslator = englishLanguageSelectedTranslator;
    }

    public void setLanguageSelectedEnglishTranslator(Translator languageSelectedEnglishTranslator) {
        this.languageSelectedEnglishTranslator = languageSelectedEnglishTranslator;
    }

    public boolean getInitSharedpreferences() {
        return initSharedpreferences;
    }

    public void setInitSharedpreferences(Boolean initSharedpreferences) {
        this.initSharedpreferences = initSharedpreferences;
    }

    public Boolean getUsingReadSpeaker() {
        return usingReadSpeaker;
    }

    public String getLanguageDetected() {
        return languageDetected;
    }

    public void setLanguageDetected(String languageDetected) {
        this.languageDetected = languageDetected;
    }

    public Translator getFrenchLanguageSelectedTranslator() {
        return frenchLanguageSelectedTranslator;
    }

    public void setFrenchLanguageSelectedTranslator(Translator frenchLanguageSelectedTranslator) {
        this.frenchLanguageSelectedTranslator = frenchLanguageSelectedTranslator;
    }

    public int getListeningDuration() {
        return listeningDuration;
    }

    public int getListeningAttempt() {
        return listeningAttempt;
    }

    public int getSpeakVolume() {
        return speakVolume;
    }

    public void setSpeakVolume(int speakVolume) {
        this.speakVolume = speakVolume;
    }

    public void setSetting(Setting setting) {
        this.setting = setting;
    }

    public Boolean getFileCreate() {
        return fileCreate;
    }

    public void setFileCreate(Boolean fileCreate) {
        this.fileCreate = fileCreate;
    }

    public ArrayList<Session> getListSession() {
        return listSession;
    }


    public void listSessionClear() {
        listSession.clear();
    }


    public void setSwitchVisibility(String switchVisibility) {
        this.switchVisibility = switchVisibility;
    }


    public void setSwitchEmotion(String switchEmotion) {
        Log.i("MMM", "BuddyGPTApplication.setSwitchEmotion(" + switchEmotion + ")");
        this.switchEmotion = switchEmotion;
        // whenever the global flag is toggled we wipe out any stored response time
        // and clear the stream object's neutral flag so that behavior stays
        // consistent even if the change happens mid-response or from another
        // fragment.
        setResponseTime(0);
        if (responseFromTeamGPT != null) {
            responseFromTeamGPT.resetEmotionNeutral();
        }
    }


    public void setSwitchdetectLanguage(String switchdetectLanguage) {
        this.switchdetectLanguage = switchdetectLanguage;
    }


    public Boolean getSpeaking() {
        return isSpeaking;
    }

    public void setSpeaking(Boolean speaking) {
        isSpeaking = speaking;
    }


    public void setNotYet(Boolean notYet) {
        this.notYet = notYet;
    }

    public int getTextSizeBullesPX() {
        return textSizeBullesPX;
    }

    public void setTextSizeBullesPX(int textSizeBullesPX) {
        this.textSizeBullesPX = textSizeBullesPX;
    }

    public boolean isActivityClosed() {
        return activityClosed;
    }

    public void setActivityClosed(boolean activityClosed) {
        this.activityClosed = activityClosed;
    }

    public Boolean getStartRecording() {
        return startRecording;
    }

    public void setStartRecording(Boolean startRecording) {
        this.startRecording = startRecording;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    public int getCurrentQuestionNubmer() {
        return currentQuestionNubmer;
    }

    public void setCurrentQuestionNubmer(int currentQuestionNubmer) {
        this.currentQuestionNubmer = currentQuestionNubmer;
    }

    public boolean isAlreadyGetAnswer() {
        return alreadyGetAnswer;
    }

    public void setAlreadyGetAnswer(boolean alreadyGetAnswer) {
        this.alreadyGetAnswer = alreadyGetAnswer;
    }


    public boolean isTimeoutExpired() {
        return timeoutExpired;
    }

    public void setTimeoutExpired(boolean timeoutExpired) {
        this.timeoutExpired = timeoutExpired;
    }

    public long getQuestionTime() {
        return questionTime;
    }

    public void setQuestionTime(long questionTime) {
        this.questionTime = questionTime;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }


    public void setAnswerHasExceededTimeOut(Boolean answerHasExceededTimeOut) {
        this.answerHasExceededTimeOut = answerHasExceededTimeOut;
    }

    public String getStoredResponse() {
        return storedResponse;
    }

    public void setStoredResponse(String storedResponse) {
        this.storedResponse = storedResponse;
    }

    public int getBestTextSize() {
        return bestTextSize;
    }


    private class TranscribeTask extends AsyncTask<byte[], Void, String> {
        @Override
        protected String doInBackground(byte[]... audioData) {
            Log.e(TAG,"doInBackground stopProcessus---------- "+stopProcessus);

            String question = Base64.encodeToString(audioData[0], Base64.NO_WRAP);
                    Log.i("FZE", "STT non local flux: TranscribeTask doInBackground length=" + (question == null ? "null" : question.length()));
                    if (Boolean.FALSE.equals(stopProcessus)){
                        Log.e(TAG,"envoie traitement de la question");
                        notifyObservers("STTQuestion_success;SPLIT;NONE;SPLIT;"+question);
                        BuddySDK.UI.stopListenAnimation();
                        setLed("neutral");
                        }
                    else {
                    if (Boolean.FALSE.equals(endRecordingAudio) && activityTemp!=null){
                            activityTemp.runOnUiThread(() -> startListeningQuestionWav(activityTemp));
                    }
            }
            return question;
        }

        @Override
        protected void onPostExecute(String transcription) {
            if (transcription != null) {
                Log.i(TAG, "------it took: ms");
            } else {
                // Gestion des erreurs

            }
        }
    }

    public void setMessageError(Boolean messageError) {
        this.messageError = messageError;
    }

    public Langue getLangue() {
        return langue;
    }

    public void setLangue(Langue langue) {
        this.langue = langue;
    }

    /**
     * initialisations
     */
    @Override
    public void onCreate() {
        super.onCreate();
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
        SettingsContentObserver mSettingsContentObserver = new SettingsContentObserver(new Handler(), getApplicationContext());
        getContentResolver().registerContentObserver(
                Settings.System.CONTENT_URI, true,
                mSettingsContentObserver);


        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());

        //create a new EncodingRegistry to use JTokkit
        try {
            new Thread(() -> registry = Encodings.newDefaultEncodingRegistry()).start();
        } catch (Exception e) {
            Log.e("TAG_STREAM_USAGE", "Exception when creating a new EncodingRegistry to use JTokkit : " + e);
            e.printStackTrace();
        }
    }

    public void init(String initOrMajOrNone) {
        Log.e(TAG, "init");

        String number_attempt = getParamFromFile("Number_listens",configurationFilePseudo);
        if(number_attempt.equals("")||Integer.parseInt(number_attempt)<=0){
            remainingAttempts= Integer.parseInt("1")-1;
        }
        else{
            remainingAttempts= Integer.parseInt(number_attempt)-1;
        }

        if (getparam("firstLaunch").isEmpty()) {
            setparam("firstLaunch", "true");
        }
        initLanguageSetting();
        initListeningSettings();
        initSpeakVolumeSetting();
        initVisibilitySetting();
        initEmotionSetting();
        initLanguageDetectionSetting();
        initChatTextSize();
        initTracking();

        notifyObservers("properties file done;SPLIT;"+initOrMajOrNone);
    }

    private void initListeningSettings() {
        if (getparam(listeningDurationPseudo).isEmpty()) {
            setparam(listeningDurationPseudo, getParamFromFile("Listening_time", configurationFilePseudo));
        }
        listeningDuration = Integer.parseInt(getparam(listeningDurationPseudo));
        if (listeningDuration < 0) {
            listeningDuration = 5;
            setparam(listeningDurationPseudo, String.valueOf(listeningDuration));
        }
        if (getparam(listeningAttemptPseudo).isEmpty()) {
            setparam(listeningAttemptPseudo, getParamFromFile("Number_listens", configurationFilePseudo));
        }
        listeningAttempt = Integer.parseInt(getparam(listeningAttemptPseudo));
        if (listeningAttempt < 0) {
            listeningAttempt = 2;
            setparam(listeningAttemptPseudo, String.valueOf(listeningAttempt));
        }
        remainingAttempts = listeningAttempt - 1;
        Log.i(TAG, "initListeningSettings: " + getParamFromFile("Listening_time", configurationFilePseudo));
    }



    public void resetSharedPreferences() {
        if (getparam("Mail_Destination").equalsIgnoreCase(getparam("Email"))) {
            setparam("Mail_Destination", "");
        }
        setparam("NomCompte", "");
        setparam("SelectedChatbot", "");
        setparam("chatbotModel", "");
        setparam("STT-TeamGPT", "");
        setparam("Environnement", "");
        setparam("TTS-TeamGPT", "");
        setparam("Header", "");
        setparam("Entete", "");
        setparam("Email", "");
        setparam("Stream_mode", "");
        setparam("Mail_sender", "");
        setparam("Smtp_host", "");
        setparam("Password_mail_sender", "");
        setparam("Smtp_port", "");
        setparam("IMEI_ID_Device", "_");
        setparam("IdCompte", "_");
        setparam("email_support", "_");
        setparam("Mail_Subject_fr", "");
        setparam("Mail_Subject_en", "");
    }

    private void initChatTextSize() {
        int textSize = Integer.parseInt(getParamFromFile("Chat_TextSize", configurationFilePseudo));
        if (textSize < 20 || textSize > 50) {
            setTextSizeBullesPX(25);
        } else setTextSizeBullesPX(textSize);


    }

    private void initSpeakVolumeSetting() {
        max = getMaxVolume();
        if (getparam(speakVolumePseudo).isEmpty()) {
            int defaultVolume = Integer.parseInt(getParamFromFile("Speech_volume", configurationFilePseudo));
            if (defaultVolume < 0 || defaultVolume > 100) {
                speakVolume = getVolume();
                defaultVolume = getClosestInt((double) (speakVolume * 100) / max);
            }

            setparam(speakVolumePseudo, String.valueOf(defaultVolume));
        }
        speakVolume = Integer.parseInt(getparam(speakVolumePseudo));
    }

    private void initVisibilitySetting() {
        if (getparam(visibilityString).isEmpty()) {
            if (getParamFromFile("Display_of_speech", configurationFilePseudo).trim().equalsIgnoreCase("No")) {
                setparam(visibilityString, "false");
            } else {
                setparam(visibilityString, "true");
            }
        }
        switchVisibility = getparam(visibilityString);
    }


    private void initLanguageSetting() {
        List<String> langueDisponible = getDisponibleLangue();
        List<Langue> langues = new ArrayList<>();
        for (int i = 1; i < langueDisponible.size(); i++) {
            String langueNom = langueDisponible.get(i - 1);
            String langueCode = langueDisponible.get(i);
            Langue langueObj = getOrCreateLangue(langueNom, langueCode, i);
            langues.add(langueObj);
            i++;
        }
        if (langues.isEmpty()) {
            Langue langueFrancais = createDefaultFrancaisLangue();
            langues.add(langueFrancais);
        }
        setCurrentLangueFromList(langues, langueDisponible.size());
    }

    private Langue getOrCreateLangue(String langueNom, String langueCode, int id) {
        if (getparam(langueNom).isEmpty()) {
            String languageCode = resolveLanguageCode(langueCode);
            Boolean isChosen = languageCode != null && getParamFromFile("Language", configurationFilePseudo).trim().equalsIgnoreCase(languageCode.split("-")[0]);
            Langue langue = new Langue(id, langueNom, isChosen, languageCode);
            setparam(langueNom, new Gson().toJson(langue));
            return langue;
        } else {
            Langue langueTemp = new Gson().fromJson(getparam(langueNom), Langue.class);
            langueTemp.setId(id);
            String languageCode = resolveLanguageCode(langueCode);
            langueTemp.setLanguageCode(languageCode);
            setparam(langueNom, new Gson().toJson(langueTemp));
            return langueTemp;
        }
    }

    private String resolveLanguageCode(String langueCode) {
        String languageCode;
        if (langueCode.contains("-")) {
            languageCode = langueCode;
        } else if (langueCode.equalsIgnoreCase("fr")) {
            languageCode = "fr-FR";
        } else if (langueCode.equalsIgnoreCase("en")) languageCode = "en-US";
        else {
            languageCode = getFirstFullLanguageCode(langueCode);
        }
        if (languageCode == null) languageCode = getFullLanguageCodeFromCountryCode(langueCode);
        return languageCode;
    }

    private Langue createDefaultFrancaisLangue() {
        Langue langueFrancais = new Langue(1, "Français", true);
        langueFrancais.setLanguageCode("fr-FR");
        setparam(langueFr, new Gson().toJson(langueFrancais));
        return new Gson().fromJson(getparam(langueFr), Langue.class);
    }

    private void setCurrentLangueFromList(List<Langue> langues, int langueDisponibleSize) {
        int iterationCount = 0;
        for (Langue language : langues) {
            iterationCount++;
            if (language.isChosen()) {
                this.langue = language;
                setLangue(language);
                break;
            }
            if (iterationCount == langueDisponibleSize / 2) {
                this.langue = language;
                setLangue(language);
            }
        }
    }

    private String getFullLanguageCodeFromCountryCode(String shortLanguageCode) {
        Locale[] locales = Locale.getAvailableLocales();

        for (Locale locale : locales) {
            if (shortLanguageCode.equalsIgnoreCase(locale.getCountry())) {
                Log.e(TAG, "getFirstFullLanguageCode " + locale.getLanguage() + "-" + locale.getCountry());
                return locale.getLanguage() + "-" + locale.getCountry();
            }
        }
        return null;
    }

    public List<String> getDisponibleLangue() {
        StringTokenizer st = new StringTokenizer(getParamFromFile("Languages_available", configurationFilePseudo), "/", false);
        List<String> list = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String result = st.nextToken();
            list.add(result.split("_")[0].trim());
            list.add(result.split("_")[1].trim());
        }
        return list;

    }

    public void downloadModel(IMLKitDownloadCallback imlKitDownloadCallback, String langue) {
        TranslatorOptions options1;
        TranslatorOptions options;

        options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(langue) // Remplacez par la langue choisie par l'utilisateur
                .build();
        englishLanguageSelectedTranslator = Translation.getClient(options);
        DownloadConditions conditions = new DownloadConditions.Builder()
                .build();
        englishLanguageSelectedTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    setEnglishLanguageSelectedTranslator(englishLanguageSelectedTranslator);
                    imlKitDownloadCallback.onDownloadEnd(true, "english");
                })
                .addOnFailureListener(e -> imlKitDownloadCallback.onDownloadEnd(false, "english"));
        options1 = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.FRENCH)
                .setTargetLanguage(langue) // Remplacez par la langue choisie par l'utilisateur
                .build();
        frenchLanguageSelectedTranslator = Translation.getClient(options1);
        DownloadConditions conditions1 = new DownloadConditions.Builder()
                .build();
        frenchLanguageSelectedTranslator.downloadModelIfNeeded(conditions1)
                .addOnSuccessListener(unused -> {
                    setFrenchLanguageSelectedTranslator(frenchLanguageSelectedTranslator);
                    imlKitDownloadCallback.onDownloadEnd(true, "french");
                })
                .addOnFailureListener(e -> imlKitDownloadCallback.onDownloadEnd(false, "french"));
        options = new TranslatorOptions.Builder()
                .setSourceLanguage(langue)
                .setTargetLanguage(TranslateLanguage.ENGLISH) // Remplacez par la langue choisie par l'utilisateur
                .build();
        languageSelectedEnglishTranslator = Translation.getClient(options);
        DownloadConditions conditions2 = new DownloadConditions.Builder()
                .build();
        languageSelectedEnglishTranslator.downloadModelIfNeeded(conditions2)
                .addOnSuccessListener(unused -> {
                    setLanguageSelectedEnglishTranslator(languageSelectedEnglishTranslator);
                    imlKitDownloadCallback.onDownloadEnd(true, "languageToEnglish");
                })
                .addOnFailureListener(e -> imlKitDownloadCallback.onDownloadEnd(false, "languageToEnglish"));

    }

    private void initEmotionSetting() {
        if (getparam(emotionString).isEmpty()) {
            if (getParamFromFile("Activation_of_emotions", configurationFilePseudo).trim().equalsIgnoreCase("No")) {
                setparam(emotionString, "false");
            } else {
                setparam(emotionString, "true");
            }
        }
        switchEmotion = getparam(emotionString);
    }

    private void initTracking(){
        Log.i(TAG, "initTracking: init");

        //Tracking activation
        if (getparam("Tracking_Activation").equals("")) {
            if(getParamFromFile("Tracking",configurationFilePseudo).equalsIgnoreCase("Yes")){
                Log.i(TAG, "initTracking: value "+getparam("Tracking_Activation"));
                setparam("Tracking_Activation","True");
            }else
               setparam("Tracking_Activation","False");


        }



        //Tracking camera display
        if (getparam("Tracking_Camera_Display").equals("")) {
            if (getParamFromFile("TRACKING_Camera",configurationFilePseudo).trim().equalsIgnoreCase("No")){
                setparam("Tracking_Camera_Display", "false");
            }
            else {
                setparam("Tracking_Camera_Display", "true");
            }
        }



        //Tracking auto listen
        if (getparam("Tracking_Auto_Listen").equals("")) {
            if (getParamFromFile("TRACKING_listening",configurationFilePseudo).trim().equalsIgnoreCase("No")){
                setparam("Tracking_Auto_Listen", "false");
            }
            else {
                setparam("Tracking_Auto_Listen", "true");
            }
        }

    }


    private void initLanguageDetectionSetting() {
        if (getparam(detectionLanguageString).isEmpty()) {
            if (getParamFromFile("Language_detection", configurationFilePseudo).trim().equalsIgnoreCase("No")) {
                setparam(detectionLanguageString, "false");
            } else {
                setparam(detectionLanguageString, "true");
            }
        }
        switchdetectLanguage = getparam(detectionLanguageString);
    }
    //#region ******************************************************* STT **********************************************************************


    //#region ******************************************************* STT Cerence Local fcf **********************************************************************

    public ResponseFromTeamGPT getResponseFromTeamGPT() {
        return responseFromTeamGPT;
    }


    //#region ******************************************************* STT Free Speech **********************************************************************

    public void setResponseFromTeamGPT(ResponseFromTeamGPT responseFromTeamGPT) {
        this.responseFromTeamGPT = responseFromTeamGPT;
    }

    public List<String> separator(String hotword) {
        StringTokenizer st = new StringTokenizer(hotword, "/", false);
        List<String> list = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String result = st.nextToken();
            list.add(result.trim());
        }
        return list;
    }

    /**
     * Cette fonction permet de lancer l'écoute STT Cerence local .fcf OU BIEN l'écoute de OK BUDDY
     *
     * @return : l'objet STTTask ou null si erreur
     */
    public void startListeningHotwor(Activity activity) {
        getTranslateHotwordList();
        neutralAnimation();
        isSpeaking = false;
        stopListening(activity);

        setAlreadyChatting(false);

        if (getCurrentLanguage().equals("en")) {
            toastSttAndroidIndispo = getString(R.string.toast_stt_android_indispo_en);
        } else if (getCurrentLanguage().equals("fr")) {
            toastSttAndroidIndispo = getString(R.string.toast_stt_android_indispo_fr);
        } else if (getCurrentLanguage().equals("de")) {
            toastSttAndroidIndispo = getString(R.string.toast_stt_android_indispo_de);
        } else if (getCurrentLanguage().equals("es")) {
            toastSttAndroidIndispo = getString(R.string.toast_stt_android_indispo_es);
        } else {
            getEnglishLanguageSelectedTranslator()
                    .translate(getString(R.string.toast_stt_android_indispo_en))
                    .addOnSuccessListener(translatedText -> toastSttAndroidIndispo = translatedText)
                    .addOnFailureListener(e -> toastSttAndroidIndispo = getString(R.string.toast_stt_android_indispo_en));
        }
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> {
            if (activity != null && !activity.isFinishing()) {
                activity.runOnUiThread(() -> {

                    //USE BLUE MIC

                    Log.i(TAG, "startListeningHotwor: avant try");
                    try {
                        Log.i(TAG, "onError: speechRecognizer.startListening 1");
                        speechRecognizer.startListening(speechRecognizerIntent2);
                        Log.i(TAG, "startListeningHotwor: apres try intent2");
                        if (!isAppInstalled(getApplicationContext(), "com.google.android.googlequicksearchbox")) {
                            showToast(toastSttAndroidIndispo);
                        }
                        Log.i(TAG, "startListeningHotwor: apres try intent3");
                        speechRecognizer.setRecognitionListener(new RecognitionListener() {
                            @Override
                            public void onReadyForSpeech(Bundle bundle) {
                                Log.e(TAG, "onReadyForSpeech");
                            }

                            @Override
                            public void onBeginningOfSpeech() {
                                Log.e(TAG, "onBeginningOfSpeech");
                            }

                            @Override
                            public void onRmsChanged(float v) {
                                Log.e(TAG, "onRmsChanged");

                            }

                            @Override
                            public void onBufferReceived(byte[] bytes) {
                                Log.e(TAG, "Hotword onBufferReceived listening  : ");
                            }

                            @Override
                            public void onEndOfSpeech() {
                                Log.e(TAG, "Hotword onEndOfSpeech listening  : ");
                            }

                            @Override
                            public void onError(int i) {
                                switch (i) {
                                    case SpeechRecognizer.ERROR_AUDIO:
                                        Log.d(TAG, "Audio recording error");
                                        logErrorSTTAndroid(i, "SpeechRecognizer.ERROR_AUDIO", "Audio recording error");
                                        break;
                                    case SpeechRecognizer.ERROR_CLIENT:
                                        Log.d(TAG, "Client side error");
                                        logErrorSTTAndroid(i, "SpeechRecognizer.ERROR_CLIENT", "Client side error");
                                        break;
                                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                                        Log.d(TAG, "Insufficient permissions");
                                        logErrorSTTAndroid(i, "SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS", "Insufficient permissions");
                                        break;
                                    case SpeechRecognizer.ERROR_NETWORK:
                                        Log.d(TAG, "Network error");
                                        logErrorSTTAndroid(i, "SpeechRecognizer.ERROR_NETWORK", "Network error");
                                        break;
                                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                                        Log.d(TAG, "Network timeout");
                                        logErrorSTTAndroid(i, "SpeechRecognizer.ERROR_NETWORK_TIMEOUT", "Network timeout");
                                        break;
                                    case SpeechRecognizer.ERROR_NO_MATCH:
                                        Log.d(TAG, "No match");
                                        logErrorSTTAndroid(i, "SpeechRecognizer.ERROR_NO_MATCH", "No match");
                                        break;
                                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                                        Log.d(TAG, "RecognitionService busy");
                                        logErrorSTTAndroid(i, "SpeechRecognizer.ERROR_RECOGNIZER_BUSY", "RecognitionService busy");
                                        break;
                                    case SpeechRecognizer.ERROR_SERVER:
                                        Log.d(TAG, "Server error");
                                        logErrorSTTAndroid(i, "SpeechRecognizer.ERROR_SERVER", "Server error");
                                        break;
                                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                                        Log.d(TAG, "No speech input");
                                        logErrorSTTAndroid(i, "SpeechRecognizer.ERROR_SPEECH_TIMEOUT", "No speech input");
                                        break;
                                    default:
                                        Log.d(TAG, "Unknown error");
                                        logErrorSTTAndroid(i, "Unknown error", "Unknown error");
                                        break;
                                }
                                Log.i(TAG, "onError: speechRecognizer.startListening 2");
                                try {
                                    speechRecognizer.cancel();
                                    speechRecognizer.destroy();
                                } catch (Exception ignored) {
                                    Log.i(TAG, "onError: "+ignored.getMessage());
                                }

                                // --- LOGIQUE DE REDÉMARRAGE AVEC RÉFÉRENCE ---

                                // 1. Initialiser le Handler et le Runnable si nécessaire
                                if (retryHotwordHandler == null) {
                                    retryHotwordHandler = new Handler(Looper.getMainLooper());
                                }

                                // 2. Définir le Runnable de redémarrage
                                retryHotwordRunnable = () -> {
                                    try {
                                        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
                                        speechRecognizer.setRecognitionListener(this); // 'this' est le RecognitionListener
                                        speechRecognizer.startListening(speechRecognizerIntent2);
                                        Log.i(TAG, "SpeechRecognizer redémarré après erreur.");

                                        // Une fois redémarré, l'objet Runnable n'est plus nécessaire dans le Handler
                                        retryHotwordRunnable = null;

                                    } catch (Exception e) {
                                        Log.e(TAG, "Failed recreating speechRecognizer: " + e);
                                        // Si l'échec persiste, vous pouvez choisir de ne pas relancer
                                    }
                                };

                                // 3. Annuler tout redémarrage précédent et poster le nouveau
                                retryHotwordHandler.removeCallbacksAndMessages(null);
                                retryHotwordHandler.postDelayed(retryHotwordRunnable, 600);
                            }

                            @Override
                            public void onResults(Bundle bundle) {
                                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                                if (data != null && !data.isEmpty()) {
                                    Log.e(TAG, "Hotword result  : " + data.get(0));
                                    checkTheHotword(data.get(0));
                                } else {
                                    Log.e(TAG, "Hotword result  size = 0 : ");
                                }
                            }

                            @Override
                            public void onPartialResults(Bundle bundle) {
                                Log.e(TAG, "Hotword onPartialResults listening  : ");
                                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                                if (data != null && !data.isEmpty()) {
                                    Log.e(TAG, "Hotword result onPartialResults  : " + data.get(0));
                                    checkTheHotword(data.get(0));
                                } else {
                                    Log.e(TAG, "Hotword result onPartialResults size = 0 : ");
                                }
                            }

                            @Override
                            public void onEvent(int i, Bundle bundle) {
                                Log.e(TAG, "Hotword onEvent listening  : ");
                            }

                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Runnable : Erreur pendant la vérification [isReadyToListen] - Hotword : " + e);
                    }


                });
            }
        });


    }

    /**
     * Cette fonction permet de lancer l'écoute STT Free Speech
     *
     * @return : l'objet STTTask ou null si erreur
     */

    private void initializeSTT(STTTask sttTask) {
        try {
            Log.i(TAG, "initializeSTT: try");
            sttTask.initialize();
        } catch (Exception e) {
            Log.e(TAG, "Runnable : Erreur pendant l'initialisation du STT Task : " + e);
        }
    }

    /**
     * Vérifie si le nom de fichier complet de la grammaire est valide pour la langue courante
     * et si le fichier existe et est accessible.
     *
     * @param fullGrammarFileName Le nom complet du fichier de grammaire (ex: 'BuddyCompanion_Combined_fr.fcf').
     * @return true si le fichier est trouvé et lisible et correspond à la langue, false sinon.
     */
    public boolean isValidGrammarFile(String fullGrammarFileName) {
        String TAG = "BuddyApp";
        String currentLang = getCurrentLanguage();
        String expectedSuffix;

        // Déterminer le suffixe attendu
        if (currentLang.equals("en")) {
            expectedSuffix = "_en.fcf";
        } else if (currentLang.equals("fr")) {
            expectedSuffix = "_fr.fcf";
        } else {
            Log.e(TAG, "isValidGrammarFile: Language '" + currentLang + "' not supported for Cerence grammar check.");
            return false;
        }

        // Vérifier que le nom de fichier configuré correspond au suffixe de la langue
        if (!fullGrammarFileName.toLowerCase().endsWith(expectedSuffix.toLowerCase())) {
            Log.w(TAG, "isValidGrammarFile: Configured file name '" + fullGrammarFileName +
                    "' does not match expected suffix for language " + currentLang +
                    " (expected " + expectedSuffix + ")");
            return false;
        }

        // Obtenir le chemin de stockage externe (correspond à /storage/emulated/0/)
        File externalStorageDir = Environment.getExternalStorageDirectory();

        // Construire le chemin complet : /storage/emulated/0/grammars/NOM_FICHIER.fcf
        File grammarDir = new File(externalStorageDir, "grammars");
        File grammarFile = new File(grammarDir, fullGrammarFileName);

        String filePath = grammarFile.getAbsolutePath(); // Pour le logging

        if (grammarFile.exists() && grammarFile.isFile() && grammarFile.canRead()) {
            Log.i(TAG, "isValidGrammarFile: Grammar file found and valid at: " + filePath);
            return true;
        } else {
            Log.w(TAG, "isValidGrammarFile: Grammar file NOT found or invalid at: " + filePath);
            return false;
        }
    }

    public void startListeningSTTForQuestion(Activity activity) {
        Log.e(TAG, "startListeningSTTForQuestion start");

        setAppIsListeningToTheQuestion(true);
        // Si Android STT est le moteur par défaut
        if (getparam("STT").trim().equalsIgnoreCase(ANDROID_STT)) {
            startListeningQuestion(activity);
        }

        // Si Cerence STT est le moteur par défaut
        else if (getparam("STT").trim().equalsIgnoreCase(CERENCE_STT)) {

            String currentLang = getCurrentLanguage();

            // Vérification de la langue supportée par Cerence avec grammaire
            if (currentLang.equals("fr") || currentLang.equals("en")) {

                String grammarParamKey = "Cerence_Grammar_Name_" + currentLang;
                String defaultGrammarFile = "companion_commands_" + currentLang + ".fcf";
                String grammarToUse = "";

                // récupérer le fichier de grammaire depuis le fichier de config
                String configuredGrammar = getParamFromFile(grammarParamKey, configurationFilePseudo).trim();

                if (!configuredGrammar.isEmpty() && isValidGrammarFile(configuredGrammar)) {
                    grammarToUse = configuredGrammar;

                } else if (isValidGrammarFile(defaultGrammarFile)) {
                    // Fichier par défaut trouvé et valide de companion
                    grammarToUse = defaultGrammarFile;

                } else {
                    if (getLangue().getNom().equals(langueEn)) {
                        showToast(getString(R.string.toast_teamgpt_cerencefcf_en));
                    } else if (getLangue().getNom().equals(langueFr)) {
                        showToast(getString(R.string.toast_teamgpt_cerencefcf_fr));
                    } else if (getLangue().getNom().equals(langueEs)) {
                        showToast(getString(R.string.toast_teamgpt_cerencefcf_es));
                    } else if (getLangue().getNom().equals(langueDe)) {
                        showToast(getString(R.string.toast_teamgpt_cerencefcf_de));
                    } else {
                        getEnglishLanguageSelectedTranslator()
                                .translate(getString(R.string.toast_teamgpt_cerencefcf_en))
                                .addOnSuccessListener(translatedText -> showToast(translatedText))
                                .addOnFailureListener(e -> showToast(getString(R.string.toast_teamgpt_cerencefcf_en)));
                    }
                    // Ni l'un ni l'autre n'est valide : FALLBACK sur Android STT
                    Log.w(TAG, "Cerence grammar (Configured: " + configuredGrammar + " | Default: " + defaultGrammarFile + ") not found or invalid. Falling back to Android STT.");
                    startListeningQuestion(activity);

                }

                // Lancer Cerence avec le nom complet du fichier de grammaire déterminé
                startListeningCerenceWithGrammar(activity, grammarToUse);


            } else {
                // Langue Cerence non supportée -> Fallback Android STT
                Log.d(TAG, "Language '" + currentLang + "' not supported by Cerence. Using Android STT.");
                startListeningQuestion(activity);

            }
        }
        ///-----------------------
        // ajout des STT Serveur
        ///-----------------------
        else if (getparam("STT").trim().equalsIgnoreCase(GOOGLE_STT) || getparam("STT").trim().equalsIgnoreCase(WHISPER_STT)) {
            startListeningQuestionWav(activityTemp);
        }

    }
    public STTTask startListeningCerenceWithGrammar(Activity activity, String fullGammarFileName) {
        Log.e(TAG, "startListeningCerenceWithGrammar start");
        alreadyGetAnswer = false;
        questionNumber++;
        stopListening(activity);
        // Construction du chemin complet du fichier de grammaire
        // Le nom complet du fichier (fullGammarFileName) est utilisé DIRECTEMENT.
        String fullFilePath = "/storage/emulated/0/grammars/" + fullGammarFileName;

        // Détermination de la Locale
        Locale locale = null;
        if (getCurrentLanguage().equals("en")) {
            locale = Locale.ENGLISH;
        } else if (getCurrentLanguage().equals("fr")) {
            locale = Locale.FRENCH;
        }

        try {
            Log.i(TAG, "startListeningCerence: try");
            if (getParamFromFile("Language_Specification_STT", configurationFilePseudo).trim().equalsIgnoreCase("No")) {
                Log.i(TAG, "startListeningCerence: Free Speech mode (Language_Specification_STT=No)");
                freeSpeechSttTask = BuddySDK.Speech.createCerenceFreeSpeechTask();
            } else {
                Log.i(TAG, "startListeningCerence: Task mode with grammar: " + fullFilePath);

                // UTILISATION DU NOM COMPLET
                freeSpeechSttTask = BuddySDK.Speech.createCerenceTask(locale, fullFilePath);
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception lors de la création de cerence " + e);
        }

        if (freeSpeechSttTask == null) {
            Log.i(TAG, "startListeningCerence: freeSpeechSttTask == null -> falling back to Android STT");
            try {
                stopRecording();
            } catch (Exception ignored) {
                Log.i(TAG, "startListeningCerence: "+ignored);
            }
            try {
                startListeningQuestion(activity);
            } catch (Exception e) {
                Log.e(TAG, "Fallback startListeningQuestion failed", e);
            }
            return null;
        } else {
            Log.i(TAG, "startListeningCerence: freeSpeechSttTask created successfully");
        }

        initializeSTT(freeSpeechSttTask);

        Log.w(TAG, "startListeningfreeSpeechStt : cerence");

        try {
            Log.w(TAG, "startListeningfreeSpeechStt :try ");
            freeSpeechSttTask.start(true, new ISTTCallback.Stub() {
                @Override
                public void onSuccess(STTResultsData sttResultsData) throws RemoteException {
                    Log.i(TAG, "Succès d'écoute cerence Free Speech");

                    if (!sttResultsData.getResults().isEmpty()) {

                        // log every result in the list, not only the first one
                        List<STTResult> results = sttResultsData.getResults();
                        for (int i = 0; i < results.size(); i++) {
                            STTResult r = results.get(i);
                            Log.e(TAG, "Listening cerence Free Speech result[" + i + "] : " +
                                    "\nScore : " + r.getConfidence() +
                                    "\nUtterance: " + r.getUtterance() +
                                    "\nRule: " + r.getRule());
                        }

                        STTResult result = results.get(0);
                        notifyObservers("STTQuestion_success;SPLIT;" + result.getUtterance()+";SPLIT;NONE;");
                        setLed("neutral");

                    }
                }

                @Override
                public void onError(String s) throws RemoteException {
                    Log.e(TAG, "onError cerence " + s);

                }
            });
            Log.i(TAG, "startListeningCerence: after try");
        } catch (Exception e) {
            Log.e(TAG, "onError cerence " + e);

        }

        setLed("listening");
        return freeSpeechSttTask;

    }

    public void refresh(String langue, Activity activity) {

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        if (getParamFromFile("Language_Specification_STT", configurationFilePseudo).trim().equalsIgnoreCase("Yes")) {
            if (getparam("STT").equalsIgnoreCase("Android")) {
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, Integer.parseInt(getParamFromFile("Android_Speech_minimum_length", configurationFilePseudo)) * 1000);
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, Integer.parseInt(getParamFromFile("Android_Speech_silence_length", configurationFilePseudo)) * 1000);
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langue);
            }
            if (getparam("STT").equalsIgnoreCase("Cerence") && !langue.toLowerCase().contains("en") && !langue.toLowerCase().contains("fr")) {
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, Integer.parseInt(getParamFromFile("Android_Speech_minimum_length", configurationFilePseudo)) * 1000);
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, Integer.parseInt(getParamFromFile("Android_Speech_silence_length", configurationFilePseudo)) * 1000);
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langue);
            }

            if (!getLangue().getNom().equals(langueFr) && !getLangue().getNom().equals(langueEn)) {
                speechRecognizerIntent2.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                speechRecognizerIntent2.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, Integer.parseInt(getParamFromFile("Android_Speech_minimum_length", configurationFilePseudo)) * 1000);
                speechRecognizerIntent2.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, Integer.parseInt(getParamFromFile("Android_Speech_silence_length", configurationFilePseudo)) * 1000);
                speechRecognizerIntent2.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");

            } else {
                speechRecognizerIntent2.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                speechRecognizerIntent2.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, Integer.parseInt(getParamFromFile("Android_Speech_minimum_length", configurationFilePseudo)) * 1000);
                speechRecognizerIntent2.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, Integer.parseInt(getParamFromFile("Android_Speech_silence_length", configurationFilePseudo)) * 1000);
                speechRecognizerIntent2.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langue);
            }
        } else {
            speechRecognizerIntent.removeExtra(RecognizerIntent.EXTRA_LANGUAGE);
            speechRecognizerIntent2.removeExtra(RecognizerIntent.EXTRA_LANGUAGE);
        }


    }

    public void startListeningQuestion(Activity activity) {
        Log.e(TAG, "startListeningFreeSpeechStt fonction start");

        // Post sur le thread UI de façon sûre (activity peut être null / finishing)
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(this::listeningAnimation);

        alreadyGetAnswer = false;
        questionNumber++;

        // Arrêter l'écoute précédente (garde l'usage de activity)
        stopListening(activity);

        if (getCurrentLanguage().equals("en")) {
            toastSttAndroidIndispo = getString(R.string.toast_stt_android_indispo_en);
        } else if (getCurrentLanguage().equals("fr")) {
            toastSttAndroidIndispo = getString(R.string.toast_stt_android_indispo_fr);
        } else if (getCurrentLanguage().equals("de")) {
            toastSttAndroidIndispo = getString(R.string.toast_stt_android_indispo_de);
        } else if (getCurrentLanguage().equals("es")) {
            toastSttAndroidIndispo = getString(R.string.toast_stt_android_indispo_es);
        } else {
            getEnglishLanguageSelectedTranslator()
                    .translate(getString(R.string.toast_stt_android_indispo_en))
                    .addOnSuccessListener(translatedText -> toastSttAndroidIndispo = translatedText)
                    .addOnFailureListener(e -> toastSttAndroidIndispo = getString(R.string.toast_stt_android_indispo_en));
        }

        // Exécuter la logique d'initialisation STT sur le thread UI (sans dépendre d'un activity non-null)
        mainHandler.post(() -> {
            try {
                Log.i(TAG, "startListeningQuestion: test");
                Log.i(TAG, "onError: speechRecognizer.startListening 4");
                speechRecognizer.startListening(speechRecognizerIntent);

                if (!isAppInstalled(getApplicationContext(), "com.google.android.googlequicksearchbox")) {
                    showToast(toastSttAndroidIndispo);
                }
                speechRecognizer.setRecognitionListener(new RecognitionListener() {
                    @Override
                    public void onReadyForSpeech(Bundle bundle) {
                        Log.e(TAG, " listen start");
                    }

                    @Override
                    public void onBeginningOfSpeech() {
                        Log.i(TAG, "start listen");
                    }

                    @Override
                    public void onRmsChanged(float v) {
                        Log.i(TAG, "onRmsChanged listen");
                    }

                    @Override
                    public void onBufferReceived(byte[] bytes) {
                        Log.i(TAG, "onBufferReceived listen");
                    }

                    @Override
                    public void onEndOfSpeech() {
                        Log.i(TAG, "onEndOfSpeech listen");
                    }

                    @Override
                    public void onError(int i) {
                        Log.i(TAG, "onError listening");
                        switch (i) {
                            case SpeechRecognizer.ERROR_AUDIO:
                                Log.d(TAG, "Audio recording error");
                                logErrorSTTAndroid(i, "SpeechRecognizer.ERROR_AUDIO", "Audio recording error");
                                break;
                            case SpeechRecognizer.ERROR_CLIENT:
                                Log.d(TAG, "Client side error");
                                logErrorSTTAndroid(i, "SpeechRecognizer.ERROR_CLIENT", "Client side error");
                                break;
                            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                                Log.d(TAG, "Insufficient permissions");
                                logErrorSTTAndroid(i, "SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS", "Insufficient permissions");
                                break;
                            case SpeechRecognizer.ERROR_NETWORK:
                                Log.d(TAG, "Network error");
                                logErrorSTTAndroid(i, "SpeechRecognizer.ERROR_NETWORK", "Network error");
                                break;
                            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                                Log.d(TAG, "Network timeout");
                                logErrorSTTAndroid(i, "SpeechRecognizer.ERROR_NETWORK_TIMEOUT", "Network timeout");
                                break;
                            case SpeechRecognizer.ERROR_NO_MATCH:
                                Log.d(TAG, "No match");
                                logErrorSTTAndroid(i, "SpeechRecognizer.ERROR_NO_MATCH", "No match");
                                break;
                            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                                Log.d(TAG, "RecognitionService busy");
                                logErrorSTTAndroid(i, "SpeechRecognizer.ERROR_RECOGNIZER_BUSY", "RecognitionService busy");
                                break;
                            case SpeechRecognizer.ERROR_SERVER:
                                Log.d(TAG, "Server error");
                                logErrorSTTAndroid(i, "SpeechRecognizer.ERROR_SERVER", "Server error");
                                break;
                            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                                Log.d(TAG, "No speech input");
                                logErrorSTTAndroid(i, "SpeechRecognizer.ERROR_SPEECH_TIMEOUT", "No speech input");
                                break;
                            default:
                                Log.d(TAG, "Unknown error");
                                logErrorSTTAndroid(i, "Unknown error", "Unknown error");
                                break;
                        }
                        // relancer l'écoute
                        try {
                            Log.i(TAG, "onError: speechRecognizer.startListening 5");
                            speechRecognizer.startListening(speechRecognizerIntent);
                        } catch (Exception ex) {
                            Log.e(TAG, "Failed to restart speechRecognizer after error", ex);
                        }
                    }

                    @Override
                    public void onResults(Bundle bundle) {
                        ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (data != null && !data.isEmpty()) {
                            Log.e(TAG, "question result onResults  : " + data.get(0));
                            notifyObservers("STTQuestion_success;SPLIT;" + data.get(0)+";SPLIT;NONE;");
                            BuddySDK.UI.stopListenAnimation();
                            setLed("neutral");
                        } else {
                            Log.e(TAG, "question result onResults size = 0 : ");
                            try {
                                Log.i(TAG, "onError: speechRecognizer.startListening 6");
                                speechRecognizer.startListening(speechRecognizerIntent);
                            } catch (Exception ex) {
                                Log.e(TAG, "Failed to restart speechRecognizer on empty results", ex);
                            }
                        }
                    }

                    @Override
                    public void onPartialResults(Bundle bundle) {
                        Log.i(TAG, "onPartialResults listen");
                        ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (data != null && !data.isEmpty()) {
                            Log.e(TAG, "question result onPartialResults  : " + data.get(0));
                            if (!data.get(0).trim().equals("")) {
                                notifyObservers("STTQuestion_success;SPLIT;" + data.get(0)+";SPLIT;NONE;");
                                BuddySDK.UI.stopListenAnimation();
                                setLed("neutral");
                            } else {
                                Log.e(TAG, "question result onPartialResults  : vide " + data.get(0));
                            }
                        } else {
                            Log.e(TAG, "question result onPartialResults size = 0 : ");
                        }
                    }

                    @Override
                    public void onEvent(int i, Bundle bundle) {
                        Log.i(TAG, "onEvent listen");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Runnable : Erreur pendant la vérification [isReadyToListen] : " + e);
            }
        });
    }


    public void logErrorSTTAndroid(int code, String type, String message) {
        String errorTXT = new Date() + ", STTAndroidERROR,ERROR CODE= " + code + ", ERROR Body{ type= " + type + ", message= " + message + "}" + System.getProperty("line.separator");
        File file2 = new File(Environment.getExternalStorageDirectory(), "BuddyGPT/ERROR-History.txt");


        try {

            try (FileWriter fileWriter = new FileWriter(file2, true)) {
                fileWriter.write(errorTXT);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void stopRecordingSTT(Boolean shouldRestartListening,Boolean shouldRestartNewCycle) {
        Log.i(TAG, "stopRecordingSTT: start");

        Log.i("nv", "STT non local flux: stopRecordingSTT");

        if (stopRecordingSttInProgress) {
            Log.i("FZE", "STT non local flux: stopRecordingSTT already running -> skip");
            return;
        }
        stopRecordingSttInProgress = true;
        try {
            byte[] audioDataF = readAudioFile(); // Read the recorded audio data
            Log.i("FZE", "STT non local flux: audio base64 length=" + (audioDataF == null ? "null" : audioDataF.length));
            // Annuler la tâche précédente si elle existe
            if (transcribeTask != null && transcribeTask.getStatus() == AsyncTask.Status.RUNNING) {
                transcribeTask.cancel(true);
            }
            Log.e(TAG,"start dbfs calcul");
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
            thread =new Thread(() -> {
                try {
                    if (!Python.isStarted()) {
                        Python.start(new AndroidPlatform(activityTemp));
                    }
                    Python py = Python.getInstance();
                    PyObject pyobj = py.getModule("calculDBFS");
                    PyObject reponse;
                    JSONObject parameters = new JSONObject();
                    parameters.put("fichier_audio", Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.wav"); // Chemin de fichier audio

                    // Appel de la fonction main avec le chemin du fichier audio
                    reponse = pyobj.callAttr("main", parameters.getString("fichier_audio"));

                    //Mettre  le dernier fichier json envoyé à l'API



                    Log.e(TAG,"result dBFS python "+reponse.toString());
                    if (!reponse.toString().trim().equals("-inf")) {
                        if (Float.parseFloat(reponse.toString()) >= Float.parseFloat(getParamFromFile("Seuil_dBFS", configurationFilePseudo))) {
                            Log.d(TAG, "volume est bien : " + Float.parseFloat(reponse.toString()));
                            transcribeTask = new TranscribeTask();
                            Log.i("FZE", "STT non local flux: transcribeTask execute");
                            transcribeTask.execute(audioDataF); // Transcribe the audio
                        } else {
                            Log.d(TAG, "volume est trop bas : " + Float.parseFloat(reponse.toString()));
                            if (activityTemp != null) {
                                startListeningQuestionWav(activityTemp);
                            } else {
                                Log.w(TAG, "stopRecordingSTT: activityTemp is null, skip restart listening");
                            }

                        }
                    }
                    else {
                        if (Boolean.TRUE.equals(shouldRestartListening)) {
                            if (activityTemp != null) {
                                startListeningQuestionWav(activityTemp);
                            } else {
                                Log.w(TAG, "stopRecordingSTT: activityTemp is null, skip restart listening");
                            }
                        } else {
                            if (Boolean.TRUE.equals(shouldRestartNewCycle)){
                                if (activityTemp != null) {
                                    activityTemp.runOnUiThread(() -> notifyObservers("restartNewCycle"));
                                } else {
                                    notifyObservers("restartNewCycle");
                                }
                            }
                            else {
                                if (activityTemp != null) {
                                    activityTemp.runOnUiThread(() -> notifyObservers("restartListeningHotword"));
                                } else {
                                    notifyObservers("restartListeningHotword");
                                }
                            }
                        }
                    }
                    if (Thread.currentThread().isInterrupted()) {
                        return; // Terminer le thread s'il a été interrompu
                    }


                } catch (PyException | JSONException p) {
                    Log.e(TAG, "Exception "+p);
                } finally {
                    stopRecordingSttInProgress = false;
                }

            });
            thread.start();
        } catch (Exception e) {
            Log.e(TAG, "Exception " + e);
            stopRecordingSttInProgress = false;
        }

    }

    Runnable periodicTask = new Runnable() {
        @Override
        public void run() {
            try {
                readAudioFile();
            } catch (IOException e) {
                Log.e(TAG, "periodicTask readAudioFile error: " + e);
            }
            Log.e(TAG,"start dbfs calcul 3---------------");
            if (thread1 != null && thread1.isAlive()) {
                thread1.interrupt();
            }
            thread1 =new Thread(() -> {
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(activityTemp));
                }
                Python py = Python.getInstance();
                PyObject pyobj = py.getModule("calculDBFS");
                try {
                    PyObject reponse;
                    JSONObject parameters = new JSONObject();
                    parameters.put("fichier_audio", Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.wav"); // Chemin de fichier audio

                    // Appel de la fonction main avec le chemin du fichier audio
                    reponse = pyobj.callAttr("main", parameters.getString("fichier_audio"));

                    //Mettre  le dernier fichier json envoyé à l'API
                    Log.e(TAG, "test comparaison flot--------------- " + reponse.toString());

                    //Mettre  le dernier fichier json envoyé à l’API
                    Log.e(TAG, "test comparaison flot--------------- " + reponse.toString());
                    Log.e(TAG, "result dBFS python--------------- " + reponse.toString());
                    Log.e(TAG, "previousVolume--------------- " + previousVolume);
                    Log.e(TAG, "previousVolume after traitement--------------- " + (previousVolume - (Math.abs(previousVolume) * Float.parseFloat(getParamFromFile("Volume_reduction", configurationFilePseudo)) / 100)));
                    if (!reponse.toString().trim().equals("-inf")){
                        if (previousVolume == 0) {
                            Log.e(TAG, "result dBFS if--------------- ");
                            previousVolume = Float.parseFloat(reponse.toString());
                        } else {
                            if (Float.parseFloat(reponse.toString()) <= (previousVolume - (Math.abs(previousVolume) * Float.parseFloat(getParamFromFile("Volume_reduction", configurationFilePseudo)) / 100))) {
                                traitementAudio();
                                previousVolume = Float.valueOf(0);
                                Log.e(TAG, "result dBFS else if--------------- ");

                            } else {
                                Log.e(TAG, "result dBFS else else--------------- ");
                                previousVolume = Float.parseFloat(reponse.toString());
                            }
                        }
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        return; // Terminer le thread s'il a été interrompu
                    }


                } catch (PyException | JSONException p) {
                    Log.e(TAG,"exception dBFS python "+p);
                }

            });
            thread1.start();
            handler2.postDelayed(this, Integer.valueOf(getParamFromFile("Duration_sound_level_checked",configurationFilePseudo))*1000);
        }
    };


    public void stopRecording() {
        if (handler2 != null && periodicTask != null) {
            handler2.removeCallbacks(periodicTask);
        }
        if (!isRecording) {
            Log.d(TAG_STREAMING, "Not recording");
            Log.i("FZE", "STT non local flux: stopRecording called but not recording");
            return;
        }

        // Arrêter la boucle du thread AVANT de libérer audioRecord
        boolean wasRecording = isRecording;
        isRecording = false;
        Log.i("FZE", "STT non local flux: stopRecording -> isRecording=false");

        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
                Log.i(TAG_STREAMING, " audioRecord stopped and released");
            } catch (Exception e) {
                Log.e(TAG_STREAMING, "Error stopping audioRecord: " + e.getMessage());
            }
        }
        if (vad != null) {
            Log.e(TAG, "+++++++++++++++++++++++++++++++++vad stop");
            vad.stop();
        }


    }

    public void traitementAudio() {
        currentState = "NOISE";
        alReadyHadSpoke = false;
        stopProcessus = false;
        stopRecording();
        stopRecordingSTT(true, false);
    }

    public List<String> getHotwordList() {
        if (getLangue().getNom().equals(langueFr)) {
            return separator(getParamFromFile("hotword_fr", configurationFilePseudo));
        } else if (getLangue().getNom().equals(langueEn)) {
            return separator(getParamFromFile("hotword_en", configurationFilePseudo));
        } else if (getLangue().getNom().equals(langueEs)) {
            return separator(getParamFromFile("hotword_es", configurationFilePseudo));
        } else if (getLangue().getNom().equals(langueDe)) {
            return separator(getParamFromFile("hotword_de", configurationFilePseudo));
        } else {
            Log.e(TAG, "getHotwordList" + translatedList);

            return separator(translatedList);
        }

    }

    public void getTranslateHotwordList() {
        if (!getLangue().getNom().equals(langueFr) && !getLangue().getNom().equals(langueEn) && !getLangue().getNom().equals(langueEs) && !getLangue().getNom().equals(langueDe)) {
            translatedList = getParamFromFile("hotword_en", configurationFilePseudo);
        }
    }

    public void checkTheHotword(String word){
        List<String> hotword = getHotwordList();
        boolean rightHottwordDetected = false;

        for (int i = 0; i < hotword.size(); i++) {
            Log.i(TAG, "checkTheHotword :" + word);
            if (word.trim().equalsIgnoreCase(hotword.get(i).trim())) {
                try {
                    rightHottwordDetected = true;

                    //  CRUCIAL : Annuler le retry handler AVANT de notifier
                    if (noMatchHandler != null) {
                        noMatchHandler.removeCallbacksAndMessages(null);
                        Log.i(TAG, "checkTheHotword: Cancelled noMatchHandler (hotword detected)");
                    }

                    //  Notifier que le hotword est détecté
                    notifyObservers("STTHotword_success");
                    Log.i(TAG, "checkTheHotword: Hotword DETECTED - notifying observers");

                } catch (Resources.NotFoundException e) {
                    Log.e(TAG, "Resources not Found " + e);
                }
                break;
            }
        }

        //  NOUVEAU : Retryer SEULEMENT si hotword n'est PAS détecté
        if (!rightHottwordDetected && speechRecognizer != null && speechRecognizerIntent2 != null) {
            Log.i(TAG, "checkTheHotword: Hotword NOT detected - retrying in 250ms");
            setLed("listening");

            // 1. Initialiser le Handler
            if (noMatchHandler == null) {
                noMatchHandler = new Handler(Looper.getMainLooper());
            }

            // 2. Définir le Runnable de redémarrage
            noMatchRunnable = () -> {
                Log.i(TAG, "checkTheHotword: noMatchRunnable executing - restarting hotword listener");
                try {
                    speechRecognizer.startListening(speechRecognizerIntent2);
                } catch (Exception e) {
                    Log.e(TAG, "Retry failed in checkTheHotword: " + e);
                }
            };

            // 3. Annuler l'ancien post et poster le nouveau
            noMatchHandler.removeCallbacksAndMessages(null);
            noMatchHandler.postDelayed(noMatchRunnable, 250);
        }
    }
    /**
     * Cette fonction permet d'arrêter l'écoute STT Free Speech
     */
    public void stopListening(Activity activity) {
        Log.i(TAG, "stopListening: start");
        Log.i("FZE", "STT non local flux: stopListening (stopProcessus->true)");

                    if (activity != null && !activity.isFinishing() ) {
                        activity.runOnUiThread(() -> {

                            boolean wasRecording = isRecording;
                            stopProcessus = true;
                            Log.i("FZE", "STT non local flux: stopListening in UI thread, isRecording=" + isRecording);
                            // ---  ANNULLER LE HANDLER D'ERREUR DE REDÉMARRAGE ---
                            if (retryHotwordHandler != null) {
                                // Annuler tous les messages postés, y compris le Runnable de redémarrage
                                retryHotwordHandler.removeCallbacksAndMessages(null);
                                Log.i(TAG, "stopListening: Hotword Retry Handler annulé.");
                            }
                            // --- NOUVEAU : Annuler le Handler de Re-tentative après non-match ---
                            if (noMatchHandler != null) {
                                noMatchHandler.removeCallbacksAndMessages(null);
                                Log.i(TAG, "stopListening: NoMatch Retry Handler annulé.");
                            }
                            try {
                                if (speechRecognizer != null) {
                                    speechRecognizer.stopListening();
                                    speechRecognizer.destroy();
                                }
                            } catch (Exception e) {
                                Log.i(TAG, "stopListening: " + e);
                            }
                            if (freeSpeechSttTask != null) {
                                Log.w(TAG, "stopListeningFreeSpeechStt");
                                try {
                                    freeSpeechSttTask.stop();
                                } catch (Exception e) {
                                    Log.e(TAG, "Erreur pendant l'arrêt d'écoute STT Free Speech : " + e);
                                }
                            }


                            stopRecording();

                          


                        });

                    }

        setLed("Neutral");


    }

    private void listeningAnimation() {
        Log.i(TAG, "startVoiceRecorder");
        BuddySDK.UI.setFacialExpression(FacialExpression.LISTENING, 1);
        BuddySDK.UI.startListenAnimation();
        setLed("listening");

    }


    //#endregion ******************************************************* STT Free Speech **********************************************************************

    //#endregion ******************************************************* STT **********************************************************************

    private void neutralAnimation() {

        BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
        BuddySDK.UI.stopListenAnimation();
        setLed("neutral");

    }

    /**
     * Cette fonction permet de prononcer le texte passé en argument de manière récursive, afin de gérer le problème de décalage entre la bouche et le discours pour les réponses plus longues.
     *
     * @param texteToSpeak         : message à dire par Buddy.
     * @param expression           : jouer un mouvement spécial de la bouche [SPEAK_ANGRY / NO_FACE / SPEAK_HAPPY / SPEAK_NEUTRAL]
     * @param texteToSpeakSplitted : Liste de phrases courtes à dire par Buddy.
     */
    public void startSpeakingSplittedText(final String texteToSpeak, LabialExpression expression, String type, String[] texteToSpeakSplitted) {

        Log.i(TAG, "startSpeakingSplittedText " + Arrays.toString(texteToSpeakSplitted) + " , " + type);


        Handler handlerAll = new Handler(Looper.getMainLooper());
        Runnable delayedTask = () -> {
            Log.i(TAG, "handler_all start ");

            try {
                Log.i("BBB", "⚠️ About to call setLabialExpression(NO_EXPRESSION) in startSpeakingSplittedText");
                Log.i("🔍_NEUTRAL_HUNT", "═══ CALLING setLabialExpression(NO_EXPRESSION) in startSpeakingSplittedText ═══");
                Log.i("🔍_NEUTRAL_HUNT", "Current active emotion: " + currentActiveEmotion);
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                for (int i = 1; i < Math.min(5, stackTrace.length); i++) {
                    Log.i("🔍_NEUTRAL_HUNT", "  [" + i + "] " + stackTrace[i].getClassName() + "." + stackTrace[i].getMethodName() + ":" + stackTrace[i].getLineNumber());
                }
                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                Log.i("BBB", "✓ setLabialExpression(NO_EXPRESSION) done");
            } catch (Exception e) {
                Log.e(TAG, "BuddySDK Exception  " + e);
            }

            if (currentIndexText < texteToSpeakSplitted.length) {

                Log.e("HOU_DEBUG", "call startSpeaking");
                Log.i("BBB", "⚠️ About to call BuddySDK.Speech.startSpeaking with text part [" + currentIndexText + "]=\"" + texteToSpeakSplitted[currentIndexText] + "\" and expression=" + expression);

                BuddySDK.Speech.startSpeaking(
                        texteToSpeakSplitted[currentIndexText],
                        expression,
                        new ITTSCallback.Stub() {
                            @Override
                            public void onSuccess(String iText) {
                                Log.i(TAG, "Succès de prononciation : " + iText);
                                Log.i("BBB", "✓ TTS phrase [" + (currentIndexText - 1) + "] spoken successfully");

                                Log.w("HOU_DEBUG", "onSuccess");

                                currentIndexText++;

                                if (!stopTTSReadSpeaker) {
                                    Log.w("HOU_DEBUG", "onSuccess 1 ");
                                    Handler handler = new Handler(Looper.getMainLooper());
                                    handler.postDelayed(() -> {
                                        Log.w("HOU_DEBUG", "onSuccess 2");
                                        Log.i("BBB", "➤ Calling next phrase in recursion, currentIndexText=" + currentIndexText);
                                        startSpeakingSplittedText(texteToSpeak, expression, type, texteToSpeakSplitted);
                                    }, 150);
                                }


                            }

                            @Override
                            public void onError(String iError) {
                                Log.e(TAG, "Erreur pendant la prononciation : " + iError);

                                Log.w("HOU_DEBUG", "onError");

                                allTextPronoucedSuccess = false;


                                currentIndexText++;

                                if (!stopTTSReadSpeaker) {
                                    Log.w("HOU_DEBUG", "onError 1 ");
                                    Handler handler = new Handler(Looper.getMainLooper());
                                    handler.postDelayed(() -> {
                                        Log.w("HOU_DEBUG", "onError 2");
                                        startSpeakingSplittedText(texteToSpeak, expression, type, texteToSpeakSplitted);
                                    }, 150);
                                }

                            }

                            @Override
                            public void onPause() {
                                // onPause()
                            }

                            @Override
                            public void onResume() {
                                //onResume()
                            }
                        }
                );

            } else {
                Log.e("HOU_DEBUG", "END OF SPEAK : " + allTextPronoucedSuccess);

                if (allTextPronoucedSuccess) {
                    //success
                    allTextPronouced(texteToSpeak, type);
                } else {
                    setLanguageDetected("");
                    //error
                    if (type.equals("storedResponse")) {
                        questionNumber++;
                        notifyObservers("TTS_error;" + texteToSpeak);
                        storedResponse = "";
                    } else {
                        questionNumber++;
                        notifyObservers("TTS_error;" + texteToSpeak);
                    }
                }
            }
        };

        handlerAll.postDelayed(delayedTask, 0);
    }

    /**
     * Cette fonction s'exécute lorsque le TTS prononce la réponse du ChatBot.
     */
    public void allTextPronouced(final String texteToSpeak, String type) {
        Log.i("BBB", "✓ allTextPronouced CALLED - All text has been spoken! type='" + type + "'");

        if (type.equals("timeOutExpired")) {
            timeoutExpired = false;
            if (getparam("Stream_mode").equals("true") && responseFromTeamGPT != null) {
                responseFromTeamGPT.resumeStreaming();
            } else {
                notifyObservers("playStoredResponse");
            }
        } else if (type.equals("storedResponse")) {
            questionNumber++;
            Log.i("BBB", "→ Notifying TTS_success");
            notifyObservers("TTS_success;" + texteToSpeak);
            storedResponse = "";
            setLanguageDetected("");
        } else {
            questionNumber++;
            setLanguageDetected("");
            if (getparam("Stream_mode").equals("true")) {
                if (getResponseFromTeamGPT() != null)
                    getResponseFromTeamGPT().onTTSEnd();
            } else {
                Log.i("BBB", "→ Notifying TTS_success");
                notifyObservers("TTS_success;" + texteToSpeak);
            }
        }
    }

    /**
     * Cette fonction permet de prononcer le texte passé en argument.
     *
     * @param texteToSpeak : message à dire par Buddy.
     * @param expression   : jouer un mouvement spécial de la bouche [SPEAK_ANGRY / NO_FACE / SPEAK_HAPPY / SPEAK_NEUTRAL]
     */
    public void speakTTS(final String texteToSpeak, LabialExpression expression, String type) {
        setAlreadyChatting(true);
        Log.e("TTS", "texteToSpeak " + texteToSpeak);
        currentIndexText = 0;
        stopTTSReadSpeaker = false;
        Log.w(TAG, "speakTTS : " + texteToSpeak);
        Log.i("BBB", "➤ speakTTS() CALLED with type='" + type + "' expression=" + expression);

        setToastTtsAndroidIndispo();

        try {
            setTTSAfterDetectingLanguage();

            if (shouldUseReadSpeaker()) {
                Log.i("BBB", "➤ Using ReadSpeaker TTS");
                handleReadSpeakerTTS(texteToSpeak, expression, type);
                Log.i("BBB", "← ReadSpeaker TTS returned");
            } else if (shouldUseAndroidTTS()) {
                Log.i("BBB", "➤ Using Android TTS");
                handleAndroidTTS(texteToSpeak, type);
                Log.i("BBB", "← Android TTS returned");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception pendant la prononciation : " + e);
            notifyObservers("TTS_exception;" + texteToSpeak);
        }
    }

    private void setToastTtsAndroidIndispo() {
        String lang = getCurrentLanguage();
        if (lang.equals("en")) {
            toastTtsAndroidIndispo = getString(R.string.toast_tts_android_indispo_en);
        } else if (lang.equals("fr")) {
            toastTtsAndroidIndispo = getString(R.string.toast_tts_android_indispo_fr);
        } else if (lang.equals("de")) {
            toastTtsAndroidIndispo = getString(R.string.toast_tts_android_indispo_de);
        } else if (lang.equals("es")) {
            toastTtsAndroidIndispo = getString(R.string.toast_tts_android_indispo_es);
        } else {
            getEnglishLanguageSelectedTranslator()
                    .translate(getString(R.string.toast_tts_android_indispo_en))
                    .addOnSuccessListener(translatedText -> toastTtsAndroidIndispo = translatedText)
                    .addOnFailureListener(e -> toastTtsAndroidIndispo = getString(R.string.toast_tts_android_indispo_en));
        }
    }

    private boolean shouldUseReadSpeaker() {
        String lang = getCurrentLanguage();
        return (((lang.equals("en") || lang.equals("fr")) && getparam("TTS").equalsIgnoreCase("ReadSpeaker") && usingReadSpeaker)
                || (getparam("TTS").equalsIgnoreCase("ReadSpeaker") && usingReadSpeaker));
    }

    private boolean shouldUseAndroidTTS() {
        return getparam("TTS").equalsIgnoreCase("Android")
                || (getparam("TTS").equalsIgnoreCase("ReadSpeaker") && getSecondTTSfromTTSList().equalsIgnoreCase("Android"));
    }


    private void handleReadSpeakerTTS(final String texteToSpeak, LabialExpression expression, String type) {
        String[] texteToSpeakSplitted;
        if (getCurrentLanguage().equals("en")) {
            BuddySDK.Speech.setSpeakerSpeed(Integer.parseInt(getParamFromFile("ReadSpeaker_speed_en", configurationFilePseudo)));
            BuddySDK.Speech.setSpeakerPitch(Integer.parseInt(getParamFromFile("ReadSpeaker_pitch_en", configurationFilePseudo)));
        } else {
            BuddySDK.Speech.setSpeakerSpeed(Integer.parseInt(getParamFromFile("ReadSpeaker_speed_fr", configurationFilePseudo)));
            BuddySDK.Speech.setSpeakerPitch(Integer.parseInt(getParamFromFile("ReadSpeaker_pitch_fr", configurationFilePseudo)));
        }
        BuddySDK.Speech.setSpeakerVolume(getSpeakVolume());
        if (BuddySDK.Speech.isReadyToSpeak()) {
            String texteToSpeakModified = texteToSpeak;
            if (texteToSpeak.toLowerCase().contains("content")) {
                texteToSpeakModified = texteToSpeak.replaceAll("\\bcontent\\b", "contents");
            }
            if (!type.equals("timeOutExpired")) {
                texteToSpeakSplitted = texteToSpeakModified.split("[.,]");
                Log.e("texteToSpeakSplitted", Arrays.toString(texteToSpeakSplitted));
                Log.d("HOU_DEBUG", "calling startSpeakingSplittedText : " + texteToSpeak);
                Log.i("BBB", "➤ About to call startSpeakingSplittedText with expression=" + expression);
                startSpeakingSplittedText(texteToSpeak, expression, type, texteToSpeakSplitted);
                Log.i("BBB", "← startSpeakingSplittedText returned");
            } else {
                Log.i("BBB", "➤ About to call BuddySDK.Speech.startSpeaking (timeOutExpired) with expression=" + expression);
                BuddySDK.Speech.startSpeaking(
                        texteToSpeakModified,
                        expression,
                        new ITTSCallback.Stub() {
                            @Override
                            public void onSuccess(String iText) {
                                Log.i(TAG, "Succès de prononciation : " + iText);
                                allTextPronouced(texteToSpeak, type);
                            }

                            @Override
                            public void onError(String iError) {
                                Log.e(TAG, "Erreur pendant la prononciation : " + iError);
                                if (type.equals("timeOutExpired")) {
                                    timeoutExpired = false;
                                    notifyObservers("playStoredResponse");
                                }
                            }

                            @Override
                            public void onPause() {
                                // comment
                            }

                            @Override
                            public void onResume() {
                                // comment
                            }
                        }
                );
            }
        }
    }

    private void handleAndroidTTS(final String texteToSpeak, String type) {
        int result = ttsAndroid.speak(texteToSpeak, TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
        if (!isAppInstalled(getApplicationContext(), "com.google.android.tts")) {
            showToast(toastTtsAndroidIndispo);
        }
        if (result == -1) {
            notifyObservers("TTS_error;" + texteToSpeak);
        } else {
            ttsAndroid.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    try {
                        BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                    } catch (Exception e) {
                        Log.e(TAG, "BuddySDK Exception  " + e);
                    }
                }

                @Override
                public void onDone(String utteranceId) {
                    try {
                        BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                    } catch (Exception e) {
                        Log.e(TAG, "BuddySDK Exception  " + e);
                    }
                    handleTTSOnDone(texteToSpeak, type);
                }

                @Override
                public void onError(String utteranceId) {
                    Log.e(TAG, "Erreur pendant la prononciation " + utteranceId);
                    handleTTSOnError(texteToSpeak, type);
                }
            });
        }
    }

    private void handleTTSOnDone(String texteToSpeak, String type) {
        if (type.equals("timeOutExpired")) {
            timeoutExpired = false;
            notifyObservers("playStoredResponse");
        } else if (type.equals("storedResponse")) {
            questionNumber++;
            notifyObservers("TTS_success;" + texteToSpeak);
            storedResponse = "";
            setLanguageDetected("");
        } else {
            questionNumber++;
            setLanguageDetected("");
            if (getparam("Stream_mode").equals("true")) {
                if (getResponseFromTeamGPT() != null)
                    getResponseFromTeamGPT().onTTSEnd();
            } else {
                notifyObservers("TTS_success;" + texteToSpeak);
            }
        }
    }

    private void handleTTSOnError(String texteToSpeak, String type) {
        if (type.equals("timeOutExpired")) {
            timeoutExpired = false;
            notifyObservers("playStoredResponse");
        } else if (type.equals("storedResponse")) {
            try {
                BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
            } catch (Exception e) {
                Log.e(TAG, "BuddySDK Exception  " + e);
            }
            questionNumber++;
            notifyObservers("TTS_error;" + texteToSpeak);
            storedResponse = "";
            setLanguageDetected("");
        } else {
            try {
                BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
            } catch (Exception e) {
                Log.e(TAG, "BuddySDK Exception  " + e);
            }
            questionNumber++;
            notifyObservers("TTS_error;" + texteToSpeak);
            setLanguageDetected("");
        }
    }


    /**
     * Cette fonction permet d'arrêter la prononciation
     */
    public void stopTTS() {
        Log.w(TAG, "stopTTS");
        stopTTSReadSpeaker = true;
        try {
            if (BuddySDK.Speech.isSpeaking()) {
                BuddySDK.Speech.stopSpeaking();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur pendant l'arrêt de la prononciation TTS : " + e);
        }
        if (ttsAndroid != null) {
            ttsAndroid.stop();
        }
        setLanguageDetected("");
    }

    /**
     * Cette méthode permet d'inialiser le TTS selon la langue du robot
     */
    public void setTTSAfterDetectingLanguage() {
        if (!getLanguageDetected().equals("")) {
            setTTSLanguage(getLanguageDetected());
        } else {
            setTTSLanguage(getCurrentLanguage());
        }
    }

    public void setTTSLanguage(String language){
        Log.e("TEST","setTTSLanguage "+language);
        Log.e("TEST","usingReadSpeaker language"+language);
        Log.e("TEST","language code      -----------   "+getLangue().getLanguageCode());
        try {
            switch(language){
                case "en":
                    setEnglishTTSLanguage();
                    break;
                case "fr":
                    setFrenchTTSLanguage();
                    break;
                case "es":
                    setSpanishTTSLanguage(language);
                    break;
                case "de":
                    setGermanTTSLanguage(language);
                    break;
                default:
                    setDefaultTTSLanguage(language);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur pendant l'initialisation de la langue TTS : " + e);
        }
    }
    private void setEnglishTTSLanguage() {
        if (getparam("TTS").equalsIgnoreCase("ReadSpeaker")) {
            if (getLangue().getLanguageCode().equals("en-US")) {
                BuddySDK.Speech.setSpeakerVoice("kate");
                Log.e("TTS", "english kate");
                usingReadSpeaker = true;
            }else {

                if (getLangue().getLanguageCode().split("-")[0].equals("en")){
                    usingReadSpeaker = false;
                    if (getparam("TTS").equalsIgnoreCase("Android") || (getparam("TTS").equalsIgnoreCase("ReadSpeaker") && getSecondTTSfromTTSList().equalsIgnoreCase("Android"))){
                        ttsAndroid.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                        ttsAndroid.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                        ttsAndroid.setLanguage(new Locale(getLangue().getLanguageCode().split("-")[0],getLangue().getLanguageCode().split("-")[1]));
                    }else if (getparam("TTS").equalsIgnoreCase("ApiGoogle") || (getparam("TTS").equalsIgnoreCase("ReadSpeaker") && getSecondTTSfromTTSList().equalsIgnoreCase("ApiGoogle"))){
                        Log.i(TAG, "setEnglishTTSLanguage: else");
                    }
                    //0.5,2.0

                }
                else {
                    BuddySDK.Speech.setSpeakerVoice("kate");
                    Log.e("TTS","english kate");
                    usingReadSpeaker = true;
                }
            }

        }
        else {
            if (getLangue().getLanguageCode().split("-")[0].equals("en")){
                usingReadSpeaker = false;
                ttsAndroid.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                ttsAndroid.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                ttsAndroid.setLanguage(new Locale(getLangue().getLanguageCode().split("-")[0],getLangue().getLanguageCode().split("-")[1]));
            }
            else {
                usingReadSpeaker = false;
                ttsAndroid.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                ttsAndroid.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                ttsAndroid.setLanguage(new Locale("en","US"));
            }
        }
    }

    private void setFrenchTTSLanguage() {
        if (getparam("TTS").equalsIgnoreCase("ReadSpeaker")){
            Log.e("TTS","usingReadSpeaker y");
            if (getLangue().getLanguageCode().equals("fr-FR")){
                Log.e("TTS","frensh roxane");
                BuddySDK.Speech.setSpeakerVoice("roxane");
                Log.e("TTS","usingReadSpeaker 1");
                usingReadSpeaker = true;
            }else {
                if (getLangue().getLanguageCode().split("-")[0].equals("fr")){
                    Log.e("TTS","usingReadSpeaker 2");
                    usingReadSpeaker = false;
                    ttsAndroid.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                    ttsAndroid.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                    ttsAndroid.setLanguage(new Locale(getLangue().getLanguageCode().split("-")[0],getLangue().getLanguageCode().split("-")[1]));
                }
                else {
                    Log.e("TTS","frensh roxane");
                    BuddySDK.Speech.setSpeakerVoice("roxane");
                    Log.e("TTS","usingReadSpeaker 3");
                    usingReadSpeaker = true;
                }
            }
        }else {
            if (getLangue().getLanguageCode().split("-")[0].equals("fr")){
                usingReadSpeaker = false;
                ttsAndroid.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                ttsAndroid.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                ttsAndroid.setLanguage(new Locale(getLangue().getLanguageCode().split("-")[0],getLangue().getLanguageCode().split("-")[1]));
            }
            else {
                usingReadSpeaker = false;
                ttsAndroid.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                ttsAndroid.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                ttsAndroid.setLanguage(new Locale("fr","FR"));
            }
        }
    }

    private void setSpanishTTSLanguage(String language) {
        usingReadSpeaker = false;
        if (getLangue().getLanguageCode().split("-")[0].equals("es")) {

            ttsAndroid.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch", configurationFilePseudo))));
            ttsAndroid.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed", configurationFilePseudo))));
            ttsAndroid.setLanguage(new Locale(getLangue().getLanguageCode().split("-")[0], getLangue().getLanguageCode().split("-")[1]));
        }
        else{
            int result = ttsAndroid.setLanguage(new Locale(language.toLowerCase(),language.toUpperCase()));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TEST", "langue non pas prise ne charge");
                String code = getFirstFullLanguageCode(language.toLowerCase());
                Log.e("TEST", "langue qui doit etre "+code);
                ttsAndroid.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                ttsAndroid.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                ttsAndroid.setLanguage(new Locale(code.split("-")[0].trim(),code.split("-")[1].trim()));

            }
        }
    }

    private void setGermanTTSLanguage(String language) {
        usingReadSpeaker = false;
        if (getLangue().getLanguageCode().split("-")[0].equals("de")) {

            ttsAndroid.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch", configurationFilePseudo))));
            ttsAndroid.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed", configurationFilePseudo))));
            ttsAndroid.setLanguage(new Locale(getLangue().getLanguageCode().split("-")[0], getLangue().getLanguageCode().split("-")[1]));
        }
        else{
            int result = ttsAndroid.setLanguage(new Locale(language.toLowerCase(),language.toUpperCase()));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TEST", "langue non pas prise ne charge");
                String code = getFirstFullLanguageCode(language.toLowerCase());
                Log.e("TEST", "langue qui doit etre "+code);
                ttsAndroid.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                ttsAndroid.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                ttsAndroid.setLanguage(new Locale(code.split("-")[0].trim(),code.split("-")[1].trim()));

            }
        }
    }

    private void setDefaultTTSLanguage(String language) {
        usingReadSpeaker = false;
        Log.e("TEST","default language "+language);
        Log.e("TEST","default getCurrentLanguage().split(\"-\")[0].trim() "+getCurrentLanguage().split("-").length);
        if (!getCurrentLanguage().equals("") && getCurrentLanguage().split("-")[0].trim().equalsIgnoreCase(language)){
            ttsAndroid.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
            ttsAndroid.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
            ttsAndroid.setLanguage(new Locale(getCurrentLanguage().split("-")[0].trim(),getCurrentLanguage().split("-")[1].trim()));
        }
        else {
            Log.e("TEST","set Langue TTS "+language.toLowerCase()+","+language.toUpperCase());
            int result = ttsAndroid.setLanguage(new Locale(language.toLowerCase(),language.toUpperCase()));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TEST", "langue non pas prise ne charge");
                String code = getFirstFullLanguageCode(language.toLowerCase());
                Log.e("TEST", "langue qui doit etre "+code);
                ttsAndroid.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                ttsAndroid.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                ttsAndroid.setLanguage(new Locale(code.split("-")[0].trim(),code.split("-")[1].trim()));

            }
        }
    }



    public String getSecondTTSfromTTSList() {
        String[] listTTS = getParamFromFile("Text_To_Speech_List", configurationFilePseudo).split("/");
        if (listTTS.length > 1) {
            if (listTTS[1].trim().equalsIgnoreCase("Android")) {
                return listTTS[1].trim();
            } else return "Android";
        } else return "Android";
    }

    private float getConvertedPitchAndSpeedValue(int nombre) {
        int valeurMinEntree = 50;
        int valeurMaxEntree = 150;

        Log.e("TEST", "converted value :nombre= " + nombre);
        // Vérification si le nombre se trouve dans l'intervalle d'entrée
        if (nombre < valeurMinEntree || nombre > valeurMaxEntree) {
            nombre = (valeurMinEntree + valeurMinEntree) / 2;
        }
        float valeurFloat = (nombre - 50) / 100.0f * 1.5f;

        // Ajouter 0.5f pour obtenir l'intervalle 0.5f à 2.0f
        valeurFloat += 0.5f;

        Log.e("TEST", "converted value :nombre= " + nombre + "  converted =" + valeurFloat);
        return valeurFloat;


    }

    private String getFirstFullLanguageCode(String shortLanguageCode) {
        Locale[] locales = Locale.getAvailableLocales();
        Boolean hasThesame = false;
        boolean firstLanguageCode = true;
        String fullLanguageCode = "";
        for (Locale locale : locales) {
            if (shortLanguageCode.equalsIgnoreCase(locale.getLanguage()) && !locale.getCountry().isEmpty()) {
                if (firstLanguageCode) {
                    firstLanguageCode = false;
                    fullLanguageCode = locale.getLanguage() + "-" + locale.getCountry();
                }
                if (shortLanguageCode.equalsIgnoreCase(locale.getCountry())) {
                    hasThesame = true;
                    break;
                }
                Log.e("MMMM", "getFirstFullLanguageCode if " + locale.getLanguage() + "-" + locale.getCountry());

            }
        }
        if (Boolean.TRUE.equals(hasThesame)) {
            fullLanguageCode = shortLanguageCode.toLowerCase() + "-" + shortLanguageCode.toUpperCase();
        }
        return fullLanguageCode;
    }

    /**
     * Cette méthode permet d'inialiser le TTS d'android
     */
    public void initTTSAndroid() {
        ttsAndroid = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // TTS is initialized successfully
                Log.e("TTS_Android", "TTS is initialized successfully");
            } else {
                Log.e("TTS_Android", "TTS Initilization Failed!" + status);

            }

        }, "com.google.android.tts");
    }
    private AudioRecord initAudioRecordWithFallback() {
        int sampleRate = 8000;
        int[] audioSources = new int[]{
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.VOICE_RECOGNITION
        };

            for (int src : audioSources) {
                int minBuf = AudioRecord.getMinBufferSize(sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT);
                Log.i(TAG_STREAMING, "Trying AudioRecord sr=" + sampleRate + " src=" + src + " minBuf=" + minBuf);
                if (minBuf == AudioRecord.ERROR || minBuf == AudioRecord.ERROR_BAD_VALUE) continue;
                int buf = Math.max(minBuf * 2, sampleRate / 10); // safety margin
                try {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        Log.w(TAG_STREAMING, "RECORD_AUDIO permission not granted");
                        return null;
                    }
                    AudioRecord ar = new AudioRecord(src, sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT, buf);
                    if (ar.getState() == AudioRecord.STATE_INITIALIZED) {
                        // update globals used elsewhere
                        // Note: SAMPLE_RATE constant may be used elsewhere; prefer to use local sr where needed
                        Log.i(TAG_STREAMING, "AudioRecord initialized (sr=" + sampleRate + ", src=" + src + ", buf=" + buf + ")");
                        return ar;
                    } else {
                        ar.release();
                    }
                } catch (Exception e) {
                    Log.w(TAG_STREAMING, "initAudioRecordWithFallback exception", e);
                }
            }

        return null;
    }

    public void startListeningQuestionWav(Activity activity){
        Log.d(TAG_STREAMING, "startListeningQuestionWav start");
        Log.i("FZE", "STT non local flux: startListeningQuestionWav");
        alReadyHadSpoke=false;
        activityTemp = activity;
        // Post sur le thread UI de façon sûre (activity peut être null / finishing)
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(this::listeningAnimation);
        speechRecognizer.destroy();
        stopListening(activity);

        if (isRecording) {
            Log.d(TAG_STREAMING, "Already recording");
            return;
        }

        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
            Log.e(TAG_STREAMING, "RECORD_AUDIO permission not granted");
            // notify to request permission
            notifyObservers("RECORD_AUDIO_PERMISSION_NEEDED");
            return;
        }

        // init audioRecord with fallback
        AudioRecord ar = initAudioRecordWithFallback();
        if (ar == null) {
            Log.e(TAG_STREAMING, "No valid AudioRecord configuration found");
            return;
        }
        audioRecord = ar;

        String outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.pcm";

        try {
            setLed("listening");
            audioRecord.startRecording();
            Log.i(TAG_STREAMING, "after startRecording: recordingState=" + audioRecord.getRecordingState() + " audioRecord state: " + audioRecord.getState());
            Log.i("FZE", "STT non local flux: startRecording state=" + audioRecord.getRecordingState());
            if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                Log.e(TAG_STREAMING, "startRecording did not put AudioRecord into RECORDING state");
                audioRecord.release();
                audioRecord = null;
                return;
            }
            isRecording = true;
            currentState = "";
            // start VAD (keeps using configured sample rate - VAD expects matching sample rate)
            Log.i("FZE", "STT non local flux: startVAD");
            startVAD();
            Log.i("FZE", "STT non local flux: processAudio file=" + outputFile);
            processAudio(outputFile);
        } catch (Exception e) {
            Log.e(TAG_STREAMING, "Failed to start recording", e);
            if (audioRecord != null) {
                try { audioRecord.release(); } catch (Exception ignored) {
                    Log.i(TAG, "startListeningQuestionWav: Exception "+ignored.getMessage());
                }
                audioRecord = null;
            }
        }
    }



    /*
     * VAD library only accepts 16-bit mono PCM audio stream and can work with the next Sample Rates and Frame Sizes :
     *
     *  Valid Sample Rate     Valid Frame Size
     *      8000Hz              80, 160, 240
     *      16000Hz             160, 320, 480
     *      32000Hz             320, 640, 960
     *      48000Hz             480, 960, 1440
     *
     * the number of bytes received by the BlueMic is by default 40 (AUDIO_PACKAGE_SIZE=40).
     * in order to be able to pass the audio stream to the VAD function with a SampleRate of 8000Hz
     * we have to find a way to modify the number of processed bytes to 80 bytes (AUDIO_PACKAGE_SIZE=80)
     *
     * we are going to build a new shorts[80] which is the combination of two shorts[40] received from the BlueMic.
     *
     * Algo:
     * I store each new short[40] in a circularBuffer and wait for the next short[40] to be received.
     * Once received, I combine the two in a short[80] and send it in the callback : onNewAudioData
     */
    private final VadListener vadListener = new VadListener() {
        @Override
        public void onSpeechDetected() {
            Log.d(TAG_STREAMING, "Speech detected!");
            Log.i("FZE", "STT non local flux: VAD speech detected");
            // lorsque la parole est détectée
            if (!currentState.equals("SPEECH")) {
                currentState = "SPEECH";
                alReadyHadSpoke=true;
                if (!getParamFromFile("Volume_reduction",configurationFilePseudo).trim().equals("")
                        && !getParamFromFile("Volume_reduction",configurationFilePseudo).trim().equals("0")
                        && !getParamFromFile("Duration_sound_level_checked",configurationFilePseudo).trim().equals("")
                        && !getParamFromFile("Duration_sound_level_checked",configurationFilePseudo).trim().equals("0")
                ){
                    handler2.postDelayed(periodicTask,Integer.valueOf(getParamFromFile("Duration_sound_level_checked",configurationFilePseudo))*1000 );
                }
            }
        }

        @Override
        public void onNoiseDetected() {
            Log.d(TAG_STREAMING, "Noise detected!");
            Log.i("nv", "STT non local flux: VAD noise detected");

            // lorsque du bruit est détecté
            if (!currentState.equals("NOISE")) {
                currentState = "NOISE";
                if(alReadyHadSpoke){
                    alReadyHadSpoke=false;
                    stopProcessus =false;
                    stopRecording();
                    stopRecordingSTT(true,false);
                }

            }
        }

    };
    private void startVAD() {
        Log.i(TAG, "startVAD: start 1");
        Log.i("FZE", "STT non local flux: startVAD init");
        int silenceTime;
        if (!getParamFromFile("Silence_time",configurationFilePseudo).trim().equals("")){
            try {
                silenceTime= Integer.parseInt(getParamFromFile("Silence_time",configurationFilePseudo).trim()) *1000;
            }
            catch (Exception e){
                silenceTime = 500;
            }
        }
        else{
            silenceTime = 500;
        }
        // Configure and start VAD
        vad = new Vad(VadConfig.newBuilder()
                .setSampleRate(VadConfig.SampleRate.SAMPLE_RATE_8K)
                .setFrameSize(VadConfig.FrameSize.FRAME_SIZE_80)
                .setMode(VadConfig.Mode.VERY_AGGRESSIVE)
                .setSilenceDurationMillis(silenceTime)
                .setVoiceDurationMillis(500)
                .build());
        vad.start();
        Log.i("FZE", "STT non local flux: VAD started");
    }

    private void processAudio(String outputFile) {
        Log.i(TAG, "processAudio: start 1");
        Log.i(TAG, "processAudio: start 1 FILE"+outputFile);
        Log.i("FZE", "STT non local flux: processAudio start");

        new Thread(() -> {
            short[] buffer = new short[BUFFER_SIZE / 2]; // Divided by 2 because each short is 2 bytes
            try {
                Log.i(TAG, "processAudio: start try");
                // Vérifier que audioRecord n'est pas null
                if (audioRecord == null) {
                    Log.w(TAG, "processAudio: audioRecord is null, exiting thread");
                    return;
                }
                Log.i(TAG, "recordingState: " + audioRecord.getRecordingState());
                FileOutputStream fos = new FileOutputStream(outputFile);
                while (isRecording) {
                    //  Vérifier que audioRecord n'est pas null à chaque itération
                    if (audioRecord == null) {
                        Log.w(TAG, "processAudio: audioRecord became null, stopping loop");
                        fos.close();
                        return;
                    }
                    Log.i(TAG, "processAudio: start try FOS "+fos);
                    int numRead = audioRecord.read(buffer, 0, buffer.length);
                    Log.i(TAG, "processAudio: start try : "+numRead);

                    if (numRead > 0) {
                        Log.i(TAG, "processAudiof: >0");
                        // Vérifier que vad n'est pas null non plus
                        if (vad != null) {
                            vad.addContinuousSpeechListener(buffer, vadListener);
                        }
                        Log.i(TAG, "processAudiof: fos");
                        fos.write(shortArrayToByteArray(buffer), 0, numRead * 2);
                    }
                }
                fos.close();
            } catch (NullPointerException e) {
                Log.e(TAG, "❌ processAudio: NullPointerException (likely audioRecord was released): " + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "❌ processAudio: IOException: " + e.getMessage());
                e.printStackTrace();
            } finally {
                Log.e(TAG,"processAudioFinally");
                Log.i("FZE", "STT non local flux: processAudio end");
            }
        }).start();
    }

    // Convertir un tableau de shorts en un tableau de bytes (pour le buffer combiné)
    private byte[] shortArrayToByteArray(short[] shortArray) {
        int length = shortArray.length;
        byte[] byteArray = new byte[length * 2]; // Each short is 2 bytes
        for (int i = 0; i < length; i++) {
            byteArray[i * 2] = (byte) (shortArray[i] & 0xFF);
            byteArray[i * 2 + 1] = (byte) ((shortArray[i] >> 8) & 0xFF);
        }
        return byteArray;
    }
    public void readAudioBase64(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            Log.w(getClass().getSimpleName(), "readAudioBase64: base64 string empty");
            return;
        }
        Log.i(TAG, "readAudioBase64: start");
        // Décodage et écriture sur un thread background
        new Thread(() -> {
            Log.i(TAG, "readAudioBase64: start");
            File outFile = new File(Environment.getExternalStorageDirectory(), "audio_response.wav");

            try {
                Log.i(TAG, "readAudioBase64: start try");
                byte[] audioBytes = Base64.decode(base64, Base64.DEFAULT);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(audioBytes);
                    fos.flush();

                    Log.i(TAG, "readAudioBase64: audio written to " + outFile.getAbsolutePath());
// Lecture sur le thread UI
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(() -> {
                        if (outFile == null || !outFile.exists() || outFile.length() == 0) {
                            Log.e(TAG, "readAudioBase64: fichier introuvable ou vide: " + (outFile != null ? outFile.getAbsolutePath() : "null"));
                            return;
                        }
                        MediaPlayer mp = new MediaPlayer();
                        try {
                            mp.setDataSource(outFile.getAbsolutePath());
                            mp.setAudioAttributes(new AudioAttributes.Builder()
                                             .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build());
                            mp.setOnPreparedListener(mediaPlayer -> {
                                mediaPlayer.setVolume(1f, 1f);
                                mediaPlayer.start();
                            });
                            mp.setOnCompletionListener(mediaPlayer -> {
                                try {
                                    mediaPlayer.reset();
                                    mediaPlayer.release();
                                } catch (Exception ignored) {
                                    Log.i(TAG, "readAudioBase64: "+ignored.getMessage());
                                }
                                Log.i(TAG, "readAudioBase64: lecture terminée");
                            });
                            mp.prepareAsync();
                        } catch (IOException e) {
                            Log.e(TAG, "readAudioBase64: erreur préparation MediaPlayer", e);
                        }

                    });
                }
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), "readAudioBase64: decode/write failed", e);
                if (outFile != null && outFile.exists()) outFile.delete();
            }
        }).start();
    }
    private String convertBase64Wav() {
        try {
            String inputPcm = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.pcm";
            String outputWav = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.wav";

            // Convert PCM → WAV
            PcmToWavConverter.convert(inputPcm, outputWav);

            File wavFile = new File(outputWav);
            if (!wavFile.exists()) {
                Log.e("FileError", "WAV file does not exist");
                return null;
            }

            // Read WAV → bytes
            byte[] wavBytes = Files.readAllBytes(wavFile.toPath());

            // Convert bytes → Base64
            return Base64.encodeToString(wavBytes, Base64.NO_WRAP);

        } catch (Exception e) {
            Log.e("ERR", "Error converting PCM → WAV → Base64", e);
            return null;
        }
    }

    private byte[] readAudioFile() throws IOException {
        // Convert PCM data to WAV format
        String outputFileWav = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.wav";
        PcmToWavConverter.convert(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.pcm", outputFileWav);

        Log.d("FilePath", "File path: " + outputFileWav);
        File audioFileWav = new File(outputFileWav);
        if (audioFileWav.exists()) {
            return Files.readAllBytes(audioFileWav.toPath());
        } else {
            // Handle the case where the file does not exist
            Log.e("FileError", "The file does not exist at the specified path.");
            return null;
        }
    }
    //#endregion ******************************************************* TTS **********************************************************************


    //#region ******************************************************* LEDs **********************************************************************

    public void playUsingReadSpeakerCaseError(String text, ITTSCallbacks ittsCallbacks) {
        if (Boolean.TRUE.equals(usingReadSpeaker)) {
            ittsCallbacks.onError("error is in readspeaker not ttsAndroid");
            return;
        }
        String voice = getVoiceForCurrentLanguage();
        showAppropriateToastForTTS();

        BuddySDK.Speech.setSpeakerVoice(voice);

        if (BuddySDK.Speech.isReadyToSpeak()) {
            Log.e("HOU_TEST", "start play from TTS error");
            BuddySDK.Speech.startSpeaking(
                    text,
                    LabialExpression.SPEAK_NEUTRAL,
                    new ITTSCallback.Stub() {
                        @Override
                        public void onSuccess(String s) {
                            ittsCallbacks.onSuccess(s);
                            Log.e("HOU_TEST", "start play from TTS error  onSuccess");
                        }

                        @Override
                        public void onPause() {
                            // onPause
                        }

                        @Override
                        public void onResume() {
                            // onResume
                        }

                        @Override
                        public void onError(String s) {
                            ittsCallbacks.onError(s);
                            Log.e("HOU_TEST", "start play from TTS error  onERRor");
                        }
                    });
        } else {
            Log.e("HOU_TEST", "else---------- start play from TTS error");
            ittsCallbacks.onError("ReadSpeaker indisponible");
        }
    }

    private String getVoiceForCurrentLanguage() {
        switch (getCurrentLanguage()) {
            case "en":
            case "de":
            case "es":
                return "kate";
            case "fr":
                return "roxane";
            default:
                return "kate";
        }
    }

    private void showAppropriateToastForTTS() {
        String lang = getCurrentLanguage();
        String ttsType = getparam("TTS");
        String secondTTS = getSecondTTSfromTTSList();

        if (lang.equals("en")) {
            showToastForEnglish(ttsType, secondTTS);
        } else if (lang.equals("fr")) {
            showToastForFrench(ttsType, secondTTS);
        } else if (lang.equals("de")) {
            showToastForGerman(ttsType);
        } else if (lang.equals("es")) {
            showToast(getString(R.string.toast_tts_android_indispo_es));
        } else {
            showToastForOtherLanguages(ttsType);
        }
    }

    private void showToastForEnglish(String ttsType, String secondTTS) {
        String toastTtsGoogleApiIndispo = getString(R.string.toast_tts_googleApi_indispo_en);
        toastTtsAndroidIndispo = getString(R.string.toast_tts_android_indispo_en);
        if (ttsType.equalsIgnoreCase("ApiGoogle") || (ttsType.equalsIgnoreCase("ReadSpeaker") && secondTTS.equalsIgnoreCase("ApiGoogle"))) {
            showToast(toastTtsGoogleApiIndispo);
        } else {
            showToast(toastTtsAndroidIndispo);
        }
    }

    private void showToastForFrench(String ttsType, String secondTTS) {
        toastTtsAndroidIndispo = getString(R.string.toast_tts_android_indispo_fr);
        String toastTtsGoogleApiIndispo = getString(R.string.toast_tts_googleApi_indispo_fr);
        if (ttsType.equalsIgnoreCase("ApiGoogle") || (ttsType.equalsIgnoreCase("ReadSpeaker") && secondTTS.equalsIgnoreCase("ApiGoogle"))) {
            showToast(toastTtsGoogleApiIndispo);
        } else {
            showToast(toastTtsAndroidIndispo);
        }
    }

    private void showToastForGerman(String ttsType) {
        toastTtsAndroidIndispo = getString(R.string.toast_tts_android_indispo_de);
        String toastTtsGoogleApiIndispo = getString(R.string.toast_tts_googleApi_indispo_de);
        if (ttsType.equalsIgnoreCase("ReadSpeaker")) {
            showToast(toastTtsGoogleApiIndispo);
        } else {
            showToast(toastTtsAndroidIndispo);
        }
    }

    private void showToastForOtherLanguages(String ttsType) {
        if (ttsType.equalsIgnoreCase("ReadSpeaker")) {
            getEnglishLanguageSelectedTranslator()
                    .translate(getString(R.string.toast_tts_googleApi_indispo_en))
                    .addOnSuccessListener(translatedText -> showToast(translatedText))
                    .addOnFailureListener(e -> showToast(getString(R.string.toast_tts_googleApi_indispo_en)));
        } else {
            getEnglishLanguageSelectedTranslator()
                    .translate(getString(R.string.toast_tts_android_indispo_en))
                    .addOnSuccessListener(translatedText -> {
                        toastTtsAndroidIndispo = translatedText;
                        showToast(toastTtsAndroidIndispo);
                    })
                    .addOnFailureListener(e -> {
                        toastTtsAndroidIndispo = getString(R.string.toast_tts_android_indispo_en);
                        showToast(toastTtsAndroidIndispo);
                    });
        }
    }


    /**
     * La fonction setLed() permet de changer la couleur des LEDs.
     *
     * @param state : "listening" : pour la couleur GREEN #53B300
     *              "neutral"   : pour la couleur BLUE  #00D4D0
     *              "off"       : pour la couleur BLACK #000000
     */
    public void setLed(String state) {
        SystemClock.sleep(200);
        try {
            switch (state) {
                case "listening":
                    BuddySDK.USB.updateAllLed("#53B300", iUsbLedCommandRsp);
                    break;
                case "neutral":
                    BuddySDK.USB.updateAllLed("#00D4D0", iUsbLedCommandRsp);
                    break;
                case "off":
                    BuddySDK.USB.updateAllLed("#000000", iUsbLedCommandRsp);
                    break;
                default:
                    BuddySDK.USB.updateAllLed("#00D4D0", iUsbLedCommandRsp);
            }
            Log.i(TAG, "Changement de couleurs des LEDs [" + state + "]");
        } catch (Exception e) {
            Log.e(TAG, "Erreur pendant le changement de couleurs des LEDs [" + state + "]: " + e);
        }
    }

    //#endregion ******************************************************* LEDs **********************************************************************

    //#region ******************************************************* Fonctions utiles *********************************************************

    /**
     * Cette méthode permet de personaliser l'affichage de toast
     * @param message est le message à afficher dans le toast
     */
    public void showToast(String message) {
        if (mToast != null) {
            mToast.cancel();
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            mToast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
            mToast.setDuration(Toast.LENGTH_LONG);
            mToast.show();
        });
    }

    public void showInputDialog(Activity activity, String message, String attention) {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();

            Log.w("BuddyGPTApp", "Dialog shown: ");
            // Create a new Dialog and remove default title for a more modern look
            if (activity == null || activity.isFinishing()) {
                Log.e("BuddyGPTApp", "Activity is null or finishing, cannot show dialog");
                return;
            }
            dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            // Create a LinearLayout with improved styling
            LinearLayout layout = new LinearLayout(activity);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(60, 50, 60, 50);  // Updated padding for better spacing
            layout.setBackgroundColor(Color.parseColor("#000000")); // Background color
            layout.setGravity(Gravity.START);  // Left-align the main layout content

            // Set rounded corners for the dialog layout
            GradientDrawable layoutDrawable = new GradientDrawable();
            layoutDrawable.setColor(Color.WHITE);
            layoutDrawable.setCornerRadius(30);  // Rounded corners for the dialog
            layout.setBackground(layoutDrawable);

            // Create and style the TextView for the message
            TextView textView = new TextView(activity);
            textView.setText(attention + "\n" + message);
            textView.setTextColor(Color.BLACK);
            textView.setTextSize(18);  // Adjusted text size
            textView.setPadding(0, 0, 0, 20);  // Bottom padding for spacing before button
            textView.setGravity(Gravity.START);  // Align text to the left

            // Add the TextView to the layout
            layout.addView(textView);

            // Create a LinearLayout for the button and align it to the right
            LinearLayout buttonLayout = new LinearLayout(activity);
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonLayout.setGravity(Gravity.RIGHT);  // Align button layout to the right

            Button okButton = new Button(activity);
            okButton.setText("OK");
            okButton.setTextColor(Color.WHITE);
            okButton.setTextSize(16);  // Reduced text size for smaller appearance
            okButton.setPadding(5, 2, 5, 2);  // Smaller padding for compact look

            // Set smaller width and height for the OK button
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    WRAP_CONTENT,  // Width wraps content
                    WRAP_CONTENT  // Height wraps content
            );
            okButton.setLayoutParams(buttonParams);

            // Set rounded corners and background color for the OK button
            GradientDrawable buttonDrawable = new GradientDrawable();
            buttonDrawable.setColor(Color.parseColor("#00d4d1")); // Button color
            buttonDrawable.setCornerRadius(20);  // Smaller rounded corners for compact look
            okButton.setBackground(buttonDrawable);

            // Set onClick listener to dismiss dialog
            okButton.setOnClickListener(v -> dialog.dismiss());

            // Add the OK button to the button layout, and then add button layout to main layout
            buttonLayout.addView(okButton);
            layout.addView(buttonLayout);

            // Set the layout as the content view for the dialog
            dialog.setContentView(layout);

            // Adjust dialog width to 60% of the screen width
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.6);
            dialog.getWindow().setAttributes(params);
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            // Display the dialog
            try {
                dialog.show();
            } catch (WindowManager.BadTokenException e) {
                Log.w("BuddyGPT", "Dialog could not be shown: " + e.getMessage());
            }
        });
    }

    public void showInputDialog2(Activity activity, String message, String attention) {
        notifyObservers("end of timer");
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();

            Log.w("BuddyGPT", "Dialog shown: ");
            // Create a new Dialog and remove default title for a more modern look
            dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            // Create a LinearLayout with improved styling
            LinearLayout layout = new LinearLayout(activity);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(60, 50, 60, 50);  // Updated padding for better spacing
            layout.setBackgroundColor(Color.parseColor("#000000")); // Background color
            layout.setGravity(Gravity.START);  // Left-align the main layout content

            // Set rounded corners for the dialog layout
            GradientDrawable layoutDrawable = new GradientDrawable();
            layoutDrawable.setColor(Color.WHITE);
            layoutDrawable.setCornerRadius(30);  // Rounded corners for the dialog
            layout.setBackground(layoutDrawable);

            // Create and style the TextView for the message
            TextView textView = new TextView(activity);
            textView.setText(attention + "\n" + message);
            textView.setTextColor(Color.BLACK);
            textView.setTextSize(18);  // Adjusted text size
            textView.setPadding(0, 0, 0, 20);  // Bottom padding for spacing
            textView.setGravity(Gravity.START);  // Align text to the left

            // Add the TextView to the layout
            layout.addView(textView);

            // Set the layout as the content view for the dialog
            dialog.setContentView(layout);

            // Adjust dialog width to 60% of the screen width
            WindowManager.LayoutParams params = Objects.requireNonNull(dialog.getWindow()).getAttributes();
            params.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.6);
            dialog.getWindow().setAttributes(params);
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            // Display the dialog
            try {
                dialog.show();
            } catch (WindowManager.BadTokenException e) {
                Log.w("BuddyGPT", "Dialog could not be shown: " + e.getMessage());
            }
            dialog.setCanceledOnTouchOutside(true);
            // Listener for dismissing the dialog when clicked outside
            dialog.setOnCancelListener(dialogInterface ->
                    setAnimation("BuddyFace_Neutral"));
            notifyObservers("QST_LAYOUT_DISMISSED");
            // Auto-dismiss the dialog after 30 seconds
            handler.postDelayed(() -> {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                    setAnimation("BuddyFace_Neutral");
                    notifyObservers("QST_LAYOUT_DISMISSED");
                }
            }, 30000);  // 30000 milliseconds = 30 seconds
        });
    }

    public void setAnimation(String emotion) {
        Log.i(TAG, "setAnimation: test " + emotion);
        Log.i("BBB", "setAnimation requested: " + emotion);
        // Store the current emotion for later restoration
        currentActiveEmotion = emotion;
        Log.i("BBB", "📌 Storing currentActiveEmotion: " + emotion);
        if (emotion.equalsIgnoreCase("BuddyFace_Happy")) {
            BuddySDK.UI.setFacialExpression(FacialExpression.HAPPY, 1);
            Log.i("BBB", "Applied emotion: BuddyFace_Happy");
        } else if (emotion.equalsIgnoreCase("BuddyFace_Thinking")) {
            BuddySDK.UI.setFacialExpression(FacialExpression.THINKING, 1);
            Log.i("BBB", "Applied emotion: BuddyFace_Thinking");
        } else if (emotion.equalsIgnoreCase("BuddyFace_Sick")) {
            BuddySDK.UI.setFacialExpression(FacialExpression.SICK, 1);
            Log.i("BBB", "Applied emotion: BuddyFace_Sick");
        } else if (emotion.equalsIgnoreCase("BuddyFace_Love")) {
            BuddySDK.UI.setFacialExpression(FacialExpression.LOVE, 1);
            Log.i("BBB", "Applied emotion: BuddyFace_Love");
        } else if (emotion.equalsIgnoreCase("BuddyFace_Tired")) {
            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
            Log.i("BBB", "Applied emotion: BuddyFace_Tired");
        } else if (emotion.equalsIgnoreCase("BuddyFace_Listening")) {
            BuddySDK.UI.setFacialExpression(FacialExpression.LISTENING, 1);
            Log.i("BBB", "Applied emotion: BuddyFace_Listening");
        } else if (emotion.equalsIgnoreCase("BuddyFace_Surprised")) {
            BuddySDK.UI.setFacialExpression(FacialExpression.SURPRISED, 1);
            Log.i("BBB", "Applied emotion: BuddyFace_Surprised");
        } else if (emotion.equalsIgnoreCase("BuddyFace_Grumpy")) {
            BuddySDK.UI.setFacialExpression(FacialExpression.GRUMPY, 1);
            Log.i("BBB", "Applied emotion: BuddyFace_Grumpy");
        } else if (emotion.equalsIgnoreCase("BuddyFace_Scared")) {
            BuddySDK.UI.setFacialExpression(FacialExpression.SCARED, 1);
            Log.i("BBB", "Applied emotion: BuddyFace_Scared");
        } else if (emotion.equalsIgnoreCase("BuddyFace_Angry")) {
            BuddySDK.UI.setFacialExpression(FacialExpression.ANGRY, 1);
            Log.i("BBB", "Applied emotion: BuddyFace_Angry");
        } else if (emotion.equalsIgnoreCase("BuddyFace_Sad")) {
            Log.i(TAG, "setAnimation: sad");
            BuddySDK.UI.setFacialExpression(FacialExpression.SAD, 1);
            Log.i("BBB", "Applied emotion: BuddyFace_Sad");
        } else if (emotion.equalsIgnoreCase("BuddyFace_Neutral")) {
            Log.i(TAG, "setAnimation: neutral");
            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
            Log.i("BBB", "Applied emotion: BuddyFace_Neutral (now neutral)");
        }
    }

    public boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean nombreDeMotsCheck(String chaine) {
        // Utilisation d'une expression régulière pour vérifier si la chaîne contient au moins 3 mots
        // Crée un itérateur de mots pour la langue par défaut (la détection automatique de la langue)
        BreakIterator wordIterator = BreakIterator.getWordInstance();

        // Définit la chaîne de texte sur laquelle l'itérateur de mots va travailler
        wordIterator.setText(chaine);

        int wordCount = 0;

        // Boucle pour compter les mots en utilisant l'itérateur de mots
        while (wordIterator.next() != BreakIterator.DONE) {
            int currentIndex = wordIterator.current();

            // Vérifie si l'index actuel n'est pas un espace
            if (Character.isLetterOrDigit(chaine.charAt(currentIndex - 1))) {
                wordCount++;
            }
        }
        Log.e("TTS", "nombre de mots  ------------ " + wordCount);
        return wordCount >= Integer.parseInt(getParamFromFile("Number_of_words", configurationFilePseudo));
    }

    /**
     * Cette méthode permet de checker si le device est connecté à l'internet
     */
    public boolean isConnectedToInternet() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;
        cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final Network n = cm.getActiveNetwork();
        if (n != null) {
            final NetworkCapabilities nc = cm.getNetworkCapabilities(n);
            if (nc != null) {
                if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    haveConnectedWifi = true;

                } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    haveConnectedMobile = true;
                }
            }

            return haveConnectedWifi || haveConnectedMobile;
        }
        return false;
    }

    /**
     * cette méthode permet de récupérer la langue à utiliser
     */
    public String getCurrentLanguage() {
        switch (this.langue.getNom()) {
            case langueFr:
                return "fr";
            case langueEn:
                return "en";
            case langueEs:
                return "es";
            case langueDe:
                return "de";
            default:
                return this.langue.getLanguageCode();
        }
    }

    /**
     * cette fonction permet de stocker un paramètre dans la mémoire
     */
    public void setparam(String a, String b) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(a, b);
        editor.apply();
    }

    /**
     * cette fonction permet de récupérer la valeur du paramètre stocké
     */
    public String getparam(String a) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getString(a, "");
    }

    /**
     * la fonction notifyObservers() permet d'envoyer un message "notification" aux classes qui implémentent IDBObserver.
     *
     * @param message : le message à envoyer
     */
    public void notifyObservers(String message) {
        Log.i(TAG, "notifyObservers: " + message);
        for (int i = 0; i < observers.size(); i++) {
            try {
                IDBObserver ob = observers.get(i);
                ob.update(message);
            } catch (IOException e) {
                Log.e(TAG, "Erreur lors de l'envoi de la notification aux observateurs [ " + message + " ] :" + e);
            }
        }
    }

    /**
     * la fonction registerObserver() permet de s'enregistrer au pattern Observer afin de recevoir les notifications
     */
    public void registerObserver(IDBObserver observer) {
        observers.add(observer);
    }

    /**
     * la fonction removeObserver() permet de se désinscrire du pattern Observer pour ne plus recevoir les notifications
     */
    public void removeObserver(IDBObserver observer) {
        observers.remove(observer);
    }

    /**
     * Cette fonction permet de cacher les barres du système
     */
    public int hideSystemUI(Activity myActivityReference) {
        View decorView = myActivityReference.getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
        return (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /**
     * Cette fonction permet de récupérer un paramètre depuis le fichier de configuration
     */
    public String getParamFromFile(String param, String fileName) {
        File directory = new File(getString(R.string.path), "BuddyGPT");
        CustomProperties props = ConfigurationFile.props;
        CustomProperties newProps = ConfigurationFile.loadproperties(directory, fileName, props);
        return newProps.getProperty(param);
    }

    /**
     * Cette fonction permet de créer le fichier de configuration
     */
    public String createPropertiesFile() {
        File directory = new File(getString(R.string.path), "BuddyGPT");
        String initOrMajOrNone = ConfigurationFile.createConfigurationFile(directory);
        Log.i(TAG, "createPropertiesFile: initOrMajOrNone "+initOrMajOrNone );

        init(initOrMajOrNone);
        notYet = false;
        return initOrMajOrNone;
    }

    /**
     * Cette fonction permet de changer le volume du device
     */
    public void setVolume(int percentage, int type) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);


        int volume = getClosestInt((double) (percentage * maxVolume) / 100);


        if (audioManager.isBluetoothScoOn()) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
            audioManager.setStreamVolume(6, volume, type);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        } else {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, type);
        }
    }

    /**
     * cette fonction permet de récupérer le volume du device
     */
    public int getVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    /**
     * Cette fonction permet de récupérer le volume max du device
     */
    public int getMaxVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.isBluetoothScoOn())
            return audioManager.getStreamMaxVolume(6);
        else
            return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    /**
     * Cette fonction permet de récupérer l'entier le plus proche au double passé en argument
     */
    public int getClosestInt(double x) {
        return (int) Math.rint(x);
    }

    //fonction pour push files


    /**
     *  Cleanup all running handlers and threads when app closes
     */
    public void cleanup() {
        Log.i(TAG, " cleanup: Stopping all handlers and threads");

        //  CRUCIAL : Arrêter isRecording IMMÉDIATEMENT
        isRecording = false;
        Log.i(TAG, " cleanup: isRecording set to false");

        //  Arrêter les threads AVANT de libérer les ressources
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            try {
                thread.join(1000);  // ← Attendre que le thread se termine
                Log.i(TAG, " cleanup: thread joined successfully");
            } catch (InterruptedException e) {
                Log.w(TAG, " cleanup: thread join interrupted: " + e.getMessage());
            }
        }

        if (thread1 != null && thread1.isAlive()) {
            thread1.interrupt();
            try {
                thread1.join(1000);
                Log.i(TAG, " cleanup: thread1 joined successfully");
            } catch (InterruptedException e) {
                Log.w(TAG, " cleanup: thread1 join interrupted: " + e.getMessage());
            }
        }

        // PUIS libérer audioRecord
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
                Log.i(TAG, " cleanup: audioRecord stopped");
            } catch (Exception e) {
                Log.e(TAG, " cleanup: Error stopping audioRecord: " + e.getMessage());
            }
        }

        if (handler2 != null) {
            handler2.removeCallbacksAndMessages(null);
            Log.i(TAG, " cleanup: handler2 stopped");
        }
        if (retryHotwordHandler != null) {
            retryHotwordHandler.removeCallbacksAndMessages(null);
            Log.i(TAG, " cleanup: retryHotwordHandler stopped");
        }
        if (noMatchHandler != null) {
            noMatchHandler.removeCallbacksAndMessages(null);
            Log.i(TAG, " cleanup: noMatchHandler stopped");
        }
        if (periodicTask != null) {
            handler2.removeCallbacks(periodicTask);
            Log.i(TAG, " cleanup: periodicTask stopped");
        }
        // Arrêter VAD
        if (vad != null) {
            vad.stop();
            Log.i(TAG, " cleanup: VAD stopped");
        }

        // Arrêter STT
        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel();
                speechRecognizer.destroy();
                Log.i(TAG, " cleanup: speechRecognizer stopped");
            } catch (Exception e) {
                Log.e(TAG, " cleanup: Error stopping speechRecognizer: " + e.getMessage());
            }
        }

        if (freeSpeechSttTask != null) {
            try {
                freeSpeechSttTask.stop();
                Log.i(TAG, " cleanup: freeSpeechSttTask stopped");
            } catch (Exception e) {
                Log.e(TAG, " cleanup: Error stopping freeSpeechSttTask: " + e.getMessage());
            }
        }

        // Arrêter TTS
        if (ttsAndroid != null) {
            try {
                ttsAndroid.stop();
                ttsAndroid.shutdown();
                Log.i(TAG, " cleanup: ttsAndroid stopped");
            } catch (Exception e) {
                Log.e(TAG, " cleanup: Error stopping ttsAndroid: " + e.getMessage());
            }
        }

        // Arrêter ResponseFromTeamGPT
        if (responseFromTeamGPT != null) {
            responseFromTeamGPT.reset();
            Log.i(TAG, " cleanup: responseFromTeamGPT stopped");
        }

        Log.i(TAG, " cleanup: Complete");
    }
    //#endregion ******************************************************* Fonctions utiles *********************************************************

}
