import { Component, computed, input } from '@angular/core';

import { RotuloPipe } from '../../pipes/rotulo.pipe';

// Etiqueta colorida para a situação do veículo ou do agendamento
@Component({
  selector: 'app-status-chip',
  imports: [RotuloPipe],
  template: `<span [class]="classe()">{{ valor() | rotulo: tipo() }}</span>`,
  styles: `
    .chip {
      display: inline-block;
      padding: 3px 12px;
      border-radius: 999px;
      font-size: 0.8rem;
      font-weight: 600;
      white-space: nowrap;
      background: var(--mat-sys-surface-container-high);
      color: var(--mat-sys-on-surface);
    }
    .chip-disponivel, .chip-confirmado { background: #d8f3dc; color: #1b5e20; }
    .chip-vendido, .chip-realizado { background: #dbe7ff; color: #0d3c8c; }
    .chip-inativo, .chip-cancelado { background: #fde2e1; color: #9b1c1c; }
  `,
})
export class StatusChipComponent {
  readonly valor = input.required<string>();
  readonly tipo = input<'situacao' | 'status'>('status');

  protected readonly classe = computed(() => 'chip chip-' + this.valor().toLowerCase());
}
