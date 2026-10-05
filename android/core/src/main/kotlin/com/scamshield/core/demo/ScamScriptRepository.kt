package com.scamshield.core.demo

import com.scamshield.core.domain.model.ScamType

object ScamScriptRepository {

    val digitalArrestScenario = DemoScenario(
        scenarioId = "digital_arrest_cbi",
        title = "Digital Arrest Scam (Fake CBI)",
        description = "Target is falsely accused of money laundering, ordered into video confinement, and coerced to transfer 2 lakhs to a 'safe RBI account'.",
        category = "DIGITAL_ARREST",
        turns = listOf(
            DemoTurn(
                step = 1,
                speaker = "Caller (Fake CBI)",
                text = "Hello, I am calling from the CBI cyber crime department.",
                expectedSignals = listOf(ScamType.AUTHORITY_IMPERSONATION),
                delayMs = 2500L
            ),
            DemoTurn(
                step = 2,
                speaker = "Caller (Fake CBI)",
                text = "Your Aadhaar has been linked to a money laundering case.",
                expectedSignals = listOf(ScamType.IDENTITY_DOCUMENT_THREAT),
                delayMs = 2500L
            ),
            DemoTurn(
                step = 3,
                speaker = "Caller (Fake CBI)",
                text = "You will be arrested if you do not cooperate.",
                expectedSignals = listOf(ScamType.LEGAL_THREAT),
                delayMs = 2500L
            ),
            DemoTurn(
                step = 4,
                speaker = "Caller (Fake CBI)",
                text = "Do not tell your family about this investigation.",
                expectedSignals = listOf(ScamType.SECRECY),
                delayMs = 2500L
            ),
            DemoTurn(
                step = 5,
                speaker = "Caller (Fake CBI)",
                text = "Stay on this video call.",
                expectedSignals = listOf(ScamType.VIDEO_CONFINEMENT),
                delayMs = 2500L
            ),
            DemoTurn(
                step = 6,
                speaker = "Caller (Fake CBI)",
                text = "You need to transfer two lakh rupees to a safe RBI account immediately.",
                expectedSignals = listOf(ScamType.FINANCIAL_REQUEST, ScamType.SAFE_ACCOUNT_SCAM, ScamType.URGENCY),
                delayMs = 3000L
            )
        )
    )

    val legitBankScenario = DemoScenario(
        scenarioId = "legit_bank_call",
        title = "Legitimate Call (Bank KYC)",
        description = "Normal bank call informing user of branch KYC hours. Demonstrates that ScamShield avoids false positives on benign calls.",
        category = "LEGITIMATE",
        turns = listOf(
            DemoTurn(
                step = 1,
                speaker = "Caller (City Bank)",
                text = "Hello, this is City Bank calling. Your branch is open until 5 PM today for KYC updates. No action is required over the phone.",
                expectedSignals = emptyList(),
                delayMs = 2500L
            )
        )
    )

    val fakePoliceHindiScenario = DemoScenario(
        scenarioId = "fake_police_hindi",
        title = "Police (Hindi / Hinglish)",
        description = "Hindi speaking scammer posing as CBI cyber crime department threatening immediate arrest and demanding safe account transfer.",
        category = "DIGITAL_ARREST",
        turns = listOf(
            DemoTurn(
                step = 1,
                speaker = "Caller (Fake CBI Hindi)",
                text = "Main CBI cyber crime department se bol raha hoon.",
                expectedSignals = listOf(ScamType.AUTHORITY_IMPERSONATION),
                delayMs = 2500L
            ),
            DemoTurn(
                step = 2,
                speaker = "Caller (Fake CBI Hindi)",
                text = "Aapke Aadhaar se illegal transaction hua hai.",
                expectedSignals = listOf(ScamType.IDENTITY_DOCUMENT_THREAT),
                delayMs = 2500L
            ),
            DemoTurn(
                step = 3,
                speaker = "Caller (Fake CBI Hindi)",
                text = "Agar aap cooperate nahi karenge toh arrest ho jayega.",
                expectedSignals = listOf(ScamType.LEGAL_THREAT),
                delayMs = 2500L
            ),
            DemoTurn(
                step = 4,
                speaker = "Caller (Fake CBI Hindi)",
                text = "Kisi ko phone mat karna, yeh mamla secret hai.",
                expectedSignals = listOf(ScamType.SECRECY),
                delayMs = 2500L
            ),
            DemoTurn(
                step = 5,
                speaker = "Caller (Fake CBI Hindi)",
                text = "Abhi safe RBI account mein paisa transfer karo.",
                expectedSignals = listOf(ScamType.FINANCIAL_REQUEST, ScamType.SAFE_ACCOUNT_SCAM),
                delayMs = 3000L
            )
        )
    )

    val bankKycScenario = DemoScenario(
        scenarioId = "bank_kyc_anydesk",
        title = "SBI / HDFC Bank KYC & AnyDesk Takeover",
        description = "Fake bank manager threatening account block, coercing victim to install AnyDesk and reveal OTP.",
        category = "BANK_FRAUD",
        turns = listOf(
            DemoTurn(
                step = 1,
                speaker = "Caller (Fake Bank Officer)",
                text = "Good afternoon, I am calling from State Bank of India customer service head office.",
                expectedSignals = listOf(ScamType.AUTHORITY_IMPERSONATION),
                delayMs = 2000L
            ),
            DemoTurn(
                step = 2,
                speaker = "Caller (Fake Bank Officer)",
                text = "Your bank account and ATM card will be blocked within 1 hour due to incomplete KYC.",
                expectedSignals = listOf(ScamType.URGENCY),
                delayMs = 2500L
            ),
            DemoTurn(
                step = 3,
                speaker = "Caller (Fake Bank Officer)",
                text = "Please install AnyDesk app immediately so our officer can verify your account.",
                expectedSignals = listOf(ScamType.REMOTE_ACCESS_REQUEST),
                delayMs = 2500L
            ),
            DemoTurn(
                step = 4,
                speaker = "Caller (Fake Bank Officer)",
                text = "Open Google Pay and enter your UPI PIN to approve the verification token.",
                expectedSignals = listOf(ScamType.CREDENTIAL_REQUEST),
                delayMs = 3000L
            ),
            DemoTurn(
                step = 5,
                speaker = "Caller (Fake Bank Officer)",
                text = "A 6-digit OTP has been sent. Share the OTP right now to unfreeze your account.",
                expectedSignals = listOf(ScamType.CREDENTIAL_REQUEST, ScamType.URGENCY),
                delayMs = 3000L
            )
        )
    )
}
