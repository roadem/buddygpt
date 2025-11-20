package com.robotique.aevaweb.buddygpt.chatbotresponse;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;

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
import com.robotique.aevaweb.buddygpt.utilis.ResponseCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
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
    private boolean hasSentAudioTextInput = false;
    private boolean hasSentAudioResponse = false;
    private boolean isResponseTimeSaved = false;
    private JSONArray existingHistoryArray;
    private SimpleDateFormat sdf;

    // New structure to pair text and audio chunks
    private static class StreamItem {
        String text;
        final Queue<String> audioChunks = new LinkedList<>();
        boolean audioReady = false; // true when at least one audio chunk has been attached

        StreamItem(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return "StreamItem{" +
                    "text='" + text + '\'' +
                    ", audioChunksSize=" + audioChunks.size() +
                    ", audioReady=" + audioReady +
                    '}';
        }
    }

    private final Queue<StreamItem> streamQueue = new LinkedList<>();
    private StreamItem currentPlayingItem = null;
    private boolean isPlayingAudio = false;

    public ResponseFromTeamGPT(BuddyGPTApplication context) {
        this.buddyGPTApplication = context;
        chatBotServerNoResponceFr = buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_fr",
                "BuddyGPT.properties");
        chatBotServerNoResponceEn = buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_en",
                "BuddyGPT.properties");
        chatBotServerNoResponceEs = buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_es",
                "BuddyGPT.properties");
        chatBotServerNoResponceDe = buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_de",
                "BuddyGPT.properties");

    }

    // --- Méthode de Récupération des Paramètres ---
    public void getParameters(ResponseCallback responseCallback) {
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
                    responseCallback.onSuccess();
                    buddyGPTApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "FALSE");
                    buddyGPTApplication.setparam("ENV_ERROR", "FALSE");
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
                                buddyGPTApplication.setparam("Password_mail_sender",
                                        parameters.getPasswordMailSender());
                                buddyGPTApplication.setparam("Smtp_port", String.valueOf(parameters.getSmtpPort()));
                                buddyGPTApplication.setparam("chatbotModel", parameters.getChatbotModel());
                                buddyGPTApplication.setparam("Mail_Subject_fr", parameters.getMailSubjectFr());
                                buddyGPTApplication.setparam("Mail_Subject_en", parameters.getMailSubjectEn());

                                if (parameters.getEmailSupport() != null
                                        && !parameters.getEmailSupport().equalsIgnoreCase(""))
                                    buddyGPTApplication.setparam("email_support", parameters.getEmailSupport());
                                else
                                    buddyGPTApplication.setparam("email_support", " _ ");

                                if (parameters.getImeiIdDevice() != null
                                        && !parameters.getImeiIdDevice().equalsIgnoreCase(""))
                                    buddyGPTApplication.setparam("IMEI_ID_Device", parameters.getImeiIdDevice());
                                else
                                    buddyGPTApplication.setparam("IMEI_ID_Device", " _ ");
                                if (parameters.getIdCompte() != null && !parameters.getIdCompte().equalsIgnoreCase(""))
                                    buddyGPTApplication.setparam("IdCompte", parameters.getIdCompte());
                                else
                                    buddyGPTApplication.setparam("IdCompte", " _ ");
                                if (buddyGPTApplication.getparam("STT-TeamGPT").equalsIgnoreCase("local"))
                                    buddyGPTApplication.setparam("STT", "Android");
                                else
                                    buddyGPTApplication.setparam("STT", parameters.getStt());

                                if (buddyGPTApplication.getparam("STT-TeamGPT").equalsIgnoreCase("local")
                                        && buddyGPTApplication.getParamFromFile("Change_STT", "BuddyGPT.properties")
                                        .equalsIgnoreCase("no")) {
                                    buddyGPTApplication.setparam("STT", buddyGPTApplication.getparam("STT_chosen"));
                                    Log.i("USED_STT", " USED_STT : " + buddyGPTApplication.getparam("STT"));
                                }
                                if (buddyGPTApplication.getparam("TTS-TeamGPT").equalsIgnoreCase("local"))
                                    buddyGPTApplication.setparam("TTS", "ReadSpeaker");
                                else
                                    buddyGPTApplication.setparam("TTS", parameters.getTts());

                            }
                        }

                    }
                } else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                    responseCallback.onSuccess();
                    Log.i(TAG_PARAM, "run: notifyObservers response msg " + con.getResponseMessage());
                    Log.i(TAG_PARAM, "run: notifyObservers INVALID_TEAMGPT_KEY 1");
                    buddyGPTApplication.setparam(TeamGPTKey, gptKey);
                    buddyGPTApplication.resetSharedPreferences();
                    buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "TRUE");
                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    responseCallback.onSuccess();
                    buddyGPTApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                    buddyGPTApplication.notifyObservers("ENV_ERROR");
                    buddyGPTApplication.resetSharedPreferences();
                    buddyGPTApplication.setparam("session_id", "");
                    buddyGPTApplication.setparam("ENV_ERROR", "TRUE");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "FALSE");
                } else {
                    responseCallback.onFailure();
                    Log.e(TAG_PARAM, "Unexpected response code: " + responseCode);
                    buddyGPTApplication.setparam("session_id", "");
                    buddyGPTApplication.setparam("ENV_ERROR", "FALSE");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "FALSE");
                }
                con.disconnect();
            } catch (Exception e) {
                responseCallback.onFailure();
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

    public void sendPutRequestStream(String question, String audioData) {
        Log.i("TAG", "sendPutRequestStream: start ");
        isEmotionNeutral = false;
        String baseUrl = buddyGPTApplication.getparam("TeamGPT_url");
        String endpoint = buddyGPTApplication.getparam("TeamGPT_ApiEndpoint_Response");
        String gptKey = buddyGPTApplication.getparam("TeamGPT_Key");
        String imeiDevice = buddyGPTApplication.getparam("TeamGPT_ID_Device");

        Request payload = new Request();
        payload.setImeiIdDevice(imeiDevice);
        payload.setEmotion(buddyGPTApplication.getparam("switch_emotion").equals("true"));
        payload.setCommandes(false);
        payload.setLangue(buddyGPTApplication.getLangue().getLanguageCode().split("-")[0]);
        if (!buddyGPTApplication.getparam("session_id").isEmpty()) {
            payload.setSessionId(buddyGPTApplication.getparam("session_id"));
        } else
            payload.setSessionId("");

        // Add text input only if provided
        if (question != null && !question.trim().isEmpty()) {
            payload.setTextInput(question);
            updateMessageHistory(question);
        }

        // Add audio if provided
        if (audioData != null && !audioData.trim().isEmpty()) {

            payload.setAudioInput(audioData);
        }

        saveRequestToFile(payload);

        long requestStartTime = System.currentTimeMillis();
        sdf = new SimpleDateFormat("HH:mm:ss:SSS");
        if (question != null) {
            buddyGPTApplication.setQuestionTime(requestStartTime);
            Log.i(TAG_STREAM, "Request sent at: " + sdf.format(new Date(requestStartTime)));
        }

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

                Log.i("TAG", "sendPutRequestStream responseCode : " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    if (question != null) {
                        long responseStartTime = System.currentTimeMillis();
                        buddyGPTApplication.setResponseTime(responseStartTime);
                        Log.i(TAG_STREAM, "First response received at: " + sdf.format(new Date(responseStartTime)));
                        long responseTime = buddyGPTApplication.getResponseTime()
                                - buddyGPTApplication.getQuestionTime();
                        Log.i(TAG_STREAM, "Response time: " + responseTime + " ms");
                    }

                    buddyGPTApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
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
                    buddyGPTApplication.setparam("ENV_ERROR", "TRUE");
                    buddyGPTApplication.setparam("session_id", "");
                } else {
                    handleError();
                }

            } catch (Exception e) {
                Log.e(TAG_STREAM, "Exception in sendPutRequestStream: ", e);
                handleError();
            } finally {
                if (connection != null)
                    connection.disconnect();
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
            if (file.exists() && file.isFile())
                file.delete();
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
                // Ne pas notifier MODE_STREAM_SPEAK si on a une entrée audio (transcription)
                // ou une sortie audio (base64) pour éviter doublons / conflits TTS
                if (!hasSentAudioResponse && !hasSentAudioTextInput) {
                    buddyGPTApplication.notifyObservers("MODE_STREAM_SPEAK;SPLIT;" + phraseToPronounce);
                } else {
                    Log.i(TAG_STREAM, "pronouncePhrase: MODE_STREAM_SPEAK skipped due to audio input/output");
                }
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
        Log.i("TAG", "showPhrase: " + phrase);
        isDisplayFinished = false;
        if (isError)
            currentDisplayedText = "";
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

    // --- Gestion du Streaming ---
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

            // Correction : ne pas lire deux fois la ligne, et ignorer lignes vides
            while ((line = reader.readLine()) != null && !isReset && !isError) {
                if (line.trim().isEmpty())
                    continue;
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
        // Les données arrivent sous la forme "data: {...}"
        String data = line.trim();
        if (data.startsWith("data:")) {
            data = data.substring("data:".length()).trim();
        }
        if (data.isEmpty())
            return;

        // Ajout au fichier de debug
        try {
            JSONObject jsonObject = new JSONObject(data);
            String formattedObject = jsonObject.toString(4);
            formattedContent.append("data: ").append(formattedObject).append("\n\n");
            Log.w(TAG_STREAM, "Received JSON object: " + formattedObject);

            // Gérer Text_input (transcription STT)
            if (jsonObject.has("Text_input") && !jsonObject.getString("Text_input").trim().isEmpty()) {
                handleTextInput(jsonObject);
            }

            // Gérer les flags de fin de stream
            if (jsonObject.has("is_finished") && jsonObject.getBoolean("is_finished")
                    && jsonObject.has("STT_is_finished") && jsonObject.getBoolean("STT_is_finished")
                    && jsonObject.has("Chatbot_is_finished") && jsonObject.getBoolean("Chatbot_is_finished")
                    && jsonObject.has("TTS_is_finished") && jsonObject.getBoolean("TTS_is_finished")) {

                isFullResponseReceived = true;
                isSessionIdProcessed = false;
                 //on laisse processPhrasesWithDelay détecter la fin si les files sont vides
                 //(sauf si on est en audio playback)
                 if (currentPlayingItem == null && !isPlayingAudio) {

                 // Forcer la vérification de fin si tout est fini
                 if (streamQueue.isEmpty() && phrasesQueue.isEmpty()) {
                 processPhrasesWithDelay();
                 }
                 }
            } else {
                handleEmotion(jsonObject);
                handleSessionId(jsonObject);
                handleAnswer(jsonObject);
                handleAudioResponse(jsonObject); // gère audio base64 s'il y en a
            }

        } catch (JSONException e) {
            Log.e(TAG_STREAM, "processStreamLine JSON parse error", e);
            throw e;
        }
    }

    // Méthode pour gérer le Text_input provenant de l'audio
    private void handleTextInput(JSONObject jsonObject) throws JSONException {
        if (!hasSentAudioTextInput && jsonObject.has("Text_input")) {
            String textInput = jsonObject.getString("Text_input");
            if (textInput != null && !textInput.trim().isEmpty()) {
                hasSentAudioTextInput = true;
                long requestStartTime = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss:SSS");
                buddyGPTApplication.setQuestionTime(requestStartTime);
                Log.i(TAG_STREAM, "Request sent at: " + sdf.format(new Date(requestStartTime)));
                // Mettre à jour l'historique des messages avec le texte transcrit
                try {
                    updateMessageHistory(textInput);
                } catch (Exception e) {
                    Log.e(TAG_STREAM, "updateMessageHistory failed for audio text", e);
                }
                // Afficher immédiatement le texte d'entrée (transcription)
                buddyGPTApplication.notifyObservers("AUDIO_TEXT_INPUT;SPLIT;" + textInput);
                Log.i(TAG_STREAM, "handleTextInput: forwarded audio text -> " + textInput);
            }
        }
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
                Log.i(TAG_STREAM, "handleStreamingResponse: session " + buddyGPTApplication.getparam("session_id"));

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
            newSessionObject.put("Session", buddyGPTApplication.getparam("SelectedChatbot") + " - "
                    + buddyGPTApplication.getparam("chatbotModel"));
            existingHistoryArray.put(newSessionObject);
            buddyGPTApplication.setparam(historicMessages, existingHistoryArray.toString());
        } catch (Exception e) {
            Log.e(TAG_STREAM, "Error adding session to history", e);
        }
    }

    // Gère la réception de l'Answer (texte de la réponse)
    private void handleAnswer(JSONObject jsonObject) throws JSONException {
        if (jsonObject.has("Answer") && !jsonObject.getString("Answer").equalsIgnoreCase("")) {
            String resp = jsonObject.getString("Answer");
            if (!resp.isEmpty()) {
                Log.i(TAG_STREAM, "handleAnswer: received -> " + resp);
                answer += " " + resp;

                // Déterminer si l'entrée était audio (transcription reçue) ou texte.
                if (hasSentAudioTextInput) {
                    if (isResponseTimeSaved == false) {
                        long responseStartTime = System.currentTimeMillis();
                        buddyGPTApplication.setResponseTime(responseStartTime);
                        Log.i(TAG_STREAM, "First response received at: " + sdf.format(new Date(responseStartTime)));
                        long responseTime = buddyGPTApplication.getResponseTime()
                                - buddyGPTApplication.getQuestionTime();
                        Log.i(TAG_STREAM, "Response time: " + responseTime + " ms");
                        isResponseTimeSaved = true;
                    }
                    // Cas AUDIO INPUT -> AUDIO OUTPUT (serveur)
                    Log.i(TAG_STREAM, "handleAnswer: Routing to StreamQueue (Server Audio expected)");
                    StreamItem item = new StreamItem(resp);
                    synchronized (streamQueue) {
                        streamQueue.add(item);
                        Log.i(TAG_STREAM, "StreamItem ajouté. Taille actuelle de streamQueue : " + streamQueue.size());
                    }

                    Log.i(TAG_STREAM, "handleAnswer: currentPlayingItem : "+currentPlayingItem+ " isPlayingAudio : "+isPlayingAudio);
                    // Tenter de démarrer la lecture si c'est le premier item (
                    // démarrera seulement si un chunk audio arrive dans handleAudioResponse
                    if (currentPlayingItem == null && !isPlayingAudio) {
                        Log.i("TAG",
                                "handleAnswer:  if (currentPlayingItem == null && !isPlayingAudio) " + isPlayingAudio);
                        startNextReadyItemIfAny();
                        }

                } else {
                    // Cas TEXT INPUT -> TEXT OUTPUT (TTS local)
                    Log.i(TAG_STREAM, "handleAnswer: Routing to PhrasesQueue (Local TTS)");
                    phrase = resp;
                    onNewPhrase(); // Ajoute à phrasesQueue pour TTS local
                }
            }
        }
    }

    // Gère la réception des chunks audio Base64
    private void handleAudioResponse(JSONObject jsonObject) {
        Log.i(TAG_STREAM, "handleAudioResponse: start ");
        if (jsonObject.has("Audio_reponse")) {
            String base64Audio = jsonObject.optString("Audio_reponse", "");

            if (base64Audio != null && !base64Audio.isEmpty()) {
                Log.i(TAG_STREAM, "Audio chunk reçu (len=" + base64Audio.length() + ")");
                // Mettre ce flag à true pour confirmer que le serveur a répondu en audio
                hasSentAudioResponse = true;
                StreamItem target = null;
                synchronized (streamQueue) {
                    if (currentPlayingItem != null && isPlayingAudio) {
                        // Si on est déjà en train de jouer, ajouter au currentPlayingItem
                        target = currentPlayingItem;
                    } else {
                        // Sinon, ajouter au premier item en attente dans la queue
                        target = streamQueue.peek();
                    }

                    if (target == null) {
                        // Cas rare : audio sans texte précédent. Créer un placeholder si nécessaire.
                        target = new StreamItem("");
                        streamQueue.add(target);
                    }

                    target.audioChunks.add(base64Audio);
                    target.audioReady = true; // Marquer qu'on a reçu au moins un chunk
                    Log.i(TAG_STREAM, "handleAudioResponse: " + target.toString());
                }

                buddyGPTApplication.notifyObservers("AUDIO_BASE64;SPLIT;");

                // Si rien n'est en lecture, commencer le playback dès que le premier chunk
                // arrive
                if (!isPlayingAudio && currentPlayingItem == null) {
                    Log.i(TAG_STREAM, "handleAudioResponse: if (!isPlayingAudio && currentPlayingItem == null)");
                    startNextReadyItemIfAny();
                }
            }
        }
    }

    // Lit le chunk suivant pour l'item courant
    private void playNextChunkForCurrentItem() {
        Log.i("TAG", "playNextChunkForCurrentItem: start");
        if (currentPlayingItem == null) {
            isPlayingAudio = false;
            hasSentAudioResponse = false;
            // Si le stream complet est terminé, on déclenche la fin
            if (isFullResponseReceived && streamQueue.isEmpty() && phrasesQueue.isEmpty()) {
                processPhrasesWithDelay();
            }
            return;
        }

        String nextChunk = currentPlayingItem.audioChunks.poll();
        if (nextChunk == null) {
            // Fini pour l'item courant
            Log.w(TAG_STREAM, "--- FIN D'ITEM DETECTEE. Tentative de relance de la queue ---");
            Log.i(TAG_STREAM, "Finished playing item: " + currentPlayingItem.text);

            // --- L'action CRUCIALE de nettoyage ---
            currentPlayingItem = null;
            isPlayingAudio = false; // Important pour permettre au startNextReadyItemIfAny de fonctionner
            // ----------------------------------------

            buddyGPTApplication.notifyObservers("AUDIO_PLAYBACK_FINISHED;SPLIT;");
            // Tenter de démarrer l'item suivant
            startNextReadyItemIfAny(); // Cet appel est correct ici
            return;
        }

        // Écrire et jouer ce chunk sur un thread séparé
        new Thread(() -> {
            File outFile = null;
            try {
                // Décodage Base64
                byte[] audioBytes = Base64.decode(nextChunk, Base64.DEFAULT);
                Log.i(TAG_STREAM, "playNextChunkForCurrentItem: ");
                // Création du fichier temporaire dans le cache de l'application
                outFile = new File(
                        Environment.getExternalStorageDirectory(),
                        "chunk_" + System.currentTimeMillis() + ".wav");

                // Écriture du fichier WAV
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(audioBytes);
                }
                Log.i(TAG_STREAM, "playNextChunkForCurrentItem: Wrote " + audioBytes.length + " bytes to "
                        + outFile.getAbsolutePath());

                final File fileToPlay = outFile;

                // Lecture sur le thread principal
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    MediaPlayer mp = null;
                    try {
                        mp = new MediaPlayer();
                        mp.setDataSource(fileToPlay.getAbsolutePath());
                        mp.setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build());

                        final MediaPlayer finalMp = mp;

                        mp.setOnPreparedListener(MediaPlayer::start);

                        mp.setOnCompletionListener(player -> {
                            player.release();
                            // Suppression du fichier temporaire
                            // if (fileToPlay.exists()) {
                            // boolean deleted = fileToPlay.delete();
                            // Log.i(TAG_STREAM, "Chunk file deleted: " + deleted);
                            // }
                            // Lire le chunk suivant du même item
                            playNextChunkForCurrentItem();
                        });
                        mp.prepareAsync();
                    } catch (Exception e) {
                        Log.e(TAG_STREAM, "Error playing chunk for current item", e);
                        if (mp != null)
                            mp.release();
                        if (fileToPlay.exists())
                            fileToPlay.delete();
                        // Continuer avec le chunk suivant en cas d'erreur de lecture
                        playNextChunkForCurrentItem();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG_STREAM, "Erreur chunk audio (item): " + e.getMessage(), e);
                if (outFile != null && outFile.exists())
                    outFile.delete();
                // Continuer avec le chunk suivant en cas d'erreur de décodage/écriture
                playNextChunkForCurrentItem();
            }
        }).start();
    }

    // Démarre la lecture pour le prochain StreamItem prêt (avec audio attaché)
    private void startNextReadyItemIfAny() {
        Log.i(TAG_STREAM, "startNextReadyItemIfAny: start. Current state: currentPlayingItem=" + currentPlayingItem + ", isPlayingAudio=" + isPlayingAudio);

        // Si la lecture est déjà active, on ne démarre rien de nouveau
        if (currentPlayingItem != null || isPlayingAudio) {
            return;
        }

        synchronized (streamQueue) {
            // 1. Regarder le prochain élément sans le retirer (Peek)
            StreamItem si = streamQueue.peek();

            if (si != null && si.audioReady) {
                // 2. L'élément est prêt : le retirer de la queue (Poll)
                StreamItem itemToPlay = streamQueue.poll();

                // 3. Lancer la lecture
                startPlaybackForItem(itemToPlay);

                Log.i(TAG_STREAM, "startNextReadyItemIfAny: Playback started for: " + itemToPlay.text);
                Log.i(TAG_STREAM, "startNextReadyItemIfAny: New queue size: " + streamQueue.size());

            } else if (si != null) {
                Log.i(TAG_STREAM, "startNextReadyItemIfAny: Item found, but audio not yet ready. Waiting...");
            } else {
                Log.i(TAG_STREAM, "startNextReadyItemIfAny: Queue is empty.");
            }
        }
    }

    // Démarre l'affichage du texte et la lecture de l'audio pour un StreamItem
    private void startPlaybackForItem(StreamItem item) {
        currentPlayingItem = item;
        isPlayingAudio = true;
        hasSentAudioResponse = true;

        Log.i(TAG_STREAM, "startPlaybackForItem: " + item.text + " (chunks=" + item.audioChunks.size() + ")");

        // Afficher le texte (affichge progressif via showPhrase)
        if (item.text != null && !item.text.isEmpty()) {
            // on appelle showPhrase pour l'affichage progressif
            if (buddyGPTApplication.getparam("switch_visibility").equals("true")) {
                Log.i("TAG", "startPlaybackForItem: calling showPhrase for item text");
                showPhrase(item.text);
            } else {
                Log.i("TAG", "startPlaybackForItem: immediate display for item text");

            }
        }

        // Commencer la lecture du premier chunk
        playNextChunkForCurrentItem();
    }

    private void updateHistoryWithResponse() {
        try {
            String jsonArrayString = buddyGPTApplication.getparam(historicMessages);
            existingHistoryArray = new JSONArray(jsonArrayString);
            JSONObject newRespObject = new JSONObject();
            long responseTime = buddyGPTApplication.getResponseTime() - buddyGPTApplication.getQuestionTime();
            DecimalFormat df = new DecimalFormat("#,###");
            String formattedTime = df.format(responseTime);
            if (!buddyGPTApplication.isTimeoutExpired() && !answer.isEmpty()) {
                newRespObject.put("Response", answer + ";SPLIT;" + formattedTime + " ms");
                existingHistoryArray.put(newRespObject);
            }

            buddyGPTApplication.setparam(historicMessages, existingHistoryArray.toString());
        } catch (Exception e) {
            Log.e(TAG_STREAM, "Error updating history with response", e);
        }
    }

    private void onNewPhrase() {
        Log.w(TAG_STREAM, "Phrase (TTS local): " + phrase);
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

    // --- Processus de Prononciation des Phrases (TTS Local) ---
    private void processPhrasesWithDelay() {
        Log.i(TAG_STREAM, "processPhrasesWithDelay: phrasesQueue.isEmpty()=" + phrasesQueue.isEmpty());
        Log.i(TAG_STREAM, "processPhrasesWithDelay: isDisplayFinished= " + isDisplayFinished);
        // Si la queue TTS n'est pas vide ET l'affichage est fini ET AUCUN audio serveur
        // n'est en cours
        if (!phrasesQueue.isEmpty() && isDisplayFinished && !isPlayingAudio && !hasSentAudioResponse) {
            Log.i(TAG_STREAM, "processPhrasesWithDelay: TTS local IF");
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
                                                Log.e(TAG_STREAM, "languageIdentifier : Can't identify language of : "
                                                        + phraseToPronounce);
                                                pronouncePhrase(phraseToPronounce);
                                            } else {
                                                // Utiliser la première langue identifiée
                                                IdentifiedLanguage language = identifiedLanguages.get(0);
                                                String languageCode = language.getLanguageTag();
                                                float confidence = language.getConfidence();
                                                Log.i("MRA_idetifyLanguage", "Language of : [ " + phraseToPronounce
                                                        + " ] is : " + languageCode + ", Confidence: " + confidence);
                                                if (buddyGPTApplication.getParamFromFile("Detection_confidence_rate",
                                                        "BuddyGPT.properties") != null &&
                                                        !buddyGPTApplication
                                                                .getParamFromFile("Detection_confidence_rate",
                                                                        "BuddyGPT.properties")
                                                                .trim().isEmpty()
                                                        &&
                                                        !buddyGPTApplication
                                                                .getParamFromFile("Detection_confidence_rate",
                                                                        "BuddyGPT.properties")
                                                                .trim().equals("0")) {
                                                    if (Integer.parseInt(buddyGPTApplication.getParamFromFile(
                                                            "Detection_confidence_rate",
                                                            "BuddyGPT.properties")) <= (confidence * 100)) {
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
            Log.i(TAG_STREAM, "processPhrasesWithDelay: ELSE");
            Log.i(TAG_STREAM, "processPhrasesWithDelay: isFullResponseReceived :" + isFullResponseReceived
                    + ", isError: " + isError);
            Log.i(TAG_STREAM, "processPhrasesWithDelay: streamQueue.isEmpty() :" + streamQueue.isEmpty());
            // Si la réponse complète est reçue ET l'affichage est terminé ET toutes les
            // queues (TTS local et Audio Stream) sont vides.
            if (isDisplayFinished && ((isFullResponseReceived && isReadyToSpeak && streamQueue.isEmpty())
                    || (isError && isReadyToSpeak)) && !isPlayingAudio) {
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
    // ... (Autres méthodes utilitaires : storeStreamResponse, onStartStreaming,
    // onErrorStreaming, clearHandlersAndQueues, resetLabialExpression,
    // handleResponseNotSuccessful, logErrorToFile, getLocalizedErrorMessage,
    // AsyncHttpCallback, asyncHttpRequest ) ...

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
            processPhrasesWithDelay();
            pronouncePhrase(errorMsg);
        }
    }

    // Dans ResponseFromTeamGPT.java

    private void clearHandlersAndQueues() {
        if (phrasesRunnable != null)
            phrasesHandler.removeCallbacks(phrasesRunnable);
        phrasesHandler.removeCallbacksAndMessages(null);
        phrasesQueue.clear();

        // Nouvelle file d'attente à nettoyer
        synchronized (streamQueue) {
            streamQueue.clear();
            currentPlayingItem = null;
            isPlayingAudio = false;
        }

        if (wordsRunnable != null)
            wordsHandler.removeCallbacks(wordsRunnable);
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
                        .translate(buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_en",
                                "BuddyGPT.properties"))
                        .addOnSuccessListener(translatedText -> errorMsg = translatedText)
                        .addOnFailureListener(e -> errorMsg = buddyGPTApplication
                                .getParamFromFile("chatBotServerNoResponce_en", "BuddyGPT.properties"));
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
            if (file1.exists() && file1.isFile())
                file1.delete();
            try (FileWriter fileWriter = new FileWriter(file1)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                String jsonStringF = gson.toJson(errorLOG);
                fileWriter.write(jsonStringF);
            }

            String errorTXT = new Date() + ", OpenAIERROR,ERROR CODE= " + response.code()
                    + ", ERROR Body{ message= " + message + ", type= " + type + ", param= " + param + ", code= " + code
                    + "}"
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
                                .addOnFailureListener(
                                        e -> errorMsg = buddyGPTApplication.getString(R.string.chatBotNoFound_en));
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
                                .addOnFailureListener(
                                        e -> errorMsg = buddyGPTApplication.getString(R.string.chatBot_ERROR_en));
                        return buddyGPTApplication.getString(R.string.chatBot_ERROR_en);
                }
        }
    }

    /**
     * Simple asynchronous HTTP helper using HttpURLConnection.
     * Usage: asyncHttpRequest(url, "POST", jsonBody, headersMap, new
     * AsyncHttpCallback{...});
     */
    public interface AsyncHttpCallback {
        void onSuccess(String body, int statusCode);

        void onFailure(Exception e);
    }

    public void asyncHttpRequest(String urlString,
                                 String method,
                                 String jsonBody,
                                 java.util.Map<String, String> headers,
                                 AsyncHttpCallback callback) {
        if (urlString == null || callback == null)
            return;
        new Thread(() -> {
            HttpURLConnection con = null;
            try {
                URL url = new URL(urlString);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod(method != null ? method : "GET");
                con.setConnectTimeout(15000);
                con.setReadTimeout(15000);
                // Apply headers
                if (headers != null) {
                    for (java.util.Map.Entry<String, String> entry : headers.entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null)
                            con.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
                // Write body for POST/PUT
                if (jsonBody != null && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))) {
                    con.setDoOutput(true);
                    con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    try (OutputStream os = con.getOutputStream()) {
                        os.write(input, 0, input.length);
                        os.flush();
                    }
                }
                int status = con.getResponseCode();
                InputStream is = (status >= 200 && status < 400) ? con.getInputStream() : con.getErrorStream();
                StringBuilder sb = new StringBuilder();
                if (is != null) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line).append('\n');
                        }
                    }
                }
                final String body = sb.toString();
                final int finalStatus = status;
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        callback.onSuccess(body, finalStatus);
                    } catch (Exception e) {
                        Log.e(TAG_STREAM, "asyncHttpRequest callback onSuccess failed", e);
                    }
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        callback.onFailure(e);
                    } catch (Exception ex) {
                        Log.e(TAG_STREAM, "asyncHttpRequest callback onFailure failed", ex);
                    }
                });
            } finally {
                if (con != null)
                    con.disconnect();
            }
        }).start();
    }

    public void reset() {
        Log.i(TAG_STREAM, "------------------reset-------------------");
        isReset = true;
        // reset phrasesQueue (TTS local):
        if (phrasesRunnable != null)
            phrasesHandler.removeCallbacks(phrasesRunnable);
        phrasesHandler.removeCallbacksAndMessages(null);
        phrasesQueue.clear();
        isReadyToSpeak = true;
        isResponseTimeSaved = false;
        // reset streamQueue (Audio serveur):
        synchronized (streamQueue) {
            streamQueue.clear();
            currentPlayingItem = null;
            isPlayingAudio = false;
        }

        // reset wordsQueue:
        if (wordsRunnable != null)
            wordsHandler.removeCallbacks(wordsRunnable);
        wordsHandler.removeCallbacksAndMessages(null);
        buddyGPTApplication.setResponseFromTeamGPT(null);
        // Réinitialiser les flags d'audio / texte audio pour la session suivante
        hasSentAudioResponse = false;
        hasSentAudioTextInput = false;
    }

}