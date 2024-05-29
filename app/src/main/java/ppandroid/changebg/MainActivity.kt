package ppandroid.changebg

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import com.luck.picture.lib.utils.ToastUtils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc


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
        var sourceImage=findViewById<ImageView>(R.id.imageView)
        var resultImage=findViewById<ImageView>(R.id.imageView2)

        findViewById<Button>(R.id.btn_select).setOnClickListener {
            PictureSelector.create(this)
                .openGallery(SelectMimeType.ofImage())
                .setMaxSelectNum(1)
                .setImageEngine(GlideEngine.createGlideEngine())
                .forResult(object : OnResultCallbackListener<LocalMedia?> {
                    override fun onResult(result: ArrayList<LocalMedia?>) {
                        // 假设只选择了一张图片
                        if (result.isNotEmpty()) {
                            var avPath=result[0]!!.availablePath
                            var uri=Uri.parse(avPath)
                            avPath= getRealPathFromUri(this@MainActivity,uri)
                            Glide.with(this@MainActivity).load(avPath).into(sourceImage)
                            ToastUtils.showToast(this@MainActivity,avPath)
                           // change(path = avPath)
                        }

                    }
                    override fun onCancel() {

                    }
                })

        }

        findViewById<Button>(R.id.btn_b_r).setOnClickListener {


        }

    }
    fun change(path:String){
        // 读取图像
        val image = Imgcodecs.imread(path)
        // 图像缩放
        val img = Mat()
        Imgproc.resize(image, img, Size(), 0.5, 0.5)
        val rows = img.rows()
        val cols = img.cols()
        val channels = img.channels()
        println("$rows $cols $channels")
        // 转换为HSV色彩空间
        val hsv = Mat()
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_BGR2HSV)
        // 图像二值化处理
        val mask = Mat()
        Core.inRange(hsv, Scalar(90.0, 70.0, 70.0), Scalar(110.0, 255.0, 255.0), mask)
        // 腐蚀
        val erode = Mat()
        Imgproc.erode(mask, erode, Mat(), Point(), 1)
        // 膨胀
        val dilate = Mat()
        Imgproc.dilate(erode, dilate, Mat(), Point(), 1)
        // 遍历每个像素点，进行颜色替换
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val pixel = dilate[i, j]
                if (pixel[0] == 255.0) { // 白色像素
                    img.put(i, j, *doubleArrayOf(0.0, 0.0, 255.0)) // BGR通道
                }
            }
        }
        // 显示图像
        Imgcodecs.imwrite("output.jpg", img)
    }
}