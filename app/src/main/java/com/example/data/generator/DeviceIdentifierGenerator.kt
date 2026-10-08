package com.example.data.generator

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale
import kotlin.random.Random

/**
 * Gerador determinístico de identificadores para o LC Player.
 *
 * Diretrizes estritas:
 * - O Device ID NÃO utiliza e NÃO depende de:
 *   - Wi-Fi MAC Address
 *   - NetworkInterface / hardwareAddress
 *   - BSSID / SSID
 *   - Endereço IP / roteador / hotspot
 *   - Qualquer interface de rede (Wi-Fi, Ethernet, celular)
 * - Baseia-se exclusivamente no identificador estável do sistema: Settings.Secure.ANDROID_ID.
 * - Aplica uma transformação determinística com hash SHA-256 e salt de aplicação.
 * - O mesmo ANDROID_ID sempre produz exatamente o mesmo Device ID no formato visual AA:BB:CC:DD:EE:FF.
 */
object DeviceIdentifierGenerator {

    private const val DEVICE_ID_SALT = "LC_PLAYER_DEVICE_ID_SALT_V1:"
    private const val KEY_SALT = "LC_PLAYER_KEY_SALT_V1:"

    /**
     * Gera um Device ID estritamente determinístico no padrão AA:BB:CC:DD:EE:FF.
     * 
     * O mesmo aparelho/instalação sempre terá o mesmo Device ID, mesmo alternando redes Wi-Fi,
     * roteadores, Ethernet ou reiniciando o aparelho.
     */
    fun generateDeviceId(context: Context): String {
        val androidId = getCleanAndroidId(context)
        val seed = "$DEVICE_ID_SALT$androidId"
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(seed.toByteArray(Charsets.UTF_8))

        // Extrai os 6 primeiros bytes do hash determinístico SHA-256
        return String.format(
            Locale.US,
            "%02X:%02X:%02X:%02X:%02X:%02X",
            hash[0], hash[1], hash[2], hash[3], hash[4], hash[5]
        )
    }

    /**
     * Gera uma KEY numérica de 6 dígitos (ex: 728491) determinística baseada no ANDROID_ID.
     * Se o context for nulo, gera uma chave numérica segura de 6 dígitos.
     */
    fun generateKey(context: Context? = null): String {
        if (context != null) {
            val androidId = getCleanAndroidId(context)
            val seed = "$KEY_SALT$androidId"
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(seed.toByteArray(Charsets.UTF_8))

            val value = ((hash[0].toInt() and 0xFF) shl 24) or
                    ((hash[1].toInt() and 0xFF) shl 16) or
                    ((hash[2].toInt() and 0xFF) shl 8) or
                    (hash[3].toInt() and 0xFF)
            val positiveValue = kotlin.math.abs(value)
            val sixDigits = 100_000 + (positiveValue % 900_000)
            return sixDigits.toString()
        }

        val number = Random.nextInt(100_000, 1_000_000)
        return number.toString()
    }

    /**
     * Obtém o ANDROID_ID estável do dispositivo sem expor o valor original.
     * Nunca consulta interfaces de rede, Wi-Fi ou IP.
     */
    private fun getCleanAndroidId(context: Context): String {
        val rawId = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        } catch (_: Exception) {
            null
        }

        return if (!rawId.isNullOrBlank() && rawId != "9774d56d682e549c") {
            rawId.trim().lowercase(Locale.US)
        } else {
            // Fallback determinístico de hardware caso ANDROID_ID seja nulo em emuladores antigos
            val fallback = "${Build.MANUFACTURER}_${Build.MODEL}_${Build.BOARD}_${Build.DEVICE}"
            fallback.lowercase(Locale.US)
        }
    }
}
