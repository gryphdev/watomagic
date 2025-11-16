package com.parishod.watomagic.replyproviders;

import com.parishod.watomagic.model.preferences.PreferencesManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests para ReplyProviderFactory
 */
@RunWith(MockitoJUnitRunner.class)
public class ReplyProviderFactoryTest {
    
    @Mock
    private PreferencesManager prefs;
    
    @Test
    public void testProviderSelection_BotJsEnabled() {
        // Arrange
        when(prefs.isBotJsEnabled()).thenReturn(true);
        when(prefs.getBotJsUrl()).thenReturn("https://example.com/bot.js");
        
        // Act
        ReplyProvider provider = ReplyProviderFactory.getProvider(prefs);
        
        // Assert
        assertTrue("Should return BotJsReplyProvider when BotJS is enabled",
                   provider instanceof BotJsReplyProvider);
    }
    
    @Test
    public void testProviderSelection_BotJsEnabledButNoUrl() {
        // Arrange
        when(prefs.isBotJsEnabled()).thenReturn(true);
        when(prefs.getBotJsUrl()).thenReturn(null);
        when(prefs.isOpenAIRepliesEnabled()).thenReturn(true);
        
        // Act
        ReplyProvider provider = ReplyProviderFactory.getProvider(prefs);
        
        // Assert
        assertTrue("Should return OpenAIReplyProvider when BotJS enabled but no URL",
                   provider instanceof OpenAIReplyProvider);
    }
    
    @Test
    public void testProviderSelection_BotJsEnabledButEmptyUrl() {
        // Arrange
        when(prefs.isBotJsEnabled()).thenReturn(true);
        when(prefs.getBotJsUrl()).thenReturn("   ");
        when(prefs.isOpenAIRepliesEnabled()).thenReturn(true);
        
        // Act
        ReplyProvider provider = ReplyProviderFactory.getProvider(prefs);
        
        // Assert
        assertTrue("Should return OpenAIReplyProvider when BotJS enabled but empty URL",
                   provider instanceof OpenAIReplyProvider);
    }
    
    @Test
    public void testProviderSelection_OpenAIEnabled() {
        // Arrange
        when(prefs.isBotJsEnabled()).thenReturn(false);
        when(prefs.isOpenAIRepliesEnabled()).thenReturn(true);
        
        // Act
        ReplyProvider provider = ReplyProviderFactory.getProvider(prefs);
        
        // Assert
        assertTrue("Should return OpenAIReplyProvider when OpenAI is enabled",
                   provider instanceof OpenAIReplyProvider);
    }
    
    @Test
    public void testProviderSelection_DefaultStatic() {
        // Arrange
        when(prefs.isBotJsEnabled()).thenReturn(false);
        when(prefs.isOpenAIRepliesEnabled()).thenReturn(false);
        
        // Act
        ReplyProvider provider = ReplyProviderFactory.getProvider(prefs);
        
        // Assert
        assertTrue("Should return StaticReplyProvider as default",
                   provider instanceof StaticReplyProvider);
    }
    
    @Test
    public void testProviderSelection_PriorityOrder() {
        // Arrange - BotJS tiene prioridad sobre OpenAI
        when(prefs.isBotJsEnabled()).thenReturn(true);
        when(prefs.getBotJsUrl()).thenReturn("https://example.com/bot.js");
        when(prefs.isOpenAIRepliesEnabled()).thenReturn(true);
        
        // Act
        ReplyProvider provider = ReplyProviderFactory.getProvider(prefs);
        
        // Assert
        assertTrue("BotJS should have priority over OpenAI",
                   provider instanceof BotJsReplyProvider);
    }
}
