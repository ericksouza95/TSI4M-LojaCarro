import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import { mensagemDeErro } from '../utils/erros';

// Mensagens de sucesso e erro exibidas no rodapé da tela
@Injectable({ providedIn: 'root' })
export class NotificacaoService {
  private readonly snackBar = inject(MatSnackBar);

  sucesso(mensagem: string): void {
    this.snackBar.open(mensagem, 'OK', { duration: 4000, panelClass: 'aviso-sucesso' });
  }

  erro(mensagem: string): void {
    this.snackBar.open(mensagem, 'Fechar', { duration: 7000, panelClass: 'aviso-erro' });
  }

  // Mostra a mensagem que veio da API (ou uma mensagem padrão)
  erroHttp(erro: unknown, padrao?: string): void {
    this.erro(mensagemDeErro(erro, padrao));
  }
}
