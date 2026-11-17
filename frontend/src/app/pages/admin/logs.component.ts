import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';

import { LogSistema } from '../../core/models/api.model';
import { AdminService } from '../../core/services/admin.service';
import { NotificacaoService } from '../../core/services/notificacao.service';
import { CarregandoComponent } from '../../shared/components/carregando/carregando.component';
import { EstadoVazioComponent } from '../../shared/components/estado-vazio/estado-vazio.component';

// RF07: logs de auditoria (horário, usuário, ação e detalhes)
@Component({
  selector: 'app-logs',
  imports: [FormsModule, DatePipe, MatTableModule, MatFormFieldModule, MatInputModule, MatIconModule, CarregandoComponent, EstadoVazioComponent],
  template: `
    <div class="container pagina">
      <div class="cabecalho-pagina">
        <div>
          <h1>Logs de auditoria</h1>
          <p>Todas as operações registradas, das mais recentes para as mais antigas.</p>
        </div>
      </div>
      <div class="painel filtros-admin">
        <mat-form-field>
          <mat-label>Filtrar por ação, usuário ou descrição</mat-label>
          <input matInput [ngModel]="busca()" (ngModelChange)="busca.set($event)" />
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>
      </div>
      @if (carregando()) {
        <app-carregando />
      } @else if (filtrados().length === 0) {
        <app-estado-vazio icone="history" titulo="Nenhum registro encontrado" />
      } @else {
        <div class="tabela-rolagem">
          <table mat-table [dataSource]="filtrados()">
            <ng-container matColumnDef="dataHora">
              <th mat-header-cell *matHeaderCellDef>Data e hora</th>
              <td mat-cell *matCellDef="let l">{{ l.dataHora | date: 'dd/MM/yyyy HH:mm:ss' }}</td>
            </ng-container>
            <ng-container matColumnDef="acao">
              <th mat-header-cell *matHeaderCellDef>Ação</th>
              <td mat-cell *matCellDef="let l"><code>{{ l.acao }}</code><br /><small>{{ l.entidade }}</small></td>
            </ng-container>
            <ng-container matColumnDef="usuario">
              <th mat-header-cell *matHeaderCellDef>Usuário</th>
              <td mat-cell *matCellDef="let l">{{ l.usuario }}</td>
            </ng-container>
            <ng-container matColumnDef="descricao">
              <th mat-header-cell *matHeaderCellDef>Detalhes</th>
              <td mat-cell *matCellDef="let l">{{ l.descricao }}</td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="colunas"></tr>
            <tr mat-row *matRowDef="let linha; columns: colunas"></tr>
          </table>
        </div>
      }
    </div>
  `,
  styleUrl: './admin.scss',
})
export class LogsComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly notificacao = inject(NotificacaoService);

  protected readonly colunas = ['dataHora', 'acao', 'usuario', 'descricao'];
  protected readonly logs = signal<LogSistema[]>([]);
  protected readonly carregando = signal(true);
  protected readonly busca = signal('');

  protected readonly filtrados = computed(() => {
    const termo = this.busca().trim().toLowerCase();
    return this.logs().filter(
      (l) => !termo || `${l.acao} ${l.entidade} ${l.usuario} ${l.descricao}`.toLowerCase().includes(termo),
    );
  });

  ngOnInit(): void {
    this.adminService.logs().subscribe({
      next: (logs) => {
        this.logs.set(logs);
        this.carregando.set(false);
      },
      error: (erro) => {
        this.carregando.set(false);
        this.notificacao.erroHttp(erro, 'Não foi possível carregar os logs.');
      },
    });
  }
}
