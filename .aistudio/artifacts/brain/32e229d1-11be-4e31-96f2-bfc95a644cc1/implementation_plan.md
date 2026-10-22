# Lipi (লিপি) - Intelligent Banglish Transliteration & Bengali Assistant

Lipi is a specialized Android application designed to bridge the gap between Banglish (Bengali written in Latin characters) and authentic Bengali script, providing instant transliteration, an intelligent Bengali spelling doctor, mixed-script awareness, and a system-wide floating action badge over chat applications (WhatsApp, Messenger, Telegram).

***

## User Review & Critical Decisions

> [!IMPORTANT]
> The following key decisions were confirmed during the planning clarification:
> - **Floating Assistant Design**: Floating action badge positioned neatly adjacent to detected chat text bubbles, expanding into a sleek Bengali translation popup on tap.
> - **Engine Strategy**: Hybrid Engine — Built-in offline phonetic rules (Avro-aligned grammar, conjuncts, vowel matras) coupled with a local Bengali lexicon and spelling doctor dictionary.
> - **Visual Identity**: Padma Teal aesthetic — Deep indigo-teal backgrounds, rich cyan highlights, warm terracotta/clay accent buttons, and Bengali script typographic hierarchy.

***

## 1. Overview & Core Concept

Bengali is spoken by over 250 million people, yet a large proportion of daily digital communication on WhatsApp, Messenger, and Telegram happens in "Banglish" (e.g., *"Ami valo asi"*, *"Tumi kemon acho?"*, *"Ajke ki korba?"*). Reading long Banglish messages causes cognitive strain, while typing Bengali directly often suffers from complex conjunct spelling errors (*"আমি ভাল আছি"* instead of *"আমি ভালো আছি"*).

**Lipi** solves this dual problem:
1. **Banglish to Bengali Transliteration**: Transforms Latin-script Banglish into natural Bengali Unicode with proper vowel matras (কার), conjuncts (যুক্তাক্ষর), and phonetic nuances.
2. **Bengali Spelling Doctor**: Identifies and fixes standard Bengali spelling mistakes, missing 'ো' (o-kar), য-ফলা, ন/ণ, শ/ষ/স, and common colloquial misspellings.
3. **Smart Mixed-Script Parsing**: Separates pure English technical terms (*"meeting"*, *"call"*, *"link"*), phone numbers, URLs, and emojis from Banglish tokens, leaving intentional English and emojis completely intact.
4. **Floating Messaging Badge**: In messaging apps, Lipi detects chat bubbles and attaches an unobtrusive floating badge that expands into an instant Bengali translation without modifying or sending messages.
5. **Interactive Testing Studio & Chat Simulator**: A dedicated in-app laboratory featuring a live interactive messaging simulator (to experience the overlay immediately in the emulator), a dual-mode conversion workbench, and a personalized wordbook.

***

## 2. User Experience & Visual Design

