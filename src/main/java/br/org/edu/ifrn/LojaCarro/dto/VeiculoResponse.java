package br.org.edu.ifrn.LojaCarro.dto;

import br.org.edu.ifrn.LojaCarro.model.Combustivel;
import br.org.edu.ifrn.LojaCarro.model.SituacaoVeiculo;
import br.org.edu.ifrn.LojaCarro.model.Transmissao;
import br.org.edu.ifrn.LojaCarro.model.Veiculo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VeiculoResponse(
        Long id,
        String marca,
        String modelo,
        Integer anoFabricacao,
        Integer anoModelo,
        String cor,
        Integer quilometragem,
        Combustivel combustivel,
        Transmissao transmissao,
        BigDecimal preco,
        String descricao,
        SituacaoVeiculo situacao,
        String fotoUrl,
        LocalDateTime dataCadastro
) {
    public static VeiculoResponse de(Veiculo v) {
        // O parâmetro "v" muda quando a foto é trocada, evitando que o navegador mostre a imagem antiga do cache
        String fotoUrl = v.getFotoPrincipal() == null ? null
                : "/api/veiculos/" + v.getId() + "/foto?v=" + v.getFotoPrincipal().substring(0, 8);
        return new VeiculoResponse(v.getId(), v.getMarca(), v.getModelo(), v.getAnoFabricacao(), v.getAnoModelo(),
                v.getCor(), v.getQuilometragem(), v.getCombustivel(), v.getTransmissao(), v.getPreco(),
                v.getDescricao(), v.getSituacao(), fotoUrl, v.getDataCadastro());
    }
}
