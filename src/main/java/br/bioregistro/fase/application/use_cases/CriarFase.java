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

/** Cria a fase já com a etapa Resultado Final e os espaços de resultado quando cabem. */
@ApplicationScoped
public class CriarFase {

    private final FaseRepository fases;
    private final EspacosDePublicacao espacos;
    private final ResultadoFinalDaFase resultadoFinal;

    public CriarFase(FaseRepository fases, EspacosDePublicacao espacos, ResultadoFinalDaFase resultadoFinal) {
        this.fases = fases;
        this.espacos = espacos;
        this.resultadoFinal = resultadoFinal;
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
        resultadoFinal.garantir(instituicao, criada.id());
        espacos.sincronizarResultadoDaFase(instituicao, criada.id());
        return FaseResponse.de(criada);
    }
}
