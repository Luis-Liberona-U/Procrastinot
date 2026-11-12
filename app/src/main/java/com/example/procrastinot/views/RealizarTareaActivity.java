package com.example.procrastinot.views;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;
import android.database.sqlite.SQLiteException;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.procrastinot.R;
import com.example.procrastinot.repositories.RegistroTareaRepository;
import com.example.procrastinot.security.SesionManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Muestra los datos de una tarea y permite tomar su fotografía inicial.
 *
 * Al pulsar Iniciar tarea con una foto válida, registra el inicio real en SQLite y abre TareaEnCursoActivity}.
 * La fotografía queda como archivo en el dispositivo, el registro guarda su ruta.
 */
public class RealizarTareaActivity extends AppCompatActivity {

    private ImageView imgFotoAntes;
    private Button btnTomarFotoAntes;
    private Button btnIniciarTarea;

    /** Ruta de la última foto que se pudo cargar y mostrar. */
    private String rutaFotoAntes;
    /** Ruta del archivo de salida de una captura aún sin verificar. */
    private String rutaFotoPendiente;

    private long usuarioId;
    private long tareaId;
    private long inicioProgramado;

    /** Guarda el registro de inicio fuera del hilo principal. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();


    /**
     * Recibe el resultado de la cámara y verifica el archivo generado.
     *
     * Acepta la nueva foto si la cámara informa éxito, el archivo contiene datos y la imagen puede mostrarse.
     * Si falla o se cancela la captura, elimina el archivo pendiente, conserva la foto aceptada y muestra, "No se guardó una nueva foto".
     */
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (rutaFotoPendiente == null) {
                            return;
                        }

                        File archivo = new File(rutaFotoPendiente);

                        boolean capturada =
                                result.getResultCode() == RESULT_OK
                                        && archivo.exists()
                                        && archivo.length() > 0;

                        if (capturada && mostrarFoto(rutaFotoPendiente)) {

                            if (rutaFotoAntes != null
                                    && !rutaFotoAntes.equals(rutaFotoPendiente)) {
                                new File(rutaFotoAntes).delete();
                            }

                            rutaFotoAntes = rutaFotoPendiente;

                            btnIniciarTarea.setEnabled(true);
                            btnTomarFotoAntes.setText("Repetir foto");

                        } else {
                            archivo.delete();

                            Toast.makeText(this, "No se guardó una nueva foto", Toast.LENGTH_SHORT).show();
                        }

