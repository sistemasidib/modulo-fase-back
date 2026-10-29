package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.CriterioCorrecaoRepository;
import br.bioregistro.fase.domain.port.CriterioDesempateRepository;
import br.bioregistro.fase.domain.port.DisciplinaRepository;
import br.bioregistro.fase.domain.port.EtapaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Exclui a etapa, seus espaços de publicação vazios, as disciplinas, os critérios de correção e os de
 * desempate. Se houver candidatos, documentos ou recursos vinculados, o banco recusa e o repositório
 * lança {@code ConflitoException}.
 */
@ApplicationScoped
public class ExcluirEtapa {

    private final EtapaRepository etapas;
    private final EspacosDePublicacao espacos;
    private final DisciplinaRepository disciplinas;
    private final CriterioCorrecaoRepository criteriosCorrecao;
    private final CriterioDesempateRepository criteriosDesempate;
    private final ResultadoFinalDaFase resultadoFinal;

    public ExcluirEtapa(EtapaRepository etapas, EspacosDePublicacao espacos, DisciplinaRepository disciplinas,
                        CriterioCorrecaoRepository criteriosCorrecao, CriterioDesempateRepository criteriosDesempate,
                        ResultadoFinalDaFase resultadoFinal) {
        this.etapas = etapas;
        this.espacos = espacos;
        this.disciplinas = disciplinas;
        this.criteriosCorrecao = criteriosCorrecao;
        this.criteriosDesempate = criteriosDesempate;
        this.resultadoFinal = resultadoFinal;
    }

    @Transactional
    public void executar(Instituicao instituicao, int id) {
        Etapa etapa = etapas.buscar(instituicao, id)
                .orElseThrow(() -> new NaoEncontradoException("Etapa", id));
        if (resultadoFinal.ehObrigatoria(instituicao, etapa)) {
            throw new RegraDeNegocioException(
                    "\"Resultado Final\" é obrigatória em toda fase e não pode ser excluída.");
        }
        espacos.removerDaEtapa(instituicao, id);
        disciplinas.removerDaEtapa(instituicao, id);
        criteriosCorrecao.excluirDaEtapa(instituicao, id);
        criteriosDesempate.excluirDaEtapa(instituicao, id);
        etapas.excluir(instituicao, id);
        resultadoFinal.garantir(instituicao, etapa.faseId());
        espacos.sincronizarResultadoDaFase(instituicao, etapa.faseId());
    }
}
