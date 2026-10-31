# Stream and control protocol

[Start the proxy](README.md#start-for-raider) ·
[Raider CLI](https://github.com/DoctorRemoteControl/dsp-raider/blob/main/docs/CLI.md)

Both interfaces are TCP servers provided by the proxy. Each JSON object is
transmitted as one UTF-8 line with a trailing newline. Ports 19081 and 19082
are the Raider convention; the proxy requires host and port to be explicitly
set for each interface.

## Passive event stream

Clients connect to the stream listener and receive NDJSON events. They send
no DSP commands over it. The
[`NdjsonStreamServer`](src/main/java/com/drremote/dsp408/proxy/stream/NdjsonStreamServer.java)
implementation uses these events:

| `type` | Fields in addition to `type` |
| --- | --- |
| `session_started` | `sessionId`, `startedAt`, `listenHost`, `listenPort`, `targetHost`, `targetPort`, `clientAddress` |
| `frame` | `sessionId`, `frame` |
| `note` | `sessionId`, `note` |
| `session_ended` | `sessionId`, `endedAt`, `bytesPcToDsp`, `bytesDspToPc`, `frameCount`, `droppedEvents` |

The nested
[`frame`](src/main/java/com/drremote/dsp408/proxy/model/LoggedFrame.java) object
contains `index`, `direction` (`PC_TO_DSP` or `DSP_TO_PC`), `timestampUtc`,
`frameHex`, `validFrame`, `payloadHex`, `payloadPrettyHex`, `payloadAscii`,
`payloadLength` and `note`. Hex bytes are transported as strings.

Recordings also retain invalid frames and their status. Raider revalidates
wire bytes for its successful captures and snapshots. A connected stream
client receives traffic while connected; it does not retrieve older frames.
`droppedEvents` reveals events discarded when client queues are overloaded.

## Active control channel

Control accepts one JSON line per request and sends one JSON line in response.
It is not an HTTP API. Implementation and input fields:
[`ProxyControlServer`](src/main/java/com/drremote/dsp408/proxy/control/ProxyControlServer.java),
[`ProxyControlCommand`](src/main/java/com/drremote/dsp408/proxy/control/ProxyControlCommand.java).

| `type` | Other input fields | Effect |
| --- | --- | --- |
| `status` | none | read proxy session status |
| `ensure_session` | none | establish a standalone DSP session if necessary |
| `reset_session` | none | reset current session |
| `begin_control_transaction` | optional `timeoutMs`, default 7000 | begin control transaction window |
| `end_control_transaction` | none | end control transaction window |
| `send_frame` | `frameHex` | inject a complete valid wire frame |
| `send_payload` | `payloadHex` | add frame/checksum and inject |

Example of a pure status query without a device command:

```json
{"type":"status"}
```

Example status response; boolean values depend on the running state:

```json
{"ok":true,"sessionActive":true,"injectReady":true,"controlTxActive":false}
```

`send_frame` and `send_payload` also attempt to establish a standalone session
when not ready. The original GUI is not required. `sessionActive` alone does
not confirm injection readiness; Raider additionally checks `injectReady`.

The control response confirms the proxy operation. An `ok: true` after sending
proves neither a DSP ACK nor the actual target value. DSP responses appear in
the event stream; Raider correlates them with the observed outgoing frame and,
where applicable, the expected command. A transaction window is not a
multi-parameter device transaction with rollback. Raider's explicit
[`apply` workflow](https://github.com/DoctorRemoteControl/dsp-raider/blob/main/docs/EXECUTION.md) validates an actual DSP
ACK and full-snapshot target readback per write; it stops on failure without
retry or rollback.

Failures can return `ok: false` with `error`, or with `message` and session
status. Clients must check `ok` and must not use the message text as stable,
machine-readable confirmation of a parameter value.

## Saved sessions

Log files in the selected `--log-dir` contain session metadata, byte counters,
notes and frames. Filenames follow `proxy_session_<sessionId>.json`.
The [proxy README](README.md#data-and-analysis) documents the analysis command
and storage location.

A proxy session log, Raider `capture` and Raider `dump` are different JSON
documents. Session logs are analyzed with `ProxyAnalyzeMain`;
`dsp-raider decode` expects a Raider capture or snapshot.
