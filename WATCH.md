# FinanceFlow no Galaxy Watch 7

O módulo `wear` é o aplicativo complementar Wear OS. Mostra o saldo do mês (entradas menos saídas) e permite registrar entrada ou saída com valor, conta e categoria. O celular mantém o histórico completo. O relógio precisa estar pareado com um celular Android com Google Play Services e com a nova versão do FinanceFlow instalada.

## Instalação pessoal pelo Windows

1. Atualize o celular com `app/build/outputs/apk/debug/app-debug.apk`.
2. No relógio, habilite as opções do desenvolvedor e a depuração sem fio. Deixe computador e relógio na mesma rede Wi-Fi.
3. Em **Depuração sem fio → Parear novo dispositivo**, veja o endereço, a porta de pareamento e o código. No PowerShell, execute os comandos abaixo substituindo os valores de exemplo. A porta de conexão aparece na tela principal de depuração sem fio e é diferente da porta de pareamento.

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb pair IP_DO_RELOGIO:PORTA_DE_PAREAMENTO
& $adb connect IP_DO_RELOGIO:PORTA_DE_CONEXAO
& $adb -s IP_DO_RELOGIO:PORTA_DE_CONEXAO install -r .\wear\build\outputs\apk\debug\wear-debug.apk
```

4. Abra FinanceFlow no celular e no relógio. Cadastre ao menos uma conta no celular. No relógio, toque em **Atualizar**.
5. Depois da instalação, a depuração pode ser desligada. A comunicação normal usa o Wear OS Data Layer. Não precisa publicar na Play Store para essa instalação de teste.

Referência oficial: [depuração Wear OS por Wi-Fi](https://developer.android.com/training/wearables/get-started/debug-wifi).

Os dois APKs devem ter o mesmo applicationId e certificado de assinatura. Os builds debug deste projeto usam a mesma chave local. Para atualizar instalações anteriores, mantenha essa assinatura.

## Uso e verificação no aparelho

- Confira o saldo e a hora da última atualização. Um saldo de outro mês é identificado como antigo.
- Toque em Saída ou Entrada, digite os centavos (12345 vira 123,45), escolha conta/categoria, revise e salve. A data é o dia local do relógio; o nome é “Gasto pelo relógio” ou “Entrada pelo relógio”. Edite detalhes depois no celular.
- O lançamento fica salvo no relógio até o celular confirmar. O saldo exibido só muda após a confirmação. Não faça uma segunda cópia manual no celular.
- Sem conexão, o último saldo continua visível e até 50 lançamentos ficam na fila. Abra o app para retomar o envio quando a conexão voltar. Pendentes mostra erros e permite tentar novamente. Se a conta foi excluída, cadastre-a novamente no celular antes de tentar.
- Teste uma saída pequena, confira no celular e exclua-a após a conferência. Depois repita sem conexão e reconecte: deve existir uma única movimentação.
- A validação local automatizada cobre o protocolo, a fila persistente, migrações e importação idempotente. Bluetooth, entrega pelo Play Services, teclado e toque na tela redonda ainda precisam ser validados no Galaxy Watch físico.

## Implementação

`:app` e `:wear` compartilham `WearProtocol`. Dinheiro trafega em centavos inteiros. DataItems persistem resumos, lançamentos e confirmações. MessageClient solicita atualização. O banco v5 mantém recibos de UUID na mesma transação da importação, evitando duplicações em reenvios, inclusive após excluir ou restaurar o histórico. Os recibos são locais e não integram o backup JSON; sincronize todas as pendências antes de migrar para outro celular ou reinstalar apagando dados.

```powershell
.\gradlew.bat :app:testDebugUnitTest :wear:testDebugUnitTest :app:lintDebug :wear:lintDebug :app:assembleDebug :wear:assembleDebug
```
