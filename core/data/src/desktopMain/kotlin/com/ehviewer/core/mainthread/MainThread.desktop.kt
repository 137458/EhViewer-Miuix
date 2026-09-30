package com.ehviewer.core.mainthread

import java.awt.EventQueue

// Compose Desktop 的主线程即 AWT 事件分发线程
internal actual val isMainThread: Boolean
    get() = EventQueue.isDispatchThread()
