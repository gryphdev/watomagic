package com.parishod.watomagic.botjs;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Comprueba reglas básicas antes de ejecutar un bot descargado.
 */
public final class BotValidator {

    private static final String TAG = "BotValidator";
    private static final int MAX_BOT_SIZE_BYTES = 102_400; // 100 KB
    private static final int CONTEXT_LOOKBEHIND = 30; // Characters to check before match
    
    // Patterns that need context checking (to avoid false positives)
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("Function\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern FUNCTION_KEYWORD_PATTERN = Pattern.compile("function\\s+", Pattern.CASE_INSENSITIVE);
    
    // Patterns that don't need context checking
    private static final Pattern[] BLACKLISTED_PATTERNS = new Pattern[]{
            Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("constructor\\s*\\[", Pattern.CASE_INSENSITIVE),
            Pattern.compile("__proto__", Pattern.CASE_INSENSITIVE),
            Pattern.compile("import\\s*\\(", Pattern.CASE_INSENSITIVE)
    };

    private BotValidator() {
        // Utility class
    }

    public static boolean validate(String jsCode) {
        if (jsCode == null) {
            String message = "Bot code is null";
            Log.w(TAG, message);
            if (BotLogCapture.isEnabled()) {
                BotLogCapture.addLog("error", message);
            }
            return false;
        }

        if (jsCode.trim().isEmpty()) {
            String message = "Bot code is empty";
            Log.w(TAG, message);
            if (BotLogCapture.isEnabled()) {
                BotLogCapture.addLog("error", message);
            }
            return false;
        }

        int botSize = jsCode.getBytes(StandardCharsets.UTF_8).length;
        if (botSize > MAX_BOT_SIZE_BYTES) {
            String message = String.format("Bot too large: %d bytes (max: %d bytes)", botSize, MAX_BOT_SIZE_BYTES);
            Log.w(TAG, message);
            if (BotLogCapture.isEnabled()) {
                BotLogCapture.addLog("error", message);
            }
            return false;
        }

        // Check patterns that don't need context
        for (Pattern pattern : BLACKLISTED_PATTERNS) {
            java.util.regex.Matcher matcher = pattern.matcher(jsCode);
            if (matcher.find()) {
                int position = matcher.start();
                String patternName = pattern.pattern();
                String context = getCodeContext(jsCode, position, 50);
                String message = String.format(
                    "Dangerous pattern detected: '%s' at position %d. Context: %s",
                    patternName, position, context
                );
                Log.w(TAG, message);
                if (BotLogCapture.isEnabled()) {
                    BotLogCapture.addLog("error", message);
                }
                return false;
            }
        }
        
        // Check Function( pattern with context validation (to avoid false positives with function declarations)
        java.util.regex.Matcher functionMatcher = FUNCTION_PATTERN.matcher(jsCode);
        while (functionMatcher.find()) {
            int position = functionMatcher.start();
            
            // Check if this is part of a function declaration (safe) vs Function constructor (dangerous)
            if (!isInFunctionDeclaration(jsCode, position)) {
                String context = getCodeContext(jsCode, position, 50);
                String message = String.format(
                    "Dangerous pattern detected: 'Function(' constructor at position %d. " +
                    "The Function() constructor can execute arbitrary code and is not allowed. " +
                    "Context: %s",
                    position, context
                );
                Log.w(TAG, message);
                if (BotLogCapture.isEnabled()) {
                    BotLogCapture.addLog("error", message);
                }
                return false;
            }
        }

        if (!jsCode.contains("processNotification")) {
            String message = "Missing processNotification function. Bot code must contain a function named 'processNotification'";
            Log.w(TAG, message);
            if (BotLogCapture.isEnabled()) {
                BotLogCapture.addLog("error", message);
            }
            return false;
        }

        return true;
    }
    
    /**
     * Checks if a Function( match is part of a safe function declaration
     * (e.g., "function myFunction()") vs dangerous Function constructor
     * (e.g., "Function('code')" or "new Function(...)")
     * 
     * The pattern matches "Function(" case-insensitively, so it can match:
     * - "function(" at the start of "function myFunction()" (safe)
     * - "Function(" in "Function('code')" (dangerous)
     * - "Function(" in "new Function(...)" (dangerous)
     * 
     * @return true if it's a safe function declaration, false if it's dangerous
     */
    private static boolean isInFunctionDeclaration(String jsCode, int matchPosition) {
        // Check what the matched text actually is
        String matchedText = jsCode.substring(matchPosition, Math.min(jsCode.length(), matchPosition + 8));
        boolean isLowercaseFunction = matchedText.toLowerCase().startsWith("function");
        
        // Check if "new" appears before the match (indicates "new Function(...)")
        int contextStart = Math.max(0, matchPosition - 10);
        String contextBefore = jsCode.substring(contextStart, matchPosition);
        if (contextBefore.trim().toLowerCase().endsWith("new")) {
            // "new Function(...)" - dangerous
            return false;
        }
        
        // If the match is "function(" (lowercase), check if it's followed by an identifier
        // This would indicate "function myFunction()" which is safe
        if (isLowercaseFunction) {
            // Check what comes after "function"
            int afterFunctionStart = matchPosition + 8; // Skip "function"
            if (afterFunctionStart < jsCode.length()) {
                String afterFunction = jsCode.substring(afterFunctionStart, 
                    Math.min(jsCode.length(), afterFunctionStart + 50));
                String trimmed = afterFunction.trim();
                
                // Check if it starts with a valid identifier followed by "("
                // This would be "function identifier(" pattern
                if (trimmed.matches("^[a-zA-Z_$][a-zA-Z0-9_$]*\\s*\\(")) {
                    // Looks like "function identifier(" - safe function declaration
                    return true;
                }
            }
        }
        
        // Check if there's a "function" keyword before this match (not the match itself)
        // This handles cases where we matched something else but there's a function declaration nearby
        int lookbehindStart = Math.max(0, matchPosition - CONTEXT_LOOKBEHIND);
        String beforeMatch = jsCode.substring(lookbehindStart, matchPosition);
        java.util.regex.Matcher functionKeywordMatcher = FUNCTION_KEYWORD_PATTERN.matcher(beforeMatch);
        
        // Find the last "function" keyword before the match
        int lastFunctionPos = -1;
        while (functionKeywordMatcher.find()) {
            lastFunctionPos = functionKeywordMatcher.start();
        }
        
        if (lastFunctionPos >= 0 && !isLowercaseFunction) {
            // There's a "function" keyword before, and the match isn't "function" itself
            // Check if there's an identifier between "function" and the match
            String between = beforeMatch.substring(lastFunctionPos + 8); // After "function"
            String trimmed = between.trim();
            if (trimmed.matches("^[a-zA-Z_$][a-zA-Z0-9_$]*\\s*$")) {
                // Valid identifier between "function" and match - likely safe
                return true;
            }
        }
        
        // Default: if it's not clearly a function declaration, treat as dangerous
        return false;
    }
    
    /**
     * Gets a snippet of code around a position for error messages
     */
    private static String getCodeContext(String jsCode, int position, int contextLength) {
        int start = Math.max(0, position - contextLength / 2);
        int end = Math.min(jsCode.length(), position + contextLength / 2);
        String snippet = jsCode.substring(start, end);
        
        // Replace newlines and tabs with spaces for readability
        snippet = snippet.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
        
        // Mark the position
        int relativePos = position - start;
        if (relativePos >= 0 && relativePos < snippet.length()) {
            snippet = snippet.substring(0, relativePos) + ">>>" + snippet.substring(relativePos);
        }
        
        return snippet.trim();
    }
}
