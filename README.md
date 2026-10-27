<img width="2816" height="1536" alt="Gemini_Generated_Image_rtqpj9rtqpj9rtqp" src="https://github.com/user-attachments/assets/c526ea21-4a39-4130-b490-3fc93e19f139" />

# NexusStock

Aplicación Android para gestionar el inventario de una empresa, con **Nexus**, un asistente integrado que entiende órdenes escritas en lenguaje natural ("añade 10 tornillos", "¿cuántos tornillos hay?") y las aplica directamente sobre el inventario.

Diseño moderno y minimalista: fondo negro con rejilla gris, acentos en azul eléctrico y texto blanco.

## Funcionalidades

- **Inicio**: resumen con número de productos, unidades totales, valor del inventario y productos con stock bajo.
- **Inventario**: listado con búsqueda, alta y edición de productos y ajuste rápido de stock con los botones `+` / `−`.
- **Nexus (chat)**: asistente con IA (Groq · `gpt-oss-120b`) que consulta y modifica el inventario mediante *tool calling*. Sin conexión o sin API key funciona en modo básico con reglas locales.
- Persistencia local con Room: los datos se guardan en el dispositivo.

## Nexus con IA (Groq)

Nexus envía tu mensaje a Groq junto con una lista de herramientas (`listar_productos`, `ajustar_stock`, `guardar_producto`, `eliminar_producto`, `stock_bajo`…). El modelo decide qué herramientas usar, la app las ejecuta contra la base de datos local y el modelo redacta la respuesta. Así entiende peticiones libres como *"entraron 20 cajas de cartón a 1,50 € y avísame de lo que haya que reponer"*.

### Configurar la API key

1. Crea una clave en [console.groq.com/keys](https://console.groq.com/keys).
2. Añádela a `local.properties` (ese archivo no se sube a git):

   ```properties
   GROQ_API_KEY=gsk_tu_clave
   ```

3. Sincroniza Gradle y ejecuta la app. En el chat verás **IA · Groq** bajo el nombre de Nexus.

> Aviso: en un prototipo la clave va dentro del APK y se puede extraer. Para publicar la app, pon la llamada a Groq en un backend propio.

### Modo básico (respaldo)

Si no hay API key, o falla la conexión antes de tocar el inventario, Nexus responde con reglas locales. Si la conexión falla después de haber hecho cambios, no repite la orden, para no aplicarla dos veces.

### Comandos del modo básico

| Acción | Ejemplo |
|---|---|
| Añadir stock (crea el producto si no existe) | `añade 10 tornillos` |
| Retirar stock | `quita 3 unidades de tornillos` |
| Consultar stock | `¿cuántos tornillos hay?` · `stock de tornillos` |
| Ver todo | `lista` · `inventario` |
| Productos bajo mínimos | `stock bajo` |
| Buscar | `busca torn` |
| Eliminar producto | `elimina tornillos` |
| Ver ayuda | `ayuda` |

## Tecnologías

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Room** para la base de datos local
- **Navigation Compose**
- **Coroutines / Flow** + `StateFlow`
- **Groq API** (`openai/gpt-oss-120b`) con *tool calling*, usando `HttpURLConnection` y `org.json` sin librerías extra
- Inyección de dependencias manual (`AppContainer`)

## Arquitectura: MVVM

```
Vista (Compose)  ──eventos──▶  ViewModel  ──▶  Repositorio  ──▶  Room (DAO)
      ▲                            │
      └──────── StateFlow ◀────────┘
```

- **Modelo**: datos y lógica de acceso (`data/`). La UI solo conoce la interfaz `InventoryRepository`.
- **Vista**: pantallas Compose sin lógica de negocio (`ui/**/…Screen.kt`).
- **ViewModel**: expone un `UiState` inmutable mediante `StateFlow` y recibe las acciones del usuario (`ui/**/…ViewModel.kt`).

## Estructura del proyecto

```
app/src/main/java/com/nexusstock/
├── NexusStockApp.kt          # Application: crea el AppContainer
├── MainActivity.kt
├── di/AppContainer.kt        # Dependencias (BD, repositorio, bot)
├── data/
│   ├── model/                # Product, ChatMessage
│   ├── local/                # Room: entidad, DAO y base de datos
│   └── repository/           # InventoryRepository + implementación
├── bot/
│   ├── NexusBot.kt           # Elige IA o modo básico (respaldo)
│   ├── GroqNexus.kt          # Conversación con Groq + bucle de herramientas
│   ├── InventoryTools.kt     # Herramientas que el modelo puede invocar
│   ├── GroqClient.kt         # Llamada HTTP a la API de Groq
│   └── RuleBasedNexus.kt     # Reglas locales sin conexión
└── ui/
    ├── theme/                # Colores, tipografía y tema oscuro
    ├── components/           # Fondo con rejilla, tarjetas
    ├── navigation/           # NavHost + barra inferior
    ├── dashboard/            # Pantalla de inicio
    ├── inventory/            # Listado y editor de productos
    └── chat/                 # Chat con Nexus
```

## Cómo ejecutarlo

1. Abrir el proyecto en **Android Studio** (versión reciente con soporte para AGP 9).
2. Esperar a que Gradle sincronice.
3. Ejecutar la configuración `app` en un emulador o dispositivo con **Android 8.0 (API 26)** o superior.

Desde terminal:

```bash
./gradlew assembleDebug
```

Tests del bot (reglas locales y bucle de herramientas con una API simulada):

```bash
./gradlew testDebugUnitTest
```

## Próximos pasos

- Mover la llamada a Groq a un backend para no incluir la API key en el APK.
- Guardar el historial del chat.
- Categorías y filtros avanzados.
- Lectura de códigos de barras.
- Sincronización en la nube y multiusuario.
