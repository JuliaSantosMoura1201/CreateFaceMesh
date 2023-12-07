package com.example.createmesh

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.createmesh.model.CustomFaceMesh
import com.example.createmesh.model.FraudType
import com.example.createmesh.model.GoogleDriveFile
import com.google.auth.oauth2.GoogleCredentials
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Stack

class CreateMeshViewModel: ViewModel() {

    var shouldStopRoutine: Boolean = false

    var videoImagesCount: Int = 0
    var maskImagesCount: Int = 0
    var realImagesCount: Int = 0
    var printImagesCount: Int = 0

    val fileListUrl = "https://www.googleapis.com/drive/v3/files?q="
    var authorizationToken: String? = null
    var googleDriveList: MutableList<GoogleDriveFile> = mutableListOf()

    private val detector = FaceMeshDetection.getClient()
    private val gson = Gson()

    var customFaceMeshList: MutableList<CustomFaceMesh> = mutableListOf()

    private val _jsonFile: MutableLiveData<String> = MutableLiveData()
    val jsonFile: LiveData<String> = _jsonFile

    private val _currentImage: MutableLiveData<ImageBitmap> = MutableLiveData(
        ImageBitmap(500, 500, hasAlpha = true, config = ImageBitmapConfig.Argb8888)
    )
    val currentImage: LiveData<ImageBitmap> = _currentImage

    fun obterTokenDeAcesso(context: Context) {
        val assetManager: AssetManager = context.assets
        val fileInputStream = assetManager.open("face-to-face-406722-cbbb12d0a4c1.json")

        val credentials = GoogleCredentials
            .fromStream(fileInputStream)
            .createScoped(
                listOf(
                    "https://www.googleapis.com/auth/drive",
                )
            )

        viewModelScope.launch(Dispatchers.IO) {
            val accessToken = credentials.refreshAccessToken()

            authorizationToken = accessToken.tokenValue
        }
    }

    fun getFilesListFromGoogleDrive(rootFolderId: String) {
        if (authorizationToken.isNullOrEmpty()) {
            return
        }

        val folderIdStack = Stack<Pair<String, String>>()
        folderIdStack.push(Pair(rootFolderId, "root"))

        googleDriveList.clear()

        var i = 1
        do {
            val folderInfo = folderIdStack.pop()
            val folderId = folderInfo.first
            val folderName = folderInfo.second

            val listFilesUrl = "$fileListUrl'$folderId' in parents and trashed = false"

            val request = Request.Builder()
                .url(listFilesUrl)
                .header("Authorization", "Bearer $authorizationToken")
                .build()

            val client = OkHttpClient()
            client.newCall(request).execute().also {response ->
                if (!response.isSuccessful)
                    return@also

                val responseBody = response.body?.string() ?: return@also

                val parsedJson = Json.parseToJsonElement(responseBody).jsonObject
                val filesInfoList = parsedJson["files"]!!.jsonArray

                filesInfoList.forEach{ file ->
                    val jsonFile = file.jsonObject
                    Log.d(TAG, "nº: ${googleDriveList.size}, " +
                            "File Name: ${jsonFile["name"]!!.jsonPrimitive.content}, " +
                            "File ID: ${jsonFile["id"]!!.jsonPrimitive.content}")

                    if (jsonFile["mimeType"]!!.jsonPrimitive.content == "application/vnd.google-apps.folder") {
                        // Se for uma pasta, adiciona à pilha para listar os arquivos dela posteriormente
                        folderIdStack.push(
                            Pair(jsonFile["id"]!!.jsonPrimitive.content, jsonFile["name"]!!.jsonPrimitive.content)
                        )
                    }

                    if (jsonFile["mimeType"]!!.jsonPrimitive.content == "image/jpeg") {
                        val imageFile = GoogleDriveFile(
                            jsonFile["kind"]!!.jsonPrimitive.content,
                            jsonFile["mimeType"]!!.jsonPrimitive.content,
                            jsonFile["id"]!!.jsonPrimitive.content,
                            jsonFile["name"]!!.jsonPrimitive.content,
                            folderName
                        )

                        googleDriveList.add(imageFile)
                    }
                }

                i++
            }
        } while(!folderIdStack.isEmpty() && googleDriveList.size <= 10000)
        // && googleDriveList.size <= 10000
    }

