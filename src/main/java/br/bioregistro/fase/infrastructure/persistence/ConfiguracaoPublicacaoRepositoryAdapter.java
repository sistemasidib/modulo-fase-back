package br.bioregistro.fase.infrastructure.persistence;

import br.bioregistro.fase.domain.model.ConfiguracaoDePublicacao;
import br.bioregistro.fase.domain.model.FormatoDePublicacao;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.ConfiguracaoPublicacaoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ConfiguracaoPublicacaoRepositoryAdapter implements ConfiguracaoPublicacaoRepository {

    private final Sql sql;

    public ConfiguracaoPublicacaoRepositoryAdapter(Sql sql) {
        this.sql = sql;
    }

    @Override
    public Optional<ConfiguracaoDePublicacao> padrao(Instituicao instituicao, int tipoPublicacaoId) {
        return uma(instituicao, """
                SELECT tipo_publicacao_id, referencia_codigo, dias_apos_referencia, dias_no_ar, formato
                FROM dbo.config_publicacao_padrao WHERE tipo_publicacao_id = ?1
                """, tipoPublicacaoId);
    }

    @Override
    public Optional<ConfiguracaoDePublicacao> doEdital(Instituicao instituicao, int editalId, int tipoPublicacaoId) {
        return uma(instituicao, """
                SELECT tipo_publicacao_id, referencia_codigo, dias_apos_referencia, dias_no_ar, formato
                FROM dbo.config_publicacao_edital
                WHERE edital_id = ?1 AND tipo_publicacao_id = ?2
                """, editalId, tipoPublicacaoId);
    }

    @Override
    @Transactional
    public ConfiguracaoDePublicacao salvarDoEdital(Instituicao instituicao, int editalId,
                                                   ConfiguracaoDePublicacao config) {
        OffsetDateTime agora = OffsetDateTime.now();
        Object existe = sql.valor(instituicao, """
                SELECT id FROM dbo.config_publicacao_edital
                WHERE edital_id = ?1 AND tipo_publicacao_id = ?2
                """, editalId, config.tipoPublicacaoId());
        if (existe == null) {
            sql.executar(instituicao, """
                    INSERT INTO dbo.config_publicacao_edital
                    (edital_id, tipo_publicacao_id, referencia_codigo, dias_apos_referencia, dias_no_ar, formato, updated_at)
                    VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
                    """, editalId, config.tipoPublicacaoId(), Sql.texto(config.referenciaCodigo()),
                    Sql.inteiro(config.diasAposReferencia()), config.diasNoAr(), config.formato().name(), agora);
        } else {
            sql.executar(instituicao, """
                    UPDATE dbo.config_publicacao_edital
                    SET referencia_codigo = ?3, dias_apos_referencia = ?4, dias_no_ar = ?5, formato = ?6, updated_at = ?7
                    WHERE edital_id = ?1 AND tipo_publicacao_id = ?2
                    """, editalId, config.tipoPublicacaoId(), Sql.texto(config.referenciaCodigo()),
                    Sql.inteiro(config.diasAposReferencia()), config.diasNoAr(), config.formato().name(), agora);
        }
        return config;
    }

    @Override
    @Transactional
    public void excluirDoEdital(Instituicao instituicao, int editalId, int tipoPublicacaoId) {
        sql.executar(instituicao, """
                DELETE FROM dbo.config_publicacao_edital
                WHERE edital_id = ?1 AND tipo_publicacao_id = ?2
                """, editalId, tipoPublicacaoId);
    }

    private Optional<ConfiguracaoDePublicacao> uma(Instituicao instituicao, String sqlTexto, Object... params) {
        List<Object[]> linhas = sql.linhas(instituicao, sqlTexto, params);
        if (linhas.isEmpty()) {
            return Optional.empty();
        }
        Object[] r = linhas.getFirst();
        return Optional.of(new ConfiguracaoDePublicacao(
                Sql.comoInteiro(r[0]),
                r[1] == null ? null : r[1].toString(),
                Sql.comoInteiro(r[2]),
                Sql.comoInteiro(r[3]),
                FormatoDePublicacao.de(r[4].toString())));
    }
}
