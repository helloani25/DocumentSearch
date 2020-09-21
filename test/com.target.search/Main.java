package com.target.search;

import java.io.IOException;

public class Main {
    public static void main(String... args) {
        GeneratePhrases generatePhrases = new GeneratePhrases();
        try {
            generatePhrases.generateWords();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

