package de.fungate.translate.core.services.translators;


import com.google.inject.Inject;
import de.fungate.translate.core.models.SourceLanguage;
import de.fungate.translate.core.models.Translation;
import de.fungate.translate.core.services.Curler;
import de.fungate.translate.core.services.Translator;
import fj.data.Either;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
/**
 * Translator implementation for the leo provider
 * @author Kader Pustu
 */
public class LeoTranslator implements Translator {

    private final Curler curler;
    private static final Logger LOG = Logger.getLogger(LeoTranslator.class);
    private static final XPathExpression GERMAN_XPATH = compileXPath("//entry//side[@lang='de']//words/word[1]/text()");
    private static final XPathExpression ENGLISH_XPATH = compileXPath("//entry//side[@lang='en']//words/word[1]/text()");

    @Inject
    public LeoTranslator(Curler curler) {
        this.curler = curler;
    }

   
    /**
     * Gets the translations for the term in the given source language 
     * @ param term to be translated
     * @ param src SourceLanguange of the given term
     * @ return the set containing pairs of translations
     */
    @Override
    public Set<Translation> translate(String term, SourceLanguage src) {
        String url = buildURL(term, src);
        Either<InputStream, Exception> content = curler.getStream(url);

        if (content.isLeft() && !term.isEmpty()) {
            try {
                return crawl(content,15);
            } catch (IOException | SAXException
                    | XPathExpressionException e) {
                LOG.error("Failed to extract translations from XML", e);
                return Collections.emptySet();
            }
        } else {
            LOG.error("Could not get data from leo.org", content.right().value());
            return Collections.emptySet();
        }
    }


    /**
     * builds the url for the term in the given source language
     * @param term to be translated
     * @param src SourceLanguage of the given term
     * @return the URL for the term translations 
     */
    public String buildURL(String term, SourceLanguage src) {
        try {
            return "http://dict.leo.org/dictQuery/m-vocab/ende/query.xml?tolerMode=nof&lp=ende&lang=de&rmWords=off&rmSearch=on&directN=0&search="
                    + URLEncoder.encode(term, "UTF-8")
                    + "&searchLoc="
                    + changeSourceLanguage(src)
                    + "&resultOrder=basic&multiwordShowSingle=on&sectLenMax=16";
        } catch (UnsupportedEncodingException e) {
            LOG.error("Failed to encode the URL for term " + term, e);
            return "";
        }
    }

    /**
     * Crawls the given content to return a set of translations found
     * @param content 
     * @param limit restricting the number of translations
     * @return the set of crawled translations
     */
    private Set<Translation> crawl(Either<InputStream, Exception> content, int limit)
            throws XPathExpressionException, IOException, SAXException {

        Set<Translation> s = new HashSet<>();

        Document doc = makeDocumentBuilder().parse(content.left().value());

        // The text forms of all english terms
        NodeList englishTerms = (NodeList)ENGLISH_XPATH.evaluate(doc, XPathConstants.NODESET);
        // The text forms of all german terms
        NodeList germanTerms = (NodeList)GERMAN_XPATH.evaluate(doc, XPathConstants.NODESET);

        if (englishTerms.getLength() != germanTerms.getLength()) {
            // we should at least output a warning
            LOG.warn(String.format("Number of english and german terms mismatch: %d != %d",
                    englishTerms.getLength(), germanTerms.getLength()));
        }

        // Save as many terms as possible
        int numberOfTerms = Math.min(germanTerms.getLength(), englishTerms.getLength());
        for (int i = 0; i < numberOfTerms; i++) {
        	
        	if(limit>0){
	            String english = englishTerms.item(i).getNodeValue();
	            String german = germanTerms.item(i).getNodeValue();
	            s.add(new Translation(filter(english), filter(german)));
	            limit--;
        	}
        	else break;
        }

        return s;
    }

    /**
     * Filters possible articels of given word out
     * @param word
     * @return the filtered word
     */
    private String filter(String word){
		
		String[] wordCase = { "der", "die", "das", "the" };
		
		for(int i = 0; i <wordCase.length; i++){
			if(word.startsWith(wordCase[i] + " ")){
				word = word.replaceAll(wordCase[i], "").trim();
			}
		}
		return word;
	}
    
    private String changeSourceLanguage(SourceLanguage src) {
        if (src.equals(SourceLanguage.GERMAN)) {
            return "1";
        } else
            return "-1";
    }

    public String getProvider() {
        return "leo.org";
    }

    private static DocumentBuilder makeDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            LOG.error("Failed to create the document builder.", e);
            return null;
        }
    }

    private static XPathExpression compileXPath(String xpath) {
        try {
            return XPathFactory.newInstance().newXPath().compile(xpath);
        } catch (XPathExpressionException e) {
            LOG.error("Failed to compile XPath expression: " + xpath, e);
            return null;
        }
    }

}
