package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.FaseRequest;
import br.bioregistro.fase.application.dtos.FaseResponse;
import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Fase;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.FaseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Cria a fase já com seus espaços de resultado preliminar e oficial. */
@ApplicationScoped
public class CriarFase {

    private final FaseRepository fases;
    private final EspacosDePublicacao espacos;

    public CriarFase(FaseRepository fases, EspacosDePublicacao espacos) {
        this.fases = fases;
        this.espacos = espacos;
    }

    @Transactional
    public FaseResponse executar(Instituicao instituicao, int editalId, FaseRequest request) {
        if (!fases.editalExiste(instituicao, editalId)) {
            throw new NaoEncontradoException("Edital", editalId);
        }
        Fase fase = request.paraDominio(null, editalId);
        if (fases.ordemEmUso(instituicao, editalId, fase.ordem(), null)) {
            throw new ConflitoException("Já existe uma fase com a ordem " + fase.ordem() + " neste edital.");
        }
        Fase criada = fases.salvar(instituicao, fase);
        espacos.criarParaFase(instituicao, criada.id());
        return FaseResponse.de(criada);
    }
}
