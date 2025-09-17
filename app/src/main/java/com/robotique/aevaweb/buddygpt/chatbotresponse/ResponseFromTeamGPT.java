package com.robotique.aevaweb.buddygpt.chatbotresponse;


import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddysdk.BuddySDK;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.mlkit.nl.languageid.IdentifiedLanguage;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.robotique.aevaweb.buddygpt.R;
import com.robotique.aevaweb.buddygpt.application.BuddyGPTApplication;
import com.robotique.aevaweb.buddygpt.models.Parameters;
import com.robotique.aevaweb.buddygpt.models.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class ResponseFromTeamGPT {
    private static final String TAG_STREAM = "STREAM_MODE";
    private static final String TAG_PARAM = "GET_PARAM";
    private final Queue<String> phrasesQueue = new LinkedList<>();
    private final Handler phrasesHandler = new Handler();
    private final Handler wordsHandler = new Handler();
    public boolean isReadyToSpeak = true;
    public boolean isError = false;
    String chatBotServerNoResponceFr;
    String chatBotServerNoResponceEn;
    String chatBotServerNoResponceEs;
    String chatBotServerNoResponceDe;
    boolean isSessionIdProcessed = false;
    boolean isEmotionNeutral = false;
    BuddyGPTApplication buddyGPTApplication;
    private Runnable wordsRunnable;
    private String answer = "";
    private String phrase = "";
    private String errorMsg = "";
    private final String historicMessages = "messages";
    private final String TeamGPTKey = "TeamGPT_Key";
    private Runnable phrasesRunnable;
    private String currentDisplayedText = "";
    private boolean isFullResponseReceived = false;
    private boolean isDisplayFinished = true;
    private String phraseToPronounceWhenResumed;
    private boolean isReset = false;
    private boolean isPaused = false;
    private JSONArray existingHistoryArray;

    public ResponseFromTeamGPT(BuddyGPTApplication context) {
        this.buddyGPTApplication = context;
        chatBotServerNoResponceFr = buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_fr", "BuddyGPT.properties");
        chatBotServerNoResponceEn = buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_en", "BuddyGPT.properties");
        chatBotServerNoResponceEs = buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_es", "BuddyGPT.properties");
        chatBotServerNoResponceDe = buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_de", "BuddyGPT.properties");

    }

    public void getParameters() {
        final CountDownLatch latch = new CountDownLatch(1); // Initialize the latch with count 1

        new Thread(() -> {
            try {
                String url = buddyGPTApplication.getparam("TeamGPT_url");
                String endpoint = buddyGPTApplication.getparam("TeamGPT_ApiEndpoint_Params");
                String gptKey = buddyGPTApplication.getparam(TeamGPTKey);
                String imeiDevice = buddyGPTApplication.getparam("TeamGPT_ID_Device");

                URL obj = new URL(url + endpoint);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("TeamGPT-Key", gptKey);
                con.setRequestProperty("ID-Device", imeiDevice);

                int responseCode = con.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    buddyGPTApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "FALSE");
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
                        Log.i(TAG_STREAM, "run: PARAMS : " + jsonObject.toString());
                        JsonArray parametersArray = jsonObject.getAsJsonArray("parameters");
                        if (parametersArray != null && parametersArray.size() > 0) {
                            JsonObject parametersObject = parametersArray.get(0).getAsJsonObject();
                            Gson gson = new Gson();
                            Parameters parameters = gson.fromJson(parametersObject.toString(), Parameters.class);

                            if (parameters != null) {
                                if (!parameters.getStt().equalsIgnoreCase("local")
                                        || !parameters.getTts().equalsIgnoreCase("local")) {
                                    buddyGPTApplication.setparam(TeamGPTKey, gptKey);
                                    buddyGPTApplication.resetSharedPreferences();
                                    buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_DEVICE_ID");
                                    buddyGPTApplication.setparam("INVALID_TEAMGPT_DEVICE_ID", "TRUE");
                                } else {
                                    buddyGPTApplication.setparam("INVALID_TEAMGPT_DEVICE_ID", "FALSE");
                                    buddyGPTApplication.setparam("NomCompte", parameters.getNomCompte());
                                    buddyGPTApplication.setparam(TeamGPTKey, parameters.getTeamGptKey());
                                    buddyGPTApplication.setparam("SelectedChatbot", parameters.getSelectedChatbot());
                                    buddyGPTApplication.setparam("STT-TeamGPT", parameters.getStt());
                                    buddyGPTApplication.setparam("TTS-TeamGPT", parameters.getTts());
                                    buddyGPTApplication.setparam("Header", parameters.getHeader());
                                    buddyGPTApplication.setparam("Entete", parameters.getEntete());
                                    buddyGPTApplication.setparam("Email", parameters.getEmail());
                                    if (buddyGPTApplication.getparam("Mail_Destination").equalsIgnoreCase(""))
                                        buddyGPTApplication.setparam("Mail_Destination", parameters.getEmail());
                                    buddyGPTApplication.setparam("Stream_mode", parameters.getStreamMode());
                                    buddyGPTApplication.setparam("Mail_sender", parameters.getMailSender());
                                    buddyGPTApplication.setparam("Smtp_host", parameters.getSmtpHost());
                                    buddyGPTApplication.setparam("Password_mail_sender", parameters.getPasswordMailSender());
                                    buddyGPTApplication.setparam("Smtp_port", String.valueOf(parameters.getSmtpPort()));
                                    buddyGPTApplication.setparam("chatbotModel", parameters.getChatbotModel());
                                    buddyGPTApplication.setparam("Mail_Subject_fr", parameters.getMailSubjectFr());
                                    buddyGPTApplication.setparam("Mail_Subject_en", parameters.getMailSubjectEn());

                                    if (parameters.getEmailSupport() != null && !parameters.getEmailSupport().equalsIgnoreCase(""))
                                        buddyGPTApplication.setparam("email_support", parameters.getEmailSupport());
                                    else
                                        buddyGPTApplication.setparam("email_support", " _ ");

                                    if (parameters.getImeiIdDevice() != null && !parameters.getImeiIdDevice().equalsIgnoreCase(""))
                                        buddyGPTApplication.setparam("IMEI_ID_Device", parameters.getImeiIdDevice());
                                    else
                                        buddyGPTApplication.setparam("IMEI_ID_Device", " _ ");
                                    if (parameters.getIdCompte() != null && !parameters.getIdCompte().equalsIgnoreCase(""))
                                        buddyGPTApplication.setparam("IdCompte", parameters.getIdCompte());
                                    else
                                        buddyGPTApplication.setparam("IdCompte", " _ ");
                                    if (buddyGPTApplication.getparam("STT-TeamGPT").equalsIgnoreCase("local")
                                            && buddyGPTApplication.getparam("STT").equalsIgnoreCase(""))
                                        buddyGPTApplication.setparam("STT", "Android");
                                    if (buddyGPTApplication.getparam("TTS-TeamGPT").equalsIgnoreCase("local")
                                            && buddyGPTApplication.getparam("TTS").equalsIgnoreCase(""))
                                        buddyGPTApplication.setparam("TTS", "ReadSpeaker");
                                }
                            }
                        }

                    }
                } else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                    Log.i(TAG_PARAM, "run: notifyObservers response msg " + con.getResponseMessage());
                    Log.i(TAG_PARAM, "run: notifyObservers INVALID_TEAMGPT_KEY 1");
                    buddyGPTApplication.setparam(TeamGPTKey, gptKey);
                    buddyGPTApplication.resetSharedPreferences();
                    buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "TRUE");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_DEVICE_ID", "FALSE");
                }
                con.disconnect();
            } catch (Exception e) {
                Log.e(TAG_STREAM, "Exception in getParameters: ", e);
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


    public void sendPutRequestStream(String question) {
        isEmotionNeutral = false;
        String baseUrl = buddyGPTApplication.getparam("TeamGPT_url");
        String endpoint = buddyGPTApplication.getparam("TeamGPT_ApiEndpoint_Response");
        String gptKey = buddyGPTApplication.getparam("TeamGPT_Key");
        String imeiDevice = buddyGPTApplication.getparam("TeamGPT_ID_Device");

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
        String formattedTime = sdf.format(new Date(requestStartTime));
        buddyGPTApplication.setQuestionTime(requestStartTime);
        Log.i(TAG_STREAM, "Request sent at: " + formattedTime);

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(baseUrl + endpoint);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("TeamGPT-Key", gptKey);
                connection.setRequestProperty("ID_DEVICE", imeiDevice);
                connection.setDoOutput(true);
                connection.setChunkedStreamingMode(0);

                Gson gson = new Gson();
                String jsonPayload = gson.toJson(payload);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    os.flush();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    buddyGPTApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                    long responseStartTime = System.currentTimeMillis();
                    buddyGPTApplication.setResponseTime(responseStartTime);
                    String formattedTime2 = sdf.format(new Date(responseStartTime));
                    Log.i(TAG_STREAM, "First response received at: " + formattedTime2);
                    long responseTime = buddyGPTApplication.getResponseTime() - buddyGPTApplication.getQuestionTime();
                    Log.i(TAG_STREAM, "Response time: " + responseTime + " ms");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "FALSE");
                    buddyGPTApplication.setparam("ENV_ERROR", "FALSE");
                    handleStreamingResponse(connection.getInputStream());
                } else if (responseCode == 400) {
                    buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "TRUE");
                } else if (responseCode == 500) {
                    buddyGPTApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                    buddyGPTApplication.notifyObservers("Session_ID_ERROR");
                    buddyGPTApplication.setparam("session_id", "");
                } else if (responseCode == 404) {
                    buddyGPTApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                    buddyGPTApplication.notifyObservers("ENV_ERROR");
                    buddyGPTApplication.setparam("session_id", "");
                } else {
                    handleError();
                }
            } catch (Exception e) {
                Log.e(TAG_STREAM, "Exception in sendPutRequestStream: ", e);
                handleError();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void updateMessageHistory(String question) {
        try {
            if (buddyGPTApplication.getparam(historicMessages).equalsIgnoreCase(""))
                buddyGPTApplication.setparam(historicMessages, "[]");
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
        buddyGPTApplication.notifyObservers("MODE_STREAM_SPEAK;SPLIT;" + errorMessage);
    }


    private void pronouncePhrase(String phraseToPronounce) {
        Log.i(TAG_STREAM, "pronouncePhrase: " + phraseToPronounce);
        if (!isReset) {
            if (!buddyGPTApplication.isTimeoutExpired()) {
                Log.i(TAG_STREAM, "TTS : [ " + phraseToPronounce + " ]");
                if (buddyGPTApplication.getparam("switch_visibility").equals("true")) {
                    showPhrase(phraseToPronounce);
                } else {
                    isDisplayFinished = true;
                }
                buddyGPTApplication.notifyObservers("MODE_STREAM_SPEAK;SPLIT;" + phraseToPronounce);
            } else {
                Log.w(TAG_STREAM, "Pause streaming until TTS is ready again [ " + phraseToPronounce + " ]");
                pauseStreaming(phraseToPronounce);
            }
        }
    }

    private void pauseStreaming(String phraseToPronounceWhenResumed) {
        isPaused = true;
        this.phraseToPronounceWhenResumed = phraseToPronounceWhenResumed;
    }

    private void showPhrase(String phrase) {
        isDisplayFinished = false;
        if (isError) currentDisplayedText = "";
        final int totalLength = currentDisplayedText.length() + phrase.length();
        for (int i = 1; i <= phrase.length(); i++) {
            final String phraseToShow = currentDisplayedText + phrase.substring(0, i);
            wordsRunnable = () -> {
                buddyGPTApplication.notifyObservers("MODE_STREAM_TEXT;SPLIT;" + phraseToShow);
                if (phraseToShow.length() == totalLength) {
                    isDisplayFinished = true;
                }
            };
            wordsHandler.postDelayed(wordsRunnable, i);
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
            Log.i(TAG_STREAM, "handleStreamingResponse: !isReset " + !isReset);
            Log.i(TAG_STREAM, "handleStreamingResponse: !isError " + !isError);

            while ((line = reader.readLine()) != null && reader.readLine().equalsIgnoreCase("") && !isReset && !isError) {
                try {
                    Log.w("HOU_DEBUG", "Received line: " + line);

                    processStreamLine(line, formattedContent);

                } catch (JSONException e) {
                    Log.e(TAG_STREAM, "Invalid JSON data: " + line, e);
                }
            }
            updateHistoryWithResponse();
            storeStreamResponse(fileName, formattedContent.toString());

        } catch (Exception e) {
            e.printStackTrace();
            onErrorStreaming("EXCEPTION", null);
        }
    }

    private void processStreamLine(String line, StringBuilder formattedContent) throws JSONException {
        if (line.contains("\"is_finished\": true,")) {
            formattedContent.append(line);
        } else {
            JSONObject jsonObject = new JSONObject(line.replace("data:", "").trim());
            String formattedObject = jsonObject.toString(4);
            formattedContent.append("data: ").append(formattedObject);
            formattedContent.append("\n\n");
        }

        Log.w(TAG_STREAM, "Received line: " + line);
        String jsonData = line.substring("data:".length()).trim();
        JSONObject jsonObject = new JSONObject(jsonData);

        if (isEmptyEmotionAndAnswer(jsonObject)) {
            if (jsonObject.has("is_finished") && jsonObject.getBoolean("is_finished")) {
                isFullResponseReceived = true;
                isSessionIdProcessed = false;
            }
        } else {
            handleEmotion(jsonObject);
            handleSessionId(jsonObject);
            handleAnswer(jsonObject);
        }
    }

    private boolean isEmptyEmotionAndAnswer(JSONObject jsonObject) throws JSONException {
        return jsonObject.has("Emotion") && jsonObject.getString("Emotion").equalsIgnoreCase("")
                && jsonObject.has("Answer") && jsonObject.getString("Answer").equalsIgnoreCase("");
    }

    private void handleEmotion(JSONObject jsonObject) throws JSONException {
        if (buddyGPTApplication.getparam("switch_emotion").equals("true")) {
            if (jsonObject.has("Emotion") && !jsonObject.getString("Emotion").equalsIgnoreCase("")) {
                String emotion = jsonObject.getString("Emotion");
                Log.i(TAG_STREAM, "handleStreamingResponse: emo " + emotion);
                buddyGPTApplication.notifyObservers("Emotion_Change;SPLIT;" + emotion);
            } else {
                Log.i(TAG_STREAM, "handleStreamingResponse: emo null");
            }
        } else {
            if (!isEmotionNeutral) {
                isEmotionNeutral = true;
                buddyGPTApplication.notifyObservers("Emotion_Change;SPLIT;BuddyFace_Neutral");
            }
        }
    }

    private void handleSessionId(JSONObject jsonObject) throws JSONException {
        if (!isSessionIdProcessed) {
            if (jsonObject.has("session_id")) {
                String sessionId = jsonObject.getString("session_id");
                Log.i(TAG_STREAM, "handleStreamingResponse: session " + sessionId);

                if (!buddyGPTApplication.getparam("session_id").equalsIgnoreCase(sessionId)) {
                    Handler mainHandler2 = new Handler(Looper.getMainLooper());
                    mainHandler2.post(() -> buddyGPTApplication.notifyObservers("Session_ID_Changed"));
                    buddyGPTApplication.setparam("session_id", sessionId);
                    addSessionToHistory();
                }
            }
            isSessionIdProcessed = true;
        }
    }

    private void addSessionToHistory() {
        try {
            String jsonArrayString = buddyGPTApplication.getparam(historicMessages);
            existingHistoryArray = new JSONArray(jsonArrayString);
            JSONObject newSessionObject = new JSONObject();
            newSessionObject.put("Session", buddyGPTApplication.getparam("SelectedChatbot") + " - " + buddyGPTApplication.getModel());
            existingHistoryArray.put(newSessionObject);
            buddyGPTApplication.setparam(historicMessages, existingHistoryArray.toString());
        } catch (Exception e) {
            Log.e(TAG_STREAM, "Error adding session to history", e);
        }
    }

    private void handleAnswer(JSONObject jsonObject) throws JSONException {
        if (jsonObject.has("Answer")) {
            String resp = jsonObject.getString("Answer");
            if (!resp.isEmpty()) {
                Log.i(TAG_STREAM, "handleStreamingResponse: if1 " + resp);
                answer += " " + resp;
                phrase = resp;
                onNewPhrase();
                if (jsonObject.getBoolean("is_finished")) {
                    isFullResponseReceived = true;
                    isSessionIdProcessed = false;
                }
            } else {
                if (jsonObject.getBoolean("is_finished")) {
                    isFullResponseReceived = true;
                    isSessionIdProcessed = false;
                }
            }
        }
    }

    private void updateHistoryWithResponse() {
        try {
            String jsonArrayString = buddyGPTApplication.getparam(historicMessages);
            existingHistoryArray = new JSONArray(jsonArrayString);
            JSONObject newRespObject = new JSONObject();
            long responseTime = buddyGPTApplication.getResponseTime() - buddyGPTApplication.getQuestionTime();
            DecimalFormat df = new DecimalFormat("#,###");
            String formattedTime = df.format(responseTime);
            newRespObject.put("Response", answer + ";SPLIT;" + formattedTime + " ms");
            existingHistoryArray.put(newRespObject);
            buddyGPTApplication.setparam(historicMessages, existingHistoryArray.toString());
        } catch (Exception e) {
            Log.e(TAG_STREAM, "Error updating history with response", e);
        }
    }

    private void onNewPhrase() {
        Log.w(TAG_STREAM, "Phrase: " + phrase);
        phrasesQueue.add(phrase);
    }

    public void onTTSEnd() {
        Log.i(TAG_STREAM, "TTS END");
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
        } catch (Exception e) {
            Log.e(TAG_STREAM, "BuddySDK Exception : " + e);
        }
        isReadyToSpeak = true;
    }

    public void resumeStreaming() {
        if (isPaused) {
            isPaused = false;
            pronouncePhrase(phraseToPronounceWhenResumed);
        }
    }


    private void processPhrasesWithDelay() {
        Log.i(TAG_STREAM, "processPhrasesWithDelay: phrasesQueue.isEmpty()=" + phrasesQueue.isEmpty());
        Log.i(TAG_STREAM, "processPhrasesWithDelay: isDisplayFinished= " + isDisplayFinished);
        if (!phrasesQueue.isEmpty() && isDisplayFinished) {
            Log.i(TAG_STREAM, "processPhrasesWithDelay: if");
            if (isReadyToSpeak) {
                Log.i(TAG_STREAM, "processPhrasesWithDelay: isReadyToSpeak");
                isReadyToSpeak = false;
                String phraseToPronounce = phrasesQueue.poll();
                if (phraseToPronounce != null) {
                    if (buddyGPTApplication.getparam("Detection_de_langue").equals("true") &&
                            buddyGPTApplication.nombreDeMotsCheck(phraseToPronounce)) {
                        LanguageIdentifier languageIdentifier = LanguageIdentification.getClient();
                        languageIdentifier.identifyPossibleLanguages(phraseToPronounce)
                                .addOnSuccessListener(
                                        identifiedLanguages -> {
                                            if (identifiedLanguages.isEmpty()) {
                                                Log.e(TAG_STREAM, "languageIdentifier : Can't identify language of : " + phraseToPronounce);
                                                pronouncePhrase(phraseToPronounce);
                                            } else {
                                                // Utiliser la première langue identifiée
                                                IdentifiedLanguage language = identifiedLanguages.get(0);
                                                String languageCode = language.getLanguageTag();
                                                float confidence = language.getConfidence();
                                                Log.i("MRA_idetifyLanguage", "Language of : [ " + phraseToPronounce + " ] is : " + languageCode + ", Confidence: " + confidence);
                                                if (buddyGPTApplication.getParamFromFile("Detection_confidence_rate", "BuddyGPT.properties") != null &&
                                                        !buddyGPTApplication.getParamFromFile("Detection_confidence_rate", "BuddyGPT.properties").trim().isEmpty() &&
                                                        !buddyGPTApplication.getParamFromFile("Detection_confidence_rate", "BuddyGPT.properties").trim().equals("0")) {
                                                    if (Integer.parseInt(buddyGPTApplication.getParamFromFile("Detection_confidence_rate", "BuddyGPT.properties")) <= (confidence * 100)) {
                                                        buddyGPTApplication.setLanguageDetected(languageCode.trim());
                                                        pronouncePhrase(phraseToPronounce);
                                                    } else {
                                                        pronouncePhrase(phraseToPronounce);
                                                    }
                                                } else {
                                                    buddyGPTApplication.setLanguageDetected(languageCode.trim());
                                                    pronouncePhrase(phraseToPronounce);
                                                }
                                            }
                                        })
                                .addOnFailureListener(e -> pronouncePhrase(phraseToPronounce));
                    } else {
                        pronouncePhrase(phraseToPronounce);
                    }
                }
            }
        } else {
            Log.i(TAG_STREAM, "processPhrasesWithDelay: else");
            Log.i(TAG_STREAM, "processPhrasesWithDelay: isReadyToSpeak " + isReadyToSpeak);
            Log.i(TAG_STREAM, "processPhrasesWithDelay: isFullResponseReceived :" + isFullResponseReceived);
            if (isDisplayFinished && ((isFullResponseReceived && isReadyToSpeak) || (isError && isReadyToSpeak))) {
                onFinishStreaming();
                buddyGPTApplication.notifyObservers("TTS_success");
                reset();
                return;
            }
        }
        phrasesRunnable = this::processPhrasesWithDelay;
        phrasesHandler.postDelayed(phrasesRunnable, 50);
    }

    private void onFinishStreaming() {
        Log.i(TAG_STREAM, "------------------END-------------------");
    }

    private void storeStreamResponse(String fileName, String formattedContent) {
        Log.w(TAG_STREAM, "storeStreamResponse()");
        try {
            File file = new File(Environment.getExternalStorageDirectory(), "BuddyGPT/" + fileName + ".txt");
            if (file.exists() && file.isFile()) {
                boolean result = file.delete();
                Log.i(TAG_STREAM, "storeStreamResponse() : file deleted : " + result);
            }
            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.write(formattedContent);
            }
            Log.i(TAG_STREAM, "storeStreamResponse() : new file added");
        } catch (Exception e) {
            Log.e(TAG_STREAM, "storeStreamResponse() : " + e);
        }
    }

    private void onStartStreaming() {
        if (!isReset) {
            Log.i(TAG_STREAM, "------------------START-------------------");
            processPhrasesWithDelay();
        }
    }

    private void onErrorStreaming(String error, Response<ResponseBody> response) {
        Log.e(TAG_STREAM, "------------------ERROR-------------------");

        if (!isReset) {
            clearHandlersAndQueues();
            buddyGPTApplication.stopTTS();
            resetLabialExpression();

            SystemClock.sleep(1000);

            isReadyToSpeak = false;
            isError = true;

            if (error.equals("RESPONSE_NOT_SUCCESSFUL")) {
                handleResponseNotSuccessful(response);
            } else if (error.equals("FAILURE")) {
                errorMsg = getLocalizedErrorMessage("chatBotNoFound");
            } else {
                errorMsg = getLocalizedErrorMessage("chatBot_ERROR");
            }

            buddyGPTApplication.setMessageError(true);
            setTiredFacialExpressionIfNeeded();
            processPhrasesWithDelay();
            pronouncePhrase(errorMsg);
        }
    }

    private void clearHandlersAndQueues() {
        if (phrasesRunnable != null) phrasesHandler.removeCallbacks(phrasesRunnable);
        phrasesHandler.removeCallbacksAndMessages(null);
        phrasesQueue.clear();

        if (wordsRunnable != null) wordsHandler.removeCallbacks(wordsRunnable);
        wordsHandler.removeCallbacksAndMessages(null);
    }

    private void resetLabialExpression() {
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
        } catch (Exception e) {
            Log.e(TAG_STREAM, "BuddySDK Exception  " + e);
        }
    }

    private void handleResponseNotSuccessful(Response<ResponseBody> response) {
        try {
            if (response != null && response.errorBody() != null) {
                logErrorToFile(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String lang = buddyGPTApplication.getLangue().getNom();
        switch (lang) {
            case "Anglais":
                errorMsg = buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_en", "BuddyGPT.properties");
                break;
            case "Français":
                errorMsg = buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_fr", "BuddyGPT.properties");
                break;
            case "Espagnol":
                errorMsg = buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_es", "BuddyGPT.properties");
                break;
            case "Allemand":
                errorMsg = buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_de", "BuddyGPT.properties");
                break;
            default:
                buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                        .translate(buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_en", "BuddyGPT.properties"))
                        .addOnSuccessListener(translatedText -> errorMsg = translatedText)
                        .addOnFailureListener(e -> errorMsg = buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_en", "BuddyGPT.properties"));
                break;
        }
    }

    private void logErrorToFile(Response<ResponseBody> response) {
        try {
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
            reformErrorJson.addProperty("message", message);
            reformErrorJson.addProperty("type", type);
            reformErrorJson.addProperty("param", param);
            reformErrorJson.addProperty("code", code);
            errorCode.add("ERROR Body", reformErrorJson);
            errorLOG.add("OpenAIERROR", errorCode);
            String fileName = "ERROR-LOG";
            File file1 = new File(Environment.getExternalStorageDirectory(), "BuddyGPT/" + fileName + ".json");
            if (file1.exists() && file1.isFile()) file1.delete();
            try (FileWriter fileWriter = new FileWriter(file1)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                String jsonStringF = gson.toJson(errorLOG);
                fileWriter.write(jsonStringF);
            }

            String errorTXT = new Date() + ", OpenAIERROR,ERROR CODE= " + response.code()
                    + ", ERROR Body{ message= " + message + ", type= " + type + ", param= " + param + ", code= " + code + "}"
                    + System.getProperty("line.separator");
            File file2 = new File(Environment.getExternalStorageDirectory(), "BuddyGPT/ERROR-History.txt");
            try (FileWriter fileWriter2 = new FileWriter(file2, true)) {
                fileWriter2.write(errorTXT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getLocalizedErrorMessage(String key) {
        String lang = buddyGPTApplication.getLangue().getNom();
        switch (key) {
            case "chatBotNoFound":
                switch (lang) {
                    case "Anglais":
                        return buddyGPTApplication.getString(R.string.chatBotNoFound_en);
                    case "Français":
                        return buddyGPTApplication.getString(R.string.chatBotNoFound_fr);
                    case "Espagnol":
                        return buddyGPTApplication.getString(R.string.chatBotNoFound_es);
                    case "Allemand":
                        return buddyGPTApplication.getString(R.string.chatBotNoFound_de);
                    default:
                        buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                                .translate(buddyGPTApplication.getString(R.string.chatBotNoFound_en))
                                .addOnSuccessListener(translatedText -> errorMsg = translatedText)
                                .addOnFailureListener(e -> errorMsg = buddyGPTApplication.getString(R.string.chatBotNoFound_en));
                        return buddyGPTApplication.getString(R.string.chatBotNoFound_en);
                }
            case "chatBot_ERROR":
            default:
                switch (lang) {
                    case "Anglais":
                        return buddyGPTApplication.getString(R.string.chatBot_ERROR_en);
                    case "Français":
                        return buddyGPTApplication.getString(R.string.chatBot_ERROR_fr);
                    case "Espagnol":
                        return buddyGPTApplication.getString(R.string.chatBot_ERROR_es);
                    case "Allemand":
                        return buddyGPTApplication.getString(R.string.chatBot_ERROR_de);
                    default:
                        buddyGPTApplication.getEnglishLanguageSelectedTranslator()
                                .translate(buddyGPTApplication.getString(R.string.chatBot_ERROR_en))
                                .addOnSuccessListener(translatedText -> errorMsg = translatedText)
                                .addOnFailureListener(e -> errorMsg = buddyGPTApplication.getString(R.string.chatBot_ERROR_en));
                        return buddyGPTApplication.getString(R.string.chatBot_ERROR_en);
                }
        }
    }

    private void setTiredFacialExpressionIfNeeded() {
        if (!buddyGPTApplication.isOpenaialreadySwitchEmotion()) {
            try {
                BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
            } catch (Exception e) {
                Log.e(TAG_STREAM, "BuddySDK Exception  " + e);
            }
        }
    }

    public void reset() {
        Log.i(TAG_STREAM, "------------------reset-------------------");
        isReset = true;
        //reset phrasesQueue:
        if (phrasesRunnable != null) phrasesHandler.removeCallbacks(phrasesRunnable);
        phrasesHandler.removeCallbacksAndMessages(null);
        phrasesQueue.clear();
        isReadyToSpeak = true;
        //reset wordsQueue:
        if (wordsRunnable != null) wordsHandler.removeCallbacks(wordsRunnable);
        wordsHandler.removeCallbacksAndMessages(null);
        buddyGPTApplication.setResponseFromTeamGPT(null);
    }


}
