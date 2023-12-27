// GoogleDrive_Command - This class initializes, checks, and runs the GoogleDrive() command.

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

package org.openwaterfoundation.tstool.plugin.googledrive.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFrame;

import org.openwaterfoundation.tstool.plugin.googledrive.GoogleDriveAuthenticationMethodType;
import org.openwaterfoundation.tstool.plugin.googledrive.GoogleDriveSession;
import org.openwaterfoundation.tstool.plugin.googledrive.GoogleDriveToolkit;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.User;

//import org.openwaterfoundation.tstool.plugin.aws.AwsSession;
//import org.openwaterfoundation.tstool.plugin.aws.AwsToolkit;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;

import RTi.Util.String.StringUtil;

import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;

import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the GoogleDrive() command:
- see the Google Drive API: https://developers.google.com/drive/api/reference/rest/v3
*/
public class GoogleDrive_Command extends AbstractCommand
implements CommandDiscoverable, FileGenerator, ObjectListProvider
{
	/**
	Data members used for DeleteFolders parameter values.
	*/
	protected final String _AllFilesAndFolders = "AllFilesAndFolders";
	protected final String _FolderFiles = "FolderFiles";
	
	/**
	Data members used for ListObjectsScope parameter values.
	*/
	//protected final String _Root = "Root";
	protected final String _File = "File";
	protected final String _Folder = "Folder";
	protected final String _All = "All";

	/**
	Data members used for parameter values.
	*/
	protected final String _False = "False";
	protected final String _Only = "Only";
	protected final String _True = "True";

	/**
	 * Default delete folder minimum depth.
	 */
    protected final int _DeleteFoldersMinDepth = 3;

	/**
	 * Default maximum number of objects per request.
	 */
	protected final int _MaxKeys = 1000;

	/**
	 * Default maximum number of objects.
	 */
	protected final int _MaxObjects = 2000;

	/**
	Data members used for parameter values.
	*/
	protected final String _Ignore = "Ignore";
	protected final String _Warn = "Warn";
	protected final String _Fail = "Fail";

	/**
	Output file that is created by this command.
	*/
	private File __OutputFile_File = null;

	/**
	The output table that is created for discovery mode.
	*/
	private DataTable discoveryOutputTable = null;

	/**
	Constructor.
	*/
	public GoogleDrive_Command () {
		super();
		setCommandName ( "GoogleDrive" );
	}
	
	/**
	Check the command parameter for valid values, combination, etc.
	@param parameters The parameters for the command.
	@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
	@param warning_level The warning level to use when printing parse warnings
	(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
	*/
	public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
	throws InvalidCommandParameterException {
		String SessionID = parameters.getValue ( "SessionID" );
		String AuthenticationMethod = parameters.getValue ( "AuthenticationMethod" );
		String GoogleDriveCommand = parameters.getValue ( "GoogleDriveCommand" );
    	String CopyFiles = parameters.getValue ( "CopyFiles" );
    	String DeleteFiles = parameters.getValue ( "DeleteFiles" );
    	String DeleteFolders = parameters.getValue ( "DeleteFolders" );
    	//String DeleteFoldersScope = parameters.getValue ( "DeleteFoldersScope" );
    	//String DeleteFoldersMinDepth = parameters.getValue ( "DeleteFoldersMinDepth" );
    	String DownloadFiles = parameters.getValue ( "DownloadFiles" );
    	String DownloadFolders = parameters.getValue ( "DownloadFolders" );
    	String ListObjectsScope = parameters.getValue ( "ListScope" );
    	String ListFolderPath = parameters.getValue ( "ListFolderPath" );
    	String ListFiles = parameters.getValue ( "ListFiles" );
    	String ListFolders = parameters.getValue ( "ListFolders" );
    	String ListShared = parameters.getValue ( "ListShared" );
    	String ListTrashed = parameters.getValue ( "ListTrashed" );
    	String ListMax = parameters.getValue ( "ListMax" );
    	String UploadFiles = parameters.getValue ( "UploadFiles" );
    	String UploadFolders = parameters.getValue ( "UploadFolders" );
    	// Output
    	String OutputTableID = parameters.getValue ( "OutputTableID" );
    	String OutputFile = parameters.getValue ( "OutputFile" );
    	String AppendOutput = parameters.getValue ( "AppendOutput" );
		String IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
		String warning = "";
		String message;

		CommandStatus status = getCommandStatus();
		status.clearLog(CommandPhaseType.INITIALIZATION);

		if ( (SessionID == null) || SessionID.isEmpty() ) {
			message = "The session ID must be specified to locate credentials.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify a session ID ."));
		}

		GoogleDriveAuthenticationMethodType authenticationMethod = null;
		if ( (AuthenticationMethod != null) && !AuthenticationMethod.isEmpty() ) {
			authenticationMethod = GoogleDriveAuthenticationMethodType.valueOfIgnoreCase(AuthenticationMethod);
			if ( authenticationMethod == null ) {
				message = "The Google Drive authentication method (" + AuthenticationMethod + ") is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify a valid authentication method."));
			}
		}
		
		// Try to open a session to see if the credentials file is found.
		/*
		GoogleDriveSession googleDriveSession = null;
		try {
			googleDriveSession = new GoogleDriveSession ( SessionID, authenticationMethod );
		}
		catch ( FileNotFoundException fnfe ) {
			if ( googleDriveSession != null ) {
				message = "The credentials file was not found: " + googleDriveSession.getCredentialsFilePath() ;
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the a valid session ID and authentication method."));
			}
		}
		catch ( GeneralSecurityException gse ) {
			if ( googleDriveSession != null ) {
				message = "GeneralSecurityException: The credentials are not valid: " + googleDriveSession.getCredentialsFilePath() ;
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the a valid session ID and authentication method."));
			}
		}
		catch ( IOException ioe ) {
			if ( googleDriveSession != null ) {
				message = "Error reading credentials file: " + googleDriveSession.getCredentialsFilePath() ;
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the a valid session ID and authentication method."));
			}
		}
		*/

		// The existence of the file to append is not checked during initialization
		// because files may be created dynamically at runtime.

		GoogleDriveCommandType googleDriveCommand = null;
		if ( (GoogleDriveCommand == null) || GoogleDriveCommand.isEmpty() ) {
			message = "The Google Drive command must be specified.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the Google Drive command."));
		}
		else {
			googleDriveCommand = GoogleDriveCommandType.valueOfIgnoreCase(GoogleDriveCommand);
			if ( googleDriveCommand == null ) {
				message = "The Google Drive command (" + GoogleDriveCommand + ") is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify a valid Google Drive command."));
			}
		}

		// OK to use /.
		if ( (ListFolderPath != null) && !ListFolderPath.isEmpty() && !ListFolderPath.endsWith("/") ) {
			message = "The list folder path must end with /.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the list folder as a path ending in / or can leave blank if listing all files."));
		}

		/*
		if ( (DeleteFoldersScope != null) && !DeleteFoldersScope.equals("") ) {
			if ( !DeleteFoldersScope.equalsIgnoreCase(_AllFilesAndFolders) && !DeleteFoldersScope.equalsIgnoreCase(_FolderFiles) ) {
				message = "The DeleteFoldersScope parameter \"" + DeleteFoldersScope + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _AllFilesAndFolders + " or " + _FolderFiles + " (default)."));
			}
		}

		if ( (DeleteFoldersMinDepth != null) && !DeleteFoldersMinDepth.equals("") && !StringUtil.isInteger(DeleteFoldersMinDepth)) {
			message = "The DeleteFoldersMinDepth parameter (" + DeleteFoldersMinDepth + ") is not an integer.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as an integer."));
		}
		*/

		if ( (ListFiles != null) && !ListFiles.equals("") ) {
			if ( !ListFiles.equalsIgnoreCase(_False) && !ListFiles.equalsIgnoreCase(_True) ) {
				message = "The ListFiles parameter \"" + ListFiles + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _False + " or " + _True + " (default)."));
			}
		}

		if ( (ListFolders != null) && !ListFolders.equals("") ) {
			if ( !ListFolders.equalsIgnoreCase(_False) && !ListFolders.equalsIgnoreCase(_True) ) {
				message = "The ListFolders parameter \"" + ListFolders + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _False + " or " + _True + " (default)."));
			}
		}

		if ( (ListShared != null) && !ListShared.equals("") ) {
			if ( !ListShared.equalsIgnoreCase(_False) && !ListShared.equalsIgnoreCase(_True) &&
				!ListShared.equalsIgnoreCase(_Only) ) {
				message = "The ListShared parameter \"" + ListShared + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _False + " (default), " + _Only + ", or " + _True + "."));
			}
		}

		if ( (ListTrashed != null) && !ListTrashed.equals("") ) {
			if ( !ListTrashed.equalsIgnoreCase(_False) && !ListTrashed.equalsIgnoreCase(_True) ) {
				message = "The ListTrashed parameter \"" + ListTrashed + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _False + " (default) or " + _True + "."));
			}
		}

		if ( (ListMax != null) && !ListMax.isEmpty() && !StringUtil.isInteger(ListMax) ) {
			message = "The list maximum (" + ListMax + ") is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify an integer."));
		}

		if ( (AppendOutput != null) && !AppendOutput.equals("") ) {
			if ( !AppendOutput.equalsIgnoreCase(_False) && !AppendOutput.equalsIgnoreCase(_True) ) {
				message = "The AppendOutput parameter \"" + AppendOutput + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _False + " (default), or " + _True + "."));
			}
		}

		// Put ListObjectsScope at the end so combinations of parameters can be checked.
		/*
		if ( (ListObjectsScope != null) && !ListObjectsScope.equals("") ) {
			if ( !ListObjectsScope.equalsIgnoreCase(_All) && !ListObjectsScope.equalsIgnoreCase(_Folder) && !ListObjectsScope.equalsIgnoreCase(_Root)) {
				message = "The ListObjectsScope parameter \"" + ListObjectsScope + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _All + " (default), " + _Folder + ", or " + _Root + "."));
			}
			// Cannot be specified with Prefix.
			/ *
			if ( ListObjectsScope.equalsIgnoreCase(_True) && (Prefix != null) && !Prefix.isEmpty() ) {
				message = "ListObjectsScope=True cannot be specified with a Prefix value.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Change to not list root only or remove the prefix."));
			}
			* /
		}

		if ( (IfInputNotFound != null) && !IfInputNotFound.equals("") ) {
			if ( !IfInputNotFound.equalsIgnoreCase(_Ignore) && !IfInputNotFound.equalsIgnoreCase(_Warn)
		    	&& !IfInputNotFound.equalsIgnoreCase(_Fail) ) {
				message = "The IfInputNotFound parameter \"" + IfInputNotFound + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _Ignore + ", " + _Warn + " (default), or " +
						_Fail + "."));
			}
		}

		// Additional checks specific to a command.

		if ( s3Command != AwsS3CommandType.LIST_BUCKETS ) {
			// All commands except listing buckets needs the bucket.
			if ( (Bucket == null) || Bucket.isEmpty() ) {
				message = "The bucket must be specified.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the bucket."));
			}
		}

		if ( s3Command == AwsS3CommandType.COPY_OBJECTS ) {
			if ( (CopyFiles == null) || CopyFiles.isEmpty() ) {
				message = "The copy files list must be specified.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the copy files list."));
			}
			// Make sure extra commands are not provided to avoid confusion with file and folder lists.
			if ( (DeleteFiles != null) && !DeleteFiles.isEmpty() ) {
				message = "The DeleteFiles parameter is not used when copying objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the DeleteFiles parameter."));
			}
			if ( (DeleteFolders != null) && !DeleteFolders.isEmpty() ) {
				message = "The DeleteFolders parameter is not used when copying objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the DeleteFolders parameter."));
			}
			if ( (DownloadFiles != null) && !DownloadFiles.isEmpty() ) {
				message = "The DownloadFiles parameter is not used when copying objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the DownloadFiles parameter."));
			}
			if ( (DownloadFolders != null) && !DownloadFolders.isEmpty() ) {
				message = "The DownloadFolders parameter is not used when copying objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the DownloadFolders parameter."));
			}
			if ( (ListFiles != null) && !ListFiles.isEmpty() ) {
				message = "The ListFiles parameter is not used when copying objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the ListFiles parameter."));
			}
			if ( (ListFolders != null) && !ListFolders.isEmpty() ) {
				message = "The ListFolders parameter is not used when copying objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the ListFolders parameter."));
			}
			if ( (UploadFiles != null) && !UploadFiles.isEmpty() ) {
				message = "The UploadFiles parameter is not used when copying objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the UploadFiles parameter."));
			}
			if ( (UploadFolders != null) && !UploadFolders.isEmpty() ) {
				message = "The UploadFolders parameter is not used when copying objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the UploadFolders parameter."));
			}
		}
		else if ( s3Command == AwsS3CommandType.DELETE_OBJECTS ) {
			if ( ((DeleteFiles == null) || DeleteFiles.isEmpty()) && ((DeleteFolders == null) || DeleteFolders.isEmpty()) ) {
				message = "The files or folders must be specified for the delete.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify 1+ file and/or folder keys to delete."));
			}
			// Make sure extra commands are not provided to avoid confusion with file and folder lists.
			if ( (CopyFiles != null) && !CopyFiles.isEmpty() ) {
				message = "The CopyFiles parameter is not used when deleting objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the CopyFiles parameter."));
			}
			if ( (DownloadFiles != null) && !DownloadFiles.isEmpty() ) {
				message = "The DownloadFiles parameter is not used when deleting objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the DownloadFiles parameter."));
			}
			if ( (DownloadFolders != null) && !DownloadFolders.isEmpty() ) {
				message = "The DownloadFolders parameter is not used when deleting objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the DownloadFolders parameter."));
			}
			if ( (ListFiles != null) && !ListFiles.isEmpty() ) {
				message = "The ListFiles parameter is not used when deleting objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the ListFiles parameter."));
			}
			if ( (ListFolders != null) && !ListFolders.isEmpty() ) {
				message = "The ListFolders parameter is not used when deleting objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the ListFolders parameter."));
			}
			if ( (UploadFiles != null) && !UploadFiles.isEmpty() ) {
				message = "The UploadFiles parameter is not used when deleting objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the UploadFiles parameter."));
			}
			if ( (UploadFolders != null) && !UploadFolders.isEmpty() ) {
				message = "The UploadFolders parameter is not used when deleting objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the UploadFolders parameter."));
			}
		}
		*/
		else if ( googleDriveCommand == GoogleDriveCommandType.DOWNLOAD ) {
			// Make sure extra commands are not provided to avoid confusion with file and folder lists.
			if ( (CopyFiles != null) && !CopyFiles.isEmpty() ) {
				message = "The CopyFiles parameter is not used when downloading objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Remove the CopyFiles parameter."));
			}
			if ( (DeleteFiles != null) && !DeleteFiles.isEmpty() ) {
				message = "The DeleteFiles parameter is not used when downloading objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the DeleteFiles parameter."));
			}
			if ( (DeleteFolders != null) && !DeleteFolders.isEmpty() ) {
				message = "The DeleteFolders parameter is not used when downloading objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the DeleteFolders parameter."));
			}
			if ( (ListFiles != null) && !ListFiles.isEmpty() ) {
				message = "The ListFiles parameter is not used when downloading objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the ListFiles parameter."));
			}
			if ( (ListFolders != null) && !ListFolders.isEmpty() ) {
				message = "The ListFolders parameter is not used when downloading objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the ListFolders parameter."));
			}
			if ( (ListShared != null) && !ListShared.isEmpty() ) {
				message = "The ListShared parameter is not used when downloading objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the ListShared parameter."));
			}
			if ( (ListTrashed != null) && !ListTrashed.isEmpty() ) {
				message = "The ListTrashed parameter is not used when downloading objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the ListTrashed parameter."));
			}
			if ( (UploadFiles != null) && !UploadFiles.isEmpty() ) {
				message = "The UploadFiles parameter is not used when downloading objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the UploadFiles parameter."));
			}
			if ( (UploadFolders != null) && !UploadFolders.isEmpty() ) {
				message = "The UploadFolders parameter is not used when downloading objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the UploadFolders parameter."));
			}
		}
		else if ( googleDriveCommand == GoogleDriveCommandType.LIST ) {
			// Make sure extra commands are not provided to avoid confusion with file and folder lists.
			if ( (CopyFiles != null) && !CopyFiles.isEmpty() ) {
				message = "The CopyFiles parameter is not used when listing files.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Remove the CopyFiles parameter."));
			}
			if ( (DeleteFiles != null) && !DeleteFiles.isEmpty() ) {
				message = "The DeleteFiles parameter is not used when listing files.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the DeleteFiles parameter."));
			}
			if ( (DeleteFolders != null) && !DeleteFolders.isEmpty() ) {
				message = "The DeleteFolders parameter is not used when listing files.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the DeleteFolders parameter."));
			}
			if ( (DownloadFiles != null) && !DownloadFiles.isEmpty() ) {
				message = "The DownloadFiles parameter is not used when listing files.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the DownloadFiles parameter."));
			}
			if ( (DownloadFolders != null) && !DownloadFolders.isEmpty() ) {
				message = "The DownloadFolders parameter is not used when listing files.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the DownloadFolders parameter."));
			}
			if ( (UploadFiles != null) && !UploadFiles.isEmpty() ) {
				message = "The UploadFiles parameter is not used when listing files.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the UploadFiles parameter."));
			}
			if ( (UploadFolders != null) && !UploadFolders.isEmpty() ) {
				message = "The UploadFolders parameter is not used when listing files.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the UploadFolders parameter."));
			}
		}
		/*
		else if ( s3Command == AwsS3CommandType.UPLOAD_OBJECTS ) {
			// Make sure extra commands are not provided to avoid confusion with file and folder lists.
			if ( (CopyFiles != null) && !CopyFiles.isEmpty() ) {
				message = "The CopyFiles parameter is not used when uploading objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the CopyFiles parameter."));
			}
			if ( (DeleteFiles != null) && !DeleteFiles.isEmpty() ) {
				message = "The DeleteFiles parameter is not used when uploading objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the DeleteFiles parameter."));
			}
			if ( (DeleteFolders != null) && !DeleteFolders.isEmpty() ) {
				message = "The DeleteFolders parameter is not used when uploading objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the DeleteFolders parameter."));
			}
			if ( (DownloadFiles != null) && !DownloadFiles.isEmpty() ) {
				message = "The DownloadFiles parameter is not used when uploading objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the DownloadFiles parameter."));
			}
			if ( (DownloadFolders != null) && !DownloadFolders.isEmpty() ) {
				message = "The DownloadFolders parameter is not used when uploading objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the DownloadFolders parameter."));
			}
			if ( (ListFiles != null) && !ListFiles.isEmpty() ) {
				message = "The ListFiles parameter is not used when uploading objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the ListFiles parameter."));
			}
			if ( (ListFolders != null) && !ListFolders.isEmpty() ) {
				message = "The ListFolders parameter is not used when uploading objects.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.WARNING,
						message, "Remove the ListFolders parameter."));
			}
		}
		*/

		// The output table or file is needed for lists:
		// - some internal logic such as counts uses the table
		if ( //(googleDriveCommand == GoogleDriveCommandType.LIST_BUCKETS) ||
			(googleDriveCommand == GoogleDriveCommandType.LIST) ) {
			// Must specify table and/or file.
			if ( ((OutputTableID == null) || OutputTableID.isEmpty()) && ((OutputFile == null) || OutputFile.isEmpty()) ) {
				message = "The output table and/or file must be specified.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the output table ID and or file name."));
			}
		}
		Message.printStatus(2, "", "OutputTableID=\"" + OutputTableID + "\" OutputFile=\"" + OutputFile + "\"");

		// Check for invalid parameters.
		List<String> validList = new ArrayList<>(29);
		// General.
		validList.add ( "SessionID" );
		validList.add ( "AuthenticationMethod" );
		validList.add ( "GoogleDriveCommand" );
		// Copy.
		validList.add ( "CopyFiles" );
		validList.add ( "CopyBucket" );
		validList.add ( "CopyObjectsCountProperty" );
		// Delete.
		validList.add ( "DeleteFiles" );
		validList.add ( "DeleteFolders" );
		validList.add ( "DeleteFoldersScope" );
		validList.add ( "DeleteFoldersMinDepth" );
		// Download.
		validList.add ( "DownloadFolders" );
		validList.add ( "DownloadFiles" );
		// List buckets.
		validList.add ( "ListBucketsRegEx" );
		validList.add ( "ListBucketsCountProperty" );
		// List bucket objects.
		validList.add ( "ListScope" );
		validList.add ( "ListFolderPath" );
		validList.add ( "ListRegEx" );
		validList.add ( "ListFiles" );
		validList.add ( "ListFolders" );
		validList.add ( "ListShared" );
		validList.add ( "ListTrashed" );
		validList.add ( "MaxKeys" );
		validList.add ( "MaxObjects" );
		validList.add ( "ListCountProperty" );
		// Upload.
		validList.add ( "UploadFolders" );
		validList.add ( "UploadFiles" );
		// Output
		validList.add ( "OutputTableID" );
		validList.add ( "OutputFile" );
		validList.add ( "AppendOutput" );
		//
		validList.add ( "IfInputNotFound" );
		warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

		if ( warning.length() > 0 ) {
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag,warning_level),warning );
			throw new InvalidCommandParameterException ( warning );
		}
		status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
	}

	/**
	 * Download Google Drive files and folders.
	 */
	private int doGoogleDriveDownload (
		CommandProcessor processor,
		GoogleDriveSession googleDriveSession,
		List<String> downloadFilesGoogleDrivePaths, List<String> downloadFilesFiles,
		List<String> downloadFoldersGoogleDrivePaths, List<String> downloadFoldersFolders,
		CommandStatus status, int logLevel, int warningLevel, int warningCount, String commandTag
		) {
		String routine = getClass().getSimpleName() + ".doGoogleDriveDownload";
		String message;

    	if ( (downloadFilesFiles.size() == 0) && (downloadFoldersFolders.size() == 0) ) {
    		// Nothing to process.
    		if ( Message.isDebugOn ) {
    			Message.printStatus(2, routine, "No files are folders are to be downloaded.  Skipped because of checks?" );
    		}
    		return warningCount;
    	}

    	// Process folders first so that files can be downloaded into folders below.

    	/* TODO smalers 2023-12-18 enable folder download later.
      	if ( downloadFoldersFolders.size() > 0 ) {
    		// Process each folder in the list.
    		boolean error = false;
    		int iFolder = -1;
    		for ( String downloadKey : downloadFoldersKeys ) {
    			++iFolder;
    			error = false;
    			String localFolder = null;
    			try {
    				downloadKey = downloadKey.trim();
    				localFolder = downloadFoldersFolders.get(iFolder);
    				// TODO smalers 2023-02-07 remove the error check since no longer used:
    				// - most of the checks are done in the main runCommandInternal() method
    				if ( !error ) {
    					// TODO smalers 2023-02-07 path was determined in earlier code.
    					//localFolder = localFolder.trim();
    					// There is not a way to examine the filenames so can't apply a regular expression.
    					//String localFolderFull = IOUtil.verifyPathForOS(
						//IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
						//TSCommandProcessorUtil.expandParameterValue(processor,this,localFolder)));
    					if ( Message.isDebugOn ) {
    						Message.printStatus(2, routine, "Attemping to download S3 folder \"" + downloadKey
    							+ "\" to local folder \"" + localFolder + "\".");
    					}
    					DirectoryDownload download = tm.downloadDirectory(
    						DownloadDirectoryRequest.builder()
								.destinationDirectory(Paths.get(localFolder))
								.bucket(bucket)
								.prefix(downloadKey)
								.build()
						);
    					// Wait for the transfer to complete.
    					CompletedDirectoryDownload completed = download.completionFuture().join();
    					// Log failed downloads, up to 50 messages.
    					int maxMessage = 50;
    					int failCount = 0;
    					for ( FailedFileDownload fail : completed.failedTransfers() ) {
    						++failCount;
    						if ( failCount > maxMessage ) {
    							// Limit messages.
    							message = "Only listing " + maxMessage + " download errors.";
   								Message.printWarning ( warningLevel,
   									MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
   								status.addToLog(CommandPhaseType.RUN,
   									new CommandLogRecord(CommandStatusType.FAILURE,
   										message, "Check command parameters."));
    							break;
    						}
   							message = "Error downloading folder \"" + downloadKey + "\" to folder \"" + localFolder + "\"(" + fail + ").";
   							Message.printWarning ( warningLevel,
   								MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
   							status.addToLog(CommandPhaseType.RUN,
   								new CommandLogRecord(CommandStatusType.FAILURE,
   									message, "Check command parameters."));
    					}
    				}
    			}
    			catch ( Exception e ) {
    				message = "Error downloading S3 folder \"" + downloadKey + "\" to local folder \"" + localFolder + "\" (" + e + ")";
    				Message.printWarning ( warningLevel,
    					MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
    				Message.printWarning ( 3, routine, e );
    				status.addToLog(CommandPhaseType.RUN,
    					new CommandLogRecord(CommandStatusType.FAILURE,
    						message, "See the log file for details."));
    			}
    		}
    	}
    	*/

      	// Download files individually.
      	
      	int downloadCount = 0;
    	if ( downloadFilesFiles.size() > 0 ) {
    		// Process each file in the list.
    		boolean error = false;
    		int iFile = -1;
    		for ( String downloadGoogleDrivePath : downloadFilesGoogleDrivePaths ) {
    			++iFile;
    			error = false;
    			String localFile = null;
    			try {
    				downloadGoogleDrivePath = downloadGoogleDrivePath.trim();
    				localFile = downloadFilesFiles.get(iFile).trim();
    				// Apparently the folder for the file must exist so create if necessary.
    				File file = new File(localFile);
    				// Get the parent.
	    			File folder = file.getParentFile();
	    			// Create the parent.
	    			if ( !folder.exists() ) {
	    				if ( Message.isDebugOn ) {
	    					Message.printStatus(2, routine, "Creating folder: " + folder.getAbsolutePath() );
	    				}
	    				folder.mkdirs();
	    			}
    				
    				if ( !error ) {
    					// Convert the requested file path to a file ID:
    					String downloadFilePath = downloadFilesGoogleDrivePaths.get(iFile);
    					String downloadFileId = GoogleDriveToolkit.getInstance().getFileIdForPath(
    						googleDriveSession.getService(), downloadFilePath);
    					if ( downloadFileId == null ) {
							message = "Error getting Google Drive ID for file path \"" + downloadFilePath + "\".";
	 						Message.printWarning ( warningLevel,
	 							MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
	 						status.addToLog(CommandPhaseType.RUN,
	 							new CommandLogRecord(CommandStatusType.FAILURE,
	 								message, "See the log file for details."));
	 						throw new CommandException ( message );
    					}
    					else {
							message = "Downloading Google Drive ID \"" + downloadFileId +
								"\" to local file path \"" + downloadFilePath + "\".";
    						try {
    							OutputStream outputStream = new FileOutputStream(localFile);
   								googleDriveSession.getService()
   									// Request to execute.
   									.files()
   									// Holds the parameters for the request.
   									.get(downloadFileId)
       								// Invoke the remote operation.
       								.executeMediaAndDownloadTo(outputStream);

   								// Successful if no exception.
   								++downloadCount;
   							}
   							catch ( Exception e ) {
   								message = "Error downloading Google Drive file \"" + downloadFilePath + "\".";
		 						Message.printWarning ( warningLevel,
		 							MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
		 						Message.printWarning ( 3, routine, e );
		 						status.addToLog(CommandPhaseType.RUN,
		 							new CommandLogRecord(CommandStatusType.FAILURE,
		 								message, "See the log file for details."));
		 						throw new CommandException ( message );
   							}
    					}
    	    		}
    	    	}
    	    	catch ( Exception e ) {
    	    		message = "Error downloading Google Drive file \"" + downloadGoogleDrivePath + "\" to file \"" + localFile + "\" (" + e + ")";
    	    		Message.printWarning ( warningLevel,
    	    			MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
    	    		Message.printWarning ( 3, routine, e );
    	    		status.addToLog(CommandPhaseType.RUN,
    	    			new CommandLogRecord(CommandStatusType.FAILURE,
    	    				message, "See the log file for details."));
    	    	}
    		}
      	}

    	// Return the updated warning count.
    	return warningCount;
	}

	/**
	 * Execute Google Drive "list":
	 * - see https://developers.google.com/drive/api/reference/rest/v3/files/list
	 */
	private int doGoogleDriveList (
		CommandProcessor processor,
		GoogleDriveSession googleDriveSession,
		String listScope, String listFolderPath, boolean listFiles, boolean listFolders, boolean listShared, boolean listTrashed,
		String regex,
		DataTable table, int listCreationTimeCol, int listIdCol, int listLastModifiedTimeCol, int listLastModifiedUserCol,
		int listMax,
		int listNameCol, int listOriginalFilenameCol, int listOwnedByMeCol, int listOwnerCol,
		int listParentFolderCol, int listParentFolderIdCol,
		int listSharedCol, int listSharedWithMeTimeCol, int listSharingUserCol,
   		int listSizeCol,
		int listTrashedCol, int listTrashedTimeCol, int listTrashedUserCol,
   		int listTypeCol, int listWebViewLinkCol,
		String listCountProperty,
		CommandStatus status, int logLevel, int warningLevel, int warningCount, String commandTag
		)
		throws CommandException, Exception {
		String routine = getClass().getSimpleName() + ".doGoogleDriveList";
		String message;
		
   		// Folder to list:
   		// - convert the G: drive path to a folder ID
   		// - do not include "My Drive" in the path
		
		StringBuilder q = new StringBuilder();
		if ( listFolderPath.equals("/") ) {
			// List the root folder.
			if ( listShared ) {
				// Shared files have to be listed independent of parent folder.
				q = new StringBuilder("sharedWithMe=" + listShared );
			}
			else {
				q = new StringBuilder("'root' in parents and trashed=" + listTrashed );
			}
		}
		else {
			// List a specific folder.
			String folderPath = listFolderPath;
			String folderId = null;
			try {
				folderId = GoogleDriveToolkit.getInstance().getFolderIdForPath ( googleDriveSession.getService(), folderPath );
			}
			catch ( Exception e ) {
				message = "Error converting folder \"" + folderPath + " to Google Drive ID.";
				Message.printWarning ( warningLevel,
					MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
				Message.printWarning ( 3, routine, e );
				status.addToLog(CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "See the log file for details."));
				throw new CommandException ( message );
			}
			if ( folderId == null ) {
				message = "Unable to convert folder \"" + folderPath + " to Google Drive ID - does not exist?";
				Message.printWarning ( warningLevel,
					MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
				status.addToLog(CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "See the log file for details."));
				throw new CommandException ( message );
			}
			// List files in a folder:
			// - 'folder' is the folder to list and 'in parents' means to list the files in that folder
			// - default is to list trashed files and folders so always specify how handled
			// - trashed=true will list ONLY trashed files and folders, false will list only NOT trashed files and folders
			// - trashed objects are listed in the Google Drive recycling bin
			if ( listShared ) {
				// Shared files have to be listed independent of parent folder.
				q = new StringBuilder("sharedWithMe=" + listShared);
			}
			else {
				q = new StringBuilder("'" + folderId + "' in parents and trashed=" + listTrashed );
			}
		}

   		// Print the names and IDs for up to 10 files.
   		FileList result = null;
   		Drive.Files.List request = null;
   		try {
   			// Do the initial request.
   			request = googleDriveSession.getService()
   				// Request to execute.
   				.files()
   				// Holds the parameters for the request.
   				.list()
   				// The "space" is the location where files are stored:
   				// - "drive" - user's personal Google Drive
   				// - "appDataFolder" - hidden folder in the user's Google Drive that is only
   				//   accessible by the application that created it
   				// - "photos" - for Google Photos
   				// - can specify both separated by a comma to list all files
   				.setSpaces("drive")
   				// Set the folder to list:
   				// See: https://developers.google.com/drive/api/guides/search-files
   				.setQ(q.toString())
   				// Page size for returned files:
   				// - currently use the maximum of 1000
   				// - TODO smalers 2023-12-27 need to implement paging
       			//.setPageSize(1000)
       			// Which fields to include in the response:
       			// - if not specified nulls will be returned by the "get" methods below
       			// - see https://developers.google.com/drive/api/guides/ref-search-terms#drive_properties
       			// - don't specify any fields to return all fields (will be slower) but it is a pain to figure out fields
       			//   since they don't seem to be documented well
       			// - specifying * returns everything and may be slower
       			// - when using files(), need to do a better job including only file fields in the parentheses
       			//.setFields("nextPageToken, files(createdTime, description, id, mimeType, modifiedTime, name, ownedByMe, owners, parents, permissions, shared, sharingUser, size, trashed, trashedTime)")
       			//.setFields("nextPageToken, createdTime, description, id, mimeType, modifiedTime, name, ownedByMe, owners, parents, permissions, shared, sharingUser, size, trashed, trashedTime")
       			.setFields("*");
			// Invoke the remote operation.
   			result = request.execute();
   		}
   		catch ( Exception e ) {
   			message = "Error listing Google Drive files.";
		 	Message.printWarning ( warningLevel,
		 		MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
		 	Message.printWarning ( 3, routine, e );
		 	status.addToLog(CommandPhaseType.RUN,
		 		new CommandLogRecord(CommandStatusType.FAILURE,
		 			message, "See the log file for details."));
		 	throw new CommandException ( message );
   		}
   		
   		// Process the result files and if necessary make additional requests.
		int fileCount = 0;
		int folderCount = 0;
		int objectCount = 0;
		int pageCount = 0;
   		while ( (result.getFiles() != null) && (result.getFiles().size() > 0) ) {
   			++pageCount;
   			List<com.google.api.services.drive.model.File> files = result.getFiles();
   			if ( (files == null) || files.isEmpty() ) {
   				Message.printStatus(2, routine, "No more files found (page=" + pageCount + ").");
   			}
   			else {
   				// Have files and folders to process.
   				Message.printStatus(2, routine, "Processing " + files.size() + " files (page=" + pageCount + ").");
   				// Do not allow duplicates in the output.
   				boolean allowDuplicates = false;
   				TableRecord rec = null;
   				for ( com.google.api.services.drive.model.File file : files ) {
   					//Message.printStatus(2, routine, String.format("%s (%s)\n", file.getName(), file.getId()));
   					// Output to table:
   					// - key is the full path to the file
   					// - have size, owner and modification time properties
					if ( regex != null ) {
						// Want to apply a regular expression to the name.
						if ( !file.getName().matches(regex) ) {
							if ( Message.isDebugOn ) {
								Message.printStatus(2, routine, "Does not match regular expression - skipping: " + file.getName());
							}
							continue;
						}
					}
					String type = null;
					if ( file.getMimeType().equals("application/vnd.google-apps.folder") ) {
						if ( !listFolders ) {
							// Is a folder and don't want folders so continue.
							if ( Message.isDebugOn ) {
								Message.printStatus(2, routine, "Is a folder and ignoring folder - skipping: " + file.getName());
							}
							continue;
						}
						else {
							// Will include the folder in the listing below.
							type = "folder";
							++folderCount;
							++objectCount;
						}
					}
					else if ( !file.getMimeType().equals("application/vnd.google-apps.folder") ) {
						if ( !listFiles ) {
							// Is a file and don't files want so continue.
							if ( Message.isDebugOn ) {
								Message.printStatus(2, routine, "Is a file and ignoring files - skipping: " + file.getName());
							}
							continue;
						}
						else {
							// Will include the file in the listing below.
							type = "file";
							++fileCount;
							++objectCount;
						}
					}
					// If here, the object should be listed in the output table.
 					if ( table != null ) {
   						rec = null;
   						if ( !allowDuplicates ) {
   							// Try to match the object key, which is the unique identifier.
   							rec = table.getRecord ( listIdCol, file.getId() );
   						}
   						if ( rec == null ) {
   							// Create a new record.
   							rec = table.addRecord(table.emptyRecord());
   						}
   						// Set the data in the record:
   						// - list in the order of the table
   						rec.setFieldValue(listIdCol,file.getId());
						rec.setFieldValue(listNameCol,file.getName());
						String parentPath = "";
						String parentId = "";
						String parentIdPrev = ""; // Use to increase performance.
						List<String> parents = file.getParents();
						if ( (parents != null) && (parents.size() > 0) ) {
							// Show the parent folder:
							// - the primary parent is the first one and others are like symbolic links
							// - the parents are the ID, not a nice name
							parentId = parents.get(0);
							if ( !parentId.equals(parentIdPrev) ) {
								// Also convert the parent ID to a path:
								// - only need to do this if the parent ID has changed
								// - most of the time a single folder is being listed
								Drive service = googleDriveSession.getService();
								parentPath = GoogleDriveToolkit.getInstance().getParentFolderPathFromFolderId(service, parentId);
							}
							parentIdPrev = parentId;
						}
						rec.setFieldValue(listParentFolderCol,parentPath.toString());
						rec.setFieldValue(listParentFolderIdCol,parentId);
   						rec.setFieldValue(listTypeCol,type);
   						rec.setFieldValue(listSizeCol,file.getSize());
   						List<User> users = file.getOwners();
   						// Can have multiple owners.
   						StringBuilder userCsv = new StringBuilder();
   						if ( users != null ) {
   							for ( User user : users ) {
   								if ( userCsv.length() > 0 ) {
   									userCsv.append(",");
   								}
   								userCsv.append(user.getDisplayName());
   							}
   						}
   						rec.setFieldValue(listOwnerCol, userCsv.toString());
   						rec.setFieldValue(listOwnedByMeCol, file.getOwnedByMe());
   						// Google API has its own DateTime in the API so convert to TSTool type for consistency.
   						com.google.api.client.util.DateTime fileCreationTime = file.getCreatedTime();
   						if ( fileCreationTime != null ) {
   							DateTime creationTime = DateTime.parse(fileCreationTime.toString());
   							rec.setFieldValue(listCreationTimeCol, creationTime);
   						}
   						rec.setFieldValue(listSharedCol, file.getShared());
   						User sharingUser = file.getSharingUser();
   						if ( sharingUser != null ) {
   							rec.setFieldValue(listSharingUserCol, sharingUser.getDisplayName());
   						}
   						// Google API has its own DateTime in the API so convert to TSTool type for consistency.
   						com.google.api.client.util.DateTime fileSharedWithMeTime = file.getSharedWithMeTime();
   						if ( fileSharedWithMeTime != null ) {
   							DateTime sharedWithMeTime = DateTime.parse(fileSharedWithMeTime.toString());
   							rec.setFieldValue(listSharedWithMeTimeCol, sharedWithMeTime);
   						}
   						// Google API has its own DateTime in the API so convert to TSTool type for consistency.
   						com.google.api.client.util.DateTime fileModifiedTime = file.getModifiedTime();
   						if ( fileModifiedTime != null ) {
   							DateTime modificationTime = DateTime.parse(fileModifiedTime.toString());
   							rec.setFieldValue(listLastModifiedTimeCol, modificationTime);
   						}
   						User lastModifyingUser = file.getLastModifyingUser();
   						if ( lastModifyingUser != null ) {
   							rec.setFieldValue(listLastModifiedUserCol, file.getLastModifyingUser().getDisplayName());
   						}
   						rec.setFieldValue(listTrashedCol, file.getTrashed());
   						User trashingUser = file.getTrashingUser();
   						if ( trashingUser != null ) {
   							rec.setFieldValue(listTrashedUserCol, trashingUser.getDisplayName());
   						}
   						// Google API has its own DateTime in the API so convert to TSTool type for consistency.
   						com.google.api.client.util.DateTime fileTrashedTime = file.getTrashedTime();
   						if ( fileTrashedTime != null ) {
   							DateTime trashedTime = DateTime.parse(fileTrashedTime.toString());
   							rec.setFieldValue(listTrashedTimeCol, trashedTime);
   						}
   						rec.setFieldValue(listOriginalFilenameCol, file.getOriginalFilename());
   						rec.setFieldValue(listWebViewLinkCol, file.getWebViewLink());
   					}
 					
 					// Break out of the loop if the object count has been reached.
 					if ( (listMax > 0) && (objectCount == listMax) ) {
 						break;
 					}
   				}

				// Break out of the loop if the object count has been reached.
				if ( (listMax > 0) && (objectCount == listMax) ) {
					break;
				}
   			}

			// Break out of the loop if the object count has been reached.
			if ( (listMax > 0) && (objectCount == listMax) ) {
				break;
			}
   		
   			// Get the next page of results.
   			String nextPageToken = result.getNextPageToken();
   			if ( nextPageToken == null ) {
   				break;
   			}
   			else {
   				//Message.printStatus(2, routine, "Next page token = \"" + nextPageToken + "\"" );
   				request.setPageToken(nextPageToken);
   				result = request.execute();
   			}
   		}

    	Message.printStatus ( 2, routine, "List has fileCount=" + fileCount + ", folderCount="
    		+ folderCount + ", objectCount=" + objectCount );
    	// Set the property indicating the number of bucket objects.
       	if ( (listCountProperty != null) && !listCountProperty.equals("") ) {
       		//int numObjects = objectCount;
       		int numObjects = table.getNumberOfRecords();
           	PropList requestParams = new PropList ( "" );
           	requestParams.setUsingObject ( "PropertyName", listCountProperty );
           	requestParams.setUsingObject ( "PropertyValue", new Integer(numObjects) );
           	try {
               	processor.processRequest( "SetProperty", requestParams);
           	}
           	catch ( Exception e ) {
               	message = "Error requesting SetProperty(" + listCountProperty + "=\"" + numObjects + "\") from processor.";
               	Message.printWarning(logLevel,
                   	MessageUtil.formatMessageTag( commandTag, ++warningCount),
                   	routine, message );
                    	status.addToLog ( CommandPhaseType.RUN,
                   	new CommandLogRecord(CommandStatusType.FAILURE,
                       	message, "Report the problem to software support." ) );
           	}
       	}

   		return warningCount;
	}

	/**
	Edit the command.
	@param parent The parent JFrame to which the command dialog will belong.
	@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
	*/
	public boolean editCommand ( JFrame parent ) {
		// The command will be modified if changed.
    	List<String> tableIDChoices =
        	TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            	(TSCommandProcessor)getCommandProcessor(), this);
		return new GoogleDrive_JDialog ( parent, this, tableIDChoices ).ok();
	}

	/**
	Return the table that is read by this class when run in discovery mode.
	*/
	private DataTable getDiscoveryTable() {
    	return this.discoveryOutputTable;
	}
	
	/**
	Return the list of files that were created by this command.
	*/
	public List<File> getGeneratedFileList () {
    	List<File> list = new ArrayList<>();
    	if ( getOutputFile() != null ) {
        	list.add ( getOutputFile() );
    	}
    	return list;
	}

	/**
	Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
	The following classes can be requested:  DataTable
	*/
	@SuppressWarnings("unchecked")
	public <T> List<T> getObjectList ( Class<T> c ) {
   	DataTable table = getDiscoveryTable();
    	List<T> v = null;
    	if ( (table != null) && (c == table.getClass()) ) {
        	v = new ArrayList<>();
        	v.add ( (T)table );
    	}
    	return v;
	}

	/**
	Return the output file generated by this file.  This method is used internally.
	*/
	private File getOutputFile () {
    	return __OutputFile_File;
	}

	/**
	Run the command.
	@param command_number Command number in sequence.
	@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
	@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
	*/
	public void runCommand ( int command_number )
	throws InvalidCommandParameterException, CommandWarningException, CommandException {
    	runCommandInternal ( command_number, CommandPhaseType.RUN );
	}

	/**
	Run the command in discovery mode.
	@param command_number Command number in sequence.
	@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
	@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
	*/
	public void runCommandDiscovery ( int command_number )
	throws InvalidCommandParameterException, CommandWarningException, CommandException {
    	runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
	}

	// Useful examples:
	// https://technicalsand.com/file-operations-in-google-drive-api-with-spring-boot/
	
	/**
	Run the command.
	@param commandNumber Number of command in sequence (1+).
	@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
	@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
	@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
	*/
	private void runCommandInternal ( int commandNumber, CommandPhaseType commandPhase )
	throws InvalidCommandParameterException, CommandWarningException, CommandException {
		String routine = getClass().getSimpleName() + ".runCommand", message;
		int warningLevel = 2;
		int logLevel = 3; // Level for non-user messages for log file.
		String commandTag = "" + commandNumber;
		int warningCount = 0;

		PropList parameters = getCommandParameters();

    	CommandProcessor processor = getCommandProcessor();
		CommandStatus status = getCommandStatus();
    	Boolean clearStatus = new Boolean(true); // Default.
    	try {
    		Object o = processor.getPropContents("CommandsShouldClearRunStatus");
    		if ( o != null ) {
    			clearStatus = (Boolean)o;
    		}
    	}
    	catch ( Exception e ) {
    		// Should not happen.
    	}
    	if ( clearStatus ) {
			status.clearLog(commandPhase);
		}

    	// Clear the output file.
    	setOutputFile ( null );

		String GoogleDriveCommand = parameters.getValue ( "GoogleDriveCommand" );
		GoogleDriveCommandType googleDriveCommand = GoogleDriveCommandType.valueOfIgnoreCase(GoogleDriveCommand);
		String SessionID = parameters.getValue ( "SessionID" );
		String AuthenticationMethod = parameters.getValue ( "AuthenticationMethod" );
		GoogleDriveAuthenticationMethodType authenticationMethod = null;
		if ( AuthenticationMethod == null ) {
			authenticationMethod = GoogleDriveAuthenticationMethodType.SERVICE_ACCOUNT_KEY; // Default.
		}
		else {
			authenticationMethod = GoogleDriveAuthenticationMethodType.valueOfIgnoreCase(AuthenticationMethod);
			if ( authenticationMethod == null ) {
				message = "Invalid authentication method.";
				Message.printWarning ( warningLevel,
					MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
				status.addToLog(CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "See the log file for details."));
				throw new CommandException ( message );
			}	
		}
		
		// Get command parameters for: Copy - CopyFiles

		// Copy.
    	/*
		String CopyFiles = parameters.getValue ( "CopyFiles" );
		CopyFiles = TSCommandProcessorUtil.expandParameterValue(processor,this,CopyFiles);
		int copyFilesCount = 0;
		List<String> copyFilesSourceKeyList = new ArrayList<>();
		List<String> copyFilesDestKeyList = new ArrayList<>();
    	if ( (CopyFiles != null) && !CopyFiles.isEmpty() && (CopyFiles.indexOf(":") > 0) ) {
        	// First break map pairs by comma.
        	List<String>pairs = StringUtil.breakStringList(CopyFiles, ",", 0 );
        	// Now break pairs and put in lists.
        	for ( String pair : pairs ) {
        		++copyFilesCount;
            	String [] parts = pair.split(":");
            	if ( parts.length == 2 ) {
            		String sourceKey = parts[0].trim();
            		String destKey = parts[1].trim();
            		if ( commandPhase == CommandPhaseType.RUN ) {
            			if ( sourceKey.indexOf("${") >= 0 ) { // } To match bracket.
       			   			message = "File source key " + copyFilesCount + " (" + sourceKey +
       			   				") contains ${ due to unknown processor property - skipping to avoid copy error."; // } To match bracket.
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the property is defined." ) );
        		   			continue;
        	   			}
            			if ( destKey.indexOf("${") >= 0 ) { // } To match bracket.
       			   			message = "File destination key " + copyFilesCount + " (" + destKey +
       			   				") contains ${ due to unknown processor property - skipping to avoid copy error."; // } To match bracket.
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the property is defined." ) );
        		   			continue;
        	   			}
            			if ( sourceKey.endsWith("/") ) {
       			   			message = "File source key " + copyFilesCount + " (" + sourceKey +
       			   				") ends with /, which indicates a folder - skipping.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the key for the source file is not a folder (does not end in /).") );
        		   			continue;
        	   			}
            			if ( destKey.endsWith("/") ) {
       			   			message = "File destination key " + copyFilesCount + " (" + sourceKey +
       			   				") ends with /, which indicates a folder - skipping.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the key for the destination file is not a folder (does not end in /)." ) );
        		   			continue;
        	   			}
            			if ( sourceKey.indexOf("*") >= 0 ) {
            				// Source key has a wildcard:
            				// - not supported
            				message = "File source key uses * wildcard - skipping.";
			       			Message.printWarning(warningLevel,
				   				MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
			       			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
				   				message, "Wildcard is not allowed for the file source key." ) );
			       			continue;
            			}
            			if ( destKey.endsWith("*") ) {
   						    // Replace the wildcard with the file name for the source.
            				// Get the name from the source key.
            				int pos = sourceKey.lastIndexOf("/");
            				String sourceName = null;
            				if ( pos >= 0 ) {
            					// This will work because sourceKey was checked above for ending in /.
            					sourceName = sourceKey.substring(pos + 1);
            				}
            				else {
            					// Source has no folder so just use the folder name.
            					sourceName = sourceKey;
            				}
            				// Handle specific wildcard cases for destKey:
           					// - destination must equal "*" or end in "/*"
           					// - handles the edge case for copying root folder files
           					if ( destKey.equals("*") ) {
           						destKey = sourceName;
   						    	if ( Message.isDebugOn ) {
   						    		Message.printStatus(2, routine, "                     File xource key: " + sourceKey );
   						    		Message.printStatus(2, routine, "File destination key from * wildcard: " + destKey );
   						    	}
           					}
           					else if ( destKey.endsWith("/*") ) {
            					destKey = destKey.replace("*", sourceName );
   						    	if ( Message.isDebugOn ) {
   						    		Message.printStatus(2, routine, "                      File source key: " + sourceKey );
   						    		Message.printStatus(2, routine, "File destination key from /* wildcard: " + destKey );
   						    	}
           					}
           					else {
           						message = "File destination key must equal * or end in /* to use source key file name.";
		       					Message.printWarning(warningLevel,
			   						MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
		       					status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
			   						message, "Fix the file destination key to equal * or end in /*." ) );
		       					continue;
           					}	
            			}
            			// Add to the lists for further processing.
            			//uploadFilesOrig.add(localFile);
            			copyFilesSourceKeyList.add(sourceKey);
            			copyFilesDestKeyList.add(destKey);
            		}
            	}
        	}
    	}
    	*/

		// Get command parameters for: Bucket (or Google Drive) TBD

    	/*
		// Bucket must be final because of lambda use below.
		String copyBucket0 = parameters.getValue ( "CopyBucket" );
		final String copyBucket = TSCommandProcessorUtil.expandParameterValue(processor,this,copyBucket0);
    	String CopyObjectsCountProperty = parameters.getValue ( "CopyObjectsCountProperty" );
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		CopyObjectsCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, CopyObjectsCountProperty);
    	}
    	*/

    	/*
		// Get command parameters for: Delete - DeleteFiles

		String DeleteFiles = parameters.getValue ( "DeleteFiles" );
		DeleteFiles = TSCommandProcessorUtil.expandParameterValue(processor,this,DeleteFiles);
		List<String> deleteFilesKeys = new ArrayList<>();
		int deleteFilesCount = 0;
		if ( (DeleteFiles == null) || !DeleteFiles.isEmpty() ) {
			List<String> deleteFilesKeys0 = StringUtil.breakStringList(DeleteFiles,",", StringUtil.DELIM_TRIM_STRINGS);
			for ( String deleteFilesKey : deleteFilesKeys0 ) {
				++deleteFilesCount;
           		if ( commandPhase == CommandPhaseType.RUN ) {
           			if ( deleteFilesKey.endsWith("/") ) {
		   				message = "File key " + deleteFilesCount + " (" + deleteFilesKey +
		   					") ends with /, which indicates a folder - skipping.";
     	   				Message.printWarning(warningLevel,
   		   				MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
      	   				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
    		   				message, "Confirm that the key for the file to delete is not a folder (does not end in /).") );
   		   				continue;
           			}
           			// Add the file to delete.
           			deleteFilesKeys.add(deleteFilesKey);
           		}
			}
		}
		*/

		// Get command parameters for: Delete - DeleteFolders

    	/*
		String DeleteFolders = parameters.getValue ( "DeleteFolders" );
		DeleteFolders = TSCommandProcessorUtil.expandParameterValue(processor,this,DeleteFolders);
		List<String> deleteFoldersKeys = new ArrayList<>();
		int deleteFoldersCount = 0;
		if ( (DeleteFolders == null) || !DeleteFolders.isEmpty() ) {
			List<String> deleteFoldersKeys0 = StringUtil.breakStringList(DeleteFolders,",", StringUtil.DELIM_TRIM_STRINGS);
			for ( String deleteFoldersKey : deleteFoldersKeys0 ) {
				++deleteFoldersCount;
           		if ( commandPhase == CommandPhaseType.RUN ) {
           			if ( !deleteFoldersKey.endsWith("/") ) {
		   				message = "Folder key " + deleteFoldersCount + " (" + deleteFoldersKey +
		   					") does not end with /, which indicates a file - skipping.";
     	   				Message.printWarning(warningLevel,
   		   				MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
      	   				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
    		   				message, "Confirm that the key for the folder to delete is not a file (folder key should end with /).") );
   		   				continue;
           			}
           			// Add the folder to delete.
           			deleteFoldersKeys.add(deleteFoldersKey);
           		}
			}
		}
		String DeleteFoldersScope = parameters.getValue ( "DeleteFoldersScope" );
		if ( (DeleteFoldersScope == null) || DeleteFoldersScope.isEmpty() ) {
			DeleteFoldersScope = this._FolderFiles;
		}
		String DeleteFoldersMinDepth = parameters.getValue ( "DeleteFoldersMinDepth" );
		int deleteFoldersMinDepth = _DeleteFoldersMinDepth;
		if ( (DeleteFoldersMinDepth != null) && !DeleteFoldersMinDepth.isEmpty() ) {
			try {
				deleteFoldersMinDepth = Integer.parseInt(DeleteFoldersMinDepth.trim());
			}
			catch ( NumberFormatException e ) {
				// Warning will have been generated by checkCommandParameters().  Use the default.
				deleteFoldersMinDepth = _DeleteFoldersMinDepth;
			}
		}
		*/

		// Get command parameters for: Download - DownloadFolders

    	String DownloadFolders = parameters.getValue ( "DownloadFolders" );
		DownloadFolders = TSCommandProcessorUtil.expandParameterValue(processor,this,DownloadFolders);
		// Can't use a hashtable because sometimes download the same folders to multiple locations.
    	List<String> downloadFoldersGoogleDrivePaths = new ArrayList<>();
    	List<String> downloadFoldersDirectories = new ArrayList<>();
       	String localFolder = null;
       	String remoteFolder = null;
    	if ( (DownloadFolders != null) && (DownloadFolders.length() > 0) && (DownloadFolders.indexOf(":") > 0) ) {
        	// First break map pairs by comma.
        	List<String>pairs = StringUtil.breakStringList(DownloadFolders, ",", 0 );
        	// Now break pairs and put in lists.
       		int downloadFoldersCount = 0;
        	for ( String pair : pairs ) {
        		++downloadFoldersCount;
            	String [] parts = pair.split(":");
            	if ( parts.length == 2 ) {
            		// Download into a specific folder.
            		remoteFolder = parts[0].trim();
            		localFolder = parts[1].trim();
            		if ( commandPhase == CommandPhaseType.RUN ) {
           				// Convert the command parameter folder to absolute path.
		   				String localFolderFull = IOUtil.verifyPathForOS(
		      				IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
		        				TSCommandProcessorUtil.expandParameterValue(processor,this,localFolder)));
            			if ( localFolder.isEmpty() ) {
       			   			message = "Local folder " + downloadFoldersCount + " is empty - skipping to avoid download error.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the local folder is specified." ) );
        		   			continue;
        	   			}
            			if ( remoteFolder.isEmpty() ) {
       			   			message = "Remote folder " + downloadFoldersCount + " is empty - skipping to avoid download error.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the remote folder is specified." ) );
        		   			continue;
        	   			}
			   			// Make sure that the local folder does not contain wildcard or ${ for properties. // } To match bracket.
            			if ( localFolderFull.indexOf("${") >= 0 ) { // } To match bracket.
       						message = "Local folder " + downloadFoldersCount + " (" + localFolder +
       							") contains ${ due to unknown processor property - skipping to avoid download error."; // } To match bracket.
	        				Message.printWarning(warningLevel,
		    	   				MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    	   				message, "Confirm that the property is defined." ) );
        					continue;
        	   			}
            			if ( localFolderFull.indexOf("*") >= 0 ) {
       						message = "Local folder " + downloadFoldersCount + " (" + localFolder +
       							") contains wildcard * in name, which is not allowed - skipping folder.";
	        				Message.printWarning(warningLevel,
		    	  				MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    	   				message, "Check the local folder name." ) );
        					continue;
        	   			}
           				if ( remoteFolder.indexOf("${") >= 0 ) { // } To match bracket.
      			   				message = "Remote folder (object key) for folder " + downloadFoldersCount + " (" + remoteFolder +
      			   					") contains ${ due to unknown processor property - skipping to avoid download error."; // } To match bracket.
        	   				Message.printWarning(warningLevel,
	    		   				MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
        	   				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
	    		   				message, "Confirm that the property is defined." ) );
       		   				continue;
       	   				}
           				if ( remoteFolder.indexOf("*") >= 0 ) {
   			   				message = "Remote folder (object key) for folder " + downloadFoldersCount + " (" + remoteFolder +
   			   					") contains * in name - skipping to avoid download error.";
        	   				Message.printWarning(warningLevel,
	    		   				MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
        	   				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
	    		   				message, "Confirm that the remote folder is correct." ) );
       		   				continue;
       	   				}
           				if ( !remoteFolder.endsWith("/") ) {
   			   				message = "Remote folder (object key) for folder " + downloadFoldersCount + " (" + remoteFolder +
   			   					") does not end in / to indicate a folder.";
        	   				Message.printWarning(warningLevel,
	    		   				MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
        	   				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
	    		   			message, "Confirm that the folder ends in / to indicate a folder." ) );
       		   				continue;
       	   				}
           				// Expand the local filename:
            			// - this will expand the leading folder(s) for properties
			   			localFolderFull = IOUtil.verifyPathForOS(
			      			IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
			        			TSCommandProcessorUtil.expandParameterValue(processor,this,localFolder)), true);
            			downloadFoldersGoogleDrivePaths.add(remoteFolder);
            			downloadFoldersDirectories.add(localFolderFull);
            			if ( Message.isDebugOn ) {
           					Message.printStatus(2, routine, "             Remote folder: " + remoteFolder );
            				Message.printStatus(2, routine, "              Local folder: " + localFolderFull );
            			}
            		}
            	}
           		else {
   			   		message = "Folder data \"" + pair + "\" contains " + parts.length
   			   			+ " parts, expecting 2 parts S3Folder:LocalFolder.";
        	   		Message.printWarning(warningLevel,
	    				MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
        	   		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
	    				message, "Check the folder data." ) );
           		}
        	}
    	}

    	// Get command parameters for: Download - DownloadFiles
    	
    	String DownloadFiles = parameters.getValue ( "DownloadFiles" );
		DownloadFiles = TSCommandProcessorUtil.expandParameterValue(processor,this,DownloadFiles);
		// Can't use a hashtable because sometimes download the same files to multiple local locations.
    	List<String> downloadFilesGoogleDrivePaths = new ArrayList<>();
    	List<String> downloadFilesFiles = new ArrayList<>();
    	if ( (DownloadFiles != null) && (DownloadFiles.length() > 0) && (DownloadFiles.indexOf(":") > 0) ) {
        	// First break map pairs by comma.
        	List<String>pairs = StringUtil.breakStringList(DownloadFiles, ",", 0 );
        	// Now break pairs and put in lists.
        	int downloadFilesCount = 0;
        	for ( String pair : pairs ) {
        		++downloadFilesCount;
            	String [] parts = pair.split(":");
            	if ( parts.length == 2 ) {
            		String remoteFile = parts[0].trim();
            		String localFile = parts[1].trim();
            		if ( commandPhase == CommandPhaseType.RUN ) {
            			if ( localFile.isEmpty() ) {
       			   			message = "Local file " + downloadFilesCount + " is empty - skipping to avoid download error.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the local file is specified." ) );
        		   			continue;
        	   			}
            			if ( remoteFile.isEmpty() ) {
       			   			message = "Remote file " + downloadFilesCount + " is empty - skipping to avoid download error.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the remote file is specified." ) );
        		   			continue;
        	   			}
            			if ( localFile.indexOf("${") >= 0 ) { // } To match bracket.
       			   			message = "Local file " + downloadFilesCount + " (" + localFile +
       			   				") contains ${ due to unknown processor property - skipping to avoid download error."; // } To match bracket.
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the property is defined." ) );
        		   			continue;
        	   			}
            			if ( remoteFile.indexOf("${") >= 0 ) { // } To match bracket.
       			   			message = "Remote file (object key) for file " + downloadFilesCount + " (" + remoteFile +
       			   				") contains ${ due to unknown processor property - skipping to avoid download error."; // } To match bracket.
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the property is defined." ) );
        		   			continue;
        	   			}
            			if ( remoteFile.endsWith("/") ) {
       			   			message = "Remote file (object key) " + downloadFilesCount + " (" + remoteFile +
       			   				") ends with /, which indicates a folder.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the S3 file does not end in /." ) );
        		   			continue;
        	   			}
            		}
            		boolean localHadWildcard = false;
            		if ( localFile.indexOf("*") >= 0 ) {
            			// Local file has a wildcard so it should be a folder with wildcard for the file:
            			// - this does not handle * in the folder as in: folder/* /folder/file.*
            			if ( Message.isDebugOn ) {
            				Message.printStatus(2, routine, "Local file has a wildcard.");
            			}
            			localHadWildcard = true;
            			// Handle Linux and Windows paths.
            			if ( !localFile.endsWith("/*") && !localFile.endsWith("\\*") ) {
            				// Remote file must end with /* so that local file can also be used.
            				// This limits wildcards in the root folder but that is unlikely.
            				message = "Local file uses * wildcard but does not end in /* - skipping.";
			        		Message.printWarning(warningLevel,
				    			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
			        		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
				    			message, "Specify the local file with /* at the end." ) );
			        		continue;
            			}
            			// Replace the wildcard with the remote file name without trailing /.
            			File remoteFileObj = new File(remoteFile);
            			String remoteName = remoteFileObj.getName();
            			if ( (remoteName != null) && !remoteName.isEmpty() ) {
            				localFile = localFile.replace("*", remoteName);
            			}
            		}
            		// Expand the local filename:
            		// - this will expand the leading folder(s) for properties
			   		String localFileFull = IOUtil.verifyPathForOS(
			      		IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
			        		TSCommandProcessorUtil.expandParameterValue(processor,this,localFile)), true);
            		//downloadFilesOrig.add(localFile);
            		downloadFilesFiles.add(localFileFull);
            		downloadFilesGoogleDrivePaths.add(remoteFile);
            		if ( Message.isDebugOn ) {
           				Message.printStatus(2, routine, "             Remote file: " + remoteFile );
            			if ( localHadWildcard ) {
            				Message.printStatus(2, routine, "Local file from wildcard: " + localFileFull );
            			}
            			else {
            				Message.printStatus(2, routine, "              Local file: " + localFileFull );
            			}
            		}
            	}
            	else {
       				message = "File data \"" + pair + "\" contains " + parts.length
       					+ " parts, expecting 2 parts S3File:LocalFile.";
	        		Message.printWarning(warningLevel,
		    			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    			message, "Check the folder data." ) );
            	}
        	}
    	}

		// Get command parameters for: List Buckets (TODO smalers 2023-12-18 convert to list Drives and handled shared)

    	// List buckets.
		String ListBucketsRegEx = parameters.getValue ( "ListBucketsRegEx" );
		// TODO smalers 2023-01-27 evaluate whether regex can be expanded or will have conflicts.
		//ListBucketsRegEx = TSCommandProcessorUtil.expandParameterValue(processor,this,ListBucketsRegEx);
		// Convert the RegEx to Java style.
		String listBucketsRegEx = null;
		if ( (ListBucketsRegEx != null) && !ListBucketsRegEx.isEmpty() ) {
			if ( ListBucketsRegEx.toUpperCase().startsWith("JAVA:") ) {
				// Use as is for a Java regular expression.
				listBucketsRegEx = ListBucketsRegEx.substring(5);
			}
			else {
				// Default to glob so convert to Java regex.
				// TODO smalers 2023-02-01 need to hanle [abc] and [a-z].
				listBucketsRegEx = ListBucketsRegEx.replace(".", "\\.").replace("*", ".*");
			}
		}
    	String ListBucketsCountProperty = parameters.getValue ( "ListBucketsCountProperty" );
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		ListBucketsCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, ListBucketsCountProperty);
    	}

		// Get command parameters for: List - ListFolders

   		String ListScope = parameters.getValue ( "ListScope" );
    	ListScope = TSCommandProcessorUtil.expandParameterValue(processor, this, ListScope);
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		ListScope = TSCommandProcessorUtil.expandParameterValue(processor, this, ListScope);
    	}
   		String ListFolderPath = parameters.getValue ( "ListFolderPath" );
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		ListFolderPath = TSCommandProcessorUtil.expandParameterValue(processor, this, ListFolderPath);
    	}

		// Get command parameters for: List - ListFiles

   		String ListFiles = parameters.getValue ( "ListFiles" );
	  	boolean listFiles = true; // Default.
	  	if ( (ListFiles != null) && ListFiles.equalsIgnoreCase("false") ) {
			listFiles = false;
	  	}
	  	String ListFolders = parameters.getValue ( "ListFolders" );
	  	boolean listFolders = true; // Default.
	  	if ( (ListFolders != null) && ListFolders.equalsIgnoreCase("false") ) {
			listFolders = false;
	  	}
   		String ListShared = parameters.getValue ( "ListShared" );
	  	if ( (ListShared == null) || ListShared.isEmpty() ) {
			ListShared = _False; // Default.
	  	}
   		String ListTrashed = parameters.getValue ( "ListTrashed" );
	  	boolean listTrashed = false; // Default.
	  	if ( (ListTrashed != null) && ListTrashed.equalsIgnoreCase("true") ) {
			listTrashed = true;
	  	}
   		String ListMax = parameters.getValue ( "ListMax" );
   		int listMax = -1;
	  	if ( (ListMax != null) && !ListMax.isEmpty() ) {
			listMax = Integer.parseInt(ListMax);
	  	}
	  	String ListCountProperty = parameters.getValue ( "ListCountProperty" );
	  	if ( commandPhase == CommandPhaseType.RUN ) {
	  		ListCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, ListCountProperty);
	  	}

    	String ListRegEx = parameters.getValue ( "ListRegEx" );
	  	// TODO smalers 2023-01-27 evaluate whether regex can be expanded or will have conflicts.
	  	//ListRegEx = TSCommandProcessorUtil.expandParameterValue(processor,this,ListRegEx);
	  	// Convert the RegEx to Java style.
	  	String listRegEx = null;
	  	if ( (ListRegEx != null) && !ListRegEx.isEmpty() ) {
	  		if ( ListRegEx.toUpperCase().startsWith("JAVA:") ) {
		  		// Use as is for a Java regular expression.
		  		listRegEx = ListRegEx.substring(5);
	  		}
	  		else {
		  		// Default to glob so convert * to Java .*
		  		listRegEx = ListRegEx.replace("*", ".*");
	  		}
		}
    		
		// Get command parameters for: Upload - UploadFolders

    	/*
    	// Upload.
    	String UploadFolders = parameters.getValue ( "UploadFolders" );
		UploadFolders = TSCommandProcessorUtil.expandParameterValue(processor,this,UploadFolders);
		// Can't use a hashtable because sometimes upload the same folders to multiple S3 locations.
    	List<String> uploadFoldersOrig = new ArrayList<>(); // For log messages.
    	List<String> uploadFoldersDirectoryList = new ArrayList<>();
    	List<String> uploadFoldersKeyList = new ArrayList<>();
       	int uploadFoldersCount = 0;
    	if ( (UploadFolders != null) && (UploadFolders.length() > 0) && (UploadFolders.indexOf(":") > 0) ) {
        	// First break map pairs by comma.
        	List<String>pairs = StringUtil.breakStringList(UploadFolders, ",", 0 );
        	// Now break pairs and put in lists.
        	for ( String pair : pairs ) {
        		++uploadFoldersCount;
            	String [] parts = pair.split(":");
            	if ( parts.length == 2 ) {
            		localFolder = parts[0].trim();
            		remoteFolder = parts[1].trim();
            		if ( commandPhase == CommandPhaseType.RUN ) {
            			// Convert the command parameter folder to absolute path.
			   			String localFolderFull = IOUtil.verifyPathForOS(
			      			IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
			        			TSCommandProcessorUtil.expandParameterValue(processor,this,parts[0].trim())));
			   			// Make sure that the local folder does not contain wildcard or ${ for properties. // } To match bracket.
            			if ( localFolderFull.indexOf("${") >= 0 ) { // } To match bracket.
       			   			message = "Local folder " + uploadFoldersCount + " (" + localFolder +
       			   				") contains ${ due to unknown processor property - skipping to avoid upload error."; // } To match bracket.
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the property is defined." ) );
        		   			continue;
        	   			}
            			if ( localFolderFull.indexOf("*") >= 0 ) {
       			   			message = "Local folder " + uploadFoldersCount + " (" + localFolder +
       			   				") contains wildcard * in name, which is not allowed - skipping folder.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Check the local folder name." ) );
        		   			continue;
        	   			}
            			File f = new File(localFolderFull);
            			if ( !f.exists() ) {
       			   			message = "Local folder " + uploadFoldersCount + " (" + localFolder +
       			   				") does not exist - skipping to avoid upload error.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the folder exists." ) );
        		   			continue;
        	   			}
            			if ( !f.isDirectory() ) {
       			   			message = "Local folder " + uploadFoldersCount + " (" + localFolder + ") is not a folder - skipping.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the folder exists." ) );
        		   			continue;
        	   			}
            			if ( remoteFolder.indexOf("${") >= 0 ) { // } To match bracket.
       			   			message = "Remote folder (object key) for folder " + uploadFoldersCount + " (" + remoteFolder +
       			   				") contains ${ due to unknown processor property - skipping to avoid unexpected file on S3."; // } To match bracket.
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the property is defined." ) );
        		   			continue;
        	   			}
            			if ( remoteFolder.indexOf("*") >= 0 ) {
       			   			message = "Remote folder (object key) for folder " + uploadFoldersCount + " (" + remoteFolder +
       			   				") contains * in name - skipping to avoid unexpected file on S3.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the property is defined." ) );
        		   			continue;
        	   			}
            			if ( !remoteFolder.endsWith("/") ) {
       			   			message = "Remote folder (object key) for folder " + uploadFoldersCount + " (" + remoteFolder +
       			   				") does not end in / to indicate a folder.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the folder ends in / to indicate a folder." ) );
        		   			continue;
        	   			}
            			// If here then the input seems valid.
            			uploadFoldersOrig.add(localFolder);
               			uploadFoldersDirectoryList.add(localFolderFull);
               			uploadFoldersKeyList.add(remoteFolder);
            		}
            	}
           		else {
   			   		message = "Folder data \"" + pair + "\" contains " + parts.length +
   			   			" parts, expecting 2 parts LocalFolder:S3Folder.";
        	   		Message.printWarning(warningLevel,
	    				MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
        	   		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
	    				message, "Check the folder data." ) );
           		}
        	}
    	}
    	*/

		// Get command parameters for: Upload - UploadFiles

    	/*
    	String UploadFiles = parameters.getValue ( "UploadFiles" );
    	// Expand the entire parameter string before parsing into pairs.
		UploadFiles = TSCommandProcessorUtil.expandParameterValue(processor,this,UploadFiles);
		// Can't use a hashtable because sometimes upload the same files to multiple S3 locations.
    	List<String> uploadFilesOrig = new ArrayList<>(); // For log messages.
    	List<String> uploadFilesFileList = new ArrayList<>();
    	List<String> uploadFilesKeyList = new ArrayList<>();
        int uploadFilesCount = 0;
    	if ( (UploadFiles != null) && !UploadFiles.isEmpty() && (UploadFiles.indexOf(":") > 0) ) {
        	// First break map pairs by comma.
        	List<String>pairs = StringUtil.breakStringList(UploadFiles, ",", 0 );
        	// Now break pairs and put in lists.
        	for ( String pair : pairs ) {
        		++uploadFilesCount;
            	String [] parts = pair.split(":");
            	if ( parts.length == 2 ) {
            		String localFile = parts[0].trim();
            		String remoteFile = parts[1].trim();
            		if ( commandPhase == CommandPhaseType.RUN ) {
            			if ( localFile.indexOf("${") >= 0 ) { // } To match bracket.
       			   			message = "Local file " + uploadFilesCount + " (" + localFile +
       			   				") contains ${ due to unknown processor property - skipping to avoid upload error."; // } To match bracket.
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the property is defined." ) );
        		   			continue;
        	   			}
            			if ( remoteFile.indexOf("${") >= 0 ) { // } To match bracket.
       			   			message = "Remote file (object key) for file " + uploadFilesCount + " (" + remoteFile +
       			   				") contains ${ due to unknown processor property - skipping to avoid unexpected file on S3."; // } To match bracket.
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the property is defined." ) );
        		   			continue;
        	   			}
            			if ( remoteFile.endsWith("/") ) {
       			   			message = "Remote file (object key) " + uploadFilesCount + " (" + remoteFile +
       			   				") ends with /, which indicates a folder.";
	        	   			Message.printWarning(warningLevel,
		    		   			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	        	   			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    		   			message, "Confirm that the S3 file does not end in /." ) );
        		   			continue;
        	   			}
            		}
            		if ( localFile.indexOf("*") >= 0 ) {
            			// Local file has a wildcard so it should be a folder with file wildcard pattern:
            			// - this does not handle * in the folder as in: folder/ * /folder/file.*
            			Message.printStatus(2, routine, "Local file has a wildcard.");
            			if ( !remoteFile.endsWith("/*") ) {
            				// Remote file must end with /* so that local file can also be used on S3.
            				// This limits wildcards in the root folder but that is unlikely.
            				message = "Local file uses * wildcard but bucket key does not end in /* - skipping.";
			        		Message.printWarning(warningLevel,
				    			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
			        		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
				    			message, "Specify the bucket key with /* at the end." ) );
			        		continue;
            			}
            			// Local file has a wildcard so need to expand to matching files and then process each:
            			// - this will expand the leading folder(s) for properties
			   			String localFileFull = IOUtil.verifyPathForOS(
			      			IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
			        			TSCommandProcessorUtil.expandParameterValue(processor,this,parts[0].trim())));
            			List<File> localPathList = null;
            			try {
            				Message.printStatus(2,routine,"Getting local file list using wildcard:" + localFileFull );
            				// The following method requires forward slashes.
            				localPathList = IOUtil.getFilesMatchingPattern("glob:" + localFileFull.replace("\\", "/"));
            				for ( File localPath : localPathList ) {
            					// Use a copy because otherwise the wildcard is removed after the first iteration.
            					String remoteFile2 = remoteFile;
            					if ( remoteFile.endsWith("/*") ) {
            						 remoteFile2 = remoteFile.replace("*", localPath.getName());
            					}
            					uploadFilesOrig.add(localFile);
            					uploadFilesFileList.add(localPath.getAbsolutePath());
            					uploadFilesKeyList.add(remoteFile2);
            					if ( Message.isDebugOn ) {
            						Message.printStatus(2, routine, "Local file from wildcard: " + localPath.getAbsolutePath() );
            						Message.printStatus(2, routine, "             Remote file: " + remoteFile2 );
            					}
            				}
            			}
            			catch ( Exception e ) {
            				message = "Error getting list of local files for \"" + localFileFull + "\" (" + e + ").";
			        		Message.printWarning(warningLevel,
				    			MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
			        		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
				    			message, "Report problem to software support." ) );
            			}
            		}
            		else {
            			// Simple file with no wildcard at the end.
            			Message.printStatus(2, routine, "Local file is a simple file.");
            			// Convert the command parameter local file to absolute path.
			   			String localFileFull = IOUtil.verifyPathForOS(
			      			IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
			        			TSCommandProcessorUtil.expandParameterValue(processor,this,parts[0].trim())));
			   			File f = new File(localFileFull);
		   				if ( !f.exists() ) {
		   					// Local file does not exist.
		   					message = "Local file " + uploadFilesCount + " (" + localFile + ") does not exist - skipping.";
		   					Message.printWarning(warningLevel,
		   						MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
		   					status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		   						message, "Verify that the file exists." ) );
		   					continue;
			   			}
		   				if ( f.isDirectory() ) {
		   					// File path is actually a folder.
		   					message = "Local file " + uploadFilesCount + " (" + localFile + ") is actually a folder - skipping.";
		   					Message.printWarning(warningLevel,
		   						MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
		   					status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		   						message, "Use the UploadFolders parameter to upload folders." ) );
		   					continue;
			   			}
            			// Have passed checks so can upload.
            			uploadFilesOrig.add(localFile);
               			uploadFilesFileList.add(localFileFull);
               			if ( remoteFile.endsWith("/*") ) {
               				Message.printStatus(2, routine, "Remote file ends with /*");
               				// Use the file from the local path and replace *.
               				remoteFile = remoteFile.replace("*", f.getName());
               				Message.printStatus(2, routine, "Remote file after replacing * is: " + remoteFile );
               			}
               			else {
               				// Just add the remote file as is without adjusting the remote name.
               			}
               			uploadFilesKeyList.add(remoteFile);
            		}
            	}
        	}
    	}
       	*/

		// Get command parameters for: Output

    	// Expand the entire parameter string before parsing into pairs.
    	// Output.
		boolean doTable = false;
		String OutputTableID = parameters.getValue ( "OutputTableID" );
		OutputTableID = TSCommandProcessorUtil.expandParameterValue(processor,this,OutputTableID);
		if ( (OutputTableID != null) && !OutputTableID.isEmpty() ) {
			doTable = true;
		}
		// If an output file is to be written:
		// - output using the table, if available
		// - if an output table is not being created, create a temporary table and write it
		boolean doOutputFile = false;
		String OutputFile = parameters.getValue ( "OutputFile" ); // Expand below.
		if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
			doOutputFile = true;
		}
		String IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
		if ( (IfInputNotFound == null) || IfInputNotFound.equals("")) {
	    	IfInputNotFound = _Warn; // Default
		}
		String AppendOutput = parameters.getValue ( "AppendOutput" );
		boolean appendOutput = false;
		if ( (AppendOutput != null) && AppendOutput.equalsIgnoreCase(_True)) {
			appendOutput = true;
		}

    	// Final checks, based on the command parameters:
		// - only warn if the original file and folder list was not empty
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		/*
    		if ( s3Command == AwsS3CommandType.UPLOAD_OBJECTS ) {
    			if ( (uploadFoldersCount != 0) && (uploadFoldersDirectoryList.size() == 0) ) {
      				message = "No folders were found for the upload.";
      				Message.printWarning(warningLevel,
      					MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
      				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Check the UploadFolders parameter and existence of associated files." ) );
    			}
    			if ( (uploadFilesCount != 0) && (uploadFilesFileList.size() == 0) ) {
      				message = "No files were found for the upload.";
      				Message.printWarning(warningLevel,
      					MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
      				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Check the UploadFiles parameter and existence of associated files." ) );
    			}
    		}
    		*/
    	}

		// Get the table to process:
		// - only if appending
		// - if not appending, (re)create below

		DataTable table = null;
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		PropList requestParams = null;
			CommandProcessorRequestResultsBean bean = null;
		  	if ( (OutputTableID != null) && !OutputTableID.isEmpty() && appendOutput ) {
				// Get the table to be updated.
				requestParams = new PropList ( "" );
				requestParams.set ( "TableID", OutputTableID );
				try {
					bean = processor.processRequest( "GetTable", requestParams);
			 		PropList bean_PropList = bean.getResultsPropList();
			  		Object o_Table = bean_PropList.getContents ( "Table" );
			  		if ( o_Table != null ) {
				  		// Found the table so no need to create it below.
				  		table = (DataTable)o_Table;
				  		Message.printStatus(2, routine, "Found existing table for append.");
			  		}
				}
				catch ( Exception e ) {
			 		message = "Error requesting GetTable(TableID=\"" + OutputTableID + "\") from processor (" + e + ").";
			  		Message.printWarning(warningLevel,
				  		MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
			  		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
				  		message, "Report problem to software support." ) );
				}
		  	}
    	}

		if ( warningCount > 0 ) {
			message = "There were " + warningCount + " warnings about command parameters.";
			Message.printWarning ( warningLevel,
			MessageUtil.formatMessageTag(commandTag, ++warningCount), routine, message );
			throw new InvalidCommandParameterException ( message );
		}

		// Have processed input. Now run the command.
		
		try {
			if ( commandPhase == CommandPhaseType.RUN ) {
				// Create a session, which has the credential.
				GoogleDriveSession googleDriveSession = null;

				try {
					googleDriveSession = new GoogleDriveSession(SessionID, authenticationMethod);
				}
				catch ( Exception e ) {
					message = "Error creating a Google Drive session.";
					Message.printWarning ( warningLevel,
						MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
					Message.printWarning ( 3, routine, e );
					status.addToLog(CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
							message, "See the log file for details."));
					throw new CommandException ( message );
				}

    			// Column numbers are used later.

	    		// Drive list columns.
        		int driveNameCol = -1;
        		int driveCreationDateCol = -1;

        		// List columns:
        		// - list alphabetically
        		int listCreationTimeCol = -1;
        		int listIdCol = -1;
   	    		int listLastModifiedTimeCol = -1;
   	    		int listLastModifiedUserCol = -1;
        		int listNameCol = -1;
   	    		int listOriginalFilenameCol = -1;
   	    		int listOwnedByMeCol = -1;
   	    		int listOwnerCol = -1;
        		int listParentFolderCol = -1;
        		int listParentFolderIdCol = -1;
   	    		int listSharedCol = -1;
   	    		int listSharedWithMeTimeCol = -1;
   	    		int listSharingUserCol = -1;
   	    		int listSizeCol = -1;
        		int listTrashedCol = -1;
        		int listTrashedTimeCol = -1;
        		int listTrashedUserCol = -1;
        		int listTypeCol = -1;
        		int listWebViewLinkCol = -1;

	    		if ( doTable || doOutputFile) {
	    			// Requested a table and/or file:
	    			// - if only file is request, create a temporary table that is then written to output
    	    		if ( (table == null) || !appendOutput ) {
    	        		// The table needs to be created because it does not exist or NOT appending (so need new table):
    	    			// - the table columns depend on the S3 command being executed
    	    			// 1. Define the column names based on S3 commands.
    	        		List<TableField> columnList = new ArrayList<>();
    	        		if ( googleDriveCommand == GoogleDriveCommandType.LIST_DRIVES ) {
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "DriveName", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_DATETIME, "CreationDate", -1) );
    	        		}
    	        		else if ( googleDriveCommand == GoogleDriveCommandType.LIST ) {
    	        			// List in order that makes sense (not alphabetical).
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Id", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Name", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "ParentFolder", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "ParentFolderId", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Type", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_LONG, "Size", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Owner", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_BOOLEAN, "OwnedByMe", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_DATETIME, "CreationTime", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_BOOLEAN, "Shared", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "SharingUser", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_DATETIME, "SharedWithMeTime", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_DATETIME, "LastModifiedTime", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "LastModifiedUser", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_BOOLEAN, "Trashed", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "TrashedUser", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_DATETIME, "TrashedTime", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "OriginalFilename", -1) );
    	        			columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "WebViewLink", -1) );
    	        		}
    	        		// 2. Create the table if not found from the processor above.
    	        		if ( (googleDriveCommand == GoogleDriveCommandType.LIST_DRIVES) ||
    	        			(googleDriveCommand == GoogleDriveCommandType.LIST) ) {
    	        			// Create the table.
    	        			table = new DataTable( columnList );
    	        		}
                		// 3. Get the column numbers from the names for later use.
    	        		if ( googleDriveCommand == GoogleDriveCommandType.LIST_DRIVES ) {
    	        			driveNameCol = table.getFieldIndex("DriveName");
    	        			driveCreationDateCol = table.getFieldIndex("CreationDate");
    	        		}
    	        		else if ( googleDriveCommand == GoogleDriveCommandType.LIST ) {
    	        			// List in the same order as "add" calls above.
    	        			listIdCol = table.getFieldIndex("Id");
    	        			listNameCol = table.getFieldIndex("Name");
    	        			listParentFolderCol = table.getFieldIndex("ParentFolder");
    	        			listParentFolderIdCol = table.getFieldIndex("ParentFolderId");
    	        			listTypeCol = table.getFieldIndex("Type");
    	        			listSizeCol = table.getFieldIndex("Size");
    	        			listOwnerCol = table.getFieldIndex("Owner");
    	        			listOwnedByMeCol = table.getFieldIndex("OwnedByMe");
    	        			listCreationTimeCol = table.getFieldIndex("CreationTime");
    	        			listSharedCol = table.getFieldIndex("Shared");
    	        			listSharingUserCol = table.getFieldIndex("SharingUser");
    	        			listSharedWithMeTimeCol = table.getFieldIndex("SharedWithMeTime");
    	        			listLastModifiedTimeCol = table.getFieldIndex("LastModifiedTime");
    	        			listLastModifiedUserCol = table.getFieldIndex("LastModifiedUser");
    	        			listTrashedCol = table.getFieldIndex("Trashed");
    	        			listTrashedUserCol = table.getFieldIndex("TrashedUser");
    	        			listTrashedTimeCol = table.getFieldIndex("TrashedTime");
    	        			listOriginalFilenameCol = table.getFieldIndex("OriginalFilename");
    	        			listWebViewLinkCol = table.getFieldIndex("WebViewLink");
    	        		}
    	        		// 4. Set the table in the processor:
    	        		//    - if new will add
    	        		//    - if append will overwrite by replacing the matching table ID
    	        		if ( (googleDriveCommand == GoogleDriveCommandType.LIST_DRIVES) ||
    	        			(googleDriveCommand == GoogleDriveCommandType.LIST) ) {
    	        			if ( (OutputTableID != null) && !OutputTableID.isEmpty() ) {
    	        				table.setTableID ( OutputTableID );
                				Message.printStatus(2, routine, "Created new table \"" + OutputTableID + "\" for output.");
                				// Set the table in the processor:
                				// - do not set if a temporary table is being used for the output file
                				PropList requestParams = new PropList ( "" );
                				requestParams.setUsingObject ( "Table", table );
                				try {
                    				processor.processRequest( "SetTable", requestParams);
                				}
                				catch ( Exception e ) {
                    				message = "Error requesting SetTable(Table=...) from processor.";
                    				Message.printWarning(warningLevel,
                        				MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
                    				status.addToLog ( commandPhase,
                        				new CommandLogRecord(CommandStatusType.FAILURE,
                           				message, "Report problem to software support." ) );
                				}
    	        			}
    	        			else {
    	        				// Temporary table used for file only:
    	        				// - do not set in the processor
    	        				table.setTableID ( "GoogleDrive" );
    	        			}
    	        		}
    	        		// 5. The table contents will be filled in when the doS3* methods are called.
    	    		}
    	    		else {
    	    			// Table exists:
    	        		// - make sure that the needed columns exist and otherwise add them
    	        		if ( googleDriveCommand == GoogleDriveCommandType.LIST_DRIVES ) {
    	        			driveNameCol = table.getFieldIndex("DriveName");
    	        			driveCreationDateCol = table.getFieldIndex("CreationDate");
    	        			if ( driveNameCol < 0 ) {
    	            			driveNameCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "DriveName", -1), "");
    	        			}
    	        			if ( driveCreationDateCol < 0 ) {
    	            			driveCreationDateCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "CreationDate", -1), "");
    	        			}
    	        		}
    	        		else if ( googleDriveCommand == GoogleDriveCommandType.LIST ) {
    	        			// List in the order that columns were added.
    	        			if ( listIdCol < 0 ) {
    	            			listIdCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "Id", -1), "");
    	        			}
    	        			if ( listNameCol < 0 ) {
    	            			listNameCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "Name", -1), "");
    	        			}
    	        			if ( listParentFolderCol < 0 ) {
    	            			listParentFolderCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "ParentFolder", -1), "");
    	        			}
    	        			if ( listParentFolderIdCol < 0 ) {
    	            			listParentFolderIdCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "ParentFolderId", -1), "");
    	        			}
    	        			if ( listTypeCol < 0 ) {
    	            			listTypeCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "Type", -1), "");
    	        			}
    	        			if ( listSizeCol < 0 ) {
    	            			listSizeCol = table.addField(new TableField(TableField.DATA_TYPE_LONG, "Size", -1), "");
    	        			}
    	        			if ( listOwnerCol < 0 ) {
    	            			listOwnerCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "Owner", -1), "");
    	        			}
    	        			if ( listOwnedByMeCol < 0 ) {
    	            			listOwnedByMeCol = table.addField(new TableField(TableField.DATA_TYPE_BOOLEAN, "OwnedByMe", -1), "");
    	        			}
    	        			if ( listCreationTimeCol < 0 ) {
    	            			listCreationTimeCol = table.addField(new TableField(TableField.DATA_TYPE_DATETIME, "CreationTime", -1), "");
    	        			}
    	        			if ( listSharedCol < 0 ) {
    	            			listSharedCol = table.addField(new TableField(TableField.DATA_TYPE_BOOLEAN, "Shared", -1), "");
    	        			}
    	        			if ( listSharingUserCol < 0 ) {
    	            			listSharingUserCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "SharingUser", -1), "");
    	        			}
    	        			if ( listSharedWithMeTimeCol < 0 ) {
    	            			listSharedWithMeTimeCol = table.addField(new TableField(TableField.DATA_TYPE_DATETIME, "SharedWithMeTime", -1), "");
    	        			}
    	        			if ( listLastModifiedTimeCol < 0 ) {
    	            			listLastModifiedTimeCol = table.addField(new TableField(TableField.DATA_TYPE_DATETIME, "LastModifiedTime", -1), "");
    	        			}
    	        			if ( listLastModifiedUserCol < 0 ) {
    	            			listLastModifiedUserCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "LastModifiedUser", -1), "");
    	        			}
    	        			if ( listTrashedCol < 0 ) {
    	            			listTrashedCol = table.addField(new TableField(TableField.DATA_TYPE_BOOLEAN, "Trashed", -1), "");
    	        			}
    	        			if ( listTrashedUserCol < 0 ) {
    	            			listTrashedUserCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "TrashedUser", -1), "");
    	        			}
    	        			if ( listTrashedTimeCol < 0 ) {
    	            			listTrashedTimeCol = table.addField(new TableField(TableField.DATA_TYPE_DATETIME, "TrashedTime", -1), "");
    	        			}
    	        			if ( listOriginalFilenameCol < 0 ) {
    	            			listOriginalFilenameCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "OriginalFilename", -1), "");
    	        			}
    	        			if ( listWebViewLinkCol < 0 ) {
    	            			listWebViewLinkCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "WebViewLink", -1), "");
    	        			}
    	        		}
    	        	}
    	    	}

    	    	// Call the service that was requested to create the requested output.
   	    		// S3Client:
   	    		//    https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/S3Client.html

	    		/*
    	    	if ( s3Command == AwsS3CommandType.COPY_OBJECTS ) {
    	    		warningCount = doS3CopyObjects(
    	    			processor,
		  	    		s3,
		  	    		bucket, copyFilesSourceKeyList, copyBucket, copyFilesDestKeyList,
  	    				CopyObjectsCountProperty,
		  	    		invalidateCloudFront, cloudFrontPaths,
		  	    		status, logLevel, warningCount, commandTag );
    	    	}
    	    	else if ( s3Command == AwsS3CommandType.DELETE_OBJECTS ) {
    	    		warningCount = doS3DeleteObjects (
    	    			s3,
		  	    		bucket,
   	        			deleteFilesKeys, deleteFoldersKeys,
   	        			DeleteFoldersScope, deleteFoldersMinDepth,
		  	    		invalidateCloudFront, cloudFrontPaths,
		  	    		status, logLevel, warningLevel, warningCount, commandTag );
    	    	}
    	    	*/
    	    	else if ( googleDriveCommand == GoogleDriveCommandType.DOWNLOAD ) {
    	    		warningCount = doGoogleDriveDownload (
    	    			processor,
    	    			googleDriveSession,
    	    			downloadFilesGoogleDrivePaths, downloadFilesFiles,
    	    			downloadFoldersGoogleDrivePaths, downloadFoldersDirectories,
    	    			status, logLevel, warningLevel, warningCount, commandTag );
    	    	}
	    		/*
    	    	else if ( s3Command == AwsS3CommandType.LIST_BUCKETS ) {
    	    		warningCount = doS3ListBuckets (
    	    			processor,
    	    			s3,
    	    			table, bucketNameCol, bucketCreationDateCol,
    	    			listBucketsRegEx, ListBucketsCountProperty,
    	    			status, logLevel, warningCount, commandTag );
    	    	}
    	    	*/
   	        	if ( googleDriveCommand == GoogleDriveCommandType.LIST ) {
   	        		if ( !ListShared.equalsIgnoreCase(_Only) ) {
   	        			// Get the list of files and folders that is not shared:
   	        			// - list these before shared files
   	        			// - the shared files will be queried below and appended
   	        			// - if "Only" was specified then this list is skipped
   	        			boolean listShared = false;
   	        			warningCount = doGoogleDriveList (
   	        				processor,
    			      		googleDriveSession,
    			      		ListScope, ListFolderPath, listFiles, listFolders, listShared, listTrashed, listRegEx,
    			      		table,
    			      		listCreationTimeCol, listIdCol,
    			      		listLastModifiedTimeCol, listLastModifiedUserCol,
    			      		listMax,
    			      		listNameCol, listOriginalFilenameCol, listOwnedByMeCol, listOwnerCol,
    			      		listParentFolderCol, listParentFolderIdCol,
    			      		listSharedCol, listSharedWithMeTimeCol, listSharingUserCol,
    			      		listSizeCol,
    			      		listTrashedCol, listTrashedTimeCol, listTrashedUserCol,
    			      		listTypeCol, listWebViewLinkCol,
    			      		ListCountProperty,
    			      		status, logLevel, warningLevel, warningCount, commandTag );
   	        		}
   	        		if ( ListShared.equalsIgnoreCase(_Only) || ListShared.equalsIgnoreCase(_True) ) {
   	        			// Get the list of shared files and folders:
   	        			// - will append to the above list
   	        			boolean listShared = true;
   	        			warningCount = doGoogleDriveList (
   	        				processor,
    			      		googleDriveSession,
    			      		ListScope, ListFolderPath, listFiles, listFolders, listShared, listTrashed, listRegEx,
    			      		table,
    			      		listCreationTimeCol, listIdCol, listLastModifiedTimeCol, listLastModifiedUserCol,
    			      		listMax,
    			      		listNameCol, listOriginalFilenameCol, listOwnedByMeCol, listOwnerCol,
    			      		listParentFolderCol, listParentFolderIdCol,
    			      		listSharedCol, listSharedWithMeTimeCol, listSharingUserCol,
    			      		listSizeCol,
    			      		listTrashedCol, listTrashedTimeCol, listTrashedUserCol,
    			      		listTypeCol, listWebViewLinkCol,
    			      		ListCountProperty,
    			      		status, logLevel, warningLevel, warningCount, commandTag );
   	        		}
    	    	}
   	        	/*
   	        	else if ( s3Command == AwsS3CommandType.UPLOAD_OBJECTS ) {
   	        		warningCount = doS3UploadObjects (
   	        			processor,
   	        			credentialsProvider,
   	        			bucket, region,
   	        			uploadFilesOrig, uploadFilesFileList, uploadFilesKeyList,
   	        			uploadFoldersOrig, uploadFoldersDirectoryList, uploadFoldersKeyList,
		  	    		invalidateCloudFront, cloudFrontPaths,
   	        			status, logLevel, warningLevel, warningCount, commandTag );
    	    	}
   	        	else {
					message = "Unknown S3 command: " + S3Command;
					Message.printWarning(warningLevel,
						MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
					status.addToLog ( commandPhase,
						new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Use the command editor to select an S3 command." ) );
   	        	}
   	        	*/

	        	// Create the output file:
	    	   	// - write the table to a delimited file
	    	   	// - TODO smalers 2023-01-28 for now do not write comments, keep very basic

	    	   	if ( doOutputFile ) {
	    		   	String OutputFile_full = IOUtil.verifyPathForOS(
	        		   	IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	            		   	TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
	    		   	if ( OutputFile_full.toUpperCase().endsWith("CSV") ) {
	    			   	boolean writeColumnNames = true;
	    			   	List<String> comments = null;
	    			   	String commentLinePrefix = "#";
	    			   	HashMap<String,Object> writeProps = new HashMap<>();
	    			   	if ( appendOutput && ((OutputTableID == null) || OutputTableID.isEmpty()) ) {
	    			   		// Requested append but the output table was not given:
	    			   		// - therefore the output table was a temporary table
	    			   		// - the output is only for this command so must append to the file (if it exists)
	    				   	writeProps.put("Append", "True");
	    			   	}
	    			   	table.writeDelimitedFile(OutputFile_full, ",", writeColumnNames, comments, commentLinePrefix, writeProps);
	           			setOutputFile(new File(OutputFile_full));
	    		   	}
	    		   	// TODO smalers 2023-01-31 need to implement.
	    		   	//else if ( OutputFile_full.toUpperCase().endsWith("JSON") ) {
	    		   	//}
	    		   	else {
                	   	message = "Requested output file has unknown extension - don't know how to write.";
                	   	Message.printWarning(warningLevel,
                		   	MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
                	   	status.addToLog ( commandPhase,
                		   	new CommandLogRecord(CommandStatusType.FAILURE,
                		   	message, "Use an output file with 'csv' file extension." ) );
	    		   	}
	    	   	}
	    	}
	    	else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
   	        	if ( (googleDriveCommand == GoogleDriveCommandType.LIST_DRIVES) ||
   	        		(googleDriveCommand == GoogleDriveCommandType.LIST) ) {
   	        		if ( (table == null) && (OutputTableID != null) && !OutputTableID.isEmpty() ) {
	               		// Did not find table so is being created in this command.
	               		// Create an empty table and set the ID.
	               		table = new DataTable();
	               		table.setTableID ( OutputTableID );
	           		}
	           		setDiscoveryTable ( table );
   	        	}
	    	}

		}
		/*
    	catch ( S3Exception e ) {
  	    	if ( s3Command == AwsS3CommandType.COPY_OBJECTS ) {
				message = "Unexpected error copying objects (" + e.awsErrorDetails().errorMessage() + ").";
			}
  	    	else if ( s3Command == AwsS3CommandType.DELETE_OBJECTS ) {
				message = "Unexpected error deleting object (" + e.awsErrorDetails().errorMessage() + ").";
			}
  	    	else if ( s3Command == AwsS3CommandType.DOWNLOAD_OBJECTS ) {
				message = "Unexpected error downloading object(s) (" + e.awsErrorDetails().errorMessage() + ").";
			}
  	    	else if ( s3Command == AwsS3CommandType.LIST_BUCKETS ) {
				message = "Unexpected error listing buckets (" + e.awsErrorDetails().errorMessage() + ").";
			}
        	else if ( s3Command == AwsS3CommandType.LIST_OBJECTS ) {
				message = "Unexpected error listing bucket objects (" + e.awsErrorDetails().errorMessage() + ").";
        	}
  	    	else if ( s3Command == AwsS3CommandType.UPLOAD_OBJECTS ) {
				message = "Unexpected error uploading object(s) (" + e.awsErrorDetails().errorMessage() + ").";
			}
			else {
				message = "Unexpected error for unknown S3 command (" + e.awsErrorDetails().errorMessage() + ": " + S3Command;
			}
			Message.printWarning ( warningLevel,
				MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
			Message.printWarning ( 3, routine, e );
			status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "See the log file for details."));
			throw new CommandException ( message );
    	}
    	*/
    	catch ( Exception e ) {
    		/*
  	    	if ( s3Command == AwsS3CommandType.COPY_OBJECTS ) {
				message = "Unexpected error copying objects (" + e + ").";
			}
  	    	else if ( s3Command == AwsS3CommandType.DELETE_OBJECTS ) {
				message = "Unexpected error deleting object (" + e + ").";
			}
			*/
  	    	if ( googleDriveCommand == GoogleDriveCommandType.DOWNLOAD ) {
				message = "Unexpected error downloading files (" + e + ").";
			}
  	    	/*
  	    	else if ( s3Command == AwsS3CommandType.LIST_BUCKETS ) {
				message = "Unexpected error listing buckets (" + e + ").";
			}
			*/
        	if ( googleDriveCommand == GoogleDriveCommandType.LIST ) {
				message = "Unexpected error listing Google Drive (" + e + ").";
        	}
        	/*
  	    	else if ( s3Command == AwsS3CommandType.UPLOAD_OBJECTS ) {
				message = "Unexpected error uploading objects (" + e + ").";
			}
			*/
			else {
				message = "Unexpected error for unknown Google Drive command: " + googleDriveCommand;
			}
			Message.printWarning ( warningLevel,
				MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
			Message.printWarning ( 3, routine, e );
			status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "See the log file for details."));
			throw new CommandException ( message );
		}

    	if ( warningCount > 0 ) {
        	message = "There were " + warningCount + " warnings processing the command.";
        	Message.printWarning ( warningLevel,
            	MessageUtil.formatMessageTag(
            	commandTag, ++warningCount),
            	routine,message);
        	throw new CommandWarningException ( message );
    	}

		status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
	}

	/**
	Set the output file that is created by this command.  This is only used internally.
	@param file the output file created by this command
	*/
	private void setOutputFile ( File file ) {
    	__OutputFile_File = file;
	}

	/**
	Set the table that is read by this class in discovery mode.
	@param table the output table created by this command
	*/
	private void setDiscoveryTable ( DataTable table ) {
    	this.discoveryOutputTable = table;
	}

	/**
	Return the string representation of the command.
	@param parameters to include in the command
	@return the string representation of the command
	*/
	public String toString ( PropList parameters ) {
		String [] parameterOrder = {
			// General.
			"SessionID",
			"AuthenticationMethod",
			"GoogleDriveCommand",
			//"Profile",
			//"Region",
			//"Bucket",
			// Copy.
			//"CopyFiles",
			//"CopyBucket",
			//"CopyObjectsCountProperty",
			// Delete.
			//"DeleteFiles",
			//"DeleteFolders",
			//"DeleteFoldersScope",
			//"DeleteFoldersMinDepth",
			// Download.
			"DownloadFolders",
			"DownloadFiles",
			// List buckets.
			//"ListBucketsRegEx",
			//"ListBucketsCountProperty",
			// List files.
			"ListScope",
			"ListFolderPath",
			"ListRegEx",
			"ListFiles",
			"ListFolders",
			"ListShared",
			"ListTrashed",
			"ListMax",
			"ListCountProperty",
			// Upload.
			//"UploadFolders",
			//"UploadFiles",
			// Output.
			"OutputTableID",
			"OutputFile",
			"AppendOutput",
			"IfInputNotFound"
		};
		return this.toString(parameters, parameterOrder);
	}

}