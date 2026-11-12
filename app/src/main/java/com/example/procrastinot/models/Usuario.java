package com.example.procrastinot.models;


/**
 * Representa los datos de una cuenta de usuario de Procrastinot.
 *
 * Permite transportar información entre los repositorios y las pantallas.
 * Esta clase no ejecuta consultas SQL ni válida las credenciales.
 *
 * El atributo passwordHash conserva el valor asignado, pero esta clase no aplica ningún algoritmo de hash.
 */
public class Usuario {

    /** Identificador del usuario, correspondiente a la columna _id de SQLite. */
    private long id;

    /** Nombre del usuario que se muestra en la aplicación. */
    private String nombre;

    /** Correo electrónico utilizado para identificar la cuenta al iniciar sesión. */
    private String correo;

    /** Valor de contraseña asociado al usuario. */
    private String passwordHash;

    /**
     * Crea un usuario sin datos asignados explícitamente.
     *
     * Permite completar sus atributos posteriormente mediante los setters.
     */
    public Usuario() {
    }

    /**
     * Crea un usuario con sus datos de identificación, sin contraseña.
     *
     * @param id Identificador del usuario en SQLite.
     * @param nombre Nombre del usuario.
     * @param correo Correo electrónico de la cuenta.
     */
    public Usuario(long id, String nombre, String correo) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
    }

    /**
     * Crea un usuario incluyendo el valor de su contraseña.
     *
     * Reutiliza el constructor de tres parámetros para asignar los datos de identificación.
     *
     * @param id Identificador del usuario en SQLite.
     * @param nombre Nombre del usuario.
     * @param correo Correo electrónico de la cuenta.
     * @param passwordHash Valor de contraseña que se asignará sin transformarlo.
     */
    public Usuario(
            long id,
            String nombre,
            String correo,
            String passwordHash
    ) {
        this(id, nombre, correo);
        this.passwordHash = passwordHash;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}