import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API } from '../models/api.model';
import { Agendamento, AgendamentoFiltro, AgendamentoRequest } from '../models/agendamento.model';

@Injectable({ providedIn: 'root' })
export class AgendamentoService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API}/agendamentos`;

  agendar(dados: AgendamentoRequest): Observable<Agendamento> {
    return this.http.post<Agendamento>(this.url, dados);
  }

  meus(): Observable<Agendamento[]> {
    return this.http.get<Agendamento[]>(`${this.url}/meus`);
  }

  // Consulta do administrador
  listar(filtro: AgendamentoFiltro = {}): Observable<Agendamento[]> {
    let params = new HttpParams();
    for (const [chave, valor] of Object.entries(filtro)) {
      if (valor !== null && valor !== undefined && valor !== '') {
        params = params.set(chave, String(valor));
      }
    }
    return this.http.get<Agendamento[]>(this.url, { params });
  }

  cancelar(id: number): Observable<Agendamento> {
    return this.http.patch<Agendamento>(`${this.url}/${id}/cancelar`, {});
  }

  marcarComoRealizado(id: number): Observable<Agendamento> {
    return this.http.patch<Agendamento>(`${this.url}/${id}/realizado`, {});
  }
}
