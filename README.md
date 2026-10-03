# fe-mobile

Cliente movil de Solventa: Android nativo en Kotlin con Jetpack Compose.

## Como correrlo

```
./gradlew assembleDebug        # APK de depuracion
./gradlew testDebugUnitTest    # reglas de negocio, JUnit 5
./gradlew connectedAndroidTest # interfaz instrumentada, Espresso y Compose
./gradlew detekt               # estilo
```

### El JDK

Requiere **JDK 21**. Gradle 8.13 **no soporta el JDK 25** que viene dentro de
Android Studio: la compilacion falla con el numero de version como unico
mensaje de error.

Android Studio resuelve esto solo. Al abrir el proyecto descarga un JDK 21 y
lo deja en:

```
C:\Users\<usuario>\.jdks\jbr-21.0.11
```

**Ese es el JDK del proyecto.** Conviene que todos usemos el mismo, en el IDE
y en la consola, para no levantar un daemon de Gradle por cada version:

- **En Android Studio:** Settings, Build, Execution, Deployment, Build Tools,
  Gradle, campo *Gradle JDK*.
- **En la consola:** `JAVA_HOME` apuntando a esa misma ruta.

Si Android Studio avisa que `JAVA_HOME` y el JDK de Gradle son distintos, la
solucion es apuntar `JAVA_HOME` al JDK 21, **nunca al reves**: igualar hacia
el JDK que trae el IDE rompe la compilacion.

El SDK se declara en `local.properties`, que no se versiona:

```
sdk.dir=C:/ruta/al/Android/Sdk
```

## El tema no se escribe a mano

Los colores, la escala tipografica y los radios salen de las variables del
archivo de Figma **Solventa Design System**, volcadas en
`design/figma-tokens.json`. Es el **mismo archivo que usa el cliente web**: de
ahi sale la paridad de estilo entre los dos canales.

```
python tools/tokens_to_kotlin.py
```

Eso regenera `ui/tema/Color.kt`, `Esquemas.kt`, `Tipografia.kt` y `Formas.kt`.
Esos cuatro archivos **no se editan**: se vuelve a extraer de Figma y se
regenera. `TemaTest` falla si alguien escribe un color a mano.

El producto es claro. El esquema oscuro queda declarado porque el archivo de
Figma lo trae, pero no se activa solo: el prototipo se diseno en claro y es lo
que se valido con usuarios.

## Estructura

```
app/src/main/kotlin/com/solventa4bits/movil/
  MainActivity.kt        pantalla de arranque, se reemplaza por el recorrido
  ui/tema/               el tema, generado desde Figma
app/src/test/kotlin/     reglas de negocio con JUnit 5
app/src/androidTest/     interfaz instrumentada con Espresso y Compose
design/                  el volcado de variables de Figma
tools/                   el generador del tema
```

## Decisiones

- **minSdk 26.** Es el piso que sostiene biometria, camara, notificaciones y
  ubicacion con las APIs modernas.
- **Compose con Material 3.** El prototipo es M3, y los tokens del Design
  System son los de M3: el mapeo es directo.
- **JUnit 5 para reglas de negocio, Espresso para interfaz.** Es lo que declara
  la Estrategia de Pruebas.
- **Las copias de seguridad quedan deshabilitadas.** La app maneja datos
  financieros y de identidad bajo consentimiento.
