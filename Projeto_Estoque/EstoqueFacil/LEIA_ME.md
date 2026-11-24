# Estoque Fácil

Projeto Integrador de Laboratório de Programação. Aplicação de desktop em Java 17, sem bibliotecas externas, para cadastro de produtos, controle de entradas e saídas, consulta de saldo, alertas de reposição, exportação CSV e backup.

O cenário de pequeno comércio e os dados de exemplo são simulados. O projeto não afirma ter sido implantado em uma empresa real. O relatório apresenta resultados de testes de software efetivamente executados.

## Executar no Windows

1. Extraia o ZIP inteiro para uma pasta do computador. Não execute dentro do ZIP.
2. Confira no Prompt de Comando: `java -version`. É necessário Java 17 ou superior; o JDK 17 também permite compilar as fontes.
3. Se necessário, obtenha o Java na distribuição oficial escolhida, por exemplo https://adoptium.net/temurin/releases/?version=17 . Selecione Windows, a arquitetura correta e JDK 17. Habilite a opção de configurar o PATH no instalador, se oferecida.
4. Abra `executar.bat`. Alternativa pelo terminal, dentro desta pasta: `java -jar EstoqueFacil.jar`.
5. Para experimentar sem preencher tudo, abra a aba **Backup e ajuda** e pressione **Carregar exemplo simulado**. Esse botão só funciona em uma base vazia.

O programa inicia com uma base vazia. Os seis produtos de demonstração não são carregados automaticamente na base de uso. Os arquivos de exemplo prontos estão em `exemplos/`.

## Linux e macOS

Com Java 17 ou superior e ambiente gráfico, execute `sh executar.sh` ou `java -jar EstoqueFacil.jar` nesta pasta. A distribuição foi compilada e testada em Linux com OpenJDK 17.0.20. A instalação e a janela em Windows/macOS não foram testadas neste ambiente.

## Primeiro uso

1. Na aba **Cadastro**, informe código único, nome, categoria, custo unitário e estoque mínimo. O custo aceita `12,50` ou `12.50`, sem separador de milhar.
2. Cadastre o produto. Seu saldo começa em zero.
3. Na aba **Movimentações**, selecione produto e **Entrada**, informe quantidade e motivo. Lance a quantidade inicial por essa tela.
4. Para uma retirada, escolha **Saída**. Quantidades acima do saldo são recusadas sem mudar o histórico.
5. Na aba **Estoque**, pesquise por código, nome ou categoria. A pesquisa ignora caixa e acentuação. O filtro de reposição inclui saldo igual ou menor que o mínimo.
6. Para editar, selecione o produto na tabela e pressione **Editar selecionado**. O código permanece fixo. A edição não muda o saldo nem apaga movimentos antigos.
7. Exporte o estoque ou o histórico usando os respectivos botões. Informe um nome de arquivo novo; arquivos existentes não são sobrescritos.

Quantidades são unidades inteiras. “Arroz 5 kg” representa uma embalagem como uma unidade; não existe venda fracionada por peso. O valor total é a soma de saldo vezes custo unitário cadastrado atualmente. Não representa lucro, receita, custo médio ou CMV.

## Dados e backup

A base padrão é `dados/estoque.efdb`, relativa à pasta de execução. Os scripts fornecidos entram automaticamente na pasta do projeto. O arquivo `.lock` pode continuar existindo após fechar; ele não contém os produtos e sua existência, sozinha, não mantém a base bloqueada.

Use **Criar backup** e escolha um arquivo novo, preferencialmente em outra pasta ou dispositivo. O formato é binário próprio, com cabeçalho, versão e CRC32 para detectar corrupção acidental; não há criptografia ou autenticação.

Para restaurar sem apagar uma base existente:

```text
java -jar EstoqueFacil.jar --dados restaurado/estoque.efdb
```

Na nova janela, abra **Backup e ajuda** e escolha **Restaurar em base vazia**. Selecione o arquivo `.efdb` de backup. A restauração em uma base preenchida é recusada. Feche a outra janela antes de abrir novamente a mesma base. Não use uma pasta de rede ou sincronização simultânea para a base ativa.

