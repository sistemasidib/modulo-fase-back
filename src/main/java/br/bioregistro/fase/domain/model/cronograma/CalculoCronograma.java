package br.bioregistro.fase.domain.model.cronograma;

import br.bioregistro.fase.domain.exception.RegraDeNegocioException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cálculo puro do cronograma (RN11–RN24): sem banco e sem relógio — "hoje" entra como parâmetro.
 * Porta o {@code calc}/{@code calcEff}/{@code ovWith}/{@code moved} do protótipo.
 *
 * <p>Início = data de referência + dias; fim = início + duração; tudo em dias corridos. Etapas
 * removidas continuam na cadeia, para que as que dependem delas fiquem no mesmo lugar (RN22).
 * A prova (E15) é âncora quando a data dela é informada; senão é uma sugestão provisória. Nada é
 * deslocado sozinho: o cálculo só avisa (spec 005, RN09).
 */
public final class CalculoCronograma {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final String[] DIA_SEMANA = {"seg", "ter", "qua", "qui", "sex", "sáb", "dom"};

    private CalculoCronograma() {
    }

    /** Posições em dias desde o edital. */
    private record Posicoes(Map<String, Integer> inicio, Map<String, Integer> fim) {
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
            List<ItemPersonalizado> itens = edicao.itens().stream()
                    .filter(i -> i.grupo().equals(grupo.codigo()))
                    .sorted(Comparator.comparingInt((ItemPersonalizado i) -> inicioDoItem(i, pos))
                            .thenComparing(ItemPersonalizado::codigo))
                    .toList();
            for (ItemPersonalizado item : itens) {
                EtapaCalculada calculada = linhaDoItem(item, grupoAtivo, d, pos, prova);
                if (calculada.ativa()) {
                    ativas++;
                }
                etapas.add(calculada);
            }
            grupos.add(new CronogramaCalculado.Grupo(grupo.codigo(), grupo.nome(), grupo.fase(), grupo.modulo(),
                    grupo.nota(), grupoAtivo, List.copyOf(etapas)));
        }

