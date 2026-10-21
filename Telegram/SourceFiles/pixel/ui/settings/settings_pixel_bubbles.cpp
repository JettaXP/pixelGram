// This is the source code of pixelGram for Desktop.
// Based on AyuGram Desktop and Telegram Desktop, licensed under GPLv3.
#include "pixel/ui/settings/settings_pixel_bubbles.h"

#include "lang_auto.h"
#include "ayu/ayu_settings.h"
#include "ayu/ui/components/message_preview.h"
#include "ayu/ui/settings/ayu_builder.h"
#include "ayu/ui/settings/settings_ayu_utils.h"
#include "ayu/ui/settings/settings_main.h"
#include "settings/settings_builder.h"
#include "settings/settings_common.h"
#include "styles/style_menu_icons.h"
#include "styles/style_settings.h"
#include "ui/wrap/vertical_layout.h"
#include "window/window_session_controller.h"

namespace Settings {

using namespace Builder;
using namespace AyuBuilder;

namespace {

constexpr auto kBubbleRadiusSteps = 17;

void BuildBubbles(SectionBuilder &builder, AyuSectionBuilder &ayu) {
	auto *settings = &AyuSettings::getInstance();
	const auto controller = builder.controller();

	builder.addSkip();
	builder.addSubsectionTitle({
		.id = u"pixel/bubbleRadius"_q,
		.title = tr::pixel_BubbleRadiusHeader(),
		.keywords = { u"bubble"_q, u"radius"_q, u"corner"_q },
	});

	auto *previewRaw = static_cast<MessagePreview*>(nullptr);
	builder.add([&](const WidgetContext &ctx) -> SectionBuilder::WidgetToAdd {
		auto preview = object_ptr<MessagePreview>(ctx.container, controller);
		previewRaw = preview.data();
		return {
			.widget = std::move(preview),
			.margin = style::margins(
				0,
				st::defaultVerticalListSkip,
				0,
				st::settingsPrivacySkipTop),
		};
	});

	ayu.addSlider({
		.id = u"ayu/messageBubbleRadius"_q,
		.title = tr::ayu_MessageBubbleRadius(),
		.steps = kBubbleRadiusSteps,
		.current = settings->messageBubbleRadius(),
		.indexToValue = [](int index) { return index; },
		.onChanged = [=](int index) {
			if (previewRaw) {
				previewRaw->setBubbleRadius(index);
			}
		},
		.onFinalChanged = [=](int index) {
			if (previewRaw) {
				previewRaw->setBubbleRadius(index);
			}
			AyuSettings::getInstance().setMessageBubbleRadius(index);
			ShowRestartPrompt(controller);
		},
		.formatLabel = [](int index) {
			return QString::number(index);
		},
	});

	ayu.addSettingToggle({
		.id = u"ayu/removeMessageTail"_q,
		.title = tr::ayu_RemoveMessageTail(),
		.getter = &AyuSettings::removeMessageTail,
		.setter = &AyuSettings::setRemoveMessageTail,
	});

	builder.addSkip();
	builder.addDividerText(tr::pixel_BubbleRadiusDescription());
	builder.addSkip();
}

const auto kMeta = BuildHelper({
	.id = PixelBubbles::Id(),
	.parentId = AyuMain::Id(),
	.title = &tr::pixel_CategoryBubbles,
	.icon = &st::menuIconReply,
}, [](SectionBuilder &builder) {
	auto ayu = AyuSectionBuilder(builder);
	BuildBubbles(builder, ayu);
});

} // namespace

rpl::producer<QString> PixelBubbles::title() {
	return tr::pixel_CategoryBubbles();
}

PixelBubbles::PixelBubbles(
	QWidget *parent,
	not_null<Window::SessionController*> controller)
: Section(parent, controller) {
	setupContent();
}

void PixelBubbles::setupContent() {
	const auto content = Ui::CreateChild<Ui::VerticalLayout>(this);
	build(content, kMeta.build);
	Ui::ResizeFitChild(this, content);
}

} // namespace Settings
