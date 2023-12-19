// GoogleDriveToolkit - utility functions for Google Drive as singleton

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;

import RTi.Util.Message.Message;

/**
 * Google Drive toolkit singleton.
 * Retrieve the instance with getInstance() and then use methods.
 */
public class GoogleDriveToolkit {
	/**
	 * Singleton object.
	 */
	private static GoogleDriveToolkit instance = null;

	/**
	 * Get the AwsToolkit singleton instance.
	 */
	public static GoogleDriveToolkit getInstance() {
		// Use lazy loading.
		if ( instance == null ) {
			instance = new GoogleDriveToolkit();
		}
		return instance;
	}
	
	/**
	 * Private constructor.
	 */
	private GoogleDriveToolkit () {
	}

	/**
	 * Get the Google Drive file ID given a path to the file.
	 * This code was generated by ChatGPT.
	 * @param driveService Drive service object
	 * @param googleDriveFilePath folder path using syntax "/path/to/folder/" (no leading G: or G:/My Drive).
	 * The leading and trailing / are optional.
	 * @return the Google Drive folder ID, or null if not matched
	 * @throws IOException
	 */
	public String getFileIdForPath ( Drive driveService, String googleDriveFilePath ) throws IOException {
		String routine = getClass().getSimpleName() + ".getFileIdForPath";
		boolean debug = true;
		if ( debug ) {
		 	Message.printStatus ( 2, routine, "Getting Google Drive ID for file path \"" + googleDriveFilePath + "\"." );
		}
		// First get the ID for the parent folder:
		// - make sure to convert to forward slashes
		File file = new File(googleDriveFilePath);
		String parentFolderPath = file.getParent().replace("\\", "/");
		if ( debug ) {
		 	Message.printStatus ( 2, routine, "Getting Google Drive ID for parent folder path \"" + parentFolderPath + "\"." );
		}
		String parentFolderId = getFolderIdForPath(driveService,parentFolderPath);
		if ( parentFolderId == null ) {
   			String message = "Cannot get Google Drive ID for parent folder \"" + parentFolderPath + "\".";
		 	Message.printWarning ( 3, routine, message );
			return null;
		}
		else {
			if ( debug ) {
		 		Message.printStatus ( 2, routine, "Parent folder path \"" + parentFolderPath + "\" has ID \"" +
		 			parentFolderId + "\"." );
			}
		}
		// Then list the files in the folder to find a matching name.
   		FileList result = null;
   		boolean listTrashed = false;
   		StringBuilder q = new StringBuilder("'" + parentFolderId + "' in parents and trashed=" + listTrashed );
   		try {
   			result = driveService
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
   				// Page size for returned files.
       			//.setPageSize(10)
       			// Which fields to include in the response:
       			// - if not specified nulls will be returned by the "get" methods below
       			// - see https://developers.google.com/drive/api/guides/ref-search-terms#drive_properties
       			// - don't specify any fields to return all fields (will be slower) but it is a pain to figure out fields
       			//   since they don't seem to be documented well
       			// - specifying * returns everything and may be slower
       			// - when using files(), need to do a better job including only file fields in the parentheses
       			//.setFields("nextPageToken, files(createdTime, description, id, mimeType, modifiedTime, name, ownedByMe, owners, parents, permissions, shared, sharingUser, size, trashed, trashedTime)")
       			//.setFields("nextPageToken, createdTime, description, id, mimeType, modifiedTime, name, ownedByMe, owners, parents, permissions, shared, sharingUser, size, trashed, trashedTime")
       			.setFields("*")
       			// Invoke the remote operation.
       			.execute();

   			List<com.google.api.services.drive.model.File> files = result.getFiles();
   			if ( (files == null) || files.isEmpty() ) {
   				Message.printStatus(2, routine, "No files found.");
   			}
   			else {
   				// Have files and folders to process.
   				for ( com.google.api.services.drive.model.File googleFile : files ) {
   					if ( googleFile.getName().equals(file.getName()) ) {
   						// Have a matching file:
   						// - return its ID
   						return googleFile.getId();
   					}
   				}
   			}
   		}
   		catch ( Exception e ) {
   			String message = "Error listing Google Drive files in parent folder \"" + parentFolderPath + "\".";
		 	Message.printWarning ( 3, routine, message );
		 	Message.printWarning ( 3, routine, e );
   		}
		// Return the ID for the matching file, or null if no match.
		return null;
	}

