package com.target.search;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE;

public class GeneratePhrases {

    private Map<String, String> fileMap;

    public GeneratePhrases() {
        fileMap = DocumentSearchUtils.readDirectory(DocumentSearchConstants.DOCUMENT_SEARCH_DIRECTORY);
    }

    public void generateWords() throws IOException {
        Set<String> targetSet = new HashSet<>();

        for (String filename: fileMap.keySet()) {
            String[] words = fileMap.get(filename).split("\\s+");
            for (int i = 0; i < words.length; i++) {
                words[i] = words[i].strip();
                words[i] = words[i].replaceAll("\\[\\d\\]", "");
                words[i] = words[i].replaceAll("(\"|!|\\[|\\]|\\(|\\)|\\,|\\.|\\:|\\?)","" );
                targetSet.add(words[i]);
                if (words[i].matches("\\-")) {
                    String[] splitwords = words[i].split("\\-");
                    targetSet.addAll(Arrays.asList(splitwords));
                }
                targetSet.add(words[i]);
            }

            Set<String> stopWordSet = getAllStopwords();
            for (Iterator<String> i = targetSet.iterator(); i.hasNext();) {
                String word = i.next();
                if (stopWordSet.contains(word.toLowerCase())) {
                    i.remove();
                }
            }
            String[] finalWords = new String[targetSet.size()];
            targetSet.toArray(finalWords);
            writeToFile(String.join("\n", finalWords));
        }
    }

    public void generateText() {
        for (String filename : fileMap.keySet()) {
            String[] sentences = fileMap.get(filename).split("\\.");
            for (int i = 0; i < sentences.length; i++) {
                sentences[i].replaceAll("\\[\\d\\]", "");
                String[] phrases = sentences[i].split("\\,");
            }
        }
    }

    private void writeToFile(String rawData) {
        String s = rawData;
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of("resources/generate_words.txt"))) {
            writer.write(s, 0, s.length());
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }

    public Set<String> getAllStopwords() throws IOException {
        List<String> stopwords = Files.readAllLines(Paths.get("resources/english_stopwords.txt"));
        Set<String> stopWordsSet = new HashSet<>(stopwords);
        return stopWordsSet;
    }

}
