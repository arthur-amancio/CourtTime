package br.com.courttime.controller;

import br.com.courttime.config.SecurityConfig;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.ReservaService;
import br.com.courttime.service.SituacaoSolicitacao;
import br.com.courttime.service.SituacaoSolicitacaoService;
import br.com.courttime.service.UsuarioDetailsService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InicioController.class)
@Import({SecurityConfig.class, UsuarioDetailsService.class})
class InicioControllerTest {

    private static final String TITULO_PAINEL = "Sua solicitação mais recente";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private SituacaoSolicitacaoService situacaoSolicitacaoService;

    @Test
    void studentSeesSituationAndNextActionOfLatestRequest() throws Exception {
        Usuario aluno = usuario(10L, "aluno@teste.local", Perfil.ALUNO);
        Reserva reserva = reserva(30L);
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
        when(reservaService.listarSolicitacoesDoUsuario(aluno)).thenReturn(List.of(reserva));
        when(situacaoSolicitacaoService.descrever(reserva)).thenReturn(new SituacaoSolicitacao(
                "Pendente de documentação",
                "Revise os participantes e envie o PDF preparado e assinado fora do sistema.",
                false));

        mockMvc.perform(get("/inicio").with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(TITULO_PAINEL)))
                .andExpect(content().string(containsString("Pendente de documentação")))
                .andExpect(content().string(containsString(
                        "Revise os participantes e envie o PDF")))
                .andExpect(content().string(containsString("/solicitacoes/30")));
    }

    @Test
    void studentWithoutRequestsDoesNotSeeThePanel() throws Exception {
        Usuario aluno = usuario(10L, "aluno@teste.local", Perfil.ALUNO);
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
        when(reservaService.listarSolicitacoesDoUsuario(aluno)).thenReturn(List.of());

        mockMvc.perform(get("/inicio").with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(TITULO_PAINEL))));
    }

    @Test
    void approvedRequestIsShownWithTheApprovedVariant() throws Exception {
        Usuario aluno = usuario(10L, "aluno@teste.local", Perfil.ALUNO);
        Reserva reserva = reserva(31L);
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));
        when(reservaService.listarSolicitacoesDoUsuario(aluno)).thenReturn(List.of(reserva));
        when(situacaoSolicitacaoService.descrever(reserva)).thenReturn(new SituacaoSolicitacao(
                "Aprovada",
                "A utilização da quadra foi autorizada para a data e o horário informados.",
                true));

        mockMvc.perform(get("/inicio").with(user(aluno.getEmail()).roles("ALUNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("badge--aprovada")))
                .andExpect(content().string(containsString("Aprovada")));
    }

    @Test
    void otherProfilesDoNotQueryStudentRequests() throws Exception {
        Usuario secretaria = usuario(11L, "secretaria@teste.local", Perfil.SECRETARIA);
        when(usuarioRepository.findByEmailIgnoreCase(secretaria.getEmail()))
                .thenReturn(Optional.of(secretaria));

        mockMvc.perform(get("/inicio").with(user(secretaria.getEmail()).roles("SECRETARIA")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(TITULO_PAINEL))));

        verify(reservaService, never()).listarSolicitacoesDoUsuario(any());
    }

    private Usuario usuario(Long id, String email, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Pessoa de teste");
        usuario.setEmail(email);
        usuario.setSenha("hash-de-teste");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuario;
    }

    private Reserva reserva(Long id) {
        Reserva reserva = new Reserva();
        reserva.setId(id);
        reserva.setDataUtilizacao(LocalDate.of(2026, 10, 20));
        reserva.setHorarioInicio(LocalTime.of(8, 0));
        reserva.setHorarioFim(LocalTime.of(9, 0));
        reserva.setFinalidade("Treino da turma");
        return reserva;
    }
}
