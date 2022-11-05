package top.kmar.ai

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import top.kmar.ai.tasks.CompareFace
import top.kmar.ai.tasks.CropFace
import top.kmar.ai.tasks.FileLibManager
import top.kmar.ai.ui.ImageLibComponent
import java.awt.Dimension
import java.awt.Font
import java.awt.Point
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel

val frame = JFrame("人脸工具集").apply {
    size = Dimension(1000, 660)
    defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    setLocationRelativeTo(null)
    layout = null
    isResizable = false
    background
    isVisible = true
}

@OptIn(DelicateCoroutinesApi::class)
fun main() {
    val panel = JPanel(null).apply {
        location = Point(5, 55)
        size = Dimension(frame.rootPane.width - 10, frame.rootPane.height - 70)
        frame.add(this)
    }
    buildButton("人脸对比").apply {
        location = Point(10, 10)
        frame.add(this)
        addActionListener {
            panel.removeAll()
            CompareFace(panel)
            panel.repaint()
        }
    }
    buildButton("人脸提取").apply {
        location = Point(130, 10)
        frame.add(this)
        addActionListener {
            panel.removeAll()
            CropFace(panel)
            panel.repaint()
        }
    }
    buildButton("人脸图库").apply {
        location = Point(250, 10)
        frame.add(this)
        addActionListener {
            panel.removeAll()
            ImageLibComponent.initPanel(panel)
            panel.repaint()
        }
    }
    GlobalScope.launch(Dispatchers.IO) {
        FileLibManager.iterator()
    }
}

private fun buildButton(text: String) = JButton(text).apply {
    size = Dimension(100, 35)
    font = Font("HarmonyOS Sans SC", Font.BOLD, 16)
    isFocusPainted = false
}