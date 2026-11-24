package br.edu.estoquefacil;

import br.edu.estoquefacil.Modelo.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Casos de uso. Só publica uma alteração na memória depois de salvar o arquivo. */
public final class EstoqueServico {
    private final Repositorio repositorio;
    private final Clock relogio;
    private Dados dados;
    private Map<String,Integer> saldos;
    public EstoqueServico(Repositorio repositorio) throws IOException { this(repositorio, Clock.systemUTC()); }
    public EstoqueServico(Repositorio repositorio, Clock relogio) throws IOException {
        this.repositorio = repositorio; this.relogio = relogio;
        dados = repositorio.carregar(); saldos = Modelo.validar(dados);
    }
    public synchronized Dados dados() { return dados; }
    private void confirmar(Dados proposta) throws IOException {
        Map<String,Integer> novos = Modelo.validar(proposta);
        repositorio.salvar(proposta); // Falha de disco: a memória e o arquivo anterior permanecem como estavam.
        dados = proposta; saldos = novos;
    }
    public synchronized void cadastrar(Produto produto) throws IOException {
        if (dados.produtos().containsKey(produto.codigo())) throw new IllegalArgumentException("Já existe um produto com esse código.");
        Map<String,Produto> ps = new LinkedHashMap<>(dados.produtos()); ps.put(produto.codigo(), produto);
        confirmar(new Dados(ps, dados.movimentos()));
    }
    public synchronized void editar(Produto produto) throws IOException {
        produto(produto.codigo());
        Map<String,Produto> ps = new LinkedHashMap<>(dados.produtos()); ps.put(produto.codigo(), produto);
        confirmar(new Dados(ps, dados.movimentos()));
    }
    public synchronized Produto produto(String codigo) {
        Produto p = dados.produtos().get(Modelo.codigo(codigo));
        if (p == null) throw new IllegalArgumentException("Produto não encontrado.");
        return p;
    }
    public synchronized int saldo(String codigo) { return saldos.get(produto(codigo).codigo()); }
    public synchronized void movimentar(String codigo, Tipo tipo, int quantidade, String motivo) throws IOException {
        Produto p = produto(codigo);
        if (tipo == null) throw new IllegalArgumentException("Selecione entrada ou saída.");
        long id = dados.movimentos().isEmpty() ? 1 : Math.addExact(dados.movimentos().get(dados.movimentos().size()-1).id(), 1);
        Movimento m = new Movimento(id, relogio.instant(), p.codigo(), p.nome(), tipo, quantidade, motivo);
        if (tipo == Tipo.SAIDA && quantidade > saldo(codigo)) throw new IllegalArgumentException("Saldo insuficiente. Disponível: " + saldo(codigo) + " unidade(s).");
        List<Movimento> ms = new ArrayList<>(dados.movimentos()); ms.add(m);
        confirmar(new Dados(dados.produtos(), ms));
    }
    public synchronized List<Linha> listar(String busca, boolean somenteBaixos) {
        String termo = Modelo.pesquisa(busca);
        return dados.produtos().values().stream().map(p -> new Linha(p, saldos.get(p.codigo())))
            .filter(l -> Modelo.pesquisa(l.produto().codigo()+" "+l.produto().nome()+" "+l.produto().categoria()).contains(termo))
            .filter(l -> !somenteBaixos || l.baixo())
            .sorted(Comparator.comparing(l -> l.produto().nome(), String.CASE_INSENSITIVE_ORDER)).toList();
    }
    public synchronized long valorTotal() { long v = 0; for (Linha l : listar("", false)) v = Math.addExact(v,l.valorCentavos()); return v; }
    public synchronized long unidades() { return saldos.values().stream().mapToLong(Integer::longValue).sum(); }
    public synchronized long alertas() { return listar("",true).size(); }
    public synchronized void backup(Path destino) throws IOException { RepositorioArquivo.backup(destino, dados); }
    public synchronized void restaurar(Path origem) throws IOException {
        if (!dados.produtos().isEmpty() || !dados.movimentos().isEmpty())
            throw new IllegalArgumentException("Restauração permitida somente em uma base vazia. Use outra pasta para preservar os dados atuais.");
        confirmar(RepositorioArquivo.ler(origem));
    }
    public synchronized void demonstracao() throws IOException {
        if (!dados.produtos().isEmpty() || !dados.movimentos().isEmpty()) throw new IllegalArgumentException("O exemplo só pode ser carregado em uma base vazia.");
        Map<String,Produto> ps = new LinkedHashMap<>(); List<Movimento> ms = new ArrayList<>();
        String[][] base = {{"ARZ001","Arroz 5 kg","Alimentos","24,90","5","20","6"},
            {"FEJ001","Feijão 1 kg","Alimentos","8,50","8","15","10"},
            {"CAF001","Café 500 g","Alimentos","16,80","4","12","8"},
            {"LEI001","Leite 1 L","Bebidas","4,60","10","30","6"},
            {"DET001","Detergente 500 mL","Limpeza","2,20","5","18","3"},
            {"ACU001","Açúcar 1 kg","Alimentos","4,30","6","10","7"}};
        long id = 1; Instant t = Instant.parse("2026-10-28T13:00:00Z");
        for (String[] r : base) {
            Produto p = new Produto(r[0],r[1],r[2],Modelo.centavos(r[3]),Integer.parseInt(r[4])); ps.put(p.codigo(),p);
            ms.add(new Movimento(id++,t.plusSeconds(id*60),p.codigo(),p.nome(),Tipo.ENTRADA,Integer.parseInt(r[5]),"Carga inicial simulada"));
            ms.add(new Movimento(id++,t.plusSeconds(id*60),p.codigo(),p.nome(),Tipo.SAIDA,Integer.parseInt(r[6]),"Retirada simulada para demonstração"));
        }
        confirmar(new Dados(ps,ms));
    }
    /** Aspas e neutralização de fórmulas em campos textuais para exportação a planilhas. */
    static String csv(String valor) {
        String s = valor;
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'"+s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
    public synchronized void exportarEstoque(Path destino) throws IOException {
        StringBuilder b = new StringBuilder("\uFEFFcodigo;produto;categoria;saldo_unidades;minimo;custo_unitario_reais;valor_estoque_reais;situacao\r\n");
        for (Linha l : listar("",false)) {
            Produto p = l.produto();
            b.append(csv(p.codigo())).append(';').append(csv(p.nome())).append(';').append(csv(p.categoria())).append(';')
             .append(l.saldo()).append(';').append(p.minimo()).append(';').append(Modelo.decimal(p.custoCentavos())).append(';')
             .append(Modelo.decimal(l.valorCentavos())).append(';').append(l.baixo()?"REPOR":"REGULAR").append("\r\n");
        }
        Files.writeString(destino,b.toString(),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
    }
    public synchronized void exportarHistorico(Path destino) throws IOException {
        StringBuilder b = new StringBuilder("\uFEFFid;data_hora_utc;codigo;nome_na_data;tipo;quantidade_unidades;motivo\r\n");
        for (Movimento m : dados.movimentos()) b.append(m.id()).append(';').append(csv(m.instante().toString())).append(';')
            .append(csv(m.produto())).append(';').append(csv(m.nomeNaData())).append(';').append(m.tipo()).append(';')
            .append(m.quantidade()).append(';').append(csv(m.motivo())).append("\r\n");
        Files.writeString(destino,b.toString(),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
    }
}
