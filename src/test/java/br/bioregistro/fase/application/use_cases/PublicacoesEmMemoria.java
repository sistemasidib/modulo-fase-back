package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.Publicacao;
import br.bioregistro.fase.domain.port.PublicacaoRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Repositório falso de publicações. Usa uma única instituição por teste. */
class PublicacoesEmMemoria implements PublicacaoRepository {

    final Map<Integer, Publicacao> publicacoes = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public Optional<Publicacao> buscar(Instituicao instituicao, int id) {
        return Optional.ofNullable(publicacoes.get(id));
    }

    @Override
    public List<Publicacao> listarPorFase(Instituicao instituicao, int faseId) {
        return publicacoes.values().stream()
                .filter(p -> p.faseId() == faseId)
                .sorted(Comparator.comparing(Publicacao::id))
                .toList();
    }

    @Override
    public List<Publicacao> listarPorEtapa(Instituicao instituicao, int etapaId) {
        return publicacoes.values().stream().filter(p -> Objects.equals(p.etapaId(), etapaId)).toList();
    }

    @Override
    public Publicacao salvar(Instituicao instituicao, Publicacao publicacao) {
        Publicacao salva = publicacao.id() == null ? publicacao.comId(proximoId++) : publicacao;
        publicacoes.put(salva.id(), salva);
        return salva;
    }

    @Override
    public void excluir(Instituicao instituicao, int id) {
        publicacoes.remove(id);
    }
}
