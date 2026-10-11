package br.bioregistro.fase.domain.model.cronograma;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cálculo puro do cronograma (RN11–RN24): sem banco e sem relógio — "hoje" entra como parâmetro.
 * Porta o {@code calc}/{@code calcEff}/{@code ovWith}/{@code moved} do protótipo.
 *
 * <p>Início = data de referência + dias; fim = início + duração; tudo em dias corridos. Etapas
 * removidas continuam na cadeia, para que as que dependem delas fiquem no mesmo lugar (RN22).
 */
public final class CalculoCronograma {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final String[] DIA_SEMANA = {"seg", "ter", "qua", "qui", "sex", "sáb", "dom"};

    private CalculoCronograma() {
    }

    /** Posições em dias desde o edital. {@code extra} = ajuste da prova ao domingo. */
    private record Posicoes(Map<String, Integer> inicio, Map<String, Integer> fim, int extra) {
    }

    public static CronogramaCalculado calcular(Esqueleto esqueleto, Parametros parametros, Edicao edicao,
                                               LocalDate hoje) {
        Posicoes pos = posicoes(esqueleto, parametros, edicao);
        LocalDate d = edicao.dataEdital();
        int prova = pos.inicio().get(EtapaModelo.PROVA);

        List<CronogramaCalculado.Grupo> grupos = new ArrayList<>();
        int ativas = 0;
        for (GrupoModelo grupo : esqueleto.grupos()) {
            boolean grupoAtivo = grupo.ativo(edicao.modulos());
            List<EtapaCalculada> etapas = new ArrayList<>();
            for (EtapaModelo etapa : esqueleto.etapasDoGrupo(grupo.codigo())) {
                EtapaCalculada calculada = linha(etapa, grupoAtivo, parametros, edicao, pos, prova);
                if (calculada.ativa()) {
                    ativas++;
                }
                etapas.add(calculada);
            }
            grupos.add(new CronogramaCalculado.Grupo(grupo.codigo(), grupo.nome(), grupo.fase(), grupo.modulo(),
                    grupo.nota(), grupoAtivo, List.copyOf(etapas)));
        }

        int resultado = pos.inicio().get(EtapaModelo.RESULTADO_FINAL);
        var indicadores = new CronogramaCalculado.Indicadores(ativas, esqueleto.etapas().size(), d,
                prova, data(d, prova), resultado, data(d, resultado));
        return new CronogramaCalculado(d != null, d, pos.extra(), List.copyOf(grupos), indicadores,
                checagens(parametros, edicao, pos, hoje));
    }

    /** RN20: efeito de uma edição de célula, sem gravar. */
    public static Previa prever(Esqueleto esqueleto, Parametros parametros, Edicao edicao, Mudanca mudanca) {
        EtapaModelo etapa = esqueleto.etapa(mudanca.codigo());
        boolean dias = mudanca.campo() == Mudanca.Campo.DIAS;
        exigirEditavel(etapa, dias, mudanca.valor(), edicao);

        int antes = dias ? diasAtuais(etapa, parametros, edicao) : duracaoAtual(etapa, parametros, edicao);
        Edicao nova = comValor(edicao, etapa, parametros, dias, mudanca.valor());
        List<Previa.Item> itens = new ArrayList<>();
        itens.add(new Previa.Item(etapa.codigo(), dias ? "dias até a etapa" : "duração",
                String.valueOf(antes), String.valueOf(mudanca.valor())));

        Posicoes a = posicoes(esqueleto, parametros, edicao);
        if (mudanca.modo() == Mudanca.Modo.SO_ESTA) {
            Posicoes b = posicoes(esqueleto, parametros, nova);
            for (EtapaModelo dependente : esqueleto.etapas()) {
                int delta = deslocamento(dependente.referencia(), etapa.codigo(), a, b);
                if (delta != 0 && esqueleto.grupo(dependente.grupo()).ativo(edicao.modulos())) {
                    int velho = diasAtuais(dependente, parametros, nova);
                    nova = comValor(nova, dependente, parametros, true, velho - delta);
                    itens.add(new Previa.Item(dependente.codigo(), "dias (compensado)",
                            String.valueOf(velho), String.valueOf(velho - delta)));
                }
            }
        }

        Posicoes b = posicoes(esqueleto, parametros, nova);
        int movidas = 0;
        for (EtapaModelo outra : esqueleto.etapas()) {
            if (outra.codigo().equals(etapa.codigo()) || edicao.removidas().contains(outra.codigo())
                    || !esqueleto.grupo(outra.grupo()).ativo(edicao.modulos())) {
                continue;
            }
            int de = a.inicio().get(outra.codigo());
            int para = b.inicio().get(outra.codigo());
            if (de != para) {
                movidas++;
                itens.add(new Previa.Item(outra.codigo(), edicao.dataEdital() != null ? "data" : "início",
                        rotulo(edicao.dataEdital(), de), rotulo(edicao.dataEdital(), para)));
            }
        }
        return new Previa(nova, List.copyOf(itens), movidas);
    }

