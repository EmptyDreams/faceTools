package top.kmar.ai.tasks

import com.cnsugar.ai.face.FaceHelper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import top.kmar.ai.ui.ImageComponent
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Point
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileFilter

/**
 * 人像比对
 * @author EmptyDreams
 */
@OptIn(DelicateCoroutinesApi::class)
class CompareFace(panel: JPanel) {

    private var leftFile: File? = null
    private var rightFile: File? = null

    init {
        val width = panel.width.ushr(1) - 100
        val left = buildPanel(panel, 50) { leftFile = it }
        val right = buildPanel(panel, panel.width - width - 50) { rightFile = it }
        JButton("重置").apply {
            size = Dimension(100, 30)
            location = Point(80, panel.height - height - 15)
            isFocusPainted = false
            addActionListener {
                left.image = null
                right.image = null
                leftFile = null
                rightFile = null
                panel.repaint()
            }
            panel.add(this)
        }
        JButton("对比").apply {
            size = Dimension(100, 30)
            location = Point(panel.width - width - 80, panel.height - height - 15)
            isFocusPainted = false
            addActionListener {
                if (leftFile == null || rightFile == null) return@addActionListener
                GlobalScope.launch(Dispatchers.IO) {
                    val result = FaceHelper.compare(leftFile, rightFile)
                    panel.removeAll()
                    val text = when {
                        result < -1.5F -> "右图无人像"
                        result < -0.5F -> "左图无人像"
                        else -> "人脸匹配度：${String.format("%.1f%%", result * 100)}"
                    }
                    val label = JLabel(text, JLabel.CENTER).apply {
                        font = Font("Harmony Sans SC", Font.BOLD, 100)
                        size = Dimension(panel.width, panel.height)
                    }
                    panel.add(label)
                    panel.repaint()
                }
            }
            panel.add(this)
        }
    }

    companion object {

        fun buildPanel(panel: JPanel, x: Int, consumer: (File) -> Unit): ImageComponent {
            val result = ImageComponent()
            val value = JPanel(null).apply {
                val height = panel.height - 100
                val width = panel.width.ushr(1) - 100
                size = Dimension(width, height)
                location = Point(x, 25)
                border = BorderFactory.createBevelBorder(0)
                ImageComponent()
                result.size = Dimension(width - 4, height - 4)
                result.location = Point(2, 2)
                add(buildButton(width, height) {
                    GlobalScope.launch(Dispatchers.IO) {
                        val file = chooseFile(panel) ?: return@launch
                        result.image = ImageIO.read(file)
                        consumer(file)
                        panel.repaint()
                    }
                })
                add(result)
            }
            panel.add(value)
            return result
        }

        private fun buildButton(width: Int, height: Int, action: () -> Unit) =
            JButton("+").apply {
                size = Dimension(width - 50, width - 50)
                location = Point((width - this.width) shr 1, (height - this.height) shr 1)
                isFocusPainted = false
                isOpaque = false
                isContentAreaFilled = false
                border = null
                foreground = Color(128, 128, 128, 100)
                background = Color(0, 0, 0, 0)
                font = Font("Harmony Sans SC", Font.BOLD, 400)
                addActionListener { action() }
            }

        fun chooseFile(panel: JPanel): File? {
            val chooser = JFileChooser()
            chooser.addChoosableFileFilter(ImageFilter)
            chooser.isAcceptAllFileFilterUsed = false
            val option = chooser.showOpenDialog(panel)
            if (option == JFileChooser.APPROVE_OPTION)
                return chooser.selectedFile
            return null
        }

    }

    object ImageFilter : FileFilter() {

        override fun accept(file: File): Boolean {
            if (file.isDirectory) return true
            val extension = file.extension
            return extensionList.binarySearch(extension) >= 0
        }

        override fun getDescription() = "Image Only Filter"

        private val extensionList =
            ImageIO.getReaderFormatNames().apply { sort() }

    }

}