package br.com.courttime.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Preenche o formulário oficial da ETEC (reserva da quadra) escrevendo texto
 * por cima das páginas originais.
 *
 * O PDF oficial não tem campos de formulário, então nada é "preenchido": o
 * texto é acrescentado em coordenadas medidas no próprio documento, no modo
 * APPEND, que não reescreve o conteúdo original. Logotipos, cláusulas e
 * espaços de assinatura ficam exatamente como estão.
 *
 * Nunca escreve: assinaturas, presença ou nome do vigilante, data de
 * aprovação e assinatura da direção.
 *
 * As coordenadas são em pontos, com origem no canto INFERIOR esquerdo.
 */
public final class FormularioReservaPdf {

    /** A página de participantes do formulário oficial tem 30 linhas. */
    public static final int LINHAS_POR_PAGINA = 30;

    private static final float MENOR_FONTE = 6f;

    // Página 1 — linhas medidas no PDF oficial.
    // O traço desses campos é desenhado na altura da linha de base; o texto
    // fica 2,5 pt acima para não encostar nele.
    private static final float LINHA_NOME = 611.5f + 2.5f;
    private static final float LINHA_CPF = 598.0f + 2.5f;
    private static final float LINHA_DATA_UTILIZACAO = 115.3f;
    private static final float LINHA_HORARIO = 88.3f;
    private static final float LINHA_DATA_PREENCHIMENTO = 71.1f;

    // Página 2 — tabela de participantes.
    private static final float LINHA_1 = 655.2f + 1f;
    private static final float ALTURA_LINHA = 18.92f;
    private static final float X_NOME = 69f;
    private static final float LARGURA_NOME = 189f;
    private static final float X_RM_OU_RG = 265f;
    private static final float LARGURA_RM_OU_RG = 69f;
    private static final float CENTRO_MARCA_ALUNO = 353.5f;
    private static final float CENTRO_MARCA_OUTRO = 409.2f;
    private static final float LINHA_AVISO_CONTINUACAO = 712f;

    private FormularioReservaPdf() {
    }

    public static byte[] gerar(byte[] modelo, DadosFormularioReserva dados) throws IOException {
        List<PDDocument> copiasDoModelo = new ArrayList<>();
        try (PDDocument documento = Loader.loadPDF(modelo)) {
            if (documento.getNumberOfPages() != 2) {
                throw new IllegalStateException(
                        "O modelo do formulário oficial deve ter duas páginas");
            }
            PDType1Font fonte = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            preencherTermo(documento, documento.getPage(0), fonte, dados);

            // Mais de 30 participantes: a página oficial de participantes é
            // repetida (copiada do próprio modelo), sem omitir ninguém.
            List<DadosFormularioReserva.Participante> participantes = dados.participantes();
            int paginas = Math.max(1, (participantes.size() + LINHAS_POR_PAGINA - 1) / LINHAS_POR_PAGINA);
            for (int indice = 0; indice < paginas; indice++) {
                PDPage pagina;
                if (indice == 0) {
                    pagina = documento.getPage(1);
                } else {
                    PDDocument copia = Loader.loadPDF(modelo);
                    copiasDoModelo.add(copia);
                    pagina = documento.importPage(copia.getPage(1));
                }
                int inicio = indice * LINHAS_POR_PAGINA;
                int fim = Math.min(inicio + LINHAS_POR_PAGINA, participantes.size());
                preencherParticipantes(documento, pagina, fonte,
                        participantes.subList(inicio, fim), inicio, participantes.size(), paginas > 1);
            }

            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            documento.save(saida);
            return saida.toByteArray();
        } finally {
            for (PDDocument copia : copiasDoModelo) {
                copia.close();
            }
        }
    }

