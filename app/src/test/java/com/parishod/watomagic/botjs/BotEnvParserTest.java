package com.parishod.watomagic.botjs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Map;

public class BotEnvParserTest {

    @Test
    public void parse_empty_returnsEmptyMap() {
        assertTrue(BotEnvParser.parse(null).isEmpty());
        assertTrue(BotEnvParser.parse("").isEmpty());
        assertTrue(BotEnvParser.parse("   \n  ").isEmpty());
    }

    @Test
    public void parse_singleQuotes() {
        Map<String, String> env = BotEnvParser.parse("API_KEY='sk-abc123'");
        assertEquals("sk-abc123", env.get("API_KEY"));
    }

    @Test
    public void parse_doubleQuotes() {
        Map<String, String> env = BotEnvParser.parse("WEBHOOK_URL=\"https://example.com\"");
        assertEquals("https://example.com", env.get("WEBHOOK_URL"));
    }

    @Test
    public void parse_multipleLines() {
        String text = "API_KEY='abc'\nWEBHOOK='https://x.com'\nTOKEN=\"xyz\"";
        Map<String, String> env = BotEnvParser.parse(text);
        assertEquals(3, env.size());
        assertEquals("abc", env.get("API_KEY"));
        assertEquals("https://x.com", env.get("WEBHOOK"));
        assertEquals("xyz", env.get("TOKEN"));
    }

    @Test
    public void parse_ignoresCommentsAndEmptyLines() {
        String text = "# comment\n\nAPI_KEY='val'\n# another";
        Map<String, String> env = BotEnvParser.parse(text);
        assertEquals(1, env.size());
        assertEquals("val", env.get("API_KEY"));
    }

    @Test
    public void parse_valueWithSpecialChars() {
        Map<String, String> env = BotEnvParser.parse("MSG='hello: world! @#$%'");
        assertEquals("hello: world! @#$%", env.get("MSG"));
    }

    @Test
    public void parse_invalidLinesIgnored() {
        Map<String, String> env = BotEnvParser.parse("VALID='ok'\ninvalid line\nNO_QUOTES=bad");
        assertEquals(1, env.size());
        assertEquals("ok", env.get("VALID"));
    }

    @Test
    public void parse_laterKeyOverridesEarlier() {
        Map<String, String> env = BotEnvParser.parse("KEY='first'\nKEY='second'");
        assertEquals("second", env.get("KEY"));
    }

    @Test
    public void countInvalidLines() {
        assertEquals(0, BotEnvParser.countInvalidLines(""));
        assertEquals(0, BotEnvParser.countInvalidLines("KEY='ok'"));
        assertEquals(2, BotEnvParser.countInvalidLines("KEY='ok'\nbad\nNOQ=val"));
    }

    @Test
    public void parse_invalidKeyName_ignored() {
        assertTrue(BotEnvParser.parse("123='bad'").isEmpty());
    }
}
