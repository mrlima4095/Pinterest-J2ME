<?php
/**
 * proxy.php — small, allow-listed image proxy for the Pinterest J2ME client.
 *
 * MIDP phones usually cannot open HTTPS and many old devices choke on TLS, so
 * the MIDlet fetches pin images through this endpoint:
 *
 *   /proxy.php?url=https%3A%2F%2Fi.pinimg.com%2F236x%2F...jpg
 *
 * Only Pinterest image hosts are allowed by default. Set PROXY_ALLOWED_HOSTS
 * (comma-separated suffixes) to add your own CDN. Redirects are followed only
 * when they stay inside the allow-list, and responses are limited to
 * PROXY_MAX_BYTES (8 MB by default).
 */

function proxy_env_int($name, $default)
{
    $value = getenv($name);
    if ($value === false || !is_numeric($value)) {
        return $default;
    }
    return (int) $value;
}

function proxy_allowed_hosts()
{
    $raw = getenv('PROXY_ALLOWED_HOSTS');
    if ($raw === false || trim($raw) === '') {
        $raw = 'pinimg.com';
    }
    $hosts = array();
    foreach (explode(',', strtolower($raw)) as $host) {
        $host = trim(ltrim($host, '.'));
        if ($host !== '') {
            $hosts[] = $host;
        }
    }
    return $hosts;
}

function proxy_host_allowed($host, $allowed)
{
    $host = strtolower($host);
    foreach ($allowed as $domain) {
        if ($host === $domain || substr($host, -strlen('.' . $domain)) === '.' . $domain) {
            return true;
        }
    }
    return false;
}

function proxy_validate_url($url, $allowed)
{
    $parts = parse_url($url);
    if (!is_array($parts) || empty($parts['scheme']) || empty($parts['host'])) {
        return false;
    }
    $scheme = strtolower($parts['scheme']);
    if ($scheme !== 'http' && $scheme !== 'https') {
        return false;
    }
    if (isset($parts['user']) || isset($parts['pass'])) {
        return false;
    }
    if (isset($parts['port']) && (int) $parts['port'] !== 80 && (int) $parts['port'] !== 443) {
        return false;
    }
    return proxy_host_allowed($parts['host'], $allowed);
}

function proxy_fail($code, $message)
{
    http_response_code($code);
    header('Content-Type: text/plain; charset=utf-8');
    header('Cache-Control: no-store');
    echo $message;
    exit;
}

function proxy_fetch($url, $allowed, $maxBytes)
{
    $current = $url;
    for ($redirect = 0; $redirect < 4; $redirect++) {
        if (!proxy_validate_url($current, $allowed)) {
            return array('error' => 'blocked', 'code' => 403);
        }

        $body = '';
        $tooLarge = false;
        $ch = curl_init($current);
        curl_setopt_array($ch, array(
            CURLOPT_RETURNTRANSFER => false,
            CURLOPT_HEADER => false,
            CURLOPT_FOLLOWLOCATION => false,
            CURLOPT_CONNECTTIMEOUT => 8,
            CURLOPT_TIMEOUT => 20,
            CURLOPT_USERAGENT => 'PinterestJ2ME-Proxy/1.0',
            CURLOPT_PROTOCOLS => CURLPROTO_HTTP | CURLPROTO_HTTPS,
            CURLOPT_WRITEFUNCTION => function ($handle, $chunk) use (&$body, &$tooLarge, $maxBytes) {
                if (strlen($body) + strlen($chunk) > $maxBytes) {
                    $tooLarge = true;
                    return 0;
                }
                $body .= $chunk;
                return strlen($chunk);
            },
        ));

        $ok = curl_exec($ch);
        $status = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $contentType = (string) curl_getinfo($ch, CURLINFO_CONTENT_TYPE);
        $location = (string) curl_getinfo($ch, CURLINFO_REDIRECT_URL);
        curl_close($ch);

        if ($tooLarge) {
            return array('error' => 'too_large', 'code' => 413);
        }
        if ($ok === false) {
            return array('error' => 'upstream', 'code' => 502);
        }
        if ($status >= 300 && $status < 400 && $location !== '') {
            $current = $location;
            continue;
        }
        if ($status < 200 || $status >= 300) {
            return array('error' => 'upstream', 'code' => 502);
        }
        if (stripos($contentType, 'image/') !== 0) {
            return array('error' => 'not_image', 'code' => 415);
        }
        return array('body' => $body, 'content_type' => $contentType);
    }
    return array('error' => 'redirects', 'code' => 502);
}

$allowed = proxy_allowed_hosts();
$maxBytes = proxy_env_int('PROXY_MAX_BYTES', 8 * 1024 * 1024);
if ($maxBytes < 65536) {
    $maxBytes = 65536;
}

$url = isset($_GET['url']) ? (string) $_GET['url'] : '';
if ($url === '' && isset($_SERVER['QUERY_STRING'])) {
    // Also accept /proxy.php?https%3A%2F%2F... (no "url=" key).
    $query = (string) $_SERVER['QUERY_STRING'];
    if (strpos($query, 'url=') === 0) {
        $query = substr($query, 4);
    }
    $url = urldecode($query);
}

$url = trim($url);
if ($url === '') {
    proxy_fail(400, 'Missing url parameter');
}
if (!preg_match('#^https?://#i', $url)) {
    $url = 'https://' . $url;
}

$result = proxy_fetch($url, $allowed, $maxBytes);
if (isset($result['error'])) {
    if ($result['error'] === 'too_large') {
        proxy_fail(413, 'Image too large');
    }
    if ($result['error'] === 'not_image') {
        proxy_fail(415, 'Upstream response is not an image');
    }
    if ($result['error'] === 'blocked') {
        proxy_fail(403, 'Host not allowed');
    }
    proxy_fail(502, 'Upstream image unavailable');
}

header('Content-Type: ' . $result['content_type']);
header('Content-Length: ' . strlen($result['body']));
header('Cache-Control: public, max-age=86400');
header('X-Content-Type-Options: nosniff');
header('Access-Control-Allow-Origin: *');
echo $result['body'];
