package ppandroid.changebg

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.ThreadUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import com.luck.picture.lib.utils.ToastUtils
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Calendar


class MainActivity : AppCompatActivity() {
    fun getRealPathFromUri(context: Context, uri: Uri): String? {
        var realPath: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex: Int = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                realPath = it.getString(columnIndex)
            }
        }
        cursor?.close()
        return realPath
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        OpenCVLoader.initLocal()
        var sourceImage = findViewById<ImageView>(R.id.imageView)


        findViewById<Button>(R.id.btn_select).setOnClickListener {
            PictureSelector.create(this)
                .openGallery(SelectMimeType.ofImage())
                .setMaxSelectNum(1)
                .setImageEngine(GlideEngine.createGlideEngine())
                .forResult(object : OnResultCallbackListener<LocalMedia?> {
                    override fun onResult(result: ArrayList<LocalMedia?>) {
                        // 假设只选择了一张图片
                        if (result.isNotEmpty()) {
                            var avPath = result[0]!!.availablePath
                            var uri = Uri.parse(avPath)
                            avPath = getRealPathFromUri(this@MainActivity, uri)
                            Glide.with(this@MainActivity).load(avPath).into(sourceImage)
                            ToastUtils.showToast(this@MainActivity, avPath)
                            path=avPath
                            // change(path = avPath)
                        }

                    }

                    override fun onCancel() {

                    }
                })

        }

        findViewById<Button>(R.id.btn_w_r).setOnClickListener {
            ThreadUtils.executeBySingle(object :ThreadUtils.SimpleTask<String>(){
                override fun doInBackground(): String {
                    return changeWhiteToRed()
                }

                override fun onSuccess(result: String?) {
                    extracted(result)

                }

            })

        }

        findViewById<Button>(R.id.btn_w_b).setOnClickListener {
            ThreadUtils.executeBySingle(object :ThreadUtils.SimpleTask<String>(){
                override fun doInBackground(): String {
                    return changeWhiteToBlue()
                }

                override fun onSuccess(result: String?) {

                    extracted(result)
                }

            })

        }

        findViewById<Button>(R.id.btn_b_r).setOnClickListener {
            ThreadUtils.executeBySingle(object :ThreadUtils.SimpleTask<String>(){
                override fun doInBackground(): String {
                    return changeBlueToRed()
                }

                override fun onSuccess(result: String?) {

                    extracted(result)
                }

            })

        }

        findViewById<Button>(R.id.btn_b_w).setOnClickListener {
            ThreadUtils.executeBySingle(object :ThreadUtils.SimpleTask<String>(){
                override fun doInBackground(): String {
                    return changeBlueToWhite()
                }

                override fun onSuccess(result: String?) {

                    extracted(result)
                }

            })

        }

        findViewById<Button>(R.id.btn_r_b).setOnClickListener {
            ThreadUtils.executeBySingle(object :ThreadUtils.SimpleTask<String>(){
                override fun doInBackground(): String {
                    return changeRedToBlue()
                }

                override fun onSuccess(result: String?) {

                    extracted(result)
                }

            })

        }

        findViewById<Button>(R.id.btn_r_w).setOnClickListener {
            ThreadUtils.executeBySingle(object :ThreadUtils.SimpleTask<String>(){
                override fun doInBackground(): String {
                    return changeRedToWhite()
                }

                override fun onSuccess(result: String?) {

                    extracted(result)
                }

            })

        }
        findViewById<Button>(R.id.btn_save).setOnClickListener {
            saveImageToGallery(resultPath,"改背景"+Calendar.getInstance().timeInMillis,"改背景"+Calendar.getInstance().timeInMillis)

        }

    }
    fun saveImageToGallery(imagePath: String, title: String, description: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, title)
            put(MediaStore.Images.Media.DESCRIPTION, description)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        }

        // 使用ContentResolver将图片插入到相册
        val contentResolver = contentResolver
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let { imageUri ->
            try {
                contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                    FileInputStream(imagePath).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Toast.makeText(this, "图片保存成功", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "保存图片失败", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun changeRedToWhite(): String {
        // 读取图像
        val image = Imgcodecs.imread(path)

        // 图像缩放
        val img = Mat()
        Imgproc.resize(image, img, Size(), 1.0, 1.0)

        // 转换为HSV色彩空间
        val hsv = Mat()
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_BGR2HSV)

        // 图像二值化处理
        val mask = Mat()
        Core.inRange(hsv, Scalar(0.0, 135.0, 135.0), Scalar(180.0, 245.0, 230.0), mask) // 红色的HSV范围

        // 遍历每个像素点，将红色像素替换为白色
        for (i in 0 until img.rows()) {
            for (j in 0 until img.cols()) {
                val pixel = mask[i, j]
                if (pixel[0] == 255.0) { // 红色像素
                    img.put(i, j, 255.0, 255.0, 255.0) // 将红色像素替换成白色
                }
            }
        }

        // 保存图像
        val savePath = filesDir.absolutePath + File.separator + "output_white.jpg"
        Imgcodecs.imwrite(savePath, img)

        return savePath
    }


    fun changeRedToBlue(): String {
        // 读取图像
        val image = Imgcodecs.imread(path)

        // 图像缩放
        val img = Mat()
        Imgproc.resize(image, img, Size(), 1.0, 1.0)

        // 转换为HSV色彩空间
        val hsv = Mat()
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_BGR2HSV)

        // 图像二值化处理
        val mask = Mat()
        Core.inRange(hsv, Scalar(0.0, 135.0, 135.0), Scalar(180.0, 245.0, 230.0), mask) // 红色的HSV范围

        // 遍历每个像素点，将红色像素替换为蓝色
        for (i in 0 until img.rows()) {
            for (j in 0 until img.cols()) {
                val pixel = mask[i, j]
                if (pixel[0] == 255.0) { // 红色像素
                    img.put(i, j, 255.0, 0.0, 0.0) // 将红色像素替换成蓝色
                }
            }
        }

        // 保存图像
        val savePath = filesDir.absolutePath + File.separator + "output_blue.jpg"
        Imgcodecs.imwrite(savePath, img)

        return savePath
    }



    fun changeBlueToWhite(): String {
        // 读取图像
        val image = Imgcodecs.imread(path)

        // 图像缩放
        val img = Mat()
        Imgproc.resize(image, img, Size(), 1.0, 1.0)

        // 转换为HSV色彩空间
        val hsv = Mat()
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_BGR2HSV)

        // 图像二值化处理
        val mask = Mat()
        Core.inRange(hsv, Scalar(90.0, 50.0, 50.0), Scalar(120.0, 255.0, 255.0), mask) // 蓝色的HSV范围

        // 遍历每个像素点，将蓝色像素替换为白色
        for (i in 0 until img.rows()) {
            for (j in 0 until img.cols()) {
                val pixel = mask[i, j]
                if (pixel[0] == 255.0) { // 蓝色像素
                    img.put(i, j, 255.0, 255.0, 255.0) // 将蓝色像素替换成白色
                }
            }
        }

        // 保存图像
        val savePath = filesDir.absolutePath + File.separator + "output_white.jpg"
        Imgcodecs.imwrite(savePath, img)

        return savePath
    }

    private var resultPath=""


    private fun extracted(result: String?) {
        resultPath=result.toString()
        var resultImage = findViewById<ImageView>(R.id.imageView2)
        Glide.with(this@MainActivity).load(result)
            .skipMemoryCache(true) // 禁用内存缓存
            .diskCacheStrategy(DiskCacheStrategy.NONE) // 禁用磁盘缓存
            .into(resultImage)
    }

    fun changeBlueToRed(): String {
        // 读取图像
        val image = Imgcodecs.imread(path)

        // 图像缩放
        val img = Mat()
        Imgproc.resize(image, img, Size(), 1.0, 1.0)

        // 转换为HSV色彩空间
        val hsv = Mat()
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_BGR2HSV)

        // 图像二值化处理
        val mask = Mat()
        Core.inRange(hsv, Scalar(90.0, 50.0, 50.0), Scalar(120.0, 255.0, 255.0), mask) // 蓝色的HSV范围

        // 遍历每个像素点，进行颜色替换
        for (i in 0 until img.rows()) {
            for (j in 0 until img.cols()) {
                val pixel = mask[i, j]
                if (pixel[0] == 255.0) { // 蓝色像素
                    img.put(i, j, 0.0, 0.0, 255.0) // 将蓝色像素替换成红色
                }
            }
        }

        // 保存图像
        val savePath = filesDir.absolutePath + File.separator + "output_red.jpg"
        Imgcodecs.imwrite(savePath, img)

        return savePath
    }



    var path=""
    fun changeWhiteToRed(): String {
        // 读取图像
        val image = Imgcodecs.imread(path)

        // 图像缩放
        val img = Mat()
        Imgproc.resize(image, img, Size(), 1.0, 1.0)

        // 转换为HSV色彩空间
        val hsv = Mat()
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_BGR2HSV)

        // 图像二值化处理
        val mask = Mat()
        Core.inRange(hsv, Scalar(0.0, 0.0, 200.0), Scalar(180.0, 30.0, 255.0), mask)

        // 遍历每个像素点，进行颜色替换
        for (i in 0 until img.rows()) {
            for (j in 0 until img.cols()) {
                val pixel = mask[i, j]
                if (pixel[0] == 255.0) { // 白色像素
                    img.put(i, j, 0.0, 0.0, 255.0) // 将白色像素替换成红色
                }
            }
        }

        // 保存图像
        val savePath = filesDir.absolutePath + File.separator + "output.jpg"
        Imgcodecs.imwrite(savePath, img)

        return savePath
    }

    fun changeWhiteToBlue(): String {
        // 读取图像
        val image = Imgcodecs.imread(path)

        // 图像缩放
        val img = Mat()
        Imgproc.resize(image, img, Size(), 1.0, 1.0)

        // 转换为HSV色彩空间
        val hsv = Mat()
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_BGR2HSV)

        // 图像二值化处理
        val mask = Mat()
        Core.inRange(hsv, Scalar(0.0, 0.0, 200.0), Scalar(180.0, 30.0, 255.0), mask)

        // 遍历每个像素点，进行颜色替换
        for (i in 0 until img.rows()) {
            for (j in 0 until img.cols()) {
                val pixel = mask[i, j]
                if (pixel[0] == 255.0) { // 白色像素
                    img.put(i, j, 255.0, 0.0, 0.0) // 将白色像素替换成蓝色
                }
            }
        }

        // 保存图像
        val savePath = filesDir.absolutePath + File.separator + "output_blue.jpg"
        Imgcodecs.imwrite(savePath, img)

        return savePath
    }


}