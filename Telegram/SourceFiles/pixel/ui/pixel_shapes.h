// This is the source code of pixelGram for Desktop.
// Based on AyuGram Desktop and Telegram Desktop, licensed under GPLv3.
#pragma once

#include <QtGui/QPainterPath>
#include <QtGui/QImage>

namespace PixelShapes {

// Official Material 3 shapes, in androidx MaterialShapes order.
// Values are persisted in settings, append new shapes at the end only.
enum class AvatarShape : int {
	Custom = 0, // AyuGram corner radius slider.
	Circle,
	Square,
	Slanted,
	Arch,
	Fan,
	Arrow,
	SemiCircle,
	Oval,
	Pill,
	Triangle,
	Diamond,
	ClamShell,
	Pentagon,
	Gem,
	Sunny,
	VerySunny,
	Cookie4Sided,
	Cookie6Sided,
	Cookie7Sided,
	Cookie9Sided,
	Cookie12Sided,
	Ghostish,
	Clover4Leaf,
	Clover8Leaf,
	Burst,
	SoftBurst,
	Boom,
	SoftBoom,
	Flower,
	Puffy,
	PuffyDiamond,
	PixelCircle,
	PixelTriangle,
	Bun,
	Heart,

	kCount,
};

[[nodiscard]] AvatarShape AvatarShapeFromInt(int value);

void SetAvatarShape(AvatarShape shape);
[[nodiscard]] AvatarShape CurrentAvatarShape();

// True when a Material 3 shape replaces AyuGram corner rounding.
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
