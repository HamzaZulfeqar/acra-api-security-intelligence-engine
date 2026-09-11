package io.acra.core.extraction.defaults;

import io.acra.core.domain.common.*;
import io.acra.core.domain.uri.*;
import java.util.Locale;
import java.util.regex.Pattern;

public final class IdentifierDetector {
    private static final Pattern UUID = Pattern.compile("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
    private static final Pattern HEX = Pattern.compile("(?i)^(?:0x)?[0-9a-f]{8,}$");
    private static final Pattern SLUG = Pattern.compile("^[A-Za-z0-9]+(?:[-_][A-Za-z0-9]+)+$");
    private static final Pattern NUMERIC = Pattern.compile("^[0-9]+$");

    public IdentifierCandidate detect(String value, IdentifierLocation location, String source, String nameHint) {
        if (value == null || value.isBlank()) return null;
        if (UUID.matcher(value).matches()) return candidate(value, location, IdentifierType.UUID, ConfidenceBasis.EXACT_OBSERVED, "uuid-pattern", source);
        if (NUMERIC.matcher(value).matches()) {
            IdentifierType t = value.length() > 1 && value.startsWith("0") ? IdentifierType.NUMERIC_STRING : IdentifierType.INTEGER;
            ConfidenceBasis b = nameLooksLikeId(nameHint) ? ConfidenceBasis.EXPLICIT_METADATA : ConfidenceBasis.STRUCTURAL_INFERENCE;
            return candidate(value, location, t, b, nameLooksLikeId(nameHint) ? "identifier-name-and-numeric-value" : "numeric-value", source);
        }
        if (HEX.matcher(value).matches() && value.chars().anyMatch(c -> Character.isLetter(c)))
            return candidate(value, location, IdentifierType.HEXADECIMAL, ConfidenceBasis.HEURISTIC, "hex-pattern", source);
        if (nameLooksLikeId(nameHint))
            return candidate(value, location, IdentifierType.NAMED_IDENTIFIER, ConfidenceBasis.EXPLICIT_METADATA, "identifier-naming-convention", source);
        if (value.length() >= 20 && entropy(value) >= 3.5)
            return candidate(value, location, IdentifierType.HIGH_ENTROPY_TOKEN, ConfidenceBasis.HEURISTIC, "high-entropy-token-pattern", source);
        if (SLUG.matcher(value).matches())
            return candidate(value, location, IdentifierType.SLUG, ConfidenceBasis.WEAK_HEURISTIC, "slug-pattern", source);
        return null;
    }

    private static boolean nameLooksLikeId(String hint) {
        if (hint == null) return false;
        String s=hint.toLowerCase(Locale.ROOT).replace('-', '_');
        return s.equals("id") || s.endsWith("_id") || s.endsWith("id") || s.contains("identifier");
    }
    private static IdentifierCandidate candidate(String v, IdentifierLocation l, IdentifierType t, ConfidenceBasis b, String reason, String src) {
        return new IdentifierCandidate(v,l,t,Confidence.of(b),reason,src);
    }
    private static double entropy(String s) {
        int[] counts=new int[256];
        for (char c:s.toCharArray()) if(c<256) counts[c]++;
        double h=0.0, n=s.length();
        for(int c:counts) if(c>0){ double p=c/n; h-=p*(Math.log(p)/Math.log(2)); }
        return h;
    }
}
