import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { Agendamento } from '../../core/models/agendamento.model';
import { AgendamentoService } from '../../core/services/agendamento.service';
import { NotificacaoService } from '../../core/services/notificacao.service';
import { MeusAgendamentosComponent } from './meus-agendamentos.component';

registerLocaleData(localePt);

function agendamento(id: number, data: string, status: Agendamento['status'], cancelavel: boolean): Agendamento {
  return {
    id, data, status, cancelavel, veiculoId: 1, veiculo: 'Toyota Corolla 2023/2024', veiculoFotoUrl: null,
    clienteId: 1, clienteNome: 'Ana', clienteEmail: 'ana@email.com', clienteTelefone: null,
    horarioInicio: '14:00', horarioFim: '14:30', dataCriacao: '2030-03-01T10:00:00', dataCancelamento: null,
  };
}

describe('MeusAgendamentosComponent', () => {
  let fixture: ComponentFixture<MeusAgendamentosComponent>;
  let tela: HTMLElement;
  let cancelar: ReturnType<typeof vi.fn>;
  let confirmou: boolean;

  beforeEach(async () => {
    confirmou = true;
    cancelar = vi.fn().mockImplementation((id: number) => of({ ...agendamento(id, '2030-03-10', 'CANCELADO', false) }));
    TestBed.configureTestingModule({
      imports: [MeusAgendamentosComponent],
      providers: [
        provideRouter([]),
        { provide: LOCALE_ID, useValue: 'pt-BR' },
        {
          provide: AgendamentoService,
          useValue: {
            meus: () => of([
              agendamento(1, '2030-03-10', 'CONFIRMADO', true),
              agendamento(2, '2030-03-08', 'CONFIRMADO', true),
              agendamento(3, '2030-02-01', 'REALIZADO', false),
            ]),
            cancelar,
          },
        },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(confirmou) }) } },
        { provide: NotificacaoService, useValue: { sucesso: vi.fn(), erroHttp: vi.fn() } },
      ],
    });
    fixture = TestBed.createComponent(MeusAgendamentosComponent);
    tela = fixture.nativeElement;
    await fixture.whenStable();
  });

  function botoesCancelar(): HTMLButtonElement[] {
    return Array.from(tela.querySelectorAll('button')).filter((b) => b.textContent?.trim() === 'Cancelar');
  }

  it('separa os próximos test drives (do mais próximo para o mais distante) do histórico', () => {
    const listas = tela.querySelectorAll('.lista');
    const proximos = listas[0].querySelectorAll('article');
    const historico = listas[1].querySelectorAll('article');

    expect(proximos.length).toBe(2);
    expect(proximos[0].textContent).toContain('08');
    expect(historico.length).toBe(1);
    expect(historico[0].textContent).toContain('Realizado');
    expect(botoesCancelar().length).toBe(2);
  });

  it('cancela após a confirmação e move o agendamento para o histórico', async () => {
    botoesCancelar()[0].click();
    await fixture.whenStable();

    expect(cancelar).toHaveBeenCalledWith(2);
    expect(TestBed.inject(NotificacaoService).sucesso).toHaveBeenCalledWith('Test drive cancelado.');
    expect(tela.querySelectorAll('.lista')[0].querySelectorAll('article').length).toBe(1);
  });

  it('não cancela quando o cliente desiste no diálogo', async () => {
    confirmou = false;

    botoesCancelar()[0].click();
    await fixture.whenStable();

    expect(cancelar).not.toHaveBeenCalled();
  });
});
