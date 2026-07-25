import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AuthService } from '../../core/auth.service';
import { describeWriteError } from '../../core/errors';
import { OrderDoc, OrderStatus } from '../../core/models';
import { OrdersService } from '../../core/orders.service';

type DeliveryTab = 'delivery' | 'progress' | 'completed' | 'rejected';

/** Tracking steps mirrored from the mobile OrderTrackerView. */
const STEP_LABELS = ['Placed', 'Confirmed', 'Out for delivery', 'Delivered'];

/**
 * Order delivery tracking (Admin FR-04).
 *
 * Orders the artist has handed to the delivery section arrive in the
 * "In delivery" tab. Marking one delivered settles the sale; rejecting it
 * returns the stock. Both push straight to the customer's and artist's
 * order screens, which listen to Firestore in real time.
 */
@Component({
  selector: 'app-deliveries',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './deliveries.component.html',
  styleUrl: './deliveries.component.css'
})
export class DeliveriesComponent {

  private readonly ordersService = inject(OrdersService);
  private readonly authService = inject(AuthService);

  readonly stepLabels = STEP_LABELS;

  readonly tabs: Array<{ key: DeliveryTab; label: string }> = [
    { key: 'delivery', label: 'In delivery' },
    { key: 'progress', label: 'With artist' },
    { key: 'completed', label: 'Completed' },
    { key: 'rejected', label: 'Rejected' }
  ];

  readonly activeTab = signal<DeliveryTab>('delivery');
  readonly loading = this.ordersService.loading;
  readonly busyId = signal<string | null>(null);
  readonly confirmingId = signal<string | null>(null);
  readonly toast = signal<{ text: string; error: boolean } | null>(null);

  readonly countsByTab = computed<Record<DeliveryTab, number>>(() => ({
    delivery: this.ordersService.outForDelivery().length,
    progress: this.ordersService.inProgress().length,
    completed: this.ordersService.completed().length,
    rejected: this.ordersService.rejected().length
  }));

  readonly visibleOrders = computed(() => {
    switch (this.activeTab()) {
      case 'delivery': return this.ordersService.outForDelivery();
      case 'progress': return this.ordersService.inProgress();
      case 'completed': return this.ordersService.completed();
      case 'rejected': return this.ordersService.rejected();
    }
  });

  shortId(order: OrderDoc): string {
    return this.ordersService.shortId(order.id);
  }

  statusLabel(status: OrderStatus | undefined): string {
    switch (status) {
      case 'confirmed': return 'Confirmed';
      case 'out_for_delivery': return 'Out for delivery';
      case 'completed': return 'Delivered';
      case 'rejected': return 'Rejected';
      default: return 'Pending';
    }
  }

  /** Index of the furthest reached tracking step; -1 for a rejected order. */
  trackerStep(status: OrderStatus | undefined): number {
    switch (status) {
      case 'confirmed': return 1;
      case 'out_for_delivery': return 2;
      case 'completed': return 3;
      case 'rejected': return -1;
      default: return 0;
    }
  }

  itemsSummary(order: OrderDoc): string {
    return (order.items ?? [])
      .map((item) => `${item.title ?? 'Untitled'} ×${item.quantity ?? 0}`)
      .join(', ');
  }

  async markDelivered(order: OrderDoc): Promise<void> {
    await this.act(order,
      () => this.ordersService.markDelivered(order, this.adminUid()),
      `Order #${this.shortId(order)} marked delivered - sale recorded`);
  }

  /** Rejection is destructive for the buyer, so it asks for a second tap. */
  async rejectOrder(order: OrderDoc): Promise<void> {
    if (this.confirmingId() !== order.id) {
      this.confirmingId.set(order.id);
      setTimeout(() => {
        if (this.confirmingId() === order.id) this.confirmingId.set(null);
      }, 4000);
      return;
    }
    this.confirmingId.set(null);
    await this.act(order,
      () => this.ordersService.rejectOrder(order, this.adminUid()),
      `Order #${this.shortId(order)} rejected - stock returned`);
  }

  private adminUid(): string {
    return this.authService.admin()?.uid ?? '';
  }

  private async act(order: OrderDoc, action: () => Promise<void>, successText: string): Promise<void> {
    this.busyId.set(order.id);
    try {
      await action();
      this.showToast(successText, false);
    } catch (e: unknown) {
      this.showToast(describeWriteError(e, 'order'), true);
    } finally {
      this.busyId.set(null);
    }
  }

  private showToast(text: string, error: boolean): void {
    this.toast.set({ text, error });
    setTimeout(() => this.toast.set(null), 3500);
  }
}
