import { Injectable, computed, signal } from '@angular/core';

// Conta as requisições HTTP em andamento para exibir a barra de progresso global
@Injectable({ providedIn: 'root' })
export class CarregamentoService {
  private readonly pendentes = signal(0);

  readonly ativo = computed(() => this.pendentes() > 0);

  iniciar(): void {
    this.pendentes.update((n) => n + 1);
  }

  finalizar(): void {
    this.pendentes.update((n) => Math.max(0, n - 1));
  }
}
