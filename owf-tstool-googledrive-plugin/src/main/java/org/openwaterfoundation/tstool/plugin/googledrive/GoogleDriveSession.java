// GoogleDriveSession - Google Drive session data

/* NoticeStart

OWF TSTool Google Drive Plugin
Copyright (C) 2023 Open Water Foundation

OWF TSTool Google Drive Plugin is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

OWF TSTool Google Drive Plugin is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

You should have received a copy of the GNU General Public License
    along with OWF TSTool Google Drive Plugin.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package org.openwaterfoundation.tstool.plugin.googledrive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.ServiceAccountSigner;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.common.collect.ImmutableList;

import RTi.Util.IO.IOUtil;
import RTi.Util.Message.Message;

/*
 * Google Drive session for a user, which provides connection and credential information.
 * See Google Java Quickstart: https://developers.google.com/drive/api/quickstart/java
 */
public class GoogleDriveSession {
	
	/**
	 * Session ID, which is appended to the credentials file name.
	 */
	private String sessionId = null;
	
	/**
	 * Authentication method.
	 */
	private GoogleDriveAuthenticationMethodType authenticationMethod = null;
	
	/**
	 * Path to the credentials file.
	 */
	private String credentialsFilePath = null;
	
	/**
	 * Whether the credentials are OK and the session is authenticated
	 */
	private boolean isSessionAuthenticated = false;
	
	/**
	 * The error if credentials are not OK.
	 */
	private String problem = "";

	/**
	 * Recommendation to fix the problem.
	 */
	private String problemRecommendation = "";

	/**
	 * Path to the tokens folder, used by the Google Drive API to cache and optimize the use of tokens.
	 */
	private String tokensFolderPath = null;
	
	/**
	 * Old-style google credential, used to authenticate client, etc.
	 * The credentials are applicable throughout the session.
	 */
	private Credential credential = null;

	/**
	 * Newer credential.
	 */
	private GoogleCredentials credentials = null;
	
	// The following are from the Google Drive Quick Start:
	//   https://developers.google.com/drive/api/quickstart/java
	
	/**
	 * Application name.
	 */
	private final String APPLICATION_NAME = "TSTool";
	
	/**
	 * Global instance of the JSON factory.
	 */
	private final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	  
	/**
	 * Folder to store authorization tokens for this application.
	 */
	//private final String TOKENS_DIRECTORY_PATH = "";
	
	/**
	 * Global instance of the scopes required by this quickstart.
	 * If modifying these scopes, delete your previously saved tokens/ folder.
	 */
	private static final List<String> SCOPES =
		Collections.singletonList(DriveScopes.DRIVE_READONLY);

	//private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

	private NetHttpTransport httpTransport = null;
	
