package com.robotique.aevaweb.buddygpt.models;

import com.google.gson.annotations.SerializedName;

public class Parameters {

    @SerializedName("IdCompte")
    private String idCompte;

    @SerializedName("IMEI_ID_Device")
    private String imeiIdDevice;

    @SerializedName("TTS")
    private String tts;

    @SerializedName("STT")
    private String stt;

    @SerializedName("Header")
    private String header;

    @SerializedName("Entete")
    private String entete;

    @SerializedName("Stream_mode")
    private String streamMode;



    @SerializedName("email_support")
    private String emailSupport;

    @SerializedName("Mail_sender")
    private String mailSender;

    @SerializedName("Mail_Subject_fr")
    private String mailSubjectFr;

    @SerializedName("Mail_Subject_en")
    private String mailSubjectEn;

    @SerializedName("Message_mail_send_fr")
    private String messageMailSendFr;

    @SerializedName("Message_mail_send_en")
    private String messageMailSendEn;

    @SerializedName("username")
    private String username;

    @SerializedName("username_Password")
    private String usernamePassword;

    @SerializedName("Smtp_port")
    private int smtpPort;

    @SerializedName("Smtp_host")
    private String smtpHost;

    @SerializedName("show_price")
    private boolean showPrice;

    @SerializedName("selected_chatbot")
    private String selectedChatbot;

    @SerializedName("isEncryptionRequired")
    private boolean isEncryptionRequired;

    @SerializedName("allow_conversation_storage")
    private boolean allowConversationStorage;

    @SerializedName("project_id_TeamRAG")
    private String projectIdTeamRag;

    @SerializedName("Password_mail_sender")
    private String passwordMailSender;

    @SerializedName("Response_format_fr")
    private String responseFormatFr;

    @SerializedName("Response_format_en")
    private String responseFormatEn;

    @SerializedName("Response_filter")
    private String responseFilter;

    @SerializedName("Language_detection")
    private String languageDetection;

    @SerializedName("Number_of_words")
    private int numberOfWords;

    @SerializedName("Detection_confidence_rate")
    private double detectionConfidenceRate;

    @SerializedName("TeamRAG_Header")
    private String teamRagHeader;

    @SerializedName("TeamRAG_Entete")
    private String teamRagEntete;

    @SerializedName("Modele_STT")
    private String modeleStt;

    @SerializedName("NomCompte")
    private String nomCompte;

    @SerializedName("Email")
    private String email;

    @SerializedName("TeamGPT_Key")
    private String teamGptKey;

    @SerializedName("teamgpt_version")
    private String teamGptVersion;

    @SerializedName("chatbot_model")
    private String chatbotModel;

    // --------- GETTERS & SETTERS ---------

    public String getIdCompte() { return idCompte; }
    public void setIdCompte(String idCompte) { this.idCompte = idCompte; }

    public String getImeiIdDevice() { return imeiIdDevice; }
    public void setImeiIdDevice(String imeiIdDevice) { this.imeiIdDevice = imeiIdDevice; }

    public String getTts() { return tts; }
    public void setTts(String tts) { this.tts = tts; }

    public String getStt() { return stt; }
    public void setStt(String stt) { this.stt = stt; }

    public String getHeader() { return header; }
    public void setHeader(String header) { this.header = header; }

    public String getEntete() { return entete; }
    public void setEntete(String entete) { this.entete = entete; }

    public String getStreamMode() { return streamMode; }
    public void setStreamMode(String streamMode) { this.streamMode = streamMode; }

    public String getEmailSupport() {
        return emailSupport;
    }

    public void setEmailSupport(String emailSupport) {
        this.emailSupport = emailSupport;
    }
    public String getMailSender() { return mailSender; }
    public void setMailSender(String mailSender) { this.mailSender = mailSender; }

    public String getMailSubjectFr() { return mailSubjectFr; }
    public void setMailSubjectFr(String mailSubjectFr) { this.mailSubjectFr = mailSubjectFr; }

