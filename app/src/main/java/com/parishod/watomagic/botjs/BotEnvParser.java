package com.parishod.watomagic.botjs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Parses bot environment variables from a multiline string.
 * Format: KEY='value' or KEY="value" (one per line).
 */
public final class BotEnvParser {

    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private BotEnvParser() {}

    @NonNull
    public static Map<String, String> parse(@Nullable String text) {
        Map<String, String> result = new LinkedHashMap<>();
        if (text == null || text.isEmpty()) {
            return result;
        }

        for (String rawLine : text.split("\n", -1)) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            ParsedLine parsed = parseLine(line);
            if (parsed != null) {
                result.put(parsed.key, parsed.value);
            }
        }
        return result;
    }

    /** Returns the number of non-empty, non-comment lines that could not be parsed. */
    public static int countInvalidLines(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int invalid = 0;
        for (String rawLine : text.split("\n", -1)) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (parseLine(line) == null) {
                invalid++;
            }
        }
        return invalid;
    }

    @Nullable
    private static ParsedLine parseLine(@NonNull String line) {
        int eqIndex = line.indexOf('=');
        if (eqIndex <= 0) {
            return null;
        }

        String key = line.substring(0, eqIndex).trim();
        if (!KEY_PATTERN.matcher(key).matches()) {
            return null;
        }

        String rest = line.substring(eqIndex + 1).trim();
        if (rest.length() < 2) {
            return null;
        }

        char quote = rest.charAt(0);
        if (quote != '\'' && quote != '"') {
            return null;
        }
        if (rest.charAt(rest.length() - 1) != quote) {
            return null;
        }

        String value = rest.substring(1, rest.length() - 1);
        return new ParsedLine(key, value);
    }

    private static final class ParsedLine {
        final String key;
        final String value;

        ParsedLine(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
