import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { DEFAULT_CURRENCY_CODE, LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { Veiculo } from '../../core/models/veiculo.model';
import { NotificacaoService } from '../../core/services/notificacao.service';
import { VeiculoService } from '../../core/services/veiculo.service';
import { VeiculoListaComponent } from './veiculo-lista.component';

registerLocaleData(localePt);

function veiculo(id: number, marca: string, modelo: string, preco: number): Veiculo {
  return {
    id, marca, modelo, preco, anoFabricacao: 2022, anoModelo: 2023, cor: 'Prata', quilometragem: 25000,
    combustivel: 'FLEX', transmissao: 'AUTOMATICA', descricao: null, situacao: 'DISPONIVEL',
    fotoUrl: `/api/veiculos/${id}/foto?v=abc`, dataCadastro: '2030-01-01T10:00:00',
  };
}

describe('VeiculoListaComponent', () => {
  let fixture: ComponentFixture<VeiculoListaComponent>;
  let listar: ReturnType<typeof vi.fn>;

  async function criar(): Promise<HTMLElement> {
    fixture = TestBed.createComponent(VeiculoListaComponent);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    listar = vi.fn();
    TestBed.configureTestingModule({
      imports: [VeiculoListaComponent],
      providers: [
        provideRouter([]),
        { provide: VeiculoService, useValue: { listar } },
        { provide: NotificacaoService, useValue: { erroHttp: vi.fn() } },
        { provide: LOCALE_ID, useValue: 'pt-BR' },
        { provide: DEFAULT_CURRENCY_CODE, useValue: 'BRL' },
      ],
    });
  });

  it('renderiza um card para cada veículo com foto, modelo e preço', async () => {
    listar.mockReturnValue(of([veiculo(1, 'Toyota', 'Corolla', 135000), veiculo(2, 'Honda', 'Civic', 120000)]));

    const tela = await criar();

    const cards = tela.querySelectorAll('app-veiculo-card');
    expect(cards.length).toBe(2);
    expect(cards[0].textContent).toContain('Corolla');
    expect(cards[0].textContent).toContain('135.000,00');
    expect(cards[0].querySelector('img')?.getAttribute('src')).toBe('/api/veiculos/1/foto?v=abc');
    expect(cards[0].querySelector('a')?.getAttribute('href')).toBe('/veiculos/1');
    expect(tela.textContent).toContain('2 veículos encontrados');
  });

  it('aplica os filtros da URL na consulta', async () => {
    listar.mockReturnValue(of([]));
    fixture = TestBed.createComponent(VeiculoListaComponent);
    fixture.componentRef.setInput('busca', 'civic');
    fixture.componentRef.setInput('precoMax', '100000');
    await fixture.whenStable();

    expect(listar).toHaveBeenCalledWith(expect.objectContaining({ busca: 'civic', precoMax: 100000, precoMin: null }));
  });

  it('mostra mensagem quando nenhum veículo é encontrado', async () => {
    listar.mockReturnValue(of([]));

    const tela = await criar();

    expect(tela.querySelector('app-veiculo-card')).toBeNull();
    expect(tela.textContent).toContain('Nenhum veículo encontrado');
  });

  it('avisa o usuário quando a API falha', async () => {
    listar.mockReturnValue(throwError(() => new Error('falha')));

    await criar();

    expect(TestBed.inject(NotificacaoService).erroHttp).toHaveBeenCalled();
  });
});
