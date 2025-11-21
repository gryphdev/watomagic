# GitHub Actions Migration Guide

This guide documents the migration from **Codemagic** to **GitHub Actions** for Watomagic CI/CD.

## Overview

The GitHub Actions workflow (`.github/workflows/android-release.yml`) provides feature parity with Codemagic plus optimizations:

- ✅ Android APK builds with signing
- ✅ Unit tests and lint checks (non-blocking)
- ✅ Artifact storage (30 days retention)
- ✅ Automatic version code management
- ✅ **Linux runners** (faster, free for public repos)
- ✅ **Early secret validation** (fails fast if secrets missing)
- ✅ **Optimized caching** (Gradle + Android SDK)

## Quick Setup

### 1. Configure GitHub Environment Secrets

**Settings → Environments → watomagic → Add secret**

The workflow uses the **`watomagic` environment** with 4 secrets:

| Secret Name | Description |
|-------------|-------------|
| `KEYSTORE_BASE64` | Base64-encoded keystore file |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias (e.g., `watomagic`) |
| `KEY_PASSWORD` | Key password |

**Note**: Environment secrets are used instead of repository secrets for better security isolation.

### 2. Encode Keystore

```bash
# Encode keystore to base64
base64 -i watomagic-release.keystore | tr -d '\n' > keystore.b64

# Copy to clipboard
cat keystore.b64 | pbcopy  # macOS
cat keystore.b64 | xclip -selection clipboard  # Linux

# Paste into GitHub secret KEYSTORE_BASE64
```

### 3. Run Workflow

1. Go to **Actions** tab → **Watomagic Android Release Build**
2. Click **Run workflow** → **Run workflow**
3. Download APK from **Artifacts** after completion

## Workflow Features

### Triggers
- **Manual**: Actions tab → Run workflow
- **Automatic**: Push to `main` branch
- **Tags**: Push tags matching `v*` pattern

### Version Management
- **Base Version Code**: `10000`
- **Dynamic**: `BASE_VERSION_CODE + GITHUB_RUN_NUMBER`
- **Version Name**: `1.{GITHUB_RUN_NUMBER}`

**Example:**
- Run #1 → Version Code: `10001`, Name: `1.1`
- Run #2 → Version Code: `10002`, Name: `1.2`

### Optimizations
- **Early secret validation**: Fails in ~10 seconds if secrets missing (vs ~5 minutes)
- **Gradle cache**: Speeds up dependency downloads
- **Android SDK cache**: Reduces setup time
- **Linux runners**: 20-40% faster than macOS, free for public repos

## Differences from Codemagic

| Feature | Codemagic | GitHub Actions |
|---------|-----------|----------------|
| **Runners** | macOS | **Linux (ubuntu-latest)** |
| **Build speed** | Standard | **Faster (optimized cache)** |
| **Free tier** | Limited | 2,000 min/month (private) |
| **Keystore** | File upload | Base64 in environment secrets |
| **Integration** | Separate UI | ✅ Native GitHub |
| **Secret validation** | Late failure | **Early validation** |

## Troubleshooting

### "KEYSTORE_BASE64 secret not set"

**Solution**: The workflow uses the **`watomagic` environment** and validates secrets early:
1. Check workflow logs for which secrets are missing
2. Go to **Settings → Environments → watomagic**
3. Verify all 4 secrets are configured:
   - `KEYSTORE_BASE64`
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`

**Note**: The workflow is configured to use `environment: watomagic` (line 29). If you need to use a different environment, update the workflow file.

### "Keystore file too small"

**Solution**: Base64 encoding failed. Re-encode:
```bash
base64 -i watomagic-release.keystore | tr -d '\n' | wc -c
# Should be > 1000 characters
```

### APK Not Signed

**Solution**: Verify all secrets are set:
- `KEYSTORE_BASE64` (base64-encoded file)
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

### Tests/Lint Fail but Build Continues

**Expected behavior**: Tests and lint are non-blocking (`continue-on-error: true`). Check artifacts for reports.

## Migration Checklist

- [x] GitHub Actions workflow created
- [x] GitHub environment `watomagic` configured with 4 secrets
- [ ] First workflow run successful
- [ ] APK downloaded and verified (signed, installable)
- [ ] (Optional) Archive `codemagic.yaml`

## Security Reminder

⚠️ **IMPORTANT**: The keystore is critical for app updates:
- Back up keystore securely (password manager, secure vault)
- Secure GitHub secrets access
- If keystore is lost, you cannot update the app on Google Play

## Support

If you encounter issues:
1. Check workflow logs in GitHub Actions
2. Review early secret validation output
3. Verify all secrets are correctly set
4. Compare with previous Codemagic builds (if available)
