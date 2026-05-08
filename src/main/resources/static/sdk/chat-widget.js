/**
 * 智能客服 Widget SDK
 *
 * 使用方式：
 * <script src="http://localhost:8080/sdk/chat-widget.js" data-tenant-code="YOUR_TENANT_CODE"></script>
 */
(function() {
    'use strict';

    // 获取当前 script 标签的属性
    const currentScript = document.currentScript || (function() {
        const scripts = document.getElementsByTagName('script');
        return scripts[scripts.length - 1];
    })();

    // 从 data-tenant-code 属性获取租户编码
    const TENANT_CODE = currentScript.getAttribute('data-tenant-code');
    
    // 读取全局配置对象
    const config = window.ChatWidgetConfig || {};
    
    // 优先级：配置对象 > script 属性 > 默认值
    const NO_BUTTON = config.noButton || (currentScript.getAttribute('data-no-button') === 'true');
    const WINDOW_WIDTH = config.width || currentScript.getAttribute('data-width');
    const WINDOW_HEIGHT = config.height || currentScript.getAttribute('data-height');

    if (!TENANT_CODE) {
        return;
    }

    // 获取当前脚本的基础 URL（用于构建其他资源路径）
    const scriptSrc = currentScript.src;
    const baseUrl = scriptSrc.substring(0, scriptSrc.lastIndexOf('/'));
    
    // 从脚本 URL 提取运营商服务器地址（协议 + 域名 + 端口）
    const operatorDomain = scriptSrc.match(/^(https?:\/\/[^\/]+)/)[1];
    
    // Chat 页面地址（使用运营商服务器地址）
    const CHAT_PAGE_URL = operatorDomain + '/chat';

    /**
     * 创建悬浮按钮
     */
    function createButton() {
        const btn = document.createElement('div');
        btn.id = 'chat-widget-button';
        btn.innerHTML = '💬';
        btn.style.cssText = `
            position: fixed;
            right: 20px;
            bottom: 20px;
            width: 50px;
            height: 50px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border-radius: 50%;
            display: flex;
            justify-content: center;
            align-items: center;
            cursor: pointer;
            box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
            transition: all 0.3s ease;
            z-index: 99999;
            font-size: 24px;
            user-select: none;
        `;

        btn.addEventListener('mouseenter', function() {
            this.style.transform = 'scale(1.1)';
        });

        btn.addEventListener('mouseleave', function() {
            this.style.transform = 'scale(1)';
        });

        btn.addEventListener('click', function() {
            toggleWindow();
        });

        document.body.appendChild(btn);
    }

    /**
     * 获取窗口尺寸（响应式适配）
     */
    function getWindowSize() {
        const isMobile = window.innerWidth <= 768;
        
        // 优先使用配置的尺寸
        if (WINDOW_WIDTH || WINDOW_HEIGHT) {
            return {
                width: WINDOW_WIDTH || (isMobile ? '100%' : '400px'),
                height: WINDOW_HEIGHT || (isMobile ? '100%' : '600px'),
                right: isMobile ? '0' : '20px',
                bottom: isMobile ? '0' : '10px',
                borderRadius: isMobile ? '0' : '12px'
            };
        }
        
        // 默认尺寸
        return {
            width: isMobile ? '100%' : '400px',
            height: isMobile ? '100%' : '600px',
            right: isMobile ? '0' : '20px',
            bottom: isMobile ? '0' : '10px',
            borderRadius: isMobile ? '0' : '12px'
        };
    }

    /**
     * 创建 Chat 窗口
     */
    function createWindow() {
        const size = getWindowSize();
        const win = document.createElement('div');
        win.id = 'chat-widget-window';
        win.style.cssText = `
            display: none;
            position: fixed;
            right: ${size.right};
            bottom: ${size.bottom};
            width: ${size.width};
            height: ${size.height};
            background: white;
            border-radius: ${size.borderRadius};
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
            z-index: 99998;
            overflow: hidden;
        `;

        // 关闭按钮
        const closeBtn = document.createElement('div');
        closeBtn.innerHTML = '✕';
        closeBtn.style.cssText = `
            position: absolute;
            top: 10px;
            right: 10px;
            width: 30px;
            height: 30px;
            background: rgba(0, 0, 0, 0.1);
            border-radius: 50%;
            display: flex;
            justify-content: center;
            align-items: center;
            cursor: pointer;
            z-index: 100000;
            font-size: 16px;
            color: #666;
        `;

        closeBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            closeWindow();
        });

        // 外部链接按钮（新标签页打开）
        const externalBtn = document.createElement('div');
        externalBtn.innerHTML = '↗';
        externalBtn.style.cssText = `
            position: absolute;
            top: 10px;
            right: 45px;
            width: 30px;
            height: 30px;
            background: rgba(0, 0, 0, 0.1);
            border-radius: 50%;
            display: flex;
            justify-content: center;
            align-items: center;
            cursor: pointer;
            z-index: 100000;
            font-size: 16px;
            color: #666;
        `;

        externalBtn.addEventListener('mouseenter', function() {
            this.style.background = 'rgba(0, 0, 0, 0.2)';
        });

        externalBtn.addEventListener('mouseleave', function() {
            this.style.background = 'rgba(0, 0, 0, 0.1)';
        });

        externalBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            openInNewTab();
        });

        // iframe
        const iframe = document.createElement('iframe');
        iframe.id = 'chat-widget-iframe';
        iframe.frameBorder = '0';
        iframe.style.cssText = `
            width: 100%;
            height: 100%;
            border: none;
        `;
        
        // 调试：监听 iframe 加载事件
        iframe.onerror = function() {
        };

        win.appendChild(closeBtn);
        win.appendChild(externalBtn);
        win.appendChild(iframe);
        document.body.appendChild(win);
    }

    /**
     * 在新标签页打开
     */
    function openInNewTab() {
        const iframe = document.getElementById('chat-widget-iframe');
        if (iframe && iframe.src) {
            window.open(iframe.src, '_blank');
        } else {
            console.warn('[Chat Widget] 窗口尚未加载，无法打开新标签页');
        }
    }

    /**
     * 切换窗口显示/隐藏
     */
    function toggleWindow() {
        const win = document.getElementById('chat-widget-window');
        const iframe = document.getElementById('chat-widget-iframe');
        const btn = document.getElementById('chat-widget-button');
        const size = getWindowSize();

        if (win.style.display === 'none' || !win.style.display) {
            // 打开窗口
            console.log('[调试] 打开窗口，准备设置 iframe.src');
            console.log('  - CHAT_PAGE_URL:', CHAT_PAGE_URL);
            console.log('  - 设置前 iframe.getAttribute("src"):', iframe.getAttribute('src'));

            if (!iframe.getAttribute('src')) {
                // 第一次打开：先调用后端验证 tenant_code，获取 tenantId
                console.log('[Chat Widget] 开始验证 tenant_code');
                
                fetch(`${operatorDomain}/api/chat/init`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ tenantCode: TENANT_CODE })
                })
                .then(response => response.json())
                .then(result => {
                    if (result.code === 200 && result.data && result.data.tenantId) {
                        // 验证通过，拼接 URL 加载 chat 页面
                        const chatUrl = operatorDomain + '/chat?tenantId=' + result.data.tenantId;
                        iframe.src = chatUrl;
                        
                        // 显示窗口
                        win.style.display = 'block';
                        adjustButtonPosition(size.height);
                    } else {
                        alert('租户验证失败：' + (result.message || '未知错误'));
                    }
                })
                .catch(error => {
                    alert('网络错误，请稍后重试');
                });
            } else {
                win.style.display = 'block';
                adjustButtonPosition(size.height);
            }
        } else {
            // 关闭窗口
            win.style.display = 'none';
            btn.style.bottom = '20px';
        }
    }

    /**
     * 调整按钮位置（适配移动端）
     */
    function adjustButtonPosition(height) {
        const btn = document.getElementById('chat-widget-button');
        if (!btn) return; // 自定义模式下没有按钮
        
        const isMobile = window.innerWidth <= 768;
        
        if (isMobile) {
            // 移动端：窗口全屏，隐藏按钮
            btn.style.display = 'none';
        } else {
            // PC 端：按钮上移到窗口上方
            btn.style.bottom = (10 + parseInt(height) + 1) + 'px';
        }
    }

    /**
     * 打开窗口
     */
    function openWindow() {
        const win = document.getElementById('chat-widget-window');
        const iframe = document.getElementById('chat-widget-iframe');
        const btn = document.getElementById('chat-widget-button');
        const size = getWindowSize();

        if (win) {
            // 如果 iframe 还没有 src，说明第一次打开，走 toggleWindow 的验证逻辑
            if (!iframe.getAttribute('src')) {
                toggleWindow();
                return;
            }

            win.style.display = 'block';
            adjustButtonPosition(size.height);
        }
    }

    /**
     * 关闭窗口
     */
    function closeWindow() {
        const win = document.getElementById('chat-widget-window');
        const btn = document.getElementById('chat-widget-button');
        if (win) {
            win.style.display = 'none';
            // 移动端关闭窗口时恢复按钮显示
            if (window.innerWidth <= 768) {
                btn.style.display = 'flex';
            }
            btn.style.bottom = '20px';
        }
    }

    // 初始化
    function init() {
        // 如果不使用内置按钮，只创建窗口
        if (NO_BUTTON) {
            createWindow();
            console.log('[Chat Widget] 自定义模式：租户需自行创建按钮并调用 ChatWidget.open()');
        } else {
            // 默认模式：创建内置按钮和窗口
            createButton();
            createWindow();
        }
    }

    // 等待 DOM 加载完成后初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    // 暴露全局 API
    window.ChatWidget = {
        open: openWindow,
        close: closeWindow,
        toggle: toggleWindow
    };

})();
