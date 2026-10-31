# Changelog

## 0.46.3 Gem

- Calculator in the message field: the answer fades in, blends into the text when inserted, and the Kitty cursor trails to the end of the number.
- Window buttons with the Telegram icon pack use the Telegram Desktop title bar icons, all the same size.
- The Expressive settings style keeps its name in every language.

## 0.46.2 Gem

- Shorter emoji style descriptions.

## 0.46.1 Gem

- The icon pack descriptions and the About text are translated into Russian and Hebrew.
- The emoji style description shows only the selected set.
- Credits moved from About to the README.

## 0.46.0 Gem

### Icon packs
- Choose the icons for the whole app in pixelGram Preferences → Appearance: Material (Google), Solar (480 Design) or Telegram (Telegram Desktop). Each icon is replaced with the closest one from the pack.

### pixelGram Preferences
- The preferences are grouped into General, Appearance, Chats, Window and system, Experimental features and Other, with subsections for fonts, avatars, bubbles, corners, chat folders, the side menu, message actions, the cursor and About.
- The theme and accent colors moved to Chat Settings, with the wallpaper at the top.
- Disable number rounding: show 1,234 views instead of 1.2K.
- Zalgo filter: removes stacked combining symbols from names and messages.
- Chat list title: pixelGram, your username or your own text.
- Folder titles: names with icons, names only or icons only. With icons only, the chat list title shows the open folder's name.
- About has a link to the website and credits for exteraGram, Material Symbols, Solar and Telegram Desktop.

### Chats
- Calculator in the message field: type an example ending with =, like 5+5=, and the answer appears in gray. Press → to insert it.
- Experimental: run Python from messages. With the option on, send .py "code" and pixelGram runs it with the system Python 3, then sends the output and the code in two quotes. Only messages you send are run. PyPI packages can be installed in the same section.

### QR codes
- The QR reveal follows the Android 16 share screen: the code fades in as one layer while the pieces grow outward from the center, dots light up along the backdrop edge first, and the corner rings and their cookie centers come in last.

### Windows installer
- The installer is in English only.

### Translations
- The remaining settings strings are translated into Russian and Hebrew.

## 0.45.1 Sunny

### QR codes
- QR codes now appear with an animation: the background grows in, the code pieces pop out from the center to the edges, then the corner rings and their centers. When the sign-in QR code refreshes, the old code folds away and the new one builds up.
- The centers of the corner marks get a random cookie shape every time a QR code is made.
- In the light theme the QR code background is now visible against the dialog.

### Windows installer
- When installation finishes, the logo turns into a cookie, the checkmark draws in, and the cookie slowly turns.

### Interface
- Removed story posting from the side menu and the channel menu. Stories can still be viewed.
- Removed the version label from the side menu.

### Performance
- Fast scrolling through chats with many GIFs and videos no longer freezes the app. Videos start playing only after they stay on screen for a moment, and at most six play at once.

## 0.45.0 Sunny

### Windows installer
- New pixelGram Setup.exe in Material 3 Expressive: the logo in your theme colors, the install folder with its own folder picker (home, apps, drives, new folder, or the system dialog), a static wavy progress bar that shows the real installation progress and the current step, and a Launch pixelGram checkbox with a Done button at the end. The installer follows the Windows theme and language. The .msi and the portable .zip are still available.

### Accounts
- The Cancel button on the sign-in screen appears only while adding a new account. It no longer shows on the loading screen, where it could remove an account that was already signed in.

## 0.44.1 Puffy

- Fixed a crash when many animated video stickers were shown, for example while scrolling the sticker or emoji panel.
- Fixed a possible crash when starting a call or receiving video in a video chat.
- Fixed a crash when dragging a crop corner at the edge of a photo or cropping a very small photo.
- Search results, mentions and bot results with repeated items no longer crash the list.
- New Rendering setting in Advanced: Auto, DirectX, OpenGL or Software. In Auto mode on Windows, pixelGram switches to OpenGL after it closes unexpectedly twice in a row.

## 0.44.0 Puffy

### Proxy
- New Proxy section in Settings for SOCKS5, HTTP and MTProto proxies, with ping, copy link, edit and delete. The proxy is used by all accounts.
- The sign-in screen has a Proxy button, so you can connect before signing in.
- t.me/proxy and t.me/socks links ask to connect.

