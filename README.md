# Config-Mods — Tan's Huge Trees (1.20.1 Forge) · build con fixes de servidor

Repositorio de trabajo para el mod **TansHugeTrees-1.8.0-Forge-1.20.1-BETA** publicado en la release
[v1.0](https://github.com/Santi-PdR/Config-Mods/releases/tag/v1.0).

Aquí está el **código fuente del mod ya parcheado**: configuración de dimensiones por servidor, arreglo de
los saplings que no crecían y limpieza del spam de consola.

```
mod/                        proyecto Gradle completo (Forge 1.20.1 · 47.4.10)
  src/main/java/...         código fuente (base: fork de mantenimiento LMaxRouterCN, ver abajo)
  src/main/resources/#main.zip   pack de datos embebido (69 MB, sin tocar)
```

---

## 1. Configurar en qué dimensiones aparece el mod

Archivo: `config/tanshugetrees/config.toml` (se genera solo al arrancar).

```toml
world_gen_dimensions_mode = 'whitelist'
world_gen_dimensions = 'minecraft-overworld'
```

- `world_gen_dimensions_mode`:
  - `'whitelist'` → el mod **solo** funciona en las dimensiones listadas.
  - `'blacklist'` → el mod funciona en todas **menos** las listadas.
- `world_gen_dimensions` → lista separada por ` / ` o comas. Nombres = id de dimensión tal como sale en el
  log, con `:` cambiado por `-` (también se acepta con `:`).
- **Funciona con cualquier dimensión**, vanilla o de mod, y soporta comodines:

| Ejemplo | Significado |
|---|---|
| `minecraft-overworld` | solo el Overworld |
| `minecraft-overworld / minecraft-the_nether` | Overworld y Nether |
| `aether-the_aether` | The Aether |
| `twilightforest-twilight_forest` | Twilight Forest |
| `deeperdarker-otherside` | Deeper and Darker |
| `twilightforest-*` | **todas** las dimensiones de Twilight Forest |
| `*-the_aether` | cualquier dimensión llamada `the_aether` (de cualquier mod) |
| `minecraft-the_*` | `the_nether`, `the_end`, `the_void`… |
| `minecraft-*` | solo vanilla (todo lo de mods queda fuera) |
| `*` | todas (default, comportamiento original) |

Recetas típicas:

```toml
# Solo Overworld: Aether, Twilight Forest, Nether, End y cualquier dimensión de mod quedan sin árboles
world_gen_dimensions_mode = 'whitelist'
world_gen_dimensions = 'minecraft-overworld'
```

```toml
# Todo menos Aether y Twilight Forest
world_gen_dimensions_mode = 'blacklist'
world_gen_dimensions = 'aether-the_aether / twilightforest-*'
```

Comandos en juego (nivel de permiso 2, o desde la consola del servidor):

| Comando | Para qué |
|---|---|
| `/tanshugetrees dimensions_list` | lista **todas** las dimensiones que tiene el servidor, con su ID exacto y si están `ON`/`OFF` según el filtro actual |
| `/tanshugetrees dimensions_reload` | relee los dos valores de `config.toml` sin reiniciar el mundo |
| `/tanshugetrees restart` | recarga todo el mod |

Notas:

- Lista vacía o `*` = sin filtro (comportamiento original). Si la config está mal escrita, se cae al
  comportamiento sin filtro en vez de dejar el mundo sin árboles.
- El filtro se aplica en **cuatro puntos** del pipeline: la feature de worldgen
  (`WorldGenStepBeforePlants.place`), `WorldGen.stepBeforePlants`, la carga de chunk
  (`EventCenter.eventChunkLoaded`), y la cola de colocación / pregeneración
  (`TreePlacer.start`, `DeferredQueue.add/addForced`, `PregenObserver`). Una dimensión excluida no lee
  disco, no encola trabajo y no genera regiones: cero costo de CPU y memoria.
- La consulta está memoizada por dimensión (el worldgen la llama una vez por chunk).
- Al arrancar queda una línea con el filtro efectivo:
  `[THT] World generation dimensions: whitelist [minecraft-overworld]`.
- Los árboles ya generados no se borran al cambiar la config.

## 2. Saplings que se quedaban congelados y spam en consola

### Causas encontradas

| # | Causa | Efecto |
|---|---|---|
| 1 | `GameUtils.Data.setBlockLogic/setBlockText/setBlockNumber` hacían `persistentData.merge(compound con una sola clave)`, lo que **reemplaza** todo el compound del mod en ese bloque. Al escribir `sapling_started` se borraba el `path` que se acababa de guardar. | El sapling quedaba "iniciado" pero sin preset → se borraba el bloque / no crecía nunca. |
| 2 | `Sapling.click` buscaba el preset en `dev/temporary/presets/#main/<árbol>` (ruta de la 1.5.x). El pack 1.8.0 los guarda en `presets/<árbol>/<árbol>.txt`. | Todos los saplings respondían "Sapling Not Available Yet" y nunca arrancaban. |
| 3 | Sólo se podía arrancar con harina de huesos y 5 % de probabilidad (no-creativo). | Un sapling colocado y abandonado no crecía jamás. |
| 4 | Si no había `path`, el tick generaba un `minecraft:marker` `tree_generator`; `Loops` lo re-ejecuta cada tick, falla y vuelve a loguear. | Spam de consola continuo + entidades basura. |
| 5 | `OutsideUtils.exception` imprimía 3 líneas de separador + stacktrace completo en cada fallo; `Core.logger.info("[TST Command] ...")` se ejecutaba en cada comando `tellraw`/`data merge`; varios `System.out.println("[THT-DEBUG] ...")` sin condicionar. | Cientos de líneas por segundo en el log del servidor. |

### Cambios aplicados

- `GameUtils.Data.setBlockText/setBlockLogic/setBlockNumber` y `setEntityText/setEntityLogic/setEntityNumber`:
  ahora escriben **una sola clave** sobre el compound existente (no lo reemplazan).
- `Sapling.resolvePresetPath` (nuevo): resuelve el preset por nombre de bloque en `dev/temporary/presets`,
  con fallback a `custom_packs/presets`; cachea el resultado por nombre de bloque (no recorre ~900
  archivos en cada tick).
- `Sapling.tick`: rescate de saplings viejos (si `sapling_started` está en `true` y falta `path`, lo
  re-resuelve); si no hay preset, se apaga el estado en vez de crear un marker que falla en bucle.
- `Sapling.autoStart` + `procedures/SaplingTickProcedure`: nueva clave
  **`sapling_auto_grow_chance`** en `config.toml` (default `0.01`, ≈ una vez cada 100 s por sapling).
  Con `0` el comportamiento es el antiguo (sólo harina de huesos). La harina de huesos sigue igual.
- `CustomPackOrganizing`: el pack principal (`#main.zip`) siempre se resuelve con id `#main`, así
  `path_settings = presets/#main/...` se encuentra aunque el `info.txt` del pack no se detecte.
- `TreeGenerator.create`: segundo candidato de ruta (`presets/#main/...`) para packs descomprimidos en
  carpetas con otro nombre.
- `FileManager.extractZIP`: normaliza separadores `\` → `/` y trata como directorio las entradas que
  terminan en `/` (issue #12 del autor: ZIPs hechos en Windows daban cero árboles en servidores Linux).
- Nuevo `outside/LogLimiter`: agrupa los errores repetidos (misma causa = una línea cada 30 s, con
  contador `+N more`) y expone `BoundedSet` para los avisos "una vez por posición".
- `OutsideUtils.exception`, `Caches` (shape data), `TreePlacer` (shape corrupta), `EventCenter` y
  `TreePlacer` (`printStackTrace` → log agrupado), comandos (`[TST Command]` sólo con
  `debug_log_print = true` en `lmax-debuglog.toml`).
- `DebugLogToml` / `lmax-debuglog.toml`: `debug_log_print = true` vuelve a mostrar todo (diagnóstico).

Resultado esperado: el sapling crece solo (o con harina de huesos) y la consola deja de repetir el mismo
error; los primeros avisos siguen apareciendo completos.

## 3. Versión más reciente del mod

- No existe una versión de Minecraft más nueva que portar: el autor (TannyJung) publica **1.8.0** como
  última versión para 1.20.1 Forge y también para 1.21.1 NeoForge. No hay 1.20.2+ / 1.21.x Forge que
  traer a 1.20.1.
- El código de este repo parte del **fork de mantenimiento `LMaxRouterCN/TansHugeTrees`**
  (rama `1.20.1forge-LMaxFixAndImprove`, commit `332c6b9`), que ya incluye sobre el 1.8.0 original:
  configs TOML bilingües, cola de colocación diferida con presupuesto por tick, corrección del "árbol
  partido" entre chunks, guarda de eventos de cliente y `lmax-debuglog.toml`. Es la base más nueva y
  estable disponible para 1.20.1 Forge 47.4.10.
- Sobre esa base se aplicaron los fixes de este repositorio (secciones 1 y 2).

## 4. Compilar

### En GitHub (recomendado, no hace falta instalar nada)

Workflow: `.github/workflows/build.yml` (Actions → **Build mod jar** → *Run workflow*).

1. Entrá a la pestaña **Actions** del repo → `Build mod jar` → **Run workflow** (podés cambiar el sufijo
   del nombre del jar, por defecto `configmods`).
2. Cuando termina (~5 min), el jar queda en **Artifacts → `tanshugetrees-jar`**:
   `TansHugeTrees-1.8.0-Forge-1.20.1-configmods.jar` (~64,4 MB).
3. Alternativa: creá un tag (`git tag v1.1 && git push origin v1.1`) y el workflow adjunta el jar a una
   **release en borrador** automáticamente, lista para publicar.

El workflow usa JDK 17 (Temurin), cachea Gradle, compila con `./gradlew build` y **verifica el jar**
antes de subirlo: que exista `LogLimiter.class`, que las claves `world_gen_dimensions` y
`sapling_auto_grow_chance` estén en `Handcode$Config`, y que el pack `#main.zip` siga embebido (sin él el
mod no genera árboles). Si algo falta, el job falla en vez de publicar un build incompleto.

> Estado: el build de esta rama ya corrió en GitHub Actions y terminó en verde
> (artifact `tanshugetrees-jar`, 64.382.741 bytes).

### En tu máquina

Requiere **JDK 17**:

```bash
cd mod
./gradlew build          # Windows: gradlew.bat build
```

El jar sale en `mod/build/libs/tanshugetrees-1.8.0-<timestamp>.jar`.

> El entorno donde se preparó este código no tenía JDK ni acceso a `maven.minecraftforge.net`, así que
> **el jar no está compilado aquí**: se compila con el workflow de GitHub o localmente con JDK 17. Los
> fuentes modificados se validaron sintácticamente (parser Java) y contra la base original.

## 5. Licencia / créditos

- Mod original: **TannyJung** — *Tan's Huge Trees* (All Rights Reserved).
- Fixes de rendimiento/correctitud de la base: **LMaxRouterCN**.
- Los cambios de este repositorio son parches sobre esa base. **El autor original dio permiso explícito
  para distribuir esta compilación** (por eso el workflow puede adjuntar el jar a una release). Conviene
  mantener el crédito a TannyJung y a LMaxRouterCN en la descripción de la release.
