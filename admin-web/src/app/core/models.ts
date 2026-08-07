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
  /**
   * True once a one-of-a-kind piece (Painting, Drawing, Mixed Media) has sold
   * and its delivery completed - it leaves the browse gallery and the artist's
   * My Art, but the document stays for reports and the buyer's collection. Set
   * by OrdersService.markDelivered; never on reproducible pieces.
   */
  retired?: boolean;
  /** Null on artworks listed before moderation - treated as approved. */
  moderationStatus?: ReviewStatus;
  /** Staged edit of a published artwork awaiting review. */
  pendingChanges?: Record<string, unknown> | null;
  reviewedAt?: number;
  reviewedBy?: string;
}

export type OrderStatus =
  | 'pending'            // legacy, treated like "processing"
  | 'processing'         // placed, awaiting the artist
  | 'confirmed'          // artist accepted, preparing
  | 'out_for_delivery'   // handed to delivery - the admin's queue
  | 'completed'          // delivery confirmed by the admin
  | 'rejected';

/** One line of an order (mirrors mobile models/OrderItem.java). */
export interface OrderItemDoc {
  artworkId?: string;
  title?: string;
  imageUrl?: string;
  /** Category at the time of purchase; absent on orders placed before it was recorded. */
  categoryName?: string;
  unitPrice?: number;
  quantity?: number;
}

/** orders/{id} - mirrors mobile models/Order.java. */
export interface OrderDoc {
  id: string;
  customerId?: string;
  customerName?: string;
  artistId?: string;
  deliveryAddress?: string;
  items?: OrderItemDoc[];
  totalAmount?: number;
  status?: OrderStatus;
  paymentMethod?: string;
  orderDate?: number;
  confirmedAt?: number;
  dispatchedAt?: number;
  deliveredAt?: number;
  settledBy?: string;
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
