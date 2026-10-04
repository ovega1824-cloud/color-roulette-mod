package com.colorroulette;

/**
 * Los 8 colores de la ruleta, en orden horario empezando desde arriba.
 * El orden define a qué segmento corresponde cada color.
 */
public enum RouletteColor {
    PURPLE("purple", 0x7B3BFF),
    BLUE("blue", 0x2F52FF),
    CYAN("cyan", 0x00D9E6),
    GREEN("green", 0x2EE04F),
    YELLOW("yellow", 0xFFE32E),
    ORANGE("orange", 0xFF8A1C),
    RED("red", 0xFF2E2E),
    MAGENTA("magenta", 0xE62EDB);

    public final String id;
    public final int rgb;

    RouletteColor(String id, int rgb) {
        this.id = id;
        this.rgb = rgb;
    }

    public static RouletteColor byIndex(int index) {
        RouletteColor[] values = values();
        return values[Math.floorMod(index, values.length)];
    }
}