```
┌──────────────────────────────────────────────────────────────┐
│  Lipi (লিপি) - Modern Padma Teal Interface                   │
├──────────────────────────────────────────────────────────────┤
│  [Top Bar]  🌿 Lipi (লিপি)        [Floating Service: ON/OFF] │
├──────────────────────────────────────────────────────────────┤
│  [Tabs]  [Studio / Workbench]  [Chat Simulator]  [Settings]  │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ Mode: [● Banglish → বাংলা]  [○ বানান সংশোধন]  [Auto]    │  │
│  ├────────────────────────────────────────────────────────┤  │
│  │ Input:                                                 │  │
│  │ "Ami valo asi, tumi kemon acho? Ajke 5pm e call dao"   │  │
│  ├────────────────────────────────────────────────────────┤  │
│  │ Result:                                                │  │
│  │ "আমি ভালো আছি, তুমি কেমন আছো? আজকে 5pm এ কল দাও"        │  │
│  │                                                        │  │
│  │ [Copy] [Share] [Pronounce / Audio] [Save to Wordbook]  │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌─ Live Assistant Status ────────────────────────────────┐  │
│  │  Overlay Permission: [Granted]    Accessibility: [Active] │  │
│  │  Target Apps: [WhatsApp ✓] [Messenger ✓] [Telegram ✓]  │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

### Key User Flows

1. **Studio Workbench Flow**:
   - User types or pastes text in the main studio.
   - Lipi dynamically tokenizes the input: identifies Banglish tokens, Bengali tokens, English words, numbers, and emojis.
   - Shows split view or converted card with instant one-tap copy, token breakdown, and spelling suggestions.
2. **Chat Simulator Flow (In-App Live Experience)**:
   - Emulates active chats from popular apps (e.g., a WhatsApp/Messenger dialogue containing realistic Banglish messages like *"Kire koi tui? Taratari ay!"*).
   - Demonstrates the floating action badge docked beside the message bubble.
   - Tapping the badge smoothly animates a floating card revealing the accurate Bengali script (*"কিরে কই তুই? তাড়াতাড়ি আয়!"*).
3. **Floating Overlay & System Integration Flow**:
   - Onboarding card guides the user through Android `SYSTEM_ALERT_WINDOW` and `AccessibilityService` permissions.
   - App picker allows toggling which messenger apps Lipi listens to (WhatsApp, Messenger, Telegram, Messages, Instagram).
   - Toggle to turn background detection on/off with an active persistent status tile.
4. **Spelling Doctor & Wordbook Flow**:
   - In "Bengali Spelling" mode, paste or type Bengali text.
   - Lipi highlights corrected words with subtle clay badges and provides before/after explanation.
   - Users can add custom transliteration pairs to their personal dictionary.

### Visual Identity & Theme
- **Dominant Palette**:
  - `Primary`: Deep Padma Teal (`#0A3641`) and Vibrant Cyan (`#00A896`)
  - `Secondary / Accent`: Warm Terracotta Clay (`#D86B43` / `#E27D60`)
  - `Background`: Soft Off-white / Muted Pearl (`#F7F9FA`) in light mode; Deep Indigo Navy (`#0E1626`) in dark mode
  - `Surface`: Layered cards with subtle border strokes (`#E0E7EB` / `#1F2937`) and 12dp rounded corners
- **Typography**:
  - Clean Bengali typography hierarchy, utilizing system Bengali fonts (`Noto Sans Bengali` fallback) with legible line-height (`1.5`) and ample touch targets (min 48dp).
- **Motion & Feedback**:
  - Spring-animated pill expansions for the floating badge.
  - Smooth card cross-fades when toggling between Banglish and Spell-Doctor modes.

***

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Hybrid Rule-Based Phonetic Engine with Local Dictionary**
  - *Chosen Approach*: Implement an Avro-style phonetics state machine (handling independent vowels, vowel matras, consonants, consonant clusters/conjuncts like `kk` -> `ক্ক`, `ng` -> `ঙ/ং`, `kh` -> `খ`, `th` -> `থ`, `dh` -> `ধ`) supplemented by a 2000+ common word frequency lookup and spelling rule adjuster.
  - *Why*: 100% offline, zero latency (< 2ms), no network data usage, complete privacy for sensitive private chat conversations.
  - *Alternatives Considered*: Pure LLM/Cloud API (rejected due to network latency, offline failure, and privacy concerns for private messaging).
- **Decision 2: Intelligent Mixed-Script Tokenizer**
  - *Chosen Approach*: Regex and character-class based tokenization that tags tokens into: `BANGLISH_PHONETIC`, `BENGALI_UNICODE`, `ENGLISH_DICTIONARY_TERM`, `NUMERICAL`, `PUNCTUATION_EMOJI`. Only `BANGLISH_PHONETIC` and `BENGALI_UNICODE` are processed by their respective transformers; English abbreviations/technical words, numbers, and emojis pass through untouched.
- **Decision 3: Dual Architecture for Native Floating vs Emulator Experience**
  - *Chosen Approach*: Real native Android `AccessibilityService` (`LipiAccessibilityService`) and `WindowManager` overlay service (`LipiOverlayService`) declared in `AndroidManifest.xml` with real permission checks, combined with a built-in **Chat Simulator** in the Compose UI so the floating badge experience can be fully demonstrated and tested inside the AI Studio streaming emulator without requiring third-party messaging apps installed.

