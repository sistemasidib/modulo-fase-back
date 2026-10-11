package br.bioregistro.fase.domain.model.cronograma;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * O que foge do padrão no concurso (RN26).
 *
 * @param etapaCodigo {@code null} = todo o cronograma
 */
public record Particularidade(Integer id, String texto, String assunto, String etapaCodigo, String autor,
                              OffsetDateTime criadoEm) {

    public static final List<String> ASSUNTOS = List.of(
            "Inscrição", "Isenção", "Prova objetiva", "Eliminação", "Recursos", "Cotas e PcD", "Resultado", "Outro");

    public static final int TEXTO_MAX = 500;

    public Particularidade {
        if (texto == null || texto.isBlank()) {
            throw new RegraDeNegocioException("O texto da particularidade é obrigatório.");
        }
        texto = texto.strip();
        if (texto.length() > TEXTO_MAX) {
            throw new RegraDeNegocioException("O texto da particularidade deve ter no máximo " + TEXTO_MAX + " caracteres.");
        }
        if (!ASSUNTOS.contains(assunto)) {
            throw new RegraDeNegocioException("Assunto inválido. Use um destes: " + String.join(", ", ASSUNTOS) + ".");
        }
        if (etapaCodigo != null && etapaCodigo.isBlank()) {
            etapaCodigo = null;
        }
    }

    public String alvo() {
        return etapaCodigo == null ? "todo o cronograma" : etapaCodigo;
    }
}
