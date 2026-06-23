# Login por telefone — AuthServer + Apps Android e iOS

Trabalho da disciplina (PUCPR) — **Tema 1: Login por telefone**.

O projeto tem três partes:

- **`loginawstelefone/`** — Backend (Spring Boot 4 + Kotlin). Expõe os endpoints de login,
  confirmação e atualização de usuário, e envia o SMS de confirmação via **Amazon SNS**.
- **`app/`** (módulo Android, projeto raiz `LoginPhoneAws`) — App em Jetpack Compose que
  realiza o login por telefone de fato no celular.
- **`IOS/LoginPhoneApp/`** — App iOS (SwiftUI) com cadastro (nome, email, senha, telefone),
  verificação por SMS e tela Home, usando o `identifierForVendor` como uuid.

## Integrantes

- Wellington Rodrigues

## Vídeo de demonstração

https://youtu.be/ztYIbk2_RzY

---

## Como funciona o fluxo

1. **`POST /users/login`** — corpo `{ phone, uuid }`.
   - Se `phone` + `uuid` pertencem a um usuário **ativo** → faz o login e retorna **200** + usuário.
   - Se o telefone não existe, existe com outro `uuid`, ou ainda não foi confirmado →
     gera um código de 6 dígitos, **envia SMS** e retorna **202** (sem corpo).
2. **`POST /users/confirm`** — corpo `{ phone, uuid, code }`.
   - Se não há código pendente para esse `phone` + `uuid` → **404**.
   - Se o código confere → **ativa um novo usuário** ou **substitui o `uuid`** do usuário
     existente, e retorna **200** + usuário.
   - Código errado/expirado → **400**.
3. **`PUT /users/{id}`** — corpo `{ name?, description?, avatar? }`. Atualiza os demais dados
   do usuário. Retorna **200** + usuário (ou **404** se o id não existir).
4. **`GET /users/{id}`** — auxiliar, retorna o usuário.

No Android, o `uuid` é **gerado uma vez e persistido** no aparelho
(`data/DeviceId.kt`, via SharedPreferences).

---

## Rodando o backend

Requisitos: **JDK 25** (definido no `build.gradle.kts` via toolchain).

```bash
cd loginawstelefone
./gradlew bootRun
```

O servidor sobe em `http://localhost:8080`.

Por padrão o envio de SMS está **desligado** (`AWS_SNS_ENABLED=false`) e o código de
confirmação é **impresso no log** — ideal para testar sem AWS. Para ativar o envio real
via Amazon SNS, siga o **[INSTRUCOES_AWS.md](INSTRUCOES_AWS.md)**.

### Garantindo que as variáveis de ambiente cheguem ao servidor

As propriedades de AWS no `application.yaml` leem variáveis de ambiente
(`${AWS_REGION}`, `${AWS_ACCESS_KEY_ID}`, etc.). O ponto de atenção é que o **daemon do
Gradle guarda o ambiente de quando foi iniciado** — então variáveis exportadas *depois*
não chegam ao `bootRun`. O mesmo vale ao rodar pela IDE (que não herda o env do terminal).

Confirme no log de inicialização a linha do `SmsService`:

- `... ATIVADO (região=us-east-1, ..., credenciais=variáveis de ambiente (accessKeyId=AKIA****XY))` → ok.
- `... DESATIVADO` ou aviso de que as credenciais não chegaram → o processo não recebeu as variáveis.

Formas confiáveis de rodar com as variáveis (escolha uma):

```bash
# A) Rode o JAR direto — herda o ambiente do shell atual, sem daemon:
./gradlew clean bootJar
java -jar build/libs/loginawstelefone-0.0.1-SNAPSHOT.jar

# B) Force o Gradle sem daemon, no mesmo shell onde exportou as variáveis:
env | grep AWS         # confira que estão setadas
./gradlew --no-daemon bootRun

# C) Se um daemon antigo já estava rodando, mate-o e rode de novo:
./gradlew --stop
./gradlew bootRun
```

Na IDE (IntelliJ), adicione as variáveis em *Run/Debug Configurations → Environment variables*.

### Testes do backend

```bash
cd loginawstelefone
./gradlew test
```

Cobrem o fluxo de login/confirmação (`src/test/kotlin/.../UserServiceTest.kt`).

### Teste rápido via curl

```bash
curl -i -X POST http://localhost:8080/users/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"+5541999990000","uuid":"device-1"}'   # -> 202

# pegue o código no log do servidor, então:
curl -i -X POST http://localhost:8080/users/confirm \
  -H "Content-Type: application/json" \
  -d '{"phone":"+5541999990000","uuid":"device-1","code":"123456"}'  # -> 200 + usuário
```

---

## Rodando o app Android

Requisitos: Android Studio recente (o projeto usa AGP 9 / Compose BOM 2026), `minSdk 31`.

1. Abra a **raiz do projeto** (`Cloud AWS/`) no Android Studio.
2. Garanta que o backend está rodando.
3. Ajuste a URL do servidor em **`app/src/main/java/.../data/ApiClient.kt`**:
   - **Emulador:** `http://10.0.2.2:8080/` (já configurado — `10.0.2.2` é o `localhost` do host).
   - **Celular físico:** `http://SEU_IP_NA_REDE:8080/` (o celular precisa estar na mesma rede).
