package com.example.procrastinot.views;

import android.os.Bundle;
import android.widget.Button;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Locale;

public class CrearTareaActivity extends AppCompatActivity {

    private int horaInicio = -1;
    private int minutoInicio = -1;

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

        // También abre la ventana si Personalizar ya está marcado.
        rbPersonalizar.setOnClickListener(v -> {
            mostrarSelectorDias(rgRepeticion);
        });
    }

    private void mostrarSelectorDias(RadioGroup grupo) {

        // Evita abrir dos ventanas por el mismo clic.
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
                        (dialog, posicion, seleccionado) -> {
                            seleccionTemporal[posicion] = seleccionado;
                        }
                )
                .setPositiveButton("Aceptar", null)
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    grupo.check(repeticionAnterior);
                })
                .create();

        // Se ejecuta al cerrar con Atrás o tocando fuera.
        dialogo.setOnCancelListener(dialog -> {
            grupo.check(repeticionAnterior);
        });

        dialogo.setOnDismissListener(dialog -> {
            selectorDiasAbierto = false;
        });

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
                            Toast.makeText(
                                    this,
                                    "Selecciona al menos un día",
                                    Toast.LENGTH_SHORT
                            ).show();

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

        rbPersonalizar.setText(
                "Personalizar: " + resumen
        );
    }
}