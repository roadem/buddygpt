package com.robotique.aevaweb.buddygpt.utilis;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

public class ConfigurationFile {

    private static final String TAG = "BuddyGPT_ConfigurationFile";
    private static final int FILE_VERSION = 4; // upgrade this whenever you want to overwrite the file
    public static CustomProperties props = new CustomProperties();
    public static InputStream is = null;

    public static String createConfigurationFile(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            Log.i(TAG, "Le dossier 'BuddyGPT' existe déjà");
        }
        else {
            Log.i(TAG, "Création du dossier 'BuddyGPT' ...");
            directory.mkdir();
        }
        File configFile = new File(directory.getPath(), "BuddyGPT.properties");
        return WriteProperties(configFile);
    }


    public static CustomProperties loadproperties(File directory, String fileName, CustomProperties properties) {

        // First try loading from the current directory
        try {
            File f = new File(directory, fileName);
            is = new FileInputStream(f);

            if (is == null) {
                // Try loading from classpath
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                is = loader.getResourceAsStream(fileName);
            }
            // Try loading properties from the file (if found)
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                properties.load(reader);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading config file : "+e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error : "+e);
            }

        }
        return properties;
    }

    public static String WriteProperties(File configFile) {
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
                }
                else{
                    Log.i(TAG, "La version du fichier existant n'est pas à jour --> Ecraser le fichier.");
                    props = new CustomProperties();
                    initOrMajOrNone = "MAJ";
                }
            }

            // ---------------------------- VERSION DU FICHIER DE CONFIG -----------------
            //todo : upgrade FILE_VERSION whenever you want to overwrite the file !
            props.setProperty("fileVersion", String.valueOf(FILE_VERSION));


            // ---------------------------- PARAMETERS -----------------------------------
            props.addPropertyComment("TeamGPT_Key","");
            props.addPropertyComment("TeamGPT_Key", "TeamGPT Key  for communication with the server TeamGPT");
            setProperty("TeamGPT_Key","");
            props.addPropertyComment("TeamGPT_url", "");
            props.addPropertyComment("TeamGPT_url", "TeamGPT parameters");
            setProperty("TeamGPT_url", "https://chat.teamgpt.fr/api/");
            setProperty("TeamGPT_ApiEndpoint_Params","get_parameters");
            setProperty("TeamGPT_ApiEndpoint_Response","get-response");

            props.addPropertyComment("TeamGPT_ID_Device", "TeamGPT Device ID");
            setProperty("TeamGPT_ID_Device", "1");
            //props.addPropertyComment("Speech_To_Text_List","");
            //props.addPropertyComment("Speech_To_Text_List","Speech to Text : SpeechRecognizer/ApiGoogle/Whisper/Cerence");
            //setProperty("Speech_To_Text_List","SpeechRecognizer/ApiGoogle/Whisper/Cerence");
//            setProperty("Speech_To_Text","SpeechRecognizer");
            //setProperty("ApiGoogle_Key","AIzaSyAZDkir2v7wk1j-5rKrORCyOA_tlfSaa8E");

            //props.addPropertyComment("Change_STT","");
            //props.addPropertyComment("Change_STT","Possibility of changing the STT (Yes/No)");
            //setProperty("Change_STT","Yes");

            props.addPropertyComment("Android_Speech_minimum_length","");
            props.addPropertyComment("Android_Speech_minimum_length","Android Speech To Text config in seconds");
            setProperty("Android_Speech_minimum_length","15");
            setProperty("Android_Speech_silence_length","1");

            props.addPropertyComment("Language_Specification_STT", "");
            props.addPropertyComment("Language_Specification_STT", "Language_Specification_STT=Yes to specify the language parameter");
            setProperty("Language_Specification_STT","Yes");

