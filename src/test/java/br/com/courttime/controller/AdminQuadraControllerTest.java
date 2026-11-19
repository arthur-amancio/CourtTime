package br.com.courttime.controller;

import br.com.courttime.config.SecurityConfig;
import br.com.courttime.entity.DiaSemana;
import br.com.courttime.entity.DisponibilidadeQuadra;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Quadra;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.AdministracaoQuadraService;
import br.com.courttime.service.CalendarioDisponibilidadeService;
import br.com.courttime.service.PainelAdministrativo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminQuadraController.class)
@Import(SecurityConfig.class)
class AdminQuadraControllerTest {

    private static final LocalDate DATA = LocalDate.of(2030, 10, 21);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdministracaoQuadraService administracaoService;
    @MockitoBean
    private CalendarioDisponibilidadeService calendarioService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private Usuario administrador;
    private Quadra quadra;

    @BeforeEach
    void setUp() {
        administrador = new Usuario();
        administrador.setId(10L);
        administrador.setNome("Administrador");
        administrador.setEmail("admin@escola.test");
        administrador.setSenha("hash");
        administrador.setPerfil(Perfil.ADMINISTRADOR);
        administrador.setAtivo(true);

        quadra = new Quadra();
        quadra.setId(1L);
        quadra.setNome("Quadra Poliesportiva");
        quadra.setDescricao("Quadra esportiva da Etec.");
        quadra.setAtiva(true);
        // O @UsuarioAtual busca o usuário da sessão em toda requisição autenticada.
        when(usuarioRepository.findByEmailIgnoreCase(administrador.getEmail()))
                .thenReturn(Optional.of(administrador));
    }

    @Test
    void usuarioNaoAutenticadoEEnviadoAoLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void alunoNaoPodeAcessarAdministracao() throws Exception {
        mockMvc.perform(get("/admin")
                        .with(user("aluno@escola.test").roles("ALUNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void segurancaNaoPodeAlterarHorario() throws Exception {
        mockMvc.perform(post("/admin/horarios")
                        .with(user("seguranca@escola.test").roles("SEGURANCA"))
                        .with(csrf())
                        .param("diaSemana", "SEGUNDA")
                        .param("horarioInicio", "08:00")
                        .param("horarioFim", "10:00"))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorVisualizaDadosReaisDoPainel() throws Exception {
        prepararAdministradorAutenticado();
        PainelAdministrativo painel = new PainelAdministrativo(
                quadra, List.of(), List.of(), 8, 2, 1, 3, 1, 1);
        when(administracaoService.carregarPainel(administrador)).thenReturn(painel);

        mockMvc.perform(get("/admin")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Painel administrativo")))
                .andExpect(content().string(containsString("Quadra Poliesportiva")))
                .andExpect(content().string(containsString("Aguardando documentação")))
                .andExpect(content().string(containsString("Canceladas")));
    }

    @Test
    void cadastroDeHorarioUsaAdministradorAutenticado() throws Exception {
        prepararAdministradorAutenticado();

        mockMvc.perform(post("/admin/horarios")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR"))
                        .with(csrf())
                        .param("diaSemana", "SEGUNDA")
                        .param("horarioInicio", "08:15")
                        .param("horarioFim", "10:15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/horarios"));

        verify(administracaoService).cadastrarHorario(
                administrador,
                DiaSemana.SEGUNDA,
                LocalTime.of(8, 15),
                LocalTime.of(10, 15));
    }

    @Test
    void cadastroDeHorarioSemCsrfERecusado() throws Exception {
        mockMvc.perform(post("/admin/horarios")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR"))
                        .param("diaSemana", "SEGUNDA")
                        .param("horarioInicio", "08:00")
                        .param("horarioFim", "10:00"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(administracaoService);
    }

    @Test
    void edicaoDeHorarioSemCsrfERecusada() throws Exception {
        mockMvc.perform(post("/admin/horarios/20")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR"))
                        .param("diaSemana", "SEGUNDA")
                        .param("horarioInicio", "08:00")
                        .param("horarioFim", "10:00"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(administracaoService);
    }

    @Test
    void alternanciaDeHorarioSemCsrfERecusada() throws Exception {
        mockMvc.perform(post("/admin/horarios/20/alternar")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(administracaoService);
    }

    @Test
    void periodoInvalidoRetornaFormularioComErro() throws Exception {
        mockMvc.perform(post("/admin/horarios")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR"))
                        .with(csrf())
                        .param("diaSemana", "SEGUNDA")
                        .param("horarioInicio", "10:00")
                        .param("horarioFim", "09:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Revise os dados informados")))
                .andExpect(content().string(containsString("posterior ao horário inicial")));
    }

    @Test
    void bloqueioComDataOuHorarioInvalidoMostraMensagemCompreensivel() throws Exception {
        prepararAdministradorAutenticado();

        mockMvc.perform(post("/admin/bloqueios")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR"))
                        .with(csrf())
                        .param("data", "31/31/2026")
                        .param("horarioInicio", "8h")
                        .param("horarioFim", "09:00")
                        .param("motivo", "Manutenção"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Informe uma data válida")))
                .andExpect(content().string(containsString("Informe um horário válido")))
                .andExpect(content().string(not(containsString("Failed to convert"))));

        verifyNoInteractions(administracaoService);
    }

    @Test
    void cadastroDeBloqueioUsaAdministradorAutenticado() throws Exception {
        prepararAdministradorAutenticado();

        mockMvc.perform(post("/admin/bloqueios")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR"))
                        .with(csrf())
                        .param("data", DATA.toString())
                        .param("horarioInicio", "08:00")
                        .param("horarioFim", "09:00")
                        .param("motivo", "Manutenção"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/bloqueios"));

        verify(administracaoService).cadastrarBloqueio(
                administrador,
                DATA,
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                "Manutenção");
    }

    @Test
    void cadastroDeBloqueioSemCsrfERecusado() throws Exception {
        mockMvc.perform(post("/admin/bloqueios")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR"))
                        .param("data", DATA.toString())
                        .param("horarioInicio", "08:00")
                        .param("horarioFim", "09:00")
                        .param("motivo", "Manutenção"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(administracaoService);
    }

    @Test
    void desativacaoDeBloqueioSemCsrfERecusada() throws Exception {
        mockMvc.perform(post("/admin/bloqueios/30/desativar")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(administracaoService);
    }

    @Test
    void administradorPodeEditarEAlternarHorario() throws Exception {
        prepararAdministradorAutenticado();
        DisponibilidadeQuadra horario = new DisponibilidadeQuadra();
        horario.setId(20L);
        horario.setQuadra(quadra);
        horario.setDiaSemana(DiaSemana.SEGUNDA);
        horario.setHorarioInicio(LocalTime.of(8, 0));
        horario.setHorarioFim(LocalTime.of(10, 0));
        horario.setAtiva(true);
        when(administracaoService.obterHorario(20L, administrador)).thenReturn(horario);
        when(administracaoService.alternarHorario(20L, administrador)).thenReturn(horario);

        mockMvc.perform(get("/admin/horarios/20/editar")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Editar horário semanal")));

        mockMvc.perform(post("/admin/horarios/20/alternar")
                        .with(user(administrador.getEmail()).roles("ADMINISTRADOR"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/horarios"));
    }

    private void prepararAdministradorAutenticado() {
        when(usuarioRepository.findByEmailIgnoreCase(administrador.getEmail()))
                .thenReturn(Optional.of(administrador));
    }
}
