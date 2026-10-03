# -*- coding: utf-8 -*-
"""Convierte el volcado de variables de Figma en el tema de Compose.

La fuente es design/figma-tokens.json, el mismo archivo que usa el cliente
web: los dos canales comparten tokens, que es lo que sostiene la paridad de
estilo entre movil y web. Ningun color ni tamano se escribe a mano.

    python tools/tokens_to_kotlin.py
"""
import io
import json
import os

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ORIGEN = os.path.join(RAIZ, 'design', 'figma-tokens.json')
PAQUETE = os.path.join(RAIZ, 'app', 'src', 'main', 'kotlin',
                       'com', 'solventa4bits', 'movil', 'ui', 'tema')

# Material 3 de Compose nombra los roles en camelCase.
def camel(nombre):
    partes = nombre.split('-')
    return partes[0] + ''.join(p.capitalize() for p in partes[1:])


def color_kt(hexa):
    return '0xFF' + hexa.lstrip('#').upper()


def main():
    t = json.load(io.open(ORIGEN, encoding='utf-8'))
    esquemas = t['esquemas']
    marca = t['marca']

    cab = ('// Generado por tools/tokens_to_kotlin.py a partir de '
           'design/figma-tokens.json.\n'
           '// Archivo de Figma: %s (%s).\n'
           '// No editar a mano: volver a extraer de Figma y ejecutar el '
           'generador.\n\n' % (t['$origen']['archivo'], t['$origen']['fileKey']))

    # --- Color.kt --------------------------------------------------------
    c = [cab, 'package com.solventa4bits.movil.ui.tema\n\n',
         'import androidx.compose.ui.graphics.Color\n\n']
    for modo in ('light', 'dark'):
        sufijo = 'Claro' if modo == 'light' else 'Oscuro'
        c.append('// Esquema %s\n' % modo)
        for rol, val in esquemas.items():
            c.append('internal val %s%s = Color(%s)\n'
                     % (camel(rol), sufijo, color_kt(val[modo])))
        c.append('\n')
    c.append('// Marca. El teal de acento y la tipografia del logo no son '
             'roles del\n// sistema: pertenecen al logo y no a la interfaz '
             'operativa.\n')
    c.append('internal val AccentTeal = Color(%s)\n' % color_kt(marca['accent-teal']))
    c.append('internal val BrandBlack = Color(%s)\n' % color_kt(marca['brand-black']))
    c.append('internal val BrandWhite = Color(%s)\n' % color_kt(marca['brand-white']))
    io.open(os.path.join(PAQUETE, 'Color.kt'), 'w', encoding='utf-8').write(''.join(c))

    # --- Esquemas.kt -----------------------------------------------------
    # Compose nombra "background" y "surface" igual que M3; los roles fixed
    # no existen en ColorScheme y se dejan fuera a proposito.
    ROLES = [
        'primary', 'on-primary', 'primary-container', 'on-primary-container',
        'secondary', 'on-secondary', 'secondary-container', 'on-secondary-container',
        'tertiary', 'on-tertiary', 'tertiary-container', 'on-tertiary-container',
        'error', 'on-error', 'error-container', 'on-error-container',
        'background', 'on-background', 'surface', 'on-surface',
        'surface-variant', 'on-surface-variant', 'outline', 'outline-variant',
        'scrim', 'inverse-surface', 'inverse-on-surface', 'inverse-primary',
        'surface-dim', 'surface-bright', 'surface-container-lowest',
        'surface-container-low', 'surface-container', 'surface-container-high',
        'surface-container-highest', 'surface-tint',
    ]
    e = [cab, 'package com.solventa4bits.movil.ui.tema\n\n',
         'import androidx.compose.material3.darkColorScheme\n',
         'import androidx.compose.material3.lightColorScheme\n\n']
    for modo, sufijo, fabrica in (('light', 'Claro', 'lightColorScheme'),
                                  ('dark', 'Oscuro', 'darkColorScheme')):
        e.append('internal val Esquema%s = %s(\n' % (sufijo, fabrica))
        for rol in ROLES:
            if rol in esquemas:
                e.append('    %s = %s%s,\n' % (camel(rol), camel(rol), sufijo))
        e.append(')\n\n')
    io.open(os.path.join(PAQUETE, 'Esquemas.kt'), 'w', encoding='utf-8').write(''.join(e))

    # --- Tipografia.kt ---------------------------------------------------
    ESCALAS = [
        ('display-large', 'displayLarge'), ('display-medium', 'displayMedium'),
        ('display-small', 'displaySmall'), ('headline-large', 'headlineLarge'),
        ('headline-medium', 'headlineMedium'), ('headline-small', 'headlineSmall'),
        ('title-large', 'titleLarge'), ('title-medium', 'titleMedium'),
        ('title-small', 'titleSmall'), ('body-large', 'bodyLarge'),
        ('body-medium', 'bodyMedium'), ('body-small', 'bodySmall'),
        ('label-large', 'labelLarge'), ('label-medium', 'labelMedium'),
        ('label-small', 'labelSmall'),
    ]
    PESOS = {'regular': 'Normal', 'medium': 'Medium', 'bold': 'SemiBold'}
    ty = [cab, 'package com.solventa4bits.movil.ui.tema\n\n',
          'import androidx.compose.material3.Typography\n',
          'import androidx.compose.ui.text.TextStyle\n',
          'import androidx.compose.ui.text.font.FontFamily\n',
          'import androidx.compose.ui.text.font.FontWeight\n',
          'import androidx.compose.ui.unit.sp\n\n',
          '// %s. El archivo de fuente se agrega en res/font cuando exista;\n'
          '// mientras tanto se usa la familia por defecto del sistema.\n'
          'internal val FamiliaProducto = FontFamily.Default\n\n'
          % t['fuentes']['plain']]
    ty.append('internal val Tipografia = Typography(\n')
    for clave, nombre in ESCALAS:
        v = t['typescale'][clave]
        ty.append('    %s = TextStyle(\n'
                  '        fontFamily = FamiliaProducto,\n'
                  '        fontWeight = FontWeight.%s,\n'
                  '        fontSize = %s.sp,\n'
                  '        lineHeight = %s.sp,\n'
                  '        letterSpacing = %s.sp,\n'
                  '    ),\n'
                  % (nombre, PESOS[v['weight']], v['size'],
                     v['line-height'], v['tracking']))
    ty.append(')\n')
    io.open(os.path.join(PAQUETE, 'Tipografia.kt'), 'w', encoding='utf-8').write(''.join(ty))

    # --- Formas.kt -------------------------------------------------------
    f = [cab, 'package com.solventa4bits.movil.ui.tema\n\n',
         'import androidx.compose.foundation.shape.RoundedCornerShape\n',
         'import androidx.compose.material3.Shapes\n',
         'import androidx.compose.ui.unit.dp\n\n',
         'internal val Formas = Shapes(\n']
    for clave, nombre in (('corner-extra-small', 'extraSmall'),
                          ('corner-small', 'small'),
                          ('corner-medium', 'medium'),
                          ('corner-large', 'large'),
                          ('corner-extra-large', 'extraLarge')):
        f.append('    %s = RoundedCornerShape(%s.dp),\n' % (nombre, t['forma'][clave]))
    f.append(')\n')
    io.open(os.path.join(PAQUETE, 'Formas.kt'), 'w', encoding='utf-8').write(''.join(f))

    print('generados Color.kt, Esquemas.kt, Tipografia.kt y Formas.kt: '
          '%d roles de color en dos modos, %d escalas, %d radios'
          % (len(esquemas), len(ESCALAS), 5))


if __name__ == '__main__':
    main()
