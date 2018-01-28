package com.ar.augmentedreality;

import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

/**
 * Created by Petri on 14.7.2017.
 */

public class VoiceRecognitionListener  implements RecognitionListener {

    private static VoiceRecognitionListener instance = null;

    IVoiceControl listener;
    static MainActivity main;

    public static VoiceRecognitionListener getInstance(MainActivity main2) {
        if (instance == null) {
            instance = new VoiceRecognitionListener(main2);
        }
        return instance;
    }

    private VoiceRecognitionListener(MainActivity main3) { this.main = main3; }

    public void setListener(IVoiceControl listener) {
        this.listener = listener;
    }

    public void processVoiceCommands(String... voiceCommands) {
        listener.processVoiceCommands(voiceCommands);
    }

    public void onResults(Bundle data) {
       // ArrayList matches = data.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
       // String[] commands = new String[matches.size()];
    }

    public void onBeginningOfSpeech() {
        System.out.println("Aloitetaan kuuntelu");
    }

    public void onBufferReceived(byte[] buffer) { }

    public void onEndOfSpeech() {
        System.out.println("Odotetaan tuloksia...");
        if (listener != null) {
            listener.restartListeningService();
        }
    }

    public void onError(int error) {
        if (listener != null) {
            System.out.println("Aloitetaan alusta !");
            listener.restartListeningService();
        }
        System.out.println("Kierrokset loppuu !");
    }
    public void onEvent(int eventType, Bundle params) { }

    public void onPartialResults(Bundle partialResults) {
        try {
            ArrayList data = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String word = (String) data.get(data.size() - 1);
            if(word != null && word != "" && word.length() != 0 && !word.contentEquals("") && !word.isEmpty()) {
                System.out.println(word);
                main.text(word);
            }
        } catch (Exception e) {
            System.out.println( e.toString());
            e.printStackTrace();
        }
    }

    public void onReadyForSpeech(Bundle params) {  }

    public void onRmsChanged(float rmsdB) { }
}
