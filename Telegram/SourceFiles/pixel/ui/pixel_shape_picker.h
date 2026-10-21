// This is the source code of pixelGram for Desktop.
// Based on AyuGram Desktop and Telegram Desktop, licensed under GPLv3.
#pragma once

#include "ui/rp_widget.h"

namespace PixelUi {

// Grid of Material 3 avatar shapes.
class AvatarShapePicker final : public Ui::RpWidget {
public:
	AvatarShapePicker(QWidget *parent, int current);

	[[nodiscard]] rpl::producer<int> chosen() const;

protected:
	int resizeGetHeight(int newWidth) override;
	void paintEvent(QPaintEvent *e) override;
	void mouseMoveEvent(QMouseEvent *e) override;
	void mousePressEvent(QMouseEvent *e) override;
	void mouseReleaseEvent(QMouseEvent *e) override;
	void leaveEventHook(QEvent *e) override;

private:
	[[nodiscard]] int count() const;
	[[nodiscard]] int indexAt(QPoint point) const;
	[[nodiscard]] QRect cellRect(int index) const;
	void setOver(int index);

	int _current = 0;
	int _over = -1;
	int _pressed = -1;
	int _columns = 1;
	rpl::event_stream<int> _chosen;

};

} // namespace PixelUi
