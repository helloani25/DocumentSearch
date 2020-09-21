import com.target.search.IndexedDocumentSearch;
import com.target.search.RegexDocumentSearch;
import com.target.search.SimpleDocumentSearch;

import java.io.IOException;

public class Main {

    public static void main(String... args) {
        String phrase = "faster-than-light";
        SimpleDocumentSearch simpleDocumentSearch = new SimpleDocumentSearch();
        simpleDocumentSearch.setUp();
        simpleDocumentSearch.getSearchResults(phrase);
        RegexDocumentSearch regexDocumentSearch = new RegexDocumentSearch();
        regexDocumentSearch.setup();
        regexDocumentSearch.getSearchResults(phrase);
        IndexedDocumentSearch indexedDocumentSearch = new IndexedDocumentSearch();

        try {
            indexedDocumentSearch.setup();
            indexedDocumentSearch.getSearchResults(phrase);
        } catch (IOException e) {
            System.err.println("IndexedSearch Failed " + e.getMessage());
            e.printStackTrace();
        }

    }

}
