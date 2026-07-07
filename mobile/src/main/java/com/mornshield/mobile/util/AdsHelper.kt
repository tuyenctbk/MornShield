package com.mornshield.mobile.util

import android.app.Activity
import android.content.Context
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import com.mornshield.mobile.R
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.firebase.analytics.FirebaseAnalytics

object AdsHelper {

    private var mInterstitialAd: InterstitialAd? = null
    private var isListeningToLoad = false
    private var appStartTime = System.currentTimeMillis()
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        try {
            MobileAds.initialize(context) {
                loadInterstitial(context.applicationContext)
            }
            incrementOpenCount(context)
        } catch (e: Exception) {}
    }

    private fun incrementOpenCount(context: Context) {
        val prefs = context.getSharedPreferences("mornshield_prefs", Context.MODE_PRIVATE)
        val count = prefs.getInt("app_open_count", 0)
        val firstOpen = prefs.getLong("first_open_time", 0L)
        
        with(prefs.edit()) {
            putInt("app_open_count", count + 1)
            if (firstOpen == 0L) {
                putLong("first_open_time", System.currentTimeMillis())
            }
            apply()
        }
    }

    fun canShowAds(context: Context, isPremium: Boolean): Boolean {
        if (isPremium) return false
        
        val prefs = context.getSharedPreferences("mornshield_prefs", Context.MODE_PRIVATE)
        val openCount = prefs.getInt("app_open_count", 0)
        val firstOpenTime = prefs.getLong("first_open_time", System.currentTimeMillis())
        
        // Logic driven by Remote Config:
        val minDays = RemoteConfigHelper.getAdsMinDays()
        val minOpens = RemoteConfigHelper.getAdsMinOpens()
        val minSessionSeconds = RemoteConfigHelper.getAdsMinSessionSeconds()
        
        val minDaysMs = minDays * 24 * 60 * 60 * 1000
        val isOldEnough = System.currentTimeMillis() - firstOpenTime > minDaysMs
        val hasOpenedEnough = openCount >= minOpens
        val isSessionLongEnough = System.currentTimeMillis() - appStartTime > (minSessionSeconds * 1000)
        
        return hasOpenedEnough && isOldEnough && isSessionLongEnough
    }

    fun loadInterstitial(context: Context) {
        if (mInterstitialAd != null || isListeningToLoad) return
        isListeningToLoad = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            "ca-app-pub-3940256099942544/1033173712", // Interstitial Test ID
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    isListeningToLoad = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mInterstitialAd = null
                    isListeningToLoad = false
                }
            }
        )
    }

    fun showInterstitial(context: Context, isPremium: Boolean) {
        if (!canShowAds(context, isPremium)) return
        val activity = findActivity(context) ?: return
        
        val ad = mInterstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    firebaseAnalytics?.logEvent("ad_interstitial_shown", null)
                }

                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    loadInterstitial(context.applicationContext)
                    firebaseAnalytics?.logEvent("ad_interstitial_dismissed", null)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    mInterstitialAd = null
                    loadInterstitial(context.applicationContext)
                    firebaseAnalytics?.logEvent("ad_interstitial_failed_to_show", null)
                }
            }
            ad.show(activity)
        } else {
            loadInterstitial(context.applicationContext)
        }
    }

    private fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    @Composable
    fun BannerAd(modifier: Modifier = Modifier, isPremium: Boolean = false) {
        val context = LocalContext.current
        if (!canShowAds(context, isPremium)) return

        if (LocalInspectionMode.current) {
            BannerAdPlaceholder(modifier)
            return
        }

        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp

        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(
                        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                            ctx,
                            screenWidth
                        )
                    )
                    adUnitId = "ca-app-pub-3940256099942544/9214589741" // Banner Test ID
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }

    @Composable
    private fun BannerAdPlaceholder(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(58.dp)
                .background(Color(0xFF0F0C20))
                .border(1.dp, Color(0xFFC0B3FF).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("${stringResource(id = R.string.app_name)} Ad", color = Color(0xFFC0B3FF).copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }

    @Composable
    fun NativeAd(modifier: Modifier = Modifier, isPremium: Boolean = false) {
        val context = LocalContext.current
        if (!canShowAds(context, isPremium)) return

        if (LocalInspectionMode.current) {
            NativeAdPlaceholder(modifier)
            return
        }

        var nativeAdState by remember { mutableStateOf<NativeAd?>(null) }
        var loadFailed by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            try {
                val adLoader = AdLoader.Builder(context, "ca-app-pub-3940256099942544/2247696110") // Native Test ID
                    .forNativeAd { ad ->
                        nativeAdState = ad
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            loadFailed = true
                        }
                    })
                    .build()
                adLoader.loadAd(AdRequest.Builder().build())
            } catch (e: Exception) {
                loadFailed = true
            }
        }

        DisposableEffect(nativeAdState) {
            onDispose {
                nativeAdState?.destroy()
            }
        }

        val currentAd = nativeAdState
        if (currentAd != null && !loadFailed) {
            AndroidView(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF15102A).copy(alpha = 0.4f))
                    .border(1.dp, Color(0xFFC0B3FF).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                factory = { ctx ->
                    val adView = NativeAdView(ctx)
                    val container = LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }

                    // Header row
                    val header = LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val titleText = TextView(ctx).apply {
                        setTextColor(AndroidColor.WHITE)
                        textSize = 14f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    adView.headlineView = titleText
                    header.addView(titleText)

                    val badge = TextView(ctx).apply {
                        text = "Ad"
                        setTextColor(AndroidColor.WHITE)
                        textSize = 9f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setBackgroundColor(AndroidColor.parseColor("#7A60FF"))
                        setPadding(12, 4, 12, 4)
                    }
                    header.addView(badge)
                    container.addView(header)

                    // Spacer
                    val spacer = android.view.View(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (8 * ctx.resources.displayMetrics.density).toInt()
                        )
                    }
                    container.addView(spacer)

                    // Body
                    val bodyText = TextView(ctx).apply {
                        setTextColor(AndroidColor.parseColor("#C0B3FF"))
                        textSize = 12f
                    }
                    adView.bodyView = bodyText
                    container.addView(bodyText)

                    adView.addView(container)
                    adView
                },
                update = { adView ->
                    val titleText = adView.headlineView as? TextView
                    val bodyText = adView.bodyView as? TextView

                    titleText?.text = currentAd.headline
                    bodyText?.text = currentAd.body

                    adView.setNativeAd(currentAd)
                }
            )
        } else {
            NativeAdPlaceholder(modifier)
        }
    }

    @Composable
    private fun NativeAdPlaceholder(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF15102A).copy(alpha = 0.5f))
                .border(1.dp, Color(0xFFC0B3FF).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.sponsored_recommendation),
                        color = Color(0xFFC0B3FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF7A60FF), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Ad", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.premium_description),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }

    /**
     * TV-specific premium promotion card that displays a QR code placeholder.
     */
    @Composable
    fun TvPremiumPromoCard(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1F1A3A),
                            Color(0xFF0F0C20)
                        )
                    )
                )
                .border(2.dp, Color(0xFF7A60FF).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "UPGRADE TO MORNSHIELD PREMIUM",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC0B3FF),
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Unlock live sleep cycle sync & full task checklists on Android TV.",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scan the QR code on the right with your phone to subscribe instantly.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.width(32.dp))
                
                // Mock QR Code
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Simple QR pattern simulation
                    Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                        repeat(5) { rowIndex ->
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                repeat(5) { colIndex ->
                                    val isFilled = (rowIndex + colIndex) % 2 == 0 || (rowIndex == 0 && colIndex == 0) || (rowIndex == 4 && colIndex == 4) || (rowIndex == 0 && colIndex == 4) || (rowIndex == 4 && colIndex == 0)
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(if (isFilled) Color.Black else Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
