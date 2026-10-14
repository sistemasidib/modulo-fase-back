package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.EntidadeDtos.EditalSemEntidadeResponse;
import br.bioregistro.fase.application.dtos.EntidadeDtos.EntidadeResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.EditalResumo;
import br.bioregistro.fase.domain.model.Entidade;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.CronogramaRepository;
import br.bioregistro.fase.domain.port.EntidadeRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Leitura das entidades e dos editais soltos. */
@ApplicationScoped
public class ConsultarEntidades {

    private final EntidadeRepository entidades;
    private final CronogramaRepository cronogramas;

    public ConsultarEntidades(EntidadeRepository entidades, CronogramaRepository cronogramas) {
        this.entidades = entidades;
        this.cronogramas = cronogramas;
    }

    public List<EntidadeResponse> listar(Instituicao instituicao) {
        return entidades.listar(instituicao).stream().map(e -> resposta(instituicao, e)).toList();
    }

    public EntidadeResponse buscar(Instituicao instituicao, int id) {
        return resposta(instituicao, exigir(entidades, instituicao, id));
    }

    /**
     * A sugestão é só uma marca na tela: nada é agrupado sozinho. Sugere a entidade de outro edital
     * com o mesmo nome de concurso, ou a entidade com esse nome.
     */
    public List<EditalSemEntidadeResponse> editaisSemEntidade(Instituicao instituicao) {
        Map<String, Entidade> porNome = new HashMap<>();
        for (Entidade entidade : entidades.listar(instituicao)) {
            porNome.putIfAbsent(chave(entidade.nome()), entidade);
            for (EditalResumo edital : entidades.editais(instituicao, entidade.id())) {
                porNome.putIfAbsent(chave(edital.nome()), entidade);
            }
        }
        return entidades.editaisSemEntidade(instituicao).stream()
                .map(e -> {
                    Entidade sugerida = porNome.get(chave(e.nome()));
                    return new EditalSemEntidadeResponse(e.id(), e.nome(),
                            sugerida == null ? null : sugerida.id(), sugerida == null ? null : sugerida.nome());
                })
                .toList();
    }

    static Entidade exigir(EntidadeRepository entidades, Instituicao instituicao, int id) {
        return entidades.buscar(instituicao, id).orElseThrow(() -> new NaoEncontradoException("Entidade", id));
    }

    EntidadeResponse resposta(Instituicao instituicao, Entidade entidade) {
        return EntidadeResponse.de(entidade, entidades.editais(instituicao, entidade.id()),
                cronogramas.cronogramasDosEditais(instituicao, entidade.id()));
    }

    private static String chave(String nome) {
        return nome == null ? "" : nome.strip().toLowerCase(Locale.ROOT);
    }
}
