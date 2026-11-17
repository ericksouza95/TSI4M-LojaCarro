import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';

import { Usuario } from '../../core/models/usuario.model';
import { AuthService } from '../../core/services/auth.service';
import { NotificacaoService } from '../../core/services/notificacao.service';
import { mensagemDeErro } from '../../core/utils/erros';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './auth.scss',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notificacao = inject(NotificacaoService);

  // Página para onde voltar depois do login (ex.: o agendamento que o visitante tentou abrir)
  readonly redirect = input<string>();

  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly mostrarSenha = signal(false);

  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', Validators.required],
  });

  protected entrar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.erro.set(null);
    this.auth.login(this.formulario.getRawValue()).subscribe({
      next: (usuario) => {
        this.notificacao.sucesso(`Bem-vindo(a), ${usuario.nome.split(' ')[0]}!`);
        this.router.navigateByUrl(this.destino(usuario));
      },
      error: (erro) => {
        this.erro.set(mensagemDeErro(erro, 'Não foi possível entrar.'));
        this.enviando.set(false);
      },
    });
  }

  private destino(usuario: Usuario): string {
    const redirect = this.redirect();
    // Só aceita caminhos internos, para não redirecionar para outro site
    if (redirect && redirect.startsWith('/') && !redirect.startsWith('//')) {
      return redirect;
    }
    return usuario.perfil === 'ADMIN' ? '/admin' : '/veiculos';
  }
}
