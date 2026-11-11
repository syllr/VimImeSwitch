import java.io.File

plugins {
	kotlin("jvm") version "2.4.0"
	id("org.jetbrains.intellij.platform") version "2.20.0"
}

group = "io.github.syllr"
version = "0.0.2"

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
		version = "0.0.2"
		ideaVersion {
			// 仅 2026.3 及以上；不设 until-build，后续版本默认继续兼容
			sinceBuild = "263"
		}
	}

	// 版本更新时用 ./gradlew publishPlugin 自动发布；token 从环境变量读取，避免写进仓库
	publishing {
		token = providers.environmentVariable("PUBLISH_TOKEN")
		channels = listOf("default")
	}

	// signPlugin 无需在此声明：未显式配置时，插件默认读取
	// CERTIFICATE_CHAIN / PRIVATE_KEY / PRIVATE_KEY_PASSWORD 三个环境变量。

	// 默认 recommended() 会下载多个 IDE；这里只校验当前目标平台，避免重复下载
	pluginVerification {
		ides {
			current()
		}
	}
}

kotlin {
	jvmToolchain(21)
}
