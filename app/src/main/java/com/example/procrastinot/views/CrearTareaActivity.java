package com.example.procrastinot.views;

import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.procrastinot.R;
import com.example.procrastinot.repositories.TareaRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CrearTareaActivity extends AppCompatActivity {

    private int horaInicio = -1;
    private int minutoInicio = -1;
    private long usuarioId;

    private EditText edtNombreTarea;
    private EditText edtDescripcionTarea;
    private EditText edtDuracion;
    private Button btnGuardarTarea;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final String[] nombresDias = {
            "Lunes",
            "Martes",
            "Miércoles",
            "Jueves",
            "Viernes",
            "Sábado",
            "Domingo"
    };

    private boolean[] diasSeleccionados = new boolean[7];

    private int repeticionAnterior = R.id.rbUnaVez;
    private boolean selectorDiasAbierto = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_crear_tarea);

        usuarioId = getIntent().getLongExtra("usuario_id", -1);

        if (usuarioId == -1) {
            Toast.makeText(this, "No se recibió el usuario. Inicia sesión nuevamente.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        edtNombreTarea = findViewById(R.id.edtNombreTarea);
        edtDescripcionTarea = findViewById(R.id.edtDescripcionTarea);
        edtDuracion = findViewById(R.id.edtDuracion);
        btnGuardarTarea = findViewById(R.id.btnGuardarTarea);

        btnGuardarTarea.setOnClickListener(v -> guardarTarea());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main),
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

        // SELECTOR DE HORA
        Button btnHoraInicio = findViewById(R.id.btnHoraInicio);

        btnHoraInicio.setOnClickListener(v -> {
            MaterialTimePicker reloj = new MaterialTimePicker.Builder()
                    .setTitleText("Hora de inicio")
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                    .setHour(horaInicio == -1 ? 8 : horaInicio)
                    .setMinute(minutoInicio == -1 ? 0 : minutoInicio)
                    .build();

            reloj.addOnPositiveButtonClickListener(view -> {
                horaInicio = reloj.getHour();
                minutoInicio = reloj.getMinute();

                btnHoraInicio.setText(
                        String.format(
                                Locale.getDefault(),
                                "%02d:%02d",
                                horaInicio,
                                minutoInicio
                        )
                );
            });

            reloj.show(
                    getSupportFragmentManager(),
                    "RELOJ_INICIO"
            );
        });

        // REPETICIÓN
        RadioGroup rgRepeticion = findViewById(R.id.rgRepeticion);
        RadioButton rbPersonalizar = findViewById(R.id.rbElegirDias);

        rgRepeticion.check(R.id.rbUnaVez);

        rgRepeticion.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbElegirDias) {
                mostrarSelectorDias(group);
            } else {
                repeticionAnterior = checkedId;
            }
        });

        // Permite volver a abrir Personalizar si ya está seleccionado.
        rbPersonalizar.setOnClickListener(v ->
                mostrarSelectorDias(rgRepeticion)
        );
    }

    private void mostrarSelectorDias(RadioGroup grupo) {
        if (selectorDiasAbierto) {
            return;
        }

        selectorDiasAbierto = true;

        boolean[] seleccionTemporal = diasSeleccionados.clone();

        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle("Repetir los días")
                .setMultiChoiceItems(
                        nombresDias,
                        seleccionTemporal,
                        (dialog, posicion, seleccionado) ->
                                seleccionTemporal[posicion] = seleccionado
                )
                .setPositiveButton("Aceptar", null)
                .setNegativeButton("Cancelar", (dialog, which) ->
                        grupo.check(repeticionAnterior)
                )
                .create();

        dialogo.setOnCancelListener(dialog ->
                grupo.check(repeticionAnterior)
        );

        dialogo.setOnDismissListener(dialog ->
                selectorDiasAbierto = false
        );

        dialogo.setOnShowListener(dialog -> {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {
                        boolean haySeleccion = false;

                        for (boolean seleccionado : seleccionTemporal) {
                            if (seleccionado) {
                                haySeleccion = true;
                                break;
                            }
                        }

                        if (!haySeleccion) {
                            Toast.makeText(this, "Selecciona al menos un día", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        diasSeleccionados = seleccionTemporal.clone();
                        repeticionAnterior = R.id.rbElegirDias;

                        actualizarTextoPersonalizar();
                        dialogo.dismiss();
                    });
        });

        dialogo.show();
    }

    private void actualizarTextoPersonalizar() {
        StringBuilder resumen = new StringBuilder();

        for (int i = 0; i < nombresDias.length; i++) {
            if (diasSeleccionados[i]) {
                if (resumen.length() > 0) {
                    resumen.append(", ");
                }

                resumen.append(nombresDias[i]);
            }
        }

        RadioButton rbPersonalizar = findViewById(R.id.rbElegirDias);
        rbPersonalizar.setText("Personalizar: " + resumen);
    }

    private void guardarTarea() {
        String nombre = edtNombreTarea.getText().toString().trim();
        String descripcion = edtDescripcionTarea.getText().toString().trim();
        String textoDuracion = edtDuracion.getText().toString().trim();

        if (nombre.isEmpty()) {
            edtNombreTarea.setError("Ingresa el nombre de la tarea");
            edtNombreTarea.requestFocus();
            return;
        }

        if (horaInicio == -1 || minutoInicio == -1) {
            Toast.makeText(this, "Selecciona la hora de inicio", Toast.LENGTH_SHORT).show();
            return;
        }

        int duracion;

        try {
            duracion = Integer.parseInt(textoDuracion);
        } catch (NumberFormatException e) {
            edtDuracion.setError("Ingresa una duración válida en minutos");
            edtDuracion.requestFocus();
            return;
        }

        if (duracion <= 0) {
            edtDuracion.setError("La duración debe ser mayor que cero");
            edtDuracion.requestFocus();
            return;
        }

        RadioGroup grupo = findViewById(R.id.rgRepeticion);
        int opcion = grupo.getCheckedRadioButtonId();

        String repeticion;
        String fechaUnica = null;
        boolean[] dias = new boolean[7];

        if (opcion == R.id.rbUnaVez) {
            repeticion = "UNA_VEZ";
            fechaUnica = calcularFechaUnica();

        } else if (opcion == R.id.rbTodosLosDias) {
            repeticion = "DIARIA";

        } else if (opcion == R.id.rbLunesaAViernes) {
            repeticion = "DIAS_SEMANA";

            for (int i = 0; i < 5; i++) {
                dias[i] = true;
            }

        } else if (opcion == R.id.rbElegirDias) {
            repeticion = "DIAS_SEMANA";
            dias = diasSeleccionados.clone();

            boolean hayDia = false;

            for (boolean seleccionado : dias) {
                if (seleccionado) {
                    hayDia = true;
                    break;
                }
            }

            if (!hayDia) {
                Toast.makeText(this, "Selecciona al menos un día", Toast.LENGTH_SHORT).show();
                return;
            }

        } else {
            Toast.makeText(this, "Selecciona una opción de repetición", Toast.LENGTH_SHORT).show();

            return;
        }

        final String repeticionGuardar = repeticion;
        final String fechaGuardar = fechaUnica;
        final boolean[] diasGuardar = dias.clone();
        final int horaGuardar = horaInicio;
        final int minutoGuardar = minutoInicio;

        btnGuardarTarea.setEnabled(false);

        executor.execute(() -> {
            TareaRepository repository = new TareaRepository(getApplicationContext());

            try {
                repository.crearTarea(
                        usuarioId,
                        nombre,
                        descripcion,
                        horaGuardar,
                        minutoGuardar,
                        repeticionGuardar,
                        fechaGuardar,
                        duracion,
                        diasGuardar
                );

                mostrarResultadoGuardado("Tarea guardada correctamente", true);

            } catch (SQLiteException | IllegalArgumentException e) {
                mostrarResultadoGuardado("No se pudo guardar la tarea", false);

            } finally {
                repository.cerrar();
            }
        });
    }

    private String calcularFechaUnica() {
        Calendar ahora = Calendar.getInstance();
        Calendar inicio = (Calendar) ahora.clone();

        inicio.set(Calendar.HOUR_OF_DAY, horaInicio);
        inicio.set(Calendar.MINUTE, minutoInicio);
        inicio.set(Calendar.SECOND, 0);
        inicio.set(Calendar.MILLISECOND, 0);

        if (!inicio.after(ahora)) {
            inicio.add(Calendar.DAY_OF_MONTH, 1);
        }

        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        return formato.format(inicio.getTime());
    }

    private void mostrarResultadoGuardado(
            String mensaje,
            boolean guardada
    ) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            btnGuardarTarea.setEnabled(true);

            Toast.makeText(CrearTareaActivity.this, mensaje, Toast.LENGTH_SHORT).show();

            if (guardada) {
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