import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ArtworksService } from '../../core/artworks.service';
import { AuthService } from '../../core/auth.service';
import { describeWriteError } from '../../core/errors';
import { ArtworkDoc, FieldChange } from '../../core/models';

type ArtworkTab = 'new' | 'edits' | 'rejected';

/**
 * Artwork moderation (Admin FR-02).
 *
 * "New submissions": artworks uploaded by artists that are hidden from
 * buyers until approved here. "Edit requests": staged changes to published
 * artworks, shown as an old -> new diff; approving publishes the changes,
 * rejecting keeps the current listing. Artists are notified in-app either way.
 */
@Component({
  selector: 'app-artwork-approvals',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './artwork-approvals.component.html',
  styleUrl: './artwork-approvals.component.css'
})
export class ArtworkApprovalsComponent {

  private readonly artworksService = inject(ArtworksService);
  private readonly authService = inject(AuthService);

  readonly tabs: Array<{ key: ArtworkTab; label: string }> = [
    { key: 'new', label: 'New submissions' },
    { key: 'edits', label: 'Edit requests' },
    { key: 'rejected', label: 'Rejected' }
  ];

  readonly activeTab = signal<ArtworkTab>('new');
  readonly loading = this.artworksService.loading;
  readonly busyId = signal<string | null>(null);
  readonly toast = signal<{ text: string; error: boolean } | null>(null);

  readonly countsByTab = computed<Record<ArtworkTab, number>>(() => ({
    new: this.artworksService.pendingNew().length,
    edits: this.artworksService.editRequests().length,
    rejected: this.artworksService.rejected().length
  }));

  readonly visibleArtworks = computed(() => {
    switch (this.activeTab()) {
      case 'new': return this.artworksService.pendingNew();
      case 'edits': return this.artworksService.editRequests();
      case 'rejected': return this.artworksService.rejected();
    }
  });

  changesOf(artwork: ArtworkDoc): FieldChange[] {
    return this.artworksService.changesOf(artwork);
  }

  coverOf(artwork: ArtworkDoc): string | null {
    return artwork.imageUrls?.[0] ?? null;
  }

  async approve(artwork: ArtworkDoc): Promise<void> {
    const isEdit = this.activeTab() === 'edits';
    await this.act(artwork,
      () => isEdit
        ? this.artworksService.approveEdit(artwork, this.adminUid())
        : this.artworksService.approveNew(artwork, this.adminUid()),
      isEdit ? `Changes to "${artwork.title}" published` : `"${artwork.title}" is now live`);
  }

  async reject(artwork: ArtworkDoc): Promise<void> {
    const isEdit = this.activeTab() === 'edits';
    await this.act(artwork,
      () => isEdit
        ? this.artworksService.rejectEdit(artwork, this.adminUid())
        : this.artworksService.rejectNew(artwork, this.adminUid()),
      isEdit ? `Changes to "${artwork.title}" discarded` : `"${artwork.title}" rejected`);
  }

  private adminUid(): string {
    return this.authService.admin()?.uid ?? '';
  }

  private async act(artwork: ArtworkDoc, action: () => Promise<void>, successText: string): Promise<void> {
    this.busyId.set(artwork.id);
    try {
      await action();
      this.showToast(successText, false);
    } catch (e: unknown) {
      this.showToast(describeWriteError(e, 'artwork'), true);
    } finally {
      this.busyId.set(null);
    }
  }

  private showToast(text: string, error: boolean): void {
    this.toast.set({ text, error });
    setTimeout(() => this.toast.set(null), 3500);
  }
}
