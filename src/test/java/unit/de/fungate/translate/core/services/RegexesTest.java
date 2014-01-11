package unit.de.fungate.translate.core.services;

import de.fungate.translate.core.services.Regexes;
import de.fungate.translate.core.services.translators.DictccTranslator;
import org.junit.Test;
import scala.util.matching.Regex;

import java.util.regex.Pattern;

import static de.fungate.translate.core.services.Regexes.any;
import static de.fungate.translate.core.services.Regexes.seq;
import static org.junit.Assert.assertEquals;

public class RegexesTest {

    @Test
    public void anyShouldMatchOneAlternative() {
        String regex = any(Regexes.INFINITIVE_TO);
        assertDeletedAllOccurences(regex, "to stay", "stay");
    }

    @Test
    public void anyShouldMatchAllAlternatives() {
        String regex = any(Regexes.INFINITIVE_TO, Regexes.PARENTHESIS);
        assertDeletedAllOccurences(regex, "to stay(Trefferanzeige am Schie\u00DFstand)", "stay");
    }

    @Test
    public void seqShouldMatchOneAlternative() {
        String regex = seq(Regexes.INFINITIVE_TO);
        assertDeletedAllOccurences(regex, "to stay", "stay");
    }

    @Test
    public void seqShouldMatchSequentially() {
        String regex = seq(Regexes.INFINITIVE_TO, Regexes.PARENTHESIS);
        assertDeletedAllOccurences(regex, "to (Trefferanzeige am Schie\u00DFstand)stay", "stay");
    }

    private static void assertDeletedAllOccurences(String regex, String string, String expected) {
        String actual = Pattern.compile(regex).matcher(string).replaceAll("");
        assertEquals(expected, actual);
    }
    
    @Test
    public void anyTextWithoutCurlyBrackets() {
        String regex = Regexes.SQUARE_BRACKETS;
        assertDeletedAllOccurences(regex, "[a�klfhpahpw] wand", " wand");
    }

    @Test
    public void anyTextWithoutAnyBrackets() {
        String regex = any(Regexes.ANGLE_BRACKETS, Regexes.CURLY_BRACES, Regexes.SQUARE_BRACKETS,
        		Regexes.PARENTHESIS);
        assertDeletedAllOccurences(regex, "<bla> Wand [haus] (wand) {blub}", " Wand   ");
    }
    
    @Test
    public void anyTextWithoutAnyBracketsAndWhiteSpaces() {
        String regex = any(Regexes.ANGLE_BRACKETS, Regexes.CURLY_BRACES, Regexes.SQUARE_BRACKETS,
        		Regexes.PARENTHESIS);
        assertDeletedAllOccurences(regex, "<bla>Wand[haus](wand){blub}", "Wand");
    }
    
    @Test
    public void anyTextWithoutAnyBracketsAndTrimed() {
        String regex = any(Regexes.ANGLE_BRACKETS, Regexes.CURLY_BRACES, Regexes.SQUARE_BRACKETS,
        		Regexes.PARENTHESIS, Regexes.DUPLICATE_WHITESPACE);
        String text = "Busch {blub} <adj> Wand";
        assertDeletedAllOccurences(regex, text , "BuschWand");
    }

    @Test
    public void anyShouldMatchOneMoreAlternative() {
        String regex = Regexes.INFINITIVE_TO;
        assertDeletedAllOccurences(regex, "to Wand", "Wand");
    }

    @Test
    public void seqOneMoreShouldMatchSequentially() {
        String regex = seq(Regexes.INFINITIVE_TO, Regexes.PARENTHESIS);
        assertDeletedAllOccurences(regex, "to (Trefferanzeige am Schie\u00DFstand)stay", "stay");
    }

    @Test
    public void proofIfStringIsNotEmpty() {
        String regex = seq(Regexes.WORDS_WITH_POINT);
        assertDeletedAllOccurences(regex, "Because.", "Because.");
    }

    @Test
    public void wordsWithMultiplePointsAreMatched() {
        assertDeletedAllOccurences(Regexes.WORDS_WITH_POINT, "to wand sb.sth.", "to wand");
    }
    
    @Test
    public void proofIfStringIsCorrectlyTrimed() {
        String regex = seq(Regexes.WORDS_WITH_POINT);
        assertDeletedAllOccurences(regex, "sth. bla bla bla.", " bla bla");
    }
}
