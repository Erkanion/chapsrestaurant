# Chaps Thermal Printer

Aplicación Android nativa para buscar impresoras térmicas Bluetooth y enviar una impresión de prueba usando comandos ESC/POS.

## Funciones

- Solicita permisos Bluetooth modernos (`BLUETOOTH_SCAN` y `BLUETOOTH_CONNECT`) en Android 12+.
- Muestra impresoras/dispositivos Bluetooth emparejados.
- Busca dispositivos Bluetooth cercanos.
- Permite seleccionar una impresora y mandar un ticket de prueba térmico por SPP/RFCOMM.

## Abrir en Android Studio

1. Abre Android Studio.
2. Selecciona **Open** y elige esta carpeta del proyecto.
3. Espera a que Gradle sincronice el proyecto.
4. Conecta un teléfono Android real con Bluetooth habilitado.
5. Ejecuta la app, concede los permisos, presiona **Buscar impresoras Bluetooth**, selecciona la impresora y pulsa **Imprimir prueba térmica**.

> Nota: muchas impresoras térmicas Bluetooth requieren emparejarse primero desde los ajustes del teléfono usando un PIN como `0000` o `1234`.
