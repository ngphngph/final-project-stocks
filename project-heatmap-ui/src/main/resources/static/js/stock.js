// K線圖（Candlestick Chart）渲染
async function loadStockDetail() {
    const infoEl = document.getElementById('stock-info');
    const stockId = infoEl.getAttribute('data-stock-id');

    const response = await fetch(`/api/ohlc?id=${stockId}`);
    if (!response.ok) {
        infoEl.textContent = 'Failed to load stock data.';
        return;
    }
    const detail = await response.json();

    infoEl.innerHTML = `<h2>${detail.symbol} - ${detail.companyName || ''}</h2>
        <p>Industry: ${detail.industry || 'N/A'} | Market Cap: ${detail.marketCap || 'N/A'}</p>`;

    const ohlcs = detail.ohlcs || [];
    if (ohlcs.length === 0) {
        document.getElementById('chart-container').textContent = 'No OHLC data available yet.';
        return;
    }

    drawCandlestick(ohlcs, detail.earningsEvents || []);
}

function drawCandlestick(data, earningsEvents) {
    const container = document.getElementById('chart-container');
    const width = container.clientWidth || window.innerWidth - 40;
    const height = container.clientHeight || 500;
    const margin = { top: 20, right: 30, bottom: 30, left: 50 };

    // 在 K 線下方、x 軸線上方留一條 18px 的「事件標記」專用走道，
    // 放財報（E）標記，跟 K 棒分開，避免互相遮擋。
    const EVENTS_LANE_HEIGHT = 18;
    const chartBottom = height - margin.bottom;
    const eventsRowY = chartBottom - EVENTS_LANE_HEIGHT / 2;

    const parsedData = data.map(d => ({
        date: new Date(d.date),
        open: d.open,
        high: d.high,
        low: d.low,
        close: d.close
    }));

    const x = d3.scaleBand()
        .domain(parsedData.map(d => d.date))
        .range([margin.left, width - margin.right])
        .padding(0.3);

    const y = d3.scaleLinear()
        .domain([d3.min(parsedData, d => d.low) * 0.98, d3.max(parsedData, d => d.high) * 1.02])
        .range([chartBottom - EVENTS_LANE_HEIGHT, margin.top]);

    // 找出跟某個日期最接近的那根 K 棒索引（財報日期可能落在非交易日）
    function findNearestIndex(dateStr) {
        const target = new Date(dateStr).getTime();
        let bestIndex = 0;
        let bestDiff = Infinity;
        parsedData.forEach((d, i) => {
            const diff = Math.abs(d.date.getTime() - target);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestIndex = i;
            }
        });
        return bestIndex;
    }

    const svg = d3.select('#chart-container')
        .append('svg')
        .attr('width', width)
        .attr('height', height)
        .style('background', '#1b1b1b');

    svg.append('g')
        .attr('transform', `translate(0,${height - margin.bottom})`)
        .call(d3.axisBottom(x).tickFormat(d3.timeFormat('%m/%d')).tickValues(x.domain().filter((d, i) => i % Math.ceil(parsedData.length / 10) === 0)))
        .attr('color', '#aaa');

    svg.append('g')
        .attr('transform', `translate(${margin.left},0)`)
        .call(d3.axisLeft(y))
        .attr('color', '#aaa');

    svg.selectAll('.wick')
        .data(parsedData)
        .enter().append('line')
        .attr('class', 'wick')
        .attr('x1', d => x(d.date) + x.bandwidth() / 2)
        .attr('x2', d => x(d.date) + x.bandwidth() / 2)
        .attr('y1', d => y(d.high))
        .attr('y2', d => y(d.low))
        .attr('stroke', d => d.close >= d.open ? '#2ecc71' : '#e74c3c');

    svg.selectAll('.body')
        .data(parsedData)
        .enter().append('rect')
        .attr('class', 'body')
        .attr('x', d => x(d.date))
        .attr('y', d => y(Math.max(d.open, d.close)))
        .attr('width', x.bandwidth())
        .attr('height', d => Math.max(1, Math.abs(y(d.open) - y(d.close))))
        .attr('fill', d => d.close >= d.open ? '#2ecc71' : '#e74c3c');

    // --- 事件標記：財報（E 方形），畫在 K 線下方的專用走道上 ---
    const eventsLayer = svg.append('g').attr('class', 'events-layer');

    (earningsEvents || []).forEach(ev => {
        if (!ev.date || parsedData.length === 0) return;
        const idx = findNearestIndex(ev.date);
        const cx = x(parsedData[idx].date) + x.bandwidth() / 2;

        // 顏色依實際 EPS 跟預估 EPS 比較：優於預期＝綠、不如預期＝紅、缺資料＝中性藍
        let fill = '#2dd4ff';
        if (ev.epsActual != null && ev.epsEstimate != null) {
            fill = ev.epsActual >= ev.epsEstimate ? '#2ecc71' : '#e74c3c';
        }

        const g = eventsLayer.append('g').attr('class', 'event-marker event-earnings');
        g.append('rect')
            .attr('x', cx - 7)
            .attr('y', eventsRowY - 7)
            .attr('width', 14)
            .attr('height', 14)
            .attr('rx', 3)
            .attr('fill', '#13131f')
            .attr('stroke', fill)
            .attr('stroke-width', 1.5);
        g.append('text')
            .attr('x', cx)
            .attr('y', eventsRowY + 3)
            .attr('text-anchor', 'middle')
            .attr('font-size', '9px')
            .attr('font-family', "'JetBrains Mono', monospace")
            .attr('fill', fill)
            .text('E');
        g.append('title')
            .text(`財報 Earnings ${ev.date}` +
                (ev.epsActual != null ? `\nEPS 實際: ${ev.epsActual}` : '') +
                (ev.epsEstimate != null ? `\nEPS 預估: ${ev.epsEstimate}` : ''));
    });

    // --- 十字線（crosshair）：滑鼠移到圖上時，顯示對應的日期/價格 ---
    const crosshair = svg.append('g')
        .attr('class', 'crosshair')
        .style('display', 'none')
        .style('pointer-events', 'none');

    crosshair.append('line')
        .attr('class', 'crosshair-x')
        .attr('y1', margin.top)
        .attr('y2', height - margin.bottom)
        .attr('stroke', '#5df5ec')
        .attr('stroke-width', 1)
        .attr('stroke-dasharray', '3,3');

    crosshair.append('line')
        .attr('class', 'crosshair-y')
        .attr('x1', margin.left)
        .attr('x2', width - margin.right)
        .attr('stroke', '#5df5ec')
        .attr('stroke-width', 1)
        .attr('stroke-dasharray', '3,3');

    // 底部日期標籤（跟著十字線的垂直線移動）
    const dateLabel = crosshair.append('g').attr('class', 'crosshair-date-label');
    const dateRect = dateLabel.append('rect')
        .attr('height', 16)
        .attr('y', height - margin.bottom)
        .attr('fill', '#001a1a')
        .attr('stroke', '#5df5ec')
        .attr('stroke-width', 1);
    const dateText = dateLabel.append('text')
        .attr('fill', '#5df5ec')
        .attr('font-size', '10px')
        .attr('font-family', "'JetBrains Mono', monospace")
        .attr('y', height - margin.bottom + 11)
        .attr('text-anchor', 'middle');

    // 左側價格標籤（跟著十字線的水平線移動）
    const priceLabel = crosshair.append('g').attr('class', 'crosshair-price-label');
    const priceRect = priceLabel.append('rect')
        .attr('height', 16)
        .attr('fill', '#13001a')
        .attr('stroke', '#ff2bd6')
        .attr('stroke-width', 1);
    const priceText = priceLabel.append('text')
        .attr('fill', '#ff6be8')
        .attr('font-size', '10px')
        .attr('font-family', "'JetBrains Mono', monospace")
        .attr('text-anchor', 'middle');

    // 依座標更新十字線、日期/價格標籤的共用函式（滑鼠跟觸控共用同一套邏輯）
    function updateCrosshair(mx, my) {
        const step = x.step();

        // 依 x 座標換算出最接近的資料索引，吸附到那根 K 棒的中心
        let index = Math.round((mx - margin.left) / step);
        index = Math.max(0, Math.min(parsedData.length - 1, index));
        const d = parsedData[index];

        const snappedX = x(d.date) + x.bandwidth() / 2;
        const clampedY = Math.max(margin.top, Math.min(height - margin.bottom, my));
        const price = y.invert(clampedY);

        crosshair.style('display', null);
        crosshair.select('.crosshair-x').attr('x1', snappedX).attr('x2', snappedX);
        crosshair.select('.crosshair-y').attr('y1', clampedY).attr('y2', clampedY);

        dateText.attr('x', snappedX).text(d3.timeFormat('%Y-%m-%d')(d.date));
        const dateW = dateText.node().getBBox().width + 12;
        dateRect.attr('width', dateW).attr('x', snappedX - dateW / 2);

        priceText.attr('x', margin.left).attr('y', clampedY + 3).text(price.toFixed(2));
        const priceW = priceText.node().getBBox().width + 12;
        priceRect.attr('width', priceW).attr('x', margin.left - priceW).attr('y', clampedY - 8);
        priceText.attr('x', margin.left - priceW / 2);
    }

    function hideCrosshair() {
        crosshair.style('display', 'none');
    }

    // 透明覆蓋層，負責偵測滑鼠/手指位置並把資料「吸附」到最接近的那根 K 棒
    svg.append('rect')
        .attr('class', 'overlay')
        .attr('x', margin.left)
        .attr('y', margin.top)
        .attr('width', Math.max(0, width - margin.left - margin.right))
        .attr('height', Math.max(0, height - margin.top - margin.bottom))
        .attr('fill', 'transparent')
        .style('cursor', 'crosshair')
        .style('touch-action', 'none')
        // 滑鼠（桌面）
        .on('mousemove', function (event) {
            const [mx, my] = d3.pointer(event);
            updateCrosshair(mx, my);
        })
        .on('mouseleave', hideCrosshair)
        // 觸控（手機/平板）：手指按住拖動時跟著顯示十字線，放開後隱藏
        .on('touchstart touchmove', function (event) {
            event.preventDefault(); // 避免拖動圖表時整頁跟著滾動
            const touch = event.touches[0];
            const [mx, my] = d3.pointer(touch, this);
            updateCrosshair(mx, my);
        })
        .on('touchend touchcancel', hideCrosshair);
}

loadStockDetail();
