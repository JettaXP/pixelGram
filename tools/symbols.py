import json, os, re, sys, urllib.request
from pathlib import Path
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / 'src/main/kotlin'
OUT = SRC / 'com/pixelgram/desktop/ui/icons'
CACHE = ROOT / 'build/symbols'
FONT_URL = 'https://raw.githubusercontent.com/google/material-design-icons/master/variablefont/MaterialSymbolsRounded%5BFILL%2CGRAD%2Copsz%2Cwght%5D.ttf'
WEIGHT = int(os.environ.get('SYMBOLS_WEIGHT', '500'))
RENAMED = {
    'InsertDriveFile': 'description', 'Collections': 'photo_library', 'EmojiEmotions': 'mood',
    'EmojiFlags': 'flag', 'FavoriteBorder': 'favorite', 'Phone': 'call', 'PhoneAndroid': 'mobile',
    'PhoneIphone': 'mobile_2', 'SystemUpdate': 'mobile_arrow_down', 'CardGiftcard': 'redeem',
}


def font(fill):
    CACHE.mkdir(parents=True, exist_ok=True)
    variable = CACHE / 'rounded.ttf'
    if not variable.exists():
        urllib.request.urlretrieve(FONT_URL, variable)
    static = CACHE / f'rounded-{WEIGHT}-{fill}.ttf'
    if not static.exists():
        instancer.instantiateVariableFont(TTFont(variable), {'wght': WEIGHT, 'GRAD': 0, 'opsz': 24, 'FILL': fill}).save(static)
    return TTFont(static).getGlyphSet()


def snake(name):
    s = re.sub(r'(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])', '_', name).lower()
    return re.sub(r'(?<=[a-z])(?=[0-9])', '_', s)


def glyph(glyphs, name, fill):
    base = RENAMED.get(name) or snake(name)
    names = [base] + [base[: -len(s)] for s in ('_outline', '_outlined', '_rounded') if base.endswith(s)]
    for n in names:
        for g in ([n + '.fill', n] if fill else [n]):
            if g in glyphs:
                return g
    return None


def main():
    used = set()
    for file in SRC.rglob('*.kt'):
        if OUT in file.parents:
            continue
        used.update(re.findall(r'Icons\.((?:AutoMirrored\.)?(?:Outlined|Filled|Rounded|Default))\.(\w+)', file.read_text()))
    glyphs = {0: font(0), 1: font(1)}
    packages = {}
    missing = []
    for style, name in sorted(used):
        mirrored = style.startswith('AutoMirrored.')
        kind = style.split('.')[-1]
        kind = 'Filled' if kind == 'Default' else kind
        fill = 0 if kind == 'Outlined' else 1
        found = glyph(glyphs[fill], name, fill)
        if found is None:
            missing.append(f'{style}.{name}')
            continue
        pen = SVGPathPen(glyphs[fill])
        glyphs[fill][found].draw(TransformPen(pen, (1, 0, 0, -1, 0, 960)))
        path = re.sub(r'(\d+\.\d{2})\d+', r'\1', pen.getCommands())
        package = ('automirrored.' if mirrored else '') + kind.lower()
        packages.setdefault(package, []).append((name, path, mirrored, kind))
    if missing:
        sys.exit('No symbol for: ' + ', '.join(missing) + '. Add them to RENAMED.')
    for package, icons in packages.items():
        receiver = ('Icons.AutoMirrored.' if package.startswith('automirrored') else 'Icons.') + icons[0][3]
        lines = [f'package com.pixelgram.desktop.ui.icons.{package}', '',
                 'import androidx.compose.ui.graphics.vector.ImageVector',
                 'import com.pixelgram.desktop.ui.icons.Icons',
                 'import com.pixelgram.desktop.ui.icons.symbol', '']
        for name, path, mirrored, kind in icons:
            var = name[0].lower() + name[1:] + 'Symbol'
            mirror = ', mirror = true' if mirrored else ''
            lines += [f'private val {var} by lazy {{ symbol("{kind}.{name}", "{path}"{mirror}) }}',
                      f'val {receiver}.{name}: ImageVector get() = {var}', '']
        target = OUT / package.replace('.', '/') / 'Symbols.kt'
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text('\n'.join(lines))
        print(package, len(icons))


main()
