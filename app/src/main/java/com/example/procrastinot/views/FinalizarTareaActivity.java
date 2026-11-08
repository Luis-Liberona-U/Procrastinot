package com.example.procrastinot.views;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.procrastinot.models.RegistroTarea;
import com.example.procrastinot.repositories.RegistroTareaRepository;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Permite tomar la fotografía final de una tarea y guardar su resultado.
 *
 * Recupera de SQLite los tiempos reales para mostrar la duración. Al confirmar el resultado, guarda la ruta de la foto final, marca el registro como
 * completado y abre ResultadoTareaActivity.
 */
public class FinalizarTareaActivity extends AppCompatActivity {

    private long usuarioId;
    private long registroId;

    /** Ruta de la última foto final que se pudo cargar y mostrar. */
    private String rutaFotoDespues;
    /** Ruta del archivo de salida de una captura aun sin verificar. */
    private String rutaFotoPendiente;

    private boolean registroListo = false;
    private boolean guardando = false;

    private ImageView imgFotoDespues;
    private TextView txtTiempoFinal;
    private Button btnTomarFotoDespues;
    private Button btnGuardarResultado;

    /** Ejecuta las consultas y el guardado en SQLite fuera del hilo principal. */
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    /**
     * Recibe el resultado de la cámara y comprueba que la imagen pueda mostrarse.
     *
     * Si la nueva foto es válida, reemplaza la anterior. Si la captura falla o se cancela, intenta borrar el archivo pendiente, conserva la foto anterior y
     * muestra "No se guardó una nueva foto".
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
                            if (rutaFotoDespues != null
                                    && !rutaFotoDespues.equals(rutaFotoPendiente)) {
                                new File(rutaFotoDespues).delete();
                            }

                            rutaFotoDespues = rutaFotoPendiente;
                            btnTomarFotoDespues.setText("Repetir foto");

                        } else {
                            archivo.delete();

                            Toast.makeText(this, "No se guardó una nueva foto", Toast.LENGTH_SHORT).show();
                        }

                        rutaFotoPendiente = null;
                        actualizarBotones();
                    }
            );

    /** Abre la cámara si se concede el permiso y muestra un aviso si se deniega. */
    private final ActivityResultLauncher<String> permisoCamaraLauncher = registerForActivityResult(
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
     * Recibe el usuario y el registro por Intent, prepara los controles y solicita la carga de la realización.
     *
     * Si falta algún identificador, muestra "No se recibieron los datos de la realización" y cierra la pantalla.
     * Si Android recreó la pantalla, recupera las rutas guardadas e intenta restaurar la vista previa.
     *
     * @param savedInstanceState Estado guardado por Android o null si no existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_finalizar_tarea);

        usuarioId = getIntent().getLongExtra("usuario_id", -1);
        registroId = getIntent().getLongExtra("registro_id", -1);

