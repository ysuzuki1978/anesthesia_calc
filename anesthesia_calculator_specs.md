# 麻酔管理料計算アプリ 技術仕様書

**開発者**: YASUYUKI SUZUKI  
**バージョン**: 1.0  
**対応診療報酬**: 令和6年（2024年）改訂版  
**プラットフォーム**: iOS（SwiftUI）

## 概要

麻酔科専門医向けの麻酔管理料算定支援アプリ。複数レベルの麻酔時間と各種加算を考慮した正確な診療報酬計算を実現。

## アーキテクチャ

### 核心となるクラス構造

```swift
// メインの計算エンジン
class AnesthesiaCalculator: ObservableObject

// データ構造
struct AnesthesiaLevelTime {
    let level: Int      // 1-5
    var minutes: Int    // 麻酔時間（分）
}

// 列挙型
enum SeverityLevel: String, CaseIterable {
    case difficult = "イ"  // 困難患者
    case normal = "ロ"     // 通常患者
}

enum EpiduralLocation: String, CaseIterable {
    case cervicalThoracic = "頸・胸部"
    case lumbar = "腰部"
    case sacral = "仙骨部"
}

enum NerveBlockType: String, CaseIterable {
    case epiduralSubstitute = "硬膜外代用"
    case other = "その他"
}
```

## UI構造（TabView）

### 1. 基本設定タブ (`doc.text`)
- 重症度選択（セグメントコントロール）
- レベル別麻酔時間入力（1-5レベル）
- 総時間表示
- リセットボタン（**重要**: 実機では長押し必要）

### 2. 追加設定タブ (`plus.circle`)
- 硬膜外麻酔設定（Toggle + Picker）
- 神経ブロック設定（Toggle + Picker）
- 時間外・深夜休日加算（Toggle）

### 3. 計算結果タブ (`chart.bar.doc.horizontal`)
- 総合計表示（点数・金額）
- 詳細内訳
- 計算詳細
- 注意事項

### 4. 情報タブ (`info.circle`)
- アプリ情報・著作権
- 診療報酬改訂対応情報
- 免責事項

## 計算ロジック詳細

### 1. 基本点数表

```swift
private let basePointsTable: [Int: [SeverityLevel: Int]] = [
    1: [.difficult: 24900, .normal: 18200],
    2: [.difficult: 16720, .normal: 12190],
    3: [.difficult: 12610, .normal: 9170],
    4: [.difficult: 9130, .normal: 6610],
    5: [.difficult: 8300, .normal: 6000]
]
```

### 2. 時間延長加算表（30分単位）

```swift
private let timeExtensionTable: [Int: Int] = [
    1: 1800,  // レベル1: 1800点/30分
    2: 1200,  // レベル2: 1200点/30分
    3: 900,   // レベル3: 900点/30分
    4: 660,   // レベル4: 660点/30分
    5: 600    // レベル5: 600点/30分
]
```

### 3. 硬膜外麻酔加算

**基本加算**:
```swift
private let epiduralPointsTable: [EpiduralLocation: Int] = [
    .cervicalThoracic: 750,  // 頸・胸部
    .lumbar: 400,            // 腰部
    .sacral: 170             // 仙骨部
]
```

**時間延長加算**:
```swift
private let epiduralTimeExtensionTable: [EpiduralLocation: Int] = [
    .cervicalThoracic: 375,  // 頸・胸部
    .lumbar: 200,            // 腰部
    .sacral: 85              // 仙骨部
]
```

### 4. 神経ブロック加算

```swift
private let nerveBlockPointsTable: [NerveBlockType: Int] = [
    .epiduralSubstitute: 450,  // 硬膜外代用
    .other: 45                 // その他
]
```

### 5. 時間外・深夜休日加算

- **時間外加算**: 基本合計 × 1.4倍
- **深夜・休日加算**: 基本合計 × 1.8倍

## 核心計算アルゴリズム：借用ロジック方式

### 重要な計算原理

1. **基本時間**: 120分（2時間）まで基本点数で算定
2. **延長時間**: 120分超過分を30分単位で切り上げ算定
3. **最高レベル基準**: 最も高いレベルの麻酔を基準として基本点数を決定
4. **借用ロジック**: 高レベル麻酔の延長時間不足を下位レベルから「借用」

### 計算手順

