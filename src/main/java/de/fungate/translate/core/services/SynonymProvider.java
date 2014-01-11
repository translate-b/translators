package de.fungate.translate.core.services;

import de.fungate.translate.core.models.SourceLanguage;

import java.util.Set;

/**
 * Implementing classes provide synonyms for passed terms in a given source language.
 */
public interface SynonymProvider {

    /**
     * Provides a set of synonyms for the passed term in a given source language.
     * @param term the term to get synonyms for.
     * @param sourceLanguage the source language in which to look for synonyms.
     * @return the set of synonyms.
     */
	Set<String> getSynonyms(String term, SourceLanguage sourceLanguage);

}
