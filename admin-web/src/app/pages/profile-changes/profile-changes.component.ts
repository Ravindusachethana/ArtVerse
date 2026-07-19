import { Component, inject, signal } from '@angular/core';
import { ArtistsService } from '../../core/artists.service';
import { AuthService } from '../../core/auth.service';
import { describeWriteError } from '../../core/errors';
import { ArtistRow, FieldChange } from '../../core/models';

/**
 * Artist profile-edit review (Admin FR-03).
 *
 * Artists can edit their public profile on mobile, but the changes are
 * staged in artists/{uid}.pendingChanges and only go live once approved
 * here. Approval writes name/phone/photo to users/{uid} and the studio
 * fields to artists/{uid}; rejection discards the staged edit. The artist
 * is notified in-app either way.
 */
@Component({
  selector: 'app-profile-changes',
  standalone: true,
  templateUrl: './profile-changes.component.html',
  styleUrl: './profile-changes.component.css'
})
export class ProfileChangesComponent {

  private readonly artistsService = inject(ArtistsService);
  private readonly authService = inject(AuthService);

  readonly loading = this.artistsService.loading;
  readonly requests = this.artistsService.profileEdits;
  readonly busyUid = signal<string | null>(null);
  readonly toast = signal<{ text: string; error: boolean } | null>(null);

  changesOf(row: ArtistRow): FieldChange[] {
    return this.artistsService.profileChangesOf(row);
  }

  async approve(row: ArtistRow): Promise<void> {
    await this.act(row, () => this.artistsService.approveProfileEdit(row, this.adminUid()),
      `Profile changes for ${row.artist.businessName} published`);
  }

  async reject(row: ArtistRow): Promise<void> {
    await this.act(row, () => this.artistsService.rejectProfileEdit(row, this.adminUid()),
      `Profile changes for ${row.artist.businessName} discarded`);
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
      this.showToast(describeWriteError(e, 'profile'), true);
    } finally {
      this.busyUid.set(null);
    }
  }

  private showToast(text: string, error: boolean): void {
    this.toast.set({ text, error });
    setTimeout(() => this.toast.set(null), 3500);
  }
}
