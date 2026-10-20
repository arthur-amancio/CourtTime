/*
 * CourtTime — menu lateral no celular e no tablet (ate 1024 px).
 *
 * Melhoria progressiva: sem este arquivo, o botao "Menu" e um link para
 * #menu-principal e a barra lateral aparece abaixo do conteudo.
 * Com ele, o menu abre por cima da pagina e:
 *   - informa o estado em aria-expanded;
 *   - leva o foco ao primeiro link e mantem Tab/Shift+Tab dentro do menu;
 *   - deixa o resto da pagina inerte (inert) e sem rolagem;
 *   - fecha com Esc, com o botao Fechar, ao tocar fora ou ao escolher um link;
 *   - devolve o foco ao botao Menu.
 * Nenhuma regra de acesso depende deste script.
 */
(function () {
    "use strict";
    document.documentElement.classList.add("js");

    function iniciar() {
        var menu = document.getElementById("menu-principal");
        var botao = document.querySelector("[data-menu-botao]");
        var link = document.querySelector("[data-menu-link]");
        var fechar = document.querySelector("[data-menu-fechar]");
        if (!menu || !botao) {
            return;
        }
        var movel = window.matchMedia("(max-width: 1024px)");
        var sombra = document.createElement("div");
        sombra.className = "lateral-sombra";
        sombra.hidden = true;
        document.body.appendChild(sombra);

        botao.hidden = false;
        if (link) { link.hidden = true; }
        if (fechar) { fechar.hidden = false; }

        function fundo() {
            return Array.prototype.filter.call(document.body.children, function (el) {
                return el !== menu && el !== sombra && el.tagName !== "SCRIPT";
            });
        }
        function focaveis() {
            return Array.prototype.filter.call(
                menu.querySelectorAll("a[href], button:not([disabled]):not([hidden])"),
                function (el) { return el.offsetParent !== null; });
        }
        function aberto() { return menu.classList.contains("is-aberta"); }

        function abrir() {
            menu.classList.add("is-aberta");
            sombra.hidden = false;
            botao.setAttribute("aria-expanded", "true");
            menu.setAttribute("role", "dialog");
            menu.setAttribute("aria-modal", "true");
            fundo().forEach(function (el) { el.inert = true; });
            document.documentElement.classList.add("menu-aberto");
            var primeiro = menu.querySelector(".lateral__link");
            if (primeiro) { primeiro.focus(); }
        }

        function fecharMenu(devolverFoco) {
            if (!aberto()) { return; }
            menu.classList.remove("is-aberta");
            sombra.hidden = true;
            botao.setAttribute("aria-expanded", "false");
            menu.removeAttribute("role");
            menu.removeAttribute("aria-modal");
            fundo().forEach(function (el) { el.inert = false; });
            document.documentElement.classList.remove("menu-aberto");
            if (devolverFoco) { botao.focus(); }
        }

        botao.addEventListener("click", function () {
            if (aberto()) { fecharMenu(true); } else { abrir(); }
        });
        if (fechar) {
            fechar.addEventListener("click", function () { fecharMenu(true); });
        }
        sombra.addEventListener("click", function () { fecharMenu(true); });
        menu.addEventListener("click", function (evento) {
            if (evento.target.closest("a[href]")) { fecharMenu(false); }
        });
        document.addEventListener("keydown", function (evento) {
            if (!aberto()) { return; }
            if (evento.key === "Escape") {
                evento.preventDefault();
                fecharMenu(true);
                return;
            }
            if (evento.key === "Tab") {
                var lista = focaveis();
                if (!lista.length) { return; }
                var primeiro = lista[0];
                var ultimo = lista[lista.length - 1];
                if (evento.shiftKey && document.activeElement === primeiro) {
                    evento.preventDefault();
                    ultimo.focus();
                } else if (!evento.shiftKey && document.activeElement === ultimo) {
                    evento.preventDefault();
                    primeiro.focus();
                }
            }
        });
        var aoMudar = function () { if (!movel.matches) { fecharMenu(false); } };
        if (movel.addEventListener) {
            movel.addEventListener("change", aoMudar);
        } else if (movel.addListener) {
            movel.addListener(aoMudar);
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", iniciar);
    } else {
        iniciar();
    }
})();
