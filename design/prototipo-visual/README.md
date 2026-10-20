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
| 3. Calendário / consulta da quadra | `quadra.html` | todos |
| 4. Nova solicitação | `aluno-nova-solicitacao.html` | aluno |
| 4. Acompanhamento (5 situações) | `aluno-solicitacoes.html`, `aluno-solicitacao.html?estado=…` | aluno |
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
    ├── DIRECOES-VISUAIS.md   pesquisa, direções A/B/C, análise do Lovable e escolha
    ├── DESIGN-SYSTEM.md      tokens, componentes e regras de uso
    ├── RECURSOS-VISUAIS.md   fotografias pesquisadas, fontes e autorização
    ├── MIGRACAO-THYMELEAF.md  passos para levar o visual aprovado às telas reais
    ├── RELATORIO-QUALIDADE.md acessibilidade e responsividade verificadas
    ├── capturas/              capturas do PROTÓTIPO (não da aplicação em execução)
    └── referencias/lovable/   capturas de referência do protótipo do Lovable
```

## Importante para a revisão

- As capturas em `docs/capturas/` são do **protótipo visual**, renderizado em Chromium. Elas **não** são evidência da aplicação Spring Boot em execução.
- Nenhuma fotografia externa foi incorporada. A área de imagem da página inicial é uma composição gráfica própria, identificada como espaço reservado. Veja `docs/RECURSOS-VISUAIS.md`.
- Não houve integração com Figma nesta sessão: a ferramenta não estava disponível. O protótipo HTML é a entrega principal.
- O protótipo **não está aprovado**. Ele aguarda revisão externa e decisão do grupo antes de qualquer implementação.
