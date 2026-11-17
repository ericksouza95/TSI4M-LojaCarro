import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';
import { NotificacaoService } from '../services/notificacao.service';

// Se uma requisição autenticada voltar 401, o token deixou de valer (expirou ou o usuário foi removido):
// encerra a sessão e leva para o login, voltando depois para a página em que o usuário estava.
export const sessaoExpiradaInterceptor: HttpInterceptorFn = (requisicao, proximo) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const notificacao = inject(NotificacaoService);

  return proximo(requisicao).pipe(
    catchError((erro: unknown) => {
      const enviouToken = requisicao.headers.has('Authorization');
      if (erro instanceof HttpErrorResponse && erro.status === 401 && enviouToken) {
        auth.encerrarSessao();
        notificacao.erro('Sua sessão expirou. Entre novamente.');
        router.navigate(['/login'], { queryParams: { redirect: router.url } });
      }
      return throwError(() => erro);
    }),
  );
};
