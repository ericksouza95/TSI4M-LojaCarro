import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { Veiculo } from '../../core/models/veiculo.model';
import { AuthService } from '../../core/services/auth.service';
import { VeiculoService } from '../../core/services/veiculo.service';
import { mensagemDeErro } from '../../core/utils/erros';
import { CarregandoComponent } from '../../shared/components/carregando/carregando.component';
import { EstadoVazioComponent } from '../../shared/components/estado-vazio/estado-vazio.component';
import { StatusChipComponent } from '../../shared/components/status-chip/status-chip.component';
import { RotuloPipe } from '../../shared/pipes/rotulo.pipe';

@Component({
  selector: 'app-veiculo-detalhe',
  imports: [
    RouterLink, MatButtonModule, MatIconModule, CurrencyPipe, DecimalPipe, RotuloPipe,
    CarregandoComponent, EstadoVazioComponent, StatusChipComponent,
  ],
  templateUrl: './veiculo-detalhe.component.html',
  styleUrl: './veiculo-detalhe.component.scss',
})
export class VeiculoDetalheComponent {
  private readonly veiculoService = inject(VeiculoService);
  protected readonly auth = inject(AuthService);

  readonly id = input.required<string>();

  protected readonly veiculo = signal<Veiculo | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);

  constructor() {
    effect(() => {
      const id = Number(this.id());
      untracked(() => this.carregar(id));
    });
  }

  private carregar(id: number): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.veiculoService.buscar(id).subscribe({
      next: (veiculo) => {
        this.veiculo.set(veiculo);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        const naoEncontrado = erro instanceof HttpErrorResponse && erro.status === 404;
        this.erro.set(naoEncontrado ? 'Este veículo não existe ou não está mais disponível.' : mensagemDeErro(erro));
        this.carregando.set(false);
      },
    });
  }
}
