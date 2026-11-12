package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.QrPairingPayload
import com.example.network.NetworkUtils
import com.example.network.pairing.QrPairingManager
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
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TwinGuard CCTV", appName)
    }

    @Test
    fun `test qr payload serialization and parsing`() {
        val payload = QrPairingPayload(
            deviceId = "TG-ABC123",
            deviceName = "TwinGuard Test Cam",
            ip = "192.168.1.100",
            port = 8080,
            token = "SECURE12345",
            expiresAt = System.currentTimeMillis() + 60000
        )
        val jsonStr = QrPairingManager.createPayloadString(payload)
        val (parsed, error) = QrPairingManager.parsePayload(jsonStr)

        assertNotNull(parsed)
        assertEquals("TG-ABC123", parsed?.deviceId)
        assertEquals("192.168.1.100", parsed?.ip)
        assertEquals(8080, parsed?.port)
        assertEquals("SECURE12345", parsed?.token)
    }

    @Test
    fun `test session token generation`() {
        val token = NetworkUtils.generateSessionToken()
        assertNotNull(token)
        assertTrue(token.length >= 8)
    }
}
