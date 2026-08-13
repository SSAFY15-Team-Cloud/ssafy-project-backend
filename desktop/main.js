const { app, BrowserWindow, session, shell, desktopCapturer } = require('electron')
const path = require('path')

const FRONTEND_URL = process.env.MEETINY_URL || 'http://localhost:5173'

function createWindow() {
  const win = new BrowserWindow({
    width: 1440,
    height: 900,
    minWidth: 960,
    minHeight: 640,
    backgroundColor: '#0e1530',
    title: 'Meetiny',
    autoHideMenuBar: true,
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
    },
  })

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

  // 새 창(문서 다운로드 링크 등)은 기본 브라우저로
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith('http://') || url.startsWith('https://')) {
      shell.openExternal(url)
    }
    return { action: 'deny' }
  })

  // 프론트 dev 서버가 꺼져있으면 안내 화면
  win.webContents.on('did-fail-load', () => {
    win.loadFile(path.join(__dirname, 'offline.html'))
  })

  win.loadURL(FRONTEND_URL)
}

app.whenReady().then(() => {
  createWindow()
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})