### Stories and channels
- Post stories from the side menu: a photo or video, a caption, who can see it, how long it stays and whether it stays on your profile. Channel admins can post stories from the chat menu.
- Manage chat has Reactions (all, some or none), Discussion to link a comments group, Statistics with growth, Boosts with level progress and the boost link, and Join requests.
- Boost channels and groups from the chat menu.

### Chats
- Translate messages into the app language.
- Report messages and chats with Telegram's report reasons.
- Transcribe voice and round video messages.
- Share your location live for 15 minutes, an hour or 8 hours, and stop sharing at any time.
- Admins see pending join requests in the chat, can approve or dismiss them and can require approval for new members.
- The scroll-down button shows whenever you are away from the newest messages, also in channels.
- Scrolling up through long chats is smoother and older messages load earlier.

### Security
- Two-step verification in Privacy and Security: set, change or turn off the cloud password, with a hint and a recovery email.

## 0.43.1 Arrow

- Updating the installed Windows version works again: the installer starts after the app closes, and the app reopens when it finishes. If the install fails, the next start says why.
- The update notice is shown in your language.
- The theme button in the side menu shows Day or Night for the current theme.
- Labels on the side menu buttons stay on one line.
- In the compact chat list next to a forum, the hover and selection highlight follows the avatar shape.

## 0.43.0 Arrow

### Video chats and live streams
- Groups and channels with an active video chat show a bar above the messages with the number of participants and a Join button.
- The video chat window shows participants' video, your camera and screen sharing as tiles, and a participant list with speaking, muted and raised-hand states.
- Controls for microphone, camera, screen sharing, raising your hand and leaving.
- Admins can start a video chat or a live stream from the chat menu, see the RTMP link and stream key, mute participants or let them speak, change their volume, lower hands and end the chat for everyone.
- Live streams in channels can be watched.

### Notifications
- After starting the app, you no longer get notifications for messages that arrived while it was closed.

## 0.42.3 Fan

### Stories and video
- The story viewer opens and closes with a fade and scale animation, and the viewers list slides up from the bottom.
- The viewers list shows likes and other reactions, and the header shows the reaction count.
- Video and story playback no longer slows down: sound is converted to a standard format, and the picture keeps its own steady clock.

### Chats
- Enter confirms the delete message, pin message, delete chat, clear history and block dialogs.
- Jumping to an unread reaction or mention marks it as read, so the badge goes away.
- The mentions folder also shows unread private chats.
- Capitalize sentences also works on pasted text.
- The bot command and mention popups animate out when you erase the / or @.
- Avatars highlight in their own shape on hover instead of a square.
- Holding a chat to preview it takes a little longer, so normal clicks don't open the preview.
- Opening and closing a forum animates smoothly, without text wrapping or jumps.

### Updates and crash reports
- Portable updates show a rounded "pixelGram is updating…" window in your language while the files are replaced.
- Crash reports list the last things the app did before it closed.

## 0.42.2 Fan

- New setting in Chat Settings: Capitalize sentences. When it's on, the first letter of each sentence becomes a capital letter as you type.
- Call buttons shrink and turn when pressed, and the microphone, camera and screen buttons morph from a circle to a cookie shape when switched on.
- The QR code and Cancel buttons on the sign-in screen animate as a Material 3 button group.

## 0.42.1 Fan

### Photo editor
- Text, shapes and stickers open with the first click, even if the mouse moves a little.
- The brush size slider follows the mouse reliably.
- The eraser also removes blur strokes.
- The bottom bar animates like other Material 3 Expressive button groups, and the sticker button has a sticker icon.
- Enter only presses Done in the editor.

### Chats
- Buttons and menu items no longer react to Enter after you click them.
- Menu items highlight edge to edge when pressed.
- Clearing the text of a message you are editing and pressing Enter asks to delete it.
- The attach menu lines up with its button again.

### Crash reports
- If the app closes unexpectedly without an error, the next start saves a crash log with the recent app and TDLib logs.
- Crash dumps from native code in the temp folder are now always collected into the logs folder.

## 0.42.0 Fan

