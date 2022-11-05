package top.kmar.ai.ui

import com.cnsugar.ai.face.FaceHelper
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.*
import top.kmar.ai.frame
import top.kmar.ai.tasks.CompareFace
import top.kmar.ai.tasks.FileLibManager
import top.kmar.ai.tasks.stream
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.streams.toList

/**
 * 图像库
 * @author EmptyDreams
 */
object ImageLibComponent {

    @OptIn(DelicateCoroutinesApi::class)
    fun initPanel(panel: JPanel) {
        val page = PageComponent().apply {
            size = Dimension(panel.width, panel.height - 100)
        }
        panel.add(page)
        val import = JButton("导入图片").apply {
            size = Dimension(100, 30)
            location = Point(200, panel.height - 50)
            isFocusPainted = false
            font = Font("HarmonyOS Sans SC", Font.BOLD, 16)
            addActionListener {
                GlobalScope.launch(Dispatchers.IO) {
                    val file = CompareFace.chooseFile(panel)
                    FileLibManager.putIn(ImageIO.read(file))
                    page.reset()
                }
            }
        }
        val saved = JButton("保存").apply {
            size = Dimension(70, 30)
            location = Point(340, panel.height - 50)
            isFocusPainted = false
            font = Font("HarmonyOS Sans SC", Font.BOLD, 16)
            addActionListener {
                GlobalScope.launch(Dispatchers.IO) {
                    FileLibManager.saveAll()
                }
            }
        }
        val importAll = JButton("批量导入").apply {
            size = Dimension(100, 30)
            location = Point(440, panel.height - 50)
            isFocusPainted = false
            font = Font("HarmonyOS Sans SC", Font.BOLD, 16)
            addActionListener {
                val file = chooseFiles(panel) ?: return@addActionListener
                val list = file.listFiles() ?: return@addActionListener
                val startTime = System.currentTimeMillis()
                val progress: JProgressBar
                val label = JLabel("", JLabel.CENTER)
                val value = AtomicInteger()
                var jd: JDialog? = JDialog(frame, "正在加载", true).apply {
                    defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
                    size = Dimension(300, 185)
                    layout = null
                    setLocationRelativeTo(null)
                    progress = JProgressBar(0, list.size * 3)
                    progress.size = Dimension(250, 20)
                    progress.location = Point(25, (height - 20) shr 1)
                    add(progress)
                    label.size = Dimension(250, 30)
                    label.location = Point(25, progress.y - 20)
                    label.font = Font("HarmonyOS Sans SC", Font.BOLD, 16)
                    add(label)
                }
                GlobalScope.launch(Dispatchers.IO) {
                    label.text = "读取文件……"
                    val read = list.stream().parallel()
                        .map {
                            progress.value = value.incrementAndGet()
                            ImageIO.read(it)
                        }.toList()
                    label.text = "人脸识别……"
                    read.stream().map {
                            ++progress.value
                            FaceHelper.crop(it)
                        }
                        .forEach {
                            label.text = "数据入库……"
                            ++progress.value
                            FileLibManager.putIn(it)
                        }
                    label.text = "数据保存……"
                    FileLibManager.saveAll()
                    println("加载完毕")
                    page.reset()
                    frame.repaint()
                    jd!!.dispose()
                    jd = null
                }
                jd?.isVisible = true
                val endTime = System.currentTimeMillis()
                println("加载耗时：${endTime - startTime}")
            }
        }
        val search = JButton("搜索").apply {
            size = Dimension(100, 30)
            location = Point(560, panel.height - 50)
            isFocusPainted = false
            font = Font("HarmonyOS Sans SC", Font.BOLD, 16)
            addActionListener { _ ->
                val progress = JProgressBar(0, FileLibManager.size)
                val jd = JDialog(frame, "正在搜索", true).apply {
                    defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
                    size = Dimension(300, 165)
                    setLocationRelativeTo(null)
                    layout = null
                    progress.size = Dimension(250, 20)
                    progress.location = Point(25, (height - 20) shr 1)
                    add(progress)
                }
                GlobalScope.launch(Dispatchers.IO) {
                    val file = CompareFace.chooseFile(panel) ?: return@launch
                    val array = FaceHelper.crop(ImageIO.read(file))
                    var result = false
                    if (array != null)
                        o@for (it in array) {
                            for ((_, image) in FileLibManager.iterator()) {
                                if (FaceHelper.compare(it, image) >= 0.9F) {
                                    result = true
                                    break@o
                                }
                                ++progress.value
                            }
                        }
                    panel.remove(page)
                    val text = if (result) "包含匹配的人脸" else "不包含匹配的人脸"
                    val label = JLabel(text, JLabel.CENTER)
                    label.size = panel.size
                    panel.add(label)
                    panel.repaint()
                    jd.dispose()
                }
                jd.isVisible = true
            }
        }
        panel.add(import)
        panel.add(saved)
        panel.add(importAll)
        panel.add(search)
    }

}

private class PageComponent : JPanel(null) {

    private var itor = FileLibManager.iterator()
    private val list = ObjectArrayList<Array<BufferedImage?>>()
    private var index = 0
    private val left: JButton = JButton("<").apply {
        isFocusPainted = false
        font = Font("HarmonyOS Sans SC", Font.BOLD, 10)
        size = Dimension(40, 50)
        addActionListener {
            if (index != 0) {
                --index
                updateShower()
            }
        }
    }
    private val right: JButton = JButton(">").apply {
        isFocusPainted = false
        font = Font("HarmonyOS Sans SC", Font.BOLD, 10)
        size = Dimension(40, 50)
        addActionListener {
            load()
            if (index != list.lastIndex) {
                ++index
                updateShower()
            }
        }
    }
    private val shower = JPanel(GridLayout(3, 5, 1, 1))

    init {
        load()
        shower.location = Point(50, 0)
        add(left)
        add(right)
        add(shower)
        updateShower()
    }

    @Deprecated("Deprecated in Java")
    override fun resize(width: Int, height: Int) {
        @Suppress("DEPRECATION")
        super.resize(width, height)
        shower.size = Dimension(width - 100, height)
        val mid = (height - left.height) shr 1
        left.location = Point(5, mid)
        right.location = Point(width - right.width - 5, mid)
    }

    fun updateShower() {
        if (index >= list.size) return
        shower.removeAll()
        for (y in 0 until 3) {
            for (x in 0 until 5) {
                val cmpt = ImageComponent().apply {
                    image = list[index][x + y * 5]
                    border = BorderFactory.createBevelBorder(0)
                }
                shower.add(cmpt)
            }
        }
        frame.repaint()
    }

    fun reset() {
        itor = FileLibManager.iterator()
        list.clear()
        load()
        updateShower()
    }

    private fun load() {
        if (!itor.hasNext()) return
        val array = Array<BufferedImage?>(3 * 5) { null }
        for (i in array.indices) {
            if (!itor.hasNext()) break
            array[i] = itor.next().second
        }
        list.add(array)
    }

}

private fun chooseFiles(panel: JPanel): File? {
    val chooser = JFileChooser()
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    val option = chooser.showOpenDialog(panel)
    if (option == JFileChooser.APPROVE_OPTION)
        return chooser.selectedFile
    return null
}