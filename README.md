# 麻酔管理料計算アプリ

[![Live Demo](https://img.shields.io/badge/Live%20Demo-GitHub%20Pages-blue)](https://ysuzuki1978.github.io/anesthesia_calc/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

iOS版から移植された麻酔管理料計算アプリのWebバージョンです。保険点数の計算ロジックを正確に移植し、複数の方法で利用できます。

## 🚀 すぐに使う

**最も簡単な方法**: [**GitHub Pages でアプリを開く**](https://ysuzuki1978.github.io/anesthesia_calc/)

または

**ローカルで使用**: `index.html` をダブルクリック → ブラウザで開く → すぐに利用可能

## ✨ 特徴

- **🎯 計算ロジックの正確な移植**: Swift版の複雑な計算ロジック（借用ロジック方式）を完全移植
- **📱 完全スタンドアロン**: `index.html` 1ファイルだけで動作、サーバー不要
- **🔧 複数の起動方法**: JavaベースのWebサーバー版とスタンドアロンHTML版を提供
- **⚡ 即座に利用可能**: 依存関係なし、インストール不要
- **📱 レスポンシブデザイン**: デスクトップ・モバイル・タブレット対応

## 移植された機能

### 計算ロジック
- 麻酔レベル別基本点数（困難患者・通常患者）
- 時間延長加算（借用ロジック方式）
- 硬膜外麻酔加算・時間延長加算
- 神経ブロック加算
- 時間外・深夜休日加算

### UI機能
- リアルタイム計算
- 詳細内訳表示
- 入力値検証
- レスポンシブデザイン

## 📖 使用方法

### 🌐 方法1: GitHub Pages（推奨）

[**アプリを開く**](https://ysuzuki1978.github.io/anesthesia_calc/) → すぐに使える

### 💻 方法2: ローカルで使用（スタンドアロン）

1. **`index.html` をダブルクリック** → ブラウザで開く → 完了
   - ✅ Webサーバー不要
   - ✅ インターネット接続不要
   - ✅ どのブラウザでも動作
   - ✅ スマホ・タブレットでも利用可能

### 🖥️ 方法3: Javaベース Webサーバー版（開発者向け）

1. 起動スクリプトを実行:
```bash
./run.sh
```

2. ブラウザで http://localhost:8080 にアクセス

## ファイル構成

- `AnesthesiaCalculator.java` - 計算ロジック（Swiftから移植）
- `AnesthesiaWebApp.java` - Webサーバー（Java標準ライブラリのみ使用）
- `index.html` - スタンドアロン版（JavaScript実装）
- `run.sh` - 起動スクリプト
- `README.md` - このファイル

## 技術仕様

### Java版
- **言語**: Java（標準ライブラリのみ）
- **Webサーバー**: com.sun.net.httpserver.HttpServer
- **ポート**: 8080
- **依存関係**: なし

### HTML版
- **言語**: HTML + CSS + JavaScript
- **フレームワーク**: なし（Vanilla JS）
- **ブラウザ要件**: モダンブラウザ

## 計算仕様

### 基本点数表（令和6年診療報酬改訂対応）
| レベル | 困難患者（イ） | 通常患者（ロ） |
|--------|-------------|-------------|
| 1      | 24,900点    | 18,200点    |
| 2      | 16,720点    | 12,190点    |
| 3      | 12,610点    | 9,170点     |
| 4      | 9,130点     | 6,610点     |
| 5      | 8,300点     | 6,000点     |

### 時間延長加算（30分単位）
| レベル | 加算点数 |
|--------|---------|
| 1      | 1,800点 |
| 2      | 1,200点 |
| 3      | 900点   |
| 4      | 660点   |
| 5      | 600点   |

### 硬膜外麻酔加算
| 部位      | 基本加算 | 時間延長加算（30分単位） |
|-----------|---------|---------------------|
| 頸・胸部  | 750点   | 375点               |
| 腰部      | 400点   | 200点               |
| 仙骨部    | 170点   | 85点                |

### 神経ブロック加算
- 硬膜外代用: 450点
- その他: 45点

### 時間外・深夜休日加算
- 時間外加算: 1.4倍
- 深夜・休日加算: 1.8倍

## 借用ロジック方式について

元のSwift版で実装されている複雑な時間延長計算方式を正確に移植:

1. 基本時間（120分）を高いレベルから順に消費
2. 延長時間は30分ブロック単位で計算
3. 不足分は下位レベルから借用
4. 各レベルの加算点数で計算

## 注意事項

- 本アプリは計算支援ツールです
- 実際の診療報酬請求には最新の点数表を確認してください
- 施設基準や算定要件の確認が必要です

## 🛠️ 開発情報

- **原版**: iOS Swift版（AnesthesiaCalculator_iphone_LTS）
- **移植版**: Java + HTML/JavaScript
- **移植日**: 2025年
- **対応診療報酬**: 令和6年（2024年）改訂版

## 📄 ライセンス

MIT License

Copyright (c) 2025 YASUYUKI SUZUKI

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## 🤝 貢献

プルリクエストやイシューを歓迎します。

## ⚠️ 免責事項

- 本ソフトウェアは計算支援ツールです
- 実際の診療報酬請求には最新の点数表と施設基準を確認してください
- 開発者は計算結果の正確性について一切の責任を負いません