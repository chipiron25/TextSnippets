# TextSnippets — Instrucciones de instalación

## ¿Qué hace esta app?

Muestra una **ventana flotante semitransparente** con tus textos personalizados
cada vez que tocas un campo de escritura en cualquier app. Al pulsar uno de los
botones de la ventana, el texto se inserta automáticamente en donde estés escribiendo.

---

## Requisitos

- Android Studio (versión Hedgehog 2023.1.1 o superior recomendada)
- Android 8.0 (API 26) o superior en el dispositivo / emulador
- JDK 8 o superior

---

## Pasos para compilar e instalar

### 1. Abrir el proyecto
- Abre Android Studio → "Open" → selecciona la carpeta `TextSnippets`
- Espera a que Gradle sincronice el proyecto (puede tardar unos minutos la primera vez)

### 2. Compilar e instalar
- Conecta tu móvil por USB con **depuración USB activada**, o usa el emulador
- Pulsa el botón ▶ "Run" en Android Studio

### 3. Configurar la app (primera vez)

Una vez instalada:

**a) Activar el Servicio de Accesibilidad**
1. Pulsa "Activar Accesibilidad" en la app
2. En los ajustes de Android, busca **"TextSnippets — Textos Rápidos"**
3. Actívalo y acepta el aviso de permisos

**b) Añadir tus textos rápidos**
1. Vuelve a la app
2. Pulsa "＋ Añadir texto rápido"
3. Escribe el texto completo que quieres insertar con un botón
4. Repite para todos los textos que necesites

### 4. Usar la ventana flotante

- Abre cualquier app (WhatsApp, email, navegador, etc.)
- Toca cualquier campo de texto → aparecerá la ventana flotante
- Pulsa el botón con el texto que quieres insertar → se escribe automáticamente
- La ventana es **arrastrable** (tira desde el encabezado azul oscuro)
- El botón **✕** cierra la ventana manualmente

---

## Notas importantes

- **Permiso de superposición** (`SYSTEM_ALERT_WINDOW`): en la mayoría de dispositivos
  no es necesario porque el servicio usa `TYPE_ACCESSIBILITY_OVERLAY`. Si la ventana
  no aparece, actívalo desde los ajustes de la app.
- Los textos se guardan localmente y persisten entre reinicios.
- Probado en Android 10–14. En algunas ROMs (MIUI, One UI) puede ser necesario
  activar el "inicio automático" de la app para que el servicio sobreviva en segundo plano.

---

## Estructura del proyecto

```
app/src/main/
├── java/com/example/textsnippets/
│   ├── MainActivity.java           ← pantalla de configuración
│   ├── TextAccessibilityService.java ← servicio principal (ventana flotante + inyección de texto)
│   └── SnippetManager.java         ← lectura/escritura de snippets en SharedPreferences
└── res/
    ├── layout/
    │   ├── activity_main.xml       ← pantalla principal
    │   ├── item_snippet.xml        ← fila de cada snippet en la lista
    │   ├── dialog_snippet.xml      ← diálogo de añadir/editar
    │   └── floating_window.xml     ← la ventana flotante
    └── xml/
        └── accessibility_service_config.xml
```
