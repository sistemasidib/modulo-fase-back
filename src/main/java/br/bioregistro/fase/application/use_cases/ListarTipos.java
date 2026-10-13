package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.TipoEtapaResponse;
import br.bioregistro.fase.application.dtos.TipoPublicacaoResponse;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.TipoEtapa;
import br.bioregistro.fase.domain.port.TipoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;

/** Opções de múltipla escolha: tipos de etapa e tipos de publicação. */
@ApplicationScoped
public class ListarTipos {

    private final TipoRepository tipos;

    public ListarTipos(TipoRepository tipos) {
        this.tipos = tipos;
    }

    /** @param categoria {@code null} = todos; {@code execucao} = só os oferecidos na geração (spec 003, RN05) */
    public List<TipoEtapaResponse> etapas(Instituicao instituicao, String categoria) {
        TipoEtapa.Categoria filtro = categoria(categoria);
        return tipos.tiposEtapa(instituicao).stream()
                .filter(t -> filtro == null || t.categoria() == filtro)
                .map(t -> TipoEtapaResponse.de(t, tipos.publicacoesPadrao(instituicao, t.id())))
                .toList();
    }

    private static TipoEtapa.Categoria categoria(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return TipoEtapa.Categoria.valueOf(valor.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RegraDeNegocioException("Categoria inválida: " + valor + ". Use execucao, analise ou legado.");
        }
    }

    public List<TipoPublicacaoResponse> publicacoes(Instituicao instituicao) {
        return tipos.tiposPublicacao(instituicao).stream().map(TipoPublicacaoResponse::de).toList();
    }
}
