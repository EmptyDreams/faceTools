package top.kmar.ai.tasks

import com.cnsugar.ai.face.FaceHelper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import top.kmar.ai.ui.ImageCarouselComponent
import java.awt.Dimension
import java.awt.Font
import java.awt.Point
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JButton
import javax.swing.JPanel

/**
 * 人脸提取
 * @author EmptyDreams
 */
@OptIn(DelicateCoroutinesApi::class)
class CropFace(panel: JPanel) {

    private var selectFile: File? = null

    init {
        val width = panel.width.ushr(1) - 100
        CompareFace.buildPanel(panel, (panel.width - width) shr 1) { selectFile = it }
        JButton("提取").apply {
            size = Dimension(100, 30)
            location = Point((panel.width - 100) shr 1, panel.height - 40)
            font = Font("Harmony Sans SC", Font.BOLD, 16)
            addActionListener {
                val file = selectFile ?: return@addActionListener
                panel.removeAll()
                GlobalScope.launch(Dispatchers.IO) {
                    val result = FaceHelper.crop(ImageIO.read(file))
                    val carousel = ImageCarouselComponent(result)
                    carousel.size = Dimension(panel.width, panel.height)
                    carousel.updateImage()
                    panel.add(carousel)
                    panel.repaint()
                }
            }
            panel.add(this)
        }
    }

}