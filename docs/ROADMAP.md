# pixelGram roadmap

Planned features, not started yet.

## Bugs to fix next

- Noto 3D emoji: now drawn as pictures; check on Windows that nothing is
  cut off or off-centre any more.

## Sign-in

- While the phone number screen is loading, show the Material 3 Expressive
  loading animation: rotating shapes that morph into each other.

## Connection status

- When Telegram can't connect, show a small pill at the bottom of the chat
  list, over the chats: "Connecting…" (and "Waiting for network…",
  "Updating…") like the official client. Instead of a plain spinner it has
  the Material 3 Expressive loading indicator, shapes that morph into each
  other. It slides in when the connection drops and out when it's back.

## Emoji

- More emoji styles to choose from: Apple (iOS) and Windows (Fluent).

## Message types

- Support every kind of message, all drawn in Material 3:
  - service messages with a pill and time, e.g. "You sent a gift for 15 Stars";
  - gifts: a card with the gift sticker, "Gift for <name>" and a "View" button;
  - calls: "Incoming call" / "Outgoing call" / "Canceled call" / "Missed call",
    an arrow showing the direction, time and duration, a call icon on the right;
  - files: a round icon (file, puzzle piece for plugins, music for audio),
    name and size in KB/MB, time and ticks;
  - and the rest: stickers, GIFs, video, voice, video messages, polls, contacts,
    locations, music.

## .plugin files

- Clicking a `.plugin` file opens a Material 3 dialog with the plugin's info,
  read from the variables at the top of the file:

  ```python
  __id__ = "media_glow"
  __name__ = "Media Glow"
  __description__ = "добавляет мягкую размытую подсветку по краям полноэкранных фото и видео"
  __author__ = "@itskotovski & @mishabotov"
  __version__ = "1.1"
  __icon__ = "sfsymbols/347"
  ```

  It shows the icon, the name, "Version 1.1 • @author", the description, a
  folder button (show the file) and Close. No "Plugins are not supported" line.

