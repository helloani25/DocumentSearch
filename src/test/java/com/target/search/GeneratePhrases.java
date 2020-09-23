package com.target.search;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.*;

public class GeneratePhrases {

    private Map<String, String> fileMap;
    private Set<String> targetSet;
    private Set<String> stopWordSet;
    private String stopwordsRegex;

    public GeneratePhrases() {
        fileMap = DocumentSearchUtils.readDirectory(DocumentSearchConstants.DOCUMENT_SEARCH_DIRECTORY);
        targetSet = new HashSet<>();
    }

    public void generateRandomPhrases() {
        try {
            stopWordSet = getAllStopwords();
            generateWords();
            generateText();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void generateWords() throws IOException {
        for (String filename : fileMap.keySet()) {
            String[] words = fileMap.get(filename).split("\\s+");
            for (int i = 0; i < words.length; i++) {
                words[i] = words[i].strip();
                words[i] = words[i].replaceAll("\\[\\d\\]", "");
                words[i] = words[i].replaceAll("(\"|!|\\[|\\]|\\(|\\)|\\,|\\.|\\:|\\?|;)", "");
                targetSet.add(words[i]);
                if (words[i].matches("\\-")) {
                    String[] splitWords = words[i].split("\\-");
                    targetSet.addAll(Arrays.asList(splitWords));
                }
                targetSet.add(words[i]);
            }

            for (Iterator<String> i = targetSet.iterator(); i.hasNext(); ) {
                String word = i.next();
                if (stopWordSet.contains(word.toLowerCase()) || word.strip().length() == 0 || word.strip().equals("-")) {
                    i.remove();
                }
            }
            writeToFile();
        }
    }

    private void generateText() {
        for (String filename : fileMap.keySet()) {
            String[] sentences = fileMap.get(filename).split("\\.");
            for (int i = 0; i < sentences.length; i++) {
                sentences[i] = sentences[i].replaceAll("\\[\\d\\]", "");
                sentences[i] = sentences[i].replaceAll("(\"|!|\\[|\\]|\\(|\\)|\\,|\\.|\\:|\\?|;)", "");
                sentences[i] = sentences[i].strip();
                if (sentences[i].strip().length() == 0) {
                    continue;
                }
                for (int n = 2; n <= 5; n++) {
                    targetSet.addAll(ngrams(n, sentences[i]));
                }
                targetSet.addAll(Arrays.asList(sentences[i]));
            }
            writeToFile();
        }
    }

    public List<String> ngrams(int n, String str) {
        List<String> ngrams = new ArrayList<String>();
        String[] words = str.split(" ");
        for (int i = 0; i < words.length - n + 1; i++) {
            String phrase = concat(words, i, i + n);
            if (phrase.replaceAll(stopwordsRegex, "").strip().length() == 0
                    || phrase.replaceAll(stopwordsRegex, "").strip().equals("-")) {
                continue;
            }
            ngrams.add(phrase.strip());
        }
        return ngrams;
    }

    public String concat(String[] words, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++)
            sb.append((i > start ? " " : "") + words[i]);
        return sb.toString();
    }

    private void writeToFile() {
        // Create the set of options for appending to the file.
        Set<OpenOption> options = new HashSet<OpenOption>();
        options.add(APPEND);
        options.add(CREATE);
        // Create the custom permissions attribute.
        Set<PosixFilePermission> perms =
                PosixFilePermissions.fromString("rw-r-----");
        FileAttribute<Set<PosixFilePermission>> attr =
                PosixFilePermissions.asFileAttribute(perms);


        Path file = Paths.get("resources/generate_phrases.txt");

        try (SeekableByteChannel sbc =
                     Files.newByteChannel(file, options, attr)) {
            for (String phrase : targetSet) {
                // Convert the string to a ByteBuffer.
                byte data[] = phrase.concat("\n").getBytes();
                ByteBuffer bb = ByteBuffer.wrap(data);
                sbc.write(bb);
            }
        } catch (IOException x) {
            System.out.println("Exception thrown: " + x);
        }
    }

    public Set<String> getAllStopwords() throws IOException {
        List<String> stopwords = Files.readAllLines(Paths.get("src/main/resources/english_stopwords.txt"));
        stopwordsRegex = stopwords.stream().collect(Collectors.joining("|", "(?i)\\b(", ")\\b\\s?"));
        Set<String> stopWordsSet = new HashSet<>(stopwords);
        return stopWordsSet;
    }

}
