package br.com.courttime.config;

import br.com.courttime.entity.Quadra;
import br.com.courttime.repository.QuadraRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuadraInitializerTest {

    @Mock
    private QuadraRepository quadraRepository;

    @Test
    void createsDefaultQuadraWhenNoneExists() throws Exception {
        when(quadraRepository.count()).thenReturn(0L);
        QuadraInitializer initializer = new QuadraInitializer(quadraRepository);

        initializer.run(null);

        ArgumentCaptor<Quadra> quadraCaptor = ArgumentCaptor.forClass(Quadra.class);
        verify(quadraRepository).save(quadraCaptor.capture());
        Quadra quadra = quadraCaptor.getValue();
        assertEquals("Quadra Poliesportiva", quadra.getNome());
        assertEquals("Quadra esportiva da ETEC.", quadra.getDescricao());
        assertTrue(quadra.isAtiva());
    }

    @Test
    void doesNotCreateSecondQuadraWhenOneAlreadyExists() throws Exception {
        when(quadraRepository.count()).thenReturn(1L);
        QuadraInitializer initializer = new QuadraInitializer(quadraRepository);

        initializer.run(null);

        verify(quadraRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