        if (usuarioId == -1 || registroId == -1) {
            Toast.makeText(this, "No se recibieron los datos de la realización", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        TextView txtNombre = findViewById(R.id.txtNombreTareaFinalizar);

        txtTiempoFinal = findViewById(R.id.txtTiempoFinal);
        imgFotoDespues = findViewById(R.id.imgFotoDespues);

        btnTomarFotoDespues = findViewById(R.id.btnTomarFotoDespues);

        btnGuardarResultado = findViewById(R.id.btnGuardarResultado);

        String nombre = getIntent().getStringExtra("tarea_nombre");
        txtNombre.setText(nombre == null ? "Finalizar tarea" : nombre);

        txtTiempoFinal.setText("Consultando tiempo empleado…");

        if (savedInstanceState != null) {
            rutaFotoDespues = savedInstanceState.getString("foto_despues");

            rutaFotoPendiente = savedInstanceState.getString("foto_pendiente");

            if (rutaFotoDespues != null) {
                if (mostrarFoto(rutaFotoDespues)) {
                    btnTomarFotoDespues.setText("Repetir foto");
                } else {
                    rutaFotoDespues = null;
                }
            }
        }

        actualizarBotones();

        btnTomarFotoDespues.setOnClickListener(v -> comprobarPermisoCamara());
        btnGuardarResultado.setOnClickListener(v -> guardarResultado());

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

        cargarRegistro();
    }

    /**
     * Consulta la realización en segundo plano y muestra el tiempo empleado.
     *
     * Solo admite registros en estado EN_CURSO con inicio y fin reales guardados. Esto significa que el cronómetro ya terminó, pero todavía falta guardar la foto final.
     *
     * Si el registro no cumple esas condiciones o falla la consulta a SQLite, muestra el aviso de error correspondiente y cierra la pantalla.
     */
    private void cargarRegistro() {
        executor.execute(() -> {
            RegistroTareaRepository repository = new RegistroTareaRepository(getApplicationContext());

            try {
                RegistroTarea registro = repository.obtenerRegistro(registroId, usuarioId);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (registro == null
                            || registro.getInicioReal() == null
                            || registro.getFinReal() == null
                            || !RegistroTarea.EN_CURSO.equals(
                            registro.getEstado()
                    )) {
                        Toast.makeText(this, "La realización no está pendiente de foto final", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    long segundos = Math.max(
                            0L,
                            registro.getFinReal() - registro.getInicioReal()
                    ) / 1000;

                    String tiempo = String.format(
                            Locale.getDefault(),
                            "%02d:%02d:%02d",
                            segundos / 3600,
                            (segundos % 3600) / 60,
                            segundos % 60
                    );

                    txtTiempoFinal.setText("Tiempo empleado: " + tiempo);

                    registroListo = true;
                    actualizarBotones();
                });

            } catch (SQLiteException e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Toast.makeText(this, "No se pudo consultar la realización", Toast.LENGTH_LONG).show();

                    finish();
                });

            } finally {
                repository.cerrar();
            }
        });
    }

    /**
     * Crea un archivo JPG en la carpeta Pictures de la app y abre la cámara.
     *
     * Comparte la URI del archivo mediante FileProvider y concede acceso temporal a la aplicación de cámara.
     * Si no puede preparar el archivo o abrir la cámara, intenta borrar el archivo pendiente, actualiza los botones y muestra "No se pudo abrir la cámara".
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
                    "despues_",
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
                    ClipData.newRawUri("Foto final de la tarea", fotoUri)
            );

            actualizarBotones();
            cameraLauncher.launch(intent);

        } catch (IOException
                 | ActivityNotFoundException
                 | SecurityException
                 | IllegalArgumentException e) {

            if (rutaFotoPendiente != null) {
                new File(rutaFotoPendiente).delete();
                rutaFotoPendiente = null;
            }

            actualizarBotones();

            Toast.makeText(this, "No se pudo abrir la cámara", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Carga y muestra una vista previa de la foto final.
     *
     * Primero lee las dimensiones y luego ajusta el factor de muestreo para que las dimensiones estimadas no superen los 1200 píxeles.
     * Esto reduce la memoria necesaria para mostrar la imagen.
     *
     * @param ruta Ruta local del archivo de la fotografía.
     * @return true si pudo mostrar la imagen y false si las dimensiones no son válidas o no pudo decodificar el archivo.
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

        imgFotoDespues.setImageBitmap(foto);
        return true;
    }

    /**
     * Guarda la ruta de la foto final y completa el registro en SQLite.
     *
     * Solo procede si el registro está listo, existe una foto aceptada y no hay otra captura o guardado en curso.
     * Si el archivo ya no está disponible o está vacío, pide tomar la foto nuevamente.
     *
     * Al guardar correctamente, abre el resultado. Si SQLite falla o el repositorio no puede completar el registro muestra
     * "No se pudo guardar el resultado" y permite reintentar.
     */
    private void guardarResultado() {
        if (guardando || !registroListo
                || rutaFotoPendiente != null
                || rutaFotoDespues == null) {
            return;
        }

        File foto = new File(rutaFotoDespues);

        if (!foto.exists() || foto.length() == 0) {
            rutaFotoDespues = null;
            actualizarBotones();

            Toast.makeText(this, "Toma nuevamente la foto final", Toast.LENGTH_LONG).show();

            return;
        }

        final String fotoGuardar = rutaFotoDespues;

        guardando = true;
        actualizarBotones();

        executor.execute(() -> {
            RegistroTareaRepository repository = new RegistroTareaRepository(getApplicationContext());

            try {
                boolean completado = repository.completarRegistro(
                        registroId,
                        usuarioId,
                        fotoGuardar
                );

                if (!completado) {
                    throw new IllegalStateException("No se pudo completar el registro");
                }

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Toast.makeText(
                            this, "Tarea completada. Resultado guardado.", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(
                            FinalizarTareaActivity.this,
                            ResultadoTareaActivity.class
                    );

                    intent.putExtra("usuario_id", usuarioId);
                    intent.putExtra("registro_id", registroId);

                    startActivity(intent);
                    finish();
                });

            } catch (SQLiteException | IllegalStateException e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    guardando = false;
                    actualizarBotones();

                    Toast.makeText(this, "No se pudo guardar el resultado", Toast.LENGTH_LONG).show();
                });

            } finally {
                repository.cerrar();
            }
        });
    }

    /**
     * Actualiza los botones según el estado de la pantalla.
     *
     * Ambos quedan bloqueados durante la carga, la captura o el guardado.
     * Guardar resultado requiere además una foto final aceptada.
     */
    private void actualizarBotones() {
        boolean disponible = registroListo
                && !guardando
                && rutaFotoPendiente == null;

        btnTomarFotoDespues.setEnabled(disponible);

        btnGuardarResultado.setEnabled(
                disponible && rutaFotoDespues != null
        );
    }

    /** Abre la cámara si el permiso ya está concedido; si falta, lo solicita. */
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
     * Guarda las rutas de la foto aceptada y del archivo pendiente
     * para recuperarlas si Android recrea la pantalla.
     *
     * @param outState Estado en el que se guardarán las rutas.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("foto_despues", rutaFotoDespues);
        outState.putString("foto_pendiente", rutaFotoPendiente);
        super.onSaveInstanceState(outState);
    }

    /**
     * Deja de aceptar nuevas operaciones al destruirse la pantalla.
     * Las operaciones ya enviadas al ejecutor pueden terminar.
     */
    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}