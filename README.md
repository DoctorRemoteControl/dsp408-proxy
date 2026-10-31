# DSP408 Decoder Proxy

Build with `mvn verify` from the [DSP408 workspace](../README.md), or install
`../dsp408-protocol` locally before building this project alone. Framing is shared
with Raider and the controller; the shaded JAR still runs independently.

Standalone TCP proxy for the **t.racks / Thomann DSP 408**, with:

* live traffic capture
* JSON session logs
* live stream to another tool
* optional control port for active sending to the DSP

---

## Features

* forwards traffic between **Track DSP** and **DSP408**
* splits DSP frames and stores them as a session log
* streams frames live as **NDJSON over TCP**
* can inject frames or payloads into the DSP via a **control port**
* includes a small session analyzer for recorded logs

---

## Project Structure

* `proxy.server`
  main TCP proxy, session management, logging

* `proxy.stream`
  live stream sink, e.g. NDJSON over TCP

* `proxy.control`
  JSON control server for sending frames/payloads

* `proxy.analysis`
  analysis of recorded sessions

* `proxy.core`
  frame codec, hex helpers, base functions

---

## Requirements

* Java 21
* Maven 3.9+

Check:

```bash
java -version
mvn -version
```

---

## Build

Important: run Maven **in the project folder**, i.e. where the `pom.xml` is.

```bash
cd C:\Users\XLA\DSP408\DSP408DecoderProxy
mvn clean package
```

Artifact:

```text
target\dsp408-proxy-0.1.0-SNAPSHOT-all.jar
```

---

## Start Without Stream

```bash
java -jar target\dsp408-proxy-0.1.0-SNAPSHOT-all.jar ^
  --listen-host 127.0.0.1 ^
  --listen-port 9761 ^
  --target-host 192.168.0.166 ^
  --target-port 9761 ^
  --log-dir proxy_logs
```

---

## Start With Live Stream

```bash
java -jar target\dsp408-proxy-0.1.0-SNAPSHOT-all.jar ^
  --listen-host 127.0.0.1 ^
  --listen-port 9761 ^
  --target-host 192.168.0.166 ^
  --target-port 9761 ^
  --log-dir proxy_logs ^
  --stream-host 127.0.0.1 ^
  --stream-port 19081
```

---

## Start With Live Stream and Control Port

```bash
java -jar target\dsp408-proxy-0.1.0-SNAPSHOT-all.jar ^
  --listen-host 127.0.0.1 ^
  --listen-port 9761 ^
  --target-host 192.168.0.166 ^
  --target-port 9761 ^
  --log-dir proxy_logs ^
  --stream-host 127.0.0.1 ^
  --stream-port 19081 ^
  --control-host 127.0.0.1 ^
  --control-port 19082
```

---

## Parameter Meaning

* `listen-host` / `listen-port`
  address the proxy listens on.
  **Track DSP** connects here.

* `target-host` / `target-port`
  real DSP408

* `log-dir`
  directory for session logs

* `stream-host` / `stream-port`
  destination for live events as NDJSON/TCP

* `control-host` / `control-port`
  JSON control interface for active sending to the DSP

---

## Track DSP Settings

If Track DSP runs on the same PC:

* IP: `127.0.0.1`
* Port: `9761`

Then Track DSP connects to the proxy, and the proxy forwards to the real DSP.

---

## Session Logs

The proxy writes one file per connection to `proxy_logs`, for example:

```text
proxy_logs\proxy_session_20260403_072138_051.json
```

The file contains:

* session metadata
* bytes PC -> DSP
* bytes DSP -> PC
* notes
* all detected frames with payload, ASCII, and validation

---

## Live Stream Format

If `--stream-host` and `--stream-port` are set, the proxy sends NDJSON lines to the target system.

Typical events:

### session_started

```json
{"type":"session_started","sessionId":"20260403_072138_051","startedAt":"2026-04-03T07:21:38.051Z","listenHost":"127.0.0.1","listenPort":9761,"targetHost":"192.168.0.166","targetPort":9761,"clientAddress":"/127.0.0.1:51344"}
```

### frame

```json
{"type":"frame","sessionId":"20260403_072138_051","frame":{"index":1,"direction":"PC_TO_DSP","timestampUtc":"2026-04-03T07:21:38.123Z","frameHex":"1002000103...","validFrame":true,"payloadHex":"000103...","payloadPrettyHex":"00 01 03 ...","payloadAscii":"....","payloadLength":12,"note":""}}
```

