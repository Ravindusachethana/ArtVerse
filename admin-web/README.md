# ArtVerse Admin Panel (Web)

Angular web application for ArtVerse platform administration. Connects to the
same Firebase project as the Android app (`artverse-61539`) — Auth + Firestore.

## Features (current)

- **Admin login** with form validation and *role verification*: after Firebase
  Auth sign-in, the panel checks `users/{uid}.role == "admin"` in Firestore.
  Non-admin accounts (artists / art lovers) are signed out and denied.
- **Dashboard** with live platform counters (pending approvals, artists,
  art lovers, artworks).
- **Artist Approvals**: reviews the details each artist submitted at
  registration (business name, email, location, bio, categories) and
  approves or rejects. The decision is written to `artists/{uid}.status`;
  the mobile app listens to that document in real time, so an approved
  artist's device unlocks the artist dashboard automatically. A rejected /
  approved decision also drops an in-app notification for the artist.

- **Artwork Review**: new artworks are hidden from buyers until approved
  here (`artworks/{id}.moderationStatus`). Edits to already-published
  artworks arrive as a staged `pendingChanges` map and are shown as an
  old → new diff; approving merges the changes into the live listing,
  rejecting discards them. The artist is notified in-app either way.
- **Profile Changes**: artist profile edits are staged in
  `artists/{uid}.pendingChanges` and only go live after approval (name,
  phone and photo are applied to `users/{uid}`; studio name, location, bio
  and categories to `artists/{uid}`).

- **Order Delivery**: live delivery tracking. Once an artist hands an order to
  the delivery section it becomes `out_for_delivery` and appears here.
  *Delivery Completed* settles the sale — it writes the `transactions` audit
  records and credits the artist's `totalSales`, so the Sales Report only
  counts artwork that actually reached the buyer. *Reject order* cancels it
  and returns the reserved stock. Both notify the customer and the artist,
  whose order screens update live.

More admin modules will be added here later.

## Run locally

```bash
cd admin-web
npm install
npm start        # serves on http://localhost:4200
```

Production build: `npm run build` (output in `dist/artverse-admin`).

## One-time setup: create the admin account

The panel does not self-register admins (by design). Create one manually:

1. **Firebase console → Authentication → Users → Add user** — enter the admin
   email + password (e.g. `admin@artverse.com`). Copy the generated UID.
2. **Firestore → `users` collection → Add document** with the UID as document ID:

   ```
   uid:       "<the UID>"
   name:      "ArtVerse Admin"
   email:     "admin@artverse.com"
   role:      "admin"
   createdAt: <current epoch millis, number>
   ```

3. Deploy the updated Firestore rules from the repo root (`firestore.rules`),
   which grant the admin role permission to review artists, moderate artworks
   and apply profile edits:

   ```bash
   npm install -g firebase-tools   # once
   firebase login                  # once
   firebase deploy --only firestore:rules
   ```

   The repo root now carries `firebase.json` and `.firebaserc` (project
   `artverse-61539`), so the deploy command works as-is from there.

   No CLI? Paste `firestore.rules` into Firebase console → Firestore
   Database → Rules → **Publish**. That is the fastest one-off route.

   > ⚠️ **Redeploy the rules whenever `firestore.rules` changes.** The rules
   > are checked in but Firebase does not pick them up automatically. Symptom
   > of stale rules: one review action works (e.g. artist approval) while a
   > newer one fails with *"Permission denied…"* — that means the console is
   > running an older ruleset than the repo. Re-run the deploy above.

## Firebase web app registration (optional but recommended)

`src/app/core/firebase.ts` currently reuses the project's API key from
`google-services.json`, which works for Auth + Firestore. For a production
deployment, register a Web App in **Firebase console → Project settings →
Your apps → Add app → Web** and replace the config object with the generated
web config.

## Approval data model

| Field | Where | Values |
|---|---|---|
| `status` | `artists/{uid}` | `pending` (new registrations), `approved`, `rejected` |
| `pendingChanges` | `artists/{uid}` | staged profile edit awaiting review |
| `moderationStatus` | `artworks/{id}` | `pending` (new/resubmitted art), `approved`, `rejected` |
| `pendingChanges` | `artworks/{id}` | staged edit of a published artwork |
| `reviewedAt` / `reviewedBy` | both | epoch millis + admin UID of the decision |
| `status` | `orders/{id}` | `processing` → `confirmed` → `out_for_delivery` → `completed` / `rejected` |
| `confirmedAt` / `dispatchedAt` / `deliveredAt` | `orders/{id}` | stage timestamps driving the tracking bar |

Documents created **before** these features have no status field and are
treated as approved ("legacy" badge for artists), so existing accounts and
listings keep working.