                        rutaFotoPendiente = null;
                        btnTomarFotoAntes.setEnabled(true);
                    }
            );

    /**
     * Verifica la foto inicial y registra el comienzo de la realización.
     *
     * No inicia si hay una captura pendiente. Si falta la foto o el archivo ya no está disponible, avisa al usuario y permanece en la pantalla.
     *
     * Si el repositorio guarda el registro, abre la pantalla del cronómetro.
     * Si la operación falla en SQLite o el repositorio rechaza la solicitud, vuelve a habilitar los botones y muestra un aviso.
     */
    private void iniciarTarea() {
        if (rutaFotoPendiente != null) {
            return;
        }

        if (rutaFotoAntes == null) {
            Toast.makeText(this, "Primero toma una foto", Toast.LENGTH_SHORT).show();
            return;
        }

        File foto = new File(rutaFotoAntes);

        if (!foto.exists() || foto.length() == 0) {
            Toast.makeText(this, "La foto no está disponible. Tómala nuevamente.", Toast.LENGTH_LONG).show();
            return;
        }

        final String fotoGuardar = rutaFotoAntes;
        final long inicioReal = System.currentTimeMillis();

        btnIniciarTarea.setEnabled(false);
        btnTomarFotoAntes.setEnabled(false);

        executor.execute(() -> {
            RegistroTareaRepository repository = new RegistroTareaRepository(getApplicationContext());

            try {
                long registroId = repository.iniciarRegistro(
                        tareaId,
                        usuarioId,
                        inicioProgramado,
                        inicioReal,
                        fotoGuardar
                );

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Intent intent = new Intent(
                            RealizarTareaActivity.this,
                            TareaEnCursoActivity.class
                    );

                    intent.putExtra("usuario_id", usuarioId);
                    intent.putExtra("registro_id", registroId);
                    intent.putExtra("inicio_real", inicioReal);
                    intent.putExtra(
                            "tarea_nombre",
                            getIntent().getStringExtra("tarea_nombre")
                    );

                    startActivity(intent);
                    finish();
                });

            } catch (SQLiteException | IllegalArgumentException e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    btnIniciarTarea.setEnabled(true);
                    btnTomarFotoAntes.setEnabled(true);

                    Toast.makeText(this, "No se pudo iniciar. La tarea puede no estar disponible o esta realización ya está registrada.", Toast.LENGTH_LONG).show();
                });

            } finally {
                repository.cerrar();
            }
        });
    }

    /**
     * Abre la cámara si se concede el permiso. Si se deniega, indica que debe permitirse para poder tomar la foto.
     */
    private final ActivityResultLauncher<String> permisoCamaraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    concedido -> {
                        if (concedido) {
                            abrirCamara();
                        } else {
                            Toast.makeText(this, "Necesitas permitir la cámara para tomar la foto.", Toast.LENGTH_LONG).show();
                        }
                    }
            );

    /**
     * Recibe los datos de la tarea y prepara la captura de la foto inicial.
     *
     * Si faltan el usuario, la tarea o el inicio programado, muestra "No se recibieron los datos de la tarea" y cierra la pantalla.
     * Al recrearse, recupera las rutas guardadas y vuelve a mostrar la foto anterior si puede cargarla.
     *
     * @param savedInstanceState Estado guardado por Android al recrear la pantalla, o null si no existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_realizar_tarea);

        usuarioId = getIntent().getLongExtra("usuario_id", -1);

        tareaId = getIntent().getLongExtra("tarea_id", -1);
        inicioProgramado = getIntent().getLongExtra("inicio_programado", -1);

        if (usuarioId == -1 || tareaId == -1 || inicioProgramado == -1) {
            Toast.makeText(this, "No se recibieron los datos de la tarea", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (new SesionManager(this).obtenerUsuarioId() != usuarioId) {
            Toast.makeText(this, "Inicia sesión con la cuenta de esta tarea", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        TextView txtNombre = findViewById(R.id.txtNombreTareaRealizar);
        TextView txtDescripcion =
                findViewById(R.id.txtDescripcionTareaRealizar);

        String nombre = getIntent().getStringExtra("tarea_nombre");
        String descripcion = getIntent().getStringExtra("tarea_descripcion");

        txtNombre.setText(nombre == null ? "Realizar tarea" : nombre);

        txtDescripcion.setText(
                descripcion == null || descripcion.trim().isEmpty()
                        ? "Sin descripción"
                        : descripcion
        );
        imgFotoAntes = findViewById(R.id.imgFotoAntes);
        btnTomarFotoAntes = findViewById(R.id.btnTomarFotoAntes);
        btnIniciarTarea = findViewById(R.id.btnIniciarTarea);

        btnIniciarTarea.setEnabled(false);


        if (savedInstanceState != null) {
            rutaFotoAntes = savedInstanceState.getString("foto_antes");
            rutaFotoPendiente = savedInstanceState.getString("foto_pendiente");

            if (rutaFotoAntes != null && mostrarFoto(rutaFotoAntes)) {
                btnIniciarTarea.setEnabled(true);
                btnTomarFotoAntes.setText("Repetir foto");
            }

            btnTomarFotoAntes.setEnabled(rutaFotoPendiente == null);
        }

        btnTomarFotoAntes.setOnClickListener(v -> comprobarPermisoCamara());

        btnIniciarTarea.setOnClickListener(v -> iniciarTarea());

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }


    /**
     * Prepara un archivo JPG y abre una aplicación de cámara para sacar la fotografía.
     *
     * Crea el archivo en la carpeta Pictures de la app y comparte su URI mediante FileProvider.
     * Si no puede preparar el archivo o abrir la cámara, intenta borrar el archivo pendiente, habilita nuevamente el botón de captura y muestra un aviso.
     */
    private void abrirCamara() {
        try {
            File carpeta = getExternalFilesDir(
                    Environment.DIRECTORY_PICTURES
            );

            if (carpeta == null) {
                throw new IOException("Almacenamiento no disponible");
            }

            File archivo = File.createTempFile(
                    "antes_",
                    ".jpg",
                    carpeta
            );

            rutaFotoPendiente = archivo.getAbsolutePath();

            Uri fotoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    archivo
            );

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            intent.setClipData(
                    ClipData.newRawUri("Foto de la tarea", fotoUri)
            );

            btnTomarFotoAntes.setEnabled(false);
            cameraLauncher.launch(intent);

        } catch (IOException
                 | ActivityNotFoundException
                 | SecurityException
                 | IllegalArgumentException e) {

            if (rutaFotoPendiente != null) {
                new File(rutaFotoPendiente).delete();
                rutaFotoPendiente = null;
            }

            btnTomarFotoAntes.setEnabled(true);
            Toast.makeText(this, "No se pudo abrir la cámara. Revisa su configuración.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Carga y muestra una vista previa de la foto inicial.
     *
     * Lee primero las dimensiones sin cargar los píxeles. Aumenta el factor de muestreo hasta que las dimensiones estimadas no superen 1200 píxeles, para reducir el uso de memoria.
     *
     * @param ruta Ruta local del archivo de la fotografía.
     * @return true si la imagen se pudo mostrar y false si sus dimensiones no son válidas o no se pudo decodificar.
     */
    private boolean mostrarFoto(String ruta) {
        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(ruta, opciones);

        if (opciones.outWidth <= 0 || opciones.outHeight <= 0) {
            return false;
        }

        opciones.inSampleSize = 1;

        while (opciones.outWidth / opciones.inSampleSize > 1200
                || opciones.outHeight / opciones.inSampleSize > 1200) {
            opciones.inSampleSize *= 2;
        }

        opciones.inJustDecodeBounds = false;

        Bitmap foto = BitmapFactory.decodeFile(ruta, opciones);

        if (foto == null) {
            return false;
        }

        imgFotoAntes.setImageBitmap(foto);
        return true;
    }

    /**
     * Abre la cámara si el permiso ya está concedido, si falta lo solicita.
     */
    private void comprobarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Guarda las rutas de la foto aceptada y de la captura pendiente para recuperarlas si Android recrea la pantalla.
     *
     * @param outState Estado en el que se guardarán las rutas.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("foto_antes", rutaFotoAntes);
        outState.putString("foto_pendiente", rutaFotoPendiente);

        super.onSaveInstanceState(outState);
    }

    /**
     * Deja de aceptar nuevas operaciones cuando se destruye la pantalla.
     * Las operaciones ya enviadas al ejecutor pueden terminar.
     */
    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}