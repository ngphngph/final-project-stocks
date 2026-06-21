# 部署指南：Railway（3 個 Spring Boot）+ Supabase（資料庫）

## 為什麼不用 Vercel？

`project-heatmap-ui` 是一個 **Spring Boot（Java + Thymeleaf）** app，不是純前端的 React/Next.js 專案。它同時負責「畫面 render」跟「轉發 API 請求給 project-stock-data」。Vercel 只能跑靜態網頁或 Node.js serverless function，不能跑 Java 長駐進程，所以這個架構沒辦法放 Vercel。

決定：**3 個 Spring Boot app（data-provider / stock-data / heatmap-ui）全部放 Railway**，資料庫用 **Supabase（Postgres）**，Redis 用 **Railway 自己的 Redis 服務**（Supabase 沒有 Redis）。

```
瀏覽器
  │
  ▼
[Railway] project-heatmap-ui (8080, 對外開放網域)
  │  WebClient 呼叫（伺服器對伺服器，沒有 CORS 問題）
  ▼
[Railway] project-stock-data (8102, 內部網路即可)
  │                              │
  │ JPA                          │ WebClient
  ▼                              ▼
[Supabase] Postgres      [Railway] project-data-provider (8101, 內部網路即可)
  ▲                              │
  │ Redis 快取                   │ HTTPS
[Railway] Redis                Finnhub API
```

只有 `project-heatmap-ui` 需要對外公開網域，另外兩個只要服務之間能互通就好（用 Railway 的內部網路，比較安全也比較快）。

---

## 已經幫你改好的程式碼

三個 app 的 `application.yml` 已經改成「讀環境變數，本機沒設定就用原本的 localhost 預設值」，所以本機 docker-compose 開發流程完全不受影響，雲端只要設環境變數即可：

| App | 檔案 | 改了什麼 |
|---|---|---|
| project-data-provider | `application.yml` | `server.port` → `${PORT:8101}`；Finnhub key/url → 可用環境變數覆蓋 |
| project-stock-data | `application.yml` | `server.port`、DB url/帳密、Redis host/port/password、data-provider 網址 → 全部可用環境變數覆蓋 |
| project-heatmap-ui | `application.yml` | `server.port`、stock-data 網址 → 可用環境變數覆蓋 |

---

## Step 1：Supabase 建立 Postgres

1. 到 supabase.com 註冊 → New Project → 設定密碼（記下來）
2. 專案建好後，點頁面上方的綠色 **Connect** 按鈕（在 `main` / `PRODUCTION` 旁邊，新版 UI 已經把連線字串移到這裡，不在 Project Settings → Database 了）
3. 選 **URI** 分頁，會看到類似這樣的連線字串：
   ```
   postgresql://postgres:[YOUR-PASSWORD]@db.xxxxxxxxxxxx.supabase.co:5432/postgres
   ```
   注意密碼部分可能已經是 **URL-encoded**（例如 `%` 變成 `%25`、`/` 變成 `%2F`、`&` 變成 `%26`）。

4. Railway 的環境變數要拆成 3 個獨立的值（不是整串 URI），所以：
   - `DATABASE_URL`（拿 URI 裡 `@` 後面那段，轉成 JDBC 格式，開頭加 `jdbc:`、結尾加 `?sslmode=require`）：
     ```
     jdbc:postgresql://db.xxxxxxxxxxxx.supabase.co:5432/postgres?sslmode=require
     ```
   - `DATABASE_USERNAME`：`postgres`
   - `DATABASE_PASSWORD`：把密碼**還原成原始字元**再填（因為這是獨立欄位，不是塞進 URI 字串裡，不需要 URL-encode）。例如 URI 裡看到 `tJeDNPb%25%2Fjkj%2F7%26`，還原後填 `tJeDNPb%/jkj/7&`。

> 注意：Supabase 預設資料庫名稱是 `postgres`，不是你本機用的 `bootcamp_stockwatch`，沒關係，表會自動建在這個 db 裡。

> ⚠️ 真實密碼只能填在 Railway 的 Variables 介面裡，絕對不要寫進 `application.yml` 或任何會 commit 進 git 的檔案。

---

## Step 2：Railway 建立 Redis

1. 到 railway.app 註冊 → New Project
2. 在同一個 Project 裡點 **+ New → Database → Add Redis**
3. 建好後點進 Redis 服務的 **Variables** tab，記下 `REDISHOST`、`REDISPORT`、`REDISPASSWORD`（或是直接給的 `REDIS_URL`）

---

## Step 3：把 repo 推上 GitHub

Railway 是從 GitHub repo 部署的。如果 `final-project` 還沒推上 GitHub：

```bash
cd final-project
git init
git add .
git commit -m "ready for deployment"
git remote add origin https://github.com/<你的帳號>/final-project.git
git push -u origin main
```

---

## Step 4：Railway 建立 3 個 Service（同一個 repo，不同 root directory）

