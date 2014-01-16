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

import java.util.*;
import java.util.regex.Pattern;

import static de.fungate.translate.core.services.Regexes.any;

/**
 * Translator implementation for the pons provider
 * @author Kader Pustu
 *
 */

public class PonsTranslator implements Translator {

    private final Curler curler;
    private static final String CONNECTION_ERROR = "Es konnte keine Verbindung zu de.pons.eu hergestellt werden!";
    private static final Logger LOG = Logger.getLogger(PonsTranslator.class);
    private static final int LIMIT = 15;

    @Inject
    public PonsTranslator(Curler curler) {
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
        Either<String, Exception> content = curler.get(url);

        if (content.isLeft() && !term.isEmpty()) {

            return crawl(term, src, content);

        } else {
            LOG.warn(CONNECTION_ERROR, content.right().value());
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
        String url = "http://de.pons.eu/dict/search/results/?q=";
        while (term.contains(" ")) {
            url = url + term.substring(0, term.indexOf(" ")) + "+";
            term = term.substring(term.indexOf(" ") + 1);
        }
        url = url + term + "&l=deen&in=" + changeSourceLanguage(src) + "&lf="
                + changeSourceLanguage(src);

        return url;
    }

    private String changeSourceLanguage(SourceLanguage src) {
        if (src.equals(SourceLanguage.GERMAN)) {
            return "de";
        } else
            return "en";
    }

    /**
     * Crawls the given content to return a set of translations found
     * @param term to be translated
     * @param src SourceLanguage of the term
     * @param content
     * @return the set of crawled translations
     */
    private Set<Translation> crawl(String term, SourceLanguage src,
                                   Either<String, Exception> content) {

        Set<Translation> s = new HashSet<>();
        Document doc = Jsoup.parse(content.left().value());

        if(!isActualTermTranslation(doc)) return s;

        int classTranslationsSize = doc.select(".translations").size();

        Elements h3Elements = doc.select(".translations h3");

        if (classTranslationsSize != 0) {
            for (int i = 0; i < classTranslationsSize; i++) {

                if (s.size() >= LIMIT) return s;
                // further eliminition of translations to wrong terms
                if (h3Elements.get(i).text().toLowerCase()
                        .contains(term.toLowerCase())
                        || h3Elements.get(i).toString()
                        .contains("empty hidden")) {

                    Elements sourceElements = doc.select(".translations")
                            .get(i).select(".source");
                    Elements ddInnerElements = doc.select(".translations")
                            .get(i).select(".dd-inner");
                    s = parseTranslation(s, src, sourceElements,
                            ddInnerElements);
                }
            }
            return s;
        } else {

            Elements sourceElements = doc.select("[data-translation] .source");
            Elements ddInnerElements = doc
                    .select("[data-translation] .dd-inner");
            s = parseTranslation(s, src, sourceElements, ddInnerElements);

        }
        return s;
    }

    /**
     * examination whether translations for given term or only similar term
     * translations are available
     * @param doc the document to be crawled
     * @return boolean true if document contains the actual translations for term
     */
    public boolean isActualTermTranslation(Document doc){
        for (Element element : doc.getElementsByClass("alert")) {
            if (element.attr("class").equals("alert notice fuzzysearch"))
                return false;
        }
        return true;
    }


    /**
     * parses the final translation pairs
     * @param s set of translations
     * @param src SourceLanguage of the former term
     * @param sourceElements terms in the source language
     * @param ddInnerElements translations in the target language
     * @return the set of translations
     */
    private Set<Translation> parseTranslation(Set<Translation> s,
                                              SourceLanguage src, Elements sourceElements,
                                              Elements ddInnerElements) {

        for (int j = 0; j < sourceElements.size(); j++) {

            if (s.size() >= LIMIT)
                return s;

            String sourceTerm = sourceElements.get(j).text();
            String targetTerm = ddInnerElements.get(j).select(".target").text();
            // if clause when german search has been executed
            if (src.equals(SourceLanguage.GERMAN)) {
                if (sourceElements.get(j).html().contains("deutsch-englisch")) {
                    s.add(new Translation(filter(targetTerm),
                            filter(sourceTerm)));
                } else {
                    s.add(new Translation(filter(sourceTerm),
                            filter(targetTerm)));
                }
                // else clause when english search has been executed
            } else {
                if (sourceElements.get(j).html().contains("englisch-deutsch")) {
                    s.add(new Translation(filter(sourceTerm),
                            filter(targetTerm)));
                } else {
                    s.add(new Translation(filter(targetTerm),
                            filter(sourceTerm)));
                }
            }
        }
        return s;
    }

    /**
     * Filters out inapproriate characters and brackets returning the filtered term
     * @param word
     * @return the filtered term
     */
    private String filter(String word) {
        final Pattern MATCH_GARBAGE = Pattern.compile(any(Regexes.PARENTHESIS));

		String[] wordCase = { "derb", "dial", "nt", "(in)", "(f)", "(m)", "Am",
				"Brit", "liter", "old", "dial", "nordd", "pej", "also fig",
				"fam", "dat", "fig", "geh", "form", "fam!", "ASTRON", "GASTR",
				"ELEC", "MILIT", "prov", "vulg", "südd", "akk", "indef", "art",
				"Bsp", "prov", "attr", "pl", "gen", "+ sing", "vb", "m", "f",
				"sl", "nt", "Auto", "GEOG", "sep", "no art"};
        
        String[] articles = { "der", "die", "das", "the" };

        List<Pair> wordPairs = new ArrayList<>();

        for (String s : wordCase)
            wordPairs.add(getPairForOneWord(s));

        wordPairs.add(new Pair("a.", "a\\."));
        wordPairs.add(new Pair("+akk", "\\+akk"));
        wordPairs.add(new Pair("+ sing", "\\+ sing"));
        wordPairs.add(new Pair(", no", "\\, no"));
        


        StringBuilder sb = new StringBuilder(word);

        // square brackets are removed
        while (word.contains("[")) {
            if (word.lastIndexOf("]") + 2 < word.length())
                word = sb.delete(word.lastIndexOf("["),
                        word.indexOf("]", word.lastIndexOf("[")) + 2)
                        .toString();
            else
                word = sb.delete(word.lastIndexOf("["),
                        word.indexOf("]", word.lastIndexOf("[")) + 1)
                        .toString();
        }

        // parantheses are removed
        word = MATCH_GARBAGE.matcher(word).replaceAll("").trim();

        // inappropriate words matching the array content are removed
        for (Pair wordPair : wordPairs) {
            String pairWord = wordPair.getWord();
            String pairRegex = wordPair.getRegex();
            if (word.contains(" " + pairWord + " ")
                    || word.startsWith(pairWord + " ")
                    || word.endsWith(" " + pairWord)) {
                word = word.replaceAll(pairRegex, "").trim();
            }
        }
        
        //articles at the beginning of the term are removed
        for(String article : articles){
        	if(word.startsWith(article + " "))
        		word = word.replaceAll(article, "").trim();	
        }

        // "⇆" is removed
        word = word
                .replaceAll(
                        "[^\\p{ASCII}\\u00E4\\u00F6\\u00FC\\u00C4\\u00D6\\u00DC\\u00DF]",
                        "");

        // whitespaces is normalised
        word = word.replaceAll(Regexes.WHITESPACE, " ");

        return word;
    }

    private Pair getPairForOneWord(String word) {
        return new Pair(word, word);
    }

    public String getProvider() {
        return "pons.eu";
    }

}

class Pair {
    private final String word;
    private final String regex;

    public Pair(String word, String regex) {
        this.word = word;
        this.regex = regex;
    }

    public String getWord() {
        return this.word;
    }

    public String getRegex() {
        return this.regex;
    }

}
