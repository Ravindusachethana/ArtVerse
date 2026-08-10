import { initializeApp } from 'firebase/app';
import { getAuth, connectAuthEmulator } from 'firebase/auth';
import { getFirestore, connectFirestoreEmulator } from 'firebase/firestore';

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

// Demo/offline switch: run `localStorage.demo = 'true'` in the browser console,
// then reload, to point the panel at the local emulator. Remove it to go back online.
if (localStorage.getItem('demo') === 'true') {
  connectAuthEmulator(auth, 'http://localhost:9099');
  connectFirestoreEmulator(db, 'localhost', 8080);
}
