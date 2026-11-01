<?php
/**
 * index.php — landing page and health endpoint for the Pinterest J2ME proxy.
 *
 *   GET /            → this page
 *   GET /?health=1   → {"ok":1,...} (used by the Docker health check)
 */

$hasCurl = function_exists('curl_init');
if (isset($_GET['health'])) {
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store');
    echo json_encode(array(
        'ok' => $hasCurl ? 1 : 0,
        'php' => PHP_VERSION,
        'curl' => $hasCurl ? 1 : 0,
        'pinterest' => is_file(__DIR__ . '/pinterest.php') ? 1 : 0,
        'image_proxy' => is_file(__DIR__ . '/proxy.php') ? 1 : 0,
    ));
    exit;
}

function h($value)
{
    return htmlspecialchars((string) $value, ENT_QUOTES, 'UTF-8');
}

$https = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off')
    || (isset($_SERVER['HTTP_X_FORWARDED_PROTO']) && $_SERVER['HTTP_X_FORWARDED_PROTO'] === 'https');
$scheme = $https ? 'https' : 'http';
$host = isset($_SERVER['HTTP_HOST']) ? $_SERVER['HTTP_HOST'] : 'localhost';
$base = $scheme . '://' . $host;
$apiUrl = $base . '/pinterest.php';
$imageUrl = $base . '/proxy.php?url=';
$repo = 'https://github.com/mrlima4095/Pinterest-J2ME';
?>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Pinterest J2ME Proxy</title>
<meta name="description" content="Self-hosted JSON and image proxy for the Pinterest J2ME MIDlet.">
<style>
  :root {
    --bg: #0f1115;
    --panel: #171a21;
    --panel-2: #1e222b;
    --text: #e8eaed;
    --muted: #9aa0aa;
    --line: #2a2f3a;
    --red: #e60023;
    --red-soft: #ff4d67;
    --green: #3fb950;
    --amber: #d29922;
    --code: #0b0d11;
  }
  * { box-sizing: border-box; }
  body {
    margin: 0;
    background: radial-gradient(900px 450px at 15% -10%, #2a1218 0%, transparent 60%), var(--bg);
    color: var(--text);
    font: 16px/1.65 -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  }
  a { color: var(--red-soft); text-decoration: none; }
  a:hover { text-decoration: underline; }
  .wrap { max-width: 980px; margin: 0 auto; padding: 32px 20px 64px; }
  header { display: flex; align-items: center; gap: 16px; margin-bottom: 28px; }
  .logo {
    width: 56px; height: 56px; border-radius: 50%;
    background: var(--red); color: #fff; display: grid; place-items: center;
    font-size: 30px; font-weight: 800; font-family: Georgia, serif;
    box-shadow: 0 8px 30px rgba(230, 0, 35, .35);
  }
  h1 { margin: 0; font-size: clamp(26px, 4vw, 38px); letter-spacing: -.02em; }
  .sub { color: var(--muted); margin: 2px 0 0; }
  .chips { display: flex; flex-wrap: wrap; gap: 8px; margin: 18px 0 8px; }
  .chip {
    font-size: 13px; padding: 5px 11px; border: 1px solid var(--line);
    border-radius: 999px; background: var(--panel); color: var(--muted);
  }
  .chip b { color: var(--text); font-weight: 600; }
  .dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 6px; }
  .ok { background: var(--green); }
  .bad { background: var(--red); }
  section { margin-top: 34px; }
  h2 { font-size: 19px; margin: 0 0 12px; letter-spacing: -.01em; }
  p { margin: 0 0 14px; }
  .muted { color: var(--muted); }
  .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(270px, 1fr)); gap: 14px; }
  .card {
    background: var(--panel); border: 1px solid var(--line); border-radius: 14px;
    padding: 18px; min-width: 0;
  }
  .card h3 { margin: 0 0 6px; font-size: 16px; }
  .card p { margin: 0; color: var(--muted); font-size: 14px; }
  code, pre { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
  code { background: var(--panel-2); padding: 1px 6px; border-radius: 6px; font-size: 13.5px; }
  pre {
    background: var(--code); border: 1px solid var(--line); border-radius: 12px;
    padding: 14px 16px; overflow-x: auto; font-size: 13.5px; line-height: 1.55;
  }
  pre code { background: none; padding: 0; }
  table { width: 100%; border-collapse: collapse; font-size: 14px; }
  th, td { text-align: left; padding: 9px 10px; border-bottom: 1px solid var(--line); vertical-align: top; }
  th { color: var(--muted); font-weight: 600; }
  td code { white-space: nowrap; }
  .note {
    border-left: 3px solid var(--amber); background: #1c1a12; color: #d8c99b;
    padding: 12px 16px; border-radius: 0 10px 10px 0; font-size: 14px;
  }
  footer { margin-top: 46px; padding-top: 20px; border-top: 1px solid var(--line); color: var(--muted); font-size: 14px; }
</style>
</head>
<body>
<div class="wrap">
  <header>
    <div class="logo">P</div>
    <div>
      <h1>Pinterest J2ME Proxy</h1>
      <p class="sub">Self-hosted JSON and image proxy for the Pinterest J2ME MIDlet.</p>
    </div>
  </header>

  <div class="chips">
    <span class="chip"><span class="dot <?php echo $hasCurl ? 'ok' : 'bad'; ?>"></span>curl <b><?php echo $hasCurl ? 'available' : 'missing'; ?></b></span>
    <span class="chip">PHP <b><?php echo h(PHP_VERSION); ?></b></span>
    <span class="chip"><span class="dot <?php echo is_file(__DIR__ . '/pinterest.php') ? 'ok' : 'bad'; ?>"></span>pinterest.php</span>
    <span class="chip"><span class="dot <?php echo is_file(__DIR__ . '/proxy.php') ? 'ok' : 'bad'; ?>"></span>proxy.php</span>
  </div>

  <p class="muted">
    The MIDlet talks to <code>pinterest.php</code> for compact Pinterest JSON and to
    <code>proxy.php</code> for images. Both scripts run on your own server, so the phone never needs
    to open HTTPS or parse Pinterest's full web payload. This project is unofficial and is not
    affiliated with Pinterest.
  </p>

  <section>
    <h2>Quick start with Docker</h2>
    <pre><code>git clone <?php echo h($repo); ?>.git
cd Pinterest-J2ME
docker compose up -d --build</code></pre>
    <p class="muted">The service listens on <code>http://localhost:8080</code> by default. Change the
      published port or put it behind a TLS reverse proxy for real devices.</p>
  </section>

  <section>
    <h2>Point the MIDlet at this server</h2>
    <p>Open <b>Settings</b> in the app and use the URLs below.</p>
    <table>
      <tr><th>API proxy</th><td><code><?php echo h($apiUrl); ?></code></td></tr>
      <tr><th>Image proxy</th><td><code><?php echo h($imageUrl); ?></code></td></tr>
    </table>
    <p class="muted" style="margin-top:12px">Keep the <code>?url=</code> suffix on the image proxy
      URL: the MIDlet appends the encoded Pinterest image address to it.</p>
  </section>

  <section>
    <h2>Endpoints</h2>
    <div class="grid">
      <div class="card">
        <h3>Search</h3>
        <p><code>/pinterest.php?op=search&amp;q=nature&amp;scope=pins&amp;n=20</code><br>
          Use <code>scope=boards</code> for boards and <code>bm=</code> for the next page.</p>
      </div>
      <div class="card">
        <h3>Users</h3>
        <p><code>/pinterest.php?op=user&amp;u=username</code><br>
          <code>userpins</code> and <code>userboards</code> list that profile's content.</p>
      </div>
      <div class="card">
        <h3>Boards</h3>
        <p><code>/pinterest.php?op=board&amp;u=user&amp;s=slug</code><br>
          <code>boardpins</code> lists the pins inside the board.</p>
      </div>
      <div class="card">
        <h3>Pins and images</h3>
        <p><code>/pinterest.php?op=pin&amp;id=123</code><br>
          <code>/proxy.php?url=https%3A%2F%2Fi.pinimg.com%2F...</code></p>
      </div>
    </div>
    <p class="muted" style="margin-top:12px">Responses are compact JSON with one-letter keys to save
      bandwidth on CLDC devices. <code>pinterest.php</code> also implements the optional
      login/QR-session operations described in its header.</p>
  </section>

  <section>
    <h2>Security notes</h2>
    <div class="note">
      <code>proxy.php</code> only fetches Pinterest image hosts by default
      (<code>pinimg.com</code> and its subdomains). Change <code>PROXY_ALLOWED_HOSTS</code> if you
      need another CDN, and keep it narrow: an unrestricted proxy can be abused for SSRF.
    </div>
    <p class="muted" style="margin-top:12px"><code>pinterest.php</code> rate-limits requests per IP
      (90 per minute) and caches anonymous responses in the system temp directory. Sessions are
      stored there too, so they do not survive a container rebuild.</p>
  </section>

  <section>
    <h2>Health check</h2>
    <p class="muted">The Docker image checks <code>/?health=1</code>, which returns JSON such as:</p>
    <pre><code><?php echo h(json_encode(array('ok' => 1, 'php' => PHP_VERSION, 'curl' => $hasCurl ? 1 : 0))); ?></code></pre>
    <p><a href="?health=1">Open the live health response</a> ·
      <a href="pinterest.php?op=search&amp;q=nature&amp;n=3">Try a live search</a></p>
  </section>

  <footer>
    Pinterest J2ME · MIT License © 2026 Mr Lima ·
    <a href="<?php echo h($repo); ?>">Source code and MIDlet downloads</a>
  </footer>
</div>
</body>
</html>
