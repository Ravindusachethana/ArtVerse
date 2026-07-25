import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { ArtistsService } from '../../core/artists.service';
import { ArtworksService } from '../../core/artworks.service';
import { OrdersService } from '../../core/orders.service';
import { ReportsService } from '../../core/reports.service';

/** Overview page: headline platform numbers + shortcut to pending reviews. */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, DecimalPipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {

  private readonly artistsService = inject(ArtistsService);
  private readonly artworksService = inject(ArtworksService);
  private readonly ordersService = inject(OrdersService);
  private readonly reportsService = inject(ReportsService);

  /** Live report headlines, shared with the full Reports page. */
  readonly reportCompleted = computed(() => this.reportsService.completed().length);
  readonly reportRevenue = this.reportsService.revenue;
  readonly reportCancelled = computed(() =>
    this.reportsService.artistCancelled().length + this.reportsService.adminCancelled().length);

  readonly counts = signal<{ pendingArtists: number; artists: number; customers: number; artworks: number } | null>(null);
  readonly countsFailed = signal(false);

  /** Live pending count from the snapshot listener (fresher than counts()). */
  readonly livePending = computed(() =>
    this.artistsService.rows().filter((row) => row.effectiveStatus === 'pending').length);

  readonly liveArtworkReviews = this.artworksService.reviewCount;

  readonly liveProfileChanges = computed(() => this.artistsService.profileEdits().length);

  readonly liveDeliveries = this.ordersService.deliveryCount;

  async ngOnInit(): Promise<void> {
    try {
      this.counts.set(await this.artistsService.counts());
    } catch {
      this.countsFailed.set(true);
    }
  }
}
