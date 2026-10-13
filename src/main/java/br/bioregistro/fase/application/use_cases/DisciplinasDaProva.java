package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.DisciplinasDtos.DisciplinaResponse;
import br.bioregistro.fase.application.dtos.DisciplinasDtos.DisciplinasDaEtapaResponse;
import br.bioregistro.fase.application.dtos.DisciplinasDtos.NovaDisciplinaRequest;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.DisciplinaDaProva;
import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Fase;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.DisciplinaRepository;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.GrupoCargoRepository;
import br.bioregistro.fase.domain.port.TipoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Disciplinas de uma prova (etapa): as faixas de questões amarradas à etapa que valem para algum
 * cargo dos grupos que participam da fase, e o valor de cada questão. Na prova objetiva entram também as faixas
 * antigas, cadastradas sem etapa.
 */
@ApplicationScoped
public class DisciplinasDaProva {

    static final String PROVA_OBJETIVA = "prova_objetiva";

    private final EtapaRepository etapas;
    private final FaseRepository fases;
    private final GrupoCargoRepository grupos;
    private final TipoRepository tipos;
    private final DisciplinaRepository disciplinas;

    public DisciplinasDaProva(EtapaRepository etapas, FaseRepository fases, GrupoCargoRepository grupos,
                              TipoRepository tipos, DisciplinaRepository disciplinas) {
        this.etapas = etapas;
        this.fases = fases;
        this.grupos = grupos;
        this.tipos = tipos;
        this.disciplinas = disciplinas;
    }

    public DisciplinasDaEtapaResponse daEtapa(Instituicao instituicao, int etapaId) {
        return new Prova(instituicao, etapaId).resposta();
    }

    /** Uma faixa por cargo escolhido (todos os do grupo, se nenhum), presa à etapa. */
    @Transactional
    public DisciplinasDaEtapaResponse criar(Instituicao instituicao, int etapaId, NovaDisciplinaRequest r) {
        Prova prova = new Prova(instituicao, etapaId);
        String nome = DisciplinaDaProva.nomeValido(r.nome());
        DisciplinaDaProva.faixaValida(r.questaoInicio(), r.questaoFim());
        int inicio = r.questaoInicio();
        int fim = r.questaoFim();
        List<Integer> cargos = prova.cargosEscolhidos(r.cargoIds());
        for (int cargo : cargos) {
            prova.faixas.stream().filter(f -> f.conflitaCom(cargo, inicio, fim)).findFirst().ifPresent(f -> {
                throw new RegraDeNegocioException("As questões " + f.questaoInicio() + " a " + f.questaoFim()
                        + " já são de " + f.nome() + (f.cargoNome() == null ? "" : " (" + f.cargoNome() + ")")
                        + " nesta prova.");
            });
        }
        int disciplinaEditalId = disciplinas.disciplinaDoEdital(instituicao, prova.fase.editalId(), nome);
        for (int cargo : cargos) {
            disciplinas.inserirFaixa(instituicao, disciplinaEditalId, etapaId, inicio, fim, cargo);
        }
        return new Prova(instituicao, etapaId).resposta();
    }

    @Transactional
    public void remover(Instituicao instituicao, int faixaId) {
        if (disciplinas.buscar(instituicao, faixaId).isEmpty()) {
            throw new NaoEncontradoException("Disciplina", faixaId);
        }
        disciplinas.removerFaixa(instituicao, faixaId);
    }

    @Transactional
    public DisciplinaResponse definirPeso(Instituicao instituicao, int faixaId, BigDecimal peso) {
        BigDecimal valido = DisciplinaDaProva.pesoValido(peso);
        DisciplinaDaProva faixa = disciplinas.buscar(instituicao, faixaId)
                .orElseThrow(() -> new NaoEncontradoException("Disciplina", faixaId));
        disciplinas.definirPeso(instituicao, faixaId, valido);
        return DisciplinaResponse.de(faixa.comPeso(valido));
    }

    /** A etapa, sua fase, os cargos do grupo e as faixas que valem para eles. */
    private final class Prova {
        final Etapa etapa;
        final Fase fase;
        final List<Integer> grupoIds;
        final List<Integer> cargos;
        final List<DisciplinaDaProva> faixas;

        Prova(Instituicao instituicao, int etapaId) {
            etapa = etapas.buscar(instituicao, etapaId).orElseThrow(() -> new NaoEncontradoException("Etapa", etapaId));
            fase = fases.buscar(instituicao, etapa.faseId())
                    .orElseThrow(() -> new NaoEncontradoException("Fase", etapa.faseId()));
            grupoIds = grupos.gruposDaFase(instituicao, fase.id());
            cargos = grupoIds.stream().flatMap(g -> grupos.cargosDoGrupo(instituicao, g).stream()).distinct().toList();
            boolean objetiva = tipos.tipoEtapa(instituicao, etapa.tipoId())
                    .map(t -> PROVA_OBJETIVA.equals(t.valor())).orElse(false);
            faixas = disciplinas.daEtapa(instituicao, fase.editalId(), etapaId, objetiva).stream()
                    .filter(d -> d.valePara(cargos))
                    .toList();
        }

        List<Integer> cargosEscolhidos(List<Integer> pedidos) {
            if (cargos.isEmpty()) {
                throw new RegraDeNegocioException("Inclua grupos com cargos nesta fase antes de cadastrar disciplinas.");
            }
            if (pedidos == null || pedidos.isEmpty()) {
                return cargos;
            }
            List<Integer> escolhidos = pedidos.stream().distinct().toList();
            if (!cargos.containsAll(escolhidos)) {
                throw new RegraDeNegocioException("Escolha só cargos dos grupos desta fase.");
            }
            return escolhidos;
        }

        DisciplinasDaEtapaResponse resposta() {
            return DisciplinasDaEtapaResponse.de(etapa.id(), fase.id(), grupoIds, cargos, faixas);
        }
    }
}
