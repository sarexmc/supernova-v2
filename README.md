# Supernova (Forge 1.20.1)

Mod de **Forge 1.20.1** (Java 17): supernova en dos partes, bunker, nave espacial y el planeta **Gleba**.

## Compilar
- GitHub lo compila solo (pestana **Actions** -> artifact `supernova-mod`), o en local: copia `gradlew` + `gradle/` de un MDK 1.20.1 y `./gradlew build`.

## Comandos (operador)
| Comando | Que hace |
|---|---|
| `/supernova part1` / `part2` / `stop` | Las dos partes de la supernova |
| `/supernova bunker` (`bunker tp`) | Traza un bunker subterraneo oscuro bajo ti |
| `/supernova ship` | Coloca base 3x3 + nave y te da 2 tanques de dilitio |
| `/supernova gleba` | Te lleva a Gleba (pruebas) |

## La nave
1. Click derecho en la nave -> mete tanques de dilitio (+25 % cada uno, minimo 50 %) -> **ENCENDER MOTORES**.
2. Sube a la tripulacion (hasta 4, a menos de 6 bloques) y hay que **mantener CTRL** hasta llegar a 100 % (soltar = cancelar).
3. 3-2-1 -> despegue (la nave destruye lo que haya encima, tiembla la camara) -> a los 450 m anillo de condensacion y nubes ->
   a los 600 m aparece la Tierra texturizada -> la nave gira y aparece Gleba como circulo rojo -> salto temporal (aceleracion con efecto de pantalla)
   -> Tierra pequena + Gleba enorme -> aproximacion con fuego de reentrada -> aterrizaje con base + nave.
4. Los pasajeros van dentro de la cabina (no se dibuja su skin).

## Gleba (sin datapack = sin avisos "experimentales")
La dimension `supernova:gleba` se crea **por codigo** al arrancar el servidor (`GlebaDimension`). No toca `level.dat`, no usa datapack
y funciona en mundos ya existentes. Terreno propio con tres zonas (pantano de hongos con arboles de tallo y sombrero luminoso, tierras altas de
cristal y desierto rosa), montanas, lagos y bloques nuevos. Si algo fallara, el log dice `[Supernova] No se pudo crear la dimension Gleba`.

## Recursos incluidos (ya van dentro del mod; no hace falta un texture pack aparte)
Texturas procedurales de bloques, nave, Tierra y Gleba; modelo de la nave; sonidos nuevos (explosion, retumbo, motor, carga de motores, salto, pitido, zumbido).

## Fuente espacial (opcional)
Pon un .ttf en `assets/supernova/font/` y crea `assets/supernova/font/space.json`:
```json
{"providers":[{"type":"ttf","file":"supernova:space.ttf","size":11.0,"oversample":8.0,"shift":[0,0]}]}
```

## Ajustes
Tiempos: `Timeline.java` / `ShipEntity.serverTick`. Onda expansiva sin muerte: `SupernovaManager.KILL_UNPROTECTED = false`.

## Aviso
Escrito sin poder compilar contra Forge; puede requerir corregir algun nombre de API en la primera compilacion.
