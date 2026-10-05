// ScamShield Indian Scam Rules & Signal Extractor
// Ported directly from IndianScamRules.kt (Pure on-device regex rules)

export const SCAM_TAXONOMY = {
  AUTHORITY_IMPERSONATION: { name: 'Authority Impersonation', weight: 20 },
  POLICE_CBI_RBI_IMPERSONATION: { name: 'Law Enforcement', weight: 20 },
  LEGAL_THREAT: { name: 'Arrest Threat', weight: 20 },
  IDENTITY_DOCUMENT_THREAT: { name: 'Aadhaar / PAN Threat', weight: 15 },
  SECRECY: { name: 'Coerced Secrecy', weight: 15 },
  VIDEO_CONFINEMENT: { name: 'Video Confinement', weight: 15 },
  FINANCIAL_REQUEST: { name: 'Money Demand', weight: 30 },
  SAFE_ACCOUNT_SCAM: { name: 'Safe Account Scam', weight: 25 },
  CREDENTIAL_REQUEST: { name: 'Credential Harvesting', weight: 30 },
  REMOTE_ACCESS_REQUEST: { name: 'Remote Screen Control', weight: 25 },
  URGENCY: { name: 'Manufactured Urgency', weight: 10 },
  PAYMENT_LINK: { name: 'Payment Link / QR', weight: 20 }
};

