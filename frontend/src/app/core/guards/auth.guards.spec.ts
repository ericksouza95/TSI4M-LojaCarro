import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { NotificacaoService } from '../services/notificacao.service';
import { adminGuard, clienteGuard } from './auth.guards';

describe('Guards de rota', () => {
  let perfil: 'ADMIN' | 'CLIENTE' | null;

  beforeEach(() => {
    perfil = null;
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: NotificacaoService, useValue: { erro: vi.fn() } },
        {
          provide: AuthService,
          useValue: {
            token: () => (perfil ? 'token' : null),
            isAdmin: () => perfil === 'ADMIN',
            isCliente: () => perfil === 'CLIENTE',
          },
        },
      ],
    });
  });

  function executar(guard: typeof adminGuard, url: string) {
    return TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    );
  }

  function caminho(resultado: unknown): string {
    return TestBed.inject(Router).serializeUrl(resultado as UrlTree);
  }

  it('visitante é levado ao login, guardando a página que tentou abrir', () => {
    const resultado = executar(clienteGuard, '/veiculos/3/agendar');

    expect(resultado).toBeInstanceOf(UrlTree);
    expect(caminho(resultado)).toBe('/login?redirect=%2Fveiculos%2F3%2Fagendar');
  });

  it('cliente não acessa o painel administrativo', () => {
    perfil = 'CLIENTE';

    const resultado = executar(adminGuard, '/admin');

    expect(caminho(resultado)).toBe('/');
    expect(TestBed.inject(NotificacaoService).erro).toHaveBeenCalled();
  });

  it('administrador acessa o painel e cliente acessa o agendamento', () => {
    perfil = 'ADMIN';
    expect(executar(adminGuard, '/admin')).toBe(true);

    perfil = 'CLIENTE';
    expect(executar(clienteGuard, '/meus-agendamentos')).toBe(true);
  });
});
