# CourtTime — apresentação e roteiro de fala

**Versão:** 29/10/2026. **Formato preparado:** conteúdo editável em Markdown para 15 slides, com notas e imagens reais indicadas. Não foi produzido ou validado um arquivo PowerPoint nesta missão.

## Organização e duração

A sequência usa a progressão acadêmica observada no Draw Code: contexto, objetivos, método, requisitos, modelagem, implementação e resultados. A identidade deve ser a do CourtTime, usando o logotipo e a paleta existentes. Não reutilizar o visual ou o texto do outro TCC.

**Tempo total proposto: 720 segundos, ou 12 minutos**, sem perguntas. A confirmação da duração cabe à banca. O slide 12 narra o fluxo; a execução completa em navegador tem roteiro próprio em [DEMONSTRACAO.md](DEMONSTRACAO.md). Não prometer preencher e validar toda a jornada em 95 segundos.

Para uma janela total de 12 minutos incluindo demonstração, reservar aproximadamente oito minutos à exposição e quatro a estados previamente preparados. Nesse caso, condensar os slides 2–4 e 8–10 e mostrar apenas a transição do décimo participante e a substituição de vaga paga. A demonstração completa demanda ensaio e tempo adicional.

Usar uma ideia principal por slide, títulos claros e poucas linhas. As telas do acervo são recortes móveis reais; exibi-las na proporção original, sem esticar para preencher 16:9. Nos diagramas, destacar somente elementos legíveis no projetor. As figuras abaixo pertencem ao candidato congelado; eventuais recortes para slides não alteram os arquivos originais.

## Slides e notas

### 1 — Capa institucional · 25 s

**Conteúdo:** CourtTime — Sistema Web de Gerenciamento e Agendamento de Quadras Esportivas; ETEC Prof. Armando José Farinazzo; Técnico em Informática para Internet; 2026. Arthur Amancio Francisco, Carlos Eduardo Ramos Alves, Gustavo Tagliaferro Sabadini e Heitor Silva Rodrigues. Orientador: Prof. André Luis Zampieri Mosca.

**Figura real:** [logotipo horizontal, figura 6](../finalizacao-academica/figuras/figura-06.png). Não acrescentar selo de aprovação.

**Notas:** apresentar equipe e objetivo em uma frase: organizar o processo de reserva de quadra, da solicitação à liberação. A data da banca será preenchida quando oficialmente informada.

### 2 — Problema identificado · 45 s

**Conteúdo:** necessidade de consultar horários; reunir participantes e documentação; acompanhar decisões entre setores.

**Figura real:** [calendário, figura 23](../finalizacao-academica/figuras/tela-aluno-calendario.png), identificado como resposta implementada ao problema.

**Notas:** contextualizar a necessidade relatada no levantamento. Não afirmar perdas, fraudes ou tempos médios que não foram medidos. A conversa relatada ajudou a esclarecer o procedimento; não apresentá-la como entrevista formal transcrita.

### 3 — Justificativa · 30 s

**Conteúdo:** centralização de informações; acompanhamento das etapas; responsabilidades de cada perfil; consulta em dispositivos móveis.

**Figura real:** [página pública, figura 20](../finalizacao-academica/figuras/tela-pagina-publica.png).

**Notas:** explicar a utilidade institucional esperada. Distinguir benefício esperado de resultado medido: o projeto não comprovou percentualmente redução de filas ou tempo de atendimento.

### 4 — Objetivos · 40 s

**Conteúdo:** desenvolver aplicação web para reservas; controlar participantes e documentos; apoiar conferência, aprovação e portaria; preservar decisões e pagamentos registrados.

**Figura real:** [casos de uso gerais, figura 12](../finalizacao-academica/figuras/figura-12.png), destacando os quatro atores.

**Notas:** conectar cada objetivo a uma entrega real. O escopo é institucional e não inclui pagamento online, aplicativo nativo ou mensagens automáticas por WhatsApp.

### 5 — Metodologia e levantamento · 60 s

**Conteúdo:** requisitos e regras registradas; 28 registros primários de questionário; desenvolvimento incremental; modelagem e testes. Recorte descritivo: 24 registros indicam celular e quatro celular/computador.

