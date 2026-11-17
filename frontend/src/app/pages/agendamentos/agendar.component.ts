import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';

import { HorarioDisponivel } from '../../core/models/agendamento.model';
import { Veiculo } from '../../core/models/veiculo.model';
import { AgendamentoService } from '../../core/services/agendamento.service';
import { NotificacaoService } from '../../core/services/notificacao.service';
import { VeiculoService } from '../../core/services/veiculo.service';
import { hojeSemHorario, paraDataIso, proximoDiaUtil } from '../../core/utils/datas';
import { mensagemDeErro } from '../../core/utils/erros';
import { CarregandoComponent } from '../../shared/components/carregando/carregando.component';
import { EstadoVazioComponent } from '../../shared/components/estado-vazio/estado-vazio.component';

// Agendamento de test drive: o cliente escolhe a data no calendário e um horário livre na grade do dia.
// A disponibilidade mostrada aqui é só uma ajuda: quem garante que não haja conflito é o backend.
@Component({
  selector: 'app-agendar',
  imports: [
    FormsModule, RouterLink, CurrencyPipe, DatePipe, MatButtonModule, MatIconModule, MatFormFieldModule,
    MatInputModule, MatDatepickerModule, MatProgressSpinnerModule, CarregandoComponent, EstadoVazioComponent,
  ],
  templateUrl: './agendar.component.html',
  styleUrl: './agendar.component.scss',
})
export class AgendarComponent {
  private readonly veiculoService = inject(VeiculoService);
  private readonly agendamentoService = inject(AgendamentoService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly router = inject(Router);

  readonly id = input.required<string>();

  protected readonly veiculo = signal<Veiculo | null>(null);
  protected readonly erroVeiculo = signal<string | null>(null);
  protected readonly data = signal<Date>(proximoDiaUtil());
  protected readonly horarios = signal<HorarioDisponivel[]>([]);
  protected readonly carregandoHorarios = signal(false);
  protected readonly horarioEscolhido = signal<string | null>(null);
  protected readonly enviando = signal(false);

  protected readonly hoje = hojeSemHorario();
  protected readonly algumLivre = computed(() => this.horarios().some((h) => h.disponivel));
  protected readonly fimEscolhido = computed(
    () => this.horarios().find((h) => h.inicio === this.horarioEscolhido())?.fim ?? '',
  );

  // Domingo a loja não abre
  protected readonly diasPermitidos = (data: Date | null): boolean => !!data && data.getDay() !== 0;

  constructor() {
    effect(() => {
      const id = Number(this.id());
      untracked(() => this.carregarVeiculo(id));
    });
    // Recarrega a grade sempre que a data mudar
    effect(() => {
      const data = this.data();
      const veiculo = this.veiculo();
      if (veiculo) {
        untracked(() => this.carregarHorarios(veiculo.id, data));
      }
    });
  }

  protected escolherData(data: Date | null): void {
    if (data) {
      this.data.set(data);
    }
  }

  protected escolherHorario(horario: HorarioDisponivel): void {
    if (horario.disponivel) {
      this.horarioEscolhido.set(horario.inicio);
    }
  }

  protected confirmar(): void {
    const veiculo = this.veiculo();
    const horario = this.horarioEscolhido();
    if (!veiculo || !horario) {
      return;
    }
    this.enviando.set(true);
    this.agendamentoService
      .agendar({ veiculoId: veiculo.id, data: paraDataIso(this.data()), horario })
      .subscribe({
        next: (agendamento) => {
          const [ano, mes, dia] = agendamento.data.split('-');
          this.notificacao.sucesso(`Test drive confirmado para ${dia}/${mes}/${ano} às ${agendamento.horarioInicio}.`);
          this.router.navigateByUrl('/meus-agendamentos');
        },
        error: (erro: unknown) => {
          this.enviando.set(false);
          this.notificacao.erroHttp(erro, 'Não foi possível agendar o test drive.');
          // Horário ocupado por outra pessoa enquanto o cliente escolhia: atualiza a grade
          if (erro instanceof HttpErrorResponse && erro.status === 409) {
            this.carregarHorarios(veiculo.id, this.data());
          }
        },
      });
  }

  private carregarVeiculo(id: number): void {
    this.veiculoService.buscar(id).subscribe({
      next: (veiculo) => {
        if (veiculo.situacao !== 'DISPONIVEL') {
          this.erroVeiculo.set('Este veículo não está disponível para test drive.');
        } else {
          this.veiculo.set(veiculo);
        }
      },
      error: (erro) => this.erroVeiculo.set(mensagemDeErro(erro, 'Veículo não encontrado.')),
    });
  }

  private carregarHorarios(veiculoId: number, data: Date): void {
    this.carregandoHorarios.set(true);
    this.horarioEscolhido.set(null);
    this.veiculoService.horarios(veiculoId, paraDataIso(data)).subscribe({
      next: (horarios) => {
        this.horarios.set(horarios);
        this.carregandoHorarios.set(false);
      },
      error: (erro) => {
        this.horarios.set([]);
        this.carregandoHorarios.set(false);
        this.notificacao.erroHttp(erro, 'Não foi possível carregar os horários.');
      },
    });
  }
}
