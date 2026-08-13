const { app, BrowserWindow, session, shell, desktopCapturer } = require('electron')
const path = require('path')
const fs = require('fs')
const http = require('http')

const DEV_FRONTEND_URL = process.env.MEETINY_URL || 'http://localhost:5173'
const BACKEND_ORIGIN = process.env.MEETINY_BACKEND || 'http://localhost:8081'

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript',
  '.css': 'text/css',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
  '.woff2': 'font/woff2',
  '.json': 'application/json',
}

/**
 * 패키징된 앱: 내장된 프론트 빌드(dist)를 서빙하고
 * /api(HTTP)와 /ws(WebSocket)는 로컬 백엔드로 프록시하는 미니 서버.
 * 개발 모드(npm start)에서는 vite dev 서버(5173)를 그대로 사용한다.
 */
function startEmbeddedServer() {
  const distDir = path.join(process.resourcesPath, 'app-dist')
  const httpProxy = require('http-proxy')
  const proxy = httpProxy.createProxyServer({ target: BACKEND_ORIGIN, ws: true })
  proxy.on('error', (_err, _req, res) => {
    if (res && res.writeHead) {
      res.writeHead(502, { 'Content-Type': 'application/json' })
      res.end('{"message":"backend unreachable"}')
    }
  })

  const server = http.createServer((req, res) => {
    if (req.url.startsWith('/api')) {
      proxy.web(req, res)
      return
    }

    // 정적 파일 + SPA 폴백
    let filePath = path.join(distDir, decodeURIComponent(req.url.split('?')[0]))
    if (!filePath.startsWith(distDir) || !fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
      filePath = path.join(distDir, 'index.html')
    }
    const ext = path.extname(filePath).toLowerCase()
    res.writeHead(200, { 'Content-Type': MIME_TYPES[ext] || 'application/octet-stream' })
    fs.createReadStream(filePath).pipe(res)
  })

  server.on('upgrade', (req, socket, head) => {
    if (req.url.startsWith('/ws')) {
      proxy.ws(req, socket, head)
    } else {
      socket.destroy()
    }
  })

  return new Promise((resolve) => {
    server.listen(0, '127.0.0.1', () => resolve(server.address().port))
  })
}

function setupSession() {
  // 카메라/마이크 권한 자동 허용 (로컬 앱이므로)
  session.defaultSession.setPermissionRequestHandler((_wc, permission, callback) => {
    const allowed = ['media', 'display-capture', 'clipboard-sanitized-write', 'notifications']
    callback(allowed.includes(permission))
  })

  // 화면공유: 기본 화면을 소스로 제공 (Electron은 브라우저 픽커가 없음)
  session.defaultSession.setDisplayMediaRequestHandler((_request, callback) => {
    desktopCapturer.getSources({ types: ['screen'] }).then((sources) => {
      callback({ video: sources[0], audio: 'loopback' })
    })
  })
}

async function createWindow() {
  const win = new BrowserWindow({
    width: 1440,
    height: 900,
    minWidth: 960,
    minHeight: 640,
    backgroundColor: '#0e1530',
    title: 'Meetiny',
    icon: path.join(__dirname, 'icon.ico'),
    autoHideMenuBar: true,
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
    },
  })

  // 새 창(문서 다운로드 링크 등)은 기본 브라우저로
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith('http://') || url.startsWith('https://')) {
      shell.openExternal(url)
    }
    return { action: 'deny' }
  })

  // 프론트에 연결할 수 없으면 안내 화면
  win.webContents.on('did-fail-load', () => {
    win.loadFile(path.join(__dirname, 'offline.html'))
  })

  if (app.isPackaged) {
    const port = await startEmbeddedServer()
    win.loadURL(`http://127.0.0.1:${port}`)
  } else {
    win.loadURL(DEV_FRONTEND_URL)
  }
}

app.whenReady().then(() => {
  setupSession()
  void createWindow()
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) void createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})
