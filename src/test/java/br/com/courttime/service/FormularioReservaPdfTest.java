package br.com.courttime.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gera o PDF de verdade, a partir do formulário oficial, e confere o texto
 * extraído dele. Não depende de banco.
 */
class FormularioReservaPdfTest {

    @Test
    void preencheTermoEParticipantesSobreOModeloOriginal() throws IOException {
        byte[] pdf = FormularioReservaPdf.gerar(modelo(), dados(12, "Aluno Demonstração"));

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertEquals(2, documento.getNumberOfPages());
            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("Aluno Demonstração"));
            assertTrue(texto.contains("Técnico em Informática para Internet"));
            assertTrue(texto.contains("123.456.789-09"));
            assertTrue(texto.contains("Participante Fictício 1"));
            assertTrue(texto.contains("Participante Fictício 12"));
            assertTrue(texto.contains("RM-1012"));
            // O conteúdo original continua no documento.
            assertTrue(texto.contains("TERMO DE RESPONSABILIDADE"));
            assertTrue(texto.contains("assinatura do solicitante"));
            assertTrue(texto.contains("Agendamento APROVADO em"));
        }
    }

    @Test
    void naoEscreveEmAssinaturasPresencaVigilanteNemAprovacao() throws IOException {
        byte[] original = modelo();
        byte[] gerado = FormularioReservaPdf.gerar(original, dados(30, "Aluno Demonstração"));

        // Regiões que só podem ser preenchidas à mão (coordenadas de tela: origem no topo).
        assertEquals(textoDasRegioes(original), textoDasRegioes(gerado));
    }

    @Test
    void maisDeTrintaParticipantesGeramPaginasExtrasSemOmitirNinguem() throws IOException {
        byte[] pdf = FormularioReservaPdf.gerar(modelo(), dados(42, "Aluno Demonstração"));

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertEquals(3, documento.getNumberOfPages(),
                    "a página oficial de participantes é repetida para os excedentes");
            String texto = new PDFTextStripper().getText(documento);
            for (int i = 1; i <= 42; i++) {
                assertTrue(texto.contains("RM-" + (1000 + i)), "participante " + i + " ausente");
            }
            assertTrue(texto.contains("Participantes 31 a 42 de 42"));
        }
    }

    @Test
    void nomeLongoApareceInteiroECaracterSemGlifoNaoImpedeAGeracao() throws IOException {
        DadosFormularioReserva base = dados(10, "Aluno 🙂 Demonstração");
        List<DadosFormularioReserva.Participante> participantes = new ArrayList<>(base.participantes());
        participantes.set(0, new DadosFormularioReserva.Participante(
                "Maria Eduarda dos Santos Oliveira Ferreira da Conceição Albuquerque",
                "RM-9999", true));
        DadosFormularioReserva dados = new DadosFormularioReserva(
                base.nomeSolicitante(), base.curso(), base.cpf(), base.dataUtilizacao(),
                base.horarioInicio(), base.horarioFim(), base.dataPreenchimento(), participantes);

        byte[] pdf = FormularioReservaPdf.gerar(modelo(), dados);

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(documento).replaceAll("\\s+", " ");
            assertTrue(texto.contains("Maria Eduarda dos Santos Oliveira"));
            assertTrue(texto.contains("Ferreira da Conceição Albuquerque"),
                    "o nome longo é quebrado em duas linhas, não cortado");
            assertTrue(texto.contains("Aluno ? Demonstração"));
        }
    }

    @Test
    void r09VinculoDoOutroApareceNoEspacoDoFormulario() throws IOException {
        DadosFormularioReserva base = dados(10, "Ana Exemplo");
        List<DadosFormularioReserva.Participante> participantes = new ArrayList<>(base.participantes());
        participantes.set(0, new DadosFormularioReserva.Participante(
                "Carlos Pai Exemplo", "RG-1", false, "Pai/mãe de aluno"));
        DadosFormularioReserva dados = new DadosFormularioReserva(
                base.nomeSolicitante(), base.curso(), base.cpf(), base.dataUtilizacao(),
                base.horarioInicio(), base.horarioFim(), base.dataPreenchimento(), participantes);

        byte[] pdf = FormularioReservaPdf.gerar(modelo(), dados);

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            PDFTextStripperByArea area = new PDFTextStripperByArea();
            // Primeira linha da tabela, depois de "( ) Outro:".
            area.addRegion("vinculo", new Rectangle2D.Float(450, 172, 135, 20));
            area.extractRegions(documento.getPage(1));
            assertTrue(area.getTextForRegion("vinculo").contains("Pai/mãe de aluno"),
                    "o vínculo fica no traço de \"Outro:\"");
        }
    }

    private static String textoDasRegioes(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            PDFTextStripperByArea primeira = new PDFTextStripperByArea();
            primeira.addRegion("assinaturaDoSolicitante", new Rectangle2D.Float(180, 795, 240, 30));
            primeira.extractRegions(documento.getPage(0));

            PDFTextStripperByArea segunda = new PDFTextStripperByArea();
            segunda.addRegion("presenca", new Rectangle2D.Float(30, 175, 35, 570));
            segunda.addRegion("vigilante", new Rectangle2D.Float(15, 790, 200, 45));
            segunda.addRegion("aprovacaoDaDirecao", new Rectangle2D.Float(225, 755, 340, 80));
            segunda.extractRegions(documento.getPage(1));

            return primeira.getTextForRegion("assinaturaDoSolicitante")
                    + "|" + segunda.getTextForRegion("presenca")
                    + "|" + segunda.getTextForRegion("vigilante")
                    + "|" + segunda.getTextForRegion("aprovacaoDaDirecao");
        }
    }

    private static DadosFormularioReserva dados(int quantidade, String solicitante) {
        List<DadosFormularioReserva.Participante> participantes = new ArrayList<>();
        for (int i = 1; i <= quantidade; i++) {
            participantes.add(new DadosFormularioReserva.Participante(
                    "Participante Fictício " + i, "RM-" + (1000 + i), i % 7 != 0));
        }
        return new DadosFormularioReserva(
                solicitante,
                "Técnico em Informática para Internet",
                "123.456.789-09",
                LocalDate.of(2026, 10, 27),
                LocalTime.of(19, 30),
                LocalTime.of(20, 30),
                LocalDate.of(2026, 10, 20),
                participantes);
    }

    static byte[] modelo() throws IOException {
        try (InputStream entrada = new ClassPathResource(FormularioOficialService.MODELO).getInputStream()) {
            return entrada.readAllBytes();
        }
    }
}
