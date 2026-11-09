package io.github.syllr.vimimeswitch

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.setToggleOption
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.ToggleOption

/**
 * 兼容旧配置：`set keep-english-in-normal-and-restore-in-insert` 仍然可用，
 * 但等价于 `set keep-english-in-normal`，本版本不提供回到插入模式时恢复输入法。
 */
class KeepEnglishInNormalAndRestoreInInsertExtension : VimExtension {

	override fun getName(): String {
		return NAME
	}

	override fun init() {
		val optionGroup = VimPlugin.getOptionGroup()
		val option = optionGroup.getOption(KeepEnglishInNormalExtension.NAME)
			?: throw exExceptionMessage("option not found")
		@Suppress("UNCHECKED_CAST")
		optionGroup.setToggleOption(option as ToggleOption, OptionAccessScope.GLOBAL(null))
	}

	companion object {
		private const val NAME = "keep-english-in-normal-and-restore-in-insert"
	}
}
