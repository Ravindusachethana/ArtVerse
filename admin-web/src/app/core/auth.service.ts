import { Injectable, signal } from '@angular/core';
import {
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signOut,
  User
} from 'firebase/auth';
import { doc, getDoc } from 'firebase/firestore';
import { auth, db } from './firebase';
import { UserProfile } from './models';

/**
 * Handles admin sign-in and role verification. A successful Firebase Auth
 * login is NOT enough to enter the panel: the users/{uid} document must
 * carry role == "admin", otherwise the session is closed immediately.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  /** The verified admin profile, or null when signed out / not an admin. */
  readonly admin = signal<UserProfile | null>(null);

  private readonly initialCheck: Promise<void>;

  constructor() {
    this.initialCheck = new Promise<void>((resolve) => {
      const unsubscribe = onAuthStateChanged(auth, async (user) => {
        unsubscribe();
        if (user) {
          const profile = await this.fetchAdminProfile(user);
          if (profile) {
            this.admin.set(profile);
          } else {
            await signOut(auth).catch(() => { /* already signed out */ });
          }
        }
        resolve();
      });
    });
  }

  /** Resolves once the persisted session (if any) has been verified. */
  async waitForAdmin(): Promise<UserProfile | null> {
    await this.initialCheck;
    return this.admin();
  }

  /**
   * Signs in and verifies the admin role. Throws a user-readable Error on
   * bad credentials or when the account is not an administrator.
   */
  async login(email: string, password: string): Promise<UserProfile> {
    let user: User;
    try {
      const credential = await signInWithEmailAndPassword(auth, email, password);
      user = credential.user;
    } catch (e: unknown) {
      throw new Error(this.describeAuthError(e));
    }

    const profile = await this.fetchAdminProfile(user);
    if (!profile) {
      await signOut(auth).catch(() => { /* ignore */ });
      throw new Error('This account does not have administrator access.');
    }

    this.admin.set(profile);
    return profile;
  }

  async logout(): Promise<void> {
    await signOut(auth);
    this.admin.set(null);
  }

  private async fetchAdminProfile(user: User): Promise<UserProfile | null> {
    try {
      const snapshot = await getDoc(doc(db, 'users', user.uid));
      if (!snapshot.exists()) return null;
      const profile = snapshot.data() as UserProfile;
      return profile.role === 'admin' ? { ...profile, uid: user.uid } : null;
    } catch {
      return null;
    }
  }

  private describeAuthError(e: unknown): string {
    const code = (e as { code?: string })?.code ?? '';
    switch (code) {
      case 'auth/invalid-credential':
      case 'auth/wrong-password':
      case 'auth/user-not-found':
        return 'Incorrect email or password.';
      case 'auth/too-many-requests':
        return 'Too many failed attempts. Please try again later.';
      case 'auth/network-request-failed':
        return 'Network error. Check your connection and try again.';
      case 'auth/user-disabled':
        return 'This account has been disabled.';
      default:
        return 'Login failed. Please try again.';
    }
  }
}
