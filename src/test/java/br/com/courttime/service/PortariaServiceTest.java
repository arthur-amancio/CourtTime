package br.com.courttime.service;

import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.LocalPagamento;
import br.com.courttime.entity.PagamentoAdicional;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.PresencaParticipante;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.PagamentoAdicionalRepository;
import br.com.courttime.repository.ParticipanteReservaRepository;
import br.com.courttime.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortariaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ParticipanteReservaRepository participanteRepository;
    @Mock
    private ParticipanteReservaService participanteService;
    @Mock
    private PagamentoAdicionalRepository pagamentoRepository;

    private PortariaService service;
    private Usuario seguranca;
    private Usuario secretaria;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        service = new PortariaService(
                reservaRepository, participanteRepository, participanteService, pagamentoRepository);
        seguranca = usuario(1L, Perfil.SEGURANCA);
        secretaria = usuario(2L, Perfil.SECRETARIA);
        reserva = reservaDeHoje();
    }

    // ---------------------------------------------------------------- R10: a conta

    @Test
    void r10TrocaDeDuasPessoasNaoCobraNada() {
        // Lista de 10 paga; 2 faltaram e 2 foram acrescentadas: continuam 10.
        assertEquals(0, PortariaService.calcularPessoasAPagar(8 + 2, 10));
    }

    @Test
    void r10DuasPessoasAMaisCustamQuatroReais() {
        long pessoas = PortariaService.calcularPessoasAPagar(10 + 2, 10);

        assertEquals(2, pessoas);
        assertEquals(new BigDecimal("4.00"), PortariaService.calcularValor(pessoas));
    }

    @Test
    void r10QuemFaltaNaoGeraDevolucao() {
        assertEquals(0, PortariaService.calcularPessoasAPagar(10, 12));
    }

    @Test
    void r10ResumoContaPresentesDaListaEAcrescentados() {
        List<ParticipanteReserva> participantes = new ArrayList<>(lista(10, PresencaParticipante.PRESENTE));
        participantes.add(acrescentado("Visitante 1", "RG-1"));
        participantes.add(acrescentado("Visitante 2", "RG-2"));
        prepararParticipantes(participantes, List.of());

        ResumoPortaria resumo = service.resumir(reserva);

        assertEquals(12, resumo.presentes());
        assertEquals(10, resumo.pagos());
        assertEquals(2, resumo.pessoasAPagar());
        assertEquals(new BigDecimal("4.00"), resumo.valorAPagar());
        assertTrue(resumo.mostrarValores());
    }

    @Test
    void r10SemPessoasAMaisATelaNaoMostraValores() {
        prepararParticipantes(lista(10, PresencaParticipante.PRESENTE), List.of());

        ResumoPortaria resumo = service.resumir(reserva);

        assertEquals(0, resumo.pessoasAPagar());
        assertFalse(resumo.mostrarValores());
    }

    @Test
    void r10AcrescentarUsaAsMesmasRegrasDoCadastro() {
        when(reservaRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(reserva));

        service.acrescentarPessoa(20L, seguranca, "Visitante", "RG-9",
                CategoriaParticipante.ALUNO, null);

        verify(participanteService).acrescentarNaPortaria(
                reserva, seguranca, "Visitante", "RG-9", CategoriaParticipante.ALUNO, null);
    }

    // ------------------------------------------------------------- R11: liberação

    @Test
    void r11ComMenosDeDezPresentesAQuadraNaoELiberada() {
        when(reservaRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(reserva));
        List<ParticipanteReserva> participantes = new ArrayList<>(lista(8, PresencaParticipante.PRESENTE));
        participantes.addAll(lista(2, PresencaParticipante.AUSENTE));
        prepararParticipantes(participantes, List.of());

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> service.liberarQuadra(20L, seguranca));

        assertTrue(erro.getMessage().contains("8 pessoas presentes"));
        assertFalse(reserva.isLiberada());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void r11ComValorPendenteAQuadraNaoELiberada() {
        when(reservaRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(reserva));
        List<ParticipanteReserva> participantes = new ArrayList<>(lista(10, PresencaParticipante.PRESENTE));
        participantes.add(acrescentado("Visitante", "RG-1"));
        prepararParticipantes(participantes, List.of());

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> service.liberarQuadra(20L, seguranca));

        assertTrue(erro.getMessage().contains("falta pagar"));
        assertFalse(reserva.isLiberada());
    }

    @Test
    void r11ComDezPresentesDeQualquerCategoriaEValorPagoAQuadraELiberada() {
        when(reservaRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(reserva));
        // 8 alunos da lista + 2 acrescentados da categoria Outro, já pagos.
        List<ParticipanteReserva> participantes = new ArrayList<>(lista(10, PresencaParticipante.PRESENTE));
        participantes.get(0).registrarPresenca(PresencaParticipante.AUSENTE, seguranca, Instant.now());
        participantes.get(1).registrarPresenca(PresencaParticipante.AUSENTE, seguranca, Instant.now());
        ParticipanteReserva outro = acrescentado("Pai de aluno", "RG-1");
        outro.setCategoria(CategoriaParticipante.OUTRO);
        participantes.add(outro);
        participantes.add(acrescentado("Ex-aluno", "RG-2"));
        prepararParticipantes(participantes, List.of());

        service.liberarQuadra(20L, seguranca);

        assertTrue(reserva.isLiberada());
        assertSame(seguranca, reserva.getLiberadaPor());
        verify(reservaRepository).save(reserva);
    }

    @Test
    void r11ForaDoDiaDaReservaNadaAconteceNaPortaria() {
        reserva.setDataUtilizacao(LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO).plusDays(1));
        when(reservaRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(reserva));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> service.liberarQuadra(20L, seguranca));

        assertEquals(PortariaService.MENSAGEM_FORA_DO_DIA, erro.getMessage());
        verifyNoInteractions(participanteRepository, pagamentoRepository);
    }

    @Test
    void r11SomenteASegurancaLiberaEAcrescentaPessoas() {
        assertThrows(AccessDeniedException.class, () -> service.liberarQuadra(20L, secretaria));
        assertThrows(AccessDeniedException.class, () -> service.acrescentarPessoa(
                20L, secretaria, "Nome", "RG", CategoriaParticipante.ALUNO, null));
        assertThrows(AccessDeniedException.class,
                () -> service.registrarPagamentoNaSecretaria(20L, seguranca));
        verifyNoInteractions(reservaRepository);
    }

    // --------------------------------------------------------------- R11: pagamento

    @Test
    void r11PagamentoNaPortariaUsaOValorCalculadoEAguardaRepasse() {
        when(reservaRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(reserva));
        List<ParticipanteReserva> participantes = new ArrayList<>(lista(10, PresencaParticipante.PRESENTE));
        participantes.add(acrescentado("Visitante 1", "RG-1"));
        participantes.add(acrescentado("Visitante 2", "RG-2"));
        prepararParticipantes(participantes, List.of());
        when(pagamentoRepository.save(any(PagamentoAdicional.class))).thenAnswer(i -> i.getArgument(0));

        PagamentoAdicional pagamento = service.registrarPagamentoNaPortaria(20L, seguranca);

        assertEquals(2, pagamento.getQuantidadePessoas());
        assertEquals(new BigDecimal("4.00"), pagamento.getValor());
        assertEquals(LocalPagamento.PORTARIA, pagamento.getLocal());
        assertSame(seguranca, pagamento.getRegistradoPor());
        assertTrue(pagamento.aguardaRepasse());
    }

    @Test
    void r11DepoisDoPagamentoNaoHaMaisValorAPagar() {
        List<ParticipanteReserva> participantes = new ArrayList<>(lista(10, PresencaParticipante.PRESENTE));
        participantes.add(acrescentado("Visitante 1", "RG-1"));
        participantes.add(acrescentado("Visitante 2", "RG-2"));
        PagamentoAdicional pago = pagamento(2, LocalPagamento.SECRETARIA);
        prepararParticipantes(participantes, List.of(pago));

        ResumoPortaria resumo = service.resumir(reserva);

        assertEquals(12, resumo.pagos());
        assertEquals(0, resumo.pessoasAPagar());
    }

    @Test
    void r11SemValorAPagarNaoHaPagamentoParaRegistrar() {
        when(reservaRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(reserva));
        prepararParticipantes(lista(10, PresencaParticipante.PRESENTE), List.of());

        assertThrows(IllegalStateException.class,
                () -> service.registrarPagamentoNaSecretaria(20L, secretaria));
        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    void r11SecretariaConfirmaORepasseDaPortaria() {
        PagamentoAdicional pagamento = pagamento(2, LocalPagamento.PORTARIA);
        pagamento.setId(70L);
        when(pagamentoRepository.findById(70L)).thenReturn(Optional.of(pagamento));

        service.confirmarRepasse(70L, secretaria);

        assertFalse(pagamento.aguardaRepasse());
        assertSame(secretaria, pagamento.getRepasseConfirmadoPor());
        assertNotNull(pagamento.getRepasseConfirmadoEm());
        verify(pagamentoRepository).save(pagamento);
    }

    @Test
    void r11PagamentoFeitoNaSecretariaNaoTemRepasse() {
        PagamentoAdicional pagamento = pagamento(2, LocalPagamento.SECRETARIA);
        pagamento.setId(71L);
        when(pagamentoRepository.findById(71L)).thenReturn(Optional.of(pagamento));

        assertThrows(IllegalStateException.class, () -> service.confirmarRepasse(71L, secretaria));
    }

    // ------------------------------------------------------------------ auxiliares

    private void prepararParticipantes(
            List<ParticipanteReserva> participantes, List<PagamentoAdicional> pagamentos) {
        when(participanteRepository.findByReservaOrderByNomeCompletoAscIdAsc(reserva))
                .thenReturn(participantes);
        when(pagamentoRepository.findByReservaOrderByRegistradoEmAscIdAsc(reserva))
                .thenReturn(pagamentos);
    }

    private List<ParticipanteReserva> lista(int quantidade, PresencaParticipante presenca) {
        List<ParticipanteReserva> lista = new ArrayList<>();
        for (int i = 1; i <= quantidade; i++) {
            ParticipanteReserva participante = new ParticipanteReserva();
            participante.setReserva(reserva);
            participante.setNomeCompleto("Aluno " + i + " " + presenca);
            participante.setRmOuRg("RM-" + presenca + "-" + i);
            participante.setCategoria(CategoriaParticipante.ALUNO);
            participante.registrarPresenca(presenca, seguranca, Instant.now());
            lista.add(participante);
        }
        return lista;
    }

    private ParticipanteReserva acrescentado(String nome, String rg) {
        ParticipanteReserva participante = new ParticipanteReserva();
        participante.setReserva(reserva);
        participante.setNomeCompleto(nome);
        participante.setRmOuRg(rg);
        participante.setCategoria(CategoriaParticipante.ALUNO);
        participante.marcarAdicionadoNaPortaria(seguranca, Instant.now());
        return participante;
    }

    private PagamentoAdicional pagamento(int pessoas, LocalPagamento local) {
        PagamentoAdicional pagamento = new PagamentoAdicional();
        pagamento.setReserva(reserva);
        pagamento.setQuantidadePessoas(pessoas);
        pagamento.setValor(PortariaService.calcularValor(pessoas));
        pagamento.setLocal(local);
        pagamento.setRegistradoPor(local == LocalPagamento.PORTARIA ? seguranca : secretaria);
        pagamento.setRegistradoEm(Instant.now());
        return pagamento;
    }

    private Reserva reservaDeHoje() {
        Quadra quadra = new Quadra();
        quadra.setId(3L);
        quadra.setNome("Quadra Poliesportiva");
        Reserva nova = new Reserva();
        nova.setId(20L);
        nova.setQuadra(quadra);
        nova.setSolicitante(usuario(4L, Perfil.ALUNO));
        nova.setDataUtilizacao(LocalDate.now(CalendarioDisponibilidadeService.FUSO_HORARIO));
        nova.setHorarioInicio(LocalTime.of(8, 0));
        nova.setHorarioFim(LocalTime.of(9, 0));
        nova.setFinalidade("Treino da turma");
        nova.setStatus(StatusReserva.APROVADA);
        return nova;
    }

    private Usuario usuario(Long id, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuário " + id);
        usuario.setEmail("usuario" + id + "@teste.local");
        usuario.setSenha("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuario;
    }
}
