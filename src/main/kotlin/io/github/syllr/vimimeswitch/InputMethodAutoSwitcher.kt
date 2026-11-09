package io.github.syllr.vimimeswitch

import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandEvent
import com.intellij.openapi.command.CommandListener
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEventMulticasterEx
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFrame
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
 * 2. 焦点移进处于普通模式的编辑器，此时用户接下来要敲的是 vim 命令。
 *
 * 第 2 条不处理“窗口重新获得焦点”的场景：那时平台会先发 applicationActivated，
 * 随后的编辑器焦点事件只是焦点恢复，不代表用户要输入英文。
 */
@Suppress("DEPRECATION")
object InputMethodAutoSwitcher {

    private val VIM_INSERT_EXIT_MODE_ACTION = arrayOf("Escape", "Esc", "VimInsertExitModeAction")

    private val EDITING_MODES = EnumSet.of(CommandState.Mode.INSERT, CommandState.Mode.REPLACE)

    /** 窗口激活后的静默窗口，用于忽略随之而来的焦点恢复事件 */
    private const val ACTIVATION_MUTE_NANOS = 300_000_000L

    @Volatile
    var enabled: Boolean = false
        private set

    @Volatile
    private var muteFocusUntil: Long = 0L

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

    private val activationListener = object : ApplicationActivationListener {
        override fun applicationActivated(ideFrame: IdeFrame) {
            muteFocusUntil = System.nanoTime() + ACTIVATION_MUTE_NANOS
        }
    }

    private val focusListener = object : FocusChangeListener {
        override fun focusLost(editor: Editor) {
        }

        override fun focusGained(editor: Editor) {
            if (!enabled || !VimPlugin.isEnabled()) {
                return
            }
            if (System.nanoTime() < muteFocusUntil) {
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
        messageBusConnection?.subscribe(ApplicationActivationListener.TOPIC, activationListener)
        registerFocusChangeListener()
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

    private fun registerFocusChangeListener() {
        val multicaster = EditorFactory.getInstance().eventMulticaster as? EditorEventMulticasterEx ?: return
        val disposable = Disposer.newDisposable("vimimeswitch-focus")
        multicaster.addFocusChangeListener(focusListener, disposable)
        focusListenerDisposable = disposable
    }

    private const val INPUT_SOURCE_IN_NORMAL = "keep_input_source_in_normal"
}
