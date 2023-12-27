// GoogleDriveCommandType - Google Drive command enumeration

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

import java.util.ArrayList;
import java.util.List;

/**
Google Drive commands, as enumeration to simplify code.
*/
public enum GoogleDriveCommandType {
	/**
	Copy objects.
	*/
	//COPY_OBJECTS ( "CopyObjects", "Copy Google Drive objects" ),

	/**
	Delete objects.
	
	*/
	//DELETE_OBJECTS ( "DeleteObjects", "Delete Google Drive objects" ),

	/**
	Download files and/or folders.
	*/
	DOWNLOAD ( "Download", "Download one or more Google Drive files and/or folders" ),

	/**
	List drives including My Drive and shared drives.
	*/
	LIST_DRIVES ( "ListDrives", "List Google Drive drives" ),

	/**
	List Google Drive files and folders.
	*/
	LIST ( "List", "List Google Drive files and folders" );

	/**
	Upload objects (files and/or directories).
	*/
	//UPLOAD_OBJECTS ( "UploadObjects", "Upload one or more files and/or folders to Google Drive" );

	/**
	The name that is used for choices and other technical code (terse).
	*/
	private final String name;

	/**
	The description, useful for UI notes.
	*/
	private final String description;

	/**
	Construct an enumeration value.
	@param name name that should be displayed in choices, etc.
	@param descritpion command description.
	*/
	private GoogleDriveCommandType(String name, String description ) {
    	this.name = name;
    	this.description = description;
	}

	/**
	Get the list of command types, in appropriate order.
	@return the list of command types.
	*/
	public static List<GoogleDriveCommandType> getChoices() {
    	List<GoogleDriveCommandType> choices = new ArrayList<>();
    	choices.add ( GoogleDriveCommandType.DOWNLOAD );
    	choices.add ( GoogleDriveCommandType.LIST );
    	choices.add ( GoogleDriveCommandType.LIST_DRIVES );
    	return choices;
	}

	/**
	Get the list of command type as strings.
	@return the list of command types as strings.
	@param includeNote Currently not implemented.
	*/
	public static List<String> getChoicesAsStrings( boolean includeNote ) {
    	List<GoogleDriveCommandType> choices = getChoices();
    	List<String> stringChoices = new ArrayList<>();
    	for ( int i = 0; i < choices.size(); i++ ) {
        	GoogleDriveCommandType choice = choices.get(i);
        	String choiceString = "" + choice;
        	stringChoices.add ( choiceString );
    	}
    	return stringChoices;
	}

	/**
	Return the command name for the type.  This is the same as the value.
	@return the display name.
	*/
	@Override
	public String toString() {
    	return this.name;
	}

	/**
	Return the enumeration value given a string name (case-independent).
	@param name the name to match
	@return the enumeration value given a string name (case-independent), or null if not matched.
	*/
	public static GoogleDriveCommandType valueOfIgnoreCase ( String name ) {
	    if ( name == null ) {
        	return null;
    	}
    	GoogleDriveCommandType [] values = values();
    	for ( GoogleDriveCommandType t : values ) {
        	if ( name.equalsIgnoreCase(t.toString()) )  {
            	return t;
        	}
    	}
    	return null;
	}

}