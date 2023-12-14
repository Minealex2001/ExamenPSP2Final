import redis.clients.jedis.Jedis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * La clase MeteoClient permite interactuar con la información del sistema meteorológico
 * almacenada en una base de datos Redis a través de la consola.
 */
public class MeteoClient {
    private static final String FORMATO_ULTIMA_MEDIDA = "ABC:LASTMEASUREMENT:%s";
    private static final String FORMATO_TEMPERATURAS = "ABC:TEMPERATURES:%s";
    private static final String LLAVE_ALERTAS = "ABC:ALERTS";

    /**
     * El método principal de la aplicación. Se encarga de establecer la conexión con la base de datos Redis,
     * y de iniciar un bucle que espera la entrada del usuario para ejecutar comandos.
     *
     * @param args Los argumentos de la línea de comandos. No se utilizan en esta aplicación.
     * @throws IOException Si ocurre un error al leer la entrada del usuario.
     */
    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String command;

        try (Jedis jedis = new Jedis("184.73.34.167", 6379)) {
            while (true) {
                System.out.println("Enter command:");
                System.out.println("LAST ID. Muestra las últimas medidas de la estación meteorológica con ese ID.");
                System.out.println("MAXTEMP ID. Muestra la temperatura más alta de la estación meteorológica con ese ID.");
                System.out.println("MAXTEMP ALL. Muestra la temperatura más alta del sistema (busca en todas las estaciones meteorológicas).");
                System.out.println("ALERTS. Muestra las alertas actuales y las elimina.");
                System.out.println("EXIT");
                command = reader.readLine();

                if (command.startsWith("LAST")) {
                    String id = command.split(" ")[1];
                    Map<String, String> lastMeasurement = jedis.hgetAll(String.format(FORMATO_ULTIMA_MEDIDA, id));
                    System.out.println("Last measurement for station " + id + ": " + lastMeasurement);
                } else if (command.startsWith("MAXTEMP")) {
                    String id = command.split(" ")[1];
                    List<String> temperatures = jedis.lrange(String.format(FORMATO_TEMPERATURAS, id), 0, -1);
                    System.out.println("Max temperature for station " + id + ": " + temperatures.stream().mapToDouble(Double::parseDouble).max().orElse(Double.NaN));
                } else if (command.equals("MAXTEMP ALL")) {
                    Set<String> keys = jedis.keys("ABC:TEMPERATURES:*");
                    double maxTemp = keys.stream()
                            .flatMap(key -> jedis.lrange(key, 0, -1).stream())
                            .mapToDouble(Double::parseDouble)
                            .max().orElse(Double.NaN);
                    System.out.println("Max temperature for all stations: " + maxTemp);
                } else if (command.equals("ALERTS")) {
                    Set<String> alerts = jedis.smembers(LLAVE_ALERTAS);
                    alerts.forEach(System.out::println);
                    jedis.del(LLAVE_ALERTAS);
                } else if (command.equals("EXIT")) {
                    break;
                } else {
                    System.out.println("Unknown command");
                }
            }
        }
    }
}