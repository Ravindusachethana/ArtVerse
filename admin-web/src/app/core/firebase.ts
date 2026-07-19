import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';

/**
 * Same Firebase project as the ArtVerse Android app (see app/google-services.json).
 * If you register a dedicated Web App in Firebase console (Project settings ->
 * Your apps -> Add app -> Web), replace apiKey/appId below with the generated
 * web config. Auth + Firestore work with the values here.
 */
const firebaseConfig = {
  apiKey: 'AIzaSyDEuWUo4cHBVXCrWRFUHvOAa4Ek4XdPA54',
  authDomain: 'artverse-61539.firebaseapp.com',
  projectId: 'artverse-61539',
  storageBucket: 'artverse-61539.firebasestorage.app',
  messagingSenderId: '227106672464'
};

export const firebaseApp = initializeApp(firebaseConfig);
export const auth = getAuth(firebaseApp);
export const db = getFirestore(firebaseApp);
