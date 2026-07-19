import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ArtistsService } from '../../core/artists.service';
import { AuthService } from '../../core/auth.service';
import { describeWriteError } from '../../core/errors';
import { ArtistRow, ArtistStatus } from '../../core/models';

/**
 * Artist Registration Approvals (Admin FR-01).
 *
 * Lists artist registrations by status. For pending artists the admin
 * reviews the details submitted at registration (business name, email,
 * location, bio, categories) and approves or rejects. The decision is
 * written to artists/{uid}.status, which the mobile app observes in real
 * time - an approved artist's device unlocks the artist dashboard
 * automatically.
 */
@Component({
  selector: 'app-artist-approvals',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './artist-approvals.component.html',
  styleUrl: './artist-approvals.component.css'
})
export class ArtistApprovalsComponent {

  private readonly artistsService = inject(ArtistsService);
  private readonly authService = inject(AuthService);

  readonly tabs: Array<{ key: ArtistStatus; label: string }> = [
    { key: 'pending', label: 'Pending' },
    { key: 'approved', label: 'Approved' },
    { key: 'rejected', label: 'Rejected' }
  ];

  readonly activeTab = signal<ArtistStatus>('pending');
  readonly loading = this.artistsService.loading;
  readonly busyUid = signal<string | null>(null);
  readonly toast = signal<{ text: string; error: boolean } | null>(null);

  readonly countsByStatus = computed(() => {
    const counts: Record<ArtistStatus, number> = { pending: 0, approved: 0, rejected: 0 };
    for (const row of this.artistsService.rows()) counts[row.effectiveStatus]++;
    return counts;
  });

  readonly visibleRows = computed(() =>
    this.artistsService.rows().filter((row) => row.effectiveStatus === this.activeTab()));

  async approve(row: ArtistRow): Promise<void> {
    await this.act(row, () => this.artistsService.approve(row, this.adminUid()),
      `${row.artist.businessName} approved - access granted`);
  }

  async reject(row: ArtistRow): Promise<void> {
    await this.act(row, () => this.artistsService.reject(row, this.adminUid()),
      `${row.artist.businessName} rejected`);
  }

  private adminUid(): string {
    return this.authService.admin()?.uid ?? '';
  }

  private async act(row: ArtistRow, action: () => Promise<void>, successText: string): Promise<void> {
    this.busyUid.set(row.artist.uid);
    try {
      await action();
      this.showToast(successText, false);
    } catch (e: unknown) {
      this.showToast(describeWriteError(e, 'artist'), true);
    } finally {
      this.busyUid.set(null);
    }
  }

  private showToast(text: string, error: boolean): void {
    this.toast.set({ text, error });
    setTimeout(() => this.toast.set(null), 3500);
  }
}
