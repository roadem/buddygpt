package com.robotique.aevaweb.buddygpt.application;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.ibm.icu.text.BreakIterator;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.konovalov.vad.Vad;
import com.konovalov.vad.VadConfig;
import com.konovalov.vad.VadListener;
import com.robotique.aevaweb.buddygpt.R;
import com.robotique.aevaweb.buddygpt.chatbotresponse.ResponseFromTeamGPT;
import com.robotique.aevaweb.buddygpt.models.History;
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
import com.robotique.aevaweb.buddygpt.utilis.TtsGoogleApiListener;
import com.robotique.aevaweb.buddygpt.utilis.TtsGoogleC;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.StringTokenizer;

import darren.googlecloudtts.model.VoicesList;
import darren.googlecloudtts.parameter.AudioConfig;
import darren.googlecloudtts.parameter.AudioEncoding;
import darren.googlecloudtts.parameter.VoiceSelectionParams;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

public class BuddyGPTApplication extends BuddyApplication {
    private static final String TAG = "BuddyGPT_Application";
    private static final String TAG_BLUEMIC_STREAMING = "BuddyGPT_BlueMic_Streaming";
    private static final String TAG_STREAM = "MODE_STREAM";
    private static final String TAG_STREAM_USAGE = "MODE_STREAM_USAGE";
    private int listeningDuration;
    private int listeningAttempt;
    private int speakSpeed;
    private int speakVolume;
    public int max;
    private Replica question;

    private ResponseFromTeamGPT responseFromTeamGPT;
    private Replica reponse;
    private Setting setting;
    private Boolean fileCreate=true;
    private Session session;
    private ArrayList<Session> listSession=new ArrayList<>();
    private History history;
    private String switchdetectLanguage;
    private String switchModeStream;
    private String switchCommande;
    private String switchVisibility;
    private String switchBIDisplay;
    private String switchEmotion;
    private Uri fileup;
    private Boolean isSpeaking = false;
    private List<IDBObserver> observers = new ArrayList<>();
    private Boolean notYet=true;
    private int textSizeBullesPX;
    private boolean activityClosed =false;
    private Boolean startRecording=false;
    private Boolean usingEmotions=false;
    private ConnectivityManager cm;
    private int questionNumber = 0;
    private int currentQuestionNubmer = 0;
    private boolean alreadyGetAnswer = false;
    private boolean openaialreadySwitchEmotion = false ;
    private boolean timeoutExpired = false ;
    private long questionTime = 0;
    private String storedResponse = "";
    private Boolean buddyFaceisTired = false;
    private int bestTextSize = 0;
    private TextToSpeech tts_android;
    private Boolean shouldPlayEmotion = false;
    private String currentEmotion = "";
    private Boolean messageError = false;
    private Langue langue;

    private List<Replica> listRepGlobale = new ArrayList<>();
    private String french = "Français";
    private String english = "Anglais";
    private String spanish = "Espagnol";
    private String deutsch = "Allemand";
    private String listeningDurationPseudo = "listening_duration";
    private String listeningAttemptPseudo = "listening_attempt";
    private String speakVolumePseudo ="speak_volume";
    private String visibilityString = "switch_visibility";
    private String emotionString = "switch_emotion";
    private String detectionLanguageString = "Detection_de_langue";
    private String modeStreamString = "Mode_Stream";
    private String commandeString = "Commands";

    private Dialog dialog;
    private String configurationFilePseudo = "BuddyGPT.properties";
    private File fileupdate;
    private String langueFr = "Français";
    private String langueEn = "Anglais";
    private String langueEs = "Espagnol";
    private String langueDe = "Allemand";

    private  int currentIndexText = 0;

    private  boolean allTextPronoucedSuccess = true;
    private String[] texteToSpeakSplitted;
    public boolean Stop_TTS_ReadSpeaker = false;

    Runnable runnableListeningHotword;
    private Handler handlerListeningHotword =new Handler();
    private Handler handler2 = new Handler();
    private final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    private final Intent speechRecognizerIntent2 = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    android.speech.SpeechRecognizer speechRecognizer;
    private STTTask freeSpeechSttTask;



    private String header = "header";
    private String entete = "entete";
    private String cabecera = "Cabecera";
    private String kopfzeile = "Kopfzeile";
    private String openAIKey = "openAI_API_Key";
    private String langueInconfigurationFilePseudo = "Language";
    private Boolean initSharedpreferences = true;
    private Translator englishLanguageSelectedTranslator;
    private Translator frenchLanguageSelectedTranslator;
    private Translator languageSelectedEnglishTranslator;
    private TranslatorOptions options;
    private TranslatorOptions options1;
    private String translatedList="";
    private String languageDetected ="";
    private Boolean usingReadSpeaker;
    private boolean alreadyCalled =false;
    private Vad vad;


    private static final String TAG_STREAMING = "AudioCapture";
    private static final int SAMPLE_RATE = 8000; // Exemple : 8 kHz
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private String currentState = "";

    private Boolean endRecordingWhisperAudio = false;
    private Boolean stopProcessus = false;
    private Boolean alReadyHadSpoke=false;
    private Activity activityTemp;

    private long responseTime = 0;
    private Boolean answerHasExceededTimeOut =false;
    private TranscribeTask transcribeTask;
    private Thread thread;
    private Thread thread1;
    private Float previousVolume = Float.valueOf(0);
    private boolean streamMode = false;
    private EncodingRegistry registry;
    private Boolean appIsListeningToTheQuestion = false;
    private String toast_stt_android_indispo;
    private String toast_tts_android_indispo;
    private String toast_tts_googleApi_indispo;
    private TtsGoogleC googleCloudTTS;
    private VoicesList voiceList;
    private String chosenTTS = "";
    public int remainingAttempts;
    private Boolean appIsCurrentlyDealingWithTheQuestion = false;
    private Boolean BIExecution = false;
    private boolean alreadyChatting = false; // pour savoir si BUDDY doit prononcer l'invitation au dialogue ou non
    private String imeiDevice;
    private String imeiRobot;
    private String tokenHealysa;



    private boolean replicaEnd=false;

    public boolean isReplicaEnd() {
        return replicaEnd;
    }

    public void setReplicaEnd(boolean replicaEnd) {
        this.replicaEnd = replicaEnd;
    }public boolean isAlreadyChatting() {
        return alreadyChatting;
    }

    public void setAlreadyChatting(boolean alreadyChatting) {
        this.alreadyChatting = alreadyChatting;
    }


    public Boolean getBIExecution() {
        return BIExecution;
    }

