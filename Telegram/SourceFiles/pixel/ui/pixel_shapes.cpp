// This is the source code of pixelGram for Desktop.
// Based on AyuGram Desktop and Telegram Desktop, licensed under GPLv3.
#include "pixel/ui/pixel_shapes.h"

#include "lang_auto.h"

#include <QtGui/QPainter>
#include <QtGui/QTransform>

#include <cmath>

namespace PixelShapes {
namespace {

constexpr auto kPolarSteps = 360;
constexpr auto kPixelGrid = 10;

auto Current = AvatarShape::Custom;

using Radius = Fn<double(double)>;

[[nodiscard]] QPainterPath Polar(Radius radius, double rotation = 0.) {
	auto result = QPainterPath();
	for (auto i = 0; i != kPolarSteps; ++i) {
		const auto angle = 2. * M_PI * i / kPolarSteps;
		const auto r = radius(angle - rotation);
		const auto point = QPointF(r * std::cos(angle), r * std::sin(angle));
		if (!i) {
			result.moveTo(point);
		} else {
			result.lineTo(point);
		}
	}
	result.closeSubpath();
	return result;
}

[[nodiscard]] QPainterPath Cookie(int sides, double depth, double rotation) {
	return Polar([=](double angle) {
		return 1. + depth * std::cos(sides * angle);
	}, rotation);
}

// Lobes with deep, sharp notches between them.
[[nodiscard]] QPainterPath Clover(int leaves, double depth, double rotation) {
	return Polar([=](double angle) {
		const auto wave = std::abs(std::cos(leaves * angle / 2.));
		return (1. - depth) + depth * std::sqrt(wave);
	}, rotation);
}

[[nodiscard]] QPainterPath RoundedPolygon(
		int sides,
		double rounding,
		double rotation) {
	auto points = std::vector<QPointF>();
	points.reserve(sides);
	for (auto i = 0; i != sides; ++i) {
		const auto angle = rotation + 2. * M_PI * i / sides;
		points.emplace_back(std::cos(angle), std::sin(angle));
	}
	const auto lerp = [](QPointF a, QPointF b, double t) {
		return a + (b - a) * t;
	};
	auto result = QPainterPath();
	for (auto i = 0; i != sides; ++i) {
		const auto &prev = points[(i + sides - 1) % sides];
		const auto &vertex = points[i];
		const auto &next = points[(i + 1) % sides];
		const auto from = lerp(vertex, prev, rounding);
		const auto to = lerp(vertex, next, rounding);
		if (!i) {
			result.moveTo(from);
		} else {
			result.lineTo(from);
		}
		result.quadTo(vertex, to);
	}
	result.closeSubpath();
	return result;
}

[[nodiscard]] QPainterPath PillPath() {
	auto capsule = QPainterPath();
	constexpr auto kLength = 1.;
	constexpr auto kThickness = 0.6;
	capsule.addRoundedRect(
		QRectF(-kLength / 2., -kThickness / 2., kLength, kThickness),
		kThickness / 2.,
		kThickness / 2.);
	return QTransform().rotate(-45.).map(capsule);
}

[[nodiscard]] QPainterPath ArchPath() {
	auto result = QPainterPath();
	const auto bottom = 0.2;
	result.moveTo(0., 0.5);
	result.arcTo(QRectF(0., 0., 1., 1.), 180., -180.);
	result.lineTo(1., 1. - bottom);
	result.quadTo(1., 1., 1. - bottom, 1.);
	result.lineTo(bottom, 1.);
	result.quadTo(0., 1., 0., 1. - bottom);
	result.closeSubpath();
	return result;
}

[[nodiscard]] QPainterPath PixelCirclePath() {
	auto result = QPainterPath();
	const auto half = kPixelGrid / 2.;
	for (auto row = 0; row != kPixelGrid; ++row) {
		const auto y = (row + 0.5) - half;
		const auto span = std::sqrt(std::max(half * half - y * y, 0.));
		const auto cells = int(std::round(span));
		if (cells > 0) {
			result.addRect(QRectF(half - cells, row, cells * 2., 1.));
		}
	}
	return result.simplified();
}

[[nodiscard]] QPainterPath HeartPath() {
	auto result = QPainterPath();
	result.moveTo(0.5, 0.95);
	result.cubicTo(0.1, 0.7, -0.05, 0.4, 0.08, 0.2);
	result.cubicTo(0.2, 0.02, 0.42, 0.05, 0.5, 0.25);
	result.cubicTo(0.58, 0.05, 0.8, 0.02, 0.92, 0.2);
	result.cubicTo(1.05, 0.4, 0.9, 0.7, 0.5, 0.95);
	result.closeSubpath();
	return result;
}

[[nodiscard]] QPainterPath UnitPath(AvatarShape shape) {
	using Shape = AvatarShape;
	auto result = QPainterPath();
	switch (shape) {
	case Shape::Custom:
	case Shape::Circle:
	case Shape::kCount:
		result.addEllipse(QRectF(0., 0., 1., 1.));
		return result;
	case Shape::Square:
		result.addRoundedRect(QRectF(0., 0., 1., 1.), 0.3, 0.3);
		return result;
	case Shape::Pill: return PillPath();
	case Shape::Cookie4: return Cookie(4, 0.09, M_PI / 4.);
	case Shape::Cookie6: return Cookie(6, 0.065, 0.);
	case Shape::Cookie7: return Cookie(7, 0.06, -M_PI / 2.);
	case Shape::Cookie9: return Cookie(9, 0.05, -M_PI / 2.);
	case Shape::Cookie12: return Cookie(12, 0.04, 0.);
	case Shape::Clover4: return Clover(4, 0.3, M_PI / 4.);
	case Shape::Clover8: return Clover(8, 0.2, 0.);
	case Shape::Sunny: return Cookie(8, 0.07, 0.);
	case Shape::VerySunny: return Cookie(8, 0.14, 0.);
	case Shape::SoftBurst: return Cookie(10, 0.1, -M_PI / 2.);
	case Shape::Diamond: return RoundedPolygon(4, 0.28, -M_PI / 2.);
	case Shape::Pentagon: return RoundedPolygon(5, 0.25, -M_PI / 2.);
	case Shape::Arch: return ArchPath();
	case Shape::PixelCircle: return PixelCirclePath();
	case Shape::Heart: return HeartPath();
	}
	result.addEllipse(QRectF(0., 0., 1., 1.));
	return result;
}

[[nodiscard]] QPainterPath FitInto(const QPainterPath &path, const QRectF &rect) {
	const auto bounds = path.boundingRect();
	if (bounds.isEmpty() || rect.isEmpty()) {
		return QPainterPath();
	}
	const auto scale = std::min(
		rect.width() / bounds.width(),
		rect.height() / bounds.height());
	auto transform = QTransform();
	transform.translate(rect.center().x(), rect.center().y());
	transform.scale(scale, scale);
	transform.translate(-bounds.center().x(), -bounds.center().y());
	return transform.map(path);
}

} // namespace

AvatarShape AvatarShapeFromInt(int value) {
	return (value >= 0 && value < int(AvatarShape::kCount))
		? AvatarShape(value)
		: AvatarShape::Custom;
}

void SetAvatarShape(AvatarShape shape) {
	Current = shape;
}

AvatarShape CurrentAvatarShape() {
	return Current;
}

bool HasCustomPath() {
	return (Current != AvatarShape::Custom);
}

double ApproximateRadius(double size) {
	return (Current == AvatarShape::Square) ? (size * 0.3) : (size / 2.);
}

QPainterPath Path(AvatarShape shape, const QRectF &rect) {
	return FitInto(UnitPath(shape), rect);
}

QPainterPath CurrentPath(const QRectF &rect) {
	return Path(Current, rect);
}

QImage Mask(QSize size, int ratio) {
	auto result = QImage(size * ratio, QImage::Format_ARGB32_Premultiplied);
	result.fill(Qt::transparent);
	{
		auto p = QPainter(&result);
		p.setRenderHint(QPainter::Antialiasing);
		p.fillPath(CurrentPath(QRectF(QPointF(), QSizeF(size * ratio))), Qt::white);
	}
	result.setDevicePixelRatio(ratio);
	return result;
}

QImage Apply(QImage &&image) {
	if (image.isNull()) {
		return std::move(image);
	}
	const auto ratio = image.devicePixelRatio();
	auto result = QImage(image.size(), QImage::Format_ARGB32_Premultiplied);
	result.fill(Qt::transparent);
	{
		auto p = QPainter(&result);
		p.setRenderHint(QPainter::Antialiasing);
		p.fillPath(CurrentPath(QRectF(QPointF(), QSizeF(image.size()))), Qt::white);
		p.setCompositionMode(QPainter::CompositionMode_SourceIn);
		image.setDevicePixelRatio(1.);
		p.drawImage(0, 0, image);
	}
	result.setDevicePixelRatio(ratio);
	return result;
}

QString Title(AvatarShape shape) {
	using Shape = AvatarShape;
	switch (shape) {
	case Shape::Custom: return tr::pixel_ShapeCustom(tr::now);
	case Shape::Circle: return tr::pixel_ShapeCircle(tr::now);
	case Shape::Square: return tr::pixel_ShapeSquare(tr::now);
	case Shape::Pill: return tr::pixel_ShapePill(tr::now);
	case Shape::Cookie4: return tr::pixel_ShapeCookie4(tr::now);
	case Shape::Cookie6: return tr::pixel_ShapeCookie6(tr::now);
	case Shape::Cookie7: return tr::pixel_ShapeCookie7(tr::now);
	case Shape::Cookie9: return tr::pixel_ShapeCookie9(tr::now);
	case Shape::Cookie12: return tr::pixel_ShapeCookie12(tr::now);
	case Shape::Clover4: return tr::pixel_ShapeClover4(tr::now);
	case Shape::Clover8: return tr::pixel_ShapeClover8(tr::now);
	case Shape::Sunny: return tr::pixel_ShapeSunny(tr::now);
	case Shape::VerySunny: return tr::pixel_ShapeVerySunny(tr::now);
	case Shape::SoftBurst: return tr::pixel_ShapeSoftBurst(tr::now);
	case Shape::Diamond: return tr::pixel_ShapeDiamond(tr::now);
	case Shape::Pentagon: return tr::pixel_ShapePentagon(tr::now);
	case Shape::Arch: return tr::pixel_ShapeArch(tr::now);
	case Shape::PixelCircle: return tr::pixel_ShapePixelCircle(tr::now);
	case Shape::Heart: return tr::pixel_ShapeHeart(tr::now);
	case Shape::kCount: break;
	}
	return QString();
}

} // namespace PixelShapes
