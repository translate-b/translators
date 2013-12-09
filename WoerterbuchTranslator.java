package core.services.translators;

import com.google.inject.Inject;
import core.models.SourceLanguage;
import core.models.Translation;
import core.services.Curler;
import core.services.TranslationFactory;
import core.services.Translator;
import fj.data.Either;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * Translator for woerterbuch.info
 */
public class WoerterbuchTranslator implements Translator {

    public static final String URL_FORMAT = "http://www.woerterbuch.info/?query=%s&s=dict&l=en";
    private static final Logger LOG = Logger.getLogger(WoerterbuchTranslator.class);
    private final TranslationFactory translationFactory;
	private final Curler curler;

	@Inject
	public WoerterbuchTranslator(TranslationFactory translationFactory, Curler curler) {
		this.translationFactory = translationFactory;
		this.curler = curler;
	}

	@Override
	public Iterable<Translation> translate(String term, SourceLanguage source) {
		Either<String, Exception> content = curler.get(urlFor(term));
        if (content.isRight()) {
            // Get request failed and the right type is present. We can lookup the Exception
            LOG.error("GET request failed.", content.right().value());
            return Collections.emptyList();
        }

        // The left type is present, so the GET request was successful and
        // we can parse the inner string value
        Document doc = Jsoup.parse(content.left().value());

        DirectHitsParser parser = new DirectHitsParser(translationFactory, source, getProvider());
        for (Element e : doc.select("table table tr")) {
            if (parser.isFinished()) {
                break;
            }
            parser.step(e);
        }

        if (LOG.isTraceEnabled()) {
            for (Translation t : parser.getTranslations()) {
                LOG.trace(t);
            }
        }

        return parser.getTranslations();
	}

    private static String urlFor(String term) {
        return String.format(URL_FORMAT, term);
    }

    @Override
	public String getProvider() {
		return "woerterbuch.info";
	}

    /**
     * Finit state machine for parsing out the actual direct hits of the table
     */
    private static class DirectHitsParser {

        private final List<Translation> translations = new ArrayList<>();
        private final TranslationFactory translationFactory;
        private final SourceLanguage source;
        private final String provider;
        private State state = State.BEFORE;

        public List<Translation> getTranslations() {
            return translations;
        }

        public boolean isFinished() {
            return state == State.FINISHED;
        }

        public DirectHitsParser(TranslationFactory translationFactory, SourceLanguage source, String provider) {
            this.translationFactory = translationFactory;
            this.source = source;
            this.provider = provider;
        }

        public void step(Element tr) {
            switch (state) {
                case BEFORE:
                    Element header = tr.select("td.standard").first();
                    if (isSourceLangHeader(header, source)) {
                        state = State.IN_SECTION;
                    }
                    break;
                case IN_SECTION:
                    Element subHeader = tr.select("td.standard").first();
                    if (subHeader != null && subHeader.text().toLowerCase().startsWith("direkte")) {
                        state = State.IN_DIRECT_HITS;
                    }
                    break;
                case IN_DIRECT_HITS:
                    Elements columns = tr.select("td.hl");
                    if (columns.size() < 2) { // we hit the end of the section
                        state = State.FINISHED;
                        break;
                    } else {
                        // left column is english, right is german
                        String english = trimEnglish(columns.get(source == SourceLanguage.ENGLISH ? 0 : 1).text());
                        String german = columns.get(source == SourceLanguage.GERMAN ? 0 : 1).text();
                        translations.add(translationFactory.makeTranslation(english, german, provider, ""));
                    }
                    break;
                case FINISHED:
                    break;
            }
        }

        private static String trimEnglish(String term) {
            // ^to\s+ matches every "to " at the beginning of the string
            // \(\w+\.\) matches every "(Am.)" and similar string
            return term.replaceAll("(^to\\s+|\\(\\w+\\.\\))", "").trim();
        }

        private static boolean isSourceLangHeader(Element header, SourceLanguage lang) {
            if (header == null) {
                return false;
            }
            switch (lang) {
                case ENGLISH:
                    return header.text().toLowerCase().equals("englisch");
                case GERMAN:
                    return header.text().toLowerCase().equals("deutsch");
                default:
                    return false;
            }
        }

        private enum State {
            BEFORE,
            IN_SECTION,
            IN_DIRECT_HITS,
            FINISHED
        }
    }

}
