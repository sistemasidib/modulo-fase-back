package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.EntidadeDtos.EntidadeRequest;
import br.bioregistro.fase.application.dtos.EntidadeDtos.EntidadeResponse;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Entidade;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.EntidadeRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Escrita da entidade: criar, editar, excluir e agrupar editais. Um edital fica em no máximo uma
 * entidade; o banco de cada instituição garante que só se agrupa edital dela. A entidade não mexe
 * em cronograma: incluir, retirar ou excluir não muda o cronograma que cada edital segue.
 */
@ApplicationScoped
public class ManterEntidade {

    private final EntidadeRepository entidades;
    private final FaseRepository fases;
    private final ConsultarEntidades consulta;

    public ManterEntidade(EntidadeRepository entidades, FaseRepository fases, ConsultarEntidades consulta) {
        this.entidades = entidades;
        this.fases = fases;
        this.consulta = consulta;
    }

    @Transactional
    public EntidadeResponse criar(Instituicao instituicao, EntidadeRequest request) {
        Entidade entidade = new Entidade(null, request.nome(), request.cnpj());
        exigirLivre(instituicao, entidade, null);
        return consulta.resposta(instituicao, entidades.salvar(instituicao, entidade));
    }

    @Transactional
    public EntidadeResponse editar(Instituicao instituicao, int id, EntidadeRequest request) {
        ConsultarEntidades.exigir(entidades, instituicao, id);
        Entidade entidade = new Entidade(id, request.nome(), request.cnpj());
        exigirLivre(instituicao, entidade, id);
        return consulta.resposta(instituicao, entidades.salvar(instituicao, entidade));
    }

    /** Os editais voltam a ficar soltos e continuam seguindo o cronograma que seguiam. */
    @Transactional
    public void excluir(Instituicao instituicao, int id) {
        ConsultarEntidades.exigir(entidades, instituicao, id);
        entidades.excluir(instituicao, id);
    }

    @Transactional
    public EntidadeResponse incluirEdital(Instituicao instituicao, int id, int editalId) {
        Entidade entidade = ConsultarEntidades.exigir(entidades, instituicao, id);
        if (!fases.editalExiste(instituicao, editalId)) {
            throw new NaoEncontradoException("Edital", editalId);
        }
        var atual = entidades.entidadeDoEdital(instituicao, editalId);
        if (atual.isPresent() && !atual.get().id().equals(id)) {
            throw new ConflitoException("O edital " + editalId + " já está na entidade " + atual.get().nome()
                    + ". Retire-o de lá antes.");
        }
        if (atual.isEmpty()) {
            entidades.incluirEdital(instituicao, id, editalId);
        }
        return consulta.resposta(instituicao, entidade);
    }

    @Transactional
    public EntidadeResponse retirarEdital(Instituicao instituicao, int id, int editalId) {
        Entidade entidade = ConsultarEntidades.exigir(entidades, instituicao, id);
        boolean naEntidade = entidades.entidadeDoEdital(instituicao, editalId)
                .map(e -> e.id().equals(id)).orElse(false);
        if (!naEntidade) {
            throw new NaoEncontradoException("Edital na entidade " + entidade.nome() + ":", editalId);
        }
        entidades.retirarEdital(instituicao, id, editalId);
        return consulta.resposta(instituicao, entidade);
    }

    private void exigirLivre(Instituicao instituicao, Entidade entidade, Integer ignorar) {
        if (entidades.nomeEmUso(instituicao, entidade.nome(), ignorar)) {
            throw new ConflitoException("Já existe uma entidade chamada " + entidade.nome() + ".");
        }
        if (entidade.cnpj() != null && entidades.cnpjEmUso(instituicao, entidade.cnpj(), ignorar)) {
            throw new ConflitoException("Já existe uma entidade com o CNPJ " + entidade.cnpj() + ".");
        }
    }
}
