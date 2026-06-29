// 取得熱圖數據並渲染（Sector > Industry > Stock 三層，仿 Finviz 的分類版面，
// 數值越大的產業/個股排在左上、越小的往右下排列）
const FONT_MONO = "'JetBrains Mono', monospace";

// 漲跌幅 -3% ~ +3% 對應連續色階（超過範圍會被夾住），比固定幾段顏色更細膩
// 改成 Cyberpunk 配色：跌 → 霓虹洋紅紅、平盤 → 深紫黑、漲 → 霓虹綠
const colorScale = d3.scaleLinear()
    .domain([-3, 0, 3])
    .range(['#ff2155', '#1a1626', '#0aff9d'])
    .clamp(true);

function formatPct(pct) {
    return pct !== null && pct !== undefined ? `${pct > 0 ? '+' : ''}${pct.toFixed(2)}%` : '';
}

// svg 只建立一次，之後每次刷新只更新裡面的元素（顏色/文字/位置），
// 不整個拆掉重建，這樣畫面才不會閃爍（twinkle）。
let svg = null;

async function loadHeatmap() {
    const response = await fetch(`${API_BASE}/api/heatmap`);
    const stocks = await response.json();

    // 兩層分組：先按 sector，再按 industry
    const grouped = d3.group(stocks, d => d.sector || 'Other', d => d.industry || 'Other');

    const width = window.innerWidth - 40;
    const height = window.innerHeight - 100;

    const root = d3.hierarchy({
        name: 'root',
        children: Array.from(grouped, ([sector, industries]) => ({
            name: sector,
            children: Array.from(industries, ([industry, items]) => ({
                name: industry,
                children: items.map(s => ({
                    name: s.symbol,
                    value: s.marketCap || 1,
                    changePct: s.marketPriceChgPct,
                    stockId: s.stockId
                }))
            }))
        }))
    })
        .sum(d => d.value)
        // 按 value 從大到小排序，讓大產業/大公司排在左上、小的往右下，跟 Finviz 一致
        .sort((a, b) => b.value - a.value);

    // depth 0 = root, depth 1 = sector, depth 2 = industry, depth 3 = stock（leaf）
    // paddingTop 在 sector / industry 的格子裡留出空間放分類標籤色塊；
    // paddingOuter 縮小、不再畫外框，讓格子之間更貼緊，接近 Finviz 的無縫排列
    d3.treemap()
        .tile(d3.treemapResquarify)
        .size([width, height])
        .paddingOuter(d => (d.depth === 1 ? 2 : 1))
        .paddingTop(d => (d.depth === 1 ? 17 : d.depth === 2 ? 14 : 0))
        .paddingInner(1)
        .round(true)
        (root);

    // 第一次載入才建立 svg；之後的刷新沿用同一個 svg 元素
    if (!svg) {
        svg = d3.select('#heatmap-container')
            .append('svg')
            .style('font-family', FONT_MONO);
    }
    svg.attr('width', width).attr('height', height);

    // --- 第一層：Sector 標籤（頂部色塊 + 細霓虹底線，不另外畫外框）---
    const sectorNodes = root.descendants().filter(d => d.depth === 1);

    svg.selectAll('g.sector')
        .data(sectorNodes, d => d.data.name)
        .join(
            enter => {
                const g = enter.append('g')
                    .attr('class', 'sector')
                    .style('pointer-events', 'none')
                    .attr('transform', d => `translate(${d.x0},${d.y0})`);

                g.append('rect')
                    .attr('class', 'sector-bg')
                    .attr('width', d => Math.max(0, d.x1 - d.x0))
                    .attr('height', 16)
                    .attr('fill', '#13001a');

                g.append('rect')
                    .attr('class', 'sector-underline')
                    .attr('width', d => Math.max(0, d.x1 - d.x0))
                    .attr('height', 1.5)
                    .attr('y', 16)
                    .attr('fill', '#ff2bd6')
                    .style('filter', 'drop-shadow(0 0 4px rgba(255, 43, 214, 0.85))');

                g.append('text')
                    .attr('x', 4)
                    .attr('y', 12)
                    .attr('fill', '#ff6be8')
                    .attr('font-size', '12px')
                    .attr('font-weight', '700')
                    .attr('letter-spacing', '0.5px')
                    .style('filter', 'drop-shadow(0 0 3px rgba(255, 43, 214, 0.9))')
                    .text(d => d.data.name.toUpperCase());

                return g;
            },
            update => {
                update.attr('transform', d => `translate(${d.x0},${d.y0})`);
                update.select('rect.sector-bg').attr('width', d => Math.max(0, d.x1 - d.x0));
                update.select('rect.sector-underline').attr('width', d => Math.max(0, d.x1 - d.x0));
                update.select('text').text(d => d.data.name.toUpperCase());
                return update;
            },
            exit => exit.remove()
        );

    // --- 第二層：Industry 標籤（小色塊，不另外畫外框）---
    const industryNodes = root.descendants().filter(d => d.depth === 2);
    const industryKey = d => `${d.parent.data.name}|${d.data.name}`;

    svg.selectAll('g.industry')
        .data(industryNodes, industryKey)
        .join(
            enter => {
                const g = enter.append('g')
                    .attr('class', 'industry')
                    .style('pointer-events', 'none')
                    .attr('transform', d => `translate(${d.x0},${d.y0})`);

                // 格子太小（容不下文字）就不放標籤，避免擠成一團
                const labeled = g.filter(d => (d.x1 - d.x0) >= 50 && (d.y1 - d.y0) >= 28);

                labeled.append('rect')
                    .attr('class', 'industry-bg')
                    .attr('width', d => Math.max(0, d.x1 - d.x0))
                    .attr('height', 13)
                    .attr('fill', '#001a1a');

                labeled.append('text')
                    .attr('x', 3)
                    .attr('y', 10)
                    .attr('fill', '#5df5ec')
                    .attr('font-size', '9px')
                    .style('filter', 'drop-shadow(0 0 2px rgba(0, 255, 242, 0.7))')
                    .text(d => d.data.name.toUpperCase());

                return g;
            },
            update => {
                update.attr('transform', d => `translate(${d.x0},${d.y0})`);
                update.select('rect.industry-bg').attr('width', d => Math.max(0, d.x1 - d.x0));
                update.select('text').text(d => d.data.name.toUpperCase());
                return update;
            },
            exit => exit.remove()
        );

    // --- 第三層：個股格子（連續色階上色 + 文字防溢出）---
    svg.selectAll('g.stock')
        .data(root.leaves(), d => d.data.stockId)
        .join(
            enter => {
                const g = enter.append('g')
                    .attr('class', 'stock')
                    .attr('transform', d => `translate(${d.x0},${d.y0})`)
                    .style('cursor', 'pointer')
                    .on('click', (event, d) => {
                        // 點擊跳轉到 K線圖頁面（靜態版用 query string 帶 stockId）
                        window.location.href = `stock.html?id=${d.data.stockId}`;
                    });

                g.append('rect')
                    .attr('class', 'cell-bg')
                    .attr('width', d => Math.max(0, d.x1 - d.x0))
                    .attr('height', d => Math.max(0, d.y1 - d.y0))
                    .attr('fill', d => (d.data.changePct == null ? '#1a1626' : colorScale(d.data.changePct)))
                    .attr('stroke', '#0a0014')
                    .attr('stroke-width', 0.6);

                // 用 clipPath 把文字限制在自己的格子範圍內，避免格子太小時文字溢出、
                // 跟隔壁格子的文字疊在一起
                g.append('clipPath')
                    .attr('id', d => `clip-${d.data.stockId}`)
                    .append('rect')
                    .attr('class', 'clip-rect')
                    .attr('width', d => Math.max(0, d.x1 - d.x0))
                    .attr('height', d => Math.max(0, d.y1 - d.y0));

                // 股票代號：格子太小（小於 30x22）就不顯示，避免擠成一團
                g.filter(d => (d.x1 - d.x0) >= 30 && (d.y1 - d.y0) >= 22)
                    .append('text')
                    .attr('class', 'cell-symbol')
                    .attr('clip-path', d => `url(#clip-${d.data.stockId})`)
                    .attr('x', d => (d.x1 - d.x0) / 2)
                    .attr('y', d => (d.y1 - d.y0) / 2 - 6)
                    .attr('text-anchor', 'middle')
                    .attr('fill', '#f4f8fa')
                    .attr('font-size', '12px')
                    .attr('font-weight', '600')
                    .text(d => d.data.name);

                // 漲跌幅：格子要更大一點（兩行文字需要更多高度）才顯示
                g.filter(d => (d.x1 - d.x0) >= 36 && (d.y1 - d.y0) >= 34)
                    .append('text')
                    .attr('class', 'cell-pct')
                    .attr('clip-path', d => `url(#clip-${d.data.stockId})`)
                    .attr('x', d => (d.x1 - d.x0) / 2)
                    .attr('y', d => (d.y1 - d.y0) / 2 + 10)
                    .attr('text-anchor', 'middle')
                    .attr('fill', '#f4f8fa')
                    .attr('font-size', '10px')
                    .text(d => formatPct(d.data.changePct));

                return g;
            },
            update => {
                // 位置/大小理論上很少變（treemap 是按市值排版），但保留更新邏輯
                // 以防股票名單或市值排序變動。
                update.attr('transform', d => `translate(${d.x0},${d.y0})`);

                update.select('rect.cell-bg')
                    .attr('width', d => Math.max(0, d.x1 - d.x0))
                    .attr('height', d => Math.max(0, d.y1 - d.y0))
                    .transition()
                    .duration(600)
                    .attr('fill', d => (d.data.changePct == null ? '#1a1626' : colorScale(d.data.changePct)));

                update.select('clipPath rect.clip-rect')
                    .attr('width', d => Math.max(0, d.x1 - d.x0))
                    .attr('height', d => Math.max(0, d.y1 - d.y0));

                // 只更新文字內容，不重建節點，所以不會閃爍
                update.select('text.cell-symbol').text(d => d.data.name);
                update.select('text.cell-pct').text(d => formatPct(d.data.changePct));

                return update;
            },
            exit => exit.remove()
        );

    // 更新右上角「最後更新時間」字樣
    const statusEl = document.getElementById('last-updated');
    if (statusEl) {
        statusEl.textContent = new Date().toLocaleTimeString();
    }
}

// 初次載入
loadHeatmap();

// 每 30 秒自動刷新（setInterval）——只更新資料，不重建整個 svg，避免畫面閃爍
setInterval(loadHeatmap, 30000);
