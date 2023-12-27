# TSTool / Enable API #

**This documentation is under development as the plugin is tested in multiple Google Drive accounts.**

This appendix describes how to enable the Google Drive API,
which is necessary to enable Google Drive plugin features.
Enabling the API typically requires administrative privileges for the Google account manager for an organization.

*   [Overview](#overview)
*   [Enable API](#enable-api)
*   [Configuring an Internal Application](#configuring-an-internal-ppplication)
    +   [Configure the OAuth consent screen](#configure-the-oauth-consent-screen)
    +   [Configure Scopes](#configure-scopes)
*   [Authorize Credentials for a Desktop Application](#authorize-credentials-for-a-desktop-application)
    +   [Using OAuth](#using-oauth)
    +   [Using a Service Account](#using-a-service-account)

-------

## Overview ##

TSTool is used with Google Drive to automate file uploads and downloads.
The TSTool Google Drive plugin is developed and tested on Windows but can also be installed on Linux.
Google Drive is available if a plan has been purchased from Google, for example Google Workspace.

*   See the [Google Drive download page](https://www.google.com/drive/download/).
*   Desktop Google Drive does not need to be installed,
    although it can simplify troubleshooting.

Once installed, login to Google Drive, which typically maps drive `G:` on Windows.

The online Google Drive can also be accessed,
for example see the grid tool in the upper right of Google Mail and Calendar web pages.

It is generally desirable to control the API's access,
for example to restrict access to only allow reading files,
or to allow read and write only to specific folder(s).
The granularity of access control is handled via credentials as follows:

*   Use an API project (top-level granularity):
    +   if using OAuth, create a client ID with specific scope(s) to control file access
        (the granularity is implementd at the client ID level)
    +   if using a service account and API key,
        create a separate service account and define IAM conditions to control file access
        (the granularity is implemented at the service account level)

The TSTool [`GoogleDrive`](../command-ref/GoogleDrive/GoogleDrive.md) command uses a "session ID"
specified with the `SessionId` command parameter,
which allows for granularity when configuring credentials.
For example, save the credentials from one of the above approaches to a file and
use the `SessionId` to match the file.

## Enable API ##

The Google Drive API must be enabled in the Google account.

*   Because TSTool is written in Java,
    see the [Java quickstart / Enable the API](https://developers.google.com/drive/api/quickstart/java) documentation.
*   See the [Cloud APIs / Getting started](https://cloud.google.com/apis/docs/getting-started) documentation

Clicking on the ***Enable the API*** button on the above page will open the Google Cloud Console.
The API must be enabled for a project.
If a suitable project does not exist, create one by selecting the ***CREATE PROJECT*** button.
For example create a project AbcOps for an organization 

Browse the available APIs using the link
[`https://console.cloud.google.com/apis/library/browse`](https://console.cloud.google.com/apis/library/browse).

Select the Google Drive API and then select ***Enable***.

## Configuring an Internal Application ##

An internal application is suitable for internal organization use.

### Configure the OAuth consent screen ###

*   Because TSTool is written in Java,
    see the [Java quickstart / Configure the OAuth consent screen](https://developers.google.com/drive/api/quickstart/java) documentation.

Click the [***Go to OAuth consent screen***](https://console.cloud.google.com/apis/credentials/consent) button.

Then select ***Internal*** as shown below.

**<p style="text-align: center;">
![OAuth consent screen](internal-app-oauth-consent-screen.png)
</p>**

**<p style="text-align: center;">
Internal Application OAuth Consent Screen (<a href="../internal-app-oauth-consent-screen.png">see full-size image)</a>
</p>**

Enter the information for the application, for example as shown below.

**<p style="text-align: center;">
![App information](app-information.png)
</p>**

**<p style="text-align: center;">
App Information (<a href="../app-information.png">see full-size image)</a>
</p>**

**<p style="text-align: center;">
![App logo](app-logo.png)
</p>**

**<p style="text-align: center;">
App logo (<a href="../app-logo.png">see full-size image)</a>
</p>**

**<p style="text-align: center;">
![App domain](app-domain.png)
</p>**

**<p style="text-align: center;">
App domain (<a href="../app-domain.png">see full-size image)</a>
</p>**

**<p style="text-align: center;">
![Developer contact](developer-contact.png)
</p>**

**<p style="text-align: center;">
Developer Contact (<a href="../developer-contact.png">see full-size image)</a>
</p>**

Press ***SAVE AND CONTINUE*** to continue to the Scopes configuration.

### Configure Scopes ###

Scopes can be configured to control the type of data that can be accessed.
The initial ***Scopes*** page is as follows.

**<p style="text-align: center;">
![Initial scopes](scopes-init.png)
</p>**

**<p style="text-align: center;">
Initial Scopes Configuration (<a href="../scopes-init.png">see full-size image)</a>
</p>**

Although the Google Drive API could be used for sensitive scopes (user data),
it is recommended that the API is used for files that are not sensitive,
such as project data and datasets.
For example, data that are used to automate an organizations operations can be used with the API,
but not employee personal data.

Use the ***ADD OR REMOVE SCOPES*** button.

Search for "Google Drive" to see scopes for Google Drive.
Limit the scope as appropriate, for example select ***Scope*** `../auth/drive file`,
which has a ***User-facing description*** `See, edit, create, and delete only the specific Google Drive files you use with this app`.
**TODO: How are the folders used with the app specified?**

Press ***SAVE AND CONTINUE***.

## Authorize Credentials for a Desktop Application ##

The previous section enabled the API.
It is also necessary to authorize credentials for a desktop application (TSTool with Google Drive plugin).
The application can be authorized in several ways.  See:

*   [Choose the best way to use and authenticate service accounts on Google Cloud](https://cloud.google.com/blog/products/identity-security/how-to-authenticate-service-accounts-to-help-keep-applications-secure)

### Using OAuth ###

This approach results in an interactive authentication at runtime, typically via a web browser.
Consequently, it may not be suitable for processes that need to run unattended (batch or "headless").
The interactive approval is requested each time that the credentials expire.
Consequently, it may be possible to interactively approve the access ("Application X wants to access Google Drive...") once
and then run in batch mode with the same OAuth credentials on the device.

*   Because TSTool is written in Java,
    see the [Java quickstart / Configure the OAuth consent screen](https://developers.google.com/drive/api/quickstart/java) documentation.

1.  In the Google Cloud console ([`https://console.cloud.google.com`](https://console.cloud.google.com),
    go to the ***Menu / APIs & Services / Credentials***.
    The ***Go to Credentials*** link on the above page can be used.
2.  Click ***Create Credentials > OAuth client ID***.
3.  Click ***Application type > Desktop app***.
4.  In the ***Name*** field, type a name for the credential.
    This name is only shown in the Google Cloud console.
    **<p style="text-align: center;">
    ![Create OAuth client ID](create-oauth-client-id.png)
    </p>**
5.  Click ***Create***. The OAuth client created screen appears, showing your new Client ID and Client secret.
6.  Click ***OK***. The newly created credential appears under OAuth 2.0 Client IDs.
    Save the downloaded JSON file for the desktop application.
    For example, save in `C:/Users/user/AppData/Local/TSTool/GoogleDrive/oauth2-credentials-xxxxx.json`,
    which is a folder that is only visible to the specific user.
    Specify `xxxxx` as a name that matches the OAuth client ID and will be specified as the
    TSTool [`GoogleDrive`](../command-ref/GoogleDrive/GoogleDrive.md) command `SessionId` parameter.

The configuration that was created above can be reviewed and edited from the Google Cloud console.

### Using a Service Account ###

A service account can be used to allow for unattended execution of software,
for example automated processes that run on a schedule.
Each service account must have an email account,
which is used for identification, typically automatically assigned during the configuration, as described below.

**Important:  A service account is treated as a separate user.
Therefore, in order for the service account to access files,
the files (or folders) must be shared with the service account by
sharing specific items or use a shared drive that the service account can access.
The service account email can be used when sharing.**

The following instructions were created using multiple resources.
The Google Cloud Console views may vary depending on the the sequence of pages that are used.

In the [Google Cloud Console](https://console.cloud.google.com/apis/dashboard),
select ***Enable APIS AND SERVICES*** and then ***Google Workspace*** and then **Google Drive API***.

***API / Service Details / Google Drive API*** web page,
click on ***CREATE CREDENTIALS / Service Account***.

Create a service account and specify the ***Service account details***.
For example, use the following:

*   account name `xxx-ops` for an organization's automated operations (replace `xxx` with something appropriate)
*   account ID `xxx-ops`
*   an email address will automatically be generated for the account ID

Press ***CREATE AND CONTINUE*** to create the service account and continue with configuration.

The ***Grant this service account access to project*** section allows a project to be assigned,
with role(s) and conditions.
This is used to restrict the access for the service account.
Use the Viewer role for read-only access and the Editor role for create, update, and delete permissions.

To further restrict the service account, use the ***ADD IAM CONDITION***.
For example, add a condition to restrict access to only certain folders.
See the following:

*   [Resource attributes for IAM Conditions](https://cloud.google.com/iam/docs/conditions-resource-attributes)

However, Google Drive does not appear to support the same conditions used with Google Cloud Storage.
Therefore, to be safe, consider selecting the Viewer role initially.

Press ***CONTINUE*** to save the settings and continue with the configuration.

The ***Grant users access to this service account*** section allows users to be specified to access the service account.

Press ***Done*** to save the changes.

Next, generate the service account key JSON:

1.  In the ***Service accounts*** section, find the service account.
2.  Click on the pencil/edit icon next to the service account name.
3.  Navigate to the ***Keys*** tab and click on ***Add Key.***
4.  Choose the JSON key type and click ***Create***.
    This will download a JSON file containing the credentials for the service account.
5.  Move the file to the following location, which is the default for the TSTool Google Drive plugin:
    *   Windows:  `%APPDATALOCAL%\TSTool\GoogleDrive\api-key-xxxxx.json`
        (where `xxxxx` is consistent with the service account name and will be specified using the
        TSTool [`GoogleDrive`](../command-ref/GoogleDrive/GoogleDrive.md) command `SessionId` parameter).
