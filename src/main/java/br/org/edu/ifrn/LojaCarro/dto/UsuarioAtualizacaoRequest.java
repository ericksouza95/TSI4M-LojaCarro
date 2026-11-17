package br.org.edu.ifrn.LojaCarro.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Atualização de dados do usuário. Senha em branco mantém a senha atual.
public record UsuarioAtualizacaoRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres.")
        String email,

        @Pattern(regexp = "^$|^.{6,72}$", message = "A senha deve ter entre 6 e 72 caracteres.")
        String senha,

        @Pattern(regexp = "^$|^[0-9()+\\- ]{8,20}$", message = "Telefone inválido.")
        String telefone
) {
    public boolean alterouSenha() {
        return senha != null && !senha.isBlank();
    }
}
