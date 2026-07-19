/**
 * Turns a thrown Firestore/Auth error into a short, admin-readable sentence.
 * Crucially it distinguishes a rules rejection (permission-denied) from a
 * genuine outage, so a stale-rules deployment is obvious from the toast
 * instead of hiding behind a generic "try again".
 */
export function describeWriteError(e: unknown, subject: string): string {
  const code = (e as { code?: string })?.code ?? '';
  switch (code) {
    case 'permission-denied':
      return `Permission denied updating the ${subject}. Deploy the latest firestore.rules `
        + `(Firebase console -> Firestore -> Rules), then try again.`;
    case 'unavailable':
    case 'deadline-exceeded':
      return `Network problem updating the ${subject}. Check your connection and try again.`;
    case 'not-found':
      return `Could not find the ${subject} record to update. It may have been removed.`;
    default:
      return `Could not update the ${subject}${code ? ` (${code})` : ''}. Please try again.`;
  }
}
