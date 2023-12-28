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
import com.google.api.services.drive.model.DriveList;
import com.google.api.services.drive.model.FileList;

import RTi.Util.Message.Message;

/**
 * Google Drive toolkit singleton.
 * Retrieve the instance with getInstance() and then use methods.
 * This toolkit contains methods for useful tasks,
 * such as looking up a Google Drive file/folder identifier given its path.
 */
public class GoogleDriveToolkit {
	/**
	 * Singleton object.
	 */
	private static GoogleDriveToolkit instance = null;

	/**
	 * Static string to standardize leading folder for 'My Drive'.
	 */
	public static final String MY_DRIVE = "My Drive";

	/**
	 * Static string to standardize leading folder for 'Shared with me'.
	 */
	public static final String SHARED_WITH_ME = "Shared with me";

	/**
	 * Static string to standardize leading folder for 'Shared drives'.
	 */
	public static final String SHARED_DRIVES = "Shared drives";

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
		String parentFolderId = getFolderIdForPath ( driveService, parentFolderPath );
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
	 * This method should not be called with empty or root (/) path.
	 * @param driveService Drive service object
	 * @param folderPath folder path using syntax "/path/to/folder/" (no leading G: or G:/My Drive).
	 * The leading and trailing / are optional.
	 * @return the Google Drive folder ID, or null if not matched
	 * @throws IOException
	 */
	public String getFolderIdForPath ( Drive driveService, String folderPath ) throws IOException {
		String routine = getClass().getSimpleName() + ".getFolderIdForPath";

		// Remove the leading 'My Drive', which is not used in API calls.
		if ( folderPath.equals(MY_DRIVE) ) {
			// Did not match a specific sub-folder.
			return null;
		}
		else if ( folderPath.startsWith("/" + MY_DRIVE) ) {
			// Remove the leading "/My Drive".
			folderPath = folderPath.substring(MY_DRIVE.length() + 1);
		}
		else if ( folderPath.startsWith(MY_DRIVE) ) {
			// Remove the leading "My Drive".
			folderPath = folderPath.substring(MY_DRIVE.length());
		}
		else {
			// No leading "My Drive" variation.
		}

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

        // Split the path into folder names.
        String[] folderNames = folderPath.split("/");

        // Initialize the root folder ID.
        String currentFolderId = "root";

        // Iterate through each folder in the path.
        for ( String folderName : folderNames ) {
            // Search for the folder by name in the parent folder:
        	// - first time through will list 'root', then sub-folders
        	// - only match the folder name
        	// The following works with the OAuth authentication.
            String q = "name='" + folderName + "' and '" + currentFolderId + "' in parents";
            Message.printStatus(2, routine, "Getting files using q=\"" + q + "\"");
            // Only the file ID is needed.
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
            	if ( result.getFiles() == null ) {
            		Message.printStatus(2, routine, "getFiles() returned null. Returning null.");
            	}
            	else if ( result.getFiles().isEmpty() ) {
            		Message.printStatus(2, routine, "getFiles() returned empty list. Returning null.");
            	}
                return null;
            }
        }

