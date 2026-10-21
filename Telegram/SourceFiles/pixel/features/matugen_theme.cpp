// This is the source code of pixelGram for Desktop.
// Based on AyuGram Desktop and Telegram Desktop, licensed under GPLv3.
#include "pixel/features/matugen_theme.h"

#include "ayu/ayu_settings.h"
#include "base/platform/base_platform_info.h"
#include "base/timer.h"
#include "window/themes/window_theme.h"

#include <QtCore/QCryptographicHash>
#include <QtCore/QDir>
#include <QtCore/QFile>
#include <QtCore/QFileInfo>
#include <QtCore/QFileSystemWatcher>
#include <QtCore/QStandardPaths>

namespace PixelMatugen {
namespace {

constexpr auto kApplyDelay = crl::time(300);
constexpr auto kMaxPaletteSize = 1024 * 1024;

[[nodiscard]] QString ConfigFolder() {
	return QStandardPaths::writableLocation(
		QStandardPaths::GenericConfigLocation) + u"/pixelgram"_q;
}

class Watcher final {
public:
	Watcher();

private:
	void watch();
	void apply();

	QFileSystemWatcher _watcher;
	base::Timer _timer;
	QByteArray _appliedHash;

};

Watcher::Watcher()
: _timer([=] { apply(); }) {
	QDir().mkpath(ConfigFolder());
	QObject::connect(&_watcher, &QFileSystemWatcher::fileChanged, [=] {
		watch();
		_timer.callOnce(kApplyDelay);
	});
	QObject::connect(&_watcher, &QFileSystemWatcher::directoryChanged, [=] {
		watch();
		_timer.callOnce(kApplyDelay);
	});
	watch();
	_timer.callOnce(kApplyDelay);
}

void Watcher::watch() {
	// WHY: matugen replaces the file, which drops the watch on it.
	const auto folder = ConfigFolder();
	if (!_watcher.directories().contains(folder)) {
		_watcher.addPath(folder);
	}
	const auto path = PalettePath();
	if (QFileInfo::exists(path) && !_watcher.files().contains(path)) {
		_watcher.addPath(path);
	}
}

void Watcher::apply() {
	const auto path = PalettePath();
	auto file = QFile(path);
	if (!file.open(QIODevice::ReadOnly) || file.size() > kMaxPaletteSize) {
		return;
	}
	const auto content = file.readAll();
	if (content.isEmpty()) {
		return;
	}
	const auto hash = QCryptographicHash::hash(
		content,
		QCryptographicHash::Sha1);
	if (hash == _appliedHash) {
		return;
	}
	if (Window::Theme::Apply(path)) {
		Window::Theme::KeepApplied();
		_appliedHash = hash;
	}
}

std::unique_ptr<Watcher> Instance;

void ExportTemplate() {
	QDir().mkpath(ConfigFolder());
	const auto path = TemplatePath();
	auto source = QFile(u":/pixel/matugen/pixelgram.tdesktop-palette"_q);
	if (!source.open(QIODevice::ReadOnly)) {
		return;
	}
	const auto content = source.readAll();
	auto existing = QFile(path);
	if (existing.open(QIODevice::ReadOnly) && existing.readAll() == content) {
		return;
	}
	existing.close();
	auto target = QFile(path);
	if (target.open(QIODevice::WriteOnly | QIODevice::Truncate)) {
		target.write(content);
	}
}

} // namespace

bool Supported() {
	return Platform::IsLinux();
}

QString PalettePath() {
	return ConfigFolder() + u"/matugen.tdesktop-palette"_q;
}

QString TemplatePath() {
	return ConfigFolder() + u"/matugen-template.tdesktop-palette"_q;
}

QString ConfigSnippet() {
	return u"[templates.pixelgram]\n"
		"input_path = '%1'\n"
		"output_path = '%2'"_q.arg(TemplatePath(), PalettePath());
}

void Refresh() {
	const auto enabled = Supported()
		&& AyuSettings::getInstance().matugenTheme();
	if (!enabled) {
		Instance = nullptr;
		return;
	}
	ExportTemplate();
	if (!Instance) {
		Instance = std::make_unique<Watcher>();
	}
}

} // namespace PixelMatugen
