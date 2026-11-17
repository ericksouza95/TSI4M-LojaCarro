import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup } from '@angular/forms';

import { aplicarErrosDosCampos, mensagemDeErro } from './erros';

describe('Tratamento de erros da API', () => {
  it('usa a mensagem enviada pelo backend', () => {
    const erro = new HttpErrorResponse({ status: 409, error: { status: 409, erro: 'Este veículo já possui um test drive agendado.' } });

    expect(mensagemDeErro(erro)).toBe('Este veículo já possui um test drive agendado.');
  });

  it('explica quando o servidor está fora do ar ou falhou', () => {
    expect(mensagemDeErro(new HttpErrorResponse({ status: 0 }))).toContain('Não foi possível conectar ao servidor');
    expect(mensagemDeErro(new HttpErrorResponse({ status: 500, error: 'texto' }))).toContain('erro no servidor');
    expect(mensagemDeErro(new Error('qualquer'), 'Mensagem padrão')).toBe('Mensagem padrão');
  });

  it('mostra os erros de validação do backend embaixo de cada campo', () => {
    const formulario = new FormGroup({ email: new FormControl('ana@email.com'), nome: new FormControl('Ana') });
    const erro = new HttpErrorResponse({
      status: 400,
      error: { status: 400, erro: 'Dados inválidos.', campos: { email: 'E-mail inválido.', inexistente: 'x' } },
    });

    expect(aplicarErrosDosCampos(formulario, erro)).toBe(true);
    expect(formulario.controls.email.getError('servidor')).toBe('E-mail inválido.');
    expect(formulario.controls.nome.valid).toBe(true);
    expect(aplicarErrosDosCampos(formulario, new HttpErrorResponse({ status: 422, error: { erro: 'Regra' } }))).toBe(false);
  });
});
