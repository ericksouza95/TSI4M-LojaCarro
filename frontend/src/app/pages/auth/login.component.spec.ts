import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { NotificacaoService } from '../../core/services/notificacao.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let tela: HTMLElement;
  let login: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    login = vi.fn();
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { login } },
        { provide: NotificacaoService, useValue: { sucesso: vi.fn() } },
      ],
    });
    fixture = TestBed.createComponent(LoginComponent);
    tela = fixture.nativeElement;
    await fixture.whenStable();
  });

  async function preencher(email: string, senha: string): Promise<void> {
    const [campoEmail, campoSenha] = Array.from(tela.querySelectorAll('input')) as HTMLInputElement[];
    campoEmail.value = email;
    campoEmail.dispatchEvent(new Event('input'));
    campoSenha.value = senha;
    campoSenha.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  async function enviar(): Promise<void> {
    tela.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('não envia o formulário vazio e mostra os campos obrigatórios', async () => {
    await enviar();

    expect(login).not.toHaveBeenCalled();
    expect(tela.textContent).toContain('Informe o e-mail.');
    expect(tela.textContent).toContain('Informe a senha.');
  });

  it('valida o formato do e-mail', async () => {
    await preencher('ana-sem-arroba', '123456');
    await enviar();

    expect(login).not.toHaveBeenCalled();
    expect(tela.textContent).toContain('E-mail inválido.');
  });

  it('faz login e volta para a página que o usuário queria abrir', async () => {
    login.mockReturnValue(of({ id: 1, nome: 'Ana Souza', perfil: 'CLIENTE' }));
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture.componentRef.setInput('redirect', '/veiculos/3/agendar');

    await preencher('ana@email.com', '123456');
    await enviar();

    expect(login).toHaveBeenCalledWith({ email: 'ana@email.com', senha: '123456' });
    expect(navegar).toHaveBeenCalledWith('/veiculos/3/agendar');
  });

  it('mostra a mensagem da API quando a senha está errada', async () => {
    login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401, error: { status: 401, erro: 'E-mail ou senha inválidos.' } })));

    await preencher('ana@email.com', 'errada');
    await enviar();

    expect(tela.querySelector('[role=alert]')?.textContent).toContain('E-mail ou senha inválidos.');
  });
});
