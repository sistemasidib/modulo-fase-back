package br.bioregistro.fase.domain.model.cronograma;

/**
 * Edital que segue um cronograma ({@code cronograma_edital}). Cada edital segue no máximo um; um
 * cronograma pode ser seguido por vários editais (spec 005).
 *
 * @param versao lock otimista da sobreposição: salvar com versão desatualizada é conflito
 */
public record EditalDoCronograma(int id, int cronogramaId, int editalId, Sobreposicao sobreposicao, int versao) {

    public EditalDoCronograma {
        sobreposicao = sobreposicao == null ? Sobreposicao.vazia() : sobreposicao;
    }

    public EditalDoCronograma com(Sobreposicao nova) {
        return new EditalDoCronograma(id, cronogramaId, editalId, nova, versao);
    }
}