## Compilar e testar

É necessário JDK 17 ou superior, incluindo o módulo `jdk.compiler`. No Windows, abra `compilar.bat`. Em Linux/macOS, execute `sh compilar.sh`. Os scripts recompilam o JAR e executam a suíte, sem Maven, Gradle, JUnit ou downloads.

Os resultados ficam em `evidencias/resultados_testes.json`. O registro da execução usada no relatório está em `evidencias/execucao_testes.txt`: 33 casos aprovados e nenhuma falha. Quatro casos acionam componentes Swing em modo sem janela; não substituem uma avaliação com usuários ou a verificação visual no seu computador.

Para abrir em uma IDE, configure `src/` como pasta de fontes, Java 17 como nível de linguagem e `br.edu.estoquefacil.Main` como classe principal. As fontes de teste estão em `testes/`. Nenhuma biblioteca adicional é necessária.

## Reprodução da demonstração

Crie uma pasta nova para cada execução. Os arquivos de exportação existentes são preservados.

```text
java -jar EstoqueFacil.jar --demo nova-demonstracao
java -jar EstoqueFacil.jar --dados nova-demonstracao/estoque.efdb --verificar
java -Duser.timezone=America/Sao_Paulo -jar EstoqueFacil.jar --capturar novas-capturas
```

O exemplo produz seis produtos, doze movimentos, 65 unidades, três alertas e valor de R$ 614,60 ao custo atual. Os horários do histórico são exibidos no fuso do computador; o CSV mantém UTC. As capturas são renderizações dos próprios componentes Swing com dados simulados, não desenhos de telas futuras.

## Estrutura das fontes

| Arquivo | Responsabilidade |
|---|---|
| `Modelo.java` | Entidades, conversão monetária e validação do histórico. |
| `Repositorio.java` | Contrato de leitura e gravação. |
| `RepositorioArquivo.java` | Persistência, integridade, bloqueio e backup. |
| `EstoqueServico.java` | Cadastro, edição, movimentações, consultas e relatórios. |
| `EstoquePainel.java` | Formulários, tabelas e mensagens da interface. |
| `Main.java` | Inicialização, modos de demonstração e verificação. |
| `Testes.java` | Casos reproduzíveis de regras, arquivos, exportação e interface. |

## Antes de entregar à faculdade

- Confirme com o orientador o tema e a plataforma Java.
- Complete o nome do orientador, campus/polo e demais integrantes, se houver, na capa e folha de rosto do Word. A identificação já informada de Vinnicius foi incluída; revise-a.
- Publique as fontes no seu repositório ou pasta compartilhada autorizada e coloque o link na capa. Um endereço de repositório não foi inventado nem publicado em seu nome.
- Verifique se o professor consegue abrir esse link. Não inclua dados reais da pasta `dados/` no repositório.
- Confira no AVA o prazo, os formatos aceitos e a exigência de vídeo. O manual de 2023 cita vídeo; o de 2024 não repete a exigência. Há também divergência de extensão dentro do modelo: 15 a 25 páginas contra uma menção a 12. Este relatório segue a faixa de 15 a 25 páginas indicada na seção específica.
- Se houver exigência de vídeo, use `documentacao/ROTEIRO_DEFESA.md` para gravar sua própria apresentação. O roteiro não é uma gravação.
- Se alterar o Word, atualize a paginação e o sumário antes de exportar novamente para PDF.

## Limites da versão

Uso individual em um computador, até 10.000 produtos e 100.000 movimentos por limite de validação. Não foi realizado ensaio de carga nesses limites. Cada alteração grava um retrato completo da base; portanto, eles não são promessa de desempenho. A substituição atômica do arquivo depende do sistema de arquivos. Quando não é suportada, a gravação é recusada. O bloqueio protege contra outras instâncias cooperantes do programa, não contra edição externa maliciosa. Não há login, vendas, emissão fiscal, pagamento, nuvem ou sincronização.
