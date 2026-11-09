package io.github.syllr.vimimeswitch.mac

import com.sun.jna.Native

/** Created by hadix on 30/03/2017.  */
object MacNative {

	init {
		Native.register("input-source-switcher")
	}

	external fun getCurrentInputSourceID(): String

	external fun switchInputSource(inputSourceID: String): Int
}