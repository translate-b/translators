package de.fungate.translate.core.services;

/**
 * This class defines some common regexes to filter the website content.
 * @author Eike Karsten Schlicht
 */
public abstract class Regexes {

    private Regexes() {
    }

    /**
     * Combines all regexes as alternatives with (...|...|...)
     * @param regexes to combine
     * @return combined regex
     */
    public static String any(String... regexes) {
        if (regexes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (String s : regexes) {
            sb.append(s);
            sb.append('|');
        }
        sb.deleteCharAt(sb.length()-1); // delete last |
        sb.append(')');
        return sb.toString();
    }

    /**
     * Combines all regexes to happen sequentially via simple concatenation.
     * @param regexes to combine
     * @return combined regex
     */
    public static String seq(String... regexes) {
        StringBuilder sb = new StringBuilder();
        for (String s : regexes) {
            sb.append(s);
        }
        return sb.toString();
    }
    
	/**
	 * Matches the special characters -, !, ?, %, &, /, #, $
	 */
    public static final String SPECIAL_CHARACTERS = "[-!?%&/#$]+";
	
	/**
	 * Matches parenthesis with or without content
	 */
	public static final String PARENTHESIS = "\\([^\\)]*\\)";
	
	/**
	 * Matches curly braces with or without content
	 */
	public static final String CURLY_BRACES = "\\{[^\\}]*\\}";
	
	/**
	 * Matches square brackets with or without content
	 */
	public static final String SQUARE_BRACKETS = "\\[[^\\]]*\\]";
	
	/**
	 * Matches angle brackets with or without content
	 */
	public static final String ANGLE_BRACKETS = "\\<[^\\>]*\\>";
	
	/**
	 * Matches digits
	 */
	public static final String DIGITS = "\\d+";
	
	/**
	 * Matches the common english pronouns sth., sb.
	 */
	public static final String COMMON_PRONOUNS_ENG = "(sth\\.|sb\\.)";
	
	/**
	 * Matches the common german pronouns jd., etw.
	 */
	public static final String COMMON_PRONOUNS_GER = "(jd\\.|etw\\.)";
	
	/**
	 * Matches a word starting with a lowercase letter followed by a point.
	 */
	public static final String WORDS_WITH_POINT = "\\s*\\b[a-z][a-zA-Z]*\\.";

    /**
     * Matches all "to "'s at the beginning of the term
     */
    public static final String INFINITIVE_TO = "^\\s*to\\s+";
    
    /**
     * Matches all white space
     */
    public static final String WHITESPACE = "\\s+";
}
