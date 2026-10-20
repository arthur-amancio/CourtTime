# CourtTime — Protótipo visual

> **PROTÓTIPO VISUAL — NÃO MESCLAR.** Esta pasta é uma proposta de interface para avaliação. Ela não altera a aplicação Spring Boot: controllers, services, entidades, segurança, banco, `src/main/resources/templates` e `style.css` continuam exatamente como estão na `main`.

Protótipo navegável em HTML, CSS e JavaScript simples, sem framework, sem build e sem dependências. Todos os dados são fictícios e nada é gravado. Os atalhos de perfil na tela de login são navegação demonstrativa, não autenticação.

## Como abrir

**Opção 1 (mais simples):** dê dois cliques em `design/prototipo-visual/index.html`. Funciona direto do disco.

**Opção 2 (servidor local, recomendada para revisão):**

```bash
cd design/prototipo-visual
python -m http.server 8080
# abra http://localhost:8080/telas.html
```

Comece por `telas.html`, que lista todas as telas, ou por `index.html` → **Entrar** → escolha um perfil em “Somente no protótipo”.

## Telas

| Tela pedida | Arquivo | Perfil |
| --- | --- | --- |
| 1. Página inicial pública | `index.html` | visitante |
| 2. Login | `login.html` | todos |
| Início por perfil | `inicio.html?perfil=aluno` (ou `secretaria`, `administrador`, `seguranca`) | todos |
| 3. Calendário / consulta da quadra | `quadra.html` (no celular: `?calendario=a` ou `?calendario=b`) | todos |
| 4. Nova solicitação | `aluno-nova-solicitacao.html` | aluno |
| 4. Acompanhamento | `aluno-solicitacoes.html`, `aluno-solicitacao.html?estado=…` (8 variações: `documentacao`, `conferencia`, `devolvida`, `contribuicao`, `analise`, `aprovada`, `rejeitada`, `cancelada`) | aluno |
| Participantes | `aluno-participantes.html` | aluno |
| 5. Painel administrativo | `admin-painel.html` | administrador |
| Fila e análise | `admin-solicitacoes.html`, `admin-solicitacao.html` | administrador |
| Usuários, horários, bloqueios | `admin-usuarios.html`, `admin-horarios.html`, `admin-bloqueios.html` | administrador |
| 6. Secretaria | `secretaria-documentos.html`, `secretaria-conferencia.html`, `secretaria-contribuicoes.html` | secretaria |
| 6. Segurança | `seguranca-reservas.html`, `seguranca-conferencia.html` | segurança |
| Design system | `design-system.html` | documentação |
| Direções visuais | `direcoes.html` | documentação |

## Estrutura

```
design/prototipo-visual/
├── *.html                    telas do protótipo
├── assets/css/courttime.css  design system (tokens + componentes)
├── assets/js/prototipo.js    moldura, ícones, calendário e demonstrações
└── docs/
    ├── DIRECOES-VISUAIS.md    pesquisa, direções A/B/C, análise do Lovable e escolha
    ├── DESIGN-SYSTEM.md       tokens, componentes e regras de uso
    ├── CALENDARIO-MOBILE.md   variantes A e B do calendário no celular e recomendação
    ├── DADOS-E-ESTADOS.md     estados gravados × situação × etapas, histórico e indicadores
    ├── RECURSOS-VISUAIS.md    fotografias pesquisadas, fontes e autorização
    ├── MIGRACAO-THYMELEAF.md  plano em fases A–F para levar o visual às telas reais
    ├── RELATORIO-QUALIDADE.md acessibilidade, teclado, zoom e responsividade (V2)
    ├── capturas/v1/           capturas da primeira versão do PROTÓTIPO
    ├── capturas/v2/           capturas da versão atual do PROTÓTIPO
    ├── comparacao-v1-v2/      lado a lado das telas que mais mudaram
    └── referencias/lovable/   capturas de referência do protótipo do Lovable
```

## O que mudou na V2

Ajustes pedidos na revisão externa de 20/10/2026 (PR #49):

- **Login no celular:** identidade em uma faixa compacta; e-mail, senha e Entrar visíveis sem rolagem em 390 × 844.
- **Calendário no celular:** duas variantes (A sem contagem, B com vagas por dia), alternáveis nos controles do protótipo. Recomendação e custo em `docs/CALENDARIO-MOBILE.md`.
- **Envio de PDF:** controle em português (“Selecionar PDF”, nome do arquivo, 10 MB, erros acessíveis) sobre o `input type="file"` nativo.
- **Menu móvel:** fundo inerte, foco preso no menu, Esc e botão Fechar, foco devolvido ao botão Menu.
- **Barra fixa e zoom:** a barra some quando o formulário de envio aparece e deixa de ser fixa com pouca altura (zoom 400%, teclado virtual).
- **Fidelidade:** etapas separadas dos estados gravados; devolução documental, conferência e contribuição pendente demonstradas; histórico e indicadores sem dado exposto marcados como “Depende de adaptação”.
- **Documentação:** plano de migração em fases A–F e inventário de dados existentes × dependentes.

## Importante para a revisão

- As capturas em `docs/capturas/` e `docs/comparacao-v1-v2/` são do **protótipo visual**, renderizado em Chromium. Elas **não** são evidência da aplicação Spring Boot em execução.
- Nenhuma fotografia externa foi incorporada. A área de imagem da página inicial é uma composição gráfica própria, identificada como espaço reservado. Veja `docs/RECURSOS-VISUAIS.md`.
- Não houve integração com Figma nesta sessão: a ferramenta não estava disponível. O protótipo HTML é a entrega principal.
- Itens com a marca tracejada **“Depende de adaptação”** usam dados que a aplicação ainda não envia às telas (ver `docs/DADOS-E-ESTADOS.md`).
- O protótipo **não está aprovado**. Ele aguarda revisão externa e decisão do grupo antes de qualquer implementação.
