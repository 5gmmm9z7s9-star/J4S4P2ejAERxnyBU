package com.example.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricStatus {
    BIOMETRIC_READY,
    DEVICE_CREDENTIAL_ONLY,
    NONE_ENROLLED,
    HARDWARE_UNAVAILABLE
}

object BiometricAuthHelper {

    fun checkBiometricStatus(context: Context): BiometricStatus {
        val manager = BiometricManager.from(context)
        val biometricOnly = manager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
        if (biometricOnly == BiometricManager.BIOMETRIC_SUCCESS) {
            return BiometricStatus.BIOMETRIC_READY
        }
        val deviceCredential = manager.canAuthenticate(DEVICE_CREDENTIAL)
        if (deviceCredential == BiometricManager.BIOMETRIC_SUCCESS) {
            return BiometricStatus.DEVICE_CREDENTIAL_ONLY
        }
        return when (biometricOnly) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            else -> BiometricStatus.HARDWARE_UNAVAILABLE
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        useDeviceCredentialFallback: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onCancelled()
                } else {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // System prompt keeps listening and shows retry feedback automatically
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setConfirmationRequired(false)

        if (useDeviceCredentialFallback) {
            // Note: Android forbids setNegativeButtonText() when DEVICE_CREDENTIAL is included
            builder.setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        } else {
            builder.setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            builder.setNegativeButtonText(negativeButtonText)
        }

        prompt.authenticate(builder.build())
    }
}
