package br.com.courttime.controller;

import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.UsuarioRepository;
import br.com.courttime.service.ReservaService;
import br.com.courttime.service.SituacaoSolicitacaoService;
import br.com.courttime.service.SolicitacaoAluno;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class InicioController {

    private final UsuarioRepository usuarioRepository;
    private final ReservaService reservaService;
    private final SituacaoSolicitacaoService situacaoSolicitacaoService;

    public InicioController(
            UsuarioRepository usuarioRepository,
            ReservaService reservaService,
            SituacaoSolicitacaoService situacaoSolicitacaoService) {
        this.usuarioRepository = usuarioRepository;
        this.reservaService = reservaService;
        this.situacaoSolicitacaoService = situacaoSolicitacaoService;
    }

    @GetMapping("/inicio")
    public String inicio(Authentication authentication, Model model) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário autenticado não encontrado"));

        model.addAttribute("nome", usuario.getNome());
        model.addAttribute("perfil", usuario.getPerfil());
        if (usuario.getPerfil() == Perfil.ALUNO) {
            model.addAttribute("ultimaSolicitacao", obterUltimaSolicitacao(usuario));
        }
        return "inicio";
    }

    /**
     * Mostra ao aluno a situação da sua solicitação mais recente, para que ele
     * saiba o próximo passo sem precisar abrir a lista.
     *
     * A situação é calculada por SituacaoSolicitacaoService, o mesmo serviço
     * usado na lista e no detalhe. Não existe uma segunda regra aqui.
     *
     * A consulta do serviço já devolve da mais nova para a mais antiga, então
     * basta a primeira. Devolve null quando o aluno ainda não solicitou nada.
     */
    private SolicitacaoAluno obterUltimaSolicitacao(Usuario aluno) {
        List<Reserva> solicitacoes = reservaService.listarSolicitacoesDoUsuario(aluno);
        if (solicitacoes.isEmpty()) {
            return null;
        }
        Reserva maisRecente = solicitacoes.get(0);
        return new SolicitacaoAluno(
                maisRecente, situacaoSolicitacaoService.descrever(maisRecente));
    }
}
