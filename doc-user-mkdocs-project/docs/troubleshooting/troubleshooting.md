# TSTool / Troubleshooting #

Troubleshooting TSTool for Google Drive involves confirming that the core product and plugin are performing as expected.

*   [Troubleshooting Core TSTool Product](#troubleshooting-core-tstool-product)
*   [Troubleshooting Google Drive TSTool Integration](#troubleshooting-google-drive-tstool-integration)
    +   [***Commands(Plugin)*** Menu Contains Duplicate Commands](#commandsplugin-menu-contains-duplicate-commands)
    +   [Authentication and permission problems](#authentication-and-permission-problems)

------------------

## Troubleshooting Core TSTool Product ##

See the main [TSTool Troubleshooting documentation](https://opencdss.state.co.us/tstool/latest/doc-user/troubleshooting/troubleshooting/).

## Troubleshooting Google Drive TSTool Integration ##

The following are typical issues that are encountered when using TSTool with Google Drive.

### ***Commands(Plugin)*** Menu Contains Duplicate Commands ###

If the ***Commands(Plugin)*** menu contains duplicate commands,
TSTool is finding multiple plugin `jar` files.
To fix, check the `plugins` folder and subfolders for the software installation folder
and the user's `.tstool/NN/plugins` folder.
Remove extra jar files, leaving only the version that is desired (typically the most recent version).

If necessary, completely remove plugin files and re-install the plugin to avoid confusion about mixing
software files from multiple versions.

### Authentication and permission problems ###

The Google Drive plugin uses the Google Drive Application Programming Interface (API).
The API must be enabled to allow the plugin to work.

See the [Appendix - Enable API](../appendix-enable-api/enable-api.md) documentation.
