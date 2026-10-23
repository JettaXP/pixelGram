# Changelog

## 0.15.1 Banners

- Custom emoji no longer swap places with regular emoji in the same message. Fixes pieces and name emoji mixing up in checkers.
- The More menu in a profile no longer has a View profile item.

## 0.15.0 Banners

### Banners
- Profile banners: a Banner pill in the top right corner of your profile sets one from your computer or the clipboard, or removes it.
- Banners are cropped to 3:1, compressed and sent to @pixelGramCI_bot, which publishes them to the repository under your user ID.
- Other people's banners load from the repository, fade in behind their avatar and are cached on disk.

### Chats
- Custom emoji in messages, including animated ones.
- Inline buttons show their custom emoji icons and colors. Fixes games like checkers that draw the board with custom emoji.

## 0.13.0

### Chats
- Pinned messages bar under the chat header; click to jump and cycle through pins.
- Pin service messages now say who pinned what, and jump to the pinned message on click.
- Message selection: pick Select in the context menu, then click or drag up to select up to 100 messages. Forward, delete or make a Shot of them.
- Shot: render selected messages into an image with theme, background, date and colorful replies options. Copy or save as PNG. Inline buttons are included.
- Copy Link when right-clicking a link or a URL button, Copy Callback Data for callback buttons.
- `t.me/bot?start=...` links open the bot and send /start with the code.
- `t.me/user?text=...` links open the chat and put the text into the message field.
- Back button when you open a chat from another chat.
- The chat scrolls down after you send something, inline bot results included.
- Fixed messages jumping while scrolling up, especially ones with inline buttons.
- Right-click no longer reveals spoilers.

### Inline bots
- Music and voice results from inline bots (like @sounds) with a play button.
- MD3 loading shapes in place of the send button while an inline bot is answering.

### Profiles
- Message, Mute and More actions in user profiles.
- Hover your avatar for a camera pill: photo or video, camera, or clipboard.
- Profile music: right-click menu (save as, forward, delete), drag handles to reorder, autoplay of the next track.
- Hearts on every track in your own profile.
- Official pixelGram resources get a badge in the theme color next to their name.

### Player
- Previous and next buttons work for profile music, music and voice messages; voice and music use separate queues.

### Other
- The download and upload ring no longer spins and turns into a plain circle near the end.
- Smooth animation when leaving search.
