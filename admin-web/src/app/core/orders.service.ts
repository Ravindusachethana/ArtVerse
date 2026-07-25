import { Injectable, OnDestroy, computed, signal } from '@angular/core';
import {
  collection,
  doc,
  increment,
  onSnapshot,
  writeBatch
} from 'firebase/firestore';
import { db } from './firebase';
import { OrderDoc } from './models';

/**
 * Live view over the "orders" collection plus the admin's delivery actions.
 *
 * The admin owns the last leg of the lifecycle: once an artist hands an
 * order to the delivery section it becomes "out_for_delivery" and lands in
 * this queue. Marking it delivered is what actually settles the sale -
 * Transaction audit records are written and the artist's totalSales is
 * credited here, so the Sales Report only ever counts artwork that reached
 * the buyer. Rejecting instead releases the reserved stock.
 *
 * Both actions are one atomic batch, and both notify the customer and the
 * artist so their order screens update live.
 */
@Injectable({ providedIn: 'root' })
export class OrdersService implements OnDestroy {

  readonly orders = signal<OrderDoc[]>([]);
  readonly loading = signal(true);

  /** The admin's queue: in delivery, awaiting a completion decision. */
  readonly outForDelivery = computed(() =>
    this.orders().filter((o) => o.status === 'out_for_delivery'));

  /** Still with the artist - shown read-only so the admin can see what's coming. */
  readonly inProgress = computed(() =>
    this.orders().filter((o) =>
      o.status === 'processing' || o.status === 'pending' || o.status === 'confirmed'));

  readonly completed = computed(() => this.orders().filter((o) => o.status === 'completed'));
  readonly rejected = computed(() => this.orders().filter((o) => o.status === 'rejected'));

  /** Drives the sidebar badge. */
  readonly deliveryCount = computed(() => this.outForDelivery().length);

  private readonly unsubscribe: () => void;

  constructor() {
    this.unsubscribe = onSnapshot(collection(db, 'orders'), (snapshot) => {
      const list = snapshot.docs.map((d) => ({ ...(d.data() as OrderDoc), id: d.id }));
      list.sort((a, b) => (b.orderDate ?? 0) - (a.orderDate ?? 0));
      this.orders.set(list);
      this.loading.set(false);
    });
  }

  ngOnDestroy(): void {
    this.unsubscribe();
  }

  /**
   * Delivery confirmed. Settles the sale: writes one Transaction per line
   * item, credits the artist's totalSales, and notifies both parties.
   */
  async markDelivered(order: OrderDoc, adminUid: string): Promise<void> {
    const batch = writeBatch(db);
    const now = Date.now();

    batch.update(doc(db, 'orders', order.id), {
      status: 'completed',
      deliveredAt: now,
      settledBy: adminUid
    });

    // A one-of-a-kind piece (Painting, Drawing, Mixed Media) is the only copy
    // there is, so once its delivery lands it is retired from the shop: the
    // artwork document stays for the reports and the buyer's collection, but is
    // pulled from the browse gallery and the artist's My Art. Reproducible and
    // digital pieces stay listed.
    let retiredCount = 0;

    for (const item of order.items ?? []) {
      const txRef = doc(collection(db, 'transactions'));
      batch.set(txRef, {
        id: txRef.id,
        orderId: order.id,
        artworkId: item.artworkId ?? '',
        artistId: order.artistId ?? '',
        invoiceNumber: this.invoiceNumber(now, item.artworkId ?? ''),
        quantity: item.quantity ?? 0,
        unitPrice: item.unitPrice ?? 0,
        total: (item.unitPrice ?? 0) * (item.quantity ?? 0),
        date: now
      });

      if (item.artworkId && this.isOneOfAKind(item.categoryName)) {
        batch.update(doc(db, 'artworks', item.artworkId), { retired: true });
        retiredCount++;
      }
    }

    if (order.artistId) {
      // The order is scoped to one artist, so any retirements come off their
      // live listing count shown on the artist dashboard.
      batch.update(doc(db, 'artists', order.artistId), {
        totalSales: increment(order.totalAmount ?? 0),
        ...(retiredCount > 0 ? { totalArtworks: increment(-retiredCount) } : {})
      });
    }

    const shortId = this.shortId(order.id);
    this.queueNotification(batch, order.customerId, order.id, 'Order delivered',
      `Order #${shortId} has been delivered. Enjoy your artwork!`, now);
    this.queueNotification(batch, order.artistId, order.id, 'Delivery completed',
      `Order #${shortId} was delivered and the sale has been recorded.`, now);

    await batch.commit();
  }

  /**
   * Delivery failed / order cancelled by the admin. Releases the reserved
   * stock back to each artwork and notifies both parties. No Transaction is
   * written, so a rejected order never counts as revenue.
   */
  async rejectOrder(order: OrderDoc, adminUid: string): Promise<void> {
    const batch = writeBatch(db);
    const now = Date.now();

    batch.update(doc(db, 'orders', order.id), {
      status: 'rejected',
      settledBy: adminUid
    });

    for (const item of order.items ?? []) {
      if (!item.artworkId) continue;
      batch.update(doc(db, 'artworks', item.artworkId), {
        quantity: increment(item.quantity ?? 0),
        available: true
      });
    }

    const shortId = this.shortId(order.id);
    this.queueNotification(batch, order.customerId, order.id, 'Order rejected',
      `Order #${shortId} could not be completed and has been cancelled. You have not been charged.`, now);
    this.queueNotification(batch, order.artistId, order.id, 'Order rejected',
      `Order #${shortId} was cancelled during delivery and the stock has been returned to your listing.`, now);

    await batch.commit();
  }

  /** Same notification shape the mobile InAppNotifier reads. */
  private queueNotification(batch: ReturnType<typeof writeBatch>, userId: string | undefined,
                            orderId: string, title: string, message: string, when: number): void {
    if (!userId) return;
    const ref = doc(collection(db, 'notifications'));
    batch.set(ref, {
      id: ref.id,
      userId,
      orderId,
      title,
      message,
      delivered: false,
      createdAt: when
    });
  }

  /**
   * One-of-a-kind categories - a single physical piece that cannot be remade,
   * so it retires once sold. Mirrors ArtCategories.ONE_OF_A_KIND on mobile;
   * keep the two lists in step.
   */
  private static readonly ONE_OF_A_KIND = ['painting', 'drawing', 'mixed media'];

  private isOneOfAKind(categoryName?: string): boolean {
    return !!categoryName
      && OrdersService.ONE_OF_A_KIND.includes(categoryName.trim().toLowerCase());
  }

  /** Mirrors OrderActions.invoiceNumber on Android so receipts stay consistent. */
  private invoiceNumber(when: number, artworkId: string): string {
    return `INV-${when}-${artworkId.substring(0, Math.min(5, artworkId.length))}`;
  }

  shortId(orderId: string): string {
    return orderId && orderId.length >= 6 ? orderId.substring(0, 6).toUpperCase() : orderId;
  }
}
