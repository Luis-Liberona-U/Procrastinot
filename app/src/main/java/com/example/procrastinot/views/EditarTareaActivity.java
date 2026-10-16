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
import com.example.procrastinot.models.Tarea;
import com.example.procrastinot.repositories.TareaRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditarTareaActivity extends AppCompatActivity {

    private long usuarioId;
    private long tareaId;

    private int horaInicio = -1;
    private int minutoInicio = -1;

    private Tarea tareaOriginal;
    private boolean cargando = true;
    private boolean guardando = false;

    private EditText edtNombreTarea;
    private EditText edtDescripcionTarea;
    private EditText edtDuracion;

    private Button btnHoraInicio;
    private Button btnGuardarTarea;

    private RadioGroup rgRepeticion;
    private RadioButton rbPersonalizar;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final String[] nombresDias = {
            "Lunes", "Martes", "Miércoles", "Jueves",
            "Viernes", "Sábado", "Domingo"
    };

    private boolean[] diasSeleccionados = new boolean[7];
    private int repeticionAnterior = R.id.rbUnaVez;
    private boolean selectorDiasAbierto = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_editar_tarea);

        usuarioId = getIntent().getLongExtra("usuario_id", -1);
        tareaId = getIntent().getLongExtra("tarea_id", -1);

        if (usuarioId == -1 || tareaId == -1) {
            Toast.makeText(this, "No se recibieron los datos de la tarea", Toast.LENGTH_LONG).show();

            finish();
            return;
        }

        edtNombreTarea = findViewById(R.id.edtNombreTareaEditar);
        edtDescripcionTarea =
                findViewById(R.id.edtDescripcionTareaEditar);
        edtDuracion = findViewById(R.id.edtDuracionEditar);

        btnHoraInicio = findViewById(R.id.btnHoraInicioEditar);
        btnGuardarTarea = findViewById(R.id.btnGuardarTareaEditar);

        rgRepeticion = findViewById(R.id.rgRepeticionEditar);
        rbPersonalizar = findViewById(R.id.rbElegirDias);

        habilitarFormulario(false);

        btnHoraInicio.setOnClickListener(v -> mostrarReloj());

        btnGuardarTarea.setOnClickListener(v -> guardarCambios());

        rgRepeticion.setOnCheckedChangeListener((group, checkedId) -> {
            if (cargando || guardando) {
                return;
            }

            if (checkedId == R.id.rbElegirDias) {
                mostrarSelectorDias();
            } else {
                repeticionAnterior = checkedId;
            }
        });

        rbPersonalizar.setOnClickListener(v -> {
            if (!cargando && !guardando) {
                mostrarSelectorDias();
            }
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

        cargarTarea();
    }

    private void cargarTarea() {
        executor.execute(() -> {
            TareaRepository repository =
                    new TareaRepository(getApplicationContext());

            try {
                Tarea tarea = repository.obtenerPorId(tareaId, usuarioId);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (tarea == null) {
                        Toast.makeText(this, "La tarea no está disponible", Toast.LENGTH_LONG).show();

                        finish();
                        return;
                    }

                    tareaOriginal = tarea;

                    edtNombreTarea.setText(tarea.getNombre());
                    edtDescripcionTarea.setText(tarea.getDescripcion());

                    edtDuracion.setText(
                            String.valueOf(tarea.getDuracionMinutos())
                    );

                    horaInicio = tarea.getHoraInicio();
                    minutoInicio = tarea.getMinutoInicio();
                    actualizarTextoHora();

                    diasSeleccionados = tarea.getDias();
                    actualizarTextoPersonalizar();

                    int opcion;

                    if (Tarea.UNA_VEZ.equals(tarea.getRepeticion())) {
                        opcion = R.id.rbUnaVez;

                    } else if (Tarea.DIARIA.equals(tarea.getRepeticion())) {
                        opcion = R.id.rbTodosLosDias;

                    } else if (esLunesAViernes(diasSeleccionados)) {
                        opcion = R.id.rbLunesaAViernes;

                    } else {
                        opcion = R.id.rbElegirDias;
                    }

                    rgRepeticion.check(opcion);
                    repeticionAnterior = opcion;

                    cargando = false;
                    habilitarFormulario(true);
                });

            } catch (SQLiteException e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Toast.makeText(this, "No se pudo cargar la tarea", Toast.LENGTH_LONG).show();

                    finish();
                });

            } finally {
                repository.cerrar();
            }
        });
    }

    private boolean esLunesAViernes(boolean[] dias) {
        return dias[0] && dias[1] && dias[2]
                && dias[3] && dias[4]
                && !dias[5] && !dias[6];
    }

    private void mostrarReloj() {
        MaterialTimePicker reloj = new MaterialTimePicker.Builder()
                .setTitleText("Hora de inicio")
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .setHour(horaInicio)
                .setMinute(minutoInicio)
                .build();

        reloj.addOnPositiveButtonClickListener(view -> {
            horaInicio = reloj.getHour();
            minutoInicio = reloj.getMinute();
            actualizarTextoHora();
        });

        reloj.show(getSupportFragmentManager(), "RELOJ_EDITAR");
    }

    private void actualizarTextoHora() {
        btnHoraInicio.setText(
                String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        horaInicio,
                        minutoInicio
                )
        );
    }

    private void mostrarSelectorDias() {
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
                        rgRepeticion.check(repeticionAnterior)
                )
                .create();

        dialogo.setOnCancelListener(dialog ->
                rgRepeticion.check(repeticionAnterior)
        );

        dialogo.setOnDismissListener(dialog ->
                selectorDiasAbierto = false
        );

        dialogo.setOnShowListener(dialog -> {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {
                        boolean hayDia = false;

                        for (boolean seleccionado : seleccionTemporal) {
                            if (seleccionado) {
                                hayDia = true;
                                break;
                            }
                        }

                        if (!hayDia) {
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

        rbPersonalizar.setText(
                resumen.length() == 0
                        ? "Personalizar"
                        : "Personalizar: " + resumen
        );
    }

    private void guardarCambios() {
        if (cargando || guardando || tareaOriginal == null) {
            return;
        }

        String nombre = edtNombreTarea.getText().toString().trim();
        String descripcion =
                edtDescripcionTarea.getText().toString().trim();
        String textoDuracion = edtDuracion.getText().toString().trim();

        if (nombre.isEmpty()) {
            edtNombreTarea.setError("Ingresa el nombre de la tarea");
            edtNombreTarea.requestFocus();
            return;
        }

        int duracion;

        try {
            duracion = Integer.parseInt(textoDuracion);
        } catch (NumberFormatException e) {
            edtDuracion.setError("Ingresa una duración válida");
            edtDuracion.requestFocus();
            return;
        }

        if (duracion <= 0) {
            edtDuracion.setError("La duración debe ser mayor que cero");
            edtDuracion.requestFocus();
            return;
        }

        int opcion = rgRepeticion.getCheckedRadioButtonId();

        String repeticion;
        String fechaUnica = null;
        boolean[] dias = new boolean[7];

        if (opcion == R.id.rbUnaVez) {
            repeticion = Tarea.UNA_VEZ;

            // Si solo cambias nombre, descripción o duración,
            // conservamos la fecha de la tarea de una vez.
            boolean mismaProgramacion =
                    Tarea.UNA_VEZ.equals(tareaOriginal.getRepeticion())
                            && horaInicio == tareaOriginal.getHoraInicio()
                            && minutoInicio == tareaOriginal.getMinutoInicio();

            fechaUnica = mismaProgramacion
                    ? tareaOriginal.getFechaUnica()
                    : calcularFechaUnica();

        } else if (opcion == R.id.rbTodosLosDias) {
            repeticion = Tarea.DIARIA;

        } else if (opcion == R.id.rbLunesaAViernes) {
            repeticion = Tarea.DIAS_SEMANA;

            for (int i = 0; i < 5; i++) {
                dias[i] = true;
            }

        } else if (opcion == R.id.rbElegirDias) {
            repeticion = Tarea.DIAS_SEMANA;
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
            Toast.makeText(this, "Selecciona una repetición", Toast.LENGTH_SHORT).show();
            return;
        }

        final String repeticionGuardar = repeticion;
        final String fechaGuardar = fechaUnica;
        final boolean[] diasGuardar = dias.clone();
        final int horaGuardar = horaInicio;
        final int minutoGuardar = minutoInicio;

        guardando = true;
        habilitarFormulario(false);

        executor.execute(() -> {TareaRepository repository = new TareaRepository(getApplicationContext());

            try {
                boolean actualizada = repository.actualizarTarea(
                        tareaId,
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

                mostrarResultado(
                        actualizada
                                ? "Cambios guardados"
                                : "La tarea ya no está disponible",
                        actualizada
                );

            } catch (SQLiteException | IllegalArgumentException e) {mostrarResultado("No se pudieron guardar los cambios", false);

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

    private void mostrarResultado(String mensaje, boolean actualizada) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            guardando = false;
            habilitarFormulario(true);

            Toast.makeText(EditarTareaActivity.this, mensaje, Toast.LENGTH_SHORT).show();

            if (actualizada) {
                finish();
            }
        });
    }

    private void habilitarFormulario(boolean habilitado) {
        edtNombreTarea.setEnabled(habilitado);
        edtDescripcionTarea.setEnabled(habilitado);
        edtDuracion.setEnabled(habilitado);
        btnHoraInicio.setEnabled(habilitado);
        btnGuardarTarea.setEnabled(habilitado);

        for (int i = 0; i < rgRepeticion.getChildCount(); i++) {
            rgRepeticion.getChildAt(i).setEnabled(habilitado);
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}