package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UsuarioControllerTest extends IntegracaoTestBase {

    @Test
    void administradorGerenciaUsuarios() throws Exception {
        String tokenAdmin = tokenDeAdmin();
        long id = criarCliente("Maria", "maria@email.com");

        // A senha nunca deve ser devolvida
        mockMvc.perform(get("/api/usuarios/" + id).with(token(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        mockMvc.perform(put("/api/usuarios/" + id).with(token(tokenAdmin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria Silva\",\"email\":\"maria@email.com\",\"senha\":\"\",\"telefone\":\"(84) 99999-0000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.telefone").value("(84) 99999-0000"));

        mockMvc.perform(get("/api/usuarios").with(token(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(delete("/api/usuarios/" + id).with(token(tokenAdmin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/usuarios/" + id).with(token(tokenAdmin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cadastroPublicoSempreCriaClienteComSenhaEmBcrypt() throws Exception {
        // Mesmo tentando enviar "perfil":"ADMIN", a conta criada é de cliente
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Esperto\",\"email\":\"Esperto@Email.com\",\"senha\":\"123456\",\"perfil\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("CLIENTE"))
                .andExpect(jsonPath("$.email").value("esperto@email.com"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        Usuario salvo = usuarioRepository.findByEmail("esperto@email.com").orElseThrow();
        assertThat(salvo.getSenha()).startsWith("$2").isNotEqualTo("123456");
    }

    @Test
    void clienteAcessaOsPropriosDadosMasNaoOsDeOutroCliente() throws Exception {
        long idAna = criarCliente("Ana", "ana@email.com");
        long idBruno = criarCliente("Bruno", "bruno@email.com");
        String tokenAna = login("ana@email.com");

        mockMvc.perform(get("/api/usuarios/" + idAna).with(token(tokenAna)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@email.com"));
        mockMvc.perform(put("/api/usuarios/" + idAna).with(token(tokenAna)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Paula\",\"email\":\"ana@email.com\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/usuarios/" + idBruno).with(token(tokenAna)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("Você só pode acessar os seus próprios dados."));
        mockMvc.perform(put("/api/usuarios/" + idBruno).with(token(tokenAna)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Invadido\",\"email\":\"bruno@email.com\"}"))
                .andExpect(status().isForbidden());
        assertThat(usuarioRepository.findById(idBruno)).get().extracting("nome").isEqualTo("Bruno");
    }

    @Test
    void clienteNaoAcessaEndpointsAdministrativos() throws Exception {
        long idOutro = criarCliente("Bruno", "bruno@email.com");
        String tokenCliente = tokenDeCliente("Ana", "ana@email.com");

        mockMvc.perform(get("/api/usuarios").with(token(tokenCliente)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("Você não tem permissão para realizar esta operação."));
        mockMvc.perform(delete("/api/usuarios/" + idOutro).with(token(tokenCliente))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/logs").with(token(tokenCliente))).andExpect(status().isForbidden());

        assertThat(usuarioRepository.existsById(idOutro)).isTrue();
        assertThat(logRepository.findAllByOrderByIdDesc().get(0).getAcao()).isEqualTo("ACESSO_PROIBIDO");
    }

    @Test
    void naoPermiteEmailDuplicadoMesmoComMaiusculas() throws Exception {
        criarCliente("Joao", "joao@email.com");

        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Outro\",\"email\":\"JOAO@email.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value(containsString("joao@email.com")));
    }

    @Test
    void validaDadosDoCadastro() throws Exception {
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"sem-arroba\",\"senha\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.email").value("E-mail inválido."))
                .andExpect(jsonPath("$.campos.senha").value("A senha deve ter entre 6 e 72 caracteres."));
    }

    @Test
    void administradorNaoExcluiASiMesmo() throws Exception {
        long idAdmin = criarAdmin("Admin", "admin@loja.com");
        String tokenAdmin = login("admin@loja.com");

        mockMvc.perform(delete("/api/usuarios/" + idAdmin).with(token(tokenAdmin)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("Não é possível excluir o usuário que está logado."));
    }

    @Test
    void naoExcluiUsuarioQueCadastrouVeiculos() throws Exception {
        long idAdmin1 = criarAdmin("Admin Um", "admin1@loja.com");
        cadastrarVeiculo(login("admin1@loja.com"), veiculoJson("Fiat", "Argo", 2022, 2022, "75000"));
        criarAdmin("Admin Dois", "admin2@loja.com");

        mockMvc.perform(delete("/api/usuarios/" + idAdmin1).with(token(login("admin2@loja.com"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value(containsString("possui veículos cadastrados")));
    }
}
