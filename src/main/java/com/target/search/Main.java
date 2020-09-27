package com.target.search;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the search term:   ");
        String phrase = scanner.nextLine();
        System.out.println("1) String Match   2) Regular Expression    3) Indexed");
        String searchMethod = scanner.nextLine();
        switch (searchMethod) {
            case "1":
            case "String Match":
                SimpleDocumentSearch simpleDocumentSearch = new SimpleDocumentSearch();
                simpleDocumentSearch.setUp();
                simpleDocumentSearch.getSearchResults(phrase);
                break;
            case "2":
            case "Regular Expression":
                RegexDocumentSearch regexDocumentSearch = new RegexDocumentSearch();
                regexDocumentSearch.setup();
                regexDocumentSearch.getSearchResults(phrase);
                break;
            case "3":
            case "Indexed":
                IndexedDocumentSearch indexedDocumentSearch = new IndexedDocumentSearch();
                try {
                    indexedDocumentSearch.setup();
                    indexedDocumentSearch.getSearchResults(phrase);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.out.println("Invalid Entry");
                break;
        }
        scanner.close();
    }
}


