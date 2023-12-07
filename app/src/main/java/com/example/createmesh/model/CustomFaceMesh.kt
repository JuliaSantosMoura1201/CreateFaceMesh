package com.example.createmesh.model

import com.fasterxml.jackson.annotation.JsonValue
import com.google.mlkit.vision.facemesh.FaceMesh
import java.io.Serializable

class CustomFaceMesh(fileName: String, faceMesh: FaceMesh) {
    val fileName: String
    val allPoints: MutableMap<Int, Point> = mutableMapOf()
    val isFraud: Boolean
    val fraudType: FraudType

    init {
        this.fileName = fileName
        this.fraudType = identifyFraudTypeByName(fileName)
        this.isFraud = this.fraudType != FraudType.NOT_FRAUD

        faceMesh.allPoints.map { faceMeshPoint ->
            this.allPoints[faceMeshPoint.index] = Point(
                faceMeshPoint.position.x,
                faceMeshPoint.position.y,
                faceMeshPoint.position.z
            )
        }
    }

    companion object {
        fun identifyFraudTypeByName(fileName: String): FraudType {
            val patternL = "(G|Ps|Pq|Vl|Vm|Mc|Mf|Mu|Ml)"

            val regex = Regex("($patternL)")
            val matchResult = regex.find(fileName) ?: return FraudType.NOT_FRAUD

            val imageType = matchResult.groupValues[1]

            return when (imageType) {
                in listOf("Ps", "Pq") -> FraudType.PRINT
                in listOf("Vl", "Vm") -> FraudType.VIDEO
                in listOf("Mc", "Mf", "Mu", "Ml") -> FraudType.MASK
                else -> FraudType.NOT_FRAUD
            }
        }
    }
}

enum class FraudType(@JsonValue val type: String) {
    MASK("mask"),
    VIDEO("video"),
    PRINT("print"),
    NOT_FRAUD("not_fraud")
}

data class Point(
    val x: Float,
    val y: Float,
    val z: Float
) : Serializable