    private static void preencherTermo(
            PDDocument documento, PDPage pagina, PDType1Font fonte, DadosFormularioReserva dados)
            throws IOException {
        try (PDPageContentStream fluxo = new PDPageContentStream(
                documento, pagina, AppendMode.APPEND, true, true)) {
            // "Eu, ______, do curso ______," e "inscrito no CPF sob o nº ______"
            escreverAjustado(fluxo, fonte, dados.nomeSolicitante(), 44f, LINHA_NOME, 11f, 260f);
            escreverAjustado(fluxo, fonte, dados.curso(), 363f, LINHA_NOME, 11f, 205f);
            escreverAjustado(fluxo, fonte, dados.cpf(), 162f, LINHA_CPF, 11f, 142f);

            // "Reserva para o dia: ___/___/_____"
            escreverData(fluxo, fonte, dados.dataUtilizacao(), LINHA_DATA_UTILIZACAO,
                    135.2f, 168.8f, 171.9f, 205.5f, 208.6f, 262.5f);

            // "Horário: das ___h___ às ___h___"
            escreverHora(fluxo, fonte, dados.horarioInicio(), 92.2f, 139.3f, 147.2f, 180.8f);
            escreverHora(fluxo, fonte, dados.horarioFim(), 200.4f, 247.5f, 255.3f, 295.8f);

            // "Fernandópolis, ___/___/_____" — data em que o formulário foi gerado.
            escreverData(fluxo, fonte, dados.dataPreenchimento(), LINHA_DATA_PREENCHIMENTO,
                    424.1f, 457.8f, 460.9f, 494.5f, 497.6f, 551.5f);
        }
    }

    private static void preencherParticipantes(
            PDDocument documento,
            PDPage pagina,
            PDType1Font fonte,
            List<DadosFormularioReserva.Participante> participantes,
            int primeiroIndice,
            int total,
            boolean variasPaginas) throws IOException {
        try (PDPageContentStream fluxo = new PDPageContentStream(
                documento, pagina, AppendMode.APPEND, true, true)) {
            if (variasPaginas) {
                String aviso = "Participantes " + (primeiroIndice + 1) + " a "
                        + (primeiroIndice + participantes.size()) + " de " + total
                        + (primeiroIndice > 0 ? " (continuação da lista)" : "");
                escreverAjustado(fluxo, fonte, aviso, 21f, LINHA_AVISO_CONTINUACAO, 9f, 550f);
            }
            for (int linha = 0; linha < participantes.size(); linha++) {
                DadosFormularioReserva.Participante participante = participantes.get(linha);
                float y = LINHA_1 - ALTURA_LINHA * linha;
                escreverEmAteDuasLinhas(fluxo, fonte, participante.nomeCompleto(), X_NOME, y, LARGURA_NOME);
                escreverAjustado(fluxo, fonte, participante.rmOuRg(), X_RM_OU_RG, y, 9f, LARGURA_RM_OU_RG);
                // Um "X" dentro de "( ) Aluno" ou de "( ) Outro".
                float centro = participante.aluno() ? CENTRO_MARCA_ALUNO : CENTRO_MARCA_OUTRO;
                escreverCentralizado(fluxo, fonte, "X", centro - 4f, centro + 4f, y, 10f);
            }
        }
    }

    private static void escreverData(
            PDPageContentStream fluxo, PDType1Font fonte, LocalDate data, float y,
            float diaDe, float diaAte, float mesDe, float mesAte, float anoDe, float anoAte)
            throws IOException {
        if (data == null) {
            return;
        }
        float linha = y + 1.5f; // um pouco acima do sublinhado
        escreverCentralizado(fluxo, fonte, doisDigitos(data.getDayOfMonth()), diaDe, diaAte, linha, 11f);
        escreverCentralizado(fluxo, fonte, doisDigitos(data.getMonthValue()), mesDe, mesAte, linha, 11f);
        escreverCentralizado(fluxo, fonte, String.valueOf(data.getYear()), anoDe, anoAte, linha, 11f);
    }

    private static void escreverHora(
            PDPageContentStream fluxo, PDType1Font fonte, LocalTime hora,
            float horaDe, float horaAte, float minutoDe, float minutoAte) throws IOException {
        if (hora == null) {
            return;
        }
        float linha = LINHA_HORARIO + 1.5f;
        escreverCentralizado(fluxo, fonte, doisDigitos(hora.getHour()), horaDe, horaAte, linha, 11f);
        escreverCentralizado(fluxo, fonte, doisDigitos(hora.getMinute()), minutoDe, minutoAte, linha, 11f);
    }

