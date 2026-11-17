import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormGroup } from '@angular/forms';
import { provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter } from '@angular/router';

import { NotificacaoService } from '../../core/services/notificacao.service';
import { VeiculoService } from '../../core/services/veiculo.service';
import { VeiculoFormComponent, validarFoto } from './veiculo-form.component';

describe('VeiculoFormComponent', () => {
  let fixture: ComponentFixture<VeiculoFormComponent>;
  let componente: VeiculoFormComponent;
  let formulario: FormGroup;
  let cadastrar: ReturnType<typeof vi.fn>;

  const dadosValidos = {
    marca: 'Toyota', modelo: 'Corolla', anoFabricacao: 2022, anoModelo: 2023, cor: 'Prata',
    quilometragem: 20000, combustivel: 'FLEX', transmissao: 'CVT', preco: 130000, descricao: '',
  };

  beforeEach(async () => {
    cadastrar = vi.fn();
    TestBed.configureTestingModule({
      imports: [VeiculoFormComponent],
      providers: [
        provideRouter([]),
        provideNativeDateAdapter(),
        { provide: VeiculoService, useValue: { cadastrar } },
        { provide: NotificacaoService, useValue: { sucesso: vi.fn(), erroHttp: vi.fn() } },
      ],
    });
    fixture = TestBed.createComponent(VeiculoFormComponent);
    componente = fixture.componentInstance;
    formulario = (componente as unknown as { formulario: FormGroup }).formulario;
    await fixture.whenStable();
  });

  async function enviar(): Promise<void> {
    (fixture.nativeElement as HTMLElement).querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('exige os campos obrigatórios', async () => {
    await enviar();

    const tela = (fixture.nativeElement as HTMLElement).textContent!;
    expect(formulario.invalid).toBe(true);
    expect(tela).toContain('Informe a marca.');
    expect(tela).toContain('Informe o preço.');
    expect(tela).toContain('Escolha o combustível.');
    expect(cadastrar).not.toHaveBeenCalled();
  });

  it('recusa ano do modelo anterior ao de fabricação ou dois anos à frente', () => {
    formulario.patchValue({ ...dadosValidos, anoFabricacao: 2022, anoModelo: 2021 });
    expect(formulario.hasError('anosIncoerentes')).toBe(true);

    formulario.patchValue({ anoModelo: 2024 });
    expect(formulario.hasError('anosIncoerentes')).toBe(true);

    formulario.patchValue({ anoModelo: 2023 });
    expect(formulario.hasError('anosIncoerentes')).toBe(false);
  });

  it('recusa preço zero e quilometragem negativa', () => {
    formulario.patchValue({ ...dadosValidos, preco: 0, quilometragem: -5 });

    expect(formulario.get('preco')!.hasError('min')).toBe(true);
    expect(formulario.get('quilometragem')!.hasError('min')).toBe(true);
  });

  it('não cadastra sem fotografia, mesmo com os dados válidos', async () => {
    formulario.patchValue(dadosValidos);

    await enviar();

    expect(cadastrar).not.toHaveBeenCalled();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('A fotografia do veículo é obrigatória.');
  });

  it('valida o tipo e o tamanho da foto antes de enviar', () => {
    const png = new File([new Uint8Array(100)], 'carro.png', { type: 'image/png' });
    const pdf = new File([new Uint8Array(100)], 'carro.pdf', { type: 'application/pdf' });
    const grande = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'grande.jpg', { type: 'image/jpeg' });

    expect(validarFoto(png)).toBeNull();
    expect(validarFoto(pdf)).toContain('JPG, PNG ou WebP');
    expect(validarFoto(grande)).toContain('5 MB');
  });
});
