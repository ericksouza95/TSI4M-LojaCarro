import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Agendamento } from '../../core/models/agendamento.model';
import { Usuario } from '../../core/models/usuario.model';
import { Veiculo } from '../../core/models/veiculo.model';
import { AdminService } from '../../core/services/admin.service';
import { AgendamentoService } from '../../core/services/agendamento.service';
import { AuthService } from '../../core/services/auth.service';
import { NotificacaoService } from '../../core/services/notificacao.service';
import { VeiculoService } from '../../core/services/veiculo.service';
import { deDataIso, paraDataIso } from '../../core/utils/datas';
import { CarregandoComponent } from '../../shared/components/carregando/carregando.component';
import { EstadoVazioComponent } from '../../shared/components/estado-vazio/estado-vazio.component';

@Component({
  selector: 'app-painel',
  imports: [RouterLink, DatePipe, MatButtonModule, MatIconModule, CarregandoComponent, EstadoVazioComponent],
  templateUrl: './painel.component.html',
  styleUrl: './painel.component.scss',
})
export class PainelComponent implements OnInit {
  private readonly veiculoService = inject(VeiculoService);
  private readonly agendamentoService = inject(AgendamentoService);
  private readonly adminService = inject(AdminService);
  private readonly notificacao = inject(NotificacaoService);
  protected readonly auth = inject(AuthService);

  protected readonly carregando = signal(true);
  private readonly veiculos = signal<Veiculo[]>([]);
  private readonly agendamentos = signal<Agendamento[]>([]);
  private readonly usuarios = signal<Usuario[]>([]);

  private readonly hoje = paraDataIso(new Date());
  protected readonly paraData = deDataIso;

  protected readonly indicadores = computed(() => {
    const veiculos = this.veiculos();
    const confirmados = this.agendamentos().filter((a) => a.status === 'CONFIRMADO');
    return [
      { icone: 'directions_car', rotulo: 'Veículos disponíveis', valor: veiculos.filter((v) => v.situacao === 'DISPONIVEL').length, link: '/admin/veiculos' },
      { icone: 'sell', rotulo: 'Veículos vendidos', valor: veiculos.filter((v) => v.situacao === 'VENDIDO').length, link: '/admin/veiculos' },
      { icone: 'today', rotulo: 'Test drives hoje', valor: confirmados.filter((a) => a.data === this.hoje).length, link: '/admin/agendamentos' },
      { icone: 'event_upcoming', rotulo: 'Test drives futuros', valor: confirmados.filter((a) => a.cancelavel).length, link: '/admin/agendamentos' },
      { icone: 'group', rotulo: 'Clientes cadastrados', valor: this.usuarios().filter((u) => u.perfil === 'CLIENTE').length, link: '/admin/clientes' },
    ];
  });

  protected readonly proximos = computed(() =>
    this.agendamentos()
      .filter((a) => a.status === 'CONFIRMADO' && a.cancelavel)
      .slice(0, 6),
  );

  ngOnInit(): void {
    forkJoin({
      veiculos: this.veiculoService.listar({ situacao: ['DISPONIVEL', 'VENDIDO', 'INATIVO'] }),
      agendamentos: this.agendamentoService.listar(),
      usuarios: this.adminService.usuarios(),
    }).subscribe({
      next: ({ veiculos, agendamentos, usuarios }) => {
        this.veiculos.set(veiculos);
        this.agendamentos.set(agendamentos);
        this.usuarios.set(usuarios);
        this.carregando.set(false);
      },
      error: (erro) => {
        this.carregando.set(false);
        this.notificacao.erroHttp(erro, 'Não foi possível carregar o painel.');
      },
    });
  }
}
