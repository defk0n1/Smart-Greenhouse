#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <DHT.h>
#include <ArduinoJson.h>

// ==================== WiFi Settings (Wokwi) ====================
const char* ssid = "Wokwi-GUEST";
const char* password = "";

// ==================== MQTT Settings ====================
const char* mqtt_server = "f7650e29f2d1418ab38a502a12ae2a8e.s1.eu.hivemq.cloud";
const int mqtt_port = 8883;
const char* mqtt_client_id = "ESP32_Greenhouse_Wokwi";
const char* mqtt_username = "marwen";
const char* mqtt_password = "M1arwen#";

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
#define POT_PH 36    // ✅ CHANGÉ: GPIO25 → GPIO36 (ADC1 - SVP sur votre board)

// ==================== Actuator Definitions ====================
#define FAN1 16
#define FAN2 17
#define BULB1 18
#define BULB2 19
#define PUMP 21

// ==================== Control State ====================
bool manualFan1 = false;
bool manualFan2 = false;
bool manualBulb1 = false;
bool manualBulb2 = false;
bool manualPump = false;
bool manualMode = false; // Indique si le système est en mode manuel

// ==================== Thresholds ====================
const int SOIL_THRESHOLD = 2000;
const int TANK_THRESHOLD = 1000;
const int TEMP_THRESHOLD = 28;
const int LDR_THRESHOLD = 2000;
const int PH_LOW = 1500;
const int PH_HIGH = 3000;

// ==================== WiFi & MQTT ====================
WiFiClientSecure espClient;
PubSubClient client(espClient);

// ==================== Timer ====================
unsigned long lastSendTime = 0;
const unsigned long sendInterval = 10000; // 10 seconds pour l'envoi des données

// ==================== Fonction pour appliquer les commandes ====================
void applyActuatorCommands() {
  int fan1State, fan2State, bulb1State, bulb2State, pumpState;
  
  if (manualMode) {
    // Mode manuel : utiliser les valeurs manuelles
    fan1State = manualFan1;
    fan2State = manualFan2;
    bulb1State = manualBulb1;
    bulb2State = manualBulb2;
    pumpState = manualPump;
  } else {
    // Mode automatique : utiliser la logique de contrôle automatique
    float tempIndoor = dhtIndoor.readTemperature();
    int light = analogRead(LDR_SENSOR);
    int ph = analogRead(POT_PH);
    int soil = analogRead(POT_SOIL);
    int tank = analogRead(POT_TANK);
    
    fan1State = (tempIndoor > TEMP_THRESHOLD);
    fan2State = fan1State;
    bulb2State = (light < LDR_THRESHOLD);
    bulb1State = (ph < PH_LOW || ph > PH_HIGH);
    pumpState = (soil < SOIL_THRESHOLD && tank > TANK_THRESHOLD);
  }
  
  // Appliquer les commandes aux actionneurs
  digitalWrite(FAN1, fan1State);
  digitalWrite(FAN2, fan2State);
  digitalWrite(BULB1, bulb1State);
  digitalWrite(BULB2, bulb2State);
  digitalWrite(PUMP, pumpState);
}

// ==================== MQTT Callback ====================
void callback(char* topic, byte* payload, unsigned int length) {
  String msg = "";
  for (int i = 0; i < length; i++) msg += (char)payload[i];

  Serial.print("Commande reçue: ");
  Serial.println(msg);

  // Utilisation de ArduinoJson pour parser le JSON
  StaticJsonDocument<200> doc;
  DeserializationError error = deserializeJson(doc, msg);

  if (!error) {
    // Vérifier si c'est une commande manuelle
    if (doc.containsKey("Fan1") || doc.containsKey("Fan2") || 
        doc.containsKey("Bulb1") || doc.containsKey("Bulb2") || 
        doc.containsKey("Pump")) {
      
      manualMode = true; // Passer en mode manuel
      
      if (doc.containsKey("Fan1")) manualFan1 = doc["Fan1"];
      if (doc.containsKey("Fan2")) manualFan2 = doc["Fan2"];
      if (doc.containsKey("Bulb1")) manualBulb1 = doc["Bulb1"];
      if (doc.containsKey("Bulb2")) manualBulb2 = doc["Bulb2"];
      if (doc.containsKey("Pump")) manualPump = doc["Pump"];
      
      // Appliquer immédiatement les nouvelles commandes
      applyActuatorCommands();
      
      Serial.println("Mode manuel activé - Commandes appliquées immédiatement!");
    }
    
    // Vérifier si c'est une commande pour revenir en mode automatique
    if (doc.containsKey("Mode") && strcmp(doc["Mode"], "auto") == 0) {
      manualMode = false;
      Serial.println("Mode automatique activé");
    }
  } else {
    Serial.print("Erreur de parsing JSON: ");
    Serial.println(error.c_str());
  }
}

// ==================== MQTT Reconnect ====================
void reconnectMQTT() {
  while (!client.connected()) {
    Serial.print("Connecting to MQTT...");
    if (client.connect(mqtt_client_id, mqtt_username, mqtt_password)) {
      Serial.println("Connected!");
      client.subscribe("iot/control");
      client.subscribe("iot/mode");
    } else {
      Serial.print("Failed, rc=");
      Serial.print(client.state());
      Serial.println(" - Retrying in 2 seconds...");
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

  // Initialiser tous les actionneurs à OFF
  digitalWrite(FAN1, LOW);
  digitalWrite(FAN2, LOW);
  digitalWrite(BULB1, LOW);
  digitalWrite(BULB2, LOW);
  digitalWrite(PUMP, LOW);

  Serial.print("Connecting to WiFi");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(400);
    Serial.print(".");
  }
  Serial.println("\nWiFi connected!");

  espClient.setInsecure();

  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);
  reconnectMQTT();
}

// ==================== Main Loop ====================
void loop() {
  if (!client.connected()) reconnectMQTT();
  client.loop();

  unsigned long now = millis();
  
  // Si on est en mode automatique, appliquer la logique de contrôle
  if (!manualMode) {
    applyActuatorCommands();
  }
  
  // Envoyer les données périodiquement (toutes les 10 secondes)
  if (now - lastSendTime >= sendInterval) {
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

    // Lire les états actuels des actionneurs
    int fan1State = digitalRead(FAN1);
    int fan2State = digitalRead(FAN2);
    int bulb1State = digitalRead(BULB1);
    int bulb2State = digitalRead(BULB2);
    int pumpState = digitalRead(PUMP);

    // ==== SEND MQTT DATA ====
    char msg[256];
    snprintf(msg, sizeof(msg),
      "{\"TempIndoor\":%.2f,\"HumIndoor\":%.2f,\"TempOutdoor\":%.2f,\"HumOutdoor\":%.2f,"
      "\"Soil\":%d,\"Tank\":%d,\"pH\":%d,\"Light\":%d,\"Gas\":%d,"
      "\"Fan1\":%d,\"Fan2\":%d,\"Bulb1\":%d,\"Bulb2\":%d,\"Pump\":%d,"
      "\"Mode\":\"%s\"}",
      tempIndoor, humIndoor, tempOutdoor, humOutdoor,
      soil, tank, ph, light, gas,
      fan1State, fan2State, bulb1State, bulb2State, pumpState,
      manualMode ? "manual" : "auto");

    client.publish("iot/data", msg);
    Serial.println(msg);
  }
  
  // Petite pause pour éviter la surcharge CPU
  delay(50);
}