package br.bioregistro.fase.domain.model;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EntidadeTest {

    @Test
    void cnpjVazioViraNulo() {
        assertNull(new Entidade(null, "Prefeitura de X", null).cnpj());
        assertNull(new Entidade(null, "Prefeitura de X", "   ").cnpj());
    }

    @Test
    void cnpjNumericoSemPontuacao() {
        assertEquals("11222333000181", new Entidade(null, "Prefeitura de X", "11.222.333/0001-81").cnpj());
    }

    @Test
    void cnpjAlfanumerico() {
        assertEquals("12ABC34501DE35", new Entidade(null, "Prefeitura de X", "12.abc.345/01de-35").cnpj());
    }

    @Test
    void cnpjInvalido() {
        for (String cnpj : new String[]{"11.222.333/0001-82", "1122233300018", "00000000000000", "12ABC34501DEAB"}) {
            assertThrows(RegraDeNegocioException.class, () -> new Entidade(null, "Prefeitura de X", cnpj), cnpj);
        }
    }

    @Test
    void nomeObrigatorio() {
        assertThrows(RegraDeNegocioException.class, () -> new Entidade(null, " ", null));
    }
}
