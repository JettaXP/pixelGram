#!/usr/bin/env python3
# This is the source code of pixelGram for Desktop.
# Based on AyuGram Desktop and Telegram Desktop, licensed under GPLv3.
#
# Generates the matugen template (pixelgram.tdesktop-palette) from the
# default palette (lib_ui/ui/colors.palette).
#
# Keys with an explicit role below get that Material 3 role. Other keys with
# a literal default color are mapped by their default color (lightness,
# saturation, hue). Keys that are semantic colors (userpic colors, file
# type colors, calls, media viewer, etc.) keep their default values.
# Keys that reference other keys are left out, they follow their fallback.
#
# Usage: generate_template.py <colors.palette> <output>

import colorsys
import re
import sys

# Explicit Material 3 roles for the most visible keys.
ROLES = {
    # Window.
    'windowBg': 'surface',
    'windowFg': 'on_surface',
    'windowBgOver': 'surface_container_high',
    'windowBgRipple': 'surface_container_highest',
    'windowFgOver': 'on_surface',
    'windowSubTextFg': 'on_surface_variant',
    'windowSubTextFgOver': 'on_surface_variant',
    'windowBoldFg': 'on_surface',
    'windowBoldFgOver': 'on_surface',
    'windowBgActive': 'primary',
    'windowFgActive': 'on_primary',
    'windowActiveTextFg': 'primary',
    'windowShadowFgFallback': 'surface_container',
    'imageBgTransparent': 'surface',

    # Buttons.
    'activeButtonBg': 'primary',
    'activeButtonBgOver': 'primary',
    'activeButtonBgRipple': 'on_primary_container',
    'activeButtonFg': 'on_primary',
    'activeButtonFgOver': 'on_primary',
    'activeButtonSecondaryFg': 'primary_container',
    'activeLineFg': 'primary',
    'activeLineFgError': 'error',
    'lightButtonBg': 'surface',
    'lightButtonBgOver': 'secondary_container',
    'lightButtonBgRipple': 'surface_container_highest',
    'lightButtonFg': 'primary',
    'lightButtonFgOver': 'primary',
    'attentionButtonFg': 'error',
    'attentionButtonFgOver': 'error',
    'attentionButtonBgOver': 'error_container',
    'attentionButtonBgRipple': 'error_container',
    'outlineButtonBg': 'surface',
    'outlineButtonBgOver': 'secondary_container',
    'outlineButtonOutlineFg': 'outline',

    # Menus, inputs, checkboxes, sliders.
    'menuBg': 'surface_container',
    'menuBgOver': 'surface_container_highest',
    'menuIconFg': 'on_surface_variant',
    'menuIconFgOver': 'on_surface',
    'menuSubmenuArrowFg': 'on_surface_variant',
    'menuFgDisabled': 'outline',
    'menuSeparatorFg': 'outline_variant',
    'placeholderFg': 'on_surface_variant',
    'placeholderFgActive': 'outline',
    'inputBorderFg': 'outline_variant',
    'filterInputBorderFg': 'primary',
    'filterInputInactiveBg': 'surface_container_high',
    'checkboxFg': 'outline',
    'sliderBgInactive': 'surface_container_highest',
    'sliderBgActive': 'primary',
    'tooltipBg': 'inverse_surface',
    'tooltipFg': 'inverse_on_surface',
    'tooltipBorderFg': 'inverse_surface',
    'toastBg': 'inverse_surface',
    'toastFg': 'inverse_on_surface',
    'boxTitleFg': 'on_surface',
    'boxTitleAdditionalFg': 'on_surface_variant',
    'boxBg': 'surface_container_high',
    'boxDividerBg': 'surface_container_low',
    'emojiPanBg': 'surface_container',
    'emojiPanCategories': 'surface_container',
    'emojiIconFg': 'on_surface_variant',
    'emojiIconFgActive': 'primary',

    # Title bar.
    'titleBg': 'surface_container',
    'titleBgActive': 'surface_container',
    'titleButtonBg': 'surface_container',
    'titleButtonBgActive': 'surface_container',
    'titleButtonFg': 'on_surface_variant',
    'titleButtonFgActive': 'on_surface',
    'titleButtonBgOver': 'surface_container_highest',
    'titleButtonFgOver': 'on_surface',
    'titleFg': 'on_surface_variant',
    'titleFgActive': 'on_surface',

    # Folders side bar (Material 3 navigation rail).
    'sideBarBg': 'surface_container',
    'sideBarBgActive': 'secondary_container',
    'sideBarBgRipple': 'surface_container_highest',
    'sideBarTextFg': 'on_surface_variant',
    'sideBarTextFgActive': 'on_surface',
    'sideBarIconFg': 'on_surface_variant',
    'sideBarIconFgActive': 'on_secondary_container',
    'sideBarBadgeBg': 'primary',
    'sideBarBadgeBgMuted': 'outline',
    'sideBarBadgeFg': 'on_primary',

    # Chat list.
    'dialogsBg': 'surface_container_low',
    'dialogsBgOver': 'surface_container_high',
    'dialogsBgActive': 'secondary_container',
    'dialogsNameFg': 'on_surface',
    'dialogsNameFgActive': 'on_secondary_container',
    'dialogsTextFg': 'on_surface_variant',
    'dialogsTextFgActive': 'on_secondary_container',
    'dialogsTextFgService': 'primary',
    'dialogsTextFgServiceActive': 'on_secondary_container',
    'dialogsDateFg': 'on_surface_variant',
    'dialogsDateFgActive': 'on_secondary_container',
    'dialogsUnreadBg': 'primary',
    'dialogsUnreadBgActive': 'on_secondary_container',
    'dialogsUnreadFg': 'on_primary',
    'dialogsUnreadFgActive': 'secondary_container',
    'dialogsUnreadBgMuted': 'outline',
    'dialogsUnreadBgMutedActive': 'on_secondary_container',
    'dialogsVerifiedIconBg': 'primary',
    'dialogsVerifiedIconFg': 'on_primary',
    'dialogsSendingIconFgActive': 'on_secondary_container',
    'dialogsSentIconFgActive': 'on_secondary_container',
    'dialogsDraftFgActive': 'on_secondary_container',
    'dialogsOnlineBadgeFgActive': 'secondary_container',
    'dialogsRippleBg': 'surface_container_highest',
    'dialogsRippleBgActive': 'secondary_container',
    'dialogsMenuIconFg': 'on_surface_variant',
    'dialogsMenuIconFgOver': 'on_surface',

    # Chat.
    'historyComposeAreaBg': 'surface_container',
    'historyComposeAreaFg': 'on_surface',
    'historyComposeIconFg': 'on_surface_variant',
    'historyComposeIconFgOver': 'primary',
    'historySendIconFg': 'primary',
    'historySendIconFgOver': 'primary',
    'historyComposeButtonBg': 'surface_container',
    'historyComposeButtonBgOver': 'surface_container_high',
    'historyUnreadBarBg': 'surface_container',
    'historyUnreadBarFg': 'primary',
    'historyLinkInFg': 'primary',
    'historyLinkOutFg': 'on_primary_container',
    'historyTextInFg': 'on_surface',
    'historyTextOutFg': 'on_primary_container',
    'msgInBg': 'surface_container_high',
    'msgInBgSelected': 'secondary_container',
    'msgOutBg': 'primary_container',
    'msgOutBgSelected': 'secondary_container',
    'msgInServiceFg': 'primary',
    'msgOutServiceFg': 'on_primary_container',
    'msgOutServiceFgSelected': 'on_secondary_container',
    'msgInDateFg': 'on_surface_variant',
    'msgOutDateFg': 'on_primary_container',
    'msgOutDateFgSelected': 'on_secondary_container',
    'msgInReplyBarColor': 'primary',
    'msgOutReplyBarColor': 'on_primary_container',
    'msgInMonoFg': 'tertiary',
    'msgOutMonoFg': 'on_primary_container',
    'msgFileInBg': 'primary',
    'msgFileOutBg': 'on_primary_container',
    'msgFileThumbLinkInFg': 'primary',
    'msgFileThumbLinkOutFg': 'on_primary_container',
    'historyOutIconFg': 'on_primary_container',
    'historyOutIconFgSelected': 'on_secondary_container',
    'historySendingOutIconFg': 'on_primary_container',
    'msgWaveformInActive': 'primary',
    'msgWaveformInInactive': 'outline_variant',
    'msgWaveformOutActive': 'on_primary_container',
    'msgWaveformOutInactive': 'outline',
    'historyFileInIconFg': 'on_primary',
    'historyFileOutIconFg': 'primary_container',

    # Profile, main menu, media player.
    'mainMenuBg': 'surface_container_low',
    'mainMenuCoverBg': 'primary',
    'mainMenuCoverFg': 'on_primary',
    'profileVerifiedCheckBg': 'primary',
    'mediaPlayerBg': 'surface_container',
    'mediaPlayerActiveFg': 'primary',
    'mediaPlayerInactiveFg': 'outline_variant',
    'mediaPlayerDisabledFg': 'outline',

    # Intro (login) screen.
    'introBg': 'surface',
    'introTitleFg': 'on_surface',
    'introDescriptionFg': 'on_surface_variant',
    'introErrorFg': 'error',
}

