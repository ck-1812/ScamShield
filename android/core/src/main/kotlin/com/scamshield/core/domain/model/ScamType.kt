package com.scamshield.core.domain.model

enum class ScamType(val displayName: String, val baseWeight: Int, val description: String) {
    AUTHORITY_IMPERSONATION(
        displayName = "Authority Impersonation",
        baseWeight = 20,
        description = "Caller claims to be from CBI, Police, RBI, Customs, or Cyber Crime Department."
    ),
    POLICE_CBI_RBI_IMPERSONATION(
        displayName = "Law Enforcement Claim",
        baseWeight = 20,
        description = "Explicit assertion of police or investigative agency jurisdiction."
    ),
    LEGAL_THREAT(
        displayName = "Threat of Arrest / Legal Action",
        baseWeight = 20,
        description = "Threatens non-bailable arrest warrant, police raid, or court summons."
    ),
    IDENTITY_DOCUMENT_THREAT(
        displayName = "Aadhaar / Identity Accusation",
        baseWeight = 15,
        description = "Alleges victim's Aadhaar/PAN is linked to illegal parcel or money laundering."
    ),
    SECRECY(
        displayName = "Coerced Secrecy",
        baseWeight = 15,
        description = "Commands victim not to disclose call to family, friends, or local police."
    ),
    VIDEO_CONFINEMENT(
        displayName = "Video Confinement / Digital Arrest",
        baseWeight = 15,
        description = "Forces victim to remain on continuous video call with camera on."
    ),
    FINANCIAL_REQUEST(
        displayName = "Money Transfer Demand",
        baseWeight = 30,
        description = "Demands urgent funds transfer or security deposit."
    ),
    SAFE_ACCOUNT_SCAM(
        displayName = "Safe Verification Account",
        baseWeight = 25,
        description = "Instructs victim to move money to a 'government safe account' for clearance."
    ),
    CREDENTIAL_REQUEST(
        displayName = "Credential / OTP Request",
        baseWeight = 30,
        description = "Solicits OTP, UPI PIN, ATM PIN, or banking passwords."
    ),
    REMOTE_ACCESS_REQUEST(
        displayName = "Remote Screen Access",
        baseWeight = 25,
        description = "Instructs installation of AnyDesk, TeamViewer, or screen sharing tools."
    ),
    URGENCY(
        displayName = "Artificial Urgency",
        baseWeight = 10,
        description = "Pressures immediate action within minutes to prevent reasoned thought."
    ),
    PAYMENT_LINK(
        displayName = "Suspicious Payment Link / QR",
        baseWeight = 20,
        description = "Pushes payment link, collect request, or QR code scan."
    )
}
