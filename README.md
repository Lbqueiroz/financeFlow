# FinanceFlow

Aplicativo Android de finanças pessoais, em português, sem login ou servidor. Os lançamentos ficam em um banco Room no aparelho. Compatível com Android 7.0 ou superior.

## O que está pronto

- Cadastro, edição e exclusão com confirmação; retorno de sucesso somente após gravar no banco. Após salvar, use Desfazer para remover um lançamento novo ou recuperar os dados anteriores de uma edição, antes de concluir.
- Valor formatado automaticamente em reais: digite `100000` para obter `1.000,00`. Os dois últimos dígitos são os centavos. Valores devem ser positivos.
- Campo Nome, data selecionável, entradas/saídas e seletores de categoria, conta e origem/destino. Cadastre suas próprias contas e origens na aba Contas ou diretamente no formulário; os cadastros persistem mesmo sem lançamentos. Contas e origens já usadas são importadas na atualização.
- Menu inferior com cinco abas: Resumo, Lançamentos, Contas, Categorias e Mais. Em Mais ficam as telas de Investimentos, Transferências, Recorrências, Orçamentos e Backup. O resumo mostra entradas, gastos e aplicações/resgates separados.
- Transferências entre contas e aplicações/resgates ajustam os saldos das contas sem virar receita ou despesa. Transferências podem ser desfeitas no próprio histórico.
- Investimentos com nome e instituição, aplicações, resgates, atualização manual do valor atual (inclusive zero), resultado acumulado e histórico. Resgates acima do valor disponível são rejeitados; excluir um movimento que deixaria um resgate sem cobertura também é rejeitado. Aportes líquidos são aplicações menos resgates; resultado é valor atual menos aportes líquidos, não uma taxa de rentabilidade.
- Recorrências mensais com vencimento, conta, categoria e confirmação manual a partir do vencimento. Só a confirmação cria um lançamento. O dia 31 se ajusta ao último dia de meses curtos e volta ao dia 31 nos seguintes. Confirmações repetidas não duplicam a parcela. Editar/excluir a recorrência preserva as parcelas já registradas.
- Orçamentos por categoria e mês, com barra de consumo, saldo disponível e destaque quando ultrapassados. O resumo indica recorrências vencidas e limites ultrapassados.
- Exclusão de contas e origens/destinos em Contas → Cadastrar contas e origens, com confirmação. Excluir um cadastro remove a opção para novos lançamentos, preservando os registros e saldos antigos. Backups v2 preservam essa exclusão.
- Saldo acumulado por conta considerando todo o histórico.
- Busca por nome, categoria, conta e origem, ignorando acentos e maiúsculas; filtro por tipo e por data ou intervalo inclusivo. Use `dd/MM/aa` ou `dd/MM/aaaa`; anos de dois dígitos representam 2000–2099. Preencha só a data inicial para pesquisar um dia. Datas inexistentes e intervalos invertidos são rejeitados.
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

- O banco `financeFlow_db` está na versão 6. As migrações preservam o histórico; v5 adiciona recibos do relógio e v6 adiciona o planejamento em centavos inteiros, separado da tabela de receitas/despesas.
- Backups JSON v3 incluem planejamento e cadastros. Backups v1/v2 continuam aceitos, mas não têm investimentos, transferências, recorrências ou orçamentos: restaurá-los limpa esses registros, após aviso e confirmação. A restauração de todas as tabelas ocorre em uma única transação.
- Datas são gravadas como `yyyy-MM-dd` e exibidas como `dd/MM/yyyy`.
- O campo `valor` permanece `double` por compatibilidade com o banco existente; cálculos de saldo convertem cada valor em centavos inteiros.
- O módulo Wear OS sincroniza saldo mensal e lançamentos rápidos com o celular. Veja [instalação e uso no Galaxy Watch 7](WATCH.md). Não existe integração com bancos ou nuvem própria.
- As telas são construídas em Java por `BaseActivity`; o item do histórico usa `item_lancamento.xml`.
- Dados demonstrativos não são inseridos no banco de produção.
- Firebase, login, perfil e saudação por nome permanecem para uma etapa posterior; nenhuma configuração ou dependência Firebase foi adicionada.
