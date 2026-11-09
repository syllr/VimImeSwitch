plugins {
	kotlin("jvm") version "2.2.20"
	id("org.jetbrains.intellij.platform") version "2.20.0"
}

group = "io.github.syllr"
version = "1.0.0"

repositories {
	mavenCentral()
	intellijPlatform {
		defaultRepositories()
	}
}

dependencies {
	intellijPlatform {
		local("/Users/yutao/Applications/IntelliJ IDEA.app")
		plugin("IdeaVIM", "2.48.1")
	}
}

intellijPlatform {
	pluginConfiguration {
		id = "io.github.syllr.vimimeswitch"
		name = "VimImeSwitch"
		version = "1.0.0"
		ideaVersion {
			sinceBuild = "263"
		}
	}
}

kotlin {
	jvmToolchain(21)
}
