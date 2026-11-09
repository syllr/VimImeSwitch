package io.github.syllr.vimimeswitch

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandEvent
import com.intellij.openapi.command.CommandListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.util.messages.MessageBusConnection
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import io.github.syllr.vimimeswitch.mac.MacInputMethodSwitcher
import java.awt.KeyboardFocusManager
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
 * 2. 焦点落进处于普通模式的编辑器。
 *
 * 第 2 条要求该编辑器就是当前选中的编辑器。窗口重新激活时，IDEA/Air 可能先把焦点
 * 短暂交给一个背景编辑器再转回终端，这类事件不代表用户要输入英文。
 */
@Suppress("DEPRECATION")
object InputMethodAutoSwitcher {

    private val LOG = Logger.getInstance("io.github.syllr.vimimeswitch")

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
                LOG.info("switch on command=$commandName")
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
                onEditorFocused(editor, event)
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

    private fun onEditorFocused(editor: Editor, event: FocusEvent) {
        if (!enabled || !VimPlugin.isEnabled()) {
            return
        }
        val mode = CommandState.getInstance(editor).mode
        val project = editor.project
        val documentName = FileDocumentManager.getInstance().getFile(editor.document)?.name
        val selected = project?.let { FileEditorManager.getInstance(it).selectedTextEditor }
        val selectedIsThis = selected === editor
        val showing = editor.contentComponent.isShowing
        val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner

        LOG.info(
            "focus gained cause=${event.cause} doc=$documentName showing=$showing " +
                "selectedIsThis=$selectedIsThis selected=${documentNameOrNull(selected)} mode=$mode " +
                "focusOwner=${focusOwner?.javaClass?.simpleName}"
        )

        if (mode in EDITING_MODES) {
            return
        }
        if (!showing || !selectedIsThis) {
            return
        }
        LOG.info("switch on focus doc=$documentName")
        executor?.execute { switcher.switchToEnglish() }
    }

    private fun documentNameOrNull(editor: Editor?): String? =
        editor?.let { FileDocumentManager.getInstance().getFile(it.document)?.name }

    private const val INPUT_SOURCE_IN_NORMAL = "keep_input_source_in_normal"
}
