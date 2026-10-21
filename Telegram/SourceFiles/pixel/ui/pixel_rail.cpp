// This is the source code of pixelGram for Desktop.
// Based on AyuGram Desktop and Telegram Desktop, licensed under GPLv3.
#include "pixel/ui/pixel_rail.h"

#include "ui/effects/animations.h"
#include "ui/painter.h"
#include "ui/rp_widget.h"
#include "styles/style_pixel_ui.h"
#include "styles/style_window.h"

#include <QtCore/QEvent>

namespace PixelUi {
namespace {

constexpr auto kExpandDuration = crl::time(200);

class RailIndicator final : public QObject {
public:
	RailIndicator(
		not_null<Ui::RpWidget*> button,
		Fn<bool()> active,
		RailMode mode);

protected:
	bool eventFilter(QObject *o, QEvent *e) override;

private:
	void paint();
	[[nodiscard]] QRectF pillRect(float64 progress) const;

	const not_null<Ui::RpWidget*> _button;
	const Fn<bool()> _active;
	const RailMode _mode;
	Ui::Animations::Simple _expand;
	bool _wasActive = false;

};

RailIndicator::RailIndicator(
	not_null<Ui::RpWidget*> button,
	Fn<bool()> active,
	RailMode mode)
: QObject(button.get())
, _button(button)
, _active(std::move(active))
, _mode(mode)
, _wasActive(_active()) {
	_button->installEventFilter(this);
}

bool RailIndicator::eventFilter(QObject *o, QEvent *e) {
	// WHY: runs before the button's own paintEvent, so the pill ends up
	// below the icon and the label.
	if (o == _button.get() && e->type() == QEvent::Paint) {
		paint();
	}
	return false;
}

QRectF RailIndicator::pillRect(float64 progress) const {
	const auto width = _button->width();
	if (_mode == RailMode::TextOnly) {
		const auto full = QRectF(_button->rect()).marginsRemoved(
			st::pixelRailTextPillMargin);
		const auto shown = full.width() * progress;
		return QRectF(
			full.x() + (full.width() - shown) / 2.,
			full.y(),
			shown,
			full.height());
	}
	const auto size = st::pixelRailPillSize;
	const auto pillWidth = std::min(size.width(), width - 2 * 8)
		* progress;
	const auto centerY = (_mode == RailMode::IconsOnly)
		? (_button->height() / 2.)
		: float64(st::pixelRailPillCenterTop);
	return QRectF(
		(width - pillWidth) / 2.,
		centerY - size.height() / 2.,
		pillWidth,
		size.height());
}

void RailIndicator::paint() {
	const auto active = _active();
	if (_wasActive != active) {
		_wasActive = active;
		_expand.start(
			[=] { _button->update(); },
			active ? 0. : 1.,
			active ? 1. : 0.,
			kExpandDuration,
			anim::easeOutCirc);
	}
	auto p = QPainter(_button);
	p.fillRect(_button->rect(), st::windowFiltersButton.textBg);

	const auto progress = _expand.value(active ? 1. : 0.);
	if (progress <= 0.) {
		return;
	}
	const auto rect = pillRect(progress);
	const auto radius = (_mode == RailMode::TextOnly)
		? float64(st::pixelRailTextPillRadius)
		: (rect.height() / 2.);
	auto hq = PainterHighQualityEnabler(p);
	p.setPen(Qt::NoPen);
	p.setBrush(st::windowFiltersButton.textBgActive);
	p.drawRoundedRect(rect, radius, radius);
}

} // namespace

void InstallRailIndicator(
		not_null<Ui::RpWidget*> button,
		Fn<bool()> active,
		RailMode mode) {
	new RailIndicator(button, std::move(active), mode);
}

} // namespace PixelUi