### Photo editor
- Click a photo in the send preview to edit it over a blurred window: rotate, mirror, crop with corner handles and aspect ratios (Original, Square, 3:2, 16:9, 3:4, 9:16, Free).
- Draw with a pen, arrows, a see-through marker that gets stronger where strokes overlap, a blur brush and an eraser, with any color from a full color picker, brush sizes and undo/redo.
- Add stickers, text and shapes (circle, rectangle, star, bubble, arrow, filled or outlined). Move, resize and rotate them. An animated sticker turns the photo into a GIF.
- The send preview has a menu with Send without sound, Schedule message, High Quality and Hide with Spoiler. Without High Quality, photos are sent at the standard 1280 px size like in Telegram.

### Chats
- Swipe a message with the right mouse button or the touchpad to reply, react, pin, forward, copy or select it. Pick the action for each side in Chat Settings.
- Typing can show as a bubble in the chat or only in the header.
- Secret chats: start one from a chat's menu. They show a lock, waiting and cancelled states, and the encryption key.
- Auto-delete timer for chats, and a default timer in Privacy.
- Read all mentions and reactions at once, with jump buttons to unread ones.
- Chat themes and wallpapers. A chat's own wallpaper wins over the global one, and Telegram pattern and gradient backgrounds are drawn.
- Channels with protected content can't be forwarded, copied or saved, and screen capture is blocked while such a chat is open. Pin and Delete only show where they are allowed.
- The chat menu is shorter. Archive and folders live in the chat list menu, and Rename is in the profile menu.

### Groups and channels
- Invite links: primary link, extra links with name, time and user limits or join requests, revoke and delete.

### Gifts
- Collectibles can be worn as your status, transferred, put on sale, shown or hidden on your profile and shared.
- Regular gifts can be upgraded into collectibles.

### Mini apps
- Bot mini apps open in their own window with the Telegram WebApp bridge: theme, main and back buttons, popups, links and sending data.
- Advanced settings: Spoof client as Android makes mini apps see the app as Telegram for Android.

### Settings
- Blocked users list in Privacy.
- The Material You settings style follows regular Material 3 colors.

### Fixes
- Fixed crashes after a few minutes of watching videos, GIFs or live wallpapers, and when scrolling fast through media.
- Notifications show in every corner without freezing, and their sound plays again.
- The attach button is readable in the Vibrant style.
- The tray menu opens without a delay.
- Sticker packs have a right-click menu and can save a sticker as an image.

## 0.41.0 Pentagon

### Gifts
- Gifts play their animation all at once like in Telegram, keep playing while you hover and several can play together.
- Regular gifts open their details, with a menu to show or hide them on your profile or convert them to Stars.
- No white glow when hovering a gift, and the gift grid runs much smoother.

### Topics
- Opening a forum group collapses the chat list to avatars and shows its topics in their own panel next to the chat, with a back button, and topics switch in place instead of opening a mixed feed.

### Groups and channels
- Subscribers, members and admins open their profiles, and a right click offers promote, edit rights, restrict and ban.
- Channel profiles show Mute, Discuss, Manage and More like in Telegram, with Discuss opening the linked discussion group.
- Comments under posts show avatars and open the commenter's profile.

### Stories
- Story sound plays at normal speed.
- Your own stories show who viewed and liked them.
- Reply to a story or like it right from the viewer.

### Chats
- Replies to your messages show your name instead of "You", also in screenshots.
- Files, folders and links open on Windows and macOS even when no app is set for the file type.
- In the Vibrant style, reply names and read checks in your own bubbles stay readable.

### Tray
- The tray menu opens on scaled screens and with the taskbar at the top, and animates in.

### Performance and stability
- Fixed a crash while watching videos, and video frames are no longer copied on every draw.
- Freed native memory from decoded images and stickers, which could make the app run out of memory after long use.
- Images that were already loaded show at once without flicker, Apple and 3D emoji draw cheaper.
- Spoilers and the smooth caret animate at 30 fps and the spoiler shimmer pauses in the background.
- The chat list no longer rebuilds its rows when scrolling starts or stops.
- TDLib loads off the interface thread, so the startup shapes animate smoothly.

### Other
- Logs live in a logs folder: startup.log, crash logs with the date and tdlib.log.
- The sign-in screens and the remaining interface strings are translated.
- The Hide All chats setting is gone.

## 0.40.0 Arch

