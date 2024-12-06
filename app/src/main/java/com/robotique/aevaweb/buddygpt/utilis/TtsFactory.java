package com.robotique.aevaweb.buddygpt.utilis;

import darren.googlecloudtts.GoogleCloudAPIConfig;
import darren.googlecloudtts.api.SynthesizeApi;
import darren.googlecloudtts.api.SynthesizeApiImpl;
import darren.googlecloudtts.api.VoicesApi;
import darren.googlecloudtts.api.VoicesApiImpl;

public class TtsFactory {


    public static TtsGoogleC create(String apiKey) {
        GoogleCloudAPIConfig config = new GoogleCloudAPIConfig(apiKey);
        return create(config);
    }

    public static TtsGoogleC create(GoogleCloudAPIConfig config) {
        SynthesizeApi synthesizeApi = new SynthesizeApiImpl(config);
        VoicesApi voicesApi = new VoicesApiImpl(config);
        return new TtsGoogleC(synthesizeApi, voicesApi);
    }

}