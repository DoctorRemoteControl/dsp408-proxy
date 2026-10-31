# DSP408 TCP proxy

[![Verify](https://github.com/DoctorRemoteControl/dsp408-proxy/actions/workflows/verify.yml/badge.svg)](https://github.com/DoctorRemoteControl/dsp408-proxy/actions/workflows/verify.yml)

The proxy forwards traffic between DSP clients and the device, logs frames and
optionally provides a passive event stream and an active control channel.
It is an independent Java 21/Maven project.

## Build and help

From the [multi-project build instructions](https://github.com/DoctorRemoteControl/dsp-raider/blob/main/docs/DEVELOPMENT.md#building):

```bash
mvn -B -ntp verify
```

For an individual build, provide the independent `dsp408-protocol` source
checkout alongside this project. From `tracks-dsp-proxy`, run:

```bash
bash ci/verify.sh
```

The script installs the shared library locally, then verifies the proxy.
Set `DSP408_PROTOCOL_DIR` for another source location. For the same library
source as the hosted build, use the exact commit recorded in
[`ci/protocol-revision`](ci/protocol-revision). Then display help:

```bash
java -jar target/dsp408-proxy-0.1.0-SNAPSHOT-all.jar --help
```

## Automated verification

The [GitHub Actions workflow](.github/workflows/verify.yml) prepares
verification on pushes, pull requests and manual runs. It checks out
`DoctorRemoteControl/dsp408-protocol` at the full commit SHA recorded in
`ci/protocol-revision`, validates the SHA format and runs `ci/verify.sh` with
Java 21 and a fresh temporary Maven repository. The actions themselves are
pinned to full commits; no dependency cache or physical DSP is used.

Source repository: [DoctorRemoteControl/dsp408-proxy](https://github.com/DoctorRemoteControl/dsp408-proxy).
[GitHub Actions](https://github.com/DoctorRemoteControl/dsp408-proxy/actions/workflows/verify.yml)
records the result for each published revision. A proxy clone alone does not
include the protocol source or its Maven artifact. The workflow currently
assumes readable protocol source without an additional secret; private
cross-repository access needs separate authentication configuration. No Maven
package is currently published here. No license file has been selected.

After a reviewed protocol update, publish its source commit and update
`ci/protocol-revision` to that exact full SHA before verifying the consumer.
Local `ci/verify.sh` builds the supplied source checkout, including local
changes; it does not automatically fetch or enforce the hosted revision.

## Start for Raider

Run this command from the proxy project. It starts live access; the IP is the
local example setup and must be adjusted for a different device:

```bash
java -jar target/dsp408-proxy-0.1.0-SNAPSHOT-all.jar \
  --listen-host 127.0.0.1 --listen-port 9761 \
  --target-host 192.168.0.166 --target-port 9761 \
  --stream-host 127.0.0.1 --stream-port 19081 \
  --control-host 127.0.0.1 --control-port 19082 \
  --log-dir ../../../Betriebsdaten/DSP408/proxy-logs
```

The log path matches the local `drremote` layout. For a checkout elsewhere,
choose a suitable path with `--log-dir`. Without this parameter, the program
defaults to `proxy_logs` relative to the working directory.

**Stream and control are enabled only when both host and port are specified
for each.** They are not automatically included in a default startup.
Without arguments, an interactive configuration dialog opens.

Keep the terminal open. The proxy accepts `status` and `quit` there; EOF on
stdin also stops it. Starting it in the background with stdin immediately
detached therefore does not create a persistent service.

## What does each connection do?

| Example endpoint | Protocol / role | Client |
| --- | --- | --- |
| `192.168.0.166:9761` | target device, binary DSP protocol | proxy |
| `127.0.0.1:9761` | proxy's GUI/DSP client listener | original software or another DSP client |
| `127.0.0.1:19081` | NDJSON over TCP, passive event stream | Raider `capture` and stream consumers |
| `127.0.0.1:19082` | JSON lines over TCP, control channel | Raider `connect`/`identify`/`dump`/`apply`, active scripts and tools |

`stream-host` is a **server's local bind address**. The analyzer connects to
this listener; no receiving server needs to run on the analyzer. Control is
also a TCP listener, not an HTTP or WebSocket API.

Event fields, control commands and the distinction between proxy responses
and DSP responses are documented in the [interface reference](PROTOCOL.md).
Raider's explicit [application workflow](https://github.com/DoctorRemoteControl/dsp-raider/blob/main/docs/EXECUTION.md)
checks actual DSP ACKs and readback; a successful proxy control response alone
is insufficient.

For reads, control can establish or reuse a device session. The GUI does not
need to run. For GUI captures, the original software must connect through the
GUI listener. On the same computer, it uses `127.0.0.1:9761`. On another
computer, it must use a reachable address of the proxy computer; explicitly
set the required bind address there with `--listen-host`.

## Options

| Option | Default / meaning |
| --- | --- |
| `--listen-host` | `127.0.0.1`; GUI listener |
| `--listen-port` | `9761` |
| `--target-host` | `192.168.0.166`; alias `--target-ip` |
| `--target-port` | `9761` |
| `--log-dir` | `proxy_logs` |
| `--stream-host`, `--stream-port` | optional pair to enable the stream |
| `--control-host`, `--control-port` | optional pair to enable control |
| `--help`, `-h` | help without starting a connection |

## Data and analysis

Both traffic directions use the
[shared protocol library](https://github.com/DoctorRemoteControl/dsp408-protocol). The declared length
field determines frame boundaries; `10 03` inside a payload does not terminate
a frame prematurely. Checksum status is retained in recordings.

Raw logs are source material. Raider interprets gain, mute or delay fields
using its documented firmware/source mappings. Recording alone does not let
the proxy confirm a field's meaning.

Saved sessions can be analyzed with the included analysis CLI:

```bash
java -cp target/dsp408-proxy-0.1.0-SNAPSHOT-all.jar com.drremote.dsp408.proxy.cli.ProxyAnalyzeMain proxy_session.json
```

Replace `proxy_session.json` with the path to a saved session file. Without an
argument, this analysis program displays its usage; it does not recognize
`--help` as a separate option.

## Troubleshooting

| Observation | Check |
| --- | --- |
| Raider cannot reach stream/control | Both host/port pairs set at startup? Correct endpoint? Proxy still active? |
| No events in capture | Stream connection active, but possibly no traffic during the window; GUI actions require the GUI to connect through the proxy |
| DSP unreachable | Target IP/port and existing device session; a running listener does not prove a device connection |
| GUI works but Raider does not | GUI listener 9761 is separate from stream 19081 and control 19082 |
| Proxy exits after startup | stdin closed? The interactive program needs an open terminal |
| Maven cannot find the library | Use the workspace build or install `dsp408-protocol` locally first |

[Raider workflows](https://github.com/DoctorRemoteControl/dsp-raider/blob/main/docs/WORKFLOWS.md) describe subsequent
recording and offline analysis. [Architecture](https://github.com/DoctorRemoteControl/dsp-raider/blob/main/docs/ARCHITECTURE.md) shows the
separation between framing, transport and the parameter model.
