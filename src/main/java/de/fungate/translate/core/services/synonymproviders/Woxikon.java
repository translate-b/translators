package de.fungate.translate.core.services.synonymproviders;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

import de.fungate.translate.core.models.SourceLanguage;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import de.fungate.translate.core.services.Curler;
import de.fungate.translate.core.services.SynonymProvider;
import fj.data.Either;

/**
 * @author f.supplie        
 */

public class Woxikon implements SynonymProvider {

	//private static final String DEURL = "http://synonyme.woxikon.de/synonyme-de/";
	private static final String ENURL = "http://synonyme.woxikon.de/synonyme-englisch/";

	private static final Logger LOG = Logger.getLogger(Woxikon.class);
	private final Curler curler;

	public Woxikon(Curler curler) {
		this.curler = curler;
	}
	
	
	private String buildurl(String term) {

		String url = "";

		try {
			url = ENURL + URLEncoder.encode(term, "UTF-8") + ".php";
		} catch (UnsupportedEncodingException e) {
			LOG.error("System does not support UTF-8", e);
		}

		return url;
	}
	
	@Override
	// in this implementation, the method only get synonyms for english terms
	public Set<String> getSynonyms(String term, SourceLanguage sourceLanguage) {
        Set<String> result = new HashSet<>();

		String url = buildurl(term);
		Either<String, Exception> curl = curler.get(url);

		if (curl.isLeft()) {
			String response = curl.left().value();

			Document doc = Jsoup.parse(response);

			Elements content = doc.getElementsByClass("inner");
			Elements links = content.select("a");

			for (Element link : links) {
				String synonym = link.text();
				if (!synonym.equals("") && !synonym.equals(term)) {
					result.add(synonym);
				}
			}

			return result;
			
		} else {
			LOG.warn("CONNECTION_ERROR", curl.right().value());
			return result;
		}

	}

}
