package unit.de.fungate.translate.core.services.translators;

import static org.junit.Assert.*;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import de.fungate.translate.core.models.SourceLanguage;
import de.fungate.translate.core.services.translators.PonsTranslator;
import de.fungate.translate.core.models.Translation;
import de.fungate.translate.core.services.Curler;
import fj.data.Either;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;


import java.io.IOException;

public class PonsTranslatorTest {
	
	private Curler curler;
	private PonsTranslator translator;
	
	@Before
    public void makeTranslator() {
        curler = mock(Curler.class);
        translator = new PonsTranslator(curler);
    }

	@Test
    public void shouldTranslateSingleWordFromGerman() throws IOException {
        final String term = "Riese";
        map(term, SourceLanguage.GERMAN);
        Iterable<Translation> german = german(term);
        assertThat(german, hasItems(
                new Translation( "giant","Riese"),
                new Translation( "a giant of a man","ein Riese von Mann"),
                new Translation( "red giant","roter Riese")
        ));
       
    }
	
	@Test
    public void shouldTranslateSingleWordFromEnglish() throws IOException {
        final String term = "wand";
        map(term, SourceLanguage.ENGLISH);
        Iterable<Translation> english = english(term);
        assertThat(english, hasItems(
                new Translation("wand", "Zauberstab"),
                new Translation("wand", "Mascara"),
                new Translation("wand", "Lesestift"),
                new Translation("magic wand", "Zauberstab"),
                new Translation("to wave one's magic wand", "den Zauberstab schwingen")
        ));
    }

    @Test
    public void shouldTranslateMultiWordFromGerman() throws IOException {
        final String term = "schlange stehen";
        map(term, SourceLanguage.GERMAN);
        Iterable<Translation> german = german(term);
        assertThat(german, hasItems(
                new Translation("to stand in a queue", "Schlange stehen"),
                new Translation("queue", "Schlange stehen"),
                new Translation("queueing", "Schlange stehen"),
                new Translation("to queue up", "Schlange stehen"),
                new Translation("to stand in line", "Schlange stehen")
        ));
    }
	
	
	private Iterable<Translation> english(String term) {
        return translator.translate(term, SourceLanguage.ENGLISH);
    }

    private Iterable<Translation> german(String term) {
        return translator.translate(term, SourceLanguage.GERMAN);
    }

    private void map(String term, SourceLanguage lang) throws IOException {
        when(curler.get(String.format(translator.buildURL(term, lang)))).thenReturn(Either.<String, Exception>left(
                readResource(String.format("%s.html", term))));
    }

    private String readResource(String name) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(String.format("/pons/%s", name)));
    }

}