    /** Escreve à esquerda, diminuindo a fonte até caber na largura do campo. */
    private static void escreverAjustado(
            PDPageContentStream fluxo, PDType1Font fonte, String texto,
            float x, float y, float tamanho, float larguraMaxima) throws IOException {
        String limpo = paraTextoDaFonte(fonte, texto);
        if (limpo.isEmpty()) {
            return;
        }
        float tamanhoFinal = tamanho;
        while (tamanhoFinal > MENOR_FONTE && largura(fonte, limpo, tamanhoFinal) > larguraMaxima) {
            tamanhoFinal -= 0.5f;
        }
        // Texto excepcionalmente longo: corta no limite do campo com reticências,
        // em vez de invadir a coluna vizinha.
        if (largura(fonte, limpo, tamanhoFinal) > larguraMaxima) {
            while (limpo.length() > 1 && largura(fonte, limpo + "…", tamanhoFinal) > larguraMaxima) {
                limpo = limpo.substring(0, limpo.length() - 1);
            }
            limpo = limpo.strip() + "…";
        }
        escrever(fluxo, fonte, limpo, x, y, tamanhoFinal);
    }

    /**
     * Nome do participante: uma linha, diminuindo a fonte até 7 pt; se ainda
     * não couber, quebra em duas linhas dentro da mesma célula. Assim nomes
     * longos aparecem inteiros em vez de cortados.
     */
    private static void escreverEmAteDuasLinhas(
            PDPageContentStream fluxo, PDType1Font fonte, String texto,
            float x, float y, float larguraMaxima) throws IOException {
        String limpo = paraTextoDaFonte(fonte, texto);
        float tamanho = 9f;
        while (tamanho > 7f && largura(fonte, limpo, tamanho) > larguraMaxima) {
            tamanho -= 0.5f;
        }
        int espaco = limpo.indexOf(' ');
        if (largura(fonte, limpo, tamanho) <= larguraMaxima || espaco < 0) {
            escreverAjustado(fluxo, fonte, limpo, x, y, tamanho, larguraMaxima);
            return;
        }
        // Quebra no espaço que deixa as duas linhas mais equilibradas.
        int melhor = espaco;
        for (int i = espaco; i >= 0; i = limpo.indexOf(' ', i + 1)) {
            if (Math.abs(limpo.length() - 2 * i) < Math.abs(limpo.length() - 2 * melhor)) {
                melhor = i;
            }
        }
        String primeira = limpo.substring(0, melhor).strip();
        String segunda = limpo.substring(melhor + 1).strip();
        float tamanhoDuasLinhas = 7f;
        while (tamanhoDuasLinhas > MENOR_FONTE
                && Math.max(largura(fonte, primeira, tamanhoDuasLinhas),
                        largura(fonte, segunda, tamanhoDuasLinhas)) > larguraMaxima) {
            tamanhoDuasLinhas -= 0.5f;
        }
        escreverAjustado(fluxo, fonte, primeira, x, y + 5f, tamanhoDuasLinhas, larguraMaxima);
        escreverAjustado(fluxo, fonte, segunda, x, y - 2.5f, tamanhoDuasLinhas, larguraMaxima);
    }

    private static void escreverCentralizado(
            PDPageContentStream fluxo, PDType1Font fonte, String texto,
            float de, float ate, float y, float tamanho) throws IOException {
        float x = de + (ate - de - largura(fonte, texto, tamanho)) / 2f;
        escrever(fluxo, fonte, texto, x, y, tamanho);
    }

    private static void escrever(
            PDPageContentStream fluxo, PDType1Font fonte, String texto,
            float x, float y, float tamanho) throws IOException {
        fluxo.beginText();
        fluxo.setFont(fonte, tamanho);
        fluxo.newLineAtOffset(x, y);
        fluxo.showText(texto);
        fluxo.endText();
    }

    private static float largura(PDType1Font fonte, String texto, float tamanho) throws IOException {
        return fonte.getStringWidth(texto) / 1000f * tamanho;
    }

    /**
     * A fonte padrão do PDF cobre o português (acentos e cedilha), mas não todo
     * o Unicode. Caracteres que ela não representa viram "?", para o PDF ser
     * gerado mesmo assim; quebras de linha e tabulações viram espaço.
     */
    static String paraTextoDaFonte(PDType1Font fonte, String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder resultado = new StringBuilder();
        texto.codePoints().forEach(codigo -> {
            if (Character.isWhitespace(codigo) || Character.isISOControl(codigo)) {
                resultado.append(' ');
                return;
            }
            String caractere = new String(Character.toChars(codigo));
            try {
                fonte.encode(caractere);
                resultado.append(caractere);
            } catch (IllegalArgumentException | IOException semGlifo) {
                resultado.append('?');
            }
        });
        return resultado.toString().replaceAll(" {2,}", " ").strip();
    }

    private static String doisDigitos(int valor) {
        return valor < 10 ? "0" + valor : String.valueOf(valor);
    }
}
