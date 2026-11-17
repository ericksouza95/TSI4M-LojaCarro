import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { API } from '../models/api.model';
import { AuthService } from '../services/auth.service';

// Envia o token JWT no cabeçalho Authorization de todas as chamadas à API
export const authInterceptor: HttpInterceptorFn = (requisicao, proximo) => {
  const token = inject(AuthService).token();
  if (!token || !requisicao.url.startsWith(API)) {
    return proximo(requisicao);
  }
  return proximo(requisicao.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
