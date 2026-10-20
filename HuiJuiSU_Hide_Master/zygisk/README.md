# Zygisk Next

Standalone implementation of Zygisk, providing Zygisk API support for KernelSU and a replacement of Magisk's built-in Zygisk.

## Requirements

### General

+ No multiple root implementation installed

### KernelSU

+ Minimal KernelSU version: 10940
+ Minimal KernelSU Manager (ksud) version: 11575

### Magisk

+ Minimal version: 26402
+ Built-in Zygisk turned off

## Zygisk Next API

Zygisk Next provides an API for injecting into init-oriented processes. See [zygisk_next_api.h](zygisk_next_api.h) for more information.

Example usage can be found in [ZygiskNextModuleSample](https://github.com/5ec1cff/ZygiskNextModuleSample).

Zygisk Next also supports application callbacks for HyperOS Rust Runtime, which are documented in [docs/hyos_runtime.md](docs/hyos_runtime.md). This is a native-only API and does not provide ART or JNI compatibility to regular Zygisk modules.

## Copyright Notice

Copyright © 2024-2026 LSPosed Developers. All rights reserved.

The software Zygisk Next, starting from version v4-0.9.2, is no longer under the GPL-3.0 License. Instead, all rights to the software are reserved by the owner.

The following conditions now apply:

1. **No Modifications:** The software may not be modified in any way. This includes but is not limited to changing, adding, or removing any part of the software's code or functionality.

2. **No Redistribution:** The software may not be redistributed in any form. This includes but is not limited to renaming, selling, or including the software as part of another project.

3. **No Picking:** No parts, pieces, or components of the software may be extracted and submitted to other projects. This includes, but is not limited to, code snippets, functions, and released binaries.

4. **No Claim to Succession:** Any fork of the software that was created before the license change may not claim to be an official or unofficial successor to the project. This includes but is not limited to using the project's name, branding, or reputation to imply a connection to the original project.
