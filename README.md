# スライドクロック (Slideshow Clock)

Android 向けの、写真スライドショー＋時計アプリです。端末内のフォルダを選ぶと、その中の
画像を全画面でスライド表示し、上に時計（デジタル／アナログ）を重ねて表示します。フォト
フレームや常時表示の卓上時計として使えます。

## 主な機能

- **フォルダ選択（SAF）** — 端末／SD カードの任意のフォルダを指定。フォルダに画像を追加
  したら「再読み込み」で反映。読み取り権限は永続化され、再起動後も維持されます。
- **時計** — デジタル / アナログ / なし を切り替え可能。
  - **サイズ調整**（50%〜250%）
  - **位置調整**（3×3 グリッドの 9 箇所）
  - 24 時間表示の切り替え、秒表示の切り替え
- **日付・曜日表示**（例: `6月17日 (火)`）
- **切り替え効果** — フェード / スライド / Ken Burns（ゆっくりズーム＆パン）/ なし
- **画面の常時点灯** — スライドショー中に画面を消灯させない
- **没入モード** — ステータスバー／ナビゲーションバーを隠した全画面表示
- **ダークでミニマル**な Material 3 デザイン、設定画面にはライブプレビュー付き

## 操作方法

1. アプリを起動し「フォルダを選択」で写真フォルダを指定します。
2. スライドショーが開始します。**画面をタップ**すると上部に操作バー（再生／一時停止・
   再読み込み・設定）が表示されます。
3. 歯車アイコンから設定画面を開き、時計の種類・サイズ・位置や切り替え効果などを調整します。

## 技術スタック

- Kotlin / Jetpack Compose（Material 3）
- Navigation Compose
- DataStore（設定の永続化）
- Storage Access Framework + DocumentsContract（フォルダ内画像の列挙）
- Coil（画像読み込み）
- 時計は `java.time` ＋ Compose Canvas（アナログ）で描画

## ビルド方法

### Android Studio（推奨）
1. Android Studio（Ladybug / 2024.2 以降）でこのフォルダを開きます。
2. Gradle Sync が完了したら実行（Run ▶）します。
3. 必要要件: **JDK 17**、Android SDK **API 35**（compileSdk）、最小対応 **API 26 (Android 8.0)**。

### コマンドライン
このリポジトリには `gradlew` / `gradlew.bat` を同梱していますが、バイナリの
`gradle/wrapper/gradle-wrapper.jar` は含めていません。初回のみ、ローカルの Gradle で
ラッパーを生成してください（Gradle 8.11.1 を想定）:

```bash
gradle wrapper --gradle-version 8.11.1
./gradlew assembleDebug      # Windows は gradlew.bat assembleDebug
```

Android Studio で開く場合は、IDE が Gradle を管理するためこの手順は不要です。

## プロジェクト構成

```
app/src/main/java/com/example/slideshowclock/
├── MainActivity.kt              # エントリポイント / Navigation
├── data/
│   ├── Settings.kt              # 設定モデル・enum
│   └── SettingsRepository.kt    # DataStore による永続化
├── media/
│   └── ImageRepository.kt       # SAF フォルダ内画像の列挙
└── ui/
    ├── theme/                   # ダークな Material 3 テーマ
    ├── clock/Clock.kt           # デジタル / アナログ時計・日付・配置
    ├── slideshow/               # スライドショー画面と ViewModel
    └── settings/                # 設定画面と ViewModel
```
