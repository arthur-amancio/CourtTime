package br.com.courttime.support;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Guarda de segurança dos testes de integração.
 *
 * Esses testes apagam todas as tabelas antes de cada cenário. A variável de
 * ambiente COURTTIME_POSTGRES_TEST sozinha não é proteção suficiente: basta
 * ela continuar definida na mesma janela em que alguém apontou
 * COURTTIME_DB_URL para o banco de demonstração ou de desenvolvimento, e um
 * "mvn verify" apagaria esse banco.
 *
 * Por isso a guarda confere também o banco realmente conectado: o nome precisa
 * conter "test". O CI usa courttime_test e passa; courttime_demo, courttime e
 * postgres são recusados antes de qualquer remoção.
 */
public final class BancoDeTeste {

    private BancoDeTeste() {
    }

    public static void exigirBancoDedicado(DataSource dataSource) {
        String banco = nomeDoBanco(dataSource);
        if (banco == null || !banco.toLowerCase(Locale.ROOT).contains("test")) {
            throw new IllegalStateException(
                    "Os testes de integração apagam todas as tabelas e só podem rodar em um "
                            + "banco dedicado a testes. O banco conectado é \"" + banco
                            + "\", cujo nome não contém \"test\". Aponte COURTTIME_DB_URL para "
                            + "um banco exclusivo de teste, por exemplo courttime_test, ou "
                            + "remova COURTTIME_POSTGRES_TEST do ambiente.");
        }
    }

    private static String nomeDoBanco(DataSource dataSource) {
        try (Connection conexao = dataSource.getConnection()) {
            return conexao.getCatalog();
        } catch (SQLException excecao) {
            throw new IllegalStateException(
                    "Não foi possível identificar o banco conectado para verificar se é um "
                            + "banco de teste. Por segurança, nenhum dado foi apagado.", excecao);
        }
    }
}
