package com.example.photoeditor

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.jpeg.JpegMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.*

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class EditPhotoActivity : ComponentActivity() {

    private lateinit var photoUri: Uri
    private lateinit var bitmap: Bitmap
    private lateinit var gpuImage: GPUImage
    private var currentFilter: GPUImageFilter? = null
    private var filteredBitmap: Bitmap? = null
    private val permissionRequestCode = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        photoUri = intent.getParcelableExtra<Uri>("photoUri")!!
        bitmap = decodeUriToBitmap(photoUri)

        gpuImage = GPUImage(this)
        gpuImage.setImage(bitmap)

        setContent {
            EditPhotoScreen()
        }
    }

    @Composable
    private fun EditPhotoScreen() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                bitmap = (filteredBitmap ?: bitmap).asImageBitmap(),
                contentDescription = "Photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            FilterButtonsRow()

            Spacer(modifier = Modifier.height(8.dp))

            DrawButton()

            Spacer(modifier = Modifier.height(16.dp))

            ShareButton()

            Spacer(modifier = Modifier.height(8.dp))

            SaveToGalleryButton()

            Spacer(modifier = Modifier.height(8.dp))

            ExitButton()
        }
    }

    @Composable
    private fun FilterButtonsRow() {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            item {
                FilterButton(filterName = "None") {
                    applyFilter(GPUImageFilter())
                }
                Spacer(modifier = Modifier.width(8.dp))

            }
            item {
                FilterButton(filterName = "Grayscale") {
                    applyFilter(GPUImageGrayscaleFilter())
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            item {
                FilterButton(filterName = "Color Invert") {
                    applyFilter(GPUImageColorInvertFilter())
                }
                Spacer(modifier = Modifier.width(8.dp))

            }
            item {
                FilterButton(filterName = "Swirl") {
                    applyFilter(GPUImageSwirlFilter())
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            item {
                FilterButton(filterName = "BulgeDistortion") {
                    applyFilter(GPUImageBulgeDistortionFilter())
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            item {
                FilterButton(filterName = "Toon") {
                    applyFilter(GPUImageToonFilter())
                }
                Spacer(modifier = Modifier.width(8.dp))

            }
            item {
                FilterButton(filterName = "Sepia") {
                    applyFilter(GPUImageSepiaToneFilter())
                }
                Spacer(modifier = Modifier.width(8.dp))

            }
            item {
                FilterButton(filterName = "Sketch") {
                    applyFilter(GPUImageSketchFilter())
                }
                Spacer(modifier = Modifier.width(8.dp))

            }
            item {
                FilterButton(filterName = "Emboss") {
                    applyFilter(GPUImageEmbossFilter())
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            item {
                FilterButton(filterName = "Haze") {
                    applyFilter(GPUImageHazeFilter())
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            item {
                FilterButton(filterName = "GaussianBlur") {
                    applyFilter(GPUImageGaussianBlurFilter())
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            item {
                FilterButton(filterName = "Solarize") {
                    applyFilter(GPUImageSolarizeFilter())
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            item {
                FilterButton(filterName = "Posterize") {
                    applyFilter(GPUImagePosterizeFilter())
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }

    @Composable
    private fun ExitButton() {
        Button(
            onClick = {
                setResult(Activity.RESULT_CANCELED)
                finish()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Венуться в Главное меню")
        }
    }


    @Composable
    private fun DrawButton() {
        Button(
            onClick = { startDrawingActivity() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Рисовать на изображении")
        }
    }

    private fun startDrawingActivity() {
        val intent = Intent(this, DrawingActivity::class.java)
        intent.putExtra("photoUri", photoUri)
        startActivity(intent)
    }

    @Composable
    private fun FilterButton(filterName: String, onClick: () -> Unit) {
        Button(onClick = {
            onClick()
            applyFilterToBitmap()
        }) {
            Text(text = filterName)
        }
    }

    @Composable
    private fun ShareButton() {
        Button(
            onClick = { sharePhoto() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Поделиться")
        }
    }

    @Composable
    private fun SaveToGalleryButton() {
        Button(
            onClick = { saveToGallery() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Сохранить в галерею")
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
            val orientationTag = exifDirectory?.getObject(ExifIFD0Directory.TAG_ORIENTATION) as? Int
            return when (orientationTag) {
                6 -> 90
                3 -> 180
                8 -> 270
                else -> 0
            }
        }
        return 0
    }

    private fun applyFilter(filter: GPUImageFilter) {
        currentFilter = filter
        gpuImage.setFilter(filter)
    }

    private fun applyFilterToBitmap() {
        filteredBitmap = gpuImage.bitmapWithFilterApplied
        setContent {
            EditPhotoScreen()
        }
    }

    private fun sharePhoto() {
        val sharedImageUri = getImageUri(filteredBitmap ?: bitmap)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, sharedImageUri)
        }
        startActivity(Intent.createChooser(shareIntent, "Поделиться"))
    }

    private fun saveToGallery() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            saveImageToGalleryScopedStorage()
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    permissionRequestCode
                )
            } else {
                saveImageToGalleryLegacy()
            }
        }
    }

    private fun saveImageToGalleryScopedStorage() {
        val imageFileName = "Filtered_Image_${System.currentTimeMillis()}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val contentResolver = contentResolver
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                val bitmapToSave = filteredBitmap ?: bitmap
                bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.flush()
                Toast.makeText(this, "Фото сохранено", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(this, "Не удалось сохранить фото", Toast.LENGTH_SHORT).show()
    }

    private fun saveImageToGalleryLegacy() {
        val imageFileName = "Filtered_Image_${System.currentTimeMillis()}.jpg"
        val imagesFolder =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val imageFile = File(imagesFolder, imageFileName)

        val outputStream: OutputStream = FileOutputStream(imageFile)
        val bitmapToSave = filteredBitmap ?: bitmap
        bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()

        MediaStore.Images.Media.insertImage(
            contentResolver,
            imageFile.absolutePath,
            imageFileName,
            "Filtered image"
        )

        checkIfImageSaved(imageFile.absolutePath)
    }
    private fun checkIfImageSaved(imagePath: String) {
        val file = File(imagePath)
        if (file.exists()) {
            Toast.makeText(this, "Фото сохранено", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Не удалось сохранить фото", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getImageUri(bitmap: Bitmap): Uri {
        val imagesFolder =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val file = File(imagesFolder, "shared_image.jpg")
        val outputStream: OutputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = android.graphics.Matrix()
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
}

