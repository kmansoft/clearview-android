package org.kman.clearview.util

import android.annotation.TargetApi
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils

abstract class LayoutCompat {
    companion object {
        fun factory(): LayoutCompat {
            if (Build.VERSION.SDK_INT >= 23) {
                return LayoutCompat_api23()
            }
            return LayoutCompat_base()
        }
    }

    abstract fun create(text: String, paint: TextPaint, width: Int): StaticLayout

    @Suppress("DEPRECATION")
    class LayoutCompat_base : LayoutCompat() {
        override fun create(text: String, paint: TextPaint, width: Int): StaticLayout {
            /**
            public StaticLayout (CharSequence source,
            int bufstart,
            int bufend,
            TextPaint paint,
            int outerwidth,
            Layout.Alignment align,
            float spacingmult,
            float spacingadd,
            boolean includepad,
            TextUtils.TruncateAt ellipsize,
            int ellipsizedWidth)
             */
            return StaticLayout(
                text, 0, text.length,
                paint, width,
                Layout.Alignment.ALIGN_NORMAL,
                1.0f, 0.0f, true, TextUtils.TruncateAt.END, 0
            )
        }
    }

    @TargetApi(23)
    class LayoutCompat_api23 : LayoutCompat() {
        override fun create(text: String, paint: TextPaint, width: Int): StaticLayout {
            val builder = StaticLayout.Builder.obtain(
                text, 0, text.length,
                paint, width
            )
            builder.setEllipsize(TextUtils.TruncateAt.END)
            return builder.build()
        }
    }
}

