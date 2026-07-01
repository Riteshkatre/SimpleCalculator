package com.riteshkatre.simplecalculator

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.android.gms.ads.AdListener
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.riteshkatre.simplecalculator.databinding.ActivityCalculatorsBinding
import com.riteshkatre.simplecalculator.databinding.ViewNativeAdBinding

class CalculatorsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalculatorsBinding
    private var nativeAd: NativeAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppTheme.applySavedMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivityCalculatorsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.cardGst.setOnClickListener { open(GstCalculatorActivity::class.java) }
        binding.cardPercentage.setOnClickListener { open(PercentageCalculatorActivity::class.java) }
        binding.cardEmi.setOnClickListener { open(EmiCalculatorActivity::class.java) }
        binding.cardUnit.setOnClickListener { open(UnitConverterActivity::class.java) }
        binding.cardAge.setOnClickListener { open(AgeCalculatorActivity::class.java) }
        binding.cardNumberSystem.setOnClickListener { open(NumberSystemConverterActivity::class.java) }
        binding.cardCurrency.setOnClickListener { open(CurrencyConverterActivity::class.java) }
        binding.cardPrivateVault.setOnClickListener { open(PrivateVaultActivity::class.java) }
        loadNativeAd()
    }

    private fun loadNativeAd() {
        val adBinding = ViewNativeAdBinding.inflate(layoutInflater, binding.nativeAdContainer, false)
        binding.nativeAdContainer.removeAllViews()
        binding.nativeAdContainer.addView(adBinding.root)

        AdLoader.Builder(this, getString(R.string.admob_native_unit_id))
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
                adBinding.nativeAdView.headlineView = adBinding.adHeadline
                adBinding.nativeAdView.bodyView = adBinding.adBody
                adBinding.nativeAdView.iconView = adBinding.adIcon
                adBinding.nativeAdView.advertiserView = adBinding.adAdvertiser
                adBinding.nativeAdView.callToActionView = adBinding.adCallToAction
                adBinding.nativeAdView.adChoicesView = adBinding.adChoicesView
                adBinding.adHeadline.text = ad.headline
                adBinding.adBody.text = ad.body ?: ""
                adBinding.adBody.visibility = if (ad.body == null) View.GONE else View.VISIBLE
                ad.icon?.let {
                    adBinding.adIcon.setImageDrawable(it.drawable)
                    adBinding.adIcon.visibility = View.VISIBLE
                } ?: run {
                    adBinding.adIcon.visibility = View.GONE
                }
                adBinding.adAdvertiser.text = ad.advertiser ?: ""
                adBinding.adAdvertiser.visibility = if (ad.advertiser == null) View.GONE else View.VISIBLE
                adBinding.adCallToAction.text = ad.callToAction ?: getString(android.R.string.ok)
                adBinding.adCallToAction.visibility = if (ad.callToAction == null) View.GONE else View.VISIBLE
                adBinding.nativeAdView.setNativeAd(ad)
                binding.nativeAdContainer.visibility = View.VISIBLE
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    binding.nativeAdContainer.visibility = View.GONE
                }
            })
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    private fun open(target: Class<*>) {
        startActivity(Intent(this, target))
    }

    override fun onDestroy() {
        nativeAd?.destroy()
        nativeAd = null
        super.onDestroy()
    }
}