        // Return the final folder ID.
        return currentFolderId;
    }

	/**
	 * Get the Google Drive shared drives folder ID given a path to the folder.
	 * This should be called for folders that are in shared drives (but not shared folders).
	 * This ensures that the path is not mixed up with normal My Drive,
	 * such as service accounts where typically only shared resources are available.
	 * @param driveService Drive service object
	 * @param sharedDrivePath shared folder path using syntax "/Shared drives/driveName/path/to/folder/"
	 * The leading and trailing / are optional.
	 * @return the Google Drive folder ID, or null if not matched
	 * @throws IOException
	 */
	public String getFolderIdForSharedDrivesPath ( Drive driveService, String sharedDrivePath ) throws IOException {
		String routine = getClass().getSimpleName() + ".getFolderIdForSharedDrivesPath";

		// Remove the leading "Shared drives" and "/Shared drives".
		if ( sharedDrivePath.equals(SHARED_DRIVES) ) {
			// Did not match a specific sub-folder.
			return null;
		}
		else if ( sharedDrivePath.startsWith("/" + SHARED_DRIVES) ) {
			// Remove the leading "/Shared drives".
			sharedDrivePath = sharedDrivePath.substring(SHARED_DRIVES.length() + 1);
		}
		else if ( sharedDrivePath.startsWith(SHARED_DRIVES) ) {
			// Remove the leading "Shared drives".
			sharedDrivePath = sharedDrivePath.substring(SHARED_DRIVES.length());
		}
		else {
			// No leading "Shared drives" variation.
		}

		if ( sharedDrivePath.startsWith("/") ) {
			// Remove the leading slash because it will result in an empty path below.
			if ( sharedDrivePath.length() == 1 ) {
				return null;
			}
			else {
				sharedDrivePath = sharedDrivePath.substring(1);
			}
		}
		if ( sharedDrivePath.endsWith("/") ) {
			// Remove the trailing slash because it will result in an empty path below.
			if ( sharedDrivePath.length() == 1 ) {
				return null;
			}
			else {
				sharedDrivePath = sharedDrivePath.substring(0,sharedDrivePath.length());
			}
		}

		// Split the path into parts that can be checked against Google Drive folders:
		// - the first part is the drive name
        String[] folderNames = sharedDrivePath.split("/");

        // For shared folders can't use "root" because that is used with My Drive
        // sharing user, not the current user.
        // Therefore list shared folders and match the name with the first part of the path.

        // List the shared drives to match the first part and then use that for the initial folder ID.

   		// Output the names and IDs for up to 10 files.
   		DriveList result0 = null;
   		Drive.Drives.List request = null;
		// Do the initial request.
   		String q0 = "name='" + folderNames[0] + "'";
		request = driveService
			// Request to execute.
			.drives()
			// Holds the parameters for the request.
			.list()
			// Query the shared drive name.
			.setQ(q0)
			// Set the fields that are returned (creation time, etc.).
   			.setFields("*");
		// Invoke the remote operation.
		result0 = request.execute();
		
		if ( (result0 == null) || (result0.size() == 0) ) {
			// No shared drives so return null;
			Message.printStatus(2, routine, "Could not match shared drive \"" + folderNames[1] + "\".");
			return null;
		}

		// Get the drive ID for the matching shared drive.
		String currentFolderId = result0.getDrives().get(0).getId();

        // Iterate through each folder in the path:
   		// - skip the first part, which was checked above
        for ( int i = 1; i < folderNames.length; i++ ) {
        	String folderName = folderNames[i];
            // Search for the folder by name in the parent folder:
        	// - first time through will list top shared drive, then sub-folders
        	// - only match the folder name
            String q = "mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and '"
            	+ currentFolderId + "' in parents"; // and '"; + currentFolderId + "' in drive";
            Message.printStatus(2, routine, "Getting files using q=\"" + q + "\"");
            // Only the file ID is needed.
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
            	if ( result.getFiles() == null ) {
            		Message.printStatus(2, routine, "getFiles() returned null. Returning null.");
            	}
            	else if ( result.getFiles().isEmpty() ) {
            		Message.printStatus(2, routine, "getFiles() returned empty list. Returning null.");
            	}
                return null;
            }
        }

        // Return the final folder ID.
   		Message.printStatus(2, routine, "Found ID " + currentFolderId + " for shared folder path \"" + sharedDrivePath + "\".");
        return currentFolderId;
	}

	/**
	 * Get the Google Drive shared folder ID given a path to the folder.
	 * This should be called for folders that are shared (but not shared drives).
	 * This ensures that the path is not mixed up with normal My Drive,
	 * such as service accounts where typically only shared resources are available.
	 * @param driveService Drive service object
	 * @param sharedFolderPath shared folder path using syntax "/path/to/folder/"
	 * The leading and trailing / are optional.
	 * @return the Google Drive folder ID, or null if not matched
	 * @throws IOException
	 */
	public String getFolderIdForSharedWithMePath ( Drive driveService, String sharedFolderPath ) throws IOException {
		String routine = getClass().getSimpleName() + ".getFolderIdForSharedWithMePath";

		// Remove the leading "Shared with me" and "/Shared with me".
		if ( sharedFolderPath.equals(SHARED_WITH_ME) ) {
			// Did not match a specific sub-folder.
			return null;
		}
		else if ( sharedFolderPath.startsWith("/" + SHARED_WITH_ME) ) {
			// Remove the leading "/Shared with me".
			sharedFolderPath = sharedFolderPath.substring(SHARED_WITH_ME.length() + 1);
		}
		else if ( sharedFolderPath.startsWith(SHARED_WITH_ME) ) {
			// Remove the leading "Shared with me".
			sharedFolderPath = sharedFolderPath.substring(SHARED_WITH_ME.length());
		}
		else {
			// No leading "Shared with me" variation.
		}

		if ( sharedFolderPath.startsWith("/") ) {
			// Remove the leading slash because it will result in an empty path below.
			if ( sharedFolderPath.length() == 1 ) {
				return null;
			}
			else {
				sharedFolderPath = sharedFolderPath.substring(1);
			}
		}
		if ( sharedFolderPath.endsWith("/") ) {
			// Remove the trailing slash because it will result in an empty path below.
			if ( sharedFolderPath.length() == 1 ) {
				return null;
			}
			else {
				sharedFolderPath = sharedFolderPath.substring(0,sharedFolderPath.length());
			}
		}

		// Split the path into parts that can be checked against Google Drive folders.
        String[] folderNames = sharedFolderPath.split("/");

        // For shared folders can't use "root" because the shared folder exists in the root of the
        // sharing user, not the current user.
        // Therefore list shared folders and match the name with the first part of the path.

        // List the top-level folders to match the first part and then use that for the initial folder ID:
        // - sharedWithMe=true is required to list the shared folder (but is not required below to list subfolder contents)

        String q0 = "mimeType='application/vnd.google-apps.folder' and sharedWithMe=true";
        Message.printStatus(2, routine, "Getting files using q=\"" + q0 + "\"");
        // Only the file ID is needed.
        FileList result0 = driveService.files().list()
            .setQ(q0)
            .setFields("files(id)")
            .execute();

   		List<com.google.api.services.drive.model.File> files = result0.getFiles();
        String currentFolderId = null;
   		if ( (files == null) || files.isEmpty() ) {
   			Message.printStatus(2, routine, "No shared folders.");
   			return null;
   		}
   		else {
   			currentFolderId = files.get(0).getId();
   		}

        // Iterate through each folder in the path:
   		// - skip the first part, which was checked above
        for ( int i = 1; i < folderNames.length; i++ ) {
        	String folderName = folderNames[i];
            // Search for the folder by name in the parent folder:
        	// - first time through will list top shared folder, then sub-folders
        	// - only match the folder name
        	// - using sharedWithMe=true here will return zero entries because only the top-level is shared?
            String q = "name='" + folderName + "' and '" + currentFolderId + "' in parents";
            Message.printStatus(2, routine, "Getting files using q=\"" + q + "\"");
            // Only the file ID is needed.
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
            	if ( result.getFiles() == null ) {
            		Message.printStatus(2, routine, "getFiles() returned null. Returning null.");
            	}
            	else if ( result.getFiles().isEmpty() ) {
            		Message.printStatus(2, routine, "getFiles() returned empty list. Returning null.");
            	}
                return null;
            }
        }

        // Return the final folder ID.
   		Message.printStatus(2, routine, "Found ID " + currentFolderId + " for shared folder path \"" + sharedFolderPath + "\".");
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

	/**
	 * Determine whether the path starts with '/Shared drives' or a variation.
	 * @return true if a "Shared drives" path
	 */
	public boolean pathStartsWithSharedDrives ( String folderPath ) {
		String slashSharedDrives = "/" + SHARED_DRIVES;
		if ( folderPath.startsWith(SHARED_DRIVES) || folderPath.startsWith(slashSharedDrives) ) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Determine whether the path starts with '/Shared with me' or a variation.
	 * @return true if a "Shared with me" path
	 */
	public boolean pathStartsWithSharedWithMe ( String folderPath ) {
		String slashSharedWithMe = "/" + SHARED_WITH_ME;
		if ( folderPath.startsWith(SHARED_WITH_ME) || folderPath.startsWith(slashSharedWithMe) ) {
			return true;
		}
		else {
			return false;
		}
	}

}