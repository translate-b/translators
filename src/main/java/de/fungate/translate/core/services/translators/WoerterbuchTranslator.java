package de.fungate.translate.core.services.translators;

import com.google.inject.Inject;
import de.fungate.translate.core.models.SourceLanguage;
import de.fungate.translate.core.models.Translation;
import de.fungate.translate.core.services.Curler;
import de.fungate.translate.core.services.MealyMachine;
import de.fungate.translate.core.services.Regexes;
import de.fungate.translate.core.services.Translator;
import fj.F;
import fj.P2;
import fj.data.Either;
import fj.data.Option;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static de.fungate.translate.core.services.MealyMachine.when;
import static de.fungate.translate.core.services.Regexes.any;
import static fj.P.p;
import static fj.data.Option.some;

/**
 * Translator implementation for woerterbuch.info
 * @author Sebastian Graf
 */
public class WoerterbuchTranslator implements Translator {

    public static final Pattern FILTER_ENGLISH = Pattern.compile(any(Regexes.INFINITIVE_TO, Regexes.PARENTHESIS));
    public static final Pattern FILTER_GERMAN = Pattern.compile(Regexes.PARENTHESIS);
    private static final Logger LOG = Logger.getLogger(WoerterbuchTranslator.class);
    private final Curler curler;

    /**
     * Formats a URL as string for the given query term specific to woerterbuch.info.
     * @param term to be translated.
     * @return the ASCII-encoded URL.
     */
    public static String urlFor(String term) {
        try {
            return new URIBuilder()
                    .setScheme("http")
                    .setHost("www.woerterbuch.info")
                    .setParameter("s", "dict") // as opposed to 'thesaurus', which would search for synonyms
                    .setParameter("l", "en") // only from german to <l> where l is 'en', 'fr', etc.
                    .setParameter("query", term)
                    .build().toASCIIString();
        } catch (URISyntaxException e) {
            LOG.error("URI syntax is wrong, this needs a fix", e);
            return "";
        }
    }

    /**
     * Instantiates a new WoerterbuchTranslator.
     * @param curler used to issue get requests to a URL.
     */
	@Inject
	public WoerterbuchTranslator(Curler curler) {
		this.curler = curler;
	}

    /**
     * Translates the given term from the source language into the implicit complementary target language.
     * @param term to be translated.
     * @param source SourceLanguage in which the term is queried.
     * @return the set of translations in the target language.
     */
	@Override
	public Set<Translation> translate(String term, SourceLanguage source) {
		Either<String, Exception> content = curler.get(urlFor(term));
        if (content.isRight()) {
            // Get request failed and the right type is present. We can lookup the Exception
            Exception e = content.right().value();
            if (e instanceof ConnectTimeoutException) {
                // Woerterbuch isn't as reliable as it should be.
                LOG.warn("Connection timed out.");
            } else {
                LOG.warn("GET request failed.", e);
            }
            return Collections.emptySet();
        }

        // The left type is present, so the GET request was successful and
        // we can extractTranslations the inner string value
        return extractTranslations(source, Jsoup.parse(content.left().value()));
	}

    @SuppressWarnings("unchecked")
    private Set<Translation> extractTranslations(SourceLanguage source, Document doc) {
        // The following machine parses only the direct hits out of the HTML soup.
        MealyMachine<State, Element, Option<Translation>> parser = MealyMachine.fromTransitions(
                State.BEFORE,
                when(State.BEFORE).then(lookOutForSourceLangHeader(source)),
                when(State.IN_SECTION).then(lookOutForDirectHitsHeader),
                when(State.IN_DIRECT_HITS).then(parseDirectHits(source)),
                when(State.FINISHED).then(doNothing)
        );

        // Just feed the relevant elements into the machine until it is finished.
        // Thereby adding translations to the set.
        Set<Translation> translations = new HashSet<>();
        for (Element e : doc.select("table table tr")) {
            if (parser.getState() == State.FINISHED) {
                break;
            }
            Option<Translation> t = parser.step(e);
            if (t.isSome()) {
                translations.add(t.some());
            }
        }

        if (LOG.isTraceEnabled()) {
            for (Translation t : translations) {
                LOG.trace(t);
            }
        }

        return translations;
    }

    private F<Element, P2<State, Option<Translation>>> lookOutForSourceLangHeader(final SourceLanguage source) {
        return new F<Element, P2<State, Option<Translation>>>() {
            public P2<State, Option<Translation>> f(Element tr) {
                Element header = tr.select("td.standard").first();
                State nextState = isSourceLangHeader(header, source) ? State.IN_SECTION : State.BEFORE;
                return p(nextState, Option.<Translation>none());
            }
        };
    }

    private final F<Element, P2<State, Option<Translation>>> lookOutForDirectHitsHeader = new F<Element, P2<State, Option<Translation>>>() {
        public P2<State, Option<Translation>> f(Element tr) {
            Element subHeader = tr.select("td.standard").first();
            State nextState = isDirectHitsHeader(subHeader)
                    ? State.IN_DIRECT_HITS : State.IN_SECTION;
            return p(nextState, Option.<Translation>none());
        }
    };

    private F<Element, P2<State, Option<Translation>>> parseDirectHits(final SourceLanguage source) {
        return new F<Element, P2<State, Option<Translation>>>() {
            public P2<State, Option<Translation>> f(Element tr) {
                Elements columns = tr.select("td.hl");
                if (columns.size() < 2) { // we hit the end of the section
                    return p(State.FINISHED, Option.<Translation>none());
                }
                // left column is english, right is german
                String english = filterEnglish(columns.get(source == SourceLanguage.ENGLISH ? 0 : 1).text());
                String german = filterGerman(columns.get(source == SourceLanguage.GERMAN ? 0 : 1).text());
                return p(State.IN_DIRECT_HITS, some(new Translation(english, german)));
            }
        };
    }

    private final F<Element, P2<State, Option<Translation>>> doNothing = new F<Element, P2<State, Option<Translation>>>() {
        public P2<State, Option<Translation>> f(Element tr) {
            return p(State.FINISHED, Option.<Translation>none());
        }
    };

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

    private static boolean isDirectHitsHeader(Element subHeader) {
        return subHeader != null && subHeader.text().toLowerCase().startsWith("direkte");
    }

    private static String filterGerman(String term) {
        return FILTER_GERMAN.matcher(term).replaceAll("")
                .replaceAll(Regexes.WHITESPACE, " ") // normalize whitespace
                .trim();
    }

    private static String filterEnglish(String term) {
        return FILTER_ENGLISH.matcher(term).replaceAll("")
                .replaceAll(Regexes.WHITESPACE, " ") // normalize whitespace
                .trim();
    }

    @Override
    public String getProvider() {
        return "woerterbuch.info";
    }

    private enum State {

        /**
         * Last table row was before the relevant section
         */
        BEFORE,

        /**
         * Last table row was in a nonrelevant section
         */
        IN_SECTION,

        /**
         * Last table row was in direct hits
         */
        IN_DIRECT_HITS,

        /**
         * Last table row was beyond direct hits
         */
        FINISHED

    }
}
