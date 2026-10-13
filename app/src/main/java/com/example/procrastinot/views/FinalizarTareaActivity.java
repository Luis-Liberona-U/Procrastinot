package com.example.procrastinot.views;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
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

public class FinalizarTareaActivity extends AppCompatActivity {

    private long usuarioId;
    private long registroId;

    private String rutaFotoDespues;
    private String rutaFotoPendiente;

    private boolean registroListo = false;
    private boolean guardando = false;

    private ImageView imgFotoDespues;
    private TextView txtTiempoFinal;
    private Button btnTomarFotoDespues;
    private Button btnGuardarResultado;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

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

        TextView txtNombre =
                findViewById(R.id.txtNombreTareaFinalizar);

        txtTiempoFinal = findViewById(R.id.txtTiempoFinal);
        imgFotoDespues = findViewById(R.id.imgFotoDespues);

        btnTomarFotoDespues =
                findViewById(R.id.btnTomarFotoDespues);

        btnGuardarResultado =
                findViewById(R.id.btnGuardarResultado);

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

        btnTomarFotoDespues.setOnClickListener(v -> abrirCamara());
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

    private void cargarRegistro() {
        executor.execute(() -> {
            RegistroTareaRepository repository = new RegistroTareaRepository(getApplicationContext());

            try {
                RegistroTarea registro =
                        repository.obtenerRegistro(registroId, usuarioId);

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

                    Toast.makeText(this, "Tarea completada. Resultado guardado.", Toast.LENGTH_LONG).show();

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

    private void actualizarBotones() {
        boolean disponible = registroListo
                && !guardando
                && rutaFotoPendiente == null;

        btnTomarFotoDespues.setEnabled(disponible);

        btnGuardarResultado.setEnabled(
                disponible && rutaFotoDespues != null
        );
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("foto_despues", rutaFotoDespues);
        outState.putString("foto_pendiente", rutaFotoPendiente);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}