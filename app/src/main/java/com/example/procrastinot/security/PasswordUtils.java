package com.example.procrastinot.security;

import com.lambdapioneer.argon2kt.Argon2Kt;
import com.lambdapioneer.argon2kt.Argon2Mode;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Genera y verifica hashes de contraseñas de las cuentas locales con Argon2id.
 *
 * Al registrar una cuenta, genera una sal aleatoria de 16 bytes y calcula el hash con 19 MiB de memoria, 2 iteraciones y un paralelismo de 1.
 * La cadena resultante incluye la configuración, la sal y el hash, y se guarda en SQLite en la columna password_hash.
 */
public final class PasswordUtils {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final Argon2Kt ARGON2 = new Argon2Kt();

    private PasswordUtils() {
    }

    /**
     * Genera un hash Argon2id con una sal aleatoria nueva.
     *
     * @param contrasena Contraseña ingresada al crear la cuenta.
     * @return Cadena que contiene la configuración, la sal y el hash.
     */
    public static String generarHash(String contrasena) {
        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);
        byte[] clave = contrasena.getBytes(StandardCharsets.UTF_8);

        try {
            return ARGON2.hash(
                    Argon2Mode.ARGON2_ID,
                    clave,
                    sal,
                    2,       // Iteraciones
                    19_456,  // Memoria en KiB: 19 MiB
                    1        // Paralelismo
            ).encodedOutputAsString();
        } finally {
            Arrays.fill(clave, (byte) 0);
        }
    }

    /**
     * Comprueba si la contraseña ingresada coincide con el hash guardado.
     *
     * @param contrasena Contraseña ingresada al iniciar sesión.
     * @param almacenado Cadena recuperada de la columna {@code password_hash}.
     * @return true si la contraseña coincide y false si no coincide o si el valor guardado es nulo o no empieza con el prefijo de Argon2id.
     */
    public static boolean verificar(String contrasena, String almacenado) {
        if (almacenado == null || !almacenado.startsWith("$argon2id$")) {
            return false;
        }

        byte[] clave = contrasena.getBytes(StandardCharsets.UTF_8);

        try {
            return ARGON2.verify(Argon2Mode.ARGON2_ID, almacenado, clave);
        } finally {
            Arrays.fill(clave, (byte) 0);
        }
    }
}