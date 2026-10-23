# Changelog

## 0.21.0 Locked

### Local passcode
- A lock button next to search locks pixelGram right away with an animation.
- The lock screen fades and scales in and out.
- "Wrong passcode" now animates in instead of popping up.
- The passcode is checked only when you press Enter or the enter key on the pad, so you can type as many digits as you like.
- A more compact passcode setup dialog.

### Chats
- Pinning a message asks first: "Also pin for <name>" in private chats, "Notify all members" in groups.

### Sessions
- pixelGram sessions are named after your motherboard, like Telegram Desktop, instead of "Desktop".

### Updates
- No more false "Update failed" after a successful update.

## 0.20.2

- While a new version is still being built, update checks offer the latest finished release instead of saying updates are unavailable.
- If you already have that release, pixelGram says a new version is being built and to try again in a few minutes.

## 0.20.1

- The update window shows which pixelGram version you're downloading.
- Download and Update sit next to Close at the bottom of the update window.

## 0.20.0 Upd

- Fixed portable updates failing with "There is not enough space on the disk": the update is now downloaded and unpacked inside the pixelGram folder, on the same drive, and moved into place instead of copied.
- The downloaded archive is removed after the update.
- If an update fails, pixelGram tells you why after it restarts instead of quietly starting the old version.
- Release files now have the version in their names, like pixelGram-0.20.0-windows-x64-portable.zip.

## 0.18.1

- Updates, capsule folders and Noto 3D emoji are no longer marked as beta or experimental.

## 0.18.0 Privacy

### Privacy
- Every privacy setting: phone number, last seen, profile photos, forwarded messages, calls, voice messages, messages, birthday, gifts, bio, saved music and invites.
- Choose Everybody, My contacts or Nobody, and add users or groups as exceptions.

### Active sessions
- See every device logged in to your account, with details for each one.
- Terminate a single session or all other sessions at once.
- Rename this device.
- pixelGram sessions show the pixelGram logo in your theme color.
- Fixed the doubled "pixelGram pixelGram" in the session's app name.

### Local passcode
- Lock pixelGram with a 4-digit PIN when it starts, after you've been away, or right away with Lock now.
- A Pixel-style PIN pad: keys turn into rounded squares when pressed and each digit appears as an expressive shape before settling into a dot. Typing on the keyboard works too.

## 0.17.0 Update

- One-click updates for the Windows portable build and the Linux tar.gz: press Update when the download finishes, pixelGram closes, unpacks the new version into the same folder, keeps your sessions and settings, and starts again.
- The install button for the msi and AppImage is now called Update too.
- After an update pixelGram shows "pixelGram Updated!" at the top of the window.

## 0.16.0 Battery

### Battery and Animations
- New Settings section to turn animations off and save battery.
- Animated Stickers: autoplay in the panel and in chats. When off, stickers show their first frame.
- Animated Emoji: autoplay of custom emoji in messages.
- Animations in Chats: the spoiler shimmer and message animations (new messages sliding in, list movement).
- Interface animations: turns off the motion of menus, panels and transitions across the app.

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
