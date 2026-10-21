# pixelGram

Private fork of [AyuGram Desktop](https://github.com/AyuGram/AyuGramDesktop),
itself a fork of [Telegram Desktop](https://github.com/telegramdesktop/tdesktop).

Goal: AyuGram base + selected features from rabbitGram and materialgram,
plus pixelGram's own features (kept under `Telegram/SourceFiles/pixel/`).

Licensed under GPLv3 with the OpenSSL exception (see `LICENSE`, `LEGAL`).
The upstream README is preserved as `README.upstream.md`.

## Remotes

- `upstream` — AyuGram/AyuGramDesktop
- add later: `rabbitgram`, `materialgram`, `tdesktop`

## Building

See `docs/building-linux.md` / `docs/building-win-x64.md`.
Requires your own `TDESKTOP_API_ID` and `TDESKTOP_API_HASH` (my.telegram.org).

## Material You with matugen (Linux)

1. In pixelGram: Settings → pixelGram Preferences → Appearance → Material You,
   enable **Dynamic theme from matugen** and press **Copy matugen config**.
2. Paste the copied lines into `~/.config/matugen/config.toml`:

   ```toml
   [templates.pixelgram]
   input_path = '~/.config/pixelgram/matugen-template.tdesktop-palette'
   output_path = '~/.config/pixelgram/matugen.tdesktop-palette'
   ```
3. Run `matugen image /path/to/wallpaper.png`. pixelGram applies the new colors
   automatically every time matugen regenerates the palette.

The template is generated from the default palette by
`Telegram/Resources/matugen/generate_template.py`.