在同一個 Railway Project 裡，重複 3 次「**+ New → GitHub Repo**」，每次都選 `final-project` 這個 repo，但設定不同的 **Root Directory**：

| Service 名稱 | Root Directory | 對外網域 |
|---|---|---|
| data-provider | `project-data-provider` | 不需要（內部即可）|
| stock-data | `project-stock-data` | 不需要（內部即可）|
| heatmap-ui | `project-heatmap-ui` | **需要**，Settings → Networking → Generate Domain |

每個 Service 的 Root Directory 底下有 `Dockerfile`，Railway 會自動偵測並用 Docker build，不需要額外設定 build command。

---

## Step 5：設定環境變數（每個 Service 各自的 Variables tab）

**data-provider 的 Variables：**

| Key | Value |
|---|---|
| `FINNHUB_API_KEY` | 你的 Finnhub key |

**stock-data 的 Variables：**

| Key | Value |
|---|---|
| `DATABASE_URL` | Step 1 拿到的 Supabase JDBC URL |
| `DATABASE_USERNAME` | `postgres` |
| `DATABASE_PASSWORD` | 你的 Supabase 密碼 |
| `REDIS_HOST` | Railway Redis 服務的 `REDISHOST` |
| `REDIS_PORT` | Railway Redis 服務的 `REDISPORT` |
| `REDIS_PASSWORD` | Railway Redis 服務的 `REDISPASSWORD` |
| `DATA_PROVIDER_BASE_URL` | `http://data-provider.railway.internal:8101`（見下方內部網路說明）|

**heatmap-ui 的 Variables：**

| Key | Value |
|---|---|
| `STOCK_DATA_BASE_URL` | `http://stock-data.railway.internal:8102` |

### 關於 Railway 內部網路

同一個 Railway Project 裡的 Service 可以用 `<service名稱>.railway.internal` 這個內部網址互相呼叫，不用對外公開、不用付出流量費、也比走公開網域快。Service 名稱就是你在 Step 4 建立時取的名字（例如建立時叫 `data-provider`，內部網址就是 `data-provider.railway.internal`）。

`PORT` 環境變數 Railway 會自動注入，不用你手動設定 — 我們已經讓 `application.yml` 讀 `${PORT:...}`，會自動套用。

---

## Step 6：部署順序

Railway 通常會自動部署，但建議的「第一次成功啟動」順序：

1. Supabase（資料庫，本來就一直在線）
2. Railway Redis
3. `data-provider`（沒有依賴別人，先上）
4. `stock-data`（依賴 data-provider 的網址 + Supabase + Redis）
5. `heatmap-ui`（依賴 stock-data 的網址）

`stock-data` 第一次啟動時，Hibernate 的 `ddl-auto: update` 會自動在 Supabase 建好 `stocks`、`stock_profiles`、`stock_ohlc_data` 三張表，不用手動建表。

---

## Step 7：對 Supabase 跑 Python ETL

✅ 這一步已經改好了，不用再手動編輯連線字串。兩個 notebook（`_1_load_snp500_symbol.ipynb`、`_2_load_ohlcv_data.ipynb`）的連線 cell 已經改成：

```python
from getpass import getpass
import urllib.parse

db_password = getpass("Supabase 資料庫密碼: ")  # 輸入時不會顯示在畫面上，也不會存進這個檔案
engine = create_engine(
    f"postgresql://postgres:{urllib.parse.quote_plus(db_password)}@db.qhbtmzzltgimlcmrnzye.supabase.co:5432/postgres?sslmode=require"
)
```

直接在 Jupyter 執行這個 cell，跳出輸入框時貼上你的 Supabase 資料庫密碼即可（不用自己做 URL encode，特殊字元會自動處理）。之所以用互動輸入而不是寫死密碼，是因為這兩個檔案會被 git 追蹤、push 上公開的 GitHub repo，寫死密碼會洩漏。

跑完之後資料就會直接進 Supabase，雲端上的 `stock-data` 服務馬上就能讀到。

---

## Step 8：測試

1. 打開 heatmap-ui 的 Railway 對外網址，應該看到 S&P500 熱圖
2. 點一支股票，應該跳到 `/stock/{id}` 看到 K 線圖
3. 如果畫面空白或報錯，依序檢查：
   - Railway 上 `stock-data` 的 Logs：能不能連到 Supabase（看有沒有 SSL/連線錯誤）
   - Railway 上 `data-provider` 的 Logs：Finnhub key 對不對
   - `stock-data` 的 `DATA_PROVIDER_BASE_URL`、`heatmap-ui` 的 `STOCK_DATA_BASE_URL` 是不是正確指向內部網址

---

## 費用提醒

Railway、Supabase 都有免費額度，但都需要綁信用卡或有使用上限（Railway 免費額度用完會停止服務）。實際扣款/升級方案的決定請你自己在各平台後台操作，我不會替你刷卡或送出付款表單。
