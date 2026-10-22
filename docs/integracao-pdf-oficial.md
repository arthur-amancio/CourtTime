# Integração com o formulário oficial da ETEC

Análise técnica para preencher automaticamente o documento
`formulario_de_reserva_da_quadra.pdf`, sem alterá-lo.

## Situação: implementado

Na tela **Documentos** da solicitação, enquanto ela aguarda documentação, o aluno
informa CPF e curso e clica em **Gerar formulário preenchido**. O sistema devolve
o PDF oficial com os dados escritos por cima; o aluno imprime, assina e envia o
PDF assinado na mesma tela.

| Decisão | O que foi feito |
|---|---|
| Biblioteca | Apache PDFBox 3.0.5 (Apache-2.0), única dependência nova |
| Modelo | o arquivo original, sem alteração, em `src/main/resources/modelos/formulario-reserva-quadra.pdf` |
| Como preenche | texto acrescentado no modo `APPEND` nas coordenadas medidas abaixo (`FormularioReservaPdf`) |
| CPF e curso | **alternativa A**: digitados na geração, só entram no PDF; não são gravados no banco, em arquivo nem em log. O CPF vai no corpo de um POST, nunca na URL, e é conferido pelos dígitos verificadores |
| Mais de 30 participantes | **a página oficial de participantes é repetida** (cópia do próprio modelo), com o aviso "Participantes 31 a N de N". Ninguém é omitido e nenhum limite é imposto |
| Nomes longos | a fonte diminui até caber; se preciso, o nome quebra em duas linhas na própria célula |
| Vínculo do "Outro" | o "X" é marcado em `( ) Outro`, e o texto do vínculo fica em branco para preencher à mão (o sistema não guarda esse dado) |
| Nunca preenchido | assinaturas, presença e nome do vigilante, data de aprovação e assinatura da direção |
| Modelo ausente | a geração é recusada com a mensagem "O modelo oficial do formulário da ETEC não está disponível no sistema"; nenhum termo substituto é criado |

Observação de desempenho: na primeira geração após iniciar a aplicação, o PDFBox
cria um cache das fontes do sistema (alguns segundos). As seguintes são imediatas.

O restante deste documento é a análise que levou a essas decisões.

## O ponto de partida

O arquivo tem duas páginas A4 (596 × 842 pt) e **nenhum campo AcroForm**. Não
existe "preencher o formulário": é preciso **sobrepor texto** sobre as páginas
originais.

Isso é bom para o nosso requisito. Sobrepor preserva logotipos, cláusulas e
espaços de assinatura exatamente como estão, porque o conteúdo original não é
tocado.

## Biblioteca recomendada: Apache PDFBox

| Opção | Avaliação |
|---|---|
| **Apache PDFBox** | **recomendada.** Licença Apache-2.0, uma dependência, API direta |
| iText | descartada: licença AGPL |
| Gerar HTML e converter para PDF | descartada: não preserva o documento original |

Com PDFBox, abre-se o PDF original e escreve-se em um `PDPageContentStream` no
modo `APPEND`, que acrescenta conteúdo sem reescrever o que já existe:

```java
try (PDDocument documento = Loader.loadPDF(originalBytes)) {
    PDPage pagina = documento.getPage(0);
    try (PDPageContentStream fluxo = new PDPageContentStream(
            documento, pagina, AppendMode.APPEND, true, true)) {
        fluxo.beginText();
        fluxo.setFont(new PDType1Font(HELVETICA), 10);
        fluxo.newLineAtOffset(x, y);
        fluxo.showText(valor);
        fluxo.endText();
    }
    documento.save(saida);
}
```

São cerca de 40 linhas no total, explicáveis para a banca: abrir o original,
escrever texto em coordenadas, salvar uma cópia.

## Coordenadas medidas no documento

Extraídas do PDF real. A origem (0,0) do PDF fica no canto **inferior** esquerdo.

### Página 1

