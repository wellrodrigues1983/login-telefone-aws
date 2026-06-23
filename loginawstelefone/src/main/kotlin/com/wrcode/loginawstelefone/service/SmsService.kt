package com.wrcode.loginawstelefone.service

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest

/**
 * Envia o SMS de confirmação via Amazon SNS.
 *
 * Região e credenciais são resolvidas pelo Spring a partir das variáveis de ambiente
 * (AWS_REGION, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY). Se as credenciais forem
 * fornecidas, usamos um StaticCredentialsProvider; senão, caímos na cadeia padrão
 * do SDK (~/.aws/credentials, IAM role). O envio é ativado por AWS_SNS_ENABLED=true.
 *
 * Quando o envio está desativado (ou em caso de falha de envio), faz fallback
 * registrando o código no log — assim o fluxo continua testável.
 */
@Service
class SmsService(
    @Value("\${aws.sns.enabled:false}") private val snsEnabled: Boolean,
    @Value("\${aws.region:us-east-1}") private val region: String,
    @Value("\${aws.sns.sender-id:}") private val senderId: String,
    @Value("\${aws.access-key-id:}") private val accessKeyId: String = "",
    @Value("\${aws.secret-access-key:}") private val secretAccessKey: String = "",
) {
    private val log = LoggerFactory.getLogger(SmsService::class.java)

    private val hasStaticCredentials: Boolean
        get() = accessKeyId.isNotBlank() && secretAccessKey.isNotBlank()

    /** Cliente SNS criado sob demanda. */
    private val snsClient: SnsClient? by lazy {
        if (!snsEnabled) return@lazy null
        runCatching {
            val builder = SnsClient.builder().region(Region.of(region))
            if (hasStaticCredentials) {
                builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                    )
                )
            } else {
                builder.credentialsProvider(DefaultCredentialsProvider.create())
            }
            builder.build()
        }.onFailure {
            log.error("Falha ao inicializar o cliente SNS ({}). Verifique credenciais/região.", it.message)
        }.getOrNull()
    }

    @PostConstruct
    fun logStartupMode() {
        if (snsEnabled) {
            val credSource = if (hasStaticCredentials) {
                "variáveis de ambiente (accessKeyId=${maskAccessKey(accessKeyId)})"
            } else {
                "cadeia padrão do SDK (~/.aws/credentials, IAM role)"
            }
            log.info("Envio de SMS via Amazon SNS ATIVADO (região={}, senderId={}, credenciais={}).",
                region, senderId.ifBlank { "<padrão>" }, credSource)
            if (!hasStaticCredentials) {
                log.warn(
                    "AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY não chegaram ao processo. " +
                        "Se você exportou as variáveis mas elas não aparecem aqui, o processo " +
                        "(daemon do Gradle ou IDE) provavelmente não as herdou — veja o README."
                )
            }
        } else {
            log.info("Envio de SMS via Amazon SNS DESATIVADO — códigos serão exibidos no log (fallback). " +
                "Para ativar, defina AWS_SNS_ENABLED=true no ambiente do processo.")
        }
    }

    private fun maskAccessKey(key: String): String =
        if (key.length <= 4) "****" else "${key.take(4)}****${key.takeLast(2)}"

    fun sendConfirmationCode(phone: String, code: String) {
        val target = normalizePhone(phone)
        val message = "Seu código de Ativação é: $code"

        val client = snsClient
        if (client == null) {
            if (snsEnabled) log.error("SNS habilitado, mas o cliente não está disponível. Usando fallback.")
            logFallback(target, code)
            return
        }

        runCatching {
            val request = PublishRequest.builder()
                .phoneNumber(target)
                .message(message)
                .messageAttributes(buildSmsAttributes())
                .build()
            val response = client.publish(request)
            log.info("SMS enviado via SNS para {} (messageId={})", target, response.messageId())
        }.onFailure { e ->
            log.error(
                "Falha ao enviar SMS via SNS para {}: {}. " +
                    "Possíveis causas: número não verificado no SMS sandbox, formato E.164 inválido, " +
                    "permissão IAM ausente (sns:Publish) ou limite de gasto. Usando fallback de log.",
                target, e.message,
            )
            logFallback(target, code)
        }
    }

    /** Atributos do SMS. SenderID só é enviado quando configurado (não suportado em todos os países). */
    private fun buildSmsAttributes(): Map<String, MessageAttributeValue> {
        val attrs = mutableMapOf(
            "AWS.SNS.SMS.SMSType" to MessageAttributeValue.builder()
                .dataType("String").stringValue("Transactional").build(),
        )
        if (senderId.isNotBlank()) {
            attrs["AWS.SNS.SMS.SenderID"] = MessageAttributeValue.builder()
                .dataType("String").stringValue(senderId).build()
        }
        return attrs
    }

    /** Garante o formato E.164 exigido pelo SNS (ex.: +5541999990000). */
    private fun normalizePhone(phone: String): String {
        val trimmed = phone.trim().replace(" ", "").replace("-", "")
        if (!trimmed.startsWith("+")) {
            log.warn(
                "Telefone '{}' não está no formato E.164 (sem '+' e código do país). " +
                    "O SNS pode rejeitar. Ex.: +5541999990000.",
                trimmed,
            )
        }
        return trimmed
    }

    private fun logFallback(phone: String, code: String) {
        log.info("========================================")
        log.info("[SMS FALLBACK] Para: {}", phone)
        log.info("[SMS FALLBACK] Código de confirmação: {}", code)
        log.info("========================================")
    }
}