    public String getMailSubjectEn() { return mailSubjectEn; }
    public void setMailSubjectEn(String mailSubjectEn) { this.mailSubjectEn = mailSubjectEn; }

    public String getMessageMailSendFr() { return messageMailSendFr; }
    public void setMessageMailSendFr(String messageMailSendFr) { this.messageMailSendFr = messageMailSendFr; }

    public String getMessageMailSendEn() { return messageMailSendEn; }
    public void setMessageMailSendEn(String messageMailSendEn) { this.messageMailSendEn = messageMailSendEn; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getUsernamePassword() { return usernamePassword; }
    public void setUsernamePassword(String usernamePassword) { this.usernamePassword = usernamePassword; }

    public int getSmtpPort() { return smtpPort; }
    public void setSmtpPort(int smtpPort) { this.smtpPort = smtpPort; }

    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }

    public boolean isShowPrice() { return showPrice; }
    public void setShowPrice(boolean showPrice) { this.showPrice = showPrice; }

    public String getSelectedChatbot() { return selectedChatbot; }
    public void setSelectedChatbot(String selectedChatbot) { this.selectedChatbot = selectedChatbot; }

    public boolean isEncryptionRequired() { return isEncryptionRequired; }
    public void setEncryptionRequired(boolean encryptionRequired) { isEncryptionRequired = encryptionRequired; }

    public boolean isAllowConversationStorage() { return allowConversationStorage; }
    public void setAllowConversationStorage(boolean allowConversationStorage) { this.allowConversationStorage = allowConversationStorage; }

    public String getProjectIdTeamRag() { return projectIdTeamRag; }
    public void setProjectIdTeamRag(String projectIdTeamRag) { this.projectIdTeamRag = projectIdTeamRag; }

    public String getPasswordMailSender() { return passwordMailSender; }
    public void setPasswordMailSender(String passwordMailSender) { this.passwordMailSender = passwordMailSender; }

    public String getResponseFormatFr() { return responseFormatFr; }
    public void setResponseFormatFr(String responseFormatFr) { this.responseFormatFr = responseFormatFr; }

    public String getResponseFormatEn() { return responseFormatEn; }
    public void setResponseFormatEn(String responseFormatEn) { this.responseFormatEn = responseFormatEn; }

    public String getResponseFilter() { return responseFilter; }
    public void setResponseFilter(String responseFilter) { this.responseFilter = responseFilter; }

    public String getLanguageDetection() { return languageDetection; }
    public void setLanguageDetection(String languageDetection) { this.languageDetection = languageDetection; }

    public int getNumberOfWords() { return numberOfWords; }
    public void setNumberOfWords(int numberOfWords) { this.numberOfWords = numberOfWords; }

    public double getDetectionConfidenceRate() { return detectionConfidenceRate; }
    public void setDetectionConfidenceRate(double detectionConfidenceRate) { this.detectionConfidenceRate = detectionConfidenceRate; }

    public String getTeamRagHeader() { return teamRagHeader; }
    public void setTeamRagHeader(String teamRagHeader) { this.teamRagHeader = teamRagHeader; }

    public String getTeamRagEntete() { return teamRagEntete; }
    public void setTeamRagEntete(String teamRagEntete) { this.teamRagEntete = teamRagEntete; }

    public String getModeleStt() { return modeleStt; }
    public void setModeleStt(String modeleStt) { this.modeleStt = modeleStt; }

    public String getNomCompte() { return nomCompte; }
    public void setNomCompte(String nomCompte) { this.nomCompte = nomCompte; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTeamGptKey() { return teamGptKey; }
    public void setTeamGptKey(String teamGptKey) { this.teamGptKey = teamGptKey; }

    public String getTeamGptVersion() { return teamGptVersion; }
    public void setTeamGptVersion(String teamGptVersion) { this.teamGptVersion = teamGptVersion; }

    public String getChatbotModel() { return chatbotModel; }
    public void setChatbotModel(String chatbotModel) { this.chatbotModel = chatbotModel; }
}
