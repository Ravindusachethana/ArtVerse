import { Injectable, OnDestroy, computed, signal } from '@angular/core';
import {
  collection,
  deleteField,
  doc,
  getCountFromServer,
  onSnapshot,
  query,
  updateDoc,
  where,
  writeBatch
} from 'firebase/firestore';
import { db } from './firebase';
import { ArtistProfile, ArtistRow, ArtistStatus, FieldChange, UserProfile } from './models';
import { notifyUser } from './notifications';

/**
 * Live view over the "artists" collection merged with the matching
 * "users" documents (email / joined date live there), plus the
 * approve / reject actions of the admin workflow.
 *
 * Approving or rejecting writes the status onto artists/{uid}; the mobile
 * app listens to that document in real time, so access is granted (or the
 * rejection shown) on the artist's device without any manual refresh.
 */
@Injectable({ providedIn: 'root' })
export class ArtistsService implements OnDestroy {

  readonly rows = signal<ArtistRow[]>([]);
  readonly loading = signal(true);

  /** Approved artists with a staged profile edit awaiting review. */
  readonly profileEdits = computed(() =>
    this.rows().filter((row) =>
      row.artist.pendingChanges && Object.keys(row.artist.pendingChanges).length > 0));

  private artists = new Map<string, ArtistProfile>();
  private users = new Map<string, UserProfile>();
  private readonly unsubscribers: Array<() => void> = [];

  constructor() {
    this.unsubscribers.push(
      onSnapshot(collection(db, 'artists'), (snapshot) => {
        this.artists = new Map(
          snapshot.docs.map((d) => [d.id, { ...(d.data() as ArtistProfile), uid: d.id }])
        );
        this.rebuild();
      }),
      onSnapshot(query(collection(db, 'users'), where('role', '==', 'artist')), (snapshot) => {
        this.users = new Map(
          snapshot.docs.map((d) => [d.id, { ...(d.data() as UserProfile), uid: d.id }])
        );
        this.rebuild();
      })
    );
  }

  ngOnDestroy(): void {
    this.unsubscribers.forEach((unsubscribe) => unsubscribe());
  }

  async approve(row: ArtistRow, adminUid: string): Promise<void> {
    await this.review(row, adminUid, 'approved',
      'Registration approved',
      'Welcome to ArtVerse! Your artist account has been approved - you now have full access to your artist dashboard.');
  }

  async reject(row: ArtistRow, adminUid: string): Promise<void> {
    await this.review(row, adminUid, 'rejected',
      'Registration rejected',
      'Unfortunately your artist registration was not approved. Please contact support for more information.');
  }

  /** Counts used by the dashboard overview cards. */
  async counts(): Promise<{ pendingArtists: number; artists: number; customers: number; artworks: number }> {
    const [pendingArtists, artists, customers, artworks] = await Promise.all([
      getCountFromServer(query(collection(db, 'artists'), where('status', '==', 'pending'))),
      getCountFromServer(collection(db, 'artists')),
      getCountFromServer(query(collection(db, 'users'), where('role', '==', 'customer'))),
      getCountFromServer(collection(db, 'artworks'))
    ]);
    return {
      pendingArtists: pendingArtists.data().count,
      artists: artists.data().count,
      customers: customers.data().count,
      artworks: artworks.data().count
    };
  }

  private async review(row: ArtistRow, adminUid: string, status: ArtistStatus,
                       title: string, message: string): Promise<void> {
    await updateDoc(doc(db, 'artists', row.artist.uid), {
      status,
      reviewedAt: Date.now(),
      reviewedBy: adminUid
    });
    await notifyUser(row.artist.uid, title, message);
  }

  /* ---------- Staged profile edits (Edit Artist Profile scenario) ---------- */

  /**
   * Publishes a staged profile edit: name/phone/photo go to users/{uid},
   * studio fields go to artists/{uid}, and the staging map is cleared.
   */
  async approveProfileEdit(row: ArtistRow, adminUid: string): Promise<void> {
    const staged = row.artist.pendingChanges ?? {};

    // updateDoc's typed signature wants field values, not `unknown`.
    const userUpdates: Record<string, any> = {};
    for (const key of ['name', 'phone', 'profileImageUrl']) {
      if (key in staged) userUpdates[key] = staged[key];
    }

    const artistUpdates: Record<string, any> = {};
    for (const key of ['businessName', 'location', 'bio', 'categories']) {
      if (key in staged) artistUpdates[key] = staged[key];
    }

    // Both docs go live together (or neither) - a batch is atomic and,
    // just as importantly, fails as a single unit if a rule blocks it,
    // so we never leave the staged edit half-applied.
    const batch = writeBatch(db);
    if (Object.keys(userUpdates).length > 0) {
      batch.update(doc(db, 'users', row.artist.uid), userUpdates);
    }
    batch.update(doc(db, 'artists', row.artist.uid), {
      ...artistUpdates,
      pendingChanges: deleteField(),
      reviewedAt: Date.now(),
      reviewedBy: adminUid
    });
    await batch.commit();

    await notifyUser(row.artist.uid, 'Profile update approved',
      'Your profile changes have been approved and are now visible on ArtVerse.');
  }

  /** Discards a staged profile edit; the live profile stays as it was. */
  async rejectProfileEdit(row: ArtistRow, adminUid: string): Promise<void> {
    await updateDoc(doc(db, 'artists', row.artist.uid), {
      pendingChanges: deleteField(),
      reviewedAt: Date.now(),
      reviewedBy: adminUid
    });
    await notifyUser(row.artist.uid, 'Profile update rejected',
      'Your profile changes were not approved. Your current profile stays as it is.');
  }

  /** Old -> new comparison of a staged profile edit for the review card. */
  profileChangesOf(row: ArtistRow): FieldChange[] {
    const staged = row.artist.pendingChanges ?? {};
    const changes: FieldChange[] = [];

    const asText = (v: unknown) => {
      if (v === null || v === undefined || v === '') return '—';
      return Array.isArray(v) ? v.join(', ') : String(v);
    };

    const compare = (key: string, label: string, current: unknown) => {
      if (!(key in staged)) return;
      const from = asText(current);
      const to = asText(staged[key]);
      if (from !== to) changes.push({ label, from, to });
    };

    compare('name', 'Display name', row.user?.name);
    compare('businessName', 'Studio name', row.artist.businessName);
    compare('phone', 'Phone', row.user?.phone);
    compare('location', 'Location', row.artist.location);
    compare('bio', 'Bio', row.artist.bio);
    compare('categories', 'Categories', row.artist.categories);

    if ('profileImageUrl' in staged
        && staged['profileImageUrl'] !== (row.user?.profileImageUrl ?? '')) {
      changes.push({
        label: 'Photo',
        from: row.user?.profileImageUrl ?? '',
        to: String(staged['profileImageUrl'] ?? ''),
        isImage: true
      });
    }

    return changes;
  }

  private rebuild(): void {
    const rows: ArtistRow[] = [...this.artists.values()].map((artist) => {
      const legacy = !artist.status;
      return {
        artist,
        user: this.users.get(artist.uid),
        // Artists registered before the approval feature have no status
        // field; they keep their access and show up as legacy-approved.
        effectiveStatus: artist.status ?? 'approved',
        legacy
      };
    });

    // Newest registrations first inside each status group.
    rows.sort((a, b) => (b.user?.createdAt ?? 0) - (a.user?.createdAt ?? 0));

    this.rows.set(rows);
    this.loading.set(false);
  }
}
