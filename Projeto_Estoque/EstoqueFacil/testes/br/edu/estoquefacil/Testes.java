package br.edu.estoquefacil;

import br.edu.estoquefacil.Modelo.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Suíte independente de bibliotecas externas. Cada caso falha por asserção explícita. */
public final class Testes {
    private static int aprovados=0, falhas=0;
    private static final List<String> resultados=new ArrayList<>();
    private static final Clock FIXO=Clock.fixed(Instant.parse("2026-10-28T16:00:00Z"),ZoneOffset.UTC);
    private static final class Memoria implements Repositorio {
        Dados estado=Dados.vazio();boolean falhar;
        public Dados carregar(){return estado;}
        public void salvar(Dados d)throws IOException{if(falhar)throw new IOException("Falha de disco simulada");estado=d;}
    }
    @FunctionalInterface interface Caso{void executar()throws Exception;}
    private static void caso(String nome,Caso c){
        try{c.executar();aprovados++;resultados.add("{\"caso\":\""+nome+"\",\"resultado\":\"APROVADO\"}");System.out.println("APROVADO "+nome);}
        catch(Throwable t){falhas++;resultados.add("{\"caso\":\""+nome+"\",\"resultado\":\"FALHOU\"}");System.err.println("FALHOU "+nome+": "+t);t.printStackTrace();}
    }
    private static void igual(Object esperado,Object real){if(!Objects.equals(esperado,real))throw new AssertionError("Esperado "+esperado+"; obtido "+real);}
    private static void verdadeiro(boolean valor){if(!valor)throw new AssertionError("Condição não atendida");}
    private static void rejeitar(Class<? extends Exception> tipo,String trecho,Caso c)throws Exception{
        try{c.executar();}catch(Exception e){if(!tipo.isInstance(e)||!Objects.requireNonNullElse(e.getMessage(),"").contains(trecho))throw new AssertionError("Exceção inesperada: "+e);return;}throw new AssertionError("A operação deveria ser rejeitada");
    }
    private static Produto produto(){return new Produto("P001","Café torrado","Alimentos",1234,3);}
    private static EstoqueServico novo()throws IOException{EstoqueServico s=new EstoqueServico(new Memoria(),FIXO);s.cadastrar(produto());return s;}
    private static Path pasta()throws IOException{return Files.createTempDirectory("estoque-teste-");}
    private static void ui(Caso c)throws Exception{SwingUtilities.invokeAndWait(()->{try{c.executar();}catch(Exception e){throw new RuntimeException(e);}});}
    public static void main(String[] args)throws Exception{
        System.setProperty("java.awt.headless","true");
        caso("T01 cadastro normaliza codigo",()->igual("AB_1",new Produto(" ab_1 ","Arroz","Comida",500,0).codigo()));
        caso("T02 produto inicia com saldo zero",()->igual(0,novo().saldo("P001")));
        caso("T03 codigo duplicado e rejeitado",()->{EstoqueServico s=novo();rejeitar(IllegalArgumentException.class,"Já existe",()->s.cadastrar(produto()));igual(1,s.dados().produtos().size());});
        caso("T04 entrada soma unidades",()->{EstoqueServico s=novo();s.movimentar("P001",Tipo.ENTRADA,10,"Compra teste");igual(10,s.saldo("P001"));igual(FIXO.instant(),s.dados().movimentos().get(0).instante());});
        caso("T05 saida subtrai unidades",()->{EstoqueServico s=novo();s.movimentar("P001",Tipo.ENTRADA,10,"Compra teste");s.movimentar("P001",Tipo.SAIDA,4,"Retirada teste");igual(6,s.saldo("P001"));});
        caso("T06 saida insuficiente nao altera historico",()->{EstoqueServico s=novo();rejeitar(IllegalArgumentException.class,"Saldo insuficiente",()->s.movimentar("P001",Tipo.SAIDA,1,"Retirada teste"));igual(0,s.dados().movimentos().size());igual(0,s.saldo("P001"));});
        caso("T07 quantidade zero rejeitada",()->rejeitar(IllegalArgumentException.class,"Quantidade",()->novo().movimentar("P001",Tipo.ENTRADA,0,"Compra teste")));
        caso("T08 quantidade negativa rejeitada",()->rejeitar(IllegalArgumentException.class,"Quantidade",()->novo().movimentar("P001",Tipo.ENTRADA,-2,"Compra teste")));
        caso("T09 limite de saldo respeitado",()->{EstoqueServico s=novo();s.movimentar("P001",Tipo.ENTRADA,1_000_000,"Compra teste");rejeitar(IllegalArgumentException.class,"Saldo inválido",()->s.movimentar("P001",Tipo.ENTRADA,1,"Compra teste"));igual(1_000_000,s.saldo("P001"));});
        caso("T10 motivo obrigatorio",()->rejeitar(IllegalArgumentException.class,"Motivo",()->novo().movimentar("P001",Tipo.ENTRADA,1,"")));
        caso("T11 custo com tres casas rejeitado",()->rejeitar(IllegalArgumentException.class,"Custo",()->Modelo.centavos("1,234")));
        caso("T12 centavos e total exatos",()->{igual(10L,Modelo.centavos("0,10"));EstoqueServico s=novo();s.movimentar("P001",Tipo.ENTRADA,3,"Compra teste");igual(3702L,s.valorTotal());});
        caso("T13 custo e minimo invalidos rejeitados",()->{rejeitar(IllegalArgumentException.class,"Custo",()->Modelo.centavos("0"));rejeitar(IllegalArgumentException.class,"mínimo",()->new Produto("P1","Arroz","Alimentos",100,-1));});
        caso("T14 edicao preserva saldo e nome historico",()->{EstoqueServico s=novo();s.movimentar("P001",Tipo.ENTRADA,5,"Compra teste");s.editar(new Produto("P001","Café especial","Bebidas",1500,2));igual(5,s.saldo("P001"));igual("Café torrado",s.dados().movimentos().get(0).nomeNaData());igual(7500L,s.valorTotal());});
        caso("T15 alerta inclui igualdade ao minimo",()->{EstoqueServico s=novo();s.movimentar("P001",Tipo.ENTRADA,3,"Compra teste");igual(1L,s.alertas());s.movimentar("P001",Tipo.ENTRADA,1,"Compra teste");igual(0L,s.alertas());});
        caso("T16 pesquisa ignora caixa e acento",()->{EstoqueServico s=novo();igual(1,s.listar("CAFE",false).size());igual(1,s.listar("alimentos",false).size());igual(0,s.listar("inexistente",false).size());});
        caso("T17 persistencia sobrevive reabertura",()->{Path p=pasta().resolve("base.efdb");try(RepositorioArquivo r=new RepositorioArquivo(p)){EstoqueServico s=new EstoqueServico(r);s.cadastrar(produto());s.movimentar("P001",Tipo.ENTRADA,7,"Compra teste");}try(RepositorioArquivo r=new RepositorioArquivo(p)){EstoqueServico s=new EstoqueServico(r);igual(7,s.saldo("P001"));igual(1,s.dados().movimentos().size());}});
        caso("T18 arquivo com bit alterado rejeitado",()->{Path p=pasta().resolve("base.efdb");RepositorioArquivo.backup(p,novo().dados());byte[] b=Files.readAllBytes(p);b[16]^=1;Files.write(p,b);rejeitar(IOException.class,"corrompido",()->RepositorioArquivo.ler(p));});
        caso("T19 arquivo truncado rejeitado",()->{Path p=pasta().resolve("base.efdb");Files.write(p,new byte[]{0,1,2});rejeitar(EOFException.class,"",()->RepositorioArquivo.ler(p));});
        caso("T20 segunda instancia bloqueada",()->{Path p=pasta().resolve("base.efdb");try(RepositorioArquivo a=new RepositorioArquivo(p)){rejeitar(IOException.class,"instância",()->{try(RepositorioArquivo b=new RepositorioArquivo(p)){};});}try(RepositorioArquivo c=new RepositorioArquivo(p)){igual(0,c.carregar().produtos().size());}});
        caso("T21 falha de disco preserva estado anterior",()->{Memoria r=new Memoria();EstoqueServico s=new EstoqueServico(r,FIXO);s.cadastrar(produto());r.falhar=true;rejeitar(IOException.class,"disco",()->s.movimentar("P001",Tipo.ENTRADA,2,"Compra teste"));igual(0,s.saldo("P001"));igual(0,r.estado.movimentos().size());});
        caso("T22 backup restaura em base vazia",()->{EstoqueServico s=novo();s.movimentar("P001",Tipo.ENTRADA,9,"Compra teste");Path p=pasta().resolve("backup.efdb");s.backup(p);EstoqueServico outro=new EstoqueServico(new Memoria());outro.restaurar(p);igual(9,outro.saldo("P001"));});
        caso("T23 restauracao nao sobrescreve base preenchida",()->{Path p=pasta().resolve("b.efdb");EstoqueServico s=novo();s.backup(p);rejeitar(IllegalArgumentException.class,"base vazia",()->s.restaurar(p));igual(1,s.dados().produtos().size());});
        caso("T24 demonstracao reproduz totais",()->{EstoqueServico s=new EstoqueServico(new Memoria());s.demonstracao();igual(6,s.dados().produtos().size());igual(12,s.dados().movimentos().size());igual(65L,s.unidades());igual(3L,s.alertas());igual(61460L,s.valorTotal());});
        caso("T25 demonstracao nao substitui dados existentes",()->{EstoqueServico s=novo();rejeitar(IllegalArgumentException.class,"base vazia",s::demonstracao);igual(1,s.dados().produtos().size());});
        caso("T26 CSV preserva acentos e escapa texto",()->{EstoqueServico s=new EstoqueServico(new Memoria());s.cadastrar(new Produto("A1","=SOMA;\"Café\"","Alimentos",100,0));Path p=pasta().resolve("saida.csv");s.exportarEstoque(p);String t=Files.readString(p);verdadeiro(t.startsWith("\uFEFF"));verdadeiro(t.contains("\"'=SOMA;\"\"Café\"\"\""));});
        caso("T27 exportacao nao sobrescreve arquivo existente",()->{EstoqueServico s=novo();Path p=pasta().resolve("saida.csv");Files.writeString(p,"original");rejeitar(FileAlreadyExistsException.class,"",()->s.exportarEstoque(p));igual("original",Files.readString(p));});
        caso("T28 historico com produto ausente rejeitado",()->{Movimento m=new Movimento(1,FIXO.instant(),"X1","Produto teste",Tipo.ENTRADA,1,"Teste de arquivo");rejeitar(IllegalArgumentException.class,"Histórico inconsistente",()->Modelo.validar(new Dados(Map.of(),List.of(m))));});
        caso("T29 dados publicados sao imutaveis",()->{EstoqueServico s=novo();rejeitar(UnsupportedOperationException.class,"",()->s.dados().produtos().clear());});
        caso("T30 interface cadastra por acionamento do botao",()->ui(()->{EstoqueServico s=new EstoqueServico(new Memoria());EstoquePainel p=new EstoquePainel(s,"Teste");p.codigo.setText("UI01");p.nome.setText("Produto de teste");p.categoria.setText("Teste");p.custo.setText("2,50");p.minimo.setText("1");p.salvarProduto.doClick();igual(1,s.dados().produtos().size());verdadeiro(p.mensagem.getText().startsWith("Produto salvo"));}));
        caso("T31 interface registra entrada pelo formulario",()->ui(()->{EstoqueServico s=novo();EstoquePainel p=new EstoquePainel(s,"Teste");p.quantidadeMov.setText("8");p.motivoMov.setText("Compra pela interface");p.registrarMov.doClick();igual(8,s.saldo("P001"));igual("Movimentação registrada e salva.",p.mensagem.getText());}));
        caso("T32 interface mostra erro sem alterar saldo",()->ui(()->{EstoqueServico s=novo();EstoquePainel p=new EstoquePainel(s,"Teste");p.tipoMov.setSelectedIndex(1);p.quantidadeMov.setText("1");p.motivoMov.setText("Retirada pela interface");p.registrarMov.doClick();igual(0,s.saldo("P001"));verdadeiro(p.mensagem.getText().contains("Saldo insuficiente"));}));
        caso("T33 interface filtra pesquisa e reposicao",()->ui(()->{EstoqueServico s=new EstoqueServico(new Memoria());s.demonstracao();EstoquePainel p=new EstoquePainel(s,"Teste");p.somenteBaixos.doClick();igual(3,p.estoqueTabela.getRowCount());p.busca.setText("CAFE");igual(1,p.estoqueTabela.getRowCount());}));
        String json="{\n  \"data_referencia\":\"2026-10-28\",\n  \"java\":\""+System.getProperty("java.version")+"\",\n  \"sistema\":\""+System.getProperty("os.name")+"\",\n  \"aprovados\":"+aprovados+",\n  \"falhas\":"+falhas+",\n  \"casos\":[\n    "+String.join(",\n    ",resultados)+"\n  ]\n}\n";
        if(args.length==1)Files.writeString(Path.of(args[0]),json);
        System.out.println("RESULTADO: "+aprovados+" aprovados, "+falhas+" falhas.");if(falhas!=0)System.exit(1);
    }
}
