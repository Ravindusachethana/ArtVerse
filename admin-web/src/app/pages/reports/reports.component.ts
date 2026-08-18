import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ChartConfiguration } from 'chart.js';
import { OrderDoc, OrderStatus } from '../../core/models';
import { RankedItem, ReportsService } from '../../core/reports.service';
import { ChartComponent } from '../../shared/chart.component';

type OrderTab = 'completed' | 'artistCancelled' | 'adminCancelled';

/** ArtVerse palette (mirrors styles.css / mobile colors.xml). */
const COLORS = {
  primary: '#1B4F91',
  primaryLight: '#3E74B8',
  accent: '#C98A2C',
  success: '#2E7D52',
  rejected: '#C1442D',
  pending: '#D9A400',
  processing: '#6C63FF',
  axis: '#6B6255',
  grid: 'rgba(30, 27, 22, 0.08)'
};


@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [DatePipe, DecimalPipe, ChartComponent],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.css'
})
export class ReportsComponent {

  private readonly reports = inject(ReportsService);

  readonly loading = this.reports.loading;
  readonly completed = this.reports.completed;
  readonly artistCancelled = this.reports.artistCancelled;
  readonly adminCancelled = this.reports.adminCancelled;
  readonly revenue = this.reports.revenue;
  readonly avgDeliveryDays = this.reports.avgDeliveryDays;
  readonly bestSellers = this.reports.bestSellers;
  readonly continuousBuyers = this.reports.continuousBuyers;
  readonly deliveryBreakdown = this.reports.deliveryBreakdown;

  readonly activeTab = signal<OrderTab>('completed');

  readonly tabs: Array<{ key: OrderTab; label: string }> = [
    { key: 'completed', label: 'Completed' },
    { key: 'artistCancelled', label: 'Artist rejected' },
    { key: 'adminCancelled', label: 'Admin rejected' }
  ];

  readonly countsByTab = computed<Record<OrderTab, number>>(() => ({
    completed: this.completed().length,
    artistCancelled: this.artistCancelled().length,
    adminCancelled: this.adminCancelled().length
  }));

  readonly visibleOrders = computed<OrderDoc[]>(() => {
    switch (this.activeTab()) {
      case 'completed': return this.completed();
      case 'artistCancelled': return this.artistCancelled();
      case 'adminCancelled': return this.adminCancelled();
    }
  });

  // ----- chart configs -----

  readonly bestSellerConfig = computed<ChartConfiguration>(() =>
    this.barConfig(this.bestSellers(), COLORS.accent, 'y', 'Units sold'));

  readonly buyerConfig = computed<ChartConfiguration>(() =>
    this.barConfig(this.continuousBuyers(), COLORS.primary, 'x', 'Completed orders'));

  readonly deliveryConfig = computed<ChartConfiguration>(() => {
    const b = this.deliveryBreakdown();
    return {
      type: 'doughnut',
      data: {
        labels: ['With artist', 'Out for delivery', 'Completed', 'Cancelled'],
        datasets: [{
          data: [b.withArtist, b.outForDelivery, b.completed, b.cancelled],
          backgroundColor: [COLORS.processing, COLORS.primary, COLORS.success, COLORS.rejected],
          borderColor: '#fff',
          borderWidth: 2
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '62%',
        plugins: {
          legend: {
            position: 'bottom',
            labels: { color: COLORS.axis, font: { size: 12 }, padding: 14, boxWidth: 12 }
          }
        }
      }
    };
  });

  // ----- order-card helpers -----

  shortId(order: OrderDoc): string {
    return this.reports.shortId(order);
  }

  itemsSummary(order: OrderDoc): string {
    return this.reports.itemsSummary(order);
  }

  statusLabel(status: OrderStatus | undefined): string {
    switch (status) {
      case 'completed': return 'Delivered';
      case 'rejected': return 'Rejected';
      default: return 'Pending';
    }
  }

  hasData(items: RankedItem[]): boolean {
    return items.length > 0;
  }

  private barConfig(items: RankedItem[], color: string,
                    indexAxis: 'x' | 'y', axisTitle: string): ChartConfiguration {
    const horizontal = indexAxis === 'y';
    const valueScale = {
      beginAtZero: true,
      ticks: { color: COLORS.axis, precision: 0 as const },
      grid: { color: COLORS.grid },
      title: { display: true, text: axisTitle, color: COLORS.axis }
    };
    const labelScale = {
      ticks: { color: COLORS.axis },
      grid: { display: false }
    };
    return {
      type: 'bar',
      data: {
        labels: items.map((i) => i.label),
        datasets: [{
          data: items.map((i) => i.value),
          backgroundColor: color,
          borderRadius: 6,
          maxBarThickness: 34
        }]
      },
      options: {
        indexAxis,
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: horizontal ? valueScale : labelScale,
          y: horizontal ? labelScale : valueScale
        }
      }
    };
  }
}
