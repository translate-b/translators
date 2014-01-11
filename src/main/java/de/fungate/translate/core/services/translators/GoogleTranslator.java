package de.fungate.translate.core.services.translators;

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

	private static final String ENURL = "http://translate.google.de/translate_a/t?client=t&sl=en&tl=de&hl=de&sc=2&ie=UTF-8&oe=UTF-8&ssel=0&tsel=0&q=";
	private static final String DEURL = "http://translate.google.de/translate_a/t?client=t&sl=de&tl=en&hl=de&sc=2&ie=UTF-8&oe=UTF-8&ssel=0&tsel=0&q=";

	private static final Logger LOG = Logger.getLogger(GoogleTranslator.class);
	private static final String CONNECTION_ERROR = "Es konnte keine Verbindung zu translate.google.com hergestellt werden!";

    private final Curler curler;

	public GoogleTranslator(Curler curler) {
		this.curler = curler;
	}

	@Override
	public Set<Translation> translate(String term, SourceLanguage source) {
		String url = buildurl(term, source);

		Either<String, Exception> content = curler.get(url);

		if (content.isLeft()) {
			String response = content.left().value();

			return buildresult(parse(response), source);
		} else {
			LOG.warn(CONNECTION_ERROR, content.right().value());
			return Collections.emptySet();
		}
	}

	private Set<Translation> buildresult(ArrayList<String> parse, SourceLanguage source) {

		Set<Translation> result = new HashSet<>();

		for (String s : parse) {
			JSONArray array3 = (JSONArray) JSONValue.parse(s);
			String wordclass = array3.get(0).toString();
			

			JSONArray translations = (JSONArray) array3.get(1);
			for (Object p : translations) {
				String translation = p.toString();
				
				if (source == SourceLanguage.GERMAN) {
					
					if(wordclass.toLowerCase().equals("verb")){
						translation = "to " + translation;
					}
					String term = array3.get(3).toString();
					
					result.add(new Translation(translation, term));
				} else if (source == SourceLanguage.ENGLISH) {
					
					String term = array3.get(3).toString();
					
					if(wordclass.toLowerCase().equals("verb")){
						term = "to " + array3.get(3).toString();
					}
					
					result.add(new Translation(term, translation));
				}
			}
		}

		return result;

	}

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

	private ArrayList<String> parse(String response) {

		ArrayList<String> result = new ArrayList<String>();

		Object jsonresponse = JSONValue.parse(response);
		JSONArray array = (JSONArray) jsonresponse;

		//indication that the array does not include any translations
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