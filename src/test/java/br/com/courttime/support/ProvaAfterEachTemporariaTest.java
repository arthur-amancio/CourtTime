package br.com.courttime.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.opentest4j.TestAbortedException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProvaAfterEachTemporariaTest {

    private static boolean afterEachExecutouMesmoComBeforeEachFalhando;

    @BeforeEach
    void antes(TestInfo info) {
        if (info.getDisplayName().startsWith("cenarioComGuardaRecusando")) {
            throw new TestAbortedException("simula a guarda recusando o banco no @BeforeEach");
        }
    }

    @AfterEach
    void depois(TestInfo info) {
        if (info.getDisplayName().startsWith("cenarioComGuardaRecusando")) {
            afterEachExecutouMesmoComBeforeEachFalhando = true;
        }
    }

    @Test
    @Order(1)
    void cenarioComGuardaRecusando() {
    }

    @Test
    @Order(2)
    void afterEachRodouAposFalhaDoBeforeEach() {
        assertTrue(afterEachExecutouMesmoComBeforeEachFalhando);
    }
}
