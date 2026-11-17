export type StatusAgendamento = 'CONFIRMADO' | 'CANCELADO' | 'REALIZADO';

export const STATUS_AGENDAMENTO: Record<StatusAgendamento, string> = {
  CONFIRMADO: 'Confirmado',
  CANCELADO: 'Cancelado',
  REALIZADO: 'Realizado',
};

export interface Agendamento {
  id: number;
  veiculoId: number;
  veiculo: string;
  veiculoFotoUrl: string | null;
  clienteId: number;
  clienteNome: string;
  clienteEmail: string;
  clienteTelefone: string | null;
  data: string;          // AAAA-MM-DD
  horarioInicio: string; // HH:mm
  horarioFim: string;    // HH:mm
  status: StatusAgendamento;
  cancelavel: boolean;
  dataCriacao: string;
  dataCancelamento: string | null;
}

export interface AgendamentoRequest {
  veiculoId: number;
  data: string;
  horario: string;
}

export interface HorarioDisponivel {
  inicio: string;
  fim: string;
  disponivel: boolean;
}

export interface AgendamentoFiltro {
  data?: string | null;
  veiculoId?: number | null;
  status?: StatusAgendamento | null;
}
