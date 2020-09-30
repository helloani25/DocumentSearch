package com.target.search;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the search term:   ");
        String phrase = scanner.nextLine();
        System.out.println("1) String Match   2) Regular Expression    3) Indexed");
        String searchMethod = scanner.nextLine();
        DocumentSearch documentSearch;
        switch (searchMethod) {
            case "1":
            case "String Match":
                documentSearch = new SimpleDocumentSearch();
                documentSearch.setup();
                documentSearch.getSearchResults(phrase);
                break;
            case "2":
            case "Regular Expression":
                documentSearch = new RegexDocumentSearch();
                documentSearch.setup();
                documentSearch.getSearchResults(phrase);
                break;
            case "3":
            case "Indexed":
                documentSearch = new IndexedDocumentSearch();
                documentSearch.setup();
                documentSearch.getSearchResults(phrase);
                break;
            default:
                System.out.println("Invalid Entry");
                break;
        }
        scanner.close();
    }
}


