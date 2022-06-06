package com.sopt.behance_aos.ui.create.viewmodel

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sopt.behance_aos.data.MediaStoreImage
import com.sopt.behance_aos.data.RetrofitBuilder
import com.sopt.behance_aos.data.response.ResponseFile
import com.sopt.behance_aos.ui.create.title.BehanceTitleActivity
import com.sopt.behance_aos.util.MultiPartResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class CreateViewModel() : ViewModel() {

    // uri 적용
    private val _uri = MutableLiveData<Uri>()
    val uri:LiveData<Uri> = _uri

    // 불러올 이미지 리스트들
    private val _imageList = MutableLiveData<List<MediaStoreImage>>()
    val imageList: LiveData<List<MediaStoreImage>> = _imageList

    fun setImgUri(selectedUri:Uri){
        _uri.value = selectedUri
    }

    // 갤러리 리스트 블러오기
    suspend fun queryImages(context: Context): List<MediaStoreImage> {
        val images = mutableListOf<MediaStoreImage>()

        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN
            )
            val selection = "${MediaStore.Images.Media.DATE_TAKEN} >= ?"
            val selectionArgs = arrayOf(
                dateToTimestamp(day = 1, month = 1, year = 1970).toString()
            )

            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null, // selection
                null, // selectionArgs
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateTakenColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val displayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val dateTaken = Date(cursor.getLong(dateTakenColumn))
                    val displayName = cursor.getString(displayNameColumn)
                    val contentUri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )

                    val image = MediaStoreImage(id, displayName, dateTaken, contentUri)
                    images += image
                    Log.d(ContentValues.TAG, image.toString())
                }
            }
        }

        Log.d(ContentValues.TAG, "Found ${images.size} images")
        return images
    }

    @SuppressLint("SimpleDateFormat")
    fun dateToTimestamp(day: Int, month: Int, year: Int): Long =

        SimpleDateFormat("dd.MM.yyyy").let { formatter ->
            formatter.parse("$day.$month.$year")?.time ?: 0
        }

    // 이미지 보여주기
    fun showImages(context:Context) {
        GlobalScope.launch {
            val images = queryImages(context)
            _imageList.postValue(images)
        }
    }

    // file post 서버 통신
    fun postFile(context:Context){
        val file = MultiPartResolver(context).createImgMultiPart(_uri.value!!)
        val call = RetrofitBuilder.createService.postFile(file)

        call.enqueue(object : Callback<ResponseFile> {
            override fun onResponse(
                call: Call<ResponseFile>,
                response: Response<ResponseFile>
            ) {

                if (response.isSuccessful) { // 파일 생성 성공

                    val intent = Intent(context, BehanceTitleActivity::class.java)
                    intent.putExtra("link", response.body()?.data?.link) // 통신으로 받은 유저 link 값 넘겨주기

                    context.startActivity(intent)
                    (context as Activity).finish()
                }

            }

            override fun onFailure(call: Call<ResponseFile>, t: Throwable) { // 서버 통신에러
                Log.e("NetworkTest", "error:$t")
            }

        })
    }
}