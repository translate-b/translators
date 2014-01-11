package unit.de.fungate.translate.core.services.translators;

import static org.junit.Assert.*;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.fungate.translate.core.models.SourceLanguage;
import de.fungate.translate.core.models.Translation;
import de.fungate.translate.core.services.Curler;
import fj.data.Either;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import java.net.URLEncoder;

import de.fungate.translate.core.services.Translator;
import de.fungate.translate.core.services.translators.DictccTranslator;

import java.io.IOException;
import java.net.URISyntaxException;

public class DictccTranslatorTest {

    private Curler curler;
	private Translator translator;
	private static final String EN = "http://en-de.dict.cc/?s=";
	private static final String DE = "http://de-en.dict.cc/?s=";


    @Before
    public void makeTranslator() {
        curler = mock(Curler.class);
        translator = new DictccTranslator(curler);
    }

    @Test
    public void shouldTranslateSingleWordFromGerman() throws IOException {
        final String term = "wand";
        final SourceLanguage source = SourceLanguage.GERMAN;
        map(term, source);
        Iterable<Translation> german = german(term);
        assertThat(german, hasItems(
                new Translation("wall", "Wand"),
                new Translation("wriggled", "wand"),
                new Translation("wreathed", "wand"),
                new Translation("coiled", "wand"),
                new Translation("wound", "wand")
        ));
    }

    @Test
    public void shouldTranslateSingleWordFromEnglish() throws IOException {
        final String term = "wand";
        final SourceLanguage source = SourceLanguage.ENGLISH;
        map(term, source);
        Iterable<Translation> english = english(term);
        assertThat(english, hasItems(
    		   new Translation("to wand", "abtasten"),
               new Translation("wand", "Zauberstab"),
               new Translation("wand", "Stab"),
               new Translation("wand", "Lesestift"),
               new Translation("wand", "Kelle")
        ));
    }

    @Test
    public void shouldTranslateMultiWordFromEnglish() throws IOException, URISyntaxException {
    	final String term = "apple pie";
        final SourceLanguage source = SourceLanguage.ENGLISH;
        //Using URL encode for a correct mapping
        map(URLEncoder.encode(term, "UTF-8"), source);
        Iterable<Translation> english = english(term);
        assertThat(english, hasItems(
                new Translation("apple pie", "Apfelkuchen"),
                new Translation("applepie bed", "verk\u00FCrztes Bett"),
                new Translation("idiom in applepie order", "in bester Ordnung"),
                new Translation("idiom in applepie order", "in sch\u00F6nster Ordnung"),
                new Translation("idiom as American as apple pie", "durch und durch amerikanisch")
        ));
    }

    private Iterable<Translation> english(String term) {
        return translator.translate(term, SourceLanguage.ENGLISH);
    }

    private Iterable<Translation> german(String term) {
        return translator.translate(term, SourceLanguage.GERMAN);
    }

    private void map(String term, SourceLanguage source) throws IOException {
    	if(source == SourceLanguage.GERMAN){
	        when(curler.get(DE+term)).thenReturn(Either.<String, Exception>left(
	                readResource(String.format(term+"DE.html", term))));
    	} else {
    		 when(curler.get(EN+term)).thenReturn(Either.<String, Exception>left(
 	                readResource(String.format(term+"EN.html", term))));
    	}
    }

    private String readResource(String name) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(String.format("/dictcc/%s", name)));
    }
}
