package com.robotique.aevaweb.buddygpt.utilis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

public class LoggingInterceptor implements Interceptor {
    private final Gson gson;
    private final String logDirectory;
    private final SimpleDateFormat dateFormat;

    public LoggingInterceptor(String logDirectory) {
        this.logDirectory = logDirectory;
        gson = new GsonBuilder().setPrettyPrinting().create();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();


        long startTime = System.nanoTime();

        // Create log file
        File logFile = createLogFile();
        boolean overwriteFile = shouldOverwriteFile(logFile);
        try (FileWriter writer = new FileWriter(logFile, !overwriteFile)) {
            // Log request information
            writer.append(">>>>>>>>>>>>>>>>>> Sending request\n\n");
            writer.append("URL: ").append(request.url().toString()).append("\n");
            writer.append("Method: ").append(request.method()).append("\n");
            writer.append("Headers: ").append(request.headers().toString()).append("\n");

            Request copy = request.newBuilder().build();
            try (Buffer requestBuffer = new Buffer()) {
                if (copy.body() != null) {
                    copy.body().writeTo(requestBuffer);
                    writer.append("Request body: ").append(formatJson(requestBuffer.readUtf8())).append("\n");
                }
            }
            Response response = chain.proceed(request);
            long endTime = System.nanoTime();

            // Log response information
            writer.append("\n\n\n<<<<<<<<<<<<<<<<<< Received response\n\n");
            writer.append("URL: ").append(response.request().url().toString()).append("\n");
            writer.append("Code: ").append(String.valueOf(response.code())).append("\n");
            writer.append("Headers: ").append(response.headers().toString()).append("\n");

            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                String responseBodyString = responseBody.string();
                writer.append("Response body: ").append(formatJson(responseBodyString)).append("\n");
                response = response.newBuilder()
                        .body(ResponseBody.create(responseBody.contentType(), responseBodyString.getBytes()))
                        .build();
            }

            writer.append("\nTime taken: ").append(String.valueOf((endTime - startTime) / 1e6)).append("ms\n");
            writer.append("\n\n\n****************************************************************************************************\n\n\n");

            writer.flush();


            return response;
        }
    }

private String formatJson(String json) {
        try {
            Object jsonObject = gson.fromJson(json, Object.class);
            return gson.toJson(jsonObject);
        } catch (Exception e) {
            return json;
        }
    }

    private File createLogFile() {
        File directory = new File(logDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = "LOG_ChatGPT.txt";
        return new File(directory, fileName);
    }



    private boolean shouldOverwriteFile(File file) {
        if (!file.exists()) {
            return true; // File doesn't exist, overwrite it
        }

        // Get the current date
        String currentDate = dateFormat.format(new Date());

        // Get the last modified date of the file
        String lastModifiedDate = dateFormat.format(new Date(file.lastModified()));

        // Check if the last modified date is different from the current date
        return !currentDate.equals(lastModifiedDate);
    }
}