import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ArtistsService } from '../../core/artists.service';
import { AuthService } from '../../core/auth.service';
import { describeWriteError } from '../../core/errors';
import { ArtworkDoc, FieldChange } from '../../core/models';

type ArtworkTab = 'Artist sales';



export class ArtistSalesComponent {

  private readonly artworksService = inject(ArtistsService);
  private readonly authService = inject(AuthService);
  

  readonly liveArtistSales = this.artworksService;
}
