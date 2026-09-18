# FinanceFlow

Aplicativo Android de finanças pessoais, em português, sem login ou servidor. Os lançamentos ficam em um banco Room no aparelho. Compatível com Android 7.0 ou superior.

## O que está pronto

- Cadastro, edição e exclusão com confirmação; retorno de sucesso somente após gravar no banco.
- Valor formatado automaticamente em reais: digite `100000` para obter `1.000,00`. Os dois últimos dígitos são os centavos. Valores devem ser positivos.
- Campo Nome, data selecionável, entradas/saídas e seletores de categoria, conta e origem/destino. Cadastre suas próprias contas e origens na aba Contas ou diretamente no formulário; os cadastros persistem mesmo sem lançamentos. Contas e origens já usadas são importadas na atualização.
- Menu inferior com cinco abas: Resumo, Lançamentos, Contas, Categorias e Backup. O resumo mostra saldo, entradas, saídas e últimos lançamentos do mês.
- Saldo acumulado por conta considerando todo o histórico.
- Busca por nome, categoria, conta e origem, ignorando acentos e maiúsculas; filtro por tipo e por data ou intervalo inclusivo. Use `dd/MM/aa` ou `dd/MM/aaaa`; anos de dois dígitos representam 2000–2099. Preencha só a data inicial para pesquisar um dia. Datas inexistentes e intervalos invertidos são rejeitados.
- CSV da lista filtrada, com separador `;` e UTF-8, para abrir em uma planilha.
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

Para trocar de aparelho: **Backup e restauração → Salvar backup completo** no antigo, copie o JSON e use **Restaurar backup** no novo. A restauração substitui os dados atuais, não mescla históricos. CSV é uma exportação para consulta, não um arquivo de restauração. Os backups exportados não são criptografados; escolha um local privado. O backup automático do Android também pode ocorrer conforme as configurações do sistema.

## Desenvolvimento e validação

Abra a raiz no Android Studio. O projeto usa Java 11 como nível de linguagem, JDK 21 para o Gradle, SDK Android 36.1 e o Gradle Wrapper incluído. Configure o SDK em `local.properties` (arquivo local, não versionado).

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Testes locais incluem parsing monetário, soma em centavos, CSV, ida e volta de backup, arquivo inválido, migrações do banco v1/v2, CRUD, rollback da restauração e fluxos de formulário/cadastro/edição/busca com Robolectric. Robolectric usa Android API 28 e não substitui a conferência visual em aparelho físico, especialmente no Android 15/16.

## Dados e manutenção

- O banco `financeFlow_db` está na versão 4. As migrações preservam o histórico e corrigem os campos trocados pelo formulário antigo quando o padrão conhecido é reconhecido.
- Backups JSON v2 incluem contas e origens ainda sem lançamentos. Backups v1 continuam aceitos, recuperando os cadastros a partir das movimentações. A restauração substitui tanto os lançamentos como os cadastros.
- Datas são gravadas como `yyyy-MM-dd` e exibidas como `dd/MM/yyyy`.
- O campo `valor` permanece `double` por compatibilidade com o banco existente; cálculos de saldo convertem cada valor em centavos inteiros.
- Os antigos campos `origem` e `syncStatus` permanecem por compatibilidade. Não existe sincronização com relógio, bancos ou nuvem própria.
- As telas são construídas em Java por `BaseActivity`; o item do histórico usa `item_lancamento.xml`.
- Dados demonstrativos não são inseridos no banco de produção.
