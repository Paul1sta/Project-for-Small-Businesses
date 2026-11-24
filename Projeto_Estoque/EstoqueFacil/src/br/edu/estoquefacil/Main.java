package br.edu.estoquefacil;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.file.*;
import java.util.*;

/** Ponto de entrada da distribuição JAR e dos modos reproduzíveis de demonstração. */
public final class Main {
    public static void main(String[] args) {
        try {
            if(args.length==2 && args[0].equals("--demo")) {
                Path dir=Path.of(args[1]);Files.createDirectories(dir);
                try(RepositorioArquivo r=new RepositorioArquivo(dir.resolve("estoque.efdb"))){EstoqueServico s=new EstoqueServico(r);s.demonstracao();s.exportarEstoque(dir.resolve("estoque.csv"));s.exportarHistorico(dir.resolve("historico.csv"));resumo(s);}return;
            }
            if(args.length==2 && args[0].equals("--capturar")) {
                System.setProperty("java.awt.headless","true");Path dir=Path.of(args[1]);Files.createDirectories(dir);Path temporario=Files.createTempDirectory("estoque-captura-");
                try(RepositorioArquivo r=new RepositorioArquivo(temporario.resolve("estoque.efdb"))){EstoqueServico s=new EstoqueServico(r);s.demonstracao();
                    SwingUtilities.invokeAndWait(()->{try{EstoquePainel p=new EstoquePainel(s,"Base de demonstração simulada");p.capturar(dir.resolve("tela_estoque.png"),0);p.capturar(dir.resolve("tela_cadastro.png"),1);p.capturar(dir.resolve("tela_movimentacao.png"),2);p.capturar(dir.resolve("tela_historico.png"),3);}catch(Exception e){throw new RuntimeException(e);}});
                }return;
            }
            Path base=Path.of("dados","estoque.efdb");boolean verificar=false;
            for(int i=0;i<args.length;i++){
                if(args[i].equals("--dados") && i+1<args.length)base=Path.of(args[++i]);
                else if(args[i].equals("--verificar"))verificar=true;
                else throw new IllegalArgumentException("Uso: java -jar EstoqueFacil.jar [--dados caminho.efdb] [--verificar] | --demo pasta | --capturar pasta");
            }
            RepositorioArquivo repo=new RepositorioArquivo(base);
            EstoqueServico servico;
            try{servico=new EstoqueServico(repo);}catch(Exception e){repo.close();throw e;}
            if(verificar){try(repo){resumo(servico);}return;}
            if(GraphicsEnvironment.isHeadless()){repo.close();throw new IllegalStateException("A interface precisa de um ambiente gráfico. Use --verificar para verificar a base pelo terminal.");}
            SwingUtilities.invokeLater(()->{
                try{
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                    JFrame janela=new JFrame("Estoque Fácil - Projeto Integrador");janela.setContentPane(new EstoquePainel(servico,repo.caminho().toString()));
                    janela.setSize(1240,820);janela.setMinimumSize(new Dimension(1100,760));janela.setLocationRelativeTo(null);janela.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    janela.addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent e){try{repo.close();}catch(Exception ex){System.err.println(ex.getMessage());}}});janela.setVisible(true);
                }catch(Exception e){try{repo.close();}catch(Exception ignored){}JOptionPane.showMessageDialog(null,"Não foi possível iniciar: "+e.getMessage(),"Estoque Fácil",JOptionPane.ERROR_MESSAGE);}
            });
        }catch(Exception e){
            System.err.println("Não foi possível concluir: "+e.getMessage());
            if(!GraphicsEnvironment.isHeadless())JOptionPane.showMessageDialog(null,e.getMessage(),"Estoque Fácil",JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    private static void resumo(EstoqueServico s){System.out.println("Produtos: "+s.dados().produtos().size()+" | Unidades: "+s.unidades()+" | Alertas: "+s.alertas()+" | Valor: "+Modelo.reais(s.valorTotal())+" | Movimentações: "+s.dados().movimentos().size());}
}
