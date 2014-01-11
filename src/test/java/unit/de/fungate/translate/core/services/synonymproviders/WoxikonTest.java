package unit.de.fungate.translate.core.services.synonymproviders;

import de.fungate.translate.core.models.SourceLanguage;
import de.fungate.translate.core.services.Curler;
import de.fungate.translate.core.services.synonymproviders.Woxikon;
import fj.data.Either;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URLEncoder;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WoxikonTest {

    private Curler curler;
	private Woxikon synonymprovider;

    @Before
    public void makeTranslator() {
        curler = mock(Curler.class);
        synonymprovider = new Woxikon(curler);
    }

    @Test
    public void shouldGetSynonymsFromSingleEnglishWord() throws IOException {
        final String term = "zoom";
        map(term);
        Iterable<String> synonyms = synonymprovider.getSynonyms(term, SourceLanguage.ENGLISH);
        
        assertThat(synonyms, hasItems("whir","ascend"));
    }

    @Test
    public void shouldGetSynonymsFromMultiEnglishWord() throws IOException {
        final String term = "break down";
        map(term);
        Iterable<String> synonyms = synonymprovider.getSynonyms(term, SourceLanguage.ENGLISH);
        
        assertThat(synonyms, hasItems("burst out","sob","rage","blubber","choke","fail","miscarry","founder","dissolve"
        ));
    }
//
//    @Test
//    public void shouldTranslateMultiWordFromGerman() throws IOException {
//        final String term = "schlange stehen";
//        map(term);
//        Iterable<String> synonyms = synonymprovider.getSynonyms(term);
//        
//        assertThat(synonyms, hasItems(
//                new Translation("line up", "Schlange stehen"),
//                new Translation("queue", "Schlange stehen"),
//                new Translation("queue up", "Schlange stehen"),
//                new Translation("stand in a queue", "Schlange stehen"),
//                new Translation("stand in line", "Schlange stehen")
//        ));
//    }



    private void map(String term) throws IOException {
        when(curler.get("http://synonyme.woxikon.de/synonyme-englisch/"+URLEncoder.encode(term, "UTF-8") + ".php")).thenReturn(Either.<String, Exception>left(
                readResource(String.format("%s.html", term))));
    }

    private String readResource(String name) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(String.format("/woxikon/%s", name)));
    }
}
