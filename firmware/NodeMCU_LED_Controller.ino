/*
 * NodeMCU ESP8266 Built-in LED Wi-Fi Controller
 * 
 * Features:
 * - Compatible with Android NodeMCU LED Controller App
 * - HTTP REST API on Port 80 (/status, /led/on, /led/off)
 * - UDP Discovery on Port 4210 (Responds to "DISCOVER_NODEMCU")
 * - Built-in LED Active-LOW control (LOW = ON, HIGH = OFF)
 * - Supports Home Router Wi-Fi & Mobile Hotspots (Android/iOS)
 * 
 * Instructions:
 * 1. Install ESP8266 Board Package in Arduino IDE:
 *    File -> Preferences -> Additional Board Manager URLs:
 *    http://arduino.esp8266.com/stable/package_esp8266com_index.json
 * 2. Select Board: "NodeMCU 1.0 (ESP-12E Module)"
 * 3. Update WIFI_SSID and WIFI_PASSWORD below.
 * 4. Upload to NodeMCU.
 */

#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <WiFiUdp.h>

// ================= USER CONFIGURATION =================
// Replace with your Wi-Fi Router or Phone Hotspot credentials:
const char* WIFI_SSID     = "YOUR_WIFI_SSID";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";

// Ports
const uint16_t HTTP_PORT = 80;
const uint16_t UDP_PORT  = 4210;

// Built-in LED definition (NodeMCU ESP-12E uses GPIO 2, Active-LOW)
#ifndef LED_BUILTIN
#define LED_BUILTIN 2
#endif

// ================= GLOBALS =================
ESP8266WebServer server(HTTP_PORT);
WiFiUDP udp;
char udpPacketBuffer[255];
bool ledState = false; // false = OFF, true = ON

// Apply physical state to active-LOW LED
void updatePhysicalLed() {
  // Active-LOW: LOW turns the LED ON, HIGH turns it OFF
  digitalWrite(LED_BUILTIN, ledState ? LOW : HIGH);
}

// Generate JSON status string
String getStatusJson() {
  String json = "{";
  json += "\"device\":\"NodeMCU ESP8266\",";
  json += "\"connected\":true,";
  json += "\"ip\":\"" + WiFi.localIP().toString() + "\",";
  json += "\"led\":\"" + String(ledState ? "ON" : "OFF") + "\",";
  json += "\"ledOn\":" + String(ledState ? "true" : "false") + ",";
  json += "\"httpPort\":" + String(HTTP_PORT) + ",";
  json += "\"udpPort\":" + String(UDP_PORT) + ",";
  json += "\"rssi\":" + String(WiFi.RSSI()) + ",";
  json += "\"uptime\":" + String(millis() / 1000) + ",";
  json += "\"ssid\":\"" + WiFi.SSID() + "\"";
  json += "}";
  return json;
}

// HTTP Helper with CORS headers
void sendJsonResponse(int code, String body) {
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.sendHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  server.sendHeader("Access-Control-Allow-Headers", "Content-Type");
  server.send(code, "application/json", body);
}

// Handler: GET / and GET /status
void handleStatus() {
  sendJsonResponse(200, getStatusJson());
}

// Handler: GET /led/on
void handleLedOn() {
  ledState = true;
  updatePhysicalLed();
  Serial.println("[HTTP] LED Turned ON");
  sendJsonResponse(200, getStatusJson());
}

// Handler: GET /led/off
void handleLedOff() {
  ledState = false;
  updatePhysicalLed();
  Serial.println("[HTTP] LED Turned OFF");
  sendJsonResponse(200, getStatusJson());
}

// Handler: 404 Not Found
void handleNotFound() {
  String message = "{\"error\":\"Not Found\",\"path\":\"" + server.uri() + "\"}";
  sendJsonResponse(404, message);
}

void setup() {
  Serial.begin(115200);
  delay(500);
  Serial.println();
  Serial.println("==========================================");
  Serial.println("  NodeMCU ESP8266 LED Wi-Fi Controller   ");
  Serial.println("==========================================");

  // Initialize Built-in LED
  pinMode(LED_BUILTIN, OUTPUT);
  ledState = false;
  updatePhysicalLed();

  // Connect to Wi-Fi
  WiFi.mode(WIFI_STA);
  Serial.print("Connecting to Wi-Fi: ");
  Serial.println(WIFI_SSID);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 40) {
    delay(500);
    Serial.print(".");
    // Blink while connecting
    digitalWrite(LED_BUILTIN, (attempts % 2 == 0) ? LOW : HIGH);
    attempts++;
  }

  // Restore LED OFF state after connection attempt
  ledState = false;
  updatePhysicalLed();

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n[Wi-Fi] Connected successfully!");
    Serial.print("[Wi-Fi] IP Address: ");
    Serial.println(WiFi.localIP());
    Serial.print("[Wi-Fi] Signal Strength (RSSI): ");
    Serial.print(WiFi.RSSI());
    Serial.println(" dBm");
  } else {
    Serial.println("\n[Wi-Fi] Connection failed or timed out.");
    Serial.println("Check SSID/Password or restart board.");
  }

  // Setup HTTP Web Server Endpoints
  server.on("/", HTTP_GET, handleStatus);
  server.on("/status", HTTP_GET, handleStatus);
  server.on("/led/on", HTTP_GET, handleLedOn);
  server.on("/led/off", HTTP_GET, handleLedOff);
  server.onNotFound(handleNotFound);
  server.begin();
  Serial.printf("[HTTP] Server listening on port %d\n", HTTP_PORT);

  // Setup UDP Discovery Server
  udp.begin(UDP_PORT);
  Serial.printf("[UDP] Discovery listening on port %d\n", UDP_PORT);
  Serial.println("Ready for Android App commands!");
}

void loop() {
  // Handle incoming HTTP requests
  server.handleClient();

  // Check for UDP discovery broadcast
  int packetSize = udp.parsePacket();
  if (packetSize > 0) {
    int len = udp.read(udpPacketBuffer, 254);
    if (len > 0) {
      udpPacketBuffer[len] = '\0';
    }
    String message = String(udpPacketBuffer);
    message.trim();

    // Check if discovery ping
    if (message.indexOf("DISCOVER_NODEMCU") >= 0) {
      Serial.printf("[UDP] Discovery request from %s:%d\n",
                    udp.remoteIP().toString().c_str(), udp.remotePort());

      // Prepare Discovery JSON reply
      String reply = "{";
      reply += "\"device\":\"NodeMCU ESP8266\",";
      reply += "\"ip\":\"" + WiFi.localIP().toString() + "\",";
      reply += "\"httpPort\":" + String(HTTP_PORT) + ",";
      reply += "\"udpPort\":" + String(UDP_PORT) + ",";
      reply += "\"ledOn\":" + String(ledState ? "true" : "false");
      reply += "}";

      // Send reply back to requesting device
      udp.beginPacket(udp.remoteIP(), udp.remotePort());
      udp.print(reply);
      udp.endPacket();
      Serial.println("[UDP] Sent discovery reply: " + reply);
    }
  }
}
