// This is the source code of pixelGram for Desktop.
// Based on AyuGram Desktop and Telegram Desktop, licensed under GPLv3.
#pragma once

namespace Ui {
class RpWidget;
} // namespace Ui

namespace PixelUi {

enum class RailMode {
	TextAndIcons,
	IconsOnly,
	TextOnly,
};

// Paints a Material 3 navigation rail indicator (pill) under the
// button contents. The button must use a transparent background style.
void InstallRailIndicator(
	not_null<Ui::RpWidget*> button,
	Fn<bool()> active,
	RailMode mode);

} // namespace PixelUi
