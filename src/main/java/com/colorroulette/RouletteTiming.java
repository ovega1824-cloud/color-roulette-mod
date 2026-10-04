package com.colorroulette;

/** Tiempos de la animación, en ticks (20 ticks = 1 segundo). */
public final class RouletteTiming {
    /** Duración del glitch de entrada (0.5 s). */
    public static final int GLITCH_IN = 10;
    /** Tick en el que termina el giro y se escucha la campana (4 s). */
    public static final int SPIN_END = 80;
    /** Tick en el que empieza el glitch de salida; entre SPIN_END y aquí el color se queda 2 s. */
    public static final int OUT_START = 120;
    /** Duración total de la animación (6.5 s). */
    public static final int TOTAL = 130;

    private RouletteTiming() {}
}
