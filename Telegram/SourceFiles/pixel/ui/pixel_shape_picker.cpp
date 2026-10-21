// This is the source code of pixelGram for Desktop.
// Based on AyuGram Desktop and Telegram Desktop, licensed under GPLv3.
#include "pixel/ui/pixel_shape_picker.h"

#include "ayu/ayu_ui_settings.h"
#include "pixel/ui/pixel_shapes.h"
#include "ui/painter.h"
#include "styles/style_pixel_ui.h"
#include "styles/style_widgets.h"

#include <QtGui/QtEvents>

namespace PixelUi {

AvatarShapePicker::AvatarShapePicker(QWidget *parent, int current)
: RpWidget(parent)
, _current(current) {
	setMouseTracking(true);
	resizeToWidth(st::pixelShapePickerCell * 6);
}

rpl::producer<int> AvatarShapePicker::chosen() const {
	return _chosen.events();
}

int AvatarShapePicker::count() const {
	return int(PixelShapes::AvatarShape::kCount);
}

int AvatarShapePicker::resizeGetHeight(int newWidth) {
	const auto &padding = st::pixelShapePickerPadding;
	const auto cell = st::pixelShapePickerCell;
	const auto available = newWidth - padding.left() - padding.right();
	_columns = std::max(available / cell, 1);
	const auto rows = (count() + _columns - 1) / _columns;
	return padding.top() + rows * cell + padding.bottom();
}

QRect AvatarShapePicker::cellRect(int index) const {
	const auto &padding = st::pixelShapePickerPadding;
	const auto cell = st::pixelShapePickerCell;
	return QRect(
		padding.left() + (index % _columns) * cell,
		padding.top() + (index / _columns) * cell,
		cell,
		cell);
}

int AvatarShapePicker::indexAt(QPoint point) const {
	for (auto i = 0; i != count(); ++i) {
		if (cellRect(i).contains(point)) {
			return i;
		}
	}
	return -1;
}

void AvatarShapePicker::setOver(int index) {
	if (_over == index) {
		return;
	}
	_over = index;
	setCursor((index >= 0) ? style::cur_pointer : style::cur_default);
	setToolTip((index >= 0)
		? PixelShapes::Title(PixelShapes::AvatarShape(index))
		: QString());
	update();
}

void AvatarShapePicker::paintEvent(QPaintEvent *e) {
	auto p = QPainter(this);
	auto hq = PainterHighQualityEnabler(p);
	p.setPen(Qt::NoPen);

	const auto inner = st::pixelShapePickerShape;
	const auto radius = st::pixelShapePickerRadius;
	for (auto i = 0; i != count(); ++i) {
		const auto cell = cellRect(i);
		if (!cell.intersects(e->rect())) {
			continue;
		}
		const auto selected = (i == _current);
		if (selected || i == _over) {
			p.setBrush(selected ? st::lightButtonBgOver : st::windowBgOver);
			p.drawRoundedRect(QRectF(cell).marginsRemoved(
				QMarginsF(2., 2., 2., 2.)), radius, radius);
		}
		auto shapeRect = QRectF(0., 0., inner, inner);
		shapeRect.moveCenter(QRectF(cell).center());
		p.setBrush(selected ? st::windowBgActive : st::windowSubTextFg);

		const auto shape = PixelShapes::AvatarShape(i);
		if (shape == PixelShapes::AvatarShape::Custom) {
			const auto r = double(AyuUiSettings::getAvatarCorners())
				/ AyuUiSettings::kMaxAvatarCorners
				* inner / 2.;
			p.drawRoundedRect(shapeRect, r, r);
		} else {
			p.drawPath(PixelShapes::Path(shape, shapeRect));
		}
	}
}

void AvatarShapePicker::mouseMoveEvent(QMouseEvent *e) {
	setOver(indexAt(e->pos()));
}

void AvatarShapePicker::mousePressEvent(QMouseEvent *e) {
	if (e->button() == Qt::LeftButton) {
		_pressed = indexAt(e->pos());
	}
}

void AvatarShapePicker::mouseReleaseEvent(QMouseEvent *e) {
	const auto pressed = std::exchange(_pressed, -1);
	if (e->button() != Qt::LeftButton
		|| pressed < 0
		|| pressed != indexAt(e->pos())
		|| pressed == _current) {
		return;
	}
	_current = pressed;
	update();
	_chosen.fire_copy(pressed);
}

void AvatarShapePicker::leaveEventHook(QEvent *e) {
	setOver(-1);
}

} // namespace PixelUi
