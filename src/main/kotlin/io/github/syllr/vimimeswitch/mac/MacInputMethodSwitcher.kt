package io.github.syllr.vimimeswitch.mac

/**
 * 通过 libinput-source-switcher 切换 macOS 输入源。
 *
 * 目标输入源在创建时确定，已经是目标输入源时不再触发系统切换。
 */
class MacInputMethodSwitcher(private val english: String = KEY_LAYOUT_ABC) {

    fun switchToEnglish() {
        if (MacNative.getCurrentInputSourceID() == english) {
            return
        }
        MacNative.switchInputSource(english)
    }

    companion object {
        const val KEY_LAYOUT_ABC = "com.apple.keylayout.ABC"
    }
}
