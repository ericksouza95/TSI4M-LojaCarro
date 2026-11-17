import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Observable } from 'rxjs';

export interface DadosConfirmacao {
  titulo: string;
  mensagem: string;
  confirmar?: string;
  perigo?: boolean;
}

@Component({
  selector: 'app-confirmacao-dialog',
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ dados.titulo }}</h2>
    <mat-dialog-content>{{ dados.mensagem }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Voltar</button>
      <button mat-flat-button [class.botao-perigo]="dados.perigo" [mat-dialog-close]="true" cdkFocusInitial>
        {{ dados.confirmar ?? 'Confirmar' }}
      </button>
    </mat-dialog-actions>
  `,
})
export class ConfirmacaoDialogComponent {
  protected readonly dados = inject<DadosConfirmacao>(MAT_DIALOG_DATA);
}

// Abre o diálogo e devolve true se o usuário confirmou
export function confirmar(dialog: MatDialog, dados: DadosConfirmacao): Observable<boolean | undefined> {
  return dialog.open(ConfirmacaoDialogComponent, { data: dados, width: '420px', maxWidth: '92vw' }).afterClosed();
}