| Campo | Âncora no documento | Onde escrever |
|---|---|---|
| Nome do solicitante | `"do curso"` em y=614,0 x=313,1 | mesma linha, antes de x≈313 |
| Curso | idem | depois de x≈355 |
| CPF | `"inscrito no CPF sob o nº"` y=600,5 x=21,0 | depois de x≈160 |
| Data de utilização | `"Reserva para o dia:"` y=117,8 x=21,0 | depois de x≈130 |
| Horário | `"Horário: das___h___ às ___h___"` y=90,8 x=21,0 | nos espaços de hora e minuto |
| Local e data do preenchimento | mesma linha, à direita | — |
| **Assinatura do solicitante** | y=21,0 | **nunca preencher** |

### Página 2

As 30 linhas são regulares. A fórmula foi conferida nos extremos:

```
y(n) = 657,6 − 18,92 × (n − 1)
```

Linha 1 em y=657,6 e linha 30 em y=108,8.

| Coluna | x aproximado |
|---|---|
| Número da linha | 13 a 16 |
| **Uso do Vigilante (presença)** | ≈35 — **nunca preencher** |
| Nome completo | ≈103 |
| RM ou RG | ≈271 |
| Aluno / Outro | ≈350 em diante |
| **Nome do Vigilante** | **nunca preencher** |
| **"Agendamento APROVADO em"** | **nunca preencher** |
| **Assinatura da direção** | **nunca preencher** |

## De onde vêm os dados

| Campo | Existe hoje? | Origem |
|---|---|---|
| Nome do solicitante | sim | `Usuario.nome` |
| Data de utilização | sim | `Reserva.dataUtilizacao` |
| Horário | sim | `Reserva.horarioInicio` e `horarioFim` |
| Local e data | sim | data da geração |
| Nome do participante | sim | `ParticipanteReserva.nomeCompleto` |
| RM ou RG | sim | `ParticipanteReserva.rmOuRg` |
| Aluno / Outro | sim | `ParticipanteReserva.categoria` |
| **Curso** | **não** | precisa ser coletado |
| **CPF** | **não** | precisa ser coletado |
| **Vínculo do "Outro"** | **não** | precisa ser coletado |

## Os três dados que faltam

### CPF e curso

O CPF é dado pessoal e o termo o exige. Duas alternativas, em ordem de menor
impacto:

**A. Informar só na geração, sem persistir.** O aluno digita CPF e curso na tela
que gera o PDF. Os valores entram no documento e não são gravados. É o que menos
mexe no sistema e o que melhor respeita a minimização de dados. A desvantagem é
redigitar a cada nova versão do documento.

**B. Persistir na reserva, não no cadastro do usuário.** Guardar por solicitação,
e não no perfil. Permite regerar o PDF sem redigitar. Exige decisão institucional
sobre guarda de dado pessoal e, provavelmente, aviso ao titular.

**Recomendação: começar pela alternativa A.** Ela resolve a geração do documento
sem criar guarda de CPF. Se a equipe depois julgar a redigitação incômoda, migrar
para B é uma mudança pequena.

### Vínculo do "Outro"

O formulário traz `( ) Aluno  ( ) Outro: ______`. O sistema tem a categoria, mas
não o texto do vínculo. Como é um campo curto e não sensível, cabe um campo de
texto em `ParticipanteReserva`, preenchido só quando a categoria for `OUTRO`.

Depende de confirmação da ETEC sobre quais vínculos são aceitos. O termo lista
onze: alunos, pais, filhos e cônjuges de alunos, egressos, professores, filhos e
cônjuges de professores, funcionários, filhos e cônjuges de funcionários.

## O problema das 30 linhas

A tabela tem exatamente 30 linhas, e **o termo não menciona limite de
participantes**. Ele exige o mínimo de dez alunos, não um máximo.

Então 30 é a capacidade física do formulário, não uma regra. **Não se deve impor
um limite institucional inventado.**

Mas na hora de gerar o PDF o problema é concreto. Alternativas, para decisão
humana:

1. **Perguntar à ETEC** se existe limite. É o caminho correto, e resolve de vez.
2. **Gerar páginas extras** repetindo o layout da página 2. Preserva todos os
   participantes, mas cria uma página que não é o documento oficial.
