package io.github.syllr.vimimeswitch.mac

import com.sun.jna.Native

/**
 * libinput-source-switcher 的 JNA 直接绑定。
 *
 * 库由 JNA 从 classpath 的 darwin / darwin-aarch64 目录按平台自动解压加载。
 */
object MacNative {

	init {
		Native.register("input-source-switcher")
	}

	external fun getCurrentInputSourceID(): String

	/** 返回 OSStatus，0 表示成功，负数表示失败。 */
	external fun switchInputSource(inputSourceID: String): Int
}
