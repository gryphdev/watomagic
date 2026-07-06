[English](./README_en.md) · [Español](./README.md)

# 🪄 Watomagic - Auto reply for messaging apps

Watomagic sends an automated reply to everyone who contacts you on messaging apps. It is especially useful if you are planning to migrate away from these apps, but you can also use it as a vacation auto-responder.

<a href='https://play.google.com/store/apps/details?id=com.parishod.atomatic'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' height="60" /></a>
[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="60">](https://f-droid.org/en/packages/com.parishod.watomatic/)
<a href='https://apt.izzysoft.de/fdroid/index/apk/com.parishod.watomatic'><img alt='Get it on F-Droid via IzzyOnDroid' src='https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png' height="60" /></a>

### 📸 [Screenshots](./media/screenshots/README_en.md)

| [<img src="./media/screenshots/3.png" alt="Settings Bot JavaScript">][scr-page-link] | [<img src="./media/screenshots/4.png" alt="Bot configuration">][scr-page-link] |
|:---:|:---:|

[**❯ More screenshots**](./media/screenshots/README_en.md)

---

## ✨ Features

- ✅ **Auto reply** on all supported messaging apps
- ✏️ **Customize your auto reply** message
- 👥 **Works in group chats** too
- 🔒 **Full respect for your privacy**
  - No analytics or data tracking
- 🆓 **Free and open source**

## 🧩 BotJS Platform ✅ **IMPLEMENTED**

Downloadable JavaScript bot system that lets you fully customize auto-reply logic. **Completed November 2025**.

### Implemented features:

- ✅ **Secure download** of `bot.js` over HTTPS with optional SHA-256 validation
- ✅ **Rhino JavaScript engine** (ES5/partial ES6) with full Java↔JS interoperability and controlled APIs (`Android.log`, `Android.httpRequest`, storage)
- ✅ **Full GUI** — Material 3 screen to configure, test, and manage bots
- ✅ **Auto-updates** — WorkManager checks for new versions every 6 hours
- ✅ **Robust security** — 5s timeout, rate limiting (100 runs/min, 3 min between downloads), dangerous-pattern validation
- ✅ **Automatic fallback** — If the bot fails, uses static/OpenAI reply

### How to use:

1. Open the app and go to **Settings → Bot JavaScript**
2. Enable "Bot JS Enabled"
3. Enter the HTTPS URL of your bot.js (optional: SHA-256 hash)
4. Tap "Download Bot"
5. Test your bot with the "Test Bot" button
6. Enable auto-updates if you want automatic updates

### Full documentation:

- [User guide](./docs/BOT_USER_GUIDE.md) — How to use JavaScript bots
- [Development guide](./docs/BOT_DEVELOPMENT_GUIDE.md) — How to create your own bots
- [API Reference](./docs/BOT_API_REFERENCE.md) — APIs available for bots
- [Technical architecture](./docs/ARCHITECTURE.md) — System design

---

## 💡 What is it for?

Recent changes to WhatsApp's privacy policy triggered a massive migration to more privacy-friendly apps like Signal and others. But most of us find it hard to delete WhatsApp because everyone else uses it.

**Watomagic makes your migration easier** by letting your contacts know automatically that you moved to another app. Just set an auto-reply message like *"I no longer use WhatsApp. Please contact me on Signal…"* and let the app do the work for you.

> ⚠️ **Important:** This app is not associated with any company, including WhatsApp, Facebook, or Signal.

---

## 🔧 Troubleshooting

### Auto reply does not work even though Watomagic is enabled

Watomagic relies on notifications to work. Most users already have notifications enabled, so it should work out of the box. If it does not, make sure:

- ✅ Notifications are enabled
- ✅ App-specific biometric lock is disabled for Watomagic

---

## ❓ FAQ

### Why not use a WhatsApp Business account for auto replies?

You cannot use a business account without accepting the new privacy policy everyone is trying to avoid.

### Will it be available on iOS in the future?

This app depends on Android-specific quick-reply-from-notifications. That is probably not possible on iOS.

---

## 📚 Documentation and resources

### For users:
- [Screenshots](./media/screenshots/README_en.md) — App design
- [BotJS user guide](./docs/BOT_USER_GUIDE.md) — How to configure and use bots

### For developers:
- [CLAUDE.md](./CLAUDE.md) — Full project guide for Claude Code
- [Bot development guide](./docs/BOT_DEVELOPMENT_GUIDE.md) — Create your own JavaScript bots
- [API Reference](./docs/BOT_API_REFERENCE.md) — Available API documentation
- [System architecture](./docs/ARCHITECTURE.md) — Full technical design
- [CI/CD](./docs/GITHUB_ACTIONS_MIGRATION.md) — Signed builds with GitHub Actions

---

[scr-page-link]: ./media/screenshots/README_en.md
