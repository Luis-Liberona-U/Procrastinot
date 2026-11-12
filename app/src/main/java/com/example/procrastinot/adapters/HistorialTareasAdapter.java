package com.example.procrastinot.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastinot.R;
import com.example.procrastinot.models.RegistroTarea;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * La funcion de esta clase es mostrar el historial de realizaciones completadas en un RecyclerView.
 *
 * Cada fila presenta el nombre de la tarea, la fecha de finalización y el tiempo empleado.
 * Cuando el usuario toca una fila, el adaptador comunica qué registro seleccionó para que la pantalla abra su resultado.
 */
public class HistorialTareasAdapter extends
        RecyclerView.Adapter<HistorialTareasAdapter.HistorialViewHolder> {

    private final List<RegistroTarea> registros = new ArrayList<>();
    private final AccionesHistorial acciones;

    public interface AccionesHistorial {
        void verResultado(RegistroTarea registro);
    }

    public HistorialTareasAdapter(AccionesHistorial acciones) {
        this.acciones = acciones;
    }


    /**
     * Reemplaza los registros mostrados y solicita al RecyclerView que actualice sus filas. Debe llamarse desde el hilo principal.
     *
     * @param nuevaLista Registros en el orden en que se mostrarán.
     *                   La lista no debe ser nula y cada registro debe
     *                   tener inicio y fin reales.
     */
    public void actualizarLista(List<RegistroTarea> nuevaLista) {
        registros.clear();
        registros.addAll(nuevaLista);
        notifyDataSetChanged();
    }



    /**
     * Crea una fila a partir del diseño item_historial.xml.
     *
     * @param parent Contenedor al que se incorporará la fila.
     * @param viewType Tipo de vista solicitado; se utiliza un único diseño.
     * @return ViewHolder con las referencias a los componentes de la fila.
     */
    @NonNull
    @Override
    public HistorialViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View fila = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historial, parent, false);

        return new HistorialViewHolder(fila);
    }


    /**
     * Vincula un registro con su fila y configura la acción al tocarla.
     *
     * Muestra la fecha a partir del fin real y calcula la duración como la diferencia entre el fin y el inicio reales.
     *
     * @param holder Fila que se actualizará.
     * @param position Posición del registro en la lista.
     */
    @Override
    public void onBindViewHolder(
            @NonNull HistorialViewHolder holder,
            int position
    ) {
        RegistroTarea registro = registros.get(position);

        holder.nombre.setText(registro.getNombreTarea());

        SimpleDateFormat formato = new SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
        );

        holder.fecha.setText(
                "Realizada: " + formato.format(new Date(registro.getFinReal()))
        );

        /** Convierte la duración a segundos enteros y evita mostrar valores negativos. */
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

        holder.tiempo.setText("Tiempo empleado: " + tiempo);

        holder.itemView.setOnClickListener(v ->
                acciones.verResultado(registro)
        );
    }


    /**
     * Devuelve la cantidad de filas que debe mostrar el RecyclerView.
     *
     * @return Número de registros del historial.
     */
    @Override
    public int getItemCount() {
        return registros.size();
    }

    /** Conserva las referencias a los campos de una fila reutilizable. */
    static class HistorialViewHolder extends RecyclerView.ViewHolder {

        final TextView nombre;
        final TextView fecha;
        final TextView tiempo;

        /**
         * Localiza los campos del diseño de la fila.
         *
         * @param itemView Vista de la fila del historial.
         */
        HistorialViewHolder(@NonNull View itemView) {
            super(itemView);

            nombre = itemView.findViewById(R.id.txtNombreHistorial);
            fecha = itemView.findViewById(R.id.txtFechaHistorial);
            tiempo = itemView.findViewById(R.id.txtTiempoHistorial);
        }
    }
}