package com.example.procrastinot.views;

import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Patterns;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.procrastinot.R;
import com.example.procrastinot.repositories.UsuarioRepository;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



/**
 * Muestra el formulario para crear una cuenta en Procrastinot.
 *
 * Válida los datos ingresados y UsuarioRepository realiza el registro en SQLite. Si se crea la cuenta, cierra esta pantalla para volver al inicio de sesión.
 */
public class RegistroActivity extends AppCompatActivity {

    private EditText edtNombre;
    private EditText edtCorreo;
    private EditText edtContrasena;
    private EditText edtConfirmacion;
    private Button btnCrearCuenta;

    /** Ejecuta las operaciones de SQLite fuera del hilo de la interfaz. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();


    /**
     * Carga el formulario, conecta sus campos y configura el botón de registro.
     *
     * @param savedInstanceState Estado guardado por Android al recrear la pantalla, o null si no existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Protege el formulario frente a capturas de pantalla.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro);

        edtNombre = findViewById(R.id.edtNombreRegistro);
        edtCorreo = findViewById(R.id.editTextTextEmailAddress);
        edtContrasena = findViewById(R.id.edtContrasena);
        edtConfirmacion = findViewById(R.id.edtContrasenaConfirmar);
        btnCrearCuenta = findViewById(R.id.btnCrearCuentaRegistro);

        btnCrearCuenta.setOnClickListener(v -> registrarCuenta());

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

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
     * Válida el formulario y solicita la creación de la cuenta.
     *
     * Quita los espacios de los extremos del nombre y del correo, y convierte el correo a minúsculas. La contraseña se compara con la confirmación tal como se ingresó.
     * Si el formulario es válido, comprueba en segundo plano si el correo ya existe e intenta guardar la cuenta si está disponible.
     */
    private void registrarCuenta() {
        String nombre = edtNombre.getText().toString().trim();

        String correo = edtCorreo.getText().toString().trim().toLowerCase(Locale.ROOT);

        String contrasena = edtContrasena.getText().toString();
        String confirmacion = edtConfirmacion.getText().toString();

        if (nombre.isEmpty()) {
            edtNombre.setError("Ingresa tu nombre");
            edtNombre.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            edtCorreo.setError("Ingresa un correo válido");
            edtCorreo.requestFocus();
            return;
        }

        if (contrasena.isEmpty()) {
            edtContrasena.setError("Ingresa una contraseña");
            edtContrasena.requestFocus();
            return;
        }

        if (!contrasena.equals(confirmacion)) {
            edtConfirmacion.setError("Las contraseñas no coinciden");
            edtConfirmacion.requestFocus();
            return;
        }

        btnCrearCuenta.setEnabled(false);

        executor.execute(() -> {
            UsuarioRepository repository = new UsuarioRepository(getApplicationContext());

            try {
                if (repository.existeCorreo(correo)) {
                    mostrarResultado("Este correo ya está registrado", false);
                    return;
                }

                long id = repository.registrarUsuario(
                        nombre,
                        correo,
                        contrasena
                );

                if (id == -1) {
                    mostrarResultado("No se pudo crear la cuenta", false);
                } else {
                    mostrarResultado("Cuenta creada correctamente", true);
                }

            } catch (SQLiteException e) {
                mostrarResultado("Error al guardar la cuenta", false);
            } finally {
                repository.cerrar();
            }
        });
    }

    /**
     * Actualiza la interfaz con el resultado del registro.
     *
     * Vuelve al hilo principal para habilitar el botón y mostrar el mensaje, siempre que la pantalla siga disponible. Si el registro fue exitoso, cierra el formulario.
     *
     * @param mensaje Texto que se mostrará al usuario.
     * @param creada true si se creó la cuenta, false en caso contrario.
     */
    private void mostrarResultado(String mensaje, boolean creada) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            btnCrearCuenta.setEnabled(true);

            Toast.makeText(
                    RegistroActivity.this, mensaje, Toast.LENGTH_SHORT).show();

            if (creada) {
                finish();
            }
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