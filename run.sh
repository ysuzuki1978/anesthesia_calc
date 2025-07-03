#!/bin/bash

# 麻酔管理料計算アプリ起動スクリプト

echo "麻酔管理料計算アプリを起動中..."

# Javaファイルをコンパイル
echo "Javaファイルをコンパイル中..."
javac -cp . *.java

if [ $? -eq 0 ]; then
    echo "コンパイル完了"
    echo "Webアプリケーションを起動中..."
    echo ""
    echo "ブラウザで http://localhost:8080 にアクセスしてください"
    echo "停止するには Ctrl+C を押してください"
    echo ""
    
    # アプリケーション実行
    java -cp . com.anesthesia.AnesthesiaWebApp
else
    echo "コンパイルエラーが発生しました"
    exit 1
fi