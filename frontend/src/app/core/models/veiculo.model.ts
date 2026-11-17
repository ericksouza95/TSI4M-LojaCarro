export type Combustivel = 'GASOLINA' | 'ETANOL' | 'FLEX' | 'DIESEL' | 'ELETRICO' | 'HIBRIDO';
export type Transmissao = 'MANUAL' | 'AUTOMATICA' | 'AUTOMATIZADA' | 'CVT';
export type SituacaoVeiculo = 'DISPONIVEL' | 'VENDIDO' | 'INATIVO';

export const COMBUSTIVEIS: Record<Combustivel, string> = {
  GASOLINA: 'Gasolina',
  ETANOL: 'Etanol',
  FLEX: 'Flex',
  DIESEL: 'Diesel',
  ELETRICO: 'Elétrico',
  HIBRIDO: 'Híbrido',
};

export const TRANSMISSOES: Record<Transmissao, string> = {
  MANUAL: 'Manual',
  AUTOMATICA: 'Automática',
  AUTOMATIZADA: 'Automatizada',
  CVT: 'CVT',
};

export const SITUACOES_VEICULO: Record<SituacaoVeiculo, string> = {
  DISPONIVEL: 'Disponível',
  VENDIDO: 'Vendido',
  INATIVO: 'Inativo',
};

export interface Veiculo {
  id: number;
  marca: string;
  modelo: string;
  anoFabricacao: number;
  anoModelo: number;
  cor: string;
  quilometragem: number;
  combustivel: Combustivel;
  transmissao: Transmissao;
  preco: number;
  descricao: string | null;
  situacao: SituacaoVeiculo;
  fotoUrl: string | null;
  dataCadastro: string;
}

// Dados enviados no cadastro/edição (a foto vai separada, no multipart)
export interface VeiculoDados {
  marca: string;
  modelo: string;
  anoFabricacao: number;
  anoModelo: number;
  cor: string;
  quilometragem: number;
  combustivel: Combustivel;
  transmissao: Transmissao;
  preco: number;
  descricao: string | null;
}

export interface VeiculoFiltro {
  busca?: string | null;
  precoMin?: number | null;
  precoMax?: number | null;
  anoMin?: number | null;
  anoMax?: number | null;
  situacao?: SituacaoVeiculo[];
}