	/**
	 * Create a new session, which holds the credential.
	 * @param sessionId the session ID to match the credentials file
	 * @param authenticationMethod the authentication method to use
	 */
	public GoogleDriveSession ( String sessionId, GoogleDriveAuthenticationMethodType authenticationMethod )
	throws FileNotFoundException, IOException, GeneralSecurityException {
		String routine = this.getClass().getSimpleName();
		
		// Build a new authorized API client service.
		this.sessionId = sessionId;
		this.authenticationMethod = authenticationMethod;
	    this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
	    
	    // Set the paths to credentials file and tokens folder.
		if ( IOUtil.isUNIXMachine() ) {
			// Standard folder:
			// - HOME/.googledrive/credentials.json
			String HOME = System.getenv("HOME");
			if ( (HOME != null) && !HOME.isEmpty() ) {
				if ( this.authenticationMethod == GoogleDriveAuthenticationMethodType.OAUTH ) {
					this.credentialsFilePath = HOME + "/.googledrive/oauth2-credentials-" + this.sessionId + ".json";
				}
				else if ( this.authenticationMethod == GoogleDriveAuthenticationMethodType.SERVICE_ACCOUNT_KEY ) {
					this.credentialsFilePath = HOME + "/.googledrive/api-key-" + sessionId + ".json";
				}
				this.tokensFolderPath = HOME + "/.googledrive/tokens";
			}
		}
		else {
			// Windows.
			String APP_DATA = System.getenv("LOCALAPPDATA");
			if ( (APP_DATA != null) && !APP_DATA.isEmpty() ) {
				if ( this.authenticationMethod == GoogleDriveAuthenticationMethodType.OAUTH ) {
					this.credentialsFilePath = APP_DATA + File.separator + "TSTool" + File.separator +
						"GoogleDrive" + File.separator + "oauth2-credentials-" + sessionId + ".json";
				}
				else {
					this.credentialsFilePath = APP_DATA + File.separator + "TSTool" + File.separator +
						"GoogleDrive" + File.separator + "api-key-" + sessionId + ".json";
				}
				this.tokensFolderPath = APP_DATA + File.separator + "TSTool" + File.separator +
					"GoogleDrive" + File.separator + "tokens";
			}
		}
		File file = new File(this.credentialsFilePath);
		boolean fileExists = true;
		if ( !file.exists() ) {
			this.isSessionAuthenticated = false;
			this.problem = "Credentials file does not exist: " + credentialsFilePath;
			this.problemRecommendation = "Verify that the credentials file exists for the session ID and authentication method.";
			fileExists = false;
		}
		else if ( !file.canRead() ) {
			// File exists and also check whether readable.
			this.isSessionAuthenticated = false;
			this.problem = "Credentials file is not readable: " + credentialsFilePath;
			this.problemRecommendation = "Verify that the credentials file has readable permissions.";
			fileExists = false;
		}
		
		// Create the credential.
		if ( fileExists ) {
			Message.printStatus(2, routine, "Google Drive credentials file exists: " + this.credentialsFilePath);
			try {
				createCredential ( httpTransport );
				this.isSessionAuthenticated = true;
			}
			/*
			catch ( GeneralSecurityException gse ) {
				this.areCredentialsOK = false;
				this.problem = "GeneralSecurityException: The credentials are not valid: " + this.credentialsFilePath;
				if ( this.authenticationMethod == GoogleDriveAuthenticationMethodType.OAUTH ) {
					this.problemRecommendation =
						"Verified that OAuth2 credentials are configured correctly for the Google Account API configuration.";
				}
				else if ( this.authenticationMethod == GoogleDriveAuthenticationMethodType.SERVICE_ACCOUNT_KEY ) {
					this.problemRecommendation =
						"Verified that service account credentials are configured correctly for the Google Account API configuration.";
				}
			}
			*/
			catch ( IOException ioe ) {
				this.isSessionAuthenticated = false;
				this.problem = "Error opening credentials file: " + this.credentialsFilePath;
				this.problemRecommendation = "Verify that the credentials file exists for the session ID and authentication method.";
			}
		}
		else {
			// Make sure that authentication is not indicated.
			this.isSessionAuthenticated = false;
		}
	}
	
	/**
	 * Creates an authorized Credential or GoogleCredentials object,
	 * depending on the authentication method.
	 *
	 * @param HTTP_TRANSPORT The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException If the credentials.json file cannot be found.
	 */
	private void createCredential ( final NetHttpTransport HTTP_TRANSPORT )
	    throws IOException {
		String routine = getClass().getSimpleName() + ".createCredential";
		// Load client secrets.
		// Make sure that the tokens folder exists:
		// - create if it does not
		File tokensFile = new File ( this.tokensFolderPath );
		if ( !tokensFile.exists() ) {
			Message.printStatus(2, routine, "Creating Google Drive tokens folder: " + this.tokensFolderPath );
			tokensFile.mkdir();
		}
		if ( this.authenticationMethod == GoogleDriveAuthenticationMethodType.OAUTH ) {
			// Open the stream to the OAuth2 credentials file.
			InputStream in = null;
			try {
				in = new FileInputStream ( this.credentialsFilePath );
			}
			catch ( FileNotFoundException e ) {
				this.isSessionAuthenticated = false;
				this.problem = "Credentials file does not exist: \"" + this.credentialsFilePath + "\"";
				this.problemRecommendation = "Create the file (" + this.credentialsFilePath +
					") based on Google Drive API console OAuth2 credentials.";
				return;
				//throw new FileNotFoundException ( "Could not open resource: " + this.credentialsFilePath );
			}
		
			GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

			// Build flow and trigger user authorization request.
			GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
				.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(this.tokensFolderPath)))
				.setAccessType("offline")
				.build();
			LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
			this.credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

