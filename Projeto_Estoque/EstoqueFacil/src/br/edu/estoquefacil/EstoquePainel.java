package br.edu.estoquefacil;

import br.edu.estoquefacil.Modelo.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.imageio.ImageIO;

/** Interface Swing. A mesma árvore de componentes é usada na janela e nas capturas. */
public final class EstoquePainel extends JPanel {
    private static final Color AZUL = new Color(22,45,74), FUNDO = new Color(244,247,250), VERDE = new Color(19,104,87);
    private final EstoqueServico servico;
    final JTabbedPane abas = new JTabbedPane();
    final JTextField busca = campo(), codigo = campo(), nome = campo(), categoria = campo(), custo = campo(), minimo = campo();
    final JTextField quantidadeMov = campo(), motivoMov = campo();
    final JComboBox<Produto> produtoMov = new JComboBox<>();
    final JComboBox<String> tipoMov = new JComboBox<>(new String[]{"Entrada", "Saída"});
    final JCheckBox somenteBaixos = new JCheckBox("Mostrar somente itens para reposição");
    final JButton salvarProduto = botao("Cadastrar produto"), novoProduto = botao("Limpar formulário"), registrarMov = botao("Registrar movimentação");
    final JLabel mensagem = new JLabel("Pronto. Cadastre produtos ou carregue os dados de exemplo na aba Backup e ajuda.");
    private final JLabel[] indicadores = new JLabel[4];
    private final JLabel saldoMov = new JLabel("Selecione um produto cadastrado.");
    private final DefaultTableModel estoqueModelo = modelo(new String[]{"Código","Produto","Categoria","Saldo","Mínimo","Custo un.","Situação"});
    private final DefaultTableModel historicoModelo = modelo(new String[]{"ID","Data e hora local","Produto na data","Tipo","Qtd.","Motivo"});
    final JTable estoqueTabela = tabela(estoqueModelo), historicoTabela = tabela(historicoModelo);
    private boolean editando;

