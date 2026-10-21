// This is the source code of pixelGram for Desktop.
// Based on AyuGram Desktop and Telegram Desktop, licensed under GPLv3.
//
// The shape geometry is a port of androidx graphics-shapes (RoundedPolygon)
// and androidx compose material3 MaterialShapes:
// Copyright 2023-2025 The Android Open Source Project
// Licensed under the Apache License, Version 2.0.
#include "pixel/ui/pixel_shapes.h"

#include "lang_auto.h"

#include <QtGui/QPainter>
#include <QtGui/QTransform>

#include <array>
#include <cmath>
#include <optional>

namespace PixelShapes {
namespace {

constexpr auto kDistanceEpsilon = 1e-4;

auto Current = AvatarShape::Custom;

struct Point {
	double x = 0.;
	double y = 0.;
};

Point operator+(Point a, Point b) { return { a.x + b.x, a.y + b.y }; }
Point operator-(Point a, Point b) { return { a.x - b.x, a.y - b.y }; }
Point operator*(Point a, double k) { return { a.x * k, a.y * k }; }
Point operator/(Point a, double k) { return { a.x / k, a.y / k }; }

[[nodiscard]] double Dot(Point a, Point b) {
	return a.x * b.x + a.y * b.y;
}

[[nodiscard]] double Length(Point a) {
	return std::sqrt(a.x * a.x + a.y * a.y);
}

[[nodiscard]] Point Direction(Point a) {
	const auto d = Length(a);
	return (d > 0.) ? (a / d) : Point();
}

[[nodiscard]] Point Rotate90(Point a) {
	return { -a.y, a.x };
}

[[nodiscard]] Point Lerp(Point a, Point b, double t) {
	return a * (1. - t) + b * t;
}

[[nodiscard]] Point Radial(double radius, double angle) {
	return { radius * std::cos(angle), radius * std::sin(angle) };
}

[[nodiscard]] Point RotateDegrees(Point p, double degrees, Point center) {
	const auto a = degrees * M_PI / 180.;
	const auto off = p - center;
	return Point{
		off.x * std::cos(a) - off.y * std::sin(a),
		off.x * std::sin(a) + off.y * std::cos(a),
	} + center;
}

struct Cubic {
	Point anchor0;
	Point control0;
	Point control1;
	Point anchor1;
};

[[nodiscard]] Cubic StraightLine(Point from, Point to) {
	return { from, Lerp(from, to, 1. / 3.), Lerp(from, to, 2. / 3.), to };
}

[[nodiscard]] Cubic Reverse(const Cubic &c) {
	return { c.anchor1, c.control1, c.control0, c.anchor0 };
}

[[nodiscard]] Cubic CircularArc(Point center, Point p0, Point p1) {
	const auto p0d = Direction(p0 - center);
	const auto p1d = Direction(p1 - center);
	const auto rotatedP0 = Rotate90(p0d);
	const auto rotatedP1 = Rotate90(p1d);
	const auto clockwise = Dot(rotatedP0, p1 - center) >= 0.;
	const auto cosa = Dot(p0d, p1d);
	if (cosa > 0.999) {
		return StraightLine(p0, p1);
	}
	const auto k = Length(p0 - center) * 4. / 3.
		* (std::sqrt(2. * (1. - cosa)) - std::sqrt(1. - cosa * cosa))
		/ (1. - cosa)
		* (clockwise ? 1. : -1.);
	return { p0, p0 + rotatedP0 * k, p1 - rotatedP1 * k, p1 };
}

struct Rounding {
	double radius = 0.;
	double smoothing = 0.;
};

constexpr auto kUnrounded = Rounding();
constexpr auto kRound15 = Rounding{ .radius = 0.15 };
constexpr auto kRound20 = Rounding{ .radius = 0.2 };
constexpr auto kRound30 = Rounding{ .radius = 0.3 };
constexpr auto kRound50 = Rounding{ .radius = 0.5 };
constexpr auto kRound100 = Rounding{ .radius = 1. };

[[nodiscard]] std::optional<Point> LineIntersection(
		Point p0,
		Point d0,
		Point p1,
		Point d1) {
	const auto rotatedD1 = Rotate90(d1);
	const auto den = Dot(d0, rotatedD1);
	if (std::abs(den) < kDistanceEpsilon) {
		return std::nullopt;
	}
	const auto num = Dot(p1 - p0, rotatedD1);
	if (std::abs(den) < kDistanceEpsilon * std::abs(num)) {
		return std::nullopt;
	}
	return p0 + d0 * (num / den);
}

class RoundedCorner final {
public:
	RoundedCorner(Point p0, Point p1, Point p2, Rounding rounding)
	: _p0(p0)
	, _p1(p1)
	, _p2(p2) {
		const auto v01 = p0 - p1;
		const auto v21 = p2 - p1;
		const auto d01 = Length(v01);
		const auto d21 = Length(v21);
		if (d01 > 0. && d21 > 0.) {
			_d1 = v01 / d01;
			_d2 = v21 / d21;
			_radius = rounding.radius;
			_smoothing = rounding.smoothing;
			const auto cosAngle = Dot(_d1, _d2);
			const auto sinAngle = std::sqrt(1. - cosAngle * cosAngle);
			_expectedRoundCut = (sinAngle > 1e-3)
				? (_radius * (cosAngle + 1.) / sinAngle)
				: 0.;
		}
	}

