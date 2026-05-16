# Chaps Thermal Printer

Aplicación Android nativa de POS para restaurante con login MySQL, menú principal de módulos y configuración de impresora térmica Bluetooth con prueba ESC/POS.

## Funciones

- Pantalla de login en español antes de abrir el POS.
- Cabecera superior minimalista con botón hamburguesa, usuario a la izquierda del icono de logout y texto de bienvenida debajo.
- Apartado **Configuracion de Negocio** para capturar uno o varios negocios por usuario.
- Autenticación contra MySQL mediante una API PHP con mysqli, sin PDO ni prepared statements.
- Guarda la sesión localmente y muestra el usuario antes del icono de logout para cerrar sesión.
- Solicita permisos Bluetooth modernos (`BLUETOOTH_SCAN` y `BLUETOOTH_CONNECT`) en Android 12+.
- Muestra impresoras/dispositivos Bluetooth emparejados.
- Busca dispositivos Bluetooth cercanos.
- Permite seleccionar una impresora y mandar un ticket de prueba térmico por SPP/RFCOMM.

## Abrir en Android Studio

1. Abre Android Studio.
2. Selecciona **Open** y elige esta carpeta del proyecto.
3. Espera a que Gradle sincronice el proyecto.
4. La app apunta por defecto a `http://192.168.1.16/chapsrestaurant/server/api/login.php`. Si tu API queda en otra ruta, cambia `LOGIN_URL` en `app/src/main/java/com/chapsrestaurant/thermalprinter/LoginActivity.java`.
5. Conecta un teléfono Android real con Bluetooth habilitado.
6. Ejecuta la app, inicia sesión, toca el botón **☰** de arriba para abrir el menú principal y selecciona una opción. Debajo de la cabecera verás el texto **Bienvenido**. Para probar la impresora entra en **Configuracion de Impresora**, concede los permisos, presiona **Buscar impresoras Bluetooth**, selecciona la impresora y pulsa **Imprimir prueba térmica**.

> Nota: muchas impresoras térmicas Bluetooth requieren emparejarse primero desde los ajustes del teléfono usando un PIN como `0000` o `1234`.

## Menu principal del POS

El menú se abre con el botón de hamburguesa **☰** ubicado hasta arriba. El texto **POS Restaurante** aparece dentro del menú. En la parte superior derecha aparece el usuario conectado a la izquierda del icono de logout. El diseño usa una paleta minimalista con fondo claro, superficies blancas, texto oscuro, gris neutro y un acento verde discreto. El menú contiene estas opciones:

- Dashboard
- Configuracion de Negocio
- Configuracion de Impresora
- Menu
- Informes y Estadisticas
- Corte de Caja
- Categorias
- Productos
- Ingredientes
- Nueva Comanda
- Mesa y Ordenes
- Ordenes
- Cocina
- Gestion de Meseros
- Gestion de Usuarios
- Gestios de Mesas

## Configuracion de Negocio

Desde el menu principal entra a **Configuracion de Negocio** para administrar uno o varios negocios del usuario actual. Cada negocio permite capturar:

- Nombre de la Tienda / Negocio
- Razon Social
- RFC
- Regimen Fiscal
- Telefono
- Direccion Completa
- Correo Electronico
- Slogan
- Logo

La pantalla incluye navegacion entre negocios, boton para agregar otro negocio y guardado local por usuario. La base de datos tambien incluye la tabla `businesses` para soportar la relacion de un usuario con multiples negocios.

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
6. Verifica que `login.php` responda JSON en `http://192.168.1.16/chapsrestaurant/server/api/login.php` o ajusta `LOGIN_URL` si tu endpoint queda en otra ruta.
