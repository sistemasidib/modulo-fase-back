package br.bioregistro.fase.domain.model.cronograma;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Status da etapa marcado à mão (RN30) e os automáticos (RN31). */
public enum StatusEtapa {
    A_FAZER,
    EM_ANALISE,
    PRONTO_PARA_PUBLICAR,
    PUBLICADO,
    FEITO;

    /** Indicação visual; não impede nenhuma ação. */
    public enum Automatico { ATRASADO, BLOQUEADO, PUBLICACAO_VENCIDA }

    public static List<StatusEtapa> fluxo(TipoStatus tipo) {
        return tipo == TipoStatus.ATIVIDADE
                ? List.of(A_FAZER, FEITO)
                : List.of(A_FAZER, EM_ANALISE, PRONTO_PARA_PUBLICAR, PUBLICADO);
    }

    /** Marcação à mão. Em recurso, "Em análise" só entra pelo gatilho do 1º recurso. */
    public static void exigirMarcacao(TipoStatus tipo, StatusEtapa atual, StatusEtapa novo) {
        if (!fluxo(tipo).contains(novo)) {
            throw new RegraDeNegocioException("O status " + novo + " não faz parte do fluxo desta etapa.");
        }
        if (tipo == TipoStatus.RECURSO && novo == EM_ANALISE && atual == A_FAZER) {
            throw new RegraDeNegocioException("Use \"Registrar 1º recurso\" para colocar o recurso em análise.");
        }
    }

    /** Gatilho do recurso: liberado quando o prazo abre. */
    public static StatusEtapa registrarPrimeiroRecurso(EtapaCalculada etapa, StatusEtapa atual, LocalDate hoje) {
        if (etapa.tipoStatus() != TipoStatus.RECURSO) {
            throw new RegraDeNegocioException("A etapa " + etapa.codigo() + " não é de recurso.");
        }
        if (etapa.inicio() == null || hoje.isBefore(etapa.inicio())) {
            throw new RegraDeNegocioException("O prazo de recurso da etapa " + etapa.codigo() + " ainda não abriu.");
        }
        if (atual != A_FAZER) {
            throw new RegraDeNegocioException("O 1º recurso da etapa " + etapa.codigo() + " já foi registrado.");
        }
        return EM_ANALISE;
    }

    /**
     * RN31. Atividade: atrasada quando passou do fim sem "Feito"; bloqueada quando a data chegou e
     * a etapa de referência não começou (início de) ou não terminou (fim de). Publicação: vencida
     * quando a data passou e não foi publicada. Só no cronograma materializado e em etapa ativa.
     */
    public static Automatico automatico(EtapaCalculada etapa, Map<String, StatusEtapa> status, LocalDate hoje) {
        if (!etapa.ativa() || etapa.inicio() == null) {
            return null;
        }
        StatusEtapa atual = status.getOrDefault(etapa.codigo(), A_FAZER);
        if (etapa.tipoStatus() == TipoStatus.PUBLICACAO) {
            return atual != PUBLICADO && hoje.isAfter(etapa.inicio()) ? Automatico.PUBLICACAO_VENCIDA : null;
        }
        if (etapa.tipoStatus() != TipoStatus.ATIVIDADE || atual == FEITO) {
            return null;
        }
        if (hoje.isAfter(etapa.fim())) {
            return Automatico.ATRASADO;
        }
        if (!hoje.isBefore(etapa.inicio()) && etapa.referenciaCodigo() != null) {
            StatusEtapa ref = status.getOrDefault(etapa.referenciaCodigo(), A_FAZER);
            boolean referenciaPronta = etapa.referencia().startsWith("início")
                    ? ref != A_FAZER
                    : ref == FEITO || ref == PUBLICADO;
            return referenciaPronta ? null : Automatico.BLOQUEADO;
        }
        return null;
    }
}
