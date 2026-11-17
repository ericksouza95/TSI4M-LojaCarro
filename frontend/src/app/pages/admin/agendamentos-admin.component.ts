import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { Agendamento, STATUS_AGENDAMENTO, StatusAgendamento } from '../../core/models/agendamento.model';
import { Veiculo } from '../../core/models/veiculo.model';
import { AgendamentoService } from '../../core/services/agendamento.service';
import { NotificacaoService } from '../../core/services/notificacao.service';
import { VeiculoService } from '../../core/services/veiculo.service';
import { deDataIso, paraDataIso } from '../../core/utils/datas';
import { CarregandoComponent } from '../../shared/components/carregando/carregando.component';
import { confirmar } from '../../shared/components/confirmacao-dialog/confirmacao-dialog.component';
import { EstadoVazioComponent } from '../../shared/components/estado-vazio/estado-vazio.component';
import { StatusChipComponent } from '../../shared/components/status-chip/status-chip.component';

@Component({
  selector: 'app-agendamentos-admin',
  imports: [
    FormsModule, DatePipe, MatTableModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatDatepickerModule, MatTooltipModule, CarregandoComponent, EstadoVazioComponent, StatusChipComponent,
  ],
  templateUrl: './agendamentos-admin.component.html',
  styleUrl: './admin.scss',
})
export class AgendamentosAdminComponent implements OnInit {
  private readonly agendamentoService = inject(AgendamentoService);
  private readonly veiculoService = inject(VeiculoService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly dialog = inject(MatDialog);

  protected readonly colunas = ['data', 'cliente', 'veiculo', 'status', 'acoes'];
  protected readonly statusOpcoes = Object.entries(STATUS_AGENDAMENTO) as [StatusAgendamento, string][];
  protected readonly paraData = deDataIso;

  protected readonly agendamentos = signal<Agendamento[]>([]);
  protected readonly veiculos = signal<Veiculo[]>([]);
  protected readonly carregando = signal(true);

  // Filtros (RF05: consulta por data e por veículo)
  protected readonly data = signal<Date | null>(null);
  protected readonly veiculoId = signal<number | null>(null);
  protected readonly status = signal<StatusAgendamento | null>(null);

  ngOnInit(): void {
    this.veiculoService.listar({ situacao: ['DISPONIVEL', 'VENDIDO', 'INATIVO'] }).subscribe({
      next: (veiculos) => this.veiculos.set(veiculos),
    });
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    const data = this.data();
    this.agendamentoService
      .listar({ data: data ? paraDataIso(data) : null, veiculoId: this.veiculoId(), status: this.status() })
      .subscribe({
        next: (agendamentos) => {
          this.agendamentos.set(agendamentos);
          this.carregando.set(false);
        },
        error: (erro) => {
          this.carregando.set(false);
          this.notificacao.erroHttp(erro, 'Não foi possível carregar os agendamentos.');
        },
      });
  }

  protected limparFiltros(): void {
    this.data.set(null);
    this.veiculoId.set(null);
    this.status.set(null);
    this.carregar();
  }

  // Test drive já começou e ainda está confirmado: pode ser registrado como realizado
  protected podeMarcarRealizado(a: Agendamento): boolean {
    return a.status === 'CONFIRMADO' && !a.cancelavel;
  }

  protected cancelar(a: Agendamento): void {
    confirmar(this.dialog, {
      titulo: 'Cancelar este test drive?',
      mensagem: `${a.clienteNome} — ${a.veiculo}, ${a.horarioInicio}. O horário ficará livre novamente.`,
      confirmar: 'Cancelar test drive',
      perigo: true,
    }).subscribe((ok) => ok && this.executar(this.agendamentoService.cancelar(a.id), 'Test drive cancelado.'));
  }

  protected marcarRealizado(a: Agendamento): void {
    this.executar(this.agendamentoService.marcarComoRealizado(a.id), 'Test drive marcado como realizado.');
  }

  private executar(requisicao: ReturnType<AgendamentoService['cancelar']>, mensagem: string): void {
    requisicao.subscribe({
      next: (atualizado) => {
        this.agendamentos.update((lista) => lista.map((a) => (a.id === atualizado.id ? atualizado : a)));
        this.notificacao.sucesso(mensagem);
      },
      error: (erro) => this.notificacao.erroHttp(erro),
    });
  }
}
