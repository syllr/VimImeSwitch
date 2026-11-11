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

## 发布

首次上架只能由网页手动上传，之后才能用 Gradle 自动发版。

### 1. 构建产物

```shell
./gradlew buildPlugin
```

产物：`build/distributions/VimImeSwitch-<version>.zip`。

发布前建议跑一次兼容性校验（使用本机 2026.3 平台，约 8 分钟）：

```shell
./gradlew verifyPlugin
```

报告在 `build/reports/pluginVerifier/`。

### 2. 网页上传首版

1. 用 JetBrains Account 登录 <https://plugins.jetbrains.com/author/me>。
2. 首次上传需接受开发者协议并创建 Vendor Profile。
3. 点 **Add new plugin**，选择 vendor，上传上面的 zip。
4. 表单要点：
   - **License**：Apache-2.0，并填源码地址 `https://github.com/syllr/VimImeSwitch`
   - **Tags**：`Vim`、`Editor`、`Input`
   - **Channel**：留空即正式渠道（想先灰度可以填 `beta`）
5. 提交后等待审核，通过后插件页即可访问，用户能在 IDE 内搜索安装。

上架后在后台 **Technical Information** 里补上 issue tracker 与源码链接。注意目前仓库的 Issues 是关闭状态，需要先开启，否则 issue tracker 链接无处可指。

### 3. 后续版本自动发布

在 <https://plugins.jetbrains.com/author/me/tokens> 生成 Personal Access Token（只显示一次），然后：

```shell
# 先把 build.gradle.kts 里的 version 和 plugin.xml 的 change-notes 更新掉
PUBLISH_TOKEN=perm:xxxxx ./gradlew publishPlugin
```

Marketplace 不接受同一版本号重复上传，每次发版都必须改版本号。

### 4. 插件签名（可选，推荐）

未签名的插件也能上架，只是安装时会提示来源未验证。签名后 IDE 不再告警：

```shell
# chains/private 由 Marketplace 后台 Signing 页生成，或自行用 openssl 生成
export CERTIFICATE_CHAIN="$(base64 -i chain.crt)"
export PRIVATE_KEY="$(base64 -i private.pem)"
export PRIVATE_KEY_PASSWORD='你的密码'
./gradlew signPlugin
```

`publishPlugin` 会在这个配置存在时自动先执行 `signPlugin`。

## 许可

Apache-2.0，见 [LICENSE](LICENSE) 与 [NOTICE](NOTICE)。
