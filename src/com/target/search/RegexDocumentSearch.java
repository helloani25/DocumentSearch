package com.target.search;


import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexDocumentSearch implements DocumentSearch {
    Map<String, String> fileMap;
    long msUsed = 0;

    public void setup() {
        long startTime = System.nanoTime();
        fileMap = DocumentSearchUtils.readDirectory(DocumentSearchConstants.DOCUMENT_SEARCH_DIRECTORY);
        long endTime = System.nanoTime();
         msUsed += TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

    }

    @Override
    public void getSearchResults(String phrase) {
        System.out.println("Search Results:");
        Map<Integer, List<String>> treeMap = new TreeMap<>(Collections.reverseOrder());
        long startTime = System.nanoTime();
        for (String filename:fileMap.keySet()) {
            int count = findMatch(fileMap.get(filename), phrase.trim());
            List<String> list = treeMap.getOrDefault(count, new ArrayList<>());
            list.add(filename);
            treeMap.put(count, list);
        }
        long endTime = System.nanoTime();
        msUsed += TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        printSearchResults(treeMap);
        System.out.println("Elapsed Time : " + msUsed+"ms");
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

    private void printSearchResults(Map<Integer, List<String>> treeMap) {
        for (int count: treeMap.keySet())
        if (count == 0) {
            for (String filename: treeMap.get(count))
                System.out.println(filename + " - no match");
        } else {
            for (String filename: treeMap.get(count))
                System.out.println(filename + " - matches");
        }
    }

}
