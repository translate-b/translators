package de.fungate.translate.core.services.translators;

import com.google.inject.Inject;
import de.fungate.translate.core.models.SourceLanguage;
import de.fungate.translate.core.models.Translation;
import de.fungate.translate.core.services.Curler;
import de.fungate.translate.core.services.Translator;
import fj.data.Either;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Class to get the translations of the google translate website
 * 
 * @author f.supplie
 * @version 2.5
 */
public class GoogleTranslator implements Translator {

	// constant parts of the url
	private static final String ENURL = "http://translate.google.de/translate_a/t?client=t&sl=en&tl=de&hl=de&sc=2&ie=UTF-8&oe=UTF-8&ssel=0&tsel=0&q=";
	private static final String DEURL = "http://translate.google.de/translate_a/t?client=t&sl=de&tl=en&hl=de&sc=2&ie=UTF-8&oe=UTF-8&ssel=0&tsel=0&q=";

	private static final Logger LOG = Logger.getLogger(GoogleTranslator.class);
	private static final String CONNECTION_ERROR = "Es konnte keine Verbindung zu translate.google.com hergestellt werden!";

	private final Curler curler;

    @Inject
	public GoogleTranslator(Curler curler) {
		this.curler = curler;
	}

	@Override
	/**
	 * Returns a Set of translation for a given term and sourcelanguage. Prints an
	 * warn-message if connection fails. Returns an empty set when connections
	 * fails.
	 * 
	 * @param term to be translated
	 * @param sourceLanguage of the given term
	 * @return Set<Translation>
	 */
	public Set<Translation> translate(String term, SourceLanguage source) {
		String url = buildurl(term, source);

		Either<String, Exception> content = curler.get(url);

		// if left type of either is present the get-request was successfull and
		// the translations can be parsed
		if (content.isLeft()) {
			String response = content.left().value();

			return buildresult(parse(response), source);
		}
		// if right type of either is present the get-request failed and a
		// connection error is logged and a empty set returned
		else {
			LOG.warn(CONNECTION_ERROR, content.right().value());
			return Collections.emptySet();
		}
	}

	/**
	 * Returns a Set of translation for the parsed content given in an ArrayList
	 * of Strings and the depending source language and return a set of translation.
	 * @param parse ArrayList<String> of the relevant part of the JSON response
	 * @param SourceLanguage of the term
	 * @return Set<Translation> of translations
	 */
	
	private Set<Translation> buildresult(ArrayList<String> parse,
			SourceLanguage source) {

		Set<Translation> result = new HashSet<>();

		for (String s : parse) {
			JSONArray array3 = (JSONArray) JSONValue.parse(s);
			String wordclass = array3.get(0).toString();

			JSONArray translations = (JSONArray) array3.get(1);
			for (Object p : translations) {
				String translation = p.toString();
				
				if (source == SourceLanguage.GERMAN) {

					// if the english part of this translation to add is an
					// verb, a "to" is added
					if (wordclass.toLowerCase().equals("verb")) {
						translation = "to " + translation;
					}
					String term = array3.get(3).toString();

					result.add(new Translation(translation, term));
				} else if (source == SourceLanguage.ENGLISH) {

					String term = array3.get(3).toString();

					// if the english part of this translation to add is an
					// verb, a "to" is added
					if (wordclass.toLowerCase().equals("verb")) {
						term = "to " + array3.get(3).toString();
					}

					result.add(new Translation(term, translation));
				}
			}
		}

		return result;

	}

	/**
	 * Returns the url to parse for a given term and source language. Log an
	 * error when the url encoding fails.
	 * 
	 * @param term
	 * @param source source language of the term
	 * @return String  - url for the JSON request
	 */
	private String buildurl(String term, SourceLanguage source) {

		try {
			term = URLEncoder.encode(term, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOG.error("System does not support UTF-8", e);
		}

		String url = "";

		if (source == SourceLanguage.GERMAN) {
			url = DEURL + term;
		} else if (source == SourceLanguage.ENGLISH) {
			url = ENURL + term;
		} else {
			LOG.error("Unsupported language: " + source);
		}

		return url;
	}

	
	
	/**
	 * Returns an ArrayList<String> which include the relevant data of the JSON response.
	 * @param response complete JSON response from the google server
	 * @return ArrayList<String> including JSON parts which includes the translations
	 */
	private ArrayList<String> parse(String response) {

		ArrayList<String> result = new ArrayList<String>();

		Object jsonresponse = JSONValue.parse(response);
		JSONArray array = (JSONArray) jsonresponse;

		// indication that the array does not include any translations
		if (!array.isEmpty() && !array.get(1).toString().equals("en")
				&& !array.get(1).toString().equals("de")) {

			JSONArray translationarray = (JSONArray) array.get(1);
			
			for (Object o : translationarray) {
				result.add(o.toString());
			}
		}

		return result;
	}

	@Override
	public String getProvider() {
		return "translate.google.com";
	}

}
