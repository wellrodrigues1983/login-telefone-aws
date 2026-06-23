# Instruções de configuração na AWS — Login por telefone

Este documento reúne **tudo que você precisa fazer fora do código** para o envio de SMS
de confirmação funcionar de verdade pela AWS. Enquanto você não fizer isso, o backend
continua funcionando localmente: o código de confirmação aparece **no log do servidor**
(fallback), o que é suficiente para desenvolver e testar.

---

## 1. Visão geral

O backend usa o **Amazon SNS** (Simple Notification Service) para enviar o SMS com o
código de 6 dígitos. O fluxo é:

```
App Android  --(POST /users/login)-->  Backend  --(SNS Publish)-->  SMS no celular
```

O envio é controlado por variáveis de ambiente lidas em `application.yaml`:

| Variável            | Padrão        | Função                                              |
|---------------------|---------------|-----------------------------------------------------|
| `AWS_SNS_ENABLED`   | `false`       | `true` ativa o envio real via SNS                   |
| `AWS_REGION`        | `us-east-1`   | Região da AWS usada pelo SNS                         |
| `AWS_SNS_SENDER_ID` | `LoginApp`    | Nome do remetente exibido no SMS (onde suportado)   |

---

## 2. Pré-requisitos

1. Uma conta AWS ativa.
2. AWS CLI instalada (opcional, mas recomendada): https://aws.amazon.com/cli/

---

## 3. Criar um usuário IAM com permissão de SNS

1. Console AWS → **IAM** → **Users** → **Create user**.
2. Nome: `login-sms-publisher`. **Não** marque acesso ao console.
3. Em permissões, escolha **Attach policies directly** e crie/anexe esta policy
   (princípio do menor privilégio):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "sns:Publish",
        "sns:SetSMSAttributes",
        "sns:GetSMSAttributes"
      ],
      "Resource": "*"
    }
  ]
}
```

4. Após criar o usuário, vá em **Security credentials → Create access key →
   Application running outside AWS** e guarde o **Access key ID** e o **Secret access key**.

> Nunca faça commit dessas chaves no Git.

---

## 4. Configurar SMS no SNS

1. Console AWS → **Amazon SNS** → **Text messaging (SMS)**.
2. Em **Account spend limit**, defina um limite (ex.: `1.00` USD) para evitar surpresas.
3. **Sandbox de SMS (muito importante):** contas novas começam no *SMS sandbox*. Nele só é
   possível enviar SMS para **números verificados**. Para testar:
   - Em **Sandbox destination phone numbers → Add phone number**, cadastre o seu número
     (formato E.164, ex.: `+5541999990000`) e confirme com o código que chegar por SMS.
   - Para enviar para qualquer número, é preciso **sair do sandbox** (Production access),
     que requer solicitação à AWS.

---

## 5. Fornecer as credenciais ao backend

Escolha **uma** das opções:

### Opção A — Variáveis de ambiente (recomendada para testes)

```bash
export AWS_ACCESS_KEY_ID="AKIA..."
export AWS_SECRET_ACCESS_KEY="..."
export AWS_REGION="us-east-1"
export AWS_SNS_ENABLED="true"
# então rode o servidor:
./gradlew bootRun
```

### Opção B — Arquivo `~/.aws/credentials`

```ini
[default]
aws_access_key_id = AKIA...
aws_secret_access_key = ...
region = us-east-1
```

E rode com `AWS_SNS_ENABLED=true ./gradlew bootRun`.

O SDK da AWS (v2) localiza as credenciais automaticamente via *Default Credentials Provider Chain*
(variáveis de ambiente → `~/.aws/credentials` → role da instância, nessa ordem).

---

## 6. Testar

```bash
# 1. Pede o login -> deve retornar 202 e enviar/loggar o código
curl -i -X POST http://localhost:8080/users/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"+5541999990000","uuid":"device-uuid-1"}'

# 2. Confirma com o código recebido por SMS (ou visto no log) -> retorna 200 + usuário
curl -i -X POST http://localhost:8080/users/confirm \
  -H "Content-Type: application/json" \
  -d '{"phone":"+5541999990000","uuid":"device-uuid-1","code":"123456"}'

# 3. Atualiza dados do usuário (use o id retornado acima)
curl -i -X PUT http://localhost:8080/users/SEU_ID \
  -H "Content-Type: application/json" \
  -d '{"name":"Wellington","description":"Aluno PUCPR"}'
```

Com `AWS_SNS_ENABLED=false` (padrão), o passo 1 imprime no log algo como:

```
[SMS FALLBACK] Para: +5541999990000
[SMS FALLBACK] Código de confirmação: 123456
```

---

## 7. (Opcional) Hospedar o backend na AWS

Se quiser que o app físico acesse o servidor sem usar sua máquina local:

- **AWS Elastic Beanstalk** (mais simples para Spring Boot): empacote com `./gradlew bootJar`
  e suba o `.jar`. A *instance role* já pode conter a policy de SNS do passo 3 (aí nem precisa
  de access keys).
- Ou **EC2 / ECS / App Runner**, conforme sua preferência.

Depois é só apontar o `BASE_URL` do app Android (`data/ApiClient.kt`) para a URL pública.

---

## 8. Custos

SMS via SNS é pago por mensagem (centavos de dólar por SMS, varia por país). O *spend limit*
do passo 4 protege contra gastos inesperados. Para a entrega do trabalho, o fallback de log
evita qualquer custo.
