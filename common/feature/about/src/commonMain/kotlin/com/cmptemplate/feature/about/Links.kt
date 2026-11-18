package com.cmptemplate.feature.about

/** ControlFix contact points, shared by every app built from this template. */
object ControlFixLinks {
    const val WEBSITE = "https://controlfix.net"
    const val SUPPORT_EMAIL = "support@controlfix.net"
    /** Fallback address of a bug report or suggestion when the reports service cannot be reached. */
    const val BUG_REPORT_EMAIL = "bugreport@controlfix.net"
    /** Shared reports service (Cloudflare Worker): tickets per app id. Contract: AgentsHub tools/reports/README.md. */
    const val REPORTS_BASE = "https://reports.controlfix.net/"
    /** Replace with the product's privacy policy page before release (stores require one). */
    const val PRIVACY = "https://controlfix.net/privacy"
}
