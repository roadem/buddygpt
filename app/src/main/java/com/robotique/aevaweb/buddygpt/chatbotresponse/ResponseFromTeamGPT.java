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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;

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
    private static final String TAG_NSTREAM = "NON_STREAM_MODE";
    String chatBotServerNoResponce_fr;
    String chatBotServerNoResponce_en;
    String chatBotServerNoResponce_es;
    String chatBotServerNoResponce_de;
    private final Queue<String> phrasesQueue = new LinkedList<>();
    private final Handler phrasesHandler = new Handler();
    private final Handler wordsHandler = new Handler();
    private Runnable wordsRunnable;

    private String langueFr = "Français";
    private String langueEn = "Anglais";
    private String langueEs = "Espagnol";
    private String langueDe = "Allemand";

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
    private int requestTotalTokens = 0;
    private String currentEmotion = "";
    private String result = "";
    BuddyGPTApplication buddyGPTApplication;
    public ResponseFromTeamGPT(BuddyGPTApplication context) {
        this.buddyGPTApplication = context;
        chatBotServerNoResponce_fr= buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_fr","BuddyGPT.properties");
        chatBotServerNoResponce_en= buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_en","BuddyGPT.properties");
        chatBotServerNoResponce_es= buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_es","BuddyGPT.properties");
        chatBotServerNoResponce_de= buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_de","BuddyGPT.properties");

    }
    public void getParameters() {
        final CountDownLatch latch = new CountDownLatch(1); // Initialize the latch with count 1

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = buddyGPTApplication.getparam("TeamGPT_url");
                    String endpoint = buddyGPTApplication.getparam("TeamGPT_ApiEndpoint_Params");
                    String gptKey = buddyGPTApplication.getparam("TeamGPT_Key");
                    String imeiDevice = buddyGPTApplication.getImeiRobot();

                    URL obj = new URL(url + endpoint);
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                    con.setRequestMethod("GET");
                    con.setRequestProperty("TeamGPT-Key", gptKey);
                    con.setRequestProperty("IMEI-ID-Device", imeiDevice);

                    int responseCode = con.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
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
                                buddyGPTApplication.setparam("email_support",parameters.getEmailSupport());
                                buddyGPTApplication.setparam("IMEI_ID_Device",parameters.getImeiDevice());
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
                    else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                        Log.i(TAG_NSTREAM, "run: notifyObservers INVALID_TEAMGPT_KEY 1");
                        buddyGPTApplication.setparam("TeamGPT_Key",gptKey);
                        buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
                    }
                    con.disconnect();
                } catch (Exception e) {
                    Log.e("HOU", "Exception in getParameters: ", e);
                } finally {
                    latch.countDown(); // Ensure latch is counted down regardless of success or failure
                }
            }
        }).start();

        try {
            latch.await(); // Wait for the thread to finish
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

//    public void sendPutRequestNStream(String question , int numberOfQuestion) throws IOException {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//
//                    String url = buddyGPTApplication.getparam("TeamGPT_url");
//                    String endpoint = buddyGPTApplication.getparam("TeamGPT_ApiEndpoint_Response");
//                    String gptKey = buddyGPTApplication.getparam("TeamGPT_Key");
//                    String imeiDevice = buddyGPTApplication.getImeiRobot();
//
//                    URL obj = new URL(url + endpoint);
//                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();
//                    con.setRequestMethod("POST");
//                    con.setRequestProperty("TeamGPT-Key", gptKey);
//                    con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
//                    con.setDoOutput(true);
//
//                    JSONObject jsonObject = new JSONObject();
//                    jsonObject.put("Text_input", question);
//
//                    jsonObject.put("IMEI_ID_Device", imeiDevice);
//                    if (buddyGPTApplication.getparam("switch_emotion").equals("true"))
//                        jsonObject.put("EMOTION", true);
//                    else
//                        jsonObject.put("EMOTION", false);
//                    jsonObject.put("Commandes", true);
//                    jsonObject.put("LANGUE", buddyGPTApplication.getLangue().getLanguageCode().split("-")[0]);
//                    if (!buddyGPTApplication.getparam("session_id").equals(""))
//                        jsonObject.put("session_id", buddyGPTApplication.getparam("session_id"));
//
//                    Log.i(TAG_NSTREAM, "sendPutRequest: HOU input" +jsonObject.toString());
//                    // Enable writing to the connection output stream
//                    con.setDoOutput(true);
//                    // Send request
//                    try (OutputStream os = con.getOutputStream()) {
//                        byte[] input = jsonObject.toString().getBytes("utf-8");
//                        os.write(input, 0, input.length);
//                    }
//                    String fileName1 = "TeamGPT-sent";
//
//                    File file1 = new File( Environment.getExternalStorageDirectory(), "BuddyGPT/" + fileName1 + ".json" );
//
//                    try {
//                        if (file1.exists() && file1.isFile()) {
//                            file1.delete();
//                            Log.v( "Json_API", "file deleted" );
//                        }
//
//                        FileWriter fileWriter1 = new FileWriter( file1 );
//                        Gson gson1 = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
//                        String jsonString1 = gson1.toJson( jsonObject );
//                        fileWriter1.write( jsonString1 );
//                        fileWriter1.close();
//                        Log.v( "Json_API", "new file added" );
//
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    // Read the response code
//                    int responseCode = con.getResponseCode();
//                    if (responseCode == HttpURLConnection.HTTP_OK) {
//                        buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY","FALSE");
//                        Log.i(TAG_NSTREAM, "sendPutRequest: HOU ouput" );
//                        buddyGPTApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
//                        buddyGPTApplication.setGetResponseTime(System.currentTimeMillis());
//                        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
//                        StringBuilder response = new StringBuilder();
//                        String line;
//                        while ((line = in.readLine()) != null) {
//                            response.append(line.trim());
//                        }
//                        in.close();
//                        // Traitement de la réponse :
//                        try {
//
//
//                            String[] responseParts = response.toString().split("data: ");
//                            JSONArray jsonArray = new JSONArray();
//                            List<String> answerList = new ArrayList<>();
//                            // Loop over each part of the responseParts array
//                            for (int i = 1; i < responseParts.length; i++) {
//                                String jsonString = responseParts[i].trim();
//
//                                // Convert each part into a JSONObject and add to the JSONArray
//                                JSONObject jsonObj = new JSONObject(jsonString);
//                                jsonArray.put(jsonObj);
//
//                            for (int i = 0; i < jsonArray.length(); i++) {
//                                JSONObject jsonObj = jsonArray.getJSONObject(i);
//                                // session
//                                if(jsonObj.has("session_id") && buddyGPTApplication.getparam("session_id").equalsIgnoreCase("")){
//                                    buddyGPTApplication.setparam("session_id",jsonObj.getString("session_id"));
//                                }
//                                if(jsonObj.has("session_id") && !buddyGPTApplication.getparam("session_id").equalsIgnoreCase(jsonObj.getString("session_id"))
//                                        && !buddyGPTApplication.getparam("session_id").equalsIgnoreCase("")){
//                                    buddyGPTApplication.notifyObservers("Session_ID_Changed");
//                                    buddyGPTApplication.setparam("session_id",jsonObj.getString("session_id"));
//                                }
//
//                                // Check if the "Answer" field exists and extract it
//                                if (jsonObj.has("Answer") && !jsonObj.getBoolean("is_finished")) {
//                                    String answer = jsonObj.getString("Answer");
//                                    answerList.add(answer);  // Add to the list of answers
//                                }
//                                //emotion
//
//                                if(jsonObj.has("Emotion") && buddyGPTApplication.getparam("switch_emotion").equals("true")){
//
//                                    emotion=jsonObj.getString("Emotion");
//
//                                }
//
//                                else if(jsonObj.has("session_id") && !buddyGPTApplication.getparam("session_id").equals(jsonObj.getString("session_id"))){
//                                    buddyGPTApplication.setparam("session_id",jsonObj.getString("session_id"));
//                                    //dialog
//                                }
//
//                            }
//
//                            //Mettre   le fichier le plus récent reçu
//                            String fileName2 = "TeamGPT-recv";
//
//                            File file2 = new File(Environment.getExternalStorageDirectory(), "BuddyGPT/" + fileName2 + ".json");
//                            Log.i(TAG_NSTREAM, "sendPutRequest: HOU ouput 2" );
//                            try {
//                                if (file2.exists() && file2.isFile()) {
//                                    file2.delete();
//                                    Log.v("Json_API", "file deleted");
//                                }
//
//                                FileWriter fileWriter2 = new FileWriter(file2);
//                                Gson gson2 = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
//                                String jsonString2=gson2.toJson(jsonArray);
//                                fileWriter2.write(jsonString2);
//                                fileWriter2.close();
//                                Log.v("Json_API", "new file added");
//
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                            Log.i(TAG_NSTREAM, "sendPutRequest: HOU ouput 3" );
//                            // récupérer la réponse
//                            Log.i("TEAMGPT", "run: answerList "+answerList);
//
//                            Log.i("TEAMGPT", "run: result "+result);
//
//                            result =  String.join("\n", answerList);
//
//                            if (result.trim().equals("")){
//
//                                if (buddyGPTApplication.getCurrentLanguage().equals("en")) {
//                                    buddyGPTApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + chatBotServerNoResponce_en+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
//                                }
//                                else if (buddyGPTApplication.getCurrentLanguage().equals("fr")){
//                                    buddyGPTApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + chatBotServerNoResponce_fr+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
//                                }
//                                else if (buddyGPTApplication.getCurrentLanguage().equals("es")){
//                                    buddyGPTApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + chatBotServerNoResponce_es+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
//                                }
//                                else if (buddyGPTApplication.getCurrentLanguage().equals("de")){
//                                    buddyGPTApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + chatBotServerNoResponce_de+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
//                                }
//                                else {
//                                    buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(chatBotServerNoResponce_en).addOnSuccessListener(new OnSuccessListener<String>() {
//                                        @Override
//                                        public void onSuccess(String translatedText) {
//
//                                            buddyGPTApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;"+translatedText+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
//                                        }
//
//                                    }).addOnFailureListener(new OnFailureListener() {
//                                        @Override
//                                        public void onFailure(@NonNull Exception e) {
//                                            Log.e(TAG_NSTREAM,"translatedText exception  "+e);
//                                        }
//                                    });
//
//                                }
//                            }
//                            // Play la suite de la réponse.
//                            else{
//                                Log.e("TEAMGPT","Detection_de_langue  -----"+ buddyGPTApplication.getparam("Detection_de_langue").equals("true"));
//                                setAnimation(emotion);
//
//                                if (buddyGPTApplication.getparam("Detection_de_langue").equals("true") && buddyGPTApplication.nombreDeMotsCheck(result)) {
//                                    LanguageIdentifier languageIdentifier = LanguageIdentification.getClient();
//                                    languageIdentifier.identifyLanguage(result)
//                                            .addOnSuccessListener(
//                                                    new OnSuccessListener<String>() {
//                                                        @Override
//                                                        public void onSuccess(@Nullable String languageCode) {
//                                                            if (languageCode.equals("und")) {
//                                                                Log.i("TEAMGPT", "Can't identify language.");
//                                                                buddyGPTApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;"+result+";SPLIT;"+String.valueOf(numberOfQuestion));
//
//                                                            } else {
//                                                                Log.i("TEAMGPT", "Language: " + languageCode);
//                                                                buddyGPTApplication.setLanguageDetected(languageCode.trim());
//                                                                buddyGPTApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;"+result+";SPLIT;"+String.valueOf(numberOfQuestion));
//
//                                                            }
//                                                        }
//                                                    })
//                                            .addOnFailureListener(
//                                                    new OnFailureListener() {
//                                                        @Override
//                                                        public void onFailure(@NonNull Exception e) {
//                                                            Log.i("TEAMGPT", "addOnFailureListener " );
//                                                            buddyGPTApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;"+result+";SPLIT;"+String.valueOf(numberOfQuestion));
//
//                                                        }
//                                                    });
//                                }
//                                else{
//                                    Log.i("TEAMGPT", "Can't identify language.2");
//                                    buddyGPTApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + result + ";SPLIT;" + String.valueOf(numberOfQuestion));
//
//                                }
//
//                            }
//
//
//
//                            // Gérer le cas où la réponse est vide
//
//                        } catch (Exception e) {
//
//                            if (buddyGPTApplication.getLangue().getNom().equals(langueEn)) {
//                                buddyGPTApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + buddyGPTApplication.getString(R.string.chatBot_ERROR_en)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
//                            }
//                            else if (buddyGPTApplication.getLangue().getNom().equals(langueFr)) {
//                                buddyGPTApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + buddyGPTApplication.getString(R.string.chatBot_ERROR_fr)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
//                            }
//                            else if (buddyGPTApplication.getLangue().getNom().equals(langueEs)) {
//                                buddyGPTApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + buddyGPTApplication.getString(R.string.chatBot_ERROR_es)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
//                            }
//                            else if (buddyGPTApplication.getLangue().getNom().equals(langueDe)) {
//                                buddyGPTApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + buddyGPTApplication.getString(R.string.chatBot_ERROR_de)+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
//                            }
//                            else {
//                                buddyGPTApplication.getEnglishLanguageSelectedTranslator().translate(buddyGPTApplication.getString(R.string.chatBot_ERROR_en)).addOnSuccessListener(new OnSuccessListener<String>() {
//                                    @Override
//                                    public void onSuccess(String translatedText) {
//
//                                        buddyGPTApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;"+translatedText+";SPLIT;"+String.valueOf(numberOfQuestion)+";SPLIT;onError");
//                                    }
//
//                                }).addOnFailureListener(new OnFailureListener() {
//                                    @Override
//                                    public void onFailure(@NonNull Exception e) {
//                                        Log.e(TAG_NSTREAM,"translatedText exception  "+e);
//                                    }
//                                });
//
//                            }
//
//                        }
//
//                    }
//
//                    else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST){
//                        Log.i(TAG_NSTREAM, "run: notifyObservers INVALID_TEAMGPT_KEY 2");
//                        buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
//                    }
//                    // Now you have a list of Data objects
//
//                } catch (ProtocolException ex) {
//                    throw new RuntimeException(ex);
//                } catch (MalformedURLException ex) {
//                    throw new RuntimeException(ex);
//                } catch (JSONException ex) {
//                    throw new RuntimeException(ex);
//                } catch (UnsupportedEncodingException ex) {
//                    throw new RuntimeException(ex);
//                } catch (IOException ex) {
//                    throw new RuntimeException(ex);
//                }
//            }
//        }).start();
//    }

    public void sendPutRequestStream(String question, int numberOfQuestion) {
        isEmotionNeutral=false;
        String baseUrl = buddyGPTApplication.getparam("TeamGPT_url");
        String endpoint = buddyGPTApplication.getparam("TeamGPT_ApiEndpoint_Response"); // Endpoint dynamique.
        String gptKey = buddyGPTApplication.getparam("TeamGPT_Key"); // Clé API.
        String imeiDevice = buddyGPTApplication.getImeiRobot();

        Retrofit retrofit = RetrofitClient.getClient(baseUrl);
        ApiEndpointInterface apiService = retrofit.create(ApiEndpointInterface.class);

        // Préparez le corps de la requête.
        Request payload = new Request();
        payload.setTextInput(question);
        payload.setImeiIdDevice(imeiDevice);
        payload.setEmotion(buddyGPTApplication.getparam("switch_emotion").equals("true"));
        payload.setCommandes(true);
        payload.setLangue(buddyGPTApplication.getLangue().getLanguageCode().split("-")[0]);
        if (!buddyGPTApplication.getparam("session_id").isEmpty()) {
            payload.setSessionId(buddyGPTApplication.getparam("session_id"));
        }
        saveRequestToFile(payload);
        updateMessageHistory(question);
        // Envoyez la requête avec endpoint et clé.
        Log.i("HOU_DEBUG", "sendPutRequestStream: baseUrl "+baseUrl);
        Log.i("HOU_DEBUG", "sendPutRequestStream: endpoint "+endpoint);
        Log.i("HOU_DEBUG", "sendPutRequestStream: gpt key "+gptKey);
        Log.i("HOU_DEBUG", "sendPutRequestStream: payload session id "+payload.getSessionId());
        Call<ResponseBody> call = apiService.sendRequestTeamGPT(baseUrl+""+endpoint, gptKey, payload);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        buddyGPTApplication.setResponseTime(System.currentTimeMillis());
                        // Traitez la réponse en flux.
                        handleStreamingResponse(response.body().byteStream());
                    } catch (Exception e) {
                        e.printStackTrace();
                        handleError(numberOfQuestion);
                    }
                } else if (response.code() == 400) {
                    buddyGPTApplication.notifyObservers("INVALID_TEAMGPT_KEY");
                    buddyGPTApplication.setparam("INVALID_TEAMGPT_KEY", "TRUE");
                }
                else if (response.code() == 500) {
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
                handleError(numberOfQuestion);
            }
        });
    }
    private void updateMessageHistory(String question) {
        try {
            if(buddyGPTApplication.getparam("messages").equalsIgnoreCase(""))
                buddyGPTApplication.setparam("messages","[]");
            String jsonArrayString = buddyGPTApplication.getparam("messages");
             existingHistoryArray = new JSONArray(jsonArrayString);
            JSONObject newQuestionObject = new JSONObject();
            newQuestionObject.put("Question", question);
            existingHistoryArray.put(newQuestionObject);
            buddyGPTApplication.setparam("messages", existingHistoryArray.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void saveRequestToFile(Request payload) {
        String fileName = "TeamGPT-sent";
        File file = new File(Environment.getExternalStorageDirectory(), "BuddyGPT/" + fileName + ".json");
        try {
            if (file.exists() && file.isFile()) {
                file.delete();
            }
            FileWriter fileWriter = new FileWriter(file);
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            fileWriter.write(gson.toJson(payload));
            fileWriter.close();
            Log.v("Json_API", "File saved successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to handle error messages
    private void handleError(int numberOfQuestion) {
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
        buddyGPTApplication.notifyObservers("CHATBOTS_RETURN;SPLIT;speak;SPLIT;" + errorMessage + ";SPLIT;" + numberOfQuestion + ";SPLIT;onError");
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
            wordsHandler.postDelayed(wordsRunnable = new Runnable() {
                @Override
                public void run() {
                    buddyGPTApplication.notifyObservers("MODE_STREAM_TEXT;SPLIT;"+phraseToShow);
                    if (phraseToShow.length() == totalLength) {
                        isDisplayFinished = true;
                    }
                }
            }, i );
        }
        currentDisplayedText += phrase + " ";
    }
    private void handleStreamingResponse(InputStream response) {
        Log.i(TAG_STREAM, "handleStreamingResponse: HOU ");
        try {
            onStartStreaming();

            InputStream inputStream = response;
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String fileName = "TeamGPT-recv-stream";
            StringBuilder formattedContent = new StringBuilder();
            String line;
            Log.i(TAG_STREAM, "handleStreamingResponse: !isReset "+!isReset  );
            Log.i(TAG_STREAM, "handleStreamingResponse: !isError "+!isError  );
            while ((line = reader.readLine()) != null && reader.readLine().equalsIgnoreCase("") && !isReset && !isError) {
                try {

                    Log.w("HOU_DEBUG", "Received line: " + line);

                    if (line.trim().isEmpty()) {}
                    else if (line.contains("\"is_finished\": true,")) formattedContent.append(line);
                    else{
                        JSONObject jsonObject = new JSONObject(line.replace("data:", "").trim());
                        String formattedObject = jsonObject.toString(4);
                        formattedContent.append("data: ").append(formattedObject);
                        formattedContent.append("\n\n");
                    }

                    Log.w(TAG_STREAM, "Received line: " + line);
                    String jsonData = line.substring("data:".length()).trim();

                    JSONObject jsonObject = new JSONObject(jsonData);

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

                                    mainHandler2.post(() -> {
                                        buddyGPTApplication.notifyObservers("Session_ID_Changed");
                                    });
                                    buddyGPTApplication.setparam("session_id", sessionId);
                                    String jsonArrayString = buddyGPTApplication.getparam(historicMessages);
                                    existingHistoryArray = new JSONArray(jsonArrayString);
                                    JSONObject newSessionObject = new JSONObject();
                                    newSessionObject.put("Session", "New");
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
            Log.e(TAG_STREAM,"BuddySDK Exception  "+e);
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
                                        new OnSuccessListener<List<IdentifiedLanguage>>() {
                                            @Override
                                            public void onSuccess(List<IdentifiedLanguage> identifiedLanguages) {
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
                                            }
                                        })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        pronouncePhrase(phraseToPronounce);
                                    }
                                });
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
        phrasesHandler.postDelayed(phrasesRunnable = new Runnable() {
            @Override
            public void run() {
                processPhrasesWithDelay();
            }
        }, 50);
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
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(formattedContent);
            fileWriter.close();
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
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    errorMsg = translatedText;
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    errorMsg = buddyGPTApplication.getParamFromFile("chatBotServerNoResponce_en","BuddyGPT.properties");
                                }
                            });
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
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    errorMsg = translatedText;
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    errorMsg = buddyGPTApplication.getString(R.string.chatBotNoFound_en);
                                }
                            });
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
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    errorMsg = translatedText;
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    errorMsg = buddyGPTApplication.getString(R.string.chatBot_ERROR_en);
                                }
                            });
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
