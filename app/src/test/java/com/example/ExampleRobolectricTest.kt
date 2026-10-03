package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.generator.DeviceIdentifierGenerator
import com.example.domain.model.ActivationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LC Player", appName)
    }

    @Test
    fun `verify device id format AA BB CC DD EE FF`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val deviceId = DeviceIdentifierGenerator.generateDeviceId(context)
        val regex = Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}\$")
        assertTrue("Device ID should match AA:BB:CC:DD:EE:FF pattern but was $deviceId", regex.matches(deviceId))
    }

    @Test
    fun `verify device key format 6 digits`() {
        val key = DeviceIdentifierGenerator.generateKey()
        val regex = Regex("^\\d{6}\$")
        assertTrue("Key should be a 6 digit number but was $key", regex.matches(key))
    }

    @Test
    fun `verify default status is waiting activation`() {
        val defaultStatus = ActivationStatus.WAITING_ACTIVATION
        assertEquals("Aguardando ativação", defaultStatus.title)
    }
}
