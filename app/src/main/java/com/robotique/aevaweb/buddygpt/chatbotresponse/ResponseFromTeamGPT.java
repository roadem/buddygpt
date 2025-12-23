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
import java.io.ByteArrayOutputStream;
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
    public boolean isReadyToSpeak = true; // Vrai si le TTS est prêt à parler
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
    private boolean isFullResponseReceived = false; // Vrai si l'ensemble de la réponse a été reçue
    private boolean isDisplayFinished = true; // Vrai si l'affichage est terminé
    private String phraseToPronounceWhenResumed;
    private boolean isReset = false;
    private boolean isPaused = false;
    private boolean hasSentAudioTextInput = false;
    private boolean hasSentAudioResponse = false;
    private boolean isResponseTimeSaved = false;
    private JSONArray existingHistoryArray;
    private SimpleDateFormat sdf;
    private StreamItem lastAddedItem = null; // Last item added to the queue
    // New structure to pair text and audio chunks
    private static class StreamItem {
        String text;
        final Queue<String> audioChunks = new LinkedList<>();
        boolean audioReady = false; // true when at least one audio chunk has been attached
        // Vrai quand le serveur a envoyé le flag de fin pour CETTE phrase (ou la fin de la réponse)
        // accumulate PCM chunks per-item and flag whether item uses WAV chunks or PCM
        final ByteArrayOutputStream pcmAccumulator = new ByteArrayOutputStream();
        boolean itemIsWav = false;

        boolean audioEnd = false;
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
    private MediaPlayer streamPlayer;   // player utilisé pour les chunks audio serveur
    private final Queue<StreamItem> streamQueue = new LinkedList<>();
    private StreamItem currentPlayingItem = null;
    private boolean isPlayingAudio = false; // Vrai si un item est en lecture
    private StreamItem lastCreatedItem = null;
    private ByteArrayOutputStream pcmAccumulator = new ByteArrayOutputStream(); // Accumulateur pour les chunks audio serveur PCM
    private boolean currentItemIsWav = false; // Vrai si le dernier chunk est un fichier WAV
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

    public void getEnvironnement(ResponseCallback responseCallback) {

        new Thread(() -> {
            HttpURLConnection con = null;

            try {
                // --- Préparation URL et paramètres ---
                String baseUrl = buddyGPTApplication.getparam("TeamGPT_Base_url");
                String endpoint = buddyGPTApplication.getparam("TeamGPT_ApiEndpoint_Env");
                String gptKey = buddyGPTApplication.getparam(TeamGPTKey);

                Log.i(TAG_STREAM, "getEnvironnement: Endpoint = " + endpoint);

                URL url = new URL(baseUrl + endpoint);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("TeamGPT-Key", gptKey);
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);

                int responseCode = con.getResponseCode();
                Log.i(TAG_STREAM, "HTTP Response Code = " + responseCode);

                // --- Cas 404 : clé invalide ---
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    Log.i(TAG_PARAM, "Invalid TeamGPT Key detected in getEnvironnement");

                    buddyGPTApplication.resetSharedPreferences();
                    buddyGPTApplication.setparam("Environnement", "");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "TRUE");
                    buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");

                    responseCallback.onFailure();
                    return;
                } else

                // --- Cas 200 OK ---
                if (responseCode == HttpURLConnection.HTTP_OK) {

                    // Lire réponse JSON
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    Log.i(TAG_STREAM, "Response JSON = " + response);

                    JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();

                    if (!jsonObject.has("environment")) {
                        Log.e(TAG_STREAM, "Missing field 'environment' in JSON response");
                        buddyGPTApplication.resetSharedPreferences();
                        buddyGPTApplication.setparam("Environnement", "");
                        buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "TRUE");
                        buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
                        responseCallback.onFailure();
                        return;
                    } else {
                        // Extraction env
                        String env = jsonObject.get("environment").getAsString();
                        buddyGPTApplication.setparam("Environnement", env);
                        buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "FALSE");

                        Log.i(TAG_STREAM, "Environment loaded: " + env);

                        // Enchaînement
                        getParameters(new ResponseCallback() {
                            @Override
                            public void onSuccess() {
                                Log.i("TAG", "getParameters Success");
                            }

                            @Override
                            public void onFailure() {
                                Log.i("TAG", "getParameters Failure");
                            }
                        }, env);

                        responseCallback.onSuccess();
                    }

                }
            } catch (Exception e) {
                Log.e(TAG_STREAM, "Erreur inconnue : " + e.getMessage());
                buddyGPTApplication.resetSharedPreferences();
                buddyGPTApplication.setparam("Environnement", "");
                buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "TRUE");
                buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
                responseCallback.onFailure();

            } finally {
                if (con != null)
                    con.disconnect();
            }

        }).start();
    }

    public void getParameters(ResponseCallback responseCallback, String env) {
        final CountDownLatch latch = new CountDownLatch(1); // Initialize the latch with count 1

        new Thread(() -> {
            try {
                String url = buddyGPTApplication.getparam("TeamGPT_Base_url");
                String endpoint = env + buddyGPTApplication.getparam("TeamGPT_ApiEndpoint_Params");
                String gptKey = buddyGPTApplication.getparam(TeamGPTKey);
                String imeiDevice = buddyGPTApplication.getparam("TeamGPT_ID_Device");
                Log.i(TAG_PARAM, "getParameters: " + url + endpoint);
                URL obj = new URL(url + endpoint);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("TeamGPT-Key", gptKey);
                con.setRequestProperty("ID-Device", imeiDevice);

                int responseCode = con.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.i(TAG_PARAM, "getParameters: ");
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
                        Log.i(TAG_PARAM, "run: PARAMS : " + jsonObject.toString());
                        JsonArray parametersArray = jsonObject.getAsJsonArray("parameters");
                        if (parametersArray != null && parametersArray.size() > 0) {
                            JsonObject parametersObject = parametersArray.get(0).getAsJsonObject();
                            Gson gson = new Gson();
                            Parameters parameters = gson.fromJson(parametersObject.toString(), Parameters.class);
                            buddyGPTApplication.setparam(TeamGPTKey, gptKey);
                            if (parameters != null) {

                                buddyGPTApplication.setparam("INVALID_TEAMGPT_DEVICE_ID", "FALSE");
                                buddyGPTApplication.setparam("NomCompte", parameters.getNomCompte());
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

                                buddyGPTApplication.notifyObservers("GET_PARAMETERS_SUCCESS");
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
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_DEVICE_ID", "FALSE");
                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    responseCallback.onSuccess();
                    buddyGPTApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                    buddyGPTApplication.resetSharedPreferences();
                    buddyGPTApplication.notifyObservers("ENV_ERROR");
                    buddyGPTApplication.setparam("session_id", "");
                    buddyGPTApplication.setparam("ENV_ERROR", "TRUE");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "FALSE");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_DEVICE_ID", "FALSE");
                } else {
                    responseCallback.onFailure();
                    Log.e(TAG_PARAM, "Unexpected response code: " + responseCode);
                    buddyGPTApplication.setparam("session_id", "");
                    buddyGPTApplication.setparam("ENV_ERROR", "FALSE");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "FALSE");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_DEVICE_ID", "FALSE");
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

        String baseUrl = buddyGPTApplication.getparam("TeamGPT_Base_url");
        String endpoint = buddyGPTApplication.getparam("Environnement")
                + buddyGPTApplication.getparam("TeamGPT_ApiEndpoint_Response");
        String gptKey = buddyGPTApplication.getparam("TeamGPT_Key");
        String imeiDevice = buddyGPTApplication.getparam("TeamGPT_ID_Device");

        Log.i(TAG_STREAM, "---- SEND PUT REQUEST STREAM ----");
        Log.i(TAG_STREAM, "Base URL: " + baseUrl);
        Log.i(TAG_STREAM, "Endpoint: " + endpoint);
        Log.i(TAG_STREAM, "TeamGPT Key: " + gptKey);
        Log.i(TAG_STREAM, "Device ID: " + imeiDevice);
        Log.i(TAG_STREAM, "Final URL: " + baseUrl + endpoint);
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
            Log.i(TAG_STREAM, "sendPutRequestStream: audio");
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
                    Log.e(TAG_STREAM, "400 Bad Request: INVALID KEY or BAD PAYLOAD");
                    buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "TRUE");
                } else if (responseCode == 500) {
                    Log.e(TAG_STREAM, "500 Server Error: Session may be invalid");
                    buddyGPTApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                    buddyGPTApplication.notifyObservers("Session_ID_ERROR");
                    buddyGPTApplication.setparam("session_id", "");
                } else if (responseCode == 404) {
                    Log.e(TAG_STREAM, "404 Not Found: wrong environment endpoint");
                    buddyGPTApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                    buddyGPTApplication.notifyObservers("ENV_ERROR");
                    buddyGPTApplication.setparam("ENV_ERROR", "TRUE");
                    buddyGPTApplication.setparam("session_id", "");
                } else {
                    Log.e(TAG_STREAM, "Unexpected error -> calling handleError()");
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
                    Log.i(TAG_STREAM, "pronouncePhrase: visibility off");
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
        if (buddyGPTApplication.getparam("STT-TeamGPT").equalsIgnoreCase("local")) {
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
        } else {

            final String phraseToShow = currentDisplayedText + phrase;
            // on affiche UNIQUEMENT la phrase reçue
            buddyGPTApplication.notifyObservers("MODE_STREAM_TEXT;SPLIT;" + phraseToShow);

            currentDisplayedText += phrase + " ";
            // IMPORTANT : marquer l'affichage comme terminé après envoi
            isDisplayFinished = true;
        }
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
            // Vérifier TOUS les champs qui arrivent
            Log.i(TAG_STREAM, "=== STREAM MESSAGE RECEIVED ===");
            Log.i(TAG_STREAM, "has Answer: " + jsonObject.has("Answer"));
            Log.i(TAG_STREAM, "has Audio_reponse: " + jsonObject.has("Audio_reponse"));
            Log.i(TAG_STREAM, "has is_finished: " + jsonObject.has("is_finished"));
            Log.i(TAG_STREAM, "has Chatbot_is_finished: " + jsonObject.has("Chatbot_is_finished"));
            if (jsonObject.has("Answer")) {
                Log.i(TAG_STREAM, "✓ Answer value: " + jsonObject.getString("Answer").substring(0,
                        Math.min(50, jsonObject.getString("Answer").length())));
            }
            if (jsonObject.has("Audio_reponse")) {
                String audioStr = jsonObject.getString("Audio_reponse");
                Log.i(TAG_STREAM, "✓ Audio_reponse size: " + audioStr.length() + " chars");
            }

            // Gérer Text_input (transcription STT)
            if (jsonObject.has("Text_input") && !jsonObject.getString("Text_input").trim().isEmpty()) {
                handleTextInput(jsonObject);
            }

            boolean isChatbotFinished = jsonObject.optBoolean("Chatbot_is_finished", false);

            if (isChatbotFinished) {
                Log.i(TAG_STREAM, "🎯 Chatbot_is_finished received");
                Log.i(TAG_STREAM, "  lastAddedItem is null? " + (lastAddedItem == null));
                if (lastAddedItem != null) {
                    Log.i(TAG_STREAM, "  lastAddedItem.text: " + lastAddedItem.text);
                    Log.i(TAG_STREAM, "  lastAddedItem.audioEnd before: " + lastAddedItem.audioEnd);
                    lastAddedItem.audioEnd = true;
                    Log.i(TAG_STREAM, "  lastAddedItem.audioEnd after: " + lastAddedItem.audioEnd);
                }
                lastAddedItem = null;
            }

            // Gérer le cas où tous les flags de fin sont à 'true'
            if (jsonObject.has("is_finished") && jsonObject.getBoolean("is_finished")
                    && jsonObject.has("STT_is_finished") && jsonObject.getBoolean("STT_is_finished")
                    && isChatbotFinished
                    && jsonObject.has("TTS_is_finished") && jsonObject.getBoolean("TTS_is_finished")) {

                // L'ensemble des flags signale la fin de la SESSION ou du cycle complet.
                isDisplayFinished = true;
                isFullResponseReceived = true;
                isSessionIdProcessed = false;

                // Logique de nettoyage et de fin de lecture
                if (currentPlayingItem == null && !isPlayingAudio) {
                    if (streamQueue.isEmpty() && phrasesQueue.isEmpty()) {
                        processPhrasesWithDelay();
                    } else {
                        Log.i(TAG_STREAM, "processStreamLine: queue not empty");
                    }
                }
            } else {
                // Ces handlers doivent s'exécuter même si Chatbot_is_finished est true,
                // au cas où il y ait un dernier Answer ou Audio_reponse dans le même paquet.
                handleEmotion(jsonObject);
                handleSessionId(jsonObject);
                handleAnswer(jsonObject);
                handleAudioResponse(jsonObject);
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
                    if (!isResponseTimeSaved) {
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
                        // Mettre à jour la référence au dernier item ajouté
                        lastAddedItem = item;
                        Log.i(TAG_STREAM, "StreamItem ajouté. Taille actuelle de streamQueue : " + streamQueue.size());
                    }

                    Log.i(TAG_STREAM, "handleAnswer: currentPlayingItem : " + currentPlayingItem + " isPlayingAudio : "
                            + isPlayingAudio);
                    // Tenter de démarrer la lecture si c'est le premier item (
                    // démarrera seulement si un chunk audio arrive dans handleAudioResponse
                    if (currentPlayingItem == null) {
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
            } else {
                //  NOUVEAU : Answer est vide, ne pas créer d'item
                Log.i(TAG_STREAM, "handleAnswer: Answer is empty, skipping StreamItem creation");
            }
        } else {
            Log.w(TAG_STREAM, "✗ handleAnswer: Answer field missing or empty");
        }
    }

    // Gère la réception des chunks audio Base64
    private void handleAudioResponse(JSONObject jsonObject) {
        Log.i(TAG_STREAM, "handleAudioResponse: start ");
        if (jsonObject.has("Audio_reponse")) {
            String base64Audio = jsonObject.optString("Audio_reponse", "");
            if (base64Audio != null && !base64Audio.isEmpty()) {
                Log.i(TAG_STREAM, "Audio chunk reçu (len=" + base64Audio.length() + ")");
                hasSentAudioResponse = true;
                StreamItem target = null;
                synchronized (streamQueue) {
                    if (lastAddedItem != null) {
                        target = lastAddedItem;
                    } else if (currentPlayingItem != null) {
                        target = currentPlayingItem;
                    } else {
                        Log.w(TAG_STREAM, "handleAudioResponse: no target, creating audio-only placeholder");
                        target = new StreamItem("");
                        streamQueue.add(target);
                        lastAddedItem = target;
                    }

                    try {
                        byte[] decoded = Base64.decode(base64Audio, Base64.DEFAULT);
                        boolean chunkIsWav = isWavFile(decoded);

                        if (chunkIsWav) {
                            //  MODIFICATION : WAV est complet -> stocker en base64 pour streaming immédiat
                            target.audioChunks.add(base64Audio); // Garder le base64 pour WAV
                            target.itemIsWav = true;
                            Log.i(TAG_STREAM, "handleAudioResponse: chunk is complete WAV, queued as WAV ("
                                    + decoded.length + " bytes)");
                        } else {
                            //  PCM chunks -> accumuler jusqu'à audioEnd
                            target.pcmAccumulator.write(decoded);
                            target.itemIsWav = false;
                            Log.i(TAG_STREAM, "handleAudioResponse: chunk is PCM, appended to pcmAccumulator, size="
                                    + target.pcmAccumulator.size());
                        }

                        // Mark ready when we have at least some audio data
                        if (!target.audioReady) {
                            target.audioReady = true;
                        }

                        // If server marks end, set audioEnd
                        if (jsonObject.optBoolean("Audio_end", false)) {
                            target.audioEnd = true;
                            Log.i(TAG_STREAM, "handleAudioResponse: Audio_end flag set to true");
                        }

                        lastAddedItem = target;

                    } catch (IOException e) {
                        Log.e(TAG_STREAM, "handleAudioResponse: error processing chunk", e);
                    }
                }

                // Decide to start playback
                if (!isPlayingAudio && currentPlayingItem == null) {
                    Log.i(TAG_STREAM, "handleAudioResponse: attempting startNextReadyItemIfAny()");
                    startNextReadyItemIfAny();
                } else if (target != null && currentPlayingItem == target && !isPlayingAudio) {
                    Log.i(TAG_STREAM, "handleAudioResponse: resume playback for current item");
                    isPlayingAudio = true;
                    playNextChunkForCurrentItem();
                }
            }
        }
    }

    private void playNextChunkForCurrentItem() {
        Log.i("TAG", "playNextChunkForCurrentItem: start");
        if (currentPlayingItem == null) {
            Log.i(TAG_STREAM, "playNextChunkForCurrentItem: (currentPlayingItem == null)");
            isPlayingAudio = false;
            hasSentAudioResponse = false;
            if (isFullResponseReceived && streamQueue.isEmpty() && phrasesQueue.isEmpty()) {
                processPhrasesWithDelay();
            }
            return;
        }

        // If item uses WAV chunks, keep previous logic (streaming chunk-per-chunk WAV)
        //  PATH WAV : streaming chunk-par-chunk (pas d'accumulation)
        if (currentPlayingItem.itemIsWav) {
            String nextChunk = currentPlayingItem.audioChunks.poll();
            Log.i(TAG_STREAM, "playNextChunkForCurrentItem (WAV): nextChunk == null? " + (nextChunk == null));
            if (nextChunk == null) {
                if (currentPlayingItem.audioEnd) {
                    Log.w(TAG_STREAM, "--- FIN D'ITEM CONFIRMEE PAR AUDIO_END (WAV) ---");
                    onPlaybackFinished(currentPlayingItem);
                    return;
                } else {
                    Log.i(TAG_STREAM, "Chunk queue empty (WAV), waiting for more data...");
                    isPlayingAudio = false;
                    startNextReadyItemIfAny();
                    return;
                }
            }
            // Jouer le WAV chunk immédiatement (il est complet)
            final String base64Chunk = nextChunk;
            new Thread(() -> {
                File outFile = null;
                try {
                    byte[] audioBytes = Base64.decode(base64Chunk, Base64.DEFAULT);
                    // C'est déjà un WAV complet, pas besoin d'ajouter d'en-tête
                    outFile = new File(Environment.getExternalStorageDirectory(),
                            "chunk_" + System.currentTimeMillis() + ".wav");
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        fos.write(audioBytes);
                    }
                    File fileToPlay = outFile;
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(() -> {
                        // same MediaPlayer creation & listeners as before...
                        if (streamPlayer != null) {
                            try {
                                if (streamPlayer.isPlaying())
                                    streamPlayer.stop();
                            } catch (Exception ignored) {
                                Log.i(TAG_STREAM, "playNextChunkForCurrentItem: ");
                            }
                            try {
                                streamPlayer.release();
                            } catch (Exception ignored) {
                                Log.i(TAG_STREAM, "playNextChunkForCurrentItem: ");
                            }
                            streamPlayer = null;
                        }
                        streamPlayer = new MediaPlayer();
                        MediaPlayer mp = streamPlayer;
                        try {
                            mp.setDataSource(fileToPlay.getAbsolutePath());
                            mp.setAudioAttributes(new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build());
                            mp.setOnPreparedListener(player -> player.start());
                            mp.setOnCompletionListener(player -> {
                                try {
                                    player.release();
                                } catch (Exception ignored) {
                                    Log.e(TAG_STREAM, "Error playing WAV chunk", ignored);
                                }
                                if (streamPlayer == player)
                                    streamPlayer = null;
                                if (fileToPlay.exists())
                                    fileToPlay.delete();
                                playNextChunkForCurrentItem();
                            });
                            mp.setOnErrorListener((player, what, extra) -> {
                                try {
                                    player.release();
                                } catch (Exception ignored) {
                                    Log.e(TAG_STREAM, "Error playing WAV chunk", ignored);
                                }
                                if (streamPlayer == player)
                                    streamPlayer = null;
                                if (fileToPlay.exists())
                                    fileToPlay.delete();
                                playNextChunkForCurrentItem();
                                return true;
                            });
                            mp.prepareAsync();
                        } catch (Exception e) {
                            Log.e(TAG_STREAM, "Error playing WAV chunk", e);
                            if (mp != null)
                                mp.release();
                            if (fileToPlay.exists())
                                fileToPlay.delete();
                            playNextChunkForCurrentItem();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG_STREAM, "Erreur play WAV chunk: " + e.getMessage(), e);
                    if (outFile != null && outFile.exists())
                        outFile.delete();
                    playNextChunkForCurrentItem();
                }
            }).start();
            return;
        }

        // PCM flow: we only play when server signalled audioEnd for this item.
        synchronized (streamQueue) {
            Log.i(TAG_STREAM, "playNextChunkForCurrentItem (PCM): pcm size=" + currentPlayingItem.pcmAccumulator.size()
                    + " audioEnd=" + currentPlayingItem.audioEnd);
            if (currentPlayingItem.pcmAccumulator.size() == 0) {
                // nothing to play yet
                if (currentPlayingItem.audioEnd) {
                    // nothing and end -> finish
                    onPlaybackFinished(currentPlayingItem);
                } else {
                    isPlayingAudio = false;
                }
                return;
            }

            if (!currentPlayingItem.audioEnd) {
                // wait for more PCM chunks (server not finished)
                Log.i(TAG_STREAM, "playNextChunkForCurrentItem: waiting for more PCM chunks");
                isPlayingAudio = false;
                return;
            }

            // audioEnd == true and we have PCM data -> convert once and play whole wav
            byte[] pcmBytes = currentPlayingItem.pcmAccumulator.toByteArray();
            // clear accumulator immediately to avoid reusing same bytes later
            currentPlayingItem.pcmAccumulator.reset();

            byte[] wavBytes = addWavHeader(pcmBytes);
            File outFile = new File(Environment.getExternalStorageDirectory(),
                    "pcm_chunk_" + System.currentTimeMillis() + ".wav");
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(wavBytes);
            } catch (Exception e) {
                Log.e(TAG_STREAM, "Erreur writing PCM->WAV file", e);
                onPlaybackFinished(currentPlayingItem);
                return;
            }

            final File fileToPlay = outFile;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                if (streamPlayer != null) {
                    try {
                        if (streamPlayer.isPlaying())
                            streamPlayer.stop();
                    } catch (Exception ignored) {
                        Log.i(TAG_STREAM, "playNextChunkForCurrentItem: ");
                    }
                    try {
                        streamPlayer.release();
                    } catch (Exception ignored) {
                        Log.i(TAG_STREAM, "playNextChunkForCurrentItem: ");
                    }
                    streamPlayer = null;
                }
                streamPlayer = new MediaPlayer();
                MediaPlayer mp = streamPlayer;
                try {
                    mp.setDataSource(fileToPlay.getAbsolutePath());
                    mp.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build());
                    mp.setOnPreparedListener(player -> player.start());
                    mp.setOnCompletionListener(player -> {
                        try {
                            player.release();
                        } catch (Exception ignored) {
                            Log.e(TAG_STREAM, "Error playing PCM->WAV", ignored);
                        }
                        if (streamPlayer == player)
                            streamPlayer = null;
                        if (fileToPlay.exists())
                            fileToPlay.delete();
                        onPlaybackFinished(currentPlayingItem);
                    });
                    mp.setOnErrorListener((player, what, extra) -> {
                        try {
                            player.release();
                        } catch (Exception ignored) {
                            Log.e(TAG_STREAM, "Error playing PCM->WAV", ignored);
                        }
                        if (streamPlayer == player)
                            streamPlayer = null;
                        if (fileToPlay.exists())
                            fileToPlay.delete();
                        onPlaybackFinished(currentPlayingItem);
                        return true;
                    });
                    mp.prepareAsync();
                } catch (Exception e) {
                    Log.e(TAG_STREAM, "Error playing PCM->WAV", e);
                    if (mp != null)
                        mp.release();
                    if (fileToPlay.exists())
                        fileToPlay.delete();
                    onPlaybackFinished(currentPlayingItem);
                }
            });
        }
    }

    //  HELPER 1 : Vérifier si le fichier est WAV
    private boolean isWavFile(byte[] data) {
        // WAV files start with "RIFF" (0x52 0x49 0x46 0x46)
        return data != null && data.length >= 4
                && data[0] == 0x52 && data[1] == 0x49
                && data[2] == 0x46 && data[3] == 0x46;
    }

    //  HELPER 2 : Ajouter en-tête WAV au PCM
    private byte[] addWavHeader(byte[] pcmData) {
        //  Le serveur OpenAI TTS envoie TOUJOURS du 24kHz PCM
        int sampleRate = 24000; // ← FIXE : le serveur envoie du 24kHz
        int numChannels = 1; // MONO
        int bitsPerSample = 16; // 16-bit PCM

        Log.i(TAG_STREAM, "addWavHeader: Using fixed sampleRate=24000 Hz (from OpenAI TTS)");
        Log.i(TAG_STREAM, "addWavHeader: PCM data size=" + pcmData.length + " bytes");

        // Calcul de la durée réelle
        int byteRate = sampleRate * numChannels * bitsPerSample / 8;
        int blockAlign = numChannels * bitsPerSample / 8;

        byte[] wavHeader = new byte[44];

        // RIFF header
        wavHeader[0] = 'R';
        wavHeader[1] = 'I';
        wavHeader[2] = 'F';
        wavHeader[3] = 'F';
        int fileSize = pcmData.length + 36;
        wavHeader[4] = (byte) (fileSize & 0xff);
        wavHeader[5] = (byte) ((fileSize >> 8) & 0xff);
        wavHeader[6] = (byte) ((fileSize >> 16) & 0xff);
        wavHeader[7] = (byte) ((fileSize >> 24) & 0xff);

        // WAVE format
        wavHeader[8] = 'W';
        wavHeader[9] = 'A';
        wavHeader[10] = 'V';
        wavHeader[11] = 'E';

        // fmt subchunk
        wavHeader[12] = 'f';
        wavHeader[13] = 'm';
        wavHeader[14] = 't';
        wavHeader[15] = ' ';
        wavHeader[16] = 16;
        wavHeader[17] = 0;
        wavHeader[18] = 0;
        wavHeader[19] = 0; // Subchunk1Size = 16
        wavHeader[20] = 1;
        wavHeader[21] = 0; // AudioFormat = 1 (PCM)
        wavHeader[22] = (byte) numChannels;
        wavHeader[23] = 0; // NumChannels
        wavHeader[24] = (byte) (sampleRate & 0xff);
        wavHeader[25] = (byte) ((sampleRate >> 8) & 0xff);
        wavHeader[26] = (byte) ((sampleRate >> 16) & 0xff);
        wavHeader[27] = (byte) ((sampleRate >> 24) & 0xff); // SampleRate
        wavHeader[28] = (byte) (byteRate & 0xff);
        wavHeader[29] = (byte) ((byteRate >> 8) & 0xff);
        wavHeader[30] = (byte) ((byteRate >> 16) & 0xff);
        wavHeader[31] = (byte) ((byteRate >> 24) & 0xff); // ByteRate
        wavHeader[32] = (byte) blockAlign;
        wavHeader[33] = 0; // BlockAlign
        wavHeader[34] = (byte) bitsPerSample;
        wavHeader[35] = 0; // BitsPerSample

        // data subchunk
        wavHeader[36] = 'd';
        wavHeader[37] = 'a';
        wavHeader[38] = 't';
        wavHeader[39] = 'a';
        wavHeader[40] = (byte) (pcmData.length & 0xff);
        wavHeader[41] = (byte) ((pcmData.length >> 8) & 0xff);
        wavHeader[42] = (byte) ((pcmData.length >> 16) & 0xff);
        wavHeader[43] = (byte) ((pcmData.length >> 24) & 0xff); // Subchunk2Size

        // Combiner header + data
        byte[] wavFile = new byte[wavHeader.length + pcmData.length];
        System.arraycopy(wavHeader, 0, wavFile, 0, wavHeader.length);
        System.arraycopy(pcmData, 0, wavFile, wavHeader.length, pcmData.length);

        Log.i(TAG_STREAM, "addWavHeader: WAV file created, total size=" + wavFile.length + " bytes");
        return wavFile;
    }

    // Démarre la lecture pour le prochain StreamItem prêt (avec audio attaché)
    private void startNextReadyItemIfAny() {
        Log.i(TAG_STREAM, "startNextReadyItemIfAny: start. Current state: currentPlayingItem=" + currentPlayingItem
                + ", isPlayingAudio=" + isPlayingAudio);

        //  MODIFICATION : Si on est en attente de chunks WAV (audioEnd=false),
        // on peut quand même essayer de lancer le prochain item
        if (currentPlayingItem != null && isPlayingAudio) {
            // Une lecture est en cours, ne rien démarrer
            Log.i(TAG_STREAM, "startNextReadyItemIfAny: Playback already active, skipping");
            return;
        }

        synchronized (streamQueue) {
            for (StreamItem item : streamQueue) {
                Log.i(TAG_STREAM, "startNextReadyItemIfAny: " + item.text + " audioReady=" + item.audioReady
                        + " itemIsWav=" + item.itemIsWav + " audioEnd=" + item.audioEnd);
            }

            // 1. Regarder le prochain élément sans le retirer (Peek)
            StreamItem si = streamQueue.peek();

            if (si != null && si.audioReady) {
                // IMPORTANT:
                // - si c'est un WAV on peut jouer dès qu'un chunk est présent
                // - si c'est du PCM (multi-chunks) on doit attendre audioEnd == true pour
                // récupérer/poller l'item
                boolean canStart = si.itemIsWav || // wav streaming -> start immediately
                        si.audioEnd; // pcm -> only when server signalled end

                if (!canStart) {
                    Log.i(TAG_STREAM,
                            "startNextReadyItemIfAny: Item ready but waiting for audioEnd (PCM multi-chunks).");
                    return;
                }

                Log.i(TAG_STREAM, "startNextReadyItemIfAny: size " + streamQueue.size());
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
        Log.i(TAG_STREAM, "🎬 startPlaybackForItem START");
        Log.i(TAG_STREAM, "  item.text: " + item.text);
        Log.i(TAG_STREAM, "  item.audioChunks.size(): " + item.audioChunks.size());
        Log.i(TAG_STREAM, "  item.audioReady: " + item.audioReady);
        Log.i(TAG_STREAM, "  item.audioEnd: " + item.audioEnd);

        currentPlayingItem = item;
        isPlayingAudio = true;
        hasSentAudioResponse = true;

        Log.i(TAG_STREAM, "startPlaybackForItem: " + item.text + " (chunks=" + item.audioChunks.size() + ")");

        // DÉBUT LECTURE AUDIO : Afficher expression SPEAK
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
        } catch (Exception e) {
            Log.e(TAG_STREAM, "BuddySDK Exception in startPlaybackForItem: " + e);
        }

        // Afficher le texte (affichage progressif via showPhrase)
        if (item.text != null && !item.text.isEmpty()) {
            if (buddyGPTApplication.getparam("switch_visibility").equals("true")) {
                Log.i(TAG_STREAM, "startPlaybackForItem: calling showPhrase for item text");
                showPhrase(item.text);
            } else {
                Log.i(TAG_STREAM, "startPlaybackForItem: visibility is off");
            }
        }
        Log.i(TAG_STREAM, "🎬 startPlaybackForItem: Calling playNextChunkForCurrentItem DIRECTLY");
        playNextChunkForCurrentItem();
    }

    /**
     * Appelée uniquement lorsque la file d'audio est vide ET que audioEnd est vrai.
     */
    private void onPlaybackFinished(StreamItem finishedItem) {
        Log.i(TAG_STREAM, "onPlaybackFinished: Item finished: " + finishedItem.text);

        // 1. L'action CRUCIALE de nettoyage
        currentPlayingItem = null; // nettoyer currentPlayingItem
        isPlayingAudio = false; // marquer la fin
        hasSentAudioResponse = false;

        //  FIN LECTURE AUDIO : Remettre expression à NO_EXPRESSION
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
        } catch (Exception e) {
            Log.e(TAG_STREAM, "BuddySDK Exception in onPlaybackFinished: " + e);
        }
        // 2. Notifier l'UI/autres systèmes si nécessaire
        buddyGPTApplication.notifyObservers("AUDIO_PLAYBACK_FINISHED;SPLIT;");
        // Reschedule la vérification du prochain item
        // (même si pas prêt maintenant, il peut l'être bientôt)
        if (!isFullResponseReceived) {
            Log.i(TAG_STREAM, "onPlaybackFinished: Scheduling next item check (response not complete yet)");
            phrasesRunnable = this::processPhrasesWithDelay;
            phrasesHandler.postDelayed(phrasesRunnable, 100);
            return;
        }
        // 3. Tenter de démarrer l'item suivant (si l'audio est déjà prêt)
        startNextReadyItemIfAny();
        // 4. Vérifier si TOUT est fini (incluant TTS local)
        if (isCompletelyFinished()) {
            Log.i(TAG_STREAM, " TOUT EST TERMINÉ - Envoi TTS_success");
            onFinishStreaming();
            buddyGPTApplication.notifyObservers("TTS_success");
            reset();
        }
    }

    // Méthode pour vérifier si TOUT est terminé
    private boolean isCompletelyFinished() {
        boolean queueEmpty = streamQueue.isEmpty();
        boolean noCurrentItem = currentPlayingItem == null;
        boolean notPlaying = !isPlayingAudio;
        boolean phrasesEmpty = phrasesQueue.isEmpty();
        boolean readyToSpeak = isReadyToSpeak;
        boolean displayFinished = isDisplayFinished;
        boolean fullResponseReceived = isFullResponseReceived;

        Log.i(TAG_STREAM, "isCompletelyFinished DEBUG:");
        Log.i(TAG_STREAM, "  streamQueue.isEmpty()=" + queueEmpty);
        Log.i(TAG_STREAM, "  currentPlayingItem==null=" + noCurrentItem);
        Log.i(TAG_STREAM, "  !isPlayingAudio=" + notPlaying);
        Log.i(TAG_STREAM, "  phrasesQueue.isEmpty()=" + phrasesEmpty);
        Log.i(TAG_STREAM, "  isReadyToSpeak=" + readyToSpeak);
        Log.i(TAG_STREAM, "  isDisplayFinished=" + displayFinished);
        Log.i(TAG_STREAM, "  isFullResponseReceived=" + fullResponseReceived);

        boolean result = queueEmpty && noCurrentItem && notPlaying && phrasesEmpty && readyToSpeak && displayFinished
                && fullResponseReceived;
        Log.i(TAG_STREAM, "  RESULT=" + result);

        return result;
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
        ///  NOUVEAU : Avant de chercher des phrases TTS, essayer de lancer un item
        /// audio prêt
        if (currentPlayingItem == null && !isPlayingAudio) {
            Log.i(TAG_STREAM, "processPhrasesWithDelay: Attempting to start next ready audio item...");
            startNextReadyItemIfAny();

            // Si on vient de démarrer un item, retourner (laisser la lecture se faire)
            if (currentPlayingItem != null || isPlayingAudio) {
                Log.i(TAG_STREAM, "processPhrasesWithDelay: Started audio playback, scheduling next check");
                phrasesRunnable = this::processPhrasesWithDelay;
                phrasesHandler.postDelayed(phrasesRunnable, 50);
                return;
            }
            //  SI ON TROUVE UN ITEM MAIS PAS D'AUDIO PRÊT :
            // - Si la réponse est complète (isFullResponseReceived), traiter l'item comme
            // text-only
            // - Sinon, reschedule et attendre
            StreamItem waitingItem = null;
            synchronized (streamQueue) {
                waitingItem = streamQueue.peek();
            }
            if (waitingItem != null && !waitingItem.audioReady) {
                if (isFullResponseReceived) {
                    //  La réponse est complète mais l'audio n'est pas arrivé
                    // Traiter cet item comme text-only : l'afficher et passer au suivant
                    Log.i(TAG_STREAM,
                            "processPhrasesWithDelay: Item without audio BUT response is complete. Treating as text-only.");

                    synchronized (streamQueue) {
                        streamQueue.poll(); // Retirer l'item de la queue
                    }

                    // Afficher le texte
                    if (waitingItem.text != null && !waitingItem.text.isEmpty()) {
                        if (buddyGPTApplication.getparam("switch_visibility").equals("true")) {
                            showPhrase(waitingItem.text);
                        } else {
                            Log.i(TAG_STREAM, "processPhrasesWithDelay: visibility is off");
                        }
                    }

                    // Reschedule immédiatement pour vérifier le prochain item
                    phrasesRunnable = this::processPhrasesWithDelay;
                    phrasesHandler.postDelayed(phrasesRunnable, 50);
                    return;
                } else {
                    // La réponse n'est pas complète, l'audio peut encore arriver
                    Log.i(TAG_STREAM, "processPhrasesWithDelay: Found item without audio, rescheduling...");
                    phrasesRunnable = this::processPhrasesWithDelay;
                    phrasesHandler.postDelayed(phrasesRunnable, 100); // Attendre que l'audio arrive
                    return;
                }
            }
        }

        // Cas 1 : Il y a des phrases TTS à prononcer ET pas d'audio serveur en cours
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
                                                Log.i("TAG_idetifyLanguage", "Language of : [ " + phraseToPronounce
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
        }
        // Cas 2 : Plus rien à faire - vérifier si c'est vraiment la fin
        else {
            Log.i(TAG_STREAM, "processPhrasesWithDelay: Vérification fin globale");
            Log.i(TAG_STREAM, "  streamQueue.isEmpty()=" + streamQueue.isEmpty());
            Log.i(TAG_STREAM, "  currentPlayingItem=" + (currentPlayingItem == null ? "null" : "active"));
            Log.i(TAG_STREAM, "  isPlayingAudio=" + isPlayingAudio);
            Log.i(TAG_STREAM, "  phrasesQueue.isEmpty()=" + phrasesQueue.isEmpty());
            Log.i(TAG_STREAM, "  isFullResponseReceived=" + isFullResponseReceived);

            if (isCompletelyFinished()) {
                Log.i(TAG_STREAM, " FIN CONFIRMÉE");
                onFinishStreaming();
                buddyGPTApplication.notifyObservers("TTS_success");
                reset();
                return;
            }
        }
        // Continuer la vérification périodique
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

        //  NOUVEAU : Arrêter le MediaPlayer en cours de lecture
        if (streamPlayer != null) {
            try {
                if (streamPlayer.isPlaying()) {
                    Log.i(TAG_STREAM, "reset: Stopping MediaPlayer...");
                    streamPlayer.stop();
                }
                streamPlayer.release();
                Log.i(TAG_STREAM, "reset: MediaPlayer released");
            } catch (Exception e) {
                Log.e(TAG_STREAM, "reset: Error stopping/releasing streamPlayer: " + e.getMessage());
            }
            streamPlayer = null;
        }

        // reset phrasesQueue (TTS local):
        if (phrasesRunnable != null)
            phrasesHandler.removeCallbacks(phrasesRunnable);
        phrasesHandler.removeCallbacksAndMessages(null);
        phrasesQueue.clear();
        isReadyToSpeak = true;
        answer = "";
        isResponseTimeSaved = false;

        //  NOUVEAU : Arrêter tous les handlers de playback
        if (wordsRunnable != null)
            wordsHandler.removeCallbacks(wordsRunnable);
        wordsHandler.removeCallbacksAndMessages(null);

        // reset streamQueue (Audio serveur):
        synchronized (streamQueue) {
            streamQueue.clear();
            currentPlayingItem = null;
            isPlayingAudio = false;
        }

        // reset wordsQueue:
        buddyGPTApplication.setResponseFromTeamGPT(null);
        // Réinitialiser les flags d'audio / texte audio pour la session suivante
        hasSentAudioResponse = false;
        hasSentAudioTextInput = false;
        lastCreatedItem = null;

        Log.i(TAG_STREAM, "reset: Complete");
    }

}