    // ── cadeia ────────────────────────────────────────────────────────────────

    private static Posicoes posicoes(Esqueleto esqueleto, Parametros parametros, Edicao edicao) {
        Posicoes sem = cadeia(esqueleto, parametros, edicao, 0);
        if (edicao.dataEdital() == null) {
            return sem;
        }
        LocalDate prova = edicao.dataEdital().plusDays(sem.inicio().get(EtapaModelo.PROVA));
        int extra = (7 - prova.getDayOfWeek().getValue() % 7) % 7;
        return extra == 0 ? sem : cadeia(esqueleto, parametros, edicao, extra);
    }

    private static Posicoes cadeia(Esqueleto esqueleto, Parametros parametros, Edicao edicao, int extraProva) {
        Map<String, Integer> inicio = new HashMap<>();
        Map<String, Integer> fim = new HashMap<>();
        for (EtapaModelo etapa : esqueleto.etapas()) {
            Referencia ref = etapa.referencia();
            int base = switch (ref.tipo()) {
                case MARCO_ZERO -> 0;
                case INICIO -> inicio.get(ref.codigo());
                case FIM -> fim.get(ref.codigo());
                case ULTIMO_RESULTADO -> ultimoResultado(esqueleto, edicao, inicio);
            };
            int dias = diasAtuais(etapa, parametros, edicao);
            if (etapa.codigo().equals(EtapaModelo.PROVA)) {
                dias += extraProva;
            }
            int comeco = base + dias;
            inicio.put(etapa.codigo(), comeco);
            fim.put(etapa.codigo(), comeco + duracaoAtual(etapa, parametros, edicao));
        }
        return new Posicoes(inicio, fim, extraProva);
    }

    private static int ultimoResultado(Esqueleto esqueleto, Edicao edicao, Map<String, Integer> inicio) {
        return esqueleto.etapas().stream()
                .filter(EtapaModelo::resultadoFinal)
                .filter(e -> esqueleto.grupo(e.grupo()).ativo(edicao.modulos()))
                .mapToInt(e -> inicio.get(e.codigo()))
                .max().orElse(0);
    }

    private static int deslocamento(Referencia ref, String codigo, Posicoes a, Posicoes b) {
        if (!codigo.equals(ref.codigo())) {
            return 0;
        }
        return switch (ref.tipo()) {
            case INICIO -> b.inicio().get(codigo) - a.inicio().get(codigo);
            case FIM -> b.fim().get(codigo) - a.fim().get(codigo);
            default -> 0;
        };
    }

    // ── valores atuais e travas ───────────────────────────────────────────────

    static int diasAtuais(EtapaModelo etapa, Parametros parametros, Edicao edicao) {
        Edicao.Ajuste ajuste = edicao.ajuste(etapa.codigo());
        return ajuste != null && ajuste.dias() != null ? ajuste.dias() : etapa.diasModelo(parametros);
    }

    static int duracaoAtual(EtapaModelo etapa, Parametros parametros, Edicao edicao) {
        Edicao.Ajuste ajuste = edicao.ajuste(etapa.codigo());
        return ajuste != null && ajuste.duracao() != null ? ajuste.duracao() : etapa.duracaoModelo(parametros);
    }

    private static boolean travado(String prazo, Edicao edicao) {
        return prazo != null && Parametros.PRAZOS_PADRAO.contains(prazo) && !edicao.excecoes().contains(prazo);
    }

    private static void exigirEditavel(EtapaModelo etapa, boolean dias, int valor, Edicao edicao) {
        if (edicao.removidas().contains(etapa.codigo())) {
            throw new RegraDeNegocioException("A etapa " + etapa.codigo() + " está removida. Restaure antes de editar.");
        }
        if (dias && !etapa.temDias()) {
            throw new RegraDeNegocioException("A etapa " + etapa.codigo() + " é o marco zero e não tem dias.");
        }
        if (!dias && valor < 0) {
            throw new RegraDeNegocioException("A duração não pode ser negativa.");
        }
        String prazo = dias ? etapa.parametroDias() : etapa.parametroDuracao();
        if (travado(prazo, edicao)) {
            throw new RegraDeNegocioException("Esse campo segue o prazo padrão (" + Parametros.DESCRICOES.get(prazo)
                    + "). Use exceção para alterar.");
        }
    }

    /** Valor igual ao do modelo apaga o ajuste ("voltar ao modelo"). */
    private static Edicao comValor(Edicao edicao, EtapaModelo etapa, Parametros parametros, boolean dias, int valor) {
        Edicao.Ajuste atual = edicao.ajuste(etapa.codigo());
        Integer novoDias = atual == null ? null : atual.dias();
        Integer novaDuracao = atual == null ? null : atual.duracao();
        if (dias) {
            novoDias = valor == etapa.diasModelo(parametros) ? null : valor;
        } else {
            novaDuracao = valor == etapa.duracaoModelo(parametros) ? null : valor;
        }
        return edicao.comAjuste(etapa.codigo(), new Edicao.Ajuste(novoDias, novaDuracao));
    }

