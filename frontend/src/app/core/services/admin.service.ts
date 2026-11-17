import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API, LogSistema } from '../models/api.model';
import { Usuario } from '../models/usuario.model';

// Consultas exclusivas do administrador: usuários/clientes e logs de auditoria
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  usuarios(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${API}/usuarios`);
  }

  logs(): Observable<LogSistema[]> {
    return this.http.get<LogSistema[]>(`${API}/logs`);
  }
}
