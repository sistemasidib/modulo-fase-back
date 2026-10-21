package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.ConfiguracaoDePublicacao;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.ConfiguracaoPublicacaoRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.TipoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Configuração padrão e por edital dos tipos de publicação (spec 006, RN19–RN21).
 * Valores padrão do seed: pendência P-08.
 */
@ApplicationScoped
public class ConfiguracoesDePublicacao {

    private final ConfiguracaoPublicacaoRepository configs;
    private final TipoRepository tipos;
    private final FaseRepository fases;

    public ConfiguracoesDePublicacao(ConfiguracaoPublicacaoRepository configs, TipoRepository tipos,
                                     FaseRepository fases) {
        this.configs = configs;
        this.tipos = tipos;
        this.fases = fases;
    }

    public ConfiguracaoDePublicacao padrao(Instituicao instituicao, int tipoPublicacaoId) {
        exigirTipo(instituicao, tipoPublicacaoId);
        return configs.padrao(instituicao, tipoPublicacaoId)
                .orElseThrow(() -> new NaoEncontradoException("Configuração padrão do tipo de publicação",
                        tipoPublicacaoId));
    }

    /** Override do edital, ou o padrão se não houver override. */
    public ConfiguracaoDePublicacao efetiva(Instituicao instituicao, int editalId, int tipoPublicacaoId) {
        exigirEdital(instituicao, editalId);
        exigirTipo(instituicao, tipoPublicacaoId);
        return configs.doEdital(instituicao, editalId, tipoPublicacaoId)
                .or(() -> configs.padrao(instituicao, tipoPublicacaoId))
                .orElseThrow(() -> new NaoEncontradoException("Configuração do tipo de publicação",
                        tipoPublicacaoId));
    }

    @Transactional
    public ConfiguracaoDePublicacao salvarEdital(Instituicao instituicao, int editalId,
                                                 ConfiguracaoDePublicacao config) {
        exigirEdital(instituicao, editalId);
        exigirTipo(instituicao, config.tipoPublicacaoId());
        return configs.salvarDoEdital(instituicao, editalId, config);
    }

    private void exigirTipo(Instituicao instituicao, int tipoPublicacaoId) {
        if (tipos.tipoPublicacao(instituicao, tipoPublicacaoId).isEmpty()) {
            throw new NaoEncontradoException("Tipo de publicação", tipoPublicacaoId);
        }
    }

    private void exigirEdital(Instituicao instituicao, int editalId) {
        if (!fases.editalExiste(instituicao, editalId)) {
            throw new NaoEncontradoException("Edital", editalId);
        }
    }
}
