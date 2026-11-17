import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';

import { SITUACOES_VEICULO, SituacaoVeiculo, Veiculo } from '../../core/models/veiculo.model';
import { NotificacaoService } from '../../core/services/notificacao.service';
import { VeiculoService } from '../../core/services/veiculo.service';
import { CarregandoComponent } from '../../shared/components/carregando/carregando.component';
import { confirmar } from '../../shared/components/confirmacao-dialog/confirmacao-dialog.component';
import { EstadoVazioComponent } from '../../shared/components/estado-vazio/estado-vazio.component';
import { StatusChipComponent } from '../../shared/components/status-chip/status-chip.component';

@Component({
  selector: 'app-veiculos-admin',
  imports: [
    FormsModule, RouterLink, CurrencyPipe, DecimalPipe, MatTableModule, MatButtonModule, MatIconModule, MatMenuModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatTooltipModule,
    CarregandoComponent, EstadoVazioComponent, StatusChipComponent,
  ],
  templateUrl: './veiculos-admin.component.html',
  styleUrl: './admin.scss',
})
export class VeiculosAdminComponent implements OnInit {
  private readonly veiculoService = inject(VeiculoService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly dialog = inject(MatDialog);

  protected readonly colunas = ['foto', 'veiculo', 'ano', 'km', 'preco', 'situacao', 'acoes'];
  protected readonly situacoes = Object.entries(SITUACOES_VEICULO) as [SituacaoVeiculo, string][];

  protected readonly veiculos = signal<Veiculo[]>([]);
  protected readonly carregando = signal(true);
  protected readonly busca = signal('');
  protected readonly situacoesFiltro = signal<SituacaoVeiculo[]>(['DISPONIVEL', 'VENDIDO']);

  // Filtro feito na própria tela, sobre a lista completa
  protected readonly filtrados = computed(() => {
    const termo = this.busca().trim().toLowerCase();
    const situacoes = this.situacoesFiltro();
    return this.veiculos().filter(
      (v) =>
        situacoes.includes(v.situacao) &&
        (!termo || `${v.marca} ${v.modelo}`.toLowerCase().includes(termo)),
    );
  });

  ngOnInit(): void {
    this.carregar();
  }

  protected alterarSituacao(veiculo: Veiculo, situacao: SituacaoVeiculo): void {
    const avisos: Record<SituacaoVeiculo, string> = {
      DISPONIVEL: 'O veículo voltará a aparecer no catálogo e poderá receber test drives.',
      VENDIDO: 'O veículo sairá do catálogo e os test drives futuros dele serão cancelados.',
      INATIVO: 'O veículo será desativado (exclusão lógica): sai do catálogo e os test drives futuros serão cancelados.',
    };
    confirmar(this.dialog, {
      titulo: `Marcar ${veiculo.marca} ${veiculo.modelo} como ${SITUACOES_VEICULO[situacao].toLowerCase()}?`,
      mensagem: avisos[situacao],
      confirmar: 'Confirmar',
      perigo: situacao !== 'DISPONIVEL',
    }).subscribe((ok) => {
      if (!ok) {
        return;
      }
      this.veiculoService.alterarSituacao(veiculo.id, situacao).subscribe({
        next: (atualizado) => {
          this.veiculos.update((lista) => lista.map((v) => (v.id === atualizado.id ? atualizado : v)));
          this.notificacao.sucesso(`Situação alterada para ${SITUACOES_VEICULO[situacao]}.`);
        },
        error: (erro) => this.notificacao.erroHttp(erro, 'Não foi possível alterar a situação.'),
      });
    });
  }

  private carregar(): void {
    this.veiculoService.listar({ situacao: ['DISPONIVEL', 'VENDIDO', 'INATIVO'] }).subscribe({
      next: (veiculos) => {
        this.veiculos.set(veiculos);
        this.carregando.set(false);
      },
      error: (erro) => {
        this.carregando.set(false);
        this.notificacao.erroHttp(erro, 'Não foi possível carregar os veículos.');
      },
    });
  }
}