	/**
	 * Get the Google Drive folder ID given a path to the folder.
	 * This code was generated by ChatGPT.
	 * @param driveService Drive service object
	 * @param folderPath folder path using syntax "/path/to/folder/" (no leading G: or G:/My Drive).
	 * The leading and trailing / are optional.
	 * @return the Google Drive folder ID, or null if not matched
	 * @throws IOException
	 */
	public String getFolderIdForPath ( Drive driveService, String folderPath ) throws IOException {
		String routine = getClass().getSimpleName() + ".getFolderIdForPath";
        // Split the path into folder names.
		if ( folderPath.startsWith("/") ) {
			// Remove the leading slash because it will result in an empty path below.
			if ( folderPath.length() == 1 ) {
				return null;
			}
			else {
				folderPath = folderPath.substring(1);
			}
		}
		if ( folderPath.endsWith("/") ) {
			// Remove the trailing slash because it will result in an empty path below.
			if ( folderPath.length() == 1 ) {
				return null;
			}
			else {
				folderPath = folderPath.substring(0,folderPath.length());
			}
		}
        String[] folderNames = folderPath.split("/");

        // Initialize the root folder ID.
        String currentFolderId = "root";

        // Iterate through each folder in the path.
        for ( String folderName : folderNames ) {
            // Search for the folder by name in the parent folder:
        	// - first time through will list 'root', then sub-folders
        	// - only match the folder name
            String q = "name='" + folderName + "' and '" + currentFolderId + "' in parents";
            Message.printStatus(2, routine, "Getting files using q=\"" + q + "\"");
            FileList result = driveService.files().list()
                .setQ(q)
                .setFields("files(id)")
                .execute();

            // Check if the folder (path part) was found.
            if ( (result.getFiles() != null) && !result.getFiles().isEmpty()) {
                // Update the current folder ID for the next iteration.
                currentFolderId = result.getFiles().get(0).getId();
                Message.printStatus(2, routine, "Set currentFolderId=\"" + currentFolderId + "\"");
            }
            else {
                // Folder not found, return null.
                Message.printStatus(2, routine, "Folder not found. Returning null.");
                return null;
            }
        }

        // Return the final folder ID.
        return currentFolderId;
    }

	/**
	 * Get the parent path given the parent folder Google Drive ID.
	 * @param driveService Drive service instance
	 * @param folderId the folder ID to process
	 * @return the array of parent paths starting from the top-most folder (e.g., "My Drive")
	 * @throws IOException
	 */
    public String getParentFolderPathFromFolderId ( Drive driveService, String folderId ) throws IOException {
		List<String> parentPaths = getParentFoldersFromFolderId(driveService, folderId);
		StringBuilder parentPath = new StringBuilder();
		// The paths will be from the innermost folder to the outermost.
		for ( String part : parentPaths ) {
			if ( parentPath.length() > 0 ) {
				parentPath.append("/");
			}
			parentPath.append(part);
		}
    	return parentPath.toString();
    }

	/**
	 * Get the parent folders given the parent folder Google Drive ID.
	 * The initial code was generated by ChatGPT.
	 * @param driveService Drive service instance
	 * @param folderId the folder ID to process
	 * @return the array of parent paths starting from the top-most folder (e.g., "My Drive")
	 * @throws IOException
	 */
    private List<String> getParentFoldersFromFolderId ( Drive driveService, String folderId ) throws IOException {
    	String routine = getClass().getSimpleName() + ".getParentFolders";
        List<String> parentFolders = new ArrayList<>();
        boolean debug = false;
        if ( debug ) {
        	Message.printStatus(2,routine,"Getting folders for ID=" + folderId);
        }

   		com.google.api.services.drive.model.File folder = driveService.files()
   			.get(folderId)
   			.setFields("*")
   			.execute();
        if ( debug ) {
        	Message.printStatus(2,routine,"Google folder for ID=" + folder);
        }
        if ( folder != null ) {
        	// Add the requesting folder.
            parentFolders.add(folder.getName());
            if ( debug ) {
            	Message.printStatus(2,routine,"Google folder parents=" + folder.getParents());
        	   	if ( folder.getParents() != null ) {
        		   	Message.printStatus(2,routine,"Google folder parents size=" + folder.getParents().size());
        	   	}
            }
        }

        while ( (folder != null) && (folder.getParents() != null) ) {
            String parentId = folder.getParents().get(0); // Get the primary parent.
            if ( debug ) {
            	Message.printStatus(2,routine,"Parent ID=" + parentId);
            }
            folder = driveService.files()
            	.get(parentId)
            	.setFields("*")
            	.execute();
            parentFolders.add(folder.getName());
            if ( debug ) {
            	Message.printStatus(2,routine,"Adding parent folder name=" + folder.getName());
            }
        }

        // Reverse the order since moved up through parents.
        List<String> parentFoldersSorted = new ArrayList<>();
        for ( int i = parentFolders.size() - 1; i >= 0; i-- ) {
        	parentFoldersSorted.add(parentFolders.get(i));
        }
        return parentFoldersSorted;
    }
    
	
}