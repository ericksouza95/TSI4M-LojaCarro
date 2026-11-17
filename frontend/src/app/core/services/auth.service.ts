import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';

import { API } from '../models/api.model';
import { CadastroUsuarioRequest, LoginRequest, LoginResponse, Usuario } from '../models/usuario.model';

interface Sessao {
  token: string;
  expiraEm: string;
  usuario: Usuario;
}

const CHAVE_SESSAO = 'lojacarro.sessao';

// Guarda o token JWT e o usuário logado. O estado fica em signals para a interface reagir ao login/logout.
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly sessao = signal<Sessao | null>(this.lerSessaoSalva());

  readonly usuario = computed(() => this.sessao()?.usuario ?? null);
  readonly logado = computed(() => this.sessao() !== null);
  readonly isAdmin = computed(() => this.usuario()?.perfil === 'ADMIN');
  readonly isCliente = computed(() => this.usuario()?.perfil === 'CLIENTE');

  login(dados: LoginRequest): Observable<Usuario> {
    return this.http.post<LoginResponse>(`${API}/auth/login`, dados).pipe(
      tap((resposta) => this.salvar({ token: resposta.token, expiraEm: resposta.expiraEm, usuario: resposta.usuario })),
      map((resposta) => resposta.usuario),
    );
  }

  cadastrar(dados: CadastroUsuarioRequest): Observable<Usuario> {
    return this.http.post<Usuario>(`${API}/usuarios`, dados);
  }

  // Avisa o backend (para a auditoria) e descarta o token localmente
  logout(): void {
    if (this.token()) {
      this.http.post(`${API}/auth/logout`, {}).subscribe({ error: () => undefined });
    }
    this.encerrarSessao();
    this.router.navigateByUrl('/');
  }

  encerrarSessao(): void {
    this.sessao.set(null);
    localStorage.removeItem(CHAVE_SESSAO);
  }

  // Token válido ou null (um token expirado é descartado)
  token(): string | null {
    const sessao = this.sessao();
    if (!sessao) {
      return null;
    }
    if (new Date(sessao.expiraEm).getTime() <= Date.now()) {
      this.encerrarSessao();
      return null;
    }
    return sessao.token;
  }

  private salvar(sessao: Sessao): void {
    this.sessao.set(sessao);
    localStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessao));
  }

  private lerSessaoSalva(): Sessao | null {
    try {
      const salva = localStorage.getItem(CHAVE_SESSAO);
      if (!salva) {
        return null;
      }
      const sessao = JSON.parse(salva) as Sessao;
      return new Date(sessao.expiraEm).getTime() > Date.now() ? sessao : null;
    } catch {
      return null;
    }
  }
}
