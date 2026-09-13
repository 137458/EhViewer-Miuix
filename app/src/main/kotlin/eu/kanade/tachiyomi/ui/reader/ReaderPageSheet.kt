package eu.kanade.tachiyomi.ui.reader

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.FileDownloads
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import moe.tarsin.kt.andThen

@Composable
fun ReaderPageSheetMeta(
    retry: () -> Unit,
    retryOrigin: () -> Unit,
    share: () -> Unit,
    copy: () -> Unit,
    save: () -> Unit,
    saveTo: () -> Unit,
    showAds: (() -> Unit)?,
    dismiss: () -> Unit,
) {
    @Composable
    fun Item(icon: ImageVector, @StringRes text: Int, onClick: () -> Unit) = Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(role = Role.Button, onClick = onClick andThen dismiss)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MiuixTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.size(32.dp))
        Text(text = stringResource(id = text), style = MiuixTheme.textStyles.body1)
    }
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        showAds?.let { Item(icon = MiuixIcons.Show, text = R.string.show_blocked_image, onClick = it) }
        Item(icon = MiuixIcons.Refresh, text = R.string.refresh, onClick = retry)
        Item(icon = MiuixIcons.Show, text = R.string.view_original, onClick = retryOrigin)
        Item(icon = MiuixIcons.Share, text = R.string.action_share, onClick = share)
        Item(icon = MiuixIcons.Copy, text = R.string.action_copy, onClick = copy)
        Item(icon = MiuixIcons.Download, text = R.string.action_save, onClick = save)
        Item(icon = MiuixIcons.FileDownloads, text = R.string.action_save_to, onClick = saveTo)
    }
}