# Semantic colors that should not follow the wallpaper.
KEEP = re.compile(
    r'^(historyPeer|settingsIconBg|msgFile[1-4]|mediaview|call|groupCall|'
    r'intro(?!Bg|TitleFg|DescriptionFg|ErrorFg)|botKb|titleButtonClose|'
    r'trayCounter|youtube|videoPlay|map|searchedText|boxTextFg|paymentsTip|'
    r'stickerPan|premium|giftBox|creditsBg|statisticsChart|'
    r'dialogsDraftFg$|dialogsSentIconFg|dialogsSendingIconFg$|'
    r'dialogsOnlineBadgeFg$|historyCallArrow|walletTitle)')

FORMAT_ROLE = '{{{{colors.{role}.default.hex}}}}'
FORMAT_ROLE_ALPHA = '#{{{{colors.{role}.default.hex_stripped}}}}{alpha:02x}'


def parse_palette(path):
    result = []
    pattern = re.compile(
        r'^([a-zA-Z0-9_]+):\s*([^;|/]+?)\s*(?:\|\s*[a-zA-Z0-9_]+\s*)?;')
    with open(path, encoding='utf-8') as f:
        for line in f:
            match = pattern.match(line.strip())
            if match:
                result.append((match.group(1), match.group(2)))
    return result


