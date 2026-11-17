package br.org.edu.ifrn.LojaCarro.mensageria;

import br.org.edu.ifrn.LojaCarro.dto.LogResponse;
import br.org.edu.ifrn.LojaCarro.exception.ServicoIndisponivelException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

// Consulta os logs gravados pelo microserviço (chamada HTTP interna, autenticada pela chave X-Api-Key)
@Component
public class LogsClient {

    private final RestClient restClient;

    public LogsClient(RestClient.Builder builder,
                      @Value("${loja.logs.url}") String url,
                      @Value("${loja.logs.api-key}") String chave) {
        SimpleClientHttpRequestFactory tempos = new SimpleClientHttpRequestFactory();
        tempos.setConnectTimeout(3000);
        tempos.setReadTimeout(5000);
        this.restClient = builder
                .baseUrl(url)
                .requestFactory(tempos)
                .defaultHeader("X-Api-Key", chave)
                .build();
    }

    public List<LogResponse> listar(String busca) {
        try {
            List<LogResponse> logs = restClient.get()
                    .uri(uri -> uri.path("/logs").queryParam("limite", 1000).queryParamIfPresent("busca",
                            java.util.Optional.ofNullable(busca).filter(b -> !b.isBlank())).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return logs == null ? List.of() : logs;
        } catch (RestClientException e) {
            throw new ServicoIndisponivelException("O serviço de logs está indisponível no momento. Tente novamente em instantes.");
        }
    }
}
