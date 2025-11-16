package com.parishod.watomagic.botjs;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests para BotValidator
 */
public class BotValidatorTest {
    
    @Test
    public void testValidation_ValidBot() {
        // Arrange
        String validBot = "async function processNotification(notification) { " +
                         "return { action: 'KEEP' }; " +
                         "}";
        
        // Act
        boolean result = BotValidator.validate(validBot);
        
        // Assert
        assertTrue("Valid bot should pass validation", result);
    }
    
    @Test
    public void testValidation_NullCode() {
        // Act
        boolean result = BotValidator.validate(null);
        
        // Assert
        assertFalse("Null code should fail validation", result);
    }
    
    @Test
    public void testValidation_EmptyCode() {
        // Act
        boolean result = BotValidator.validate("");
        
        // Assert
        assertFalse("Empty code should fail validation", result);
    }
    
    @Test
    public void testValidation_WhitespaceOnly() {
        // Act
        boolean result = BotValidator.validate("   \n\t  ");
        
        // Assert
        assertFalse("Whitespace-only code should fail validation", result);
    }
    
    @Test
    public void testValidation_TooLarge() {
        // Arrange - Crear un bot de más de 100KB
        StringBuilder largeBot = new StringBuilder();
        largeBot.append("async function processNotification(notification) { ");
        // Agregar contenido hasta superar 100KB
        for (int i = 0; i < 20000; i++) {
            largeBot.append("// This is a comment to make the bot larger. ");
        }
        largeBot.append("return { action: 'KEEP' }; }");
        
        // Act
        boolean result = BotValidator.validate(largeBot.toString());
        
        // Assert
        assertFalse("Bot larger than 100KB should fail validation", result);
    }
    
    @Test
    public void testValidation_DangerousPattern_Eval() {
        // Arrange
        String dangerousBot = "async function processNotification(notification) { " +
                             "eval('malicious code'); " +
                             "return { action: 'KEEP' }; }";
        
        // Act
        boolean result = BotValidator.validate(dangerousBot);
        
        // Assert
        assertFalse("Bot with eval() should fail validation", result);
    }
    
    @Test
    public void testValidation_DangerousPattern_Function() {
        // Arrange
        String dangerousBot = "async function processNotification(notification) { " +
                             "Function('malicious code'); " +
                             "return { action: 'KEEP' }; }";
        
        // Act
        boolean result = BotValidator.validate(dangerousBot);
        
        // Assert
        assertFalse("Bot with Function() should fail validation", result);
    }
    
    @Test
    public void testValidation_DangerousPattern_Constructor() {
        // Arrange
        String dangerousBot = "async function processNotification(notification) { " +
                             "constructor['prototype'] = {}; " +
                             "return { action: 'KEEP' }; }";
        
        // Act
        boolean result = BotValidator.validate(dangerousBot);
        
        // Assert
        assertFalse("Bot with constructor[] should fail validation", result);
    }
    
    @Test
    public void testValidation_DangerousPattern_Proto() {
        // Arrange
        String dangerousBot = "async function processNotification(notification) { " +
                             "obj.__proto__ = {}; " +
                             "return { action: 'KEEP' }; }";
        
        // Act
        boolean result = BotValidator.validate(dangerousBot);
        
        // Assert
        assertFalse("Bot with __proto__ should fail validation", result);
    }
    
    @Test
    public void testValidation_DangerousPattern_Import() {
        // Arrange
        String dangerousBot = "async function processNotification(notification) { " +
                             "import('malicious-module'); " +
                             "return { action: 'KEEP' }; }";
        
        // Act
        boolean result = BotValidator.validate(dangerousBot);
        
        // Assert
        assertFalse("Bot with import() should fail validation", result);
    }
    
    @Test
    public void testValidation_MissingProcessNotification() {
        // Arrange
        String invalidBot = "function otherFunction() { return 'test'; }";
        
        // Act
        boolean result = BotValidator.validate(invalidBot);
        
        // Assert
        assertFalse("Bot without processNotification should fail validation", result);
    }
    
    @Test
    public void testValidation_CaseInsensitivePatterns() {
        // Arrange - Probar que los patrones son case-insensitive
        String dangerousBot = "async function processNotification(notification) { " +
                             "EVAL('malicious code'); " +
                             "return { action: 'KEEP' }; }";
        
        // Act
        boolean result = BotValidator.validate(dangerousBot);
        
        // Assert
        assertFalse("Pattern matching should be case-insensitive", result);
    }
    
    @Test
    public void testValidation_ValidWithComments() {
        // Arrange - Bot válido con comentarios que contienen palabras peligrosas
        String validBot = "// This comment mentions eval but it's not code\n" +
                         "async function processNotification(notification) { " +
                         "return { action: 'KEEP' }; }";
        
        // Act
        boolean result = BotValidator.validate(validBot);
        
        // Assert
        assertTrue("Comments with dangerous words should not fail validation", result);
    }
}
