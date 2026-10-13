package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.domain.exception.ConflitoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.Publicacao;
import br.bioregistro.fase.domain.model.cronograma.LinhaDoCronograma;
import br.bioregistro.fase.domain.model.cronograma.StatusEtapa;
import br.bioregistro.fase.domain.port.VinculoCronogramaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * Publicações vinculadas ao cronograma (spec 005): as datas são lidas da linha do cronograma pela
 * FK, na hora, e nunca digitadas. O dia do cronograma vira o período inteiro do dia, no fuso de
 * Brasília. "Publicada" = status Publicado no cronograma.
 */
@ApplicationScoped
public class PublicacoesDoCronograma {

    static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final VinculoCronogramaRepository vinculos;
    private final EncerramentoDoLegado legado;

    public PublicacoesDoCronograma(VinculoCronogramaRepository vinculos, EncerramentoDoLegado legado) {
        this.vinculos = vinculos;
        this.legado = legado;
    }

    /** As publicações da fase, com as datas do cronograma nas que são vinculadas. */
    public List<Publicacao> comDatas(Instituicao instituicao, int faseId, List<Publicacao> publicacoes) {
        Map<Integer, LinhaDoCronograma.DaPublicacao> vinculadas = vinculos.publicacoesDaFase(instituicao, faseId);
        return publicacoes.stream()
                .map(p -> vinculadas.containsKey(p.id()) ? comDatas(p, vinculadas.get(p.id())) : p)
                .toList();
    }

    /**
     * Datas digitadas: a publicação não pode seguir o cronograma, e o edital dela só pode estar sem
     * cronograma até o fim do legado.
     */
    public void exigirDatasManuais(Instituicao instituicao, Publicacao publicacao) {
        exigirManual(instituicao, publicacao.id());
        legado.exigirAberto(instituicao, publicacao.faseId());
    }

    /** Datas, "publicar" e exclusão de publicação vinculada só mudam no cronograma. */
    public void exigirManual(Instituicao instituicao, int publicacaoId) {
        vinculos.daPublicacao(instituicao, publicacaoId).ifPresent(v -> {
            throw new ConflitoException("A publicação " + publicacaoId + " segue a etapa " + v.data().codigo()
                    + " do cronograma: datas e publicação mudam no cronograma do edital.");
        });
    }

    private static Publicacao comDatas(Publicacao p, LinhaDoCronograma.DaPublicacao linhas) {
        LinhaDoCronograma data = linhas.data();
        LinhaDoCronograma recurso = linhas.recurso();
        OffsetDateTime inicio = data.comData() ? inicioDoDia(data.inicio()) : null;
        OffsetDateTime fim = data.comData() ? fimDoDia(data.fim()) : null;
        boolean comRecurso = recurso != null && recurso.comData();
        OffsetDateTime publicadaEm = inicio != null && data.status() == StatusEtapa.PUBLICADO ? inicio : null;
        return p.comDatasDoCronograma(inicio, inicio, fim,
                comRecurso ? inicioDoDia(recurso.inicio()) : null, comRecurso ? fimDoDia(recurso.fim()) : null,
                publicadaEm);
    }

    static OffsetDateTime inicioDoDia(LocalDate dia) {
        return dia.atStartOfDay(FUSO).toOffsetDateTime();
    }

    static OffsetDateTime fimDoDia(LocalDate dia) {
        return dia.atTime(LocalTime.of(23, 59, 59)).atZone(FUSO).toOffsetDateTime();
    }
}
