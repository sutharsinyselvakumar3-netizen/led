package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DeviceConfig
import com.example.data.model.NodeMcuStatus
import com.example.data.network.NodeMcuApiClient
import com.example.data.repository.DeviceSettingsRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals("NodeMCU LED Controller", appName)
    }

    @Test
    fun `device settings repository saves and clears config`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = DeviceSettingsRepository(context)

        // Clear previous state
        repository.clearConfig()
        assertEquals("", repository.config.value.ipAddress)
        assertEquals(80, repository.config.value.httpPort)

        // Save new state
        val targetConfig = DeviceConfig(
            ipAddress = "192.168.1.150",
            httpPort = 8080,
            autoRefresh = false
        )
        repository.saveConfig(targetConfig)

        assertEquals("192.168.1.150", repository.config.value.ipAddress)
        assertEquals(8080, repository.config.value.httpPort)
        assertFalse(repository.config.value.autoRefresh)

        // Clear again
        repository.clearConfig()
        assertEquals("", repository.config.value.ipAddress)
    }

    @Test
    fun `api client builds correct URLs`() {
        val client = NodeMcuApiClient()
        val urlStatus = client.buildUrl("192.168.1.100", 80, "/status")
        assertEquals("http://192.168.1.100:80/status", urlStatus)

        val urlLedOn = client.buildUrl("http://192.168.43.20", 8080, "led/on")
        assertEquals("http://192.168.43.20:8080/led/on", urlLedOn)
    }

    @Test
    fun `node mcu status json parsing`() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(NodeMcuStatus::class.java)

        val json = """
            {
                "device": "NodeMCU ESP8266",
                "connected": true,
                "ip": "192.168.1.55",
                "led": "ON",
                "ledOn": true,
                "httpPort": 80,
                "udpPort": 4210,
                "rssi": -48,
                "uptime": 120,
                "ssid": "HomeWiFi"
            }
        """.trimIndent()

        val parsed = adapter.fromJson(json)
        assertEquals("NodeMCU ESP8266", parsed?.device)
        assertTrue(parsed?.ledOn == true)
        assertEquals("192.168.1.55", parsed?.ip)
        assertEquals(80, parsed?.httpPort)
        assertEquals(4210, parsed?.udpPort)
        assertEquals(-48, parsed?.rssi)
        assertEquals("HomeWiFi", parsed?.ssid)
    }
}
