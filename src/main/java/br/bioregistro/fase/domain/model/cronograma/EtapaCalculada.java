package br.bioregistro.fase.domain.model.cronograma;

import java.time.LocalDate;
import java.util.List;

/**
 * Uma linha do cronograma calculado.
 *
 * @param inicioRelativo dias desde o edital (D+n)
 * @param ateProva       dias até a prova objetiva (P−n / P+n)
 * @param inicio         {@code null} no rascunho
 * @param ativa          módulo ligado e etapa não removida
 * @param alterada       dias ou duração diferentes do modelo (RN21)
 */
public record EtapaCalculada(String codigo, String descricao, String grupo, String referencia,
                             String referenciaCodigo, TipoStatus tipoStatus,
                             Integer diasModelo, int dias, Integer faixaP25, Integer faixaP75,
                             int duracaoModelo, int duracao,
                             int inicioRelativo, int fimRelativo, int ateProva,
                             LocalDate inicio, LocalDate fim,
                             boolean ativa, boolean removida, boolean removivel, boolean alterada,
                             boolean diasEditavel, boolean diasTravado, String prazoDias,
                             boolean duracaoTravada, String prazoDuracao,
                             List<String> avisos) {
}
