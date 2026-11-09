# VimImeSwitch

IdeaVim 的扩展：在适合输入 vim 命令的时机，自动把 macOS 的系统输入法切回英文。仅支持 macOS。

这是 [hadix-lin/ideavim_extension](https://github.com/hadix-lin/ideavim_extension)（Apache-2.0，已停止维护）的延续版本，独立发布。

## 行为

切换到英文的时机有两处：

1. 退出插入模式（编辑器里按 `Esc` 等）
2. 焦点落进处于普通模式的编辑器

第二条只认“当前选中的编辑器”：窗口重新激活时，IDE 可能先把焦点短暂交给背景编辑器再转回终端，那类事件不触发切换。终端等非编辑器组件不产生焦点事件，因此在 opencode 之类的 TUI 里输入不会被切换。

已知限制：从编辑器切回终端时不会恢复之前的输入法，状态不跟踪。

## 配置

在编辑器的普通模式下执行，或写入 `~/.ideavimrc` 后重启 IDE：

```vim
set keep-english-in-normal
```

| 命令 | 说明 |
|---|---|
| `set keep-english-in-normal` | 开启自动切换 |
| `set nokeep-english-in-normal` | 关闭自动切换 |

可选变量：

| 变量 | 说明 |
|---|---|
| `let keep_input_source_in_normal="com.apple.keylayout.ABC"` | 普通模式切换到的输入源，默认 ABC |

输入源 ID 可以用 [im-select](https://github.com/daipeihust/im-select) 查看，例如 `ABC` 对应 `com.apple.keylayout.ABC`。

## 要求

- macOS，并已启用 ABC 或 U.S. 英文键盘
- [IdeaVim](https://plugins.jetbrains.com/plugin/164)

## 构建

需要 JDK 21 与 Gradle 9，仓库内带 wrapper：

```shell
./gradlew buildPlugin
```

目标平台按以下顺序确定：`-PidePath` 指定的本地 IDE → 常见安装位置 → 下载对应版本平台。指定本地 IDE 可以省掉下载：

```shell
./gradlew buildPlugin -PidePath="$HOME/Applications/IntelliJ IDEA.app"
```

产物位于 `build/distributions/`，在 IDE 的插件设置里用 “Install Plugin from Disk” 安装。

本地调试：

```shell
./gradlew runIde
```

## 许可

Apache-2.0，见 [LICENSE](LICENSE) 与 [NOTICE](NOTICE)。
