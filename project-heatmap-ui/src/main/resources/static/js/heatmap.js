// 取得熱圖數據並渲染
async function loadHeatmap() {
    const response = await fetch('/api/heatmap');
    const stocks = await response.json();

    // 按行業分組
    const grouped = d3.group(stocks, d => d.industry);

    // 用 D3 Treemap 渲染熱圖
    const width = window.innerWidth - 40;
    const height = window.innerHeight - 100;

    const root = d3.hierarchy({
        name: "root",
        children: Array.from(grouped, ([industry, children]) => ({
            name: industry,
            children: children.map(s => ({
                name: s.symbol,
                value: s.marketCap || 1,
                changePct: s.marketPriceChgPct,
                stockId: s.stockId
            }))
        }))
    }).sum(d => d.value);

    d3.treemap().size([width, height]).padding(2)(root);

    const svg = d3.select('#heatmap-container')
        .append('svg')
        .attr('width', width)
        .attr('height', height);

    const cells = svg.selectAll('g')
        .data(root.leaves())
        .enter().append('g')
        .attr('transform', d => `translate(${d.x0},${d.y0})`)
        .style('cursor', 'pointer')
        .on('click', (event, d) => {
            // 點擊跳轉到 K線圖頁面
            window.location.href = `/stock/${d.data.stockId}`;
        });

    cells.append('rect')
        .attr('width', d => d.x1 - d.x0)
        .attr('height', d => d.y1 - d.y0)
        .attr('fill', d => {
            const pct = d.data.changePct || 0;
            if (pct > 2) return '#1a9e5c';      // 大漲 → 深綠
            if (pct > 0) return '#2ecc71';       // 小漲 → 淺綠
            if (pct < -2) return '#c0392b';      // 大跌 → 深紅
            if (pct < 0) return '#e74c3c';       // 小跌 → 淺紅
            return '#7f8c8d';                     // 平盤 → 灰色
        });

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