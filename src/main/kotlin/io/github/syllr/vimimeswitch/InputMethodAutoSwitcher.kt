package io.github.syllr.vimimeswitch

import com.intellij.openapi.command.CommandEvent
import com.intellij.openapi.command.CommandListener
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.util.messages.MessageBusConnection
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import io.github.syllr.vimimeswitch.mac.MacInputMethodSwitcher
import java.awt.event.FocusEvent
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
 * 第 2 条不看“窗口是否刚激活”，而是看焦点事件本身的成因：
 * 窗口重新获得焦点时，AWT 恢复组件焦点用的就是 [FocusEvent.Cause.ACTIVATION]
 * （见 DefaultKeyboardFocusManager#processWindowFocusEvent），这类事件直接忽略；
 * 用户点击、Tab 切换等成因的事件才视为“用户主动进入编辑器”。
 */
@Suppress("DEPRECATION")
object InputMethodAutoSwitcher {

    private val VIM_INSERT_EXIT_MODE_ACTION = arrayOf("Escape", "Esc", "VimInsertExitModeAction")

    private val EDITING_MODES = EnumSet.of(CommandState.Mode.INSERT, CommandState.Mode.REPLACE)

    private val FOCUS_LISTENER_KEY = Key.create<java.awt.event.FocusListener>("vimimeswitch.focus")

    @Volatile
    var enabled: Boolean = false
        private set

    private var executor: ThreadPoolExecutor? = null

    private var messageBusConnection: MessageBusConnection? = null

    private var editorFactoryDisposable: com.intellij.openapi.Disposable? = null

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

    private val editorFactoryListener = object : EditorFactoryListener {
        override fun editorCreated(event: EditorFactoryEvent) {
            attach(event.editor)
        }

        override fun editorReleased(event: EditorFactoryEvent) {
            detach(event.editor)
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

        val factory = EditorFactory.getInstance()
        factory.allEditors.forEach { attach(it) }
        val disposable = Disposer.newDisposable("vimimeswitch-editors")
        factory.addEditorFactoryListener(editorFactoryListener, disposable)
        editorFactoryDisposable = disposable
    }

    fun disable() {
        if (!enabled) {
            return
        }
        messageBusConnection?.disconnect()
        messageBusConnection = null
        editorFactoryDisposable?.let { Disposer.dispose(it) }
        editorFactoryDisposable = null
        EditorFactory.getInstance().allEditors.forEach { detach(it) }
        executor?.shutdown()
        executor = null
        enabled = false
    }

    private fun attach(editor: Editor) {
        if (editor.getUserData(FOCUS_LISTENER_KEY) != null) {
            return
        }
        val listener = object : java.awt.event.FocusListener {
            override fun focusLost(event: FocusEvent) {
            }

            override fun focusGained(event: FocusEvent) {
                if (event.cause == FocusEvent.Cause.ACTIVATION) {
                    return
                }
                onEditorFocused(editor)
            }
        }
        editor.putUserData(FOCUS_LISTENER_KEY, listener)
        editor.contentComponent.addFocusListener(listener)
    }

    private fun detach(editor: Editor) {
        val listener = editor.getUserData(FOCUS_LISTENER_KEY) ?: return
        editor.contentComponent.removeFocusListener(listener)
        editor.putUserData(FOCUS_LISTENER_KEY, null)
    }

    private fun onEditorFocused(editor: Editor) {
        if (!enabled || !VimPlugin.isEnabled()) {
            return
        }
        if (CommandState.getInstance(editor).mode in EDITING_MODES) {
            return
        }
        executor?.execute { switcher.switchToEnglish() }
    }

    private const val INPUT_SOURCE_IN_NORMAL = "keep_input_source_in_normal"
}
