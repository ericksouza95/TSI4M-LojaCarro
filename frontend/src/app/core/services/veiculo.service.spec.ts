import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { VeiculoDados } from '../models/veiculo.model';
import { VeiculoService } from './veiculo.service';

describe('VeiculoService', () => {
  let service: VeiculoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(VeiculoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envia somente os filtros preenchidos na consulta do catálogo', () => {
    service.listar({ busca: 'corolla', precoMin: null, precoMax: 150000, anoMin: undefined, situacao: ['DISPONIVEL', 'VENDIDO'] })
      .subscribe((veiculos) => expect(veiculos).toEqual([]));

    const requisicao = http.expectOne((r) => r.url === '/api/veiculos');
    expect(requisicao.request.method).toBe('GET');
    expect(requisicao.request.params.get('busca')).toBe('corolla');
    expect(requisicao.request.params.get('precoMax')).toBe('150000');
    expect(requisicao.request.params.has('precoMin')).toBe(false);
    expect(requisicao.request.params.has('anoMin')).toBe(false);
    expect(requisicao.request.params.getAll('situacao')).toEqual(['DISPONIVEL', 'VENDIDO']);
    requisicao.flush([]);
  });

  it('cadastra o veículo em multipart com os dados em JSON e a foto', async () => {
    const dados: VeiculoDados = {
      marca: 'Toyota', modelo: 'Corolla', anoFabricacao: 2023, anoModelo: 2024, cor: 'Prata',
      quilometragem: 10000, combustivel: 'FLEX', transmissao: 'CVT', preco: 135000, descricao: null,
    };
    const foto = new File([new Uint8Array([0xff, 0xd8, 0xff])], 'carro.jpg', { type: 'image/jpeg' });

    service.cadastrar(dados, foto).subscribe();

    const requisicao = http.expectOne('/api/veiculos');
    expect(requisicao.request.method).toBe('POST');
    const corpo = requisicao.request.body as FormData;
    expect(corpo).toBeInstanceOf(FormData);
    expect((corpo.get('foto') as File).name).toBe('carro.jpg');
    const parteDados = corpo.get('dados') as Blob;
    expect(parteDados.type).toBe('application/json');
    expect(JSON.parse(await parteDados.text())).toEqual(dados);
    requisicao.flush({});
  });

  it('consulta a grade de horários do veículo na data informada', () => {
    service.horarios(7, '2030-03-05').subscribe();

    const requisicao = http.expectOne((r) => r.url === '/api/veiculos/7/horarios');
    expect(requisicao.request.params.get('data')).toBe('2030-03-05');
    requisicao.flush([]);
  });
});
