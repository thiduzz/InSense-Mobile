package com.apackage.utils;

import java.util.Comparator;

/**
 * Created by tschannerl on 26/10/17.
 */

public class Alternatives {
    float confidence;
    String transcript;

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public String getTranscript() {
        return transcript;
    }

    public String getTranscriptFilter(){
        int index = transcript.indexOf("Rua");
        if (index == -1){
            index = transcript.indexOf("rua");
        }

        if (index == -1){
            return transcript;
        }else{
            return transcript.substring(index, transcript.length());
        }
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public static Comparator<Alternatives> orderAlternatives = new Comparator<Alternatives>() {

        public int compare(Alternatives a1, Alternatives a2) {
            if (a2.getConfidence() > a1.getConfidence()){
                return 1;
            }else if (a2.getConfidence() < a1.getConfidence()){
                return -1;
            }else{
                return 0;
            }
        }
    };
}
