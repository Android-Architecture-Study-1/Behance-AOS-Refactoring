package com.sopt.behance_aos.ui.create

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.sopt.behance_aos.R
import com.sopt.behance_aos.databinding.ActivityBehanceCreateBinding
import com.sopt.behance_aos.ui.create.adpater.GalleryAdapter
import com.sopt.behance_aos.ui.create.viewmodel.CreateViewModel
import com.sopt.behance_aos.util.GridItemSpaceDecoration


class BehanceCreateActivity : AppCompatActivity() {


    private lateinit var binding: ActivityBehanceCreateBinding
    private val createViewModel by viewModels<CreateViewModel>()

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) { // 권한 획득 성공 시
                // 갤러리 이미지 띄우기
                initAdapter()

            } else { // 권한 획득 거부 시
                Toast.makeText(this, "갤러리에 접근하기 위해서는 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_behance_create)

        // 권한 요청
        requestPermission()

        // 버튼 클릭 이벤트
        backEvent()
        nextBtn()
    }


    // 권한 체크 함수
    private fun requestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {  // 이미 권한부여를 받았기에 권한이 필요한 작업 수행
                // 갤러리 이미지 띄우기
                initAdapter()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> { // 권한 요청 권유
                showInContextUI()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)   // 바로 권한요청하는 경우
            }
        }

    }

    private fun showInContextUI() {
        AlertDialog.Builder(this)
            .setTitle("권한 동의 필요")
            .setMessage("갤러리에 접근하기 위해서는 권한이 필요합니다.")
            .setPositiveButton("동의") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            .setNegativeButton("거부") { _, _ ->
                Toast.makeText(this, "갤러리에 접근하기 위해서는 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
            .create()
            .show()
    }

    // 리사이클러뷰 연결
    private fun initAdapter() {
        val galleryAdapter = GalleryAdapter()
        binding.rvCreateGallery.also { it ->
            it.layoutManager = GridLayoutManager(this, 4) // 아이템 개수 4개
            it.addItemDecoration(GridItemSpaceDecoration(2, 1)) // 아이템 사이에 간격 넣기
            it.adapter = galleryAdapter // 어댑터 연결
        }

        createViewModel.imageList.observe(this) {
            galleryAdapter.submitList(it)
        }

        createViewModel.showImages(this) // 이미지 데이터 연결

        // 이미지 클릭 이벤트
        galleryAdapter.setOnItemClickListener(object : GalleryAdapter.OnItemClickListener {
            override fun onItemClick(v: View, uri: Uri, pos: Int) {
                if (binding.ivCreateSquare.visibility != View.INVISIBLE) { // 안내 메세지 숨기기
                    hideContent()
                }
                createViewModel.setImgUri(uri)
                Glide.with(this@BehanceCreateActivity).load(uri).into(binding.ivCreateSelectedPhoto)
            }
        })
    }


    private fun backEvent() {
        binding.btnCreateBack.setOnClickListener {
            finish()
        }
    }

    // 안내 이미지 숨기기
    private fun hideContent() {
        binding.apply {
            ivCreateSquare.visibility = View.INVISIBLE
            tvCreateToolInfo.visibility = View.INVISIBLE
            tvCreateProjectStart.visibility = View.INVISIBLE
            tvCreateNext.setTextColor(Color.BLACK)
            tvCreateNext.isEnabled = true
            tvCreateReOrganization.setTextColor(Color.BLACK)
        }
    }

    // 파일 업로드
    private fun nextBtn() {
        binding.tvCreateNext.setOnClickListener {
            createViewModel.postFile(this)
        }
    }

}