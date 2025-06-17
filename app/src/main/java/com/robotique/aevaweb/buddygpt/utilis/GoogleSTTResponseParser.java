package com.robotique.aevaweb.buddygpt.utilis;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class GoogleSTTResponseParser {

    private static final String RESULT = "result";
    private static final String RESULT_INDEX = "result_index";
    private static final String TRANSCRIPT = "transcript";
    private static final String CONFIDENCE = "confidence";
    private static final String ALTERNATIVE = "alternative";

    private double mConfidence = 1.0;
    private String mTranscript = null;

    private static String extractJsonString(InputStream in) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(in, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(inputStreamReader);
        String s;
        StringBuilder resultContent = new StringBuilder();
        while ((s = br.readLine()) != null) {
            resultContent.append(s);
        }
        return resultContent.toString();
    }

    /**
     * parse the response
     * 
     * @param response response stream
     * @throws JSONException exception throw if the response is malformed
     */
    GoogleSTTResponseParser(InputStream response) throws JSONException {
        String json = null;
        try {
            json = extractJsonString(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        parseJson(json);
    }

    private ArrayList<Result> extractAllResults(JSONArray alternative) throws JSONException {
        int nAlternative = alternative.length();
        ArrayList<Result> res = new ArrayList<>(nAlternative);
        for (int i = 0; i < nAlternative; i++) {
            JSONObject result = alternative.getJSONObject(i);
            String str = result.getString(TRANSCRIPT);
            double conf = 1.0;
            if (result.has(CONFIDENCE))
                conf = result.getDouble(CONFIDENCE);
            res.add(new Result(str, conf));

        }
        return res;
    }

    private void parseJson(String json) throws JSONException {

        JSONObject jsonResponse = new JSONObject(json);
        JSONArray jsonArrayResults = jsonResponse.getJSONArray(RESULT);
        int respIndex = jsonResponse.getInt(RESULT_INDEX);
        JSONObject jsonObjectAlternative = jsonArrayResults.getJSONObject(respIndex);
        JSONArray jsonArrayAlternative = jsonObjectAlternative.getJSONArray(ALTERNATIVE);

        ArrayList<Result> allResult = extractAllResults(jsonArrayAlternative);
        Collections.sort(allResult);
        Result bestResult = allResult.get(0);
        mConfidence = bestResult.confidence;
        mTranscript = bestResult.transcript;

    }

    /**
     * get the best transcript
     * 
     * @return transcript or null if the is not present
     */
    @Nullable
    String getTranscript() {
        return mTranscript;
    }

    /**
     * get the confidence value (between 0 and 1) of the best transcript
     * 
     * @return confidence value for the transcript
     */
    double getConfidence() {
        return mConfidence;
    }

    private static class Result implements Comparable<Result> {

        public String transcript;
        public double confidence;

        public Result(String t, double c) {
            transcript = t;
            confidence = c;
        }

        @Override
        public int compareTo(@NonNull Result result) {
            return Double.compare(result.confidence, confidence);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Result result = (Result) o;
            return Double.compare(confidence, result.confidence) == 0 && Objects.equals(transcript, result.transcript);
        }

        @Override
        public int hashCode() {
            return Objects.hash(transcript, confidence);
        }
    }

}
