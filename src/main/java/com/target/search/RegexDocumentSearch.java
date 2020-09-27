package com.target.search;


import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexDocumentSearch implements DocumentSearch {
    Map<String, String> fileMap;

    public void setup() {
        fileMap = DocumentSearchUtils.readDirectory(DocumentSearchConstants.DOCUMENT_SEARCH_DIRECTORY);
    }

    @Override
    public void getSearchResults(String phrase) {
        Map<Integer, List<String>> treeMap = new TreeMap<>(Collections.reverseOrder());
        long startTime = System.nanoTime();
        for (String filename:fileMap.keySet()) {
            int count = findMatch(fileMap.get(filename), phrase.trim());
            List<String> list = treeMap.getOrDefault(count, new ArrayList<>());
            list.add(filename);
            treeMap.put(count, list);
        }
        double msUsed = (System.nanoTime() - startTime)/1000000.0;
        printSearchResults(treeMap, phrase, msUsed);
    }

    private int findMatch(String content, String phrase) {
        phrase = phrase.replaceAll("(\"|!|\\[|\\]|\\(|\\)|\\,|\\.|:|\\?|\\-|;)","");
        //Match with special characters removed
        content = content.replaceAll("(\"|!|\\[|\\]|\\(|\\)|\\,|\\.|:|\\?|\\-|;)","" );
        int count = 0;
        Pattern pattern = Pattern.compile("\\b("+phrase+")\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        while(matcher.find()) {
            count++;
        }
        return count;
    }

    private void printSearchResults(Map<Integer, List<String>> treeMap, String phrase, double msUsed) {
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
