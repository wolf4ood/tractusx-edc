# Data Plane Signaling Protocol API (provider data-plane server)

Implements the **provider data-plane server** side of the
[Data Plane Signaling protocol](https://eclipse-dataplane-signaling.github.io/dataplane-signaling/HEAD/)
(spec `1.0-RC2`) on top of the **legacy** EDC data-plane framework
(`org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager`).

It is a standalone, opt-in module: it does not replace the existing data-plane signaling API and is not wired
into any BOM. Add it to a data-plane runtime manually to expose the new protocol endpoints.

## Endpoints

Registered on the `ApiContext.CONTROL` web context, base path `/dataflows`. Messages are exchanged as **plain
JSON** (not JSON-LD), matching the protocol's JSON-schema message shapes.

| Method | Path                       | Request                  | Legacy framework call                | Response                       |
|--------|----------------------------|--------------------------|--------------------------------------|--------------------------------|
| POST   | `/dataflows/prepare`       | `DataFlowPrepareMessage` | `DataPlaneManager.provision(..)`     | `DataFlowStatusMessage` (200)  |
| POST   | `/dataflows/start`         | `DataFlowStartMessage`   | `validate(..)` + `start(..)`         | `DataFlowStatusMessage` (200)  |
| POST   | `/dataflows/{id}/suspend`  | `DataFlowSuspendMessage` | `suspend(id)`                        | 200                            |
| POST   | `/dataflows/{id}/resume`   | `DataFlowResumeMessage`  | `start(..)` (re-start)               | `DataFlowStatusMessage` (200)  |
| POST   | `/dataflows/{id}/terminate`| `DataFlowTerminateMessage`| `terminate(id, reason)`             | 200                            |
| GET    | `/dataflows/{id}/status`   | -                        | `getTransferState(id)`               | `DataFlowStatusResponseMessage`|
| POST   | `/dataflows/{id}/started`  | `DataFlowStartedNotificationMessage` | *stub (acknowledged, no-op)* | 200                       |
| POST   | `/dataflows/{id}/completed`| -                        | *stub (acknowledged, no-op)*         | 200                            |

The consumer-side notifications `started`/`completed` are implemented as **stubs**: they are acknowledged with HTTP
200 but not acted upon, since the legacy framework has no equivalent. Control-plane registration (`/controlplanes`)
is out of scope.

## How to plug it in

Add the module to the dependencies of your data-plane runtime (e.g. `edc-dataplane/edc-dataplane-base`):

```kotlin
implementation(project(":edc-extensions:dataplane:dataplane-signaling-api"))
```

The extension auto-registers via SPI; no configuration is required. It needs a `DataPlaneManager` and the `CONTROL`
web context on the classpath (both present in a standard EDC data plane).

## Notes / limitations

- **`transferType`** is parsed from the combined protocol string `"<destinationType>-<PULL|PUSH>[-<responseChannel>]"`
  (e.g. `HttpData-PULL`) into the framework `TransferType`.
- **`dataAddress`** on a start/resume message is mapped onto the framework `sourceDataAddress` (the provider/source
  address the control plane resolved), consistent with the EDC control plane's
  `DataPlaneSignalingFlowController`.
- **Resume** has no native equivalent in the legacy `DataPlaneManager`, so it is mapped onto `start()` for the
  existing flow. The protocol resume message does not carry `transferType`, so flows that require it on resume must
  be handled by the control plane re-sending a full start.
- **State mapping**: the framework `DataFlowStates` enum is mapped onto the protocol state vocabulary
  (`INITIALIZED/PREPARING/PREPARED/STARTING/STARTED/SUSPENDED/COMPLETED/TERMINATED`) — see `SignalingMapper`.

## Build

Requires JDK 21 (EDC `0.18.0-SNAPSHOT`).

```
./gradlew :edc-extensions:dataplane:dataplane-signaling-api:check -DincludeTags=ApiTest
```
