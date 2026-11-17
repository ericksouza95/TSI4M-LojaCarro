import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { LoginResponse } from '../models/usuario.model';
import { AuthService } from './auth.service';

function resposta(expiraEm: Date): LoginResponse {
  return {
    token: 'token-jwt',
    tipo: 'Bearer',
    expiraEm: expiraEm.toISOString(),
    usuario: { id: 1, nome: 'Ana Souza', email: 'ana@email.com', telefone: null, perfil: 'CLIENTE', dataCadastro: '' },
  };
}

describe('AuthService', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  it('guarda o token e o perfil depois do login', () => {
    const auth = TestBed.inject(AuthService);

    auth.login({ email: 'ana@email.com', senha: '123456' }).subscribe();
    http.expectOne('/api/auth/login').flush(resposta(new Date(Date.now() + 3600_000)));

    expect(auth.logado()).toBe(true);
    expect(auth.isCliente()).toBe(true);
    expect(auth.isAdmin()).toBe(false);
    expect(auth.token()).toBe('token-jwt');
    expect(localStorage.getItem('lojacarro.sessao')).toContain('token-jwt');
  });

  it('descarta a sessão salva quando o token já expirou', () => {
    localStorage.setItem('lojacarro.sessao', JSON.stringify(resposta(new Date(Date.now() - 1000))));

    const auth = TestBed.inject(AuthService);

    expect(auth.logado()).toBe(false);
    expect(auth.token()).toBeNull();
  });
});
