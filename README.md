# Color Roulette (Forge 1.20.1)

Mod con 8 fichas de colores y una ruleta animada.

## Qué hace
- 8 ítems (pestaña creativa "Color Roulette"): Ficha Morada, Azul, Cian, Verde, Amarilla, Naranja, Roja y Magenta.
- Clic derecho con una ficha: la ruleta aparece en pantalla de **todos los jugadores** y cae en el color de esa ficha.
- Animación: aparece con glitch, giro rápido, se regula y va frenando (~4 s), suena la campana de Minecraft al caer,
  todos los segmentos cambian al color elegido, se queda ~2 s y desaparece con glitch.
- Durante el giro suena un "tic" de ruleta cada vez que pasa un segmento por el puntero (baja de tono al frenar).
- Si alguien usa otra ficha mientras gira, se avisa que ya está girando (así no se encimen).

## Compilar
Requiere JDK 17 y Gradle 8.1.x (el que trae el MDK de Forge 1.20.1).
1. Si no tienes wrapper: copia las carpetas/archivos `gradlew`, `gradlew.bat` y `gradle/` del MDK de Forge 1.20.1 a esta carpeta, o ejecuta `gradle wrapper --gradle-version 8.1.1`.
2. `./gradlew build`
3. El mod queda en `build/libs/colorroulette-1.0.0.jar` → ponlo en la carpeta `mods` (cliente y servidor).
4. Para probar en desarrollo: `./gradlew runClient`

## Probar
`/give @s colorroulette:red_chip` (también blue_chip, green_chip, etc.).

## Ajustes rápidos
- Tiempos: `RouletteTiming.java`
- Velocidad / frenado: `PLATEAU` y `DECAY` en `RouletteClient.java`
- Sonidos: `playClick` y `playBell` en `RouletteClient.java` (puedes cambiar los IDs por sonidos propios en `sounds.json`)

## Compilar en GitHub (sin instalar nada)
1. Sube todo el contenido de esta carpeta a un repositorio de GitHub (incluida la carpeta oculta `.github`).
2. Entra a la pestaña **Actions** del repositorio; el workflow "Build mod" corre solo con cada subida
   (o dale "Run workflow").
3. Cuando termine con palomita verde, abre la ejecución y descarga **colorroulette-jar** en la sección *Artifacts*.
   Viene en un .zip; dentro está el .jar.
