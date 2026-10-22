package com.android.skip.ui.whitelist

/** A conservative local catalogue, not a country detector or a security boundary. */
object AutomaticWhitelist {
    enum class Reason { ANDROID_PREFIX, KNOWN_OVERSEAS_APP }

    // Dot boundaries prevent com.googlefake from matching com.google.
    private val vendorPrefixes = listOf(
        "com.google.", "com.microsoft.", "com.apple.", "com.adobe.", "com.amazon.",
        "com.facebook.", "com.instagram.", "com.spotify.", "com.netflix.",
        "org.mozilla.", "com.dropbox.", "com.protonvpn.", "ch.protonmail.",
        "me.proton.", "proton.android.", "com.openai.", "com.anthropic.",
        "com.github.", "com.valvesoftware."
    )

    private val knownPackages = setOf(
        "com.google.android.gm", "com.google.android.youtube", "com.google.android.apps.youtube.music",
        "com.android.chrome", "com.google.android.apps.maps", "com.google.android.apps.photos",
        "com.google.android.apps.docs", "com.google.android.apps.docs.editors.docs",
        "com.google.android.apps.docs.editors.sheets", "com.google.android.apps.docs.editors.slides",
        "com.google.android.calendar", "com.google.android.keep", "com.google.android.apps.translate",
        "com.google.android.googlequicksearchbox", "com.google.android.apps.bard",
        "com.google.android.apps.authenticator2", "com.google.android.apps.meetings",
        "com.google.android.apps.tachyon", "com.google.android.apps.messaging",
        "com.whatsapp", "com.whatsapp.w4b", "com.instagram.android",
        "com.facebook.katana", "com.facebook.lite", "com.facebook.orca", "com.instagram.barcelona",
        "com.twitter.android", "org.telegram.messenger", "org.telegram.messenger.web",
        "org.thoughtcrime.securesms", "com.discord", "com.reddit.frontpage",
        "com.spotify.music", "com.netflix.mediaclient", "com.amazon.avod.thirdpartyclient",
        "com.amazon.mShop.android.shopping", "com.amazon.kindle",
        "com.microsoft.emmx", "com.microsoft.office.outlook", "com.microsoft.teams",
        "com.microsoft.skydrive", "com.microsoft.office.word", "com.microsoft.office.excel",
        "com.microsoft.office.powerpoint", "org.mozilla.firefox", "org.mozilla.focus",
        "com.brave.browser", "com.openai.chatgpt", "com.anthropic.claude",
        "com.apple.movetoios", "com.azure.authenticator", "com.skype.raider",
        "com.linkedin.android", "com.linkedin.android.learning", "com.mojang.minecraftpe",
        "com.swiftkey.swiftkeyconfigurator", "tv.twitch.android.app",
        "com.audible.application", "com.imdb.mobile", "com.snapchat.android",
        "com.pinterest", "com.duckduckgo.mobile.android", "com.bitwarden.mobile",
        "com.onepassword.android", "com.agilebits.onepassword", "com.evernote",
        "com.trello", "com.asana.app", "com.atlassian.android.jira.core",
        "com.atlassian.android.confluence.core", "com.canva.editor",
        "com.figma.mirror",
        "com.airbnb.android", "com.booking", "com.ubercab", "com.ubercab.eats",
        "com.ebay.mobile", "com.paypal.android.p2pmobile", "com.strava",
        "com.garmin.android.apps.connectmobile", "com.fitbit.FitbitMobile",
        "org.khanacademy.android", "org.coursera.android", "com.udemy.android",
        "com.perplexity.ai.android", "com.medium.reader", "com.grammarly.android.keyboard",
        "com.todoist", "com.anydo", "com.automattic.simplenote",
        "notion.id", "com.todoist", "com.Slack", "us.zoom.videomeetings",
        "com.dropbox.android", "com.github.android", "com.duolingo", "com.valvesoftware.android.steam.community"
    )

    fun reason(packageName: String): Reason? = when {
        packageName.startsWith("com.android.") -> Reason.ANDROID_PREFIX
        packageName in knownPackages || vendorPrefixes.any { packageName.startsWith(it) } -> Reason.KNOWN_OVERSEAS_APP
        else -> null
    }

    fun select(installedPackages: Collection<String>): Set<String> =
        installedPackages.filterTo(mutableSetOf()) { reason(it) != null }
}
