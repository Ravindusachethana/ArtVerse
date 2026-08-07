/**
 * ArtVerse push delivery.
 *
 * The app already writes an entry to "notifications" in the same batch /
 * transaction as the order change that caused it (checkout notifies the
 * artist, OrderActions notifies the buyer, the admin panel notifies both).
 * That makes this collection the single hook for push: nothing in the app's
 * write paths had to change to gain background delivery.
 *
 * Why a server at all: a Firestore listener only runs while the app is alive,
 * so a closed app can never be told about a new order. Only FCM can wake the
 * handset, and FCM sends require trusted credentials - hence this function.
 *
 * Delivery bookkeeping: `delivered` is flipped to true only once FCM has
 * accepted the message for at least one device. If the recipient has no usable
 * token (never opened the app, reinstalled, signed out everywhere) the entry
 * stays undelivered, and the app's InAppNotifier shows it on next launch. So
 * an alert is never lost, and never shown twice.
 */

const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const { initializeApp } = require('firebase-admin/app');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');

initializeApp();
const db = getFirestore();

/** Token errors that mean "this device is gone" rather than a transient fault. */
const DEAD_TOKEN_CODES = new Set([
  'messaging/registration-token-not-registered',
  'messaging/invalid-registration-token',
  'messaging/invalid-argument'
]);

exports.sendOrderNotification = onDocumentCreated('notifications/{notificationId}', async (event) => {
  const snap = event.data;
  if (!snap) return;

  const notification = snap.data() || {};
  const userId = notification.userId;
  if (!userId) return;

  const userRef = db.doc(`users/${userId}`);
  const userSnap = await userRef.get();
  const tokens = (userSnap.exists && userSnap.get('fcmTokens')) || [];

  if (tokens.length === 0) {
    // No device to reach - leave it undelivered for the in-app fallback.
    return;
  }

  const response = await getMessaging().sendEachForMulticast({
    tokens,
    notification: {
      title: notification.title || 'ArtVerse',
      body: notification.message || ''
    },
    // Read by ArtVerseMessagingService / SplashActivity to deep link the tap.
    // FCM requires every data value to be a string.
    data: {
      route: 'orders',
      orderId: String(notification.orderId || ''),
      userId: String(userId),
      notificationId: String(event.params.notificationId)
    },
    android: {
      priority: 'high',
      notification: { channelId: 'artverse_orders' }
    }
  });

  // Prune tokens that will never work again, so the array cannot grow stale
  // across reinstalls (the app re-adds a fresh token on its next launch).
  const dead = [];
  response.responses.forEach((result, i) => {
    if (!result.success && DEAD_TOKEN_CODES.has(result.error && result.error.code)) {
      dead.push(tokens[i]);
    }
  });
  if (dead.length > 0) {
    await userRef.update({ fcmTokens: FieldValue.arrayRemove(...dead) });
  }

  if (response.successCount > 0) {
    await snap.ref.update({ delivered: true });
  }
});
