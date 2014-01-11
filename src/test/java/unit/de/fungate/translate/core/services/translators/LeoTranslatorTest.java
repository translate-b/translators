package unit.de.fungate.translate.core.services.translators;

//import com.sun.org.apache.xpath.internal.SourceTree;
import de.fungate.translate.core.models.SourceLanguage;
import de.fungate.translate.core.models.Translation;
import de.fungate.translate.core.services.Curler;
//import de.fungate.translate.core.de.fungate.translate.web.services.services.pluginloaders.core.de.fungate.translate.web.services.services.Translator;
import de.fungate.translate.core.services.translators.LeoTranslator;

import fj.data.Either;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LeoTranslatorTest {

    private Curler curler;
	private LeoTranslator translator;

    @Before
    public void makeTranslator() {
        curler = mock(Curler.class);
        translator = new LeoTranslator(curler);
    }

    @Test
    public void shouldTranslateSingleWordFromGerman() throws IOException {
        final String term = "gigantisch";
        map(term, SourceLanguage.GERMAN);
        Iterable<Translation> german = german(term);
        assertThat(german, hasItems(
                new Translation("gargantuan", "gigantisch"),
                new Translation("gigantic", "gigantisch"),
                new Translation("humongous", "gigantisch"),
                new Translation("mammoth", "gigantisch"),
                new Translation("titanic", "gigantisch"),
                new Translation("to assume vast proportions", "gigantische Dimensionen annehmen")
        ));
    }

    @Test
    public void shouldTranslateSingleWordFromEnglish() throws IOException {
        final String term = "therefore";
        map(term, SourceLanguage.ENGLISH);
        Iterable<Translation> english = english(term);
        assertThat(english, hasItems(
                new Translation("therefore", "daher"),
                new Translation("therefore", "darum"),
                new Translation("therefore", "deshalb"),
                new Translation("therefore", "deswegen"),
                new Translation("therefore", "somit"),
                new Translation("therefore", "folglich"),
                new Translation("therefore", "mithin"),
                new Translation("therefore", "sonach"),
                new Translation("therefore", "demzufolge")
        ));
    }

    @Test
    public void shouldTranslateMultiWordFromGerman() throws IOException {
        final String term = "nach Hause gehen";
        map(term, SourceLanguage.GERMAN);
        Iterable<Translation> german = german(term);
        assertThat(german, hasItems(
                new Translation("to go home", "nach Hause gehen"),
                new Translation("to go from ... to ...", "von ... nach ... gehen"),
                new Translation("to get home", "nach Hause kommen"),
                new Translation("to walk so. home", "jmdn. nach Hause begleiten")
        ));
    }

    private Iterable<Translation> english(String term) {
        return translator.translate(term, SourceLanguage.ENGLISH);
    }

    private Iterable<Translation> german(String term) {
        return translator.translate(term, SourceLanguage.GERMAN);
    }

    private void map(String term, SourceLanguage lang) throws IOException {
         when(curler.getStream(String.format(translator.buildURL(term, lang)))).thenReturn(Either.<InputStream, Exception>left(
                    readResource(String.format(term + ".xml"))));
    }

    // ließt xml Dateien
    private InputStream readResource(String name) throws IOException {
        return getClass().getResourceAsStream(String.format("/leo/%s", name));
    }
}
