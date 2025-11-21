# GitHub Actions Migration Guide

This guide documents the migration from **Codemagic** to **GitHub Actions** for Watomagic CI/CD.

## Overview

The GitHub Actions workflow (`/.github/workflows/android-release.yml`) provides feature parity with the Codemagic setup:

- ✅ Android APK builds with signing
- ✅ Unit tests and lint checks (non-blocking)
- ✅ Artifact storage and downloads
- ✅ Version code management
- ✅ macOS runners (included in GitHub Free tier)

## Setup Instructions

### 1. Configure GitHub Secrets

Go to your repository: **Settings → Secrets and variables → Actions → New repository secret**

Add these secrets (same values as Codemagic):

| Secret Name | Value | Notes |
|-------------|-------|-------|
| `KEYSTORE_BASE64` | Base64-encoded keystore file | See encoding steps below |
| `KEYSTORE_PASSWORD` | Your keystore password | Same as Codemagic `KEYSTORE_PASSWORD` |
| `KEY_ALIAS` | Your key alias | Same as Codemagic `KEY_ALIAS` |
| `KEY_PASSWORD` | Your key password | Same as Codemagic `KEY_PASSWORD` |

### 2. Encode Your Keystore for GitHub Secrets

If you have the keystore file locally:

```bash
# On macOS/Linux
base64 -i watomagic-release.keystore | tr -d '\n' > keystore.b64

# Copy the contents to clipboard
cat keystore.b64 | pbcopy  # macOS
cat keystore.b64 | xclip -selection clipboard  # Linux

# Then paste into GitHub secret KEYSTORE_BASE64
```

If you need to extract the keystore from Codemagic:

1. Download the keystore from Codemagic (if you still have access)
2. Encode it using the command above
3. Add to GitHub secrets

### 3. Test the Workflow

1. Go to **Actions** tab in your GitHub repository
2. Select **"Watomagic Android Release Build"** workflow
3. Click **"Run workflow"** → **"Run workflow"** (manual trigger)
4. Monitor the build progress
5. Download the APK from **Artifacts** section after completion

### 4. Verify Build Output

After the first successful build:

- ✅ APK should be available in **Artifacts** section
- ✅ APK should be signed (verify with `apksigner verify`)
- ✅ Version code should increment automatically
- ✅ Test reports should be available (if tests ran)

## Workflow Triggers

The workflow runs automatically on:

- **Manual trigger**: Actions tab → Run workflow
- **Push to main branch**: Any commit to `main`
- **Version tags**: Tags matching `v*` pattern (e.g., `v1.0.0`)

## Key Differences from Codemagic

| Feature | Codemagic | GitHub Actions |
|---------|-----------|----------------|
| **Build minutes** | Limited free tier | 2,000/month (private repos) |
| **macOS runners** | ✅ Yes | ✅ Yes (included) |
| **Keystore storage** | File upload in UI | Base64 in secrets |
| **Artifact retention** | 30 days default | 30 days (configurable) |
| **Notifications** | Email built-in | GitHub notifications + optional email steps |
| **Integration** | Separate UI | ✅ Native GitHub integration |

## Version Code Management

The workflow uses the same versioning strategy as Codemagic:

- **Base Version Code**: `10000` (defined in workflow `env`)
- **Dynamic Version Code**: `BASE_VERSION_CODE + GITHUB_RUN_NUMBER`
- **Version Name**: `1.{GITHUB_RUN_NUMBER}`

Example:
- Run #1 → Version Code: `10001`, Version Name: `1.1`
- Run #2 → Version Code: `10002`, Version Name: `1.2`

## Troubleshooting

### Build Fails: "KEYSTORE_BASE64 secret not set"

**Solution**: Add the `KEYSTORE_BASE64` secret in GitHub repository settings.

### Build Fails: "Keystore file not found"

**Solution**: Verify the base64 encoding is correct. The keystore should decode to a valid `.keystore` file.

### APK Not Signed

**Solution**: Check that all signing secrets are set:
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `KEYSTORE_BASE64`

### Tests Fail but Build Continues

This is expected behavior. Tests are non-blocking (`continue-on-error: true`). Check the test reports in artifacts for details.

## Migration Checklist

- [x] GitHub Actions workflow file created (`.github/workflows/android-release.yml`)
- [ ] GitHub secrets configured (KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD)
- [ ] First workflow run successful
- [ ] APK downloaded and verified (signed, installable)
- [ ] (Optional) Update README to reference GitHub Actions
- [ ] (Optional) Archive/delete `codemagic.yaml` after confirming GitHub Actions works

## Next Steps After Migration

1. **Test thoroughly**: Run multiple builds to ensure consistency
2. **Update documentation**: Update any references to Codemagic in README/docs
3. **Team notification**: Inform team members about the new CI/CD location
4. **Monitor builds**: Watch for any issues in the first few builds

## Support

If you encounter issues:

1. Check workflow logs in GitHub Actions
2. Verify all secrets are set correctly
3. Compare with Codemagic build logs (if available)
4. Review the workflow file for any environment-specific issues

## Keystore Security Reminder

⚠️ **IMPORTANT**: The keystore is critical for app updates. Ensure:

- Keystore is backed up securely (password manager, secure vault)
- GitHub secrets are properly secured
- Only trusted team members have access to secrets
- If keystore is lost, you cannot update the app on Google Play

