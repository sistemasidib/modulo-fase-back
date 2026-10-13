package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.ConcursoDtos.ConcursoResponse;
import br.bioregistro.fase.application.dtos.ConcursoDtos.EditalSemConcursoResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Concurso;
import br.bioregistro.fase.domain.model.EditalResumo;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.ConcursoRepository;
import br.bioregistro.fase.domain.port.CronogramaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Leitura dos concursos e dos editais soltos (RB01, RB02). */
@ApplicationScoped
public class ConsultarConcursos {

    private final ConcursoRepository concursos;
    private final CronogramaRepository cronogramas;

    public ConsultarConcursos(ConcursoRepository concursos, CronogramaRepository cronogramas) {
        this.concursos = concursos;
        this.cronogramas = cronogramas;
    }

    public List<ConcursoResponse> listar(Instituicao instituicao) {
        return concursos.listar(instituicao).stream().map(c -> resposta(instituicao, c)).toList();
    }

    public ConcursoResponse buscar(Instituicao instituicao, int id) {
        return resposta(instituicao, exigir(concursos, instituicao, id));
    }

    /** A sugestão é só uma marca na tela: nada é agrupado sozinho. */
    public List<EditalSemConcursoResponse> editaisSemConcurso(Instituicao instituicao) {
        Map<String, Concurso> porNome = new HashMap<>();
        for (Concurso concurso : concursos.listar(instituicao)) {
            porNome.putIfAbsent(chave(concurso.nome()), concurso);
            for (EditalResumo edital : concursos.editais(instituicao, concurso.id())) {
                porNome.putIfAbsent(chave(edital.nome()), concurso);
            }
        }
        return concursos.editaisSemConcurso(instituicao).stream()
                .map(e -> {
                    Concurso sugerido = porNome.get(chave(e.nome()));
                    return new EditalSemConcursoResponse(e.id(), e.nome(),
                            sugerido == null ? null : sugerido.id(), sugerido == null ? null : sugerido.nome());
                })
                .toList();
    }

    static Concurso exigir(ConcursoRepository concursos, Instituicao instituicao, int id) {
        return concursos.buscar(instituicao, id).orElseThrow(() -> new NaoEncontradoException("Concurso", id));
    }

    ConcursoResponse resposta(Instituicao instituicao, Concurso concurso) {
        return ConcursoResponse.de(concurso, concursos.editais(instituicao, concurso.id()),
                cronogramas.idDoConcurso(instituicao, concurso.id()).orElse(null),
                cronogramas.cronogramasDosEditais(instituicao, concurso.id()));
    }

    private static String chave(String nome) {
        return nome == null ? "" : nome.strip().toLowerCase(Locale.ROOT);
    }
}
