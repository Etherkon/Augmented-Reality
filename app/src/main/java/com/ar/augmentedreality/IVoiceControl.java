package com.ar.augmentedreality;

/**
 * Created by Petri on 13.7.2017.
 */
public interface IVoiceControl {
    public abstract void processVoiceCommands(String... voiceCommands);

    public void restartListeningService();
}