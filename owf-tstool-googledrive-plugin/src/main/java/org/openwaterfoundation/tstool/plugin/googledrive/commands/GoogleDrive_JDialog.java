// GoogleDrive_JDialog - editor for GoogleDrive command

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
    along with OWF TSTool Google Plugin.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package org.openwaterfoundation.tstool.plugin.googledrive.commands;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openwaterfoundation.tstool.plugin.googledrive.GoogleDriveAuthenticationMethodType;
import org.openwaterfoundation.tstool.plugin.googledrive.GoogleDriveSession;
import org.openwaterfoundation.tstool.plugin.googledrive.PluginMeta;

import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

@SuppressWarnings("serial")
public class GoogleDrive_JDialog extends JDialog
implements ActionListener, ChangeListener, ItemListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browseOutput_JButton = null;
private SimpleJButton __pathOutput_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private JTabbedPane __main_JTabbedPane = null;
private JLabel sessionProblem_JLabel = null;
private JLabel sessionRecommendation_JLabel = null;
private JTextField __SessionID_JTextField = null;
private SimpleJComboBox __AuthenticationMethod_JComboBox = null;
private SimpleJComboBox __GoogleDriveCommand_JComboBox = null;
// Read-only note about whether the authentication credentials are OK.
private JTextField __CredentialsStatus_JTextField = null;
//private SimpleJComboBox __IfInputNotFound_JComboBox = null;

// Copy tab.
/*
private JTextArea __CopyFiles_JTextArea = null;
private SimpleJComboBox __CopyBucket_JComboBox = null;
private JTextField __CopyObjectsCountProperty_JTextField = null;
*/

// Delete tab.
/*
private JTextArea __DeleteFiles_JTextArea = null;
private JTextArea __DeleteFolders_JTextArea = null;
private SimpleJComboBox __DeleteFoldersScope_JComboBox = null;
private JTextField __DeleteFoldersMinDepth_JTextField = null;
*/

// Download tab.
private JTextArea __DownloadFiles_JTextArea = null;
private JTextArea __DownloadFolders_JTextArea = null;
private JTextField __DownloadCountProperty_JTextField = null;

// List Drives tab.
private JTextField __ListDrivesRegEx_JTextField = null;
private JTextField __ListDrivesCountProperty_JTextField = null;

// List tab.
private SimpleJComboBox __ListScope_JComboBox = null;
private JTextField __ListFolderPath_JTextField = null;
private JTextField __ListRegEx_JTextField = null;
private SimpleJComboBox __ListFiles_JComboBox = null;
private SimpleJComboBox __ListFolders_JComboBox = null;
private SimpleJComboBox __ListSharedWithMe_JComboBox = null;
private SimpleJComboBox __ListTrashed_JComboBox = null;
private JTextField __ListMax_JTextField = null;
private JTextField __ListCountProperty_JTextField = null;

// Upload tab.
/*
private JTextArea __UploadFiles_JTextArea = null;
private JTextArea __UploadFolders_JTextArea = null;
*/

// Output tab.
private SimpleJComboBox __OutputTableID_JComboBox = null;
private JTextField __OutputFile_JTextField = null;
private SimpleJComboBox __AppendOutput_JComboBox = null;

private JTextArea __command_JTextArea = null;
private String __working_dir = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private GoogleDrive_Command __command = null;
private boolean __ok = false; // Whether the user has pressed OK to close the dialog.
private boolean ignoreEvents = false; // Ignore events when initializing, to avoid infinite loop.
private JFrame __parent = null;

/**
 * Google Drive session used to interact with services.
 */
