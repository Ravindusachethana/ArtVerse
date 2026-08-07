import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ArtistsService } from '../../core/artists.service';
import { ArtworksService } from '../../core/artworks.service';
import { OrdersService } from '../../core/orders.service';

/** Authenticated layout: brand sidebar + top bar around the admin pages. */
@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.css'
})
export class ShellComponent {

  private readonly authService = inject(AuthService);
  private readonly artistsService = inject(ArtistsService);
  private readonly artworksService = inject(ArtworksService);
  private readonly ordersService = inject(OrdersService);
  private readonly router = inject(Router);

  readonly admin = this.authService.admin;

  readonly pendingCount = computed(() =>
    this.artistsService.rows().filter((row) => row.effectiveStatus === 'pending').length);

  readonly artworkReviewCount = this.artworksService.reviewCount;

  readonly profileChangeCount = computed(() => this.artistsService.profileEdits().length);

  readonly deliveryCount = this.ordersService.deliveryCount;

  readonly initials = computed(() => {
    const name = this.admin()?.name ?? 'A';
    return name.split(' ').filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase();
  });

  async logout(): Promise<void> {
    await this.authService.logout();
    await this.router.navigate(['/login']);
  }
}
