# DreamLinks for Android

The Android app for [mydreamlinks.com](https://mydreamlinks.com). It's written in Kotlin with Jetpack Compose, and the website is its backend: everything goes through the site's `api/v1` endpoints.

## Build and run

Open this folder in Android Studio, then press Run.

From a terminal:

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # onto a running emulator or plugged-in phone
```

- **Debug builds** talk to the dev server on this computer at `http://10.0.2.2:8081/`. That address is how the emulator reaches the computer it runs on. Start the site with `php spark serve --host localhost --port 8081`.
- **Release builds** talk to `https://mydreamlinks.com/`.

Both addresses live in `gradle.properties`. A debug build installs next to a release build as "com.mydreamlinks.app.debug".

To test on a real phone against the dev server, the computer's LAN address has to go in `dreamlinks.debugUrl` and in `app/src/debug/res/xml/network_security_config.xml`.

## What's in it

| Screen | What it does |
| --- | --- |
| Log in | Email and password. It opens the website to join or reset a password. |
| Feed | Dreams you can see from others, the 30-day symbol pulse, and "I've dreamt this". |
| Journal | All your own dreams, private ones included. |
| Log / Edit | Writes a dream. It spots symbols as you type, using the site's word list. |
| Dream | One dream, with Edit and Delete if it's yours. |
| Map | Your Dreamscape: symbol map, top symbols, tappable moon phases, dreamers like you, and compare. |
| Readings | AI readings, next-reading status, tonight's moon, and recurring symbols. |
| Profile, Search, Links | Find dreamers, ask to link, accept or decline requests, and unlink. |
| Account | Bio, AI readings, default visibility, morning reminder, and log out. |

## How sign-in works

`POST api/v1/auth/login` returns a token. The app stores it in private DataStore, with backups turned off, and sends it as `Authorization: Bearer …`.

- The server only stores a SHA-256 hash of the token.
- Logging out deletes the token on the server.
- Any 401 response sends the app back to the login screen.

## API

All endpoints are under `/api/v1/` and return JSON.

| Method | Path | Notes |
| --- | --- | --- |
| POST | `auth/login` | `{email, password, device}` → `{token, user}`. Limited to 10 tries per minute per IP. |
| POST | `auth/logout` | |
| GET / POST | `me` | POST accepts any of `bio`, `ai_analysis_enabled`, `default_dream_visibility`, `reminder_enabled`, `reminder_hour`, `timezone`. |
| GET | `feed?page=` | Page 1 also includes `pulse`. |
| GET / POST | `dreams` | POST takes `{title, text, date, visibility, tags[]}`. |
| GET / PUT / DELETE | `dreams/{id}` | |
| POST | `dreams/{id}/resonate` | Toggles "I've dreamt this". |
| GET | `users/{id}`, `search?q=`, `links` | |
| POST | `users/{id}/link`, `links/{id}/accept`, `links/{id}/decline` | |
| DELETE | `links/{id}` | Unlinks. |
| GET | `dreamscape?with=`, `readings`, `symbols` | |

Errors come back as `{error, fields?}`, with status 401, 404, 422 or 429.

## Not built yet

- Push notifications. The morning reminder is still email only.
- Changing the profile picture, username, email or password in the app. Those are done on the website for now.
- Offline drafts.