private GoogleDriveSession googleDriveSession = null;

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of tables to choose from, used if appending
*/
public GoogleDrive_JDialog ( JFrame parent, GoogleDrive_Command command, List<String> tableIDChoices ) {
	super(parent, true);
	initialize ( parent, command, tableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event ) {
	String routine = getClass().getSimpleName() + ".actionPeformed";
	if ( this.ignoreEvents ) {
        return; // Startup.
    }

	Object o = event.getSource();

    if ( o == this.__AuthenticationMethod_JComboBox ) {
    	// Create the session to check authentication:
    	// - only called if ItemListener is enabled.
    	createGoogleDriveSession();
    }
    else if ( o == this.__GoogleDriveCommand_JComboBox ) {
    	setTabForGoogleDriveCommand();
    }
    else if ( o == __browseOutput_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
        }
        fc.setDialogTitle( "Select Output File");

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String directory = fc.getSelectedFile().getParent();
            String filename = fc.getSelectedFile().getName();
            String path = fc.getSelectedFile().getPath();

            if (filename == null || filename.equals("")) {
                return;
            }

            if (path != null) {
				// Convert path to relative path by default.
				try {
					__OutputFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, routine, "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory);
                refresh();
            }
        }
    }
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
    /*
    else if ( event.getActionCommand().equalsIgnoreCase("EditCopyFiles") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String CopyFiles = __CopyFiles_JTextArea.getText().trim();
        String [] notes = {
        	"Copy S3 file objects by specifying source and destination keys for bucket:  " + this.__Bucket_JComboBox.getSelected(),
            "Specify the S3 object key using a path (e.g., folder1/folder2/file.ext).",
            "The S3 destination will be created if it does not exist, or overwritten if it does exist.",
            "A leading / in the S3 bucket key is required only if the bucket uses a top-level / in object keys.",
            "${Property} notation can be used for all values to expand at run time."
        };
        String dict = (new DictionaryJDialog ( __parent, true, CopyFiles,
            "Edit CopyFiles Parameter", notes, "Source S3 Key (Path)", "Destination S3 Key (Path)", 10)).response();
        if ( dict != null ) {
            __CopyFiles_JTextArea.setText ( dict );
            refresh();
        }
    }
    */
    /*
    else if ( event.getActionCommand().equalsIgnoreCase("EditDeleteFolders") ) {
        // Edit the list in the dialog.  It is OK for the string to be blank.
        String DeleteFolders = __DeleteFolders_JTextArea.getText().trim();
        String [] notes = {
            "Specify the paths ending in / for folders to delete.  Use the S3 Browser in the command editor to view folders and their keys.",
            "All files in the folder and the folder itself will be deleted.  This requires doing a folder listing first.",
            "Do not specify a leading / unless the key actually contains a starting / (default for S3 buckets is no leading /).",
            "${Property} notation can be used to expand at run time.",
            "Use the checkboxes with Insert and Remove.",
            "All non-blank object keys will be included in the command parameter.",
        };
        String delim = ",";
        String list = (new StringListJDialog ( __parent, true, DeleteFolders,
            "Edit DeleteFolders Parameter", notes, "Folder Key", delim, 10)).response();
        if ( list != null ) {
            __DeleteFolders_JTextArea.setText ( list );
            refresh();
        }
    }
    */
    /*
    else if ( event.getActionCommand().equalsIgnoreCase("EditDeleteFiles") ) {
        // Edit the list in the dialog.  It is OK for the string to be blank.
        String DeleteFiles = __DeleteFiles_JTextArea.getText().trim();
        String [] notes = {
            "Specify the S3 object keys for file objects to delete.  Use the S3 Browser in the command editor to view objects and their keys.",
            "Do not specify a folder key ending in / (see also the DeleteFolders command parameter).",
            "Do not specify a leading / unless the key actually contains a starting / (default for S3 buckets is no leading /).",
            "${Property} notation can be used to expand at run time.",
            "Use the checkboxes with Insert and Remove.",
            "All non-blank object keys will be included in the command parameter.",
        };
        String delim = ",";
        String list = (new StringListJDialog ( __parent, true, DeleteFiles,
            "Edit DeleteFiles Parameter", notes, "S3 File Object Key", delim, 10)).response();
        if ( list != null ) {
            __DeleteFiles_JTextArea.setText ( list );
            refresh();
        }
    }
    */
    else if ( event.getActionCommand().equalsIgnoreCase("EditDownloadFolders") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String DownloadFolders = __DownloadFolders_JTextArea.getText().trim();
        String [] notes = {
            "Specify the Google Drive folder path using one of the following forms:",
            "  /My Drive/path/to/folder/",
            "  /path/to/folder/ (handled as if My Drive were specified)",
            "  /Shared with me/path/to/folder/",
            "  /Shared drives/drivename/path/to/folder/",
            "  /id/identifier/",
            "Only folders (directories) can be downloaded. Specify files to download with the 'DownloadFiles' command parameter.",
            "Leading and trailing / are required for consistency.",
            "The local folder is relative to the working folder.",
            "  " + this.__working_dir,
            "${Property} notation can be used for all values to expand at run time."
        };
        String dict = (new DictionaryJDialog ( __parent, true, DownloadFolders,
            "Edit DownloadFolders Parameter", notes, "Google Drive Folder Path", "Local Folder (optionally ending in /)",10)).response();
        if ( dict != null ) {
            __DownloadFolders_JTextArea.setText ( dict );
            refresh();
        }
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditDownloadFiles") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String DownloadFiles = __DownloadFiles_JTextArea.getText().trim();
        String [] notes = {
            "Specify the Google Drive folder path using one of the following forms:",
            "  /My Drive/path/to/file",
            "  /path/to/file (handled as if My Drive were specified)",
            "  /Shared with me/path/to/file",
            "  /Shared drives/drivename/path/to/file",
            "  /id/identifier",
            "Only files can be downloaded. Specify folders to download with the 'DownloadFolders' command parameter.",
            "A leading / is required for consistency.",
            "The local folder is relative to the working folder.",
            "  " + this.__working_dir,
            "${Property} notation can be used for all values to expand at run time."
        };
        String dict = (new DictionaryJDialog ( __parent, true, DownloadFiles,
            "Edit DownloadFiles Parameter", notes, "Google Drive File Path", "Local File",10)).response();
        if ( dict != null ) {
            __DownloadFiles_JTextArea.setText ( dict );
            refresh();
        }
    }
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "GoogleDrive", PluginMeta.getDocumentationRootUrl());
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
    else if ( o == __pathOutput_JButton ) {
        if ( __pathOutput_JButton.getText().equals(__AddWorkingDirectory) ) {
            __OutputFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__OutputFile_JTextField.getText() ) );
        }
        else if ( __pathOutput_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            try {
                __OutputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
                        __OutputFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1,"AwsS3_JDialog",
                "Error converting output file name to relative path." );
            }
        }
        refresh ();
    }
    /*
    else if ( event.getActionCommand().equalsIgnoreCase("EditUploadFolders") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String UploadFolders = __UploadFolders_JTextArea.getText().trim();
        String [] notes = {
        	"Upload local folders (and all subfolders and files in each folder) to S3 bucket:  " + this.__Bucket_JComboBox.getSelected(),
            "The local folder is relative to the working folder:",
            "    " + this.__working_dir,
            "Specify the S3 folder using a path ending in / (e.g., topfolder/childfolder/).",
            "The S3 location will be created if it does not exist, or overwritten if it does exist.",
            "Only folders (directories) can be specified. Specify files to upload with the 'UploadFiles' command parameter.",
            "All files in the folders are uploaded, resulting in corresponding virtual folders in S3.",
            "A leading / in the S3 bucket folder is required only if the bucket uses a top-level / in object keys.",
            "${Property} notation can be used for all values to expand at run time."
        };
        String dict = (new DictionaryJDialog ( __parent, true, UploadFolders,
            "Edit UploadFolders Parameter", notes, "Local Folder (optionally ending in /)", "S3 Folder Path (ending in /)", 10)).response();
        if ( dict != null ) {
            __UploadFolders_JTextArea.setText ( dict );
            refresh();
        }
    }
    */
    /*
    else if ( event.getActionCommand().equalsIgnoreCase("EditUploadFiles") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String UploadFiles = __UploadFiles_JTextArea.getText().trim();
        String [] notes = {
        	"Upload local files to S3 bucket:  " + this.__Bucket_JComboBox.getSelected(),
            "The local file is relative to the working folder:",
            "  " + this.__working_dir,
            "Specify the S3 bucket object key (S3 file path) to upload a file.",
            "Use * in the 'Local File' to match a pattern and /* at the end of the 'Bucket Key' to use the same file name in the S3 bucket.",
            "  For example, local=folder1/folder2/fileZ.txt and s3=foldera/folderb/* would save foldera/folderb/fileZ.txt on S3",
            "Only files can be uploaded with this parameter. Specify folders to upload with the 'UploadFolders' command parameter.",
            "The key is the full path for the bucket object.",
            "A leading / in the S3 bucket object key is required only if the bucket uses a top-level / in object keys.",
            "${Property} notation can be used for all values to expand at run time."
        };
        String dict = (new DictionaryJDialog ( __parent, true, UploadFiles,
            "Edit UploadFiles Parameter", notes, "Local File", "S3 Bucket Object Key (object path)", 10)).response();
        if ( dict != null ) {
            __UploadFiles_JTextArea.setText ( dict );
            refresh();
        }
    }
    */
	else {
		// Choices.
		refresh();
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	if ( this.ignoreEvents ) {
        return; // Startup.
    }
	// Put together a list of parameters to check.
	PropList props = new PropList ( "" );
	// General.
	String SessionID = __SessionID_JTextField.getText().trim();
	String AuthenticationMethod = __AuthenticationMethod_JComboBox.getSelected();
	String GoogleDriveCommand = __GoogleDriveCommand_JComboBox.getSelected();
	// Copy.
	//String CopyFiles = __CopyFiles_JTextArea.getText().trim().replace("\n"," ");
	//String CopyBucket = __CopyBucket_JComboBox.getSelected();
	//String CopyObjectsCountProperty = __CopyObjectsCountProperty_JTextField.getText().trim();
	// Delete.
	//String DeleteFiles = __DeleteFiles_JTextArea.getText().trim().replace("\n"," ");
	//String DeleteFolders = __DeleteFolders_JTextArea.getText().trim().replace("\n"," ");
	//String DeleteFoldersScope = __DeleteFoldersScope_JComboBox.getSelected();
	//String DeleteFoldersMinDepth = __DeleteFoldersMinDepth_JTextField.getText().trim();
	// Download.
	String DownloadFolders = __DownloadFolders_JTextArea.getText().trim().replace("\n"," ");
	String DownloadFiles = __DownloadFiles_JTextArea.getText().trim().replace("\n"," ");
	String DownloadCountProperty = __DownloadCountProperty_JTextField.getText().trim();
	// List drives.
	String ListDrivesRegEx = __ListDrivesRegEx_JTextField.getText().trim();
	String ListDrivesCountProperty = __ListDrivesCountProperty_JTextField.getText().trim();
	// List.
	String ListScope = __ListScope_JComboBox.getSelected();
	String ListFolderPath = __ListFolderPath_JTextField.getText().trim();
	String ListRegEx = __ListRegEx_JTextField.getText().trim();
	String ListFiles = __ListFiles_JComboBox.getSelected();
	String ListFolders = __ListFolders_JComboBox.getSelected();
	String ListSharedWithMe = __ListSharedWithMe_JComboBox.getSelected();
	String ListTrashed = __ListTrashed_JComboBox.getSelected();
	String ListMax = __ListMax_JTextField.getText().trim();
	String ListCountProperty = __ListCountProperty_JTextField.getText().trim();
	// Upload.
	//String UploadFolders = __UploadFolders_JTextArea.getText().trim().replace("\n"," ");
	//String UploadFiles = __UploadFiles_JTextArea.getText().trim().replace("\n"," ");
	// Output.
	String OutputTableID = __OutputTableID_JComboBox.getSelected();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String AppendOutput = __AppendOutput_JComboBox.getSelected();
	__error_wait = false;

	if ( (SessionID != null) && !SessionID.isEmpty() ) {
		props.set ( "SessionID", SessionID );
	}
	if ( (AuthenticationMethod != null) && !AuthenticationMethod.isEmpty() ) {
		props.set ( "AuthenticationMethod", AuthenticationMethod );
	}
	if ( (GoogleDriveCommand != null) && !GoogleDriveCommand.isEmpty() ) {
		props.set ( "GoogleDriveCommand", GoogleDriveCommand );
	}
	/*
	// Copy.
	if ( (CopyFiles != null) && !CopyFiles.isEmpty() ) {
		props.set ( "CopyFiles", CopyFiles );
	}
	if ( (CopyBucket != null) && !CopyBucket.isEmpty() ) {
		props.set ( "CopyBucket", CopyBucket );
	}
	if ( (CopyObjectsCountProperty != null) && !CopyObjectsCountProperty.isEmpty() ) {
		props.set ( "CopyObjectsCountProperty", CopyObjectsCountProperty );
	}
	// Delete.
	if ( (DeleteFiles != null) && !DeleteFiles.isEmpty() ) {
		props.set ( "DeleteFiles", DeleteFiles );
	}
	if ( (DeleteFolders != null) && !DeleteFolders.isEmpty() ) {
		props.set ( "DeleteFolders", DeleteFolders );
	}
	if ( (DeleteFoldersScope != null) && !DeleteFoldersScope.isEmpty() ) {
		props.set ( "DeleteFoldersScope", DeleteFoldersScope );
	}
	if ( (DeleteFoldersMinDepth != null) && !DeleteFoldersMinDepth.isEmpty() ) {
		props.set ( "DeleteFoldersMinDepth", DeleteFoldersMinDepth );
	}
	*/
	// Download.
	if ( (DownloadFolders != null) && !DownloadFolders.isEmpty() ) {
		props.set ( "DownloadFolders", DownloadFolders );
	}
	if ( (DownloadFiles != null) && !DownloadFiles.isEmpty() ) {
		props.set ( "DownloadFiles", DownloadFiles );
	}
	if ( (DownloadCountProperty != null) && !DownloadCountProperty.isEmpty() ) {
		props.set ( "DownloadCountProperty", DownloadCountProperty );
	}
	// List drives.
	if ( (ListDrivesRegEx != null) && !ListDrivesRegEx.isEmpty() ) {
		props.set ( "ListDrivesRegEx", ListDrivesRegEx );
	}
	if ( (ListDrivesCountProperty != null) && !ListDrivesCountProperty.isEmpty() ) {
		props.set ( "ListDrivesCountProperty", ListDrivesCountProperty );
	}
	// List.
	if ( (ListScope != null) && !ListScope.isEmpty() ) {
		props.set ( "ListScope", ListScope);
	}
	if ( (ListFolderPath != null) && !ListFolderPath.isEmpty() ) {
		props.set ( "ListFolderPath", ListFolderPath );
	}
	if ( (ListRegEx != null) && !ListRegEx.isEmpty() ) {
		props.set ( "ListRegEx", ListRegEx );
	}
	if ( (ListFiles != null) && !ListFiles.isEmpty() ) {
		props.set ( "ListFiles", ListFiles );
	}
	if ( (ListFolders != null) && !ListFolders.isEmpty() ) {
		props.set ( "ListFolders", ListFolders );
	}
	if ( (ListSharedWithMe != null) && !ListSharedWithMe.isEmpty() ) {
		props.set ( "ListSharedWithMe", ListSharedWithMe );
	}
	if ( (ListTrashed != null) && !ListTrashed.isEmpty() ) {
		props.set ( "ListTrashed", ListTrashed );
	}
	if ( (ListMax != null) && !ListMax.isEmpty() ) {
		props.set ( "ListMax", ListMax );
	}
	if ( (ListCountProperty != null) && !ListCountProperty.isEmpty() ) {
		props.set ( "ListCountProperty", ListCountProperty );
	}
	// Upload.
	/*
	if ( (UploadFolders != null) && !UploadFolders.isEmpty() ) {
		props.set ( "UploadFolders", UploadFolders );
	}
	if ( (UploadFiles != null) && !UploadFiles.isEmpty() ) {
		props.set ( "UploadFiles", UploadFiles );
	}
	*/
	// Output.
    if ( (OutputTableID != null) && !OutputTableID.isEmpty() ) {
        props.set ( "OutputTableID", OutputTableID );
    }
    if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
        props.set ( "OutputFile", OutputFile );
    }
    if ( (AppendOutput != null) && !AppendOutput.isEmpty() ) {
        props.set ( "AppendOutput", AppendOutput );
    }
    /*
	if ( IfInputNotFound.length() > 0 ) {
		props.set ( "IfInputNotFound", IfInputNotFound );
	}
	*/
	try {
		// This will warn the user.
		__command.checkCommandParameters ( props, null, 1 );
	}
	catch ( Exception e ) {
		// The warning would have been printed in the check code.
		__error_wait = true;
	}
}

