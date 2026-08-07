import { Injectable, computed, inject } from '@angular/core';
import { OrderDoc } from './models';
import { OrdersService } from './orders.service';

/** One ranked bar (best-seller artwork or repeat buyer). */
export interface RankedItem {
  label: string;
  value: number;
}

const DAY_MS = 86_400_000;

/**
 * Read-only analytics for the admin Reports page, derived entirely from the
 * live "orders" snapshot already maintained by OrdersService. Keeping it as
 * computed signals means the dashboard summary and the full report screen
 * share one source of truth and refresh together as orders settle.
 *
 * Cancellation split: an artist can only reject before the order is handed to
 * delivery (dispatchedAt is never set), whereas the admin only rejects orders
 * that are already out for delivery. So dispatchedAt cleanly separates the two
 * cancelled-order reports the spec asks for.
 */
@Injectable({ providedIn: 'root' })
export class ReportsService {

  private readonly ordersService = inject(OrdersService);

  readonly loading = this.ordersService.loading;

  readonly completed = this.ordersService.completed;
  readonly inDelivery = this.ordersService.outForDelivery;

  /** Rejected before dispatch → the artist declined it. */
  readonly artistCancelled = computed(() =>
    this.ordersService.rejected().filter((o) => !o.dispatchedAt));

  /** Rejected while out for delivery → the admin cancelled it. */
  readonly adminCancelled = computed(() =>
    this.ordersService.rejected().filter((o) => !!o.dispatchedAt));

  readonly revenue = computed(() =>
    this.completed().reduce((sum, o) => sum + (o.totalAmount ?? 0), 0));

  /** Mean days from placement to delivery across completed orders. */
  readonly avgDeliveryDays = computed(() => {
    const timed = this.completed().filter(
      (o) => o.deliveredAt && o.orderDate && o.deliveredAt >= o.orderDate);
    if (timed.length === 0) return 0;
    const total = timed.reduce((sum, o) => sum + (o.deliveredAt! - o.orderDate!), 0);
    return total / timed.length / DAY_MS;
  });

  /** Units sold per artwork across completed orders, highest first. */
  readonly bestSellers = computed<RankedItem[]>(() => {
    const byTitle = new Map<string, number>();
    for (const order of this.completed()) {
      for (const item of order.items ?? []) {
        const title = item.title || 'Untitled';
        byTitle.set(title, (byTitle.get(title) ?? 0) + (item.quantity ?? 0));
      }
    }
    return this.rank(byTitle, 8);
  });

  /**
   * Repeat ("continuous") buyers: customers with more than one completed
   * order, ranked by how many they have placed.
   */
  readonly continuousBuyers = computed<RankedItem[]>(() => {
    const byCustomer = new Map<string, number>();
    const nameOf = new Map<string, string>();
    for (const order of this.completed()) {
      const id = order.customerId || order.customerName || 'Unknown';
      byCustomer.set(id, (byCustomer.get(id) ?? 0) + 1);
      nameOf.set(id, order.customerName || 'Customer');
    }
    const repeat = [...byCustomer.entries()]
      .filter(([, count]) => count > 1)
      .map(([id, count]) => ({ label: nameOf.get(id) ?? 'Customer', value: count }));
    repeat.sort((a, b) => b.value - a.value);
    return repeat.slice(0, 8);
  });

  /** Counts for the delivery-status doughnut. */
  readonly deliveryBreakdown = computed(() => ({
    withArtist: this.ordersService.inProgress().length,
    outForDelivery: this.ordersService.outForDelivery().length,
    completed: this.completed().length,
    cancelled: this.ordersService.rejected().length
  }));

  shortId(order: OrderDoc): string {
    return this.ordersService.shortId(order.id);
  }

  itemsSummary(order: OrderDoc): string {
    return (order.items ?? [])
      .map((item) => `${item.title ?? 'Untitled'} ×${item.quantity ?? 0}`)
      .join(', ');
  }

  private rank(map: Map<string, number>, limit: number): RankedItem[] {
    return [...map.entries()]
      .map(([label, value]) => ({ label, value }))
      .sort((a, b) => b.value - a.value)
      .slice(0, limit);
  }
}
