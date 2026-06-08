package com.matias.pomodoro.consent

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class ConsentManager(private val activity: Activity) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    fun requestConsent(onComplete: (Boolean) -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    onComplete(isConsentObtained())
                }
            },
            {
                onComplete(isConsentObtained())
            }
        )
    }

    fun isConsentObtained(): Boolean = consentInformation.canRequestAds()
}
