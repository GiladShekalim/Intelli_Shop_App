package com.example.intellishopapp

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bumptech.glide.Glide
import com.example.intellishopapp.repository.CouponRepository
import com.example.intellishopapp.utilities.ApiResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress

/**
 * Coupons must actually show images. Two layers:
 *  1) DATA: the backend hands the app coupons that carry a usable http image_link.
 *  2) RENDER: Glide (the same loader the cards use) can fetch that URL into a bitmap
 *     on the device — i.e. the image really appears, not just a placeholder.
 */
@RunWith(AndroidJUnit4::class)
class CouponImageTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun backendCoupons_carryImageLinks() = runBlocking {
        val result = CouponRepository().getAllCoupons()
        assertTrue("backend must return coupons", result is ApiResult.Success)
        val coupons = (result as ApiResult.Success).data
        assertTrue("expected a non-empty catalog", coupons.isNotEmpty())
        val withImage = coupons.count { it.image_link?.startsWith("http") == true }
        // The overwhelming majority of coupons must have a real image URL.
        assertTrue(
            "too few coupons have image links: $withImage/${coupons.size}",
            withImage >= coupons.size * 0.9
        )
    }

    @Test
    fun couponImage_actuallyLoadsIntoABitmap() = runBlocking {
        val coupons = (CouponRepository().getAllCoupons() as ApiResult.Success).data
        val url = coupons.firstNotNullOf { c -> c.image_link?.takeIf { it.startsWith("http") } }
        // This one needs the outside internet (the CDN). If the device can't resolve
        // external hosts (offline CI / an emulator started without DNS), skip rather
        // than fail — the image host is not the app's responsibility to provide.
        assumeTrue("no external connectivity; skipping live image load", canResolveHost(url))
        // Glide.submit().get() blocks until the image is decoded or throws on failure —
        // so a returned bitmap proves the coupon image genuinely renders on the device.
        val bitmap = Glide.with(ctx).asBitmap().load(url).submit().get()
        assertTrue("decoded image had no pixels", bitmap is Bitmap && bitmap.width > 0 && bitmap.height > 0)
    }

    private fun canResolveHost(url: String): Boolean = try {
        val host = java.net.URL(url).host
        InetAddress.getByName(host) != null
    } catch (e: Exception) {
        false
    }
}
