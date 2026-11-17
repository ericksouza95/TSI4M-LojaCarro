import { Pipe, PipeTransform } from '@angular/core';

import { STATUS_AGENDAMENTO } from '../../core/models/agendamento.model';
import { COMBUSTIVEIS, SITUACOES_VEICULO, TRANSMISSOES } from '../../core/models/veiculo.model';

const ROTULOS: Record<string, Record<string, string>> = {
  combustivel: COMBUSTIVEIS,
  transmissao: TRANSMISSOES,
  situacao: SITUACOES_VEICULO,
  status: STATUS_AGENDAMENTO,
  perfil: { ADMIN: 'Administrador', CLIENTE: 'Cliente' },
};

// Converte o valor do enum da API em texto legível. Ex.: {{ 'AUTOMATICA' | rotulo:'transmissao' }} -> "Automática"
@Pipe({ name: 'rotulo' })
export class RotuloPipe implements PipeTransform {
  transform(valor: string | null | undefined, tipo: keyof typeof ROTULOS): string {
    if (!valor) {
      return '';
    }
    return ROTULOS[tipo]?.[valor] ?? valor;
  }
}
