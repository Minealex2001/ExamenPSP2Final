import org.eclipse.paho.client.mqttv3.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * La clase MeteoServer se encarga de recibir los mensajes MQTT de las estaciones meteorológicas,
 * procesarlos y almacenar la información en una base de datos Redis.
 */
public class MeteoServer {
    // Definición de las claves para las consultas a la base de datos
    private static final String FORMATO_ULTIMA_TEMPERATURA = "ALEJANDRO:LASTMEASUREMENT:%s";
    private static final String FORMATO_TEMPERATURAS = "ALEJANDRO:TEMPERATURES:%s";
    private static final String LLAVE_ALERTAS = "ALEJANDRO:ALERTS";

    /**
     * El método principal de la aplicación. Se encarga de establecer las conexiones con el servidor MQTT y Redis,
     * y de iniciar las estaciones meteorológicas.
     *
     * @param args Los argumentos de la línea de comandos. No se utilizan en esta aplicación.
     * @throws MqttException Si ocurre un error al establecer la conexión con el servidor MQTT.
     */
    public static void main(String[] args) throws MqttException {
        // Creación de un pool de hilos para manejar las estaciones meteorológicas
        int hilosMaximos = 10;
        ExecutorService executor = Executors.newFixedThreadPool(hilosMaximos);
        for (int i = 0; i < hilosMaximos; i++) {
            executor.execute(new MeteoStation(i));
        }
        // Creación del cliente MQTT
        String publisherId = UUID.randomUUID().toString();
        try (MqttClient publisher = new MqttClient("tcp://broker.emqx.io:1883", publisherId)) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            publisher.connect(options);

            // Configuración del callback para manejar los mensajes MQTT

            publisher.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                    System.out.println("Connection lost! " + throwable.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) {
                    handleMessageArrived(topic, mqttMessage);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                }
            });

            // Suscribirse a los mensajes de Redis
            JedisPubSub jedisPubSub = new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    if (channel.equals("STOP_CHANNEL")) {
                        String id = message.split(":")[1];
                        // Publicar el mensaje en MQTT
                        String topic = "/ALESAN/METEO/" + id + "/STOP";
                        try {
                            publisher.publish(topic, new MqttMessage("STOP".getBytes()));
                        } catch (MqttException e) {
                            System.err.println("Error al publicar el mensaje STOP");
                        }
                    }
                }
            };

            try (Jedis jedis = new Jedis("184.73.34.167", 6379)) {
                jedis.subscribe(jedisPubSub, "STOP_CHANNEL");
            }

            // Suscripción al topic de las estaciones meteorológicas
            publisher.subscribe("/ALESAN/METEO/#", 0);
        } catch (MqttException e) {
            System.err.println("Error al conectar con el servidor MQTT");
        }
    }

    /**
     * Este método se encarga de manejar los mensajes MQTT cuando llegan. Extrae la información del mensaje,
     * la procesa y la almacena en la base de datos Redis.
     *
     * @param topic El topic del mensaje MQTT.
     * @param mqttMessage El mensaje MQTT.
     */
    private static void handleMessageArrived(String topic, MqttMessage mqttMessage) {
        String idMeteo = topic.split("/")[3];
        String KeyHash = String.format(FORMATO_ULTIMA_TEMPERATURA, idMeteo);
        String KeyList = String.format(FORMATO_TEMPERATURAS, idMeteo);
        String[] splitMessage = new String(mqttMessage.getPayload()).split("#");
        String dateTime = splitMessage[0] + " " + splitMessage[1];
        String temperatura = splitMessage[2];
        // Conexión a la base de datos
        try (Jedis jedis = new Jedis("184.73.34.167", 6379)) {
            jedis.del(KeyHash);
            jedis.hset(KeyHash, dateTime, temperatura);
            jedis.rpush(KeyList, temperatura);
            // Generación de alertas en caso de temperaturas extremas
            if (Integer.parseInt(temperatura) > 30 || Integer.parseInt(temperatura) < 0) {
                jedis.rpush(LLAVE_ALERTAS, String.format("Alerta por temperaturas extremas el %s a las %s en la estación %s", splitMessage[0], splitMessage[1], idMeteo));
            }
        }
    }
}