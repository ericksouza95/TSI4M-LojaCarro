import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';

import { Veiculo, VeiculoFiltro } from '../../core/models/veiculo.model';
import { NotificacaoService } from '../../core/services/notificacao.service';
import { VeiculoService } from '../../core/services/veiculo.service';
import { CarregandoComponent } from '../../shared/components/carregando/carregando.component';
import { EstadoVazioComponent } from '../../shared/components/estado-vazio/estado-vazio.component';
import { VeiculoCardComponent } from '../../shared/components/veiculo-card/veiculo-card.component';

// Catálogo público. Os filtros ficam na URL (?busca=...&precoMax=...), então a pesquisa pode ser compartilhada.
@Component({
  selector: 'app-veiculo-lista',
  imports: [
    ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule,
    VeiculoCardComponent, CarregandoComponent, EstadoVazioComponent,
  ],
  templateUrl: './veiculo-lista.component.html',
  styleUrl: './veiculo-lista.component.scss',
})
export class VeiculoListaComponent {
  private readonly veiculoService = inject(VeiculoService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly router = inject(Router);

  // Parâmetros de consulta da URL (ligados pelo roteador)
  readonly busca = input<string>();
  readonly precoMin = input<string>();
  readonly precoMax = input<string>();
  readonly anoMin = input<string>();
  readonly anoMax = input<string>();

  protected readonly veiculos = signal<Veiculo[]>([]);
  protected readonly carregando = signal(true);
  protected readonly filtrosAbertos = signal(false);

  protected readonly filtros = inject(FormBuilder).group({
    busca: [''],
    precoMin: [null as number | null, Validators.min(0)],
    precoMax: [null as number | null, Validators.min(0)],
    anoMin: [null as number | null, [Validators.min(1900), Validators.max(2100)]],
    anoMax: [null as number | null, [Validators.min(1900), Validators.max(2100)]],
  });

  constructor() {
    // Sempre que a URL mudar, atualiza o formulário e refaz a consulta
    effect(() => {
      const filtro: VeiculoFiltro = {
        busca: this.busca() ?? null,
        precoMin: numeroOuNulo(this.precoMin()),
        precoMax: numeroOuNulo(this.precoMax()),
        anoMin: numeroOuNulo(this.anoMin()),
        anoMax: numeroOuNulo(this.anoMax()),
      };
      untracked(() => {
        this.filtros.patchValue({ ...filtro, busca: filtro.busca ?? '' }, { emitEvent: false });
        this.carregar(filtro);
      });
    });
  }

  protected aplicarFiltros(): void {
    if (this.filtros.invalid) {
      this.filtros.markAllAsTouched();
      return;
    }
    const valores = this.filtros.getRawValue();
    const queryParams: Record<string, string | number> = {};
    for (const [chave, valor] of Object.entries(valores)) {
      if (valor !== null && valor !== '') {
        queryParams[chave] = typeof valor === 'string' ? valor.trim() : valor;
      }
    }
    this.router.navigate(['/veiculos'], { queryParams });
  }

  protected limparFiltros(): void {
    this.filtros.reset();
    this.router.navigate(['/veiculos']);
  }

  protected get quantidadeFiltros(): number {
    return Object.values(this.filtros.getRawValue()).filter((v) => v !== null && v !== '').length;
  }

  private carregar(filtro: VeiculoFiltro): void {
    this.carregando.set(true);
    this.veiculoService.listar(filtro).subscribe({
      next: (veiculos) => {
        this.veiculos.set(veiculos);
        this.carregando.set(false);
      },
      error: (erro) => {
        this.veiculos.set([]);
        this.carregando.set(false);
        this.notificacao.erroHttp(erro, 'Não foi possível carregar os veículos.');
      },
    });
  }
}

function numeroOuNulo(valor: string | undefined): number | null {
  if (valor === undefined || valor === '') {
    return null;
  }
  const numero = Number(valor);
  return Number.isFinite(numero) ? numero : null;
}
