/*
 * CourtTime — resumo do periodo escolhido na nova solicitacao.
 *
 * Melhoria progressiva: sem este arquivo, o cartao marcado (radio nativo
 * name="horarioInicio") ja indica a escolha e o formulario funciona igual.
 * Com ele, a frase acima do botao "Registrar solicitacao" repete o periodo
 * escolhido. Nao valida nem reserva nada: o servidor revalida data, grade,
 * bloqueios, conflitos e antecedencia no envio.
 */
(function () {
    "use strict";

    function iniciar() {
        var resumo = document.querySelector("[data-resumo-horario]");
        if (!resumo) { return; }
        var data = resumo.getAttribute("data-data-extenso") || "";
        var radios = document.querySelectorAll('input[type="radio"][name="horarioInicio"]');

        function atualizar() {
            var escolhido = document.querySelector('input[type="radio"][name="horarioInicio"]:checked');
            if (!escolhido) { return; }
            var rotulo = document.querySelector('label[for="' + escolhido.id + '"] .slot__hora');
            var periodo = rotulo ? rotulo.textContent.trim() : escolhido.value;
            resumo.textContent = "Período escolhido: " + periodo + (data ? ", " + data : "") +
                ". O envio registra uma solicitação preliminar, ainda sem autorização de uso.";
            resumo.classList.add("resumo-escolha--ativo");
        }

        Array.prototype.forEach.call(radios, function (radio) {
            radio.addEventListener("change", atualizar);
        });
        atualizar();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", iniciar);
    } else {
        iniciar();
    }
})();
