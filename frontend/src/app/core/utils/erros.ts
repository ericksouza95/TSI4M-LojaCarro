import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl } from '@angular/forms';

import { ErroApi } from '../models/api.model';

// Converte um erro HTTP na mensagem que será mostrada ao usuário
export function mensagemDeErro(erro: unknown, padrao = 'Não foi possível concluir a operação.'): string {
  if (!(erro instanceof HttpErrorResponse)) {
    return padrao;
  }
  if (erro.status === 0) {
    return 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.';
  }
  const corpo = erro.error as ErroApi | null;
  if (corpo && typeof corpo === 'object' && corpo.erro) {
    return corpo.erro;
  }
  if (erro.status >= 500) {
    return 'Ocorreu um erro no servidor. Tente novamente em instantes.';
  }
  return padrao;
}

// Erros de validação do backend (400 com "campos") aparecem embaixo de cada campo do formulário
export function aplicarErrosDosCampos(formulario: AbstractControl, erro: unknown): boolean {
  if (!(erro instanceof HttpErrorResponse)) {
    return false;
  }
  const campos = (erro.error as ErroApi | null)?.campos;
  if (!campos) {
    return false;
  }
  let aplicou = false;
  for (const [campo, mensagem] of Object.entries(campos)) {
    const controle = formulario.get(campo);
    if (controle) {
      controle.setErrors({ servidor: mensagem });
      controle.markAsTouched();
      aplicou = true;
    }
  }
  return aplicou;
}
