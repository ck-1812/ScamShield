package com.scamshield.core.ai.signal

import com.scamshield.core.domain.model.ScamType

data class RulePattern(
    val type: ScamType,
    val regex: Regex,
    val baseSeverity: Int,
    val confidence: Float,
    val description: String
)

object IndianScamRules {

    val rules: List<RulePattern> = listOf(
        // 1. AUTHORITY_IMPERSONATION & POLICE_CBI_RBI
        RulePattern(
            type = ScamType.AUTHORITY_IMPERSONATION,
            regex = Regex(
                """\b(cbi|central bureau of investigation|crime branch|cyber crime (cell|department|branch)|customs officer|narcotics control bureau|ncb|enforcement directorate|ed department|income tax officer|rbi( officer)?|reserve bank of india|supreme court|trai|telecom regulatory)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 20,
            confidence = 0.90f,
            description = "Indian government enforcement agency impersonation"
        ),
        RulePattern(
            type = ScamType.AUTHORITY_IMPERSONATION,
            regex = Regex(
                """\b(cbi (cyber crime department )?se bol raha|cbi se bol raha|police station se|crime branch se|customs department se|income tax se)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 20,
            confidence = 0.92f,
            description = "Hindi authority impersonation"
        ),
        RulePattern(
            type = ScamType.POLICE_CBI_RBI_IMPERSONATION,
            regex = Regex(
                """\b(police officer|inspector|sub-inspector|sho|deputy commissioner|senior inspector sharma|delhi police|mumbai police)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 20,
            confidence = 0.88f,
            description = "Police authority impersonation"
        ),

        // 2. LEGAL THREAT & ARREST
        RulePattern(
            type = ScamType.LEGAL_THREAT,
            regex = Regex(
                """\b(arrest(ed)?|arrest warrant|non-bailable warrant|police will reach|case registered|fir lodged|pmla|prevention of money laundering|court summons|legal action|jail|custody)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 20,
            confidence = 0.92f,
            description = "Threat of arrest or criminal proceedings"
        ),
        RulePattern(
            type = ScamType.LEGAL_THREAT,
            regex = Regex(
                """\b(arrest kiya jayega|arrest ho jayega|police bhej rahe hain|fir darj hui hai|jail hogi|police pakad legi|warrant nikla hai)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 20,
            confidence = 0.94f,
            description = "Hindi legal and arrest threat"
        ),

        // 3. IDENTITY DOCUMENT THREAT (Aadhaar / PAN / Parcel)
        RulePattern(
            type = ScamType.IDENTITY_DOCUMENT_THREAT,
            regex = Regex(
                """\b(aadhaar( card)?|pan card|passport|identity linked|illegal transaction|illegal parcel|intercepted at (customs|airport)|drugs found in parcel|money laundering)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 15,
            confidence = 0.88f,
            description = "Identity document compromise accusation"
        ),
        RulePattern(
            type = ScamType.IDENTITY_DOCUMENT_THREAT,
            regex = Regex(
                """\b(aadhaar card se|pan card se|parcel mein illegal|aapke naam se account|black money linked)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 15,
            confidence = 0.90f,
            description = "Hindi Aadhaar/PAN misuse accusation"
        ),

        // 4. COERCED SECRECY
        RulePattern(
            type = ScamType.SECRECY,
            regex = Regex(
                """\b(do not tell (anyone|anybody|(your )?family)|don't (tell|inform) (your )?family|strictly confidential|keep this secret|secret investigation|this is secret|national security matter|do not discuss with anyone|tell nobody)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 15,
            confidence = 0.93f,
            description = "Coercive secrecy requirement"
        ),
        RulePattern(
            type = ScamType.SECRECY,
            regex = Regex(
                """\b(kisi ko (phone|call|batana) mat|kisi ko mat batana|parivar ko mat batao|ghar par mat bolna|secret investigation hai|confidential mamla hai)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 15,
            confidence = 0.95f,
            description = "Hindi secrecy coercion"
        ),

        // 5. VIDEO CONFINEMENT ("Digital Arrest")
        RulePattern(
            type = ScamType.VIDEO_CONFINEMENT,
            regex = Regex(
                """\b(stay on (the |this )?video call|digital custody|digital arrest|do not disconnect|keep (your )?camera on|don't leave the room|look at the screen|video call verification)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 15,
            confidence = 0.95f,
            description = "Forced video presence / Digital Arrest confinement"
        ),
        RulePattern(
            type = ScamType.VIDEO_CONFINEMENT,
            regex = Regex(
                """\b(video call mat cut karna|camera on rakho|screen se hatna mat|digital arrest ho aap|call mat kaatna)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 15,
            confidence = 0.95f,
            description = "Hindi video confinement command"
        ),

        // 6. FINANCIAL REQUEST
        RulePattern(
            type = ScamType.FINANCIAL_REQUEST,
            regex = Regex(
                """\b(transfer (money|funds|.*rupees|.*lakh)|send (₹|rs\.?|rupees|lakh)|security deposit|clearance fee|verification deposit|pay fine|transfer funds|penalty payment)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 30,
            confidence = 0.91f,
            description = "Direct financial transfer solicitation"
        ),
        RulePattern(
            type = ScamType.FINANCIAL_REQUEST,
            regex = Regex(
                """\b(paise transfer karo|paisa transfer karo|paise bhej do|lakh rupaye transfer|fine bharna padega|deposit jama karo)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 30,
            confidence = 0.92f,
            description = "Hindi money transfer demand"
        ),

        // 7. SAFE ACCOUNT SCAM
        RulePattern(
            type = ScamType.SAFE_ACCOUNT_SCAM,
            regex = Regex(
                """\b(safe (rbi )?account|government verification account|clearing account|security holding account|refundable after audit|safe account mein)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 25,
            confidence = 0.94f,
            description = "Fraudulent 'Safe Government Account' transfer pitch"
        ),
        RulePattern(
            type = ScamType.SAFE_ACCOUNT_SCAM,
            regex = Regex(
                """\b(safe account mein|rbi account mein paise|audit ke baad wapas|sarkari account)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 25,
            confidence = 0.94f,
            description = "Hindi safe account lure"
        ),

        // 8. CREDENTIAL / OTP REQUEST
        RulePattern(
            type = ScamType.CREDENTIAL_REQUEST,
            regex = Regex(
                """\b(otp|one time password|upi pin|atm pin|cvv|net banking password|share the code|6-digit code)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 30,
            confidence = 0.96f,
            description = "Sensitive credential harvesting"
        ),
        RulePattern(
            type = ScamType.CREDENTIAL_REQUEST,
            regex = Regex(
                """\b(otp bataiye|otp share karo|upi pin daliye|pin enter karo|code bata do)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 30,
            confidence = 0.96f,
            description = "Hindi credential harvesting"
        ),

        // 9. REMOTE ACCESS REQUEST
        RulePattern(
            type = ScamType.REMOTE_ACCESS_REQUEST,
            regex = Regex(
                """\b(anydesk|teamviewer|quicksupport|rustdesk|install (the )?app|share (your )?screen|grant remote access|9-digit code)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 25,
            confidence = 0.95f,
            description = "Remote desktop application coercion"
        ),
        RulePattern(
            type = ScamType.REMOTE_ACCESS_REQUEST,
            regex = Regex(
                """\b(anydesk download karo|screen share karo|app install kijiye|remote support)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 25,
            confidence = 0.95f,
            description = "Hindi remote access request"
        ),

        // 10. ARTIFICIAL URGENCY
        RulePattern(
            type = ScamType.URGENCY,
            regex = Regex(
                """\b(immediately|right now|within 10 minutes|within 1 hour|act now|last chance|urgently|account will be blocked|sim will be deactivated)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 10,
            confidence = 0.82f,
            description = "Manufactured urgency to bypass victim scrutiny"
        ),
        RulePattern(
            type = ScamType.URGENCY,
            regex = Regex(
                """\b(turant|abhi ke abhi|ek ghante mein|service block ho jayegi|sim band ho jayega)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 10,
            confidence = 0.85f,
            description = "Hindi artificial urgency"
        ),

        // 11. PAYMENT LINK / QR REQUEST
        RulePattern(
            type = ScamType.PAYMENT_LINK,
            regex = Regex(
                """\b(scan (this )?qr|payment link|click this link|upi collect request|refund link)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 20,
            confidence = 0.90f,
            description = "Suspicious payment link or QR code lure"
        ),
        RulePattern(
            type = ScamType.PAYMENT_LINK,
            regex = Regex(
                """\b(qr code scan karo|link par click karo|paise mangane ka link)\b""",
                RegexOption.IGNORE_CASE
            ),
            baseSeverity = 20,
            confidence = 0.91f,
            description = "Hindi QR/link demand"
        )
    )
}
