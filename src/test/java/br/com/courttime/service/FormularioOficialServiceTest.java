package br.com.courttime.service;

import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.entity.VinculoParticipante;
import br.com.courttime.repository.ParticipanteReservaRepository;
import br.com.courttime.repository.ReservaRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormularioOficialServiceTest {

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ParticipanteReservaRepository participanteRepository;
    @Mock
    private CalendarioDisponibilidadeService calendarioService;

    private FormularioOficialService service;
    private Usuario aluno;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        service = new FormularioOficialService(reservaRepository, participanteRepository, calendarioService);
        aluno = new Usuario();
        aluno.setId(10L);
        aluno.setNome("Aluno Demonstração");
        aluno.setEmail("aluno@escola.test");
        aluno.setSenha("hash");
        aluno.setPerfil(Perfil.ALUNO);
        aluno.setAtivo(true);
        reserva = new Reserva();
        reserva.setId(30L);
        reserva.setSolicitante(aluno);
        reserva.setDataUtilizacao(LocalDate.of(2026, 10, 27));
        reserva.setHorarioInicio(LocalTime.of(8, 0));
        reserva.setHorarioFim(LocalTime.of(9, 0));
        reserva.setFinalidade("Treino");
        reserva.setStatus(StatusReserva.PENDENTE_DOCUMENTACAO);
    }

    @Test
    void geraPdfComDadosDaReservaDosParticipantesECpfDigitado() throws Exception {
        when(reservaRepository.findByIdAndSolicitante(30L, aluno)).thenReturn(Optional.of(reserva));
        when(participanteRepository.findByReservaOrderByNomeCompletoAscIdAsc(reserva))
                .thenReturn(participantes(10));
        when(calendarioService.obterDataAtual()).thenReturn(LocalDate.of(2026, 10, 20));

        byte[] pdf = service.gerar(30L, aluno, "12345678909", " Técnico em  Informática ");

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("Aluno Demonstração"));
            assertTrue(texto.contains("123.456.789-09"), "CPF formatado");
            assertTrue(texto.contains("Técnico em Informática"));
            assertTrue(texto.contains("Participante 10"));
        }
        // Nada é gravado: o serviço só lê a reserva e os participantes.
        verifyNoMoreInteractions(reservaRepository, participanteRepository);
    }

    @Test
    void reservaDeOutroAlunoENegada() {
        when(reservaRepository.findByIdAndSolicitante(30L, aluno)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> service.gerar(30L, aluno, "12345678909", "Informática"));
    }

    @Test
    void outrosPerfisNaoGeramFormulario() {
        aluno.setPerfil(Perfil.SECRETARIA);

        assertThrows(AccessDeniedException.class,
                () -> service.gerar(30L, aluno, "12345678909", "Informática"));
    }

    @Test
    void soGeraEnquantoAguardaDocumentacao() {
        reserva.setStatus(StatusReserva.APROVADA);
        when(reservaRepository.findByIdAndSolicitante(30L, aluno)).thenReturn(Optional.of(reserva));

        assertThrows(IllegalStateException.class,
                () -> service.gerar(30L, aluno, "12345678909", "Informática"));
    }

    @Test
    void rascunhoNaoGeraFormularioMesmoComRequisicaoDireta() {
        reserva.setRascunho(true);
        when(reservaRepository.findByIdAndSolicitante(30L, aluno)).thenReturn(Optional.of(reserva));

        assertThrows(IllegalStateException.class,
                () -> service.gerar(30L, aluno, "12345678909", "Informática"));
    }

    @Test
    void exigeParticipantesCadastrados() {
        when(reservaRepository.findByIdAndSolicitante(30L, aluno)).thenReturn(Optional.of(reserva));
        when(participanteRepository.findByReservaOrderByNomeCompletoAscIdAsc(reserva)).thenReturn(List.of());

        assertThrows(IllegalStateException.class,
                () -> service.gerar(30L, aluno, "12345678909", "Informática"));
    }

    @Test
    void formularioExigeDezParticipantesDeQualquerCategoria() {
        when(reservaRepository.findByIdAndSolicitante(30L, aluno)).thenReturn(Optional.of(reserva));
        List<ParticipanteReserva> pessoas = participantes(10);
        pessoas.forEach(pessoa -> {
            pessoa.setCategoria(CategoriaParticipante.OUTRO);
            pessoa.setVinculo(VinculoParticipante.PAI_OU_MAE);
        });
        when(participanteRepository.findByReservaOrderByNomeCompletoAscIdAsc(reserva))
                .thenReturn(pessoas);
        when(calendarioService.obterDataAtual()).thenReturn(LocalDate.of(2026, 10, 20));

        byte[] pdf = service.gerar(30L, aluno, "12345678909", "Informática");

        assertTrue(pdf.length > 0);
    }

    @Test
    void formularioRecusaNoveParticipantes() {
        when(reservaRepository.findByIdAndSolicitante(30L, aluno)).thenReturn(Optional.of(reserva));
        when(participanteRepository.findByReservaOrderByNomeCompletoAscIdAsc(reserva))
                .thenReturn(participantes(9));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> service.gerar(30L, aluno, "12345678909", "Informática"));

        assertTrue(erro.getMessage().contains("10 participantes"));
    }

    @Test
    void modeloAusenteENomeadoSemCriarTermoSubstituto() {
        FormularioOficialService semModelo = new FormularioOficialService(
                reservaRepository, participanteRepository, calendarioService, "modelos/nao-existe.pdf");
        when(reservaRepository.findByIdAndSolicitante(30L, aluno)).thenReturn(Optional.of(reserva));
        when(participanteRepository.findByReservaOrderByNomeCompletoAscIdAsc(reserva))
                .thenReturn(participantes(10));
        when(calendarioService.obterDataAtual()).thenReturn(LocalDate.of(2026, 10, 20));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> semModelo.gerar(30L, aluno, "12345678909", "Informática"));
        assertTrue(erro.getMessage().contains("modelo oficial"));
    }

    @Test
    void cpfEConferidoPelosDigitosVerificadores() {
        assertEquals("123.456.789-09", FormularioOficialService.validarCpf("123.456.789-09"));
        assertEquals("529.982.247-25", FormularioOficialService.validarCpf("52998224725"));
        for (String invalido : new String[] {"", "123", "12345678900", "11111111111", "abc.def.ghi-jk", null}) {
            assertThrows(IllegalArgumentException.class, () -> FormularioOficialService.validarCpf(invalido));
        }
    }

    @Test
    void cursoEObrigatorioEComTamanhoLimitado() {
        assertThrows(IllegalArgumentException.class, () -> FormularioOficialService.validarCurso("   "));
        assertThrows(IllegalArgumentException.class, () -> FormularioOficialService.validarCurso("x".repeat(81)));
        assertEquals("Informática para Internet",
                FormularioOficialService.validarCurso("  Informática   para Internet "));
    }

    private List<ParticipanteReserva> participantes(int quantidade) {
        List<ParticipanteReserva> lista = new ArrayList<>();
        for (int i = 1; i <= quantidade; i++) {
            ParticipanteReserva participante = new ParticipanteReserva();
            participante.setReserva(reserva);
            participante.setNomeCompleto("Participante " + i);
            participante.setRmOuRg("RM-" + i);
            participante.setCategoria(CategoriaParticipante.ALUNO);
            lista.add(participante);
        }
        return lista;
    }
}