//            props.addPropertyComment("Seuil_dBFS", "");
//            props.addPropertyComment("Seuil_dBFS","Decibel threshold to control the sensitivity of the STT Whisper");
//            setProperty("Seuil_dBFS","-30");
//            setProperty("Whisper_model","whisper-1");
//            setProperty("Whisper_prompt","Tu t'appelles Buddy. Voici les noms des applications que tu dois reconnaître : CarteBlanche, Parle&Cartes, BuddyEmoi, JeuDeGeste, TeleBuddy, BuddyPlayer, Accueil, FreezeDance, Consignes, DiscoverBuddy, BuddyCal, PhotoBooth, Spark, BuddyLab, BuddyDiag.");

            props.addPropertyComment("Text_To_Speech_List", "");
            props.addPropertyComment("Text_To_Speech_List", "Text to Speech : ReadSpeaker/Android");
            setProperty("Text_To_Speech_List","ReadSpeaker/Android");
            props.addPropertyComment("ReadSpeaker_pitch_fr","Pitch and speed for TTS");
            setProperty("ReadSpeaker_pitch_fr","130");
            setProperty("ReadSpeaker_pitch_en","130");
            setProperty("ReadSpeaker_speed_fr","100");
            setProperty("ReadSpeaker_speed_en","100");
            setProperty("TTS_Android_pitch","130");
            setProperty("TTS_Android_speed","100");
//            setProperty("TTS_ApiGoogle_pitch","130");
//            setProperty("TTS_ApiGoogle_speed","100");
//            props.addPropertyComment("TTS_ApiGoogle_Voice_Type", "Voice type : Standard/Wavenet");
//            setProperty("TTS_ApiGoogle_Voice_Type","Standard");

//            props.addPropertyComment("ChatBot","");
//            props.addPropertyComment("ChatBot","Default ChatBot : ChatGPT/CustomGPT");
//            setProperty("ChatBot","ChatGPT");



//            props.addPropertyComment("CustomGPT_API_Key","");
//            props.addPropertyComment("CustomGPT_API_Key", "CustomGPT key and project number");
//            setProperty("CustomGPT_API_Key","");
//            setProperty("CustomGPT_Project_ID","");
//
//            props.addPropertyComment("Change_chatBot","");
//            props.addPropertyComment("Change_chatBot","Possibility of changing the chatbot (Yes/No)");
//            setProperty("Change_chatBot","Yes");

            props.addPropertyComment("hotword_fr", "");
            props.addPropertyComment("hotword_fr", "List of hot words in French, English, Spanish and German");
            setProperty("hotword_fr", "ok buddy/hello/bonjour/salut/bonsoir/écoute/écoute-moi");
            setProperty("hotword_en","ok buddy/hello/good morning/good evening/listen/listen to me");
            setProperty("hotword_es","hola/hola buddy");
            setProperty("hotword_de","hallo/hallo buddy");

            props.addPropertyComment("Listening_time","");
            props.addPropertyComment("Listening_time","Maximum listening time (seconds) and number of successive listens");
            setProperty("Listening_time","10");
            setProperty("Number_listens","3");

            props.addPropertyComment("Language","");
            props.addPropertyComment("Language","Languages available FR/EN/ES/DE");
            setProperty("Language","FR");
            setProperty("Languages_available","Français _fr/Anglais _en/Espagnol _es/Allemand _de-DE/Italien _it-IT");

            props.addPropertyComment("Speech_volume","");
            props.addPropertyComment("Speech_volume","Speech volume (between 0 and 100)");
            setProperty("Speech_volume","100");

            props.addPropertyComment("Response_Timeout_in_seconds", "");
            props.addPropertyComment("Response_Timeout_in_seconds", "Waiting time for chatbot response and messages when exceeded");
            setProperty("Response_Timeout_in_seconds","8");
            setProperty("Message_Timeout_NotRespected_fr","Ça prend un peu de temps, la connexion est un peu lente./Aah! Internet n'est pas très rapide aujourd'hui/une petite seconde je connecte mes circuits");
            setProperty("Message_Timeout_NotRespected_en","It takes a little time, the connection is a bit slow./ohh! The internet is not very fast today/Just a moment, I'm connecting my circuits.");
            setProperty("Message_Timeout_NotRespected_es","Tarda un poco, la conexión es un poco lenta./¡ohh! Internet no es muy rápido hoy en día/Un momento, estoy conectando mis circuitos.");
            setProperty("Message_Timeout_NotRespected_de","Es dauert ein wenig, die Verbindung ist ein wenig langsam./ohh! Das Internet ist heute nicht sehr schnell/Einen Moment, ich schließe meine Schaltkreise.");

            props.addPropertyComment("Display_of_speech","");
            props.addPropertyComment("Display_of_speech","Speech display, Emotion activation, Language detection, Streaming mode, Activation stimulis, Commands and tracking (Yes/No)");
            setProperty("Display_of_speech","Yes");
            setProperty("Activation_of_emotions","Yes");
            setProperty("Language_detection","Yes");
