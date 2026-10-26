import os
import re

ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/kotlin/com/pixelgram/desktop")
SETTINGS = os.path.join(ROOT, "ui/settings")
OUT = os.path.join(SETTINGS, "SettingsIndex.kt")

SECTIONS = {
    "AccountSection": "MyAccount",
    "NotificationsSection": "Notifications",
    "PrivacySection": "Privacy",
    "ChatSettingsSection": "ChatSettings",
    "FoldersSection": "Folders",
    "AdvancedSection": "Advanced",
    "DevicesSection": "SpeakersCamera",
    "BatterySection": "Battery",
    "AppearanceSection": "Appearance",
    "AvatarsSection": "Avatars",
    "BubblesSection": "Bubbles",
    "CornersSection": "Corners",
    "CursorSection": "Cursor",
    "AboutSection": "About",
}

FUN = re.compile(r"^(?:private |internal )?fun (\w+)\(", re.M)
LITERAL = re.compile(r'"((?:[^"\\$]|\\.)*)"')
SKIP = {"Default", "On", "Off", "Yes", "No", "Cancel", "OK", "Done", "Save", "Close", "Delete", "Remove", "Change", "Reset", "Back", "All", "Personal", "Private", "Work", "Info", "Terminate", "Rename", "Current", "Left", "Top", "Copy"}


def bodies():
    found = {}
    for name in os.listdir(SETTINGS):
        if not name.endswith(".kt") or name == "SettingsIndex.kt":
            continue
        text = open(os.path.join(SETTINGS, name), encoding="utf-8").read()
        starts = [(m.start(), m.group(1)) for m in FUN.finditer(text)]
        for i, (start, fun) in enumerate(starts):
            end = starts[i + 1][0] if i + 1 < len(starts) else len(text)
            found[fun] = text[start:end]
    return found


def titles(body):
    out = []
    for m in LITERAL.finditer(body):
        s = m.group(1).replace('\\"', '"')
        if len(s) < 3 or len(s) > 48 or s in SKIP:
            continue
        if not s[0].isupper() or s.endswith((".", ":", "…", "?")) or "%" in s or "{" in s or "/" in s:
            continue
        if re.match(r"^[A-Z][a-z]+[A-Z]", s) or "_" in s:
            continue
        out.append(s)
    return out


def main():
    funs = bodies()
    entries = []
    for root, section in SECTIONS.items():
        seen, queue, collected = set(), [root], []
        while queue:
            fun = queue.pop()
            if fun in seen or fun not in funs:
                continue
            seen.add(fun)
            body = funs[fun]
            collected += titles(body)
            queue += [name for name in re.findall(r"\b([A-Z]\w+)\(", body) if name in funs and name not in SECTIONS]
        for title in dict.fromkeys(collected):
            entries.append((section, title))
    lines = ["package com.pixelgram.desktop.ui.settings", "", "internal val SettingsIndex = listOf("]
    for section, title in entries:
        escaped = title.replace("\\", "\\\\").replace('"', '\\"')
        lines.append(f'    "{section}" to "{escaped}",')
    lines.append(")")
    open(OUT, "w", encoding="utf-8").write("\n".join(lines) + "\n")
    print(len(entries), "entries")


main()
