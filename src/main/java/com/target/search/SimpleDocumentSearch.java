package com.target.search;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class SimpleDocumentSearch implements DocumentSearch {
    private final Map<String, String[]> fileMapTokenzied;
    public SimpleDocumentSearch() {
        fileMapTokenzied = new HashMap<>();
    }
    // Thread local variable containing each thread's ID

    public void setUp() {
        Map<String, String> fileMap = DocumentSearchUtils.readDirectory(DocumentSearchConstants.DOCUMENT_SEARCH_DIRECTORY);
        long startTime = System.nanoTime();
        tokenizeText(fileMap);
        double timeElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
    }

    public void getSearchResults(String phrase) {
        long startTime = System.nanoTime();
        TreeMap<Integer, List<String>> treeMap = new TreeMap<>(Collections.reverseOrder());
        for (String filename: fileMapTokenzied.keySet()) {
            int count = findMatch(fileMapTokenzied.get(filename), phrase.trim());
            List<String> list = treeMap.getOrDefault(count, new ArrayList<>());
            list.add(filename);
            treeMap.put(count, list);
        }
        long msUsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        printSearchResults(treeMap, phrase, msUsed);
    }

    private int findMatch(String[] content, String phrase) {
        phrase = phrase.replaceAll("(\"|!|\\[|\\]|\\(|\\)|\\,|\\.|\\:|\\?|;)","" );
        String[] tokens = phrase.split("(\\s+|\\-)");
        int j = 0, count = 0;
        for (int i = 0; i < content.length-tokens.length+1; i++) {
            while ( j < tokens.length && (content[i].compareTo(tokens[j]) == 0 || content[i].compareToIgnoreCase(tokens[j]) == 0)) {
                i++;
                j++;
            }
            if (j == tokens.length) {
                count++;
            }
            j = 0;
        }
        return count;
    }

    private void tokenizeText(Map<String, String> fileMap) {
        for (String filename: fileMap.keySet()) {
            String[] words = fileMap.get(filename).split("(\\s+|\\-)");
            for (int i = 0; i < words.length; i++) {
                words[i] = words[i].strip();
                words[i] = words[i].replaceAll("\\[\\d+\\]", "");
                words[i] = words[i].replaceAll("(\"|!|\\[|\\]|\\(|\\)|\\,|\\.|\\:|\\?|;)","" );
            }
            fileMapTokenzied.put(filename, words);
        }
    }

    private void printSearchResults(Map<Integer, List<String>> treeMap, String phrase, long msUsed) {
        StringBuilder sb = new StringBuilder();
        sb.append("Search Results:\n");
        for (int count: treeMap.keySet())
            if (count == 0) {
                for (String filename: treeMap.get(count))
                    sb.append(filename).append(" ").append(phrase).append(" - no match\n");
            } else {
                for (String filename: treeMap.get(count))
                    sb.append(filename).append(" ").append(phrase).append(" - matches\n");
            }
        sb.append("Elapsed Time : ").append(msUsed).append("ms\n");
        System.out.println(sb.toString());
    }

}
