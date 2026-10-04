package com.colorroulette.client;

import com.colorroulette.RouletteColor;
import com.colorroulette.RouletteTiming;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

import static com.colorroulette.RouletteTiming.GLITCH_IN;
import static com.colorroulette.RouletteTiming.OUT_START;
import static com.colorroulette.RouletteTiming.SPIN_END;
import static com.colorroulette.RouletteTiming.TOTAL;

/**
 * Animación de la ruleta en pantalla (se ve en el cliente de todos los jugadores).
 *
 * Línea de tiempo:
 *   0 - 0.5 s   aparece con glitch (el giro ya va rápido)
 *   0 - 4 s     giro rápido -> se regula -> va frenando hasta caer en el color
 *   4 s         suena la campana y todos los segmentos cambian al color elegido
 *   4 - 6 s     el color se queda 2 segundos
 *   6 - 6.5 s   desaparece con glitch
 */
public final class RouletteClient {
    private static final int SEGMENTS = 8;
    private static final float SEGMENT_DEG = 360f / SEGMENTS;
    /** El puntero está a la izquierda (270 grados en sentido horario desde arriba), como en la imagen. */
    private static final float POINTER_DEG = 270f;

    // Perfil de velocidad: velocidad constante hasta A, luego frena de forma suave.
    private static final float PLATEAU = 0.25f;
    private static final float DECAY = 1.6f;

    private static final Deque<Anim> QUEUE = new ArrayDeque<>();
    private static Anim current;
    private static int age;
    private static int lastSegment;

    private RouletteClient() {}

    // ------------------------------------------------------------------ estado

    public static void start(RouletteColor target, float jitter, int turns, String playerName) {
        QUEUE.add(new Anim(target, jitter, turns, playerName));
    }

    public static void clear() {
        QUEUE.clear();
        current = null;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) {
            return;
        }
        if (current == null) {
            current = QUEUE.poll();
            if (current == null) {
                return;
            }
            age = 0;
            lastSegment = current.segmentAt(0f);
        }

        age++;

        if (age <= SPIN_END) {
            int segment = current.segmentAt(age);
            if (age == SPIN_END) {
                playBell();
            } else if (segment != lastSegment) {
                playClick(1f - age / (float) SPIN_END);
            }
            lastSegment = segment;
        }