	[[nodiscard]] double expectedRoundCut() const {
		return _expectedRoundCut;
	}
	[[nodiscard]] double expectedCut() const {
		return (1. + _smoothing) * _expectedRoundCut;
	}

	[[nodiscard]] std::vector<Cubic> cubics(
			double allowedCut0,
			double allowedCut1) const {
		const auto allowedCut = std::min(allowedCut0, allowedCut1);
		if (_expectedRoundCut < kDistanceEpsilon
			|| allowedCut < kDistanceEpsilon
			|| _radius < kDistanceEpsilon) {
			return { StraightLine(_p1, _p1) };
		}
		const auto actualRoundCut = std::min(allowedCut, _expectedRoundCut);
		const auto smoothing0 = actualSmoothing(allowedCut0);
		const auto smoothing1 = actualSmoothing(allowedCut1);
		const auto actualR = _radius * actualRoundCut / _expectedRoundCut;
		const auto centerDistance = std::sqrt(
			actualR * actualR + actualRoundCut * actualRoundCut);
		const auto center = _p1
			+ Direction((_d1 + _d2) / 2.) * centerDistance;
		const auto intersection0 = _p1 + _d1 * actualRoundCut;
		const auto intersection2 = _p1 + _d2 * actualRoundCut;
		const auto flanking0 = flankingCurve(
			actualRoundCut,
			smoothing0,
			_p0,
			intersection0,
			intersection2,
			center,
			actualR);
		const auto flanking2 = Reverse(flankingCurve(
			actualRoundCut,
			smoothing1,
			_p2,
			intersection2,
			intersection0,
			center,
			actualR));
		return {
			flanking0,
			CircularArc(center, flanking0.anchor1, flanking2.anchor0),
			flanking2,
		};
	}

private:
	[[nodiscard]] double actualSmoothing(double allowedCut) const {
		if (allowedCut > expectedCut()) {
			return _smoothing;
		} else if (allowedCut > _expectedRoundCut) {
			return _smoothing
				* (allowedCut - _expectedRoundCut)
				/ (expectedCut() - _expectedRoundCut);
		}
		return 0.;
	}

	[[nodiscard]] Cubic flankingCurve(
			double actualRoundCut,
			double actualSmoothing,
			Point sideStart,
			Point circleIntersection,
			Point otherCircleIntersection,
			Point circleCenter,
			double actualR) const {
		const auto sideDirection = Direction(sideStart - _p1);
		const auto curveStart = _p1
			+ sideDirection * actualRoundCut * (1. + actualSmoothing);
		const auto p = Lerp(
			circleIntersection,
			(circleIntersection + otherCircleIntersection) / 2.,
			actualSmoothing);
		const auto curveEnd = circleCenter
			+ Direction(p - circleCenter) * actualR;
		const auto circleTangent = Rotate90(curveEnd - circleCenter);
		const auto anchorEnd = LineIntersection(
			sideStart,
			sideDirection,
			curveEnd,
			circleTangent).value_or(circleIntersection);
		const auto anchorStart = (curveStart + anchorEnd * 2.) / 3.;
		return { curveStart, anchorStart, anchorEnd, curveEnd };
	}

