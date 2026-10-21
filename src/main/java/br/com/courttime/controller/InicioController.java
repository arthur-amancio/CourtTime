package br.com.courttime.controller;

import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.security.UsuarioAtual;
import br.com.courttime.service.ReservaService;
import br.com.courttime.service.SituacaoSolicitacaoService;
import br.com.courttime.service.SolicitacaoAluno;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Páginas de entrada: a apresentação pública, o login e o início de cada perfil.
 * As duas primeiras são públicas; /inicio exige login (SecurityConfig).
 */
@Controller
public class InicioController {

    private final ReservaService reservaService;
    private final SituacaoSolicitacaoService situacaoSolicitacaoService;

    public InicioController(
            ReservaService reservaService,
            SituacaoSolicitacaoService situacaoSolicitacaoService) {
        this.reservaService = reservaService;
        this.situacaoSolicitacaoService = situacaoSolicitacaoService;
    }

    @GetMapping("/")
    public String paginaPublica() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/inicio")
    public String inicio(@UsuarioAtual Usuario usuario, Model model) {
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
