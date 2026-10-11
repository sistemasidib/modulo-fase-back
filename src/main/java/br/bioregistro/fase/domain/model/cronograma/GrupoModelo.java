package br.bioregistro.fase.domain.model.cronograma;

import java.util.Set;

/**
 * Agrupamento fixo do esqueleto (RN07): bloco (antes/depois das fases) ou fase de execução.
 *
 * @param modulo {@code null} = núcleo (sempre ativo); {@code S05_OFF} = ativo quando S05 está desligado
 */
public record GrupoModelo(String codigo, String nome, boolean fase, String modulo, String nota) {

    public static final String S05_DESLIGADO = "S05_OFF";

    public boolean ativo(Set<String> modulosLigados) {
        if (modulo == null) {
            return true;
        }
        if (S05_DESLIGADO.equals(modulo)) {
            return !modulosLigados.contains("S05");
        }
        return modulosLigados.contains(modulo);
    }
}
