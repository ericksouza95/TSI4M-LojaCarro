import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controle: HttpTestingController;
  let token: string | null;

  beforeEach(() => {
    token = 'abc.def.ghi';
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { token: () => token } },
      ],
    });
    http = TestBed.inject(HttpClient);
    controle = TestBed.inject(HttpTestingController);
  });

  it('adiciona o token JWT nas chamadas à API', () => {
    http.get('/api/agendamentos/meus').subscribe();

    const requisicao = controle.expectOne('/api/agendamentos/meus');
    expect(requisicao.request.headers.get('Authorization')).toBe('Bearer abc.def.ghi');
    requisicao.flush([]);
  });

  it('não envia cabeçalho quando não há usuário logado nem para outros domínios', () => {
    http.get('https://fonts.googleapis.com/css').subscribe();
    expect(controle.expectOne('https://fonts.googleapis.com/css').request.headers.has('Authorization')).toBe(false);

    token = null;
    http.get('/api/veiculos').subscribe();
    expect(controle.expectOne('/api/veiculos').request.headers.has('Authorization')).toBe(false);
  });
});