//            setProperty("Stimulis","No");
//            //setProperty("Commands", "No");
//            setProperty("Tracking","No");

            props.addPropertyComment("Number_of_words","");
            props.addPropertyComment("Number_of_words","Minimum number of words in the response for activating language detection");
            setProperty("Number_of_words","3");

//            props.addPropertyComment("Pattern_End_Phrase","");
//            props.addPropertyComment("Pattern_End_Phrase","Set characters marking the end of a sentence using a regular expression for streaming TTS");
//            setProperty("Pattern_End_Phrase","[.;!?][ ]{1,3}|:\\s*|\\n");

//            props.addPropertyComment("Silence_time","");
//            props.addPropertyComment("Silence_time","Manage end of speech detection");
//            props.addPropertyComment("Silence_time","Silence time in seconds for STT ApiGoogle and Whisper");
//            setProperty("Silence_time","2");

            props.addPropertyComment("chatBotServerNoResponce_fr", "");
            props.addPropertyComment("chatBotServerNoResponce_fr", "Responses in case of API error");
            setProperty("chatBotServerNoResponce_fr","Je n’ai pas de réponse ");
            setProperty("chatBotServerNoResponce_en","I have no response ");
            setProperty("chatBotServerNoResponce_es","No tengo respuesta");
            setProperty("chatBotServerNoResponce_de","Ich habe keine Antwort");

            props.addPropertyComment("Chat_TextSize", "");
            props.addPropertyComment("Chat_TextSize", "Conversation window font size (between 20 and 50 px)");
            setProperty("Chat_TextSize","25");

            props.addPropertyComment("Mail_Sender", "");
            props.addPropertyComment("Mail_Sender", "Conversation sending email");
            setProperty("Mail_Sender","TeamChat@teamnet.fr");
            setProperty("Mail_Destination","");
            setProperty("Mail_Subject_fr","Dialogues TeamChat");
            setProperty("Mail_Subject_en","TeamChat Dialogs");
            setProperty("Message_mail_send_fr","Le mail a bien été envoyé !");
            setProperty("Message_mail_send_en","The email was sent successfully!");

            props.addPropertyComment("username", "");
            props.addPropertyComment("username", "Configuring the SMTP server");
            setProperty("username","5c6edd30875fd6cea1fdccabbd267328");
            setProperty("username_Password","1c15d6edab43d80dc53a30319c29364e");
            setProperty("mail.smtp.host", "in-v3.mailjet.com");
            setProperty("mail.smtp.port", "587");

//            props.addPropertyComment("BlueMic_Disponibility", "");
//            props.addPropertyComment("BlueMic_Disponibility", "BlueMic Availability (Yes/No)");
//            setProperty("BlueMic_Disponibility","No");
//
//            props.addPropertyComment("emotion_Temperature", "");
//            props.addPropertyComment("emotion_Temperature", "openAI settings for emotion detection");
//            setProperty("emotion_Temperature","0");
//            setProperty("emotion_Max_tokens","50");
//            setProperty("emotion_Model","gpt-3.5-turbo");
//            setProperty("prompt_fr"," Réponds toujours et seulement le nom de l'émotion ou du sentiment parmi 'joyeux, pensif, malade, amoureux, fatigué, attentif, surpris, grincheux, effrayé, en colère, triste’ et si tu ne le trouves pas, répond neutre. ");
//            setProperty("prompt_en","Answer always and only the name of the emotion or feeling among 'happy, thoughtful, sick, in love, tired, attentive, surprised, grumpy, scared, angry, sad' and if you can't find it, answer neutral.");
//            setProperty("BuddyFace_Happy","joyeux/happy");
//            setProperty("BuddyFace_Thinking","pensif/thoughtful");
//            setProperty("BuddyFace_Sick","malade/sick");
//            setProperty("BuddyFace_Love","amoureux/in love");
//            setProperty("BuddyFace_Tired","fatigué/tired");
//            setProperty("BuddyFace_Listening","attentif/attentive");
//            setProperty("BuddyFace_Surprised","surpris/suprised");
//            setProperty("BuddyFace_Grumpy","grincheux/grumpy");
//            setProperty("BuddyFace_Scared","effrayé/scared");
//            setProperty("BuddyFace_Angry","en colère/angry");
//            setProperty("BuddyFace_Sad","triste/sad");