### Groups and channels
- Manage group or channel: open it from the chat menu or the Manage button in the profile, available to admins.
- Name and description, member permissions by message type (text, photos, videos, stickers and GIFs, music, files, voice and video messages, links, polls and more) and slow mode.
- Administrators: promote members and pick each admin right separately, edit or dismiss existing admins.
- Members, exceptions and removed users: search, restrict, remove from the group, ban with an option to delete all their messages, unban.
- Recent actions: the admin log of the last 48 hours.
- Member tags: set a tag of up to 16 characters in the member card. Tags show next to sender names, purple for the owner, green for admins and neutral for everyone else.

### Topics
- Forum groups show topic tabs above the chat with unread counters.
- A topic opens with its own messages and message field.
- New topics can be created right from the tabs, and topics can be turned on or off in Manage group.

### Chats
- While someone is typing, a small bubble with three bouncing dots appears under the last message and gives way to their message when it arrives.

## 0.39.0 MD

### Look and feel
- Settings style: Expressive segmented cards or a flat Material You list with a highlighted pill, in Appearance.
- Side menu: open it over the chats or let it push them aside, in Appearance.
- Settings open on My Profile, with Add Account and Log out behind the three dots.
- Muted chats get a grey unread badge.

### Chats
- The chat list keeps loading while you scroll instead of stopping at the first 100 chats.
- Drafts come back after you edit or cancel editing a message.
- Edit only shows up where Telegram allows it and tells you when an edit fails; captions of videos, files and audio can be edited too.
- The message field takes the cursor back after you click in the chat or press an inline button.
- Search, mention and inline bot suggestions no longer show results for an older query.

### Media
- Long audio plays as a stream instead of being decoded into memory first.
- Faster playback keeps the voice pitch.
- The gifts grid only builds the gifts you can see, animates them on hover and draws collectible patterns much faster.
- The "Release to cancel" hint no longer covers the recording.

### Speed
- A change to one user, status or file no longer redraws every message and chat row.
- TDLib updates are handled in ordered batches.
- Message text is laid out in one pass, GIFs and videos in chats redraw without rebuilding the message, sticker animations are cached.
- Files are prepared for sending off the interface thread, and call video frames are released right away.

### Fixes
- Images come back after clearing the cache or switching accounts.
- Updates check that the download is complete and no longer hang on a stalled connection.
- Chat windows count as focused: their messages get read and they don't notify.
- Quitting waits for Telegram to close so the last drafts are kept.
- Settings are saved safely, the archive shows its real size, dates and "last seen" are translated, failed lookups retry, removed accounts leave no folders behind and more unsafe file types ask before opening.

## 0.38.1

### Chats
- Double click on your own message can open it for editing: pick Edit under Double click on a message in Chat settings.
- Photos, videos and GIFs keep their own proportions instead of being cropped into a fixed frame, and the bubble fits them.

### Look and feel
- Message and chat menus drop the dark backdrop around their segments, with smaller items and the theme's container colors.
- With only the main folder the folder rail is hidden, the menu button takes its place above the archive.

### Fixes
- Choosing an item in a menu, like Copy text, no longer crashes the app with "RootNodeOwner is already disposed".
- Menus close with their animation instead of leaving a Delete item behind for a moment.
- Double clicking near the edge between two messages no longer starts selecting them.

## 0.38.0 U

### Calls
- Voice and video calls in private chats: call and video call buttons in the chat header, a Material 3 call window with mute, camera, screen sharing by display and end call.
- Incoming calls ring and can be accepted or declined. Four encryption emojis show once the call is connected.
- Calls use NTgCalls. Its library is downloaded from the official release on the first call and checked before use.

### Video messages
- Click the mic to switch to the camera, hold to record a round video. A live preview in the middle of the window and a ring show the 60 second limit; lock and slide to cancel work like voice messages.

### Profiles
- Stories with albums, a story viewer with progress bars and the Story Archive for your own stories.
- Stories and Gifts are tabs next to Photos, Videos and Links, with gift previews on the tab.
- Gifts: collections, unique gifts on their backdrop with the symbol pattern and their number.
- Collectible gifts: t.me/nft links and unique gifts open a card with the owner, the model, backdrop and symbol chances, how many were issued and a copy link button.
- Groups show Members with the member list. Channels you manage show Subscribers and Administrators.
- Right click your own badge to set an emoji or custom emoji and a caption for it.
- Badges next to your name in the side menu.

