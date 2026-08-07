# ArtVerse Cloud Functions — push notifications

Delivers order alerts to a phone **even when the app is closed**.

## Why a server component is required

A Firestore snapshot listener (`InAppNotifier`) only runs while the app is
alive, so a closed app can never learn about a new order. Only **Firebase Cloud
Messaging (FCM)** can wake the handset, and FCM sends require trusted
credentials — which must never be shipped inside the APK. Hence this function.

## How it works

```
ArtLover places an order
  └─ CheckoutActivity writes orders/{id}  +  notifications/{id}   (one transaction)
       └─ [this function] onCreate(notifications/{id})
            ├─ reads users/{recipient}.fcmTokens
            ├─ sends FCM (notification + data payload)
            ├─ prunes dead tokens
            └─ marks notifications/{id}.delivered = true  (only if a send succeeded)
                 └─ Artist's phone shows "New order received"
                      └─ tap → SplashActivity → session/approval check → Orders tab
```

Nothing in the app's order-writing code had to change: every order event
already writes a `notifications` entry in the same batch/transaction, so this
one trigger covers new orders, accept/reject, and the admin's delivery updates.

`delivered` is only set to `true` once FCM accepts the message for at least one
device. If the recipient has no usable token, the entry stays undelivered and
the in-app `InAppNotifier` shows it on next launch — so an alert is never lost,
and (because both paths post under the notification's document id) never shown
twice.

## Deploying

> **Requires the Firebase Blaze (pay-as-you-go) plan.** Cloud Functions are not
> available on the free Spark plan. Blaze needs a card on file, but its free
> tier (2M invocations/month) covers this project many times over — realistic
> usage here costs nothing. Upgrade at
> *Firebase console → ⚙ → Usage and billing → Modify plan*.

```bash
npm install --prefix functions
```

```bash
firebase deploy --only functions
```

Verify it registered:

```bash
firebase functions:log --only sendOrderNotification
```

## Testing end to end

1. Sign in as the artist on a device and **open the app once** (this registers
   the device's FCM token on `users/{uid}.fcmTokens`) — grant the notification
   permission when prompted.
2. **Fully close the app** (swipe it out of recents).
3. From another device/emulator, sign in as an ArtLover and place an order for
   that artist's artwork.
4. The artist's phone shows "New order received". Tapping it opens the app on
   the artist's **Incoming Orders** tab.

If nothing arrives, check in order: the artist's `users/{uid}` document has a
non-empty `fcmTokens` array; `firebase functions:log` shows an invocation; the
device granted `POST_NOTIFICATIONS` (Android 13+).

## Notes

- Tokens are stored as an **array**, so one account signed in on several
  devices gets the push on all of them.
- Signing out detaches that device's token (`AuthActions.signOut`), so a shared
  handset stops receiving the previous account's alerts.
- Tokens that FCM reports as permanently invalid are removed automatically.
