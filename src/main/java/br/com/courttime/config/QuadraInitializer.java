package br.com.courttime.config;

import br.com.courttime.entity.Quadra;
import br.com.courttime.repository.QuadraRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class QuadraInitializer implements ApplicationRunner {

    private final QuadraRepository quadraRepository;

    public QuadraInitializer(QuadraRepository quadraRepository) {
        this.quadraRepository = quadraRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (quadraRepository.count() > 0) {
            return;
        }

        Quadra quadra = new Quadra();
        quadra.setNome("Quadra Poliesportiva");
        quadra.setDescricao("Quadra esportiva da ETEC.");
        quadra.setAtiva(true);
        quadraRepository.save(quadra);
    }
}