	Point _p0;
	Point _p1;
	Point _p2;
	Point _d1;
	Point _d2;
	double _radius = 0.;
	double _smoothing = 0.;
	double _expectedRoundCut = 0.;

};

using Polygon = std::vector<Cubic>;

[[nodiscard]] Polygon RoundedPolygon(
		const std::vector<Point> &vertices,
		const std::vector<Rounding> &rounding) {
	const auto n = int(vertices.size());
	auto corners = std::vector<RoundedCorner>();
	corners.reserve(n);
	for (auto i = 0; i != n; ++i) {
		corners.emplace_back(
			vertices[(i + n - 1) % n],
			vertices[i],
			vertices[(i + 1) % n],
			rounding[i]);
	}

	// For each side, how much of the round cut and of the smoothing cut
	// can be used, if the side is too short for both corners.
	struct CutAdjust {
		double round = 1.;
		double smooth = 1.;
	};
	auto adjusts = std::vector<CutAdjust>(n);
	for (auto i = 0; i != n; ++i) {
		const auto &next = corners[(i + 1) % n];
		const auto roundCut = corners[i].expectedRoundCut()
			+ next.expectedRoundCut();
		const auto cut = corners[i].expectedCut() + next.expectedCut();
		const auto side = Length(vertices[i] - vertices[(i + 1) % n]);
		if (roundCut > side) {
			adjusts[i] = { side / roundCut, 0. };
		} else if (cut > side) {
			adjusts[i] = { 1., (side - roundCut) / (cut - roundCut) };
		}
	}

	auto cornerCubics = std::vector<std::vector<Cubic>>();
	cornerCubics.reserve(n);
	for (auto i = 0; i != n; ++i) {
		auto allowed = std::array<double, 2>();
		for (auto delta = 0; delta != 2; ++delta) {
			const auto &adjust = adjusts[(i + n - 1 + delta) % n];
			allowed[delta] = corners[i].expectedRoundCut() * adjust.round
				+ (corners[i].expectedCut() - corners[i].expectedRoundCut())
					* adjust.smooth;
		}
		cornerCubics.push_back(corners[i].cubics(allowed[0], allowed[1]));
	}

	auto result = Polygon();
	for (auto i = 0; i != n; ++i) {
		const auto &corner = cornerCubics[i];
		result.insert(end(result), begin(corner), end(corner));
		result.push_back(StraightLine(
			corner.back().anchor1,
			cornerCubics[(i + 1) % n].front().anchor0));
	}
	return result;
}

[[nodiscard]] Polygon RoundedPolygon(
		const std::vector<Point> &vertices,
		Rounding rounding) {
	return RoundedPolygon(
		vertices,
		std::vector<Rounding>(vertices.size(), rounding));
}

[[nodiscard]] std::vector<Point> RegularVertices(int count, double radius) {
	auto result = std::vector<Point>();
	result.reserve(count);
	for (auto i = 0; i != count; ++i) {
		result.push_back(Radial(radius, M_PI / count * 2. * i));
	}
	return result;
}

[[nodiscard]] Polygon CirclePolygon(int vertices = 8) {
	const auto theta = M_PI / vertices;
	return RoundedPolygon(
		RegularVertices(vertices, 1. / std::cos(theta)),
		Rounding{ .radius = 1. });
}

[[nodiscard]] Polygon Rectangle(
		double width,
		double height,
		const std::vector<Rounding> &rounding) {
	const auto right = width / 2.;
	const auto bottom = height / 2.;
	return RoundedPolygon({
		{ right, bottom },
		{ -right, bottom },
		{ -right, -bottom },
		{ right, -bottom },
	}, rounding);
}

[[nodiscard]] Polygon Star(int count, double innerRadius, Rounding rounding) {
	auto vertices = std::vector<Point>();
	vertices.reserve(count * 2);
	for (auto i = 0; i != count; ++i) {
		vertices.push_back(Radial(1., M_PI / count * 2. * i));
		vertices.push_back(Radial(innerRadius, M_PI / count * (2. * i + 1.)));
	}
	return RoundedPolygon(vertices, rounding);
}

template <typename Transform>
[[nodiscard]] Polygon Transformed(Polygon polygon, Transform transform) {
	for (auto &cubic : polygon) {
		cubic.anchor0 = transform(cubic.anchor0);
		cubic.control0 = transform(cubic.control0);
		cubic.control1 = transform(cubic.control1);
		cubic.anchor1 = transform(cubic.anchor1);
	}
	return polygon;
}

[[nodiscard]] Polygon Rotated(Polygon polygon, double degrees) {
	return Transformed(std::move(polygon), [=](Point p) {
		return RotateDegrees(p, degrees, Point());
	});
}

[[nodiscard]] Polygon Scaled(Polygon polygon, double x, double y) {
	return Transformed(std::move(polygon), [=](Point p) {
		return Point{ p.x * x, p.y * y };
	});
}

struct PointRound {
	Point point;
	Rounding rounding;
};

[[nodiscard]] Polygon CustomPolygon(
		const std::vector<PointRound> &points,
		int reps,
		bool mirroring = false) {
	const auto center = Point{ 0.5, 0.5 };
	auto vertices = std::vector<Point>();
	auto rounding = std::vector<Rounding>();
	const auto add = [&](Point point, Rounding r) {
		vertices.push_back(point);
		rounding.push_back(r);
	};
	const auto count = int(points.size());
	if (mirroring) {
		auto angles = std::vector<double>();
		auto distances = std::vector<double>();
		for (const auto &p : points) {
			const auto off = p.point - center;
			angles.push_back(std::atan2(off.y, off.x) * 180. / M_PI);
			distances.push_back(Length(off));
		}
		const auto actualReps = reps * 2;
		const auto sectionAngle = 360. / actualReps;
		for (auto it = 0; it != actualReps; ++it) {
			const auto even = (it % 2 == 0);
			for (auto index = 0; index != count; ++index) {
				const auto i = even ? index : (count - 1 - index);
				if (i > 0 || even) {
					const auto degrees = sectionAngle * it
						+ (even
							? angles[i]
							: (sectionAngle - angles[i] + 2. * angles[0]));
					const auto a = degrees * M_PI / 180.;
					add(
						Point{ std::cos(a), std::sin(a) } * distances[i]
							+ center,
						points[i].rounding);
				}
			}
		}
	} else {
		for (auto it = 0; it != count * reps; ++it) {
			const auto &p = points[it % count];
			add(
				RotateDegrees(p.point, (it / count) * 360. / reps, center),
				p.rounding);
		}
	}
	return RoundedPolygon(vertices, rounding);
}

[[nodiscard]] Polygon Build(AvatarShape shape) {
	using Shape = AvatarShape;
	using R = Rounding;
	switch (shape) {
	case Shape::Custom:
	case Shape::kCount:
	case Shape::Circle: return CirclePolygon(10);
	case Shape::Square: return Rectangle(1., 1., std::vector<Rounding>(4, kRound30));
	case Shape::Slanted: return CustomPolygon({
		{ { 0.926, 0.970 }, R{ 0.189, 0.811 } },
		{ { -0.021, 0.967 }, R{ 0.187, 0.057 } },
	}, 2);
	case Shape::Arch: return Rotated(RoundedPolygon(
		RegularVertices(4, 1.),
		{ kRound100, kRound100, kRound20, kRound20 }), -135.);
	case Shape::Fan: return CustomPolygon({
		{ { 1.004, 1.000 }, R{ 0.148, 0.417 } },
		{ { 0.000, 1.000 }, R{ 0.151 } },
		{ { 0.000, -0.003 }, R{ 0.148 } },
		{ { 0.978, 0.020 }, R{ 0.803 } },
	}, 1);
	case Shape::Arrow: return CustomPolygon({
		{ { 0.500, 0.892 }, R{ 0.313 } },
		{ { -0.216, 1.050 }, R{ 0.207 } },
		{ { 0.499, -0.160 }, R{ 0.215, 1.000 } },
		{ { 1.225, 1.060 }, R{ 0.211 } },
	}, 1);
	case Shape::SemiCircle: return Rectangle(
		1.6,
		1.,
		{ kRound20, kRound20, kRound100, kRound100 });
	case Shape::Oval: return Rotated(Scaled(CirclePolygon(), 1., 0.64), -45.);
	case Shape::Pill: return CustomPolygon({
		{ { 0.961, 0.039 }, R{ 0.426 } },
		{ { 1.001, 0.428 }, kUnrounded },
		{ { 1.000, 0.609 }, R{ 1.000 } },
	}, 2, true);
	case Shape::Triangle: return Rotated(
		RoundedPolygon(RegularVertices(3, 1.), kRound20),
		-90.);
	case Shape::Diamond: return CustomPolygon({
		{ { 0.500, 1.096 }, R{ 0.151, 0.524 } },
		{ { 0.040, 0.500 }, R{ 0.159 } },
	}, 2);
	case Shape::ClamShell: return CustomPolygon({
		{ { 0.171, 0.841 }, R{ 0.159 } },
		{ { -0.020, 0.500 }, R{ 0.140 } },
		{ { 0.170, 0.159 }, R{ 0.159 } },
	}, 2);
	case Shape::Pentagon: return CustomPolygon({
		{ { 0.500, -0.009 }, R{ 0.172 } },
		{ { 1.030, 0.365 }, R{ 0.164 } },
		{ { 0.828, 0.970 }, R{ 0.169 } },
	}, 1, true);
	case Shape::Gem: return CustomPolygon({
		{ { 0.499, 1.023 }, R{ 0.241, 0.778 } },
		{ { -0.005, 0.792 }, R{ 0.208 } },
		{ { 0.073, 0.258 }, R{ 0.228 } },
		{ { 0.433, -0.000 }, R{ 0.491 } },
	}, 1, true);
	case Shape::Sunny: return Star(8, 0.8, kRound15);
	case Shape::VerySunny: return CustomPolygon({
		{ { 0.500, 1.080 }, R{ 0.085 } },
		{ { 0.358, 0.843 }, R{ 0.085 } },
	}, 8);
	case Shape::Cookie4Sided: return CustomPolygon({
		{ { 1.237, 1.236 }, R{ 0.258 } },
		{ { 0.500, 0.918 }, R{ 0.233 } },
	}, 4);
	case Shape::Cookie6Sided: return CustomPolygon({
		{ { 0.723, 0.884 }, R{ 0.394 } },
		{ { 0.500, 1.099 }, R{ 0.398 } },
	}, 6);
	case Shape::Cookie7Sided: return Rotated(Star(7, 0.75, kRound50), -90.);
	case Shape::Cookie9Sided: return Rotated(Star(9, 0.8, kRound50), -90.);
	case Shape::Cookie12Sided: return Rotated(Star(12, 0.8, kRound50), -90.);
	case Shape::Ghostish: return CustomPolygon({
		{ { 0.500, 0. }, R{ 1.000 } },
		{ { 1., 0. }, R{ 1.000 } },
		{ { 1., 1.140 }, R{ 0.254, 0.106 } },
		{ { 0.575, 0.906 }, R{ 0.253 } },
	}, 1, true);
	case Shape::Clover4Leaf: return CustomPolygon({
		{ { 0.500, 0.074 }, kUnrounded },
		{ { 0.725, -0.099 }, R{ 0.476 } },
	}, 4, true);
	case Shape::Clover8Leaf: return CustomPolygon({
		{ { 0.500, 0.036 }, kUnrounded },
		{ { 0.758, -0.101 }, R{ 0.209 } },
	}, 8);
	case Shape::Burst: return CustomPolygon({
		{ { 0.500, -0.006 }, R{ 0.006 } },
		{ { 0.592, 0.158 }, R{ 0.006 } },
	}, 12);
	case Shape::SoftBurst: return CustomPolygon({
		{ { 0.193, 0.277 }, R{ 0.053 } },
		{ { 0.176, 0.055 }, R{ 0.053 } },
	}, 10);
	case Shape::Boom: return CustomPolygon({
		{ { 0.457, 0.296 }, R{ 0.007 } },
		{ { 0.500, -0.051 }, R{ 0.007 } },
	}, 15);
	case Shape::SoftBoom: return CustomPolygon({
		{ { 0.733, 0.454 }, kUnrounded },
		{ { 0.839, 0.437 }, R{ 0.532 } },
		{ { 0.949, 0.449 }, R{ 0.439, 1.000 } },
		{ { 0.998, 0.478 }, R{ 0.174 } },
	}, 16, true);
	case Shape::Flower: return CustomPolygon({
		{ { 0.370, 0.187 }, kUnrounded },
		{ { 0.416, 0.049 }, R{ 0.381 } },
		{ { 0.479, 0.001 }, R{ 0.095 } },
	}, 8, true);
	case Shape::Puffy: return Scaled(CustomPolygon({
		{ { 0.500, 0.053 }, kUnrounded },
		{ { 0.545, -0.040 }, R{ 0.405 } },
		{ { 0.670, -0.035 }, R{ 0.426 } },
		{ { 0.717, 0.066 }, R{ 0.574 } },
		{ { 0.722, 0.128 }, kUnrounded },
		{ { 0.777, 0.002 }, R{ 0.360 } },
		{ { 0.914, 0.149 }, R{ 0.660 } },
		{ { 0.926, 0.289 }, R{ 0.660 } },
		{ { 0.881, 0.346 }, kUnrounded },
		{ { 0.940, 0.344 }, R{ 0.126 } },
		{ { 1.003, 0.437 }, R{ 0.255 } },
	}, 2, true), 1., 0.742);
	case Shape::PuffyDiamond: return CustomPolygon({
		{ { 0.870, 0.130 }, R{ 0.146 } },
		{ { 0.818, 0.357 }, kUnrounded },
		{ { 1.000, 0.332 }, R{ 0.853 } },
	}, 4, true);
	case Shape::PixelCircle: return CustomPolygon({
		{ { 0.500, 0.000 }, kUnrounded },
		{ { 0.704, 0.000 }, kUnrounded },
		{ { 0.704, 0.065 }, kUnrounded },
		{ { 0.843, 0.065 }, kUnrounded },
		{ { 0.843, 0.148 }, kUnrounded },
		{ { 0.926, 0.148 }, kUnrounded },
		{ { 0.926, 0.296 }, kUnrounded },
		{ { 1.000, 0.296 }, kUnrounded },
	}, 2, true);
	case Shape::PixelTriangle: return CustomPolygon({
		{ { 0.110, 0.500 }, kUnrounded },
		{ { 0.113, 0.000 }, kUnrounded },
		{ { 0.287, 0.000 }, kUnrounded },
		{ { 0.287, 0.087 }, kUnrounded },
		{ { 0.421, 0.087 }, kUnrounded },
		{ { 0.421, 0.170 }, kUnrounded },
		{ { 0.560, 0.170 }, kUnrounded },
		{ { 0.560, 0.265 }, kUnrounded },
		{ { 0.674, 0.265 }, kUnrounded },
		{ { 0.675, 0.344 }, kUnrounded },
		{ { 0.789, 0.344 }, kUnrounded },
		{ { 0.789, 0.439 }, kUnrounded },
		{ { 0.888, 0.439 }, kUnrounded },
	}, 1, true);
	case Shape::Bun: return CustomPolygon({
		{ { 0.796, 0.500 }, kUnrounded },
		{ { 0.853, 0.518 }, R{ 1. } },
		{ { 0.992, 0.631 }, R{ 1. } },
		{ { 0.968, 1.000 }, R{ 1. } },
	}, 2, true);
	case Shape::Heart: return CustomPolygon({
		{ { 0.500, 0.268 }, R{ 0.016 } },
		{ { 0.792, -0.066 }, R{ 0.958 } },
		{ { 1.064, 0.276 }, R{ 1.000 } },
		{ { 0.501, 0.946 }, R{ 0.129 } },
	}, 1, true);
	}
	return CirclePolygon(10);
}

// RoundedPolygon.normalized(): fits into the unit square, using the
// approximate (control point) bounds just like the original.
[[nodiscard]] QPainterPath NormalizedPath(const Polygon &polygon) {
	auto path = QPainterPath();
	if (polygon.empty()) {
		return path;
	}
	path.moveTo(polygon.front().anchor0.x, polygon.front().anchor0.y);
	for (const auto &cubic : polygon) {
		path.cubicTo(
			cubic.control0.x,
			cubic.control0.y,
			cubic.control1.x,
			cubic.control1.y,
			cubic.anchor1.x,
			cubic.anchor1.y);
	}
	path.closeSubpath();

	const auto bounds = path.controlPointRect();
	const auto side = std::max(bounds.width(), bounds.height());
	if (side <= 0.) {
		return QPainterPath();
	}
	auto transform = QTransform();
	transform.scale(1. / side, 1. / side);
	transform.translate(
		(side - bounds.width()) / 2. - bounds.x(),
		(side - bounds.height()) / 2. - bounds.y());
	return transform.map(path);
}

[[nodiscard]] const QPainterPath &UnitPath(AvatarShape shape) {
	static auto cache = std::array<
		std::optional<QPainterPath>,
		size_t(AvatarShape::kCount)>();
	const auto index = size_t(shape);
	if (index >= cache.size()) {
		return UnitPath(AvatarShape::Circle);
	}
	if (!cache[index]) {
		cache[index] = NormalizedPath(Build(shape));
	}
	return *cache[index];
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
	// Same as MaterialShapes toShape(): scale the unit shape to the size,
	// then center it by its bounds.
	auto result = QTransform::fromScale(rect.width(), rect.height()).map(
		UnitPath(shape));
	const auto center = result.controlPointRect().center();
	result.translate(rect.center() - center);
	return result;
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
		p.fillPath(
			CurrentPath(QRectF(QPointF(), QSizeF(size * ratio))),
			Qt::white);
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
		p.fillPath(
			CurrentPath(QRectF(QPointF(), QSizeF(image.size()))),
			Qt::white);
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
	case Shape::Slanted: return tr::pixel_ShapeSlanted(tr::now);
	case Shape::Arch: return tr::pixel_ShapeArch(tr::now);
	case Shape::Fan: return tr::pixel_ShapeFan(tr::now);
	case Shape::Arrow: return tr::pixel_ShapeArrow(tr::now);
	case Shape::SemiCircle: return tr::pixel_ShapeSemiCircle(tr::now);
	case Shape::Oval: return tr::pixel_ShapeOval(tr::now);
	case Shape::Pill: return tr::pixel_ShapePill(tr::now);
	case Shape::Triangle: return tr::pixel_ShapeTriangle(tr::now);
	case Shape::Diamond: return tr::pixel_ShapeDiamond(tr::now);
	case Shape::ClamShell: return tr::pixel_ShapeClamShell(tr::now);
	case Shape::Pentagon: return tr::pixel_ShapePentagon(tr::now);
	case Shape::Gem: return tr::pixel_ShapeGem(tr::now);
	case Shape::Sunny: return tr::pixel_ShapeSunny(tr::now);
	case Shape::VerySunny: return tr::pixel_ShapeVerySunny(tr::now);
	case Shape::Cookie4Sided: return tr::pixel_ShapeCookie4(tr::now);
	case Shape::Cookie6Sided: return tr::pixel_ShapeCookie6(tr::now);
	case Shape::Cookie7Sided: return tr::pixel_ShapeCookie7(tr::now);
	case Shape::Cookie9Sided: return tr::pixel_ShapeCookie9(tr::now);
	case Shape::Cookie12Sided: return tr::pixel_ShapeCookie12(tr::now);
	case Shape::Ghostish: return tr::pixel_ShapeGhostish(tr::now);
	case Shape::Clover4Leaf: return tr::pixel_ShapeClover4(tr::now);
	case Shape::Clover8Leaf: return tr::pixel_ShapeClover8(tr::now);
	case Shape::Burst: return tr::pixel_ShapeBurst(tr::now);
	case Shape::SoftBurst: return tr::pixel_ShapeSoftBurst(tr::now);
	case Shape::Boom: return tr::pixel_ShapeBoom(tr::now);
	case Shape::SoftBoom: return tr::pixel_ShapeSoftBoom(tr::now);
	case Shape::Flower: return tr::pixel_ShapeFlower(tr::now);
	case Shape::Puffy: return tr::pixel_ShapePuffy(tr::now);
	case Shape::PuffyDiamond: return tr::pixel_ShapePuffyDiamond(tr::now);
	case Shape::PixelCircle: return tr::pixel_ShapePixelCircle(tr::now);
	case Shape::PixelTriangle: return tr::pixel_ShapePixelTriangle(tr::now);
	case Shape::Bun: return tr::pixel_ShapeBun(tr::now);
	case Shape::Heart: return tr::pixel_ShapeHeart(tr::now);
	case Shape::kCount: break;
	}
	return QString();
}

} // namespace PixelShapes
