// This is the source code of pixelGram for Desktop.
// Based on AyuGram Desktop and Telegram Desktop, licensed under GPLv3.
#pragma once

#include <QtGui/QPainterPath>
#include <QtGui/QImage>

namespace PixelShapes {

// Values are persisted in settings, append new shapes at the end only.
enum class AvatarShape : int {
	Custom = 0, // AyuGram corner radius slider.
	Circle,
	Square,
	Pill,
	Cookie4,
	Cookie6,
	Cookie7,
	Cookie9,
	Cookie12,
	Clover4,
	Clover8,
	Sunny,
	VerySunny,
	SoftBurst,
	Diamond,
	Pentagon,
	Arch,
	PixelCircle,
	Heart,

	kCount,
};

[[nodiscard]] AvatarShape AvatarShapeFromInt(int value);

void SetAvatarShape(AvatarShape shape);
[[nodiscard]] AvatarShape CurrentAvatarShape();

// True when a non-rounded-rect shape replaces AyuGram corner rounding.
[[nodiscard]] bool HasCustomPath();

// Approximate corner radius for places that can only draw rounded rects.
[[nodiscard]] double ApproximateRadius(double size);

[[nodiscard]] QPainterPath Path(AvatarShape shape, const QRectF &rect);
[[nodiscard]] QPainterPath CurrentPath(const QRectF &rect);

// Mask image of device pixel size, white inside the shape.
[[nodiscard]] QImage Mask(QSize size, int ratio);

// Clips the image to the current shape, keeps its device pixel ratio.
[[nodiscard]] QImage Apply(QImage &&image);

[[nodiscard]] QString Title(AvatarShape shape);

} // namespace PixelShapes
