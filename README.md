# スライドクロック (Slideshow Clock)

Android 向けの、写真スライドショー＋時計アプリです。端末内のフォルダを選ぶと、その中の
画像を全画面でスライド表示し、上に時計（デジタル／アナログ）を重ねます。フォトフレームや
常時表示の卓上時計として使えます。ダークでミニマルな Material 3 デザイン。

## 主な機能

- **フォルダ選択（SAF）** — 端末／SD カードの任意のフォルダを指定。読み取り権限は永続化され、
  再起動後も維持されます。**サブフォルダも含める**（再帰読み込み）の切り替えに対応。
  「再読み込み」で追加した画像を反映。
- **時計** — デジタル / アナログ / なし を切り替え。
  - **サイズ**（50%〜250%）と**位置**（3×3 グリッドの 9 箇所）
  - **フォント** 4 種（標準 / ボールド / 明朝 / 等幅）
  - **カラー** 5 色（白 / ブルー / アンバー / ミント / ローズ）
  - **アナログのデザイン** 3 種（ミニマル / クラシック / モダン）
  - 24 時間表示・秒表示の切り替え、日付・曜日表示（例: `6月17日 (火)`）
- **切り替え効果** — フェード / スライド / Ken Burns（ゆっくりズーム＆パン）/ なし
- **常時表示向けの調整**
  - 画面の常時点灯
  - 画面の明るさを下げる（夜間・据え置き用）
  - 焼き付き防止（時計をわずかに動かして有機 EL の焼き付きを回避）
- **音楽コントロール** — 再生中の音楽アプリ（Apple Music 等）の**曲名表示・⏮ ⏯ ⏭・音量**を
  スライドショーに重ねて操作。位置（左下／右下）を選べます。利用には端末の「通知への
  アクセス」を一度有効化する必要があります（通知の内容は読み取らず、再生中メディアの取得・
  操作にのみ使用します）。
- **設定のライブプレビュー** — 変更がその場で反映。横画面では省スペースなコンパクト表示。

## 操作方法

1. 「フォルダを選択」で写真フォルダを指定するとスライドショーが始まります。
2. **画面をタップ**すると、上部の操作バー（再生／一時停止・再読み込み・設定）、
   右側の音量バー、下部の音楽コントロールが表示されます。
3. 歯車アイコンから設定画面を開き、時計・表示・音楽などを調整します。

## ビルド方法

### 必要要件
- **JDK 17**
- Android SDK **API 35**（compileSdk）／最小対応 **API 26 (Android 8.0)**

### Android Studio（推奨）
1. Android Studio（Ladybug / 2024.2 以降）でこのフォルダを開きます。
2. Gradle Sync 完了後、実行（Run ▶）します。

### コマンドライン
Gradle ラッパー（`gradlew` / `gradlew.bat` と `gradle/wrapper/gradle-wrapper.jar`）を
同梱しているので、追加準備なしでそのままビルドできます（Gradle 8.11.1）:

```bash
./gradlew assembleDebug        # Windows は gradlew.bat assembleDebug
```

生成物は `app/build/outputs/apk/debug/app-debug.apk` です。

## 技術スタック

- Kotlin / Jetpack Compose（Material 3）, Navigation Compose
- DataStore（設定の永続化）
- Storage Access Framework + DocumentsContract（フォルダ内画像の列挙）
- Coil（画像読み込み）
- 時計は `java.time` ＋ Compose Canvas（アナログ）で描画
- 音楽操作は `MediaSessionManager` ＋ `NotificationListenerService`

## プロジェクト構成

```
app/src/main/java/com/example/slideshowclock/
├── MainActivity.kt                      # エントリポイント / Navigation
├── data/
│   ├── Settings.kt                      # 設定モデル・enum
│   └── SettingsRepository.kt            # DataStore による永続化
├── media/
│   ├── ImageRepository.kt               # SAF フォルダ内画像の列挙（再帰対応）
│   ├── MediaNotificationListenerService.kt  # 通知アクセス（メディア操作の解禁用）
│   └── NowPlayingController.kt          # 再生中メディアの取得・操作
└── ui/
    ├── theme/                           # ダークな Material 3 テーマ
    ├── clock/Clock.kt                   # デジタル / アナログ時計・日付・配置
    ├── slideshow/                       # スライドショー画面と ViewModel
    └── settings/                        # 設定画面と ViewModel
```
