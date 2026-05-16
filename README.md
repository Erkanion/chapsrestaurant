# Chaps Thermal Printer

Aplicación Android nativa para iniciar sesión contra una base de datos MySQL, buscar impresoras térmicas Bluetooth y enviar una impresión de prueba usando comandos ESC/POS.

## Funciones

- Pantalla de login en español antes de abrir la impresora.
- Autenticación contra MySQL mediante una API PHP segura con consultas preparadas.
- Guarda la sesión localmente y permite cerrar sesión desde la pantalla de impresión.
- Solicita permisos Bluetooth modernos (`BLUETOOTH_SCAN` y `BLUETOOTH_CONNECT`) en Android 12+.
- Muestra impresoras/dispositivos Bluetooth emparejados.
- Busca dispositivos Bluetooth cercanos.
- Permite seleccionar una impresora y mandar un ticket de prueba térmico por SPP/RFCOMM.

## Abrir en Android Studio

1. Abre Android Studio.
2. Selecciona **Open** y elige esta carpeta del proyecto.
3. Espera a que Gradle sincronice el proyecto.
4. Cambia `LOGIN_URL` en `app/src/main/java/com/chapsrestaurant/thermalprinter/LoginActivity.java` por la URL real de tu API, por ejemplo `https://midominio.com/api/login.php`.
5. Conecta un teléfono Android real con Bluetooth habilitado.
6. Ejecuta la app, inicia sesión, concede los permisos, presiona **Buscar impresoras Bluetooth**, selecciona la impresora y pulsa **Imprimir prueba térmica**.

> Nota: muchas impresoras térmicas Bluetooth requieren emparejarse primero desde los ajustes del teléfono usando un PIN como `0000` o `1234`.

## Configurar MySQL y API de login

La app no se conecta directamente a MySQL desde Android porque exponer usuario y contraseña de la base de datos dentro de la APK no es seguro. En su lugar, Android llama a `server/api/login.php`, y ese archivo valida el usuario en MySQL.

1. Crea la base de datos ejecutando `server/database/schema.sql` en tu servidor MySQL.
2. Copia la carpeta `server/api` a tu hosting o servidor PHP.
3. La conexión incluida en `server/api/config.php` queda configurada para MySQL local: servidor `localhost`, usuario `root` y contraseña vacía. Cambia esos valores si tu servidor usa credenciales diferentes.
4. Crea usuarios guardando la contraseña con `password_hash()` de PHP. Ejemplo:

```bash
php -r "echo password_hash('123456', PASSWORD_DEFAULT) . PHP_EOL;"
```

5. Inserta el hash generado en la columna `password_hash` de la tabla `users`.
6. Verifica que `login.php` responda JSON y configura esa URL en `LOGIN_URL` dentro de la app.
