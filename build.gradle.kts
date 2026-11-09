plugins {
	kotlin("jvm") version "2.4.0"
	id("org.jetbrains.intellij.platform") version "2.20.0"
}

group = "io.github.syllr"
version = "0.0.1"

repositories {
	mavenCentral()
	intellijPlatform {
		defaultRepositories()
	}
}

dependencies {
	intellijPlatform {
		local("/Users/yutao/Applications/IntelliJ IDEA.app")
		bundledPlugin("com.intellij.modules.jna")
		plugin("IdeaVIM", "2.48.1")
	}
}

intellijPlatform {
	pluginConfiguration {
		id = "io.github.syllr.vimimeswitch"
		name = "VimImeSwitch"
		version = "0.0.1"
		ideaVersion {
			sinceBuild = "263"
		}
	}
}

kotlin {
	jvmToolchain(21)
}