### Notifications and Sounds
- Rebuilt like the official app: all accounts, taskbar flash, sound volume, name and text in previews, per chat type toggles with exceptions, reactions, events, badge counters and system Focus mode.
- Choose the display, one of six screen positions and how many pop-ups stack.

### Search
- Chat search also finds public users, groups and channels by username.
- A username that doesn't exist shows a "not found" dialog instead of opening the browser, also from bios.

### Speed
- The side menu slides over the chats instead of relaying out the whole window.
- The blinking caret, animated stickers, GIFs and live wallpapers no longer keep the window redrawing all the time; they pause while pixelGram is in the background.
- Wallpapers are scaled once to the chat size.

### Look and feel
- Photo and video viewers open over the app on a translucent backdrop, without a system window frame.
- Videos show download progress and their size in MB or GB; video messages show progress too.
- Tabs are plain pills, Add Account moved to the settings menu and the My Account page is gone.
- Vibrant colors are vivid again.
- Videos show their first frame and length before sending.

### Fixes
- Quotes no longer wrap early and their background covers the whole quote across the bubble.
- No stray corners under the profile banner and under the mini player.
- Enter no longer reopens the attach menu or the new chat menu after clicking them.

## 0.37.0 Burst

### Attach menu
- The paperclip turns into a Material 3 menu: Photo or video, Document, Poll, Checklist, Article, Location and Music.
- Music sends tracks from your profile music.
- Location sends coordinates you paste, including from map links.

### Polls
- New poll editor with a description, photos on the description and options, and up to 12 options.
- Groups: Show Who Voted, Allow Multiple Answers, Allow Adding Options, Allow Revoting, Shuffle Options, Set Correct Answer and Limit Duration.
- Channels: Allow Multiple Answers, Allow Revoting, Shuffle Options, Set Correct Answer, Restrict to Subscribers, Limit by Country and Limit Duration.
- Polls are offered only in groups and channels. Polls show their description, and Retract vote appears only when revoting is allowed.

### Checklists and articles
- Create checklists with up to 30 tasks and choose whether others can add tasks or mark them as done. Sending a checklist needs Telegram Premium.
- New article editor laid out like the official one: a title on top, undo and redo, and a bottom bar with AI, emoji, text styles, lists, tables, formulas and photos.
- Text styles for articles: Heading, Text, Quote, Pullquote, Code and Footer. Ctrl+B, Ctrl+I, Ctrl+U and Ctrl+K work inside the text.
- Every tool is open to everyone. If Telegram refuses an article or an AI request, its answer, like RICH_MESSAGE_UNSUPPORTED, shows up in a pill.
- Checklists and articles from others are shown in chats. You can tick tasks, and long articles open in full.

### Code
- Type three backticks, the language, the code and three backticks again, and it turns into a code block right in the message field, with syntax highlighting.
- Code blocks in messages show the language on top, a copy button and syntax highlighting for Kotlin, Java, Python, JavaScript, TypeScript, C, C++, C#, Go, Rust, Swift, Dart, PHP, Ruby, Bash, SQL, Lua, JSON, YAML, CSS and HTML.
- Code block is also in the formatting menu and on Ctrl+Shift+C.

### Fixes
- Your own posts in channels look like regular posts instead of outgoing bubbles.
- Enter sends a forwarded message instead of opening the forward options.

## 0.36.0 Boom

### Bots
- Bot links no longer start the bot on their own. The chat opens with a big START button at the bottom that shows the start code from the link.
- Bots you have never started show the START button too.

### Safety
- Invite links show the chat first: photo, name, members and description, and you join only with the button.
- Hidden links and bot URL buttons show the real address and ask before opening it.
- Files that can run programs (.exe, .bat, .ps1, .jar, .msi, .lnk, .sh and more) ask before opening.
- Invisible and right-to-left characters are removed from file names, so a file can't pretend to have another extension.
- Only web, TON, mail and phone links go to other apps.
- Settings backups no longer include the passcode, auto-lock, local names or the device name.
- Safer handling of the local link port, download names and update downloads.

### Fixes
- Unknown tg:// links (like proxy links) no longer bounce between the system and pixelGram forever.

## 0.35.4

### Fixes
- Swipe to reply with a touchpad fills about five times faster, the same speed as in Telegram Desktop.

## 0.35.3

