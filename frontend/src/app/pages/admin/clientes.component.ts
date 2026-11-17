import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';

import { Usuario } from '../../core/models/usuario.model';
import { AdminService } from '../../core/services/admin.service';
import { NotificacaoService } from '../../core/services/notificacao.service';
import { CarregandoComponent } from '../../shared/components/carregando/carregando.component';
import { EstadoVazioComponent } from '../../shared/components/estado-vazio/estado-vazio.component';
import { RotuloPipe } from '../../shared/pipes/rotulo.pipe';

// RF03: o administrador visualiza as informações dos clientes
@Component({
  selector: 'app-clientes',
  imports: [FormsModule, DatePipe, MatTableModule, MatFormFieldModule, MatInputModule, MatIconModule, RotuloPipe, CarregandoComponent, EstadoVazioComponent],
  template: `
    <div class="container pagina">
      <div class="cabecalho-pagina">
        <div>
          <h1>Clientes</h1>
          <p>{{ usuarios().length }} usuários cadastrados</p>
        </div>
      </div>
      <div class="painel filtros-admin">
        <mat-form-field>
          <mat-label>Buscar por nome ou e-mail</mat-label>
          <input matInput [ngModel]="busca()" (ngModelChange)="busca.set($event)" />
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>
      </div>
      @if (carregando()) {
        <app-carregando />
      } @else if (filtrados().length === 0) {
        <app-estado-vazio icone="person_search" titulo="Nenhum usuário encontrado" />
      } @else {
        <div class="tabela-rolagem">
          <table mat-table [dataSource]="filtrados()">
            <ng-container matColumnDef="nome">
              <th mat-header-cell *matHeaderCellDef>Nome</th>
              <td mat-cell *matCellDef="let u"><strong>{{ u.nome }}</strong></td>
            </ng-container>
            <ng-container matColumnDef="email">
              <th mat-header-cell *matHeaderCellDef>E-mail</th>
              <td mat-cell *matCellDef="let u">{{ u.email }}</td>
            </ng-container>
            <ng-container matColumnDef="telefone">
              <th mat-header-cell *matHeaderCellDef>Telefone</th>
              <td mat-cell *matCellDef="let u">{{ u.telefone ?? '—' }}</td>
            </ng-container>
            <ng-container matColumnDef="perfil">
              <th mat-header-cell *matHeaderCellDef>Perfil</th>
              <td mat-cell *matCellDef="let u">{{ u.perfil | rotulo: 'perfil' }}</td>
            </ng-container>
            <ng-container matColumnDef="cadastro">
              <th mat-header-cell *matHeaderCellDef>Cadastro</th>
              <td mat-cell *matCellDef="let u">{{ u.dataCadastro | date: 'dd/MM/yyyy' }}</td>
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
export class ClientesComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly notificacao = inject(NotificacaoService);

  protected readonly colunas = ['nome', 'email', 'telefone', 'perfil', 'cadastro'];
  protected readonly usuarios = signal<Usuario[]>([]);
  protected readonly carregando = signal(true);
  protected readonly busca = signal('');

  protected readonly filtrados = computed(() => {
    const termo = this.busca().trim().toLowerCase();
    return this.usuarios().filter((u) => !termo || `${u.nome} ${u.email}`.toLowerCase().includes(termo));
  });

  ngOnInit(): void {
    this.adminService.usuarios().subscribe({
      next: (usuarios) => {
        this.usuarios.set(usuarios);
        this.carregando.set(false);
      },
      error: (erro) => {
        this.carregando.set(false);
        this.notificacao.erroHttp(erro, 'Não foi possível carregar os clientes.');
      },
    });
  }
}
