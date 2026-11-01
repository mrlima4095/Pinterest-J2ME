# Pinterest J2ME

A standalone Pinterest MIDlet for J2ME phones (CLDC 1.1 / MIDP 2.0), extracted from the Pinterest
implementation built into [MIDPlay](https://github.com/mrlima4095/MIDPlay) and packaged as its own
application.

The MIDlet talks to a small PHP proxy that translates Pinterest's internal web API into compact JSON,
because MIDP devices cannot reliably speak HTTPS or handle Pinterest's full web payloads. Images go
through a second, allow-listed image proxy. Both PHP files and a Docker setup are included in this
repository.

> Unofficial client. Not affiliated with Pinterest.

## Features

- Search pins, boards and users
- Paginated pin and board lists
- Pin details with image, author, board and description
- User and board screens with drill-down
- Full-screen image viewer
- Local favorites: saved pins, saved boards and device-created folders
- Import a whole online board into a local folder
- Multi-select to save, move, download or remove several items
- Photo download to the memory card (JSR-75 / `FileConnection`)
- Search history
- Settings for the API proxy, image proxy, page size and download folder
- UI in English, Portuguese, Polish, Turkish, Vietnamese and Hebrew
- Automatic language detection from the device locale on first launch

No dependency on MIDPlay, MPGram, OpenTTY, an audio player or any other subapp.

## Requirements

- Phone with **MIDP 2.0** and **CLDC 1.1**
- **JSR-75** (`FileConnection`) for photo downloads; the rest of the app works without it
- HTTP access to the internet
- **JDK 8** to build from source (required, see below)
- **Docker** (optional) to self-host the proxy

## Install the prebuilt MIDlet

The built files are committed under `dist/`:

```
dist/Pinterest.jar
dist/Pinterest.jad
dist/SHA256SUMS
```

Copy `Pinterest.jar` and `Pinterest.jad` together to the phone (cable, Bluetooth or memory card) and
open the `.jad`. In emulators such as KEmulator, open either the `.jar` or the `.jad`.

Declared permissions:

- `javax.microedition.io.Connector.http`
- `javax.microedition.io.Connector.file.read`
- `javax.microedition.io.Connector.file.write`
- `javax.microedition.io.Connector.file.delete`
- optional: `javax.microedition.io.Connector.https`

## Build from source

```bash
git clone https://github.com/mrlima4095/Pinterest-J2ME.git
cd Pinterest-J2ME
./build.sh
```

Output:

```
dist/Pinterest.jar
dist/Pinterest.jad
dist/SHA256SUMS
```

The script finds JDK 8 automatically. If needed:

```bash
export JAVA8_HOME=/usr/lib/jvm/temurin-8-jdk-amd64
./build.sh
```

### Why JDK 8?

CLDC 1.1 has `StringBuffer` but not `StringBuilder`. Only `javac -source 1.3` (or 1.4) lowers string
concatenation to `StringBuffer`; newer compilers emit `StringBuilder`, which crashes on a real device.
ProGuard then runs with `-microedition` to add the preverification attributes required by CLDC
verifiers.

Build outputs are written to `build/` and can be deleted. The `.jar` is not bit-for-bit reproducible
because ZIP entries carry timestamps, so rebuilding overwrites `dist/Pinterest.jar` and
`dist/SHA256SUMS` even when the code did not change.

## Self-host the proxy

The app needs two endpoints:

| File | Purpose |
| --- | --- |
| `pinterest.php` | JSON API for search, users, boards, pins and optional login/QR sessions |
| `proxy.php` | Allow-listed image proxy for `i.pinimg.com` |

### Docker (recommended)

```bash
docker build -t pinterest-j2me-proxy .
docker run -d \
  --name pinterest-proxy \
  -p 8080:80 \
  --restart unless-stopped \
  pinterest-j2me-proxy
```

Or with Docker Compose:

```bash
cp .env.example .env
docker compose up -d --build
```

The proxy is then available at:

```
http://localhost:8080/pinterest.php
http://localhost:8080/proxy.php?url=
http://localhost:8080/          (landing page and documentation)
```

Check it:

```bash
curl http://localhost:8080/?health=1
curl "http://localhost:8080/pinterest.php?op=search&q=nature&n=3"
```

Compose variables (see `.env.example`):

| Variable | Default | Meaning |
| --- | --- | --- |
| `PORT` | `8080` | Host port published by Compose |
| `PROXY_ALLOWED_HOSTS` | `pinimg.com` | Comma-separated host suffixes the image proxy may fetch |
| `PROXY_MAX_BYTES` | `8388608` | Maximum image size accepted by the image proxy |

### Manual PHP deployment

Requirements:

- PHP 7.4 or newer with the **curl** extension
- Apache, Nginx + PHP-FPM, or another PHP-capable web server
- Write access to the system temp directory (rate limits, cache and session files)
- Outbound HTTPS access to Pinterest

Copy `index.php`, `pinterest.php` and `proxy.php` to your web root. Do not expose the directory
listing and do not publish the files without the allow-list in `proxy.php`.

### HTTPS and reverse proxies

Most modern devices and app stores prefer HTTPS. Put the container behind Caddy, Nginx, Traefik or a
cloud load balancer. Example with Caddy:

```
pinterest.example.com {
    reverse_proxy 127.0.0.1:8080
}
```

Then use `https://pinterest.example.com/pinterest.php` in the app.

### Configure the MIDlet

Open **Settings** in the app:

| Setting | Example |
| --- | --- |
| API proxy URL | `https://pinterest.example.com/pinterest.php` |
| Image proxy URL | `https://pinterest.example.com/proxy.php?url=` |
| Items per page | `1` to `25` (default `20`) |
| Download folder | `file:///SDCard/Pinterest/` |

Keep the `?url=` suffix on the image proxy URL: the MIDlet appends the encoded Pinterest image
address to it.

## Proxy API

### `pinterest.php`

All operations return compact JSON with short keys. Anonymous operations are cached for a few minutes;
`n` is the page size (maximum 25) and `bm` is the bookmark returned by the previous page.

| Operation | Parameters | Result |
| --- | --- | --- |
| `search` | `q`, `scope=pins\|boards`, `n`, `bm` | Pins or boards |
| `user` | `u` | Profile |
| `userpins` | `u`, `n`, `bm` | Pins from a profile |
| `userboards` | `u`, `n`, `bm` | Boards from a profile |
| `board` | `u`, `s` | Board metadata |
| `boardpins` | `u`, `s`, `bid`, `n`, `bm` | Pins inside a board |
| `pin` | `id` | One pin |
| `login` | `u`, `p` | Optional password login |
| `qrstart` / `qrpoll` | `token` | Optional QR login flow |
| `logout` | — | Clears the session |
| `me` / `home` | — | Authenticated profile / feed |

The full login and QR flow is documented in the header of `pinterest.php`. Anonymous browsing does not
need an account or session.

Rate limiting is 90 requests per minute per client IP. Cache entries and sessions live in the system
temp directory, so they are lost when the container is recreated.

### `proxy.php`

```
/proxy.php?url=https%3A%2F%2Fi.pinimg.com%2F236x%2Faa%2F98%2F...jpg
```

- Allows `pinimg.com` and its subdomains by default
- Follows up to three redirects, but only inside the allow-list
- Accepts only `image/*` responses
- Enforces `PROXY_MAX_BYTES`
- Caches successful responses for one day

## Security

- **Do not turn `proxy.php` into an open proxy.** Keep `PROXY_ALLOWED_HOSTS` narrow. An unrestricted
  fetcher can be abused for SSRF against internal networks.
- The image proxy validates scheme, host and port, rejects credentials in URLs, and re-validates every
  redirect target.
- `pinterest.php` rate-limits by IP, but it is not an authentication layer. If you enable login/QR
  sessions, remember that session state is stored on the server temp directory.
- Put the proxy behind HTTPS before distributing the app. Plain HTTP exposes searches and session
  tokens to the network.
- The scripts use Pinterest's internal web API, which can change without notice. Update
  `pinterest.php` if searches stop working.

## Project layout

```
src/pinterest/
  PinterestMIDlet.java      entry point
  net/                      HTTP, Pinterest API, downloads
  store/                    settings, favorites, history (RMS)
  ui/                       Navigator, lists, commands, sharing
  ui/screen/                Pinterest screens
  util/                     Utils and generated Lang
src/cc/nnproject/json/      JSON parser used by the proxy payloads
tools/build_lang.py         generates src/pinterest/util/Lang.java
langs/                      reduced translation tables used by the generator
res/                        icon and sprite sheet
docker/healthcheck.sh       container health check
Dockerfile                  self-hosted proxy image
docker-compose.yml          one-command deployment
index.php                   landing page and health endpoint
pinterest.php               Pinterest JSON proxy
proxy.php                   allow-listed image proxy
```

## Troubleshooting

**Search returns an error.** Pinterest changed its internal API, or the proxy cannot reach it. Test
`pinterest.php?op=search&q=nature&n=3` directly and update the proxy if needed.

**Images do not load.** The image proxy URL must end with `?url=`. Check that `PROXY_ALLOWED_HOSTS`
includes the image host and that the server can make outbound HTTPS requests.

**The MIDlet does not install.** Check MIDP 2.0 / CLDC 1.1 support, keep the `.jar` and `.jad` in the
same folder, and accept the HTTP/file permissions on first launch.

**Downloads fail.** The phone needs JSR-75 and a valid `FileConnection` path, for example
`file:///SDCard/Pinterest/`. Some devices use `file:///E:/` or a different card name.

**The build fails.** JDK 8 is mandatory. Point `JAVA8_HOME` at a JDK 8 installation and run
`./build.sh` again.

## Credits

The original Pinterest screens, proxy integration and `pinterest.php` are part of
[MIDPlay](https://github.com/mrlima4095/MIDPlay). This repository extracts them into an independent
MIDlet with a minimal UI layer, its own navigation and browser-based sharing.

The icon and sprite sheet in `res/` also come from MIDPlay.

## License

MIT — see [LICENSE](LICENSE). Copyright (c) 2026 Mr Lima.
