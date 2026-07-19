import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ArtistsService } from '../../core/artists.service';
import { ArtworksService } from '../../core/artworks.service';

/** Overview page: headline platform numbers + shortcut to pending reviews. */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {

  private readonly artistsService = inject(ArtistsService);
  private readonly artworksService = inject(ArtworksService);

  readonly counts = signal<{ pendingArtists: number; artists: number; customers: number; artworks: number } | null>(null);
  readonly countsFailed = signal(false);

  /** Live pending count from the snapshot listener (fresher than counts()). */
  readonly livePending = computed(() =>
    this.artistsService.rows().filter((row) => row.effectiveStatus === 'pending').length);

  readonly liveArtworkReviews = this.artworksService.reviewCount;

  readonly liveProfileChanges = computed(() => this.artistsService.profileEdits().length);

  async ngOnInit(): Promise<void> {
    try {
      this.counts.set(await this.artistsService.counts());
    } catch {
      this.countsFailed.set(true);
    }
  }
}
