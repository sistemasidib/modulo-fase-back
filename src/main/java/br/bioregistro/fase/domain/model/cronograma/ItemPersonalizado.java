package br.bioregistro.fase.domain.model.cronograma;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.util.regex.Pattern;

/**
 * Linha que o usuário acrescenta ao cronograma, além das etapas do esqueleto. Conta a partir de
 * uma etapa do esqueleto (ou do marco zero); nenhuma etapa conta a partir dele.
 *
 * @param codigo P01…P999, único no cronograma
 * @param grupo  bloco ou fase do esqueleto onde o item aparece
 */
public record ItemPersonalizado(String codigo, String descricao, String grupo, Referencia referencia,
                                int dias, int duracao, TipoStatus tipoStatus) {

    public static final int MAXIMO = 100;

    private static final Pattern CODIGO = Pattern.compile("P\\d{2,3}");
    private static final int DESCRICAO_MAX = 300;
    private static final int LIMITE_DIAS = 999;

    public ItemPersonalizado {
        descricao = descricao == null ? null : descricao.strip();
        tipoStatus = tipoStatus == null ? TipoStatus.ATIVIDADE : tipoStatus;
        if (referencia != null && referencia.tipo() == Referencia.Tipo.MARCO_ZERO) {
            referencia = Referencia.MARCO_ZERO;
        }
    }

    void validar(Esqueleto esqueleto) {
        if (codigo == null || !CODIGO.matcher(codigo).matches()) {
            throw new RegraDeNegocioException("Código de item inválido: " + codigo + ". Use P01, P02…");
        }
        if (descricao == null || descricao.isEmpty()) {
            throw new RegraDeNegocioException("Descreva o item " + codigo + ".");
        }
        if (descricao.length() > DESCRICAO_MAX) {
            throw new RegraDeNegocioException(
                    "A descrição do item " + codigo + " passa de " + DESCRICAO_MAX + " caracteres.");
        }
        if (grupo == null || esqueleto.grupos().stream().noneMatch(g -> g.codigo().equals(grupo))) {
            throw new RegraDeNegocioException("Escolha o bloco ou a fase do item " + codigo + ".");
        }
        if (referencia == null || referencia.tipo() == null
                || referencia.tipo() == Referencia.Tipo.ULTIMO_RESULTADO) {
            throw new RegraDeNegocioException("Diga a partir de quando o item " + codigo
                    + " conta: marco zero, início ou fim de uma etapa.");
        }
        if (referencia.tipo() != Referencia.Tipo.MARCO_ZERO
                && (referencia.codigo() == null || !esqueleto.existe(referencia.codigo()))) {
            throw new RegraDeNegocioException("O item " + codigo + " conta a partir de uma etapa que não existe: "
                    + referencia.codigo() + ".");
        }
        if (Math.abs(dias) > LIMITE_DIAS) {
            throw new RegraDeNegocioException("Os dias do item " + codigo + " ficam entre -" + LIMITE_DIAS
                    + " e " + LIMITE_DIAS + ".");
        }
        if (duracao < 0 || duracao > LIMITE_DIAS) {
            throw new RegraDeNegocioException("A duração do item " + codigo + " fica entre 0 e " + LIMITE_DIAS + ".");
        }
    }
}
