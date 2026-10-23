# Changelog

## 0.27.0

### Performance
- The chat list re-sorts much faster when messages arrive, so pixelGram stays smooth with hundreds of chats.
- Video frames are freed right away instead of piling up in memory, so videos no longer make the app lag.
- Window and tray icons are drawn much faster.
- The log now records which graphics mode pixelGram draws with, to help track down slowness.

### Fixes
- Posts with a photo no longer stretch wider than the photo when they reply to a long message, so the photo and time stay in place.
- The send and microphone buttons now fade in place when you swap them.

## 0.26.0 Iron Dome

### Photo viewer
- Browse photos in a chat without closing the viewer: arrows on the sides, Left and Right keys, or the mouse wheel. An arrow disappears when there are no more photos that way.
- The caption sits in a translucent panel.
- Arrows and caption hide when you leave the mouse still, and quickly fade back when you move it.
- Hold Ctrl and scroll to zoom.

### Easter egg
- The easter egg now also switches the pixelGram logo to a white and blue Israel edition everywhere: window, taskbar and tray icons, notifications, title bar, login and lock screens, the official badge and the version capsule. Trigger it again to go back to your theme colors.

## 0.25.5 יהודה

- Profile music and other audio in M4A, FLAC and other formats now play, not just MP3 and OGG.
- Escape leaves the archive again: focus is no longer lost when the "Archived chats" row disappears.
- Escape closes the emoji, sticker and GIF panel first instead of closing the whole chat.
- When All chats is hidden, Archived chats appear at the top of your first folder.
- Photos, videos and GIFs with a caption show just an emoji before the text in the chat list, without the "Photo" or "Video" label.

## 0.25.0 יִשְׂרָאֵל

### Voice messages
- Hold the microphone button to record a voice message. The message bar smoothly turns into a recording strip whose wave follows how loud you speak.
- Release to send: the wave folds into a voice bubble that floats into the chat. Slide left or press Escape to cancel.
- Right-click the send or microphone button to swap them; one slides away as the other slides in. The microphone button is a 4-sided cookie, and the send button is a 12-sided cookie.

### Speakers and Camera
- New settings section: choose the output device, microphone and camera, with a live microphone level and a camera preview.
- Voice messages record from the chosen microphone, and sounds and video play through the chosen output device.

### Folders
- Create, edit and delete chat folders right in Settings, including name, icon, chat types and chats.
- Drag folders to change their order.
- Many more folder icons, including a flag.
- New special folders, Unread and Mentions, with their own names and icons; turn them on in Settings → Folders.

### Channels
- "Leave a comment" bar under channel posts opens the discussion. In groups that require it, you are asked to join before commenting.

### Chats
- Hold a sticker to preview it with its emoji. Right-click a sticker for more options: send without sound, schedule, send when online, favorites, view the pack, and remove from recent (in Recent only).
- The emoji, sticker and GIF panel now closes with an animation.
- Link previews span the full width of the message.
- "To beginning" now jumps instantly.
- Video: no more stuck loader, smoother playback and better quality, plus a volume slider.

### Profiles
- Every username, your own and other people's, has a QR code button.
- Profile details show their label (Username, Bio, ID…) in a small pill.
- Muting from a profile is instant and animated.
- The avatar hover glow follows your avatar shape.

### Other
- Passcode dots use a different shape each time, never the same twice in a row.
- Escape closes the side menu first and also leaves the archive.
- The version in pixelGram Preferences now sits in a capsule. Try clicking it a few times.

## 0.22.1 Pass

- Passcode dots appear smoothly: the row makes room for each new digit and the dot fades and grows in instead of popping up.

## 0.22.0 Core

- Fixed blank or half-drawn windows on tiling window managers (niri, Hyprland, sway, river, i3 and others): the Linux launcher now turns on Java's non-reparenting mode by itself.
- Adding an account that is already in pixelGram is refused with a notice, both by phone number and by QR code.
- After too many login attempts the login screen counts down until you can try again.

## 0.21.2

- Escape goes one level back in Settings: from Active sessions to Privacy and Security, then out of Settings. It also closes the settings search.
- Escape closes My Profile, Contacts and Calls.
- The "official pixelGram resource" pill now shows at the top of the window, where "pixelGram Updated!" appears, and slides away smoothly.

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
