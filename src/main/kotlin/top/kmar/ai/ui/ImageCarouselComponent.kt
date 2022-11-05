package top.kmar.ai.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import top.kmar.ai.tasks.CompareFace
import java.awt.Dimension
import java.awt.Font
import java.awt.Point
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * 图片轮播控件
 * @author EmptyDreams
 */
class ImageCarouselComponent(var array: Array<BufferedImage>) : JPanel(null) {

    private var index = 0
    private val shower: ImageComponent
    private val right: JButton
    private val left: JButton
    private val download: JButton
    private val page = JLabel("", JLabel.CENTER)

    init {
        left = JButton("<").apply {
            isFocusPainted = false
            font = Font("Harmony Sans SC", Font.BOLD, 10)
            size = Dimension(40, 50)
            addActionListener {
                if (index == 0) index = array.lastIndex
                else --index
                updateImage()
            }
        }
        right = JButton(">").apply {
            isFocusPainted = false
            font = Font("Harmony Sans SC", Font.BOLD, 10)
            size = Dimension(40, 50)
            addActionListener {
                if (index == array.lastIndex) index = 0
                else ++index
                updateImage()
            }
        }
        download = JButton("保存").apply {
            isFocusPainted = false
            font = Font("Harmony Sans SC", Font.BOLD, 16)
            size = Dimension(100, 30)
            addActionListener { save() }
        }
        shower = ImageComponent()
        page.size = Dimension(100, 30)
        add(left)
        add(shower)
        add(right)
        add(download)
        add(page)
    }

    override fun setSize(d: Dimension) {
        super.setSize(d)
        shower.size = Dimension(d.width - 90, d.height - 40)
        shower.location = Point((d.width - shower.width) shr 1, 0)
        val y = (d.height - left.height) shr 1
        right.location = Point(d.width - 45, y)
        left.location = Point(5, y)
        download.location = Point(((d.width - download.width) shr 1) + 100, d.height - 30)
        page.location = Point(((d.width - download.width) shr 1) - 100, d.height - 30)
    }

    fun updateImage() {
        page.text = "${index + 1} / ${array.size}"
        shower.image = array[index]
        repaint()
    }

    private fun save() {
        @Suppress("OPT_IN_USAGE")
        GlobalScope.launch(Dispatchers.IO) {
            val chooser = JFileChooser()
            chooser.addChoosableFileFilter(CompareFace.ImageFilter)
            val option = chooser.showSaveDialog(this@ImageCarouselComponent)
            if (option == JFileChooser.APPROVE_OPTION) {
                var file = chooser.selectedFile
                if (file.extension != "jpg") file = File("${file.absolutePath}.jpg")
                ImageIO.write(array[index], "jpg", file)
            }
        }
    }

}