			// Check whether the credential is OK:
			// - calling refreshToken seems to interactively prompt for the approval in a browser
			// - so, don't refresh the token each time
			if ( credential.getAccessToken() == null ) {
				Message.printWarning(2, routine, "The Google Drive access token is null." );
				this.isSessionAuthenticated = false;
				this.problem = "The Google Drive access token is null.";
				this.problemRecommendation = "Create the file (" + this.credentialsFilePath +
						") based on Google Drive API console OAuth2 credentials.";
			}
			else if ( (credential.getExpirationTimeMilliseconds() > System.currentTimeMillis()) ) {
		    	// The access token is valid.
				this.isSessionAuthenticated = true;
			}
			else {
		    	// The access token has expired or is not available.
				OffsetDateTime expiration = OffsetDateTime.ofInstant(
					Instant.ofEpochMilli(credential.getExpirationTimeMilliseconds()), ZoneId.systemDefault());
				Message.printStatus(2, routine, "The Google Drive access token expired on " + expiration +
					". Refreshshing (may prompt).");
				credential.refreshToken();
			}
		
		}
		else if ( this.authenticationMethod == GoogleDriveAuthenticationMethodType.SERVICE_ACCOUNT_KEY ) {
			// Open the stream to the service account key credentials file.
			InputStream in = null;
			try {
				in = new FileInputStream ( this.credentialsFilePath );
			}
			catch ( FileNotFoundException e ) {
				this.isSessionAuthenticated = false;
				this.problem = "Credentials file does not exist: \"" + this.credentialsFilePath + "\"";
				this.problemRecommendation = "Create the file (" + this.credentialsFilePath +
					") based on Google Drive API console service account key credentials.";
				return;
				//throw new FileNotFoundException ( "Could not open resource: " + this.credentialsFilePath );
			}

			boolean doOld = false;
			if ( doOld ) {
				credential = GoogleCredential.fromStream(in).createScoped(SCOPES);
				if ( credential.getAccessToken() == null ) {
					Message.printWarning(2, routine, "The Google Drive access token is null." );
					this.isSessionAuthenticated = false;
					this.problem = "The Google Drive access token is null.";
					this.problemRecommendation = "Create the file (" + this.credentialsFilePath +
							") based on Google Drive API console service account key credentials.";
				}
				else if ( (credential.getExpirationTimeMilliseconds() > System.currentTimeMillis()) ) {
		    		// The access token is valid.
					this.isSessionAuthenticated = true;
				}
				else {
		    		// The access token has expired or is not available.
					OffsetDateTime expiration = OffsetDateTime.ofInstant(
						Instant.ofEpochMilli(credential.getExpirationTimeMilliseconds()), ZoneId.systemDefault());
					Message.printWarning(2, routine, "The Google Drive access token expired on " + expiration);
				}
			}
			else {
				// Newer API code.
				// Not sure how much this will impact other code from the quick start example.
				try {
					this.credentials = ServiceAccountCredentials
						.fromStream(in)
						//.createScoped(ImmutableList.of(DriveScopes.DRIVE));
						.createScoped(SCOPES);
					if ( this.credentials != null ) {
						// Seems like the following is needed.
						this.credentials.refresh();
					}
				}
				catch ( Exception e ) {
					this.isSessionAuthenticated = false;
					this.problem = "Error creating Google Drive service account credentials.";
					this.problemRecommendation = "Create the file (" + this.credentialsFilePath +
							") based on Google Drive API console service account key credentials.";
					this.credentials = null;
					Message.printWarning(3, routine, "Exception creating credentails.");
					Message.printWarning(3, routine, e);
					// Can't continue checking below.
					return;
				}

				// Check whether the credential is OK.
				//credentials.refreshIfExpired();
				if ( this.credentials == null ) {
					Message.printWarning(2, routine, "The Google Drive credentials object is null." );
					this.isSessionAuthenticated = false;
					this.problem = "The Google Drive credentials object is null.";
					this.problemRecommendation = "Create the file (" + this.credentialsFilePath +
							") based on Google Drive API console service account key credentials.";
				}
				else if ( this.credentials.getAccessToken() == null ) {
					Message.printWarning(2, routine, "The Google Drive access credentials access token is null." );
					this.isSessionAuthenticated = false;
					this.problem = "The Google Drive access token is null.";
					this.problemRecommendation = "Create the file (" + this.credentialsFilePath +
							") based on Google Drive API console service account key credentials.";
				}
				else {
					// Credentials are OK.
					this.isSessionAuthenticated = true;
				}
			}
		}

		// Returns an authorized Credential object.
		//return credential;
	}

	/**
	 * Return the application name.
	 * @return the application name
	 */
	public String getApplicationName () {
		return this.APPLICATION_NAME;
	}

	/**
	 * Return the credential to use for Google Drive operations.
	 * @return the credential
	 */
	public Credential getCredential () {
		return this.credential;
	}

	/**
	 * Return the credentials to use for Google Drive operations.
	 * @return the credentials
	 */
	public GoogleCredentials getCredentials () {
		return this.credentials;
	}

	/**
	 * Return the path to the credentials file.
	 * @return the path to the credentials file
	 */
	public String getCredentialsFilePath () {
		return this.credentialsFilePath;
	}

	/**
	 * Return the HTTP transport object.
	 * @return the HTTP transport object
	 */
	public NetHttpTransport getHttpTransport () {
		return this.httpTransport;
	}

	/**
	 * Return the JSON factory.
	 * @return the JSON factory
	 */
	public JsonFactory getJsonFactory () {
		return this.JSON_FACTORY;
	}

	/**
	 * Return the problem if areCredentialsOk is false.
	 * @return the problem description 
	 */
	public String getProblem () {
		return this.problem;
	}

	/**
	 * Return the problem recommendation if areCredentialsOk is false.
	 * @return the problem recommendation 
	 */
	public String getProblemRecommendation () {
		return this.problemRecommendation;
	}

	/**
	 * Return a new Drive service based on session properties.
	 * @return the drive service, which can be modified before executing a request
	 */
	public Drive getService () {
   		Drive service = null;
   		if ( this.authenticationMethod == GoogleDriveAuthenticationMethodType.OAUTH ) {
   			service = new Drive.Builder (
   			getHttpTransport(),
   			getJsonFactory(),
       		getCredential())
   				.setApplicationName(getApplicationName())
   				.build();
   		}
   		else if ( this.authenticationMethod == GoogleDriveAuthenticationMethodType.SERVICE_ACCOUNT_KEY ) {
   			// The following is from:
   			//    https://developers.google.com/drive/api/guides/search-files
   			HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(getCredentials());
   			service = new Drive.Builder (
   				getHttpTransport(),
   				getJsonFactory(),
       			requestInitializer)
   				.setApplicationName(getApplicationName())
   				.build();
   		}
		return service;
	}

	/**
	 * Return whether the session is authenticated.
	 * @return true if the session is authenticated, false if not
	 */
	public boolean isSessionAuthenticated () {
		return this.isSessionAuthenticated;
	}

}