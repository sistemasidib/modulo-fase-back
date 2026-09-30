package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.TipoEtapaResponse;
import br.bioregistro.fase.application.dtos.TipoPublicacaoResponse;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.port.TipoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/** Opções de múltipla escolha: tipos de etapa e tipos de publicação. */
@ApplicationScoped
public class ListarTipos {

    private final TipoRepository tipos;

    public ListarTipos(TipoRepository tipos) {
        this.tipos = tipos;
    }

    public List<TipoEtapaResponse> etapas(Instituicao instituicao) {
        return tipos.tiposEtapa(instituicao).stream().map(TipoEtapaResponse::de).toList();
    }

    public List<TipoPublicacaoResponse> publicacoes(Instituicao instituicao) {
        return tipos.tiposPublicacao(instituicao).stream().map(TipoPublicacaoResponse::de).toList();
    }
}
