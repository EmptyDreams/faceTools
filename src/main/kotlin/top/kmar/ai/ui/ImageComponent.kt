package top.kmar.ai.ui

import java.awt.Graphics
import java.awt.Image
import java.awt.image.BufferedImage
import java.lang.Float.min
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JPanel

/**
 * 绘制图像
 * @author EmptyDreams
 */
class ImageComponent : JPanel(null) {

    private val _image = AtomicReference<Image>()

    var image: Image?
        get() = _image.get()
        set(value) {
            _image.set(value)
        }

    init {
        isOpaque = false
    }

    override fun paint(g: Graphics) {
        image?.let {
            if (it.width == 0 || it.height == 0) return
            val arg0 = width.toFloat() / it.width
            val arg1 = height.toFloat() / it.height
            val scale = min(arg0, arg1)
            it.getScaledInstance(
                (it.width * scale).toInt(),
                (it.height * scale).toInt(),
                BufferedImage.SCALE_SMOOTH
            )
            val x = (width - it.width) shr 1
            val y = (height - it.height) shr 1
            g.drawImage(it, x, y, null)
        }
    }

}

val Image.width: Int
    get() = getWidth(null)

val Image.height: Int
    get() = getHeight(null)