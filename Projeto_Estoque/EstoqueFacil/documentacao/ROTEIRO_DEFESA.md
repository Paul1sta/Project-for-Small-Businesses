# Roteiro de apresentação do Estoque Fácil

Duração sugerida: cinco a sete minutos. Grave com suas palavras e demonstre o programa no seu computador. Os dados usados são simulados. Complete a identificação e adapte o texto ao trabalho efetivamente apresentado.

1. **Identificação e problema, aproximadamente 45 segundos.** Apresente o curso, o projeto e os autores. Explique o cenário simulado: um pequeno comércio precisa registrar produtos e movimentações e consultar o saldo para organizar a reposição. Não afirme que houve visita ou implantação em uma empresa.
2. **Objetivo e ferramentas, aproximadamente 45 segundos.** Explique que foi construída uma aplicação Java 17 com Swing, persistência local e nenhuma biblioteca externa. Diferencie o programa executável, as fontes e o relatório.
3. **Demonstração, aproximadamente dois minutos.** Execute o JAR. Cadastre `DEMO01`, `Produto para apresentação`, categoria `Teste`, custo `2,50` e mínimo `3`. Lance entrada de dez unidades. Lance saída de oito. Mostre saldo dois e alerta de reposição. Tente retirar três e explique o bloqueio. Mostre o histórico e a exportação CSV. Não reutilize o código se ele já existir; escolha outro.
4. **Estrutura do código, aproximadamente um minuto.** Mostre a separação entre `Modelo`, `EstoqueServico`, `RepositorioArquivo` e `EstoquePainel`. Explique que o saldo resulta do histórico, que o valor usa centavos inteiros e que a confirmação acontece após a gravação.
5. **Testes, aproximadamente 45 segundos.** Abra `resultados_testes.json` e apresente 33 aprovações. Destaque saída insuficiente, falha simulada de disco, reabertura de arquivo e ações em formulários. Explique o limite dos testes em modo sem janela.
6. **Conclusão, aproximadamente 45 segundos.** Explique o que foi implementado e cite limitações: operação individual, unidades inteiras, ausência de integração fiscal e ausência de validação em campo. Diferencie resultado técnico demonstrado de possível benefício operacional ainda não medido.

Se o professor exigir link de vídeo, envie conforme as regras do AVA. O manual antigo menciona YouTube no modo não listado. Antes de enviar, confira som, legibilidade da tela e acesso ao vídeo.
