// 取得熱圖數據並渲染（Sector > Industry > Stock 三層，仿 Finviz 的分類版面）
async function loadHeatmap() {
    const response = await fetch('/api/heatmap');
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
    }).sum(d => d.value);

    // depth 0 = root, depth 1 = sector, depth 2 = industry, depth 3 = stock（leaf）
    // paddingTop 在 sector / industry 的格子裡留出空間放分類標籤
    d3.treemap()
        .tile(d3.treemapResquarify)
        .size([width, height])
        .paddingOuter(d => (d.depth === 1 ? 4 : 2))
        .paddingTop(d => (d.depth === 1 ? 20 : d.depth === 2 ? 15 : 0))
        .paddingInner(1)
        .round(true)
        (root);

    const svg = d3.select('#heatmap-container')
        .append('svg')
        .attr('width', width)
        .attr('height', height);

    // --- 第一層：Sector 外框 + 標籤（仿 Finviz 左上角大分類字樣）---
    const sectorNodes = root.descendants().filter(d => d.depth === 1);

    svg.selectAll('g.sector')
        .data(sectorNodes)
        .enter().append('g')
        .attr('class', 'sector')
        .style('pointer-events', 'none')
        .attr('transform', d => `translate(${d.x0},${d.y0})`)
        .call(g => {
            g.append('rect')
                .attr('width', d => Math.max(0, d.x1 - d.x0))
                .attr('height', d => Math.max(0, d.y1 - d.y0))
                .attr('fill', 'none')
                .attr('stroke', '#f1c40f')
                .attr('stroke-width', 1.5);

            g.append('text')
                .attr('x', 4)
                .attr('y', 13)
                .attr('fill', '#f1c40f')
                .attr('font-size', '12px')
                .attr('font-weight', 'bold')
                .text(d => d.data.name.toUpperCase());
        });

    // --- 第二層：Industry 外框 + 標籤（仿 Finviz 小分類字樣）---
    const industryNodes = root.descendants().filter(d => d.depth === 2);

    svg.selectAll('g.industry')
        .data(industryNodes)
        .enter().append('g')
        .attr('class', 'industry')
        .style('pointer-events', 'none')
        .attr('transform', d => `translate(${d.x0},${d.y0})`)
        .call(g => {
            g.append('rect')
                .attr('width', d => Math.max(0, d.x1 - d.x0))
                .attr('height', d => Math.max(0, d.y1 - d.y0))
                .attr('fill', 'none')
                .attr('stroke', '#bdc3c7')
                .attr('stroke-width', 0.75)
                .attr('stroke-opacity', 0.6);

            // 格子太小（容不下文字）就不放 industry 標籤，避免擠成一團
            g.filter(d => (d.x1 - d.x0) >= 50 && (d.y1 - d.y0) >= 28)
                .append('text')
                .attr('x', 3)
                .attr('y', 10)
                .attr('fill', '#dfe6e9')
                .attr('font-size', '9px')
                .text(d => d.data.name.toUpperCase());
        });

    // --- 第三層：個股格子（沿用先前的上色 + 文字防溢出邏輯）---
    const cells = svg.selectAll('g.stock')
        .data(root.leaves())
        .enter().append('g')
        .attr('class', 'stock')
        .attr('transform', d => `translate(${d.x0},${d.y0})`)
        .style('cursor', 'pointer')
        .on('click', (event, d) => {
            // 點擊跳轉到 K線圖頁面
            window.location.href = `/stock/${d.data.stockId}`;
        });

    cells.append('rect')
        .attr('width', d => Math.max(0, d.x1 - d.x0))
        .attr('height', d => Math.max(0, d.y1 - d.y0))
        .attr('fill', d => {
            const pct = d.data.changePct || 0;
            if (pct > 2) return '#1a9e5c';      // 大漲 → 深綠
            if (pct > 0) return '#2ecc71';       // 小漲 → 淺綠
            if (pct < -2) return '#c0392b';      // 大跌 → 深紅
            if (pct < 0) return '#e74c3c';       // 小跌 → 淺紅
            return '#7f8c8d';                     // 平盤 → 灰色
        })
        .attr('stroke', '#1c1c1c')
        .attr('stroke-width', 0.5);

    // 用 clipPath 把文字限制在自己的格子範圍內，避免格子太小時文字溢出、
    // 跟隔壁格子的文字疊在一起
    cells.append('clipPath')
        .attr('id', d => `clip-${d.data.stockId}`)
        .append('rect')
        .attr('width', d => Math.max(0, d.x1 - d.x0))
        .attr('height', d => Math.max(0, d.y1 - d.y0));

    // 股票代號：格子太小（小於 30x22）就不顯示，避免擠成一團
    cells.filter(d => (d.x1 - d.x0) >= 30 && (d.y1 - d.y0) >= 22)
        .append('text')
        .attr('clip-path', d => `url(#clip-${d.data.stockId})`)
        .attr('x', d => (d.x1 - d.x0) / 2)
        .attr('y', d => (d.y1 - d.y0) / 2 - 6)
        .attr('text-anchor', 'middle')
        .attr('fill', 'white')
        .attr('font-size', '12px')
        .attr('font-weight', 'bold')
        .text(d => d.data.name);

    // 漲跌幅：格子要更大一點（兩行文字需要更多高度）才顯示
    cells.filter(d => (d.x1 - d.x0) >= 36 && (d.y1 - d.y0) >= 34)
        .append('text')
        .attr('clip-path', d => `url(#clip-${d.data.stockId})`)
        .attr('x', d => (d.x1 - d.x0) / 2)
        .attr('y', d => (d.y1 - d.y0) / 2 + 10)
        .attr('text-anchor', 'middle')
        .attr('fill', 'white')
        .attr('font-size', '10px')
        .text(d => {
            const pct = d.data.changePct;
            return pct !== null ? `${pct > 0 ? '+' : ''}${pct?.toFixed(2)}%` : '';
        });
}

// 初次載入
loadHeatmap();

// 每 30 秒自動刷新（setInterval）
setInterval(() => {
    d3.select('#heatmap-container svg').remove();
    loadHeatmap();
}, 30000);
