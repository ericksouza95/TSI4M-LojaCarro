import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { NotificacaoService } from '../services/notificacao.service';

// Proteção das rotas conforme o perfil. O backend também valida tudo: os guards só melhoram a navegação.

function irParaLogin(urlDesejada: string) {
  return inject(Router).createUrlTree(['/login'], { queryParams: { redirect: urlDesejada } });
}

// Qualquer usuário logado
export const autenticadoGuard: CanActivateFn = (_rota, estado) => {
  return inject(AuthService).token() ? true : irParaLogin(estado.url);
};

// Somente administrador
export const adminGuard: CanActivateFn = (_rota, estado) => {
  const auth = inject(AuthService);
  if (!auth.token()) {
    return irParaLogin(estado.url);
  }
  if (auth.isAdmin()) {
    return true;
  }
  inject(NotificacaoService).erro('Área restrita aos administradores.');
  return inject(Router).createUrlTree(['/']);
};

// Somente cliente (agendar e consultar test drives)
export const clienteGuard: CanActivateFn = (_rota, estado) => {
  const auth = inject(AuthService);
  if (!auth.token()) {
    return irParaLogin(estado.url);
  }
  if (auth.isCliente()) {
    return true;
  }
  inject(NotificacaoService).erro('Test drives são agendados pela conta de um cliente.');
  return inject(Router).createUrlTree(['/admin/agendamentos']);
};

// Login e cadastro só fazem sentido para quem ainda não entrou
export const visitanteGuard: CanActivateFn = () => {
  return inject(AuthService).token() ? inject(Router).createUrlTree(['/']) : true;
};
