/*
 * CourtTime — seletor de PDF em portugues (melhoria progressiva).
 *
 * Sem este arquivo, o formulario usa o <input type="file"> nativo e funciona
 * normalmente. Com ele, o input nativo continua no HTML (focavel e enviado no
 * multipart), mas a interface mostra "Selecionar PDF", o nome e o tamanho do
 * arquivo escolhido e avisos em portugues.
 *
 * Os avisos abaixo sao so uma ajuda: o envio nunca e bloqueado pelo script,
 * e o servidor continua sendo quem valida o tipo, o conteudo e o limite de
 * 10 MB (as mensagens dele aparecem depois do envio).
 */
(function () {
    "use strict";
    var LIMITE = 10 * 1024 * 1024; // mesmo limite de DocumentoReservaService

    function tamanho(bytes) {
        if (bytes < 1024 * 1024) { return Math.max(1, Math.round(bytes / 1024)) + " KB"; }
        return (bytes / (1024 * 1024)).toFixed(1).replace(".", ",") + " MB";
    }

    function iniciar(caixa) {
        var input = caixa.querySelector('input[type="file"]');
        var estado = caixa.querySelector("[data-upload-estado]");
        var erro = caixa.querySelector("[data-upload-erro]");
        var rotulo = caixa.querySelector("[data-upload-rotulo]");
        var area = caixa.querySelector("[data-upload-caixa]");
        if (!input || !estado || !erro || !rotulo || !area) { return; }
        caixa.classList.add("upload--js");

        function mostrarErro(texto) {
            erro.textContent = texto;
            erro.hidden = !texto;
            if (texto) { input.setAttribute("aria-invalid", "true"); } else { input.removeAttribute("aria-invalid"); }
            area.classList.toggle("upload__caixa--erro", Boolean(texto));
        }

        function atualizar() {
            var arquivo = input.files && input.files[0];
            mostrarErro("");
            area.classList.remove("upload__caixa--selecionado");
            if (!arquivo) {
                estado.textContent = "Nenhum arquivo selecionado.";
                rotulo.textContent = "Selecionar PDF";
                return;
            }
            rotulo.textContent = "Trocar arquivo";
            estado.textContent = arquivo.name + " · " + tamanho(arquivo.size);
            if (!/\.pdf$/i.test(arquivo.name)) {
                mostrarErro("Selecione um arquivo no formato PDF (.pdf).");
            } else if (arquivo.size > LIMITE) {
                mostrarErro("O arquivo PDF deve possuir no máximo 10 MB.");
            } else {
                area.classList.add("upload__caixa--selecionado");
                estado.textContent = arquivo.name + " · " + tamanho(arquivo.size) + " · selecionado, ainda não enviado";
            }
        }

        // O envio nunca e bloqueado aqui: o servidor valida tipo, conteudo e tamanho.
        input.addEventListener("change", atualizar);
        atualizar();
    }

    document.addEventListener("DOMContentLoaded", function () {
        Array.prototype.forEach.call(document.querySelectorAll("[data-upload]"), iniciar);
    });
})();
