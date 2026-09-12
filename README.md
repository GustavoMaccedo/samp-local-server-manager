# SAMP Local Server Manager

![Android](https://img.shields.io/badge/platform-Android-green)
![Kotlin](https://img.shields.io/badge/kotlin-2.2.21-blue)
![Compose](https://img.shields.io/badge/UI-Compose_Material3-blue)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

App Android para rodar e gerenciar um servidor SA-MP (`samp03svr` Linux x86) direto no celular, sem Termux e sem root. O binário roda sobre um runtime QEMU-i386 embutido no APK, e o app cuida do resto: instalação, `server.cfg`, gamemodes, plugins, console em tempo real e exposição na internet mesmo atrás de CGNAT.

Pacote: `com.samplocal.manager` · Versão atual: `1.0.1 (2)` · `minSdk 26`, `target/compile 36`.

## Download

APK pronto para instalar na página de [Releases](https://github.com/GustavoMaccedo/samp-local-server-manager/releases). Baixa o `SAMP-Manager-v1.0.1.apk`, instala e abre — o runtime é configurado sozinho na primeira execução.

## Linguagens

Proporção por linhas de código-fonte (binários do runtime — QEMU, `.so`, rootfs — fora da conta):

```
Kotlin  ████████████████████████████░░ 94%
Python  █░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  2%
Outros  █░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  4%   (JSON, XML, Gradle, Markdown)
```

## Sumário

- [Download](#download)
- [Linguagens](#linguagens)
- [O que ele faz](#o-que-ele-faz)
- [Como funciona](#como-funciona)
- [Conectividade](#conectividade)
- [Tecnologias](#tecnologias)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Como compilar](#como-compilar)
- [Relay de referência (pasta `relay/`)](#relay-de-referência-pasta-relay)
- [Testes](#testes)
- [Limitações](#limitações)
- [Licença](#licença)

## O que ele faz

- Sobe o `samp03svr` no próprio aparelho via QEMU-i386 user-mode + rootfs glibc i386 que já vão dentro do APK.
- Gerencia um ou mais servidores locais: criar, importar de ZIP, iniciar, parar, reiniciar, ver status, uptime e PID.
- Edita o essencial do `server.cfg` (porta, hostname, maxplayers, gamemode, plugins, RCON) preservando chaves desconhecidas.
- Importa pasta/ZIP de servidor existente: detecta binário ELF válido, achata nível único de pasta, valida `server.cfg` e gamemode.
- Console ao vivo com `server_log.txt` como fonte, dedup de linhas repetidas e retenção configurável (1/7/30 dias ou nunca).
- Dashboard com métricas do processo (CPU/mem via `/proc`), jogadores online (query SA-MP) e diagnóstico do runtime.
- Arquivos: navegar, enviar `server.cfg`, gamemode (`.amx`), plugins (`.so`), ver logs.
- Foreground service com notificação para manter o servidor ligado com a tela apagada.
- App em 4 idiomas: PT-BR, EN, ES, RU. Tema claro/escuro + cor de destaque.

## Como funciona

O `samp03svr` oficial é ELF 32-bit x86. Em celular ARM64 ele não executa nativamente, então o app leva o emulador junto:

```
APK assets/runtime/
├── bin/qemu-i386            # QEMU user-mode (bionic AArch64)
├── lib/qemu/*.so            # deps do QEMU
└── rootfs/lib/*             # glibc i386 mínima (ld-linux, libc, libm, ...)

Na primeira execução, RuntimeInstaller extrai tudo para <filesDir>/runtime/,
confere SHA-256 pelo manifest.json e faz smoke test do backend.

Start de um servidor = qemu-i386 -L <filesDir>/runtime/rootfs <serverDir>/samp03svr
```

- `ArchitectureDetector` lê o ELF (classe, machine, interpreter, NEEDED) e decide o backend (`QEMU_I386`, `NATIVE`, `UNSUPPORTED`).
- `ServerManager` valida nessa ordem: binário instalado, runtime pronto, `server.cfg` existe, porta livre, e só então inicia o processo e monitora até o log indicar `ONLINE`.
- Multi-servidor: cada servidor é uma pasta em `<filesDir>/servers/<id>/` com seu `server.cfg`, `server_log.txt`, gamemode e plugins.

## Conectividade

Celular normalmente está atrás de CGNAT, então `porta aberta no roteador` não existe. O app tem seleção automática de provedor, do mais barato para o mais garantido:

1. **Direct** — quando há IP público/UPnP de verdade, anuncia `IP:porta` direto.
2. **Playit.gg** — cria túnel pela API oficial (`api.playit.gg`), roda o agente local e expõe o servidor por endereço público da Playit. Inclui fluxo de claim (`playit.gg/claim/<código>`) e proxy local de `http_proxy/https_proxy`.
3. **Relay UDP próprio (`SLR1`)** — protocolo pequeno sobre UDP com `HMAC-SHA256(token)[0:16]` em todo pacote: `REGISTER`, `DATA_A2R/R2A`, `PING/PONG`, `CLOSE`, `STATUS`. O relay nunca faz proxy aberto: cada porta de sessão é amarrada a uma sessão registrada. Implementação de referência em `relay/server.py` (só stdlib).
4. **TURN** — alocação TURN como último recurso, com cliente STUN (RFC 5389) para classificação de NAT e medição antes de decidir.

`ConnectivityRepository` + `ProviderSelector` centralizam a decisão, e a tela de Conectividade mostra NAT detectado, latência, provedor ativo e endereço para divulgar aos jogadores.

## Tecnologias

App:

- Kotlin `2.2.21`, AGP `8.11.1`, Gradle `8.13`, Java `17`
- Jetpack Compose + Material3 `1.4.0`, Navigation Compose `2.9.0`, Activity Compose, Lifecycle + ViewModel (Compose)
- Coroutines `1.10.2` + Flow / StateFlow, DataStore Preferences, DocumentFile, Core KTX
- `org.json` para o que precisa de JSON no app
- Foreground service (`dataSync`) + FileProvider para importação de ZIP

Runtime / SA-MP:

- `samp03svr` ELF32 i386, QEMU-i386 user-mode `11.0.3`, rootfs Debian glibc i386
- Parser ELF próprio (`ElfParser`), leitura de `server.cfg`, query protocol SA-MP (players/ping), RCON

Rede:

- UDP vanilla SA-MP, STUN, TURN, HMAC-SHA256, Playit.gg REST + agente local

Relay:

- Python `3.8+`, só biblioteca padrão (`asyncio`, `hmac`, `struct`)

Testes:

- JUnit4 + `kotlinx-coroutines-test`, 26 testes unitários cobrindo parser de config, ZIP import, ELF/arquitetura, query, STUN/TURN, protocolo do relay, política de conectividade, dedup de console e instalador do runtime.

## Estrutura do projeto

```
app/src/main/java/com/samplocal/manager/
├── MainActivity.kt / SampApp.kt
├── core/        # ServerManager, ProcessManager, FileManager, LogManager, MetricsManager, AppPrefs
├── runtime/     # RuntimeManager, LinuxRuntime, RuntimeInstaller, EmulatorManager, ArchitectureDetector
├── samp/        # ConfigManager (server.cfg), GamemodeManager, PluginManager, SampBinaryProvider, ServerQueryManager
├── net/
│   ├── stun/ / turn/        # StunClient, TurnClient
│   ├── playit/              # PlayitApi, PlayitAgent, LocalProxy
│   └── relay/               # RelayProtocol (SLR1+HMAC), provedores, UdpBridge, RelaySessionManager
├── service/     # SampServerService (foreground)
├── ui/          # screens (Dashboard, Console, Files, Settings, Wizard, Connectivity), viewmodels, theme
└── util/        # ElfParser, ZipUtils, PortUtils, SecretStore/KeystoreStore, WizardFlag
relay/
├── server.py            # relay UDP de referência
└── test_integration.py  # ponta a ponta em loopback
```

## Como compilar

Pré-requisitos: JDK 17, Android SDK com `compileSdk 36`, e o `local.properties` apontando para o SDK (`sdk.dir=...` — arquivo local, não vai para o git).

```bash
./gradlew :app:assembleDebug
# APK em app/build/outputs/apk/debug/
```

Para instalar direto no aparelho:

```bash
./gradlew :app:installDebug
```

Variante `debug` usa `applicationIdSuffix .debug` para conviver com a release.

## Relay de referência (pasta `relay/`)

Sobe um relay local para teste ou um servidor próprio barato (qualquer VPS com UDP liberado):

```bash
python3 relay/server.py --port 7779 --session-base 30000 --max-sessions 256
python3 relay/test_integration.py
```

O teste cobre REGISTER/REGISTERED, ida e volta jogador → relay → Android → samp, HMAC inválido, porta sem sessão, heartbeat e STATUS.

## Testes

```bash
./gradlew :app:testDebugUnitTest
```

26 testes em `app/src/test/`. Incluem `RelayProtocolTest`, `StunCodecTest`, `TurnCodecTest`, `PlayitTest`, `ConfigParserTest`, `ZipImportTest`, `ArchitectureDetectorTest`, `BundledRuntimeTest` e `RuntimeInstallerTest`.

## Limitações

- Só IPv4 no caminho do relay nesta versão.
- Binário suportado é o `samp03svr` Linux x86; em ARM64 o custo é emulação via QEMU (suficiente para servidor pequeno/médio, não espere performance de VPS).
- Manter em segundo plano depende do `keepBackground` + foreground service; o Android pode matar o processo em aparelho com bateria agressiva.
- O APK carrega o runtime em `assets/` (~22 MB), então o download é maior que um app comum.

## Licença

MIT — ver [LICENSE](LICENSE).