### note

```json
{"type":"note","sessionId":"20260403_072138_051","note":"PC_TO_DSP error: Connection reset"}
```

### session_ended

```json
{"type":"session_ended","sessionId":"20260403_072138_051","endedAt":"2026-04-03T07:22:01.000Z","bytesPcToDsp":1234,"bytesDspToPc":5678,"frameCount":42,"droppedEvents":0}
```

---

## Control Port

If the control port is enabled, another tool can send JSON lines to the proxy.

Connection:

* Host: `127.0.0.1`
* Port: `19082`

Each request is **one JSON line**.

---

## Control Commands

### Query status

Request:

```json
{"type":"status"}
```

Response:

```json
{"ok":true,"sessionActive":true}
```

---

### Send full frame

Request:

```json
{"type":"send_frame","frameHex":"10020001021003AA"}
```

Response:

```json
{"ok":true,"sessionActive":true,"message":"Frame injected"}
```

---

### Send payload only

The proxy automatically wraps it into a DSP frame with checksum.

Request:

```json
{"type":"send_payload","payloadHex":"00010312"}
```

Response:

```json
{"ok":true,"sessionActive":true,"message":"Payload injected as frame"}
```

---

## Important Notes for Sending

* sending only works if **an active proxy session** exists
* so: first **connect Track DSP through the proxy**
* then inject via the control port
* `send_payload` is usually more convenient than `send_frame`
* for unknown commands, always capture and analyze first

---

## Example: Test With Netcat

### Check status

```bash
echo {"type":"status"} | nc 127.0.0.1 19082
```

### Send payload

```bash
echo {"type":"send_payload","payloadHex":"00010312"} | nc 127.0.0.1 19082
```

On Windows, you can also use a small Python, Node, or Java tool.

---

## Analyze a Stored Session

```bash
java -cp target\dsp408-proxy-0.1.0-SNAPSHOT-all.jar com.drremote.dsp408.proxy.cli.ProxyAnalyzeMain proxy_logs\proxy_session_20260403_072138_051.json
```

Output:

* frame count
* valid frames
* reads / writes
* detected write candidates
* first mapping hints for gain values

---

## Typical Workflow

### Capture only

1. start proxy
2. change Track DSP to proxy IP/port
3. perform actions in Track DSP
4. analyze the session log

### Stream to another tool

1. start receiver on `127.0.0.1:19081`
2. start proxy with `--stream-host` / `--stream-port`
3. connect Track DSP
4. analyze live events in the target tool

### Actively send and control

1. start proxy with `--control-host` / `--control-port`
2. connect Track DSP through the proxy
3. send `status`, `send_frame` or `send_payload` via the control port

---

## Known Limits

* currently only **one active client session**
* no authentication on the control port
* no replay queue across sessions
* no REST API, only TCP + JSON lines
* wrong payloads can affect the DSP unexpectedly

---

## Recommendation

For reverse engineering, this split is sensible:

* **Proxy** = stable capture + forwarding
* **Stream consumer** = decoding / analysis / UI
* **Control client** = targeted sending of test commands

This keeps the proxy small, robust, and neutral.

---

## Troubleshooting

### Maven cannot find pom.xml

Wrong folder. You must change into the project root:

```bash
cd C:\Users\XLA\DSP408\DSP408DecoderProxy
mvn clean package
```

### No connection to the DSP

* check `target-host` / `target-port`
* is the DSP reachable?
* did you really switch Track DSP to the proxy IP?
* check firewall

### Control does not send

* is there an active session?
* is the control port enabled?
* is `payloadHex` / `frameHex` valid hex?

### Stream does not arrive

* is the receiver running on `stream-host:stream-port`?
* is a firewall blocking it?
* does the target tool really expect NDJSON over TCP?

---

## Full Start Example

```bash
cd C:\Users\XLA\DSP408\DSP408DecoderProxy
mvn clean package

java -jar target\dsp408-proxy-0.1.0-SNAPSHOT-all.jar ^
  --listen-host 127.0.0.1 ^
  --listen-port 9761 ^
  --target-host 192.168.0.166 ^
  --target-port 9761 ^
  --log-dir proxy_logs ^
  --stream-host 127.0.0.1 ^
  --stream-port 19081 ^
  --control-host 127.0.0.1 ^
  --control-port 19082
```

Then in Track DSP:

* IP: `127.0.0.1`
* Port: `9761`
