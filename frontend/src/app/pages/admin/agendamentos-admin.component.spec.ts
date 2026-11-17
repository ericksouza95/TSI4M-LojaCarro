import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';

import { Agendamento } from '../../core/models/agendamento.model';
import { AgendamentoService } from '../../core/services/agendamento.service';
import { NotificacaoService } from '../../core/services/notificacao.service';
import { VeiculoService } from '../../core/services/veiculo.service';
import { AgendamentosAdminComponent } from './agendamentos-admin.component';

registerLocaleData(localePt);

const futuro: Agendamento = {
  id: 1, veiculoId: 3, veiculo: 'Toyota Corolla 2023/2024', veiculoFotoUrl: null, clienteId: 5, clienteNome: 'Ana Souza',
  clienteEmail: 'ana@email.com', clienteTelefone: '(84) 99999-0000', data: '2030-03-05', horarioInicio: '14:00',
  horarioFim: '14:30', status: 'CONFIRMADO', cancelavel: true, dataCriacao: '', dataCancelamento: null,
};
const jaComecou: Agendamento = { ...futuro, id: 2, clienteNome: 'Bruno', cancelavel: false };

describe('AgendamentosAdminComponent', () => {
  let fixture: ComponentFixture<AgendamentosAdminComponent>;
  let tela: HTMLElement;
  let listar: ReturnType<typeof vi.fn>;
  let marcarComoRealizado: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    listar = vi.fn().mockReturnValue(of([futuro, jaComecou]));
    marcarComoRealizado = vi.fn().mockReturnValue(of({ ...jaComecou, status: 'REALIZADO' }));
    TestBed.configureTestingModule({
      imports: [AgendamentosAdminComponent],
      providers: [
        provideNativeDateAdapter(),
        { provide: LOCALE_ID, useValue: 'pt-BR' },
        { provide: AgendamentoService, useValue: { listar, marcarComoRealizado, cancelar: vi.fn() } },
        { provide: VeiculoService, useValue: { listar: () => of([]) } },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(false) }) } },
        { provide: NotificacaoService, useValue: { sucesso: vi.fn(), erroHttp: vi.fn() } },
      ],
    });
    fixture = TestBed.createComponent(AgendamentosAdminComponent);
    tela = fixture.nativeElement;
    await fixture.whenStable();
  });

  it('mostra nome e contato do cliente, veículo, data, horário e situação', () => {
    const linhas = tela.querySelectorAll('tr.mat-mdc-row');

    expect(linhas.length).toBe(2);
    expect(linhas[0].textContent).toContain('Ana Souza');
    expect(linhas[0].textContent).toContain('(84) 99999-0000');
    expect(linhas[0].textContent).toContain('Toyota Corolla 2023/2024');
    expect(linhas[0].textContent).toContain('05/03/2030');
    expect(linhas[0].textContent).toContain('14:00 – 14:30');
    expect(linhas[0].textContent).toContain('Confirmado');
  });

  it('envia os filtros de data, veículo e situação para a API', async () => {
    const componente = fixture.componentInstance as unknown as {
      data: { set(d: Date): void }; veiculoId: { set(n: number): void }; status: { set(s: string): void }; carregar(): void;
    };
    componente.data.set(new Date(2030, 2, 5));
    componente.veiculoId.set(3);
    componente.status.set('CONFIRMADO');

    componente.carregar();
    await fixture.whenStable();

    expect(listar).toHaveBeenLastCalledWith({ data: '2030-03-05', veiculoId: 3, status: 'CONFIRMADO' });
  });

  it('só oferece "realizado" para test drive que já começou e atualiza a linha', async () => {
    const botoesRealizado = tela.querySelectorAll('button[mattooltip="Marcar como realizado"]');
    expect(botoesRealizado.length).toBe(1);

    (botoesRealizado[0] as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(marcarComoRealizado).toHaveBeenCalledWith(2);
    expect(tela.querySelectorAll('tr.mat-mdc-row')[1].textContent).toContain('Realizado');
  });
});