//            props.addPropertyComment("touchLeftEye_Behavior","");
//            props.addPropertyComment("touchLeftEye_Behavior","List of behaviors that will be used for each eye touch or caress");
//            setProperty("touchLeftEye_Behavior","Growling");
//            setProperty("touchRightEye_Behavior","Angry");
//            setProperty("touchFace_Behavior","Happy");
//            setProperty("touchLeftHead_Behavior","LeftHead");
//            setProperty("touchRightHead_Behavior","RightHead");
//            setProperty("touchCenterHead_Behavior","CenterHead");
//            setProperty("touchLeftShoulder_Behavior","LeftShoulder");
//            setProperty("touchRightShoulder_Behavior","RightShoulder");
//            setProperty("touchHeart_Behavior","CenterHeart");

//            props.addPropertyComment("use_companion_when_stimulis_disabled","");
//            props.addPropertyComment("use_companion_when_stimulis_disabled","Parameter to configure whether to keep the Companion enabled when Stimulis are disabled (Yes/No).");
//            setProperty("use_companion_when_stimulis_disabled","No");

//            props.addPropertyComment("show_openAI_prices","");
//            props.addPropertyComment("show_openAI_prices","Consumption Parameters");
//            props.addPropertyComment("show_openAI_prices","Ability to display/manage openAI prices (Yes/No)");
//            setProperty("show_openAI_prices","No");
//            setProperty("Models_price","gpt-3.5-turbo_0.0005_0.0015/gpt-3.5-turbo-instruct_0.0015_0.002/gpt-4_0.03_0.06/gpt-4-32k_0.06_0.12/whisper-1_0.006_0");
//

//            setProperty("Model","gpt-3.5-turbo");
//            props.addPropertyComment("Temperature", "Model=gpt-4");
//            setProperty("Temperature",String.valueOf(0.5));
//
//            props.addPropertyComment("Max_Tokens_req", "");
//            props.addPropertyComment("Max_Tokens_req", "Max tokens : do not exceed the maximum number of tokens of the model");
//            props.addPropertyComment("Max_Tokens_req", "Maximum number of tokens for the request with history");
//            setProperty("Max_Tokens_req",String.valueOf(2000));
//            props.addPropertyComment("Max_Tokens_resp", "Maximum number of tokens in the response");
//            setProperty("Max_Tokens_resp",String.valueOf(2000));
//            props.addPropertyComment("header", "gpt-3.5-turbo : maximum_tokens = 4096");
//            props.addPropertyComment("header", "gpt-4 : maximum_tokens = 8192");
//
//            props.addPropertyComment("header", "");
//            props.addPropertyComment("header","Customization of dialogues");
//            setProperty("Cabecera","Eres un robot humanoide llamado BUDDY, un robot emocional fabricado por Blue Frog Robotics en París, responde con 20 palabras como máximo.");
//            setProperty("Kopfzeile","Du bist ein humanoider Roboter namens BUDDY. Du bist ein emotionaler Roboter, der von der Firma Blue Frog Robotics in Paris hergestellt wird, antwort mit maximal 20 Wörtern.");
//
//            props.addPropertyComment("CustomGPT_url","");
//            props.addPropertyComment("CustomGPT_url","CustomGPT parameters");
//            setProperty("CustomGPT_url", "https://app.customgpt.ai");
//            setProperty("CustomGPT_ApiEndpoint","/api/v1/projects/{project_id}/conversations/{session_id}/messages");
//            setProperty("CustomGPT_ApiEndpoint_SessionID","/api/v1/projects/{project_id}/conversations");
//            setProperty("CustomGPT_model","gpt-4-turbo");
//
//            props.addPropertyComment("CustomGPT_header", "");
//            props.addPropertyComment("CustomGPT_header","Customization of CustomGPT dialogues");
//            setProperty("CustomGPT_header","" );
//            setProperty("CustomGPT_entete","");
//            setProperty("CustomGPT_cabecera","");
//            setProperty("CustomGPT_kopfzeile","");

            //-------------------------- COMMAND PARAMETERS ---------------------------

