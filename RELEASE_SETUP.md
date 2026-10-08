# Release APK setup

Repository: abiediendomba64-tech/android

## What you need once

Create one Android release/upload keystore and keep the keystore file safe outside the repository.

Recommended values:
- Alias: upload
- Keystore format: JKS
- Do not commit the .jks file to GitHub.
- Keep a private backup of the keystore and both passwords.

Generate a new keystore locally with Android Studio's signing tools or:

~~~bash
keytool -genkeypair -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
~~~

Convert the keystore to base64 for GitHub Actions.

PowerShell:

~~~powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("my-upload-key.jks")) | Set-Content keystore.base64
~~~

## GitHub secrets required

Repository Settings -> Secrets and variables -> Actions -> New repository secret:

1. `ANDROID_KEYSTORE_BASE64` — entire content of `keystore.base64`
2. `ANDROID_KEYSTORE_PASSWORD` — keystore password
3. `ANDROID_KEY_ALIAS` — usually `upload`
4. `ANDROID_KEY_PASSWORD` — key password

Never put these values in source code, `.env`, committed Gradle properties, or chat.

## Build

Debug APK is built automatically on pushes/PRs and uploaded as Actions artifact: `sistem-kas-debug-apk`.

Production release APK is built from GitHub Actions -> Android Release -> Run workflow, or by pushing a tag such as `v1.0.0`.

The release workflow builds, tests, signs, checks SHA-256, and uploads the APK.

## Current signing contract

The app release signing configuration uses:
- `KEYSTORE_PATH` (defaults to `my-upload-key.jks`)
- `KEY_ALIAS` (defaults to `upload`)
- `STORE_PASSWORD`
- `KEY_PASSWORD`

The release workflow supplies these from GitHub Actions secrets.
