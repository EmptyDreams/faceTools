package top.kmar.ai.tasks

import com.cnsugar.ai.face.FaceHelper
import it.unimi.dsi.fastutil.objects.Object2ObjectFunction
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.stream.Stream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

/**
 * 文件库管理器
 * @author EmptyDreams
 */
object FileLibManager : MutableIterable<Pair<String, BufferedImage>> {

    const val maxCount = 1000

    private val root = File("imageLib")

    val size: Int
        get() {
            var result = 0
            content.forEach { (_, list) -> result += list.size }
            return result
        }

    private val content: MutableMap<String, MutableList<BufferedImage>> by lazy {
        val startTime = System.currentTimeMillis()
        val result = Object2ObjectOpenHashMap<String, MutableList<BufferedImage>>()
        if (!root.exists()) return@lazy result
        val list = root.listFiles() ?: return@lazy result
        list.stream().parallel()
            .filter { it.extension == "zip" }
            .forEach { file ->
                ZipFile(file).use { zip ->
                    zip.stream().forEach {  entry ->
                        zip.getInputStream(entry).use {
                            result.computeIfAbsent(
                                file.nameWithoutExtension,
                                Object2ObjectFunction{ ObjectArrayList(maxCount) }
                            ).add(ImageIO.read(it))
                        }
                    }
                }
            }
        val endTime = System.currentTimeMillis()
        println("lib 加载耗时：${endTime - startTime}")
        result
    }

    /** 标记哪些文件被更新了 */
    private val updateMark = ObjectRBTreeSet<String>()

    /** 放入一个人像 */
    fun putIn(image: BufferedImage) {
        putIn(FaceHelper.crop(image))
    }

    /** 放入裁剪好的图像 */
    fun putIn(array: Array<BufferedImage>?) {
        if (array == null) return
        var index = array.size
        content.forEach { (key, list) ->
            while (list.size < maxCount) {
                updateMark += key
                list.add(array[--index])
                if (index == 0) return
            }
        }
        val key = System.currentTimeMillis().toString(16)
        updateMark += key
        content[key] = ObjectArrayList<BufferedImage>(maxCount).apply {
            for (i in 0 until index) {
                add(array[i])
            }
        }
    }

    /** 保存所有文件 */
    fun saveAll() {
        val startTime = System.currentTimeMillis()
        updateMark.stream().parallel()
            .forEach { key ->
                val file = File(root, "${key}.zip")
                if (!file.exists()) file.createNewFile()
                ZipOutputStream(FileOutputStream(file)).use { output ->
                    content[key]!!.stream().forEach {
                        output.putNextEntry(ZipEntry(it.hashCode().toString(16)))
                        ImageIO.write(it, "jpg", output)
                        output.closeEntry()
                    }
                }
            }
        val endTime = System.currentTimeMillis()
        println("lib 存储耗时：${endTime - startTime}")
    }

    /** 移除一个文件 */
    fun remove(key: String, image: BufferedImage) {
        content[key]?.let {
            it.remove(image)
            updateMark += key
        }
    }

    override fun iterator(): MutableIterator<Pair<String, BufferedImage>> =
        object : MutableIterator<Pair<String, BufferedImage>> {

            private val itor = content.iterator()
            private var key: String? = null
            private var sonItor: MutableIterator<BufferedImage>? = null

            override fun hasNext(): Boolean {
                if (sonItor == null) {
                    if (!itor.hasNext()) return false
                    val (key, list) = itor.next()
                    this.key = key
                    sonItor = list.iterator()
                }
                while (!sonItor!!.hasNext()) {
                    if (!itor.hasNext()) return false
                    val (key, list) = itor.next()
                    this.key = key
                    sonItor = list.iterator()
                }
                return true
            }

            override fun next(): Pair<String, BufferedImage> {
                if (sonItor == null) hasNext()
                return Pair(key!!, sonItor!!.next())
            }

            override fun remove() {
                sonItor!!.remove()
            }

        }


}

fun <T> Array<T>.stream(): Stream<T> = Arrays.stream(this)