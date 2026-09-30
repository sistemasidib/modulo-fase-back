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

/**
 * Publica: o usuário informa o período de visualização do documento e, se o tipo abre
 * recurso (ex.: gabarito preliminar), o início e o fim do recurso. O recurso fica aberto
 * dentro desse período.
 */
@ApplicationScoped
public class PublicarPublicacao {

    private final PublicacaoRepository publicacoes;

    public PublicarPublicacao(PublicacaoRepository publicacoes) {
        this.publicacoes = publicacoes;
    }

    @Transactional
    public PublicacaoResponse executar(Instituicao instituicao, int id, PublicacaoDatasRequest datas) {
        Publicacao atual = publicacoes.buscar(instituicao, id)
                .orElseThrow(() -> new NaoEncontradoException("Publicação", id));
        OffsetDateTime agora = OffsetDateTime.now();
        OffsetDateTime dataPrevista = datas.dataPrevista() != null ? datas.dataPrevista() : atual.dataPrevista();
        Publicacao publicada = atual
                .comDatas(dataPrevista, datas.visualizacaoInicio(), datas.visualizacaoFim(),
                        datas.recursoInicio(), datas.recursoFim())
                .publicar(agora);
        return PublicacaoResponse.de(publicacoes.salvar(instituicao, publicada), agora);
    }
}
