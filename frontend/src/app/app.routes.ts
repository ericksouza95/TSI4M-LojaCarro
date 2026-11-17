import { Routes } from '@angular/router';

import { adminGuard, clienteGuard, visitanteGuard } from './core/guards/auth.guards';

// Cada página é carregada sob demanda (loadComponent), reduzindo o tamanho inicial da aplicação
export const routes: Routes = [
  // ---------- Público ----------
  {
    path: '',
    title: 'LojaCarro | Seminovos com test drive',
    loadComponent: () => import('./pages/home/home.component').then((m) => m.HomeComponent),
  },
  {
    path: 'veiculos',
    title: 'Veículos | LojaCarro',
    loadComponent: () => import('./pages/veiculos/veiculo-lista.component').then((m) => m.VeiculoListaComponent),
  },
  {
    path: 'veiculos/:id',
    title: 'Detalhes do veículo | LojaCarro',
    loadComponent: () => import('./pages/veiculos/veiculo-detalhe.component').then((m) => m.VeiculoDetalheComponent),
  },
  {
    path: 'login',
    title: 'Entrar | LojaCarro',
    canActivate: [visitanteGuard],
    loadComponent: () => import('./pages/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'cadastro',
    title: 'Criar conta | LojaCarro',
    canActivate: [visitanteGuard],
    loadComponent: () => import('./pages/auth/cadastro.component').then((m) => m.CadastroComponent),
  },

  // ---------- Cliente ----------
  {
    path: 'veiculos/:id/agendar',
    title: 'Agendar test drive | LojaCarro',
    canActivate: [clienteGuard],
    loadComponent: () => import('./pages/agendamentos/agendar.component').then((m) => m.AgendarComponent),
  },
  {
    path: 'meus-agendamentos',
    title: 'Meus test drives | LojaCarro',
    canActivate: [clienteGuard],
    loadComponent: () =>
      import('./pages/agendamentos/meus-agendamentos.component').then((m) => m.MeusAgendamentosComponent),
  },

  // ---------- Administrador ----------
  {
    path: 'admin',
    canActivate: [adminGuard],
    canActivateChild: [adminGuard],
    children: [
      {
        path: '',
        title: 'Painel | LojaCarro',
        loadComponent: () => import('./pages/admin/painel.component').then((m) => m.PainelComponent),
      },
      {
        path: 'veiculos',
        title: 'Gerenciar veículos | LojaCarro',
        loadComponent: () => import('./pages/admin/veiculos-admin.component').then((m) => m.VeiculosAdminComponent),
      },
      {
        path: 'veiculos/novo',
        title: 'Novo veículo | LojaCarro',
        loadComponent: () => import('./pages/admin/veiculo-form.component').then((m) => m.VeiculoFormComponent),
      },
      {
        path: 'veiculos/:id/editar',
        title: 'Editar veículo | LojaCarro',
        loadComponent: () => import('./pages/admin/veiculo-form.component').then((m) => m.VeiculoFormComponent),
      },
      {
        path: 'agendamentos',
        title: 'Agendamentos | LojaCarro',
        loadComponent: () =>
          import('./pages/admin/agendamentos-admin.component').then((m) => m.AgendamentosAdminComponent),
      },
      {
        path: 'clientes',
        title: 'Clientes | LojaCarro',
        loadComponent: () => import('./pages/admin/clientes.component').then((m) => m.ClientesComponent),
      },
      {
        path: 'logs',
        title: 'Logs | LojaCarro',
        loadComponent: () => import('./pages/admin/logs.component').then((m) => m.LogsComponent),
      },
    ],
  },

  {
    path: '**',
    title: 'Página não encontrada | LojaCarro',
    loadComponent: () => import('./pages/nao-encontrado.component').then((m) => m.NaoEncontradoComponent),
  },
];
