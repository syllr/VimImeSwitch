# VimImeSwitch

IdeaVim 的扩展：从插入模式退回到普通模式时，自动把系统输入法切回英文。仅支持 macOS。

这是 [hadix-lin/ideavim_extension](https://github.com/hadix-lin/ideavim_extension)（Apache-2.0，已停止维护）的延续版本，独立发布，主要差异：

- 修正窗口失去焦点再切回时被误切成英文的问题
- 只保留 macOS 实现，去掉未经验证的 Windows / Linux 分支
- 插件 ID 与包名独立，不再与旧插件冲突
- 构建链迁移至 IntelliJ Platform Gradle Plugin 2.x

## 功能

- 退出插入模式时切换到英文输入法
- 焦点移进普通模式下的编辑器时切换到英文输入法（例如从终端点回代码）
- 可指定普通模式使用的输入源

从别的应用切回 IDE 时不会改变输入法状态：窗口重新获得焦点产生的焦点事件（`FocusEvent.Cause.ACTIVATION`）会被忽略。

## 配置

在编辑器的普通模式下执行以下命令，或写入 `~/.ideavimrc` 后重启 IDE：

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

`set keep-english-in-normal-and-restore-in-insert` 仍然可写，但等价于 `set keep-english-in-normal`，本版本不提供回到插入模式时恢复输入法。

## 要求

- macOS，并已启用 ABC 或 U.S. 英文键盘
- [IdeaVim](https://plugins.jetbrains.com/plugin/164)

## 构建

需要 JDK 21 与 Gradle 9（或使用仓库内的 wrapper）：

```shell
./gradlew buildPlugin
```

产物位于 `build/distributions/`。安装后在 IDE 的插件设置中选择 “Install Plugin from Disk”。

本地调试：

```shell
./gradlew runIde
```

构建脚本默认指向本机已安装的 IDE；换机器时修改 `build.gradle.kts` 里的 `local(...)` 路径。

## 许可

Apache-2.0，见 [LICENSE](LICENSE) 与 [NOTICE](NOTICE)。
