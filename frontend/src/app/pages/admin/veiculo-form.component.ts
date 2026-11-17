import { Component, OnDestroy, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { Observable, of, switchMap } from 'rxjs';

import {
  COMBUSTIVEIS, Combustivel, TRANSMISSOES, Transmissao, Veiculo, VeiculoDados,
} from '../../core/models/veiculo.model';
import { NotificacaoService } from '../../core/services/notificacao.service';
import { VeiculoService } from '../../core/services/veiculo.service';
import { aplicarErrosDosCampos, mensagemDeErro } from '../../core/utils/erros';
import { CarregandoComponent } from '../../shared/components/carregando/carregando.component';

export const TAMANHO_MAXIMO_FOTO = 5 * 1024 * 1024;
const TIPOS_FOTO = ['image/jpeg', 'image/png', 'image/webp'];

// Mesma regra do backend: o ano do modelo é o de fabricação ou o seguinte
export function anosCoerentes(grupo: AbstractControl): ValidationErrors | null {
  const fabricacao = grupo.get('anoFabricacao')?.value;
  const modelo = grupo.get('anoModelo')?.value;
  if (fabricacao == null || modelo == null) {
    return null;
  }
  return modelo < fabricacao || modelo > fabricacao + 1 ? { anosIncoerentes: true } : null;
}

// Validação prévia da foto (o backend valida de novo, inclusive o conteúdo do arquivo)
export function validarFoto(arquivo: File): string | null {
  if (!TIPOS_FOTO.includes(arquivo.type)) {
    return 'Envie uma imagem JPG, PNG ou WebP.';
  }
  if (arquivo.size > TAMANHO_MAXIMO_FOTO) {
    return 'A imagem deve ter no máximo 5 MB.';
  }
  return null;
}

@Component({
  selector: 'app-veiculo-form',
  imports: [
    ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule,
    MatIconModule, MatProgressSpinnerModule, CarregandoComponent,
  ],
  templateUrl: './veiculo-form.component.html',
  styleUrl: './veiculo-form.component.scss',
})
export class VeiculoFormComponent implements OnDestroy {
  private readonly veiculoService = inject(VeiculoService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly router = inject(Router);

  // Presente apenas na rota de edição (/admin/veiculos/:id/editar)
  readonly id = input<string>();
  protected readonly edicao = computed(() => !!this.id());

  protected readonly anoAtual = new Date().getFullYear();
  protected readonly combustiveis = Object.entries(COMBUSTIVEIS) as [Combustivel, string][];
  protected readonly transmissoes = Object.entries(TRANSMISSOES) as [Transmissao, string][];

  protected readonly carregando = signal(false);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly foto = signal<File | null>(null);
  protected readonly erroFoto = signal<string | null>(null);
  protected readonly previa = signal<string | null>(null);

  protected readonly formulario = inject(FormBuilder).group(
    {
      marca: ['', [Validators.required, Validators.maxLength(60)]],
      modelo: ['', [Validators.required, Validators.maxLength(80)]],
      anoFabricacao: [null as number | null, [Validators.required, Validators.min(1900), Validators.max(this.anoAtual)]],
      anoModelo: [null as number | null, [Validators.required, Validators.min(1900)]],
      cor: ['', [Validators.required, Validators.maxLength(40)]],
      quilometragem: [null as number | null, [Validators.required, Validators.min(0)]],
      combustivel: [null as Combustivel | null, Validators.required],
      transmissao: [null as Transmissao | null, Validators.required],
      preco: [null as number | null, [Validators.required, Validators.min(0.01)]],
      descricao: ['', Validators.maxLength(2000)],
    },
    { validators: anosCoerentes },
  );

  constructor() {
    effect(() => {
      const id = this.id();
      if (id) {
        untracked(() => this.carregar(Number(id)));
      }
    });
  }

  ngOnDestroy(): void {
    this.liberarPrevia();
  }

  protected selecionarFoto(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    const arquivo = campo.files?.[0];
    campo.value = ''; // permite escolher o mesmo arquivo de novo
    if (!arquivo) {
      return;
    }
    const problema = validarFoto(arquivo);
    this.erroFoto.set(problema);
    if (problema) {
      return;
    }
    this.liberarPrevia();
    this.foto.set(arquivo);
    this.previa.set(URL.createObjectURL(arquivo));
  }

  protected salvar(): void {
    if (!this.edicao() && !this.foto()) {
      this.erroFoto.set('A fotografia do veículo é obrigatória.');
    }
    if (this.formulario.invalid || this.erroFoto()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.erro.set(null);

    const valores = this.formulario.getRawValue();
    const dados: VeiculoDados = {
      marca: valores.marca!.trim(),
      modelo: valores.modelo!.trim(),
      anoFabricacao: valores.anoFabricacao!,
      anoModelo: valores.anoModelo!,
      cor: valores.cor!.trim(),
      quilometragem: valores.quilometragem!,
      combustivel: valores.combustivel!,
      transmissao: valores.transmissao!,
      preco: valores.preco!,
      descricao: valores.descricao?.trim() || null,
    };

    this.requisicao(dados).subscribe({
      next: (veiculo) => {
        this.notificacao.sucesso(
          this.edicao() ? `${veiculo.marca} ${veiculo.modelo} atualizado.` : `${veiculo.marca} ${veiculo.modelo} cadastrado.`,
        );
        this.router.navigateByUrl('/admin/veiculos');
      },
      error: (erro) => {
        this.enviando.set(false);
        if (!aplicarErrosDosCampos(this.formulario, erro)) {
          this.erro.set(mensagemDeErro(erro, 'Não foi possível salvar o veículo.'));
        }
      },
    });
  }

  // Cadastro envia dados + foto juntos; na edição, atualiza os dados e, se houver foto nova, troca a foto
  private requisicao(dados: VeiculoDados): Observable<Veiculo> {
    const foto = this.foto();
    if (!this.edicao()) {
      return this.veiculoService.cadastrar(dados, foto!);
    }
    const id = Number(this.id());
    return this.veiculoService
      .atualizar(id, dados)
      .pipe(switchMap((veiculo) => (foto ? this.veiculoService.trocarFoto(id, foto) : of(veiculo))));
  }

  private carregar(id: number): void {
    this.carregando.set(true);
    this.veiculoService.buscar(id).subscribe({
      next: (veiculo) => {
        this.formulario.patchValue(veiculo);
        this.previa.set(veiculo.fotoUrl);
        this.carregando.set(false);
      },
      error: (erro) => {
        this.notificacao.erroHttp(erro, 'Veículo não encontrado.');
        this.router.navigateByUrl('/admin/veiculos');
      },
    });
  }

  private liberarPrevia(): void {
    const atual = this.previa();
    if (atual?.startsWith('blob:')) {
      URL.revokeObjectURL(atual);
    }
  }
}
