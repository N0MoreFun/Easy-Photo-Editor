package com.example.photoeditor

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.drew.imaging.jpeg.JpegMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import jp.co.cyberagent.android.gpuimage.GPUImage
import java.io.File
import java.io.FileOutputStream

class DrawingActivity : AppCompatActivity() {

    private lateinit var photoUri: Uri
    private lateinit var bitmap: Bitmap
    private lateinit var gpuImage: GPUImage

    private val paths = mutableStateListOf<Path>()
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        photoUri = intent.getParcelableExtra<Uri>("photoUri")!!
        bitmap = decodeUriToBitmap(photoUri)
        gpuImage = GPUImage(this)
        gpuImage.setImage(bitmap)

        setContent {
            DrawingScreen()
        }
    }

    @Composable
    private fun DrawingScreen() {
        val (imageRect, setImageRect) = remember { mutableStateOf<Rect?>(null) }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(bitmap.width.toFloat() / bitmap.height)

            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            val topLeft = coordinates.positionInRoot()
                            val bottomRight = Offset(
                                topLeft.x + coordinates.size.width,
                                topLeft.y + coordinates.size.height
                            )
                            val rect = Rect(
                                topLeft.x.toInt(),
                                topLeft.y.toInt(),
                                bottomRight.x.toInt(),
                                bottomRight.y.toInt()
                            )
                            setImageRect(rect)
                        }
                )

                imageRect?.let { rect ->
                    DrawView(rect)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            SaveButton()
            Spacer(modifier = Modifier.height(16.dp))
            CancelButton()
        }
    }


    @Composable
    private fun DrawView(drawZoneRect: Rect) {
        val touchInProgress = remember { mutableStateOf(false) }
        val currentPath = remember { mutableStateOf(Path()) }
        AndroidView(
            factory = { context ->
                object : View(context) {
                    override fun onDraw(canvas: Canvas) {
                        for (path in paths) {
                            canvas.drawPath(path, paint)
                        }
                        if (touchInProgress.value) {
                            canvas.drawPath(currentPath.value, paint)
                        }
                    }

                    override fun onTouchEvent(event: MotionEvent): Boolean {

                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                if (drawZoneRect.contains(event.x.toInt(), event.y.toInt())) {
                                    currentPath.value = Path()
                                    currentPath.value.moveTo(event.x, event.y)
                                    paths.add(currentPath.value)
                                    touchInProgress.value = true
                                }
                            }
                            MotionEvent.ACTION_MOVE -> {
                                if (touchInProgress.value) {
                                    val clampedX = event.x.coerceIn(
                                        drawZoneRect.left.toFloat(),
                                        drawZoneRect.right.toFloat()
                                    )
                                    val clampedY = event.y.coerceIn(
                                        drawZoneRect.top.toFloat(),
                                        drawZoneRect.bottom.toFloat()
                                    )
                                    currentPath.value.lineTo(clampedX, clampedY)
                                    invalidate()
                                }
                            }
                            MotionEvent.ACTION_UP -> {
                                if (touchInProgress.value) {
                                    val clampedX = event.x.coerceIn(
                                        drawZoneRect.left.toFloat(),
                                        drawZoneRect.right.toFloat()
                                    )
                                    val clampedY = event.y.coerceIn(
                                        drawZoneRect.top.toFloat(),
                                        drawZoneRect.bottom.toFloat()
                                    )
                                    currentPath.value.lineTo(clampedX, clampedY)
                                    invalidate()
                                    touchInProgress.value = false
                                }
                            }
                        }
                        return true
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    @Composable
    private fun CancelButton() {
        Button(
            onClick = {
                setResult(Activity.RESULT_CANCELED)
                finish()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Отмена")
        }
    }


    @Composable
    private fun SaveButton() {
        Button(
            onClick = {
                saveDrawnPhoto()
                setResult(Activity.RESULT_OK)
                finish()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Сохранить")
        }
    }

    private fun decodeUriToBitmap(uri: Uri): Bitmap {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val options = BitmapFactory.Options()
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            val orientation = getOrientationFromExif(uri)
            return rotateBitmap(bitmap!!, orientation)
        }
        throw IllegalStateException("Failed to decode bitmap from URI")
    }

    private fun getOrientationFromExif(uri: Uri): Int {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val metadata = JpegMetadataReader.readMetadata(inputStream)
            val exifDirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
            val orientationTag =
                exifDirectory?.getObject(ExifIFD0Directory.TAG_ORIENTATION) as? Int
            return when (orientationTag) {
                6 -> 90
                3 -> 180
                8 -> 270
                else -> 0
            }
        }
        return 0
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    private fun saveDrawnPhoto() {
        val drawnBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(drawnBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, null) // Draw the original photo on the canvas

        for (path in paths) {
            canvas.drawPath(path, paint)
        }

        val drawnPhotoFile = File(cacheDir, "drawn_photo.jpg")
        val outputStream = FileOutputStream(drawnPhotoFile)
        drawnBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.close()

        val drawnPhotoUri = Uri.fromFile(drawnPhotoFile)
        val intent = Intent(this, EditPhotoActivity::class.java)
        intent.putExtra("photoUri", drawnPhotoUri)
        startActivity(intent)
    }
}




