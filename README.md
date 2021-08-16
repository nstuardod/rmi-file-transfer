RMI-File-Transfer
Servidor de archivos con RMI y sockets
Fecha original: Semestre 1/2018

# DESCRIPCIÓN GENERAL:

El proyecto consiste en un sistema para administrar archivos a nivel básico
en un servidor de manera remota, al estilo FTP (con limitaciones). La
comunicación de control entre cliente y servidor se realiza via Java RMI
mientras que la transferencia de archivos se efectúa via sockets.

Se proveen dos aplicaciones:
* Un servidor de RMI (rmi-server.jar)
* Un cliente de RMI (rmi-client.jar)

El cliente RMI contiene dos versiones: una versión gráfica en Swing y una de
modo texto para efectuar pruebas.

# COMPILACIÓN:
## Requisitos
1. Java JDK versión 8 (puede funcionar con JDK 7 pero no ha sido probado).
2. Apache Ant

## Instrucciones
1. Extraer el paquete en un directorio vacío.
2. En un intérprete de comandos o terminal, ejecutar `ant dist`
(para ver una lista de objetivos: `ant -p`)

# EJECUCIÓN
Una vez compilado y empaquetados las aplicaciones, podrán ejecutarse desde
el directorio `dist` mediante un intérprete de comandos o de forma gráfica.
## Servidor
Ejecutar, en el directorio donde quiere que se ejecute el servidor:
```
java -jar rmi-server-XXXXXXXX.jar
```
Luego debe crear un usuario con `adduser <nombre>`, tome nota de la contraseña.
Digite `help` para ver los comandos disponibles.
## Cliente
Ejecutar en un terminal:
```
java -jar rmi-client-XXXXXXXX.jar
```

En algunos SOs el cliente puede iniciarse haciendo doble clic en el JAR 
respectivo (puede ser necesario fijar el permiso de ejecución). En ambos casos
se cargará la versión gráfica del cliente. Para cargar la versión de texto:
```
java -jar rmi-client-XXXXXXXX.jar -t <IP o nombre servidor>
```

# OBSERVACIONES (o, ¿por qué no puedo conectar a un servidor remoto?)
RMI usa la dirección IP del host del servidor como nombre de servidor en
referencias remotas(*1), si no se tiene configurado bien el nombre de host,
Java usará y publicará *localhost* y 127.0.0.1 (o 127.0.1.1) como dirección IP.
¿Resultado? Al conectarse desde fuera, terminará con una excepción de
conexión rechazada, pero a nuestra máquina!

SOLUCIÓN: Anteponer a la línea de comando del servidor
```-Djava.rmi.server.hostname=<nombre o IP servidor>```


(*1) [https://courses.cs.washington.edu/courses/cse341/98au/java/jdk1.2beta4/docs/guide/rmi/rmiNetworkingFAQ.html]

## LIMITACIONES
* No se puede mover archivos ni directorios entre directorios mediante la GUI.
* Sólo se permite transferencia de archivos.
