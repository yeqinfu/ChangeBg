package ppandroid.changebg

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.UiMessageUtils
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
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Calendar

enum class ChangeType(
    var minB: Double, var minG: Double, var minR: Double,
    var maxB: Double, var maxG: Double, var maxR: Double,
    var B: Double, var G: Double, var R: Double,

    ) {
    RED_TO_WHITE(0.0, 135.0, 210.0, 180.0, 255.0, 255.0, 255.0, 255.0, 255.0),
    RED_TO_BLUE(0.0, 135.0, 204.0, 180.0, 255.0, 255.0, 255.0, 0.0, 0.0),

    BLUE_TO_RED(90.0, 90.0, 50.0, 120.0, 255.0, 255.0, 0.0, 0.0, 255.0),
    BLUE_TO_WHITE(90.0, 120.0, 50.0, 120.0, 255.0, 255.0, 255.0, 255.0, 255.0),


    WHITE_TO_RED(0.0, 0.0, 200.0, 180.0, 4.0, 255.0, 0.0, 0.0, 255.0),
    WHITE_TO_BLUE(0.0, 0.0, 200.0, 180.0, 4.0, 255.0, 255.0, 0.0, 0.0),


    ;
}

class MainActivity : AppCompatActivity(), View.OnClickListener {
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

    lateinit var textView2: TextView
    lateinit var textView3: TextView
    lateinit var textView4: TextView
    lateinit var textView5: TextView
    lateinit var textView6: TextView
    lateinit var textView7: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        OpenCVLoader.initLocal()
        var sourceImage = findViewById<ImageView>(R.id.imageView)

        textView2 = findViewById(R.id.textView2)
        textView3 = findViewById(R.id.textView3)
        textView4 = findViewById(R.id.textView4)
        textView5 = findViewById(R.id.textView5)
        textView6 = findViewById(R.id.textView6)
        textView7 = findViewById(R.id.textView7)

        findViewById<TextView>(R.id.textView).setOnClickListener {
            var msg="更改以下值，可能需要点颜色知识。或者一个个调感觉，就可以去掉可能人物画像被误转化成指定颜色的污染点。想搞成默认值，就后台杀死app重新打开就是默认值,默认值其实也是我瞎调出来的，能用就用，不能用自己调。"
            showAlertDialog(this,"提示",msg)
        }

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
                            path = avPath
                            // change(path = avPath)
                        }

                    }

                    override fun onCancel() {

                    }
                })

        }
        findViewById<Button>(R.id.btn_w_r).setOnClickListener(this)
        findViewById<Button>(R.id.btn_w_b).setOnClickListener(this)
        findViewById<Button>(R.id.btn_b_r).setOnClickListener(this)
        findViewById<Button>(R.id.btn_b_w).setOnClickListener(this)
        findViewById<Button>(R.id.btn_r_b).setOnClickListener(this)
        findViewById<Button>(R.id.btn_r_w).setOnClickListener(this)
        findViewById<Button>(R.id.btn_save).setOnClickListener {
            saveImageToGallery(
                resultPath,
                "改背景" + Calendar.getInstance().timeInMillis,
                "改背景" + Calendar.getInstance().timeInMillis
            )

        }

    }

    fun showAlertDialog(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                // 点击"确定"按钮的操作，这里可以留空或者添加相应逻辑
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                // 点击"取消"按钮的操作，这里可以留空或者添加相应逻辑
                dialog.dismiss()
            }
            .show()
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
        val uri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
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


    private var resultPath = ""


    private fun extracted(result: String?) {
        resultPath = result.toString()
        var resultImage = findViewById<ImageView>(R.id.imageView2)
        Glide.with(this@MainActivity).load(result)
            .skipMemoryCache(true) // 禁用内存缓存
            .diskCacheStrategy(DiskCacheStrategy.NONE) // 禁用磁盘缓存
            .into(resultImage)
    }

    var path = ""

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_r_b -> change(ChangeType.RED_TO_BLUE)
            R.id.btn_r_w -> change(ChangeType.RED_TO_WHITE)
            R.id.btn_w_r -> change(ChangeType.WHITE_TO_RED)
            R.id.btn_w_b -> change(ChangeType.WHITE_TO_BLUE)
            R.id.btn_b_r -> change(ChangeType.BLUE_TO_RED)
            R.id.btn_b_w -> change(ChangeType.BLUE_TO_WHITE)

        }
    }

    private fun change(type: ChangeType) {

        textView2.text = "最小蓝值${type.minB}"
        textView3.text = "最小绿值${type.minG}"
        textView4.text = "最小红值${type.minR}"
        textView5.text = "最大蓝值${type.maxB}"
        textView6.text = "最大绿值${type.maxG}"
        textView7.text = "最大红值${type.maxR}"

        findViewById<SeekBar>(R.id.progressBar).apply {
            progress = ((type.minB / 255) * 100).toInt()
            max = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    textView2.text = "最小蓝值${progress*2.55}"

                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })

        }
        findViewById<SeekBar>(R.id.progressBar2).apply {
            progress = ((type.minG / 255) * 100).toInt()
            max = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    textView3.text = "最小绿值${progress*2.55}"

                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })

        }
        findViewById<SeekBar>(R.id.progressBar3).apply {
            progress = ((type.minR / 255) * 100).toInt()
            max = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    textView4.text = "最小红值${progress*2.55}"

                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })

        }
        findViewById<SeekBar>(R.id.progressBar4).apply {
            progress = ((type.maxB / 255) * 100).toInt()
            max = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    textView5.text = "最大蓝值${progress*2.55}"

                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })

        }
        findViewById<SeekBar>(R.id.progressBar5).apply {
            progress = ((type.maxG / 255) * 100).toInt()
            max = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    textView6.text = "最大绿值${progress*2.55}"

                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })

        }
        findViewById<SeekBar>(R.id.progressBar6).apply {
            progress = ((type.maxR / 255) * 100).toInt()
            max = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    textView7.text = "最大红值${progress*2.55}"

                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })

        }
        realChange(type)


        findViewById<Button>(R.id.button).setOnClickListener {
            type.minB = findViewById<SeekBar>(R.id.progressBar).progress * 0.01 * 255
            type.minG = findViewById<SeekBar>(R.id.progressBar2).progress * 0.01 * 255
            type.minR = findViewById<SeekBar>(R.id.progressBar3).progress * 0.01 * 255
            type.maxB = findViewById<SeekBar>(R.id.progressBar4).progress * 0.01 * 255
            type.maxG = findViewById<SeekBar>(R.id.progressBar5).progress * 0.01 * 255
            type.maxR = findViewById<SeekBar>(R.id.progressBar6).progress * 0.01 * 255
            realChange(type)
        }
    }

    private fun realChange(type: ChangeType) {
        ThreadUtils.executeBySingle(object : ThreadUtils.SimpleTask<String>() {
            override fun doInBackground(): String {
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
                Core.inRange(
                    hsv,
                    Scalar(type.minB, type.minG, type.minR),
                    Scalar(type.maxB, type.maxG, type.maxR),
                    mask
                )

                // 遍历每个像素点，进行颜色替换
                for (i in 0 until img.rows()) {
                    for (j in 0 until img.cols()) {
                        val pixel = mask[i, j]
                        if (pixel[0] == 255.0) {
                            img.put(i, j, type.B, type.G, type.R)
                        }
                    }
                }

                // 保存图像
                val savePath = filesDir.absolutePath + File.separator + "output.jpg"
                Imgcodecs.imwrite(savePath, img)

                return savePath
            }

            override fun onSuccess(result: String?) {
                extracted(result)

            }

        })

    }


}