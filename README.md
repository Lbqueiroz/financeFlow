# FinanceFlow

Aplicativo Android de finanças pessoais, em português, sem login ou servidor. Os lançamentos ficam em um banco Room no aparelho. Compatível com Android 7.0 ou superior.

## O que está pronto

- Cadastro, edição e exclusão com confirmação; retorno de sucesso somente após gravar no banco. Após salvar, use Desfazer para remover um lançamento novo ou recuperar os dados anteriores de uma edição, antes de concluir.
- Valor formatado automaticamente em reais: digite `100000` para obter `1.000,00`. Os dois últimos dígitos são os centavos. Valores devem ser positivos.
- Campo Nome, data selecionável, entradas/saídas e seletores de categoria, conta e origem/destino. Cadastre suas próprias contas e origens na aba Contas ou diretamente no formulário; os cadastros persistem mesmo sem lançamentos. Contas e origens já usadas são importadas na atualização.
- Menu inferior com cinco abas: Resumo, Lançamentos, Contas, Categorias e Mais. Em Mais ficam as telas de Investimentos, Transferências, Parcelamentos, Recorrências, Orçamentos e Backup. O resumo mostra entradas, gastos e aplicações/resgates separados.
- Parcelamentos em uma tela própria: nome da compra, conta/cartão, categoria, valor total final, quantidade (2 a 120) e data da primeira parcela. A prévia mostra os valores e o período. A confirmação grava a compra e todas as saídas mensais de uma vez, com nomes “Compra · 1/3”, “2/3” etc. A última parcela recebe os centavos restantes; a soma sempre corresponde ao total. Datas no fim do mês se ajustam a fevereiro sem perder o dia original nos meses seguintes.
- As parcelas aparecem no histórico da conta, nos filtros e no PDF, e impactam cada mês correspondente (inclusive meses futuros). A tela Parcelamentos permite abrir/editar cada lançamento e excluir a compra com todas as suas parcelas; excluir uma parcela no histórico não apaga as demais. A data da primeira parcela é informada pelo usuário: não há cálculo automático de fechamento/fatura do cartão. Assinaturas continuam separadas em Recorrências, com confirmação mensal.
- Transferências entre contas e aplicações/resgates ajustam os saldos das contas sem virar receita ou despesa. Transferências podem ser desfeitas no próprio histórico.
- Investimentos com nome e instituição, aplicações, resgates, atualização manual do valor atual (inclusive zero), resultado acumulado e histórico. Resgates acima do valor disponível são rejeitados; excluir um movimento que deixaria um resgate sem cobertura também é rejeitado. Aportes líquidos são aplicações menos resgates; resultado é valor atual menos aportes líquidos, não uma taxa de rentabilidade.
- Recorrências mensais com vencimento, conta, categoria e confirmação manual a partir do vencimento. Só a confirmação cria um lançamento. O dia 31 se ajusta ao último dia de meses curtos e volta ao dia 31 nos seguintes. Confirmações repetidas não duplicam a parcela. Editar/excluir a recorrência preserva as parcelas já registradas.
- Orçamentos por categoria e mês, com barra de consumo, saldo disponível e destaque quando ultrapassados. O resumo indica recorrências vencidas e limites ultrapassados.
- Exclusão de contas e origens/destinos em Contas → Cadastrar contas e origens, com confirmação. Excluir um cadastro remove a opção para novos lançamentos, preservando os registros e saldos antigos. Backups v2 preservam essa exclusão.
- Saldo acumulado por conta considerando todo o histórico.
- Toque em uma conta para abrir seu histórico de entradas/saídas, com busca, filtros de data/tipo e PDF restritos à conta. Novo lançamento já preenche a conta e começa como Saída (pode mudar para Entrada). Após salvar, “Outro nesta conta” permite cadastrar várias compras em sequência. O resultado desse histórico inclui apenas receitas/despesas; o saldo acumulado na aba Contas também considera transferências e investimentos.
- A tela geral de Lançamentos também tem o seletor “Todas as contas”. Escolha uma conta para combinar esse filtro com tipo, busca e datas; o PDF respeita a seleção e novos lançamentos recebem a conta filtrada. Contas antigas com histórico continuam disponíveis no filtro mesmo após excluir o cadastro.
- Busca por nome, categoria, conta e origem, ignorando acentos e maiúsculas; filtro por conta, tipo e por data ou intervalo inclusivo. As datas são escolhidas em calendários: escolha apenas a inicial para um dia ou também a final para um período. Intervalos invertidos são rejeitados. Ao abrir o teclado na busca, filtros e ações secundárias se recolhem para preservar a área de resultados; ao fechar, reaparecem mantendo a seleção.
- PDF A4 da lista filtrada, com logo, filtros aplicados, totais de entradas/saídas/saldo, detalhes completos e paginação automática.
- Backup completo em JSON e restauração validada, com confirmação antes de substituir o histórico. A operação é atômica: uma falha mantém os dados anteriores.
- Tema sempre claro, cartões arredondados, formulários roláveis e ajuste às barras do Android e teclado.
- Preservação do formulário na recriação da tela e proteção contra repetição de uma gravação durante rotação.

