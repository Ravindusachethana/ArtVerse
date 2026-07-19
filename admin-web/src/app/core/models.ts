/** Mirrors the Firestore documents written by the Android app. */

/** users/{uid} - base profile for every role (see mobile models/User.java). */
export interface UserProfile {
  uid: string;
  name: string;
  email: string;
  phone?: string | null;
  address?: string | null;
  profileImageUrl?: string | null;
  role: 'admin' | 'artist' | 'customer';
  createdAt: number;
}

export type ArtistStatus = 'pending' | 'approved' | 'rejected';
export type ReviewStatus = 'pending' | 'approved' | 'rejected';

/** artists/{uid} - extended artist profile (see mobile models/Artist.java). */
export interface ArtistProfile {
  uid: string;
  businessName: string;
  bio?: string;
  location?: string;
  categories?: string[];
  totalSales?: number;
  totalArtworks?: number;
  /** Approval workflow. Artists created before this feature have no status
   *  field and are treated as approved (legacy accounts). */
  status?: ArtistStatus;
  reviewedAt?: number;
  reviewedBy?: string;
  /** Staged profile edit awaiting review (written by the mobile app). */
  pendingChanges?: Record<string, unknown> | null;
}

/** artworks/{id} - mirrors mobile models/Artwork.java. */
export interface ArtworkDoc {
  id: string;
  title?: string;
  description?: string;
  categoryId?: string;
  categoryName?: string;
  artistId?: string;
  artistName?: string;
  price?: number;
  quantity?: number;
  imageUrls?: string[];
  medium?: string;
  dimensions?: string;
  available?: boolean;
  createdAt?: number;
  /** Null on artworks listed before moderation - treated as approved. */
  moderationStatus?: ReviewStatus;
  /** Staged edit of a published artwork awaiting review. */
  pendingChanges?: Record<string, unknown> | null;
  reviewedAt?: number;
  reviewedBy?: string;
}

/** One row of an old -> new comparison shown to the reviewing admin. */
export interface FieldChange {
  label: string;
  from: string;
  to: string;
  /** When true, from/to are image URLs and render as thumbnails. */
  isImage?: boolean;
}

/** Merged row shown in the approvals table. */
export interface ArtistRow {
  artist: ArtistProfile;
  user?: UserProfile;
  effectiveStatus: ArtistStatus;
  legacy: boolean;
}
