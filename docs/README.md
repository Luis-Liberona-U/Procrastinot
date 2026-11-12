# Procrastinot

Procrastinot es una aplicación Android para organizar tareas y registrar su realización mediante fotografías de antes y después y el tiempo empleado.

El proyecto busca ayudar al usuario a organizar sus actividades y llevar un historial de las tareas que ha completado.

## Ficha del proyecto

- **Nombre:** Procrastinot.
- **Asignatura:** Programación Android (IEI-093).
- **Plataforma:** Android.
- **Lenguaje:** Java.
- **Base de datos:** SQLite.
- **Estado:** En desarrollo.

## Funcionalidades implementadas

- - Registro de cuentas en SQLite e inicio de sesión con verificación de contraseñas mediante Argon2id.
- Pantalla principal con el nombre del usuario y su próxima tarea activa.
- Creación de tareas con:
    - Nombre.
    - Descripción opcional.
    - Hora de inicio.
    - Duración estimada.
    - Repetición: una vez, diariamente, lunes a viernes o días personalizados.
- Listado de tareas asociadas al usuario conectado.
- Edición de tareas en una pantalla independiente.
- Eliminación lógica de tareas para conservar sus registros históricos.
- Captura obligatoria de una fotografía antes de iniciar.
- Contador del tiempo transcurrido durante la realización.
- Captura obligatoria de una fotografía después de finalizar.
- Pantalla de resultados con ambas fotografías y el tiempo empleado.
- Historial de realizaciones completadas con acceso a sus resultados.
- Solicitud del permiso de cámara durante el uso de la aplicación.
- Protección mediante `FLAG_SECURE` en inicio de sesión y registro.

## Flujo de uso

1. Crear una cuenta e iniciar sesión.
2. Crear una tarea indicando su horario y repetición.
3. Consultar la próxima tarea desde la pantalla principal.
4. Pulsar Comenzar tarea y tomar la fotografía inicial.
5. Confirmar la fotografía para registrar el inicio.
6. Realizar la actividad y pulsar Finalizar tarea.
7. Tomar y confirmar la fotografía final.
8. Consultar el resultado y acceder posteriormente desde Historial.

La duración se calcula entre la confirmación del inicio y la pulsación de Finalizar tarea. El tiempo utilizado para tomar la fotografía final no se incluye.

## Tecnologías utilizadas

- Java.
- Android SDK.
- Android Studio.
- XML y ConstraintLayout.
- Material Components.
- RecyclerView, Adapter y ViewHolder.
- SQLite y SQLiteOpenHelper.
- FileProvider y Activity Result API.
- ExecutorService y Handler.
- Argon2Kt para generar y verificar hashes Argon2id.

## Organización del proyecto

- `views`: Activities que gestionan las pantallas y las acciones del usuario.
- `models`: clases que representan usuarios, tareas y realizaciones.
- `repositories`: operaciones de consulta y modificación de SQLite.
- `database`: Contract y Helper de la base de datos.
- `adapters`: conexión de los datos con las listas RecyclerView.
- `res/layout`: diseños XML de las pantallas y filas de las listas.
- `docs`: diagramas y documentación complementaria.

## Persistencia de datos

La aplicación utiliza una base de datos local llamada `procrastinot.db`.

Sus tablas principales son:

| Tabla | Propósito |
|---|---|
| usuarios | Almacenar las cuentas de usuario. |
| tareas | Guardar la configuración, propietario y estado de cada tarea. |
| tarea_dias | Guardar los días de repetición semanal. |
| registro_tarea | Registrar cada realización, sus tiempos, estado y fotografías. |

`ProcrastinotContract` define los nombres de tablas y columnas.

`ProcrastinotDbHelper` administra la creación y actualización de la base de datos.

Los repositorios utilizan ContentValues, consultas parametrizadas y Cursor para trabajar con los datos. Las operaciones de base de datos se ejecutan en segundo plano para evitar bloquear la interfaz.

Las tareas se eliminan de forma lógica mediante el atributo `eliminada`, conservando los registros necesarios para el historial.

## Almacenamiento de fotografías

Las fotografías se guardan como archivos en la carpeta Pictures específica de la aplicación.

SQLite almacena las rutas de esos archivos en las columnas `foto_antes` y `foto_despues`. Cada realización dispone de sus propias fotografías.

FileProvider permite compartir temporalmente el archivo de destino con la aplicación de cámara.

La aplicación no implementa sincronización ni respaldo propio en la nube. Las fotografías y la base de datos se almacenan localmente; la restauración mediante copias del sistema depende de la configuración de respaldo de Android y de la aplicación.

## Permisos

La aplicación declara y solicita el permiso CAMERA antes de capturar fotografías.

Si el usuario rechaza el permiso, se informa que debe habilitarlo para continuar con la captura.

El almacenamiento de fotografías en la carpeta específica de la aplicación no requiere permisos generales de almacenamiento en las versiones de Android utilizadas por el proyecto.

## Seguridad

Los repositorios utilizan consultas parametrizadas y `ContentValues` para trabajar con SQLite sin incorporar directamente los valores ingresados por el usuario en las sentencias SQL.

La captura de fotografías utiliza `FileProvider` para compartir temporalmente una URI del archivo de destino con la cámara.

Las pantallas de inicio de sesión y registro utilizan `FLAG_SECURE` para proteger su contenido frente a capturas de pantalla.

Las contraseñas de las cuentas nuevas se guardan en SQLite como hashes
Argon2id, con una sal aleatoria por cuenta. Al iniciar sesión, la aplicación
busca la cuenta por correo y verifica la contraseña ingresada con el hash
almacenado. Las cuentas creadas con el formato anterior no se migran;
para probar esta versión, registra una cuenta nueva.

## Diagramas

Los diagramas se encuentran en la carpeta `docs`. 
## Documentación del código

El código incluye comentarios JavaDoc que describen las responsabilidades de las clases y los métodos principales.

Se documentan los parámetros mediante `@param`, los valores de retorno mediante `@return` y las excepciones que pueden propagarse mediante `@throws`, cuando corresponde.


## Pruebas básicas

- Registrar una cuenta e iniciar sesión.
- Comprobar los mensajes para correo no registrado y contraseña incorrecta.
- Crear una tarea y verificar que aparezca en Mis tareas.
- Editar sus datos y comprobar que se conserven al volver a entrar.
- Eliminar una tarea con realizaciones y comprobar que su historial se conserve.
- Probar la aceptación y el rechazo del permiso de cámara.
- Comprobar que no se permita iniciar una tarea sin una fotografía inicial.
- Completar una tarea con las fotografías inicial y final.
- Consultar ambas fotografías y el tiempo empleado desde el historial.
- Intentar tomar capturas en inicio de sesión y registro para comprobar `FLAG_SECURE`.

## Estado del proyecto

Proyecto en desarrollo. Entre las funcionalidades pendientes de completar o verificar se encuentran:

- Notificaciones previas y alarmas de inicio.
- Persistencia del cambio de activación o desactivación de tareas.
- Recuperación de realizaciones pendientes desde Principal.
- Exclusión de realizaciones ya completadas al seleccionar la próxima tarea.

## Cómo ejecutar el proyecto

1. Clona o descarga el repositorio.
2. Ábrelo en Android Studio y sincroniza Gradle.
3. Conecta un teléfono Android con depuración USB o inicia un emulador.
   La aplicación requiere Android 10 (API 29) o superior.
4. Ejecuta la app y registra una cuenta nueva.
