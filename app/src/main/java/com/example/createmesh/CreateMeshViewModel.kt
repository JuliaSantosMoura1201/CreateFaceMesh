package com.example.createmesh

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.createmesh.model.CustomFaceMesh
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CreateMeshViewModel: ViewModel() {

    private val detector = FaceMeshDetection.getClient()
    private val gson = Gson()

    var customFaceMeshList: MutableList<CustomFaceMesh> = mutableListOf()

    private val _success: MutableLiveData<Int> = MutableLiveData()
    val success: LiveData<Int> = _success

    private val _failure: MutableLiveData<Pair<Int, Exception>> = MutableLiveData()
    val failure: LiveData<Pair<Int, Exception>> = _failure

    private val _jsonFile: MutableLiveData<String> = MutableLiveData()
    val jsonFile: LiveData<String> = _jsonFile

    private fun process(image: InputImage, nextImage: Int, fileName: String) {
        detector.process(image)
            .addOnSuccessListener{ handleSuccess(it, nextImage, fileName) }
            .addOnFailureListener { handleFailure(it, nextImage) }
    }

    private fun handleSuccess(faceMeshes: List<FaceMesh>, nextImage: Int, fileName: String){
        customFaceMeshList.addAll(faceMeshes.map { faceMesh ->
            CustomFaceMesh(fileName, faceMesh, nextImage > AMOUNT_OF_GENUINE_FACES - 1) }
        )

        if(nextImage == TOTAL_FACES){
            _jsonFile.value = gson.toJson(customFaceMeshList)
        } else{
            _success.value = nextImage
        }
    }

    private fun handleFailure(exception: Exception, nextImage: Int){
        if(nextImage == TOTAL_FACES){
            _jsonFile.value = gson.toJson(customFaceMeshList)
        } else{
            _failure.value = Pair(nextImage, exception)
        }
    }

    fun addToDataSet(bitmap: Bitmap, currentImage: Int, fileName: String){
        viewModelScope.launch {
            delay(100L)
            process(InputImage.fromBitmap(bitmap, 0), currentImage + 1, fileName)
        }
    }

    companion object{
        private const val AMOUNT_OF_GENUINE_FACES = 35
        private const val AMOUNT_OF_FRAUD_FACES = 25

        private const val TOTAL_FACES = AMOUNT_OF_GENUINE_FACES + AMOUNT_OF_FRAUD_FACES - 1
    }
}