//            props.addPropertyComment("COMMAND_Model","");
//            props.addPropertyComment("COMMAND_Model","Commands parameters");
//            setProperty( "COMMAND_Model", "gpt-4o" );
//            setProperty( "COMMAND_Temperature", "0" );
//
//            setProperty( "COMMAND_Prompt_fr", "Mappe la demande avec une phrase suivante et répond avec la commande correspondante entre <> sinon rien : Qu'est-ce que tu sais faire ? <CMD_NONE> /  Quel est ton niveau de batterie ? <CMD_BATTERIE> / Mets la température à zéro <CMD_TEMP 0> / Change le volume du son à 50 (entre 0 et 100) <CMD_SOUND 50>  /   Quel jour sommes-nous ? <CMD_DATE>  /   Quelle heure est-il ? <CMD_HOUR>    /   Avance sur 3 mètres <CMD_MOVE 3> / Tourne à droite de 30 degrés (entre -360 et 360) <CMD_TURN 30>  /   Baisse la tête au maximum (entre -45 et 35) <CMD_HEAD -45>  /   Regarde le plus à droite possible <CMD_LOOK 10> /   Arrête d’écouter <CMD_STOP> /   Quitte l’application <CMD_QUIT> /   Exécute une danse <CMD_DANCE>   /   Qu'est ce que tu vois <CMD_PHOTO> / Lance le comportement \"angry 02\" <CMD_BI angry 02>  /   Quelle est la météo à \"ville\" <CMD_METEO ville>  /   Mets-moi la radio \"RTL\" <CMD_RADIO RTL>   /   Connectes-toi sur \"Dorian\" <HEALYSA_CONNECT Dorian> /   Donne 3 portions de nourriture au chat <HEALYSA_FEEDCAT 3> /   Quel est mon rythme cardiaque <HEALYSA_HRV>    /   Prend ma tension <HEALYSA_BLOODP>  /   Récupère mon taux d'oxygène <HEALYSA_SPO2>  /   Fais mon check up <HEALYSA_CHECKUP>    /   Appelle \"Cyril\" <HEALYSA_CALL Cyril>  /   Où se trouve Nabila <HEALYSA_LOC Nabila>   /   Allume la lumière <SWITCHBOT_LIGHT On>  /   Montre-moi une image de \"chien rouge\" <CMD_IMAGE chien rouge>  /   Ferme l'image <CMD_CLOSE_IMAGE>  / Mets la langue Anglais (Français/Anglais/Espagnol/Allemand/Italien) <CMD_LANGUE Anglais> / Génère une musique de \"jazz avec une trompette\" <CMD_MUSIC jazz avec une trompette>. Commande particulière : Lance l'application X parmi (CarteBlanche, Parle&Cartes, BuddyEmoi, JeuDeGeste, TeleBuddy, BuddyPlayer, Accueil, FreezeDance, Consignes, DiscoverBuddy, BuddyCal, PhotoBoot, Spark, BuddyLab, ou BuddyDiag). Si X ne se trouve pas dans la liste mais qu'il existe une application avec un nom similaire, choisis celle qui correspond le mieux. Répond seulement avec <CMD_RUN X> sinon rien" );
//            setProperty( "COMMAND_Prompt_en", "Map the request to a following phrase and responds with the corresponding command between <> otherwise nothing: What can you do? <CMD_NONE>/ What is your battery level? <CMD_BATTERIE> / Set the temperature to zero <CMD_TEMP 0> / Change the sound volume to 50 (between 0 and 100) <CMD_SOUND 50> / What day is it? <CMD_DATE> / What time is it? <CMD_HOUR> / Move forward 3 meters <CMD_MOVE 3> / Turn right 30 degrees (between -360 and 360) <CMD_TURN 30> / Lower your head as far as possible (between -45 and 35) <CMD_HEAD -45> / Look as far to the right as possible <CMD_LOOK 10> / Stop listening <CMD_STOP> / Exit the application <CMD_QUIT> / Execute a dance <CMD_DANCE> / What do you see <CMD_PHOTO > / Launch the behavior \"angry 02\" <CMD_BI angry 02> / What is the weather in \"city\" <CMD_METEO city> / Start the \"RTL\" radio <CMD_RADIO RTL> / Connect to \"Dorian\" <HEALYSA_CONNECT Dorian> / Give the cat 3 portions of food <HEALYSA_FEEDCAT 3> / What's my pace heart rate <HEALYSA_HRV> / Take my blood pressure <HEALYSA_BLOODP> / Get my oxygen level <HEALYSA_SPO2> / Do my check-up <HEALYSA_CHECKUP> / Call \"Cyril\" <HEALYSA_CALL Cyril> / Where is Nabila <HEALYSA_LOC Nabila> / Turn on the light < SWITCHBOT_LIGHT On> / Show me an image of a \"red dog\" <CMD_IMAGE red dog> / Close the image <CMD_CLOSE_IMAGE> / Set the language to French (Français/Anglais/Espagnol/Allemand/Italien) <CMD_LANGUE Français> / Generate music \"jazz  with a trumpet\" <CMD_MUSIC jazz with a trumpet>.Specific Command: Launch application X from the following list (CarteBlanche, Parle&Cartes, BuddyEmoi, JeuDeGeste, TeleBuddy, BuddyPlayer, Accueil, FreezeDance, Consignes, DiscoverBuddy, BuddyCal, PhotoBooth, Spark, BuddyLab, or BuddyDiag). If X is not in the list but there is an application with a similar name, choose the one that best matches. Respond only with <CMD_RUN X> otherwise nothing" );
//
//            setProperty( "CMD_MUSIC_fr", "Ok, je vais lire une musique correspondant à votre demande // Voila" );
//            setProperty( "CMD_MUSIC_en", "Ok, I will play music corresponding to your request // Done." );
//
//            setProperty( "CMD_DATE_fr", "// Nous sommes aujourd'hui le [1]" );
//            setProperty( "CMD_DATE_en", "// Today is [1]" );
//
//            setProperty( "CMD_HOUR_fr", "// Il est [1]" );
//            setProperty( "CMD_HOUR_en", "// It's [1]" );
//
//            setProperty( "CMD_LANGUE_fr", "Je change la langue // La langue a été changé" );
//            setProperty( "CMD_LANGUE_en", "I'm changing the language // The language has been changed" );
//
//            setProperty( "CMD_TEMP_fr", "Je change la température // La température a été changé" );
//            setProperty( "CMD_TEMP_en", "I'm changing the temperature // The temperature has been changed" );
//
//            setProperty( "CMD_BATTERIE_fr", "Je vérifie le niveau de batterie // Mon niveau de batterie est de [1] pourcents" );
//            setProperty( "CMD_BATTERIE_en", "I am checking the battery level // My battery level is [1] percent" );
//
//            setProperty( "CMD_SOUND_fr", "Je modifie le volume // Le volume a été modifié" );
//            setProperty( "CMD_SOUND_en", "I am modifying the volume // The volume has been modified" );
//
//            setProperty( "CMD_MOVE_fr", "Ok, je me déplace // Voila je me suis déplacé" );
//            setProperty( "CMD_MOVE_en", "Ok, I'm moving // Here I've moved" );
//
//            setProperty( "CMD_TURN_fr", "Ok, je tourne // Voila j'ai tourné" );
//            setProperty( "CMD_TURN_en", "Ok, I'm turning // Here I've turned" );
//
//            setProperty( "CMD_HEAD_fr", "Ok, je bouge ma tête // Voila j'ai bougé ma tête" );
//            setProperty( "CMD_HEAD_en", "Ok, I'm moving my head // There I moved my head" );
//
//            setProperty( "CMD_STOP_fr", "Ok j'arrête // " );
//            setProperty( "CMD_STOP_en", "Ok I'll stop // " );
//
//            setProperty( "CMD_QUIT_fr", "D'accord, je vais fermer l'application. // " );
//            setProperty( "CMD_QUIT_en", "Ok I'll close the app // " );
//
//            setProperty( "CMD_RUN_fr", "D'accord, je vais lancer l'application [1]. // L'application [1] n'existe pas. " );
//            setProperty( "CMD_RUN_en", "Ok I'll run the app [1]. // Application [1] does not exist." );
//
//            setProperty( "CMD_DANCE_fr", "Ok je vais danser,  // T'as vu comment je danse bien" );
//            setProperty( "CMD_DANCE_en", "Ok I'm going to dance, // Have you seen how well I dance" );
//
//            setProperty( "CMD_METEO_fr", "Je vérifie la météo à [1] // Le temps à [1] est [2], il fait [3]°C" );
//            setProperty( "CMD_METEO_en", "I'm checking the weather at [1] // The weather at [1] is [2], it's [3]°C" );
//
//            setProperty( "CMD_RADIO_fr", "Je lance la radio [1] //" );
//            setProperty( "CMD_RADIO_en", "I'm starting the radio [1] //" );
//
//            setProperty( "SWITCHBOT_LIGHT_fr", "Je mets la lumière [1] // C'est fait" );
//            setProperty( "SWITCHBOT_LIGHT_en", "I'm turning [1] the light // Done." );
//
//            setProperty( "HEALYSA_CONNECT_fr", "Je te connecte à la plateforme Healysa // Tu es connecté" );
//            setProperty( "HEALYSA_CONNECT_en", "I am connecting you to the Healysa platform // You are connected" );
//
//            setProperty( "HEALYSA_HRV_fr", "Je prend ton rythme cardiaque // Ta fréquence cardiaque est de [1] bpm" );
//            setProperty( "HEALYSA_HRV_en", "I'm taking your heart rate // Your heart rate is [1] bpm" );
//
//            setProperty( "HEALYSA_BLOODP_fr", "Je prend ta tesion // Ta tension est de [1] [2]" );
//            setProperty( "HEALYSA_BLOODP_en", "I'm taking your blood pressure // Your blood pressure is [1] [2]" );
//
//            setProperty( "HEALYSA_SPO2_fr", "Je prend ton taux d'oxygène // Ton taux d'oxygène est de [1]" );
//            setProperty( "HEALYSA_SPO2_en", "I'm taking your oxygen level // Your oxygen level is [1]" );
//
//            setProperty( "HEALYSA_CHECKUP_fr", "Je fais ton check up // Ta fréquence cardiaque est de [1], ta tension est de [2] [3], ton taux d'oxygène est de [4]" );
//            setProperty( "HEALYSA_CHECKUP_en", "I'm doing your check up // Your heart rate is [1], your blood pressure is [2] [3], your oxygen level is [4]" );
//
//            setProperty( "HEALYSA_CALL_fr", "Je lance l'appel // Voila" );
//            setProperty( "HEALYSA_CALL_en", "I'll start the call // Done." );
//
//            setProperty( "HEALYSA_LOC_fr", "Un instant je cherche // Il est à [1]" );
//            setProperty( "HEALYSA_LOC_en", "Wait I'm searching // He's at [1]" );
//
//            setProperty( "HEALYSA_FEEDCAT_fr", "Ok je nourris le chat // Il est servi" );
//            setProperty( "HEALYSA_FEEDCAT_en", "Ok I'll feed the cat // He is served" );
//
//            setProperty( "CMD_IMAGE_fr", "Je vais vous montrer une image correspondant à votre demande // Voici l'image" );
//            setProperty( "CMD_IMAGE_en", "I will show you an image corresponding to your request // Here is the image" );
//
//            setProperty( "CMD_CLOSE_IMAGE_fr", "Ok je ferme l'image. // C'est bon" );
//            setProperty( "CMD_CLOSE_IMAGE_en", "Ok I'll close the image. // Done." );
//
//            setProperty("CMD_PHOTO_fr","Je vais prendre une photo // Voila");
//            setProperty("CMD_PHOTO_en","I will take a picture // Done.");
//
//            setProperty( "CMD_BI_fr", "Je lance le comportement [1] // C'est bon // Le fichier de comportement [1] est introuvable" );
//            setProperty( "CMD_BI_en", "I'm starting the behaviour [1] // Done. // Behavior file [1] could not be found" );
//
//
//            props.addPropertyComment("Image_URL","");
//            setProperty( "Image_URL", "https://api.openai.com/" );
//            setProperty( "Image_endpoint", "v1/images/generations" );
//            setProperty( "Image_model", "dall-e-2" );
//            setProperty( "Image_size", "512x512" );
//
//            props.addPropertyComment("Music_URL","");
//            setProperty( "Music_URL", "http://34.34.175.122:5000" );
//            setProperty( "Music_endpoint", "/generate_music" );
//            setProperty( "Music_model", "small" );
//            setProperty( "Music_duration", "20" );
//
//            props.addPropertyComment("Picture_Description_URL","");
//            setProperty("Picture_Description_URL","https://api.openai.com/");
//            setProperty("Picture_Description_model","gpt-4-vision-preview");
//            setProperty("Picture_Description_max_tokens","300");
//            setProperty("Picture_Description_question_fr","Décris moi cette image?");
//            setProperty("Picture_Description_question_en","Describe this image to me?");
//
//            props.addPropertyComment("BI_danse","");
//            setProperty( "BI_danse", "Dance01/Victory02" );
//
//            props.addPropertyComment("Healysa_URL_PROD","");
//            props.addPropertyComment("Healysa_URL_PROD","Healysa");
//            setProperty( "Healysa_URL_PROD", "https://care.healysa.io/" );
//            setProperty( "Healysa_mail", "" );
//            setProperty( "Healysa_password", "" );
//
//
//            //-------------------------- Tracking ---------------------------
//            props.addPropertyComment("TRACKING_Camera","");
//            props.addPropertyComment("TRACKING_Camera", "Tracking parameters");
//            props.addPropertyComment("TRACKING_Camera", "Enabling tracking with/without the camera, head, or body.");
//            setProperty("TRACKING_Camera","No");
//            setProperty("TRACKING_Head","No");
//            setProperty("TRACKING_Body","No");
//
//            props.addPropertyComment("TRACKING_watch","");
//            props.addPropertyComment("TRACKING_watch", "Tracking is performed as soon as the robot detects that the target is looking at it.");
//            setProperty("TRACKING_watch","Yes");
//            props.addPropertyComment("TRACKING_delay_nowatch", "The time delay in seconds for re-tracking and re-centering the gaze and head when the person is no longer looking at it depends on the specific operation of the tracking system being used.");
//            setProperty("TRACKING_delay_nowatch","10");
//            props.addPropertyComment("TRACKING_delay_notrack", "The time delay in seconds for re-tracking and re-centering the gaze and head after losing sight of the tracked person depends on the specific settings and capabilities of the tracking system in use.");
//            setProperty("TRACKING_delay_notrack","30");
//            props.addPropertyComment("TRACKING_listening", "Start listening when someone watchs");
//            setProperty("TRACKING_listening","Yes");
//            props.addPropertyComment("TRACKING_delay_startlisten", "Delay in seconds for listening when someone watchs");
//            setProperty("TRACKING_delay_startlisten","2");
//            props.addPropertyComment("TRACKING_delay_stoplisten", "Delay in seconds to stop listening when no one watchs");
//            setProperty("TRACKING_delay_stoplisten","3");
//            props.addPropertyComment("TRACKING_regard_center", "Time to refocus the pupils of the eyes if the person being monitored no longer looks towards the robot");
//            setProperty("TRACKING_regard_center","30");
//            props.addPropertyComment("TRACKING_timeout", "Time in seconds to close BuddyGPT if no person is tracked within this duration ");
//            setProperty("TRACKING_timeout","0");
//
//            props.addPropertyComment("TRACKING_Welcome","");
//            props.addPropertyComment("TRACKING_Welcome", "Welcome parameters");
//            props.addPropertyComment("TRACKING_Welcome", "Start Welcome");
//            setProperty("TRACKING_Welcome","Yes");
//            props.addPropertyComment("TRACKING_delay_welcome", "Delay in minuts to welcome after watching no one");
//            setProperty("TRACKING_delay_welcome","10");
//            props.addPropertyComment("TRACKING_duration_welcome", "the person looks at Buddy for 2 seconds to issue an invitation");
//            setProperty("TRACKING_duration_welcome","2");
//            props.addPropertyComment("TRACKING_welcome_FR", "Welcoming messages");
//            String[] TRACKING_INVITATIONS_FR = {"Bonjour, comment puis-je vous aider?/Bonjour ! En quoi puis-je rendre votre journée plus agréable?/Bonjour, que puis-je faire pour vous aujourd'hui?"};
//            setProperty("TRACKING_welcome_FR", Arrays.toString(TRACKING_INVITATIONS_FR));
//            String[] TRACKING_INVITATIONS_EN = {"Hello, how can I help you?/Hello! How can I make your day more enjoyable?/Hello, what can I do for you today?"};
//            setProperty("TRACKING_welcome_EN", Arrays.toString(TRACKING_INVITATIONS_EN));
//            props.addPropertyComment("TRACKING_welcome_CHATGPT", "Welcome message with ChatGPT");
//            setProperty("TRACKING_welcome_CHATGPT","Yes");
//            props.addPropertyComment("TRACKING_welcome_model", "Welcome ChatGPT model");
//            setProperty("TRACKING_welcome_model","gpt-4o");
//            props.addPropertyComment("TRACKING_welcome_temperature", "Welcome Temperature");
//            setProperty("TRACKING_welcome_temperature","0.5");
//            props.addPropertyComment("TRACKING_welcome_prompt_FR", "Welcome Prompt for ChatGPT");
//            setProperty("TRACKING_welcome_prompt_FR","dis une invitation au dialogue");
//            setProperty("TRACKING_welcome_prompt_EN","say an invitation to dialogue");
//            props.addPropertyComment("TRACKING_welcome_maxtoken", "Welcome max token");
//            setProperty("TRACKING_welcome_maxtoken","100");
//



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