```swift
// 1. 基本時間配分（120分を高レベルから消費）
var remainingBaseTime = 120
for i in 0..<levelTimes.count {
    let availableTime = min(remainingBaseTime, levelTimes[i].minutes)
    levelTimes[i].baseUsed = availableTime
    remainingBaseTime -= availableTime
    if remainingBaseTime <= 0 { break }
}

// 2. 延長時間計算（借用ロジック適用）
for i in 0..<levelTimes.count {
    let availableTime = availableExtensionTimes[i]
    if availableTime > 0 {
        let neededBlocks = Int(ceil(Double(availableTime) / 30.0))
        let neededTime = neededBlocks * 30
        
        if neededTime > availableTime {
            // 不足分を下位レベルから借用
            let shortage = neededTime - availableTime
            // 下位レベルから順次借用処理...
        }
    }
}
```

### 借用ロジックの具体例

**例**: レベル1が90分、レベル3が180分の場合
1. 基本時間120分をレベル1（90分）、レベル3（30分）で消費
2. レベル3の残り150分 → 5ブロック（150分）必要
3. レベル3の延長可能時間150分で5ブロック算定可能
4. 結果: レベル1基本点数 + レベル3延長5ブロック

## 状態管理

### Published Properties

```swift
@Published var anesthesiaLevels: [AnesthesiaLevelTime]
@Published var severityLevel: SeverityLevel = .normal
@Published var hasEpiduralAnesthesia: Bool = false
@Published var epiduralLocation: EpiduralLocation = .lumbar
@Published var hasNerveBlock: Bool = false
@Published var nerveBlockType: NerveBlockType = .other
@Published var hasOvertimeAddition: Bool = false
@Published var hasNightHolidayAddition: Bool = false
```

### 計算プロパティ

```swift
var activeLevels: [AnesthesiaLevelTime]  // 時間>0のレベル
var highestLevel: Int                    // 最高使用レベル
var totalMinutes: Int                    // 総麻酔時間
var totalPoints: Int                     // 最終点数
```

## 重要な制約事項

### 1. 神経ブロック硬膜外代用の制約
```swift
// 硬膜外代用選択時は硬膜外麻酔加算が無効
guard !(hasNerveBlock && nerveBlockType == .epiduralSubstitute) else { return 0 }
```

### 2. 時間外・深夜加算の排他性
- 時間外加算（1.4倍）と深夜・休日加算（1.8倍）は同時適用可能
- より高い倍率が適用される

### 3. 硬膜外時間延長の基準
- 全身麻酔の総時間に基づいて算定（120分超過分）

## 実装上の注意点

### 1. リセット機能
```swift
func resetAllTimes() {
    objectWillChange.send()  // 明示的UI更新
    anesthesiaLevels = [/* 初期配列 */]  // 配列再作成
}
```

### 2. 実機対応（重要）
- **FormでのButton**: 実機では長押しが必要
- **解決策**: `ButtonStyle(.plain)` + `simultaneousGesture`
- **表記**: 「長押しでリセット」と明記

### 3. 入力値検証
```swift
.onSubmit { isInputActive = false }
let validMinutes = max(0, minutes)  // 負値防止
```

## 診療報酬算定根拠

### 令和6年改訂対応項目
1. **麻酔管理料（基本点数）** - レベル別・重症度別点数表
2. **時間延長加算** - 30分単位切り上げ方式
3. **硬膜外麻酔加算** - 部位別加算点数
4. **神経ブロック加算** - 種類別加算点数
5. **時間外・深夜・休日加算** - 倍率適用方式

### 算定要件
- 診療報酬算定要件を満たす場合のみ適用
- 施設基準・患者状態による要件確認必要
- 最新の点数表・通知確認必須

## テスト項目

### 基本機能テスト
1. レベル別時間入力・計算
2. 重症度切り替え
3. 硬膜外麻酔設定・計算
4. 神経ブロック設定・計算
5. 時間外加算計算
6. リセット機能

### 計算ロジックテスト
1. 借用ロジック動作確認
2. 30分単位切り上げ確認
3. 制約事項の適用確認
4. 境界値テスト（120分前後）

### UI/UXテスト
1. 実機でのリセットボタン動作
2. キーボード表示・非表示
3. タブ切り替え
4. スクロール動作

## 配布・メンテナンス

### バージョン管理
- 診療報酬改訂に合わせた点数表更新
- 計算ロジックの検証・改善
- UI/UX改善

### 免責事項
- 計算結果の保証なし
- 最新制度の確認必要
- 参考情報としての利用

---

**重要**: この仕様書は実装済みアプリの分析に基づく。Claude Codeでの再開発時は、特に借用ロジックの計算部分と実機でのButton動作に注意が必要。