package com.app.ujjivanbankscrapper.Utils


import android.os.Build
import androidx.annotation.RequiresApi
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

class AES {

    private  val key = "595c173109d94702ac9eb402fee4882b"
    private  val initVector = "595c173109d94702"

    fun encrypt(value: String): String? {
        try {
            val iv = IvParameterSpec(initVector.toByteArray(Charsets.UTF_8))
            val skeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv)

            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(encrypted)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun decrypt(encrypted: String): String? {
        try {
            val iv = IvParameterSpec(initVector.toByteArray(Charsets.UTF_8))
            val skeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv)

            val original = cipher.doFinal(Base64.getDecoder().decode(encrypted))
            return String(original, Charsets.UTF_8)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }
}