package br.com.courttime.service;

import br.com.courttime.entity.CategoriaParticipante;
import br.com.courttime.entity.ParticipanteReserva;
import br.com.courttime.entity.Perfil;
import br.com.courttime.entity.Reserva;
import br.com.courttime.entity.StatusReserva;
import br.com.courttime.entity.Usuario;
import br.com.courttime.repository.ParticipanteReservaRepository;
import br.com.courttime.repository.ReservaRepository;
import br.com.courttime.security.Permissao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Gera o formulário oficial da ETEC preenchido com os dados da solicitação,
 * para o aluno imprimir, assinar e enviar depois como PDF.
 *
 * CPF e curso são digitados na hora e só entram no PDF devolvido: não são
 * gravados no banco, em arquivo nem em log. O sistema não assina nada e não
 * registra aprovação.
 */
@Service
public class FormularioOficialService {

    /** O PDF oficial original, sem alterações, usado como modelo. */
    static final String MODELO = "modelos/formulario-reserva-quadra.pdf";

    private static final int TAMANHO_MAXIMO_CURSO = 80;

    private final ReservaRepository reservaRepository;
    private final ParticipanteReservaRepository participanteRepository;
    private final CalendarioDisponibilidadeService calendarioService;
    private final String caminhoDoModelo;

    // Com dois construtores, o Spring precisa saber qual usar.
    @Autowired
    public FormularioOficialService(
            ReservaRepository reservaRepository,
            ParticipanteReservaRepository participanteRepository,
            CalendarioDisponibilidadeService calendarioService) {
        this(reservaRepository, participanteRepository, calendarioService, MODELO);
    }

    FormularioOficialService(
            ReservaRepository reservaRepository,
            ParticipanteReservaRepository participanteRepository,
            CalendarioDisponibilidadeService calendarioService,
            String caminhoDoModelo) {
        this.reservaRepository = reservaRepository;
        this.participanteRepository = participanteRepository;
        this.calendarioService = calendarioService;
        this.caminhoDoModelo = caminhoDoModelo;
    }

    @Transactional(readOnly = true)
    public byte[] gerar(Long reservaId, Usuario solicitante, String cpf, String curso) {
        Permissao.exigir(solicitante, Perfil.ALUNO);
        if (reservaId == null) {
            throw new IllegalArgumentException("A solicitação é obrigatória");
        }
        Reserva reserva = reservaRepository.findByIdAndSolicitante(reservaId, solicitante)
                .orElseThrow(() -> new AccessDeniedException(
                        "A solicitação não existe ou não pertence ao usuário autenticado"));
        if (reserva.getStatus() != StatusReserva.PENDENTE_DOCUMENTACAO) {
            throw new IllegalStateException(
                    "O formulário só pode ser gerado enquanto a solicitação aguarda documentação");
        }
        String cpfFormatado = validarCpf(cpf);
        String cursoNormalizado = validarCurso(curso);

        List<ParticipanteReserva> participantes =
                participanteRepository.findByReservaOrderByNomeCompletoAscIdAsc(reserva);
        if (participantes.isEmpty()) {
            throw new IllegalStateException(
                    "Cadastre os participantes antes de gerar o formulário");
        }
        if (participantes.size() < ParticipanteReservaService.MINIMO_DE_PARTICIPANTES) {
            throw new IllegalStateException(
                    "Cadastre no mínimo 10 participantes, de qualquer categoria, antes de gerar o formulário");
        }

        DadosFormularioReserva dados = new DadosFormularioReserva(
                solicitante.getNome(),
                cursoNormalizado,
                cpfFormatado,
                reserva.getDataUtilizacao(),
                reserva.getHorarioInicio(),
                reserva.getHorarioFim(),
                calendarioService.obterDataAtual(),
                participantes.stream()
                        .map(participante -> new DadosFormularioReserva.Participante(
                                participante.getNomeCompleto(),
                                participante.getRmOuRg(),
                                participante.getCategoria() == CategoriaParticipante.ALUNO,
                                participante.getVinculo() == null
                                        ? null
                                        : participante.getVinculo().getTextoFormulario()))
                        .toList());
        try {
            return FormularioReservaPdf.gerar(carregarModelo(), dados);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível gerar o formulário oficial", exception);
        }
    }

    private byte[] carregarModelo() throws IOException {
        ClassPathResource modelo = new ClassPathResource(caminhoDoModelo);
        if (!modelo.exists()) {
            // Sem o original não há o que preencher: nunca se cria um termo substituto.
            throw new IllegalStateException(
                    "O modelo oficial do formulário da ETEC não está disponível no sistema");
        }
        try (InputStream entrada = modelo.getInputStream()) {
            return entrada.readAllBytes();
        }
    }

    /**
     * Aceita o CPF com ou sem pontuação e confere os dois dígitos verificadores,
     * para evitar erro de digitação no documento que será assinado.
     */
    static String validarCpf(String cpf) {
        String digitos = cpf == null ? "" : cpf.replaceAll("[.\\-\\s]", "");
        if (!digitos.matches("\\d{11}") || digitos.chars().distinct().count() == 1
                || digitoVerificador(digitos, 9) != digitos.charAt(9) - '0'
                || digitoVerificador(digitos, 10) != digitos.charAt(10) - '0') {
            throw new IllegalArgumentException("Informe um CPF válido");
        }
        return digitos.substring(0, 3) + "." + digitos.substring(3, 6) + "."
                + digitos.substring(6, 9) + "-" + digitos.substring(9);
    }

    private static int digitoVerificador(String digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (digitos.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    static String validarCurso(String curso) {
        String normalizado = curso == null ? "" : curso.trim().replaceAll("\\s+", " ");
        if (normalizado.isEmpty() || normalizado.length() > TAMANHO_MAXIMO_CURSO) {
            throw new IllegalArgumentException(
                    "Informe o curso, com no máximo " + TAMANHO_MAXIMO_CURSO + " caracteres");
        }
        return normalizado;
    }
}
