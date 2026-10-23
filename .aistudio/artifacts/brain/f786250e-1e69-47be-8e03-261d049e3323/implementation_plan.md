# Grouped Settings Architecture & Design Plan

Organize Lipi's settings screen into 4 distinct, cohesive Material 3 card groups with dedicated section headers, descriptive badges, and intuitive visual hierarchy.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following layout and grouping preferences have been confirmed through user consultation:
> - **Visual Style**: Categorized cards with distinct section headers, accent icons, and consistent Material 3 surface elevation.
> - **4 Core Groups**:
>   1. **Floating & Notifications** (ভাসমান সহকারী ও নোটিফিকেশন)
>   2. **Translation Engine** (রূপান্তর ইঞ্জিন ও ফরম্যাটিং)
>   3. **Custom Wordbook** (কাস্টম শব্দকোষ ও ডাটা)
>   4. **System & Battery** (সিস্টেম অনুমতি ও ব্যাটারি সুরক্ষা)
> - **Preservation of All Existing Controls**: Every existing toggle, button, test tag, search field, single-entry form, bulk import/export modal, and permission launcher will be fully preserved without any regression.

---

## 1. Overview & Core Concept

- **What It Does**: Transforms the previously flat, lengthy settings screen into 4 clearly demarcated functional groups. Each group is headed by a recognizable Material symbol icon, a Bengali title, and a subtitle describing its purpose.
- **Target Audience**: Users managing Lipi's overlay assistant, tailoring translation preferences (e.g. keeping technical English terms vs. 100% pure Bengali), adding custom vocabulary entries, and configuring battery optimization for uninterrupted background operation.
- **Key Value**: Drastically reduces cognitive load and scrolling friction, making features quick to discover and configure.

---

## 2. User Experience & Visual Design

### The 4 Setting Groups

```
┌─────────────────────────────────────────────────────────────┐
│ 🚀 ভাসমান সহকারী ও নোটিফিকেশন (Floating & Notifications)    │
├─────────────────────────────────────────────────────────────┤
│ • Master Floating Assistant Switch ('A ⇄ অ' বল)             │
│ • Direct Launch ("স্ক্রিনে সহকারী দেখান") / Hide ("লুকান")  │
│ • Persistent Quick Access Notification Bar Switch           │
│ • Notification Bar Test ("উইন্ডো পরীক্ষা করুন") & Refresh   │
│ • 3-Step Simple Copy & Translate Quick Guide                │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 🌐 রূপান্তর ইঞ্জিন ও ফরম্যাটিং (Translation Engine)          │
├─────────────────────────────────────────────────────────────┤
│ • Transliteration Style Selector:                           │
│   - [●] ১০০% খাঁটি বাংলা (All Bengali)                      │
│   - [○] কিছু ইংরেজি শব্দ সংরক্ষণ (Preserve English terms)    │
│ • অটো ক্লিপবোর্ড নজরদারি (Auto Clipboard Monitoring Switch) │
│ • সংখ্যা রূপান্তর (Convert Digits to Bengali Numerals)      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 📚 কাস্টম শব্দকোষ ও ডাটা (Custom Wordbook & Dictionary)      │
├─────────────────────────────────────────────────────────────┤
│ • একক ডাটা এন্ট্রি (Single Word Entry Form: Banglish + অর্থ) │
│ • বাল্ক ইমপোর্ট ও এক্সপোর্ট (Bulk Import / Export JSON & Text)│
│ • শব্দকোষ অনুসন্ধান ও তালিকা (Search, Filter, Edit, Delete)   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ ⚙️ সিস্টেম অনুমতি ও ব্যাটারি (System, Permissions & Battery) │
├─────────────────────────────────────────────────────────────┤
│ • ওভারলে ডিসপ্লে অনুমতি (Display Over Other Apps Status)    │
│ • ব্যাকগ্রাউন্ড ও ব্যাটারি সুরক্ষা (Unrestricted Battery Mode)│
│ • প্লে প্রোটেক্ট নিরাপদ কপি মোড ব্যাজ (Play Protect Safe)    │
└─────────────────────────────────────────────────────────────┘
```

### Visual Identity & Theme
- **Section Headers**: Integrated header row with 36dp rounded icon containers, tinted with `PadmaTealPrimary` (0xFF00A896), bold Bengali headings (16sp), and secondary category metadata.
- **Group Containers**: Elevated M3 cards with 16dp rounded corners, subtle border strokes (`MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)`), and generous padding (16dp).
- **Interactive Feedback**: Crisp ripple indicators, switch color harmonization, and test tag preservation for test suite stability.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Modular Sub-composables vs. Monolithic File**
  - *Chosen Approach*: Break down `SettingsScreen.kt` into clean, focused private composables:
    - `FloatingAndNotificationGroup`
    - `TranslationEngineGroup`
    - `CustomWordbookGroup`
    - `SystemAndBatteryGroup`
  - *Why*: Keeps the file clean, maintainable, and easily readable while preserving existing ViewModel bindings and state hoisting.
  - *Alternatives Considered*: Multi-file split was considered, but keeping them cohesively organized in `SettingsScreen.kt` maintains direct access to shared activity launchers (file picker, notification permissions) without introducing unnecessary boilerplate.

- **Decision 2: Test Tag & Robolectric Compatibility**
  - *Chosen Approach*: Maintain all existing `testTag` IDs (`floating_master_switch`, `persistent_notification_switch`, `single_data_entry_card`, `single_data_entry_banglish_input`, `save_single_data_entry_button`, `transliteration_style_all_bengali`, etc.).
  - *Why*: Guarantees zero regression on automated tests and existing UI test assertions.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                      SettingsScreen                         │
│                    (ViewModel State)                        │
└──────────────────────────────┬──────────────────────────────┘
                               │
       ┌───────────────────────┼───────────────────────┐
       ▼                       ▼                       ▼
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│  Floating &  │       │ Translation  │       │   Wordbook   │
│ Notification │       │    Engine    │       │  Management  │
│    Group     │       │    Group     │       │    Group     │
└──────┬───────┘       └──────┬───────┘       └──────┬───────┘
       │                      │                      │
       ▼                      ▼                      ▼
LipiOverlayService     MixedScriptTokenizer    LipiRepository
(Foreground Service)   (Transliteration Style) (Room / DataStore)
```

### Implementation Checklist
1. Create `SettingsSectionHeader` helper component (Icon, Title, Subtitle).
2. Group Floating Assistant, Launcher buttons, Persistent Bar, and 3-Step Guide into `FloatingAndNotificationGroup`.
3. Group Transliteration Style options, Auto Clipboard Monitoring, and Bengali Digits toggle into `TranslationEngineGroup`.
4. Group Single Data Entry card, Bulk Import/Export, and Wordbook list into `CustomWordbookGroup`.
5. Group Overlay Permission, Battery Optimization button, and Safe Mode status into `SystemAndBatteryGroup`.
6. Increment `versionCode` to 39 and `versionName` to `"1.3.8"` in `app/build.gradle.kts`.
7. Verify all Robolectric tests pass and applet compiles cleanly.
