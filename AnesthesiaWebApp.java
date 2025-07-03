package com.anesthesia;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.sun.net.httpserver.*;

public class AnesthesiaWebApp {
    private HttpServer server;
    private AnesthesiaCalculator calculator;
    private static final int PORT = 8080;
    
    public AnesthesiaWebApp() {
        this.calculator = new AnesthesiaCalculator();
    }
    
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/calculate", new CalculateHandler());
        server.createContext("/api/update", new UpdateHandler());
        server.createContext("/api/reset", new ResetHandler());
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("麻酔管理料計算アプリが起動しました: http://localhost:" + PORT);
    }
    
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
    
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            
            String content = getStaticContent(path);
            String contentType = getContentType(path);
            
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, content.getBytes("UTF-8").length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content.getBytes("UTF-8"));
            }
        }
    }
    
    private class CalculateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            
            String response = buildCalculationResponse();
            
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes("UTF-8"));
            }
        }
    }
    
    private class UpdateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            
            String requestBody = readRequestBody(exchange);
            updateCalculatorFromRequest(requestBody);
            
            String response = buildCalculationResponse();
            
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes("UTF-8"));
            }
        }
    }
    
    private class ResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            
            calculator.resetAllTimes();
            
            String response = buildCalculationResponse();
            
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes("UTF-8"));
            }
        }
    }
    
    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
    
    private void updateCalculatorFromRequest(String requestBody) {
        Map<String, String> params = parseFormData(requestBody);
        
        for (int level = 1; level <= 5; level++) {
            String timeStr = params.get("level" + level);
            if (timeStr != null && !timeStr.trim().isEmpty()) {
                try {
                    int minutes = Integer.parseInt(timeStr.trim());
                    calculator.updateTime(level, minutes);
                } catch (NumberFormatException e) {
                    calculator.updateTime(level, 0);
                }
            } else {
                calculator.updateTime(level, 0);
            }
        }
        
        String severity = params.get("severity");
        if ("difficult".equals(severity)) {
            calculator.setSeverityLevel(AnesthesiaCalculator.SeverityLevel.DIFFICULT);
        } else {
            calculator.setSeverityLevel(AnesthesiaCalculator.SeverityLevel.NORMAL);
        }
        
        calculator.setHasEpiduralAnesthesia("true".equals(params.get("hasEpidural")));
        
        String epiduralLocation = params.get("epiduralLocation");
        if ("cervical".equals(epiduralLocation)) {
            calculator.setEpiduralLocation(AnesthesiaCalculator.EpiduralLocation.CERVICAL_THORACIC);
        } else if ("sacral".equals(epiduralLocation)) {
            calculator.setEpiduralLocation(AnesthesiaCalculator.EpiduralLocation.SACRAL);
        } else {
            calculator.setEpiduralLocation(AnesthesiaCalculator.EpiduralLocation.LUMBAR);
        }
        
        calculator.setHasNerveBlock("true".equals(params.get("hasNerveBlock")));
        
        String nerveBlockType = params.get("nerveBlockType");
        if ("epiduralSubstitute".equals(nerveBlockType)) {
            calculator.setNerveBlockType(AnesthesiaCalculator.NerveBlockType.EPIDURAL_SUBSTITUTE);
        } else {
            calculator.setNerveBlockType(AnesthesiaCalculator.NerveBlockType.OTHER);
        }
        
        calculator.setHasOvertimeAddition("true".equals(params.get("hasOvertime")));
        calculator.setHasNightHolidayAddition("true".equals(params.get("hasNightHoliday")));
    }
    
    private Map<String, String> parseFormData(String formData) {
        Map<String, String> params = new HashMap<>();
        if (formData == null || formData.trim().isEmpty()) {
            return params;
        }
        
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                try {
                    String key = URLDecoder.decode(keyValue[0], "UTF-8");
                    String value = URLDecoder.decode(keyValue[1], "UTF-8");
                    params.put(key, value);
                } catch (UnsupportedEncodingException e) {
                    System.err.println("Error decoding form data: " + e.getMessage());
                }
            }
        }
        return params;
    }
    
    private String buildCalculationResponse() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        json.append("\"totalPoints\":").append(calculator.getTotalPoints()).append(",");
        json.append("\"basePoints\":").append(calculator.getBasePoints()).append(",");
        json.append("\"timeExtensionPoints\":").append(calculator.getTimeExtensionPoints()).append(",");
        json.append("\"epiduralPoints\":").append(calculator.getEpiduralPoints()).append(",");
        json.append("\"epiduralTimeExtensionPoints\":").append(calculator.getEpiduralTimeExtensionPoints()).append(",");
        json.append("\"nerveBlockPoints\":").append(calculator.getNerveBlockPoints()).append(",");
        json.append("\"totalMinutes\":").append(calculator.getTotalMinutes()).append(",");
        json.append("\"highestLevel\":").append(calculator.getHighestLevel()).append(",");
        
        json.append("\"timeExtensionDetails\":[");
        List<AnesthesiaCalculator.TimeExtensionDetail> details = calculator.getTimeExtensionDetails();
        for (int i = 0; i < details.size(); i++) {
            if (i > 0) json.append(",");
            AnesthesiaCalculator.TimeExtensionDetail detail = details.get(i);
            json.append("{\"level\":").append(detail.getLevel()).append(",\"points\":").append(detail.getPoints()).append("}");
        }
        json.append("],");
        
        json.append("\"activeLevels\":[");
        List<AnesthesiaCalculator.AnesthesiaLevelTime> activeLevels = calculator.getActiveLevels();
        for (int i = 0; i < activeLevels.size(); i++) {
            if (i > 0) json.append(",");
            AnesthesiaCalculator.AnesthesiaLevelTime level = activeLevels.get(i);
            json.append("{\"level\":").append(level.getLevel()).append(",\"minutes\":").append(level.getMinutes()).append("}");
        }
        json.append("],");
        
        json.append("\"currentTimes\":[");
        for (int level = 1; level <= 5; level++) {
            if (level > 1) json.append(",");
            json.append(calculator.getTime(level));
        }
        json.append("]");
        
        json.append("}");
        return json.toString();
    }
    
    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css")) return "text/css; charset=UTF-8";
        if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (path.endsWith(".json")) return "application/json; charset=UTF-8";
        return "text/plain; charset=UTF-8";
    }
    
    private String getStaticContent(String path) {
        if ("/index.html".equals(path)) {
            return generateIndexHtml();
        }
        return "404 Not Found";
    }
    
    private String generateIndexHtml() {
        return """
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>麻酔管理料計算アプリ</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Noto Sans JP', sans-serif;
            line-height: 1.6;
            color: #333;
            background-color: #f5f5f7;
        }
        
        .container {
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
        }
        
        h1 {
            text-align: center;
            color: #1d1d1f;
            margin-bottom: 30px;
            font-size: 2.5rem;
            font-weight: 600;
        }
        
        .card {
            background: white;
            border-radius: 12px;
            padding: 24px;
            margin-bottom: 20px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.05);
        }
        
        .section-title {
            font-size: 1.3rem;
            font-weight: 600;
            margin-bottom: 16px;
            color: #1d1d1f;
            border-bottom: 2px solid #007aff;
            padding-bottom: 8px;
        }
        
        .form-group {
            margin-bottom: 20px;
        }
        
        .form-row {
            display: flex;
            align-items: center;
            margin-bottom: 12px;
            gap: 12px;
        }
        
        label {
            font-weight: 500;
            min-width: 100px;
            color: #1d1d1f;
        }
        
        input[type="number"] {
            width: 80px;
            padding: 8px 12px;
            border: 2px solid #d1d1d6;
            border-radius: 8px;
            font-size: 16px;
            transition: border-color 0.2s;
        }
        
        input[type="number"]:focus {
            outline: none;
            border-color: #007aff;
        }
        
        select {
            padding: 8px 12px;
            border: 2px solid #d1d1d6;
            border-radius: 8px;
            font-size: 16px;
            background-color: white;
            min-width: 120px;
        }
        
        select:focus {
            outline: none;
            border-color: #007aff;
        }
        
        .checkbox-group {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 12px;
        }
        
        input[type="checkbox"] {
            width: 18px;
            height: 18px;
            accent-color: #007aff;
        }
        
        .result-card {
            background: linear-gradient(135deg, #007aff, #5856d6);
            color: white;
            text-align: center;
        }
        
        .total-points {
            font-size: 3rem;
            font-weight: 700;
            margin-bottom: 8px;
        }
        
        .total-yen {
            font-size: 1.2rem;
            opacity: 0.9;
        }
        
        .details-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 16px;
            margin-top: 20px;
        }
        
        .detail-item {
            background: #f8f9fa;
            padding: 16px;
            border-radius: 8px;
            border-left: 4px solid #007aff;
        }
        
        .detail-label {
            font-size: 0.9rem;
            color: #666;
            margin-bottom: 4px;
        }
        
        .detail-value {
            font-size: 1.1rem;
            font-weight: 600;
            color: #1d1d1f;
        }
        
        .extension-details {
            margin-top: 8px;
            font-size: 0.85rem;
            color: #666;
        }
        
        .button-group {
            display: flex;
            gap: 12px;
            justify-content: center;
            margin-top: 20px;
        }
        
        button {
            padding: 12px 24px;
            border: none;
            border-radius: 8px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s;
        }
        
        .btn-primary {
            background-color: #007aff;
            color: white;
        }
        
        .btn-primary:hover {
            background-color: #0056b3;
        }
        
        .btn-secondary {
            background-color: #8e8e93;
            color: white;
        }
        
        .btn-secondary:hover {
            background-color: #6d6d70;
        }
        
        .summary-info {
            background: #e3f2fd;
            border: 1px solid #bbdefb;
            border-radius: 8px;
            padding: 16px;
            margin-top: 20px;
        }
        
        .summary-info h4 {
            color: #1565c0;
            margin-bottom: 8px;
        }
        
        .notice {
            background: #fff3cd;
            border: 1px solid #ffeaa7;
            border-radius: 8px;
            padding: 16px;
            margin-top: 20px;
            font-size: 0.9rem;
            line-height: 1.5;
        }
        
        .notice h4 {
            color: #856404;
            margin-bottom: 8px;
        }
        
        .notice ul {
            margin-left: 20px;
        }
        
        .notice li {
            margin-bottom: 4px;
        }
        
        @media (max-width: 600px) {
            .container {
                padding: 10px;
            }
            
            h1 {
                font-size: 2rem;
            }
            
            .details-grid {
                grid-template-columns: 1fr;
            }
            
            .form-row {
                flex-direction: column;
                align-items: flex-start;
            }
            
            label {
                min-width: auto;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>麻酔管理料計算アプリ</h1>
        
        <form id="calculationForm">
            <!-- 基本設定 -->
            <div class="card">
                <h3 class="section-title">基本設定</h3>
                
                <div class="form-group">
                    <div class="form-row">
                        <label>重症度:</label>
                        <select name="severity" id="severity">
                            <option value="normal">ロ（通常患者）</option>
                            <option value="difficult">イ（困難患者）</option>
                        </select>
                    </div>
                </div>
                
                <div class="form-group">
                    <h4>麻酔時間（レベル別）</h4>
                    <div class="form-row">
                        <label>レベル1:</label>
                        <input type="number" name="level1" id="level1" min="0" placeholder="分">
                        <span>分</span>
                    </div>
                    <div class="form-row">
                        <label>レベル2:</label>
                        <input type="number" name="level2" id="level2" min="0" placeholder="分">
                        <span>分</span>
                    </div>
                    <div class="form-row">
                        <label>レベル3:</label>
                        <input type="number" name="level3" id="level3" min="0" placeholder="分">
                        <span>分</span>
                    </div>
                    <div class="form-row">
                        <label>レベル4:</label>
                        <input type="number" name="level4" id="level4" min="0" placeholder="分">
                        <span>分</span>
                    </div>
                    <div class="form-row">
                        <label>レベル5:</label>
                        <input type="number" name="level5" id="level5" min="0" placeholder="分">
                        <span>分</span>
                    </div>
                </div>
            </div>
            
            <!-- 追加設定 -->
            <div class="card">
                <h3 class="section-title">追加設定</h3>
                
                <div class="form-group">
                    <div class="checkbox-group">
                        <input type="checkbox" name="hasEpidural" id="hasEpidural">
                        <label for="hasEpidural">硬膜外麻酔を実施</label>
                    </div>
                    <div class="form-row" id="epiduralLocationRow" style="display: none;">
                        <label>部位:</label>
                        <select name="epiduralLocation" id="epiduralLocation">
                            <option value="lumbar">腰部</option>
                            <option value="cervical">頸・胸部</option>
                            <option value="sacral">仙骨部</option>
                        </select>
                    </div>
                </div>
                
                <div class="form-group">
                    <div class="checkbox-group">
                        <input type="checkbox" name="hasNerveBlock" id="hasNerveBlock">
                        <label for="hasNerveBlock">神経ブロックを実施</label>
                    </div>
                    <div class="form-row" id="nerveBlockTypeRow" style="display: none;">
                        <label>種類:</label>
                        <select name="nerveBlockType" id="nerveBlockType">
                            <option value="other">その他</option>
                            <option value="epiduralSubstitute">硬膜外代用</option>
                        </select>
                    </div>
                </div>
                
                <div class="form-group">
                    <div class="checkbox-group">
                        <input type="checkbox" name="hasOvertime" id="hasOvertime">
                        <label for="hasOvertime">時間外加算（1.4倍）</label>
                    </div>
                    <div class="checkbox-group">
                        <input type="checkbox" name="hasNightHoliday" id="hasNightHoliday">
                        <label for="hasNightHoliday">深夜・休日加算（1.8倍）</label>
                    </div>
                </div>
            </div>
            
            <div class="button-group">
                <button type="submit" class="btn-primary">計算実行</button>
                <button type="button" class="btn-secondary" onclick="resetForm()">リセット</button>
            </div>
        </form>
        
        <!-- 計算結果 -->
        <div class="card result-card" id="resultCard" style="display: none;">
            <div class="total-points" id="totalPoints">0点</div>
            <div class="total-yen" id="totalYen">¥0</div>
        </div>
        
        <!-- 詳細結果 -->
        <div class="card" id="detailsCard" style="display: none;">
            <h3 class="section-title">計算詳細</h3>
            <div class="details-grid" id="detailsGrid">
                <!-- 詳細項目がここに動的に追加されます -->
            </div>
            
            <div class="summary-info" id="summaryInfo">
                <!-- 計算サマリーがここに表示されます -->
            </div>
        </div>
        
        <!-- 注意事項 -->
        <div class="notice">
            <h4>注意事項</h4>
            <ul>
                <li>最も高いレベルの麻酔を基準として基本点数を算定</li>
                <li>2時間を超えた場合、30分単位で時間延長加算</li>
                <li>硬膜外麻酔の時間延長は全身麻酔時間に基づく</li>
                <li>神経ブロック硬膜外代用は硬膜外麻酔と併用不可</li>
                <li>時間外・深夜休日加算は診療報酬算定要件を満たす場合のみ適用</li>
                <li>本アプリは計算支援ツールです。実際の請求は最新の診療報酬点数表を確認してください</li>
            </ul>
        </div>
    </div>
    
    <script>
        // フォーム要素の参照
        const form = document.getElementById('calculationForm');
        const hasEpiduralCheckbox = document.getElementById('hasEpidural');
        const epiduralLocationRow = document.getElementById('epiduralLocationRow');
        const hasNerveBlockCheckbox = document.getElementById('hasNerveBlock');
        const nerveBlockTypeRow = document.getElementById('nerveBlockTypeRow');
        
        // チェックボックスの状態に応じて関連要素の表示/非表示を切り替え
        hasEpiduralCheckbox.addEventListener('change', function() {
            epiduralLocationRow.style.display = this.checked ? 'flex' : 'none';
        });
        
        hasNerveBlockCheckbox.addEventListener('change', function() {
            nerveBlockTypeRow.style.display = this.checked ? 'flex' : 'none';
        });
        
        // フォーム送信処理
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            calculateResults();
        });
        
        // 入力値変更時の自動計算
        form.addEventListener('input', calculateResults);
        form.addEventListener('change', calculateResults);
        
        // 計算実行関数
        async function calculateResults() {
            const formData = new FormData(form);
            
            try {
                const response = await fetch('/api/update', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: new URLSearchParams(formData)
                });
                
                const result = await response.json();
                displayResults(result);
            } catch (error) {
                console.error('計算エラー:', error);
            }
        }
        
        // 結果表示関数
        function displayResults(result) {
            const resultCard = document.getElementById('resultCard');
            const detailsCard = document.getElementById('detailsCard');
            
            if (result.totalMinutes > 0) {
                // 総計表示
                document.getElementById('totalPoints').textContent = result.totalPoints + '点';
                document.getElementById('totalYen').textContent = '¥' + (result.totalPoints * 10).toLocaleString();
                
                resultCard.style.display = 'block';
                detailsCard.style.display = 'block';
                
                // 詳細表示
                displayDetails(result);
                displaySummary(result);
            } else {
                resultCard.style.display = 'none';
                detailsCard.style.display = 'none';
            }
        }
        
        // 詳細項目表示
        function displayDetails(result) {
            const detailsGrid = document.getElementById('detailsGrid');
            detailsGrid.innerHTML = '';
            
            // 基本点数
            if (result.basePoints > 0) {
                addDetailItem(detailsGrid, `基本点数（レベル${result.highestLevel}）`, result.basePoints + '点');
            }
            
            // 時間延長加算
            if (result.timeExtensionPoints > 0) {
                let extensionText = result.timeExtensionPoints + '点';
                if (result.timeExtensionDetails.length > 0) {
                    extensionText += '<div class="extension-details">';
                    result.timeExtensionDetails.forEach(detail => {
                        extensionText += `レベル${detail.level}: ${detail.points}点<br>`;
                    });
                    extensionText += '</div>';
                }
                addDetailItem(detailsGrid, '時間延長加算', extensionText);
            }
            
            // 硬膜外麻酔加算
            if (result.epiduralPoints > 0) {
                addDetailItem(detailsGrid, '硬膜外麻酔加算', result.epiduralPoints + '点');
            }
            
            // 硬膜外時間延長加算
            if (result.epiduralTimeExtensionPoints > 0) {
                addDetailItem(detailsGrid, '硬膜外時間延長加算', result.epiduralTimeExtensionPoints + '点');
            }
            
            // 神経ブロック加算
            if (result.nerveBlockPoints > 0) {
                addDetailItem(detailsGrid, '神経ブロック加算', result.nerveBlockPoints + '点');
            }
        }
        
        // 詳細項目追加
        function addDetailItem(container, label, value) {
            const item = document.createElement('div');
            item.className = 'detail-item';
            item.innerHTML = `
                <div class="detail-label">${label}</div>
                <div class="detail-value">${value}</div>
            `;
            container.appendChild(item);
        }
        
        // サマリー表示
        function displaySummary(result) {
            const summaryInfo = document.getElementById('summaryInfo');
            
            let activeLevelsText = result.activeLevels.map(level => 
                `レベル${level.level}(${level.minutes}分)`
            ).join(', ');
            
            if (activeLevelsText === '') {
                activeLevelsText = 'なし';
            }
            
            let extensionTime = Math.max(0, result.totalMinutes - 120);
            
            summaryInfo.innerHTML = `
                <h4>計算の詳細</h4>
                <p><strong>使用レベル:</strong> ${activeLevelsText}</p>
                <p><strong>最高レベル:</strong> レベル${result.highestLevel}</p>
                <p><strong>総麻酔時間:</strong> ${result.totalMinutes}分 (${Math.floor(result.totalMinutes / 60)}時間${result.totalMinutes % 60}分)</p>
                ${extensionTime > 0 ? `<p><strong>延長時間:</strong> ${extensionTime}分</p>` : ''}
            `;
        }
        
        // リセット関数
        async function resetForm() {
            try {
                await fetch('/api/reset', { method: 'POST' });
                form.reset();
                document.getElementById('resultCard').style.display = 'none';
                document.getElementById('detailsCard').style.display = 'none';
                epiduralLocationRow.style.display = 'none';
                nerveBlockTypeRow.style.display = 'none';
            } catch (error) {
                console.error('リセットエラー:', error);
            }
        }
        
        // 初期計算実行
        calculateResults();
    </script>
</body>
</html>
        """;
    }
    
    public static void main(String[] args) {
        try {
            AnesthesiaWebApp app = new AnesthesiaWebApp();
            app.start();
            
            Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
            
            System.out.println("アプリを停止するには Ctrl+C を押してください");
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("アプリケーション起動エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }
}