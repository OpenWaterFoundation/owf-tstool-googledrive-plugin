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
import java.util.Collections;
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

import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.StringListJDialog;
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
private SimpleJComboBox __AuthenticationMethod_JComboBox = null;
private SimpleJComboBox __GoogleDriveCommand_JComboBox = null;
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
/*
private JTextArea __DownloadFiles_JTextArea = null;
private JTextArea __DownloadFolders_JTextArea = null;
*/

// List Buckets tab.
/*
private JTextField __ListBucketsRegEx_JTextField = null;
private JTextField __ListBucketsCountProperty_JTextField = null;
*/

// List Objects tab.
private SimpleJComboBox __ListObjectsScope_JComboBox = null;
private JTextField __Prefix_JTextField = null;
private JTextField __ListRegEx_JTextField = null;
private SimpleJComboBox __ListFiles_JComboBox = null;
private SimpleJComboBox __ListFolders_JComboBox = null;
private JTextField __MaxKeys_JTextField = null;
private JTextField __MaxObjects_JTextField = null;
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
 * Google Drive session used to interact with services
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

    if ( o == this.__GoogleDriveCommand_JComboBox ) {
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
    /*
    else if ( event.getActionCommand().equalsIgnoreCase("EditDownloadFolders") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String DownloadFolders = __DownloadFolders_JTextArea.getText().trim();
        String [] notes = {
            "Specify the S3 bucket folder path key (e.g., topfolder/childfolder/) to download.",
            "Only folders (directories) can be downloaded. Specify files to download with the 'DownloadFiles' command parameter.",
            "A leading / in the folder key should be used only if the S3 bucket uses a top-level /.",
            "A trailing / in the bucket prefix (S3 directory path) is required to indicate a folder.",
            "The local folder is relative to the working folder:",
            "  " + this.__working_dir,
            "${Property} notation can be used for all values to expand at run time."
        };
        String dict = (new DictionaryJDialog ( __parent, true, DownloadFolders,
            "Edit DownloadFolders Parameter", notes, "S3 Folder Path (ending in /)", "Local Folder (optionally ending in /)",10)).response();
        if ( dict != null ) {
            __DownloadFolders_JTextArea.setText ( dict );
            refresh();
        }
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditDownloadFiles") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String DownloadFiles = __DownloadFiles_JTextArea.getText().trim();
        String [] notes = {
            "Specify the bucket file object S3 key (e.g., topfolder/childfolder/file.ext) to download a file.",
            "Only files can be downloaded.  Specify folders to download with the 'DownloadFolders' command parameter.",
            "The key is the full path for the the file object.",
            "A leading / in the folder key should be used only if the S3 bucket uses a top-level /.",
            "The local file name can be * to use the same name as the S3 object.",
            "The local file is relative to the working folder:",
            "  " + this.__working_dir,
            "${Property} notation can be used for all values to expand at run time."
        };
        String dict = (new DictionaryJDialog ( __parent, true, DownloadFiles,
            "Edit DownloadFiles Parameter", notes, "S3 File Path", "Local File",10)).response();
        if ( dict != null ) {
            __DownloadFiles_JTextArea.setText ( dict );
            refresh();
        }
    }
    */
	else if ( o == __help_JButton ) {
		//HelpViewer.getInstance().showHelp("command", "AwsS3", PluginMeta.getDocumentationRootUrl());
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
	//String DownloadFolders = __DownloadFolders_JTextArea.getText().trim().replace("\n"," ");
	//String DownloadFiles = __DownloadFiles_JTextArea.getText().trim().replace("\n"," ");
	// List buckets.
	//String ListBucketsRegEx = __ListBucketsRegEx_JTextField.getText().trim();
	//String ListBucketsCountProperty = __ListBucketsCountProperty_JTextField.getText().trim();
	// List bucket objects.
	String ListObjectsScope = __ListObjectsScope_JComboBox.getSelected();
	String Prefix = __Prefix_JTextField.getText().trim();
	String ListRegEx = __ListRegEx_JTextField.getText().trim();
	String ListFiles = __ListFiles_JComboBox.getSelected();
	String ListFolders = __ListFolders_JComboBox.getSelected();
	String MaxKeys = __MaxKeys_JTextField.getText().trim();
	String MaxObjects = __MaxObjects_JTextField.getText().trim();
	String ListCountProperty = __ListCountProperty_JTextField.getText().trim();
	// Upload.
	//String UploadFolders = __UploadFolders_JTextArea.getText().trim().replace("\n"," ");
	//String UploadFiles = __UploadFiles_JTextArea.getText().trim().replace("\n"," ");
	// Output.
	String OutputTableID = __OutputTableID_JComboBox.getSelected();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String AppendOutput = __AppendOutput_JComboBox.getSelected();
	__error_wait = false;

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
	// Download.
	if ( (DownloadFolders != null) && !DownloadFolders.isEmpty() ) {
		props.set ( "DownloadFolders", DownloadFolders );
	}
	if ( (DownloadFiles != null) && !DownloadFiles.isEmpty() ) {
		props.set ( "DownloadFiles", DownloadFiles );
	}
	// List buckets.
	if ( (ListBucketsRegEx != null) && !ListBucketsRegEx.isEmpty() ) {
		props.set ( "ListBucketsRegEx", ListBucketsRegEx );
	}
	if ( (ListBucketsCountProperty != null) && !ListBucketsCountProperty.isEmpty() ) {
		props.set ( "ListBucketsCountProperty", ListBucketsCountProperty );
	}
	*/
	// List bucket objects.
	if ( (ListObjectsScope != null) && !ListObjectsScope.isEmpty() ) {
		props.set ( "ListObjectsScope", ListObjectsScope);
	}
	if ( (Prefix != null) && !Prefix.isEmpty() ) {
		props.set ( "Prefix", Prefix );
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
	if ( (MaxKeys != null) && !MaxKeys.isEmpty() ) {
		props.set ( "MaxKeys", MaxKeys );
	}
	if ( (MaxObjects != null) && !MaxObjects.isEmpty() ) {
		props.set ( "MaxObjects", MaxObjects );
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
	*/
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
	//String DownloadFolders = __DownloadFolders_JTextArea.getText().trim().replace("\n"," ");
	//String DownloadFiles = __DownloadFiles_JTextArea.getText().trim().replace("\n"," ");
	// List buckets.
	//String ListBucketsRegEx = __ListBucketsRegEx_JTextField.getText().trim();
	//String ListBucketsCountProperty = __ListBucketsCountProperty_JTextField.getText().trim();
	// List bucket objects.
	String ListObjectsScope = __ListObjectsScope_JComboBox.getSelected();
	String Prefix = __Prefix_JTextField.getText().trim();
	String ListRegEx = __ListRegEx_JTextField.getText().trim();
	String ListFiles = __ListFiles_JComboBox.getSelected();
	String ListFolders = __ListFolders_JComboBox.getSelected();
	String MaxKeys = __MaxKeys_JTextField.getText().trim();
	String MaxObjects = __MaxObjects_JTextField.getText().trim();
	String ListCountProperty = __ListCountProperty_JTextField.getText().trim();
	// Upload.
	//String UploadFolders = __UploadFolders_JTextArea.getText().trim().replace("\n"," ");
	//String UploadFiles = __UploadFiles_JTextArea.getText().trim().replace("\n"," ");
	// Output
	String OutputTableID = __OutputTableID_JComboBox.getSelected();
    String OutputFile = __OutputFile_JTextField.getText().trim();
	String AppendOutput = __AppendOutput_JComboBox.getSelected();

    // General.
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
	//__command.setCommandParameter ( "DownloadFolders", DownloadFolders );
	//__command.setCommandParameter ( "DownloadFiles", DownloadFiles );
	// List Buckets.
	//__command.setCommandParameter ( "ListBucketsRegEx", ListBucketsRegEx );
	//__command.setCommandParameter ( "ListBucketsCountProperty", ListBucketsCountProperty );
	// List Objects.
	__command.setCommandParameter ( "ListObjectsScope", ListObjectsScope );
	__command.setCommandParameter ( "Prefix", Prefix );
	__command.setCommandParameter ( "ListRegEx", ListRegEx );
	__command.setCommandParameter ( "ListFiles", ListFiles );
	__command.setCommandParameter ( "ListFolders", ListFolders );
	__command.setCommandParameter ( "MaxKeys", MaxKeys );
	__command.setCommandParameter ( "MaxObjects", MaxObjects );
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
    
    // Create a session using authentication method.
    
    try {
		String AuthenticationMethod = __command.getCommandParameters().getValue ( "AuthenticationMethod" );
		GoogleDriveAuthenticationMethodType authenticationMethod =
			GoogleDriveAuthenticationMethodType.valueOfIgnoreCase(AuthenticationMethod);
    	this.googleDriveSession = new GoogleDriveSession(authenticationMethod);
    }
    catch ( Exception e ) {
    	this.googleDriveSession = null;
    }

	// Main panel.

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Run a Google Drive command."
        + "  Google Drive provides cloud storage for files." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Google Drive stores folders and files using an ID containing characters and numbers, which is used in URLs."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"However, this command uses folder and file names as shown in the Google Drive G: drive on Windows."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Paths for this command are specified without the leading 'G:/My Drive' and should use forward slashes."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"It is recommended that file and folders on the local computer are relative to the working directory, which is:"),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel ("    " + __working_dir),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    if ( this.googleDriveSession == null ) {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
        	"<html><b>ERROR: User's Google Drive configuration is invalid</b></html>" ),
        	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    else {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
        	"User's Google Drive configuration is OK."),
        	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   	this.ignoreEvents = true; // So that a full pass of initialization can occur.

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Authentication type:"),
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

    /*
    // Panel for 'Download' parameters:
    // - map bucket objects to files and folders
    int yDownload = -1;
    JPanel download_JPanel = new JPanel();
    download_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Download", download_JPanel );

    JGUIUtil.addComponent(download_JPanel, new JLabel ("Specify files and folders to download."),
		0, ++yDownload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(download_JPanel, new JLabel ("Use the 'Edit' button to view information about S3 and local file and folder paths."),
		0, ++yDownload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(download_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yDownload, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(download_JPanel, new JLabel ("Download folders:"),
        0, ++yDownload, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DownloadFolders_JTextArea = new JTextArea (6,35);
    __DownloadFolders_JTextArea.setLineWrap ( true );
    __DownloadFolders_JTextArea.setWrapStyleWord ( true );
    __DownloadFolders_JTextArea.setToolTipText("Key1:Folder1,Key2:Folder2,...");
    __DownloadFolders_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(download_JPanel, new JScrollPane(__DownloadFolders_JTextArea),
        1, yDownload, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(download_JPanel, new JLabel ("S3 bucket key(s) (prefix) and local folder(s)."),
        3, yDownload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(download_JPanel, new SimpleJButton ("Edit","EditDownloadFolders",this),
        3, ++yDownload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(download_JPanel, new JLabel ("Download files:"),
        0, ++yDownload, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DownloadFiles_JTextArea = new JTextArea (6,35);
    __DownloadFiles_JTextArea.setLineWrap ( true );
    __DownloadFiles_JTextArea.setWrapStyleWord ( true );
    __DownloadFiles_JTextArea.setToolTipText("Key1:File1,Key2:File2,...");
    __DownloadFiles_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(download_JPanel, new JScrollPane(__DownloadFiles_JTextArea),
        1, yDownload, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(download_JPanel, new JLabel ("S3 bucket key(s) (prefix) and local file(s)."),
        3, yDownload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(download_JPanel, new SimpleJButton ("Edit","EditDownloadFiles",this),
        3, ++yDownload, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        */

    /*
    // Panel for 'List Buckets' parameters.
    int yListBuckets = -1;
    JPanel listBuckets_JPanel = new JPanel();
    listBuckets_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "List Buckets", listBuckets_JPanel );

    JGUIUtil.addComponent(listBuckets_JPanel, new JLabel ("List all buckets that are visible to the user based on the profile."),
		0, ++yListBuckets, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listBuckets_JPanel, new JLabel ("Use * in the regular expression as wildcards to filter the results."),
		0, ++yListBuckets, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listBuckets_JPanel, new JLabel ("See the 'Output' tab to specify the output table and/or file for the bucket list."),
		0, ++yListBuckets, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listBuckets_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yListBuckets, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listBuckets_JPanel, new JLabel ( "Regular expression:"),
        0, ++yListBuckets, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ListBucketsRegEx_JTextField = new JTextField ( "", 30 );
    __ListBucketsRegEx_JTextField.setToolTipText("Regular expression to filter results, default=glob (*) style");
    __ListBucketsRegEx_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listBuckets_JPanel, __ListBucketsRegEx_JTextField,
        1, yListBuckets, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listBuckets_JPanel, new JLabel ( "Optional - regular expression filter (default=none)."),
        3, yListBuckets, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listBuckets_JPanel, new JLabel("List buckets count property:"),
        0, ++yListBuckets, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ListBucketsCountProperty_JTextField = new JTextField ( "", 30 );
    __ListBucketsCountProperty_JTextField.setToolTipText("Specify the property name for the object list result size, can use ${Property} notation");
    __ListBucketsCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listBuckets_JPanel, __ListBucketsCountProperty_JTextField,
        1, yListBuckets, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listBuckets_JPanel, new JLabel ( "Optional - processor property to set as bucket count." ),
        3, yListBuckets, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        */

    // Panel for 'List Files' parameters:
    // - this includes filtering
    int yListFiles = -1;
    JPanel listFiles_JPanel = new JPanel();
    listFiles_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "List Files", listFiles_JPanel );

    JGUIUtil.addComponent(listFiles_JPanel, new JLabel (
    	"List all Google Drive files that are visible to the user based on credentials."
    	+ "  See the 'Output' tab to specify the output file and/or table for the output list."),
		0, ++yListFiles, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listFiles_JPanel, new JLabel (
    	"Limit the output using parameters as follows and by using the 'Regular expression', 'List files', and 'List folders' filters."),
		0, ++yListFiles, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
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
    		+ "       <th" + tdStyle + ">Prefix to match</th>"
    		+ "    </tr>"
    		+ "    <tr" + trStyle + ">"
    		+ "       <td" + tdStyle + ">One object</td>"
    		+ "       <td" + tdStyle + ">All (default)</td>"
    		+ "       <td" + tdStyle + ">Path for the object (/path/to/file)</td>"
    		+ "    </tr>"
    		+ "    <tr" + trStyle + ">"
    		+ "       <td" + tdStyle + ">Files in root</td>"
    		+ "       <td" + tdStyle + ">Folder</td>"
    		+ "       <td" + tdStyle + "></td>"
    		+ "    </tr>"
    		+ "    <tr" + trStyle + ">"
    		+ "       <td" + tdStyle + ">Files in folder</td>"
    		+ "       <td" + tdStyle + ">Folder</td>"
    		+ "       <td" + tdStyle + ">Folder path (key) ending in /</td>"
    		+ "    </tr>"
    		+ "    <tr" + trStyle + ">"
    		+ "       <td" + tdStyle + ">All objects matching leading path</td>"
    		+ "       <td" + tdStyle + ">All (default)</td>"
    		+ "       <td" + tdStyle + ">Path (key) to match, can be partial file name</td>"
    		+ "    </tr>"
    		+ "    <tr" + trStyle + ">"
    		+ "       <td" + tdStyle + ">All files in bucket</td>"
    		+ "       <td" + tdStyle + ">All (default)</td>"
    		+ "       <td" + tdStyle + "></td>"
    		+ "    </tr>"
    		+ "  </table>"
    		+ "</html>";
    JGUIUtil.addComponent(listFiles_JPanel, new JLabel (table),
		0, ++yListFiles, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    Message.printStatus(2, "", table);
    /*
    JGUIUtil.addComponent(listFiles_JPanel, new JLabel ("    list all objects in a bucket: ListScope=" + _All + ", Prefix"),
		0, ++yListFiles, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listFiles_JPanel, new JLabel ("    no Prefix (and list only root) - list all the top-level (root) folder objects"),
		0, ++yListFiles, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listFiles_JPanel, new JLabel ("    Prefix = folder1/ - list 'folder1/ objects (output will include the trailing /)"),
		0, ++yListFiles, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listFiles_JPanel, new JLabel ("    Prefix = file or folder1/folder2/file - list one file (must match exactly)"),
		0, ++yListFiles, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
		*/
    JGUIUtil.addComponent(listFiles_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yListFiles, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   JGUIUtil.addComponent(listFiles_JPanel, new JLabel ( "List scope:"),
		0, ++yListFiles, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ListObjectsScope_JComboBox = new SimpleJComboBox ( false );
	__ListObjectsScope_JComboBox.setToolTipText("Scope (depth) of the list, which controls the output");
	List<String> listRootChoices = new ArrayList<>();
	listRootChoices.add ( "" );	// Default.
	listRootChoices.add ( __command._All );
	listRootChoices.add ( __command._Folder );
	//listRootChoices.add ( __command._Root );
	__ListObjectsScope_JComboBox.setData(listRootChoices);
	__ListObjectsScope_JComboBox.select ( 0 );
	__ListObjectsScope_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(listFiles_JPanel, __ListObjectsScope_JComboBox,
		1, yListFiles, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listFiles_JPanel, new JLabel(
		"Optional - scope (depth) of the listing (default=" + __command._All + ")."),
		3, yListFiles, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listFiles_JPanel, new JLabel ( "Prefix to match:"),
        0, ++yListFiles, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Prefix_JTextField = new JTextField ( "", 30 );
    __Prefix_JTextField.setToolTipText("Specify the start of the key to match, ending in / if listing a folder's contents.");
    __Prefix_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listFiles_JPanel, __Prefix_JTextField,
        1, yListFiles, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listFiles_JPanel, new JLabel ( "Optional - object key prefix to match (default=list all)."),
        3, yListFiles, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listFiles_JPanel, new JLabel ( "Regular expression:"),
        0, ++yListFiles, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ListRegEx_JTextField = new JTextField ( "", 30 );
    __ListRegEx_JTextField.setToolTipText("Regular expression to filter results, default=glob (*) style");
    __ListRegEx_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listFiles_JPanel, __ListRegEx_JTextField,
        1, yListFiles, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listFiles_JPanel, new JLabel ( "Optional - regular expression filter (default=none)."),
        3, yListFiles, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

   JGUIUtil.addComponent(listFiles_JPanel, new JLabel ( "List files?:"),
		0, ++yListFiles, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ListFiles_JComboBox = new SimpleJComboBox ( false );
	__ListFiles_JComboBox.setToolTipText("Indicate whether to list files?");
	List<String> listFilesChoices = new ArrayList<>();
	listFilesChoices.add ( "" );	// Default.
	listFilesChoices.add ( __command._False );
	listFilesChoices.add ( __command._True );
	__ListFiles_JComboBox.setData(listFilesChoices);
	__ListFiles_JComboBox.select ( 0 );
	__ListFiles_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(listFiles_JPanel, __ListFiles_JComboBox,
		1, yListFiles, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listFiles_JPanel, new JLabel(
		"Optional - list files? (default=" + __command._True + ")."),
		3, yListFiles, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

   JGUIUtil.addComponent(listFiles_JPanel, new JLabel ( "List folders?:"),
		0, ++yListFiles, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ListFolders_JComboBox = new SimpleJComboBox ( false );
	__ListFolders_JComboBox.setToolTipText("Indicate whether to list files?");
	List<String> listFoldersChoices = new ArrayList<>();
	listFoldersChoices.add ( "" );	// Default.
	listFoldersChoices.add ( __command._False );
	listFoldersChoices.add ( __command._True );
	__ListFolders_JComboBox.setData(listFoldersChoices);
	__ListFolders_JComboBox.select ( 0 );
	__ListFolders_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(listFiles_JPanel, __ListFolders_JComboBox,
		1, yListFiles, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listFiles_JPanel, new JLabel(
		"Optional - list folders? (default=" + __command._True + ")."),
		3, yListFiles, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listFiles_JPanel, new JLabel ( "Maximum keys:"),
        0, ++yListFiles, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MaxKeys_JTextField = new JTextField ( "", 10 );
    __MaxKeys_JTextField.setToolTipText("Used internally by AWS web services.");
    __MaxKeys_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listFiles_JPanel, __MaxKeys_JTextField,
        1, yListFiles, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listFiles_JPanel, new JLabel ( "Optional - maximum number of object keys read per request (default="
    	+ this.__command._MaxKeys + ")."),
        3, yListFiles, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listFiles_JPanel, new JLabel ( "Maximum objects:"),
        0, ++yListFiles, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MaxObjects_JTextField = new JTextField ( "", 10 );
    __MaxObjects_JTextField.setToolTipText("Use to limit the size of the query results.");
    __MaxObjects_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listFiles_JPanel, __MaxObjects_JTextField,
        1, yListFiles, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listFiles_JPanel, new JLabel ( "Optional - maximum number of object read (default=2000)."),
        3, yListFiles, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(listFiles_JPanel, new JLabel("List objects count property:"),
        0, ++yListFiles, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ListCountProperty_JTextField = new JTextField ( "", 30 );
    __ListCountProperty_JTextField.setToolTipText("Specify the property name for the bucket object list result size, can use ${Property} notation");
    __ListCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(listFiles_JPanel, __ListCountProperty_JTextField,
        1, yListFiles, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(listFiles_JPanel, new JLabel ( "Optional - processor property to set as object count." ),
        3, yListFiles, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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
    	"The following parameters are used with 'List Buckets' and 'List Objects' commands."),
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
	// List buckets.
	String ListBucketsRegEx = "";
	String ListBucketsCountProperty = "";
	// List bucket objects.
	String ListObjectsScope = "";
	String Prefix = "";
	String ListRegEx = "";
	String ListFiles = "";
	String ListFolders = "";
	String MaxKeys = "";
	String MaxObjects = "";
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
		// List buckets.
		ListBucketsRegEx = parameters.getValue ( "ListBucketsRegEx" );
		ListBucketsCountProperty = parameters.getValue ( "ListBucketsCountProperty" );
		// List bucket objects.
		ListObjectsScope = parameters.getValue ( "ListObjectsScope" );
		Prefix = parameters.getValue ( "Prefix" );
		ListRegEx = parameters.getValue ( "ListRegEx" );
		ListFiles = parameters.getValue ( "ListFiles" );
		ListFolders = parameters.getValue ( "ListFolders" );
		MaxKeys = parameters.getValue ( "MaxKeys" );
		MaxObjects = parameters.getValue ( "MaxObjects" );
		ListCountProperty = parameters.getValue ( "ListCountProperty" );
		// Upload.
		UploadFolders = parameters.getValue ( "UploadFolders" );
		UploadFiles = parameters.getValue ( "UploadFiles" );
		// Output
		OutputTableID = parameters.getValue ( "OutputTableID" );
		OutputFile = parameters.getValue ( "OutputFile" );
		AppendOutput = parameters.getValue ( "AppendOutput" );
		//IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
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
        if ( DownloadFolders != null ) {
            __DownloadFolders_JTextArea.setText ( DownloadFolders );
        }
        if ( DeleteFoldersMinDepth != null ) {
            __DeleteFoldersMinDepth_JTextField.setText ( DeleteFoldersMinDepth );
        }
        if ( DownloadFiles != null ) {
            __DownloadFiles_JTextArea.setText ( DownloadFiles );
        }
        if ( ListBucketsRegEx != null ) {
            __ListBucketsRegEx_JTextField.setText ( ListBucketsRegEx );
        }
        if ( ListBucketsCountProperty != null ) {
            __ListBucketsCountProperty_JTextField.setText ( ListBucketsCountProperty );
        }
        */
		if ( JGUIUtil.isSimpleJComboBoxItem(__ListObjectsScope_JComboBox, ListObjectsScope,JGUIUtil.NONE, null, null ) ) {
			__ListObjectsScope_JComboBox.select ( ListObjectsScope );
		}
		else {
            if ( (ListObjectsScope == null) || ListObjectsScope.equals("") ) {
				// New command...select the default.
				__ListObjectsScope_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"ListObjectsScope parameter \"" + ListObjectsScope + "\".  Select a value or Cancel." );
			}
		}
        if ( Prefix != null ) {
            __Prefix_JTextField.setText ( Prefix );
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
        if ( MaxKeys != null ) {
            __MaxKeys_JTextField.setText ( MaxKeys );
        }
        if ( MaxObjects != null ) {
            __MaxObjects_JTextField.setText ( MaxObjects );
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
	// Download.
	DownloadFolders = __DownloadFolders_JTextArea.getText().trim().replace("\n"," ");
	DownloadFiles = __DownloadFiles_JTextArea.getText().trim().replace("\n"," ");
	// List buckets.
	ListBucketsRegEx = __ListBucketsRegEx_JTextField.getText().trim();
	ListBucketsCountProperty = __ListBucketsCountProperty_JTextField.getText().trim();
	*/
	// List bucket objects.
	ListObjectsScope = __ListObjectsScope_JComboBox.getSelected();
	Prefix = __Prefix_JTextField.getText().trim();
	ListRegEx = __ListRegEx_JTextField.getText().trim();
	ListFiles = __ListFiles_JComboBox.getSelected();
	ListFolders = __ListFolders_JComboBox.getSelected();
	MaxKeys = __MaxKeys_JTextField.getText().trim();
	MaxObjects = __MaxObjects_JTextField.getText().trim();
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
	// List buckets.
	props.add ( "ListBucketsRegEx=" + ListBucketsRegEx );
	props.add ( "ListBucketsCountProperty=" + ListBucketsCountProperty );
	// List bucket objects.
	props.add ( "ListObjectsScope=" + ListObjectsScope );
	props.add ( "Prefix=" + Prefix );
	props.add ( "ListRegEx=" + ListRegEx );
	props.add ( "ListFiles=" + ListFiles );
	props.add ( "ListFolders=" + ListFolders );
	props.add ( "MaxKeys=" + MaxKeys );
	props.add ( "MaxObjects=" + MaxObjects );
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
 * Set the parameter tab based on the selected command.
 */
private void setTabForGoogleDriveCommand() {
	String command = __GoogleDriveCommand_JComboBox.getSelected();
	/*
	if ( command.equalsIgnoreCase("" + AwsS3CommandType.COPY_OBJECTS) ) {
		__main_JTabbedPane.setSelectedIndex(0);
	}
	else if ( command.equalsIgnoreCase("" + AwsS3CommandType.DELETE_OBJECTS) ) {
		__main_JTabbedPane.setSelectedIndex(1);
	}
	else if ( command.equalsIgnoreCase("" + AwsS3CommandType.DOWNLOAD_OBJECTS) ) {
		__main_JTabbedPane.setSelectedIndex(2);
	}
	else if ( command.equalsIgnoreCase("" + AwsS3CommandType.LIST_BUCKETS) ) {
		__main_JTabbedPane.setSelectedIndex(3);
	}
	*/
	if ( command.equalsIgnoreCase("" + GoogleDriveCommandType.LIST) ) {
		__main_JTabbedPane.setSelectedIndex(0);
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