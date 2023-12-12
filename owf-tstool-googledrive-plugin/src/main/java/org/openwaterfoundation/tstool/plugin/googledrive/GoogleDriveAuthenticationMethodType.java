// GoogleDriveAuthenticationMethodType - Google Drive authentication method enumeration

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

import java.util.ArrayList;
import java.util.List;

/**
Google Drive commands, as enumeration to simplify code.
*/
public enum GoogleDriveAuthenticationMethodType {
	/**
	OAuth.
	*/
	OAUTH ( "OAuth", "OAuth 2" ),

	/**
	Service account key.
	*/
	SERVICE_ACCOUNT_KEY ( "ServiceAccountKey", "Service account key" );

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
	private GoogleDriveAuthenticationMethodType(String name, String description ) {
    	this.name = name;
    	this.description = description;
	}

	/**
	Get the list of command types, in appropriate order.
	@return the list of command types.
	*/
	public static List<GoogleDriveAuthenticationMethodType> getChoices() {
    	List<GoogleDriveAuthenticationMethodType> choices = new ArrayList<>();
    	choices.add ( GoogleDriveAuthenticationMethodType.OAUTH );
    	choices.add ( GoogleDriveAuthenticationMethodType.SERVICE_ACCOUNT_KEY );
    	return choices;
	}

	/**
	Get the list of command type as strings.
	@return the list of command types as strings.
	@param includeNote Currently not implemented.
	*/
	public static List<String> getChoicesAsStrings( boolean includeNote ) {
    	List<GoogleDriveAuthenticationMethodType> choices = getChoices();
    	List<String> stringChoices = new ArrayList<>();
    	for ( int i = 0; i < choices.size(); i++ ) {
        	GoogleDriveAuthenticationMethodType choice = choices.get(i);
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
	public static GoogleDriveAuthenticationMethodType valueOfIgnoreCase ( String name ) {
	    if ( name == null ) {
        	return null;
    	}
    	GoogleDriveAuthenticationMethodType [] values = values();
    	for ( GoogleDriveAuthenticationMethodType t : values ) {
        	if ( name.equalsIgnoreCase(t.toString()) )  {
            	return t;
        	}
    	}
    	return null;
	}

}