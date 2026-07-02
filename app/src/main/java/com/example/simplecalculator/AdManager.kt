package com.riteshkatre.simplecalculator

import android.app.Activity
import android.content.Context
import android.widget.FrameLayout
import android.view.ViewGroup
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

object AdManager {

    private var initialized = false
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var appOpenLoading = false

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        MobileAds.initialize(context) {
            loadInterstitial(context)
            loadRewarded(context)
            loadAppOpen(context)
        }
    }

    fun loadInterstitial(context: Context) {
        InterstitialAd.load(
            context,
            context.getString(R.string.admob_interstitial_unit_id),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun loadBanner(activity: Activity, container: ViewGroup) {
        val bannerCard = MaterialCardView(activity).apply {
            cardElevation = 0f
            useCompatPadding = true
            setCardBackgroundColor(ContextCompat.getColor(activity, R.color.card_background))
            strokeWidth = (activity.resources.displayMetrics.density * 1f).toInt()
            strokeColor = ContextCompat.getColor(activity, R.color.secondary_text)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val adView = AdView(activity)
        adView.adUnitId = activity.getString(R.string.admob_banner_unit_id)
        bannerCard.addView(
            adView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
        container.removeAllViews()
        container.addView(bannerCard)
        container.post {
            val widthDp = (container.width / activity.resources.displayMetrics.density).toInt().coerceAtLeast(320)
            adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp))
            adView.loadAd(AdRequest.Builder().build())
        }
    }

    fun showInterstitial(activity: Activity, onFinished: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            loadInterstitial(activity.applicationContext)
            onFinished()
            return
        }

        interstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadInterstitial(activity.applicationContext)
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                loadInterstitial(activity.applicationContext)
                onFinished()
            }
        }
        ad.show(activity)
    }

    fun loadRewarded(context: Context) {
        RewardedAd.load(
            context,
            context.getString(R.string.admob_rewarded_unit_id),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }

    fun showRewarded(
        activity: Activity,
        onRewardEarned: (RewardItem) -> Unit,
        onFinished: () -> Unit = {}
    ): Boolean {
        val ad = rewardedAd
        if (ad == null) {
            loadRewarded(activity.applicationContext)
            onFinished()
            return false
        }

        rewardedAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadRewarded(activity.applicationContext)
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                loadRewarded(activity.applicationContext)
                onFinished()
            }
        }
        ad.show(activity) { rewardItem ->
            onRewardEarned(rewardItem)
        }
        return true
    }

    fun loadAppOpen(context: Context) {
        if (appOpenLoading || appOpenAd != null) return
        appOpenLoading = true
        AppOpenAd.load(
            context,
            context.getString(R.string.admob_app_open_unit_id),
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenLoading = false
                    appOpenAd = ad
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    appOpenLoading = false
                    appOpenAd = null
                }
            }
        )
    }

    fun showAppOpen(activity: Activity, onFinished: () -> Unit): Boolean {
        val ad = appOpenAd
        if (ad == null) {
            loadAppOpen(activity.applicationContext)
            return false
        }

        appOpenAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadAppOpen(activity.applicationContext)
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                loadAppOpen(activity.applicationContext)
                onFinished()
            }
        }
        ad.show(activity)
        return true
    }
}
