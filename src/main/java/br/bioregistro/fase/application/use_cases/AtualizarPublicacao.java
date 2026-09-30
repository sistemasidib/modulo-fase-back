package br.bioregistro.fase.application.use_cases;

import br.bioregistro.fase.application.dtos.PublicacaoDatasRequest;
import br.bioregistro.fase.application.dtos.PublicacaoResponse;
import br.bioregistro.fase.domain.exception.NaoEncontradoException;
import br.bioregistro.fase.domain.model.Instituicao;
import br.bioregistro.fase.domain.model.Publicacao;
import br.bioregistro.fase.domain.port.PublicacaoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;

/** Preenche ou corrige as datas da publicação (todas manuais). */
@ApplicationScoped
public class AtualizarPublicacao {

    private final PublicacaoRepository publicacoes;

    public AtualizarPublicacao(PublicacaoRepository publicacoes) {
        this.publicacoes = publicacoes;
    }

    @Transactional
    public PublicacaoResponse executar(Instituicao instituicao, int id, PublicacaoDatasRequest datas) {
        Publicacao atual = publicacoes.buscar(instituicao, id)
                .orElseThrow(() -> new NaoEncontradoException("Publicação", id));
        Publicacao atualizada = atual.comDatas(datas.dataPrevista(), datas.visualizacaoInicio(),
                datas.visualizacaoFim(), datas.recursoInicio(), datas.recursoFim());
        return PublicacaoResponse.de(publicacoes.salvar(instituicao, atualizada), OffsetDateTime.now());
    }
}
