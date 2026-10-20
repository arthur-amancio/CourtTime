/* ==========================================================================
   CourtTime — comportamento do PROTÓTIPO VISUAL
   Este arquivo existe apenas para a demonstração navegável.
   Nada aqui autentica usuários, grava dados ou consulta o servidor.
   Na migração para Thymeleaf, a moldura (barra lateral, topo e rodapé)
   vira um fragmento `th:fragment` e os dados vêm dos controllers atuais.
   ========================================================================== */
(function () {
    "use strict";

    /* ---------- Ícones (desenho próprio, traço de 24 px) ---------------- */
    var ICONES = {
        quadra: '<rect x="3" y="5" width="18" height="14" rx="1.5"/><path d="M12 5v14"/><circle cx="12" cy="12" r="2.6"/><path d="M3 9.5h2.5v5H3M21 9.5h-2.5v5H21"/>',
        casa: '<path d="M4 11.5 12 5l8 6.5"/><path d="M6 10v9h12v-9"/><path d="M10 19v-5h4v5"/>',
        calendario: '<rect x="3.5" y="5" width="17" height="15" rx="2"/><path d="M3.5 9.5h17M8 3v4M16 3v4"/>',
        "calendario-mais": '<rect x="3.5" y="5" width="17" height="15" rx="2"/><path d="M3.5 9.5h17M8 3v4M16 3v4M12 12.5v5M9.5 15h5"/>',
        relogio: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
        lista: '<path d="M9 6.5h11M9 12h11M9 17.5h11"/><circle cx="4.75" cy="6.5" r=".9"/><circle cx="4.75" cy="12" r=".9"/><circle cx="4.75" cy="17.5" r=".9"/>',
        usuario: '<circle cx="12" cy="8.5" r="3.75"/><path d="M5 20c.8-3.6 3.6-5.5 7-5.5s6.2 1.9 7 5.5"/>',
        usuarios: '<circle cx="9" cy="8.5" r="3.4"/><path d="M3 19.5c.7-3.2 3.1-5 6-5s5.3 1.8 6 5"/><path d="M15.5 5.4a3.3 3.3 0 0 1 0 6.3M17.5 14.7c1.8.6 3 2.2 3.5 4.8"/>',
        documento: '<path d="M14 3.5H7a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-10z"/><path d="M14 3.5v5h5M8.5 13h7M8.5 16.5h5"/>',
        "documento-check": '<path d="M14 3.5H7a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-10z"/><path d="M14 3.5v5h5"/><path d="m9 14.5 2 2 4-4"/>',
        check: '<path d="m5 12.5 4.5 4.5L19 7.5"/>',
        "check-circulo": '<circle cx="12" cy="12" r="8.5"/><path d="m8.2 12.3 2.6 2.6 5-5.2"/>',
        x: '<path d="M6.5 6.5l11 11M17.5 6.5l-11 11"/>',
        "x-circulo": '<circle cx="12" cy="12" r="8.5"/><path d="m9 9 6 6M15 9l-6 6"/>',
        proibido: '<circle cx="12" cy="12" r="8.5"/><path d="m6 6 12 12"/>',
        alerta: '<path d="M12 4.2 2.8 19.5h18.4z"/><path d="M12 10v4.2M12 17h.01"/>',
        info: '<circle cx="12" cy="12" r="8.5"/><path d="M12 11v5.5M12 7.8h.01"/>',
        ampulheta: '<path d="M7 3.5h10M7 20.5h10M8 3.5c0 4.2 8 4.6 8 8.5s-8 4.3-8 8.5M16 3.5c0 2-1.8 3.2-4 4.6"/>',
        escudo: '<path d="M12 3.5 5 6v5.5c0 4.4 3 7.7 7 9 4-1.3 7-4.6 7-9V6z"/><path d="m9 12 2.2 2.2L15.2 10"/>',
        cadeado: '<rect x="5" y="10.5" width="14" height="10" rx="2"/><path d="M8 10.5V7.8a4 4 0 0 1 8 0v2.7"/>',
        sair: '<path d="M10 4.5H6a1.5 1.5 0 0 0-1.5 1.5v12A1.5 1.5 0 0 0 6 19.5h4"/><path d="M14 8l4 4-4 4M18 12H9.5"/>',
        menu: '<path d="M4 7h16M4 12h16M4 17h16"/>',
        "seta-dir": '<path d="M5 12h14M13 6l6 6-6 6"/>',
        "seta-esq": '<path d="M19 12H5M11 6l-6 6 6 6"/>',
        "chevron-dir": '<path d="m9.5 6 6 6-6 6"/>',
        "chevron-esq": '<path d="m14.5 6-6 6 6 6"/>',
        mais: '<path d="M12 5v14M5 12h14"/>',
        grade: '<rect x="3.5" y="4.5" width="17" height="15" rx="2"/><path d="M3.5 9.5h17M3.5 14.5h17M9.5 4.5v15M15 4.5v15"/>',
        upload: '<path d="M12 15.5V4.5M7.5 9 12 4.5 16.5 9"/><path d="M4.5 15.5v2.5a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2v-2.5"/>',
        download: '<path d="M12 4.5v11M7.5 11l4.5 4.5 4.5-4.5"/><path d="M4.5 15.5v2.5a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2v-2.5"/>',
        busca: '<circle cx="11" cy="11" r="6.5"/><path d="m16 16 4 4"/>',
        bandeja: '<path d="M4 13.5 6.5 5h11l2.5 8.5V19a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1z"/><path d="M4 13.5h4.5l1.5 2.5h4l1.5-2.5H20"/>',
        recibo: '<path d="M6 3.5h12v17l-3-1.8-3 1.8-3-1.8-3 1.8z"/><path d="M9 8.5h6M9 12h6M9 15.5h3.5"/>',
        editar: '<path d="M14.5 5.5 18.5 9.5 9 19H5v-4z"/><path d="M12.5 7.5l4 4"/>',
        lixeira: '<path d="M4.5 7h15M9.5 7V4.5h5V7M6.5 7l1 12.5h9l1-12.5"/>',
        chave: '<circle cx="8" cy="15" r="4"/><path d="m11 12 8.5-8.5M16 7l2.5 2.5M14 9l2 2"/>',
        olho: '<path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z"/><circle cx="12" cy="12" r="3"/>',
        bandeira: '<path d="M5 20.5V4.5M5 4.5h11l-2 4 2 4H5"/>',
        identidade: '<rect x="3" y="5.5" width="18" height="13" rx="2"/><circle cx="9" cy="11.5" r="2.2"/><path d="M5.8 16c.5-1.4 1.7-2.2 3.2-2.2s2.7.8 3.2 2.2M14.5 10h4M14.5 13.5h3"/>'
    };

    function icone(nome, classe) {
        return '<svg class="icone' + (classe ? " " + classe : "") + '" viewBox="0 0 24 24" aria-hidden="true" focusable="false">' +
            (ICONES[nome] || "") + "</svg>";
    }
    window.ctIcone = icone;

    function hidratarIcones(raiz) {
        (raiz || document).querySelectorAll("[data-icone]").forEach(function (el) {
            el.insertAdjacentHTML("afterbegin", icone(el.getAttribute("data-icone"), el.getAttribute("data-icone-classe")));
            el.removeAttribute("data-icone");
        });
    }

    /* ---------- Armazenamento opcional (só conveniência) ----------------- */
    function ler(chave) { try { return window.sessionStorage.getItem(chave); } catch (e) { return null; } }
    function gravar(chave, valor) { try { window.sessionStorage.setItem(chave, valor); } catch (e) { /* sem armazenamento */ } }

    var params = new URLSearchParams(window.location.search);

    /* ---------- Perfis e navegação --------------------------------------- */
    var PERFIS = {
        aluno: {
            rotulo: "Aluno", nome: "Lucas Andrade (fictício)", email: "aluno.ficticio@exemplo.test",
            menu: [
                ["inicio", "Início", "inicio.html?perfil=aluno", "casa"],
                ["quadra", "Consultar quadra", "quadra.html?perfil=aluno", "calendario"],
                ["nova", "Nova solicitação", "aluno-nova-solicitacao.html", "calendario-mais"],
                ["solicitacoes", "Minhas solicitações", "aluno-solicitacoes.html", "lista"]
            ]
        },
        secretaria: {
            rotulo: "Secretaria", nome: "Mariana Costa (fictícia)", email: "secretaria.ficticia@exemplo.test",
            menu: [
                ["inicio", "Início", "inicio.html?perfil=secretaria", "casa"],
                ["quadra", "Consultar quadra", "quadra.html?perfil=secretaria", "calendario"],
                ["documentos", "Conferência documental", "secretaria-documentos.html", "documento-check", 2],
                ["contribuicoes", "Contribuições presenciais", "secretaria-contribuicoes.html", "recibo", 1]
            ]
        },
        administrador: {
            rotulo: "Administrador", nome: "Prof. Ricardo Menezes (fictício)", email: "admin.ficticio@exemplo.test",
            menu: [
                ["inicio", "Início", "inicio.html?perfil=administrador", "casa"],
                ["painel", "Painel administrativo", "admin-painel.html", "grade"],
                ["analise", "Análise de solicitações", "admin-solicitacoes.html", "bandeja", 2],
                ["quadra", "Consultar quadra", "quadra.html?perfil=administrador", "calendario"],
                ["horarios", "Horários semanais", "admin-horarios.html", "relogio"],
                ["bloqueios", "Bloqueios", "admin-bloqueios.html", "proibido"],
                ["usuarios", "Usuários", "admin-usuarios.html", "usuarios"]
            ]
        },
        seguranca: {
            rotulo: "Segurança", nome: "José Carlos Pereira (fictício)", email: "seguranca.ficticio@exemplo.test",
            menu: [
                ["inicio", "Início", "inicio.html?perfil=seguranca", "casa"],
                ["quadra", "Consultar quadra", "quadra.html?perfil=seguranca", "calendario"],
                ["reservas", "Reservas autorizadas", "seguranca-reservas.html", "escudo"]
            ]
        }
    };

    function perfilAtual() {
        var corpo = document.body.getAttribute("data-perfil");
        if (corpo && corpo !== "variavel") { return corpo; }
        var p = params.get("perfil") || ler("ct-perfil") || "aluno";
        return PERFIS[p] ? p : "aluno";
    }

    function iniciais(nome) {
        return nome.replace(/\(.*\)/, "").trim().split(/\s+/).filter(function (p) { return p.length > 2 || /^[A-Z]/.test(p); })
            .slice(-2).map(function (p) { return p.charAt(0); }).join("").toUpperCase();
    }

    function montarMoldura() {
        var main = document.querySelector("main[data-moldura]");
        if (!main) { return; }
        var perfil = perfilAtual();
        gravar("ct-perfil", perfil);
        document.body.setAttribute("data-perfil-ativo", perfil);
        var dados = PERFIS[perfil];
        var pagina = document.body.getAttribute("data-pagina");

        var links = dados.menu.map(function (item) {
            var atual = item[0] === pagina ? ' aria-current="page"' : "";
            var contador = item[4] ? '<span class="contador" aria-label="' + item[4] + ' pendentes">' + item[4] + "</span>" : "";
            return '<a class="lateral__link" href="' + item[2] + '"' + atual + ">" + icone(item[3]) + "<span>" + item[1] + "</span>" + contador + "</a>";
        }).join("");

        var marca = '<a class="marca marca--clara" href="inicio.html?perfil=' + perfil + '">' +
            '<span class="marca__simbolo">' + icone("quadra") + "</span>" +
            '<span class="marca__texto"><span class="marca__nome">CourtTime</span><span class="marca__escola">ETEC Prof. Armando José Farinazzo</span></span></a>';

        var lateral = document.createElement("aside");
        lateral.className = "lateral";
        lateral.id = "menu-principal";
        lateral.setAttribute("aria-label", "Menu principal");
        lateral.innerHTML = '<div class="lateral__topo">' + marca +
            '<button type="button" class="lateral__fechar" data-fechar-menu>' + icone("x") + '<span class="visually-hidden">Fechar menu</span></button></div>' +
            '<nav class="lateral__secao" aria-label="Navegação do perfil ' + dados.rotulo + '">' +
            '<p class="lateral__titulo">' + dados.rotulo + "</p>" + links + "</nav>" +
            '<div class="lateral__usuario"><div><strong>' + dados.nome + "</strong><span>" + dados.email + "</span></div>" +
            '<a class="lateral__sair" href="login.html">' + icone("sair", "icone--sm") + "Sair</a></div>";

        var topoMovel = document.createElement("header");
        topoMovel.className = "topo-movel";
        topoMovel.innerHTML = '<a class="marca marca--clara" href="inicio.html?perfil=' + perfil + '"><span class="marca__simbolo">' + icone("quadra") +
            '</span><span class="marca__texto"><span class="marca__nome">CourtTime</span></span></a>' +
            '<button type="button" class="topo-movel__menu" aria-controls="menu-principal" aria-expanded="false">' + icone("menu") + "Menu</button>";

        var sombra = document.createElement("div");
        sombra.className = "sombra-menu";
        sombra.hidden = true;

        var rodape = document.createElement("footer");
        rodape.className = "rodape rodape--app";
        rodape.innerHTML = '<div class="rodape__inner"><p><strong>CourtTime</strong> · ETEC Prof. Armando José Farinazzo · Fernandópolis/SP</p>' +
            "<p>Protótipo visual com dados fictícios · TCC 2026</p></div>";

        var app = document.createElement("div");
        app.className = "app";
        var conteudo = document.createElement("div");
        conteudo.className = "conteudo";
        main.parentNode.insertBefore(app, main);
        app.appendChild(lateral);
        app.appendChild(conteudo);
        conteudo.appendChild(topoMovel);
        conteudo.appendChild(main);
        conteudo.appendChild(rodape);
        document.body.appendChild(sombra);

        /* Menu do celular e do tablet (até 1024 px):
           - abre com aria-expanded="true" e leva o foco ao primeiro link;
           - o restante da página fica inerte (atributo inert) e sem rolagem;
           - Tab e Shift+Tab circulam somente dentro do menu;
           - Esc, o botão Fechar, a sombra ou a escolha de um link fecham o menu;
           - ao fechar, o foco volta ao botão Menu. */
        var botao = topoMovel.querySelector(".topo-movel__menu");
        var MOVEL = window.matchMedia("(max-width: 1024px)");
        function fundo() {
            return [conteudo, document.querySelector(".faixa-prototipo"), document.querySelector(".skip-link")].filter(Boolean);
        }
        function focaveis() {
            return Array.prototype.filter.call(lateral.querySelectorAll("a[href], button:not([disabled])"), function (el) {
                return el.offsetParent !== null;
            });
        }
        function aberto() { return lateral.classList.contains("is-aberta"); }
        function abrir() {
            lateral.classList.add("is-aberta"); sombra.hidden = false;
            botao.setAttribute("aria-expanded", "true");
            lateral.setAttribute("role", "dialog"); lateral.setAttribute("aria-modal", "true");
            fundo().forEach(function (el) { el.inert = true; el.setAttribute("aria-hidden", "true"); });
            document.documentElement.classList.add("menu-aberto");
            var primeiro = lateral.querySelector(".lateral__link"); if (primeiro) { primeiro.focus(); }
        }
        function fechar(devolverFoco) {
            if (!aberto()) { return; }
            lateral.classList.remove("is-aberta"); sombra.hidden = true;
            botao.setAttribute("aria-expanded", "false");
            lateral.removeAttribute("role"); lateral.removeAttribute("aria-modal");
            fundo().forEach(function (el) { el.inert = false; el.removeAttribute("aria-hidden"); });
            document.documentElement.classList.remove("menu-aberto");
            if (devolverFoco) { botao.focus(); }
        }
        botao.addEventListener("click", function () { if (aberto()) { fechar(true); } else { abrir(); } });
        lateral.querySelector("[data-fechar-menu]").addEventListener("click", function () { fechar(true); });
        sombra.addEventListener("click", function () { fechar(true); });
        lateral.addEventListener("click", function (e) {
            if (e.target.closest("a[href]") && aberto()) { fechar(false); }
        });
        document.addEventListener("keydown", function (e) {
            if (!aberto()) { return; }
            if (e.key === "Escape") { e.preventDefault(); fechar(true); return; }
            if (e.key === "Tab") {
                var lista = focaveis(); if (!lista.length) { return; }
                var primeiro = lista[0], ultimo = lista[lista.length - 1];
                if (e.shiftKey && document.activeElement === primeiro) { e.preventDefault(); ultimo.focus(); }
                else if (!e.shiftKey && document.activeElement === ultimo) { e.preventDefault(); primeiro.focus(); }
            }
        });
        var aoMudarLargura = function () { if (!MOVEL.matches) { fechar(false); } };
        if (MOVEL.addEventListener) { MOVEL.addEventListener("change", aoMudarLargura); } else if (MOVEL.addListener) { MOVEL.addListener(aoMudarLargura); }

        document.querySelectorAll("[data-somente-perfil]").forEach(function (el) {
            var lista = el.getAttribute("data-somente-perfil").split(/\s+/);
            if (lista.indexOf(perfil) === -1) { el.remove(); }
        });
        document.querySelectorAll("[data-nome-usuario]").forEach(function (el) {
            el.textContent = dados.nome.replace(/\s*\(.*\)/, "").replace(/^Prof\.\s*/, "").split(" ")[0];
        });
        document.querySelectorAll("[data-rotulo-perfil]").forEach(function (el) { el.textContent = dados.rotulo; });
        document.querySelectorAll("[data-iniciais]").forEach(function (el) { el.textContent = iniciais(el.getAttribute("data-iniciais")); });
    }

    function montarFaixa() {
        if (document.querySelector(".faixa-prototipo")) { return; }
        var faixa = document.createElement("div");
        faixa.className = "faixa-prototipo";
        faixa.setAttribute("role", "note");
        faixa.innerHTML = icone("info", "icone--sm") +
            '<span>Protótipo visual do CourtTime — dados fictícios, sem autenticação nem gravação. <a href="telas.html">Ver todas as telas</a></span>';
        document.body.insertBefore(faixa, document.body.firstChild);
        var pular = document.createElement("a");
        pular.className = "skip-link"; pular.href = "#conteudo"; pular.textContent = "Pular para o conteúdo";
        document.body.insertBefore(pular, document.body.firstChild);
    }

    /* ---------- Agenda fictícia ------------------------------------------ */
    // Data de referência do protótipo: terça-feira, 20/10/2026.
    var HOJE = new Date(2026, 9, 20);
    var MESES = ["janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"];
    var DIAS = ["domingo", "segunda-feira", "terça-feira", "quarta-feira", "quinta-feira", "sexta-feira", "sábado"];
    var DIAS_CURTOS = ["dom", "seg", "ter", "qua", "qui", "sex", "sáb"];

    // Grade semanal FICTÍCIA (não representa o horário oficial da ETEC)
    var GRADE = {
        1: [[8, 12], [13, 18]], 2: [[8, 12], [13, 18]], 3: [[8, 12], [13, 18]],
        4: [[8, 12], [13, 18]], 5: [[8, 12], [13, 17]], 6: [[8, 12]], 0: []
    };
    // Bloqueios e reservas FICTÍCIOS
    var BLOQUEIOS = { "2026-10-28": [[8, 18]], "2026-10-23": [[13, 15]] };
    var RESERVAS = { "2026-10-20": [16], "2026-10-21": [9, 15], "2026-10-22": [16], "2026-10-26": [10, 14], "2026-10-27": [8], "2026-10-29": [13, 14, 15] };

    function chave(d) { return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0"); }
    function mesmaData(a, b) { return chave(a) === chave(b); }
    function h(n) { return String(n).padStart(2, "0") + ":00"; }
    function dataPorExtenso(d) { return DIAS[d.getDay()] + ", " + d.getDate() + " de " + MESES[d.getMonth()]; }
    function dataCurta(d) { return String(d.getDate()).padStart(2, "0") + "/" + String(d.getMonth() + 1).padStart(2, "0") + "/" + d.getFullYear(); }

    function periodosDoDia(d) {
        var lista = [];
        (GRADE[d.getDay()] || []).forEach(function (faixa) {
            for (var hora = faixa[0]; hora < faixa[1]; hora++) {
                var situacao = "disponivel";
                (BLOQUEIOS[chave(d)] || []).forEach(function (b) { if (hora >= b[0] && hora < b[1]) { situacao = "indisponivel"; } });
                if (situacao === "disponivel" && (RESERVAS[chave(d)] || []).indexOf(hora) !== -1) { situacao = "reservado"; }
                lista.push({ inicio: hora, fim: hora + 1, situacao: situacao });
            }
        });
        return lista;
    }

    function iniciarAgenda(raiz) {
        var modo = raiz.getAttribute("data-agenda"); // "consulta" ou "solicitacao"
        var antecedencia = modo === "solicitacao" ? 1 : 0;
        var minimo = new Date(HOJE); minimo.setDate(minimo.getDate() + antecedencia);
        var selecionada = new Date(minimo);
        while ((GRADE[selecionada.getDay()] || []).length === 0) { selecionada.setDate(selecionada.getDate() + 1); }
        var mesVisivel = new Date(selecionada.getFullYear(), selecionada.getMonth(), 1);
        var horarioEscolhido = null;
        var quadraAtiva = true;

        var grade = raiz.querySelector("[data-calendario-grade]");
        var titulo = raiz.querySelector("[data-calendario-mes]");
        var faixa = raiz.querySelector("[data-faixa-dias]");
        var slotsEl = raiz.querySelector("[data-slots]");
        var dataTitulo = raiz.querySelector("[data-data-titulo]");
        var estadoQuadra = raiz.querySelector("[data-estado-quadra]");
        var anterior = raiz.querySelector("[data-mes-anterior]");
        var proximo = raiz.querySelector("[data-mes-proximo]");
        var simular = document.querySelector("[data-simular-inativa]");

        function anunciar(msg) {
            var viva = raiz.querySelector("[data-anuncio]");
            if (viva) { viva.textContent = msg; }
        }

        function desenharMes() {
            titulo.textContent = MESES[mesVisivel.getMonth()] + " de " + mesVisivel.getFullYear();
            var primeiro = new Date(mesVisivel);
            var inicio = new Date(primeiro); inicio.setDate(1 - primeiro.getDay());
            var html = "<thead><tr>" + DIAS_CURTOS.map(function (d, i) {
                return '<th scope="col"><abbr title="' + DIAS[i] + '">' + d.charAt(0).toUpperCase() + "</abbr></th>";
            }).join("") + "</tr></thead><tbody>";
            var cursor = new Date(inicio);
            for (var semana = 0; semana < 6; semana++) {
                html += "<tr>";
                for (var dia = 0; dia < 7; dia++) {
                    var foraDoMes = cursor.getMonth() !== mesVisivel.getMonth();
                    if (foraDoMes) { html += "<td></td>"; cursor.setDate(cursor.getDate() + 1); continue; }
                    var funciona = (GRADE[cursor.getDay()] || []).length > 0;
                    var passado = cursor < minimo && !mesmaData(cursor, minimo);
                    var classes = ["dia"];
                    if (funciona && !passado) { classes.push("dia--funciona"); }
                    if (!funciona) { classes.push("dia--sem-funcionamento"); }
                    if (mesmaData(cursor, HOJE)) { classes.push("dia--hoje"); }
                    var rotulo = dataPorExtenso(cursor) + (funciona ? "" : ", sem funcionamento cadastrado") +
                        (passado ? (modo === "solicitacao" ? ", fora do prazo de antecedência" : ", data passada") : "");
                    html += '<td><button type="button" class="' + classes.join(" ") + '" data-data="' + chave(cursor) + '"' +
                        (passado ? " disabled" : "") + ' aria-pressed="' + mesmaData(cursor, selecionada) + '" aria-label="' + rotulo + '">' +
                        cursor.getDate() + "</button></td>";
                    cursor.setDate(cursor.getDate() + 1);
                }
                html += "</tr>";
                if (cursor.getMonth() !== mesVisivel.getMonth() && semana >= 3) { break; }
            }
            grade.innerHTML = html + "</tbody>";
            anterior.disabled = mesVisivel <= new Date(minimo.getFullYear(), minimo.getMonth(), 1);
        }

        /* Faixa de dias do celular em duas variantes:
           A — só a grade semanal (dado que a aplicação já possui);
           B — quantidade de períodos livres por dia (exige dado novo). */
        function varianteCalendario() {
            var v = params.get("calendario") || ler("ct-calendario") || "a";
            return v === "b" ? "b" : "a";
        }

        function desenharFaixa() {
            if (!faixa) { return; }
            var variante = varianteCalendario();
            faixa.setAttribute("data-variante", variante);
            var html = "";
            var d = new Date(HOJE);
            for (var i = 0; i < 21; i++) {
                var funciona = (GRADE[d.getDay()] || []).length > 0;
                var passado = d < minimo && !mesmaData(d, minimo);
                var classes = ["dia-chip"];
                var info, falado;
                if (variante === "a") {
                    if (passado) { info = ""; falado = modo === "solicitacao" ? "fora do prazo de antecedência" : "data passada"; }
                    else if (funciona) { info = '<i class="dia-chip__ponto" aria-hidden="true"></i>'; falado = "com funcionamento cadastrado"; classes.push("dia-chip--funciona"); }
                    else { info = "fechado"; falado = "sem funcionamento cadastrado"; classes.push("dia-chip--fechado"); }
                } else {
                    var periodosDia = periodosDoDia(d);
                    var livres = periodosDia.filter(function (p) { return p.situacao === "disponivel"; }).length;
                    var todosBloqueados = periodosDia.length > 0 && periodosDia.every(function (p) { return p.situacao === "indisponivel"; });
                    if (passado) { info = "—"; falado = modo === "solicitacao" ? "fora do prazo de antecedência" : "data passada"; }
                    else if (!quadraAtiva) { info = "inativa"; falado = "quadra inativa"; classes.push("dia-chip--fechado"); }
                    else if (!funciona) { info = "fechado"; falado = "sem funcionamento cadastrado"; classes.push("dia-chip--fechado"); }
                    else if (todosBloqueados) { info = "bloqueado"; falado = "dia bloqueado"; classes.push("dia-chip--bloqueado"); }
                    else if (!livres) { info = "sem vagas"; falado = "nenhum período livre"; classes.push("dia-chip--lotado"); }
                    else { info = livres + (livres === 1 ? " livre" : " livres"); falado = info; classes.push(livres <= 2 ? "dia-chip--poucos" : "dia-chip--livres"); }
                }
                html += '<button type="button" class="' + classes.join(" ") + '" data-data="' + chave(d) + '"' + (passado ? " disabled" : "") +
                    ' aria-pressed="' + mesmaData(d, selecionada) + '" aria-label="' + dataPorExtenso(d) + ", " + falado + '">' +
                    '<small aria-hidden="true">' + DIAS_CURTOS[d.getDay()] + '</small><strong aria-hidden="true">' + d.getDate() + '</strong><span aria-hidden="true">' + info + "</span></button>";
                d.setDate(d.getDate() + 1);
            }
            faixa.innerHTML = html;
            var ativo = faixa.querySelector('[aria-pressed="true"]');
            if (ativo && faixa.scrollWidth > faixa.clientWidth) {
                faixa.scrollLeft = Math.max(0, ativo.offsetLeft - faixa.offsetLeft - 8);
            }
        }

        function desenharSlots() {
            dataTitulo.textContent = dataPorExtenso(selecionada);
            if (estadoQuadra) {
                estadoQuadra.innerHTML = quadraAtiva
                    ? '<span class="badge badge--aprovada">' + icone("check-circulo") + "Quadra ativa no cadastro</span><small>Estar ativa não garante horário livre: confira cada período.</small>"
                    : '<span class="badge badge--cancelada">' + icone("proibido") + "Quadra inativa</span><small>Nenhum período aceita solicitações enquanto a quadra estiver inativa.</small>";
            }
            var periodos = periodosDoDia(selecionada);
            if (!quadraAtiva) {
                slotsEl.innerHTML = '<div class="alerta alerta--neutro">' + icone("proibido") + '<div><strong class="alerta__titulo">A quadra está inativa</strong><p>A quadra está inativa. Não há horários disponíveis para a data selecionada.</p></div></div>';
                return;
            }
            if (!periodos.length) {
                slotsEl.innerHTML = '<div class="vazio"><span class="vazio__icone">' + icone("calendario", "icone--lg") + "</span><strong>Sem funcionamento nesta data</strong>" +
                    "<p>Não há horários de funcionamento cadastrados para " + DIAS[selecionada.getDay()] + ". Escolha outra data no calendário.</p></div>";
                return;
            }
            var turnos = [["Manhã", periodos.filter(function (p) { return p.inicio < 12; })], ["Tarde", periodos.filter(function (p) { return p.inicio >= 12 && p.inicio < 18; })], ["Noite", periodos.filter(function (p) { return p.inicio >= 18; })]];
            var html = "";
            var rotulos = { disponivel: ["Disponível", "check"], reservado: ["Reservado", "cadeado"], indisponivel: ["Indisponível", "proibido"] };
            turnos.forEach(function (t) {
                if (!t[1].length) { return; }
                html += '<section class="turno" aria-label="' + t[0] + '"><h3 class="turno__titulo">' + t[0] + '</h3><ul class="slots">';
                t[1].forEach(function (p) {
                    var r = rotulos[p.situacao];
                    var hora = h(p.inicio) + " – " + h(p.fim);
                    var selecionado = horarioEscolhido && horarioEscolhido.data === chave(selecionada) && horarioEscolhido.inicio === p.inicio;
                    var livre = p.situacao === "disponivel";
                    if (modo === "consulta") {
                        html += '<li><div class="slot slot--estatico slot--' + p.situacao + '" role="group" aria-label="' + hora + ", " + r[0] + '">' +
                            '<span class="slot__hora">' + hora + '</span><span class="slot__estado">' + icone(r[1]) + r[0] + "</span></div></li>";
                    } else {
                        html += '<li><button type="button" class="slot slot--' + p.situacao + '" data-inicio="' + p.inicio + '"' +
                            (livre ? ' aria-pressed="' + !!selecionado + '"' : ' aria-disabled="true"') +
                            ' aria-label="' + hora + ", " + (selecionado ? "selecionado" : r[0]) + '">' +
                            '<span class="slot__hora">' + hora + '</span><span class="slot__estado">' + icone(selecionado ? "check-circulo" : r[1]) + (selecionado ? "Selecionado" : r[0]) + "</span></button></li>";
                    }
                });
                html += "</ul></section>";
            });
            slotsEl.innerHTML = html;
        }

        function atualizarResumo() {
            document.querySelectorAll("[data-resumo-vazio]").forEach(function (el) { el.hidden = !!horarioEscolhido; });
            document.querySelectorAll("[data-resumo-escolha]").forEach(function (el) { el.hidden = !horarioEscolhido; });
            if (horarioEscolhido) {
                var d = horarioEscolhido.dataObj;
                document.querySelectorAll("[data-resumo-data]").forEach(function (el) { el.textContent = dataPorExtenso(d); });
                document.querySelectorAll("[data-resumo-hora]").forEach(function (el) { el.textContent = h(horarioEscolhido.inicio) + " – " + h(horarioEscolhido.inicio + 1); });
            }
            document.querySelectorAll("[data-requer-horario]").forEach(function (b) {
                if (horarioEscolhido) { b.removeAttribute("aria-disabled"); } else { b.setAttribute("aria-disabled", "true"); }
            });
            document.querySelectorAll("[data-movel-resumo]").forEach(function (el) {
                el.innerHTML = horarioEscolhido ? "<span>" + dataCurta(horarioEscolhido.dataObj) + "</span><strong>" + h(horarioEscolhido.inicio) + " – " + h(horarioEscolhido.inicio + 1) + "</strong>"
                    : "<span>Nenhum período selecionado</span><strong>Escolha um horário livre</strong>";
            });
        }

        function selecionarData(valor) {
            var partes = valor.split("-");
            selecionada = new Date(+partes[0], +partes[1] - 1, +partes[2]);
            mesVisivel = new Date(selecionada.getFullYear(), selecionada.getMonth(), 1);
            desenharMes(); desenharFaixa(); desenharSlots();
            anunciar("Horários de " + dataPorExtenso(selecionada) + " carregados.");
        }

        raiz.addEventListener("click", function (e) {
            var dia = e.target.closest("[data-data]");
            if (dia && !dia.disabled) {
                selecionarData(dia.getAttribute("data-data"));
                var foco = raiz.querySelector('[data-data="' + dia.getAttribute("data-data") + '"].' + (dia.classList.contains("dia") ? "dia" : "dia-chip"));
                if (foco) { foco.focus(); }
                return;
            }
            var slot = e.target.closest(".slot[data-inicio]");
            if (slot) {
                if (slot.getAttribute("aria-disabled") === "true") {
                    anunciar("Este período não está disponível para solicitação.");
                    return;
                }
                var inicio = +slot.getAttribute("data-inicio");
                if (horarioEscolhido && horarioEscolhido.inicio === inicio && horarioEscolhido.data === chave(selecionada)) {
                    horarioEscolhido = null;
                } else {
                    horarioEscolhido = { data: chave(selecionada), dataObj: new Date(selecionada), inicio: inicio };
                }
                desenharSlots(); atualizarResumo();
                var novo = slotsEl.querySelector('[data-inicio="' + inicio + '"]'); if (novo) { novo.focus(); }
                anunciar(horarioEscolhido ? "Período " + h(inicio) + " a " + h(inicio + 1) + " selecionado." : "Seleção removida.");
            }
        });
        grade.addEventListener("keydown", function (e) {
            var atual = e.target.closest(".dia");
            if (!atual) { return; }
            var passos = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7 };
            if (!(e.key in passos)) { return; }
            e.preventDefault();
            var p = atual.getAttribute("data-data").split("-");
            var alvo = new Date(+p[0], +p[1] - 1, +p[2] + passos[e.key]);
            if (alvo < minimo && !mesmaData(alvo, minimo)) { return; }
            if (alvo.getMonth() !== mesVisivel.getMonth()) { mesVisivel = new Date(alvo.getFullYear(), alvo.getMonth(), 1); desenharMes(); }
            var botaoAlvo = grade.querySelector('[data-data="' + chave(alvo) + '"]');
            if (botaoAlvo) { botaoAlvo.focus(); }
        });
        if (faixa) {
            faixa.addEventListener("keydown", function (e) {
                var atual = e.target.closest(".dia-chip");
                if (!atual || (e.key !== "ArrowLeft" && e.key !== "ArrowRight")) { return; }
                e.preventDefault();
                var botoes = Array.prototype.filter.call(faixa.querySelectorAll(".dia-chip"), function (b) { return !b.disabled; });
                var i = botoes.indexOf(atual) + (e.key === "ArrowRight" ? 1 : -1);
                if (botoes[i]) { botoes[i].focus(); }
            });
        }
        document.querySelectorAll("[data-variante-calendario]").forEach(function (grupo) {
            grupo.querySelectorAll("input").forEach(function (r) {
                r.checked = r.value === varianteCalendario();
                r.addEventListener("change", function () {
                    gravar("ct-calendario", r.value);
                    params.set("calendario", r.value);
                    if (window.history && window.history.replaceState) { window.history.replaceState(null, "", "?" + params.toString()); }
                    desenharFaixa();
                    anunciar("Calendário do celular na opção " + r.value.toUpperCase() + ".");
                });
            });
        });

        // Barra de resumo do celular: some quando o painel de envio está visível,
        // para nunca cobrir o campo de finalidade nem os botões.
        var barra = document.querySelector(".barra-acao-movel");
        var painelEnvio = document.querySelector("[data-painel-envio]");
        if (barra && painelEnvio && "IntersectionObserver" in window) {
            new IntersectionObserver(function (entradas) {
                barra.classList.toggle("is-oculta", entradas[0].isIntersecting);
            }, { threshold: 0 }).observe(painelEnvio);
        }

        anterior.addEventListener("click", function () { mesVisivel.setMonth(mesVisivel.getMonth() - 1); desenharMes(); });
        proximo.addEventListener("click", function () { mesVisivel.setMonth(mesVisivel.getMonth() + 1); desenharMes(); });
        if (simular) {
            simular.addEventListener("change", function () {
                quadraAtiva = !simular.checked; horarioEscolhido = null; desenharFaixa(); desenharSlots(); atualizarResumo();
            });
        }

        var formulario = document.querySelector("[data-form-solicitacao]");
        if (formulario) {
            formulario.addEventListener("submit", function (e) {
                e.preventDefault();
                var erro = formulario.querySelector("[data-erro-horario]");
                if (!horarioEscolhido) {
                    erro.hidden = false;
                    erro.focus();
                    return;
                }
                erro.hidden = true;
                var finalidade = formulario.querySelector("#finalidade");
                if (!finalidade.value.trim()) {
                    finalidade.closest(".campo").classList.add("campo--erro");
                    finalidade.setAttribute("aria-invalid", "true");
                    formulario.querySelector("[data-erro-finalidade]").hidden = false;
                    finalidade.focus();
                    return;
                }
                var botao = formulario.querySelector('button[type="submit"]');
                botao.classList.add("is-loading");
                botao.setAttribute("aria-busy", "true");
                window.setTimeout(function () { window.location.href = "aluno-solicitacao.html?estado=documentacao&nova=1"; }, 650);
            });
        }

        desenharMes(); desenharFaixa(); desenharSlots(); atualizarResumo();
    }

    /* ---------- Detalhe da solicitação (aluno) ---------------------------
       Separa três coisas que não devem ser confundidas:
       - status gravado no banco (StatusReserva);
       - situação apresentada (SituacaoSolicitacaoService: título + explicação);
       - etapas do processo (apenas orientação visual, não são gravadas). */
    var ETAPAS = ["Solicitação registrada", "Participantes e PDF", "Conferência da secretaria", "Contribuição presencial", "Decisão administrativa"];

    // Eventos com data já gravada no banco (fictícios). "exibido" indica se a tela atual do aluno já mostra a data.
    var EVENTOS = {
        criada:      ["Solicitação registrada", "19/10/2026 07:56", "por você"],
        v1:          ["Versão 1 do PDF enviada", "19/10/2026 18:20", "por você"],
        v1devolvida: ["Versão 1 devolvida para correção", "20/10/2026 15:20", "secretaria", "aviso"],
        v2:          ["Versão 2 do PDF enviada", "21/10/2026 09:12", "por você"],
        v2validada:  ["Versão 2 validada", "21/10/2026 14:02", "secretaria"],
        contrib:     ["Contribuição presencial registrada", "21/10/2026 16:40", "secretaria"],
        aprovada:    ["Aprovada", "22/10/2026 08:05", "administração"],
        rejeitada:   ["Rejeitada, com motivo", "22/10/2026 08:05", "administração", "perigo"],
        cancelada:   ["Autorização cancelada, com motivo", "24/10/2026 11:30", "administração", "neutro"]
    };

    var DOC_V1_AGUARDANDO = { versao: 1, nome: "autorizacao-handebol.pdf", envio: "19/10/2026 18:20", situacao: ["documentacao", "ampulheta", "Aguardando conferência"] };
    var DOC_V1_DEVOLVIDA = { versao: 1, nome: "autorizacao-handebol.pdf", envio: "19/10/2026 18:20", situacao: ["documentacao", "seta-esq", "Devolvido para correção"],
        justificativa: "Faltam as assinaturas de dois participantes na página 2. (texto fictício)" };
    var DOC_V2_VALIDADA = { versao: 2, nome: "autorizacao-handebol-v2.pdf", envio: "21/10/2026 09:12", situacao: ["aprovada", "check", "Validado"] };

    var ESTADOS = {
        documentacao: { status: "PENDENTE_DOCUMENTACAO", badge: ["documentacao", "documento", "Pendente de documentação"], etapa: 1,
            msg: ["aviso", "alerta", "Pendente de documentação", "Revise os participantes e envie o PDF preparado e assinado fora do sistema."],
            docs: [], envio: true, editarParticipantes: true, eventos: ["criada"] },
        conferencia: { status: "PENDENTE_DOCUMENTACAO", badge: ["documentacao", "ampulheta", "Documento aguardando conferência"], etapa: 2,
            msg: ["info", "ampulheta", "Documento aguardando conferência", "A secretaria ainda precisa conferir a versão mais recente enviada."],
            docs: [DOC_V1_AGUARDANDO], envio: false, semEnvio: "Um novo envio só será liberado se a secretaria devolver a versão mais recente para correção.",
            eventos: ["v1", "criada"] },
        devolvida: { status: "PENDENTE_DOCUMENTACAO", badge: ["documentacao", "seta-esq", "Documento devolvido para correção"], etapa: 1, retorno: true,
            msg: ["aviso", "alerta", "Documento devolvido para correção", "Consulte a justificativa, corrija o documento fora do sistema e envie uma nova versão."],
            docs: [DOC_V1_DEVOLVIDA], envio: true, editarParticipantes: true, eventos: ["v1devolvida", "v1", "criada"] },
        contribuicao: { status: "PENDENTE_DOCUMENTACAO", badge: ["documentacao", "recibo", "Contribuição pendente"], etapa: 3,
            msg: ["info", "recibo", "Contribuição pendente", "O documento foi conferido. A secretaria ainda precisa registrar a contribuição presencial."],
            docs: [DOC_V2_VALIDADA, DOC_V1_DEVOLVIDA], envio: false, semEnvio: "O documento mais recente já foi validado. Não é preciso enviar outra versão.",
            eventos: ["v2validada", "v2", "v1devolvida", "v1", "criada"] },
        analise: { status: "PENDENTE_ANALISE", badge: ["analise", "ampulheta", "Pronta para análise administrativa"], etapa: 4,
            msg: ["info", "ampulheta", "Pronta para análise administrativa", "Documento e contribuição foram registrados. A solicitação aguarda a decisão da administração."],
            docs: [DOC_V2_VALIDADA, DOC_V1_DEVOLVIDA], envio: false, semEnvio: "Novos envios não são permitidos depois que a solicitação avança para análise.",
            eventos: ["contrib", "v2validada", "v2", "v1devolvida", "v1", "criada"] },
        aprovada: { status: "APROVADA", badge: ["aprovada", "check-circulo", "Aprovada"], etapa: 5, final: ["concluida", "check", "Aprovada"],
            msg: ["sucesso", "check-circulo", "Aprovada", "A utilização da quadra foi autorizada para a data e o horário informados."],
            docs: [DOC_V2_VALIDADA, DOC_V1_DEVOLVIDA], envio: false, semEnvio: "Novos envios não são permitidos depois que a solicitação avança para análise.",
            eventos: ["aprovada", "contrib", "v2validada", "v2", "v1devolvida", "v1", "criada"] },
        rejeitada: { status: "REJEITADA", badge: ["rejeitada", "x-circulo", "Rejeitada"], etapa: 5, final: ["interrompida", "x", "Rejeitada"],
            msg: ["perigo", "x-circulo", "Rejeitada", "A administração encerrou a análise sem autorizar a utilização.", "Motivo informado: o período coincide com o ensaio geral da Semana Cultural. (texto fictício)"],
            docs: [DOC_V2_VALIDADA, DOC_V1_DEVOLVIDA], envio: false, semEnvio: "Novos envios não são permitidos depois que a solicitação avança para análise.",
            eventos: ["rejeitada", "contrib", "v2validada", "v2", "v1devolvida", "v1", "criada"] },
        cancelada: { status: "CANCELADA", badge: ["cancelada", "proibido", "Cancelada pela administração"], etapa: 5, final: ["neutra", "proibido", "Aprovada e depois cancelada"],
            msg: ["neutro", "proibido", "Cancelada pela administração", "A reserva deixou de autorizar a utilização da quadra. Consulte o motivo institucional abaixo.",
                "Motivo: manutenção emergencial da cobertura da quadra. (texto fictício) A equipe da escola ainda precisa registrar qualquer contato ou encaminhamento de reagendamento."],
            docs: [DOC_V2_VALIDADA, DOC_V1_DEVOLVIDA], envio: false, semEnvio: "Novos envios não são permitidos depois que a solicitação avança para análise.",
            eventos: ["cancelada", "aprovada", "contrib", "v2validada", "v2", "v1devolvida", "v1", "criada"] }
    };

    function iniciarSolicitacao() {
        var raiz = document.querySelector("[data-solicitacao]");
        if (!raiz) { return; }
        var seletor = raiz.querySelector("[data-seletor-estado]");
        function alvo(nome) { return raiz.querySelector('[data-render="' + nome + '"]'); }

        function aplicar(chaveEstado) {
            var e = ESTADOS[chaveEstado] || ESTADOS.documentacao;
            alvo("badge").innerHTML = '<span class="badge badge--' + e.badge[0] + '">' + icone(e.badge[1]) + e.badge[2] + "</span>";
            alvo("status-gravado").textContent = e.status;

            alvo("etapas").innerHTML = ETAPAS.map(function (rotulo, i) {
                var classe = "etapa", marcador = String(i + 1), extra = "";
                if (i < e.etapa) { classe += " etapa--concluida"; marcador = icone("check", "icone--sm"); extra = '<span class="visually-hidden">Concluída: </span>'; }
                else if (i === e.etapa && !e.final) { classe += " etapa--atual"; extra = '<span class="visually-hidden">Etapa atual: </span>'; }
                if (e.final && i === 4) {
                    classe = "etapa etapa--" + e.final[0]; marcador = icone(e.final[1], "icone--sm"); rotulo = e.final[2]; extra = "";
                }
                if (e.retorno && i === 1) { classe = "etapa etapa--atual etapa--retorno"; marcador = "2"; rotulo = "Participantes e PDF (correção)"; }
                if (e.retorno && i === 2) { classe = "etapa etapa--devolvida"; marcador = icone("seta-esq", "icone--sm"); rotulo = "Conferência: devolvido"; }
                var atual = classe.indexOf("etapa--atual") !== -1 ? ' aria-current="step"' : "";
                return '<li class="' + classe + '"' + atual + '><span class="etapa__marcador" aria-hidden="true">' + marcador +
                    '</span><span class="etapa__rotulo">' + extra + rotulo + "</span></li>";
            }).join("");

            var m = e.msg;
            alvo("mensagem").innerHTML = '<div class="alerta alerta--' + m[0] + '">' + icone(m[1]) + '<div><strong class="alerta__titulo">' + m[2] +
                "</strong><p>" + m[3] + "</p>" + (m[4] ? '<p style="margin-top:.5rem">' + m[4] + "</p>" : "") + "</div></div>";

            alvo("participantes-acao").innerHTML = e.editarParticipantes
                ? '<a class="botao botao--secundario" href="aluno-participantes.html">Revisar participantes</a>'
                : '<a class="botao botao--secundario" href="aluno-participantes.html">Ver participantes</a><span class="campo__ajuda">A lista fica bloqueada durante a conferência e depois da validação.</span>';

            alvo("documentos").innerHTML = e.docs.length ? '<ul class="lista-versoes">' + e.docs.map(function (d) {
                return '<li class="arquivo-pdf"><span class="arquivo-pdf__icone" aria-hidden="true">PDF</span><div><strong>v' + d.versao + " — " + d.nome +
                    "</strong><span>Enviada em " + d.envio + "</span>" + (d.justificativa ? '<p class="justificativa"><strong>Justificativa da secretaria:</strong> ' + d.justificativa + "</p>" : "") +
                    '</div><span class="badge badge--' + d.situacao[0] + '">' + icone(d.situacao[1]) + d.situacao[2] + "</span></li>";
            }).join("") + "</ul>" : '<div class="vazio vazio--compacto"><span class="vazio__icone">' + icone("documento", "icone--lg") +
                "</span><strong>Nenhum PDF enviado</strong><p>A solicitação continua aguardando documentação.</p></div>";

            raiz.querySelector("[data-envio]").hidden = !e.envio;
            var semEnvio = raiz.querySelector("[data-sem-envio]");
            semEnvio.hidden = e.envio;
            semEnvio.textContent = e.semEnvio || "";

            alvo("historico").innerHTML = e.eventos.map(function (k) {
                var ev = EVENTOS[k];
                var tom = ev[3] ? " historico__ponto--" + ev[3] : "";
                return '<li><span class="historico__ponto' + tom + '" aria-hidden="true"></span><div><strong>' + ev[0] + "</strong><span>" + ev[1] + " · " + ev[2] + "</span></div></li>";
            }).join("");

            seletor.querySelectorAll("button").forEach(function (b) { b.setAttribute("aria-pressed", String(b.value === chaveEstado)); });
        }

        seletor.addEventListener("click", function (ev) {
            var b = ev.target.closest("button"); if (!b) { return; }
            aplicar(b.value);
            if (window.history && window.history.replaceState) { window.history.replaceState(null, "", "?estado=" + b.value); }
        });
        aplicar(ESTADOS[params.get("estado")] ? params.get("estado") : "documentacao");
        if (params.get("nova") === "1") { raiz.querySelector("[data-aviso-nova]").hidden = false; }
    }

    /* ---------- Envio de PDF: controle em português sobre o input nativo ----
       O <input type="file"> continua existindo, focável e funcional.
       O protótipo só valida extensão e tamanho para mostrar o retorno visual;
       nada é enviado nem guardado. */
    var LIMITE_PDF = 10 * 1024 * 1024; // igual a DocumentoReservaService.TAMANHO_MAXIMO_BYTES

    function formatarTamanho(bytes) {
        if (bytes < 1024 * 1024) { return Math.max(1, Math.round(bytes / 1024)) + " KB"; }
        return (bytes / (1024 * 1024)).toFixed(1).replace(".", ",") + " MB";
    }

    function iniciarUpload() {
        document.querySelectorAll("[data-upload]").forEach(function (caixa) {
            var input = caixa.querySelector('input[type="file"]');
            var estado = caixa.querySelector("[data-upload-estado]");
            var erro = caixa.querySelector("[data-upload-erro]");
            var remover = caixa.querySelector("[data-upload-remover]");
            var rotulo = caixa.querySelector("[data-upload-rotulo]");
            var area = caixa.querySelector("[data-upload-caixa]");

            function mostrarErro(texto) {
                erro.textContent = texto; erro.hidden = !texto;
                input.setAttribute("aria-invalid", texto ? "true" : "false");
                area.classList.toggle("upload__caixa--erro", !!texto);
            }
            function atualizar() {
                var arquivo = input.files && input.files[0];
                mostrarErro("");
                if (!arquivo) {
                    estado.textContent = "Nenhum arquivo selecionado.";
                    area.classList.remove("upload__caixa--selecionado");
                    remover.hidden = true; rotulo.textContent = "Selecionar PDF";
                    return;
                }
                var ehPdf = /\.pdf$/i.test(arquivo.name) && (!arquivo.type || arquivo.type === "application/pdf");
                if (!ehPdf) {
                    estado.textContent = "Arquivo não aceito: " + arquivo.name;
                    mostrarErro("Selecione um arquivo no formato PDF (.pdf).");
                } else if (arquivo.size > LIMITE_PDF) {
                    estado.textContent = "Arquivo grande demais: " + arquivo.name + " (" + formatarTamanho(arquivo.size) + ")";
                    mostrarErro("O arquivo PDF deve possuir no máximo 10 MB.");
                } else {
                    estado.innerHTML = '<span class="upload__nome">' + arquivo.name.replace(/[<>&]/g, "") + '</span><span class="upload__meta">' + formatarTamanho(arquivo.size) + " · selecionado, ainda não enviado</span>";
                }
                area.classList.toggle("upload__caixa--selecionado", ehPdf && arquivo.size <= LIMITE_PDF);
                remover.hidden = false; rotulo.textContent = "Trocar arquivo";
            }
            input.addEventListener("change", atualizar);
            remover.addEventListener("click", function () { input.value = ""; atualizar(); input.focus(); });

            var form = caixa.closest("form[data-upload-form]");
            if (form) {
                form.addEventListener("submit", function (e) {
                    e.preventDefault();
                    var ok = form.querySelector("[data-upload-ok]");
                    ok.hidden = true;
                    if (!input.files || !input.files[0]) { mostrarErro("Selecione um arquivo PDF antes de enviar."); input.focus(); return; }
                    if (input.getAttribute("aria-invalid") === "true") { input.focus(); return; }
                    ok.hidden = false; ok.focus();
                });
            }
        });
    }

    /* ---------- Pequenos comportamentos ---------------------------------- */
    function iniciarContadores() {
        document.querySelectorAll("textarea[maxlength][data-contador]").forEach(function (t) {
            var alvo = document.getElementById(t.getAttribute("data-contador"));
            function atualizar() {
                alvo.textContent = t.value.length + " de " + t.getAttribute("maxlength") + " caracteres";
                if (t.value.trim()) {
                    t.removeAttribute("aria-invalid");
                    var c = t.closest(".campo"); if (c) { c.classList.remove("campo--erro"); }
                    var erro = c && c.querySelector(".campo__erro"); if (erro && erro.hasAttribute("data-erro-finalidade")) { erro.hidden = true; }
                }
            }
            t.addEventListener("input", atualizar); atualizar();
        });
    }

    function iniciarDemonstracoes() {
        // Formulários do protótipo não enviam nada: mostram somente o retorno visual.
        document.querySelectorAll("form[data-demo]").forEach(function (form) {
            form.addEventListener("submit", function (e) {
                e.preventDefault();
                var obrigatorios = form.querySelectorAll("[required]");
                var invalido = null;
                obrigatorios.forEach(function (campo) {
                    var vazio = !String(campo.value || "").trim();
                    var caixa = campo.closest(".campo");
                    if (caixa) { caixa.classList.toggle("campo--erro", vazio); }
                    campo.setAttribute("aria-invalid", String(vazio));
                    var msg = caixa && caixa.querySelector(".campo__erro");
                    if (msg) { msg.hidden = !vazio; }
                    if (vazio && !invalido) { invalido = campo; }
                });
                if (invalido) { invalido.focus(); return; }
                var destino = document.getElementById(form.getAttribute("data-demo"));
                if (destino) {
                    destino.hidden = false;
                    destino.setAttribute("tabindex", "-1");
                    destino.focus();
                }
            });
        });
        document.querySelectorAll("[data-demo-carregando]").forEach(function (b) {
            b.addEventListener("click", function () {
                b.classList.add("is-loading"); b.setAttribute("aria-busy", "true");
                window.setTimeout(function () { b.classList.remove("is-loading"); b.removeAttribute("aria-busy"); }, 1400);
            });
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        montarFaixa();
        montarMoldura();
        hidratarIcones();
        document.querySelectorAll("[data-agenda]").forEach(iniciarAgenda);
        iniciarSolicitacao();
        iniciarUpload();
        iniciarContadores();
        iniciarDemonstracoes();
    });
})();
