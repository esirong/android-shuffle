/*
 * Copyright (C) 2009 Android Shuffle Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dodgybits.android.shuffle.activity;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.android.shuffle.model.Project;
import org.dodgybits.android.shuffle.model.State;
import org.dodgybits.android.shuffle.provider.Shuffle;
import org.dodgybits.android.shuffle.util.BindingUtils;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

public class ProjectEditorActivity extends AbstractEditorActivity<Project> {

    private static final String cTag = "ProjectEditorActivity";
   
    private EditText mNameWidget;
    private Spinner mDefaultContextSpinner;
    
    private String[] mContextNames;
    private int[] mContextIds;

    @Override
    protected void onCreate(Bundle icicle) {
        Log.d(cTag, "onCreate+");
        super.onCreate(icicle);
        
        // The text view for our project description, identified by its ID in the XML file.
        mNameWidget = (EditText) findViewById(R.id.name);
        mDefaultContextSpinner = (Spinner) findViewById(R.id.default_context);

        Cursor contactCursor = getContentResolver().query(Shuffle.Contexts.CONTENT_URI, new String[] {Shuffle.Contexts._ID, Shuffle.Contexts.NAME}, null, null, null);
        int size = contactCursor.getCount() + 1;
        mContextIds = new int[size];
        mContextIds[0] = 0;
        mContextNames = new String[size];
        mContextNames[0] = "None";
        for (int i = 1; i < size; i++) {
        	contactCursor.moveToNext();
        	mContextIds[i] = contactCursor.getInt(0);
        	mContextNames[i] = contactCursor.getString(1);
        }
        contactCursor.close();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mContextNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDefaultContextSpinner.setAdapter(adapter);

        // Get the project!
        mCursor = managedQuery(mUri, Shuffle.Projects.cFullProjection, null, null, null);
    }
    
    @Override
    protected void onResume() {
        Log.d(cTag, "onResume+");
        super.onResume();

        // If we didn't have any trouble retrieving the data, it is now
        // time to get at the stuff.
        if (mCursor != null) {
            // Make sure we are at the one and only row in the cursor.
            mCursor.moveToFirst();

            // Modify our overall title depending on the mode we are running in.
            if (mState == State.STATE_EDIT) {
                setTitle(R.string.title_edit_project);
            } else if (mState == State.STATE_INSERT) {
                setTitle(R.string.title_new_project);
            }

            // This is a little tricky: we may be resumed after previously being
            // paused/stopped.  We want to put the new text in the text view,
            // but leave the user where they were (retain the cursor position
            // etc).  This version of setText does that for us.
            Project project = BindingUtils.readProject(mCursor);
            mNameWidget.setTextKeepState(project.name);
            Integer defaultContextId = project.defaultContextId;
            if (defaultContextId == null) {
            	mDefaultContextSpinner.setSelection(0);
            } else {
            	for (int i = 1; i < mContextIds.length; i++) {
            		if (mContextIds[i] == defaultContextId) {
            			mDefaultContextSpinner.setSelection(i);
            			break;
            		}
            	}
            }

            // If we hadn't previously retrieved the original project, do so
            // now.  This allows the user to revert their changes.
            if (mOriginalItem == null) {
            	mOriginalItem = project;
            }
        } else {
            setTitle(getText(R.string.error_title));
            mNameWidget.setText(getText(R.string.error_message));
        }
        
        // select the description
        mNameWidget.selectAll();
    }

    @Override
    protected void onPause() {
        Log.d(cTag, "onPause+");
        super.onPause();

        // The user is going somewhere else, so make sure their current
        // changes are safely saved away in the provider.  We don't need
        // to do this if only viewing.
        if (mCursor != null) {
            String name = mNameWidget.getText().toString();

            // If this activity is finished, and there is no text, then we
            // do something a little special: simply delete the project entry.
            if (isFinishing() && mState == State.STATE_INSERT && TextUtils.isEmpty(name) ) {
                setResult(RESULT_CANCELED);
                deleteItem();
            } else {
            	if (TextUtils.isEmpty(name) && mOriginalItem != null) {
            		// we'll assume deleting the name was an accident
            		name = mOriginalItem.name;
            	}
            	Integer defaultContextId = null;
            	int selectedItemPosition = mDefaultContextSpinner.getSelectedItemPosition();
				if (selectedItemPosition > 0) {
            		defaultContextId = mContextIds[selectedItemPosition];
            	}
            	boolean archived = false;
            	Project project  = new Project(name, defaultContextId, archived);
                ContentValues values = new ContentValues();
            	writeItem(values, project);

                // Commit all of our changes to persistent storage. When the update completes
                // the content provider will notify the cursor of the change, which will
                // cause the UI to be updated.
                getContentResolver().update(mUri, values, null, null);    	
                //showSaveToast();
            }
        }
    }
       
    /**
     * Take care of deleting a project.  Simply deletes the entry.
     */
    @Override
    protected void deleteItem() {
    	super.deleteItem();
        mNameWidget.setText("");
    }
    
    /**
     * @return id of layout for this view
     */
    @Override
    protected int getContentViewResId() {
    	return R.layout.project_editor;
    }

    @Override
    protected Project restoreItem(Bundle icicle) {
    	return BindingUtils.restoreProject(icicle);
    }
    
    @Override
    protected void saveItem(Bundle outState, Project item) {
    	BindingUtils.saveProject(outState, item);
    }

    @Override
    protected void writeItem(ContentValues values, Project project) {
    	BindingUtils.writeProject(values, project);
    }
    
    @Override
    protected Intent getInsertIntent() {
    	return new Intent(Intent.ACTION_INSERT, Shuffle.Projects.CONTENT_URI);
    }
    
    @Override
    protected CharSequence getItemName() {
    	return getString(R.string.project_name);
    }

}
