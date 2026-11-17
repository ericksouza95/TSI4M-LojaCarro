import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API } from '../models/api.model';
import { HorarioDisponivel } from '../models/agendamento.model';
import { SituacaoVeiculo, Veiculo, VeiculoDados, VeiculoFiltro } from '../models/veiculo.model';

@Injectable({ providedIn: 'root' })
export class VeiculoService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API}/veiculos`;

  listar(filtro: VeiculoFiltro = {}): Observable<Veiculo[]> {
    let params = new HttpParams();
    for (const [chave, valor] of Object.entries(filtro)) {
      if (Array.isArray(valor)) {
        valor.forEach((item) => (params = params.append(chave, item)));
      } else if (valor !== null && valor !== undefined && valor !== '') {
        params = params.set(chave, String(valor));
      }
    }
    return this.http.get<Veiculo[]>(this.url, { params });
  }

  buscar(id: number): Observable<Veiculo> {
    return this.http.get<Veiculo>(`${this.url}/${id}`);
  }

  // multipart/form-data: parte "dados" (JSON) + parte "foto" (arquivo)
  cadastrar(dados: VeiculoDados, foto: File): Observable<Veiculo> {
    const formulario = new FormData();
    formulario.append('dados', new Blob([JSON.stringify(dados)], { type: 'application/json' }));
    formulario.append('foto', foto);
    return this.http.post<Veiculo>(this.url, formulario);
  }

  atualizar(id: number, dados: VeiculoDados): Observable<Veiculo> {
    return this.http.put<Veiculo>(`${this.url}/${id}`, dados);
  }

  trocarFoto(id: number, foto: File): Observable<Veiculo> {
    const formulario = new FormData();
    formulario.append('foto', foto);
    return this.http.put<Veiculo>(`${this.url}/${id}/foto`, formulario);
  }

  alterarSituacao(id: number, situacao: SituacaoVeiculo): Observable<Veiculo> {
    return this.http.patch<Veiculo>(`${this.url}/${id}/situacao`, { situacao });
  }

  horarios(id: number, data: string): Observable<HorarioDisponivel[]> {
    return this.http.get<HorarioDisponivel[]>(`${this.url}/${id}/horarios`, { params: { data } });
  }
}