export const INDIAN_SCAM_RULES = [
  // 1. AUTHORITY_IMPERSONATION & POLICE_CBI_RBI
  {
    type: 'AUTHORITY_IMPERSONATION',
    regex: /\b(cbi|central bureau of investigation|crime branch|cyber crime (cell|department|branch)|customs officer|narcotics control bureau|ncb|enforcement directorate|ed department|income tax officer|rbi( officer)?|reserve bank of india|supreme court|trai|telecom regulatory)\b/i,
    weight: 20,
    desc: 'Indian government enforcement agency impersonation'
  },
  {
    type: 'AUTHORITY_IMPERSONATION',
    regex: /\b(cbi (cyber crime department )?se bol raha|cbi se bol raha|police station se|crime branch se|customs department se|income tax se)\b/i,
    weight: 20,
    desc: 'Hindi authority impersonation'
  },
  {
    type: 'POLICE_CBI_RBI_IMPERSONATION',
    regex: /\b(police officer|inspector|sub-inspector|sho|deputy commissioner|senior inspector|delhi police|mumbai police)\b/i,
    weight: 20,
    desc: 'Police authority impersonation'
  },

  // 2. LEGAL THREAT & ARREST
  {
    type: 'LEGAL_THREAT',
    regex: /\b(arrest(ed)?|arrest warrant|non-bailable warrant|police will reach|case registered|fir lodged|pmla|prevention of money laundering|court summons|legal action|jail|custody)\b/i,
    weight: 20,
    desc: 'Threat of arrest or criminal proceedings'
  },
  {
    type: 'LEGAL_THREAT',
    regex: /\b(arrest kiya jayega|arrest ho jayega|police bhej rahe hain|fir darj hui hai|jail hogi|police pakad legi|warrant nikla hai)\b/i,
    weight: 20,
    desc: 'Hindi legal and arrest threat'
  },

  // 3. IDENTITY DOCUMENT THREAT (Aadhaar / PAN / Parcel / Drugs)
  {
    type: 'IDENTITY_DOCUMENT_THREAT',
    regex: /\b(aadhaar( card)?|pan card|passport|identity linked|illegal transaction|illegal parcel|intercepted at (customs|airport)|drugs found in parcel|money laundering|twenty-four.*accounts|24.*accounts)\b/i,
    weight: 15,
    desc: 'Identity document compromise accusation'
  },
  {
    type: 'IDENTITY_DOCUMENT_THREAT',
    regex: /\b(aadhaar card se|pan card se|parcel mein illegal|aapke naam se account|black money linked|suspicious parcel|passports?|narcotics)\b/i,
    weight: 15,
    desc: 'Hindi Aadhaar/PAN misuse accusation'
  },

  // 4. COERCED SECRECY
  {
    type: 'SECRECY',
    regex: /\b(do not tell (anyone|anybody|(your )?family)|don't (tell|inform) (your )?family|strictly confidential|keep this secret|secret investigation|this is secret|national security matter|do not discuss with anyone|tell nobody|section 144)\b/i,
    weight: 15,
    desc: 'Coercive secrecy requirement'
  },
  {
    type: 'SECRECY',
    regex: /\b(kisi ko (phone|call|batana) mat|kisi ko mat batana|parivar ko mat batao|ghar par mat bolna|secret investigation hai|confidential mamla hai)\b/i,
    weight: 15,
    desc: 'Hindi secrecy coercion'
  },

  // 5. VIDEO CONFINEMENT ("Digital Arrest")
  {
    type: 'VIDEO_CONFINEMENT',
    regex: /\b(stay on (the |this )?video call|digital custody|digital arrest|do not disconnect|keep (your )?camera on|don't leave the room|look at the screen|video call verification|switch on your video)\b/i,
    weight: 15,
    desc: 'Forced video presence / Digital Arrest confinement'
  },
  {
    type: 'VIDEO_CONFINEMENT',
    regex: /\b(video call mat cut karna|camera on rakho|screen se hatna mat|digital arrest ho aap|call mat kaatna|video call par aaiye|digital custody)\b/i,
    weight: 15,
    desc: 'Hindi video confinement command'
  },

  // 6. FINANCIAL REQUEST
  {
    type: 'FINANCIAL_REQUEST',
    regex: /\b(transfer (money|funds|.*rupees|.*lakh)|send (₹|rs\.?|rupees|lakh)|security deposit|clearance fee|verification deposit|pay fine|transfer funds|penalty payment|five lakh rupees|5 lakh)\b/i,
    weight: 30,
    desc: 'Direct financial transfer solicitation'
  },
  {
    type: 'FINANCIAL_REQUEST',
    regex: /\b(paise transfer karo|paisa transfer karo|paise bhej do|lakh rupaye transfer|fine bharna padega|deposit jama karo|penalty deposit)\b/i,
    weight: 30,
    desc: 'Hindi money transfer demand'
  },

  // 7. SAFE ACCOUNT SCAM
  {
    type: 'SAFE_ACCOUNT_SCAM',
    regex: /\b(safe (rbi )?account|government verification account|clearing account|security holding account|refundable after audit|safe account mein|reserve bank of india clearance account)\b/i,
    weight: 25,
    desc: "Fraudulent 'Safe Government Account' transfer pitch"
  },
  {
    type: 'SAFE_ACCOUNT_SCAM',
    regex: /\b(safe account mein|rbi account mein paise|audit ke baad wapas|sarkari account|government escrow account)\b/i,
    weight: 25,
    desc: 'Hindi safe account lure'
  },

  // 8. CREDENTIAL / OTP REQUEST
  {
    type: 'CREDENTIAL_REQUEST',
    regex: /\b(otp|one time password|upi pin|atm pin|cvv|net banking password|share the code|6-digit code)\b/i,
    weight: 30,
    desc: 'Sensitive credential harvesting'
  },
  {
    type: 'CREDENTIAL_REQUEST',
    regex: /\b(otp bataiye|otp share karo|upi pin daliye|pin enter karo|code bata do)\b/i,
    weight: 30,
    desc: 'Hindi credential harvesting'
  },

  // 9. REMOTE ACCESS REQUEST
  {
    type: 'REMOTE_ACCESS_REQUEST',
    regex: /\b(anydesk|teamviewer|quicksupport|rustdesk|install (the )?app|share (your )?screen|grant remote access|9-digit code)\b/i,
    weight: 25,
    desc: 'Remote desktop application coercion'
  },
  {
    type: 'REMOTE_ACCESS_REQUEST',
    regex: /\b(anydesk download karo|screen share karo|app install kijiye|remote support)\b/i,
    weight: 25,
    desc: 'Hindi remote access request'
  },

  // 10. ARTIFICIAL URGENCY
  {
    type: 'URGENCY',
    regex: /\b(immediately|right now|within 10 minutes|within 20 minutes|within 1 hour|act now|last chance|urgently|account will be blocked|sim will be deactivated|judicial asset seizure)\b/i,
    weight: 10,
    desc: 'Manufactured urgency to bypass victim scrutiny'
  },
  {
    type: 'URGENCY',
    regex: /\b(turant|abhi ke abhi|ek ghante mein|service block ho jayegi|sim band ho jayega|adhe ghante mein|varna)\b/i,
    weight: 10,
    desc: 'Hindi artificial urgency'
  },

  // 11. PAYMENT LINK / QR REQUEST
  {
    type: 'PAYMENT_LINK',
    regex: /\b(scan (this )?qr|payment link|click this link|upi collect request|refund link)\b/i,
    weight: 20,
    desc: 'Suspicious payment link or QR code lure'
  },
  {
    type: 'PAYMENT_LINK',
    regex: /\b(qr code scan karo|link par click karo|paise mangane ka link)\b/i,
    weight: 20,
    desc: 'Hindi QR/link demand'
  }
];

/**
 * Extracts scam signals directly from actual recognized speech transcripts.
 * Pure evidence-based extraction matching IndianScamRules.kt.
 */
export function extractScamSignals(transcriptText) {
  if (!transcriptText || typeof transcriptText !== 'string' || transcriptText.trim().length === 0) {
    return [];
  }

  const detectedSignals = [];
  const seenTypes = new Set();

  for (const rule of INDIAN_SCAM_RULES) {
    if (seenTypes.has(rule.type)) continue;

    const match = transcriptText.match(rule.regex);
    if (match) {
      seenTypes.add(rule.type);
      const matchedSnippet = match[0];
      detectedSignals.push({
        id: rule.type,
        name: SCAM_TAXONOMY[rule.type] ? SCAM_TAXONOMY[rule.type].name : rule.type,
        weight: rule.weight,
        snippet: matchedSnippet,
        fullQuote: transcriptText,
        confidence: 'Not reported' // Strictly comply with Section 10 (no fake confidence percentage)
      });
    }
  }

  return detectedSignals;
}