    // ── linha, checagens e formatação ─────────────────────────────────────────

    private static EtapaCalculada linha(EtapaModelo etapa, boolean grupoAtivo, Parametros parametros,
                                        Edicao edicao, Posicoes pos, int prova) {
        String codigo = etapa.codigo();
        LocalDate d = edicao.dataEdital();
        boolean removida = edicao.removidas().contains(codigo);
        boolean ativa = grupoAtivo && !removida;
        int dias = diasAtuais(etapa, parametros, edicao);
        int duracao = duracaoAtual(etapa, parametros, edicao);
        int inicio = pos.inicio().get(codigo);
        int fim = pos.fim().get(codigo);
        Edicao.Ajuste ajuste = edicao.ajuste(codigo);

        List<String> avisos = new ArrayList<>();
        if (d != null && ativa) {
            if (codigo.equals(EtapaModelo.PROVA)) {
                if (pos.extra() > 0) {
                    avisos.add("+" + dias(pos.extra()) + " para cair no domingo (automático)");
                }
            } else {
                LocalDate data = d.plusDays(inicio);
                DayOfWeek dia = data.getDayOfWeek();
                if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
                    LocalDate segunda = data.plusDays(dia == DayOfWeek.SATURDAY ? 2 : 1);
                    avisos.add("cai em " + DIA_SEMANA[dia.getValue() - 1] + " · ajusta para segunda "
                            + segunda.format(DIA_MES));
                }
            }
        }
        if (ativa && etapa.temFaixa() && etapa.temDias() && (dias < etapa.faixaP25() || dias > etapa.faixaP75())) {
            avisos.add("fora da faixa histórica (" + etapa.faixaP25() + "–" + etapa.faixaP75() + " dias)");
        }

        Referencia ref = etapa.referencia();
        return new EtapaCalculada(codigo, etapa.descricao(), etapa.grupo(), ref.texto(), ref.codigo(),
                etapa.tipoStatus(), etapa.temDias() ? etapa.diasModelo(parametros) : null, dias,
                etapa.faixaP25(), etapa.faixaP75(), etapa.duracaoModelo(parametros), duracao,
                inicio, fim, inicio - prova, data(d, inicio), data(d, fim),
                ativa, removida, etapa.removivel(), ajuste != null && !removida,
                etapa.temDias(), travado(etapa.parametroDias(), edicao), etapa.parametroDias(),
                travado(etapa.parametroDuracao(), edicao), etapa.parametroDuracao(), List.copyOf(avisos));
    }

    private static CronogramaCalculado.Checagens checagens(Parametros parametros, Edicao edicao, Posicoes pos,
                                                           LocalDate hoje) {
        List<CronogramaCalculado.Checagem> itens = new ArrayList<>();
        int minimo = parametros.valor(Parametros.RM01_MINIMO);
        int intervalo = pos.inicio().get(EtapaModelo.PROVA) - pos.fim().get(EtapaModelo.INSCRICOES);
        itens.add(new CronogramaCalculado.Checagem("RM-01",
                "Do fim das inscrições (E04) até a prova (E15): no mínimo " + dias(minimo),
                intervalo >= minimo, dias(intervalo)));

        LocalDate d = edicao.dataEdital();
        itens.add(new CronogramaCalculado.Checagem("EDITAL_NO_PRAZO",
                "A data do edital não pode ser anterior a hoje",
                d == null || !d.isBefore(hoje),
                d == null ? "sem data do edital (rascunho)" : d.format(DATA)));

        LocalDate prova = data(d, pos.inicio().get(EtapaModelo.PROVA));
        itens.add(new CronogramaCalculado.Checagem("PROVA_DOMINGO", "Prova objetiva em domingo",
                prova == null || prova.getDayOfWeek() == DayOfWeek.SUNDAY,
                prova == null ? "ajustada ao informar o edital" : "ajuste automático de +" + dias(pos.extra())));

        boolean viola = itens.stream().anyMatch(c -> !c.cumprida());
        return new CronogramaCalculado.Checagens(viola, List.copyOf(itens));
    }

    private static LocalDate data(LocalDate edital, int deslocamento) {
        return edital == null ? null : edital.plusDays(deslocamento);
    }

    private static String rotulo(LocalDate edital, int deslocamento) {
        if (edital != null) {
            return edital.plusDays(deslocamento).format(DATA);
        }
        return deslocamento == 0 ? "D" : deslocamento > 0 ? "D+" + deslocamento : "D−" + (-deslocamento);
    }

    private static String dias(int n) {
        return n == 1 ? "1 dia" : n + " dias";
    }
}