    public void setBIExecution(Boolean BIExecution) {
        this.BIExecution = BIExecution;
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

    public String getChosenTTS() {
        return chosenTTS;
    }

    public void setChosenTTS(String chosenTTS) {
        this.chosenTTS = chosenTTS;
    }

    public EncodingRegistry getRegistry() {
        return registry;
    }

    public Dialog getDialog() {
        return dialog;
    }

    public void setDialog(Dialog dialog) {
        this.dialog = dialog;
    }

    public boolean getStreamMode() {
        return streamMode;
    }

    public void setStreamMode(boolean streamMode) {
        this.streamMode = streamMode;
    }


    public String getModel(){
        String model = "";
        String selectedChatbot = getparam("SelectedChatbot").toLowerCase();

        switch (selectedChatbot) {
            case "openai":
                model = getparam("Modele_Openai");
                break;
            case "customgpt":
                model = getparam("CustomGPT_model");
                break;
            case "mistral":
                model = getparam("Modele_Mistral");
                break;
            case "gemini":
                model = getparam("Modele_gemini");
                break;
            default:
                // Handle the case where the chatbot is unknown
                model = "";
                break;
        }
        return model;
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

    public Translator getLanguageSelectedEnglishTranslator() {
        return languageSelectedEnglishTranslator;
    }

    public void setLanguageSelectedEnglishTranslator(Translator languageSelectedEnglishTranslator) {
        this.languageSelectedEnglishTranslator = languageSelectedEnglishTranslator;
    }

    public Boolean getInitSharedpreferences() {
        return initSharedpreferences;
    }

    public void setInitSharedpreferences(Boolean initSharedpreferences) {
        this.initSharedpreferences = initSharedpreferences;
    }

    public Boolean getUsingReadSpeaker() {
        return usingReadSpeaker;
    }

    public void setUsingReadSpeaker(Boolean usingReadSpeaker) {
        this.usingReadSpeaker = usingReadSpeaker;
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

    public void setListeningDuration(int listeningDuration) {
        this.listeningDuration = listeningDuration;
    }

    public int getListeningAttempt() {
        return listeningAttempt;
    }

    public void setListeningAttempt(int listeningAttempt) {
        this.listeningAttempt = listeningAttempt;
    }

    public int getSpeakSpeed() {
        return speakSpeed;
    }

    public void setSpeakSpeed(int speakSpeed) {
        this.speakSpeed = speakSpeed;
    }

    public File getFileupdate() {
        return fileupdate;
    }

    public void setFileupdate(File fileupdate) {
        this.fileupdate = fileupdate;
    }

    public int getSpeakVolume() {
        return speakVolume;
    }

    public void setSpeakVolume(int speakVolume) {
        this.speakVolume = speakVolume;
    }


    public Replica getQuestion() {
        return question;
    }

    public void setQuestion(Replica question) {
        this.question = question;
    }

    public Replica getReponse() {
        return reponse;
    }

    public void setReponse(Replica reponse) {
        this.reponse = reponse;
    }

    public Setting getSetting() {
        return setting;
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

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public ArrayList<Session> getListSession() {
        return listSession;
    }

    public void setListSession(ArrayList<Session> listSession) {
        this.listSession = listSession;
    }

    public void listSessionClear(){
        listSession.clear();
    }

    public History getHistory() {
        return history;
    }

    public void setHistory(History history) {
        this.history = history;
    }

    public String getSwitchVisibility() {
        return switchVisibility;
    }

    public void setSwitchVisibility(String switchVisibility) {
        this.switchVisibility = switchVisibility;
    }

    public String getSwitchBIDisplay() {
        return switchBIDisplay;
    }

    public void setSwitchBIDisplay(String switchBIDisplay) {
        this.switchBIDisplay = switchBIDisplay;
    }

    public int getCurrentIndexText() {
        return currentIndexText;
    }

    public void setCurrentIndexText(int currentIndexText) {
        this.currentIndexText = currentIndexText;
    }

    public boolean isAllTextPronoucedSuccess() {
        return allTextPronoucedSuccess;
    }

    public void setAllTextPronoucedSuccess(boolean allTextPronoucedSuccess) {
        this.allTextPronoucedSuccess = allTextPronoucedSuccess;
    }

    public boolean isStop_TTS_ReadSpeaker() {
        return Stop_TTS_ReadSpeaker;
    }

    public void setStop_TTS_ReadSpeaker(boolean stop_TTS_ReadSpeaker) {
        Stop_TTS_ReadSpeaker = stop_TTS_ReadSpeaker;
    }

    public boolean isAlreadyCalled() {
        return alreadyCalled;
    }

    public void setAlreadyCalled(boolean alreadyCalled) {
        this.alreadyCalled = alreadyCalled;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void setRecording(boolean recording) {
        isRecording = recording;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public Boolean getStopProcessus() {
        return stopProcessus;
    }

    public void setStopProcessus(Boolean stopProcessus) {
        this.stopProcessus = stopProcessus;
    }

    public Boolean getAlReadyHadSpoke() {
        return alReadyHadSpoke;
    }

    public void setAlReadyHadSpoke(Boolean alReadyHadSpoke) {
        this.alReadyHadSpoke = alReadyHadSpoke;
    }

    public Float getPreviousVolume() {
        return previousVolume;
    }

    public void setPreviousVolume(Float previousVolume) {
        this.previousVolume = previousVolume;
    }

    public String getSwitchEmotion() {
        return switchEmotion;
    }

    public String getSwitchdetectLanguage() {
        return switchdetectLanguage;
    }

    public void setSwitchdetectLanguage(String switchdetectLanguage) {
        this.switchdetectLanguage = switchdetectLanguage;
    }

    public void setSwitchEmotion(String switchEmotion) {
        this.switchEmotion = switchEmotion;
    }

    public String getSwitchModeStream() {
        return switchModeStream;
    }

    public void setSwitchModeStream(String switchModeStream) {
        this.switchModeStream = switchModeStream;
    }

    public String getSwitchCommande() {
        return switchCommande;
    }

    public void setSwitchCommande(String switchCommande) {
        this.switchCommande = switchCommande;
    }

    public Uri getFileup() {
        return fileup;
    }

    public void setFileup(Uri fileup) {
        this.fileup = fileup;
    }

    public Boolean getSpeaking() {
        return isSpeaking;
    }

    public void setSpeaking(Boolean speaking) {
        isSpeaking = speaking;
    }

    public List<IDBObserver> getObservers() {
        return observers;
    }

    public void setObservers(List<IDBObserver> observers) {
        this.observers = observers;
    }

    public Boolean getNotYet() {
        return notYet;
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

    public ConnectivityManager getCm() {
        return cm;
    }

    public void setCm(ConnectivityManager cm) {
        this.cm = cm;
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

    public boolean isOpenaialreadySwitchEmotion() {
        return openaialreadySwitchEmotion;
    }

    public void setOpenaialreadySwitchEmotion(boolean openaialreadySwitchEmotion) {
        this.openaialreadySwitchEmotion = openaialreadySwitchEmotion;
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

    public Boolean getAnswerHasExceededTimeOut() {
        return answerHasExceededTimeOut;
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

    public Boolean getBuddyFaceisTired() {
        return buddyFaceisTired;
    }

    public void setBuddyFaceisTired(Boolean buddyFaceisTired) {
        this.buddyFaceisTired = buddyFaceisTired;
    }

    public int getBestTextSize() {
        return bestTextSize;
    }

    public void setBestTextSize(int bestTextSize) {
        this.bestTextSize = bestTextSize;
    }


    public Boolean getShouldPlayEmotion() {
        return shouldPlayEmotion;
    }

    public void setShouldPlayEmotion(Boolean shouldPlayEmotion) {
        this.shouldPlayEmotion = shouldPlayEmotion;
    }

    public String getCurrentEmotion() {
        return currentEmotion;
    }

    public void setCurrentEmotion(String currentEmotion) {
        this.currentEmotion = currentEmotion;
    }

    public Boolean getMessageError() {
        return messageError;
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

    public TextToSpeech getTts_android() {
        return tts_android;
    }

    public void setTts_android(TextToSpeech tts_android) {
        this.tts_android = tts_android;
    }

    public TtsGoogleC getGoogleCloudTTS() {
        return googleCloudTTS;
    }

    public void setGoogleCloudTTS(TtsGoogleC googleCloudTTS) {
        this.googleCloudTTS = googleCloudTTS;
    }

    public VoicesList getVoiceList() {
        return voiceList;
    }

    public void setVoiceList(VoicesList voiceList) {
        this.voiceList = voiceList;
    }

    public String getImeiDevice() {
        return imeiDevice;
    }

    public String getTokenHealysa() {
        return tokenHealysa;
    }

    public void setTokenHealysa(String tokenHealysa) {
        this.tokenHealysa = tokenHealysa;
    }

    public void setImeiDevice(String imeiDevice) {
        this.imeiDevice = imeiDevice;
    }

    /**
     * initialisations
     */
    @Override
    public void onCreate() {
        super.onCreate();
        AudioManager amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
        SettingsContentObserver mSettingsContentObserver = new SettingsContentObserver(new Handler(), getApplicationContext());
        getContentResolver().registerContentObserver(
                android.provider.Settings.System.CONTENT_URI, true,
                mSettingsContentObserver);


        speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(getApplicationContext());

        //create a new EncodingRegistry to use JTokkit
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    registry = Encodings.newDefaultEncodingRegistry();
                }
            }).start();
        } catch (Exception e) {
            Log.e("MODE_STREAM_USAGE", "Exception when creating a new EncodingRegistry to use JTokkit : " + e);
            e.printStackTrace();
        }
    }

    public void init() throws IOException {
        Log.e("MRAA", "init");
        remainingAttempts = Integer.parseInt(getParamFromFile("Number_listens", configurationFilePseudo).trim()) - 1;
        if (getparam("firstLaunch").equals("")) {
            setparam("firstLaunch", "true");
        }


//        //initialisation du mail du destinataire
//        if (getparam("Mail_Destination").equals("")) {
//            setparam("Mail_Destination", getParamFromFile("Mail_Destination", configurationFilePseudo));
//        }
        initLanguageSetting();
        initListeningSettings();
//        initProjectID();

        initSpeakVolumeSetting();
        initVisibilitySetting();
//        initSTTSetting();
        //initTTSSetting();
//        initOpenAiSettings();
        initEmotionSetting();
        initLanguageDetectionSetting();
        initChatTextSize();
        //initModeStreamSetting();
        //initCommandeSetting();
      //  initBIDisplay();
       // initTracking();
       // Log.e("MRAA", "init google api");
//        if (!alreadyCalled) {
//            Log.e("MRAA", "init google api out if");
//            if (getparam("STT_chosen").equalsIgnoreCase("Google")) {
//                Log.e("MRAA", "init google api inside if");
//                alreadyCalled = true;
//                releaseGoogleAPI();
//                initGoogleAPI();
//            }
//        }
        notifyObservers("properties file done");
    }

    private static @NonNull String getFirstLaunch() {
        return "firstLaunch";
    }

    private void initOpenAiSettings() {
        double totalConsumption = 0;
        String totalConsumptionSaved = getparam("Total_cons");
        if (totalConsumptionSaved != null && !totalConsumptionSaved.isEmpty()) {
            totalConsumption = Double.parseDouble(totalConsumptionSaved);
        }
        setparam("Total_cons", totalConsumption + "");
    }

    private void initListeningSettings() {
        if (getparam(listeningDurationPseudo).equals("")) {
            setparam(listeningDurationPseudo, getParamFromFile("Listening_time", configurationFilePseudo));
        }
        listeningDuration = Integer.parseInt(getparam(listeningDurationPseudo));
        if (listeningDuration < 0) {
            listeningDuration = 5;
            setparam(listeningDurationPseudo, String.valueOf(listeningDuration));
        }
        if (getparam(listeningAttemptPseudo).equals("")) {
            setparam(listeningAttemptPseudo, getParamFromFile("Number_listens", configurationFilePseudo));
        }
        listeningAttempt = Integer.parseInt(getparam(listeningAttemptPseudo));
        if (listeningAttempt < 0) {
            listeningAttempt = 2;
            setparam(listeningAttemptPseudo, String.valueOf(listeningAttempt));
        }
        remainingAttempts = listeningAttempt - 1;
        Log.i(TAG, "initListeningSettings: HOU " +getParamFromFile("Listening_time", configurationFilePseudo));
    }

    private void initProjectID() {
        if (getparam("CustomGPT_Project_ID").equals("")) {
            setparam("CustomGPT_Project_ID", getParamFromFile("CustomGPT_Project_ID", configurationFilePseudo));
        }
    }

    public void initTeamGPTSettings() throws IOException {
        //setparam("INVALID_TEAMGPT_KEY","FALSE");
        if(responseFromTeamGPT != null){
            responseFromTeamGPT.reset();
        }
        responseFromTeamGPT=new ResponseFromTeamGPT(this);

        setparam("TeamGPT_url", getParamFromFile("TeamGPT_url", configurationFilePseudo));
        setparam("TeamGPT_ApiEndpoint_Params", getParamFromFile("TeamGPT_ApiEndpoint_Params", configurationFilePseudo));
        setparam("TeamGPT_ApiEndpoint_Response", getParamFromFile("TeamGPT_ApiEndpoint_Response", configurationFilePseudo));

        if (getparam("TeamGPT_Key").equalsIgnoreCase("")) {

            setparam("TeamGPT_Key", getParamFromFile("TeamGPT_Key", configurationFilePseudo));
            Log.i("TAG", "run: getParameters 4");
            if(!getparam("TeamGPT_Key").equalsIgnoreCase(""))
                responseFromTeamGPT.getParameters();
            else{
                if (getLangue().getNom().equals("Anglais")){
                    showToast(getString(R.string.toast_teamgpt_key_indispo_en));
                }
                else if (getLangue().getNom().equals("Français")) {
                    showToast(getString(R.string.toast_teamgpt_key_indispo_fr));
                }
                else{
                    getEnglishLanguageSelectedTranslator()
                            .translate(getString(R.string.toast_teamgpt_key_indispo_en))
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    showToast(translatedText);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    showToast(getString(R.string.toast_teamgpt_key_indispo_en));
                                }
                            });
                }
            }
        }
        else
            responseFromTeamGPT.getParameters();




        if (getparam("firstLaunch").equals("true")) {
            //setparam("messages", "[]");

            if (getparam(header).equals("")) {
                setparam(header, getParamFromFile(header, configurationFilePseudo));
            }
            if (getparam(entete).equals("")) {
                setparam(entete, getParamFromFile(entete, configurationFilePseudo));
            }
            if (getparam(cabecera).equals("")) {
                setparam(cabecera, getParamFromFile(cabecera, configurationFilePseudo));
            }
            if (getparam(kopfzeile).equals("")) {
                setparam(kopfzeile, getParamFromFile(kopfzeile, configurationFilePseudo));
            }




            setparam("firstLaunch", "false");
        }

        // TeamGPT



    }

    private void initChatTextSize() {
        int textSize = Integer.parseInt(getParamFromFile("Chat_TextSize", configurationFilePseudo));
        if (textSize < 20 || textSize > 50) {
            setTextSizeBullesPX(25);
        } else setTextSizeBullesPX(textSize);


    }

    private void initSpeakVolumeSetting() {
        max = getMaxVolume();
        if (getparam(speakVolumePseudo).equals("")) {
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
        if (getparam(visibilityString).equals("")) {
            if (getParamFromFile("Display_of_speech", configurationFilePseudo).trim().equalsIgnoreCase("No")) {
                setparam(visibilityString, "false");
            } else {
                setparam(visibilityString, "true");
            }
        }
        switchVisibility = getparam(visibilityString);
    }

    private void initBIDisplay() {
        if (getparam("Stimulis").equals("")) {
            if (getParamFromFile("Stimulis", configurationFilePseudo).trim().equalsIgnoreCase("No")) {
                setparam("Stimulis", "false");
            } else {
                setparam("Stimulis", "true");
            }
        }
        switchBIDisplay = getparam("Stimulis");
    }

    private void initSTTSetting() {
//        String can_change_stt = getParamFromFile("Change_STT", "BuddyGPT.properties");
//        if (getparam("STT_chosen").equals("") || (can_change_stt == null || can_change_stt.trim().equalsIgnoreCase("No"))) {
//            Log.e("MRAA", "initSTTSetting inside if");
////            if (getParamFromFile("Speech_To_Text", "BuddyGPT.properties").trim().equalsIgnoreCase("SpeechRecognizer")) {
////                setparam("STT_chosen", "Android");
////            }
////            else if (getParamFromFile("Speech_To_Text", "BuddyGPT.properties").trim().equalsIgnoreCase("ApiGoogle")) {
////                setparam("STT_chosen", "Google");
////            } else if (getParamFromFile("Speech_To_Text", "BuddyGPT.properties").trim().equalsIgnoreCase("Whisper")) {
////                setparam("STT_chosen", "Whisper");
////            }
//            else if (getParamFromFile("Speech_To_Text", "BuddyGPT.properties").trim().equalsIgnoreCase("Cerence")) {
//                setparam("STT_chosen", "Cerence");
//            } else {
//
//                String[] listSTT = getParamFromFile("Speech_To_Text_List", "BuddyGPT.properties").split("/");
//                if (listSTT.length > 0) {
//                    if (listSTT[0].trim().equalsIgnoreCase("SpeechRecognizer")) {
//                        setparam("STT_chosen", "Android");
//                    } else if (listSTT[0].trim().equalsIgnoreCase("ApiGoogle")) {
//                        setparam("STT_chosen", "Google");
//                    } else if (listSTT[0].trim().equalsIgnoreCase("Whisper")) {
//                        setparam("STT_chosen", "Whisper");
//                    } else if (listSTT[0].trim().equalsIgnoreCase("Cerence")) {
//                        setparam("STT_chosen", "Cerence");
//                    } else setparam("STT_chosen", "Android");
//                } else setparam("STT_chosen", "Android");
//
//
//                if (getLangue().getNom().equals("Anglais")) {
//                    showToast("Chosen STT is not found. Buddy will use " + getparam("STT_chosen"));
//                }
//                else if (getLangue().getNom().equals("Français")) {
//                    showToast("Le STT choisi est introuvable. Buddy utilisera " + getparam("STT_chosen"));
//                }
////                else if (getLangue().getNom().equals("Espagnol")) {
////                    showToast("No se encuentra el STT elegido. Buddy usará " + getparam("STT_chosen"));
////                } else if (getLangue().getNom().equals("Allemand")) {
////                    showToast("Ausgewählte STT wurde nicht gefunden. Buddy wird " + getparam("STT_chosen") + " verwenden");
////                }
//                else {
//                    showToast("Chosen STT is not found. Buddy will use " + getparam("STT_chosen"));
//                }
//
//            }
//
//
//        }
    }

    private void initTTSSetting() {
        Log.e("MRAA", "initTTSSetting");
        String[] listTTS = {"ReadSpeaker", "Android"};
        if (listTTS.length > 0) {
            if (listTTS[0].trim().equalsIgnoreCase("ReadSpeaker")) {
                chosenTTS = "ReadSpeaker";
            } else if (listTTS[0].trim().equalsIgnoreCase("Android")) {
                chosenTTS = "Android";
            }
//            else if (listTTS[0].trim().equalsIgnoreCase("ApiGoogle")) {
//                chosenTTS = "ApiGoogle";
//            }
            else {
                chosenTTS = "ReadSpeaker";

                if (getLangue().getNom().equals("Anglais")) {
                    showToast("Chosen TTS is not found. Buddy will use " + chosenTTS);
                } else if (getLangue().getNom().equals("Français")) {
                    showToast("Le TTS choisi est introuvable. Buddy utilisera " + chosenTTS);
                } else if (getLangue().getNom().equals("Espagnol")) {
                    showToast("No se encuentra el TTS elegido. Buddy usará " + chosenTTS);
                } else if (getLangue().getNom().equals("Allemand")) {
                    showToast("Ausgewählte TTS wurde nicht gefunden. Buddy wird " + chosenTTS + " verwenden");
                } else {
                    showToast("Chosen TTS is not found. Buddy will use " + chosenTTS);
                }

            }
        } else {
            chosenTTS = "ReadSpeaker";
            if (getLangue().getNom().equals("Anglais")) {
                showToast("Chosen TTS is not found. Buddy will use " + chosenTTS);
            } else if (getLangue().getNom().equals("Français")) {
                showToast("Le TTS choisi est introuvable. Buddy utilisera " + chosenTTS);
            } else if (getLangue().getNom().equals("Espagnol")) {
                showToast("No se encuentra el TTS elegido. Buddy usará " + chosenTTS);
            } else if (getLangue().getNom().equals("Allemand")) {
                showToast("Ausgewählte TTS wurde nicht gefunden. Buddy wird " + chosenTTS + " verwenden");
            } else {
                showToast("Chosen TTS is not found. Buddy will use " + chosenTTS);
            }
        }
    }

    private void initLanguageSetting() {

        List<String> langueDisponible = getDisponibleLangue();
        List<Langue> langues = new ArrayList<>();
        for (int i = 1; i < langueDisponible.size(); i++) {

            if (getparam(langueDisponible.get(i - 1)).equals("")) {
                String languageCode;
                if (langueDisponible.get(i).contains("-")) {
                    languageCode = langueDisponible.get(i);
                } else {
                    if (langueDisponible.get(i).equalsIgnoreCase("fr")) {
                        languageCode = "fr-FR";
                    } else if (langueDisponible.get(i).equalsIgnoreCase("en")) {
                        languageCode = "en-US";
                    } else {
                        languageCode = getFirstFullLanguageCode(langueDisponible.get(i));
                    }
                }
                if (languageCode == null) {
                    languageCode = getFullLanguageCodeFromCountryCode(langueDisponible.get(i));
                }
                Boolean isChosen;
                if (getParamFromFile(langueInconfigurationFilePseudo, configurationFilePseudo).trim().equalsIgnoreCase(languageCode.split("-")[0])) {
                    isChosen = true;
                } else {
                    isChosen = false;
                }
                Langue langue_utili = new Langue(i, langueDisponible.get(i - 1), isChosen, languageCode);
                Gson json_langue_utili = new Gson();
                String jsonString_langue_utili = json_langue_utili.toJson(langue_utili);
                setparam(langueDisponible.get(i - 1), jsonString_langue_utili);
            } else {
                Langue langueTemp = new Gson().fromJson(getparam(langueDisponible.get(i - 1)), Langue.class);
                langueTemp.setId(i);
                String languageCode;
                if (langueDisponible.get(i).contains("-")) {
                    languageCode = langueDisponible.get(i);
                } else {
                    if (langueDisponible.get(i).equalsIgnoreCase("fr")) {
                        languageCode = "fr-FR";
                    } else if (langueDisponible.get(i).equalsIgnoreCase("en")) {
                        languageCode = "en-US";
                    } else {
                        languageCode = getFirstFullLanguageCode(langueDisponible.get(i));
                    }
                }
                Log.e("MMMM", "languageCode " + languageCode);
                if (languageCode == null) {
                    languageCode = getFullLanguageCodeFromCountryCode(langueDisponible.get(i));
                }
                langueTemp.setLanguageCode(languageCode);
                setparam(langueDisponible.get(i - 1), new Gson().toJson(langueTemp));
            }
            langues.add(new Gson().fromJson(getparam(langueDisponible.get(i - 1)), Langue.class));
//            }
            i++;
        }
        if (langues.isEmpty()) {
            Langue langue_francais = new Langue(1, "Français", true);
            langue_francais.setLanguageCode("fr-FR");
            Gson json_langue_francais = new Gson();
            String jsonString_langue_francais = json_langue_francais.toJson(langue_francais);
            setparam(french, jsonString_langue_francais);
            langues.add(new Gson().fromJson(getparam(french), Langue.class));
        }
        int iterationCount = 0;
        for (Langue language : langues) {
            iterationCount++;
            if (language.isChosen()) {
                this.langue = language;
                setLangue(language);
                break;
            }
            if (iterationCount == langueDisponible.size() / 2) {
                this.langue = language;
                setLangue(language);
            }
        }
    }

    private String getFullLanguageCodeFromCountryCode(String shortLanguageCode) {
        Locale[] locales = Locale.getAvailableLocales();

        for (Locale locale : locales) {
            if (shortLanguageCode.equalsIgnoreCase(locale.getCountry())) {
                Log.e("MMMM", "getFirstFullLanguageCode " + locale.getLanguage() + "-" + locale.getCountry());
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

        options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(langue) // Remplacez par la langue choisie par l'utilisateur
                .build();
        englishLanguageSelectedTranslator = Translation.getClient(options);
        DownloadConditions conditions = new DownloadConditions.Builder()
                .build();
        englishLanguageSelectedTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        setEnglishLanguageSelectedTranslator(englishLanguageSelectedTranslator);
                        imlKitDownloadCallback.onDownloadEnd(true, "english");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        imlKitDownloadCallback.onDownloadEnd(false, "english");
                    }
                });
        options1 = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.FRENCH)
                .setTargetLanguage(langue) // Remplacez par la langue choisie par l'utilisateur
                .build();
        frenchLanguageSelectedTranslator = Translation.getClient(options1);
        DownloadConditions conditions1 = new DownloadConditions.Builder()
                .build();
        frenchLanguageSelectedTranslator.downloadModelIfNeeded(conditions1)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        setFrenchLanguageSelectedTranslator(frenchLanguageSelectedTranslator);
                        imlKitDownloadCallback.onDownloadEnd(true, "french");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        imlKitDownloadCallback.onDownloadEnd(false, "french");
                    }
                });
        options = new TranslatorOptions.Builder()
                .setSourceLanguage(langue)
                .setTargetLanguage(TranslateLanguage.ENGLISH) // Remplacez par la langue choisie par l'utilisateur
                .build();
        languageSelectedEnglishTranslator = Translation.getClient(options);
        DownloadConditions conditions2 = new DownloadConditions.Builder()
                .build();
        languageSelectedEnglishTranslator.downloadModelIfNeeded(conditions2)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        setLanguageSelectedEnglishTranslator(languageSelectedEnglishTranslator);
                        imlKitDownloadCallback.onDownloadEnd(true, "languageToEnglish");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        imlKitDownloadCallback.onDownloadEnd(false, "languageToEnglish");
                    }
                });

    }


    private void initEmotionSetting() {
        if (getparam(emotionString).equals("")) {
            if (getParamFromFile("Activation_of_emotions", configurationFilePseudo).trim().equalsIgnoreCase("No")) {
                setparam(emotionString, "false");
            } else {
                setparam(emotionString, "true");
            }
        }
        switchEmotion = getparam(emotionString);
    }

    private void initLanguageDetectionSetting() {
        if (getparam(detectionLanguageString).equals("")) {
            if (getParamFromFile("Language_detection", configurationFilePseudo).trim().equalsIgnoreCase("No")) {
                setparam(detectionLanguageString, "false");
            } else {
                setparam(detectionLanguageString, "true");
            }
        }
        switchdetectLanguage = getparam(detectionLanguageString);
    }


    public ResponseFromTeamGPT getResponseFromTeamGPT() {
        return responseFromTeamGPT;
    }

    public void setResponseFromTeamGPT(ResponseFromTeamGPT responseFromTeamGPT) {
        this.responseFromTeamGPT = responseFromTeamGPT;
    }
    private void initCommandeSetting() {
        if (getparam(commandeString).equals("")) {
            if (getParamFromFile(commandeString, configurationFilePseudo).trim().equalsIgnoreCase("No")) {
                setparam(commandeString, "false");
            } else {
                setparam(commandeString, "true");
            }
        }
        setparam("COMMAND_Model", getParamFromFile("COMMAND_Model", configurationFilePseudo));
        setparam("COMMAND_Temperature", getParamFromFile("COMMAND_Temperature", configurationFilePseudo));
        switchCommande = getparam(commandeString);
    }

    private void initTracking() {

        //Tracking activation
        if (getparam("Tracking_Activation").equals("")) {
            if (getParamFromFile("Tracking", configurationFilePseudo).trim().equalsIgnoreCase("No")) {
                setparam("Tracking_Activation", "false");
            } else {
                setparam("Tracking_Activation", "true");
            }
        }

        //Tracking camera display
        if (getparam("Tracking_Camera_Display").equals("")) {
            if (getParamFromFile("TRACKING_Camera", configurationFilePseudo).trim().equalsIgnoreCase("No")) {
                setparam("Tracking_Camera_Display", "false");
            } else {
                setparam("Tracking_Camera_Display", "true");
            }
        }

        //Tracking head
        if (getparam("Tracking_Head").equals("")) {
            if (getParamFromFile("TRACKING_Head", configurationFilePseudo).trim().equalsIgnoreCase("No")) {
                setparam("Tracking_Head", "false");
            } else {
                setparam("Tracking_Head", "true");
            }
        }

        //Tracking body
        if (getparam("Tracking_Body").equals("")) {
            if (getParamFromFile("TRACKING_Body", configurationFilePseudo).trim().equalsIgnoreCase("No")) {
                setparam("Tracking_Body", "false");
            } else {
                setparam("Tracking_Body", "true");
            }
        }

        //Tracking auto listen
        if (getparam("Tracking_Auto_Listen").equals("")) {
            if (getParamFromFile("TRACKING_listening", configurationFilePseudo).trim().equalsIgnoreCase("No")) {
                setparam("Tracking_Auto_Listen", "false");
            } else {
                setparam("Tracking_Auto_Listen", "true");
            }
        }

        //Tracking invitation
        if (getparam("Tracking_Invitation").equals("")) {
            if (getParamFromFile("TRACKING_Welcome", configurationFilePseudo).trim().equalsIgnoreCase("No")) {
                setparam("Tracking_Invitation", "false");
            } else {
                setparam("Tracking_Invitation", "true");
            }
        }

        //Tracking invitation chatGpt
//        if (getparam("Tracking_Invitation_ChatGpt").equals("")) {
//            if (getParamFromFile("TRACKING_welcome_CHATGPT",configurationFilePseudo).trim().equalsIgnoreCase("No")){
//                setparam("Tracking_Invitation_ChatGpt", "false");
//            }
//            else {
//                setparam("Tracking_Invitation_ChatGpt", "true");
//            }
//        }
    }

    public List<String> separator(String hotword) {
        StringTokenizer st = new StringTokenizer(hotword, "/", false);
        List<String> list = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String result = st.nextToken();
            list.add(result.trim());
        }
        return list;
    }
    //#region ******************************************************* STT **********************************************************************

    //#region ******************************************************* blue mic *******************************************************




    //#endregion ******************************************************* blue mic *******************************************************

    //#region ******************************************************* STT Cerence Local fcf **********************************************************************


    /**
     * Cette fonction permet de lancer l'écoute STT Cerence local .fcf OU BIEN l'écoute de OK BUDDY
     * @return : l'objet STTTask ou null si erreur
     */
    public void startListeningHotwor(Activity activity) {
        getTranslateHotwordList();
        neutralAnimation();
        isSpeaking = false;
        currentEmotion = "";
        shouldPlayEmotion = false;

        stopListening(activity);

        setAlreadyChatting(false);

        if (getCurrentLanguage().equals("en")) {
            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_en);
        } else if (getCurrentLanguage().equals("fr")) {
            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_fr);
        } else if (getCurrentLanguage().equals("de")) {
            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_de);
        } else if (getCurrentLanguage().equals("es")) {
            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_es);
        } else {
            getEnglishLanguageSelectedTranslator()
                    .translate(getString(R.string.toast_stt_android_indispo_en))
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String translatedText) {
                            toast_stt_android_indispo = translatedText;
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_en);
                        }
                    });
        }
        activity.runOnUiThread(() -> {

            //USE BLUE MIC


                try {
                    speechRecognizer.startListening(speechRecognizerIntent2);
                    if (!isAppInstalled(getApplicationContext(), "com.google.android.googlequicksearchbox")) {
                        showToast(toast_stt_android_indispo);
                    }
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
                            speechRecognizer.startListening(speechRecognizerIntent2);
                        }

                        @Override
                        public void onResults(Bundle bundle) {
                            ArrayList<String> data = bundle.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                            if (data != null && data.size() > 0) {
                                Log.e(TAG, "Hotword result  : " + data.get(0));
                                checkTheHotword(data.get(0));
                            } else {
                                Log.e(TAG, "Hotword result  size = 0 : ");
                                speechRecognizer.startListening(speechRecognizerIntent2);
                            }
                        }

                        @Override
                        public void onPartialResults(Bundle bundle) {
                            Log.e(TAG, "Hotword onPartialResults listening  : ");
                            ArrayList<String> data = bundle.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                            if (data != null && data.size() > 0) {
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


    //#region ******************************************************* STT Free Speech **********************************************************************


    /**
     * Cette fonction permet de lancer l'écoute STT Free Speech
     * @return : l'objet STTTask ou null si erreur
     */

    private void initializeSTT(STTTask sttTask) {
        try{
            sttTask.initialize();
        }
        catch (Exception e) {
            Log.e("MRRR","Runnable : Erreur pendant l'initialisation du STT Task : "+e);
        }
    }

    public STTTask startListeningCerence(Activity activity) {
        Log.e("MRRR", "startListeningFreeSpeechStt fonction start");
        alreadyGetAnswer = false;
        questionNumber++;
        currentEmotion = "";
        shouldPlayEmotion = false;
        stopListening(activity);
        try {
            if (getParamFromFile("Language_Specification_STT", configurationFilePseudo).trim().equalsIgnoreCase("No")) {
                freeSpeechSttTask = BuddySDK.Speech.createCerenceFreeSpeechTask();
            } else {
                if (getCurrentLanguage().equals("en")) {
                    Log.e("MRRR", "init ENfreeSpeechSttTask");
                    freeSpeechSttTask = BuddySDK.Speech.createCerenceFreeSpeechTask(Locale.ENGLISH);
                } else {
                    freeSpeechSttTask = BuddySDK.Speech.createCerenceFreeSpeechTask(Locale.FRENCH);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception lors de la création de cerence " + e);
        }


        if (freeSpeechSttTask == null) return null;

        initializeSTT(freeSpeechSttTask);

        Log.w("MRRR", "startListeningfreeSpeechStt : cerence");

        try {
            Log.w("MRRR", "startListeningfreeSpeechStt :try ");
            freeSpeechSttTask.start(true, new ISTTCallback.Stub() {
                @Override
                public void onSuccess(STTResultsData sttResultsData) throws RemoteException {
                    Log.i("MRA", "Succès d'écoute cerence Free Speech");

                    if (!sttResultsData.getResults().isEmpty()) {

                        STTResult result = sttResultsData.getResults().get(0);

                        Log.e("MRRR", "Listening cerence Free Speech : " +
                                "\nScore : " + result.getConfidence() + //the recognition score
                                "\nUtterance: " + result.getUtterance() +  //actual phrase pronounced by the user and recognised by free speech (google/cerence)
                                "\nRule: " + result.getRule()); //the respective tag of the Uterrance, as described in the grammar
                        notifyObservers("STTQuestion_success;" + result.getUtterance());
                        setLed("neutral");

                    }
                }

                @Override
                public void onError(String s) throws RemoteException {
                    Log.e("MRRR", "onError cerence " + s);

                }
            });
        } catch (Exception e) {
            Log.e("MRRR", "onError cerence " + e);

        }

        setLed("listening");


        return freeSpeechSttTask;

    }

    public void refresh(String langue, Activity activity) {

        speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(activity);
        if (getParamFromFile("Language_Specification_STT", configurationFilePseudo).trim().equalsIgnoreCase("Yes")) {
            if (getparam("STT").equalsIgnoreCase("Android")) {
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, Integer.parseInt(getParamFromFile("Android_Speech_minimum_length", configurationFilePseudo)) * 1000);
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, Integer.parseInt(getParamFromFile("Android_Speech_silence_length", configurationFilePseudo)) * 1000);
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langue);
            }
            if (getparam("STT").equalsIgnoreCase("Cerence")) {
                if (!langue.toLowerCase().contains("en") && !langue.toLowerCase().contains("fr")) {
                    speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, Integer.parseInt(getParamFromFile("Android_Speech_minimum_length", configurationFilePseudo)) * 1000);
                    speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, Integer.parseInt(getParamFromFile("Android_Speech_silence_length", configurationFilePseudo)) * 1000);
                    speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langue);
                }
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
        }


    }

    public void startListeningQuestion(Activity activity) {
        Log.e(TAG, "startListeningFreeSpeechStt fonction start");
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listeningAnimation();
            }
        });
        alreadyGetAnswer = false;
        questionNumber++;
        currentEmotion = "";
        shouldPlayEmotion = false;
        stopListening(activity);

        if (getCurrentLanguage().equals("en")) {
            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_en);
        } else if (getCurrentLanguage().equals("fr")) {
            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_fr);
        } else if (getCurrentLanguage().equals("de")) {
            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_de);
        } else if (getCurrentLanguage().equals("es")) {
            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_es);
        } else {
            getEnglishLanguageSelectedTranslator()
                    .translate(getString(R.string.toast_stt_android_indispo_en))
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String translatedText) {
                            toast_stt_android_indispo = translatedText;
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            toast_stt_android_indispo = getString(R.string.toast_stt_android_indispo_en);
                        }
                    });
        }
        activity.runOnUiThread(() -> {
            try {
                speechRecognizer.startListening(speechRecognizerIntent);
                if (!isAppInstalled(getApplicationContext(), "com.google.android.googlequicksearchbox")) {
                    showToast(toast_stt_android_indispo);
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
                        speechRecognizer.startListening(speechRecognizerIntent);

                    }

                    @Override
                    public void onResults(Bundle bundle) {
                        ArrayList<String> data = bundle.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                        if (data != null && data.size() > 0) {
                            Log.e(TAG, "question result onResults  : " + data.get(0));
                            notifyObservers("STTQuestion_success;" + data.get(0));
                            BuddySDK.UI.stopListenAnimation();
                            setLed("neutral");
                        } else {
                            Log.e(TAG, "question result onResults size = 0 : ");
                            speechRecognizer.startListening(speechRecognizerIntent);
                        }
                    }

                    @Override
                    public void onPartialResults(Bundle bundle) {
                        Log.i(TAG, "onPartialResults listen");
                        ArrayList<String> data = bundle.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                        if (data != null && data.size() > 0) {
                            Log.e(TAG, "question result onPartialResults  : " + data.get(0));
                            notifyObservers("STTQuestion_success;" + data.get(0));
                            BuddySDK.UI.stopListenAnimation();
                            setLed("neutral");
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
        String errorTXT = new Date().toString() + ", STTAndroidERROR,ERROR CODE= " + String.valueOf(code) + ", ERROR Body{ type= " + type + ", message= " + message + "}" + System.getProperty("line.separator");
        File file2 = new File(Environment.getExternalStorageDirectory(), "BuddyGPT/ERROR-History.txt");


        try {

            FileWriter fileWriter = new FileWriter(file2, true);
            fileWriter.write(errorTXT);
            fileWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void startWhisperRecording(Activity activity){
//        Log.e("MRA","startWhisperRecording");
//        alReadyHadSpoke=false;
//        activityTemp =activity;
//        activity.runOnUiThread( new Runnable() {
//            @Override
//            public void run() {
//                listeningAnimation();
//            }
//        });
//        alreadyGetAnswer = false;
//        questionNumber++;
//        currentEmotion="";
//        shouldPlayEmotion=false;
//        stopListening(activity);
//        if (isRecording) {
//            Log.d(TAG_STREAMING, "Already recording");
//            return;
//        }
//
//        audioRecord = new AudioRecord(
//                MediaRecorder.AudioSource.MIC,
//                SAMPLE_RATE,
//                CHANNEL_CONFIG,
//                AUDIO_FORMAT,
//                BUFFER_SIZE);
//
//        // Adjust the path and file name as needed
//        String outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.pcm";
//
//        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
//            audioRecord.startRecording();
//            isRecording = true;
//            currentState="";
//            startVAD();
//            processAudio(outputFile);
//        } else {
//            Log.e(TAG_STREAMING, "Failed to initialize AudioRecord");
//        }
    }
    public void stopWhisperSTT(Boolean shouldRestartListening,Boolean shouldRestartNewCycle) {
        try {
            byte[] audioDataF = readAudioFile(); // Read the recorded audio data
            // Annuler la tâche précédente si elle existe
            if (transcribeTask != null && transcribeTask.getStatus() == AsyncTask.Status.RUNNING) {
                transcribeTask.cancel(true);
            }
            Log.e("MIDO","start dbfs calcul");
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
            thread =new Thread(() -> {
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(activityTemp));
                }
                Python py = Python.getInstance();
                PyObject pyobj = py.getModule("calculDBFS");
                try {
                    PyObject reponse;
                    JSONObject parameters = new JSONObject();
                    parameters.put("fichier_audio", Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.wav"); // Chemin de votre fichier audio

                    // Appel de la fonction main avec le chemin du fichier audio
                    reponse = pyobj.callAttr("main", parameters.getString("fichier_audio"));

                    //Mettre  le dernier fichier json envoyé à l’API



                    Log.e("MIDO","result dBFS python "+reponse.toString());
                    if (!reponse.toString().trim().equals("-inf")) {
                        if (Float.parseFloat(reponse.toString()) >= Float.parseFloat(getParamFromFile("Seuil_dBFS", configurationFilePseudo))) {
                            Log.d("MIDO", "volume est bien : " + Float.parseFloat(reponse.toString()));
                            transcribeTask = new TranscribeTask();
                            transcribeTask.execute(audioDataF); // Transcribe the audio
                        } else {
                            Log.d("MIDO", "volume est trop bas : " + Float.parseFloat(reponse.toString()));
                            if (shouldRestartListening) {
                            startWhisperRecording(activityTemp);
                            } else {
                                Log.e("ARR","stopWhisper restartNewCycle  shouldRestartNewCycle"+shouldRestartListening);
                                if (shouldRestartNewCycle){
                                    activityTemp.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            notifyObservers("restartNewCycle");
                                        }
                                    });
                                }
                                else {
                                    activityTemp.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            notifyObservers("restartListeningHotword");
                                        }
                                    });
                                }
                            }
                        }
                    }
                    else {
                        if (shouldRestartListening) {
                            startWhisperRecording(activityTemp);
                        } else {
                            if (shouldRestartNewCycle){
                                activityTemp.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        notifyObservers("restartNewCycle");
                                    }
                                });
                            }
                            else {
                                activityTemp.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        notifyObservers("restartListeningHotword");
                                    }
                                });
                            }
                        }
                    }
                    if (Thread.currentThread().isInterrupted()) {
                        return; // Terminer le thread s'il a été interrompu
                    }


                } catch (PyException | JSONException p) {
                    Log.e(TAG, "Exception "+p);
                }

            });
            thread.start();
        } catch (Exception e) {
            Log.e("MRA", "Exception " + e);
        }

    }
    private int getAudioDuration() {
        try {

            // Use MediaPlayer to get the duration
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.wav");
            mediaPlayer.prepare();
            int duration = mediaPlayer.getDuration();

            // Release the MediaPlayer resources
            mediaPlayer.release();

            return duration;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
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

    //    private void writeAudioDataToFile() {
//        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/BuddyGPT/audioF.raw";
//        File file = new File(filePath);
//
//        try {
//            FileOutputStream os = new FileOutputStream(file);
//            byte[] buffer = new byte[BUFFER_SIZE];
//
//            while (isRecording) {
//                int read = audioRecord.read(buffer, 0, BUFFER_SIZE);
//                if (read != AudioRecord.ERROR_INVALID_OPERATION) {
//                    os.write(buffer, 0, read);
//                }
//            }
//
//            os.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        // Une fois l'enregistrement terminé, convertir le fichier audio brut en MP3
//        convertToMp3(filePath);
//    }
//
//    private void convertToMp3(String inputPath) {
//        Log.e("MMMM","start convertToMp3");
//        String outputPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/BuddyGPT/audioF.mp3";
//
//        String[] cmd = new String[]{"-b", "128", inputPath, outputPath};
//
//        try {
//            Process process = Runtime.getRuntime().exec(cmd);
//            process.waitFor();
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        // Supprimer le fichier audio brut (facultatif)
//        File rawFile = new File(inputPath);
//        if (rawFile.exists()) {
//            rawFile.delete();
//        }
//        Log.e("MMMM","end convertToMp3");
//        byte[] audioDataF;
//        try {
//            File audioFileF = new File("/storage/emulated/0/BuddyGPT/audioF.mp3");
//            audioDataF = Files.readAllBytes(audioFileF.toPath());
//
//            new TranscribeTask().execute(audioDataF);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
    public String transcribe(byte[] audioData) throws IOException {
        Log.e("MMMM","start TRanscribe");
        String language =new Gson().fromJson(getparam(getLangue().getNom()), Langue.class).getLanguageCode().split("-")[0];
        if (getParamFromFile("Language_Specification_STT",configurationFilePseudo).trim().equalsIgnoreCase("No")){
            language="";
        }
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", getParamFromFile("Whisper_model",configurationFilePseudo).trim())
                .addFormDataPart("file", "GPTAudio.mp3",
                        RequestBody.create(MediaType.parse("audio/mp3"), audioData))
                .addFormDataPart("language",language)
                .addFormDataPart("prompt",getParamFromFile("Whisper_prompt",configurationFilePseudo).trim())
                .build();

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/audio/transcriptions")
                .header("Authorization", "Bearer " + getparam("openAI_API_Key"))
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                Log.i("MRA", "Calling Whisper API is successful");

                double durationInMinutes = (double) getAudioDuration() / (60 * 1000);
                Log.i("MYA", "Calling Whisper API is successful1------"+durationInMinutes);
                calcul_consommation(getParamFromFile("Whisper_model","BuddyGPT.properties"), (double) durationInMinutes,0);

                Gson gson = new Gson();
                String responseBody = response.body().string();
                TranscriptionResult result = gson.fromJson(responseBody, TranscriptionResult.class);
                return result.text;

            } else {
                int checkErrorCode = response.code();
                // Calcul de la consommation openai de le cas d'echec
                if (checkErrorCode == 500 || checkErrorCode == 503 || checkErrorCode == 504) {
                    double durationInMinutes = (double) getAudioDuration() / (60 * 1000);
                    calcul_consommation(getParamFromFile("Whisper_model","BuddyGPT.properties"), (double) durationInMinutes,0);
                }
                Log.i("MRA", "Calling Whisper API is failed");
                throw new IOException("Unexpected response code: " + response.code());
            }
        }
        catch (Exception e){
            throw new IOException("Unexpected response code: " + e.toString());
        }
    }

    public String getImeiRobot() {
        return imeiRobot;
    }
    public void setImeiRobot(String imeiRobot) {
        this.imeiRobot=imeiRobot;
    }

    private static class TranscriptionResult {
        public String text;
    }

    private class TranscribeTask extends AsyncTask<byte[], Void, String> {
        String question = "";
        @Override
        protected String doInBackground(byte[]... audioData) {
            try {
                //duration = System.currentTimeMillis();
                Log.e("MRA","doInBackground stopProcessus---------- "+stopProcessus);
                if (!stopProcessus) {
                    Log.e("MRA","doInBackground stopProcessus if---------- "+stopProcessus);
                    question =transcribe(audioData[0]);
                    Log.e("MRA","doInBackground stopProcessus question---------- "+question);
                    if (!question.trim().contains("Thank you") && !question.equals("")) {
                        if (!stopProcessus) {
                            if (activityTemp!=null){
                                activityTemp.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e("MRA","envoie traitement de la question");
                                        notifyObservers("STTQuestion_success;"+question);
                                        BuddySDK.UI.stopListenAnimation();
                                        setLed("neutral");
                                    }
                                });
                            }

                        }

                    } else {
                        if (!endRecordingWhisperAudio) {
                            if (activityTemp!=null){
                                activityTemp.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        startWhisperRecording(activityTemp);
                                    }
                                });
                            }



                        }
                    }
                }
                return question;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String transcription) {
            if (transcription != null) {
                // endTime = System.currentTimeMillis(); // Record the end time
                Log.i("MRA", "------it took: ms");
            } else {
                // Gestion des erreurs

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
            // Votre code lorsque la parole est détectée
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
            // Votre code lorsque du bruit est détecté
            if (!currentState.equals("NOISE")) {
                currentState = "NOISE";
                if(alReadyHadSpoke){
                    alReadyHadSpoke=false;
                    stopProcessus =false;

                    stopRecording();

                }

            }
        }

    };
    Runnable periodicTask = new Runnable() {
        @Override
        public void run() {
            try {
                readAudioFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.e("MRAE","start dbfs calcul 3---------------");
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
                    parameters.put("fichier_audio", Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioF.wav"); // Chemin de votre fichier audio

                    // Appel de la fonction main avec le chemin du fichier audio
                    reponse = pyobj.callAttr("main", parameters.getString("fichier_audio"));

                    //Mettre  le dernier fichier json envoyé à l’API
                    Log.e("MRAE", "test comparaison flot--------------- " + reponse.toString());
                    Log.e("MRAE", "result dBFS python--------------- " + reponse.toString());
                    Log.e("MRAE", "previousVolume--------------- " + previousVolume);
                    Log.e("MRAE", "previousVolume after traitement--------------- " + (previousVolume - (Math.abs(previousVolume) * Float.parseFloat(getParamFromFile("Volume_reduction", configurationFilePseudo)) / 100)));
                    if (!reponse.toString().trim().equals("-inf")){
                        if (previousVolume == 0) {
                            Log.e("MRAE", "result dBFS if--------------- ");
                            previousVolume = Float.parseFloat(reponse.toString());
                        } else {
                            if (Float.parseFloat(reponse.toString()) <= (previousVolume - (Math.abs(previousVolume) * Float.parseFloat(getParamFromFile("Volume_reduction", configurationFilePseudo)) / 100))) {
                                traitementAudio(false);
                                previousVolume = Float.valueOf(0);
                                Log.e("MRAE", "result dBFS else if--------------- ");

                            } else {
                                Log.e("MRAE", "result dBFS else else--------------- ");
                                previousVolume = Float.parseFloat(reponse.toString());
                            }
                        }
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        return; // Terminer le thread s'il a été interrompu
                    }


                } catch (PyException | JSONException p) {
                    Log.e("MRAE","exception dBFS python "+p);
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
            return;
        }
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (vad != null) {
            Log.e("MRA","+++++++++++++++++++++++++++++++++vad stop");
            vad.stop();
        }
    }

    private void startVAD() {
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
    }

    private void processAudio(String outputFile) {
        new Thread(() -> {
            short[] buffer = new short[BUFFER_SIZE / 2]; // Divided by 2 because each short is 2 bytes
            try {
                FileOutputStream fos = new FileOutputStream(outputFile);
                while (isRecording) {
                    int numRead = audioRecord.read(buffer, 0, buffer.length);
                    if (numRead > 0) {
                        vad.addContinuousSpeechListener(buffer, vadListener);
                        fos.write(shortArrayToByteArray(buffer), 0, numRead * 2); // * 2 because each short is 2 bytes
                    }
                }
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Log.e(TAG,"processAudioFinally");
//                stopWhisperSTT(); // Stop recording and process the remaining audio
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
    public void traitementAudio(boolean shouldRestartNewCycle){
        currentState = "NOISE";

        alReadyHadSpoke = false;
        stopProcessus =false;
        stopRecording();
    }

    public static Locale getLocale(String language){

        Locale[] locales = Locale.getAvailableLocales();

        for (Locale locale : locales) {
            if (locale.toString().equals(language)) {
                Log.w("GoogleSTT","getLocale("+language+") result : " + locale);
                return locale;
            }
        }

        Log.e("GoogleSTT","getLocale("+language+") result : null");

        return Locale.ENGLISH;
    }


    public List<String> getHotwordList(){
        if (getLangue().getNom().equals(langueFr)){
            return separator(getParamFromFile("hotword_fr", configurationFilePseudo));
        }else if (getLangue().getNom().equals(langueEn)) {
            return separator(getParamFromFile("hotword_en", configurationFilePseudo));
        }else if (getLangue().getNom().equals(langueEs)) {
            return separator(getParamFromFile("hotword_es", configurationFilePseudo));
        }else if (getLangue().getNom().equals(langueDe)){
            return separator(getParamFromFile("hotword_de", configurationFilePseudo));
        }
        else {
            Log.e(TAG,"getHotwordList"+translatedList);

            return separator(translatedList);
        }

    }
    public void getTranslateHotwordList(){
        if (!getLangue().getNom().equals(langueFr) && !getLangue().getNom().equals(langueEn) && !getLangue().getNom().equals(langueEs) && !getLangue().getNom().equals(langueDe)){
            translatedList=getParamFromFile("hotword_en", configurationFilePseudo);
//            getEnglishLanguageSelectedTranslator().translate(getParamFromFile("hotword_en", configurationFilePseudo)).addOnSuccessListener(new OnSuccessListener<String>() {
//                @Override
//                public void onSuccess(String s) {
//                    translatedList = s.trim();
//                }
//            }).addOnFailureListener(new OnFailureListener() {
//                @Override
//                public void onFailure(@NonNull Exception e) {
//                    Log.e(TAG,"liste traduite onFailure "+e);
//                }
//            });
        }
    }
    public void checkTheHotword(String word){
        List<String> hotword =getHotwordList();
        for (int i = 0; i < hotword.size(); i++) {
            Log.i(TAG, "checkTheHotword :" + word);
            if (word.trim().equalsIgnoreCase(hotword.get(i).trim())) {
                try {
                    notifyObservers("STTHotword_success");


                } catch (Resources.NotFoundException e) {
                    Log.e(TAG, "Resources not Found " + e);
                }

            }
        }
    }



    /**
     * Cette fonction permet d'arrêter l'écoute STT Free Speech
     */
    public void stopListening(Activity activity) {



        activity.runOnUiThread(() -> {

            stopProcessus =true;

            if (handlerListeningHotword != null && runnableListeningHotword != null) {

                handlerListeningHotword.removeCallbacksAndMessages(null);
                handlerListeningHotword.removeCallbacks(runnableListeningHotword);

            }
            try{
                if (speechRecognizer != null ) {
                    speechRecognizer.stopListening();
                    speechRecognizer.stopListening();
                    speechRecognizer.destroy();
                }
            }
            catch (Exception e){}
                if(freeSpeechSttTask != null) {
                    Log.w(TAG, "stopListeningFreeSpeechStt");
                    try {
                        freeSpeechSttTask.stop();
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur pendant l'arrêt d'écoute STT Free Speech : "+e);
                    }
                }


            stopRecording();


        });

        setLed("Neutral");


    }





    private void listeningAnimation() {
        Log.i(TAG, "startVoiceRecorder");
        BuddySDK.UI.setFacialExpression(FacialExpression.LISTENING,1);
        BuddySDK.UI.startListenAnimation();
        setLed("listening");

    }

    private void neutralAnimation() {

        BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
        BuddySDK.UI.stopListenAnimation();
        setLed("neutral");

    }




    //#endregion ******************************************************* STT Free Speech **********************************************************************

    //#endregion ******************************************************* STT **********************************************************************


    /**
     * Cette fonction permet de prononcer le texte passé en argument de manière récursive, afin de gérer le problème de décalage entre la bouche et le discours pour les réponses plus longues.
     * @param texteToSpeak : message à dire par Buddy.
     * @param expression : jouer un mouvement spécial de la bouche [SPEAK_ANGRY / NO_FACE / SPEAK_HAPPY / SPEAK_NEUTRAL]
     * @param texteToSpeakSplitted : Liste de phrases courtes à dire par Buddy.
     *
     */
    public void startSpeakingSplittedText(final String texteToSpeak , LabialExpression expression,String type, String[] texteToSpeakSplitted ){

        Log.i("FCH_DEBUG", "startSpeakingSplittedText "+ Arrays.toString(texteToSpeakSplitted) + " , " + type);



        Handler handler_all = new Handler(Looper.getMainLooper());
        Runnable delayedTask = new Runnable() {
            @Override
            public void run() {
                Log.i("FCH_DEBUG", "handler_all start ");

                try {
                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                }
                catch (Exception e){
                    Log.e(TAG,"BuddySDK Exception  "+e);
                }

                if(currentIndexText < texteToSpeakSplitted.length ){

                    Log.e("FCH_DEBUG", "call startSpeaking");

                    BuddySDK.Speech.startSpeaking(
                            texteToSpeakSplitted[currentIndexText],
                            expression,
                            new ITTSCallback.Stub() {
                                @Override
                                public void onSuccess(String iText) throws RemoteException {
                                    Log.i(TAG, "Succès de prononciation : "+iText);

                                    Log.w("FCH_DEBUG", "onSuccess");

                                    currentIndexText++;

                                    if (!Stop_TTS_ReadSpeaker) {
                                        Log.w("FCH_DEBUG", "onSuccess 1 ");
                                        Handler handler = new Handler(Looper.getMainLooper());
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                Log.w("FCH_DEBUG", "onSuccess 2");
                                                startSpeakingSplittedText(texteToSpeak, expression, type, texteToSpeakSplitted);
                                            }
                                        }, 150);
                                    }


                                }
                                @Override
                                public void onError(String iError) throws RemoteException {
                                    Log.e(TAG, "Erreur pendant la prononciation : "+iError);

                                    Log.w("FCH_DEBUG", "onError");

                                    allTextPronoucedSuccess = false;


                                    currentIndexText++;

                                    if (!Stop_TTS_ReadSpeaker) {
                                        Log.w("FCH_DEBUG", "onError 1 ");
                                        Handler handler = new Handler(Looper.getMainLooper());
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                Log.w("FCH_DEBUG", "onError 2");
                                                startSpeakingSplittedText(texteToSpeak, expression, type, texteToSpeakSplitted);
                                            }
                                        }, 150);
                                    }

                                }
                                @Override
                                public void onPause() throws RemoteException {}
                                @Override
                                public void onResume() throws RemoteException {}
                            }
                    );

                }

                else{
                    Log.e("FCH_DEBUG", "END OF SPEAK : " + allTextPronoucedSuccess);

                    if(allTextPronoucedSuccess){
                        //success
                        allTextPronouced(texteToSpeak,  type);
                    }

                    else{
                        setLanguageDetected("");
                        //error
                        if (type.equals("storedResponse")){
                            questionNumber++;
                            notifyObservers("TTS_error;"+texteToSpeak);
                            storedResponse="";
                        }
                        else {
                            questionNumber++;
                            notifyObservers("TTS_error;"+texteToSpeak);
                        }
                    }
                }
            }
        };

        handler_all.postDelayed(delayedTask, 0);
    }
    /**
     * Cette fonction s'exécute lorsque le TTS prononce la réponse du ChatBot.
     */
    public  void allTextPronouced(final String texteToSpeak, String type){

        if (type.equals("timeOutExpired")){
            timeoutExpired = false;
            if (getparam("Stream_mode").equals("true") && responseFromTeamGPT != null){
                responseFromTeamGPT.resumeStreaming();
            }
            else{
                notifyObservers("playStoredResponse");
            }
        }
        else if (type.equals("storedResponse")){
            questionNumber++;
            notifyObservers("TTS_success;" + texteToSpeak);
            storedResponse="";
            setLanguageDetected("");
        }
        else {
            questionNumber++;
            setLanguageDetected("");
            if (getparam("Stream_mode").equals("true") ){
                if(getResponseFromTeamGPT()!=null)
                    getResponseFromTeamGPT().onTTSEnd();
            }
            else{
                notifyObservers("TTS_success;" + texteToSpeak);
            }
        }
    }


    /**
     * Cette fonction permet de prononcer le texte passé en argument.
     * @param texteToSpeak : message à dire par Buddy.
     * @param expression : jouer un mouvement spécial de la bouche [SPEAK_ANGRY / NO_FACE / SPEAK_HAPPY / SPEAK_NEUTRAL]
     */
    public void speakTTS(final String texteToSpeak , LabialExpression expression, String type){
        setAlreadyChatting(true);
        Log.e("MEHDI","texteToSpeak "+texteToSpeak);
        currentIndexText = 0;
        Stop_TTS_ReadSpeaker = false;
        Log.w(TAG, "speakTTS : "+texteToSpeak);

        currentIndexText = 0;
        Stop_TTS_ReadSpeaker = false;

        if (getCurrentLanguage().equals("en")) {
            toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_en);
        }
        else if (getCurrentLanguage().equals("fr")){
            toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_fr);
        }
        else if (getCurrentLanguage().equals("de")) {
            toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_de);
        }
        else if (getCurrentLanguage().equals("es")) {
            toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_es);
        }
        else{
            getEnglishLanguageSelectedTranslator()
                    .translate(getString(R.string.toast_tts_android_indispo_en))
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String translatedText) {
                            toast_tts_android_indispo = translatedText;
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_en);
                        }
                    });
        }

        try {
            setTTSAfterDetectingLanguage();
//            if (usingEmotions && !type.equals("timeOutExpired") && !messageError){
//                shouldPlayEmotion= true;
//                notifyObservers("playEmotion");
//            }
            if (((getCurrentLanguage().equals("en") || getCurrentLanguage().equals("fr")) && getparam("TTS").equalsIgnoreCase("ReadSpeaker") && usingReadSpeaker) || (getparam("TTS").equalsIgnoreCase("ReadSpeaker") && usingReadSpeaker) ){
                Log.e("TEST","using readspeaker");
                if (getCurrentLanguage().equals("en")){
                    BuddySDK.Speech.setSpeakerSpeed(Integer.parseInt(getParamFromFile("ReadSpeaker_speed_en",configurationFilePseudo)));
                    BuddySDK.Speech.setSpeakerPitch(Integer.parseInt(getParamFromFile("ReadSpeaker_pitch_en",configurationFilePseudo)));
                }
                else{
                    BuddySDK.Speech.setSpeakerSpeed(Integer.parseInt(getParamFromFile("ReadSpeaker_speed_fr",configurationFilePseudo)));
                    BuddySDK.Speech.setSpeakerPitch(Integer.parseInt(getParamFromFile("ReadSpeaker_pitch_fr",configurationFilePseudo)));
                }
                BuddySDK.Speech.setSpeakerVolume(getSpeakVolume());
                if(BuddySDK.Speech.isReadyToSpeak()) {
                    String texteToSpeak_modified = texteToSpeak;
                    if (texteToSpeak.toLowerCase().contains("content")) {
                        texteToSpeak_modified = texteToSpeak.replaceAll("\\bcontent\\b", "contents");
                    }
                    if (!type.equals("timeOutExpired")) {
                        // Split the text based on periods and commas
                        texteToSpeakSplitted = texteToSpeak_modified.split("[.,]");
                        Log.e("texteToSpeakSplitted", texteToSpeakSplitted.toString());

                        Log.d("FCH_DEBUG", "calling startSpeakingSplittedText : " + texteToSpeak);
                        startSpeakingSplittedText(texteToSpeak, expression, type, texteToSpeakSplitted);
                    } else {
                        BuddySDK.Speech.startSpeaking(
                                texteToSpeak_modified,
                                expression,
                                new ITTSCallback.Stub() {
                                    @Override
                                    public void onSuccess(String iText) throws RemoteException {
                                        Log.i(TAG, "Succès de prononciation : " + iText);
                                        allTextPronouced(texteToSpeak,  type);
                                    }

                                    @Override
                                    public void onError(String iError) throws RemoteException {
                                        Log.e(TAG, "Erreur pendant la prononciation : " + iError);
                                        if (type.equals("timeOutExpired")) {
                                            timeoutExpired = false;

                                            notifyObservers("playStoredResponse");

                                        }


                                    }

                                    @Override
                                    public void onPause() throws RemoteException {
                                    }

                                    @Override
                                    public void onResume() throws RemoteException {
                                    }
                                }
                        );
                    }
                }
            }
            else if (getparam("TTS").equalsIgnoreCase("Android") || (getparam("TTS").equalsIgnoreCase("ReadSpeaker") && getSecondTTSfromTTSList().equalsIgnoreCase("Android"))){
                Log.e("TEST","using tts android");
                int result = tts_android.speak(texteToSpeak, TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
                if(!isAppInstalled(getApplicationContext(),"com.google.android.tts")) {
                    showToast(toast_tts_android_indispo);
                }
                if(result == -1){
                    notifyObservers("TTS_error;"+texteToSpeak);
                }else{
                    tts_android.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            try {
                                BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                            }
                            catch (Exception e){
                                Log.e(TAG,"BuddySDK Exception  "+e);
                            }
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            try {
                                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                            }
                            catch (Exception e){
                                Log.e(TAG,"BuddySDK Exception  "+e);
                            }

                            if (type.equals("timeOutExpired")){
                                timeoutExpired = false;


                                    notifyObservers("playStoredResponse");


                            }
                            else if (type.equals("storedResponse")){
                                questionNumber++;
                                notifyObservers("TTS_success;" + texteToSpeak);
                                storedResponse="";
                                setLanguageDetected("");
                            }
                            else {
                                questionNumber++;
                                setLanguageDetected("");
                                if (getparam("Stream_mode").equals("true")){
                                    if(getResponseFromTeamGPT()!=null)
                                        getResponseFromTeamGPT().onTTSEnd();
                                }
                                else{
                                    notifyObservers("TTS_success;" + texteToSpeak);
                                }
                            }


                        }

                        @Override
                        public void onError(String utteranceId) {
                            Log.e(TAG, "Erreur pendant la prononciation "+utteranceId);
                            if (type.equals("timeOutExpired")){
                                timeoutExpired = false;


                                    notifyObservers("playStoredResponse");


                            }
                            else if (type.equals("storedResponse")){
                                try {
                                    BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                                }
                                catch (Exception e){
                                    Log.e(TAG,"BuddySDK Exception  "+e);
                                }
                                questionNumber++;
                                notifyObservers("TTS_error;"+texteToSpeak);
                                storedResponse="";
                                setLanguageDetected("");

                            }
                            else {
                                try {
                                    BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                                }
                                catch (Exception e){
                                    Log.e(TAG,"BuddySDK Exception  "+e);
                                }
                                questionNumber++;
                                notifyObservers("TTS_error;"+texteToSpeak);
                                setLanguageDetected("");

                            }
                        }
                    });
                }
            }
            else if ((getparam("TTS").equalsIgnoreCase("ReadSpeaker") && getSecondTTSfromTTSList().equalsIgnoreCase("Android"))){
                String languageToUseInApiGoogle ="";
                if (!getLanguageDetected().equals("")){
                    languageToUseInApiGoogle=getLanguageDetected();
                }
                else{
                    languageToUseInApiGoogle=getCurrentLanguage();
                }
                try {
                    switch(languageToUseInApiGoogle){
                        case "en":

                                if (getLangue().getLanguageCode().split("-")[0].equals("en")){
                                    usingReadSpeaker = false;
                                    speakGoogleCloudTTS((getLangue().getLanguageCode().split("-")[0]+"-"+getLangue().getLanguageCode().split("-")[1]),texteToSpeak,type);
                                }
                                else {
                                    usingReadSpeaker = false;
                                    speakGoogleCloudTTS("en-US",texteToSpeak,type);
                                }

                            break;
                        case "fr":

                                if (getLangue().getLanguageCode().split("-")[0].equals("fr")){
                                    usingReadSpeaker = false;
                                    speakGoogleCloudTTS((getLangue().getLanguageCode().split("-")[0]+"-"+getLangue().getLanguageCode().split("-")[1]),texteToSpeak,type);
                                }
                                else {
                                    usingReadSpeaker = false;
                                    speakGoogleCloudTTS("fr-FR",texteToSpeak,type);
                                }


                            break;
                        case "es":
                            usingReadSpeaker = false;
                            if (getLangue().getLanguageCode().split("-")[0].equals("es")) {

                                speakGoogleCloudTTS((getLangue().getLanguageCode().split("-")[0]+"-"+getLangue().getLanguageCode().split("-")[1]),texteToSpeak,type);
                            }
                            else{
                                speakGoogleCloudTTS((languageToUseInApiGoogle.toLowerCase()+"-"+languageToUseInApiGoogle.toUpperCase()),texteToSpeak,type);
                            }
                            break;
                        case "de":
                            usingReadSpeaker = false;
                            if (getLangue().getLanguageCode().split("-")[0].equals("de")) {

                                speakGoogleCloudTTS((getLangue().getLanguageCode().split("-")[0]+"-"+getLangue().getLanguageCode().split("-")[1]),texteToSpeak,type);
                            }
                            else{
                                speakGoogleCloudTTS((languageToUseInApiGoogle.toLowerCase()+"-"+languageToUseInApiGoogle.toUpperCase()),texteToSpeak,type);
                            }
                            break;
                        default:
                            usingReadSpeaker = false;
                            Log.e("TEST","default language "+languageToUseInApiGoogle);
                            Log.e("TEST","default getCurrentLanguage().split(\"-\")[0].trim() "+getCurrentLanguage().split("-").length);
                            if (!getCurrentLanguage().equals("") && getCurrentLanguage().split("-")[0].trim().equalsIgnoreCase(languageToUseInApiGoogle)){
                                speakGoogleCloudTTS((getCurrentLanguage().split("-")[0].trim()+"-"+getCurrentLanguage().split("-")[1].trim()),texteToSpeak,type);
                            }
                            else {
                                Log.e("TEST","set Langue TTS "+languageToUseInApiGoogle.toLowerCase()+","+languageToUseInApiGoogle.toUpperCase());
                                speakGoogleCloudTTS((languageToUseInApiGoogle.toLowerCase()+"-"+languageToUseInApiGoogle.toUpperCase()),texteToSpeak,type);
                            }
                            break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erreur pendant l'initialisation de la langue TTS : "+e);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception pendant la prononciation : "+e);
            notifyObservers("TTS_exception;"+texteToSpeak);
        }

    }

    /**
     * Cette fonction permet d'arrêter la prononciation
     */
    public void stopTTS() {
        Stop_TTS_ReadSpeaker = true;
        Log.w(TAG, "stopTTS");
        Stop_TTS_ReadSpeaker = true;
        try {
            if (BuddySDK.Speech.isSpeaking()) {
                BuddySDK.Speech.stopSpeaking();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur pendant l'arrêt de la prononciation TTS : "+e);
        }
        if (tts_android!= null){
            tts_android.stop();
        }
        if (googleCloudTTS != null ){
            googleCloudTTS.stop();
        }
        setLanguageDetected("");
    }

    /**
     * Cette méthode permet d'inialiser le TTS selon la langue du robot
     */
    public void setTTSAfterDetectingLanguage(){
        if (!getLanguageDetected().equals("")){
            setTTSLanguage(getLanguageDetected());
        }
        else{
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
                    if (getparam("TTS").equalsIgnoreCase("ReadSpeaker")){
                        if (getLangue().getLanguageCode().equals("en-US")){
                            BuddySDK.Speech.setSpeakerVoice("kate");
                            Log.e("MRAA","english kate");
                            usingReadSpeaker = true;
                        }else {

                            if (getLangue().getLanguageCode().split("-")[0].equals("en")){
                                usingReadSpeaker = false;
                                if (getparam("TTS").equalsIgnoreCase("Android") || (getparam("TTS").equalsIgnoreCase("ReadSpeaker") && getSecondTTSfromTTSList().equalsIgnoreCase("Android"))){
                                    tts_android.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                                    tts_android.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                                    tts_android.setLanguage(new Locale(getLangue().getLanguageCode().split("-")[0],getLangue().getLanguageCode().split("-")[1]));
                                }else if (getparam("TTS").equalsIgnoreCase("ApiGoogle") || (getparam("TTS").equalsIgnoreCase("ReadSpeaker") && getSecondTTSfromTTSList().equalsIgnoreCase("ApiGoogle"))){

                                }
                                //0.5,2.0

                            }
                            else {
                                BuddySDK.Speech.setSpeakerVoice("kate");
                                Log.e("MRAA","english kate");
                                usingReadSpeaker = true;
                            }
                        }

                    }
                    else {
                        if (getLangue().getLanguageCode().split("-")[0].equals("en")){
                            usingReadSpeaker = false;
                            tts_android.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                            tts_android.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                            tts_android.setLanguage(new Locale(getLangue().getLanguageCode().split("-")[0],getLangue().getLanguageCode().split("-")[1]));
                        }
                        else {
                            usingReadSpeaker = false;
                            tts_android.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                            tts_android.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                            tts_android.setLanguage(new Locale("en","US"));
                        }
                    }
                    break;
                case "fr":
                    if (getparam("TTS").equalsIgnoreCase("ReadSpeaker")){
                        Log.e("MEHDI","usingReadSpeaker y");
                        if (getLangue().getLanguageCode().equals("fr-FR")){
                            Log.e("MRAA","frensh roxane");
                            BuddySDK.Speech.setSpeakerVoice("roxane");
                            Log.e("MEHDI","usingReadSpeaker 1");
                            usingReadSpeaker = true;
                        }else {
                            if (getLangue().getLanguageCode().split("-")[0].equals("fr")){
                                Log.e("MEHDI","usingReadSpeaker 2");
                                usingReadSpeaker = false;
                                tts_android.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                                tts_android.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                                tts_android.setLanguage(new Locale(getLangue().getLanguageCode().split("-")[0],getLangue().getLanguageCode().split("-")[1]));
                            }
                            else {
                                Log.e("MRAA","frensh roxane");
                                BuddySDK.Speech.setSpeakerVoice("roxane");
                                Log.e("MEHDI","usingReadSpeaker 3");
                                usingReadSpeaker = true;
                            }
                        }
                    }else {
                        if (getLangue().getLanguageCode().split("-")[0].equals("fr")){
                            usingReadSpeaker = false;
                            tts_android.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                            tts_android.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                            tts_android.setLanguage(new Locale(getLangue().getLanguageCode().split("-")[0],getLangue().getLanguageCode().split("-")[1]));
                        }
                        else {
                            usingReadSpeaker = false;
                            tts_android.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                            tts_android.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                            tts_android.setLanguage(new Locale("fr","FR"));
                        }
                    }

                    break;
                case "es":
                    usingReadSpeaker = false;
                    if (getLangue().getLanguageCode().split("-")[0].equals("es")) {

                        tts_android.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch", configurationFilePseudo))));
                        tts_android.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed", configurationFilePseudo))));
                        tts_android.setLanguage(new Locale(getLangue().getLanguageCode().split("-")[0], getLangue().getLanguageCode().split("-")[1]));
                    }
                    else{
                        int result = tts_android.setLanguage(new Locale(language.toLowerCase(),language.toUpperCase()));
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e("TEST", "langue non pas prise ne charge");
                            String code = getFirstFullLanguageCode(language.toLowerCase());
                            Log.e("TEST", "langue qui doit etre "+code);
                            tts_android.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                            tts_android.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                            tts_android.setLanguage(new Locale(code.split("-")[0].trim(),code.split("-")[1].trim()));

                        }
                    }
                    break;
                case "de":
                    usingReadSpeaker = false;
                    if (getLangue().getLanguageCode().split("-")[0].equals("de")) {

                        tts_android.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch", configurationFilePseudo))));
                        tts_android.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed", configurationFilePseudo))));
                        tts_android.setLanguage(new Locale(getLangue().getLanguageCode().split("-")[0], getLangue().getLanguageCode().split("-")[1]));
                    }
                    else{
                        int result = tts_android.setLanguage(new Locale(language.toLowerCase(),language.toUpperCase()));
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e("TEST", "langue non pas prise ne charge");
                            String code = getFirstFullLanguageCode(language.toLowerCase());
                            Log.e("TEST", "langue qui doit etre "+code);
                            tts_android.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                            tts_android.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                            tts_android.setLanguage(new Locale(code.split("-")[0].trim(),code.split("-")[1].trim()));

                        }
                    }
                    break;
                default:
                    usingReadSpeaker = false;
                    Log.e("TEST","default language "+language);
                    Log.e("TEST","default getCurrentLanguage().split(\"-\")[0].trim() "+getCurrentLanguage().split("-").length);
                    if (!getCurrentLanguage().equals("") && getCurrentLanguage().split("-")[0].trim().equalsIgnoreCase(language)){
                        tts_android.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                        tts_android.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                        tts_android.setLanguage(new Locale(getCurrentLanguage().split("-")[0].trim(),getCurrentLanguage().split("-")[1].trim()));
                    }
                    else {
                        Log.e("TEST","set Langue TTS "+language.toLowerCase()+","+language.toUpperCase());
                        int result = tts_android.setLanguage(new Locale(language.toLowerCase(),language.toUpperCase()));
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e("TEST", "langue non pas prise ne charge");
                            String code = getFirstFullLanguageCode(language.toLowerCase());
                            Log.e("TEST", "langue qui doit etre "+code);
                            tts_android.setPitch(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_pitch",configurationFilePseudo))));
                            tts_android.setSpeechRate(getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_Android_speed",configurationFilePseudo))));
                            tts_android.setLanguage(new Locale(code.split("-")[0].trim(),code.split("-")[1].trim()));

                        }
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur pendant l'initialisation de la langue TTS : "+e);
        }
    }
    public void speakGoogleCloudTTS(String languageCode,String texteToSpeak,String type){
        Log.e("MRA","speakGoogleCloudTTS  LanguageCode  "+languageCode);
        String voice ="";
        if (languageCode.split("-")[0].equals("ar")){
            languageCode="ar-XA";
        }
        else if (languageCode.split("-")[0].equals("zh")){
            languageCode= "cmn-CN";
        }
        else if (languageCode.split("-")[0].equals("he")){
            languageCode= "he-IL";
        }
        else if (languageCode.split("-")[0].equals("id")){
            languageCode= "id-ID";
        }
        Boolean languageCodeExist = false;
        if (getVoiceList()!=null) {
            for (String code : getVoiceList().getLanguageCodes()) {
                if (code.equals(languageCode)) {
                    languageCodeExist = true;
                    for (String voiceName : getVoiceList().getVoiceNames(languageCode)) {
                        if (voiceName.equals(languageCode + "-" + getParamFromFile("TTS_ApiGoogle_Voice_Type", configurationFilePseudo).trim() + "-A")) {
                            voice = languageCode + "-" + getParamFromFile("TTS_ApiGoogle_Voice_Type", configurationFilePseudo).trim() + "-A";
                            break;
                        }
                        voice = voiceName;
                    }
                    break;
                }

            }
            if (!languageCodeExist){
                for (String code : getVoiceList().getLanguageCodes()) {
                    if (code.split("-")[0].equals(languageCode.split("-")[0])){
                        languageCodeExist=true;
                        languageCode=code;
                        for (String voiceName : getVoiceList().getVoiceNames(languageCode)) {
                            if (voiceName.equals(languageCode + "-" + getParamFromFile("TTS_ApiGoogle_Voice_Type", configurationFilePseudo).trim() + "-A")) {
                                voice = languageCode + "-" + getParamFromFile("TTS_ApiGoogle_Voice_Type", configurationFilePseudo).trim() + "-A";
                                break;
                            }
                            voice = voiceName;
                        }
                        break;
                    }
                }
            }
            if (!languageCodeExist){
                languageCode = "en-US";
                voice = "en-US-Standard-C";
            }
        }

        String finalLanguageCode = languageCode;
        String finalVoice =voice;
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {

                    getGoogleCloudTTS().setVoiceSelectionParams(new VoiceSelectionParams(finalLanguageCode, finalVoice))
                            .setAudioConfig(new AudioConfig(AudioEncoding.MP3, getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_ApiGoogle_speed",configurationFilePseudo))) , getConvertedPitchAndSpeedValue(Integer.parseInt(getParamFromFile("TTS_ApiGoogle_pitch",configurationFilePseudo)))));


                    getGoogleCloudTTS().setTtsListener(new TtsGoogleApiListener() {
                        @Override
                        public void onStart() {
                            try {
                                BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                            }
                            catch (Exception e){
                                Log.e(TAG,"BuddySDK Exception  "+e);
                            }
                        }

                        @Override
                        public void onDone() {
                            getGoogleCloudTTS().close();
                            try {
                                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                            }
                            catch (Exception e){
                                Log.e(TAG,"BuddySDK Exception  "+e);
                            }

                            if (type.equals("timeOutExpired")){
                                timeoutExpired = false;

                                    notifyObservers("playStoredResponse");


                            }
                            else if (type.equals("storedResponse")){
                                questionNumber++;
                                notifyObservers("TTS_success;" + texteToSpeak);
                                storedResponse="";
                                setLanguageDetected("");
                            }
                            else {
                                questionNumber++;
                                setLanguageDetected("");
                                if (getparam("Stream_mode").equals("true")){
                                    if(getResponseFromTeamGPT()!=null)
                                        getResponseFromTeamGPT().onTTSEnd();
                                }else
                                    notifyObservers("TTS_success;" + texteToSpeak);

                            }
                        }

                        @Override
                        public void onError() {
                            Log.e("MRA","speakGoogleCloudTTS  onError-----------  ");
                        }
                    });
                    getGoogleCloudTTS().start(texteToSpeak);

                } catch (Exception e) {
                    Log.e("MRA","speakGoogleCloudTTS  Exception-----------  "+e);
                    Log.e(TAG,"Exception "+e);
                    try {
                        int startIndex = e.getMessage().indexOf('{');
                        // Trouver la fin de la réponse JSON
                        int endIndex = e.getMessage().lastIndexOf('}') + 1;
                        // Extraire la réponse JSON
                        String jsonContent = e.getMessage().substring(startIndex, endIndex);
                        JsonObject errorLOG = JsonParser.parseString(jsonContent).getAsJsonObject();

                        //Mettre   le fichier le plus récent reçu
                        String fileName = "ERROR-LOG";
                        File file1 = new File(Environment.getExternalStorageDirectory(), "BuddyGPT/" + fileName + ".json");
                        if (file1.exists() && file1.isFile()) {
                            file1.delete();
                        }
                        FileWriter fileWriter = new FileWriter(file1);
                        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                        String jsonStringF=gson.toJson(errorLOG);
                        fileWriter.write(jsonStringF);
                        fileWriter.close();
                        String errorTXT= new Date().toString()+", GoogleCloudTTSERROR,ERROR CODE= "+errorLOG.getAsJsonObject("error").get("code")+", ERROR Body{ message= "+errorLOG.getAsJsonObject("error").get("message")+", status= "+errorLOG.getAsJsonObject("error").get("status")+"}"+System.getProperty("line.separator");
                        File file2 = new File(Environment.getExternalStorageDirectory(), "BuddyGPT/ERROR-History.txt");
                        FileWriter fileWriter2 = new FileWriter(file2,true);
                        fileWriter2.write(errorTXT);
                        fileWriter2.close();

                    } catch (IOException ej) {
                        e.printStackTrace();
                    }
                    getGoogleCloudTTS().close();
                    if (type.equals("timeOutExpired")){
                        timeoutExpired = false;

                            notifyObservers("playStoredResponse");


                    }
                    else if (type.equals("storedResponse")){
                        try {
                            BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                        }
                        catch (Exception ej){
                            Log.e(TAG,"BuddySDK Exception  "+ej);
                        }
                        questionNumber++;
                        notifyObservers("TTS_error;"+texteToSpeak);
                        storedResponse="";
                        setLanguageDetected("");

                    }
                    else {
                        try {
                            BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                        }
                        catch (Exception ej){
                            Log.e(TAG,"BuddySDK Exception  "+ej);
                        }
                        questionNumber++;
                        notifyObservers("TTS_error;"+texteToSpeak);
                        setLanguageDetected("");

                    }

                }
                return null;
            }
        }.execute();
    }
    public String getSecondTTSfromTTSList(){
        String[] listTTS= getParamFromFile("Text_To_Speech_List",configurationFilePseudo).split("/");
        if (listTTS.length>1){
            if (listTTS[1].trim().equalsIgnoreCase("Android")){
                return listTTS[1].trim();
            }
            else return "Android";
        }
        else return "Android";
    }
    private float getConvertedPitchAndSpeedValue(int nombre){
        int valeurMinEntree = 50;
        int valeurMaxEntree = 150;
        float valeurMinSortie = 0.5f;
        float valeurMaxSortie = 2.0f;
        Log.e("TEST","converted value :nombre= "+nombre);
        // Vérification si le nombre se trouve dans l'intervalle d'entrée
        if (nombre < valeurMinEntree || nombre > valeurMaxEntree) {
            nombre=(valeurMinEntree+valeurMinEntree)/2;
        }
        float valeurFloat = (nombre - 50) / 100.0f*1.5f;

        // Ajouter 0.5f pour obtenir l'intervalle 0.5f à 2.0f
        valeurFloat += 0.5f;

        Log.e("TEST","converted value :nombre= "+nombre+"  converted ="+valeurFloat);
        return valeurFloat;


    }
    private String getFirstFullLanguageCode(String shortLanguageCode) {
        Locale[] locales = Locale.getAvailableLocales();
        Boolean hasThesame =false;
        boolean firstLanguageCode = true;
        String FullLanguageCode="";
        for (Locale locale : locales) {
            if (shortLanguageCode.equalsIgnoreCase(locale.getLanguage()) && !locale.getCountry().isEmpty()) {
                if (firstLanguageCode){
                    firstLanguageCode=false;
                    FullLanguageCode =locale.getLanguage() + "-" + locale.getCountry();
                }
                if (shortLanguageCode.equalsIgnoreCase(locale.getCountry())){
                    hasThesame =true;
                    break;
                }
                Log.e("MMMM","getFirstFullLanguageCode if "+locale.getLanguage() + "-" + locale.getCountry());

            }
        }
        if (hasThesame){
            FullLanguageCode = shortLanguageCode.toLowerCase()+"-"+shortLanguageCode.toUpperCase();
        }
        return FullLanguageCode;
    }
    /**
     * Cette méthode permet d'inialiser le TTS d'android
     */
    public void initTTSAndroid(){
        tts_android = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // TTS is initialized successfully
                    Log.e("TTS_Android","TTS is initialized successfully");
                }else {
                    Log.e("TTS_Android", "TTS Initilization Failed!" + status);

                }

            }
        },"com.google.android.tts");
    }
    public void initTTSGoogleCoud(){
//        googleCloudTTS = TtsFactory.create(getParamFromFile("ApiGoogle_Key",configurationFilePseudo));
//        new AsyncTask<Void, Void, Void>() {
//            @Override
//            protected Void doInBackground(Void... voids) {
//                try {
//
//                    voiceList = googleCloudTTS.load();
//
//
//
//                } catch (Exception e) {
//                    Log.e("MRA","load  Exception-----------  "+e);
//                    Log.e(TAG,"Exception "+e);
//                    try {
//                        int startIndex = e.getMessage().indexOf('{');
//                        // Trouver la fin de la réponse JSON
//                        int endIndex = e.getMessage().lastIndexOf('}') + 1;
//                        // Extraire la réponse JSON
//                        String jsonContent = e.getMessage().substring(startIndex, endIndex);
//                        JsonObject errorLOG = JsonParser.parseString(jsonContent).getAsJsonObject();
//
//                        //Mettre   le fichier le plus récent reçu
//                        String fileName = "ERROR-LOG";
//                        File file1 = new File(Environment.getExternalStorageDirectory(), "BuddyGPT/" + fileName + ".json");
//                        if (file1.exists() && file1.isFile()) {
//                            file1.delete();
//                        }
//                        FileWriter fileWriter = new FileWriter(file1);
//                        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
//                        String jsonStringF=gson.toJson(errorLOG);
//                        fileWriter.write(jsonStringF);
//                        fileWriter.close();
//                        String errorTXT= new Date().toString()+", GoogleCloudTTSERROR,ERROR CODE= "+errorLOG.getAsJsonObject("error").get("code")+", ERROR Body{ message= "+errorLOG.getAsJsonObject("error").get("message")+", status= "+errorLOG.getAsJsonObject("error").get("status")+"}"+System.getProperty("line.separator");
//                        File file2 = new File(Environment.getExternalStorageDirectory(), "BuddyGPT/ERROR-History.txt");
//                            FileWriter fileWriter2 = new FileWriter(file2,true);
//                            fileWriter2.write(errorTXT);
//                            fileWriter2.close();
//
//                    } catch (IOException ej) {
//                        e.printStackTrace();
//                    }
//                }
//                return null;
//            }
//        }.execute();

    }



    public void playUsingReadSpeakerCaseError(String text, ITTSCallbacks ittsCallbacks){
        if(usingReadSpeaker){
            ittsCallbacks.onError("error is in readspeaker not tts_android");
            return;
        }
        String voice; //kate ou roxane

        if (getCurrentLanguage().equals("en")) {
            toast_tts_googleApi_indispo =getString(R.string.toast_tts_googleApi_indispo_en);
            toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_en);
            voice = "kate";
            if (getparam("TTS").equalsIgnoreCase("ApiGoogle") || (getparam("TTS").equalsIgnoreCase("ReadSpeaker") && getSecondTTSfromTTSList().equalsIgnoreCase("ApiGoogle"))){
                showToast(toast_tts_googleApi_indispo);
            }
            else{
                showToast(toast_tts_android_indispo);
            }
        }
        else if (getCurrentLanguage().equals("fr")){
            toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_fr);
            toast_tts_googleApi_indispo =getString(R.string.toast_tts_googleApi_indispo_fr);
            voice = "roxane";
            if (getparam("TTS").equalsIgnoreCase("ApiGoogle") || (getparam("TTS").equalsIgnoreCase("ReadSpeaker") && getSecondTTSfromTTSList().equalsIgnoreCase("ApiGoogle"))){
                showToast(toast_tts_googleApi_indispo);
            }
            else{
                showToast(toast_tts_android_indispo);
            }
        }
        else if (getCurrentLanguage().equals("de")) {
            toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_de);
            toast_tts_googleApi_indispo =getString(R.string.toast_tts_googleApi_indispo_de);
            voice = "kate";
            if ( getparam("TTS").equalsIgnoreCase("ReadSpeaker")){
                showToast(toast_tts_googleApi_indispo);
            }
            else{
                showToast(toast_tts_android_indispo);
            }
        }
        else if (getCurrentLanguage().equals("es")) {
            toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_es);
            toast_tts_googleApi_indispo =getString(R.string.toast_tts_googleApi_indispo_es);
            voice = "kate";

                showToast(toast_tts_android_indispo);

        }
        else{
            voice = "kate";
            if (getparam("TTS").equalsIgnoreCase("ReadSpeaker") ){

                getEnglishLanguageSelectedTranslator()
                        .translate(getString(R.string.toast_tts_googleApi_indispo_en))
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String translatedText) {
                                toast_tts_googleApi_indispo = translatedText;
                                showToast(toast_tts_googleApi_indispo);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                toast_tts_googleApi_indispo = getString(R.string.toast_tts_googleApi_indispo_en);
                                showToast(toast_tts_googleApi_indispo);
                            }
                        });
            }
            else{
                getEnglishLanguageSelectedTranslator()
                        .translate(getString(R.string.toast_tts_android_indispo_en))
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String translatedText) {
                                toast_tts_android_indispo = translatedText;
                                showToast(toast_tts_android_indispo);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                toast_tts_android_indispo = getString(R.string.toast_tts_android_indispo_en);
                                showToast(toast_tts_android_indispo);
                            }
                        });

            }

        }


        BuddySDK.Speech.setSpeakerVoice(voice);

        if(BuddySDK.Speech.isReadyToSpeak()) {
            Log.e("FCH_TEST","start play from TTS error");
            BuddySDK.Speech.startSpeaking(
                    text,
                    LabialExpression.SPEAK_NEUTRAL,
                    new ITTSCallback.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            ittsCallbacks.onSuccess(s);
                            Log.e("FCH_TEST","start play from TTS error  onSuccess");
                        }
                        @Override
                        public void onPause() throws RemoteException {}
                        @Override
                        public void onResume() throws RemoteException {}
                        @Override
                        public void onError(String s) throws RemoteException {
                            ittsCallbacks.onError(s);
                            Log.e("FCH_TEST","start play from TTS error  onERRor");
                        }
                    });
        }
        else{
            Log.e("FCH_TEST","else---------- start play from TTS error");
            ittsCallbacks.onError("ReadSpeaker indisponible");
        }

    }

    //#endregion ******************************************************* TTS **********************************************************************


    //#region ******************************************************* LEDs **********************************************************************

    public final IUsbCommadRsp iUsbLedCommandRsp = new IUsbCommadRsp.Stub(){
        @Override
        public void onSuccess(String success) throws RemoteException {
            Log.i(TAG, "Led success : " + success);
        }
        @Override
        public void onFailed(String error) throws RemoteException {
            Log.e(TAG, "Led error : " + error);
        }
    };

    /**
     * La fonction setLed() permet de changer la couleur des LEDs.
     * @param state : "listening" : pour la couleur GREEN #53B300
     *                "neutral"   : pour la couleur BLUE  #00D4D0
     *                "off"       : pour la couleur BLACK #000000
     */
    public void setLed(String state)  {
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
            }
            Log.i(TAG, "Changement de couleurs des LEDs ["+state+"]");
        } catch (Exception e) {
            Log.e(TAG, "Erreur pendant le changement de couleurs des LEDs ["+state+"]: "+e);
        }
    }

    //#endregion ******************************************************* LEDs **********************************************************************

    //#region ******************************************************* Fonctions utiles *********************************************************

    private Toast mToast;
    public void showToast(String message) {
        if (mToast != null) {
            mToast.cancel();
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mToast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                mToast.setDuration(Toast.LENGTH_LONG);
                mToast.show();
            }
        });
    }

    public void showInputDialog(Activity activity, String message, String attention) {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            if(dialog != null && dialog.isShowing()) dialog.dismiss();

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
                    LinearLayout.LayoutParams.WRAP_CONTENT,  // Width wraps content
                    LinearLayout.LayoutParams.WRAP_CONTENT  // Height wraps content
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
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            // Display the dialog
            try {
                dialog.show();
            } catch (WindowManager.BadTokenException e) {
                Log.w("BuddyGPT", "Dialog could not be shown: " + e.getMessage());
            }
        });
    }
    public void showInputDialog2(Activity activity, String message, String attention) {
        notifyObservers("CANCEL_RESPONSE_TIMEOUT");
        setAnimation("BuddyFace_Neutral");
        stopTTS();
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
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.6);
            dialog.getWindow().setAttributes(params);

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

    public void setAnimation(String emotion){
        Log.i("TAG_NSTREAM", "setAnimation: test "+emotion);
        if (emotion.equalsIgnoreCase("BuddyFace_Happy")){
            BuddySDK.UI.setFacialExpression(FacialExpression.HAPPY,1);
        }
        else if (emotion.equalsIgnoreCase("BuddyFace_Thinking")){
            BuddySDK.UI.setFacialExpression(FacialExpression.THINKING,1);
        }
        else if (emotion.equalsIgnoreCase("BuddyFace_Sick")){
            BuddySDK.UI.setFacialExpression(FacialExpression.SICK,1);
        }
        else if (emotion.equalsIgnoreCase("BuddyFace_Love")){
            BuddySDK.UI.setFacialExpression(FacialExpression.LOVE,1);
        }
        else if (emotion.equalsIgnoreCase("BuddyFace_Tired")){
            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED,1);
        }
        else if (emotion.equalsIgnoreCase("BuddyFace_Listening")){
            BuddySDK.UI.setFacialExpression(FacialExpression.LISTENING,1);
        }
        else if (emotion.equalsIgnoreCase("BuddyFace_Surprised")){
            BuddySDK.UI.setFacialExpression(FacialExpression.SURPRISED,1);
        }
        else if (emotion.equalsIgnoreCase("BuddyFace_Grumpy")){
            BuddySDK.UI.setFacialExpression(FacialExpression.GRUMPY,1);
        }
        else if (emotion.equalsIgnoreCase("BuddyFace_Scared")){
            BuddySDK.UI.setFacialExpression(FacialExpression.SCARED,1);
        }
        else if (emotion.equalsIgnoreCase("BuddyFace_Angry")){
            BuddySDK.UI.setFacialExpression(FacialExpression.ANGRY,1);
        }
        else if (emotion.equalsIgnoreCase("BuddyFace_Sad")){
            Log.i("TAG_NSTREAM", "setAnimation: sad");
            BuddySDK.UI.setFacialExpression(FacialExpression.SAD,1);
        }
        else if (emotion.equalsIgnoreCase("BuddyFace_Neutral")){
            Log.i("TAG_NSTREAM", "setAnimation: neutral");
            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
        }
    }

    public boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        }
        catch (Exception e) {
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
        int lastIndex = 0;

        // Boucle pour compter les mots en utilisant l'itérateur de mots
        while (wordIterator.next() != BreakIterator.DONE) {
            int currentIndex = wordIterator.current();

            // Vérifie si l'index actuel n'est pas un espace
            if (Character.isLetterOrDigit(chaine.charAt(currentIndex - 1))) {
                wordCount++;
            }
            lastIndex = currentIndex;
        }
        Log.e("MEHDI","nombre de mots  ------------ "+wordCount);
        return wordCount >= Integer.parseInt(getParamFromFile("Number_of_words","BuddyGPT.properties"));
    }

    /**
     * Cette méthode permet de checker si le device est connecté à l'internet
     */
    public boolean isConnectedToInternet() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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
    public String getCurrentLanguage(){
        if(this.langue.getNom() .equals("Français")){
            return "fr";
        }
        else if (this.langue.getNom() .equals("Anglais")){
            return "en";
        }
        else if (this.langue.getNom() .equals("Espagnol")){
            return "es";
        }
        else if (this.langue.getNom() .equals("Allemand")){
            return "de";
        }
        else return this.langue.getLanguageCode();
    }
    /**
     * cette fonction permet de stocker un paramètre dans la mémoire
     */
    public void setparam(String a,String b){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(a, b);
        editor.apply();
    }

    /**
     * cette fonction permet de récupérer la valeur du paramètre stocké
     */
    public String getparam(String a){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getString(a, "");
    }

    /**
     * la fonction notifyObservers() permet d'envoyer un message "notification" aux classes qui implémentent IDBObserver.
     * @param message : le message à envoyer
     */
    public void notifyObservers(String message) {
        Log.i("mehdi", "notifyObservers: " + message);
        for (int i = 0; i < observers.size(); i++) {
            try {
                IDBObserver ob = observers.get(i);
                ob.update(message);
            } catch (IOException e) {
                Log.e(TAG, "Erreur lors de l'envoi de la notification aux observateurs [ "+message+" ] :" + e);
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
    public String createPropertiesFile() throws IOException {
        File directory = new File(getString(R.string.path), "BuddyGPT");
        String initOrMajOrNone = ConfigurationFile.createConfigurationFile(directory);
        init();
        notYet=false;
        return initOrMajOrNone;
    }

    /**
     * Cette fonction permet de changer le volume du device
     */
    public void setVolume(int percentage) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        if (audioManager.isBluetoothScoOn()) {
            max = audioManager.getStreamMaxVolume(6);
        }

        int volume = getClosestInt((double) (percentage * max) / 100);


        if (audioManager.isBluetoothScoOn()) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
            audioManager.setStreamVolume(6, volume, AudioManager.FLAG_SHOW_UI);
        } else {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
        }
    }

    /**
     * cette fonction permet de récupérer le volume du device
     */
    public int getVolume(){
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
    public int getClosestInt(double x){
        return (int)Math.rint(x);
    }

    public void calcul_consommation(String modelName, double inputTokens, double outputTokens) {
        Log.i("USAGE","----------- calcul_consommation : modelName=" + modelName + " , inputTokens=" + inputTokens + " , outputTokens=" + outputTokens);

        String show_openAI_prices = getParamFromFile("show_openAI_prices", "BuddyGPT.properties");
        if (show_openAI_prices != null && show_openAI_prices.trim().equalsIgnoreCase("yes")) {


            double totalConsumptionSaved = Double.parseDouble(getparam("Total_cons"));
            Log.i("USAGE", "totalConsumption (avant calcul) : "+totalConsumptionSaved);

            double prixCalcul = 0;
            boolean modelFound = false;



            if (!modelFound) {
                Log.e("USAGE","Model ("+modelName+") not found !");
                priceNotAvailable();
            } else {
                totalConsumptionSaved += prixCalcul;
                setparam("Total_cons", String.valueOf(totalConsumptionSaved));
                Log.i("USAGE", "totalConsumption (après calcul) : "+totalConsumptionSaved);
            }
        }
        else{
            Log.e("USAGE","show_openAI_prices is not set to 'YES' ");
        }
    }



    public void priceNotAvailable(){
        if (getLangue().getNom().equals("Anglais")){
            showToast(getString(R.string.toast_pricing_indispo_en));
        }
        else if (getLangue().getNom().equals("Français")) {
            showToast(getString(R.string.toast_pricing_indispo_fr));
        }
        else if (getLangue().getNom().equals("Espagnol")) {
            showToast(getString(R.string.toast_pricing_indispo_de));
        }
        else if (getLangue().getNom().equals("Allemand")){
            showToast(getString(R.string.toast_pricing_indispo_es));
        }
        else{
            getEnglishLanguageSelectedTranslator()
                    .translate(getString(R.string.toast_pricing_indispo_en))
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String translatedText) {
                            showToast(translatedText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            showToast(getString(R.string.toast_pricing_indispo_en));
                        }
                    });
        }
    }

    public int getRequestTotalTokens(RequestBody requestBody){
        try{
            int requestTotalTokens = 0;
            int totalTokensContent = 0;
            int totalTokensRole = 0;
            Optional<Encoding> encoding = getRegistry().getEncodingForModel(getparam("model"));
            if (encoding.isPresent()) {
                Encoding actualEncoding = encoding.get();
                Log.i(TAG_STREAM_USAGE, "Encoding is available for the model "+getparam("model") + " --> " + actualEncoding.getName());
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);
                String requestBodyString = buffer.readUtf8();
                Log.d(TAG_STREAM, "requestBody: " + requestBodyString);
                JSONObject json = new JSONObject(requestBodyString);
                if (json.has("messages")) {
                    JSONArray messagesArray = json.getJSONArray("messages");
                    String[] roleArray = new String[messagesArray.length()];
                    String[] contentArray = new String[messagesArray.length()];
                    for (int i = 0; i < messagesArray.length(); i++) {
                        JSONObject message = messagesArray.getJSONObject(i);
                        if (message.has("role") && message.has("content")) {
                            String role = message.getString("role");
                            String content = message.getString("content");
                            roleArray[i] = role;
                            contentArray[i] = content;
                            totalTokensContent += actualEncoding.countTokens(content);
                            totalTokensRole += actualEncoding.countTokens(role);
                        }
                    }
                    //Log.v(TAG_STREAM_USAGE, "contentArray: " + Arrays.toString(contentArray));
                    //Log.v(TAG_STREAM_USAGE, "Total Tokens for Content: " + totalTokensContent);
                    //Log.v(TAG_STREAM_USAGE, "roleArray: " + Arrays.toString(roleArray));
                    //Log.v(TAG_STREAM_USAGE, "Total Tokens for Role: " + totalTokensRole);
                    requestTotalTokens = totalTokensContent + totalTokensRole;
                    Log.d(TAG_STREAM_USAGE, "Request Total Tokens : " + requestTotalTokens);
                    return requestTotalTokens;
                }
                else {
                    Log.e(TAG_STREAM_USAGE, "No 'messages' array found in the JSON.");
                    return 0;
                }
            }
            else {
                Log.e(TAG_STREAM_USAGE, "Encoding is not available for the model "+getparam("model"));
                return 0;
            }
        }
        catch(Exception e){
            Log.e(TAG_STREAM_USAGE, "getRequestTotalTokens() ERROR : " + e);
            e.printStackTrace();
            return 0;
        }
    }
    //fonction pour push files

    public void pushFiles(String assetPath, String dir) {

        Log.i(TAG, "pushFiles( " + assetPath + " , " + dir + " )");

        File mWorkingPath = new File(dir);

        // if this directory does not exist, make one.
        if (!mWorkingPath.exists()) {
            if (!mWorkingPath.mkdirs()) {
                Log.i(TAG, "pushFiles : create directory");
            }
        } else {
            Log.i(TAG, "pushFiles : directory already exists");
        }


        // Check if assetPath is a file
        File outFile = new File(mWorkingPath, assetPath.substring(assetPath.lastIndexOf('/') + 1));
        if (outFile.exists()) {
            Log.i(TAG, "pushFiles : file already exists, skipping copy");
            return; // If file exists, do nothing
        }
                Log.i(TAG, "pushFiles : Check if assetPath is a file if");
                // assetPath is a file path, not a directory
                try (InputStream in = getAssets().open(assetPath);
                     OutputStream out = new FileOutputStream(outFile)) {

                    byte[] buf = new byte[1024];
                    int len;
                    Log.i(TAG, "pushFiles : Check if assetPath is a file while");
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }

                    Log.i(TAG, "pushFiles : Copied file: " + assetPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "pushFiles : IOException : " + e);
                }


    }



    //#endregion ******************************************************* Fonctions utiles *********************************************************

}
