package com.example.procrastinot.views;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
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
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.procrastinot.R;
import com.example.procrastinot.repositories.RegistroTareaRepository;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RealizarTareaActivity extends AppCompatActivity {

    private ImageView imgFotoAntes;
    private Button btnTomarFotoAntes;
    private Button btnIniciarTarea;

    private String rutaFotoAntes;
    private String rutaFotoPendiente;

    private long usuarioId;
    private long tareaId;
    private long inicioProgramado;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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

        btnTomarFotoAntes.setOnClickListener(v -> abrirCamara());

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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("foto_antes", rutaFotoAntes);
        outState.putString("foto_pendiente", rutaFotoPendiente);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}