import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Blocks every admin route until the signed-in user is a verified admin. */
export const adminGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const admin = await authService.waitForAdmin();
  return admin ? true : router.createUrlTree(['/login']);
};
