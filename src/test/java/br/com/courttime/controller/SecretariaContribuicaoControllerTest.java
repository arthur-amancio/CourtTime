package br.com.courttime.controller;

import br.com.courttime.config.SecurityConfig;
import br.com.courttime.entity.AjusteContribuicaoApm;
import br.com.courttime.entity.ConferenciaDocumento;
import br.com.courttime.entity.ContribuicaoApm;
import br.com.courttime.entity.DocumentoReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.ContribuicaoApmService;
import br.com.courttime.service.ContribuicaoPendente;
import br.com.courttime.service.DetalheContribuicaoApm;
import br.com.courttime.service.PortariaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecretariaContribuicaoController.class)
@Import(SecurityConfig.class)
class SecretariaContribuicaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContribuicaoApmService contribuicaoService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private PortariaService portariaService;

    private Usuario secretaria;
    private Reserva reserva;
    private ContribuicaoApm contribuicao;
    private DetalheContribuicaoApm detalhe;

    @BeforeEach
    void setUp() {
        secretaria = usuario(10L, "Secretaria da escola", Perfil.SECRETARIA);
        Usuario aluno = usuario(11L, "Ana Aluna", Perfil.ALUNO);
        reserva = new Reserva();
        reserva.setId(20L);
        reserva.setSolicitante(aluno);
        reserva.setDataUtilizacao(LocalDate.of(2030, 10, 21));
        reserva.setHorarioInicio(LocalTime.of(8, 0));
        reserva.setHorarioFim(LocalTime.of(9, 0));
        reserva.setFinalidade("Treino da turma");
        reserva.setStatus(StatusReserva.PENDENTE_DOCUMENTACAO);

        DocumentoReserva documento = new DocumentoReserva();
        documento.setId(30L);
        documento.setReserva(reserva);
        documento.setVersao(2);
        ConferenciaDocumento conferencia = new ConferenciaDocumento();
        conferencia.setDocumento(documento);

        contribuicao = new ContribuicaoApm();
        contribuicao.setId(40L);
        contribuicao.setReserva(reserva);
        contribuicao.setConferenciaDocumento(conferencia);
        contribuicao.setValor(new BigDecimal("20.00"));
        contribuicao.setObservacoes("Recebimento no balcão.");
        contribuicao.setRegistradaPor(secretaria);
        ReflectionTestUtils.setField(
                contribuicao, "registradaEm", Instant.parse("2026-10-18T10:00:00Z"));
        detalhe = new DetalheContribuicaoApm(contribuicao, List.of());
    }

    @Test
    void usuarioNaoAutenticadoEEnviadoAoLogin() throws Exception {
        mockMvc.perform(get("/secretaria/contribuicoes"))
                .andExpect(status().is3xxRedirection());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALUNO", "ADMINISTRADOR", "SEGURANCA"})
    void outrosPerfisNaoConsultamNemRegistram(String perfil) throws Exception {
        mockMvc.perform(get("/secretaria/contribuicoes")
                        .with(user("usuario@escola.test").roles(perfil)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/secretaria/contribuicoes/solicitacoes/20")
                        .with(user("usuario@escola.test").roles(perfil))
                        .with(csrf())
                        .param("valor", "20.00"))
                .andExpect(status().isForbidden());
    }

    @Test
    void r03SecretariaVeValorCalculadoSemCampoDeValorLivre() throws Exception {
        prepararSecretaria();
        when(contribuicaoService.listarPendentes(secretaria))
                .thenReturn(List.of(new ContribuicaoPendente(reserva, 11L)));
        when(contribuicaoService.listarRegistradas(secretaria))
                .thenReturn(List.of(contribuicao));

        mockMvc.perform(get("/secretaria/contribuicoes")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().string(containsString("Ana Aluna")))
                .andExpect(content().string(containsString("R$ 20,00")))
                .andExpect(content().string(containsString(
                        "Histórico de contribuições presenciais")))
                .andExpect(content().string(containsString(
                        "11 participantes × R$ 2,00 = R$ 22,00")))
                .andExpect(content().string(containsString("Registrar recebimento de R$ 22,00")))
                .andExpect(content().string(not(containsString("name=\"valor\""))))
                .andExpect(content().string(not(containsString("Referência institucional pendente"))));
    }

    @Test
    void r04FilaMostraCreditoDiferencaEExcedente() throws Exception {
        prepararSecretaria();
        when(contribuicaoService.listarPendentes(secretaria)).thenReturn(List.of(
                new ContribuicaoPendente(reserva, 10L, 3L, new BigDecimal("22.00"))));
        when(contribuicaoService.listarRegistradas(secretaria)).thenReturn(List.of());

        mockMvc.perform(get("/secretaria/contribuicoes")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("da reserva cancelada nº 3")))
                .andExpect(content().string(containsString("R$ 0,00")))
                .andExpect(content().string(containsString("Aplicar crédito")))
                .andExpect(content().string(containsString("R$ 2,00 — pendente de decisão da ETEC")));
    }


    @Test
    void r11SecretariaRegistraValorAMaisEConfirmaRepasseComCsrf() throws Exception {
        mockMvc.perform(post("/secretaria/contribuicoes/portaria/20/pagamento")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(portariaService);

        prepararSecretaria();
        mockMvc.perform(post("/secretaria/contribuicoes/portaria/20/pagamento")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA"))
                        .with(csrf())
                        .param("valor", "999.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/secretaria/contribuicoes"))
                .andExpect(flash().attribute("mensagemSucesso",
                        "Valor a mais registrado. A portaria já pode liberar a entrada."));
        // O valor vem do cálculo do serviço; o enviado à mão é ignorado.
        verify(portariaService).registrarPagamentoNaSecretaria(20L, secretaria);

        mockMvc.perform(post("/secretaria/contribuicoes/portaria/repasses/70")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensagemSucesso", "Repasse da portaria confirmado."));
        verify(portariaService).confirmarRepasse(70L, secretaria);
    }

    @Test
    void r11SegurancaNaoUsaAsAcoesDaSecretaria() throws Exception {
        mockMvc.perform(post("/secretaria/contribuicoes/portaria/20/pagamento")
                        .with(user("seguranca@escola.test").roles("SEGURANCA"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(portariaService);
    }

    @Test
    void registroUsaSecretariaDaSessaoEExigeCsrf() throws Exception {
        mockMvc.perform(post("/secretaria/contribuicoes/solicitacoes/20")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA"))
                        .param("valor", "20.00"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(contribuicaoService);

        prepararSecretaria();
        when(contribuicaoService.registrar(
                20L, secretaria, "Recebido presencialmente."))
                .thenReturn(detalhe);
        mockMvc.perform(post("/secretaria/contribuicoes/solicitacoes/20")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA"))
                        .with(csrf())
                        .param("valor", "20.00")
                        .param("observacoes", "Recebido presencialmente.")
                        .param("registradaPorId", "999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/secretaria/contribuicoes/solicitacoes/20"))
                .andExpect(flash().attribute(
                        "mensagemSucesso", "Contribuição presencial registrada."));

        // Um valor enviado à mão é ignorado: o serviço calcula o devido (R03.2).
        verify(contribuicaoService).registrar(
                20L, secretaria, "Recebido presencialmente.");
    }

    @Test
    void erroDeElegibilidadeNaoAlegaRecebimento() throws Exception {
        prepararSecretaria();
        when(contribuicaoService.registrar(20L, secretaria, ""))
                .thenThrow(new IllegalStateException(
                        "A versão mais recente do documento ainda não foi validada"));

        mockMvc.perform(post("/secretaria/contribuicoes/solicitacoes/20")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA"))
                        .with(csrf())
                        .param("valor", "20.00")
                        .param("observacoes", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/secretaria/contribuicoes"))
                .andExpect(flash().attribute("mensagemErro", containsString(
                        "ainda não foi validada")));
    }

    @Test
    void detalheExibeRegistroEAjusteAppendOnly() throws Exception {
        prepararSecretaria();
        AjusteContribuicaoApm ajuste = new AjusteContribuicaoApm();
        ajuste.setContribuicao(contribuicao);
        ajuste.setValorAnterior(new BigDecimal("18.00"));
        ajuste.setValorNovo(new BigDecimal("20.00"));
        ajuste.setObservacoesAnteriores("Valor digitado incorretamente.");
        ajuste.setObservacoesNovas("Recebimento no balcão.");
        ajuste.setMotivo("Correção de digitação.");
        ajuste.setAjustadaPor(secretaria);
        ReflectionTestUtils.setField(
                ajuste, "ajustadaEm", Instant.parse("2026-10-18T11:00:00Z"));
        when(contribuicaoService.obterPorReserva(20L, secretaria))
                .thenReturn(new DetalheContribuicaoApm(contribuicao, List.of(ajuste)));

        mockMvc.perform(get("/secretaria/contribuicoes/solicitacoes/20")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Responsável pelo registro")))
                .andExpect(content().string(containsString("Versão 2")))
                .andExpect(content().string(containsString("R$ 18,00 → R$ 20,00")))
                .andExpect(content().string(containsString("Correção de digitação.")))
                .andExpect(content().string(containsString(
                        "Histórico de ajustes da contribuição")))
                .andExpect(content().string(containsString(
                        "não aprova a reserva")))
                .andExpect(content().string(containsString("Não calculado")));
    }

    @Test
    void r03DetalheMostraContaDoValorDevido() throws Exception {
        prepararSecretaria();
        contribuicao.setValorDevido(new BigDecimal("22.00"));
        contribuicao.setValor(new BigDecimal("22.00"));
        when(contribuicaoService.obterPorReserva(20L, secretaria))
                .thenReturn(new DetalheContribuicaoApm(contribuicao, List.of()));

        mockMvc.perform(get("/secretaria/contribuicoes/solicitacoes/20")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "11 participantes × R$ 2,00 = R$ 22,00")))
                .andExpect(content().string(not(containsString("Não calculado"))));
    }

    @Test
    void ajusteUsaOperadorAutenticadoERedirecionaAoRegistroReal() throws Exception {
        prepararSecretaria();
        when(contribuicaoService.ajustar(
                40L, secretaria, "22.00", "Valor corrigido.", "Conferência do recibo."))
                .thenReturn(detalhe);

        mockMvc.perform(post("/secretaria/contribuicoes/40/ajustes")
                        .with(user(secretaria.getEmail()).roles("SECRETARIA"))
                        .with(csrf())
                        .param("novoValor", "22.00")
                        .param("novasObservacoes", "Valor corrigido.")
                        .param("motivo", "Conferência do recibo.")
                        .param("reservaId", "999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/secretaria/contribuicoes/solicitacoes/20"))
                .andExpect(flash().attribute(
                        "mensagemSucesso", "Ajuste registrado no histórico."));
    }

    private void prepararSecretaria() {
        when(usuarioRepository.findByEmailIgnoreCase(secretaria.getEmail()))
                .thenReturn(Optional.of(secretaria));
    }

    private Usuario usuario(Long id, String nome, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setEmail("usuario" + id + "@teste.local");
        usuario.setSenha("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuario;
    }
}
