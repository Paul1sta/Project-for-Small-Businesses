package br.edu.estoquefacil;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;

/** Entidades imutáveis e regras comuns do domínio. Quantidades são unidades inteiras. */
public final class Modelo {
    private Modelo() {}
    public static final int LIMITE_QUANTIDADE = 1_000_000;
    public static final int LIMITE_PRODUTOS = 10_000;
    public static final int LIMITE_MOVIMENTOS = 100_000;
    public static final long LIMITE_CUSTO = 100_000_000L;

    public static String texto(String valor, String campo, int minimo, int maximo) {
        String s = Objects.requireNonNullElse(valor, "").strip();
        if (s.length() < minimo || s.length() > maximo || s.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException(campo + ": informe de " + minimo + " a " + maximo + " caracteres, sem quebras de linha.");
        return s;
    }
    public static String codigo(String valor) {
        String s = Objects.requireNonNullElse(valor, "").strip().toUpperCase(Locale.ROOT);
        if (!s.matches("[A-Z0-9_-]{1,24}"))
            throw new IllegalArgumentException("Código: use até 24 letras sem acento, números, hífen ou sublinhado.");
        return s;
    }
    public static String pesquisa(String valor) {
        return Normalizer.normalize(Objects.requireNonNullElse(valor, ""), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
    public static long centavos(String valor) {
        String s = Objects.requireNonNullElse(valor, "").strip();
        if (!s.matches("[0-9]{1,7}([,.][0-9]{1,2})?"))
            throw new IllegalArgumentException("Custo: use valor positivo com até duas casas, por exemplo 12,50. Não use separador de milhar.");
        long c = new BigDecimal(s.replace(',', '.')).movePointRight(2).longValueExact();
        if (c <= 0 || c > LIMITE_CUSTO) throw new IllegalArgumentException("Custo deve ser maior que zero e até R$ 1.000.000,00.");
        return c;
    }
    public static String decimal(long centavos) { return BigDecimal.valueOf(centavos, 2).toPlainString().replace('.', ','); }
    public static String reais(long centavos) {
        return java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(BigDecimal.valueOf(centavos, 2));
    }
    public record Produto(String codigo, String nome, String categoria, long custoCentavos, int minimo) {
        public Produto {
            codigo = Modelo.codigo(codigo);
            nome = texto(nome, "Nome", 2, 100);
            categoria = texto(categoria, "Categoria", 2, 60);
            if (custoCentavos <= 0 || custoCentavos > LIMITE_CUSTO) throw new IllegalArgumentException("Custo inválido.");
            if (minimo < 0 || minimo > LIMITE_QUANTIDADE) throw new IllegalArgumentException("Estoque mínimo deve ficar entre 0 e 1.000.000.");
        }
        @Override public String toString() { return codigo + " - " + nome; }
    }
    public enum Tipo { ENTRADA, SAIDA }
    public record Movimento(long id, Instant instante, String produto, String nomeNaData, Tipo tipo, int quantidade, String motivo) {
        public Movimento {
            if (id <= 0 || instante == null || tipo == null) throw new IllegalArgumentException("Movimentação inválida.");
            produto = codigo(produto);
            nomeNaData = texto(nomeNaData, "Nome na data", 2, 100);
            motivo = texto(motivo, "Motivo", 3, 240);
            if (quantidade < 1 || quantidade > LIMITE_QUANTIDADE) throw new IllegalArgumentException("Quantidade deve ficar entre 1 e 1.000.000 unidades.");
        }
    }
    public record Dados(Map<String, Produto> produtos, List<Movimento> movimentos) {
        public Dados {
            produtos = Collections.unmodifiableMap(new LinkedHashMap<>(produtos));
            movimentos = List.copyOf(movimentos);
        }
        public static Dados vazio() { return new Dados(Map.of(), List.of()); }
    }
    public record Linha(Produto produto, int saldo) {
        public boolean baixo() { return saldo <= produto.minimo(); }
        public long valorCentavos() { return Math.multiplyExact((long) saldo, produto.custoCentavos()); }
    }

    /** Confere também arquivos importados; o saldo é reconstruído pelo histórico. */
    public static Map<String, Integer> validar(Dados dados) {
        if (dados.produtos().size() > LIMITE_PRODUTOS || dados.movimentos().size() > LIMITE_MOVIMENTOS)
            throw new IllegalArgumentException("Limite de registros excedido.");
        Map<String, Integer> saldos = new HashMap<>();
        dados.produtos().forEach((chave, p) -> {
            if (!chave.equals(p.codigo())) throw new IllegalArgumentException("Código inconsistente no arquivo.");
            saldos.put(chave, 0);
        });
        long anterior = 0;
        for (Movimento m : dados.movimentos()) {
            if (m.id() <= anterior || !saldos.containsKey(m.produto())) throw new IllegalArgumentException("Histórico inconsistente no arquivo.");
            anterior = m.id();
            int novo = Math.addExact(saldos.get(m.produto()), m.tipo() == Tipo.ENTRADA ? m.quantidade() : -m.quantidade());
            if (novo < 0 || novo > LIMITE_QUANTIDADE) throw new IllegalArgumentException("Saldo inválido no histórico.");
            saldos.put(m.produto(), novo);
        }
        return saldos;
    }
}
