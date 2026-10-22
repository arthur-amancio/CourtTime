> Publicação em 22/10/2026: consulte ATUALIZACAO-PUBLICACAO.md. A main evoluiu e os rótulos das figuras referem-se à base histórica analisada.

# Diagramas do TCC CourtTime

Modelo final pretendido, com base implementada no SHA 7b40dcb e extensões explicitamente separadas. Consulte RELATORIO.md antes de tratar qualquer proposta como funcionalidade concluída.

- Abra `svg/` para visualizar ou inserir imagens vetoriais.
- Edite `fontes/*.dot`; execute `node renderizar.cjs` com @viz-js/viz instalado fora da aplicação.
- Alternativamente, use Graphviz: `dot -Tsvg fontes/01a-casos-geral.dot -o svg/01a-casos-geral.svg`.
- `textos-academicos.md` organiza títulos, fontes e parágrafos por figura.
- `dicionario-atributos.md` e `inventario-jpa.json` detalham as onze entidades reais.
- `matriz-rastreabilidade.md` vincula regras a casos, entidades, tabelas, diagramas e evidências.

UML: atores em retângulos «actor», casos em elipses, associações simples, dependência «include». Não há «extend» artificial. Classes possuem atributos selecionados; não se deduz composição de uma FK. Atividades são subdivididas, com referências entre figuras; caixas de continuação não são novos estados de Reserva.

No DER, PK = chave primária; FK = referência; UQ = unicidade; NN = não nulo. Cardinalidade 0..N é relacional. Campos novos estão isolados em 03e. Os recortes formam um só modelo lógico, não bancos independentes.
