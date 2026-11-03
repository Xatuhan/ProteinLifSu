package com.proteinlifsu.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * İkon kütüphanesi yok: gereken birkaç ikon 24x24 ızgarada elle çizilmiş ImageVector'dür.
 * Hepsi 1.8 birim çizgi kalınlığıyla, uçları yuvarlak.
 */
object AppIcons {

    private fun stroked(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        ).build()

    val ChevronLeft: ImageVector by lazy {
        stroked("ChevronLeft") { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) }
    }

    val ChevronRight: ImageVector by lazy {
        stroked("ChevronRight") { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }
    }

    val Close: ImageVector by lazy {
        stroked("Close") {
            moveTo(6.5f, 6.5f); lineTo(17.5f, 17.5f)
            moveTo(17.5f, 6.5f); lineTo(6.5f, 17.5f)
        }
    }

    val Plus: ImageVector by lazy {
        stroked("Plus") {
            moveTo(12f, 5f); lineTo(12f, 19f)
            moveTo(5f, 12f); lineTo(19f, 12f)
        }
    }

    val Search: ImageVector by lazy {
        stroked("Search") {
            // r=6 daire, merkez (10.5, 10.5)
            moveTo(16.5f, 10.5f)
            arcTo(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 4.5f, y1 = 10.5f)
            arcTo(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 16.5f, y1 = 10.5f)
            moveTo(15f, 15f); lineTo(20f, 20f)
        }
    }

    /** Su damlası: üstte sivri uç, altta yarım daire. */
    val Drop: ImageVector by lazy {
        stroked("Drop") {
            moveTo(12f, 3.5f)
            curveTo(12f, 3.5f, 6f, 10f, 6f, 14f)
            arcTo(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 18f, y1 = 14f)
            curveTo(18f, 10f, 12f, 3.5f, 12f, 3.5f)
            close()
        }
    }

    val Calendar: ImageVector by lazy {
        stroked("Calendar") {
            moveTo(6f, 5.5f); lineTo(18f, 5.5f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 20f, y1 = 7.5f)
            lineTo(20f, 18f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 18f, y1 = 20f)
            lineTo(6f, 20f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 4f, y1 = 18f)
            lineTo(4f, 7.5f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 6f, y1 = 5.5f)
            close()
            moveTo(4f, 10f); lineTo(20f, 10f)
            moveTo(8.5f, 3.5f); lineTo(8.5f, 7f)
            moveTo(15.5f, 3.5f); lineTo(15.5f, 7f)
        }
    }

    /** Ayarlar: üç yatay kaydırıcı (dişli çark yerine sade bir işaret). */
    val Sliders: ImageVector by lazy {
        stroked("Sliders") {
            moveTo(4f, 7f); lineTo(20f, 7f)
            moveTo(4f, 12f); lineTo(20f, 12f)
            moveTo(4f, 17f); lineTo(20f, 17f)
            moveTo(9f, 5f); lineTo(9f, 9f)
            moveTo(15f, 10f); lineTo(15f, 14f)
            moveTo(7f, 15f); lineTo(7f, 19f)
        }
    }

    val Trash: ImageVector by lazy {
        stroked("Trash") {
            moveTo(4.5f, 7f); lineTo(19.5f, 7f)
            moveTo(9.5f, 7f); lineTo(9.5f, 4.5f); lineTo(14.5f, 4.5f); lineTo(14.5f, 7f)
            moveTo(6.5f, 7f); lineTo(7.5f, 19.5f); lineTo(16.5f, 19.5f); lineTo(17.5f, 7f)
            moveTo(10.5f, 10.5f); lineTo(10.5f, 16.5f)
            moveTo(13.5f, 10.5f); lineTo(13.5f, 16.5f)
        }
    }
}
