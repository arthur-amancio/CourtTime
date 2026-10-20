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
        lateral.innerHTML = marca +
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

        var botao = topoMovel.querySelector(".topo-movel__menu");
        function fechar() {
            lateral.classList.remove("is-aberta"); sombra.hidden = true;
            botao.setAttribute("aria-expanded", "false");
        }
        botao.addEventListener("click", function () {
            var abrir = !lateral.classList.contains("is-aberta");
            if (!abrir) { fechar(); botao.focus(); return; }
            lateral.classList.add("is-aberta"); sombra.hidden = false;
            botao.setAttribute("aria-expanded", "true");
            var primeiro = lateral.querySelector("a"); if (primeiro) { primeiro.focus(); }
        });
        sombra.addEventListener("click", fechar);
        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape" && lateral.classList.contains("is-aberta")) { fechar(); botao.focus(); }
        });

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

        function desenharFaixa() {
            if (!faixa) { return; }
            var html = "";
            var d = new Date(HOJE);
            for (var i = 0; i < 21; i++) {
                var funciona = (GRADE[d.getDay()] || []).length > 0;
                var passado = d < minimo && !mesmaData(d, minimo);
                var livres = periodosDoDia(d).filter(function (p) { return p.situacao === "disponivel"; }).length;
                var info = passado ? "—" : (!funciona ? "fechado" : (livres ? livres + " livres" : "lotado"));
                html += '<button type="button" class="dia-chip" data-data="' + chave(d) + '"' + (passado ? " disabled" : "") +
                    ' aria-pressed="' + mesmaData(d, selecionada) + '" aria-label="' + dataPorExtenso(d) + ", " + info + '">' +
                    "<small>" + DIAS_CURTOS[d.getDay()] + "</small><strong>" + d.getDate() + "</strong><span>" + info + "</span></button>";
                d.setDate(d.getDate() + 1);
            }
            faixa.innerHTML = html;
            var ativo = faixa.querySelector('[aria-pressed="true"]');
            if (ativo && ativo.scrollIntoView && faixa.scrollWidth > faixa.clientWidth) {
                faixa.scrollLeft = Math.max(0, ativo.offsetLeft - 8);
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
        anterior.addEventListener("click", function () { mesVisivel.setMonth(mesVisivel.getMonth() - 1); desenharMes(); });
        proximo.addEventListener("click", function () { mesVisivel.setMonth(mesVisivel.getMonth() + 1); desenharMes(); });
        if (simular) {
            simular.addEventListener("change", function () {
                quadraAtiva = !simular.checked; horarioEscolhido = null; desenharSlots(); atualizarResumo();
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

    /* ---------- Variações de estado da solicitação ----------------------- */
    function iniciarEstados() {
        var seletor = document.querySelector("[data-seletor-estado]");
        if (!seletor) { return; }
        var estado = params.get("estado") || "documentacao";
        function aplicar(valor) {
            document.querySelectorAll("[data-estado]").forEach(function (el) {
                var lista = el.getAttribute("data-estado").split(/\s+/);
                el.hidden = lista.indexOf(valor) === -1;
            });
            seletor.querySelectorAll("button").forEach(function (b) { b.setAttribute("aria-pressed", String(b.value === valor)); });
        }
        seletor.addEventListener("click", function (e) {
            var b = e.target.closest("button"); if (!b) { return; }
            aplicar(b.value);
            if (window.history && window.history.replaceState) {
                window.history.replaceState(null, "", "?estado=" + b.value);
            }
        });
        aplicar(estado);
        if (params.get("nova") === "1") {
            var aviso = document.querySelector("[data-aviso-nova]");
            if (aviso) { aviso.hidden = false; }
        }
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
        iniciarEstados();
        iniciarContadores();
        iniciarDemonstracoes();
    });
})();
