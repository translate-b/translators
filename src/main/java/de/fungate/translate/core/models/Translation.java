package de.fungate.translate.core.models;

import javax.annotation.Nonnull;

/**
 * Domain model representing a translation. That is, a pair of an english and a german term.
 * @author Florian Supplie
 */
public class Translation implements Comparable<Translation> {

	private final @Nonnull String english;
	private final @Nonnull String german;

    public Translation(@Nonnull String english, @Nonnull String german) {
        this.english = english;
        this.german = german;
    }

    @Nonnull
    public String getEnglish() {
		return english;
	}

    @Nonnull
    public String getGerman() {
		return german;
	}

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Translation{");
        sb.append("english='").append(english).append('\'');
        sb.append(", german='").append(german).append('\'');
        sb.append('}');
        return sb.toString();
    }

    /**
     * Two translations are considered equal, iff their english and german terms are equal in a case sensitive way.
     * @param o the other object.
     * @return true, iff o is of the same class and is equal to this instance by the criteria stated above.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Translation that = (Translation) o;

        if (!english.equals(that.english)) return false;
        if (!german.equals(that.german)) return false;

        return true;
    }



    @Override
    public int hashCode() {
        int result = english.hashCode();
        result = 31 * result + german.hashCode();
        return result;
    }

    @Override
    public int compareTo(Translation other) {
        if (this == other) return 0;
        int cmp = getEnglish().compareTo(other.getEnglish());
        if (cmp != 0) {
            return cmp;
        }

        return getGerman().compareTo(other.getGerman());
    }
}
