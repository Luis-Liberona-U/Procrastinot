package com.example.procrastinot.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastinot.R;
import com.example.procrastinot.models.Tarea;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Esta clase se encarga de mostrar las tareas del usuario en un RecyclerView.
 *
 * Cada fila presenta el nombre, la hora, la repetición y el estado de la tarea.
 * Las acciones de editar, eliminar y activar o desactivar se comunican a la pantalla mediante "AccionesTarea".
 */
public class ListaTareaAdapter extends RecyclerView.Adapter<ListaTareaAdapter.TareaViewHolder> {

    //Lista de tareas
    private final List<Tarea> tareas = new ArrayList<>();

    /** Recibe las acciones que el usuario realiza sobre una fila. */
    private final AccionesTarea acciones;

    public interface AccionesTarea {

        /**
         * Solicita editar la tarea seleccionada.
         *
         * @param tarea Tarea que se desea editar.
         */
        void editar(Tarea tarea);

        /**
         * Solicita eliminar la tarea seleccionada.
         *
         * @param tarea Tarea que se desea eliminar.
         */
        void eliminar(Tarea tarea);

        /**
         * Comunica el nuevo estado elegido con el interruptor.
         *
         * @param tarea Tarea cuyo estado se desea cambiar.
         * @param activa {@code true} para activar; {@code false} para desactivar.
         */
        void cambiarActiva(Tarea tarea, boolean activa);
    }

    /**
     * Crea el adaptador con las acciones que se harán en la pantalla.
     *
     * @param acciones Implementación que recibe las acciones de cada fila.
     */
    public ListaTareaAdapter(AccionesTarea acciones) {
        this.acciones = acciones;
    }

    /**
     * Reemplaza las tareas mostradas y solicita al RecyclerView que actualice sus filas. Debe llamarse desde el hilo principal.
     *
     * @param nuevaLista Lista no nula de tareas, en el orden en que se mostrarán.
     */
    public void actualizarLista(List<Tarea> nuevaLista) {
        tareas.clear();
        tareas.addAll(nuevaLista);

        notifyDataSetChanged();
    }

    /**
     * Crea una fila a partir del diseño de item_tarea.xml.
     *
     * @param parent Contenedor al que se incorporará la fila.
     * @param viewType Tipo de vista solicitado; se utiliza un único diseño.
     * @return ViewHolder con las referencias a los componentes de la fila.
     */
    @NonNull
    @Override
    public TareaViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View fila = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tarea, parent, false);

        return new TareaViewHolder(fila);
    }

    /**
     * Muestra los datos de una tarea y conecta las acciones de su fila.
     *
     * @param holder ViewHolder de la fila que se actualizará.
     * @param position Posición de la tarea en la lista.
     */
    @Override
    public void onBindViewHolder(
            @NonNull TareaViewHolder holder,
            int position
    ) {
        Tarea tarea = tareas.get(position);

        holder.nombre.setText(tarea.getNombre());

        holder.hora.setText(
                String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        tarea.getHoraInicio(),
                        tarea.getMinutoInicio()
                )
        );

        holder.repeticion.setText(textoRepeticion(tarea));

        holder.activa.setOnCheckedChangeListener(null);
        holder.activa.setChecked(tarea.isActiva());

        holder.activa.setOnCheckedChangeListener(
                (button, checked) -> {
                    acciones.cambiarActiva(tarea, checked);
                }
        );

        holder.editar.setOnClickListener(v -> {
            acciones.editar(tarea);
        });

        holder.eliminar.setOnClickListener(v -> {
            acciones.eliminar(tarea);
        });
    }

    /**
     * Indica cuántas filas debe mostrar el RecyclerView.
     *
     * @return Cantidad de tareas en la lista.
     */
    @Override
    public int getItemCount() {
        return tareas.size();
    }

    /**
     * Convierte la repetición en el texto de la fila.
     *
     * Para una tarea de una sola vez muestra su fecha si está disponible.
     * Para una tarea semanal abrevia los días seleccionados.
     *
     * @param tarea Tarea cuya repetición se desea mostrar.
     * @return Texto de repetición para la fila.
     */
    private String textoRepeticion(Tarea tarea) {
        String repeticion = tarea.getRepeticion();

        if (Tarea.UNA_VEZ.equals(repeticion)) {
            return tarea.getFechaUnica() == null
                    ? "Una vez"
                    : tarea.getFechaUnica();
        }

        if (Tarea.DIARIA.equals(repeticion)) {
            return "Diaria";
        }

        if (Tarea.DIAS_SEMANA.equals(repeticion)) {
            boolean[] dias = tarea.getDias();

            boolean lunesAViernes =
                    dias[0] && dias[1] && dias[2]
                            && dias[3] && dias[4]
                            && !dias[5] && !dias[6];

            if (lunesAViernes) {
                return "Lun-Vie";
            }

            String[] nombres = {
                    "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"
            };

            StringBuilder resumen = new StringBuilder();

            for (int i = 0; i < dias.length; i++) {
                if (dias[i]) {
                    if (resumen.length() > 0) {
                        resumen.append(", ");
                    }

                    resumen.append(nombres[i]);
                }
            }

            return resumen.length() == 0
                    ? "Sin días"
                    : resumen.toString();
        }

        return "Sin repetición";
    }

    /**
     * Localiza los textos y controles definidos en la fila.
     *
     * @param "itemView" Vista raíz de item_tarea.xml.
     */
    static class TareaViewHolder extends RecyclerView.ViewHolder {

        final TextView nombre;
        final TextView hora;
        final TextView repeticion;

        final Switch activa;
        final ImageButton editar;
        final Button eliminar;

        TareaViewHolder(@NonNull View itemView) {
            super(itemView);

            nombre = itemView.findViewById(R.id.txtNombreTareaLista);

            hora = itemView.findViewById(R.id.txtHoraTareaLista);

            repeticion = itemView.findViewById(R.id.txtRepeticionTatrea);

            activa = itemView.findViewById(R.id.switchActivaTarea);

            editar = itemView.findViewById(R.id.btnEditarTarea);

            eliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}