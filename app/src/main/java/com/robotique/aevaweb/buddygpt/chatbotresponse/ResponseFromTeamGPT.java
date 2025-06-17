package com.robotique.aevaweb.buddygpt.chatbotresponse;


import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddysdk.BuddySDK;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.mlkit.nl.languageid.IdentifiedLanguage;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;

import com.robotique.aevaweb.buddygpt.R;
import com.robotique.aevaweb.buddygpt.application.BuddyGPTApplication;
import com.robotique.aevaweb.buddygpt.models.Parameters;
import com.robotique.aevaweb.buddygpt.models.ParametersResponse;
import com.robotique.aevaweb.buddygpt.models.Request;
import com.robotique.aevaweb.buddygpt.utilis.ApiEndpointInterface;
import com.robotique.aevaweb.buddygpt.utilis.RetrofitClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ResponseFromTeamGPT{
    private static final String TAG_STREAM = "STREAM_MODE";
    private static final String TAG_PARAM = "GET_PARAM";
    String chatBotServerNoResponceFr;
    String chatBotServerNoResponceEn;
    String chatBotServerNoResponceEs;
    String chatBotServerNoResponceDe;
    private final Queue<String> phrasesQueue = new LinkedList<>();
    private final Handler phrasesHandler = new Handler();
    private final Handler wordsHandler = new Handler();
    private Runnable wordsRunnable;
    private String answer = "";
    private String phrase = "";
    private String errorMsg = "";
    private String historicMessages = "messages";
    private Runnable phrasesRunnable;
    private String currentDisplayedText = "";
    public boolean isReadyToSpeak = true;
    private boolean isFullResponseReceived = false;
    private boolean isDisplayFinished = true;
    private String text = "";
    private String phraseToPronounceWhenResumed;
    private boolean isReset = false;
    public boolean isError = false;
    private boolean isPaused = false;
    private JSONArray existingHistoryArray;
    boolean isSessionIdProcessed = false;
    boolean isEmotionNeutral = false;

    BuddyGPTApplication buddyGPTApplication;
    public ResponseFromTeamGPT(BuddyGPTApplication context) {
        this.buddyGPTApplication = context;
        chatBotServerNoResponceFr= buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_fr","BuddyGPT.properties");
        chatBotServerNoResponceEn= buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_en","BuddyGPT.properties");
        chatBotServerNoResponceEs= buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_es","BuddyGPT.properties");
        chatBotServerNoResponceDe= buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_de","BuddyGPT.properties");

    }
    public void getParameters() {
        final CountDownLatch latch = new CountDownLatch(1); // Initialize the latch with count 1

        new Thread(() -> {
            try {
                String url = buddyGPTApplication.getparam("TeamGPT_url");
                String endpoint = buddyGPTApplication.getparam("TeamGPT_ApiEndpoint_Params");
                String gptKey = buddyGPTApplication.getparam("TeamGPT_Key");
                String imeiDevice = buddyGPTApplication.getparam("TeamGPT_ID_Device");

                URL obj = new URL(url + endpoint);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("TeamGPT-Key", gptKey);
                con.setRequestProperty("IMEI-ID-Device", imeiDevice);

                int responseCode = con.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    buddyGPTApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY","FALSE");
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    String contentType = con.getHeaderField("Content-Type");

                    if (contentType != null && contentType.contains("application/json")) {
                        JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
                        Log.i(TAG_STREAM, "run: PARAMS : "+jsonObject.toString());
                        JsonObject parametersObject = jsonObject.getAsJsonObject("parameters");

                        Gson gson = new Gson();
                        Parameters parameters = gson.fromJson(parametersObject.toString(), Parameters.class);

                        if (parameters != null) {
                            if (!parameters.getStt().equalsIgnoreCase("local")
                                    || !parameters.getTts().equalsIgnoreCase("local")){
                                buddyGPTApplication.setparam("TeamGPT_Key",gptKey);
                                buddyGPTApplication.resetSharedPreferences();
                                buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_DEVICE_ID");
                                buddyGPTApplication.setparam("INVALID_TEAMGPT_DEVICE_ID", "TRUE");
                            }else{
                                buddyGPTApplication.setparam("INVALID_TEAMGPT_DEVICE_ID", "FALSE");
                                buddyGPTApplication.setparam("NomCompte", parameters.getNomCompte());
                                buddyGPTApplication.setparam("TeamGPT_Key", parameters.getTeamGptKey());
                                buddyGPTApplication.setparam("SelectedChatbot", parameters.getSelectedChatbot());
                                buddyGPTApplication.setparam("STT-TeamGPT", parameters.getStt());
                                buddyGPTApplication.setparam("TTS-TeamGPT", parameters.getTts());
                                buddyGPTApplication.setparam("Header", parameters.getHeader());
                                buddyGPTApplication.setparam("Entete", parameters.getEntete());
                                buddyGPTApplication.setparam("Email", parameters.getEmail());
                                if(buddyGPTApplication.getparam("Mail_Destination").equalsIgnoreCase(""))
                                    buddyGPTApplication.setparam("Mail_Destination",parameters.getEmail());
                                buddyGPTApplication.setparam("Stream_mode",parameters.getStreamMode());
                                buddyGPTApplication.setparam("Mail_sender",parameters.getMailSender());
                                buddyGPTApplication.setparam("Smtp_host",parameters.getSmtpHost());
                                buddyGPTApplication.setparam("Password_mail_sender",parameters.getPasswordMailSender());
                                buddyGPTApplication.setparam("Smtp_port",parameters.getSmtpPort());
                                buddyGPTApplication.setparam("show_price",parameters.getShowPrice());
                                buddyGPTApplication.setparam("CustomGPT_model",parameters.getCustomGptModel());
                                buddyGPTApplication.setparam("Modele_Mistral",parameters.getModeleMistral());
                                buddyGPTApplication.setparam("Modele_Openai",parameters.getModeleOpenai());
                                buddyGPTApplication.setparam("Mail_Subject_fr",parameters.getMailSubjectFr());
                                buddyGPTApplication.setparam("Mail_Subject_en",parameters.getMailSubjectEn());

                            if(parameters.getEmailSupport()!=null && !parameters.getEmailSupport().equalsIgnoreCase(""))
                                buddyGPTApplication.setparam("email_support",parameters.getEmailSupport());
                            else
                                buddyGPTApplication.setparam("email_support"," _ ");

                            if(parameters.getImeiDevice()!=null && !parameters.getImeiDevice().equalsIgnoreCase(""))
                                buddyGPTApplication.setparam("IMEI_ID_Device",parameters.getImeiDevice());
                            else
                                buddyGPTApplication.setparam("IMEI_ID_Device"," _ ");
                            if(parameters.getIdCompte()!=null && !parameters.getIdCompte().equalsIgnoreCase(""))
                                buddyGPTApplication.setparam("IdCompte",parameters.getIdCompte());
                            else
                                buddyGPTApplication.setparam("IdCompte"," _ ");
                            buddyGPTApplication.setparam("Modele_gemini",parameters.getModeleGemini());
                            if(buddyGPTApplication.getparam("STT-TeamGPT").equalsIgnoreCase("local")
                                && buddyGPTApplication.getparam("STT").equalsIgnoreCase(""))
                                buddyGPTApplication.setparam("STT", "Android");
                            if(buddyGPTApplication.getparam("TTS-TeamGPT").equalsIgnoreCase("local")
                                    && buddyGPTApplication.getparam("TTS").equalsIgnoreCase(""))
                                buddyGPTApplication.setparam("TTS", "ReadSpeaker");
                        }
                        }
                    }
                }
                else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                    Log.i(TAG_PARAM, "run: notifyObservers response msg "+con.getResponseMessage());
                    Log.i(TAG_PARAM, "run: notifyObservers INVALID_TEAMGPT_KEY 1");
                    buddyGPTApplication.setparam("TeamGPT_Key",gptKey);
                    buddyGPTApplication.resetSharedPreferences();
                    buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "TRUE");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_DEVICE_ID", "FALSE");
                }
                con.disconnect();
            } catch (Exception e) {
                Log.e("HOU", "Exception in getParameters: ", e);
            } finally {
                latch.countDown(); // Ensure latch is counted down regardless of success or failure
            }
        }).start();

        try {
            latch.await(); // Wait for the thread to finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Re-interrupt the thread
            Log.e(TAG_PARAM, "Thread was interrupted while waiting for latch", e);
        }

    }


    public void sendPutRequestStream(String question, int numberOfQuestion) {
        isEmotionNeutral=false;
        String baseUrl = buddyGPTApplication.getparam("TeamGPT_url");
        String endpoint = buddyGPTApplication.getparam("TeamGPT_ApiEndpoint_Response"); // Endpoint dynamique.
        String gptKey = buddyGPTApplication.getparam("TeamGPT_Key"); // Clé API.
        String imeiDevice = buddyGPTApplication.getparam("TeamGPT_ID_Device");

        Retrofit retrofit = RetrofitClient.getClient(baseUrl);
        ApiEndpointInterface apiService = retrofit.create(ApiEndpointInterface.class);

        // Préparez le corps de la requête.
        Request payload = new Request();
        payload.setTextInput(question);
        payload.setImeiIdDevice(imeiDevice);
        payload.setEmotion(buddyGPTApplication.getparam("switch_emotion").equals("true"));
        payload.setCommandes(false);
        payload.setLangue(buddyGPTApplication.getLangue().getLanguageCode().split("-")[0]);
        if (!buddyGPTApplication.getparam("session_id").isEmpty()) {
            payload.setSessionId(buddyGPTApplication.getparam("session_id"));
        }
        saveRequestToFile(payload);
        updateMessageHistory(question);
        long requestStartTime = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss:SSS");

        String formattedTime =sdf.format(new Date(requestStartTime));
        // Enregistrer le temps d'envoi de la requête

        buddyGPTApplication.setQuestionTime(requestStartTime);
        Log.i(TAG_STREAM, "Request sent at: " + formattedTime);

        // Envoyez la requête avec endpoint et clé.
        Log.i("HOU_DEBUG", "sendPutRequestStream: baseUrl "+baseUrl+"endpoint "+endpoint);

        Call<ResponseBody> call = apiService.sendRequestTeamGPT(baseUrl+""+endpoint, gptKey, payload);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {

                    buddyGPTApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                    try {
                        // Enregistrer le temps de réception de la première réponse
                        long responseStartTime = System.currentTimeMillis();
                        buddyGPTApplication.setResponseTime(responseStartTime);
                        String formattedTime2 =sdf.format(new Date(responseStartTime));
                        Log.i(TAG_STREAM, "First response received at: " + formattedTime2);

                        // Calculer et enregistrer le temps de réponse
                        long responseTime = buddyGPTApplication.getResponseTime() - buddyGPTApplication.getQuestionTime();
                        Log.i(TAG_STREAM, "Response time: " + responseTime + " ms");


                        buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "FALSE");
                        // Traitez la réponse en flux.
                        handleStreamingResponse(response.body().byteStream());
                    } catch (Exception e) {
                        e.printStackTrace();
                        handleError();
                    }
                } else if (response.code() == 400) {
                    buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "TRUE");
                }
                else if (response.code() == 500) {
                    buddyGPTApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                    buddyGPTApplication.notifyObservers("Session_ID_ERROR");
                    buddyGPTApplication.setparam("session_id","");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (t instanceof java.net.SocketTimeoutException) {
                    Log.e("Retrofit", "Timeout de connexion ou de lecture !");
                } else if (t instanceof java.io.IOException) {
                    Log.e("Retrofit", "Erreur réseau ou serveur !");
                } else {
                    Log.e("Retrofit", "Erreur inattendue : " + t.getMessage());
                }
                handleError();
            }
        });
    }
    private void updateMessageHistory(String question) {
        try {
            if(buddyGPTApplication.getparam(historicMessages).equalsIgnoreCase(""))
                buddyGPTApplication.setparam(historicMessages,"[]");
            String jsonArrayString = buddyGPTApplication.getparam(historicMessages);
             existingHistoryArray = new JSONArray(jsonArrayString);
            JSONObject newQuestionObject = new JSONObject();
            newQuestionObject.put("Question", question);
            existingHistoryArray.put(newQuestionObject);
            buddyGPTApplication.setparam(historicMessages, existingHistoryArray.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void saveRequestToFile(Request payload) {
        String fileName = "TeamGPT-sent";
        File file = new File(Environment.getExternalStorageDirectory(), "BuddyGPT/" + fileName + ".json");
        try {
            if (file.exists() && file.isFile()) file.delete();
            try (FileWriter fileWriter = new FileWriter(file)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                fileWriter.write(gson.toJson(payload));
            }
            Log.v("Json_API", "File saved successfully.");
        } catch (IOException e) {
            Log.i("TAG", "saveRequestToFile: ");
        }
    }

    // Method to handle error messages
    private void handleError() {
        String errorMessage;
        switch (buddyGPTApplication.getCurrentLanguage()) {
            case "en":
                errorMessage = buddyGPTApplication.getString(R.string.chatBot_ERROR_en);
                break;
            case "fr":
                errorMessage = buddyGPTApplication.getString(R.string.chatBot_ERROR_fr);
                break;
            case "es":
                errorMessage = buddyGPTApplication.getString(R.string.chatBot_ERROR_es);
                break;
            case "de":
                errorMessage = buddyGPTApplication.getString(R.string.chatBot_ERROR_de);
                break;
            default:
                errorMessage = buddyGPTApplication.getString(R.string.chatBot_ERROR_en);
                break;
        }
        buddyGPTApplication.notifyObservers("MODE_STREAM_SPEAK;SPLIT;" + errorMessage );
    }


    private void pronouncePhrase(String phraseToPronounce){
        Log.i(TAG_STREAM, "pronouncePhrase: "+phraseToPronounce);
        if(!isReset){
            if (!buddyGPTApplication.isTimeoutExpired()) {
                Log.i(TAG_STREAM, "TTS : [ " + phraseToPronounce + " ]");
                if (buddyGPTApplication.getparam("switch_visibility").equals("true")) {
                    showPhrase(phraseToPronounce);
                }
                else{
                    isDisplayFinished = true;
                }
                buddyGPTApplication.notifyObservers("MODE_STREAM_SPEAK;SPLIT;"+phraseToPronounce);
            }
            else {
                Log.w(TAG_STREAM, "Pause streaming until TTS is ready again [ " + phraseToPronounce + " ]");
                pauseStreaming(phraseToPronounce);
            }
        }
    }
    private void pauseStreaming(String phraseToPronounceWhenResumed){
        isPaused = true;
        this.phraseToPronounceWhenResumed = phraseToPronounceWhenResumed;
    }

    private void showPhrase(String phrase) {
        isDisplayFinished = false;
        if(isError) currentDisplayedText = "";
        final int totalLength = currentDisplayedText.length() + phrase.length();
        for (int i = 1; i <= phrase.length(); i++) {
            final String phraseToShow = currentDisplayedText + phrase.substring(0, i);
            wordsHandler.postDelayed(wordsRunnable = () -> {
                buddyGPTApplication.notifyObservers("MODE_STREAM_TEXT;SPLIT;"+phraseToShow);
                if (phraseToShow.length() == totalLength) {
                    isDisplayFinished = true;
                }
            }, i );
        }
        currentDisplayedText += phrase + " ";
    }
    private void handleStreamingResponse(InputStream response) {
        Log.i(TAG_STREAM, "handleStreamingResponse: HOU ");
        try {
            onStartStreaming();

            InputStreamReader inputStreamReader = new InputStreamReader(response);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String fileName = "TeamGPT-recv-stream";
            StringBuilder formattedContent = new StringBuilder();
            String line;
            Log.i(TAG_STREAM, "handleStreamingResponse: !isReset "+!isReset  );
            Log.i(TAG_STREAM, "handleStreamingResponse: !isError "+!isError  );
            while ((line = reader.readLine()) != null && reader.readLine().equalsIgnoreCase("") && !isReset && !isError) {
                try {

                    Log.w("HOU_DEBUG", "Received line: " + line);

                    if (line.contains("\"is_finished\": true,")) formattedContent.append(line);
                    else{
                        JSONObject jsonObject = new JSONObject(line.replace("data:", "").trim());
                        String formattedObject = jsonObject.toString(4);
                        formattedContent.append("data: ").append(formattedObject);
                        formattedContent.append("\n\n");
                    }

                    Log.w(TAG_STREAM, "Received line: " + line);
                    String jsonData = line.substring("data:".length()).trim();

                    JSONObject jsonObject = new JSONObject(jsonData);

                    if(jsonObject.has("Emotion") && jsonObject.getString("Emotion").equalsIgnoreCase("") &&
                            jsonObject.has("Answer") && jsonObject.getString("Answer").equalsIgnoreCase("")){
                        Log.i(TAG_STREAM, "handleStreamingResponse: continue");
                        if(jsonObject.has("is_finished") && jsonObject.getBoolean("is_finished")){
                            isFullResponseReceived=true;
                            isSessionIdProcessed=false;
                        }

                    }else {
                        Log.i(TAG_STREAM, "handleStreamingResponse: else continue");
                        // Handle "emotion"
                        if (buddyGPTApplication.getparam("switch_emotion").equals("true")) {

                            if ( jsonObject.has("Emotion") && !jsonObject.getString("Emotion").equalsIgnoreCase("")) {
                                // Si l'émotion n'a pas encore été traitée, définir l'animation

                                String emotion = jsonObject.getString("Emotion");
                                Log.i(TAG_STREAM, "handleStreamingResponse: emo " + emotion);
                                buddyGPTApplication.notifyObservers("Emotion_Change;SPLIT;" + emotion);

                            } else {
                                Log.i(TAG_STREAM, "handleStreamingResponse: emo null");
                            }

                        }else{
                            if(!isEmotionNeutral) {
                                isEmotionNeutral = true;
                                buddyGPTApplication.notifyObservers("Emotion_Change;SPLIT;BuddyFace_Neutral");
                            }

                        }


                        // Handle "session_id"
                        Handler mainHandler2 = new Handler(Looper.getMainLooper());
                        if(!isSessionIdProcessed){
                            if (jsonObject.has("session_id")) {
                                String sessionId = jsonObject.getString("session_id");
                                Log.i(TAG_STREAM, "handleStreamingResponse: session "+jsonObject.getString("session_id"));

                                if (!buddyGPTApplication.getparam("session_id").equalsIgnoreCase(sessionId)) {

                                    mainHandler2.post(() ->
                                        buddyGPTApplication.notifyObservers("Session_ID_Changed")
                                    );
                                    buddyGPTApplication.setparam("session_id", sessionId);
                                    String jsonArrayString = buddyGPTApplication.getparam(historicMessages);
                                    existingHistoryArray = new JSONArray(jsonArrayString);
                                    JSONObject newSessionObject = new JSONObject();
                                    newSessionObject.put("Session", buddyGPTApplication.getparam("SelectedChatbot")+" - "+ buddyGPTApplication.getModel());
                                    existingHistoryArray.put(newSessionObject);
                                    buddyGPTApplication.setparam(historicMessages, existingHistoryArray.toString());
                                }


                            }
                            isSessionIdProcessed = true;
                        }


                        // Handle "Answer"
                        if (jsonObject.has("Answer")) {

                            String resp = jsonObject.getString("Answer");

                            if (!resp.isEmpty()) {
                                Log.i(TAG_STREAM, "handleStreamingResponse: if1 "+resp);
                                answer += " "+resp;
                                phrase =resp;
                                onNewPhrase();
                                if(jsonObject.getBoolean("is_finished")){
                                    isFullResponseReceived=true;
                                    isSessionIdProcessed=false;
                                }

                            }else{
                                if(jsonObject.getBoolean("is_finished")){
                                    isFullResponseReceived=true;
                                    isSessionIdProcessed=false;
                                }
                            }
                        }

                    }


                } catch (JSONException e) {
                    Log.e(TAG_STREAM, "Invalid JSON data: " + line, e);
                }

            }
            String jsonArrayString = buddyGPTApplication.getparam(historicMessages);
            existingHistoryArray = new JSONArray(jsonArrayString);
            JSONObject newRespObject = new JSONObject();
            long responseTime = buddyGPTApplication.getResponseTime() - buddyGPTApplication.getQuestionTime();
            DecimalFormat df = new DecimalFormat("#,###");
            String formattedTime= df.format(responseTime);
            newRespObject.put("Response", answer +";SPLIT;"+formattedTime+" ms");
            existingHistoryArray.put(newRespObject);
            buddyGPTApplication.setparam(historicMessages, existingHistoryArray.toString());
            // Save formattedContent in ChatGPT-recv-stream.txt
            storeStreamResponse(fileName, formattedContent.toString());

        } catch (Exception e) {
            e.printStackTrace();
            onErrorStreaming("EXCEPTION", null);
        }
    }

    private void onNewPhrase() {
        Log.w(TAG_STREAM, "Phrase: " + phrase);
        phrasesQueue.add(phrase);
    }

    public void onTTSEnd(){
        Log.i(TAG_STREAM,"TTS END");
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
        }
        catch (Exception e){
            Log.e(TAG_STREAM,"BuddySDK Exception : "+e);
        }
        isReadyToSpeak = true;
    }
    public void resumeStreaming(){
        if(isPaused) {
            isPaused = false;
            pronouncePhrase(phraseToPronounceWhenResumed);
        }
    }


    private void processPhrasesWithDelay() {
        Log.i(TAG_STREAM, "processPhrasesWithDelay: phrasesQueue.isEmpty()="+phrasesQueue.isEmpty());
        Log.i(TAG_STREAM, "processPhrasesWithDelay: isDisplayFinished= "+isDisplayFinished);
        if (!phrasesQueue.isEmpty() && isDisplayFinished) {
            Log.i(TAG_STREAM, "processPhrasesWithDelay: if");
            if(isReadyToSpeak) {
                Log.i(TAG_STREAM, "processPhrasesWithDelay: isReadyToSpeak");
                isReadyToSpeak = false;
                String phraseToPronounce = phrasesQueue.poll();
                if(phraseToPronounce != null){
                    if (buddyGPTApplication.getparam("Detection_de_langue").equals("true") &&
                            buddyGPTApplication.nombreDeMotsCheck(phraseToPronounce)) {
                        LanguageIdentifier languageIdentifier = LanguageIdentification.getClient();
                        languageIdentifier.identifyPossibleLanguages(phraseToPronounce)
                                .addOnSuccessListener(
                                        identifiedLanguages -> {
                                            if (identifiedLanguages.isEmpty()) {
                                                Log.e("MRA_idetifyLanguage", "languageIdentifier : Can't identify language of : " + phraseToPronounce);
                                                pronouncePhrase(phraseToPronounce);
                                            } else {
                                                // Utiliser la première langue identifiée
                                                IdentifiedLanguage language = identifiedLanguages.get(0);
                                                String languageCode = language.getLanguageTag();
                                                float confidence = language.getConfidence();
                                                Log.i("MRA_idetifyLanguage", "Language of : [ " + phraseToPronounce + " ] is : " + languageCode + ", Confidence: " + confidence);
                                                if (buddyGPTApplication.getParamFromFile("Detection_confidence_rate","BuddyGPT.properties")!=null &&
                                                        !buddyGPTApplication.getParamFromFile("Detection_confidence_rate","BuddyGPT.properties").trim().equals("")&&
                                                        !buddyGPTApplication.getParamFromFile("Detection_confidence_rate","BuddyGPT.properties").trim().equals("0")) {
                                                    if (Integer.parseInt(buddyGPTApplication.getParamFromFile("Detection_confidence_rate", "BuddyGPT.properties")) <= (confidence * 100)) {
                                                        buddyGPTApplication.setLanguageDetected(languageCode.trim());
                                                        pronouncePhrase(phraseToPronounce);
                                                    } else {
                                                        pronouncePhrase(phraseToPronounce);
                                                    }
                                                }
                                                else {
                                                    buddyGPTApplication.setLanguageDetected(languageCode.trim());
                                                    pronouncePhrase(phraseToPronounce);
                                                }
                                            }
                                        })
                                .addOnFailureListener(e -> pronouncePhrase(phraseToPronounce));
                    }
                    else{
                        pronouncePhrase(phraseToPronounce);
                    }
                }
            }
        }
        else{
            Log.i(TAG_STREAM, "processPhrasesWithDelay: else");
            Log.i(TAG_STREAM, "processPhrasesWithDelay: isReadyToSpeak "+isReadyToSpeak);
            Log.i(TAG_STREAM, "processPhrasesWithDelay: isFullResponseReceived :" + isFullResponseReceived);
            if( isDisplayFinished && ((isFullResponseReceived && isReadyToSpeak) || (isError && isReadyToSpeak) )){
                onFinishStreaming();
                buddyGPTApplication.notifyObservers("TTS_success");
                reset();
                return;
            }
        }
        phrasesHandler.postDelayed(phrasesRunnable = this::processPhrasesWithDelay, 50);
    }

    private void onFinishStreaming(){
        Log.i(TAG_STREAM, "------------------END-------------------");
    }

    private void storeStreamResponse(String fileName, String formattedContent) {
        Log.w(TAG_STREAM, "storeStreamResponse()");
        try{
            File file = new File(Environment.getExternalStorageDirectory(), "BuddyGPT/" + fileName + ".txt");
            if (file.exists() && file.isFile()) {
                boolean result = file.delete();
                Log.i(TAG_STREAM, "storeStreamResponse() : file deleted : "+result);
            }
            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.write(formattedContent);
            }
            Log.i(TAG_STREAM, "storeStreamResponse() : new file added");
        }
        catch(Exception e){
            Log.e(TAG_STREAM, "storeStreamResponse() : "+e);
            e.printStackTrace();
        }
    }
    private void onStartStreaming(){
        if(!isReset){
            Log.i(TAG_STREAM, "------------------START-------------------");
            processPhrasesWithDelay();
        }
    }
    private void onErrorStreaming(String error,Response<ResponseBody> response){
        Log.e(TAG_STREAM, "------------------ERROR-------------------");

        if(!isReset){

            if(phrasesRunnable != null) phrasesHandler.removeCallbacks(phrasesRunnable);
            phrasesHandler.removeCallbacksAndMessages(null);
            phrasesQueue.clear();

            if(wordsRunnable != null) wordsHandler.removeCallbacks(wordsRunnable);
            wordsHandler.removeCallbacksAndMessages(null);

            buddyGPTApplication.stopTTS();
            try {
                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
            }
            catch (Exception e){
                Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
            }

            SystemClock.sleep(1000);

            isReadyToSpeak = false;
            isError = true;

            if(error.equals("RESPONSE_NOT_SUCCESSFUL")){

                try {
                    if (response != null && response.errorBody() != null){
                        JsonObject errorLOG = new JsonObject();
                        JsonObject errorCode = new JsonObject();
                        errorCode.addProperty("ERROR CODE", response.code());
                        String jsonString = response.errorBody().string();
                        JSONObject jsonErrorContent = new JSONObject(jsonString);
                        JSONObject errorObject = jsonErrorContent.getJSONObject("error");
                        String message = errorObject.getString("message");
                        String type = errorObject.getString("type");
                        String param = errorObject.getString("param");
                        String code = errorObject.getString("code");
                        JsonObject reformErrorJson = new JsonObject();
                        reformErrorJson.addProperty("message",message);
                        reformErrorJson.addProperty("type",type);
                        reformErrorJson.addProperty("param",param);
                        reformErrorJson.addProperty("code",code);
                        errorCode.add("ERROR Body",reformErrorJson);
                        errorLOG.add("OpenAIERROR",errorCode);
                        String fileName = "ERROR-LOG";
                        File file1 = new File(Environment.getExternalStorageDirectory(), "BuddyGPT/" + fileName + ".json");
                        try {
                            if (file1.exists() && file1.isFile()) {
                                file1.delete();
                            }
                            FileWriter fileWriter = new FileWriter(file1);
                            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                            String jsonStringF=gson.toJson(errorLOG);
                            fileWriter.write(jsonStringF);
                            fileWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        String errorTXT= new Date().toString()+", OpenAIERROR,ERROR CODE= "+response.code()
                                +", ERROR Body{ message= "+message+", type= "+type+", param= "+param+", code= "+code+"}"
                                +System.getProperty("line.separator");
                        File file2 = new File(Environment.getExternalStorageDirectory(), "BuddyGPT/ERROR-History.txt");
                        try {
                            FileWriter fileWriter = new FileWriter(file2,true);
                            fileWriter.write(errorTXT);
                            fileWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }

                if (buddyGPTApplication.getLangue().getNom().equals("Anglais")){
                    errorMsg = buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_en","BuddyGPT.properties");
                }
                else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                    errorMsg =  buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_fr","BuddyGPT.properties");
                }
                else if (buddyGPTApplication.getLangue().getNom().equals("Espagnol")) {
                    errorMsg = buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_es","BuddyGPT.properties");
                }
                else if (buddyGPTApplication.getLangue().getNom().equals("Allemand")){
                    errorMsg =  buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_de","BuddyGPT.properties");
                }
                else{
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_en","BuddyGPT.properties"))
                            .addOnSuccessListener(translatedText -> errorMsg = translatedText)
                            .addOnFailureListener(e -> errorMsg = buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_en","BuddyGPT.properties"));
                }
            }
            else if(error.equals("FAILURE")){
                if (buddyGPTApplication.getLangue().getNom().equals("Anglais")){
                    errorMsg = buddyGPTApplication.getString(R.string.chatBotNoFound_en);
                }
                else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                    errorMsg =  buddyGPTApplication.getString(R.string.chatBotNoFound_fr);
                }
                else if (buddyGPTApplication.getLangue().getNom().equals("Espagnol")) {
                    errorMsg = buddyGPTApplication.getString(R.string.chatBotNoFound_es);
                }
                else if (buddyGPTApplication.getLangue().getNom().equals("Allemand")){
                    errorMsg =  buddyGPTApplication.getString(R.string.chatBotNoFound_de);
                }
                else{
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getString(R.string.chatBotNoFound_en))
                            .addOnSuccessListener(translatedText -> errorMsg = translatedText)
                            .addOnFailureListener(e -> errorMsg = buddyGPTApplication.getString(R.string.chatBotNoFound_en));
                }
            }
            else{
                if (buddyGPTApplication.getLangue().getNom().equals("Anglais")){
                    errorMsg = buddyGPTApplication.getString(R.string.chatBot_ERROR_en);
                }
                else if (buddyGPTApplication.getLangue().getNom().equals("Français")) {
                    errorMsg =  buddyGPTApplication.getString(R.string.chatBot_ERROR_fr);
                }
                else if (buddyGPTApplication.getLangue().getNom().equals("Espagnol")) {
                    errorMsg = buddyGPTApplication.getString(R.string.chatBot_ERROR_es);
                }
                else if (buddyGPTApplication.getLangue().getNom().equals("Allemand")){
                    errorMsg =  buddyGPTApplication.getString(R.string.chatBot_ERROR_de);
                }
                else{
                    buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getString(R.string.chatBot_ERROR_en))
                            .addOnSuccessListener(translatedText -> errorMsg = translatedText)
                            .addOnFailureListener(e -> errorMsg = buddyGPTApplication.getString(R.string.chatBot_ERROR_en));
                }
            }

            buddyGPTApplication.setMessageError(true);
            if (!buddyGPTApplication.isOpenaialreadySwitchEmotion()) {
                try {
                    BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                }
                catch (Exception e){
                    Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
                }
            }
            processPhrasesWithDelay();
            pronouncePhrase(errorMsg);
        }
    }


    public void reset(){
        String result = "";
        Log.i(TAG_STREAM, "------------------reset-------------------");
        isReset = true;
        //reset phrasesQueue:
        if(phrasesRunnable != null) phrasesHandler.removeCallbacks(phrasesRunnable);
        phrasesHandler.removeCallbacksAndMessages(null);
        phrasesQueue.clear();
        isReadyToSpeak = true;
        result="";
        //reset wordsQueue:
        if(wordsRunnable != null) wordsHandler.removeCallbacks(wordsRunnable);
        wordsHandler.removeCallbacksAndMessages(null);
        buddyGPTApplication.setResponseFromTeamGPT(null);
    }


}
