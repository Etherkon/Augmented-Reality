package com.ar.augmentedreality;

/**
 * Created by Petri on 13.7.2017.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;

public abstract class ListeningActivity extends Activity implements IVoiceControl {

    protected SpeechRecognizer sr;
    protected Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
    }


    protected void stopListening() {
        if (sr != null) {
            sr.stopListening();
            sr.cancel();
            sr.destroy();
        }
        sr = null;
    }


    @Override
    public void finish() {
        stopListening();
        super.finish();
    }

    @Override
    protected void onStop() {
        stopListening();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (sr != null) {
            sr.stopListening();
            sr.cancel();
            sr.destroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if(sr!=null){
            sr.stopListening();
            sr.cancel();
            sr.destroy();
            System.out.println("Paussi !");
        }
        sr = null;

        super.onPause();
    }

    @Override
    public abstract void processVoiceCommands(String ... voiceCommands);

}