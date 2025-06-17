package com.robotique.aevaweb.buddygpt.utilis;

import android.media.MediaPlayer;

import java.io.IOException;

import darren.googlecloudtts.api.SynthesizeApi;
import darren.googlecloudtts.api.VoicesApi;
import darren.googlecloudtts.exception.ApiException;
import darren.googlecloudtts.model.VoicesList;
import darren.googlecloudtts.parameter.AudioConfig;
import darren.googlecloudtts.parameter.SynthesisInput;
import darren.googlecloudtts.parameter.VoiceSelectionParams;
import darren.googlecloudtts.request.SynthesizeRequest;
import darren.googlecloudtts.response.SynthesizeResponse;
import darren.googlecloudtts.response.VoicesResponse;

public class TtsGoogleC implements AutoCloseable{

    private SynthesizeApi mSynthesizeApi;
    private VoicesApi mVoicesApi;

    private VoiceSelectionParams mVoiceSelectionParams;
    private AudioConfig mAudioConfig;

    private MediaPlayer mMediaPlayer;

    private int mVoiceLength = -1;
    private TtsGoogleApiListener mTtsListener;

    public TtsGoogleC(SynthesizeApi synthesizeApi, VoicesApi voicesApi) {
        mSynthesizeApi = synthesizeApi;
        mVoicesApi = voicesApi;
    }

    public  TtsGoogleC setVoiceSelectionParams(VoiceSelectionParams voiceSelectionParams) {
        mVoiceSelectionParams = voiceSelectionParams;
        return this;
    }

    public TtsGoogleC setAudioConfig(AudioConfig audioConfig) {
        mAudioConfig = audioConfig;
        return this;
    }
    public void setTtsListener(TtsGoogleApiListener ttsListener) {
        mTtsListener = ttsListener;
    }


    public VoicesList load() {
        VoicesResponse response = mVoicesApi.get();
        VoicesList voicesList = new VoicesList();

        for (VoicesResponse.Voices voices : response.getVoices()) {
            String languageCode = voices.getLanguageCodes().get(0);
            VoiceSelectionParams params = new VoiceSelectionParams(
                    languageCode,
                    voices.getName(),
                    voices.getSsmlGender()
            );
            voicesList.add(languageCode, params);
        }

        return voicesList;
    }

    public void start(String text) {
        if (mVoiceSelectionParams == null) {
            throw new NullPointerException("You forget to setVoiceSelectionParams()");
        }

        if (mAudioConfig == null) {
            throw new NullPointerException("You forget to setAudioConfig()");
        }

        SynthesizeRequest request = new SynthesizeRequest(new SynthesisInput(text), mVoiceSelectionParams, mAudioConfig);

        try {
            SynthesizeResponse response = mSynthesizeApi.get(request);
            stop();
            String base64EncodedString = response.getAudioContent();
            String url = "data:audio/mp3;base64," + base64EncodedString;
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(url);
            mMediaPlayer.setOnPreparedListener(mp -> {
                if (mTtsListener != null) {
                    mTtsListener.onStart();
                }
                mp.start();
            });

            mMediaPlayer.setOnCompletionListener(mp -> {
                if (mTtsListener != null) {
                    mTtsListener.onDone();
                }
                mp.release();
            });

            mMediaPlayer.prepareAsync();
        } catch (Exception e) {
            if (mTtsListener != null) {
                mTtsListener.onError();
            }
            throw new ApiException(e);
        }
    }

    public void stop() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mVoiceLength = -1;
        }
    }

    public void pause() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mVoiceLength = mMediaPlayer.getCurrentPosition();
        }
    }

    public void resume() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying() && mVoiceLength != -1) {
            mMediaPlayer.seekTo(mVoiceLength);
            mMediaPlayer.start();
        }
    }


    public void close() {
        stop();
        if (mMediaPlayer !=null){
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

    }

}