def parse_color(value):
    if not re.fullmatch(r'#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?', value):
        return None
    r, g, b = (int(value[i:i + 2], 16) for i in (1, 3, 5))
    a = int(value[7:9], 16) if len(value) == 9 else 255
    return r, g, b, a


def guess_role(name, color):
    r, g, b, a = color
    h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
    hue = h * 360
    foreground = name.endswith(('Fg', 'FgOver', 'FgActive', 'FgSelected'))

    # Black and white overlays and shadows: keep.
    if a < 255 and s < 0.1 and (l < 0.05 or l > 0.95):
        return None
    if s < 0.15 or l > 0.97 or l < 0.03:
        # Neutral colors.
        if foreground:
            if l < 0.45:
                return 'on_surface'
            if l < 0.8:
                return 'on_surface_variant'
            return 'outline_variant'
        if l > 0.97:
            return 'surface'
        if l > 0.93:
            return 'surface_container'
        if l > 0.88:
            return 'surface_container_high'
        if l > 0.7:
            return 'surface_container_highest'
        if l > 0.45:
            return 'outline'
        return 'on_surface_variant'
    if hue < 20 or hue >= 340:
        return 'error_container' if (l > 0.85 and not foreground) else 'error'
    if 70 <= hue < 250:
        # Blue accents and green outgoing bubble accents.
        if l > 0.85:
            return 'on_primary_container' if foreground else 'primary_container'
        return 'primary'
    return None


def main():
    if len(sys.argv) != 3:
        print('Usage: generate_template.py <colors.palette> <output>')
        return 1
    palette = parse_palette(sys.argv[1])
    lines = [
        '// pixelGram Material You palette, generated by matugen.',
        '// Template source: Telegram/Resources/matugen/generate_template.py',
        '',
    ]
    for name, value in palette:
        role = ROLES.get(name)
        color = parse_color(value)
        if not role:
            if not color or KEEP.match(name):
                continue
            role = guess_role(name, color)
            if not role:
                continue
        alpha = color[3] if color else 255
        if alpha == 255:
            lines.append(f'{name}: ' + FORMAT_ROLE.format(role=role) + ';')
        else:
            lines.append(
                f'{name}: '
                + FORMAT_ROLE_ALPHA.format(role=role, alpha=alpha)
                + ';')
    with open(sys.argv[2], 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines) + '\n')
    return 0


if __name__ == '__main__':
    sys.exit(main())
