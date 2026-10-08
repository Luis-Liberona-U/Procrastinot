package com.example.procrastinot.views;

import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Patterns;
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

public class RegistroActivity extends AppCompatActivity {

    private EditText edtNombre;
    private EditText edtCorreo;
    private EditText edtContrasena;
    private EditText edtConfirmacion;
    private Button btnCrearCuenta;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}