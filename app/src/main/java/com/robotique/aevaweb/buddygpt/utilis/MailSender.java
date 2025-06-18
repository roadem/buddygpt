package com.robotique.aevaweb.buddygpt.utilis;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.robotique.aevaweb.buddygpt.application.BuddyGPTApplication;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailSender extends AsyncTask<Void, Void, Void> {

    private static final String TAG = MailSender.class.getSimpleName();
    private static final String configFile = "BuddyGPT.properties";
    public boolean isMailSentSuccess = false;
    String username = "";
    String password = "";
    Properties props = null;
    String content = "";
    boolean isEmailAlerte = true;
    String mailTo = "";
    String subject = "";
    BuddyGPTApplication app;
    Activity activity;

    public MailSender(Activity activity, String content, String mailTo, String subject) {
        this.activity = activity;
        app = (BuddyGPTApplication) activity.getApplicationContext();
        this.content = content;
        isEmailAlerte = false;
        this.mailTo = mailTo;
        this.subject = subject;
    }

    public MailSender(String content, boolean isEmailAlerte) {

        this.content = content;
        this.isEmailAlerte = isEmailAlerte;

    }

    public void onMailSentSuccess() {
        isMailSentSuccess = true;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        username = app.getParamFromFile("username", configFile);
        password = app.getParamFromFile("username_Password", configFile);
        props = new Properties();

        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", app.getParamFromFile("mail.smtp.host", configFile));
        props.put("mail.smtp.port", app.getParamFromFile("mail.smtp.port", configFile));
        props.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");

    }

    @Override
    protected Void doInBackground(Void... voids) {
        sendMail(this.content, mailTo, subject);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        // Vérifier si l'e-mail a été envoyé avec succès
        if (isMailSentSuccess) {
            Log.e(TAG, "envoi succès");
            // L'e-mail a été envoyé avec succès
            // Ajoutez ici la logique appropriée pour votre application
        } else {
            Log.e(TAG, "envoi failed");
            // Il y a eu une erreur lors de l'envoi de l'e-mail
            // Ajoutez ici la logique appropriée pour gérer les erreurs
        }
    }

    public void sendMail(String msg, String mailTo, String subject) {

        String emailfrom = app.getParamFromFile("Mail_Sender", configFile);

        try {
            Session session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }
                    });
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailfrom));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(mailTo));
            message.setSubject(subject);

            message.setContent(msg, "text/html; charset=UTF-8");
            Transport.send(message);
            app.notifyObservers("mailSend");

        } catch (MessagingException e) {
            app.notifyObservers("ErrorSending");
            Log.e(TAG, "exception mail " + e);
        }
    }

}