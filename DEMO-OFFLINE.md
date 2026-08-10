# ArtVerse — Offline Demo Guide

Run the whole app (Android + admin panel) with **no internet**, using the Firebase
Local Emulator Suite. This is a demo-only mode; the default build still uses the
real cloud. Flip it off and you are back online — nothing here touches your cloud data.

---

## A. One-time setup (do this while you HAVE internet)

Already done once for this repo, but to reproduce on another machine:

```bash
npm install -g firebase-tools
firebase setup:emulators:firestore
firebase setup:emulators:storage
firebase setup:emulators:ui
```

This installs the CLI and downloads the emulator binaries into your Firebase cache
so they are available offline afterwards.

---

## B. Seed demo data (once, while online is fine but not required)

The emulator starts empty. Create the data your demo will show, then export it so
every demo starts with the same content.

1. Start the emulators:
   ```bash
   firebase emulators:start
   ```
2. Turn demo mode ON (see sections D/E) and run the app + admin panel against it.
3. Through the app/admin, create: a few artists (approve them), some artworks,
   a customer, and a sample order.
4. In a second terminal, export what you created:
   ```bash
   firebase emulators:export ./demo-seed
   ```

From now on, start the emulator with that seed loaded:

```bash
firebase emulators:start --import ./demo-seed --export-on-exit
```

`--export-on-exit` saves any changes you make during the demo back into `./demo-seed`.

---

## C. Demo day — start the local backend (offline)

```bash
firebase emulators:start --import ./demo-seed
```

Leave this running. The Emulator UI is at http://localhost:4000 if you want to
inspect data live. Ports: Auth 9099 · Firestore 8080 · Storage 9199 · Functions 5001.

---

## D. Android app — turn demo mode ON

Demo mode is controlled by the `demo` Gradle property (default OFF = cloud).

**From the command line** (uses the cached Gradle 8.6 distribution):

```bash
"C:\Users\sache\.gradle\wrapper\dists\gradle-8.6-bin\afr5mpiioh2wthjmwnkmdsd5w\gradle-8.6\bin\gradle.bat" assembleDebug -Pdemo=true --offline
```

**From Android Studio** (for the Run button): add this line to `gradle.properties`,
then Sync + Run. Remove it (or set `false`) to go back to the cloud.

```properties
demo=true
```

Notes:
- The app talks to the emulator via `10.0.2.2` — the Android Emulator's alias for
  your laptop's `localhost`. Use the **Android Emulator (AVD)**, not a browser.
- **Physical phone instead of AVD:** put phone and laptop on the same Wi-Fi/hotspot
  (no internet needed). In `FirebaseUtil.java`, change `EMULATOR_HOST` from
  `10.0.2.2` to the laptop's LAN IP (e.g. `192.168.x.x`), add that same IP to
  `res/xml/network_security_config.xml`, then rebuild.

---

## E. Admin panel — turn demo mode ON

The Angular panel runs in the browser on the same laptop, so it uses `localhost`
directly. No rebuild needed to toggle:

1. Open the panel in the browser.
2. Open DevTools console and run: `localStorage.demo = 'true'`
3. Reload. It now uses the emulator. To go back online: `localStorage.removeItem('demo')` and reload.

---

## F. Rebuild after code changes, offline

Cached dependencies make this work with no internet:

```bash
"C:\Users\sache\.gradle\wrapper\dists\gradle-8.6-bin\afr5mpiioh2wthjmwnkmdsd5w\gradle-8.6\bin\gradle.bat" assembleDebug -Pdemo=true --offline
```

(First build of a brand-new dependency needs internet once to cache it.)

---

## G. Going back online (production)

- **Android:** build without `-Pdemo=true` (or set `demo=false` in `gradle.properties`).
- **Admin:** `localStorage.removeItem('demo')` and reload.
- Stop the emulator (Ctrl+C). Your real cloud project is untouched throughout.

---

## Known limitation

**Push notifications (FCM) do not fire offline** — push is delivered by Google's
servers, which the emulator cannot replace. In-app notifications still work, because
they are read from the Firestore `notifications` collection. So the only thing the
offline demo loses is the phone buzzing while the app is closed.
