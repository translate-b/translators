package de.fungate.translate.core.services;

import de.fungate.translate.core.models.SourceLanguage;
import de.fungate.translate.core.models.Translation;

import java.util.Set;

/**
 * Represents a translator which is able to translate terms from a source language
 * into the implicit complementary target language. Also has a name uniquely identifying its source.
 */
public interface Translator {

    /**
     * Translates a term in the source language into the complementary target language.
     * @param term to be translated.
     * @param source SourceLanguage of the term.
     * @return the set of translations.
     */
	Set<Translation> translate(String term, SourceLanguage source);

    /**
     * A string uniquely identifying the underlying implementations.
     * @return the string.
     */
	String getProvider();

}
