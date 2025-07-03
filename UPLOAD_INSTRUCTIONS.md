# GitHub アップロード手順

## 1. GitHubリポジトリ作成

1. GitHubにログイン
2. 右上の「+」→「New repository」
3. Repository name: `AnesthesiaCalc`
4. Description: `麻酔管理料計算アプリ - iOS版から移植されたWeb版`
5. Public に設定
6. **Initialize this repository with:** のチェックは全て外す
7. 「Create repository」をクリック

## 2. ローカルでGit初期化とプッシュ

```bash
cd /Users/ysuzuki/Dropbox/claude_work/AnesthesiaCalc

# Git初期化
git init

# ファイルを追加
git add .

# 初回コミット
git commit -m "Initial commit: 麻酔管理料計算アプリ

- iOS Swift版からJava/JavaScript版に移植
- index.htmlスタンドアロン版（サーバー不要）
- Javaベース Webサーバー版
- 令和6年診療報酬改訂対応
- MIT License"

# リモートリポジトリを追加（YOUR_USERNAMEを実際のユーザー名に変更）
git remote add origin https://github.com/YOUR_USERNAME/AnesthesiaCalc.git

# プッシュ
git push -u origin main
```

## 3. GitHub Pages 設定

1. GitHubのリポジトリページで「Settings」タブ
2. 左サイドバーの「Pages」
3. Source: `Deploy from a branch`
4. Branch: `main`
5. Folder: `/ (root)`
6. 「Save」をクリック

## 4. README.mdのリンク更新

GitHub Pages の URL が決まったら：

1. README.md の `YOUR_USERNAME` を実際のユーザー名に変更
2. 例: `https://ysuzuki.github.io/AnesthesiaCalc/`

```bash
# README.mdを編集後
git add README.md
git commit -m "Update GitHub Pages URL in README"
git push
```

## 5. 完了

- **リポジトリURL**: `https://github.com/YOUR_USERNAME/AnesthesiaCalc`
- **アプリURL**: `https://YOUR_USERNAME.github.io/AnesthesiaCalc/`

数分後にGitHub Pagesでアプリが公開されます。

## ファイル一覧

✅ `index.html` - スタンドアロンWebアプリ（メインファイル）
✅ `README.md` - プロジェクト説明（GitHub Pages対応）
✅ `LICENSE` - MITライセンス
✅ `.gitignore` - Git除外設定
✅ `AnesthesiaCalculator.java` - Java版計算ロジック
✅ `AnesthesiaWebApp.java` - Java版Webサーバー
✅ `run.sh` - Java版起動スクリプト

全て準備完了です！