3. **Bloquear acima de 30** ao gerar, com mensagem clara. Simples, mas é impor
   uma regra que o termo não tem.
4. **Gerar as 30 primeiras e avisar.** Pior opção: omite participantes em
   silêncio para quem não ler o aviso.

**Implementado:** a alternativa 2, porque a missão de finalização pediu para
tratar a capacidade sem omitir participantes e sem inventar limite. Se a ETEC
definir um limite, ele deve ser aplicado na lista de participantes, não no PDF.

## Limites que o sistema nunca deve cruzar

- Não preencher assinatura do solicitante, do vigilante ou da direção.
- Não marcar a coluna de presença.
- Não preencher a data de aprovação da direção.
- Não alterar cláusulas, logotipos ou formatação.
- Não modificar um PDF que já foi assinado.
- Não presumir que um PDF enviado tem assinatura válida.
- Não criar um documento substituto do termo oficial.

---

# Conformidade entre o sistema e o termo oficial

| Regra do termo | O que o sistema faz | Diferença | Precisa da ETEC? |
|---|---|---|---|
| Mínimo registrado no modelo: 10 alunos | CourtTime exige 10 participantes de qualquer categoria, conforme decisão atual do grupo | divergência entre a regra atual do software e o modelo oficial | **sim; atualização/aprovação da ETEC pendente** |
| Participantes com documento com foto | a segurança consulta nome e RM/RG para conferir | a conferência é humana, na portaria | não |
| Contribuição de R$ 2,00 por participante por hora | a secretaria digita o valor; o sistema não calcula | o sistema não confere o valor | **sim** |
| Reserva só se concretiza com a contribuição | a contribuição é pré-requisito da análise | nenhuma | não |
| Autorização institucional | só a administração aprova | nenhuma | não |
| Categorias autorizadas | o sistema tem apenas Aluno e Outro | falta o texto do vínculo | **sim** |
| **Entrega do termo assinado com um dia de antecedência** | o sistema exige um dia de antecedência **na solicitação** | **ver abaixo** | **sim** |

## A diferença que merece atenção

O termo diz: *"preencher os dados abaixo e deixá-la devidamente assinada na
Secretaria da Etec no mínimo um dia de antecedência do horário pretendido"*.

A antecedência, no texto, é da **entrega do documento assinado**. O PR #26 aplica
a antecedência ao **momento de criar a solicitação**, que é um momento anterior.

**Consequência prática:** hoje um aluno pode criar a solicitação com uma semana
de antecedência e enviar o PDF **uma hora antes** do uso. A solicitação cumpriria
a regra do sistema e violaria a regra do termo.

**Implementação recomendada:** recusar o envio do documento quando faltar menos
de um dia para o horário de utilização. Seria uma validação em
`DocumentoReservaService.enviar`, no mesmo estilo das que já existem.

**Por que não foi implementado:** depende de a escola confirmar se o envio do PDF
no sistema equivale à "entrega na Secretaria" do termo. Se a entrega continuar
sendo física, a regra pertence ao balcão e não ao sistema. **Essa é uma decisão
institucional, não técnica.**

Também vale registrar: o termo **não estabelece prazo para a decisão da
direção**. O PR #27 assume apenas o limite mínimo defensável, que é não autorizar
algo que já aconteceu, e **não inventa prazo**.

## Divergência sobre o mínimo de participantes

O modelo original em `src/main/resources/modelos/formulario-reserva-quadra.pdf`
foi mantido sem alteração e contém a cláusula de mínimo de dez alunos. A decisão
de projeto registrada em 22/10/2026 instrui o CourtTime a contar dez pessoas no
total, incluindo Aluno e Outro, na solicitação/formulário. A validação do código
segue a instrução mais recente do grupo; isso não prova que a cláusula do termo
tenha sido alterada pela ETEC. A equipe precisa obter uma versão atualizada ou
confirmação institucional antes de declarar o fluxo documental homologado.
