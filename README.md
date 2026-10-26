# 🎨 Drawing for Kids (Android)

A modern, offline-ready, step-by-step drawing and coloring application for kids built with **Kotlin**, **Android Jetpack**, and **Material 3**.

---

## 🌟 Features

- ✏️ **15 Guided Drawing Categories**: Step-by-step tutorials from easy to advanced (Animals, Vehicles, Characters, Nature).
- 🎨 **Smart Drawing Engine**:
  - Smooth quad-bezier brush strokes.
  - Natural eraser tool.
  - Interactive HSV color wheel picker.
  - Adjustable brush thickness with live preview.
- 🔄 **Undo & Redo System**: Easily undo and redo any stroke.
- 💾 **Safe Gallery Export**: Direct export to device gallery using modern `MediaStore API` (Android 10+ scoped storage compliant).
- 📤 **One-Click Share**: Share drawings with friends and family via Android `FileProvider`.
- 🌐 **Multilingual**: Full support for English and Arabic (العربية).
- 🚀 **100% Offline Capable**: Core drawing and lessons work completely without internet.
- 📢 **Monetization Ready**: Integrated modern AdMob banner ads with non-intrusive placement.

---

## 🏗️ Tech Stack

- **Language**: Kotlin 2.0+
- **Architecture**: Modern Android Jetpack
- **UI Components**: Material 3, ViewPager2, RecyclerView, ConstraintLayout
- **Graphics**: Custom hardware-accelerated `Canvas`, `Path`, `Paint`
- **Storage**: `MediaStore` API + `FileProvider`
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`) with Version Catalog (`libs.versions.toml`)
- **Min SDK**: 21 (Android 5.0 Lollipop)
- **Target SDK**: 35 (Android 15)

---

## 🚀 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/Sa4aC3n/Drawing.git
   ```
2. Open the project in **Android Studio** (Hedgehog or newer recommended).
3. Allow Gradle to synchronize dependencies.
4. Run on an emulator or physical device (API 21+).

---

## 🗺️ Roadmap
See [ROADMAP.md](ROADMAP.md) for the active phase-by-phase implementation plan.
