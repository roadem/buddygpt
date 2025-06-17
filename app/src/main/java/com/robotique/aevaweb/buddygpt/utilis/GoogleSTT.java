package com.robotique.aevaweb.buddygpt.utilis;

import android.os.AsyncTask;

import org.json.JSONException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;


public class GoogleSTT {


    private static final String ASR_URL_FORMAT ="https://www.google.com/speech-api/v2/recognize?xjerr=1&mqtt=chromium&lang=%s&key=%s";

    private URL mServiceUrl;

    public GoogleSTT(String key, Locale language) throws MalformedURLException {
        if (language != null) {
            mServiceUrl = new URL(String.format(language,ASR_URL_FORMAT,language.toString(),key));
        } else {
            mServiceUrl = new URL(String.format(ASR_URL_FORMAT, "", key));
        }
    }

    public void sendRequest(byte[] recording, int samplingRate, GoogleSTTCallbacks googleSTTCallbacks){
        new UploadMessageTask(googleSTTCallbacks).execute(recording, samplingRate);
    }

    private class UploadMessageTask extends AsyncTask<Object, Void, UploadMessageTask.STTResult> {

        private GoogleSTTCallbacks googleSTTCallbacks;

        UploadMessageTask(GoogleSTTCallbacks googleSTTCallbacks){
            this.googleSTTCallbacks = googleSTTCallbacks;
        }

        @Override
        protected STTResult doInBackground(Object... params) {
            byte[] buffer = (byte[]) params[0];
            int samplingRate = (int) params[1];
            HttpURLConnection con;
            try {
                con = createUrlRequest(samplingRate);
                appendData(con, buffer);
                con.connect();
                if(hasCorrectResponse(con)) return extractBestAnswer(con);
                else return new STTResult("ERROR_RECEIVING_DATA");

            } catch (IOException e) {
                e.printStackTrace();
                return new STTResult("IO_CONNECTION_ERROR");
            }
        }

        @Override
        protected void onPostExecute(STTResult s) {
            googleSTTCallbacks.onResponseError(s.status);
            googleSTTCallbacks.onResponse(s.result);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            googleSTTCallbacks.onRequestSent();
        }

        private HttpURLConnection createUrlRequest(int samplingRate) throws IOException {
            HttpURLConnection con = (HttpURLConnection) mServiceUrl.openConnection();
            con.setDefaultUseCaches(false);
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "audio/l16; rate=" + samplingRate);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setChunkedStreamingMode(0);
            con.setInstanceFollowRedirects(true);
            return con;
        }

        private void appendData(HttpURLConnection con, byte[] buffer) throws IOException {
            OutputStream os = con.getOutputStream();
            os.write(buffer);
            os.close();
        }

        private boolean hasCorrectResponse(HttpURLConnection conn) throws IOException {
            return conn.getResponseCode()==HttpURLConnection.HTTP_OK;
        }

        private STTResult extractBestAnswer(HttpURLConnection con){
            GoogleSTTResponseParser parser;
            try {
                parser = new GoogleSTTResponseParser(con.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
                return new STTResult("IO_CONNECTION_ERROR");
            } catch (JSONException e) {
                e.printStackTrace();
                return new STTResult("TOKEN_NOT_RECOGNIZED");
            }

            if(parser.getTranscript()==null) return new STTResult("TOKEN_NOT_RECOGNIZED");
            else if (parser.getConfidence() < 0.75f) return new STTResult("LOW_CONFIDENCE");
            else return new STTResult("NO_ERROR",parser.getTranscript());
        }

        class STTResult{

            String status;
            String result;

            STTResult(String status, String result){
                this.status = status;
                this.result=result;
            }

            STTResult(String status){
                this(status,null);
            }
        }

    }

}

