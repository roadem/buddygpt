package com.robotique.aevaweb.buddygpt.utilis;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient(String baseUrl) {
        if (retrofit == null) {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS) // Timeout de connexion
                    .readTimeout(30, TimeUnit.SECONDS)   // Timeout de lecture
                    .writeTimeout(30, TimeUnit.SECONDS)  // Timeout d'écriture
                    .retryOnConnectionFailure(true)      // Réessayer automatiquement en cas de défaillance
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient) // Associer le client personnalisé
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
