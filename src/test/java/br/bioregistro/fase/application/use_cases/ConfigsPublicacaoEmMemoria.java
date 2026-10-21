package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.ConfiguracaoDePublicacao;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.ConfiguracaoPublicacaoRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class ConfigsPublicacaoEmMemoria implements ConfiguracaoPublicacaoRepository {

    private final Map<Integer, ConfiguracaoDePublicacao> padrao = new HashMap<>();
    private final Map<String, ConfiguracaoDePublicacao> edital = new HashMap<>();

    void comPadrao(ConfiguracaoDePublicacao config) {
        padrao.put(config.tipoPublicacaoId(), config);
    }

    @Override
    public Optional<ConfiguracaoDePublicacao> padrao(Instituicao instituicao, int tipoPublicacaoId) {
        return Optional.ofNullable(padrao.get(tipoPublicacaoId));
    }

    @Override
    public Optional<ConfiguracaoDePublicacao> doEdital(Instituicao instituicao, int editalId, int tipoPublicacaoId) {
        return Optional.ofNullable(edital.get(chave(editalId, tipoPublicacaoId)));
    }

    @Override
    public ConfiguracaoDePublicacao salvarDoEdital(Instituicao instituicao, int editalId,
                                                   ConfiguracaoDePublicacao config) {
        edital.put(chave(editalId, config.tipoPublicacaoId()), config);
        return config;
    }

    @Override
    public void excluirDoEdital(Instituicao instituicao, int editalId, int tipoPublicacaoId) {
        edital.remove(chave(editalId, tipoPublicacaoId));
    }

    private static String chave(int editalId, int tipoId) {
        return editalId + ":" + tipoId;
    }
}
