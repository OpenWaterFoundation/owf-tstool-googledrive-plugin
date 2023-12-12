# TSTool / Getting Started #

This chapter provides an overview of getting started with TSTool and the Google Drive plugin commands.

*   [Introduction](#introduction)
*   [Installing and Configuring TSTool](#installing-and-configuring-tstool)
*   [Starting TSTool](#starting-tstool)
*   [Installing the Google Drive Plugin](#installing-the-googledrive-plugin)
*   [Using the Google Drive Plugin Commands](#using-the-googledrive-plugin-commands)

----------------

## Introduction ##

The TSTool user interface (UI) provides features to browse data, create command file workflows to process data,
and view time series and other information products.
The TSTool UI design follows several basic concepts and interactions.

*   See the [TSTool / Getting Started](https://opencdss.state.co.us/tstool/latest/doc-user/getting-started/getting-started/)
    documentation for core product information.

The remainder of this documentation focuses on getting started with TSTool using the Google Drive plugin.

## Installing and Configuring TSTool ##

If not already done, [install TSTool](https://opencdss.state.co.us/tstool/latest/doc-user/appendix-install/install/).
TSTool is most often installed on Windows but also runs on Linux.
Contact the Open Water Foundation if a Linux version is required.

## Starting TSTool ##

TSTool can be run in interactive mode and batch (command line) mode.
Interactive mode is typically used to prototype and configure automated workflows and products.
Batch mode can be used to run the software in headless mode on a server,
for example on a schedule or in response to web requests.

When using the State of Colorado’s TSTool default configuration on Windows,
the software can be started using ***Start / Programs / CDSS / TSTool-Version***.
The menus vary slightly depending on the operating system version.

Use the `tstool` script to start the software on Linux.
The program can be found in the software's `/bin` folder.
Multiple versions of TSTool can be installed to facilitate transition from older to newer versions.

To process a command file in batch mode without showing the user interface,
use a command line similar to the following:

```
tstool -–commands commands.tstool
```

It is customary to name command files with a `.tstool` file extension.
It may be necessary to specify a full (absolute) path to the command file when
running in batch mode in order for TSTool to fully understand the working directory.

See the [Running TSTool in Various Modes appendix](https://opencdss.state.co.us/tstool/latest/doc-user/appendix-running/running/)
for more information about running in batch and other modes.

## Installing the Google Drive Plugin ##

See the [Appendix - Install Plugin](../appendix-install/install.md) documentation for instructions to install the plugin.

## Using the Google Drive Plugin Commands ##

TSTool Google Drive plugin commands are provided for commonly used Google Drive features.
Additional commands will be added in the future as needed.
The primary uses of the plugin commands are:

*   streamline publishing TSTool output products to the cloud, for use in web applications and downloads
*   integrating TSTool with larger workflows and systems

The following is a summary of plugin commands,
which are listed in the ***Commmands(Plugin)*** menu.
See also the [Reference - Commands / Overview](../command-ref/overview.md) documentation.

**<p style="text-align: center;">
Google Drive Plugin Commands
</p>**

| **Command**&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | **Description** |
| -- | -- |
| [`GoogleDrive`](../command-ref/GoogleDrive/GoogleDrive.md) | Perform Google Drive tasks: <ul><li>list files</li></ul>|
