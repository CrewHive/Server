package com.pat.crewhive.common;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class PasswordUtil {

    private static final int MIN_LEN = 12;
    private static final int MAX_LEN = 20;

    private final PasswordEncoder passwordEncoder;

    // Hash fittizio calcolato una volta con lo stesso encoder: serve a spendere lo stesso tempo di un
    // confronto reale quando l'account non esiste o e' disattivato (vedi burnMatch).
    private final String dummyHash;

    public PasswordUtil(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * Checks if the password is strong according to the following criteria:
     * - Length between 8 and 20 characters
     * - Contains at least one uppercase letter
     * - Contains at least one lowercase letter
     * - Contains at least one digit
     * - Contains at least one symbol or punctuation character

     * @param password the password to check
     * @return true if the password is strong, false otherwise
     */
    public boolean isStrong(String password) {

        if (password == null) return false;

        final int len = password.codePointCount(0, password.length());

        if (len < MIN_LEN || len > MAX_LEN) return false;

        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSymbol = false;

        // Single-pass sui code point (Unicode-safe)
        for (int i = 0; i < password.length(); ) {

            final int cp = password.codePointAt(i);

            if (Character.isUpperCase(cp)) hasUpper = true;
            else if (Character.isLowerCase(cp)) hasLower = true;
            else if (Character.isDigit(cp)) hasDigit = true;
            else if (isSymbolOrPunctuation(cp)) hasSymbol = true;
            i += Character.charCount(cp);
            if (hasUpper && hasLower && hasDigit && hasSymbol) break;
        }

        return hasUpper && hasLower && hasDigit && hasSymbol;
    }

    /**
     * Checks if the code point is a symbol or punctuation character.
     *
     * @param cp the code point to check
     * @return true if the code point is a symbol or punctuation, false otherwise
     */
    private boolean isSymbolOrPunctuation(int cp) {

        int type = Character.getType(cp);

        return type == Character.OTHER_PUNCTUATION
                || type == Character.DASH_PUNCTUATION
                || type == Character.MATH_SYMBOL
                || type == Character.CURRENCY_SYMBOL
                || type == Character.MODIFIER_SYMBOL
                || type == Character.OTHER_SYMBOL
                || type == Character.START_PUNCTUATION
                || type == Character.END_PUNCTUATION
                || type == Character.INITIAL_QUOTE_PUNCTUATION
                || type == Character.FINAL_QUOTE_PUNCTUATION
                || type == Character.CONNECTOR_PUNCTUATION;
    }

    /**
     * Encodes the raw password using the configured PasswordEncoder.
     *
     * @param raw the raw password to encode
     * @return the encoded password
     * @throws NullPointerException if the raw password is null
     */
    public String encodePassword(String raw) {
        Objects.requireNonNull(raw, "raw password must not be null");
        return passwordEncoder.encode(raw);
    }

    /**
     * Checks if the raw password does not match the encoded password.
     *
     * @param rawPassword the raw password to check
     * @param encodedPassword the encoded password to compare against
     * @return true if the raw password does not match the encoded password, false otherwise
     */
    public boolean NotMatches(String rawPassword, String encodedPassword) {
        return !passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * Esegue un confronto contro un hash fittizio e scarta il risultato. Va chiamato quando non c'e'
     * un hash reale da verificare (account inesistente o disattivato), cosi' il tempo di risposta
     * non rivela se l'account esiste.
     *
     * @param rawPassword the raw password received in the request
     */
    public void burnMatch(String rawPassword) {
        passwordEncoder.matches(rawPassword, dummyHash);
    }

}
