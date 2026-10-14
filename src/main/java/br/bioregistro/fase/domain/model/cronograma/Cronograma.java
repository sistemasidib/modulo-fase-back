package br.bioregistro.fase.domain.model.cronograma;

import java.util.Map;

/**
 * Cronograma salvo (RN03). Nasce de um edital e pode ser seguido por outros editais, cada um com a
 * própria sobreposição ({@link EditalDoCronograma}, spec 005). Não pertence à entidade: uma
 * entidade pode ter vários concursos ao mesmo tempo, cada um com o seu cronograma.
 *
 * @param parametros cópia dos parâmetros do modelo no momento da criação: mudar o modelo não
 *                   altera cronograma salvo (RN17)
 * @param status     status manual por código de etapa; ausente = A fazer
 * @param versao     lock otimista: salvar com versão desatualizada é conflito
 */
public record Cronograma(Integer id, String esqueleto, Parametros parametros,
                         Edicao edicao, Map<String, StatusEtapa> status, String justificativa, int versao) {

    public Cronograma {
        status = Map.copyOf(status == null ? Map.of() : status);
    }

    public Cronograma comEdicao(Edicao nova, String novaJustificativa) {
        return new Cronograma(id, esqueleto, parametros, nova, status, novaJustificativa, versao);
    }

    /** Cronograma próprio de um edital que se separa: a edição e os status efetivos dele. */
    public Cronograma separado(EditalDoCronograma edital) {
        Sobreposicao s = edital.sobreposicao();
        return new Cronograma(null, esqueleto, parametros, s.aplicar(edicao), s.aplicarStatus(status),
                justificativa, 0);
    }

    /** Visão do edital: edição e status com a sobreposição dele. */
    public Cronograma doEdital(EditalDoCronograma edital) {
        Sobreposicao s = edital.sobreposicao();
        return new Cronograma(id, esqueleto, parametros, s.aplicar(edicao), s.aplicarStatus(status),
                justificativa, edital.versao());
    }

    public StatusEtapa status(String codigo) {
        return status.getOrDefault(codigo, StatusEtapa.A_FAZER);
    }
}
