/**
 * 公共工具函数库
 * 用于所有 iframe 子页面复用
 */

/**
 * 渲染致命错误页面（CDN 加载失败时使用）
 * @param {string} message - 错误信息
 */
function renderFatalError(message) {
    const el = document.getElementById('app');
    if (!el) return;
    el.removeAttribute('v-cloak');
    el.innerHTML = `
        <div style="padding: 40px 20px; text-align: center; color: #f56c6c; font-family: Arial;">
            <div style="font-size: 16px; font-weight: 600; margin-bottom: 8px;">页面加载失败</div>
            <div style="font-size: 13px; color: #999;">${message || '请检查网络或刷新重试'}</div>
            <div style="margin-top: 16px;">
                <button onclick="location.reload()" style="padding: 8px 16px; cursor: pointer;">刷新</button>
            </div>
        </div>
    `;
}

/**
 * 计算表格高度（自适应窗口大小）
 * @param {object} tableHeightRef - Vue ref 对象
 * @param {number} reservedHeight - 预留高度（顶部工具栏 + 分页 + padding）
 * @param {number} minHeight - 最小高度
 * @returns {object} { computeTableHeight, cleanupResize }
 */
function createTableHeight(tableHeightRef, reservedHeight = 260, minHeight = 320) {
    const computeTableHeight = () => {
        tableHeightRef.value = Math.max(minHeight, window.innerHeight - reservedHeight);
    };

    const cleanupResize = () => {
        window.removeEventListener('resize', computeTableHeight);
    };

    // 立即计算一次
    computeTableHeight();

    // 监听窗口大小变化
    window.addEventListener('resize', computeTableHeight);

    return { computeTableHeight, cleanupResize };
}

/**
 * 格式化日期时间
 * @param {string|Date} dateStr - 日期字符串或 Date 对象
 * @returns {string} 格式化后的日期时间字符串 (YYYY-MM-DD HH:mm:ss)
 */
function formatDateTime(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

/**
 * 格式化日期（不含时间）
 * @param {string|Date} dateStr - 日期字符串或 Date 对象
 * @returns {string} 格式化后的日期字符串 (YYYY-MM-DD)
 */
function formatDate(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

// 暴露到全局
window.renderFatalError = renderFatalError;
window.createTableHeight = createTableHeight;
window.formatDateTime = formatDateTime;
window.formatDate = formatDate;
