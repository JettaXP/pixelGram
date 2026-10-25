# Changelog

## 0.33.0 Pill

### macOS
- pixelGram now runs on macOS (Apple Silicon) with a DMG, the native menu bar, Dock icon, Notification Center and tg:// links. Thanks to MrFaDeveloper.

### Chats
- Big animated emoji play once, replay on click without restarting, and show Telegram's own click effect.
- "Forwarded from" sits at the top of the message with the source's avatar.
- Long posts are shown in full again, without Show more.
- Group chats in the list show who sent the last message.
- Jumping to the beginning, a pinned message or a quoted message loads a full page around it, and quotes jump to messages that aren't loaded yet.
- The emoji button closes the panel on the second click.
- Copy Callback Data only appears on inline buttons.
- Downloaded files are saved to Downloads/pixelGram.

### Look and feel
- Badge descriptions show as a pill in the middle of the window.
- Your own badge shows in My Profile, with a new main developer icon </>.
- Screens change with a scale and fade, menus open with a single expressive animation.
- Sliders stretch and narrow when held and released.
- Emoji previews switch right away when you change the emoji style.
- Vibrant colors keep dark message bubbles.

### Search and speed
- Search shows matching chats first, then messages.
- The chat list takes at most 45% of the window, so chats never squeeze into a strip.
- Fast scrolling through the chat list no longer decodes every avatar it passes.

### Fixes
- The Israel flag no longer pops up on the add account screen.

## 0.32.0 12-side cookie

### Colors and look
- Colors from wallpaper now match Android: the image is sampled like on Pixel and the theme uses the 2025 Tonal spot spec. It's on by default and updates on every launch.
- New hue slider: smooth dragging, an expressive split track and a thumb that narrows while pressed.
- Menus are one solid card, with each item as its own segment and gaps between groups.
- The tray menu uses the same grouped style and opens and closes with expressive motion.
- Apple emoji style, next to Noto and Noto 3D.
- Custom fonts get real bold weights and can be used across the whole app.

### Chats
- Drag across messages to select them. The list scrolls smoothly at the edges, and selection animates.
- New reactions pop in.
- Long captions under photos and videos collapse behind Show more, and photos and videos are bigger.
- Record button: click to switch between voice and video messages, hold to record, with a hint on hover. Right click works the same way.
- The reply panel no longer overlaps the message field.
- Gifts: View opens gift details, and the price with its currency is shown in "You gifted…".
- Unsaved drafts are kept when you close the app.
- Copying an image no longer freezes the app.

### Files
- Drag downloaded files out of the app, and use Show in folder or Copy file path.
- Read .md files right in the app.
- Saved media tabs animate, and links and files have a context menu.

### Profiles and people
- The profile window no longer fills the screen, and the banner scrolls away with a parallax effect.
- The banner also shows in the side menu account card.
- Main developer badge, and badge descriptions show right away.
- Contacts open instantly, with a badge for mutual contacts.

### Other
- Click the active folder to scroll back to the top of the chat list.
- When the app is locked, notifications show only the sender, not the message text.
- On Windows, pinned taskbar, Start menu and desktop shortcuts get the themed app icon.
- Animated stickers, wallpaper and spoilers redraw less, so scrolling is lighter.

## 0.31.0 Back

### Material 3 Expressive
- Icons are now Material Symbols Rounded with a heavier weight, generated from Google's official font.
- Headlines, titles and buttons use bolder Google Sans, like Google's own apps.
- Buttons change shape when pressed, list rows round up under your finger, dialogs pop in and lists animate when chats move.
- Menus are split into groups, and every item is its own rounded segment: the chat menu (three dots) and the message menu.
- New side menu: an account card, Night and Saved tiles like Android quick settings, grouped items and the version at the bottom.
- A new chat button in the chat list opens a menu with New message, New group and New channel.

### Colors
- Color styles: Tonal spot, Expressive, Vibrant, Fruit salad, Rainbow, Content, Neutral and Monochrome, picked like in Android's Wallpaper & style.
- Pick any hue with the new color slider.

### Wallpapers and fonts
- Chat wallpaper in Chat Settings, with dimming for the dark theme. Chats that have their own background in Telegram keep it.
- Recent wallpapers in a Material 3 carousel.
- Colors from wallpaper: the accent color is picked from the image with Google's Material color utilities.
- Message font and size: any installed font or a .ttf or .otf file.
- Corners: separate roundness for lists, cards, dialogs and menus in pixelGram Preferences.
- Every slider has a reset button, and About has Reset all settings (accounts, chats and passcode stay).

### Profiles and Saved Messages
- Profiles show photos, videos, files, links, music, voice messages and GIFs, plus groups in common.
- Clicking the Saved Messages header opens your saved messages by chat: My Notes, Author Hidden and every chat you saved from, with their own media tabs.
- Personal channel and other single rows are rounded again.
- Badges in a 12-sided cookie: a wrench for the developer, a dollar for the main sponsor and a bug net for the tester. Click one to see what it means.

### Fixes
- Channels you haven't joined show a big Join button instead of Mute. Blocked people show a big Unblock button instead of the message field.
- Reactions no longer stay small.
- A draft sent from another device disappears from the message field.
- Emoji in inline games and names no longer get mixed up.
- Jumping to a pinned message or to the beginning takes one request instead of loading page after page.
- Messages no longer end up out of order after jumping around a chat.
- Videos play smoothly: about 48 instead of 3 frames per second on a 1080p60 test video.

## 0.30.0 Bye...

### Forwarding
- Forwarding now works like in Telegram: pick a chat, and a forward bar appears above the message field. Send it with the send button, with or without a comment.
- Click the bar to open the forward options: hide the sender's name so the message comes from you, hide the caption of a photo or file, change the recipient, or don't forward at all.

### Other
- Volume slider in the mini player.
- Sticker preview (hold a sticker) now covers the whole window instead of just the sticker panel.
- @usernames in a bio are clickable and open that person's profile.
- In channels where you can't post, the message field is replaced by a Mute/Unmute bar.
- Block and unblock people from the More menu in their profile.
- Fixed reactions that stayed small after the reaction list changed.

### Stickers
- Video stickers (WebM) now play in chats, the sticker panel and sticker packs, with transparency. This includes animated custom emoji.
- Sticker packs show every sticker animated, not only the static ones.
- Click a sticker in a sticker pack to send it to the chat.

## 0.28.0 IL

### Easter egg
- The easter egg now also turns on a special white and blue theme for the whole app, notifications included. It lasts while the egg is active and switches off with the second activation.
- While it is on, the theme can't be changed: Settings show Light, and the accent color and matugen are locked. The special theme is not listed among the themes.

### Fixes
- Opening photos and videos full screen is much smoother, especially on Linux: the viewer no longer uses a transparent full-screen window.
- Updates keep working when the GitHub API is unreachable or rate-limited: pixelGram falls back to the GitHub releases page. The log now records why a check failed.
- System notifications work without notify-send: pixelGram talks to the notification daemon over D-Bus, so the System/pixelGram style switch shows up on more Linux setups.

## 0.27.1 Cookie

- The official pixelGram channel always has a 12-sided cookie avatar, whatever avatar shape you picked: in the chat list, the chat header and its profile.
- The Message, Mute and More buttons in a profile become rounder while you press them.

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
