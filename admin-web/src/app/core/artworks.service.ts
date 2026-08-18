import { Injectable, OnDestroy, computed, signal } from '@angular/core';
import {
  collection,
  deleteField,
  doc,
  onSnapshot,
  updateDoc
} from 'firebase/firestore';
import { db } from './firebase';
import { ArtworkDoc, FieldChange } from './models';
import { notifyUser } from './notifications';

/**
 * Live view over the "artworks" collection plus the moderation actions:
 *
 *  - New submissions carry moderationStatus == "pending" and are invisible
 *    to buyers until approved here.
 *  - Edits of published artworks arrive as a pendingChanges map; approval
 *    merges the map into the live document, rejection discards it. Either
 *    way the artist is notified in-app.
 */
@Injectable({ providedIn: 'root' })
export class ArtworksService implements OnDestroy {

  readonly artworks = signal<ArtworkDoc[]>([]);
  readonly loading = signal(true);

  /** New artworks waiting for their first review. */
  readonly pendingNew = computed(() =>
    this.artworks().filter((a) => a.moderationStatus === 'pending'));

  /** Published artworks with a staged edit awaiting review. */
  readonly editRequests = computed(() =>
    this.artworks().filter((a) =>
      a.moderationStatus !== 'pending'
      && a.pendingChanges && Object.keys(a.pendingChanges).length > 0));

  readonly rejected = computed(() =>
    this.artworks().filter((a) => a.moderationStatus === 'rejected'));

  /** Everything on the review desk - drives the sidebar badge. */
  readonly reviewCount = computed(() => this.pendingNew().length + this.editRequests().length);

  private readonly unsubscribe: () => void;

  constructor() {
    this.unsubscribe = onSnapshot(collection(db, 'artworks'), (snapshot) => {
      const list = snapshot.docs.map((d) => ({ ...(d.data() as ArtworkDoc), id: d.id }));
      list.sort((a, b) => (b.createdAt ?? 0) - (a.createdAt ?? 0));
      this.artworks.set(list);
      this.loading.set(false);
    });
  }

  ngOnDestroy(): void {
    this.unsubscribe();
  }

  async approveNew(artwork: ArtworkDoc, adminUid: string): Promise<void> {
    await updateDoc(doc(db, 'artworks', artwork.id), {
      moderationStatus: 'approved',
      reviewedAt: Date.now(),
      reviewedBy: adminUid
    });
    await this.tellArtist(artwork, 'Artwork approved',
      `Great news! "${artwork.title}" has been approved and is now live on ArtVerse.`);
  }

  async rejectNew(artwork: ArtworkDoc, adminUid: string): Promise<void> {
    await updateDoc(doc(db, 'artworks', artwork.id), {
      moderationStatus: 'rejected',
      reviewedAt: Date.now(),
      reviewedBy: adminUid
    });
    await this.tellArtist(artwork, 'Artwork rejected',
      `Unfortunately "${artwork.title}" was not approved for publication. You can edit and resubmit it from My Art.`);
  }

  /** Publishes the staged edit: merge pendingChanges into the live fields. */
  async approveEdit(artwork: ArtworkDoc, adminUid: string): Promise<void> {
    await updateDoc(doc(db, 'artworks', artwork.id), {
      ...(artwork.pendingChanges ?? {}),
      pendingChanges: deleteField(),
      reviewedAt: Date.now(),
      reviewedBy: adminUid
    });
    await this.tellArtist(artwork, 'Artwork update approved',
      `Your changes to "${artwork.title}" have been approved and are now live.`);
  }

  /** Discards the staged edit; the live listing stays as it was. */
  async rejectEdit(artwork: ArtworkDoc, adminUid: string): Promise<void> {
    await updateDoc(doc(db, 'artworks', artwork.id), {
      pendingChanges: deleteField(),
      reviewedAt: Date.now(),
      reviewedBy: adminUid
    });
    await this.tellArtist(artwork, 'Artwork update rejected',
      `Your changes to "${artwork.title}" were not approved. The current listing stays as it is.`);
  }

  /** Old -> new comparison of a staged artwork edit for the review card. */
  changesOf(artwork: ArtworkDoc): FieldChange[] {
    const staged = artwork.pendingChanges ?? {};
    const changes: FieldChange[] = [];

    const compare = (key: keyof ArtworkDoc, label: string, format: (v: unknown) => string) => {
      if (!(key in staged)) return;
      const from = format(artwork[key]);
      const to = format(staged[key as string]);
      if (from !== to) changes.push({ label, from, to });
    };

    const asText = (v: unknown) => (v === null || v === undefined || v === '' ? '—' : String(v));
    const asPrice = (v: unknown) => (typeof v === 'number' ? 'LKR ' + v.toLocaleString('en-US') : asText(v));
    const asdPrice = (v: unknown) => (typeof v === 'number' ? 'LKR ' + v.toLocaleString('en-US') : asText(v));

    compare('title', 'Title', asText);
    compare('description', 'Description', asText);
    compare('categoryName', 'Category', asText);
    compare('price', 'Price', asPrice);
    compare('dprice', 'dPrice', asdPrice);
    compare('quantity', 'Quantity', asText);
    compare('medium', 'Medium', asText);
    compare('dimensions', 'Dimensions', asText);

    const oldImage = artwork.imageUrls?.[0] ?? '';
    const stagedImages = staged['imageUrls'];
    const newImage = Array.isArray(stagedImages) ? String(stagedImages[0] ?? '') : oldImage;
    if ('imageUrls' in staged && oldImage !== newImage) {
      changes.push({ label: 'Image', from: oldImage, to: newImage, isImage: true });
    }

    return changes;
  }

  private tellArtist(artwork: ArtworkDoc, title: string, message: string): Promise<void> {
    return artwork.artistId ? notifyUser(artwork.artistId, title, message) : Promise.resolve();
  }
}
