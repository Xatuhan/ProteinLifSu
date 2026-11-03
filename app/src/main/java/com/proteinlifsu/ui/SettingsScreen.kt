package com.proteinlifsu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proteinlifsu.BuildConfig
import com.proteinlifsu.domain.AppLanguage
import com.proteinlifsu.domain.AppSettings
import com.proteinlifsu.domain.Fmt
import com.proteinlifsu.domain.ThemeMode
import com.proteinlifsu.ui.components.Hairline
import com.proteinlifsu.ui.components.IconAction
import com.proteinlifsu.ui.components.ScreenTopBar
import com.proteinlifsu.ui.components.SectionLabel
import com.proteinlifsu.ui.components.SegmentedPills
import com.proteinlifsu.ui.i18n.LocalStrings
import com.proteinlifsu.ui.theme.AppIcons
import com.proteinlifsu.ui.theme.NumStyle
import com.proteinlifsu.ui.theme.Palette

@Composable
fun SettingsScreen(
    viewModel: TrackerViewModel,
    settings: AppSettings,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val s = LocalStrings.current
    val foods by viewModel.customFoods.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = SidePadding,
            end = SidePadding,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item(key = "top") { ScreenTopBar(s.settings, s.back, onBack) }

        item(key = "lang") {
            Column(Modifier.padding(vertical = 16.dp)) {
                SectionLabel(s.language)
                Spacer(Modifier.height(10.dp))
                // Dil adları her zaman kendi dilinde yazılır.
                SegmentedPills(
                    options = AppLanguage.entries,
                    selected = settings.language,
                    label = { if (it == AppLanguage.TR) "Türkçe" else "English" },
                    accent = Palette.textPrimary,
                    onSelect = viewModel::setLanguage,
                    modifier = Modifier.fillMaxWidth(),
                    itemWidth = null,
                )
            }
        }
        item(key = "theme") {
            Column(Modifier.padding(bottom = 16.dp)) {
                SectionLabel(s.theme)
                Spacer(Modifier.height(10.dp))
                SegmentedPills(
                    options = ThemeMode.entries,
                    selected = settings.theme,
                    label = {
                        when (it) {
                            ThemeMode.SYSTEM -> s.themeSystem
                            ThemeMode.LIGHT -> s.themeLight
                            ThemeMode.DARK -> s.themeDark
                        }
                    },
                    accent = Palette.textPrimary,
                    onSelect = viewModel::setTheme,
                    modifier = Modifier.fillMaxWidth(),
                    itemWidth = null,
                )
            }
        }
        item(key = "sep1") { Hairline() }
        item(key = "foods-title") { SectionLabel(s.myFoods, Modifier.padding(top = 16.dp, bottom = 8.dp)) }
        if (foods.isEmpty()) {
            item(key = "foods-empty") { Text(s.noCustomFoods, style = NumStyle.Small, modifier = Modifier.padding(bottom = 16.dp)) }
        }
        items(foods, key = { it.id }, contentType = { "custom" }) { f ->
            Row(Modifier.fillMaxWidth().animateItem(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(f.nameTr, style = NumStyle.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        s.per100(Fmt.grams(f.proteinPer100g, s.sep), Fmt.grams(f.fiberPer100g, s.sep)),
                        style = NumStyle.Small,
                    )
                    f.noteTr?.let { Text(it, style = NumStyle.Small.copy(color = Palette.textMuted), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                IconAction(AppIcons.Trash, "${s.delete}: ${f.nameTr}", { viewModel.deleteCustomFood(f.id) }, tint = Palette.textMuted)
            }
        }
        item(key = "sep2") { Hairline(Modifier.padding(top = 8.dp)) }
        item(key = "about") {
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel(s.dataSource)
                Text(s.dataSourceBody, style = NumStyle.Small)
                Spacer(Modifier.height(10.dp))
                SectionLabel(s.privacy)
                Text(s.privacyBody, style = NumStyle.Small)
                Spacer(Modifier.height(10.dp))
                Text("v${BuildConfig.VERSION_NAME}", style = NumStyle.Small.copy(color = Palette.textMuted))
            }
        }
    }
}
