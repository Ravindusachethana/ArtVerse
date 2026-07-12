# ArtVerse — Art Gallery Sales Management App

A native Android app (Java) for buying and selling artwork, built on Firebase.
Two roles share one app: **Art Lovers (customers)** browse, cart, and buy;
**Artists** list work, fulfil orders, and track sales — matching the modules
in your dissertation (FR01–FR09).

## What's included

- Full Android Studio project (Java, XML layouts, Material 3 theming)
- Firebase Authentication (email/password) with role-based routing
- Firestore data layer for users, artists, artworks, cart, orders, transactions
- Firebase Cloud Storage for artwork images
- Customer flow: browse/search, artwork detail, cart, checkout, order history, profile
- Artist flow: dashboard, manage artwork (add/edit/delete with image upload),
  incoming orders (accept/reject), sales report
- `firestore.rules` and `storage.rules` implementing the access-control model
  from your Security section (2.1.3.2)
- A warm, gallery-inspired visual style (burgundy + gold on a warm-white canvas)
  with Material 3 components, card-based layouts, and a custom launcher icon

## 1. Open the project

1. Install **Android Studio** (Ladybird/Koala or newer).
2. `File → Open` and select the `ArtVerse` folder (the one containing
   `settings.gradle`).
3. Let Gradle sync — it will download the Firebase, Material, Glide, and
   AndroidX dependencies automatically the first time.

## 2. Create your Firebase project

1. Go to the [Firebase Console](https://console.firebase.google.com) →
   **Add project** → name it (e.g. "ArtVerse").
2. Inside the project, click **Add app → Android**.
   - Package name: `com.artverse.app` (must match exactly — this is set in
     `app/build.gradle`).
   - Download the generated **`google-services.json`**.
3. Copy `google-services.json` into the `app/` folder of this project
   (next to `build.gradle`). It's git-ignored on purpose — every developer
   uses their own Firebase project/credentials.

## 3. Turn on the Firebase services the app uses

In the Firebase Console:

- **Authentication → Sign-in method** → enable **Email/Password**.
- **Firestore Database** → Create database (start in production mode).
  - Go to **Rules** and paste in the contents of `firestore.rules` from this
    repo, then **Publish**.
- **Storage** → Get started (production mode).
  - Go to **Rules** and paste in the contents of `storage.rules`, then
    **Publish**.

## 4. Run it

Build and run on an emulator or device (minSdk 24 / Android 7.0+). From the
splash screen you can register as either an **Art Lover** or an **Artist** —
try both to see the full flow: list a piece as an artist, then browse and buy
it as a customer in a second account.

## Firestore data model

Mirrors the ER diagram in Chapter 3 of the dissertation, mapped onto
Firestore's document/collection structure (Section 3.7):

| Collection | Purpose |
|---|---|
| `users/{uid}` | Shared profile for both roles (name, email, phone, address, role) |
| `artists/{uid}` | Artist-only extension: business name, bio, categories, running totals |
| `artworks/{id}` | One listing per piece — price, images, category, `artistId` |
| `users/{uid}/cart/{artworkId}` | Per-customer cart subcollection |
| `orders/{id}` | One order per artist per checkout (an order is scoped to a single artist, per the ER model); status flows `pending → processing/rejected` |
| `transactions/{id}` | Settled sale line — the audit trail from Section 2.1.3.2, feeds the Sales Report (FR09) |

Real-time listeners (`addSnapshotListener`) are used on the browse grid,
cart, both order lists, and the sales report, so changes made by one role are
reflected immediately for the other — satisfying NFR04.

## Where each requirement lives

| Req | Screen / class |
|---|---|
| FR01 Customer registration | `auth/CustomerRegisterActivity` |
| FR02 Artist registration | `auth/ArtistRegisterActivity` |
| FR03 Login | `auth/LoginActivity` |
| FR04 Artwork management | `artist/AddEditArtworkActivity`, `artist/fragments/MyArtworkFragment` |
| FR05 Browse & search | `customer/fragments/HomeFragment` |
| FR06 Cart management | `customer/fragments/CartFragment`, `adapters/CartAdapter` |
| FR07 Order management | `customer/CheckoutActivity`, `customer/fragments/CustomerOrdersFragment`, `artist/fragments/ArtistOrdersFragment` |
| FR08 Payment (simulated) | `customer/CheckoutActivity#placeOrder()` — swap in a real gateway callback here |
| FR09 Sales tracking | `artist/fragments/SalesReportFragment` |

## Extending it

- **Payment gateway**: `CheckoutActivity.placeOrder()` currently confirms the
  order directly; wire in Stripe/PayHere/etc. before that call and only
  proceed to the batch write on a successful charge callback.
- **Multiple images per artwork**: the `Artwork.imageUrls` field is already a
  list (matches "up to five images" in the ER model) — `AddEditArtworkActivity`
  currently uploads one cover image; extend `imagePicker` to
  `ActivityResultContracts.GetMultipleContents()` to add more.
- **Push notifications**: listed as future work in your dissertation (6.3) —
  add Firebase Cloud Messaging and trigger from the order-status update calls
  already in `ArtistOrdersFragment`.
- **App icon / branding**: generated as a burgundy-and-gold adaptive icon in
  `res/mipmap-anydpi-v26`; regenerate via Android Studio's Image Asset tool
  if you want a different mark.

## Notes for your write-up

- Chapter 4 (Implementation) code screenshots: the modules described there
  map directly onto the package structure — `auth/`, `customer/`, `artist/`,
  `models/`, `adapters/`, `utils/`.
- Chapter 5 (Testing): the app is structured with data-access logic
  (`utils/FirebaseUtil`) separated from UI (Activities/Fragments), which
  keeps unit tests of validation logic (`utils/ValidationUtil`) independent
  of any Android UI dependency.
