// Todas as chamadas usam caminho relativo: em desenvolvimento o proxy do "ng serve" encaminha /api para o backend
export const API = '/api';

// Formato padrão de erro devolvido pelo backend (ErroResponse)
export interface ErroApi {
  status: number;
  erro: string;
  campos?: Record<string, string>;
  caminho?: string;
  dataHora?: string;
}

export interface LogSistema {
  id: number;
  dataHora: string;
  acao: string;
  entidade: string;
  descricao: string;
  usuario: string;
}
