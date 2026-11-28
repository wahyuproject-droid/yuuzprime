# YuuZPrime
Android adaptation and branding by YuuZGilakPrompt. Original source: TMDB-To-VOD-Playlist by gogetta69, CC BY-NC 4.0 (attribution retained).

## Structure
- `backend/`: original PHP/Node.js source after removing the dedicated adult-content module, matching the supplied cleaned archive (114 verified files). The one-time import runs in GitHub Actions. It does not execute upstream binaries or start a streaming server.
- `app/`: standalone Android catalog and local-video client. Requires a TMDB v3 API key. Does not bundle or run the PHP/Node backend, and does not provide remote movie playback.
- `download/`: static download website, linking to the latest verified GitHub Release.
- `.github/workflows/build-apk.yml`: build, verify, and publish the signed testing APK.

The backend requires its own server and setup. Use only authorized content. No external provider playback or device test has been performed. Public APK is for testing; use a secure private signing key for production. This product uses the TMDB API but is not endorsed or certified by TMDB.
