package com.example.data.generator

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

/**
 * Gerador de identificadores para o LC Player.
 *
 * Conforme especificado nas diretrizes:
 * - Device ID: Formato visual AA:BB:CC:DD:EE:FF
 *   NOTA: Este valor NÃO é nem finge ser o MAC físico real da placa de rede.
 *   É um identificador exclusivo do LC Player gerado para a instalação/dispositivo,
 *   garantindo estabilidade e privacidade.
 * - KEY: Chave numérica de 6 dígitos (ex: 728491) curta para ativação no LC Admin.
 */
object DeviceIdentifierGenerator {

    /**
     * Gera um Device ID de 6 octetos hexadecimais no padrão AA:BB:CC:DD:EE:FF.
     * Utiliza entropia combinada (ANDROID_ID + UUID aleatório) processada com SHA-256.
     */
    fun generateDeviceId(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: UUID.randomUUID().toString()
            
            val seed = "$androidId-${UUID.randomUUID()}-${System.currentTimeMillis()}"
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(seed.toByteArray(Charsets.UTF_8))
            
            // Extrai os primeiros 6 bytes para formar o formato AA:BB:CC:DD:EE:FF
            String.format(
                Locale.US,
                "%02X:%02X:%02X:%02X:%02X:%02X",
                hash[0], hash[1], hash[2], hash[3], hash[4], hash[5]
            )
        } catch (e: Exception) {
            // Fallback seguro em caso de restrição do sistema
            val randomBytes = ByteArray(6).apply { Random.nextBytes(this) }
            String.format(
                Locale.US,
                "%02X:%02X:%02X:%02X:%02X:%02X",
                randomBytes[0], randomBytes[1], randomBytes[2],
                randomBytes[3], randomBytes[4], randomBytes[5]
            )
        }
    }

    /**
     * Gera uma chave numérica de 6 dígitos (ex: 728491) para ativação no painel.
     */
    fun generateKey(): String {
        val number = Random.nextInt(100_000, 1_000_000)
        return number.toString()
    }
}
