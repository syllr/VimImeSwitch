package io.github.syllr.vimimeswitch

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandEvent
import com.intellij.openapi.command.CommandListener
import com.intellij.util.messages.MessageBusConnection
import com.maddyhome.idea.vim.VimPlugin
import io.github.syllr.vimimeswitch.mac.MacInputMethodSwitcher
import org.apache.commons.lang3.SystemUtils
import java.lang.Long.MAX_VALUE
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 退出插入模式时把系统输入法切换回英文。
 *
 * 触发条件只有一条：IDE 执行了“退出插入模式”这类命令。不使用编辑器焦点事件，
 * 因为焦点恢复并不代表用户想输入英文，会在窗口切回时误切换输入法。
 */
object InputMethodAutoSwitcher {

    private val VIM_INSERT_EXIT_MODE_ACTION = arrayOf("Escape", "Esc", "VimInsertExitModeAction")

    @Volatile
    var enabled: Boolean = false
        private set

    private var executor: ThreadPoolExecutor? = null

    private var messageBusConnection: MessageBusConnection? = null

    /** 首次触发时才读取配置并加载 native 库 */
    private val switcher: MacInputMethodSwitcher by lazy {
        val configured = VimPlugin.getVariableService()
            .getGlobalVariableValue(INPUT_SOURCE_IN_NORMAL)?.asString()
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

    fun enable() {
        if (enabled) {
            return
        }
        if (!SystemUtils.IS_OS_MAC) {
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
    }

    fun disable() {
        if (!enabled) {
            return
        }
        messageBusConnection?.disconnect()
        messageBusConnection = null
        executor?.shutdown()
        executor = null
        enabled = false
    }

    private const val INPUT_SOURCE_IN_NORMAL = "keep_input_source_in_normal"
}