        int resultado = pos.inicio().get(EtapaModelo.RESULTADO_FINAL);
        var indicadores = new CronogramaCalculado.Indicadores(ativas,
                esqueleto.etapas().size() + edicao.itens().size(), d,
                prova, data(d, prova), resultado, data(d, resultado));
        return new CronogramaCalculado(d != null, d, edicao.provaInformada(), List.copyOf(grupos), indicadores,
                checagens(parametros, edicao, pos, hoje));
    }

    /** RN20: efeito de uma edição de célula, sem gravar. As dependentes se movem em cascata. */
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
        for (ItemPersonalizado item : edicao.itens()) {
            int de = inicioDoItem(item, a);
            int para = inicioDoItem(item, b);
            if (de != para && esqueleto.grupo(item.grupo()).ativo(edicao.modulos())) {
                movidas++;
                itens.add(new Previa.Item(item.codigo(), edicao.dataEdital() != null ? "data" : "início",
                        rotulo(edicao.dataEdital(), de), rotulo(edicao.dataEdital(), para)));
            }
        }
        return new Previa(nova, List.copyOf(itens), movidas);
    }

    // ── cadeia ────────────────────────────────────────────────────────────────

    private static Posicoes posicoes(Esqueleto esqueleto, Parametros parametros, Edicao edicao) {
        Map<String, Integer> inicio = new HashMap<>();
        Map<String, Integer> fim = new HashMap<>();
        List<EtapaModelo> pendentes = new ArrayList<>(esqueleto.etapas());
        while (!pendentes.isEmpty()) {
            int antes = pendentes.size();
            for (var it = pendentes.iterator(); it.hasNext(); ) {
                EtapaModelo etapa = it.next();
                Referencia ref = etapa.referencia();
                Integer base = switch (ref.tipo()) {
                    case MARCO_ZERO -> 0;
                    case INICIO -> inicio.get(ref.codigo());
                    case FIM -> fim.get(ref.codigo());
                    case ULTIMO_RESULTADO -> ultimoResultado(esqueleto, edicao, inicio);
                };
                if (base == null) {
                    continue;
                }
                int comeco = etapa.codigo().equals(EtapaModelo.PROVA) && edicao.provaInformada()
                        ? provaInformada(edicao)
                        : base + diasAtuais(etapa, parametros, edicao);
                inicio.put(etapa.codigo(), comeco);
                fim.put(etapa.codigo(), comeco + duracaoAtual(etapa, parametros, edicao));
                it.remove();
            }
            if (pendentes.size() == antes) {
                throw new IllegalStateException("Referência circular no esqueleto " + esqueleto.nome() + ": "
                        + pendentes.stream().map(EtapaModelo::codigo).toList());
            }
        }
        return new Posicoes(inicio, fim);
    }

    private static int provaInformada(Edicao edicao) {
        return (int) ChronoUnit.DAYS.between(edicao.dataEdital(), edicao.dataProva());
    }

    private static int base(Referencia ref, Posicoes pos) {
        return switch (ref.tipo()) {
            case INICIO -> pos.inicio().get(ref.codigo());
            case FIM -> pos.fim().get(ref.codigo());
            default -> 0;
        };
    }

    /** {@code null} enquanto algum resultado definitivo ativo ainda não foi calculado. */
    private static Integer ultimoResultado(Esqueleto esqueleto, Edicao edicao, Map<String, Integer> inicio) {
        List<EtapaModelo> resultados = esqueleto.etapas().stream()
                .filter(EtapaModelo::resultadoFinal)
                .filter(e -> esqueleto.grupo(e.grupo()).ativo(edicao.modulos()))
                .toList();
        if (resultados.stream().anyMatch(e -> !inicio.containsKey(e.codigo()))) {
            return null;
        }
        return resultados.stream().mapToInt(e -> inicio.get(e.codigo())).max().orElse(0);
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
        if (dias && etapa.codigo().equals(EtapaModelo.PROVA) && edicao.provaInformada()) {
            throw new RegraDeNegocioException("A prova tem data informada (" + edicao.dataProva().format(DATA)
                    + "). Mude a data da prova.");
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
        boolean ehProva = codigo.equals(EtapaModelo.PROVA);
        int duracao = duracaoAtual(etapa, parametros, edicao);
        int inicio = pos.inicio().get(codigo);
        int fim = pos.fim().get(codigo);
        int dias = ehProva && edicao.provaInformada()
                ? inicio - base(etapa.referencia(), pos)
                : diasAtuais(etapa, parametros, edicao);
        Edicao.Ajuste ajuste = edicao.ajuste(codigo);

        List<String> avisos = new ArrayList<>();
        if (d != null && ativa) {
            if (ehProva) {
                if (!edicao.provaInformada()) {
                    avisos.add("data provisória · informe a data da prova");
                }
            } else {
                avisoDeFimDeSemana(d, inicio, avisos);
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
                travado(etapa.parametroDuracao(), edicao), etapa.parametroDuracao(), List.copyOf(avisos), false);
    }

    private static int inicioDoItem(ItemPersonalizado item, Posicoes pos) {
        return base(item.referencia(), pos) + item.dias();
    }

    /** Sem modelo nem faixa: os valores do item são os do usuário. */
    private static EtapaCalculada linhaDoItem(ItemPersonalizado item, boolean grupoAtivo, LocalDate d, Posicoes pos,
                                              int prova) {
        int inicio = inicioDoItem(item, pos);
        int fim = inicio + item.duracao();
        List<String> avisos = new ArrayList<>();
        if (d != null && grupoAtivo) {
            avisoDeFimDeSemana(d, inicio, avisos);
        }
        Referencia ref = item.referencia();
        return new EtapaCalculada(item.codigo(), item.descricao(), item.grupo(), ref.texto(), ref.codigo(),
                item.tipoStatus(), item.dias(), item.dias(), null, null, item.duracao(), item.duracao(),
                inicio, fim, inicio - prova, data(d, inicio), data(d, fim),
                grupoAtivo, false, true, false,
                true, false, null, false, null, List.copyOf(avisos), true);
    }

    private static void avisoDeFimDeSemana(LocalDate edital, int inicio, List<String> avisos) {
        LocalDate data = edital.plusDays(inicio);
        DayOfWeek dia = data.getDayOfWeek();
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
            LocalDate segunda = data.plusDays(dia == DayOfWeek.SATURDAY ? 2 : 1);
            avisos.add("cai em " + DIA_SEMANA[dia.getValue() - 1] + " · ajusta para segunda " + segunda.format(DIA_MES));
        }
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
        List<CronogramaCalculado.Checagem> alertas = new ArrayList<>();
        alertas.add(new CronogramaCalculado.Checagem("EDITAL_NO_PRAZO",
                "Data do edital no passado (publicado, reprogramado ou retificado?)",
                d == null || !d.isBefore(hoje),
                d == null ? "sem data do edital (rascunho)" : d.format(DATA)));
        alertas.add(new CronogramaCalculado.Checagem("PROVA_INFORMADA", "Data da prova objetiva informada",
                d == null || edicao.provaInformada(),
                edicao.provaInformada()
                        ? edicao.dataProva().format(DATA) + " · " + DIA_SEMANA[edicao.dataProva().getDayOfWeek().getValue() - 1]
                        : "sugestão provisória"));

        boolean viola = itens.stream().anyMatch(c -> !c.cumprida());
        return new CronogramaCalculado.Checagens(viola, List.copyOf(itens), List.copyOf(alertas));
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
