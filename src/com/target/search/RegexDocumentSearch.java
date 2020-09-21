package com.target.search;


import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexDocumentSearch implements DocumentSearch {
    Map<Path, String> fileMap;


    public void setup() {
        fileMap = DocumentSearchUtils.readDirectory(DocumentSearchConstants.DOCUMENT_SEARCH_DIRECTORY);
    }

    @Override
    public void getSearchResults(String phrase) {
        System.out.println("Search Results:");
        long startTime = System.nanoTime();
        for (Path file:fileMap.keySet()) {
            if (findMatch(fileMap.get(file), phrase.trim())) {
                System.out.println(file.getFileName().toString() + " - matches");
            } else {
                System.out.println(file.getFileName().toString() + " - no match");
            }
        }
        long endTime = System.nanoTime();
        long msUsed = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        System.out.println("Elapsed Time : " + msUsed+"ms");
    }

    private boolean findMatch(String content, String phrase) {
        Pattern pattern = Pattern.compile("\\b("+phrase+")\\b");
        Matcher matcher = pattern.matcher(content);
        if(matcher.find()) {
            //get the MatchResult Object
            MatchResult result = matcher.toMatchResult();
            return true;
        }

        //Match with special characters removed
        content = content.replaceAll("\\[\\d\\]", "");
        content = content.replaceAll("(\"|!|\\[|\\]|\\(|\\)|\\,|\\.|:|\\?|\\-)","" );
        pattern = Pattern.compile("\\b("+phrase+")\\b", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(content);
        if(matcher.find()) {
            //get the MatchResult Object
            MatchResult result = matcher.toMatchResult();
            return true;
        }

        return false;
    }

}
