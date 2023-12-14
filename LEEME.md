# Proyecto de Estaciones Meteorológicas

Este proyecto consiste en un sistema de estaciones meteorológicas que publican mediciones a un servidor MQTT. Las mediciones son luego procesadas y almacenadas en una base de datos Redis. El sistema está compuesto por tres componentes principales: las estaciones meteorológicas (`MeteoStation.java`), el servidor (`MeteoServer.java`) y el cliente (`MeteoClient.java`).

## MeteoStation

La clase `MeteoStation` representa una estación meteorológica individual. Cada estación tiene un ID único y un cliente MQTT para publicar los mensajes. La estación genera mediciones meteorológicas aleatorias y las publica en el servidor MQTT cada 5 segundos.

## MeteoServer

La clase `MeteoServer` se encarga de recibir los mensajes MQTT de las estaciones meteorológicas, procesarlos y almacenar la información en una base de datos Redis. El servidor se suscribe a los mensajes de las estaciones meteorológicas y, cuando recibe un mensaje, extrae la información del mensaje y la almacena en la base de datos Redis.

## MeteoClient

La clase `MeteoClient` permite interactuar con la información del sistema meteorológico almacenada en una base de datos Redis a través de la consola. El cliente espera la entrada del usuario para ejecutar comandos que recuperan y muestran la información almacenada en la base de datos Redis.

## Comandos del Cliente

El cliente soporta los siguientes comandos:

- `LAST ID`: Muestra las últimas medidas de la estación meteorológica con ese ID.
- `MAXTEMP ID`: Muestra la temperatura más alta de la estación meteorológica con ese ID.
- `MAXTEMP ALL`: Muestra la temperatura más alta del sistema (busca en todas las estaciones meteorológicas).
- `ALERTS`: Muestra las alertas actuales y las elimina.

## Configuración y Ejecución

Para ejecutar el sistema, necesitarás tener instalado Java y Maven en tu máquina. También necesitarás tener acceso a un servidor MQTT y a una base de datos Redis.

Para compilar y ejecutar el sistema, puedes usar los siguientes comandos:

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="MeteoServer"
mvn exec:java -Dexec.mainClass="MeteoClient"
```

Por favor, asegúrate de actualizar las direcciones del servidor MQTT y Redis en el código antes de ejecutar el sistema.