package kr.co.hoonproj.webviewappdemo.services

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.coroutine.TedPermission
import gun0912.tedimagepicker.builder.TedImagePicker
import gun0912.tedimagepicker.builder.type.MediaType
import kr.co.hoonproj.webviewappdemo.model.local.ErrorCode
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG: String = "[WebViewAppDemo] PhotoImageService"

class PhotoImageService(private val context: Context) {

    suspend fun requestAuthorization(): Boolean {
        val permissionBuilder = TedPermission.create()
//            .setPermissionListener(object : PermissionListener {
//                override fun onPermissionGranted() {
//                    Log.i(TAG, "PhotoImageService:: onPermissionGranted()")
//                }
//                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
//                    Log.w(TAG, "PhotoImageService:: onPermissionDenied()")
//                }
//            })
            .setRationaleMessage("앱을 이용하기 위해서는 '사진' 권한이 필요합니다.")
            .setDeniedMessage("요청한 권한을 거부할 경우, 앱을 이용할 수 없습니다.\n설정의 앱 정보에서 해당 권한을 활성화해주세요.")

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionBuilder.setPermissions(
                Manifest.permission.READ_MEDIA_IMAGES
            ).checkGranted()
        } else {
            permissionBuilder.setPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ).checkGranted()
        }
    }

    suspend fun openPhotoPicker(maxSelection: Int): List<Uri>? = suspendCoroutine { continuation ->
        TedImagePicker.with(context)
            .mediaType(MediaType.IMAGE)
            .showCameraTile(false)
            .max(maxSelection, "선택 가능한 이미지는 최대 ${maxSelection}개 입니다.")

            .cancelListener {
                Log.w(TAG, "OpenPhotoPicker_Canceled")
                continuation.resume(null)
            }
            .errorListener {
                continuation.resumeWithException(it)
            }
            .startMultiImage { imageList ->
                runCatching {
                    Log.d(TAG, "OpenPhotoPicker_Finished")
                    continuation.resume(imageList)
                }.onFailure {
                    continuation.resumeWithException(it)
                }
            }
    }

    suspend fun getPhotoImage(uri: Uri, width: Int, height: Int): Bitmap = suspendCoroutine { continuation ->
        Glide.with(context)
            .asBitmap()
            .load(uri)
            .override(width, height)

            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {}
                override fun onLoadFailed(errorDrawable: Drawable?) {
//                    super.onLoadFailed(errorDrawable)

                    Log.e(TAG, "GetPhotoImage_Failure")
                    continuation.resumeWithException(Exception(ErrorCode.ImageDataFailure.description))
                }
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    Log.d(TAG, "GetPhotoImage_Success")
                    continuation.resume(resource)
                }
            })
    }

    suspend fun convertImageToBase64(resource: Bitmap): String? = suspendCoroutine { continuation ->
        val outputStream = ByteArrayOutputStream()
//        resource.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        resource.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

        val image = outputStream.toByteArray()
        val base64Image = Base64.encodeToString(image, Base64.NO_WRAP)
        continuation.resume(base64Image)
    }
}