        if (age >= TOTAL) {
            current = null;
        }
    }

    // ------------------------------------------------------------------ sonido

    private static void play(String soundId, float pitch, float volume) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(soundId));
        if (sound != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
        }
    }

    /** "Tic" de ruleta cada vez que pasa un segmento por el puntero; baja de tono al frenar. */
    private static void playClick(float speed) {
        play("block.note_block.hat", 0.9f + 0.8f * speed, 0.7f);
    }

    /** Campana de Minecraft cuando la ruleta se detiene. */
    private static void playBell() {
        play("block.note_block.bell", 1.0f, 1.0f);
    }

    // ------------------------------------------------------------------ animación

    private static final class Anim {
        final RouletteColor target;
        final String playerName;
        final float totalDeg;

        Anim(RouletteColor target, float jitter, int turns, String playerName) {
            this.target = target;
            this.playerName = playerName;
            // Ángulo final para que el centro del segmento objetivo quede bajo el puntero.
            float finalAngle = POINTER_DEG - SEGMENT_DEG / 2f - target.ordinal() * SEGMENT_DEG + jitter;
            this.totalDeg = turns * 360f + mod360(finalAngle);
        }

        float theta(float a) {
            return totalDeg * profile(a / SPIN_END);
        }

        int segmentAt(float a) {
            float local = mod360(POINTER_DEG - theta(a));
            return (int) (local / SEGMENT_DEG) % SEGMENTS;
        }
    }

    /** Posición normalizada (0..1) del giro: rápido, se regula y frena hasta 0 de velocidad. */
    private static float profile(float t) {
        t = clamp01(t);
        float norm = PLATEAU + (1f - PLATEAU) / (DECAY + 1f);
        float pos;
        if (t < PLATEAU) {
            pos = t;
        } else {
            float u = (t - PLATEAU) / (1f - PLATEAU);
            pos = PLATEAU + (1f - PLATEAU) * (1f - (float) Math.pow(1f - u, DECAY + 1f)) / (DECAY + 1f);
        }
        return pos / norm;
    }

    private static float glitchAmount(float a) {
        if (a < GLITCH_IN) {
            return 1f - a / GLITCH_IN;
        }
        if (a >= OUT_START) {
            return (a - OUT_START) / (TOTAL - OUT_START);
        }
        if (a >= SPIN_END && a < SPIN_END + 4) {
            return 0.3f * (1f - (a - SPIN_END) / 4f); // pequeño temblor al caer
        }
        return 0f;
    }

    // ------------------------------------------------------------------ render

    public static void render(GuiGraphics gg, float partialTick, int screenW, int screenH) {
        Anim an = current;
        if (an == null) {
            return;
        }
        float a = Math.min(age + partialTick, TOTAL);

        float radius = Math.max(30f, Math.min(screenH * 0.22f, screenW * 0.22f));
        float cx = screenW / 2f;
        float cy = screenH * 0.40f;

        float theta = an.theta(a);
        float morph = clamp01((a - SPIN_END) / 10f);
        float glitch = glitchAmount(a);
        Random rnd = new Random((long) (a * 3f) * 7919L + System.identityHashCode(an));

        // Parpadeo de visibilidad al aparecer / desaparecer.
        float alpha = 1f;
        if (a < GLITCH_IN || a >= OUT_START) {
            float visible = a < GLITCH_IN ? a / GLITCH_IN : 1f - (a - OUT_START) / (TOTAL - OUT_START);
            alpha = rnd.nextFloat() < visible + 0.1f ? 0.55f + 0.45f * rnd.nextFloat() : 0f;
        }
        if (alpha <= 0f) {
            drawNoise(gg, rnd, cx, cy, radius, Math.max(glitch, 0.4f), an);
            return;
        }

        if (glitch <= 0.001f) {
            drawWheel(gg, cx, cy, radius, theta, an, morph, alpha, 1f, 1f, 1f, false, a);
            return;
        }

        // Separación de canales rojo / cian.
        float shift = glitch * radius * 0.06f;
        drawWheel(gg, cx - shift, cy, radius, theta, an, morph, alpha * 0.45f, 1f, 0.1f, 0.1f, true, a);
        drawWheel(gg, cx + shift, cy, radius, theta, an, morph, alpha * 0.45f, 0.1f, 1f, 1f, true, a);

        // Rebanadas horizontales desplazadas al azar.
        int top = Math.round(cy - radius * 1.3f);
        int bottom = Math.round(cy + radius * 2.0f);
        int bands = 18;
        for (int b = 0; b < bands; b++) {
            int y0 = top + (bottom - top) * b / bands;
            int y1 = top + (bottom - top) * (b + 1) / bands;
            float dx = 0f;
            if (rnd.nextFloat() < 0.55f * glitch) {
                dx = (rnd.nextFloat() - 0.5f) * 2f * glitch * radius * 0.55f;
            }
            gg.enableScissor(0, y0, screenW, y1);
            drawWheel(gg, cx + dx, cy, radius, theta, an, morph, alpha, 1f, 1f, 1f, false, a);
            gg.disableScissor();
        }

        drawNoise(gg, rnd, cx, cy, radius, glitch, an);
    }

    /** Barras de ruido de colores típicas de un glitch. */
    private static void drawNoise(GuiGraphics gg, Random rnd, float cx, float cy, float radius, float glitch, Anim an) {
        int bars = (int) (glitch * 7f) + 1;
        for (int i = 0; i < bars; i++) {
            RouletteColor c = RouletteColor.byIndex(rnd.nextInt(SEGMENTS));
            int w = (int) (radius * (0.3f + 1.1f * rnd.nextFloat()));
            int h = 1 + rnd.nextInt(4);
            int x = (int) (cx - radius * 1.2f + rnd.nextFloat() * radius * 2.4f);
            int y = (int) (cy - radius * 1.1f + rnd.nextFloat() * radius * 2.2f);
            int argb = (0x99 << 24) | c.rgb;
            gg.fill(x, y, x + w, y + h, argb);
        }
    }

    private static void drawWheel(GuiGraphics gg, float cx, float cy, float R, float theta, Anim an,
                                  float morph, float alpha, float tr, float tg, float tb,
                                  boolean ghost, float a) {
        Matrix4f m = gg.pose().last().pose();
        float[] tint = {tr, tg, tb};
        float[] target = rgb(an.target.rgb);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder b = tesselator.getBuilder();
        b.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        float[] dark = mul(new float[]{0.10f, 0.07f, 0.06f}, tint);

        // Aro exterior oscuro.
        ring(b, m, cx, cy, R, R * 1.06f, dark, alpha);

        // Segmentos con degradado (centro claro -> borde del color).
        int steps = 10;
        float stepDeg = SEGMENT_DEG / steps;
        for (int i = 0; i < SEGMENTS; i++) {
            float[] base = rgb(RouletteColor.byIndex(i).rgb);
            float[] shown = mul(lerp(base, target, morph), tint);
            float[] center = lerp(shown, new float[]{1f, 1f, 1f}, 0.40f);
            for (int s = 0; s < steps; s++) {
                float a1 = theta + i * SEGMENT_DEG + s * stepDeg;
                float a2 = a1 + stepDeg;
                v(b, m, cx, cy, center, alpha);
                v(b, m, px(cx, R, a1), py(cy, R, a1), shown, alpha);
                v(b, m, px(cx, R, a2), py(cy, R, a2), shown, alpha);
            }
        }

        // Separadores oscuros entre segmentos.
        float w = Math.max(1f, R * 0.014f);
        for (int i = 0; i < SEGMENTS; i++) {
            float ang = theta + i * SEGMENT_DEG;
            float dx = (float) Math.sin(Math.toRadians(ang));
            float dy = -(float) Math.cos(Math.toRadians(ang));
            float nx = -dy * w;
            float ny = dx * w;
            float ex = cx + dx * R;
            float ey = cy + dy * R;
            quad(b, m, cx - nx, cy - ny, cx + nx, cy + ny, ex + nx, ey + ny, ex - nx, ey - ny, dark, alpha);
        }

        // Brillo en el borde.
        ring(b, m, cx, cy, R * 0.965f, R, new float[]{1f, 1f, 1f}, alpha * 0.25f);

        // Centro dorado.
        disc(b, m, cx, cy, R * 0.13f, dark, dark, alpha);
        disc(b, m, cx, cy, R * 0.10f,
                mul(new float[]{1f, 0.92f, 0.55f}, tint), mul(new float[]{0.75f, 0.52f, 0.12f}, tint), alpha);

        // Puntero a la izquierda (borde blanco + relleno rojo).
        float s = R / 120f;
        float[] white = mul(new float[]{1f, 1f, 1f}, tint);
        float[] red = mul(new float[]{0.95f, 0.15f, 0.15f}, tint);
        triangle(b, m, cx - R + 17f * s, cy, cx - R - 33f * s, cy - 17f * s, cx - R - 33f * s, cy + 17f * s, white, alpha);
        triangle(b, m, cx - R + 12f * s, cy, cx - R - 28f * s, cy - 13f * s, cx - R - 28f * s, cy + 13f * s, red, alpha);

        tesselator.end();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        if (ghost) {
            return;
        }

        // Textos: jugador y, al caer, el color elegido.
        Minecraft mc = Minecraft.getInstance();
        int ai = (int) (alpha * 255f);
        if (ai > 8) {
            Component who = Component.translatable("message.colorroulette.used_by", an.playerName);
            gg.drawCenteredString(mc.font, who, Math.round(cx), Math.round(cy + R * 1.2f), (ai << 24) | 0xFFFFFF);
        }
        float ta = alpha * clamp01((a - SPIN_END) / 6f);
        int ti = (int) (ta * 255f);
        if (ti > 8) {
            Component name = Component.translatable("color.colorroulette." + an.target.id)
                    .withStyle(ChatFormatting.BOLD);
            gg.pose().pushPose();
            gg.pose().translate(cx, cy + R * 1.2f + 14f, 0f);
            gg.pose().scale(2f, 2f, 1f);
            gg.drawCenteredString(mc.font, name, 0, 0, (ti << 24) | an.target.rgb);
            gg.pose().popPose();
        }
    }

    // ------------------------------------------------------------------ geometría

    private static float px(float cx, float r, float deg) {
        return cx + r * (float) Math.sin(Math.toRadians(deg));
    }

    private static float py(float cy, float r, float deg) {
        return cy - r * (float) Math.cos(Math.toRadians(deg));
    }

    private static void v(BufferBuilder b, Matrix4f m, float x, float y, float[] c, float alpha) {
        b.vertex(m, x, y, 0f).color(c[0], c[1], c[2], alpha).endVertex();
    }

    private static void triangle(BufferBuilder b, Matrix4f m, float x1, float y1, float x2, float y2,
                                 float x3, float y3, float[] c, float alpha) {
        v(b, m, x1, y1, c, alpha);
        v(b, m, x2, y2, c, alpha);
        v(b, m, x3, y3, c, alpha);
    }

    private static void quad(BufferBuilder b, Matrix4f m, float x1, float y1, float x2, float y2,
                             float x3, float y3, float x4, float y4, float[] c, float alpha) {
        triangle(b, m, x1, y1, x2, y2, x3, y3, c, alpha);
        triangle(b, m, x1, y1, x3, y3, x4, y4, c, alpha);
    }

    private static void ring(BufferBuilder b, Matrix4f m, float cx, float cy, float inner, float outer,
                             float[] c, float alpha) {
        int n = 72;
        for (int k = 0; k < n; k++) {
            float a1 = k * 360f / n;
            float a2 = (k + 1) * 360f / n;
            quad(b, m,
                    px(cx, outer, a1), py(cy, outer, a1),
                    px(cx, outer, a2), py(cy, outer, a2),
                    px(cx, inner, a2), py(cy, inner, a2),
                    px(cx, inner, a1), py(cy, inner, a1),
                    c, alpha);
        }
    }

    private static void disc(BufferBuilder b, Matrix4f m, float cx, float cy, float r,
                             float[] centerColor, float[] edgeColor, float alpha) {
        int n = 28;
        for (int k = 0; k < n; k++) {
            float a1 = k * 360f / n;
            float a2 = (k + 1) * 360f / n;
            v(b, m, cx, cy, centerColor, alpha);
            v(b, m, px(cx, r, a1), py(cy, r, a1), edgeColor, alpha);
            v(b, m, px(cx, r, a2), py(cy, r, a2), edgeColor, alpha);
        }
    }

    // ------------------------------------------------------------------ utilidades

    private static float[] rgb(int rgb) {
        return new float[]{((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f};
    }

    private static float[] lerp(float[] a, float[] b, float t) {
        return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    private static float[] mul(float[] c, float[] tint) {
        return new float[]{c[0] * tint[0], c[1] * tint[1], c[2] * tint[2]};
    }

    private static float clamp01(float x) {
        return x < 0f ? 0f : Math.min(x, 1f);
    }

    private static float mod360(float x) {
        return ((x % 360f) + 360f) % 360f;
    }
}
