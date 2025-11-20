# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Mission

**Watomagic** is a fork of [Watomatic](https://github.com/adeekshith/watomatic) with ONE PRIMARY GOAL:

**Add downloadable JavaScript bot functionality (`bot.js`) while maintaining 100% compatibility with upstream Watomatic updates.**

### Core Requirements
1. **Upstream Compatibility**: Must be able to merge Watomatic updates without conflicts
2. **GUI Required**: Bot configuration must have a graphical interface
3. **Respect Watomatic Architecture**: Don't break existing patterns
4. **DRY/KISS Principles**: Simple, efficient, synthetic code with visual documentation
5. **Bot Capability**: JavaScript bots must be able to implement custom logic and communicate with external APIs

### Project Philosophy
- **Simplicity over complexity**: Fewer features done well > many features done poorly
- **Maintainability first**: Code should be self-documenting and easy to understand
- **Minimal invasiveness**: Keep changes to upstream code minimal and isolated

## Build Commands

### Building the Project
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (unsigned) for Default flavor (F-Droid variant)
./gradlew assembleDefaultRelease

# Build release APK for GooglePlay flavor
./gradlew assembleGooglePlayRelease

# Build all variants
./gradlew build
```

### Testing
```bash
# Run all unit tests
./gradlew test

# Run unit tests with coverage
./gradlew testDebugUnitTest --info

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests com.parishod.watomatic.model.utils.AppUtilsTest
```

### Code Quality
```bash
# Run lint checks
./gradlew lint

# Run lint for specific flavor
./gradlew lintDefaultRelease
./gradlew lintGooglePlayRelease

# Clean build
./gradlew clean
```

### Development
```bash
# List all available tasks
./gradlew tasks

# Install debug APK on connected device
./gradlew installDebug

# Uninstall from device
adb uninstall com.parishod.watomagic
```

## Architecture Overview

### The Strategy Pattern Solution

To maintain upstream compatibility, we use the **Strategy Pattern** for reply generation. This isolates BotJS code from Watomatic core:

```
Incoming Notification → NotificationService (Watomatic)
    ↓
ReplyProviderFactory (NEW - minimal bridge)
    ├─ StaticReplyProvider (Watomatic logic extracted)
    ├─ OpenAIReplyProvider (Watomatic logic extracted)
    └─ BotJsReplyProvider (NEW - our addition)
         ↓
    BotJsEngine (NEW - QuickJS sandbox)
         ↓
    User's bot.js script
```

### Why This Architecture?

**Upstream Compatibility**: Only ~20 lines changed in `NotificationService.sendReply()` - all BotJS code lives in separate packages that upstream doesn't touch.

**Original Watomatic Code** (~150 lines in sendReply):
```java
private void sendReply(StatusBarNotification sbn) {
    // 150+ lines of OpenAI logic, static replies, etc.
    // Hard to maintain, impossible to extend without conflicts
}
```

**Our Refactored Approach** (~20 lines):
```java
private void sendReply(StatusBarNotification sbn) {
    // Extract notification data
    // Select provider (Factory Pattern)
    // Execute with callback
    // Send reply or fallback
}
```

All complexity moved to **isolated providers** in the `com.parishod.watomagic` package.

### Package Structure

```
com.parishod.watomagic/          # ALL new BotJS code (isolated from upstream)
├── replyproviders/              # Strategy pattern for reply generation
│   ├── ReplyProvider.java       # Interface
│   ├── ReplyProviderFactory.java # Selector (✅ integrated BotJsReplyProvider)
│   ├── StaticReplyProvider.java  # Watomatic static replies (extracted)
│   ├── OpenAIReplyProvider.java  # Watomatic OpenAI (extracted)
│   ├── BotJsReplyProvider.java  # ✅ NEW: Bot execution provider (147 lines)
│   └── model/NotificationData.java
├── botjs/                       # QuickJS runtime + management
│   ├── BotJsEngine.java         # JavaScript executor
│   ├── BotAndroidAPI.java       # APIs exposed to bots
│   ├── BotValidator.java        # Security validation
│   ├── TimeoutExecutor.java     # 5s timeout enforcement
│   ├── RateLimiter.java         # 100 exec/min limit
│   ├── BotRepository.java       # ✅ NEW: Download/update system (268 lines)
│   └── BotExecutionException.java
├── activity/botconfig/          # ✅ NEW: Bot configuration UI
│   └── BotConfigActivity.kt     # Material 3 GUI (219 lines)
├── workers/                     # ✅ NEW: Background tasks
│   └── BotUpdateWorker.java     # Auto-update every 6h (96 lines)

com.parishod.watomatic/          # ORIGINAL Watomatic code (preserve compatibility)
├── service/NotificationService  # Modified minimally (~20 lines)
├── model/preferences/           # ✅ Extended: +8 BotJS methods (53 lines added)
│   └── PreferencesManager.java
├── fragment/SettingsFragment    # ✅ Modified: +14 lines for BotConfig entry
└── ... (rest unchanged)
```

## BotJS Implementation Status

### Current State (2025-11-19) - ✅ COMPLETE
- ✅ Strategy Pattern architecture implemented
- ✅ TypeScript definitions for bot developers (`bot-types.d.ts`)
- ✅ Example bot with real-world patterns (`example-bot.js`)
- ✅ QuickJS runtime scaffolding (engine, APIs, validators)
- ✅ **BotJsReplyProvider fully integrated** (commit 745fd66)
- ✅ **GUI implemented** - Material 3 BotConfigActivity (commit 745fd66)
- ✅ **Download/update system implemented** - BotRepository + BotUpdateWorker (commit 745fd66)
- ✅ **Auto-updates** - WorkManager scheduled every 6 hours
- ✅ **SHA-256 validation** - Optional hash verification on download
- ✅ **Import verification** - Automated script for checking imports (commit fff410c)
- ✅ **Compilation successful** - All components compile without errors

### What Bots Can Do

Bots receive notifications and return actions:

```javascript
// bot.js - User's custom logic
async function processNotification(notification) {
    // notification = { appPackage, title, body, timestamp, isGroup, ... }

    // Example: Call external API
    const response = await Android.httpRequest({
        url: 'https://api.example.com/classify',
        method: 'POST',
        headers: { 'Authorization': 'Bearer KEY' },
        body: JSON.stringify({ message: notification.body })
    });

    const data = JSON.parse(response);

    // Return action: REPLY, DISMISS, KEEP, or SNOOZE
    return {
        action: 'REPLY',
        replyText: data.suggestedReply
    };
}
```

### Bot APIs (Android object)
- `Android.log(level, message)` - Logging to Logcat
- `Android.storageGet/Set/Remove(key)` - Persistent key-value storage
- `Android.httpRequest(options)` - HTTP/HTTPS requests (HTTPS only)
- `Android.getCurrentTime()` - Timestamp for rate limiting
- `Android.getAppName(packageName)` - Friendly app names

### Bot Security
- **Timeout**: 5 seconds max execution
- **Size limit**: 100 KB max
- **Sandbox**: No filesystem, no eval(), no dangerous patterns
- **HTTPS only**: All network requests must be secure
- **Rate limiting**: 100 executions per minute

## Key Design Decisions

### 1. Minimal Upstream Modifications
**Files Modified in Watomatic Code**:
- `NotificationService.sendReply()`: ~20 line refactor (was 150+ lines)
- `PreferencesManager`: Add BotJS preference keys
- Future: Add BotConfig activity entry in settings

**Everything Else**: New code in `com.parishod.watomagic` package (zero conflicts).

### 2. QuickJS Over V8/Rhino
- **Size**: ~2 MB vs ~7 MB (V8)
- **ES2020 support**: Modern JavaScript
- **Maintained**: Active development by Cash App
- **Already added**: Dependency in `app/build.gradle.kts:114`

### 3. No Over-Engineering
The `docs/PLAN_BOTJS_SYSTEM.md` describes many "nice to have" features. **Focus on essentials**:
- ✅ Bot execution (processNotification → BotResponse)
- ✅ HTTP API calls (Android.httpRequest)
- ✅ Persistent storage (Android.storage*)
- ✅ GUI for bot configuration
- ✅ Basic security (timeout, validation, HTTPS)
- ❓ Auto-updates, marketplace, advanced features → Future/optional

### 4. GUI Design Philosophy
- **Material 3 design** (matches Watomatic)
- **Simple cards**: Enable/disable, URL input, bot info, test button
- **Integrated in settings**: Natural extension of existing UI
- **Clear feedback**: Success/error states, progress indicators

## Development Guidelines

### When Adding BotJS Features

1. **Check upstream impact**: Will this conflict with Watomatic updates?
2. **Isolate in watomagic package**: Keep separation clean
3. **Follow KISS**: Simplest solution that works
4. **Document visually**: Code should be self-explanatory
5. **Test fallback**: What happens if bot fails? (Answer: use static reply)

### Code Style Principles

✅ **DO**:
```java
// Clear, self-documenting code
public class BotJsEngine {
    private static final int TIMEOUT_MS = 5000;

    public String executeBot(String jsCode, String notificationJson) {
        // Execute with timeout, return response JSON
    }
}
```

❌ **DON'T**:
```java
// Over-abstracted, hard to follow
public class AbstractBotExecutionStrategyFactoryBuilder {
    private AbstractBotExecutionStrategyFactoryBuilder() {}
    // ...10 layers of indirection...
}
```

### Testing Strategy
- **Unit tests**: For all providers, validators, utilities
- **Mock external deps**: QuickJS, HTTP, OpenAI
- **Integration test**: Full flow (notification → bot → reply)
- **Target**: >75% coverage for BotJS code

### Merging Upstream Watomatic

```bash
# Setup (one time)
git remote add upstream https://github.com/adeekshith/watomatic.git

# Regular sync
git fetch upstream
git checkout main
git merge upstream/main

# Conflicts should be minimal:
# - NotificationService.sendReply(): Keep our ~20 line version
# - PreferencesManager: Keep both their keys and our BotJS keys
# - Build files: Keep our QuickJS dependency
```

## Important Files

### Documentation
- `docs/PLAN_BOTJS_SYSTEM.md` - Complete roadmap (reference, not gospel)
- `docs/ARCHITECTURE.md` - Technical architecture
- `docs/BOT_DEVELOPMENT_GUIDE.md` - How to write bots
- `docs/BOT_API_REFERENCE.md` - Bot API contract

### Bot Assets
- `app/src/main/assets/bot-types.d.ts` - TypeScript definitions
- `app/src/main/assets/example-bot.js` - Reference implementation

### Configuration
- `app/build.gradle.kts` - Dependencies (QuickJS on line 114)
- `codemagic.yaml` - CI/CD pipeline
- `gradle.properties` - Gradle configuration

### Core Implementation
- `app/src/main/java/com/parishod/watomatic/service/NotificationService.java` - Entry point
- `app/src/main/java/com/parishod/watomagic/replyproviders/` - Strategy pattern
- `app/src/main/java/com/parishod/watomagic/botjs/` - QuickJS runtime

## Implementation Complete ✅

All core features have been implemented as of 2025-11-19:

1. ✅ **BotJsEngine connected** - Runtime fully functional
2. ✅ **BotJsReplyProvider created** - Bot execution integrated into notification flow
3. ✅ **GUI built** - Material 3 BotConfigActivity with complete bot management
4. ✅ **BotRepository implemented** - Download from HTTPS, SHA-256 validation, metadata
5. ✅ **Auto-updates** - WorkManager scheduling, background updates
6. ✅ **Error handling** - Comprehensive error messages, user feedback, edge cases covered
7. ✅ **Import verification** - Automated script (`scripts/check_imports.sh`)

### What's Working Now

- **Download bots** from any HTTPS URL with optional SHA-256 validation
- **Execute bots** automatically on incoming notifications
- **Auto-update** bots every 6 hours (configurable)
- **GUI configuration** - Enable/disable, set URL, view bot info, test execution
- **Secure sandbox** - 5s timeout, size limits, HTTPS-only, rate limiting
- **Persistent storage** - Bot metadata (URL, hash, timestamp)
- **Fallback system** - If bot fails, falls back to static/OpenAI replies

### Next Steps (Optional Enhancements)

1. **Testing** - Add comprehensive unit + integration tests
2. **Bot marketplace** - Curated list of verified bots
3. **Advanced GUI** - Bot logs viewer, performance metrics
4. **Enhanced validation** - More security checks, signature verification
5. **Documentation** - User guide for non-technical users

## Critical Reminders

- ⚠️ **Always test upstream merge compatibility**
- ⚠️ **GUI is mandatory, not optional**
- ⚠️ **Simplicity > Features**
- ⚠️ **Code must be visually documented (clear variable names, structure)**
- ⚠️ **DRY/KISS > Clever abstractions**
- ⚠️ **When in doubt, check what Watomatic does and follow that pattern**

## Dependencies

### Android & Kotlin
- Kotlin 2.1.0
- Android SDK: minSdk=24, targetSdk=35, compileSdk=35
- Java 17

### Core Libraries (Already in Watomatic)
- AndroidX (AppCompat, Material, ConstraintLayout, etc.)
- Room Database
- Retrofit + OkHttp (reuse for bot downloads!)
- Gson (JSON parsing)
- WorkManager (for auto-updates if implemented)

### BotJS Addition
- **QuickJS Android 0.9.2** - JavaScript engine (~2 MB added to APK)

### Build Flavors
- **Default**: F-Droid variant (`com.parishod.watomagic`)
- **GooglePlay**: Google Play variant (`com.parishod.atomatic`) with Firebase

## Project Context

This is a **fork with a specific mission**: Add JavaScript bot capability to Watomatic while staying mergeable with upstream. The extensive documentation in `docs/` was created during planning - use it as reference but don't feel obligated to implement every detail. **Pragmatism over perfectionism.**
