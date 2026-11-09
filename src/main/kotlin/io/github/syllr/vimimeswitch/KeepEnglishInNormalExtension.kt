package io.github.syllr.vimimeswitch

import com.maddyhome.idea.vim.extension.VimExtension

/**
 * `set keep-english-in-normal` 对应的 IdeaVim 扩展：开启时注册输入法切换。
 */
class KeepEnglishInNormalExtension : VimExtension {

	override fun getName(): String {
		return NAME
	}

	override fun init() {
		InputMethodAutoSwitcher.enable()
	}

	override fun dispose() {
		InputMethodAutoSwitcher.disable()
	}

	companion object {
		const val NAME = "keep-english-in-normal"
	}
}
