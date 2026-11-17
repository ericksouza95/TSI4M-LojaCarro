import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { Agendamento } from '../../core/models/agendamento.model';
import { AgendamentoService } from '../../core/services/agendamento.service';
import { NotificacaoService } from '../../core/services/notificacao.service';
import { deDataIso } from '../../core/utils/datas';
import { CarregandoComponent } from '../../shared/components/carregando/carregando.component';
import { confirmar } from '../../shared/components/confirmacao-dialog/confirmacao-dialog.component';
import { EstadoVazioComponent } from '../../shared/components/estado-vazio/estado-vazio.component';
import { StatusChipComponent } from '../../shared/components/status-chip/status-chip.component';

@Component({
  selector: 'app-meus-agendamentos',
  imports: [RouterLink, DatePipe, NgTemplateOutlet, MatButtonModule, MatIconModule, CarregandoComponent, EstadoVazioComponent, StatusChipComponent],
  templateUrl: './meus-agendamentos.component.html',
  styleUrl: './meus-agendamentos.component.scss',
})
export class MeusAgendamentosComponent implements OnInit {
  private readonly agendamentoService = inject(AgendamentoService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly dialog = inject(MatDialog);

  protected readonly agendamentos = signal<Agendamento[]>([]);
  protected readonly carregando = signal(true);
  protected readonly cancelando = signal<number | null>(null);

  // Próximos: confirmados que ainda podem acontecer (do mais próximo para o mais distante)
  protected readonly proximos = computed(() =>
    this.agendamentos()
      .filter((a) => a.status === 'CONFIRMADO' && a.cancelavel)
      .sort((a, b) => (a.data + a.horarioInicio).localeCompare(b.data + b.horarioInicio)),
  );
  protected readonly historico = computed(() => this.agendamentos().filter((a) => !this.proximos().includes(a)));

  protected readonly paraData = deDataIso;

  ngOnInit(): void {
    this.carregar();
  }

  protected cancelar(agendamento: Agendamento): void {
    confirmar(this.dialog, {
      titulo: 'Cancelar test drive?',
      mensagem: `O horário de ${agendamento.horarioInicio} com o ${agendamento.veiculo} ficará livre para outros clientes.`,
      confirmar: 'Cancelar test drive',
      perigo: true,
    }).subscribe((confirmado) => {
      if (!confirmado) {
        return;
      }
      this.cancelando.set(agendamento.id);
      this.agendamentoService.cancelar(agendamento.id).subscribe({
        next: (atualizado) => {
          this.agendamentos.update((lista) => lista.map((a) => (a.id === atualizado.id ? atualizado : a)));
          this.cancelando.set(null);
          this.notificacao.sucesso('Test drive cancelado.');
        },
        error: (erro) => {
          this.cancelando.set(null);
          this.notificacao.erroHttp(erro, 'Não foi possível cancelar o test drive.');
        },
      });
    });
  }

  private carregar(): void {
    this.agendamentoService.meus().subscribe({
      next: (agendamentos) => {
        this.agendamentos.set(agendamentos);
        this.carregando.set(false);
      },
      error: (erro) => {
        this.carregando.set(false);
        this.notificacao.erroHttp(erro, 'Não foi possível carregar seus agendamentos.');
      },
    });
  }
}
