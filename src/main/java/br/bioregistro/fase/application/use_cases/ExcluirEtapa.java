package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.DisciplinaRepository;
import br.bioregistro.fase.domain.port.EtapaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Exclui a etapa, seus espaços de publicação vazios e as disciplinas da prova. Se houver
 * candidatos, documentos ou recursos vinculados, o banco recusa e o repositório lança
 * {@code ConflitoException}.
 */
@ApplicationScoped
public class ExcluirEtapa {

    private final EtapaRepository etapas;
    private final EspacosDePublicacao espacos;
    private final DisciplinaRepository disciplinas;

    public ExcluirEtapa(EtapaRepository etapas, EspacosDePublicacao espacos, DisciplinaRepository disciplinas) {
        this.etapas = etapas;
        this.espacos = espacos;
        this.disciplinas = disciplinas;
    }

    @Transactional
    public void executar(Instituicao instituicao, int id) {
        if (etapas.buscar(instituicao, id).isEmpty()) {
            throw new NaoEncontradoException("Etapa", id);
        }
        espacos.removerDaEtapa(instituicao, id);
        disciplinas.removerDaEtapa(instituicao, id);
        etapas.excluir(instituicao, id);
    }
}