**Figura recomendada:** quadro textual elaborado a partir das contagens verificadas em [BIBLIOGRAFIA-E-PESQUISA.md](../finalizacao-academica/BIBLIOGRAFIA-E-PESQUISA.md). Não usar fotografia ou gráfico fictício; não projetar a planilha com dados individuais.

**Notas:** informar o período de 6 a 24/10/2026 e a limitação de amostragem. Dizer “28 registros”, sem converter automaticamente em 28 pessoas únicas. A classificação metodológica e a hipótese permanecem sujeitas ao parecer do orientador.

### 6 — Solução proposta e implementada · 40 s

**Conteúdo:** Aluno solicita; Secretaria confere; Administrador decide; Segurança controla o uso. Uma aplicação web com etapas e histórico.

**Figura real:** [solicitação e administração, figura 13](../finalizacao-academica/figuras/figura-13.png).

**Notas:** apresentar as responsabilidades, sem tratar a contribuição como transação bancária. A assinatura ocorre externamente e a conferência documental é humana.

### 7 — Requisitos principais · 50 s

**Conteúdo:** menos de dez pessoas = rascunho; ALUNO + OUTRO contam no mínimo; documentação antes do uso; ausência de conflito; dez presentes e pendências resolvidas para liberar.

**Figura real:** [participantes, figura 25](../finalizacao-academica/figuras/tela-aluno-participantes.png).

**Notas:** o backend mantém a regra mesmo se a interface for contornada. Informar a divergência ainda aberta: o termo histórico menciona dez alunos; a decisão de projeto implementa dez pessoas totais. Não anunciar homologação dessa interpretação pela ETEC.

### 8 — Tecnologias utilizadas · 35 s

**Conteúdo:** Java 17 e Spring Boot; MVC/Security e Thymeleaf; PostgreSQL e Maven; Git/GitHub e Playwright.

**Figura real:** [arquitetura, figura 19](../finalizacao-academica/figuras/figura-19.png), usada como apoio, sem logotipos adicionais desnecessários.

**Notas:** explicar a função de cada grupo: servidor/regras, páginas/autorização, persistência/build e versionamento/testes. Não atribuir à equipe uma motivação histórica de escolha que não esteja documentada.

### 9 — Arquitetura do sistema · 40 s

**Conteúdo:** navegador → controllers MVC → serviços de negócio → repositories/JPA → PostgreSQL; autorização e armazenamento protegido de documentos.

**Figura real:** [componentes arquiteturais, figura 19](../finalizacao-academica/figuras/figura-19.png).

**Notas:** explicar que a aplicação renderiza as páginas no servidor. Controllers recebem as ações; serviços validam regras e transações. A geração do PDF e o armazenamento dos documentos integram a aplicação existente. Não descrever microsserviços ou APIs novas.

### 10 — Modelagem UML e DER · 45 s

**Conteúdo:** Reserva e participantes; documentos e versões; contribuições e conferência; atributo `rascunho` coerente com a implementação.

**Figuras reais:** [classes do núcleo, figura 15](../finalizacao-academica/figuras/classes-nucleo.png) e [DER do núcleo, figura 17](../finalizacao-academica/figuras/der-nucleo.png).

**Notas:** mostrar uma figura em destaque e a outra como apoio alternativo. Explicar a diferença entre classes e estrutura relacional. As vistas finais selecionam atributos; não são uma listagem de todas as colunas. Não usar uma versão Astah antiga sem `rascunho` como segunda representação final contraditória.

### 11 — Telas reais · 45 s

**Conteúdo:** calendário; documentos; conferência da Secretaria. Interface adaptada à consulta móvel.

**Figuras reais:** [calendário](../finalizacao-academica/figuras/tela-aluno-calendario.png), [documentos](../finalizacao-academica/figuras/tela-aluno-documentos.png) e [conferência](../finalizacao-academica/figuras/tela-secretaria-documento.png), figuras 23, 26 e 28.

**Notas:** identificar o perfil de cada tela e sua finalidade. As imagens são capturas de execução anterior, com versão registrada no inventário, e não uma sessão ao vivo. Priorizar legibilidade em vez de miniaturas de todas as telas.

### 12 — Fluxo completo e diferencial · 95 s

**Conteúdo:** disponibilidade → rascunho → dez participantes → PDF → Secretaria/contribuição → aprovação → presenças/adicionais → liberação. Diferencial: vaga adicional paga pode ser reutilizada antes da liberação.

