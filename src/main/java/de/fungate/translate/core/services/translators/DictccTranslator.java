package de.fungate.translate.core.services.translators;

import com.google.inject.Inject;
import de.fungate.translate.core.models.SourceLanguage;
import de.fungate.translate.core.models.Translation;
import de.fungate.translate.core.services.Curler;
import de.fungate.translate.core.services.Regexes;
import de.fungate.translate.core.services.Translator;
import fj.data.Either;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import static de.fungate.translate.core.services.Regexes.any;

/**
 * Class to gets the translation-contant of the dictcc Provider
 * @author Eike Karsten Schlicht
 * @version 9.0 
 */
public class DictccTranslator implements Translator{

	private static final String MAIN_PAGE_URL = "http://www.dict.cc/?s=";
	private static final String SELECT_ENG = "table tr[id] td:eq(1)";
	private static final String SELECT_GER = "table tr[id] td:eq(2)";
    private static final String CONNECTION_ERROR = "Es konnte keine Verbindung zu www.dict.cc hergestellt werden!";
    private static final Logger LOG = Logger.getLogger(DictccTranslator.class);
    public static final Pattern MATCH_GARBAGE = Pattern.compile(any(
            Regexes.DIGITS,
            Regexes.PARENTHESIS,
            Regexes.SQUARE_BRACKETS,
            Regexes.CURLY_BRACES,
            Regexes.ANGLE_BRACKETS,
            Regexes.SPECIAL_CHARACTERS,
            Regexes.WORDS_WITH_POINT
    ));
    private final Curler curler;

    @Inject
    public DictccTranslator(Curler curler) {
        this.curler = curler;
	}
    
    /**
     * Exception-Handling and returning the Translated contant for an request
     * into the web Elements.
     * Prints an error-message if connection failed.
     * Returns the empty string, if there is no value present in content.
     * @param term to be translated
     * @param source sourcelanguage enumeration
     * @return <Translation> a Set of translations
     */
	@Override
	public Set<Translation> translate(String term, SourceLanguage source) {
		String conURL = buildConnectionUrl(term, MAIN_PAGE_URL, source);
		Either<String, Exception> content = curler.get(conURL);
		//dictcc could be reached if the left type is present
		if (content.isLeft()) {
			Document doc = Jsoup.parse(content.left().value());
			return crawl(doc);
		} else {
			// if request failed and the right type is present (Shows an error-console Message).
			LOG.warn(CONNECTION_ERROR, content.right().value());
			return Collections.emptySet();
		}
	}
	
	/**
	 * Set the current provider to Dictcc (Depending on this crawler).
	 * @return string of providername
	 */
	@Override
	public String getProvider() {
		return "dict.cc";
	}
    
    /**
     * Builds the URL to access dictcc Website via Curler (conURL) via Sourcelanguage.
     * Handles the terms, which is delivered in the url of the Website
     * and cases with term out of more than one word.
     * @param term to be translated
     * @param url containing the page-URL 
     * @param source sourcelangueage enumeration
     * @return connection-url to open the result-page at provider website
     */
    private String buildConnectionUrl(String term, String url, SourceLanguage source){
    	//connecting to term-search via URL: "http://www.dict.cc/?s=" + term
    	String curUrl = url;
    	if(source == SourceLanguage.GERMAN){
    		curUrl = "http://de-en.dict.cc/?s=";
    	} else if(source == SourceLanguage.ENGLISH){ 
    		curUrl = "http://en-de.dict.cc/?s=";
    	} else {
    		LOG.error("Unsupported language: "+source);
    	}
    	try {
        	return curUrl + URLEncoder.encode(term, "UTF-8");
    	} catch (UnsupportedEncodingException e) {
    		LOG.error("Cannot happen.", e);
    		return "";
    	}
    }

    /**
     * Crawls the Dict.cc-Webpage by using defined html-selectors and build a Set
     * of translations (Englisch, German).
     * @param newdoc reperesentation of result-page at provider website
     * @return <Translation> a Set of translations
     */
    private Set<Translation> crawl(Document newdoc){
		//row passing through and line passing through in content table of dictcc:
		//Filters the english terms, german terms and word classes out of the HTML-content
		Elements englishTerms = newdoc.body().select(SELECT_ENG);
		Elements germanTerms = newdoc.body().select(SELECT_GER);
	
	    final int limit = 10;
        Set<Translation> translations = new HashSet<>(limit);
        Iterator<Element> engIter = englishTerms.iterator();
        Iterator<Element> gerIter = germanTerms.iterator();
        while (engIter.hasNext() 
                && gerIter.hasNext() 
                && translations.size() < limit) {
            String eng = toStringFiltre(engIter.next().text());
            String ger = toStringFiltre(gerIter.next().text());
            translations.add(new Translation(eng, ger));
        }
        return translations;
    }

    /**
     * Cascadet filter for the translated terms.
     * Uses some Regex-Pattern to replace unimportent or 
     * undesirable content wit whitespace and trim it to term-length.
     * @param input unfiltered terms
     * @return filtered terms
     */
    private String toStringFiltre(String input) {
        // Filter until we found the fix point at which no more garbage can be deleted
        boolean foundFixPoint = false;
        while (!foundFixPoint) {
            String before = input;
            input = MATCH_GARBAGE.matcher(input).replaceAll("");
            foundFixPoint = before.equals(input);
        }
        // whitespaces is normalised
    	return input.replaceAll(Regexes.WHITESPACE, " ").trim();
    }
}
