package br.com.courttime.service;

import br.com.courttime.entity.DisponibilidadeQuadra;
import br.com.courttime.entity.Quadra;
import br.com.courttime.repository.DisponibilidadeQuadraRepository;
import br.com.courttime.repository.QuadraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class QuadraService {

    private final QuadraRepository quadraRepository;
    private final DisponibilidadeQuadraRepository disponibilidadeQuadraRepository;

    public QuadraService(QuadraRepository quadraRepository,
                         DisponibilidadeQuadraRepository disponibilidadeQuadraRepository) {
        this.quadraRepository = quadraRepository;
        this.disponibilidadeQuadraRepository = disponibilidadeQuadraRepository;
    }

    @Transactional(readOnly = true)
    public Quadra obterQuadra() {
        return quadraRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Quadra não encontrada"));
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadeQuadra> obterDisponibilidadesAtivas(Quadra quadra) {
        return disponibilidadeQuadraRepository
                .findByQuadraAndAtivaTrue(quadra)
                .stream()
                .sorted(Comparator.comparing(DisponibilidadeQuadra::getDiaSemana)
                        .thenComparing(DisponibilidadeQuadra::getHorarioInicio))
                .toList();
    }
}
