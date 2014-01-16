package de.fungate.translate.core.services.synonymproviders;

import com.google.inject.Inject;
import de.fungate.translate.core.models.SourceLanguage;
import de.fungate.translate.core.services.Curler;
import de.fungate.translate.core.services.SynonymProvider;
import fj.data.Either;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

/**
 * Class to get synonyms from a german or english term from Woxikon
 * 
 * @author f.supplie
 * @version 7.0
 */

public class Woxikon implements SynonymProvider {

	// fix parts for the url to parse
	private static final String DEURL = "http://synonyme.woxikon.de/synonyme/";
	private static final String ENURL = "http://synonyme.woxikon.de/synonyme-englisch/";

	private static final Logger LOG = Logger.getLogger(Woxikon.class);
	private static final String CONNECTION_ERROR = "";
	private final Curler curler;

    @Inject
	public Woxikon(Curler curler) {
		this.curler = curler;
	}

	/**
	 * Returns the url for a given term and the sourcelanguage. Prints an
	 * error-message if soruelanguage is invalid. Returns the empty string, if
	 * sourcelanguage is not english or germam.
	 * 
	 * @param term
	 * @param sourceLanguage
	 * @return String
	 */
	private String buildurl(String term, SourceLanguage sourceLanguage) {

		String url = "";

		// Delete slashes in the term and url encode the term
		// throw exceptions when URLEncode fails
		try {
			term = term.replace("/", "");
			term = URLEncoder.encode(term, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOG.error("System does not support UTF-8", e);
		}

		// merge the url parts (constant url part, term, extension) together
		// for the given sourcelanguage
		// log an error when sourcelanguage is unsupported and return a empty
		// string
		if (sourceLanguage == SourceLanguage.GERMAN) {
			url = DEURL + term + ".php";
		} else if (sourceLanguage == SourceLanguage.ENGLISH) {
			url = ENURL + term + ".php";
		} else {
			LOG.error("Unsupported language: " + sourceLanguage);
		}

		return url;

	}

	/**
	 * Returns a Set of synonyms for a given term and sourcelanguage. Prints an
	 * warn-message if connection fails. Returns an empty set when connections
	 * fails.
	 * 
	 * @param term
	 * @param sourceLanguage
	 * @return Set<String>
	 */
	@Override
	public Set<String> getSynonyms(String term, SourceLanguage sourceLanguage) {
		Set<String> result = new HashSet<>();

		String url = buildurl(term, sourceLanguage);

		// the curler get the contant from the given url
		Either<String, Exception> curl = curler.get(url);

		// if left type of the either is present the get request was successfull
		// and the synonyms can be parsed
		if (curl.isLeft()) {
			String response = curl.left().value();

			Document doc = Jsoup.parse(response);

			// go to the div container with the class and get all the hyperlinks
			// in this section
			Elements content = doc.getElementsByClass("inner");
			Elements links = content.select("a");

			// get the description for all hyperlinks and add them to the set
			// when they not empty or equal to the given term
			for (Element link : links) {
				String synonym = link.text();
				if (!synonym.equals("") && !synonym.equals(term)) {
					result.add(synonym);
				}
			}

			return result;

		} else {
			// when right type is present: get request failed and a warning is
			// logged and a empty set is returned
			LOG.warn(CONNECTION_ERROR, curl.right().value());
			return result;
		}

	}

}
