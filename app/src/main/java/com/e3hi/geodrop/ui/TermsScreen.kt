package com.e3hi.geodrop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape
import com.e3hi.geodrop.R

@Composable
internal fun TermsAcceptanceScreen(
    onAccept: () -> Unit,
    onExit: () -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableStateOf(0) }
    val agreementText = remember(selectedTab) {
        if (selectedTab == 0) TERMS_OF_SERVICE_TEXT else PRIVACY_POLICY_TEXT
    }

    LaunchedEffect(selectedTab) {
        scrollState.scrollTo(0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Welcome to GeoDrop",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = TERMS_PRIVACY_SUMMARY,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider()
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TERMS_PRIVACY_TABS.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 360.dp)
                        .verticalScroll(scrollState)
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = agreementText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                }
                Text(
                    text = "By tapping Accept & Continue you agree to these terms and policies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onExit) {
                        Text("Exit app")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onAccept) {
                        Text("Accept & Continue")
                    }
                }
            }
        }
    }
}

@Composable
internal fun TermsPrivacyDialog(
    initialTab: Int,
    onDismiss: () -> Unit
) {
    val tabCount = TERMS_PRIVACY_TABS.size
    var selectedTab by remember { mutableStateOf(initialTab.coerceIn(0, tabCount - 1)) }
    val scrollState = rememberScrollState()
    val agreementText = remember(selectedTab) {
        if (selectedTab == 0) TERMS_OF_SERVICE_TEXT else PRIVACY_POLICY_TEXT
    }

    LaunchedEffect(initialTab) {
        val clamped = initialTab.coerceIn(0, tabCount - 1)
        selectedTab = clamped
    }

    LaunchedEffect(selectedTab) {
        scrollState.scrollTo(0)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.terms_privacy_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = TERMS_PRIVACY_SUMMARY,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider()
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TERMS_PRIVACY_TABS.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 360.dp)
                        .verticalScroll(scrollState)
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = agreementText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.terms_privacy_dialog_close))
                    }
                }
            }
        }
    }
}

private val TERMS_PRIVACY_TABS = listOf(
    "Terms of Service",
    "Privacy Policy"
)

private const val TERMS_PRIVACY_SUMMARY =
    "GeoDrop uses your location and saved preferences to help you discover nearby drops. " +
            "Use the tabs below to review our Terms of Service and Privacy Policy, then accept to continue."

private val TERMS_OF_SERVICE_TEXT = """
📜 GeoDrop – Terms of Service
Last updated: 10/02/2025
Welcome to GeoDrop! By using our app, you agree to the following:

1. Use of the App
You may use GeoDrop to create and discover location-based messages, media, or coupons (“drops”).
You agree not to post harmful, illegal, hateful, or malicious content.
You agree not to spam, harass, or misuse the service.

2. Location Services
GeoDrop uses your device’s location to notify you of nearby drops.
You must grant location permissions for the app to function properly.

3. User Content
You are responsible for any content you drop.
GeoDrop may remove content that violates these terms.
Coupons, promotions, or offers from businesses are managed by those businesses — GeoDrop is not responsible for their accuracy or fulfillment.

4. NSFW Content
GeoDrop includes an optional NSFW (Not Safe For Work) feature.
By enabling NSFW mode, you confirm you are at least 18 years old (or the age of majority in your country).
NSFW content may include mature, adult, or offensive material.
GeoDrop is not responsible for the nature of user-generated NSFW content.
Businesses are prohibited from posting NSFW coupons or promotions.

5. Accounts & Data
GeoDrop collects only the information needed to operate the service, such as your device ID, location (while you use the app), and any drops you create.
We never sell your personal data.
You may delete your account at any time from the in-app settings.

6. Business Accounts
Business users must keep their account information up to date.
Business users are responsible for honoring coupons or offers they distribute through GeoDrop.
GeoDrop may suspend business accounts that violate these terms or applicable laws.

7. Changes to the Terms
We may update these terms from time to time. If the changes are material, we'll notify you in the app.
Continuing to use GeoDrop after an update means you accept the revised terms.

8. Contact
Questions? Reach us at support@geodrop.app.

📍 Location Notice
GeoDrop relies on accurate GPS data. Turn on high accuracy mode for the best experience.

📢 Notifications
GeoDrop may send push notifications about nearby drops, reminders, or account updates. You can opt out in your device settings.

🔒 Data Security
We use industry-standard safeguards to protect your data. However, we cannot guarantee the security of information transmitted over the internet.

By accepting, you agree to follow these terms whenever you use GeoDrop.
""".trimIndent()

private val PRIVACY_POLICY_TEXT = """
🔐 GeoDrop – Privacy Policy
Last updated: 10/02/2025

1. Information We Collect
• Account basics: email address for explorer and business accounts.
• Location data: precise GPS coordinates while you use key features like the map and background alerts for nearby drops.
• Content you provide: text, media, and coupons that you create or redeem.
• Device data: app version, device model, and crash diagnostics.

2. How We Use Information
• Deliver core features such as finding and unlocking drops near you.
• Maintain the safety of the community by moderating content and preventing abuse.
• Provide analytics to improve app performance and plan future features.
• Notify you about nearby drops, redemption reminders, or account changes.

3. Sharing & Disclosure
• We never sell your personal information.
• Drop content is shared with other explorers in the area based on your privacy settings.
• Business partners only see analytics for drops they create.
• Service providers (like cloud hosting and crash reporting) process data on our behalf under strict confidentiality agreements.

4. Your Choices
• You can disable push notifications or location permissions at any time in system settings.
• You may delete drops you posted or remove collected notes from your inventory.
• Request account deletion or data export by emailing support@geodrop.app.

5. Data Retention
• Account and drop data are retained as long as your account is active.
• We keep minimal logs for fraud prevention, typically no longer than 30 days.

6. Children’s Privacy
• GeoDrop is not intended for children under 13.
• NSFW mode is restricted to users 18+ only.

7. Updates
• We will notify you in-app or via email (for business accounts) when this policy changes.

By accepting, you acknowledge that you have read and understood how GeoDrop handles your data.
""".trimIndent()