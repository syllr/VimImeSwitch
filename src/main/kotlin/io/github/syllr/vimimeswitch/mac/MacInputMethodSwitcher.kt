package io.github.syllr.vimimeswitch.mac

/**
 * 通过 libinput-source-switcher 切换 macOS 输入源。
 *
 * 目标输入源在第一次使用时确定，之后每次只做一次 native 调用。
 */
class MacInputMethodSwitcher(private val english: String = KEY_LAYOUT_ABC) {

    fun switchToEnglish() {
        MacNative.switchInputSource(english)
    }

    companion object {
        const val KEY_LAYOUT_ABC = "com.apple.keylayout.ABC"
    }
}