## Usar no celular

Compile e instale pelo Android Studio, ou gere o APK de desenvolvimento:

```powershell
.\gradlew.bat assembleDebug
```

O arquivo fica em `app/build/outputs/apk/debug/app-debug.apk`. Transfira-o para o aparelho e abra-o para instalar. Para atualizar uma instalação anterior, use a mesma assinatura; faça um backup antes de desinstalar o app.

No primeiro uso, cadastre uma conta na aba Contas (ou no formulário) e depois suas entradas e despesas. Para representar um saldo que já existia, crie uma entrada com nome “Saldo inicial”, categoria “Outros” e a conta correspondente. O painel mensal considera a data de cada lançamento, inclusive datas futuras; não há conciliação bancária nem distinção entre previsto e pago. Contas de cartão são etiquetas de agrupamento, sem cálculo automático de fatura ou parcelas.

Para trocar de aparelho: **Mais → Backup e restauração → Salvar backup completo** no antigo, copie o JSON e use **Restaurar backup** no novo. A restauração substitui os dados atuais, não mescla históricos. PDF é um relatório dos lançamentos de receitas/despesas para consulta ou compartilhamento; transferências e investimentos têm seus próprios históricos e são incluídos no backup JSON. Os backups exportados não são criptografados; escolha um local privado. O backup automático do Android também pode ocorrer conforme as configurações do sistema.

## Desenvolvimento e validação

Abra a raiz no Android Studio. O projeto usa Java 11 como nível de linguagem, JDK 21 para o Gradle, SDK Android 36.1 e o Gradle Wrapper incluído. Configure o SDK em `local.properties` (arquivo local, não versionado).

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Testes locais incluem parsing monetário, soma em centavos, paginação e renderização do relatório PDF, ida e volta de backup, arquivo inválido, migrações do banco v1/v2, CRUD, rollback da restauração e fluxos de formulário/cadastro/edição/busca com Robolectric. Robolectric usa Android API 28 e não substitui a conferência em aparelho físico, especialmente no Android 15/16. O layout do relatório é validado pelo mesmo renderizador Canvas do app; a gravação nativa via PdfDocument deve ser conferida no celular, pois não é implementada pelo ambiente de testes.

## Dados e manutenção

- O banco `financeFlow_db` está na versão 7. As migrações preservam o histórico; v5 adiciona recibos do relógio, v6 adiciona planejamento e v7 adiciona compras parceladas e seus vínculos com os lançamentos.
- Backups JSON v4 incluem planejamento, cadastros e compras parceladas com os vínculos das parcelas. Backups v1/v2/v3 continuam aceitos: dados ausentes no arquivo substituem os atuais por uma lista vazia, após aviso e confirmação. A restauração de todas as tabelas ocorre em uma única transação. Vínculos inválidos ou parcelas duplicadas são rejeitados antes de alterar o banco.
- Datas são gravadas como `yyyy-MM-dd` e exibidas como `dd/MM/yyyy`.
- O campo `valor` permanece `double` por compatibilidade com o banco existente; cálculos de saldo convertem cada valor em centavos inteiros.
- O módulo Wear OS sincroniza saldo mensal e lançamentos rápidos com o celular. Veja [instalação e uso no Galaxy Watch 7](WATCH.md). Não existe integração com bancos ou nuvem própria.
- As telas são construídas em Java por `BaseActivity`; o item do histórico usa `item_lancamento.xml`.
- Dados demonstrativos não são inseridos no banco de produção.
- Firebase, login, perfil e saudação por nome permanecem para uma etapa posterior; nenhuma configuração ou dependência Firebase foi adicionada.
