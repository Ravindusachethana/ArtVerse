import { Routes } from '@angular/router';
import { adminGuard } from './core/admin.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  {
    path: '',
    canActivate: [adminGuard],
    loadComponent: () => import('./pages/shell/shell.component').then(m => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'artist-approvals', loadComponent: () => import('./pages/artist-approvals/artist-approvals.component').then(m => m.ArtistApprovalsComponent) },
      { path: 'artwork-approvals', loadComponent: () => import('./pages/artwork-approvals/artwork-approvals.component').then(m => m.ArtworkApprovalsComponent) },
      { path: 'profile-changes', loadComponent: () => import('./pages/profile-changes/profile-changes.component').then(m => m.ProfileChangesComponent) }
    ]
  },
  { path: '**', redirectTo: '' }
];
