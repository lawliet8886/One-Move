# Asset factory and Android continuation — 22 September 2026

Two isolated workstreams, neither merged into main:
- Android: chatgpt/android-lab. Latest inspected remote73cd1d59756927b3b772c6bfec3f1d9b2718097a, native run35755235139, all three jobs successful.
- This preparation: chatgpt/tripo-asset-prep. Original factoryc0d22c0; NumPy CI dependency fixd18aa0c; subsequent controlled-fixture action-name compatibility update. No runtime assets or physics changed on this branch.

## Verified Android delivery

Read the actual73cd device result.tsv: seven native cases PASS (lifecycle, twelve known-solution campaign paths, fourteen authored wrong choices/retries, basic retry, Back, compact1.3x font, large2.0x font including repeated touches). These are scripted Android tests, not blind visual exploration. Native artifact10708682023, APK10706864956, physics10708640194. Reviewed screenshots from level10, the large-font board, compact success and level9; large-font board remains289x385.5dp. Crash filter passed only for this run, not an absolute no-bug guarantee.

Preserved parallel commits214fc7d and73cd1d5: new Return Flight level10, arched sanctuary and corrected compact-counter test. The earlier7c run's compact/wrong-retry failures are historical, not failures of this newer successful run. A blocked next-layout helper write in this conversation was never executed or rerouted; the later Android commits came from the parallel workstream.

## Offline preparation evidence

Windows Blender5.2.0 LTS completed the full local smoke at .local-lab/art-factory-smoke-20260922-135225: nine atlas tests, GLB and FBX export/import, eight rigged samples per format, one frontal reference, alpha/pivot/hash/WebP checks and bone-matrix variation for all four test clips. Credits spent0. Test poses reuse the original Pip and are not final character performances or Android footage.

Cloud Blender4.0.2 exposed two genuine portability issues: its glTF exporter required NumPy, then its importer named the action OneMove_fixture_Pip_Rig. Install the missing package and use the SINGLE inspected action only for the controlled fixture recipe. Retain strict explicit production bindings, root-anchor checks and actual changing-bone assertions. Await the next cloud result before claiming cloud approval.

## Billing and handoff

CLI0.5.1 is installed locally in OneMove-Lab/.tools/tripo-cli-0.5.1. Doctor returned1, balance3 because no API authentication exists. API balance is UNKNOWN, not zero. Studio~4000 credits and01/10 expiry are Gabriel's reported values, not independently read. Official Tripo Help confirms Studio credits cannot call the API/CLI. No login, purchase, generation, retopo, texturing, rig or animation service was called. Next safe local command is tools/art_factory/OneMove-AssetFactory.ps1 -Action Preflight from this branch; actual Studio generation waits for browser/account/wallet/quote verification and explicit spending release.

Original OneMove-Lab checkout e36fe48 and its existing dirty source/cache files remain intact. ScanFlow emulator is untouched. One Move Lab automation was found paused and was not enabled or duplicated. The prep directory may contain untracked tools/art/__pycache__; it is a local import cache, not source. A separately mentioned branch chatgpt/tripo-asset-factory-20260922 was not present in the remote refs when checked; do not infer that a local parallel branch is absent or overwrite it.
