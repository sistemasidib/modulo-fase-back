package br.bioregistro.fase.domain.model.cronograma;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF textual do cronograma público no formato Anexo II (spec 006, RN07–RN09).
 * Colunas: Item | Descrição da atividade | Início | Término.
 */
public final class PdfCronogramaPublico {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GERADO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private PdfCronogramaPublico() {
    }

    public static byte[] gerar(CronogramaPublico publico) {
        List<String> linhas = new ArrayList<>();
        linhas.add("ANEXO II - CRONOGRAMA DE EXECUCAO");
        if (publico.nomeConcurso() != null && !publico.nomeConcurso().isBlank()) {
            linhas.add(publico.nomeConcurso());
        }
        if (publico.editais() != null && !publico.editais().isEmpty()) {
            linhas.add("Editais: " + String.join(", ", publico.editais()));
        }
        if (publico.geradoEm() != null) {
            linhas.add("Gerado em: " + GERADO.format(publico.geradoEm()));
        }
        linhas.add("");
        linhas.add(String.format("%-6s %-52s %-12s %-12s", "ITEM", "DESCRICAO DA ATIVIDADE", "INICIO", "TERMINO"));
        linhas.add("-".repeat(84));
        for (CronogramaPublico.Item item : publico.itens()) {
            String inicio = item.inicio() == null ? "-" : DATA.format(item.inicio());
            String fim = item.fim() == null ? "-" : DATA.format(item.fim());
            String desc = item.descricao() == null ? "" : item.descricao();
            if (desc.length() > 52) {
                desc = desc.substring(0, 49) + "...";
            }
            String marca = item.destaque() ? "* " : "  ";
            linhas.add(String.format("%-6s %s%-50s %-12s %-12s", item.ordem(), marca, desc, inicio, fim));
        }
        return documento(linhas);
    }

    /** PDF 1.4 mínimo com texto Helvetica. */
    static byte[] documento(List<String> linhas) {
        StringBuilder conteudo = new StringBuilder("BT /F1 9 Tf 40 780 Td 12 TL\n");
        for (String linha : linhas) {
            conteudo.append('(').append(escapar(linha)).append(") Tj T*\n");
        }
        conteudo.append("ET\n");
        byte[] stream = conteudo.toString().getBytes(StandardCharsets.US_ASCII);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            List<Integer> offsets = new ArrayList<>();
            escrever(out, "%PDF-1.4\n");
            offsets.add(out.size());
            escrever(out, "1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n");
            offsets.add(out.size());
            escrever(out, "2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj\n");
            offsets.add(out.size());
            escrever(out, "3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                    + "/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>endobj\n");
            offsets.add(out.size());
            escrever(out, "4 0 obj<< /Length " + stream.length + " >>stream\n");
            out.write(stream);
            escrever(out, "\nendstream\nendobj\n");
            offsets.add(out.size());
            escrever(out, "5 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj\n");
            int xref = out.size();
            escrever(out, "xref\n0 6\n");
            escrever(out, "0000000000 65535 f \n");
            for (int off : offsets) {
                escrever(out, String.format("%010d 00000 n \n", off));
            }
            escrever(out, "trailer<< /Size 6 /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String escapar(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c == '\\' || c == '(' || c == ')') {
                b.append('\\').append(c);
            } else if (c < 32 || c > 126) {
                b.append('?');
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static void escrever(ByteArrayOutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.US_ASCII));
    }
}
