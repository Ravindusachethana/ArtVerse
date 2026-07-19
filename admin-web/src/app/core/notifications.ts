import { addDoc, collection } from 'firebase/firestore';
import { db } from './firebase';

/**
 * Drops an in-app notification in the exact shape the mobile InAppNotifier
 * reads (models/AppNotification.java). Best-effort: review decisions must
 * not fail just because the notification write did.
 */
export async function notifyUser(userId: string, title: string, message: string): Promise<void> {
  await addDoc(collection(db, 'notifications'), {
    userId,
    orderId: null,
    title,
    message,
    delivered: false,
    createdAt: Date.now()
  }).catch(() => { /* best-effort */ });
}
