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

    drawCandlestick(ohlcs);
}

function drawCandlestick(data) {
    const container = document.getElementById('chart-container');
    const width = container.clientWidth || window.innerWidth - 40;
    const height = container.clientHeight || 500;
    const margin = { top: 20, right: 30, bottom: 30, left: 50 };

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
        .range([height - margin.bottom, margin.top]);

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
}

loadStockDetail();
