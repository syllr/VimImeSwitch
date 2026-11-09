# VimImeSwitch

IdeaVim 的扩展：从插入模式退回到普通模式时，自动把系统输入法切回英文。

这是 [hadix-lin/ideavim_extension](https://github.com/hadix-lin/ideavim_extension)（Apache-2.0，已停止维护）的延续版本，独立发布，主要差异：

- 修正窗口失去焦点再切回时被误切成英文的问题
- 插件 ID 与包名独立，不再与旧插件冲突
- 构建链迁移至 IntelliJ Platform Gradle Plugin 2.x

## 功能

- 退出插入模式时切换到英文输入法
- 可选：回到插入模式时恢复原来的输入法
- 可分别指定普通模式与插入模式使用的输入源（macOS）

## 配置

在编辑器的普通模式下执行以下命令，或写入 `~/.ideavimrc` 后重启 IDE：

```vim
set keep-english-in-normal
```

| 命令 | 说明 |
|---|---|
| `set keep-english-in-normal` | 开启自动切换 |
| `set keep-english-in-normal-and-restore-in-insert` | 回到插入模式时恢复输入法 |
| `set nokeep-english-in-normal-and-restore-in-insert` | 保留自动切换，但不恢复输入法 |
| `set nokeep-english-in-normal` | 关闭自动切换 |

可选变量：

| 变量 | 说明 |
|---|---|
| `let keep_input_source_in_normal="com.apple.keylayout.ABC"` | 普通模式使用的输入源（macOS） |
| `let keep_input_source_in_insert="com.bytedance.inputmethod.doubaoime.pinyin"` | 插入模式恢复的目标输入源（macOS） |
| `let context_aware=1` | 回到插入模式时按光标上下文决定是否恢复 |
| `let rime_ascii=1` | Linux 下使用 fcitx5-rime 的 ASCII 模式 |

输入源 ID 可以用 [im-select](https://github.com/daipeihust/im-select) 查看，例如 `ABC` 对应 `com.apple.keylayout.ABC`。

## 平台

macOS 为主要验证平台。Windows 需要启用美式英文键盘；Linux 需要 fcitx（`fcitx-remote`）或 ibus。其他平台未验证。

## 构建

需要 JDK 21 与 Gradle 9（或使用仓库内的 wrapper）：

```shell
./gradlew buildPlugin
```

产物位于 `build/distributions/`。安装后在 IDE 的插件设置中选择 “Install Plugin from Disk”。

本地调试可以指向已安装的 IDE，避免下载目标平台：

```shell
./gradlew runIde
```

## 许可

Apache-2.0，见 [LICENSE](LICENSE) 与 [NOTICE](NOTICE)。
