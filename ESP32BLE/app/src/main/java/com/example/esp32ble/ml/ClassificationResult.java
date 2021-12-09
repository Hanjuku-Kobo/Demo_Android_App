package com.example.esp32ble.ml;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.max;

public class ClassificationResult {

    private final Map<String, Float> classConfidences;

    public ClassificationResult() {
        classConfidences = new HashMap<>();
    }

    public Set<String> getAllClasses() {
        return classConfidences.keySet();
    }

    public float getClassConfidence(String className) {
        return classConfidences.containsKey(className) ? classConfidences.get(className) : 0;
    }

    public String getMaxConfidenceClass() {
        return max(
                classConfidences.entrySet(),
                (entry1, entry2) -> (int) (entry1.getValue() - entry2.getValue()))
                .getKey();
    }

    public void incrementClassConfidence(String className) {
        classConfidences.put(className,
                classConfidences.containsKey(className) ? classConfidences.get(className) + 1 : 1);
    }

    public void putClassConfidence(String className, float confidence) {
        classConfidences.put(className, confidence);
    }
}