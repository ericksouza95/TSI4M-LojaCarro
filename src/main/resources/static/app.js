// Funções usadas por todas as telas do sistema (menos a de login)

// Monta o menu e manda para o login se ninguém estiver logado
async function montarMenu() {
    const resposta = await fetch("/usuario/logado");
    if (!resposta.ok) {
        window.location.href = "login.html";
        throw new Error("Usuário não logado");
    }
    const usuario = await resposta.json();

    const menu = document.getElementById("menu");
    menu.innerHTML =
        '<a href="index.html">Início</a>' +
        '<a href="carros.html">Carros</a>' +
        '<a href="usuarios.html">Usuários</a>' +
        '<a href="logs.html">Logs</a>' +
        ' | Logado como <b id="nomeLogado"></b> ' +
        '<button type="button" onclick="sair()">Sair</button>';
    document.getElementById("nomeLogado").textContent = usuario.nome;
    return usuario;
}

async function sair() {
    await fetch("/usuario/logout", { method: "POST" });
    window.location.href = "login.html";
}

// Faz a requisição e volta para o login se a sessão tiver expirado
async function api(url, opcoes) {
    const resposta = await fetch(url, opcoes);
    if (resposta.status === 401) {
        window.location.href = "login.html";
        throw new Error("Sessão expirada");
    }
    return resposta;
}

function enviarJson(url, metodo, dados) {
    return api(url, {
        method: metodo,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(dados)
    });
}

function mostrarMensagem(texto, erro) {
    const div = document.getElementById("mensagem");
    div.textContent = texto;
    div.className = "mensagem " + (erro ? "erro" : "sucesso");
}

async function lerErro(resposta) {
    try {
        const corpo = await resposta.json();
        return corpo.erro || "Erro " + resposta.status;
    } catch (e) {
        return "Erro " + resposta.status;
    }
}

function formatarData(data) {
    return data ? new Date(data).toLocaleString("pt-BR") : "";
}

// Adiciona uma linha na tabela com os valores informados (como texto)
function adicionarLinha(tabela, valores) {
    const linha = document.createElement("tr");
    valores.forEach(function (valor) {
        const celula = document.createElement("td");
        celula.textContent = valor;
        linha.appendChild(celula);
    });
    tabela.appendChild(linha);
    return linha;
}

// Adiciona a coluna com os botões Editar e Excluir
function adicionarAcoes(linha, editar, excluir) {
    const acoes = document.createElement("td");
    const btnEditar = document.createElement("button");
    btnEditar.textContent = "Editar";
    btnEditar.onclick = editar;
    const btnExcluir = document.createElement("button");
    btnExcluir.textContent = "Excluir";
    btnExcluir.onclick = excluir;
    acoes.appendChild(btnEditar);
    acoes.appendChild(document.createTextNode(" "));
    acoes.appendChild(btnExcluir);
    linha.appendChild(acoes);
}
