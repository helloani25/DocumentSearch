package com.target.search;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleDocumentSearch implements DocumentSearch {
    private Map<String, List<String[]>> fileMapTokenzied;

    public SimpleDocumentSearch() {
        fileMapTokenzied = new HashMap<>();
    }

    public void setUp() {
        Map<Path, String> fileMap = DocumentSearchUtils.readDirectory(DocumentSearchConstants.DOCUMENT_SEARCH_DIRECTORY);
        tokenizeText(fileMap);
        filterAndTokenizeText(fileMap);
    }

    public void getSearchResults(String phrase) {
        boolean search = false;

        for (String file: fileMapTokenzied.keySet()) {
            search = findMatch(fileMapTokenzied.get(file), phrase.trim());
            if (search) {
                System.out.println(file + " " + phrase + "matches");
            } else {
                System.out.println(file + " " + phrase + "no match");
            }
        }
    }

    boolean findMatch(List<String[]> content, String phrase) {
        String[] tokens = phrase.split("\\s+");
        int j = 0;
        for (int i = 0; i < content.get(0).length; i++) {
            String word = content.get(0)[i];
            while ( j < tokens.length && (word.compareTo(tokens[j]) == 0 || word.compareToIgnoreCase(tokens[j]) == 0)) {
                i++;
                j++;
            }
            if (j == tokens.length) {
                return true;
            }
            j = 0;
        }
        for (int i = 0; i < content.get(0).length; i++) {
            String word = content.get(0)[i];
            while ( j < tokens.length && (word.compareTo(tokens[j]) == 0 || word.compareToIgnoreCase(tokens[j]) == 0)) {
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
            if (fileMap.get(file) == null) {
                continue;
            } else {
                String[] words = fileMap.get(file).split("(\\s+)");
                for (int i = 0; i < words.length; i++) {
                    words[i] = words[i].strip();
                    words[i] = words[i].replaceAll("(\"|\\[|\\]|\\(|\\)|\\,|\\.|\\:|\\?)","" );

                }
                List<String[]> list = new ArrayList<>(2);
                list.add(words);
                fileMapTokenzied.put(file.getFileName().toString(),list);
            }
        }
    }

    private void filterAndTokenizeText(Map<Path, String> fileMap) {
        for (Path file: fileMap.keySet()) {
            if (fileMap.get(file) == null) {
                continue;
            } else {
                String[] words = fileMap.get(file).split("(\\s+|\\-)");
                for (int i = 0; i < words.length; i++) {
                    words[i]= words[i].strip();
                    words[i] = words[i].replaceAll("\\[\\d\\]", "");
                    words[i] = words[i].replaceAll("(\"|!|\\[|\\]|\\(|\\)|\\,|\\.|:|\\?)","" );
                }
                List<String[]> list = fileMapTokenzied.get(file.getFileName().toString());
                list.add(words);
                fileMapTokenzied.put(file.getFileName().toString(),list);
            }
        }
    }
}
