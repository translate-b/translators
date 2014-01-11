package de.fungate.translate.core.services.translators;


import de.fungate.translate.core.models.SourceLanguage;
import de.fungate.translate.core.models.Translation;
import de.fungate.translate.core.services.Curler;
import de.fungate.translate.core.services.Translator;
import de.fungate.translate.core.services.Regexes;
import fj.data.Either;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.*;
import java.util.regex.Pattern;
import static de.fungate.translate.core.services.Regexes.any;

public class PonsTranslator implements Translator {
	
	private final Curler curler;
	private static final String CONNECTION_ERROR = "Es konnte keine Verbindung zu de.pons.eu hergestellt werden!";
    private static final Logger LOG = Logger.getLogger(PonsTranslator.class);

	public PonsTranslator(Curler curler) {
		this.curler = curler;
	}
	
	@Override
	public Set<Translation> translate(String term, SourceLanguage src) {
		String url  = buildURL(term,src);
		Either<String, Exception> content = curler.get(url);
		
		if (content.isLeft() && !term.isEmpty()) { 
			
			return crawl(term,src,content);
			
		} else {
			LOG.warn(CONNECTION_ERROR, content.right().value());
			return Collections.emptySet();
		}	
	}
	
	public String buildURL(String term, SourceLanguage src){
		String url = "http://de.pons.eu/dict/search/results/?q=";
		while (term.contains(" ")) {
			url = url + term.substring(0, term.indexOf(" ")) + "+";
			term = term.substring(term.indexOf(" ") + 1);
		}
		url = url + term + "&l=deen&in=" + changeSourceLanguage(src) + "&lf="  + changeSourceLanguage(src);
		
		return url;
	}
	
	private String changeSourceLanguage(SourceLanguage src){
		if(src.equals(SourceLanguage.GERMAN)){
			return "de";
		}
		else return "en";	
	}

	
	private Set<Translation> crawl(String term, SourceLanguage src , Either<String, Exception> content){
		
		Set<Translation> s = new HashSet<Translation>();
		Document doc = Jsoup.parse(content.left().value());                    
		
		for (Element ele : doc.getElementsByClass("alert")){
			if (ele.attr("class").equals("alert notice fuzzysearch"))
				return s;
		}
		
		String translations = doc.getElementsByClass("translations").html();
		int sizeTranslations = doc.getElementsByClass("translations").size();
		Document translationsDoc = Jsoup.parse(translations);                        
		Elements h3 = translationsDoc.select("h3");   
		
		String source, target;
	
		if(sizeTranslations != 0){
			for(int i= 0; i < sizeTranslations;i++) {	
				if(h3.get(i).text().toLowerCase().contains(term.toLowerCase()) || h3.get(i).toString().contains("empty hidden")){          
					Document iTranslationsDoc = Jsoup.parse(doc.getElementsByClass("translations").get(i).html());
					for (int j = 0; j < iTranslationsDoc.getElementsByClass("source").size(); j++) {

							source = iTranslationsDoc.getElementsByClass("source").get(j).text();
							target = iTranslationsDoc.getElementsByClass("dd-inner").get(j).html();
							Document targetDocument = Jsoup.parse(target);
							target = targetDocument.getElementsByClass("target").text();
						if(src.equals(SourceLanguage.GERMAN)){	
							if(iTranslationsDoc.getElementsByClass("source").get(j).html().contains("deutsch-englisch")){ 
								s.add(new Translation(filter(target), filter(source)));
							}else {
								s.add(new Translation(filter(source), filter(target)));
							}
						}
						else {
							if(iTranslationsDoc.getElementsByClass("source").get(j).html().contains("englisch-deutsch")){
								s.add(new Translation(filter(source), filter(target)));
								}
								else {
									s.add(new Translation(filter(target), filter(source)));
								}
						}
					}	
				}
			}
		}else{
			translationsDoc = Jsoup.parse(doc.getElementsByAttribute("data-translation").html());
			for (int j = 0; j < translationsDoc.getElementsByClass("source").size(); j++) {
				
				
				source = translationsDoc.getElementsByClass("source").get(j).text();
				target = translationsDoc.getElementsByClass("dd-inner").get(j).html();
				Document targetDocument = Jsoup.parse(target);
				target = targetDocument.getElementsByClass("target").text();
					
				if(src.equals(SourceLanguage.GERMAN)){	
					if(translationsDoc.getElementsByClass("source").get(j).html().contains("deutsch-englisch")){ 
						s.add(new Translation(filter(target), filter(source)));
					}else {
						s.add(new Translation(filter(source), filter(target)));
					}
				}
				else {
					if(translationsDoc.getElementsByClass("source").get(j).html().contains("englisch-deutsch")){
						s.add(new Translation(filter(source), filter(target)));
					}
					else {
						s.add(new Translation(filter(target), filter(source)));
					}	
				}
			}
		}
		return s;
}

	

	private String filter(String word){
		final Pattern MATCH_GARBAGE = Pattern.compile(any(Regexes.PARENTHESIS));
		 
		String[] wordCase = { "der", "die", "das", "the", "derb","dial", "nt", "(in)", "(f)", "(m)","Am", "Brit", "liter", "old", "dial", "nordd", "pej", "also fig","fam", "dat", "fig", "geh", "form", "fam!", "ASTRON", "GASTR",
							"ELEC", "⇆", "MILIT","prov", "vulg", "südd", "akk", "indef" , "art", "Bsp", "prov", "attr", "pl", "gen", "+ sing", "vb", "m", "f", "sl", "nt", "Auto"};
		
		List<Pair> wordPairs = new ArrayList<Pair>();
		
		for (String s : wordCase)
			wordPairs.add(getPairForOneWord(s));

		wordPairs.add(new Pair("a.", "a\\."));
		wordPairs.add(new Pair("+akk", "+akk\\"));
		
		StringBuilder test = new StringBuilder(word);
		
		while(word.contains("[")){			
			if(word.lastIndexOf("]") + 2 < word.length())
				word = test.delete(word.lastIndexOf("[") , word.indexOf("]", word.lastIndexOf("["))+2 ).toString();
			else
				word = test.delete(word.lastIndexOf("[") , word.indexOf("]", word.lastIndexOf("["))+1 ).toString();
		}
	
		word = MATCH_GARBAGE.matcher(word).replaceAll("").trim();
		
		for(int i = 0; i <wordPairs.size(); i++){
			String pairword = wordPairs.get(i).getWord();
			String pairregex = wordPairs.get(i).getRegex();
			if(word.contains(" " + pairword + " ") || word.startsWith(pairword + " ") || word.endsWith(" " + pairword)){
				word = word.replaceAll(pairregex, "").trim();
			}
		}
		word = word.replaceAll("\\s+", " ");
		return word;
		}
	
	private Pair getPairForOneWord(String word){
		return new Pair(word, word);
	}

	public String getProvider() {
		return "pons.eu";
	}


}

class Pair{
	private String word;
	private String regex;
	
	public Pair(String word, String regex){
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
