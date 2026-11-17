package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.dto.SituacaoVeiculoRequest;
import br.org.edu.ifrn.LojaCarro.dto.VeiculoFiltro;
import br.org.edu.ifrn.LojaCarro.dto.VeiculoRequest;
import br.org.edu.ifrn.LojaCarro.dto.VeiculoResponse;
import br.org.edu.ifrn.LojaCarro.model.SituacaoVeiculo;
import br.org.edu.ifrn.LojaCarro.services.VeiculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/veiculos")
@Tag(name = "Veículos", description = "Catálogo e gerenciamento de veículos")
public class VeiculoController {

    private final VeiculoService veiculoService;

    public VeiculoController(VeiculoService veiculoService) {
        this.veiculoService = veiculoService;
    }

    @GetMapping
    @Operation(summary = "Lista veículos com filtros opcionais",
            description = "Retorna apenas veículos disponíveis. O filtro 'situacao' só é aplicado para administradores.")
    public List<VeiculoResponse> listar(@RequestParam(required = false) String busca,
                                        @RequestParam(required = false) BigDecimal precoMin,
                                        @RequestParam(required = false) BigDecimal precoMax,
                                        @RequestParam(required = false) Integer anoMin,
                                        @RequestParam(required = false) Integer anoMax,
                                        @RequestParam(required = false) List<SituacaoVeiculo> situacao) {
        return veiculoService.buscar(new VeiculoFiltro(busca, precoMin, precoMax, anoMin, anoMax, situacao));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhes de um veículo")
    public VeiculoResponse buscarPorId(@PathVariable Long id) {
        return veiculoService.buscarPorId(id);
    }

    // multipart/form-data com duas partes: "dados" (JSON do VeiculoRequest) e "foto" (imagem até 5 MB)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cadastra um veículo com a fotografia principal (obrigatória)")
    public ResponseEntity<VeiculoResponse> cadastrar(@Valid @RequestPart("dados") VeiculoRequest dados,
                                                     @RequestPart("foto") MultipartFile foto) {
        VeiculoResponse criado = veiculoService.cadastrar(dados, foto);
        return ResponseEntity.created(URI.create("/api/veiculos/" + criado.id())).body(criado);
    }

    @GetMapping("/{id}/foto")
    @Operation(summary = "Fotografia principal do veículo")
    public ResponseEntity<Resource> foto(@PathVariable Long id) {
        VeiculoService.FotoVeiculo foto = veiculoService.buscarFoto(id);
        return ResponseEntity.ok()
                .contentType(foto.tipo())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(foto.arquivo());
    }

    @PutMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Substitui a fotografia principal do veículo")
    public VeiculoResponse trocarFoto(@PathVariable Long id, @RequestPart("foto") MultipartFile foto) {
        return veiculoService.trocarFoto(id, foto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de um veículo")
    public VeiculoResponse atualizar(@PathVariable Long id, @Valid @RequestBody VeiculoRequest dados) {
        return veiculoService.atualizar(id, dados);
    }

    @PatchMapping("/{id}/situacao")
    @Operation(summary = "Altera a situação (disponível, vendido ou inativo)")
    public VeiculoResponse alterarSituacao(@PathVariable Long id, @Valid @RequestBody SituacaoVeiculoRequest dados) {
        return veiculoService.alterarSituacao(id, dados.situacao());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativa o veículo (exclusão lógica)")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        veiculoService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