***

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Lipi Android Architecture                     │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │                        Compose UI Layer                        │   │
│   │  ┌───────────────┐  ┌──────────────────┐  ┌─────────────────┐  │   │
│   │  │ Studio Screen │  │  Chat Simulator  │  │ Settings & Apps │  │   │
│   │  │ (Workbench &  │  │ (Floating Badge  │  │ (Permissions &  │  │   │
│   │  │  Spell Doctor)│  │   Interaction)   │  │   Dictionary)   │  │   │
│   │  └───────┬───────┘  └─────────┬────────┘  └────────┬────────┘  │   │
│   └──────────┼────────────────────┼────────────────────┼───────────┘   │
│              │                    │                    │               │
│              ▼                    ▼                    ▼               │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │                      LipiViewModel                             │   │
│   │   - inputState / convertedState / tokenState                   │   │
│   │   - activeMode: BANGLISH_TO_BN | SPELL_DOCTOR | AUTO_DETECT    │   │
│   │   - overlayEnabled / serviceStatus / enabledApps               │   │
│   └───────────────────────────────┬────────────────────────────────┘   │
│                                   │                                    │
│              ┌────────────────────┴───────────────────┐                │
│              ▼                                        ▼                │
│   ┌───────────────────────┐              ┌─────────────────────────┐   │
│   │   Phonetic Engine     │              │ Bengali Spelling Doctor │   │
│   │   & Tokenizer         │              │ & Lexicon Repository    │   │
│   │   - Avro-style rules  │              │ - Common misspellings   │   │
│   │   - Matra & Conjuncts │              │ - Diacritic normalizer  │   │
│   │   - Mixed-token parser│              │ - Custom wordbook       │   │
│   └───────────────────────┘              └─────────────────────────┘   │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │                 Android System Services (Native)               │   │
│   │   - LipiAccessibilityService (Node reader for target apps)     │   │
│   │   - LipiOverlayService (WindowManager floating badge view)     │   │
│   └────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
```

### Components to Implement
1. **Core Processing Engine (`com.example.lipi.engine`)**:
   - `BanglishPhoneticEngine`: Avro-style rule-based mapper with prefixes, suffixes, conjunct combinations (`sh`, `ch`, `th`, `dh`, `kh`, `ph`, `bh`, `ng`, `nj`, `nt`, `nd`, `tr`, `kr`, `pr`, `br`), vowel signs (আ-কার `a`, ই-কার `i`, ঈ-কার `ee`, উ-কার `u`, ঊ-কার `oo`, এ-কার `e`, ও-কার `o`, ইত্যাদি).
   - `BengaliSpellDoctor`: Lexicon-based error corrector and normalizer for colloquial/common typos (e.g. `ভাল` -> `ভালো`, `করছিলা` -> `করছিলে`, `অসুস্হ` -> `অসুস্থ`, `পুরষ্কার` -> `পুরস্কার`).
   - `MixedScriptTokenizer`: Segmenter for Latin/Banglish, Bengali, English words, numbers, punctuation, and emojis.
2. **Data & Storage (`com.example.lipi.data`)**:
   - User custom word dictionary & history repository.
   - App configuration preferences (selected target apps: WhatsApp, Messenger, Telegram, Instagram, Signal, SMS).
3. **Native Services (`com.example.lipi.service`)**:
   - `LipiAccessibilityService`: Subclass of `AccessibilityService` filtering target package names (`com.whatsapp`, `com.facebook.orca`, `org.telegram.messenger`).
   - `LipiOverlayService`: Floating action badge controller using `WindowManager` and `LayoutParams.TYPE_APPLICATION_OVERLAY`.
4. **Jetpack Compose UI (`com.example.lipi.ui`)**:
   - `StudioScreen`: Main workbench with instant conversion, copy/share, phonetic breakdown, and spell-check analysis.
   - `ChatSimulatorScreen`: Realistic interactive chat interface with incoming Banglish messages, demonstrating the floating badge and popup experience.
   - `AppSettingsScreen`: Accessibility and Overlay permission onboarding with direct system intent launchers, target app selection chips, and custom wordbook manager.
