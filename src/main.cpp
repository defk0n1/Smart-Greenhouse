#include <WiFi.h>
#include <PubSubClient.h>
#include <DHT.h>

// ==================== WiFi Settings (Wokwi) ====================
const char* ssid = "Wokwi-GUEST";
const char* password = "";

// ==================== MQTT Settings ====================
const char* mqtt_server = "test.mosquitto.org";
const int mqtt_port = 1883;
const char* mqtt_client_id = "ESP32_Greenhouse_Wokwi";

// ==================== Sensor Definitions ====================
#define DHTTYPE DHT22
#define DHT_INT_PIN 4
#define DHT_EXT_PIN 5

DHT dhtIndoor(DHT_INT_PIN, DHTTYPE);
DHT dhtOutdoor(DHT_EXT_PIN, DHTTYPE);

#define GAS_SENSOR 34
#define LDR_SENSOR 35
#define POT_SOIL 32
#define POT_TANK 33
#define POT_PH 25

// ==================== Actuator Definitions ====================
#define FAN1 16
#define FAN2 17
#define BULB1 18
#define BULB2 19
#define PUMP 21

// ==================== Control State ====================
int manualFan1 = -1;
int manualFan2 = -1;
int manualBulb1 = -1;
int manualBulb2 = -1;
int manualPump = -1;

// ==================== Thresholds ====================
const int SOIL_THRESHOLD = 2000;
const int TANK_THRESHOLD = 1000;
const int TEMP_THRESHOLD = 28;
const int LDR_THRESHOLD = 2000;
const int PH_LOW = 1500;
const int PH_HIGH = 3000;

// ==================== WiFi & MQTT ====================
WiFiClient espClient;
PubSubClient client(espClient);

// ==================== Timer ====================
unsigned long lastSendTime = 0;
const unsigned long sendInterval = 10000; // 10 seconds

// ==================== MQTT Callback ====================
void callback(char* topic, byte* payload, unsigned int length) {
  String msg = "";
  for (int i = 0; i < length; i++) msg += (char)payload[i];

  Serial.print("Commande reçue: ");
  Serial.println(msg);

  // JSON manuelle
  if (msg.indexOf("fan1") != -1) manualFan1 = msg.indexOf("fan1\":1") != -1;
  if (msg.indexOf("fan2") != -1) manualFan2 = msg.indexOf("fan2\":1") != -1;
  if (msg.indexOf("bulb1") != -1) manualBulb1 = msg.indexOf("bulb1\":1") != -1;
  if (msg.indexOf("bulb2") != -1) manualBulb2 = msg.indexOf("bulb2\":1") != -1;
  if (msg.indexOf("pump") != -1) manualPump = msg.indexOf("pump\":1") != -1;
}

// ==================== MQTT Reconnect ====================
void reconnectMQTT() {
  while (!client.connected()) {
    Serial.print("Connecting to MQTT...");
    if (client.connect(mqtt_client_id)) {
      Serial.println("Connected!");
      client.subscribe("greenhouse/control");  // Listen for commands
    } else {
      Serial.print("Failed, rc=");
      Serial.println(client.state());
      delay(2000);
    }
  }
}

// ==================== Setup ====================
void setup() {
  Serial.begin(115200);

  dhtIndoor.begin();
  dhtOutdoor.begin();

  pinMode(FAN1, OUTPUT);
  pinMode(FAN2, OUTPUT);
  pinMode(BULB1, OUTPUT);
  pinMode(BULB2, OUTPUT);
  pinMode(PUMP, OUTPUT);

  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(400);
    Serial.print(".");
  }
  Serial.println("\nWiFi connected!");

  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);
  reconnectMQTT();
}

// ==================== Main Loop ====================
void loop() {
  if (!client.connected()) reconnectMQTT();
  client.loop();

  unsigned long now = millis();
  if (now - lastSendTime < sendInterval) return;
  lastSendTime = now;

  // ==== Read sensors ====
  float tempIndoor = dhtIndoor.readTemperature();
  float humIndoor = dhtIndoor.readHumidity();
  float tempOutdoor = dhtOutdoor.readTemperature();
  float humOutdoor = dhtOutdoor.readHumidity();

  int soil = analogRead(POT_SOIL);
  int tank = analogRead(POT_TANK);
  int ph = analogRead(POT_PH);
  int light = analogRead(LDR_SENSOR);
  int gas = analogRead(GAS_SENSOR);

  // ==== Control logic ====
  int fan1State = (tempIndoor > TEMP_THRESHOLD);
  int fan2State = fan1State;
  int bulb2State = (light < LDR_THRESHOLD);
  int bulb1State = (ph < PH_LOW || ph > PH_HIGH);
  int pumpState = (soil < SOIL_THRESHOLD && tank > TANK_THRESHOLD);

  // ==== Apply manual override ====
  if (manualFan1 != -1) fan1State = manualFan1;
  if (manualFan2 != -1) fan2State = manualFan2;
  if (manualBulb1 != -1) bulb1State = manualBulb1;
  if (manualBulb2 != -1) bulb2State = manualBulb2;
  if (manualPump != -1) pumpState = manualPump;

  digitalWrite(FAN1, fan1State);
  digitalWrite(FAN2, fan2State);
  digitalWrite(BULB1, bulb1State);
  digitalWrite(BULB2, bulb2State);
  digitalWrite(PUMP, pumpState);

  // ==== SEND MQTT DATA ====
  char msg[256];
  snprintf(msg, sizeof(msg),
    "{\"TempIndoor\":%.2f,\"HumIndoor\":%.2f,\"TempOutdoor\":%.2f,\"HumOutdoor\":%.2f,"
    "\"Soil\":%d,\"Tank\":%d,\"pH\":%d,\"Light\":%d,\"Gas\":%d,"
    "\"Fan1\":%d,\"Fan2\":%d,\"Bulb1\":%d,\"Bulb2\":%d,\"Pump\":%d}",
    tempIndoor, humIndoor, tempOutdoor, humOutdoor,
    soil, tank, ph, light, gas,
    fan1State, fan2State, bulb1State, bulb2State, pumpState);

  client.publish("greenhouse/data", msg);
  Serial.println(msg);
}
