# Guía de montaje de Kanto

Esta campaña no contiene coordenadas inventadas. Entra a Kanto, activa
`/emiprogresion admin on`, párate exactamente donde debe quedar cada personaje
y ejecuta el comando indicado. El NPC mirará en la misma dirección que tú.

Los comandos de entrenador eligen automáticamente el siguiente equipo no usado.
Repite el mismo comando el número de veces indicado. Si algo queda mal, párate a
menos de ocho bloques y usa `montaje mover_cercano` o `eliminar_cercano`.

## 1. Pueblo Paleta y Ruta 1

En el laboratorio/casas:

```text
/emiprogresion colocar npc oak
/emiprogresion colocar npc delia
/emiprogresion colocar npc vecino_paleta
/emiprogresion colocar jefe rival_paleta
/emiprogresion colocar terminal Pueblo Paleta
```

En la Ruta 1, coloca dos entrenadores y una Poképarada entre ellos o al final:

```text
/emiprogresion colocar entrenador ruta_1
/emiprogresion colocar entrenador ruta_1
/emiprogresion colocar pokeparada
```

## 2. Ciudad Verde y Bosque Verde

```text
/emiprogresion colocar npc dependiente_verde
/emiprogresion colocar npc enfermera_verde
/emiprogresion colocar npc guardia_giovanni
/emiprogresion colocar gimnasio 8 entrada
/emiprogresion colocar terminal Ciudad Verde
```

El dependiente va detrás del mostrador real de la tienda. La entrada del gimnasio
8 va en el umbral, mirando hacia el interior. En el Bosque Verde coloca cinco:

```text
/emiprogresion colocar entrenador bosque_verde
```

Repite cinco veces. Coloca una Poképarada tras el segundo y otra cerca de la
salida si el tramo se siente largo.

## 3. Ciudad Plateada, Brock y Ruta 3

```text
/emiprogresion colocar npc enfermera_plateada
/emiprogresion colocar npc guia_plateada
/emiprogresion colocar gimnasio 1 entrada
/emiprogresion colocar gimnasio 1 entrenador
/emiprogresion colocar gimnasio 1 entrenador
/emiprogresion colocar gimnasio 1 lider
/emiprogresion colocar terminal Ciudad Plateada
```

Coloca cinco entrenadores de `ruta_3`; distribuye una Poképarada aproximadamente
después del segundo o tercero.

## 4. Monte Moon y Ciudad Celeste

Dentro de Monte Moon coloca cuatro entrenadores de `monte_luna`, el investigador
al final del recorrido y una o dos Poképaradas según la longitud:

```text
/emiprogresion colocar entrenador monte_luna
/emiprogresion colocar npc cientifico_monte_luna
```

En Ciudad Celeste y el cabo:

```text
/emiprogresion colocar npc enfermera_celeste
/emiprogresion colocar npc guia_celeste
/emiprogresion colocar gimnasio 2 entrada
/emiprogresion colocar gimnasio 2 entrenador
/emiprogresion colocar gimnasio 2 entrenador
/emiprogresion colocar gimnasio 2 lider
/emiprogresion colocar jefe rival_celeste
/emiprogresion colocar npc bill
/emiprogresion colocar terminal Ciudad Celeste
```

En el Puente Pepita y el cabo coloca cinco entrenadores de `rutas_24_25`.

## 5. Ciudad Carmín y S.S. Anne

En las rutas 5 y 6 coloca cuatro entrenadores de `rutas_5_6` y al menos una
Poképarada. En la ciudad/barco:

```text
/emiprogresion colocar npc enfermera_carmin
/emiprogresion colocar npc presidente_fanclub
/emiprogresion colocar jefe rival_ss_anne
/emiprogresion colocar npc capitan_anne
/emiprogresion colocar gimnasio 3 entrada
/emiprogresion colocar gimnasio 3 entrenador
/emiprogresion colocar gimnasio 3 entrenador
/emiprogresion colocar gimnasio 3 entrenador
/emiprogresion colocar gimnasio 3 lider
/emiprogresion colocar terminal Ciudad Carmín
```

Dentro del S.S. Anne coloca cinco entrenadores de `ss_anne`.

## 6. Túnel Roca, Lavanda y Azulona

Coloca cuatro entrenadores de `rutas_9_10`, cuatro de `tunel_roca` y Poképaradas
en ambos tramos. En Pueblo Lavanda:

```text
/emiprogresion colocar npc enfermera_lavanda
/emiprogresion colocar npc senor_fuji
/emiprogresion colocar terminal Pueblo Lavanda
```

