import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';

import { CarregamentoService } from '../services/carregamento.service';

// Liga a barra de progresso do topo enquanto houver requisições em andamento
export const carregamentoInterceptor: HttpInterceptorFn = (requisicao, proximo) => {
  const carregamento = inject(CarregamentoService);
  carregamento.iniciar();
  return proximo(requisicao).pipe(finalize(() => carregamento.finalizar()));
};
