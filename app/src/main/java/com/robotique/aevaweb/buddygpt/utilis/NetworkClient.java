package com.robotique.aevaweb.buddygpt.utilis;

import com.robotique.aevaweb.buddygpt.application.BuddyGPTApplication;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class NetworkClient {
    public static Retrofit retrofit;

    /*
     This public static method will return Retrofit client
     anywhere in the application
     */
    public static Retrofit getRetrofitClient(BuddyGPTApplication buddyGPTApplication, String URL, Integer TimeOut){

        //If condition to ensure we don't create multiple retrofit instances in a single application
        if (retrofit==null || !retrofit.baseUrl().toString().equals(URL)) {
            //Defining the Retrofit using Builder
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .connectTimeout(1, TimeUnit.MINUTES)
                    .readTimeout(TimeOut, TimeUnit.SECONDS)
                    .writeTimeout(TimeOut, TimeUnit.SECONDS);

            Retrofit.Builder builder = new Retrofit.Builder()
                    .baseUrl(URL)
                    .callbackExecutor(Executors.newSingleThreadExecutor())
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create());
            builder.client(httpClient.build());
            retrofit = builder.build();
        }
        return retrofit;
    }
}
