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
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.ServiceAccountSigner;
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
	 * Authentication method.
	 */
	private GoogleDriveAuthenticationMethodType authenticationMethod = null;
	
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
	 * @param authenticationMethod the authentication method to use
	 */
	public GoogleDriveSession ( GoogleDriveAuthenticationMethodType authenticationMethod )
	throws IOException, GeneralSecurityException {
		// Build a new authorized API client service.
		this.authenticationMethod = authenticationMethod;
	    this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		createCredential ( httpTransport );
	}
	
	/**
	 * Creates an authorized Credential or GoogleCredentials object.
	 *
	 * @param HTTP_TRANSPORT The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException If the credentials.json file cannot be found.
	 */
	private void createCredential ( final NetHttpTransport HTTP_TRANSPORT )
	    throws IOException {
		String routine = getClass().getSimpleName() + ".createCredential";
		// Load client secrets.
		// Original.
		//InputStream in = DriveQuickstart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
		// Get the credentials from the default location if it exists.
		String credentialsFilePath = null;
		String tokensFolderPath = null;
		if ( IOUtil.isUNIXMachine() ) {
			// Standard folder:
			// - HOME/.googledrive/credentials.json
			String HOME = System.getenv("HOME");
			if ( (HOME != null) && !HOME.isEmpty() ) {
				if ( this.authenticationMethod == GoogleDriveAuthenticationMethodType.OAUTH ) {
					credentialsFilePath = HOME + "/.googledrive/credentials.json";
				}
				else if ( this.authenticationMethod == GoogleDriveAuthenticationMethodType.SERVICE_ACCOUNT_KEY ) {
					credentialsFilePath = HOME + "/.googledrive/api-key.json";
				}
				tokensFolderPath = HOME + "/.googledrive/tokens";
			}
		}
		else {
			// Windows.
			String APP_DATA = System.getenv("LOCALAPPDATA");
			if ( (APP_DATA != null) && !APP_DATA.isEmpty() ) {
				if ( this.authenticationMethod == GoogleDriveAuthenticationMethodType.OAUTH ) {
					credentialsFilePath = APP_DATA + File.separator + "TSTool" + File.separator +
						"GoogleDrive" + File.separator + "credentials.json";
				}
				else {
					credentialsFilePath = APP_DATA + File.separator + "TSTool" + File.separator +
						"GoogleDrive" + File.separator + "api-key.json";
				}
				tokensFolderPath = APP_DATA + File.separator + "TSTool" + File.separator +
					"GoogleDrive" + File.separator + "tokens";
			}
		}
		// Make sure that the tokens folder exists.
		File tokensFile = new File ( tokensFolderPath );
		if ( !tokensFile.exists() ) {
			Message.printStatus(2, routine, "Creating Google Drive tokens folder: " + tokensFolderPath );
			tokensFile.mkdir();
		}
		File file = new File(credentialsFilePath);
		if ( !file.exists() ) {
			throw new FileNotFoundException ( "Credentials file does not exist: " + credentialsFilePath );
		}
		if ( !file.canRead() ) {
			throw new FileNotFoundException ( "Credentials file is not readable: " + credentialsFilePath );
		}
		// Original example apparently reads the credentials file from the Java path.
		//InputStream in = GoogleDriveSession.class.getResourceAsStream ( credentialsFilePath );
		InputStream in = null;
		try {
			in = new FileInputStream ( credentialsFilePath );
		}
		catch ( FileNotFoundException e ) {
			throw new FileNotFoundException ( "Could not open resource: " + credentialsFilePath );
		}
		//if ( in == null ) {
		//	throw new FileNotFoundException ( "Could not open resource: " + credentialsFilePath );
		//}
		
		if ( this.authenticationMethod == GoogleDriveAuthenticationMethodType.OAUTH ) {
			GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

			// Build flow and trigger user authorization request.
			GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
				.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokensFolderPath)))
				.setAccessType("offline")
				.build();
			LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
			this.credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

			// Check whether the credential is OK:
			// - calling refreshToken seems to interactively prompt for the approval in a browser
			//credential.refreshToken();
			if ( credential.getAccessToken() == null ) {
				Message.printWarning(2, routine, "The Google Drive access token is null." );
			}
			else if ( (credential.getExpirationTimeMilliseconds() > System.currentTimeMillis()) ) {
		    	// The access token is valid.
			}
			else {
		    	// The access token has expired or is not available.
				OffsetDateTime expiration = OffsetDateTime.ofInstant(
					Instant.ofEpochMilli(credential.getExpirationTimeMilliseconds()), ZoneId.systemDefault());
				Message.printWarning(2, routine, "The Google Drive access token expired on " + expiration);
			}
		
		}
		else if ( this.authenticationMethod == GoogleDriveAuthenticationMethodType.SERVICE_ACCOUNT_KEY ) {
			boolean doOld = false;
			if ( doOld ) {
				credential = GoogleCredential.fromStream(in).createScoped(SCOPES);
				if ( credential.getAccessToken() == null ) {
					Message.printWarning(2, routine, "The Google Drive access token is null." );
				}
				else if ( (credential.getExpirationTimeMilliseconds() > System.currentTimeMillis()) ) {
		    		// The access token is valid.
				}
				else {
		    		// The access token has expired or is not available.
					OffsetDateTime expiration = OffsetDateTime.ofInstant(
						Instant.ofEpochMilli(credential.getExpirationTimeMilliseconds()), ZoneId.systemDefault());
					Message.printWarning(2, routine, "The Google Drive access token expired on " + expiration);
				}
			}
			else {
				// Not sure how much this will impact other code from the quick start example.
				this.credentials = ServiceAccountCredentials
					.fromStream(in)
					.createScoped(ImmutableList.of(DriveScopes.DRIVE));

				// Check whether the credential is OK.
				credentials.refreshIfExpired();
				if ( credentials.getAccessToken() == null ) {
					Message.printWarning(2, routine, "The Google Drive access token is null." );
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
	 * Return a new Drive service based on session properties.
	 * @return the drive service, which can be modified before executing a request
	 */
	public Drive getService () {
   		Drive service = new Drive.Builder (
   			getHttpTransport(),
   			getJsonFactory(),
       		getCredential())
   				.setApplicationName(getApplicationName())
   				.build();
		return service;
	}

}