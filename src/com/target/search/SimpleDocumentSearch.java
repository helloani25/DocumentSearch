package com.target.search;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SimpleDocumentSearch implements DocumentSearch {
    private Map<String, List<String[]>> fileMapTokenzied;
    private long startTime, endTime;
    public SimpleDocumentSearch() {
        fileMapTokenzied = new HashMap<>();
    }

    public void setUp() {
        startTime = System.nanoTime();
        Map<Path, String> fileMap = DocumentSearchUtils.readDirectory(DocumentSearchConstants.DOCUMENT_SEARCH_DIRECTORY);
        tokenizeText(fileMap);
        filterAndTokenizeText(fileMap);
    }

    public void getSearchResults(String phrase) {
        System.out.println("Search Results:");
        for (String file: fileMapTokenzied.keySet()) {
            if (findMatch(fileMapTokenzied.get(file), phrase.trim())) {
                System.out.println(file + " - matches");
            } else {
                System.out.println(file + " - no match");
            }
        }
        endTime = System.nanoTime();
        long msUsed = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        System.out.println("Elapsed Time : " + msUsed+"ms");
    }

    boolean findMatch(List<String[]> content, String phrase) {
        phrase = phrase.replaceAll("(\"|!|\\[|\\]|\\,|\\.|:|\\?)","" );
        String[] tokens = phrase.split("\\s+");
        int j = 0;
        for (int i = 0; i < content.get(0).length; i++) {
            while ( j < tokens.length && (content.get(0)[i].compareTo(tokens[j]) == 0 || content.get(0)[i].compareToIgnoreCase(tokens[j]) == 0)) {
                i++;
                j++;
            }
            if (j == tokens.length) {
                return true;
            }
            j = 0;
        }
        for (int i = 0; i < content.get(1).length; i++) {
            while ( j < tokens.length && (content.get(1)[i].compareTo(tokens[j]) == 0 || content.get(1)[i].compareToIgnoreCase(tokens[j]) == 0)) {
                i++;
                j++;
            }
            if (j == tokens.length) {
                return true;
            }
            j = 0;
        }
        return false;
    }

    private void tokenizeText(Map<Path, String> fileMap) {
        for (Path file: fileMap.keySet()) {
            String[] words = fileMap.get(file).split("(\\s+)");
            for (int i = 0; i < words.length; i++) {
                words[i] = words[i].strip();
                words[i] = words[i].replaceAll("\\[\\d\\]", "");
                words[i] = words[i].replaceAll("(\"|!|\\[|\\]|\\(|\\)|\\,|\\.|\\:|\\?)","" );

            }
            List<String[]> list = new ArrayList<>(2);
            list.add(words);
            fileMapTokenzied.put(file.getFileName().toString(),list);
        }
    }

    private void filterAndTokenizeText(Map<Path, String> fileMap) {
        for (Path file: fileMap.keySet()) {
            String[] words = fileMap.get(file).split("(\\s+|\\-)");
            for (int i = 0; i < words.length; i++) {
                words[i]= words[i].strip();
                words[i] = words[i].replaceAll("(\"|!|\\[|\\]|\\,|\\.|:|\\?)","" );
            }
            List<String[]> list = fileMapTokenzied.get(file.getFileName().toString());
            list.add(words);
            fileMapTokenzied.put(file.getFileName().toString(),list);

        }
    }
}
