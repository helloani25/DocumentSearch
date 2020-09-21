import com.target.search.IndexedDocumentSearch;
import com.target.search.RegexDocumentSearch;
import com.target.search.SimpleDocumentSearch;

public class Main {

    public static void main(String... args) {
        String phrase = "EVE";
        SimpleDocumentSearch simpleDocumentSearch = new SimpleDocumentSearch();
        simpleDocumentSearch.setUp();
        simpleDocumentSearch.getSearchResults(phrase);
        RegexDocumentSearch regexDocumentSearch = new RegexDocumentSearch();
        regexDocumentSearch.setup();
        regexDocumentSearch.getSearchResults(phrase);
        IndexedDocumentSearch indexedDocumentSearch = new IndexedDocumentSearch();
        indexedDocumentSearch.setup();
        indexedDocumentSearch.getSearchResults(phrase);


    }

}
