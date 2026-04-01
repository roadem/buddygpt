package com.robotique.aevaweb.buddygpt.utilis;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigurationFile {


    private static final String TAG = "BuddyGPT_ConfigurationFile";
    private static final int FILE_VERSION = 11; // upgrade this whenever you want to overwrite the file
    public static CustomProperties props = new CustomProperties();
    public static InputStream is = null;

    private ConfigurationFile() {
        throw new IllegalStateException("Utility class");
    }

    public static String createConfigurationFile(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            Log.i(TAG, "Le dossier 'BuddyGPT' existe déjà");
        } else {
            Log.i(TAG, "Création du dossier 'BuddyGPT' ...");
            directory.mkdir();
        }
        File configFile = new File(directory.getPath(), "BuddyGPT.properties");
        return writeProperties(configFile);
    }


    public static CustomProperties loadproperties(File directory, String fileName, CustomProperties properties) {

        // First try loading from the current directory
        try {
            File f = new File(directory, fileName);
            if (f.exists() && f.isFile()) {
                is = new FileInputStream(f);
            } else {
                // Try loading from classpath
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                is = loader.getResourceAsStream(fileName);
            }
            // Try loading properties from the file (if found)
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                properties.load(reader);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading config file : " + e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error : " + e);
            }

        }
        return properties;
    }

    public static String writeProperties(File configFile) {
        String initOrMajOrNone = "NONE";
        try {

            if (!configFile.exists()) {
                Log.i(TAG, "Création du fichier de configuration 'BuddyGPT.properties' ...");
                props = new CustomProperties();
                initOrMajOrNone = "INIT";
            } else {
                Log.i(TAG, "Le fichier de configuration 'BuddyGPT.properties' existe déjà >> charger les propriétés existantes");
                File directory = new File("/storage/emulated/0/", "BuddyGPT");
                String fileName = "BuddyGPT.properties";
                props = loadproperties(directory, fileName, new CustomProperties());
                int existingFileVersion = props.containsKey("fileVersion") ? Integer.parseInt(props.getProperty("fileVersion")) : 0;
                if (existingFileVersion == FILE_VERSION) {
                    Log.i(TAG, "Pas besoin d'écraser. La version du fichier existant est compatible.");
                    initOrMajOrNone = "NONE";
                    return initOrMajOrNone;
                } else {
                    Log.i(TAG, "La version du fichier existant n'est pas à jour --> Ecraser le fichier.");
                    props = new CustomProperties();
                    initOrMajOrNone = "MAJ";
                }
            }

            // ---------------------------- VERSION DU FICHIER DE CONFIG -----------------
            // upgrade FILE_VERSION whenever you want to overwrite the file !
            props.setProperty("fileVersion", String.valueOf(FILE_VERSION));


            // ---------------------------- PARAMETERS -----------------------------------
            props.addPropertyComment("TeamGPT_Key", "");
            props.addPropertyComment("TeamGPT_Key", "TeamGPT Key  for communication with the server TeamGPT");
            setProperty("TeamGPT_Key", "");
            props.addPropertyComment("TeamGPT_Base_url", "");
            props.addPropertyComment("TeamGPT_Base_url", "TeamGPT parameters");
            setProperty("TeamGPT_Base_url", "https://chat.teamgpt.fr");
            setProperty("TeamGPT_ApiEndpoint_Env", "/api/get_env_key");
            setProperty("TeamGPT_ApiEndpoint_Params", "/get_parameters");
            setProperty("TeamGPT_ApiEndpoint_Response", "/get_response");

            setProperty("TeamGPT_ID_Device", "");
            props.addPropertyComment("Speech_To_Text_List","");
            props.addPropertyComment("Speech_To_Text_List","Speech to Text : SpeechRecognizer/Cerence");
            setProperty("Speech_To_Text_List","SpeechRecognizer/Cerence");
            setProperty("Speech_To_Text","SpeechRecognizer");

            props.addPropertyComment("Cerence_Grammar_Name","Grammar file to use by the Cerence ASR engine (my_grammar_en.fcf / my_grammar_fr.fcf)");
            setProperty("Cerence_Grammar_Name_fr","companion_commands_fr.fcf");
            setProperty("Cerence_Grammar_Name_en","companion_commands_en.fcf");

            props.addPropertyComment("Cerence_Speech_confidence", "Minimum confidence score required to accept a Cerence grammar recognition result.");
            props.addPropertyComment("Cerence_Speech_confidence", "If the recognition score is below this threshold, the result is considered unreliable");
            props.addPropertyComment("Cerence_Speech_confidence", "and a fallback response is triggered.");
            props.addPropertyComment("Cerence_Speech_confidence", "Default value is set based on empirical testing (calm environment 5614, noisy environment 4950).");
            setProperty("Cerence_Speech_confidence", "4950");

            props.addPropertyComment("Cerence_Speech_failed", "Fallback responses used when the confidence threshold is not reached.");
            props.addPropertyComment("Cerence_Speech_failed", "Separate multiple responses with a slash \"/\".");
            props.addPropertyComment("Cerence_Speech_failed", "For any other language than French or English, the selected response will be");
            props.addPropertyComment("Cerence_Speech_failed", "automatically translated from English into the detected language.");
            setProperty("Cerence_Speech_failed", "Je n'ai pas compris/Peux-tu répéter/Désolé mais mon vocabulaire est limité");
            setProperty("Cerence_Speech_failed_en", "I didn't understand/Could you repeat that/Sorry but my vocabulary is limited");

            props.addPropertyComment("Change_STT","");
            props.addPropertyComment("Change_STT","Possibility of changing the STT (Yes/No)");
            setProperty("Change_STT","Yes");

            props.addPropertyComment("Android_Speech_minimum_length", "");
            props.addPropertyComment("Android_Speech_minimum_length", "Android Speech To Text config in seconds");
            setProperty("Android_Speech_minimum_length", "15");
            setProperty("Android_Speech_silence_length", "1");

            props.addPropertyComment("Language_Specification_STT", "");
            props.addPropertyComment("Language_Specification_STT", "Language_Specification_STT=Yes to specify the language parameter");
            setProperty("Language_Specification_STT", "Yes");
            props.addPropertyComment("Seuil_dBFS", "");
            props.addPropertyComment("Seuil_dBFS","Decibel threshold to control the sensitivity of the STT Whisper");
            setProperty("Seuil_dBFS","-30");

            props.addPropertyComment("Text_To_Speech_List", "");
            props.addPropertyComment("Text_To_Speech_List", "Text to Speech : ReadSpeaker/Android");
            setProperty("Text_To_Speech_List", "ReadSpeaker/Android");
            props.addPropertyComment("ReadSpeaker_pitch_fr", "Pitch and speed for TTS");
            setProperty("ReadSpeaker_pitch_fr", "130");
            setProperty("ReadSpeaker_pitch_en", "180");
            setProperty("ReadSpeaker_speed_fr", "100");
            setProperty("ReadSpeaker_speed_en", "100");
            setProperty("TTS_Android_pitch_fr", "115");
            setProperty("TTS_Android_pitch_en", "87");
            setProperty("TTS_Android_speed_fr", "100");
            setProperty("TTS_Android_speed_en", "70");

            props.addPropertyComment("hotword_fr", "");
            props.addPropertyComment("hotword_fr", "List of hot words in French, English, Spanish and German");
            setProperty("hotword_fr", "ok buddy/hello/bonjour/salut/bonsoir/écoute/écoute-moi");
            setProperty("hotword_en", "ok buddy/hello/good morning/good evening/listen/listen to me");
            setProperty("hotword_es", "hola/hola buddy");
            setProperty("hotword_de", "hallo/hallo buddy");

            props.addPropertyComment("Listening_time", "");
            props.addPropertyComment("Listening_time", "Maximum listening time (seconds) and number of successive listens");
            setProperty("Listening_time", "10");
            setProperty("Number_listens", "3");

            props.addPropertyComment("Language", "");
            props.addPropertyComment("Language", "Languages available Français/Anglais/Espagnol/Allemand/Italien/Japonais/Arabe/Chinois/Danois/Néerlandais/Norvégien");
            setProperty("Language", "Français");
            setProperty("Languages_available", "Français /Anglais /Espagnol /Allemand /Italien /Japonais /Arabe /Chinois /Danois /Néerlandais /Norvégien");
            setProperty("Language_Code_Used_In_Mlkit", "fr/en/es/de/it/ja/ar/zh/da/nl/no");
            setProperty("Language_Code_Used_In_STT_Android", "fr-FR/en-US/es-ES/de-DE/it-IT/ja-JP/ar-DZ/cmn-Hans-CN/da-DK/nl-NL/no-NO");
            setProperty("Language_Code_Used_In_TTS_Android", "fr-FR/en-US/es-ES/de-DE/it-IT/ja-JP/ar-DZ/zh-CN/da-DK/nl-NL/nb-NO");
            setProperty("Readspeaker_voices", "roxane/kate/manuel/max/elisa/show/amir/ / /guus/");
            setProperty("BuddyOS_1.3_voices", "fr:roxane/en:kate");
            setProperty("BuddyOS_1.4_voices", "fr:roxane/en:kate,mark,alice/ar:amir,yasmin/de:lena,max/es:lola,manuel/it:elisa/ja:show/nl:alex,guus");

            props.addPropertyComment("Speech_volume", "");
            props.addPropertyComment("Speech_volume", "Speech volume (between 0 and 100)");
            setProperty("Speech_volume", "100");

            props.addPropertyComment("Response_Timeout_in_seconds", "");
            props.addPropertyComment("Response_Timeout_in_seconds", "Waiting time for chatbot response and messages when exceeded");
            setProperty("Response_Timeout_in_seconds", "10");
            setProperty("Message_Timeout_NotRespected_fr", "Ça prend un peu de temps, la connexion est un peu lente./Aah! Internet n'est pas très rapide aujourd'hui/une petite seconde je connecte mes circuits");
            setProperty("Message_Timeout_NotRespected_en", "It takes a little time, the connection is a bit slow./ohh! The internet is not very fast today/Just a moment, I'm connecting my circuits.");
            setProperty("Message_Timeout_NotRespected_es", "Tarda un poco, la conexión es un poco lenta./¡ohh! Internet no es muy rápido hoy en día/Un momento, estoy conectando mis circuitos.");
            setProperty("Message_Timeout_NotRespected_de", "Es dauert ein wenig, die Verbindung ist ein wenig langsam./ohh! Das Internet ist heute nicht sehr schnell/Einen Moment, ich schließe meine Schaltkreise.");

            props.addPropertyComment("Display_of_speech", "");
            props.addPropertyComment("Display_of_speech", "Speech display, Emotion activation, Language detection ,Tracking");
            setProperty("Display_of_speech", "Yes");
            setProperty("Activation_of_emotions", "Yes");
            setProperty("Language_detection", "Yes");
            setProperty("Tracking","No");

            props.addPropertyComment("Number_of_words", "");
            props.addPropertyComment("Number_of_words", "Minimum number of words in the response for activating language detection");
            setProperty("Number_of_words", "5");
            setProperty("Detection_confidence_rate", "90");

            props.addPropertyComment("Silence_time","");
            props.addPropertyComment("Silence_time","Manage end of speech detection");
            props.addPropertyComment("Silence_time","Silence time in seconds for STT ApiGoogle and Whisper");
            setProperty("Silence_time","2");
            props.addPropertyComment("Processing_the_audio_sequence","Activate processing of the audio sequence for click on the mouth and volume reduction");
            setProperty("Processing_the_audio_sequence","Yes");
            props.addPropertyComment("Volume_reduction","");
            props.addPropertyComment("Volume_reduction","Percentage of volume reduction");
            setProperty("Volume_reduction","3");

            props.addPropertyComment("Duration_sound_level_checked","");
            props.addPropertyComment("Duration_sound_level_checked", "Duration in seconds, at which the sound level should be checked");
            setProperty("Duration_sound_level_checked","3");
            props.addPropertyComment("chatBotServerNoResponce_fr", "");
            props.addPropertyComment("chatBotServerNoResponce_fr", "Responses in case of API error");
            setProperty("chatBotServerNoResponce_fr", "Je n’ai pas de réponse ");
            setProperty("chatBotServerNoResponce_en", "I have no response ");
            setProperty("chatBotServerNoResponce_es", "No tengo respuesta");
            setProperty("chatBotServerNoResponce_de", "Ich habe keine Antwort");

            props.addPropertyComment("Chat_TextSize", "");
            props.addPropertyComment("Chat_TextSize", "Conversation window font size (between 20 and 50 px)");
            setProperty("Chat_TextSize", "25");

            props.addPropertyComment("Mail_Sender", "");
            props.addPropertyComment("Mail_Sender", "Conversation sending email");
            setProperty("Mail_Sender", "TeamChat@teamnet.fr");
            setProperty("Mail_Destination", "");
            setProperty("Message_mail_send_fr", "Le mail a bien été envoyé !");
            setProperty("Message_mail_send_en", "The email was sent successfully!");

            props.addPropertyComment("username", "");
            props.addPropertyComment("username", "Configuring the SMTP server");
            setProperty("username", "5c6edd30875fd6cea1fdccabbd267328");
            setProperty("username_Password", "1c15d6edab43d80dc53a30319c29364e");
            setProperty("mail.smtp.host", "in-v3.mailjet.com");
            setProperty("mail.smtp.port", "587");
            //-------------------------- Tracking ---------------------------
            props.addPropertyComment("TRACKING_Camera","");
            props.addPropertyComment("TRACKING_Camera", "Tracking parameters");
            props.addPropertyComment("TRACKING_Camera", "Enabling tracking with/without the camera.");
            setProperty("TRACKING_Camera","No");

            props.addPropertyComment("TRACKING_watch","");
            props.addPropertyComment("TRACKING_watch", "Tracking is performed as soon as the robot detects that the target is looking at it.");
            setProperty("TRACKING_watch","Yes");

            props.addPropertyComment("TRACKING_listening", "Start listening when someone watchs");
            setProperty("TRACKING_listening","Yes");

            props.addPropertyComment("TRACKING_delay_startlisten", "Delay in seconds for listening when someone watchs");
            setProperty("TRACKING_delay_startlisten","2");

            props.addPropertyComment("TRACKING_delay_stoplisten", "Delay in seconds to stop listening when no one watchs");
            setProperty("TRACKING_delay_stoplisten","3");

            FileOutputStream fileOut = new FileOutputStream(configFile);
            props.store(fileOut, "BuddyGPT configuration file");
            fileOut.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return initOrMajOrNone;
    }

    public static void setProperty(String key, String value) {
        // Always set the property, even if it already exists
        props.setProperty(key, value);
    }

}