4. Rode o app. Informe o telefone → receba/veja o código → confirme → complete o perfil.

> O app declara `android:usesCleartextTraffic="true"` para permitir HTTP em
> desenvolvimento. Em produção, use HTTPS.

---

## Rodando o app iOS

Requisitos: Xcode recente (o projeto usa o formato de grupos sincronizados; deployment iOS 26).

1. Abra **`IOS/LoginPhoneApp/LoginPhoneApp.xcodeproj`** no Xcode.
2. Garanta que o backend está rodando.
3. A URL do servidor está em **`APIClient.swift`** (`baseURL`):
   - **Simulador:** `http://localhost:8080` (já configurado — o simulador compartilha a rede do Mac).
   - **iPhone físico:** troque por `http://SEU_IP_NA_REDE:8080` e adicione uma exceção de ATS
     (veja abaixo), pois o iOS bloqueia HTTP por padrão.
4. Rode no simulador. Fluxo: **Cadastro** (nome, email, senha, telefone) → o servidor envia o
   SMS e retorna **202** → **Verificação** (digite o código; em dev ele aparece no log do
   servidor) → ao confirmar, os dados do cadastro são salvos (`PUT /users/{id}`) → **Home**.
   Na próxima abertura, o app tenta **auto-login** e vai direto para a Home.

### Exceção de ATS (apenas para IP/dispositivo físico em HTTP)

Para testar via IP da rede em HTTP, adicione ao **Info** do target (ou a um `Info.plist`):

```xml
<key>NSAppTransportSecurity</key>
<dict>
  <key>NSAllowsLocalNetworking</key>
  <true/>
</dict>
```

No simulador apontando para `localhost`, normalmente não é necessário.

#### Fluxo do app iOS

```
Cadastro (nome,email,senha,telefone)
        │  POST /users/login {phone, uuid=identifierForVendor}
        ▼
   202 → SMS enviado ──► Verificação (código)
        │  POST /users/confirm {phone, uuid, code}
        ▼
   200 → PUT /users/{id} {name,email,password} ──► Home
```

---

## Estrutura do código

### Backend (`loginawstelefone/src/main/kotlin/com/wrcode/loginawstelefone/`)

| Arquivo                         | Responsabilidade                                        |
|---------------------------------|---------------------------------------------------------|
| `controller/UserController.kt`  | Endpoints REST (login, confirm, PUT, GET)               |
| `controller/GlobalExceptionHandler.kt` | Mapeia exceções para 404/400                    |
| `service/UserService.kt`        | Regras de negócio do login por telefone                 |
| `service/SmsService.kt`         | Envio via Amazon SNS + fallback de log                  |
| `repository/UserRepository.kt`  | Armazenamento in-memory (usuários e códigos)            |
| `model/`                        | `User`, `ConfirmationCode`                              |
| `dto/Dtos.kt`                   | Requests e responses da API                             |

### Android (`app/src/main/java/br/tec/wrcoder/loginphoneaws/`)

| Arquivo                  | Responsabilidade                                   |
|--------------------------|----------------------------------------------------|
| `MainActivity.kt`        | Host Compose, injeta o uuid no ViewModel           |
| `ui/LoginScreen.kt`      | Telas: telefone → código → logado/perfil           |
| `ui/LoginViewModel.kt`   | Estado e orquestração das chamadas                 |
| `data/AuthRepository.kt` | Traduz respostas HTTP (202/200/404) em resultados  |
| `data/AuthApi.kt`        | Interface Retrofit                                 |
| `data/ApiClient.kt`      | Configuração do Retrofit/OkHttp (BASE_URL)         |
| `data/DeviceId.kt`       | Gera e persiste o uuid do dispositivo              |

### iOS (`IOS/LoginPhoneApp/LoginPhoneApp/`)

| Arquivo                | Responsabilidade                                           |
|------------------------|-----------------------------------------------------------|
| `LoginPhoneAppApp.swift` | Entry point; injeta o `AppState`                        |
| `ContentView.swift`    | Roteia entre fases: launching → signUp → verification → home |
| `AppState.swift`       | Orquestra o fluxo, auto-login e chamadas à API            |
| `SignUpView.swift`     | Tela de cadastro (nome, email, senha, telefone)           |
| `VerificationView.swift` | Tela de verificação por SMS                             |
| `HomeView.swift`       | Tela principal após validação                             |
| `APIClient.swift`      | Cliente HTTP (URLSession), trata 202/200/404              |
| `Models.swift`         | Structs Codable de request/response                       |
| `DeviceID.swift`       | uuid via `identifierForVendor`                            |
| `SessionStore.swift`   | Sessão em UserDefaults (auto-login)                       |

---

## Observações

- O armazenamento é **in-memory**: reiniciar o servidor zera os usuários. Para persistência
  real, troque a implementação de `UserRepository` por DynamoDB/JPA mantendo a mesma interface.
- A **senha** é guardada com hash (SHA-256 + salt) e nunca retornada pela API. Em produção,
  prefira BCrypt/Argon2.
- Configuração da AWS: veja **[INSTRUCOES_AWS.md](INSTRUCOES_AWS.md)**.
