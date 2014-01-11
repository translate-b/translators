package unit.de.fungate.translate.core.services.translators;

import de.fungate.translate.core.models.SourceLanguage;
import de.fungate.translate.core.services.Curler;
import de.fungate.translate.core.services.Translator;
import de.fungate.translate.core.services.translators.WoerterbuchTranslator;
import de.fungate.translate.core.models.Translation;
import fj.data.Either;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WoerterbuchTranslatorTest {

    private Curler curler;
	private Translator translator;

    @Before
    public void makeTranslator() {
        curler = mock(Curler.class);
        translator = new WoerterbuchTranslator(curler);
    }

    @Test
    public void shouldTranslateSingleWordFromGerman() throws IOException {
        final String term = "wand";
        map(term);
        Iterable<Translation> german = german(term);
        assertThat(german, hasItems(
                new Translation("coiled", "wand"),
                new Translation("wreathed", "wand"),
                new Translation("wriggled", "wand"),
                new Translation("septum", "Wand"),
                new Translation("wall", "Wand")
        ));
    }

    @Test
    public void shouldTranslateSingleWordFromEnglish() throws IOException {
        final String term = "wand";
        map(term);
        Iterable<Translation> english = english(term);
        assertThat(english, hasItems(
                new Translation("wand", "Kelle"),
                new Translation("wand", "Lesestift"),
                new Translation("wand", "Stab")
        ));
    }

    @Test
    public void shouldTranslateMultiWordFromGerman() throws IOException {
        final String term = "schlange stehen";
        map(term);
        Iterable<Translation> german = german(term);
        assertThat(german, hasItems(
                new Translation("line up", "Schlange stehen"),
                new Translation("queue", "Schlange stehen"),
                new Translation("queue up", "Schlange stehen"),
                new Translation("stand in a queue", "Schlange stehen"),
                new Translation("stand in line", "Schlange stehen")
        ));
    }

    private Iterable<Translation> english(String term) {
        return translator.translate(term, SourceLanguage.ENGLISH);
    }

    private Iterable<Translation> german(String term) {
        return translator.translate(term, SourceLanguage.GERMAN);
    }

    private void map(String term) throws IOException {
        when(curler.get(WoerterbuchTranslator.urlFor(term))).thenReturn(Either.<String, Exception>left(
                readResource(String.format("%s.html", term))));
    }

    private String readResource(String name) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(String.format("/woerterbuch/%s", name)));
    }
}
