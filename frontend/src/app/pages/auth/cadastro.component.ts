import { Component, inject, input, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { NotificacaoService } from '../../core/services/notificacao.service';
import { aplicarErrosDosCampos, mensagemDeErro } from '../../core/utils/erros';

// Confirmação de senha precisa ser igual à senha
export function senhasIguais(grupo: AbstractControl): ValidationErrors | null {
  const senha = grupo.get('senha')?.value;
  const confirmacao = grupo.get('confirmacao')?.value;
  return senha && confirmacao && senha !== confirmacao ? { senhasDiferentes: true } : null;
}

@Component({
  selector: 'app-cadastro',
  imports: [
    ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './cadastro.component.html',
  styleUrl: './auth.scss',
})
export class CadastroComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notificacao = inject(NotificacaoService);

  readonly redirect = input<string>();

  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly formulario = inject(FormBuilder).nonNullable.group(
    {
      nome: ['', [Validators.required, Validators.maxLength(120)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(160)]],
      telefone: ['', Validators.pattern(/^[0-9()+\- ]{8,20}$/)],
      senha: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(72)]],
      confirmacao: ['', Validators.required],
    },
    { validators: senhasIguais },
  );

  protected cadastrar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { nome, email, telefone, senha } = this.formulario.getRawValue();
    this.enviando.set(true);
    this.erro.set(null);

    // Cria a conta e já entra com ela
    this.auth
      .cadastrar({ nome: nome.trim(), email: email.trim(), senha, telefone: telefone.trim() || null })
      .pipe(switchMap(() => this.auth.login({ email, senha })))
      .subscribe({
        next: (usuario) => {
          this.notificacao.sucesso(`Conta criada! Bem-vindo(a), ${usuario.nome.split(' ')[0]}.`);
          const redirect = this.redirect();
          this.router.navigateByUrl(redirect?.startsWith('/') && !redirect.startsWith('//') ? redirect : '/veiculos');
        },
        error: (erro) => {
          if (!aplicarErrosDosCampos(this.formulario, erro)) {
            this.erro.set(mensagemDeErro(erro, 'Não foi possível criar a conta.'));
          }
          this.enviando.set(false);
        },
      });
  }
}
