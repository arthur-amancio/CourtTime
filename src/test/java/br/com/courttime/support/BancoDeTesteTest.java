package br.com.courttime.support;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Teste de unidade da guarda: roda sempre, sem PostgreSQL, e garante que os
 * testes de integração recusam bancos que não são de teste.
 */
class BancoDeTesteTest {

    @Test
    void aceitaBancoDeTeste() throws SQLException {
        assertDoesNotThrow(() -> BancoDeTeste.exigirBancoDedicado(dataSourceDoBanco("courttime_test")));
        assertDoesNotThrow(() -> BancoDeTeste.exigirBancoDedicado(dataSourceDoBanco("COURTTIME_TEST")));
    }

    @Test
    void recusaBancoDeDemonstracaoDesenvolvimentoEPadrao() throws SQLException {
        for (String banco : new String[] {"courttime_demo", "courttime", "postgres"}) {
            DataSource dataSource = dataSourceDoBanco(banco);
            assertThrows(IllegalStateException.class, () -> BancoDeTeste.exigirBancoDedicado(dataSource), banco);
        }
    }

    @Test
    void recusaQuandoNaoConsegueIdentificarOBanco() throws SQLException {
        assertThrows(IllegalStateException.class, () -> BancoDeTeste.exigirBancoDedicado(dataSourceDoBanco(null)));

        DataSource semConexao = mock(DataSource.class);
        when(semConexao.getConnection()).thenThrow(new SQLException("sem conexao"));
        assertThrows(IllegalStateException.class, () -> BancoDeTeste.exigirBancoDedicado(semConexao));
    }

    @Test
    void devolveAConexaoUsadaNaVerificacao() throws SQLException {
        Connection conexao = mock(Connection.class);
        when(conexao.getCatalog()).thenReturn("courttime_demo");
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(conexao);

        assertThrows(IllegalStateException.class, () -> BancoDeTeste.exigirBancoDedicado(dataSource));
        verify(conexao).close();
    }

    private static DataSource dataSourceDoBanco(String nome) throws SQLException {
        Connection conexao = mock(Connection.class);
        when(conexao.getCatalog()).thenReturn(nome);
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(conexao);
        return dataSource;
    }
}