**Figuras reais:** [portaria e contribuições, figura 14](../finalizacao-academica/figuras/figura-14.png) e [conferência encerrada, figura 33](../finalizacao-academica/figuras/tela-seguranca-reserva.png).

**Notas:** narrar “A paga, sai e fica AUSENTE; B ocupa a capacidade paga; C ultrapassa essa capacidade e exige mais R$ 2,00”. O histórico de A permanece. O registro de recebimento pela Segurança não confirma o repasse à Secretaria. A figura 33 mostra um estado final já liberado; não a apresentar como prova isolada de todos os passos narrados. Para navegação, usar o roteiro separado e explicar as reservas de datas distintas.

### 13 — Testes e resultados · 60 s

**Conteúdo:** 477 testes por matriz PostgreSQL 16/18 na CI #319; zero falhas/erros/skips; execução de navegador anterior com 1.122 verificações, quatro perfis, desktop/mobile; 44 varreduras axe.

**Figura recomendada:** tabela textual de resultados com os links de [REVISAO-ORIENTADOR.md](REVISAO-ORIENTADOR.md); opcionalmente abrir a página real da [CI #319](https://github.com/arthur-amancio/CourtTime/actions/runs/40868642896), sem inventar uma captura.

**Notas:** citar os SHAs e datas disponíveis no relatório. Diferenciar casos Java, verificações de navegador e varreduras de acessibilidade. Informar o erro ambiental de symlink da execução local Windows se perguntado; a aprovação citada é a da CI. Testes automatizados não substituem homologação da instituição.

### 14 — Conclusão, limites e próximos passos · 65 s

**Conteúdo:** fluxo implementado e evidenciado; TCC candidato com 82 páginas, 33 figuras e sete quadros; regras institucionais ainda a confirmar; revisão do orientador e integração futura autorizada.

**Figura real:** [cancelamento, figura 27](../finalizacao-academica/figuras/tela-final-detalhe-cancelada.png), se necessário para ilustrar histórico e limites da remarcação; pode ser slide somente textual.

**Notas:** concluir em relação ao objetivo inicial. Separar funcionalidade demonstrada de benefício esperado e homologação. Citar o mínimo de participantes, prazo, vínculos, crédito excedente e retenção como decisões pendentes, sem sugerir que a equipe já decidiu em nome da ETEC.

### 15 — Agradecimentos e perguntas · 45 s

**Conteúdo:** agradecimento ao orientador, à instituição e às pessoas que contribuíram; espaço para perguntas; link ao material de revisão.

**Figura real:** [logotipo horizontal](../finalizacao-academica/figuras/figura-06.png). Exibir o endereço do material apenas conforme o acesso autorizado; não alterar compartilhamento para criar um QR público.

**Notas:** retomar em uma frase a contribuição do sistema e abrir perguntas. Não preencher nomes de examinadores ou uma aprovação antes de sua confirmação. O saldo do tempo pode absorver transições entre apresentadores.

## Perguntas técnicas: respostas de apoio

| Pergunta | Resposta sustentada pela implementação |
|---|---|
| Por que Java/Spring e Thymeleaf? | A stack reúne MVC, validação, segurança e transações, e renderiza páginas no servidor. Os autores devem explicar sua motivação histórica, sem uma justificativa inventada. |
| Como evitam horários conflitantes? | O serviço revalida disponibilidade e conflito; a aprovação usa controle transacional e lock da quadra. Há testes de integração PostgreSQL. |
| Quem pode ver documentos? | O backend aplica perfil e titularidade; a Segurança não dispõe do acesso documental/financeiro da Secretaria. Ocultar botões não é a única proteção. |
| Como comprovam dez participantes? | Soma ALUNO e OUTRO no backend; menos de dez mantém rascunho e bloqueia documentação. O termo institucional divergente permanece pendente de decisão. |
| Se A paga e vai embora? | Ausência preserva histórico; B pode ocupar a capacidade paga antes da liberação. Não há estorno nem crédito por essa saída; C paga apenas o excedente de capacidade. |
| O sistema está homologado? | Não há homologação institucional comprovada. Há implementação, evidências técnicas e candidato acadêmico pronto para revisão do orientador. |

**Ensaio ainda necessário:** distribuir as falas entre os quatro autores, cronometrar em projetor real, testar legibilidade e confirmar o tempo com a banca. Este roteiro não é registro de ensaio executado.