/**
Commit the edits to the command.
In this case the command parameters have already been checked and no errors were detected.
*/
private void commitEdits () {
	// General.
	String SessionID = __SessionID_JTextField.getText().trim();
	String AuthenticationMethod = __AuthenticationMethod_JComboBox.getSelected();
	String GoogleDriveCommand = __GoogleDriveCommand_JComboBox.getSelected();
	// Copy.
	//String CopyFiles = __CopyFiles_JTextArea.getText().trim().replace("\n"," ");
	//String CopyBucket = __CopyBucket_JComboBox.getSelected();
	//String CopyObjectsCountProperty = __CopyObjectsCountProperty_JTextField.getText().trim();
	// Delete.
	//String DeleteFiles = __DeleteFiles_JTextArea.getText().trim().replace("\n"," ");
	//String DeleteFolders = __DeleteFolders_JTextArea.getText().trim().replace("\n"," ");
	//String DeleteFoldersScope = __DeleteFoldersScope_JComboBox.getSelected();
	//String DeleteFoldersMinDepth = __DeleteFoldersMinDepth_JTextField.getText().trim();
	// Download.
	String DownloadFolders = __DownloadFolders_JTextArea.getText().trim().replace("\n"," ");
	String DownloadFiles = __DownloadFiles_JTextArea.getText().trim().replace("\n"," ");
	String DownloadCountProperty = __DownloadCountProperty_JTextField.getText().trim();
	// List drives.
	String ListDrivesRegEx = __ListDrivesRegEx_JTextField.getText().trim();
	String ListDrivesCountProperty = __ListDrivesCountProperty_JTextField.getText().trim();
	// List.
	String ListScope = __ListScope_JComboBox.getSelected();
	String ListFolderPath = __ListFolderPath_JTextField.getText().trim();
	String ListRegEx = __ListRegEx_JTextField.getText().trim();
	String ListFiles = __ListFiles_JComboBox.getSelected();
	String ListFolders = __ListFolders_JComboBox.getSelected();
	String ListSharedWithMe = __ListSharedWithMe_JComboBox.getSelected();
	String ListTrashed = __ListTrashed_JComboBox.getSelected();
	String ListMax = __ListMax_JTextField.getText().trim();
	String ListCountProperty = __ListCountProperty_JTextField.getText().trim();
	// Upload.
	//String UploadFolders = __UploadFolders_JTextArea.getText().trim().replace("\n"," ");
	//String UploadFiles = __UploadFiles_JTextArea.getText().trim().replace("\n"," ");
	// Output
	String OutputTableID = __OutputTableID_JComboBox.getSelected();
    String OutputFile = __OutputFile_JTextField.getText().trim();
	String AppendOutput = __AppendOutput_JComboBox.getSelected();

    // General.
	__command.setCommandParameter ( "SessionID", SessionID );
	__command.setCommandParameter ( "AuthenticationMethod", AuthenticationMethod );
	__command.setCommandParameter ( "GoogleDriveCommand", GoogleDriveCommand );
	// Copy.
	//__command.setCommandParameter ( "CopyFiles", CopyFiles );
	//__command.setCommandParameter ( "CopyBucket", CopyBucket );
	//__command.setCommandParameter ( "CopyObjectsCountProperty", CopyObjectsCountProperty );
	// Delete.
	//__command.setCommandParameter ( "DeleteFiles", DeleteFiles );
	//__command.setCommandParameter ( "DeleteFolders", DeleteFolders );
	//__command.setCommandParameter ( "DeleteFoldersScope", DeleteFoldersScope );
	//__command.setCommandParameter ( "DeleteFoldersMinDepth", DeleteFoldersMinDepth );
	// Download.
	__command.setCommandParameter ( "DownloadFolders", DownloadFolders );
	__command.setCommandParameter ( "DownloadFiles", DownloadFiles );
	__command.setCommandParameter ( "DownloadCountProperty", DownloadCountProperty );
	// List drives.
	__command.setCommandParameter ( "ListDrivesRegEx", ListDrivesRegEx );
	__command.setCommandParameter ( "ListDrivesCountProperty", ListDrivesCountProperty );
	// List.
	__command.setCommandParameter ( "ListScope", ListScope );
	__command.setCommandParameter ( "ListFolderPath", ListFolderPath );
	__command.setCommandParameter ( "ListRegEx", ListRegEx );
	__command.setCommandParameter ( "ListFiles", ListFiles );
	__command.setCommandParameter ( "ListFolders", ListFolders );
	__command.setCommandParameter ( "ListSharedWithMe", ListSharedWithMe );
	__command.setCommandParameter ( "ListTrashed", ListTrashed );
	__command.setCommandParameter ( "ListMax", ListMax );
	__command.setCommandParameter ( "ListCountProperty", ListCountProperty );
	// Upload.
	//__command.setCommandParameter ( "UploadFolders", UploadFolders );
	//__command.setCommandParameter ( "UploadFiles", UploadFiles );
	// Output.
	__command.setCommandParameter ( "OutputTableID", OutputTableID );
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "AppendOutput", AppendOutput );
}

/**
 * Create the Google Drive Session, which is used to test the connection.
 * This should be called after the UI components are created to display authentication stations and problems.
 */