    public EstoquePainel(EstoqueServico servico, String arquivo) {
        this.servico = servico;
        setLayout(new BorderLayout(0,12)); setBackground(FUNDO); setBorder(new EmptyBorder(0,0,12,0));
        JPanel cabecalho = new JPanel(new BorderLayout()); cabecalho.setBackground(AZUL); cabecalho.setBorder(new EmptyBorder(20,26,18,26));
        JLabel titulo = new JLabel("Estoque Fácil"); titulo.setFont(new Font("SansSerif",Font.BOLD,27)); titulo.setForeground(Color.WHITE);
        JLabel subtitulo = new JLabel("Controle de produtos e movimentações  |  Operação local individual");
        subtitulo.setForeground(new Color(213,225,239)); subtitulo.setBorder(new EmptyBorder(7,0,0,0));
        cabecalho.add(titulo,BorderLayout.NORTH); cabecalho.add(subtitulo,BorderLayout.SOUTH);
        JPanel superior = new JPanel(new BorderLayout(0,14)); superior.setOpaque(false); superior.add(cabecalho,BorderLayout.NORTH);
        JPanel cards = new JPanel(new GridLayout(1,4,14,0)); cards.setOpaque(false); cards.setBorder(new EmptyBorder(0,24,0,24));
        String[] rotulos = {"PRODUTOS CADASTRADOS","UNIDADES EM ESTOQUE","VALOR AO CUSTO ATUAL","ITENS PARA REPOSIÇÃO"};
        for (int i=0;i<4;i++) {
            JPanel card = new JPanel(new BorderLayout(0,8)); card.setBackground(Color.WHITE);
            card.setBorder(new CompoundBorder(new LineBorder(new Color(220,227,235)),new EmptyBorder(14,16,14,16)));
            JLabel legenda = new JLabel(rotulos[i]); legenda.setFont(new Font("SansSerif",Font.PLAIN,11)); legenda.setForeground(new Color(87,102,120));
            indicadores[i] = new JLabel("0"); indicadores[i].setFont(new Font("SansSerif",Font.BOLD,25)); indicadores[i].setForeground(AZUL);
            card.add(legenda,BorderLayout.NORTH); card.add(indicadores[i],BorderLayout.CENTER); cards.add(card);
        }
        superior.add(cards,BorderLayout.CENTER); add(superior,BorderLayout.NORTH);
        abas.addTab("Estoque", estoque()); abas.addTab("Cadastro", cadastro()); abas.addTab("Movimentações", movimentacao());
        abas.addTab("Histórico", historico()); abas.addTab("Backup e ajuda", ajuda(arquivo));
        abas.setBorder(new EmptyBorder(0,24,0,24)); abas.setFont(new Font("SansSerif",Font.BOLD,14)); add(abas,BorderLayout.CENTER);
        mensagem.setBorder(new EmptyBorder(8,26,0,20)); mensagem.setFont(new Font("SansSerif",Font.PLAIN,13)); add(mensagem,BorderLayout.SOUTH);
        busca.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){atualizarEstoque();} public void removeUpdate(DocumentEvent e){atualizarEstoque();} public void changedUpdate(DocumentEvent e){atualizarEstoque();}});
        somenteBaixos.addActionListener(e->atualizarEstoque());
        produtoMov.addActionListener(e->atualizarSaldo());
        salvarProduto.addActionListener(e->acao(()->{
            Produto p = new Produto(codigo.getText(),nome.getText(),categoria.getText(),Modelo.centavos(custo.getText()),inteiro(minimo.getText(),"Estoque mínimo"));
            if(editando)servico.editar(p); else servico.cadastrar(p);
            limparCadastro(); atualizar(); sucesso("Produto salvo. O saldo é alterado pela aba Movimentações.");
        }));
        novoProduto.addActionListener(e->limparCadastro());
        registrarMov.addActionListener(e->acao(()->{
            Produto p=(Produto)produtoMov.getSelectedItem(); if(p==null)throw new IllegalArgumentException("Cadastre ou selecione um produto.");
            servico.movimentar(p.codigo(),tipoMov.getSelectedIndex()==0?Tipo.ENTRADA:Tipo.SAIDA,inteiro(quantidadeMov.getText(),"Quantidade"),motivoMov.getText());
            quantidadeMov.setText(""); motivoMov.setText(""); atualizar(); sucesso("Movimentação registrada e salva.");
        }));
        estoqueTabela.getSelectionModel().addListSelectionListener(e->{ if(!e.getValueIsAdjusting()) atualizarSaldo(); });
        limparCadastro(); atualizar();
    }
    private JPanel estoque() {
        JPanel p=painel();
        JPanel ferramentas=new JPanel(new BorderLayout(12,0)); ferramentas.setOpaque(false);
        ferramentas.add(new JLabel("Buscar código, produto ou categoria"),BorderLayout.WEST); ferramentas.add(busca,BorderLayout.CENTER);
        JPanel linha=new JPanel(new BorderLayout(0,10)); linha.setOpaque(false); linha.add(ferramentas,BorderLayout.NORTH);
        somenteBaixos.setOpaque(false); linha.add(somenteBaixos,BorderLayout.SOUTH); p.add(linha,BorderLayout.NORTH);
        estoqueTabela.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean selected,boolean focus,int row,int col){
                Component c=super.getTableCellRendererComponent(t,v,selected,focus,row,col);
                if(!selected){int m=t.convertRowIndexToModel(row); boolean baixo="REPOR".equals(estoqueModelo.getValueAt(m,6)); c.setBackground(baixo?new Color(255,245,223):(row%2==0?Color.WHITE:new Color(248,250,252)));c.setForeground(baixo?new Color(133,79,12):AZUL);}
                return c;
            }
        });
        JScrollPane scroll=new JScrollPane(estoqueTabela); scroll.setColumnHeaderView(estoqueTabela.getTableHeader()); scroll.setBorder(new LineBorder(new Color(216,225,235)));p.add(scroll,BorderLayout.CENTER);
        estoqueTabela.getColumnModel().getColumn(1).setPreferredWidth(260);
        JPanel botoes=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));botoes.setOpaque(false);
        JButton editar=botao("Editar selecionado"), mover=botao("Movimentar selecionado"), exportar=botao("Exportar estoque CSV");
        editar.addActionListener(e->acao(()->{Produto pr=selecionado();preencherCadastro(pr);abas.setSelectedIndex(1);}));
        mover.addActionListener(e->acao(()->{Produto pr=selecionado();produtoMov.setSelectedItem(pr);atualizarSaldo();abas.setSelectedIndex(2);}));
        exportar.addActionListener(e->escolher("estoque.csv",false,servico::exportarEstoque));
        botoes.add(editar);botoes.add(mover);botoes.add(exportar);p.add(botoes,BorderLayout.SOUTH);return p;
    }
    private JPanel cadastro(){
        JPanel p=painel();p.add(titulo("Cadastro e edição de produtos","Cadastre a identificação e o custo. Registre o saldo inicial como uma entrada."),BorderLayout.NORTH);
        JPanel f=formulario(); adicionar(f,0,"Código único",codigo);adicionar(f,1,"Nome do produto",nome);adicionar(f,2,"Categoria",categoria);
        adicionar(f,3,"Custo unitário em reais",custo);adicionar(f,4,"Estoque mínimo em unidades",minimo);
        JPanel b=new JPanel(new FlowLayout(FlowLayout.LEFT,12,0));b.setOpaque(false);b.add(salvarProduto);b.add(novoProduto);
        adicionar(f,5,"",b);p.add(f,BorderLayout.CENTER);return p;
    }
    private JPanel movimentacao(){
        JPanel p=painel();p.add(titulo("Entrada e saída de unidades","Toda movimentação exige uma quantidade inteira e um motivo."),BorderLayout.NORTH);
        JPanel f=formulario(); adicionar(f,0,"Produto",produtoMov);adicionar(f,1,"Saldo disponível",saldoMov);adicionar(f,2,"Tipo de movimentação",tipoMov);
        adicionar(f,3,"Quantidade em unidades",quantidadeMov);adicionar(f,4,"Motivo ou documento",motivoMov);adicionar(f,5,"",registrarMov);
        p.add(f,BorderLayout.CENTER);return p;
    }
    private JPanel historico(){
        JPanel p=painel();p.add(titulo("Histórico de movimentações","Registros em ordem inversa de lançamento. O CSV usa data e hora em UTC."),BorderLayout.NORTH);
        JScrollPane scroll=new JScrollPane(historicoTabela);scroll.setColumnHeaderView(historicoTabela.getTableHeader());p.add(scroll,BorderLayout.CENTER);
        int[] larguras={45,170,240,65,50,340};for(int i=0;i<larguras.length;i++)historicoTabela.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
        JButton exportar=botao("Exportar histórico CSV");exportar.addActionListener(e->escolher("historico.csv",false,servico::exportarHistorico));
        JPanel b=new JPanel(new FlowLayout(FlowLayout.LEFT));b.setOpaque(false);b.add(exportar);p.add(b,BorderLayout.SOUTH);return p;
    }
    private JPanel ajuda(String arquivo){
        JPanel p=painel();p.add(titulo("Backup e primeiros passos","Salve uma cópia com nome novo em outra pasta ou dispositivo."),BorderLayout.NORTH);
        JTextArea texto=new JTextArea("COMO USAR\n\n1. Cadastre o produto e defina o estoque mínimo.\n2. Lance uma entrada com a quantidade inicial.\n3. Registre entradas e saídas com um motivo claro.\n4. Confira os alertas e exporte os relatórios.\n\nO custo informa o valor do estoque ao custo unitário atual.\nO programa não calcula lucro, faturamento ou custo médio.\nAs quantidades representam unidades inteiras, inclusive embalagens.\n\nRESTAURAÇÃO\nSó é permitida em uma base vazia. Para preservar uma base existente,\nabra o programa com --dados apontando para outra pasta.\nO arquivo de backup precisa ter sido gerado pelo Estoque Fácil.\n\nBASE ATUAL\n"+arquivo);
        texto.setEditable(false);texto.setFont(new Font("SansSerif",Font.PLAIN,14));texto.setBackground(Color.WHITE);texto.setBorder(new EmptyBorder(14,16,14,16));p.add(new JScrollPane(texto),BorderLayout.CENTER);
        JPanel b=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));b.setOpaque(false);JButton backup=botao("Criar backup"),restaurar=botao("Restaurar em base vazia"),demo=botao("Carregar exemplo simulado");
        backup.addActionListener(e->escolher("backup-estoque.efdb",false,servico::backup));
        restaurar.addActionListener(e->escolher("backup-estoque.efdb",true,path->{servico.restaurar(path);atualizar();}));
        demo.addActionListener(e->acao(()->{servico.demonstracao();atualizar();sucesso("Exemplo simulado carregado: 6 produtos e 12 movimentações.");}));
        b.add(backup);b.add(restaurar);b.add(demo);p.add(b,BorderLayout.SOUTH);return p;
    }
    private Produto selecionado(){
        int row=estoqueTabela.getSelectedRow();if(row<0)throw new IllegalArgumentException("Selecione um produto na tabela.");
        return servico.produto(estoqueModelo.getValueAt(estoqueTabela.convertRowIndexToModel(row),0).toString());
    }
    private void preencherCadastro(Produto p){editando=true;codigo.setText(p.codigo());codigo.setEnabled(false);nome.setText(p.nome());categoria.setText(p.categoria());custo.setText(Modelo.decimal(p.custoCentavos()));minimo.setText(Integer.toString(p.minimo()));salvarProduto.setText("Salvar alterações");}
    private void limparCadastro(){editando=false;codigo.setEnabled(true);codigo.setText("");nome.setText("");categoria.setText("");custo.setText("");minimo.setText("0");salvarProduto.setText("Cadastrar produto");}
    private void atualizarEstoque(){estoqueModelo.setRowCount(0);for(Linha l:servico.listar(busca.getText(),somenteBaixos.isSelected())){Produto pr=l.produto();estoqueModelo.addRow(new Object[]{pr.codigo(),pr.nome(),pr.categoria(),l.saldo(),pr.minimo(),Modelo.reais(pr.custoCentavos()),l.baixo()?"REPOR":"REGULAR"});}}
    void atualizar(){
        indicadores[0].setText(Integer.toString(servico.dados().produtos().size()));indicadores[1].setText(Long.toString(servico.unidades()));indicadores[2].setText(Modelo.reais(servico.valorTotal()));indicadores[3].setText(Long.toString(servico.alertas()));atualizarEstoque();
        Produto anterior=(Produto)produtoMov.getSelectedItem();produtoMov.removeAllItems();for(Linha l:servico.listar("",false))produtoMov.addItem(l.produto());
        if(anterior!=null){for(int i=0;i<produtoMov.getItemCount();i++)if(produtoMov.getItemAt(i).codigo().equals(anterior.codigo()))produtoMov.setSelectedIndex(i);}atualizarSaldo();
        historicoModelo.setRowCount(0);java.util.List<Movimento> ms=servico.dados().movimentos();DateTimeFormatter fmt=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
        for(int i=ms.size()-1;i>=0;i--){Movimento m=ms.get(i);historicoModelo.addRow(new Object[]{m.id(),fmt.format(m.instante()),m.produto()+" - "+m.nomeNaData(),m.tipo()==Tipo.ENTRADA?"Entrada":"Saída",m.quantidade(),m.motivo()});}
    }
    private void atualizarSaldo(){Produto p=(Produto)produtoMov.getSelectedItem();saldoMov.setText(p==null?"Nenhum produto cadastrado.":servico.saldo(p.codigo())+" unidade(s)");}
    private void sucesso(String texto){mensagem.setForeground(VERDE);mensagem.setText(texto);}
    private void acao(Operacao r){try{r.executar();}catch(Exception ex){mensagem.setForeground(new Color(165,37,34));mensagem.setText("Não concluído: "+Objects.requireNonNullElse(ex.getMessage(),ex.getClass().getSimpleName()));}}
    private void escolher(String nome,boolean abrir,ArquivoAcao acao){
        JFileChooser f=new JFileChooser();f.setSelectedFile(new java.io.File(nome));
        int r=abrir?f.showOpenDialog(this):f.showSaveDialog(this);if(r!=JFileChooser.APPROVE_OPTION)return;
        acao(()->{acao.executar(f.getSelectedFile().toPath());sucesso(abrir?"Backup restaurado.":"Arquivo criado. Guarde a cópia em local seguro.");});
    }
    private static int inteiro(String s,String nome){try{return Integer.parseInt(s.strip());}catch(NumberFormatException e){throw new IllegalArgumentException(nome+": informe um número inteiro válido.");}}
    private static JTextField campo(){JTextField f=new JTextField();f.setFont(new Font("SansSerif",Font.PLAIN,15));f.setMargin(new Insets(6,8,6,8));f.setPreferredSize(new Dimension(350,36));return f;}
    private static JButton botao(String s){JButton b=new JButton(s);b.setFont(new Font("SansSerif",Font.BOLD,13));b.setMargin(new Insets(8,13,8,13));b.setFocusPainted(false);return b;}
    private static JPanel painel(){JPanel p=new JPanel(new BorderLayout(0,16));p.setBackground(Color.WHITE);p.setBorder(new EmptyBorder(20,20,20,20));return p;}
    private static JPanel titulo(String a,String b){JPanel p=new JPanel(new GridLayout(2,1,0,7));p.setOpaque(false);JLabel t=new JLabel(a);t.setFont(new Font("SansSerif",Font.BOLD,19));t.setForeground(AZUL);p.add(t);JLabel s=new JLabel(b);s.setForeground(new Color(91,105,120));p.add(s);return p;}
    private static JPanel formulario(){JPanel f=new JPanel(new GridBagLayout());f.setOpaque(false);return f;}
    private static void adicionar(JPanel p,int y,String texto,Component c){
        GridBagConstraints g=new GridBagConstraints();g.gridx=0;g.gridy=y;g.anchor=GridBagConstraints.WEST;g.insets=new Insets(7,0,7,20);JLabel l=new JLabel(texto);l.setFont(new Font("SansSerif",Font.PLAIN,14));p.add(l,g);
        g.gridx=1;g.weightx=1;g.fill=GridBagConstraints.HORIZONTAL;g.insets=new Insets(7,0,7,80);p.add(c,g);
        if(y==5){g.gridx=0;g.gridy=6;g.weighty=1;p.add(Box.createVerticalGlue(),g);}
    }
    private static DefaultTableModel modelo(String[] cs){return new DefaultTableModel(cs,0){@Override public boolean isCellEditable(int r,int c){return false;}@Override public Class<?> getColumnClass(int c){return getRowCount()>0&&getValueAt(0,c)!=null?getValueAt(0,c).getClass():Object.class;}};}
    private static JTable tabela(DefaultTableModel m){JTable t=new JTable(m);t.setFont(new Font("SansSerif",Font.PLAIN,13));t.setRowHeight(36);t.setShowGrid(false);t.setIntercellSpacing(new Dimension(12,1));t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);t.setAutoCreateRowSorter(true);t.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,13));t.getTableHeader().setPreferredSize(new Dimension(100,38));t.setFillsViewportHeight(true);return t;}
    public void capturar(Path destino,int aba) throws IOException {
        abas.setSelectedIndex(aba);setSize(1240,800);dispor(this);
        BufferedImage img=new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=img.createGraphics();g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(Color.WHITE);g.fillRect(0,0,getWidth(),getHeight());printAll(g);g.dispose();ImageIO.write(img,"png",destino.toFile());
    }
    private static void dispor(Container c){c.doLayout();for(Component f:c.getComponents())if(f instanceof Container cc)dispor(cc);}
    @FunctionalInterface private interface Operacao{void executar()throws Exception;}
    @FunctionalInterface private interface ArquivoAcao{void executar(Path p)throws Exception;}
}
