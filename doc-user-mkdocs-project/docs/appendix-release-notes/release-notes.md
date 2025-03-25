# TSTool Google Drive Plugin / Release Notes #

Release notes are available for the core TSTool product and plugin.
The core software and plugins are maintained separately and may be updated at different times.

*   [TSTool core product release notes](http://opencdss.state.co.us/tstool/latest/doc-user/appendix-release-notes/release-notes/).
*   [TSTool Version Compatibility](#tstool-version-compatibility)
*   [Release Note Details](#release-note-details)

## TSTool Version Compatibility ##

The following table lists TSTool and plugin software version compatibility.

**<p style="text-align: center;">
TSTool and Plugin Version Compatibility
</p>**

| **Plugin Version** | **Required TSTool Version** | **Comments** |
| -- | -- | -- |
| 2.0.0 | >=  15.0.0 | TSTool and plugin updated to Java 11, new plugin manager. |
| 1.3.0+ | >= 14.6.0 | |

## Release Note Details ##

Plugin release notes are listed below.
The repository issue for release note item is shown where applicable.

*   [Version 2.0.0](#version-200)
*   [Version 1.0.1](#version-101)
*   [Version 1.0.0](#version-100)

----------

## Version 2.0.0 ##

**Major release to use Java 11.**

*   ![change](change.png) Update the plugin to use Java 11:
    +   The Java version is consistent with TSTool 15.0.0.
    *   The plugin installation now uses a version folder,
        which allows multiple versions of the plugin to be installed at the same time,
        for use with different versions of TSTool.

## Version 1.0.1 ##

**Maintenance release to fix authentication.**

*   ![new](new.png) [1.0.1] Minor fix to deal with default authentication method in the
    [`GooogleDrive`](../command-ref/GoogleDrive/GoogleDrive.md) command.

## Version 1.0.0 ##

**Initial release.**

*   ![new](new.png) [1.0.0] The initial release includes the following command:
    +   [`GooogleDrive`](../command-ref/GoogleDrive/GoogleDrive.md) command with functioning `Download`, `List`, and `ListDrives` commands
        for OAuth and service account authentication.
