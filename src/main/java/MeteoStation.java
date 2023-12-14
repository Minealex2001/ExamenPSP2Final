import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;
/**
 * Esta clase representa una estación meteorológica que publica mediciones a un servidor MQTT.
 * Cada estación tiene un ID único y un cliente MQTT para publicar los mensajes.
 */
public class MeteoStation implements Runnable {
    private final int id;
    private final MqttClient publisher;

    /**
     * Constructor de la clase MeteoStation.
     * Inicializa el ID de la estación y establece la conexión con el servidor MQTT.
     *
     * @param id El ID de la estación meteorológica.
     * @throws MqttException Si ocurre un error al establecer la conexión con el servidor MQTT.
     */
    public MeteoStation(int id) throws MqttException {
        this.id = id;
        String publisherId = String.valueOf(id);
        publisher = new MqttClient("tcp://broker.emqx.io:1883", publisherId);
        MqttConnectOptions opciones = new MqttConnectOptions();
        opciones.setAutomaticReconnect(true);
        opciones.setCleanSession(true);
        opciones.setConnectionTimeout(10);
        publisher.connect(opciones);
    }

    /**
     * Este método se ejecuta cuando se inicia el hilo.
     * Genera mediciones meteorológicas aleatorias y las publica en el servidor MQTT cada 5 segundos.
     */
    @Override
    public void run() {
        Random random = new Random();
        String tema = String.format("/ALESAN/METEO/%s/MESUREMENTS", this.id);
        while (true) {
            // Generación del mensaje con la información meteorológica
            String mensaje = generateMessage(random);
            try {
                // Publicación del mensaje en el tema correspondiente
                publisher.publish(tema, new MqttMessage(mensaje.getBytes()));
                System.out.println("Publicado: " + mensaje + " de " + publisher.getClientId());
                Thread.sleep(5000);
            } catch (MqttException | InterruptedException e) {
                System.err.println("Error al publicar el mensaje");
            }
        }
    }

    /**
     * Genera un mensaje con una medición meteorológica aleatoria.
     *
     * @param random Un objeto Random para generar la medición aleatoria.
     * @return Un String con el mensaje a publicar.
     */
    private String generateMessage(Random random) {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String temperatura = String.valueOf(random.nextInt(40 - 10) + 10);
        return String.format("date=%s#hour%s#temperature%s", fecha, hora, temperatura);
    }
}