### Chats
- Swipe to reply with a touchpad works like Telegram Desktop: the direction locks at the start so scrolling no longer triggers it, the reply happens when you let go past the threshold so you can pull back to cancel, the message stretches a little past it and the reply icon bounces when you reach it.
- Drag files onto a chat in the list to open it with the send preview. Holding files over a chat for a moment opens it too.
- Thanks to Telegram Desktop (GPLv3) for the swipe logic, credited in the README.

## 0.35.2 I-beam

### Cursor
- New Kitty trail cursor style, ported from the kitty terminal's cursor trail. While you type the cursor stays put, on big jumps a trail stretches behind it and pulls in.
- Pick Liquid or Kitty trail in pixelGram Preferences → Cursor. Kitty trail has its own settings for the trail speed and how far the cursor has to jump before the trail starts.
- Thanks to Kovid Goyal and kitty (GPLv3), credited in the README.

## 0.35.1 Oval

### Reactions and emoji
- Reacting shows the animated emoji big right on the reaction with its effect around it, then it shrinks back into the reaction. Always big reactions makes it bigger.
- When the other person taps a big emoji, you see its effect too.
- The big emoji effect starts from the emoji itself instead of the empty space next to it.
- Right click a big emoji to copy it as a normal emoji.

### Chats
- The chat header shows what the other person is doing: typing, recording a voice message, choosing a sticker, watching 😂 and more.
- Search in the emoji and sticker panels. GIF search was already there.

### Settings
- Cursor settings moved to pixelGram Preferences, and the Message bubbles block is gone from Chat Settings.
- Material shapes in the empty chat area can be turned off in Chat Settings.
- Launch minimized and the monochrome tray icon appear with an animation.
- Birthday: day and month take two digits, up to 31 and 12, the year starts at 1877.

### Fixes
- The liquid cursor keeps up with fast typing instead of leaving a dot behind.
- Enter no longer opens the emoji and sticker panel after you used it once.

## 0.35.0 Oval

### Typing
- Liquid cursor in the message field: it stretches and flows to its new place, with settings for stretch, side pull, speed, width and smooth blinking in Chat Settings.
- The message field draws emoji in the chosen Apple or 3D style.

### Chats
- Hold a chat in the list to preview it without reading it. Scroll up for older messages, mark it as read or open it.
- Add people to contacts, edit or delete contacts, and rename anyone locally without adding them.
- Forwarding an album sends every part of it, and the forward preview shows photo and video thumbnails.
- Without an open chat, the empty area shows the mini player and Material 3 shapes.

### Reactions
- Reaction effects play over the reaction when you react, bigger with Always big reactions.
- The reaction pop animation plays every time, not every other time.

### Gifts
- Profiles show received gifts with sender avatars, and gift details show the sender, the recipient and whether the gift is saved.

### Window
- pixelGram remembers the window size and position. Turn it off in Advanced.
- Rounded window corners on systems that do not round them, like Windows 10.

### Fixes
- The language, local names and launch at login settings are kept after a restart.

## 0.34.0 Flower

### Languages
- pixelGram speaks Russian and Hebrew. Hebrew flips the whole interface right to left. Pick a language in Settings → Language, the other Telegram languages are listed and will come later.

### Media
- GIFs and videos play right inside messages without sound. A second click opens a viewer window with sound instead of a fullscreen one.
- Autoplay for GIFs and videos can be turned off in Battery and Animations, then the first click plays inside the message.
- Live wallpapers: pick a video as the chat wallpaper, and the theme colors come from its first frame.
- Right click a GIF to save it to your GIFs.

### Settings
- Search finds single options in every section, shows where they live and jumps to them with a highlight.
- The menu next to search has Edit profile and Log out.
- Advanced has the window title options (chat name, account, unread count, system frame) and system integration: tray icon, monochrome tray icon, launch at login and launch minimized.

### More
- Slide up while recording a voice message to lock it, the button turns into Send, and the cancel hint stops where releasing cancels.
- Verified badge for verified people and channels, channel profiles show their link, every username and the description.
- Always big reactions in Chat Settings.
- A menorah badge, and the pixelGram channel got its own banner.

### Fixes
- Badges no longer shift when you click them.
- Slider thumbs stay in place when you grab them.
- Swipe to reply works only with a touchpad and is easier to trigger. Mouse drags no longer move messages.

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
