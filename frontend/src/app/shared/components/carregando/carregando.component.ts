import { Component, input } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-carregando',
  imports: [MatProgressSpinnerModule],
  template: `
    <div class="carregando" role="status" aria-live="polite">
      <mat-spinner diameter="40" />
      <span>{{ mensagem() }}</span>
    </div>
  `,
  styles: `
    .carregando {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      padding: 48px 16px;
      color: var(--mat-sys-on-surface-variant);
    }
  `,
})
export class CarregandoComponent {
  readonly mensagem = input('Carregando...');
}
