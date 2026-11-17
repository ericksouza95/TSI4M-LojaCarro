import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { Veiculo } from '../../../core/models/veiculo.model';
import { RotuloPipe } from '../../pipes/rotulo.pipe';

@Component({
  selector: 'app-veiculo-card',
  imports: [RouterLink, MatIconModule, CurrencyPipe, DecimalPipe, RotuloPipe],
  template: `
    @let v = veiculo();
    <a class="card" [routerLink]="['/veiculos', v.id]" [attr.aria-label]="v.marca + ' ' + v.modelo">
      <div class="foto">
        @if (v.fotoUrl) {
          <img [src]="v.fotoUrl" [alt]="'Foto do ' + v.marca + ' ' + v.modelo" loading="lazy" />
        } @else {
          <mat-icon>directions_car</mat-icon>
        }
        <span class="ano">{{ v.anoFabricacao }}/{{ v.anoModelo }}</span>
      </div>
      <div class="corpo">
        <span class="marca">{{ v.marca }}</span>
        <h3>{{ v.modelo }}</h3>
        <ul class="specs">
          <li><mat-icon>speed</mat-icon>{{ v.quilometragem | number }} km</li>
          <li><mat-icon>local_gas_station</mat-icon>{{ v.combustivel | rotulo: 'combustivel' }}</li>
          <li><mat-icon>settings</mat-icon>{{ v.transmissao | rotulo: 'transmissao' }}</li>
        </ul>
        <strong class="preco">{{ v.preco | currency }}</strong>
      </div>
    </a>
  `,
  styleUrl: './veiculo-card.component.scss',
})
export class VeiculoCardComponent {
  readonly veiculo = input.required<Veiculo>();
}
