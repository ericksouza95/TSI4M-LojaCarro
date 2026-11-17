import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

// Mensagem exibida quando uma lista não tem itens; ações podem ser passadas como conteúdo
@Component({
  selector: 'app-estado-vazio',
  imports: [MatIconModule],
  template: `
    <div class="vazio">
      <mat-icon>{{ icone() }}</mat-icon>
      <h3>{{ titulo() }}</h3>
      @if (mensagem()) {
        <p>{{ mensagem() }}</p>
      }
      <ng-content />
    </div>
  `,
  styles: `
    .vazio {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 48px 16px;
      text-align: center;
      color: var(--mat-sys-on-surface-variant);
    }
    mat-icon { font-size: 48px; width: 48px; height: 48px; color: var(--mat-sys-outline); }
    h3 { margin: 0; color: var(--mat-sys-on-surface); }
    p { margin: 0 0 8px; max-width: 420px; }
  `,
})
export class EstadoVazioComponent {
  readonly icone = input('search_off');
  readonly titulo = input.required<string>();
  readonly mensagem = input<string>();
}
