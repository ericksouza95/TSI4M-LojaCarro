import { registerLocaleData } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { HorarioDisponivel } from '../../core/models/agendamento.model';
import { Veiculo } from '../../core/models/veiculo.model';
import { AgendamentoService } from '../../core/services/agendamento.service';
import { NotificacaoService } from '../../core/services/notificacao.service';
import { VeiculoService } from '../../core/services/veiculo.service';
import { AgendarComponent } from './agendar.component';

registerLocaleData(localePt);

const corolla = {
  id: 3, marca: 'Toyota', modelo: 'Corolla', anoFabricacao: 2023, anoModelo: 2024, preco: 135000,
  situacao: 'DISPONIVEL', fotoUrl: null,
} as Veiculo;

const grade: HorarioDisponivel[] = [
  { inicio: '08:00', fim: '08:30', disponivel: true },
  { inicio: '08:30', fim: '09:00', disponivel: false },
  { inicio: '09:00', fim: '09:30', disponivel: true },
];

describe('AgendarComponent', () => {
  let fixture: ComponentFixture<AgendarComponent>;
  let tela: HTMLElement;
  let horarios: ReturnType<typeof vi.fn>;
  let agendar: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    horarios = vi.fn().mockReturnValue(of(grade));
    agendar = vi.fn();
    TestBed.configureTestingModule({
      imports: [AgendarComponent],
      providers: [
        provideRouter([]),
        provideNativeDateAdapter(),
        { provide: LOCALE_ID, useValue: 'pt-BR' },
        { provide: VeiculoService, useValue: { buscar: vi.fn().mockReturnValue(of(corolla)), horarios } },
        { provide: AgendamentoService, useValue: { agendar } },
        { provide: NotificacaoService, useValue: { sucesso: vi.fn(), erroHttp: vi.fn() } },
      ],
    });
    fixture = TestBed.createComponent(AgendarComponent);
    fixture.componentRef.setInput('id', '3');
    tela = fixture.nativeElement;
    await fixture.whenStable();
  });

  function botoesDeHorario(): HTMLButtonElement[] {
    return Array.from(tela.querySelectorAll('button.horario'));
  }

  function botaoConfirmar(): HTMLButtonElement {
    return Array.from(tela.querySelectorAll('button')).find((b) => b.textContent?.includes('Confirmar agendamento'))!;
  }

  it('mostra a grade do dia e bloqueia os horários ocupados', () => {
    const botoes = botoesDeHorario();

    expect(horarios).toHaveBeenCalledWith(3, expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/));
    expect(botoes.map((b) => b.textContent!.trim())).toEqual(['08:00', '08:30', '09:00']);
    expect(botoes[1].disabled).toBe(true);
    expect(botoes[0].disabled).toBe(false);
  });

  it('só permite confirmar depois de escolher um horário e envia veículo, data e horário', async () => {
    agendar.mockReturnValue(of({ id: 10, data: '2030-03-05', horarioInicio: '09:00' }));
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    expect(botaoConfirmar().disabled).toBe(true);

    botoesDeHorario()[2].click();
    await fixture.whenStable();
    expect(botaoConfirmar().disabled).toBe(false);
    expect(tela.textContent).toContain('das 09:00 às 09:30');

    botaoConfirmar().click();
    await fixture.whenStable();

    expect(agendar).toHaveBeenCalledWith({ veiculoId: 3, data: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/), horario: '09:00' });
    expect(TestBed.inject(Router).navigateByUrl).toHaveBeenCalledWith('/meus-agendamentos');
  });

  it('atualiza a grade quando o horário foi ocupado por outra pessoa (409)', async () => {
    agendar.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409, error: { erro: 'Horário ocupado' } })));
    botoesDeHorario()[0].click();
    await fixture.whenStable();

    botaoConfirmar().click();
    await fixture.whenStable();

    expect(TestBed.inject(NotificacaoService).erroHttp).toHaveBeenCalled();
    expect(horarios).toHaveBeenCalledTimes(2);
  });
});
