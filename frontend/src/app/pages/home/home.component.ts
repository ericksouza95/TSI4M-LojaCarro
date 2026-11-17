import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';

import { Veiculo } from '../../core/models/veiculo.model';
import { VeiculoService } from '../../core/services/veiculo.service';
import { CarregandoComponent } from '../../shared/components/carregando/carregando.component';
import { EstadoVazioComponent } from '../../shared/components/estado-vazio/estado-vazio.component';
import { VeiculoCardComponent } from '../../shared/components/veiculo-card/veiculo-card.component';

@Component({
  selector: 'app-home',
  imports: [FormsModule, RouterLink, MatButtonModule, MatIconModule, VeiculoCardComponent, CarregandoComponent, EstadoVazioComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  private readonly veiculoService = inject(VeiculoService);
  private readonly router = inject(Router);

  protected readonly destaques = signal<Veiculo[]>([]);
  protected readonly carregando = signal(true);
  protected readonly falhou = signal(false);
  protected busca = '';

  ngOnInit(): void {
    this.veiculoService.listar().subscribe({
      next: (veiculos) => {
        // Os mais recentes primeiro
        const recentes = [...veiculos].sort((a, b) => b.dataCadastro.localeCompare(a.dataCadastro));
        this.destaques.set(recentes.slice(0, 6));
        this.carregando.set(false);
      },
      error: () => {
        this.falhou.set(true);
        this.carregando.set(false);
      },
    });
  }

  protected pesquisar(): void {
    const busca = this.busca.trim();
    this.router.navigate(['/veiculos'], { queryParams: busca ? { busca } : {} });
  }
}
