package io.github.syllr.vimimeswitch

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandEvent
import com.intellij.openapi.command.CommandListener
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEventMulticasterEx
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.util.Disposer
import com.intellij.util.messages.MessageBusConnection
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import io.github.syllr.vimimeswitch.mac.MacInputMethodSwitcher
import java.lang.Long.MAX_VALUE
import java.util.EnumSet
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 在两种时机把系统输入法切换到英文：
 *
 * 1. 退出插入模式（编辑器里按 Esc 等）；
 * 2. 焦点落进处于普通模式的编辑器。
 *
 * 规则只有“焦点在编辑器 + 模式是普通模式”这一条，不区分焦点来自点击、Tab 还是窗口重新激活。
 * 终端等非编辑器组件不产生焦点事件，因此在 TUI 里工作不会被切换。
 */
@Suppress("DEPRECATION")
object InputMethodAutoSwitcher {

    private val VIM_INSERT_EXIT_MODE_ACTION = arrayOf("Escape", "Esc", "VimInsertExitModeAction")

    private val EDITING_MODES = EnumSet.of(CommandState.Mode.INSERT, CommandState.Mode.REPLACE)

    @Volatile
    var enabled: Boolean = false
        private set

    private var executor: ThreadPoolExecutor? = null

    private var messageBusConnection: MessageBusConnection? = null

    private var focusListenerDisposable: com.intellij.openapi.Disposable? = null

    /** 首次触发时才读取配置并加载 native 库 */
    private val switcher: MacInputMethodSwitcher by lazy {
        val configured = VimPlugin.getVariableService()
            .getGlobalVariableValue(INPUT_SOURCE_IN_NORMAL)?.toVimString()?.value
        if (configured != null) MacInputMethodSwitcher(configured) else MacInputMethodSwitcher()
    }

    private val exitInsertModeListener = object : CommandListener {
        override fun beforeCommandFinished(commandEvent: CommandEvent) {
            val commandName = commandEvent.commandName ?: return
            if (commandName.isBlank()) {
                return
            }
            if (commandName in VIM_INSERT_EXIT_MODE_ACTION) {
                executor?.execute { switcher.switchToEnglish() }
            }
        }
    }

    private val focusListener = object : FocusChangeListener {
        override fun focusLost(editor: Editor) {
        }

        override fun focusGained(editor: Editor) {
            if (!enabled || !VimPlugin.isEnabled()) {
                return
            }
            if (CommandState.getInstance(editor).mode in EDITING_MODES) {
                return
            }
            executor?.execute { switcher.switchToEnglish() }
        }
    }

    fun enable() {
        if (enabled) {
            return
        }
        if (!System.getProperty("os.name").startsWith("Mac", ignoreCase = true)) {
            return
        }
        enabled = true
        if (executor?.isShutdown != false) {
            executor = ThreadPoolExecutor(
                1, 1,
                MAX_VALUE, TimeUnit.DAYS,
                ArrayBlockingQueue(10),
                { runnable ->
                    val thread = Thread(runnable, "vimimeswitch")
                    thread.isDaemon = true
                    thread.priority = Thread.MAX_PRIORITY
                    thread
                },
                ThreadPoolExecutor.DiscardPolicy()
            )
        }
        messageBusConnection = ApplicationManager.getApplication().messageBus.connect()
        messageBusConnection?.subscribe(CommandListener.TOPIC, exitInsertModeListener)
        registerFocusListener()
    }

    fun disable() {
        if (!enabled) {
            return
        }
        messageBusConnection?.disconnect()
        messageBusConnection = null
        focusListenerDisposable?.let { Disposer.dispose(it) }
        focusListenerDisposable = null
        executor?.shutdown()
        executor = null
        enabled = false
    }

    private fun registerFocusListener() {
        val multicaster = EditorFactory.getInstance().eventMulticaster as? EditorEventMulticasterEx ?: return
        val disposable = Disposer.newDisposable("vimimeswitch-focus")
        multicaster.addFocusChangeListener(focusListener, disposable)
        focusListenerDisposable = disposable
    }

    private const val INPUT_SOURCE_IN_NORMAL = "keep_input_source_in_normal"
}
