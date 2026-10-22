# Validação e entrega

## Escopo concluído

- reconstrução dos casos de uso gerais e do recorte portaria/Secretaria;
- reconstrução das classes do núcleo e do recorte documentação/contribuição/portaria;
- DER lógico das doze entidades JPA;
- atividades de reserva, cancelamento/remarcação e liberação;
- arquitetura lógica e tecnológica;
- fontes `.drawio` individuais e multipágina;
- SVG e PDF vetoriais e PNG em alta resolução;
- legendas acadêmicas, texto para capítulos IV e V, rastreabilidade e decisões.

## Critérios de aceite

| Critério | Evidência |
|---|---|
| Base inclui alterações ainda não integradas à `main` | commit-base do PR #87; PRs #85 e #86 confirmados como ancestrais |
| Não usar apenas `4df4907` | todos os diagramas derivam de `6fc1aee...` |
| Fonte realmente editável | dez arquivos `.drawio` XML, com páginas, vértices e conectores independentes |
| Não alegar `.asta` inexistente | limitação da edição Community documentada; nenhum `.asta` produzido |
| Cardinalidades verificadas | matriz em `correspondencia-codigo.md` e restrições `PK/FK/UQ/NN` no DER |
| Regras atualizadas visíveis | dez cadastrados, dez presentes, R$ 2,00, adicional, vaga paga, repasse e congelamento nos diagramas e textos |
| Adequação a A4 | canvas vetorial 1123 × 794 e PDFs A4 paisagem de uma página |
| Entregas vetoriais e raster | nove SVG, nove PDF e nove PNG 3508 × 2480 |
| Revisão visual | as nove imagens foram abertas; cruzamentos, sobreposições e rótulos cortados foram corrigidos |

## Validações executadas

Os comandos reproduzíveis são:

```powershell
node docs\tcc\modelagem-profissional\gerar-diagramas.cjs
.\docs\tcc\modelagem-profissional\exportar.ps1
.\docs\tcc\modelagem-profissional\validar.ps1
.\mvnw.cmd clean verify
```

`validar.ps1` verifica:

- nove diagramas individuais e nove páginas no arquivo consolidado;
- XML bem formado em `.drawio` e SVG;
- existência de vértices e conectores editáveis;
- presença de SVG, PNG e PDF para cada item do manifesto;
- cabeçalho PDF e dimensões mínimas dos artefatos;
- ocorrência textual das regras críticas no conjunto.

Resultado da verificação final:

- **Maven:** `mvnw clean verify` executado duas vezes. Em cada execução, 472 testes foram descobertos: 406 passaram, 65 foram ignorados pela configuração e um terminou em erro antes da asserção. `DocumentoStorageLocalTest.recusaLinkSimbolicoMesmoComChaveValida` não conseguiu criar o link simbólico de preparação porque a conta do Windows não possui o privilégio necessário (`FileSystemException: O cliente não tem o privilégio necessário`). A repetição fora da restrição do workspace teve o mesmo resultado. Nenhum teste funcional falhou e nenhum código da aplicação foi alterado por esta entrega.
- **Commit:** pendente de criação.
- **Pull request draft:** pendente de publicação.

## Riscos e limites

- O DER deve ser conferido novamente se houver mudanças nas entidades ou se uma migração versionada passar a definir o schema real.
- Uma edição visual feita diretamente no `.drawio` precisa ser refletida no gerador antes de regenerar o pacote.
- A diagramação segue A4 e convenções acadêmicas disponíveis no projeto; a paginação definitiva depende do manual completo adotado pela unidade.
- O branch desta entrega depende do conteúdo do PR #87. Enquanto ele não estiver integrado, o pull request dos diagramas deve usar o branch do PR #87 como base ou registrar essa dependência.
