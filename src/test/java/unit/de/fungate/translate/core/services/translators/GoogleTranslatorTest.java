package unit.de.fungate.translate.core.services.translators;

import de.fungate.translate.core.models.SourceLanguage;
import de.fungate.translate.core.services.Curler;
import de.fungate.translate.core.services.Translator;
import de.fungate.translate.core.services.translators.GoogleTranslator;
import de.fungate.translate.core.models.Translation;
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

public class GoogleTranslatorTest {

	private Curler curler;
	private Translator translator;

	@Before
	public void makeTranslator() {
		curler = mock(Curler.class);
		translator = new GoogleTranslator(curler);
	}

	@Test
	public void shouldTranslateSingleWordFromGerman() throws IOException {
		final String term = "wand";
		map(term, "de");
		Iterable<Translation> german = german(term);
		assertThat(
				german,
				hasItems(new Translation("septum", "Wand"), new Translation(
						"wall", "Wand")));
	}

	@Test
	public void shouldTranslateSingleWordFromEnglish() throws IOException {
		final String term = "wand";
		map(term, "en");
		Iterable<Translation> english = english(term);
		assertThat(
				english,
				hasItems(new Translation("wand", "Zauberstab"),
						new Translation("wand", "Amtsstab"), new Translation(
								"wand", "Lesestift"), new Translation("wand",
								"Stab")));
	}

	@Test
	public void shouldTranslateMultiWordFromGerman() throws IOException {
		final String term = "schlange stehen";
		map(term, "de");
		Iterable<Translation> german = german(term);
		assertThat(
				german,
				hasItems(new Translation("to queue", "Schlange stehen"),
						new Translation("to stand in line", "Schlange stehen")));
	}

	private Iterable<Translation> english(String term) {
		return translator.translate(term, SourceLanguage.ENGLISH);
	}

	private Iterable<Translation> german(String term) {
		return translator.translate(term, SourceLanguage.GERMAN);
	}

	private void map(String term, String sourcelanguage) throws IOException {

		String url = "";

		if (sourcelanguage.equals("de")) {
			url = "http://translate.google.de/translate_a/t?client=t&sl=de&tl=en&hl=de&sc=2&ie=UTF-8&oe=UTF-8&ssel=0&tsel=0&q=";
		} else if (sourcelanguage.equals("en")) {
			url = "http://translate.google.de/translate_a/t?client=t&sl=en&tl=de&hl=de&sc=2&ie=UTF-8&oe=UTF-8&ssel=0&tsel=0&q=";
		}

		when(curler.get(url + URLEncoder.encode(term, "UTF-8"))).thenReturn(
				Either.<String, Exception> left(readResource(term + "_"
						+ sourcelanguage)));
	}

	private String readResource(String name) throws IOException {
		return IOUtils.toString(getClass().getResourceAsStream(
				String.format("/google/%s", name)));
	}
}