En la Torre Pokémon coloca cinco entrenadores de `torre_pokemon`.

En Ciudad Azulona:

```text
/emiprogresion colocar npc enfermera_azulona
/emiprogresion colocar npc anciana_te
/emiprogresion colocar npc informante_rocket
/emiprogresion colocar jefe giovanni_azulona
/emiprogresion colocar gimnasio 4 entrada
/emiprogresion colocar gimnasio 4 entrenador
/emiprogresion colocar gimnasio 4 entrenador
/emiprogresion colocar gimnasio 4 entrenador
/emiprogresion colocar gimnasio 4 lider
/emiprogresion colocar terminal Ciudad Azulona
```

Nota: el identificador correcto del jefe es `giovanni_azulona` (con doble n).
Dentro de la guarida coloca cuatro entrenadores de `guarida_rocket`.

## 7. Ciudad Fucsia

En las rutas costeras 12–15 coloca cinco entrenadores de `rutas_12_15` y dos
Poképaradas si el recorrido es largo.

```text
/emiprogresion colocar npc enfermera_fucsia
/emiprogresion colocar npc guarda_safari
/emiprogresion colocar gimnasio 5 entrada
/emiprogresion colocar gimnasio 5 entrenador
/emiprogresion colocar gimnasio 5 entrenador
/emiprogresion colocar gimnasio 5 entrenador
/emiprogresion colocar gimnasio 5 entrenador
/emiprogresion colocar gimnasio 5 lider
/emiprogresion colocar terminal Ciudad Fucsia
```

## 8. Silph S.A. y Ciudad Azafrán

En Silph coloca cinco entrenadores de `silph` y estos personajes:

```text
/emiprogresion colocar jefe rival_silph
/emiprogresion colocar jefe giovanni_silph
/emiprogresion colocar npc presidente_silph
```

En Ciudad Azafrán:

```text
/emiprogresion colocar npc enfermera_azafran
/emiprogresion colocar gimnasio 6 entrada
/emiprogresion colocar gimnasio 6 entrenador
/emiprogresion colocar gimnasio 6 entrenador
/emiprogresion colocar gimnasio 6 entrenador
/emiprogresion colocar gimnasio 6 entrenador
/emiprogresion colocar gimnasio 6 lider
/emiprogresion colocar terminal Ciudad Azafrán
```

## 9. Ciclovía, mar, Islas Espuma e Isla Canela

Coloca cuatro entrenadores de `rutas_16_18`, cinco de `rutas_marinas`, cuatro de
`islas_espuma` y Poképaradas según la distancia. En Isla Canela:

```text
/emiprogresion colocar npc enfermera_canela
/emiprogresion colocar npc investigador_mansion
/emiprogresion colocar gimnasio 7 entrada
/emiprogresion colocar gimnasio 7 entrenador
/emiprogresion colocar gimnasio 7 entrenador
/emiprogresion colocar gimnasio 7 entrenador
/emiprogresion colocar gimnasio 7 entrenador
/emiprogresion colocar gimnasio 7 lider
/emiprogresion colocar terminal Isla Canela
```

Dentro de la Mansión coloca cuatro entrenadores de `mansion_canela`. En la Ruta
21 coloca tres de `ruta_21`.

## 10. Giovanni, Calle Victoria y Liga

Regresa al gimnasio de Ciudad Verde y coloca, además de la entrada ya preparada:

```text
/emiprogresion colocar gimnasio 8 entrenador
/emiprogresion colocar gimnasio 8 entrenador
/emiprogresion colocar gimnasio 8 entrenador
/emiprogresion colocar gimnasio 8 entrenador
/emiprogresion colocar gimnasio 8 lider
/emiprogresion colocar jefe rival_liga
```

En Ruta 22 coloca tres entrenadores de `ruta_22`. En Calle Victoria coloca seis
de `calle_victoria`, Poképaradas de descanso y la recepción al final:

```text
/emiprogresion colocar npc recepcion_liga
/emiprogresion colocar terminal Meseta Añil
/emiprogresion colocar alto_mando 1
/emiprogresion colocar alto_mando 2
/emiprogresion colocar alto_mando 3
/emiprogresion colocar alto_mando 4
/emiprogresion colocar campeon
```

## Revisión final

```text
/emiprogresion montaje validar
/emiprogresion montaje reconstruir
/emiprogresion story reset
/emiprogresion admin off
```

`montaje validar` informa los elementos pendientes y cualquier equipo inexistente.
Prueba la historia con un jugador limpio; el modo de montaje desactivado permite
comprobar los cierres de gimnasio exactamente como los verá un jugador normal.
