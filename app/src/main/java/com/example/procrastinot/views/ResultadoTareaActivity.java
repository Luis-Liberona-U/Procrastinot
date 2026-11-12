package com.example.procrastinot.views;

import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.procrastinot.R;
import com.example.procrastinot.models.RegistroTarea;
import com.example.procrastinot.repositories.RegistroTareaRepository;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



/**
 * Muestra el resultado de una realización completada.
 *
 * Presenta el nombre de la tarea, el tiempo empleado y las fotografías de antes y después. Consulta los datos en SQLite y carga las imágenes
 * desde las rutas guardadas en el registro.
 */
public class ResultadoTareaActivity extends AppCompatActivity {

    private long usuarioId;
    private long registroId;

    private TextView txtNombreTarea;
    private TextView txtTiempo;
    private ImageView imgAntes;
    private ImageView imgDespues;

    /** Ejecuta la consulta y la carga de imágenes fuera del hilo principal. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();


    /**
     * Obtiene los identificadores del usuario y del registro, prepara la pantalla y solicita la carga del resultado.
     *
     * Si falta alguno de los identificadores, muestra "No se recibieron los datos del resultado" y cierra la pantalla.
     *
     * @param savedInstanceState Estado guardado por Android al recrear la pantalla, o null si no existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resultado_tarea);

        usuarioId = getIntent().getLongExtra("usuario_id", -1);
        registroId = getIntent().getLongExtra("registro_id", -1);

        if (usuarioId == -1 || registroId == -1) {
            Toast.makeText(this, "No se recibieron los datos del resultado", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        txtNombreTarea = findViewById(R.id.txtNombreTareaResultado);
        txtTiempo = findViewById(R.id.txtTiempoResultado);
        imgAntes = findViewById(R.id.imgAntesResultado);
        imgDespues = findViewById(R.id.imgDespuesResultado);

        txtNombreTarea.setText("Cargando resultado…");
        txtTiempo.setText("");

        // Botón volver al menú principal
        Button btnVolverPrincipal = findViewById(R.id.btnVolverPrincipal);

        btnVolverPrincipal.setOnClickListener(v -> {
            Intent intent = new Intent(
                    ResultadoTareaActivity.this,
                    PrincipalActivity.class
            );

            intent.putExtra("usuario_id", usuarioId);

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            startActivity(intent);
            finish();
        });

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

        cargarResultado();
    }

    /**
     * Consulta el registro del usuario y carga sus fotografías en segundo plano.
     *
     * El registro debe estar completado y tener inicio y fin reales. Si no cumple esas condiciones, muestra "No se encontró un resultado completado"
     * y cierra la pantalla. Si la consulta a SQLite falla, muestra "No se pudo cargar el resultado" y también la cierra.
     *
     * Si alguna fotografía no se puede cargar, muestra el nombre, el tiempo y las imágenes que estén disponibles, junto con un aviso.
     */
    private void cargarResultado() {
        executor.execute(() -> {
            RegistroTareaRepository repository = new RegistroTareaRepository(getApplicationContext());

            try {
                RegistroTarea registro = repository.obtenerRegistro(registroId, usuarioId);

                if (registro == null
                        || registro.getInicioReal() == null
                        || registro.getFinReal() == null
                        || !RegistroTarea.COMPLETADA.equals(
                        registro.getEstado()
                )) {
                    mostrarError("No se encontró un resultado completado");
                    return;
                }

                String nombre = repository.obtenerNombreTarea(
                        registro.getTareaId(),
                        usuarioId
                );

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

                Bitmap fotoAntes = cargarFoto(registro.getFotoAntes());
                Bitmap fotoDespues = cargarFoto(registro.getFotoDespues());

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    txtNombreTarea.setText(nombre);
                    txtTiempo.setText("Tiempo empleado: " + tiempo);

                    imgAntes.setImageBitmap(fotoAntes);
                    imgDespues.setImageBitmap(fotoDespues);

                    if (fotoAntes == null || fotoDespues == null) {
                        Toast.makeText(this, "Una o ambas fotos ya no están disponibles", Toast.LENGTH_LONG).show();
                    }
                });

            } catch (SQLiteException e) {
                mostrarError("No se pudo cargar el resultado");

            } finally {
                repository.cerrar();
            }
        });
    }

    /**
     * Carga una fotografía a partir de su ruta local.
     *
     * Primero lee las dimensiones sin cargar los píxeles. Luego aumenta el factor de muestreo hasta que las dimensiones estimadas no
     * superen 1200 píxeles, para reducir el uso de memoria.
     *
     * @param ruta Ruta del archivo de la fotografía.
     * @return Imagen decodificada o null si la ruta está vacía, no se indicó o el archivo no se pudo decodificar.
     */
    private Bitmap cargarFoto(String ruta) {
        if (ruta == null || ruta.isEmpty()) {
            return null;
        }

        BitmapFactory.Options opciones = new BitmapFactory.Options();

        opciones.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(ruta, opciones);

        if (opciones.outWidth <= 0 || opciones.outHeight <= 0) {
            return null;
        }

        opciones.inSampleSize = 1;

        while (opciones.outWidth / opciones.inSampleSize > 1200
                || opciones.outHeight / opciones.inSampleSize > 1200) {
            opciones.inSampleSize *= 2;
        }

        opciones.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(ruta, opciones);
    }


    /**
     * Muestra un aviso y cierra la pantalla desde el hilo principal.
     *
     * @param mensaje Texto que explica por qué no se pudo mostrar el resultado.
     */
    private void mostrarError(String mensaje) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
            finish();
        });
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