    fun downloadAndProcessImages() {
        val fileDownloadUrlBase = "https://www.googleapis.com/drive/v3/files/"
        var i = 0
        googleDriveList.forEach {file ->
            val fileName = "${file.parentFolderName}/${file.name}"

            if (shouldStopRoutine)
                return@forEach

            val fileImageType = CustomFaceMesh.identifyFraudTypeByName(fileName)

            when (fileImageType) {
                FraudType.MASK -> {
                    if (maskImagesCount > 50) return@forEach
                    maskImagesCount++
                }
                FraudType.VIDEO -> {
                    if (realImagesCount > 50) return@forEach
                    realImagesCount++
                }
                FraudType.PRINT -> {
                    if (printImagesCount > 50) return@forEach
                    printImagesCount++
                }
                FraudType.NOT_FRAUD -> {
                    if (realImagesCount > 150) return@forEach
                    realImagesCount++
                }
            }

            val downloadFileUrl = "$fileDownloadUrlBase${file.id}?alt=media"

            val request = Request.Builder()
                .url(downloadFileUrl)
                .header("Authorization", "Bearer $authorizationToken")
                .build()

            val client = OkHttpClient()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful)
                return@forEach

            val inputStream = response.body?.byteStream()

            if (inputStream != null) {
                i++
                val imageFile = File.createTempFile("image", ".jpg")
                try {
                    val outputStream = FileOutputStream(imageFile)
                    inputStream.use { input ->
                        outputStream.use { fileOut ->
                            input.copyTo(fileOut)
                        }
                    }
                    Log.d(TAG, "Arquivo $i - $fileName baixado")
                    processImage(fileName, imageFile) {
                        outputStream.close()
                        inputStream.close()
                        imageFile.delete()
                    }
                } catch (e: Exception) {
                    inputStream.close()
                    imageFile.delete()
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            createJsonFileAndUploadToGoogleDrive()
        }
    }

    private fun processImage(fileName: String, imageFile: File, onFinishProcessing: () -> Boolean) {
        val bitmapFile = BitmapFactory.decodeFile(imageFile.absolutePath)
        val inputImage = InputImage.fromBitmap(bitmapFile, 0)
        detector.process(inputImage).addOnSuccessListener { facesList ->
            val faceMesh = facesList.firstOrNull() ?: return@addOnSuccessListener
            val customFaceMesh = CustomFaceMesh(
                fileName,
                faceMesh
            )

            customFaceMeshList.add(customFaceMesh)

            viewModelScope.launch(Dispatchers.Main) {
                _currentImage.value = bitmapFile.asImageBitmap()
            }

            onFinishProcessing()
        }
    }

    private suspend fun createJsonFileAndUploadToGoogleDrive() {
        Log.d(TAG, "Fazendo o upload do dataset")
        delay(1000)
        val gson = Gson()
        val json = gson.toJson(customFaceMeshList)
        val folderToSaveId = "1NnzRRyURDdFWjmV8LaY1KBE1sW3XQbCg"
        val jsonFileName = "dataset"

        // Escrever o JSON para um arquivo
        val inputStream = json.byteInputStream()
        val jsonFile = File.createTempFile(jsonFileName, ".json")
        val outputStream = FileOutputStream(jsonFile)
        inputStream.use { input ->
            outputStream.use { fileOut ->
                input.copyTo(fileOut)
            }
        }

        val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "metadata",
                null,
                RequestBody.create(
                    "application/json".toMediaTypeOrNull(),
                    "{\"name\": \"$jsonFileName.json\", \"parents\": [\"$folderToSaveId\"]}"
                )
            )
            .addFormDataPart(
                "file",
                jsonFile.name,
                RequestBody.create("application/json".toMediaTypeOrNull(), jsonFile)
            )
            .build()

        val request = Request.Builder()
            .header("Authorization", "Bearer $authorizationToken")
            .url(uploadUrl)
            .post(requestBody)
            .build()

        // Execução da requisição
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Lidar com falha na requisição
            }

            override fun onResponse(call: Call, response: Response) {
                // Lidar com a resposta da requisição
                val responseBody = response.body?.string() ?: return
                outputStream.close()
                inputStream.close()
            }
        })
    }


    companion object{
        private const val AMOUNT_OF_GENUINE_FACES = 35
        private const val AMOUNT_OF_FRAUD_FACES = 25

        private const val TOTAL_FACES = AMOUNT_OF_GENUINE_FACES + AMOUNT_OF_FRAUD_FACES - 1

        private const val TAG = "CreateMeshViewModel"
    }
}