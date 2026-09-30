package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.CriarPublicacaoRequest;
import br.bioregistro.fase.application.dtos.PublicacaoResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.exception.RegraDeNegocioException;
import br.bioregistro.fase.domain.model.Etapa;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.Publicacao;
import br.bioregistro.fase.domain.model.TipoPublicacao;
import br.bioregistro.fase.domain.port.EtapaRepository;
import br.bioregistro.fase.domain.port.FaseRepository;
import br.bioregistro.fase.domain.port.PublicacaoRepository;
import br.bioregistro.fase.domain.port.TipoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;

/** Espaço de publicação extra, além dos criados automaticamente. */
@ApplicationScoped
public class CriarPublicacao {

    private final FaseRepository fases;
    private final EtapaRepository etapas;
    private final TipoRepository tipos;
    private final PublicacaoRepository publicacoes;

    public CriarPublicacao(FaseRepository fases, EtapaRepository etapas, TipoRepository tipos,
                           PublicacaoRepository publicacoes) {
        this.fases = fases;
        this.etapas = etapas;
        this.tipos = tipos;
        this.publicacoes = publicacoes;
    }

    @Transactional
    public PublicacaoResponse executar(Instituicao instituicao, int faseId, CriarPublicacaoRequest request) {
        if (fases.buscar(instituicao, faseId).isEmpty()) {
            throw new NaoEncontradoException("Fase", faseId);
        }
        if (request.tipoPublicacaoId() == null) {
            throw new RegraDeNegocioException("O tipo da publicação é obrigatório.");
        }
        TipoPublicacao tipo = tipos.tipoPublicacao(instituicao, request.tipoPublicacaoId())
                .orElseThrow(() -> new NaoEncontradoException("Tipo de publicação", request.tipoPublicacaoId()));
        if (request.etapaId() != null) {
            Etapa etapa = etapas.buscar(instituicao, request.etapaId())
                    .orElseThrow(() -> new NaoEncontradoException("Etapa", request.etapaId()));
            if (etapa.faseId() != faseId) {
                throw new RegraDeNegocioException("A etapa " + etapa.id() + " não pertence à fase " + faseId + ".");
            }
        }
        Publicacao criada = publicacoes.salvar(instituicao, Publicacao.espaco(faseId, request.etapaId(), tipo));
        return PublicacaoResponse.de(criada, OffsetDateTime.now());
    }
}
