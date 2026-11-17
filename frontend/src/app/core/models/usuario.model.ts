export type Perfil = 'ADMIN' | 'CLIENTE';

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  telefone: string | null;
  perfil: Perfil;
  dataCadastro: string;
}

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface LoginResponse {
  token: string;
  tipo: string;
  expiraEm: string;
  usuario: Usuario;
}

export interface CadastroUsuarioRequest {
  nome: string;
  email: string;
  senha: string;
  telefone?: string | null;
}
