package br.bioregistro.fase.domain.model.cronograma;

import java.util.Map;

/**
 * Cronograma salvo de um concurso (RN03).
 *
 * @param parametros cópia dos parâmetros do modelo no momento da criação: mudar o modelo não
 *                   altera cronograma salvo (RN17)
 * @param status     status manual por código de etapa; ausente = A fazer
 * @param versao     lock otimista: salvar com versão desatualizada é conflito
 */
public record Cronograma(Integer id, int concursoId, String esqueleto, Parametros parametros, Edicao edicao,
                         Map<String, StatusEtapa> status, String justificativa, int versao) {

    public Cronograma {
        status = Map.copyOf(status == null ? Map.of() : status);
    }

    public Cronograma comEdicao(Edicao nova, String novaJustificativa) {
        return new Cronograma(id, concursoId, esqueleto, parametros, nova, status, novaJustificativa, versao);
    }

    public StatusEtapa status(String codigo) {
        return status.getOrDefault(codigo, StatusEtapa.A_FAZER);
    }
}
