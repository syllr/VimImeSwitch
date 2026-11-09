import java.io.File

plugins {
	kotlin("jvm") version "2.4.0"
	id("org.jetbrains.intellij.platform") version "2.20.0"
}

group = "io.github.syllr"
version = "0.0.3"

repositories {
	mavenCentral()
	intellijPlatform {
		defaultRepositories()
	}
}

/**
 * 目标平台：优先用 -PidePath 指定的本地 IDE，其次找常见安装位置，
 * 都没有才下载对应版本的平台。
 */
val localIde: File? = sequenceOf(
	providers.gradleProperty("idePath").orNull,
	"/Applications/IntelliJ IDEA.app",
	System.getProperty("user.home") + "/Applications/IntelliJ IDEA.app",
).filterNotNull().map(::File).firstOrNull { it.exists() }

dependencies {
	intellijPlatform {
		if (localIde != null) local(localIde) else intellijIdeaUltimate("2026.3")
		bundledPlugin("com.intellij.modules.jna")
		plugin("IdeaVIM", "2.48.1")
	}
}

intellijPlatform {
	pluginConfiguration {
		id = "io.github.syllr.vimimeswitch"
		name = "VimImeSwitch"
		version = "0.0.3"
		ideaVersion {
			sinceBuild = "263"
		}
	}
}

kotlin {
	jvmToolchain(21)
}
