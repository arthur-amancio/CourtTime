package br.com.courttime.controller;

import br.com.courttime.config.SecurityConfig;
import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.PresencaParticipante;
import br.com.courttime.entity.Usuario;
import br.com.courttime.entity.VinculoParticipante;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.ConsultaSegurancaService;
import br.com.courttime.service.DetalheReservaAutorizada;
import br.com.courttime.service.ParticipanteConferencia;
import br.com.courttime.service.PortariaService;
import br.com.courttime.service.ReservaAutorizadaResumo;
import br.com.courttime.service.ResumoPortaria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.doThrow;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SegurancaReservaController.class)
@Import(SecurityConfig.class)
class SegurancaReservaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultaSegurancaService consultaSegurancaService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private PortariaService portariaService;

    private Usuario seguranca;
    private ReservaAutorizadaResumo resumo;

    @BeforeEach
    void setUp() {
        seguranca = usuario(1L, "Agente da segurança", Perfil.SEGURANCA);
        resumo = new ReservaAutorizadaResumo(
                20L,
                LocalDate.of(2030, 10, 21),
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                "Quadra Poliesportiva",
                "Ana Aluna",
                1);
    }

    @Test
    void usuarioNaoAutenticadoEEnviadoAoLogin() throws Exception {
        mockMvc.perform(get("/seguranca/reservas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALUNO", "ADMINISTRADOR", "SECRETARIA"})
    void outrosPerfisRecebemAcessoNegado(String perfil) throws Exception {
        mockMvc.perform(get("/seguranca/reservas")
                        .with(user("usuario@escola.test").roles(perfil)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/seguranca/reservas/20")
                        .with(user("usuario@escola.test").roles(perfil)))
                .andExpect(status().isForbidden());
    }

    @Test
    void segurancaConsultaListaEDetalheSomenteComDadosNecessarios() throws Exception {
        prepararSeguranca();
        when(consultaSegurancaService.listarAutorizadas(seguranca))
                .thenReturn(List.of(resumo));
        when(consultaSegurancaService.detalhar(20L, seguranca))
                .thenReturn(new DetalheReservaAutorizada(
                        resumo,
                        List.of(new ParticipanteConferencia(
                                "Carlos Participante", "RM-123", "Aluno"))));

        mockMvc.perform(get("/seguranca/reservas")
                        .with(user(seguranca.getEmail()).roles("SEGURANCA")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().string(containsString("Reservas autorizadas")))
                .andExpect(content().string(containsString("Quadra Poliesportiva")))
                .andExpect(content().string(containsString("Ana Aluna")));

        mockMvc.perform(get("/seguranca/reservas/20")
                        .with(user(seguranca.getEmail()).roles("SEGURANCA")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().string(containsString("Autorizada")))
                .andExpect(content().string(containsString("Carlos Participante")))
                .andExpect(content().string(containsString("RM-123")))
                .andExpect(content().string(not(containsString("aluna@teste.local"))))
                .andExpect(content().string(not(containsString("PDF"))))
                .andExpect(content().string(not(containsString("Contribuição"))))
                .andExpect(content().string(not(containsString("Rejeição"))))
                .andExpect(content().string(not(containsString("Registrar ocorrência"))));
    }

    @Test
    void reservaSemAutorizacaoAtualRetornaNaoEncontrado() throws Exception {
        prepararSeguranca();
        when(consultaSegurancaService.detalhar(99L, seguranca))
                .thenThrow(new IllegalArgumentException(
                        "Reserva autorizada não encontrada"));

        mockMvc.perform(get("/seguranca/reservas/99")
                        .with(user(seguranca.getEmail()).roles("SEGURANCA")))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALUNO", "SECRETARIA", "ADMINISTRADOR"})
    void r08SomenteASegurancaMarcaPresenca(String perfil) throws Exception {
        mockMvc.perform(post("/seguranca/reservas/20/presencas")
                        .with(user("usuario@escola.test").roles(perfil))
                        .with(csrf())
                        .param("presenca-31", "PRESENTE"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(consultaSegurancaService);
    }

    @Test
    void r08MarcacaoExigeCsrfEUsaASegurancaDaSessao() throws Exception {
        mockMvc.perform(post("/seguranca/reservas/20/presencas")
                        .with(user(seguranca.getEmail()).roles("SEGURANCA"))
                        .param("presenca-31", "PRESENTE"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(consultaSegurancaService);

        prepararSeguranca();
        when(consultaSegurancaService.registrarPresencas(20L, seguranca, Map.of(
                31L, PresencaParticipante.PRESENTE, 32L, PresencaParticipante.AUSENTE)))
                .thenReturn(2);
        mockMvc.perform(post("/seguranca/reservas/20/presencas")
                        .with(user(seguranca.getEmail()).roles("SEGURANCA"))
                        .with(csrf())
                        .param("presenca-31", "PRESENTE")
                        .param("presenca-32", "AUSENTE")
                        .param("status", "CANCELADA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seguranca/reservas/20"))
                .andExpect(flash().attribute("mensagemSucesso", "Presença registrada."));
        verify(consultaSegurancaService).registrarPresencas(20L, seguranca, Map.of(
                31L, PresencaParticipante.PRESENTE, 32L, PresencaParticipante.AUSENTE));
    }

    @Test
    void r08ValorInvalidoDePresencaERecusado() throws Exception {
        prepararSeguranca();
        mockMvc.perform(post("/seguranca/reservas/20/presencas")
                        .with(user(seguranca.getEmail()).roles("SEGURANCA"))
                        .with(csrf())
                        .param("presenca-31", "APROVADA"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(consultaSegurancaService);
    }

    @Test
    void r08NoDiaDaReservaATelaMostraAMarcacao() throws Exception {
        prepararSeguranca();
        when(consultaSegurancaService.detalhar(20L, seguranca))
                .thenReturn(new DetalheReservaAutorizada(
                        resumo,
                        List.of(new ParticipanteConferencia(
                                31L, "Carlos Participante", "RM-123", "Aluno",
                                PresencaParticipante.PRESENTE)),
                        true));

        mockMvc.perform(get("/seguranca/reservas/20")
                        .with(user(seguranca.getEmail()).roles("SEGURANCA")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"presenca-31\"")))
                .andExpect(content().string(containsString("Registrar presença")))
                .andExpect(content().string(containsString("checked")));
    }


    @Test
    void r10AcrescentarPessoaExigeCsrfEUsaASegurancaDaSessao() throws Exception {
        mockMvc.perform(post("/seguranca/reservas/20/pessoas")
                        .with(user(seguranca.getEmail()).roles("SEGURANCA"))
                        .param("nomeCompleto", "Visitante"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(portariaService);

        prepararSeguranca();
        mockMvc.perform(post("/seguranca/reservas/20/pessoas")
                        .with(user(seguranca.getEmail()).roles("SEGURANCA"))
                        .with(csrf())
                        .param("nomeCompleto", "Pai de aluno")
                        .param("rmOuRg", "RG-77")
                        .param("categoria", "OUTRO")
                        .param("vinculo", "PAI_OU_MAE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seguranca/reservas/20"))
                .andExpect(flash().attribute("mensagemSucesso",
                        "Pessoa acrescentada e marcada como presente."));
        verify(portariaService).acrescentarPessoa(20L, seguranca, "Pai de aluno", "RG-77",
                CategoriaParticipante.OUTRO, VinculoParticipante.PAI_OU_MAE);
    }

    @Test
    void r11QuadraSemMinimoVoltaComOMotivo() throws Exception {
        prepararSeguranca();
        doThrow(new IllegalStateException("Quadra não liberada: 8 pessoas presentes."))
                .when(portariaService).liberarQuadra(20L, seguranca);

        mockMvc.perform(post("/seguranca/reservas/20/liberar")
                        .with(user(seguranca.getEmail()).roles("SEGURANCA"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seguranca/reservas/20"))
                .andExpect(flash().attribute("mensagemErro",
                        "Quadra não liberada: 8 pessoas presentes."));
    }

    @Test
    void r11PagamentoNaPortariaELiberacaoDelegamAoServico() throws Exception {
        prepararSeguranca();
        mockMvc.perform(post("/seguranca/reservas/20/pagamento")
                        .with(user(seguranca.getEmail()).roles("SEGURANCA"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensagemSucesso",
                        "Valor recebido na portaria. Entregue o dinheiro à secretaria."));
        verify(portariaService).registrarPagamentoNaPortaria(20L, seguranca);

        mockMvc.perform(post("/seguranca/reservas/20/liberar")
                        .with(user(seguranca.getEmail()).roles("SEGURANCA"))
                        .with(csrf()))
                .andExpect(flash().attribute("mensagemSucesso", "Quadra liberada."));
        verify(portariaService).liberarQuadra(20L, seguranca);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALUNO", "SECRETARIA", "ADMINISTRADOR"})
    void r11SomenteASegurancaUsaAsAcoesDaPortaria(String perfil) throws Exception {
        for (String acao : new String[] {"pessoas", "pagamento", "liberar"}) {
            mockMvc.perform(post("/seguranca/reservas/20/" + acao)
                            .with(user("usuario@escola.test").roles(perfil))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(portariaService);
    }

    @Test
    void r11NoDiaATelaMostraLiberacaoEValorSomenteQuandoHaAPagar() throws Exception {
        prepararSeguranca();
        when(consultaSegurancaService.detalhar(20L, seguranca))
                .thenReturn(new DetalheReservaAutorizada(
                        resumo,
                        List.of(new ParticipanteConferencia(
                                31L, "Visitante", "RG-1", "Aluno",
                                PresencaParticipante.PRESENTE, true)),
                        true,
                        new ResumoPortaria(12, 2, new BigDecimal("4.00"), null)));

        mockMvc.perform(get("/seguranca/reservas/20")
                        .with(user(seguranca.getEmail()).roles("SEGURANCA")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Liberar quadra")))
                .andExpect(content().string(containsString("Falta pagar R$ 4,00.")))
                .andExpect(content().string(containsString("Acrescentado na portaria")))
                .andExpect(content().string(containsString("Acrescentar pessoa")))
                .andExpect(content().string(not(containsString("Contribuição"))));
    }

    @Test
    void r11SemPessoasAMaisATelaNaoMostraValores() throws Exception {
        prepararSeguranca();
        when(consultaSegurancaService.detalhar(20L, seguranca))
                .thenReturn(new DetalheReservaAutorizada(
                        resumo,
                        List.of(new ParticipanteConferencia(
                                31L, "Carlos Participante", "RM-123", "Aluno",
                                PresencaParticipante.PRESENTE)),
                        true,
                        new ResumoPortaria(10, 0, new BigDecimal("0.00"), null)));

        mockMvc.perform(get("/seguranca/reservas/20")
                        .with(user(seguranca.getEmail()).roles("SEGURANCA")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Liberar quadra")))
                .andExpect(content().string(not(containsString("R$"))));
    }

    @Test
    void r11DepoisDaLiberacaoTelaEncerraConferenciaEEscondeAlteracoes() throws Exception {
        prepararSeguranca();
        when(consultaSegurancaService.detalhar(20L, seguranca))
                .thenReturn(new DetalheReservaAutorizada(
                        resumo,
                        List.of(new ParticipanteConferencia(
                                31L, "Carlos Participante", "RM-123", "Aluno",
                                PresencaParticipante.PRESENTE, true)),
                        true,
                        new ResumoPortaria(10, 0, new BigDecimal("0.00"), Instant.now())));

        mockMvc.perform(get("/seguranca/reservas/20")
                        .with(user(seguranca.getEmail()).roles("SEGURANCA")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("A conferência foi encerrada")))
                .andExpect(content().string(not(containsString("name=\"presenca-31\""))))
                .andExpect(content().string(not(containsString("Registrar presença"))))
                .andExpect(content().string(not(containsString("Acrescentar pessoa"))))
                .andExpect(content().string(not(containsString("Recebi o valor na portaria"))))
                .andExpect(content().string(not(containsString("R$"))));
    }

    private void prepararSeguranca() {
        when(usuarioRepository.findByEmailIgnoreCase(seguranca.getEmail()))
                .thenReturn(Optional.of(seguranca));
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