private void createGoogleDriveSession () {
	String routine = getClass().getSimpleName() + ".createGoogleDriveSession";
    try {
    	String SessionID = __SessionID_JTextField.getText().trim();
	   	String AuthenticationMethod = __AuthenticationMethod_JComboBox.getSelected();
		if ( (AuthenticationMethod == null) || AuthenticationMethod.isEmpty() ) {
			// Use the default.
			AuthenticationMethod = "" + GoogleDriveAuthenticationMethodType.SERVICE_ACCOUNT_KEY;
		}
		Message.printStatus(2, routine, "Creating a GoogleDriveSession for SessionID=\"" + SessionID +
			"\", AuthenticationMethod=\"" + AuthenticationMethod + "\"");
		if ( (SessionID == null) || SessionID.isEmpty() ||
			(AuthenticationMethod == null) || AuthenticationMethod.isEmpty() ) {
			// Don't have data.  Set to null.
			this.googleDriveSession = null;
			this.__CredentialsStatus_JTextField.setText("Not authenticated");
			this.sessionProblem_JLabel.setText (
				"<html><b>ERROR:  Can't check the Google Drive authentication.</b></html>" );
			this.sessionRecommendation_JLabel.setText (
				"<html><b>RECOMMENDATION:  Set the Session ID.</b></html>" );
			this.__CredentialsStatus_JTextField.setText("");
			return;
		}
		else {
			// Have data to create a session.
			GoogleDriveAuthenticationMethodType authenticationMethod =
				GoogleDriveAuthenticationMethodType.valueOfIgnoreCase(AuthenticationMethod);
			this.googleDriveSession = new GoogleDriveSession ( SessionID, authenticationMethod );
		}
    }
    catch ( Exception e ) {
    	this.googleDriveSession = null;
       	this.__CredentialsStatus_JTextField.setText("Not authenticated");
    	Message.printWarning(3, routine, "Error creating Google Drive session.");
    	Message.printWarning(3, routine, e );
    }

    // Set the UI text fields that indicate whether authentication worked.

    if ( this.googleDriveSession == null ) {
        this.sessionProblem_JLabel.setText (
        	"<html><b>ERROR: User's Google Drive configuration is invalid.</b></html>" );
    	this.sessionRecommendation_JLabel.setText (
        	"<html><b>RECOMMENDATION: Confirm that the session ID and authentication method are OK.  See the log file.</b></html>" );
       	this.__CredentialsStatus_JTextField.setText("Not authenticated");
    }
    else {
    	if ( this.googleDriveSession.isSessionAuthenticated() ) {
    		this.sessionProblem_JLabel.setText ( "The user's Google Drive configuration is authenticated." );
    		this.sessionRecommendation_JLabel.setText( "Additional checks are performed when running the command." );
        	this.__CredentialsStatus_JTextField.setText("Authenticated");
    	}
    	else {
        	this.sessionProblem_JLabel.setText (
        		"<html><b>ERROR: " + this.googleDriveSession.getProblem() + "</b></html>" );
        	this.sessionRecommendation_JLabel.setText (
        		"<html><b>RECOMMENDATION: " + this.googleDriveSession.getProblemRecommendation() + "</b></html>" );
        	this.__CredentialsStatus_JTextField.setText("Not authenticated");
    	}
    }
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of tables to choose from, used if appending
*/
private void initialize ( JFrame parent, GoogleDrive_Command command, List<String> tableIDChoices ) {
	this.__command = command;
	this.__parent = parent;
	CommandProcessor processor =__command.getCommandProcessor();

	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel.

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Run a Google Drive command."
        + "  Google Drive provides cloud storage for files." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Google Drive stores folders and files using a long ID containing characters and numbers, which is used in URLs."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"However, this command uses paths and names separated by / as shown 'My Drive', 'Shared with me', and 'Shared drives'."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"It is recommended that file and folders on the local computer are relative to the working directory, which is:"),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel ("    " + __working_dir),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    // Labels will be populated below:
    // - the following initial messages should only be shown for an instant
    this.sessionProblem_JLabel = new JLabel("Google Drive authentication is unknown.");
   	JGUIUtil.addComponent(main_JPanel, this.sessionProblem_JLabel,
       	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    this.sessionRecommendation_JLabel = new JLabel("Checks are performed after initializing the data.");
   	JGUIUtil.addComponent(main_JPanel, this.sessionRecommendation_JLabel,
       	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   	this.ignoreEvents = true; // So that a full pass of initialization can occur.

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Session ID:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SessionID_JTextField = new JTextField ( "", 30 );
    __SessionID_JTextField.setToolTipText("Session ID for credentials.");
    __SessionID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __SessionID_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - session ID for credentials."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Authentication method:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AuthenticationMethod_JComboBox = new SimpleJComboBox ( false );
	__AuthenticationMethod_JComboBox.setToolTipText("Google Drive command to execute.");
	List<String> authChoices = GoogleDriveAuthenticationMethodType.getChoicesAsStrings(false);
	authChoices.add(0,"");
	__AuthenticationMethod_JComboBox.setData(authChoices);
	__AuthenticationMethod_JComboBox.select ( 0 );
	__AuthenticationMethod_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __AuthenticationMethod_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Optional - authorization type (default="
		+ GoogleDriveAuthenticationMethodType.SERVICE_ACCOUNT_KEY + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Credentials status:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CredentialsStatus_JTextField = new JTextField ( "", 30 );
    __CredentialsStatus_JTextField.setToolTipText("Whether credentials are OK.");
    __CredentialsStatus_JTextField.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, __CredentialsStatus_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Are credentials OK?"),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Google Drive command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__GoogleDriveCommand_JComboBox = new SimpleJComboBox ( false );
	__GoogleDriveCommand_JComboBox.setToolTipText("Google Drive command to execute.");
	List<String> commandChoices = GoogleDriveCommandType.getChoicesAsStrings(false);
	__GoogleDriveCommand_JComboBox.setData(commandChoices);
	__GoogleDriveCommand_JComboBox.select ( 0 );
	__GoogleDriveCommand_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __GoogleDriveCommand_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - Google Drive command to run (see tabs below)."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __main_JTabbedPane = new JTabbedPane ();
    __main_JTabbedPane.addChangeListener(this);
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for 'Copy' parameters:
    // - specify original and copy
    /*
    int yCopy = -1;
    JPanel copy_JPanel = new JPanel();
    copy_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Copy", copy_JPanel );

    JGUIUtil.addComponent(copy_JPanel, new JLabel ("Copy S3 object(s) by specifying source and destination keys."),
		0, ++yCopy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JLabel ("Currently only single file objects can be copied (not folders)."),
		0, ++yCopy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JLabel ("Use object keys (paths) similar to:  folder1/folder2/file.ext"),
		0, ++yCopy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JLabel ("The keys should not start with / unless the bucket uses top-level / for keys."),
		0, ++yCopy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JLabel ("The keys should not end with / (support for copying folders may be added in the future)."),
		0, ++yCopy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JLabel ("Use the 'Browse Google Drive' button to visually confirm S3 object keys (paths)."),
		0, ++yCopy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yCopy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(copy_JPanel, new JLabel ("Copy files:"),
        0, ++yCopy, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CopyFiles_JTextArea = new JTextArea (6,35);
    __CopyFiles_JTextArea.setLineWrap ( true );
    __CopyFiles_JTextArea.setWrapStyleWord ( true );
    __CopyFiles_JTextArea.setToolTipText("SourceKey1:DestKey1,SourceKey2:DestKey2,...");
    __CopyFiles_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(copy_JPanel, new JScrollPane(__CopyFiles_JTextArea),
        1, yCopy, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JLabel ("Source and destination key(s)."),
        3, yCopy, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(copy_JPanel, new SimpleJButton ("Edit","EditCopyFiles",this),
        3, ++yCopy, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(copy_JPanel, new JLabel ( "Copy destination bucket:"),
		0, ++yCopy, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__CopyBucket_JComboBox = new SimpleJComboBox ( false );
	__CopyBucket_JComboBox.setToolTipText("AWS S3 destination bucket.");
	// Choices will be populated when refreshed, based on profile.
	__CopyBucket_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(copy_JPanel, __CopyBucket_JComboBox,
		1, yCopy, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JLabel(
		"Optional - S3 destination bucket (default=source bucket)."),
		3, yCopy, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(copy_JPanel, new JLabel("Copy objects count property:"),
        0, ++yCopy, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CopyObjectsCountProperty_JTextField = new JTextField ( "", 30 );
    __CopyObjectsCountProperty_JTextField.setToolTipText("Specify the property name for the copy result size, can use ${Property} notation");
    __CopyObjectsCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(copy_JPanel, __CopyObjectsCountProperty_JTextField,
        1, yCopy, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(copy_JPanel, new JLabel ( "Optional - processor property to set as copy count." ),
        3, yCopy, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        */

    /*
    // Panel for 'Delete' parameters:
    // - specify S3 files and folders to delete
    int yDelete = -1;
    JPanel delete_JPanel = new JPanel();
    delete_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Delete", delete_JPanel );

    JGUIUtil.addComponent(delete_JPanel, new JLabel ("Specify the S3 object(s) to delete."),
		0, ++yDelete, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(delete_JPanel, new JLabel ("Object keys should not start with / unless the bucket uses a top-level / object."),
		0, ++yDelete, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(delete_JPanel, new JLabel ("File object keys should NOT end with /."),
		0, ++yDelete, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(delete_JPanel, new JLabel ("Folder path keys should end with /."),
		0, ++yDelete, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(delete_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yDelete, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(delete_JPanel, new JLabel ("Delete files:"),
        0, ++yDelete, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DeleteFiles_JTextArea = new JTextArea (6,35);
    __DeleteFiles_JTextArea.setLineWrap ( true );
    __DeleteFiles_JTextArea.setWrapStyleWord ( true );
    __DeleteFiles_JTextArea.setToolTipText("S3 file object keys to delete, separated by commas.");
    __DeleteFiles_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(delete_JPanel, new JScrollPane(__DeleteFiles_JTextArea),
        1, yDelete, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(delete_JPanel, new JLabel ("S3 bucket key(s)."),
        3, yDelete, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(delete_JPanel, new SimpleJButton ("Edit","EditDeleteFiles",this),
        3, ++yDelete, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(delete_JPanel, new JLabel ("Delete folders:"),
        0, ++yDelete, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DeleteFolders_JTextArea = new JTextArea (6,35);
    __DeleteFolders_JTextArea.setLineWrap ( true );
    __DeleteFolders_JTextArea.setWrapStyleWord ( true );
    __DeleteFolders_JTextArea.setToolTipText("Folders to delete, as paths ending in /, separated by commas.");
    __DeleteFolders_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(delete_JPanel, new JScrollPane(__DeleteFolders_JTextArea),
        1, yDelete, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(delete_JPanel, new JLabel ("S3 folders(s) ending in /."),
        3, yDelete, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(delete_JPanel, new SimpleJButton ("Edit","EditDeleteFolders",this),
        3, ++yDelete, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(delete_JPanel, new JLabel ( "Delete folders scope:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DeleteFoldersScope_JComboBox = new SimpleJComboBox ( false );
	__DeleteFoldersScope_JComboBox.setToolTipText("AWS S3 bucket.");
	List<String> deleteChoices = new ArrayList<>();
	deleteChoices.add("");
	deleteChoices.add(command._AllFilesAndFolders);
	deleteChoices.add(command._FolderFiles);
	__DeleteFoldersScope_JComboBox.setData(deleteChoices);
	__DeleteFoldersScope_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(delete_JPanel, __DeleteFoldersScope_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(delete_JPanel, new JLabel(
		"Optional - scope of folder delete (default=" + command._FolderFiles + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(delete_JPanel, new JLabel ( "Delete folders minimum depth:"),
        0, ++yDelete, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DeleteFoldersMinDepth_JTextField = new JTextField ( "", 10 );
    __DeleteFoldersMinDepth_JTextField.setToolTipText("Folder depth that is required to delete, to guard against deleting top folders.");
    __DeleteFoldersMinDepth_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(delete_JPanel, __DeleteFoldersMinDepth_JTextField,
        1, yDelete, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(delete_JPanel, new JLabel ( "Optional - minimum required folder depth (default=" + command._DeleteFoldersMinDepth + ")."),
        3, yDelete, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        */

    // Panel for 'Download' parameters:
    // - map bucket objects to files and folders
    int yDownload = -1;
    JPanel download_JPanel = new JPanel();
    download_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Download", download_JPanel );

    JGUIUtil.addComponent(download_JPanel, new JLabel ("Specify files and folders to download."),
		0, ++yDownload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(download_JPanel, new JLabel ("<html><b>Currently only files (not folders) can be downloaded.</b></html>."),
		0, ++yDownload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(download_JPanel, new JLabel ("Use the 'Edit' button to view information about Google Drive and local file and folder paths."),
		0, ++yDownload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(download_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yDownload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(download_JPanel, new JLabel ("Download folders:"),
        0, ++yDownload, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DownloadFolders_JTextArea = new JTextArea (6,35);
    __DownloadFolders_JTextArea.setLineWrap ( true );
    __DownloadFolders_JTextArea.setWrapStyleWord ( true );
    __DownloadFolders_JTextArea.setToolTipText("GoogleDrivePath1:Folder1,GoogleDrivePath2:Folder2,...");
    __DownloadFolders_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(download_JPanel, new JScrollPane(__DownloadFolders_JTextArea),
        1, yDownload, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(download_JPanel, new JLabel ("Google Drive folder path(s) and local folder(s)."),
        3, yDownload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    SimpleJButton folderEditButton = new SimpleJButton ("Edit","EditDownloadFolders",this);
    folderEditButton.setEnabled(false);
    JGUIUtil.addComponent(download_JPanel, folderEditButton,
        3, ++yDownload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(download_JPanel, new JLabel ("Download files:"),
        0, ++yDownload, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DownloadFiles_JTextArea = new JTextArea (6,35);
    __DownloadFiles_JTextArea.setLineWrap ( true );
    __DownloadFiles_JTextArea.setWrapStyleWord ( true );
    __DownloadFiles_JTextArea.setToolTipText("GoogleDrivePath1:File1,GoogleDrivePath2:File2,...");
    __DownloadFiles_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(download_JPanel, new JScrollPane(__DownloadFiles_JTextArea),
        1, yDownload, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(download_JPanel, new JLabel ("Google Drive file path(s) and local file(s)."),
        3, yDownload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(download_JPanel, new SimpleJButton ("Edit","EditDownloadFiles",this),
        3, ++yDownload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(download_JPanel, new JLabel("Download count property:"),
        0, ++yDownload, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DownloadCountProperty_JTextField = new JTextField ( "", 30 );
    __DownloadCountProperty_JTextField.setToolTipText("Specify the property name for the download result size, can use ${Property} notation");
    __DownloadCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(download_JPanel, __DownloadCountProperty_JTextField,
        1, yDownload, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(download_JPanel, new JLabel ( "Optional - processor property to set as download count." ),
        3, yDownload, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for 'List Drives' parameters.
    int yListDrives = -1;
    JPanel listDrives_JPanel = new JPanel();
    listDrives_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "List Drives", listDrives_JPanel );

    JGUIUtil.addComponent(listDrives_JPanel, new JLabel ("List all Google Drive shared drives that are visible to the user based on credentials."),
		0, ++yListDrives, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listDrives_JPanel, new JLabel ("The My Drive drive is not listed."),
		0, ++yListDrives, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listDrives_JPanel, new JLabel ("Use * in the regular expression as wildcards to filter the results."),
		0, ++yListDrives, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listDrives_JPanel, new JLabel ("See the 'Output' tab to specify the output table and/or file for the drive list."),
		0, ++yListDrives, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listDrives_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yListDrives, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listDrives_JPanel, new JLabel ( "Regular expression:"),
        0, ++yListDrives, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ListDrivesRegEx_JTextField = new JTextField ( "", 30 );
    __ListDrivesRegEx_JTextField.setToolTipText("Regular expression to filter results, default=glob (*) style");
    __ListDrivesRegEx_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listDrives_JPanel, __ListDrivesRegEx_JTextField,
        1, yListDrives, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listDrives_JPanel, new JLabel ( "Optional - regular expression filter (default=none)."),
        3, yListDrives, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listDrives_JPanel, new JLabel("List drives count property:"),
        0, ++yListDrives, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ListDrivesCountProperty_JTextField = new JTextField ( "", 30 );
    __ListDrivesCountProperty_JTextField.setToolTipText("Specify the property name for the list result size, can use ${Property} notation");
    __ListDrivesCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listDrives_JPanel, __ListDrivesCountProperty_JTextField,
        1, yListDrives, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listDrives_JPanel, new JLabel ( "Optional - processor property to set as drive count." ),
        3, yListDrives, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for 'List' parameters:
    // - this includes filtering
    int yList = -1;
    JPanel list_JPanel = new JPanel();
    list_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "List", list_JPanel );

    JGUIUtil.addComponent(list_JPanel, new JLabel (
    	"List Google Drive files and folders that are visible to the user based on credentials."
    	+ "  See the 'Output' tab to specify the output file and/or table for the output list."),
		0, ++yList, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel (
    	"Limit the output using parameters as follows and by using the 'Regular expression', 'List files',"
    	+ " 'List folders', 'List shared with me', and 'List trashed' filters."),
		0, ++yList, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    //String style = " style=\"border: 1px solid black; border-collapse: collapse; background-color: white;\"";
    String style = " style=\"border-collapse: collapse; border-spacing: 0px;\"";
    String tableStyle = style;
    String trStyle = "";
    String tdStyle = " style=\"border: 1px solid black; background-color: white;\"";
    String table =
    		  "<html>"
    		  + "<table " + tableStyle + ">"
    		+ "    <tr" + trStyle + ">"
    		+ "       <th" + tdStyle + ">List what?</th>"
    		+ "       <th" + tdStyle + ">List scope</th>"
    		+ "       <th" + tdStyle + ">Folder or file to match</th>"
    		+ "    </tr>"
    		+ "    <tr" + trStyle + ">"
    		+ "       <td" + tdStyle + ">One file (<b>not implemented</b>)</td>"
    		+ "       <td" + tdStyle + "><code>All</code></td>"
    		+ "       <td" + tdStyle + "><code>/path/to/file.ext</code></td>"
    		+ "    </tr>"
    		+ "    <tr" + trStyle + ">"
    		+ "       <td" + tdStyle + ">Files in 'My Drive' root"
    		+ "       <td" + tdStyle + "><code>Folder</code></td>"
    		+ "       <td" + tdStyle + "><code>/</code> or <code>/My Drive/</code> (will include shared files and folders if <code>ListSharedWithMe=True</code>)</td>"
    		+ "    </tr>"
    		+ "    <tr" + trStyle + ">"
    		+ "       <td" + tdStyle + ">Files in 'My Drive' folder</td>"
    		+ "       <td" + tdStyle + "><code>Folder</code></td>"
    		+ "       <td" + tdStyle + "><code>/folder/path/</code> or <code>/My Drive/folder/path</code></td>"
    		+ "    </tr>"
    		+ "    <tr" + trStyle + ">"
    		+ "       <td" + tdStyle + ">Files in 'Shared with me' folder</td>"
    		+ "       <td" + tdStyle + "><code>Folder</code></td>"
    		+ "       <td" + tdStyle + "><code>/Shared with me/folder/path/</code></td>"
    		+ "    </tr>"
    		+ "    <tr" + trStyle + ">"
    		+ "       <td" + tdStyle + ">Files in 'Shared drives' folder</td>"
    		+ "       <td" + tdStyle + "><code>Folder</code></td>"
    		+ "       <td" + tdStyle + "><code>/Shared drives/drivename/folder/path/</code></td>"
    		+ "    </tr>"
    		+ "    <tr" + trStyle + ">"
    		+ "       <td" + tdStyle + ">Files in folder given its Google Drive identifier</td>"
    		+ "       <td" + tdStyle + "><code>Folder</code></td>"
    		+ "       <td" + tdStyle + "><code>/id/identifier/</code></td>"
    		+ "    </tr>"
    		+ "    <tr" + trStyle + ">"
    		+ "       <td" + tdStyle + ">All files in folder and sub-folders (<b>not implemented</b>)</td>"
    		+ "       <td" + tdStyle + "><code>All</code></td>"
    		+ "       <td" + tdStyle + "><code>/folder/path/</code> or <code>/My Drive/folder/path/</code></td>"
    		+ "    </tr>"
    		+ "  </table>"
    		+ "</html>";
    JGUIUtil.addComponent(list_JPanel, new JLabel (table),
		0, ++yList, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    Message.printStatus(2, "", table);
    /*
    JGUIUtil.addComponent(list_JPanel, new JLabel ("    list all objects in a bucket: ListScope=" + _All + ", Prefix"),
		0, ++yList, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel ("    no Prefix (and list only root) - list all the top-level (root) folder objects"),
		0, ++yList, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel ("    Prefix = folder1/ - list 'folder1/ objects (output will include the trailing /)"),
		0, ++yList, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel ("    Prefix = file or folder1/folder2/file - list one file (must match exactly)"),
		0, ++yList, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
		*/
    JGUIUtil.addComponent(list_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yList, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   JGUIUtil.addComponent(list_JPanel, new JLabel ( "List scope:"),
		0, ++yList, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ListScope_JComboBox = new SimpleJComboBox ( false );
	__ListScope_JComboBox.setToolTipText("Scope (depth) of the list, which controls the output");
	List<String> listRootChoices = new ArrayList<>();
	listRootChoices.add ( "" );	// Default.
	//listRootChoices.add ( __command._All );
	//listRootChoices.add ( __command._File );
	listRootChoices.add ( __command._Folder );
	__ListScope_JComboBox.setData(listRootChoices);
	__ListScope_JComboBox.select ( 0 );
	__ListScope_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(list_JPanel, __ListScope_JComboBox,
		1, yList, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel(
		"Optional - scope (depth) of the listing (default=" + __command._Folder + ")."),
		3, yList, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(list_JPanel, new JLabel ( "Folder path to list:"),
        0, ++yList, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ListFolderPath_JTextField = new JTextField ( "", 30 );
    __ListFolderPath_JTextField.setToolTipText("Specify the folder to list, ending in /.");
    __ListFolderPath_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(list_JPanel, __ListFolderPath_JTextField,
        1, yList, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel ( "Optional - folder path to list (default=list all)."),
        3, yList, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(list_JPanel, new JLabel ( "Regular expression:"),
        0, ++yList, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ListRegEx_JTextField = new JTextField ( "", 30 );
    __ListRegEx_JTextField.setToolTipText("Regular expression to filter results, default=glob (*) style");
    __ListRegEx_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(list_JPanel, __ListRegEx_JTextField,
        1, yList, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel ( "Optional - regular expression filter (default=none)."),
        3, yList, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

   JGUIUtil.addComponent(list_JPanel, new JLabel ( "List files?:"),
		0, ++yList, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ListFiles_JComboBox = new SimpleJComboBox ( false );
	__ListFiles_JComboBox.setToolTipText("Indicate whether to list files?");
	List<String> listFilesChoices = new ArrayList<>();
	listFilesChoices.add ( "" );	// Default.
	listFilesChoices.add ( __command._False );
	listFilesChoices.add ( __command._True );
	__ListFiles_JComboBox.setData(listFilesChoices);
	__ListFiles_JComboBox.select ( 0 );
	__ListFiles_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(list_JPanel, __ListFiles_JComboBox,
		1, yList, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel(
		"Optional - list files? (default=" + __command._True + ")."),
		3, yList, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

   JGUIUtil.addComponent(list_JPanel, new JLabel ( "List folders?:"),
		0, ++yList, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ListFolders_JComboBox = new SimpleJComboBox ( false );
	__ListFolders_JComboBox.setToolTipText("Indicate whether to list files?");
	List<String> listFoldersChoices = new ArrayList<>();
	listFoldersChoices.add ( "" );	// Default.
	listFoldersChoices.add ( __command._False );
	listFoldersChoices.add ( __command._True );
	__ListFolders_JComboBox.setData(listFoldersChoices);
	__ListFolders_JComboBox.select ( 0 );
	__ListFolders_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(list_JPanel, __ListFolders_JComboBox,
		1, yList, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel(
		"Optional - list folders? (default=" + __command._True + ")."),
		3, yList, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

   JGUIUtil.addComponent(list_JPanel, new JLabel ( "List 'Shared with me'?:"),
		0, ++yList, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ListSharedWithMe_JComboBox = new SimpleJComboBox ( false );
	__ListSharedWithMe_JComboBox.setToolTipText("Indicate whether to list 'Shared with me' files.");
	List<String> listSharedChoices = new ArrayList<>();
	listSharedChoices.add ( "" );	// Default.
	listSharedChoices.add ( __command._False );
	listSharedChoices.add ( __command._Only );
	listSharedChoices.add ( __command._True );
	__ListSharedWithMe_JComboBox.setData(listSharedChoices);
	__ListSharedWithMe_JComboBox.select ( 0 );
	__ListSharedWithMe_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(list_JPanel, __ListSharedWithMe_JComboBox,
		1, yList, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel(
		"Optional - list 'Shared with me' files? (default=" + __command._False + ")."),
		3, yList, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

   JGUIUtil.addComponent(list_JPanel, new JLabel ( "List trashed?:"),
		0, ++yList, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ListTrashed_JComboBox = new SimpleJComboBox ( false );
	__ListTrashed_JComboBox.setToolTipText("Indicate whether to list trashed files?");
	List<String> listTrashedChoices = new ArrayList<>();
	listTrashedChoices.add ( "" );	// Default.
	listTrashedChoices.add ( __command._False );
	listTrashedChoices.add ( __command._True );
	__ListTrashed_JComboBox.setData(listTrashedChoices);
	__ListTrashed_JComboBox.select ( 0 );
	__ListTrashed_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(list_JPanel, __ListTrashed_JComboBox,
		1, yList, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel(
		"Optional - list trashed files? (default=" + __command._False + ")."),
		3, yList, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(list_JPanel, new JLabel ( "List maximum:"),
        0, ++yList, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ListMax_JTextField = new JTextField ( "", 10 );
    __ListMax_JTextField.setToolTipText("Use to limit the size of the query results.");
    __ListMax_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(list_JPanel, __ListMax_JTextField,
        1, yList, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel ( "Optional - maximum number of items read (default=no limit)."),
        3, yList, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(list_JPanel, new JLabel("List count property:"),
        0, ++yList, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ListCountProperty_JTextField = new JTextField ( "", 30 );
    __ListCountProperty_JTextField.setToolTipText("Specify the property name for the list result size, can use ${Property} notation");
    __ListCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(list_JPanel, __ListCountProperty_JTextField,
        1, yList, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel ( "Optional - processor property to set as object count." ),
        3, yList, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    /*
    // Panel for 'Upload' parameters:
    // - map files and folders to bucket objects
    int yUpload = -1;
    JPanel upload_JPanel = new JPanel();
    upload_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Upload", upload_JPanel );

    JGUIUtil.addComponent(upload_JPanel, new JLabel ("Specify files and folders (directories) to upload."),
		0, ++yUpload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(upload_JPanel, new JLabel ("Use the 'Edit' button to view information about local and S3 file and folder paths."),
		0, ++yUpload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(upload_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yUpload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(upload_JPanel, new JLabel ("Upload folders:"),
        0, ++yUpload, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __UploadFolders_JTextArea = new JTextArea (6,35);
    __UploadFolders_JTextArea.setLineWrap ( true );
    __UploadFolders_JTextArea.setWrapStyleWord ( true );
    __UploadFolders_JTextArea.setToolTipText("Folder1:Key1,Folder2:Key2,...");
    __UploadFolders_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(upload_JPanel, new JScrollPane(__UploadFolders_JTextArea),
        1, yUpload, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(upload_JPanel, new JLabel ("Local folder(s) and S3 bucket key(s) ending in /."),
        3, yUpload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(upload_JPanel, new SimpleJButton ("Edit","EditUploadFolders",this),
        3, ++yUpload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(upload_JPanel, new JLabel ("Upload files:"),
        0, ++yUpload, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __UploadFiles_JTextArea = new JTextArea (6,35);
    __UploadFiles_JTextArea.setLineWrap ( true );
    __UploadFiles_JTextArea.setWrapStyleWord ( true );
    __UploadFiles_JTextArea.setToolTipText("File1:Key1,File2:Key2,...");
    __UploadFiles_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(upload_JPanel, new JScrollPane(__UploadFiles_JTextArea),
        1, yUpload, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(upload_JPanel, new JLabel ("Local file(s) and S3 bucket key(s)."),
        3, yUpload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(upload_JPanel, new SimpleJButton ("Edit","EditUploadFiles",this),
        3, ++yUpload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        */

    // Panel for output.
    int yOutput = -1;
    JPanel output_JPanel = new JPanel();
    output_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output", output_JPanel );

    JGUIUtil.addComponent(output_JPanel, new JLabel (
    	"The following parameters are used with the 'List' and 'ListDrives' commands."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("An output table and/or file can be created."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("An existing table will be appended to if found."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("The output file uses the specified table (or a temporary table) to create the output file."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("Specify the output file name with extension to indicate the format: csv"),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("See also other commands to write tables."),
		0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yOutput, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Output Table ID:" ),
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputTableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit.
    __OutputTableID_JComboBox.setToolTipText("Table for output, available for List Buckets and List Objects");
    tableIDChoices.add(0,""); // Add blank to ignore table.
    __OutputTableID_JComboBox.setData ( tableIDChoices );
    __OutputTableID_JComboBox.addItemListener ( this );
    __OutputTableID_JComboBox.getJTextComponent().addKeyListener ( this );
    //__OutputTableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(output_JPanel, __OutputTableID_JComboBox,
        1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel( "Optional - table for output."),
        3, yOutput, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(output_JPanel, new JLabel ("Output file:" ),
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFile_JTextField = new JTextField ( 50 );
    __OutputFile_JTextField.setToolTipText(
    	"Output file, available for List Buckets and List Objects, can use ${Property} notation.");
    __OutputFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel OutputFile_JPanel = new JPanel();
	OutputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(OutputFile_JPanel, __OutputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseOutput_JButton = new SimpleJButton ( "...", this );
	__browseOutput_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(OutputFile_JPanel, __browseOutput_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathOutput_JButton = new SimpleJButton( __RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(OutputFile_JPanel, __pathOutput_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(output_JPanel, OutputFile_JPanel,
		1, yOutput, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   JGUIUtil.addComponent(output_JPanel, new JLabel ( "Append output?:"),
		0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AppendOutput_JComboBox = new SimpleJComboBox ( false );
	__AppendOutput_JComboBox.setToolTipText("Append output to existing table or file?");
	List<String> appendChoices = new ArrayList<>();
	appendChoices.add ( "" );	// Default.
	appendChoices.add ( __command._False );
	appendChoices.add ( __command._True );
	__AppendOutput_JComboBox.setData(appendChoices);
	__AppendOutput_JComboBox.select ( 0 );
	__AppendOutput_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(output_JPanel, __AppendOutput_JComboBox,
		1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel(
		"Optional - append to output (default=" + __command._False + ")."),
		3, yOutput, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    /*
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "If input not found?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfInputNotFound_JComboBox = new SimpleJComboBox ( false );
	List<String> notFoundChoices = new ArrayList<>();
	notFoundChoices.add ( "" );	// Default.
	notFoundChoices.add ( __command._Ignore );
	notFoundChoices.add ( __command._Warn );
	notFoundChoices.add ( __command._Fail );
	__IfInputNotFound_JComboBox.setData(notFoundChoices);
	__IfInputNotFound_JComboBox.select ( 0 );
	__IfInputNotFound_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __IfInputNotFound_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - action if input file is not found (default=" + __command._Warn + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
		*/

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 60 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.addKeyListener ( this );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel,
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

    button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + "() command" );

    this.ignoreEvents = false; // After initialization of components let events happen to dynamically cause cascade.

	// Refresh the contents.
    refresh ();

    // Create a session using authentication method:
    // - will only work if the SessionID and AuthenticationMethod are set (e.g, from previously-edited command)
    // - this displays whether the authentication is OK
    // - put this after UI components have been created with command parameter values and to receive the output
    createGoogleDriveSession();

    pack();
    JGUIUtil.center( this );
	// Dialogs do not need to be resizable.
	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e ) {
	if ( this.ignoreEvents ) {
        return; // Startup.
    }
	Object o = e.getSource();
    if ( o == this.__GoogleDriveCommand_JComboBox ) {
    	setTabForGoogleDriveCommand();
    }
    if ( o == this.__AuthenticationMethod_JComboBox ) {
    	// Create the session to check authentication:
    	// - only called if ItemListener is enabled.
    	createGoogleDriveSession();
    }
	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event ) {
	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
	}
}

public void keyReleased ( KeyEvent event ) {
	if ( event.getComponent() == this.__SessionID_JTextField ) {
    	// Get the session to check authentication:
		// - if this is slow, may need to do mouse motion event and check for exiting the field?
    	createGoogleDriveSession();
	}
	refresh();
}

public void keyTyped ( KeyEvent event ) {
}

/**
Indicate if the user pressed OK (cancel otherwise).
*/
public boolean ok () {
	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh () {
	String routine = getClass().getSimpleName() + ".refresh";
	// General.
	String SessionID = "";
	String AuthenticationMethod = "";
	String GoogleDriveCommand = "";
	// Copy.
	String CopyFiles = "";
	String CopyBucket = "";
	String CopyObjectsCountProperty = "";
	// Delete.
	String DeleteFiles = "";
	String DeleteFolders = "";
	String DeleteFoldersScope = "";
	String DeleteFoldersMinDepth = "";
	// Download.
	String DownloadFolders = "";
	String DownloadFiles = "";
	String DownloadCountProperty = "";
	// List drives.
	String ListDrivesRegEx = "";
	String ListDrivesCountProperty = "";
	// List.
	String ListScope = "";
	String ListFolderPath = "";
	String ListRegEx = "";
	String ListFiles = "";
	String ListFolders = "";
	String ListSharedWithMe = "";
	String ListTrashed = "";
	String ListMax = "";
	String ListCountProperty = "";
	// Upload.
	String UploadFolders = "";
	String UploadFiles = "";
	// Output.
	String OutputTableID = "";
	String OutputFile = "";
	String AppendOutput = "";
	//String IfInputNotFound = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
        parameters = __command.getCommandParameters();
        // General.
		SessionID = parameters.getValue ( "SessionID" );
		AuthenticationMethod = parameters.getValue ( "AuthenticationMethod" );
		GoogleDriveCommand = parameters.getValue ( "GoogleDriveCommand" );
		// Copy.
		CopyFiles = parameters.getValue ( "CopyFiles" );
		CopyBucket = parameters.getValue ( "CopyBucket" );
		CopyObjectsCountProperty = parameters.getValue ( "CopyObjectsCountProperty" );
		// Delete.
		DeleteFiles = parameters.getValue ( "DeleteFiles" );
		DeleteFolders = parameters.getValue ( "DeleteFolders" );
		DeleteFoldersScope = parameters.getValue ( "DeleteFoldersScope" );
		DeleteFoldersMinDepth = parameters.getValue ( "DeleteFoldersMinDepth" );
		// Download.
		DownloadFolders = parameters.getValue ( "DownloadFolders" );
		DownloadFiles = parameters.getValue ( "DownloadFiles" );
		DownloadCountProperty = parameters.getValue ( "DownloadCountProperty" );
		// List drives.
		ListDrivesRegEx = parameters.getValue ( "ListDrivesRegEx" );
		ListDrivesCountProperty = parameters.getValue ( "ListDrivesCountProperty" );
		// List.
		ListScope = parameters.getValue ( "ListScope" );
		ListFolderPath = parameters.getValue ( "ListFolderPath" );
		ListRegEx = parameters.getValue ( "ListRegEx" );
		ListFiles = parameters.getValue ( "ListFiles" );
		ListFolders = parameters.getValue ( "ListFolders" );
		ListSharedWithMe = parameters.getValue ( "ListSharedWithMe" );
		ListTrashed = parameters.getValue ( "ListTrashed" );
		ListMax = parameters.getValue ( "ListMax" );
		ListCountProperty = parameters.getValue ( "ListCountProperty" );
		// Upload.
		UploadFolders = parameters.getValue ( "UploadFolders" );
		UploadFiles = parameters.getValue ( "UploadFiles" );
		// Output
		OutputTableID = parameters.getValue ( "OutputTableID" );
		OutputFile = parameters.getValue ( "OutputFile" );
		AppendOutput = parameters.getValue ( "AppendOutput" );
		//IfInputNotFound = parameters.getValue ( "IfInputNotFound" );

		// Set the parameter values in the UI components.
        if ( SessionID != null ) {
            __SessionID_JTextField.setText ( SessionID );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__AuthenticationMethod_JComboBox, AuthenticationMethod,JGUIUtil.NONE, null, null ) ) {
			__AuthenticationMethod_JComboBox.select ( AuthenticationMethod );
		}
		else {
            if ( (AuthenticationMethod == null) || AuthenticationMethod.equals("") ) {
				// New command...select the default.
				__AuthenticationMethod_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"AutenticationType command parameter \"" + AuthenticationMethod + "\".  Select a value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__GoogleDriveCommand_JComboBox, GoogleDriveCommand,JGUIUtil.NONE, null, null ) ) {
			__GoogleDriveCommand_JComboBox.select ( GoogleDriveCommand );
		}
		else {
            if ( (GoogleDriveCommand == null) || GoogleDriveCommand.equals("") ) {
				// New command...select the default.
				__GoogleDriveCommand_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"GoogleDriveCommand parameter \"" + GoogleDriveCommand + "\".  Select a value or Cancel." );
			}
		}
        /*
        if ( CopyFiles != null ) {
            __CopyFiles_JTextArea.setText ( CopyFiles );
        }
        // Populate the copy bucket choices, which depends on the above profile and region.
        AwsToolkit.getInstance().uiPopulateBucketChoices( this.awsSession, getSelectedRegion(), __CopyBucket_JComboBox, true );
		if ( JGUIUtil.isSimpleJComboBoxItem(__CopyBucket_JComboBox, CopyBucket,JGUIUtil.NONE, null, null ) ) {
			__CopyBucket_JComboBox.select ( CopyBucket );
		}
		else {
            if ( (CopyBucket == null) || CopyBucket.equals("") ) {
				// New command...select the default.
            	if ( __CopyBucket_JComboBox.getItemCount() > 0 ) {
            		__CopyBucket_JComboBox.select ( 0 );
            	}
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"CopyBucket parameter \"" + CopyBucket + "\".  Select a value or Cancel." );
			}
		}
		*/
		/*
        if ( CopyObjectsCountProperty != null ) {
            __CopyObjectsCountProperty_JTextField.setText ( CopyObjectsCountProperty );
        }
        if ( DeleteFiles != null ) {
            __DeleteFiles_JTextArea.setText ( DeleteFiles );
        }
        if ( DeleteFolders != null ) {
            __DeleteFolders_JTextArea.setText ( DeleteFolders );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__DeleteFoldersScope_JComboBox, DeleteFoldersScope,JGUIUtil.NONE, null, null ) ) {
			__DeleteFoldersScope_JComboBox.select ( DeleteFoldersScope );
		}
		else {
            if ( (DeleteFoldersScope == null) || DeleteFoldersScope.equals("") ) {
				// New command...select the default.
				__DeleteFoldersScope_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"DeleteFoldersScope parameter \"" + DeleteFoldersScope + "\".  Select a value or Cancel." );
			}
		}
        if ( DeleteFoldersMinDepth != null ) {
            __DeleteFoldersMinDepth_JTextField.setText ( DeleteFoldersMinDepth );
        }
		*/
		// Download.
        if ( DownloadFolders != null ) {
            __DownloadFolders_JTextArea.setText ( DownloadFolders );
        }
        if ( DownloadFiles != null ) {
            __DownloadFiles_JTextArea.setText ( DownloadFiles );
        }
        if ( DownloadCountProperty != null ) {
            __DownloadCountProperty_JTextField.setText ( DownloadCountProperty );
        }
        // List Drives.
        if ( ListDrivesRegEx != null ) {
            __ListDrivesRegEx_JTextField.setText ( ListDrivesRegEx );
        }
        if ( ListDrivesCountProperty != null ) {
            __ListDrivesCountProperty_JTextField.setText ( ListDrivesCountProperty );
        }
        // List.
		if ( JGUIUtil.isSimpleJComboBoxItem(__ListScope_JComboBox, ListScope,JGUIUtil.NONE, null, null ) ) {
			__ListScope_JComboBox.select ( ListScope );
		}
		else {
            if ( (ListScope == null) || ListScope.equals("") ) {
				// New command...select the default.
				__ListScope_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"ListScope parameter \"" + ListScope + "\".  Select a value or Cancel." );
			}
		}
        if ( ListFolderPath != null ) {
            __ListFolderPath_JTextField.setText ( ListFolderPath );
        }
        if ( ListRegEx != null ) {
            __ListRegEx_JTextField.setText ( ListRegEx );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__ListFiles_JComboBox, ListFiles,JGUIUtil.NONE, null, null ) ) {
			__ListFiles_JComboBox.select ( ListFiles );
		}
		else {
            if ( (ListFiles == null) ||	ListFiles.equals("") ) {
				// New command...select the default.
				__ListFiles_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"ListFiles parameter \"" + ListFiles + "\".  Select a value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__ListFolders_JComboBox, ListFolders,JGUIUtil.NONE, null, null ) ) {
			__ListFolders_JComboBox.select ( ListFolders );
		}
		else {
            if ( (ListFolders == null) ||	ListFolders.equals("") ) {
				// New command...select the default.
				__ListFolders_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"ListFolders parameter \"" + ListFolders + "\".  Select a value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__ListSharedWithMe_JComboBox, ListSharedWithMe,JGUIUtil.NONE, null, null ) ) {
			__ListSharedWithMe_JComboBox.select ( ListSharedWithMe );
		}
		else {
            if ( (ListSharedWithMe == null) ||	ListSharedWithMe.equals("") ) {
				// New command...select the default.
				__ListSharedWithMe_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"ListSharedWithMe parameter \"" + ListSharedWithMe + "\".  Select a value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__ListTrashed_JComboBox, ListTrashed,JGUIUtil.NONE, null, null ) ) {
			__ListTrashed_JComboBox.select ( ListTrashed );
		}
		else {
            if ( (ListTrashed == null) ||	ListTrashed.equals("") ) {
				// New command...select the default.
				__ListTrashed_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"ListTrashed parameter \"" + ListTrashed + "\".  Select a value or Cancel." );
			}
		}
        if ( ListMax != null ) {
            __ListMax_JTextField.setText ( ListMax );
        }
        if ( ListCountProperty != null ) {
            __ListCountProperty_JTextField.setText ( ListCountProperty );
        }
        /*
        if ( UploadFolders != null ) {
            __UploadFolders_JTextArea.setText ( UploadFolders );
        }
        if ( UploadFiles != null ) {
            __UploadFiles_JTextArea.setText ( UploadFiles );
        }
        */
        if ( OutputTableID == null ) {
            // Select default.
            __OutputTableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __OutputTableID_JComboBox,OutputTableID, JGUIUtil.NONE, null, null ) ) {
                __OutputTableID_JComboBox.select ( OutputTableID );
            }
            else {
                // Creating new table so add in the first position.
                if ( __OutputTableID_JComboBox.getItemCount() == 0 ) {
                    __OutputTableID_JComboBox.add(OutputTableID);
                }
                else {
                    __OutputTableID_JComboBox.insert(OutputTableID, 0);
                }
                __OutputTableID_JComboBox.select(0);
            }
        }
        if ( OutputFile != null ) {
            __OutputFile_JTextField.setText ( OutputFile );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__AppendOutput_JComboBox, AppendOutput,JGUIUtil.NONE, null, null ) ) {
			__AppendOutput_JComboBox.select ( AppendOutput );
		}
		else {
            if ( (AppendOutput == null) ||	AppendOutput.equals("") ) {
				// New command...select the default.
				__AppendOutput_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"AppendOutput parameter \"" + AppendOutput + "\".  Select a value or Cancel." );
			}
		}
        /*
		if ( JGUIUtil.isSimpleJComboBoxItem(__IfInputNotFound_JComboBox, IfInputNotFound,JGUIUtil.NONE, null, null ) ) {
			__IfInputNotFound_JComboBox.select ( IfInputNotFound );
		}
		else {
            if ( (IfInputNotFound == null) ||	IfInputNotFound.equals("") ) {
				// New command...select the default.
				__IfInputNotFound_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"IfInputNotFound parameter \"" + IfInputNotFound + "\".  Select a value or Cancel." );
			}
		}
		*/
		// Set the tab to the input.
		setTabForGoogleDriveCommand();
	}
	// Regardless, reset the command from the fields.
	// This is only  visible information that has not been committed in the command.
	// General.
	SessionID = __SessionID_JTextField.getText().trim();
	AuthenticationMethod = __AuthenticationMethod_JComboBox.getSelected();
	GoogleDriveCommand = __GoogleDriveCommand_JComboBox.getSelected();
	/*
	// Copy.
	CopyFiles = __CopyFiles_JTextArea.getText().trim().replace("\n"," ");
	CopyBucket = __CopyBucket_JComboBox.getSelected();
	if ( CopyBucket == null ) {
		CopyBucket = "";
	}
	CopyObjectsCountProperty = __CopyObjectsCountProperty_JTextField.getText().trim();
	// Delete.
	DeleteFiles = __DeleteFiles_JTextArea.getText().trim().replace("\n"," ");
	DeleteFolders = __DeleteFolders_JTextArea.getText().trim().replace("\n"," ");
	DeleteFoldersScope = __DeleteFoldersScope_JComboBox.getSelected();
	DeleteFoldersMinDepth = __DeleteFoldersMinDepth_JTextField.getText().trim();
	*/
	// Download.
	DownloadFolders = __DownloadFolders_JTextArea.getText().trim().replace("\n"," ");
	DownloadFiles = __DownloadFiles_JTextArea.getText().trim().replace("\n"," ");
	DownloadCountProperty = __DownloadCountProperty_JTextField.getText().trim();
	// List drives.
	ListDrivesRegEx = __ListDrivesRegEx_JTextField.getText().trim();
	ListDrivesCountProperty = __ListDrivesCountProperty_JTextField.getText().trim();
	// List files and folders.
	ListScope = __ListScope_JComboBox.getSelected();
	ListFolderPath = __ListFolderPath_JTextField.getText().trim();
	ListRegEx = __ListRegEx_JTextField.getText().trim();
	ListFiles = __ListFiles_JComboBox.getSelected();
	ListFolders = __ListFolders_JComboBox.getSelected();
	ListSharedWithMe = __ListSharedWithMe_JComboBox.getSelected();
	ListTrashed = __ListTrashed_JComboBox.getSelected();
	ListMax = __ListMax_JTextField.getText().trim();
	ListCountProperty = __ListCountProperty_JTextField.getText().trim();
	/*
	// Upload.
	UploadFolders = __UploadFolders_JTextArea.getText().trim().replace("\n"," ");
	UploadFiles = __UploadFiles_JTextArea.getText().trim().replace("\n"," ");
	*/
	// Output
	OutputTableID = __OutputTableID_JComboBox.getSelected();
	OutputFile = __OutputFile_JTextField.getText().trim();
	AppendOutput = __AppendOutput_JComboBox.getSelected();
	//IfInputNotFound = __IfInputNotFound_JComboBox.getSelected();
    // General.
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "SessionID=" + SessionID );
	props.add ( "AuthenticationMethod=" + AuthenticationMethod );
	props.add ( "GoogleDriveCommand=" + GoogleDriveCommand );
	// Copy.
	props.add ( "CopyFiles=" + CopyFiles );
	props.add ( "CopyBucket=" + CopyBucket );
	props.add ( "CopyObjectsCountProperty=" + CopyObjectsCountProperty );
	// Delete.
	props.add ( "DeleteFiles=" + DeleteFiles );
	props.add ( "DeleteFolders=" + DeleteFolders );
	props.add ( "DeleteFoldersScope=" + DeleteFoldersScope );
	props.add ( "DeleteFoldersMinDepth=" + DeleteFoldersMinDepth );
	// Download.
	props.add ( "DownloadFolders=" + DownloadFolders );
	props.add ( "DownloadFiles=" + DownloadFiles );
	props.add ( "DownloadCountProperty=" + DownloadCountProperty );
	// List drives.
	props.add ( "ListDrivesRegEx=" + ListDrivesRegEx );
	props.add ( "ListDrivesCountProperty=" + ListDrivesCountProperty );
	// List.
	props.add ( "ListScope=" + ListScope );
	props.add ( "ListFolderPath=" + ListFolderPath );
	props.add ( "ListRegEx=" + ListRegEx );
	props.add ( "ListFiles=" + ListFiles );
	props.add ( "ListFolders=" + ListFolders );
	props.add ( "ListSharedWithMe=" + ListSharedWithMe );
	props.add ( "ListTrashed=" + ListTrashed );
	props.add ( "ListMax=" + ListMax );
	props.add ( "ListCountProperty=" + ListCountProperty );
	// Upload.
	props.add ( "UploadFolders=" + UploadFolders );
	props.add ( "UploadFiles=" + UploadFiles );
	// Output.
	props.add ( "OutputTableID=" + OutputTableID );
	props.add ( "OutputFile=" + OutputFile );
	props.add ( "AppendOutput=" + AppendOutput );
	//props.add ( "IfInputNotFound=" + IfInputNotFound );
	__command_JTextArea.setText( __command.toString(props).trim() );
	// Set the default values as FYI.
	// Check the path and determine what the label on the path button should be.
    if ( __pathOutput_JButton != null ) {
		if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
			__pathOutput_JButton.setEnabled ( true );
			File f = new File ( OutputFile );
			if ( f.isAbsolute() ) {
				__pathOutput_JButton.setText ( __RemoveWorkingDirectory );
				__pathOutput_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathOutput_JButton.setText ( __AddWorkingDirectory );
            	__pathOutput_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathOutput_JButton.setEnabled(false);
		}
    }
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
public void response ( boolean ok ) {
	__ok = ok;
	if ( ok ) {
		// Commit the changes.
		commitEdits ();
		if ( __error_wait ) {
			// Not ready to close out.
			return;
		}
	}
	// Now close out.
	setVisible( false );
	dispose();
}

/**
 * Set the parameter tab based on the Google Drive command to execute.
 */
private void setTabForGoogleDriveCommand() {
	String command = __GoogleDriveCommand_JComboBox.getSelected();
	/*
	if ( command.equalsIgnoreCase("" + GoogleDriveCommandType.COPY_OBJECTS) ) {
		__main_JTabbedPane.setSelectedIndex(0);
	}
	if ( command.equalsIgnoreCase("" + GoogleDriveCommandType.DELETE_OBJECTS) ) {
		__main_JTabbedPane.setSelectedIndex(1);
	}
	*/
	if ( command.equalsIgnoreCase("" + GoogleDriveCommandType.DOWNLOAD) ) {
		__main_JTabbedPane.setSelectedIndex(0);
	}
	else if ( command.equalsIgnoreCase("" + GoogleDriveCommandType.LIST_DRIVES) ) {
		__main_JTabbedPane.setSelectedIndex(1);
	}
	else if ( command.equalsIgnoreCase("" + GoogleDriveCommandType.LIST) ) {
		__main_JTabbedPane.setSelectedIndex(2);
	}
	/*
	else if ( command.equalsIgnoreCase("" + AwsS3CommandType.UPLOAD_OBJECTS) ) {
		__main_JTabbedPane.setSelectedIndex(5);
	}
	*/
}

/**
 * Handle JTabbedPane changes.
 */
public void stateChanged ( ChangeEvent event ) {
	//JTabbedPane sourceTabbedPane = (JTabbedPane)event.getSource();
	//int index = sourceTabbedPane.getSelectedIndex();
}

/**
Responds to WindowEvents.
@param event WindowEvent object
*/
public void windowClosing( WindowEvent event ) {
	response ( false );
}

public void windowActivated( WindowEvent evt ) {
}

public void windowClosed( WindowEvent evt ) {
}

public void windowDeactivated( WindowEvent evt ) {
}

public void windowDeiconified( WindowEvent evt ) {
}

public void windowIconified( WindowEvent evt ) {
}

public void windowOpened( WindowEvent evt ) {
}

}