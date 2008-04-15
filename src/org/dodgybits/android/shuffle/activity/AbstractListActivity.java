package org.dodgybits.android.shuffle.activity;

import org.dodgybits.android.shuffle.util.MenuUtils;

import android.app.Activity;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

public abstract class AbstractListActivity<T> extends ListActivity  {

	protected final int NEW_ITEM = 1;
	// after a new item is added, select it
	private Long mItemIdToSelect = null;
	
	@SuppressWarnings("unused")
	private static final String cTag = "AbstractListActivity";
    
    /**
     * Cursor to select for list of items
     */
    protected Cursor mCursor;

    /** Called when the activity is first created. */
    @SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(getContentViewResId());
        setDefaultKeyMode(SHORTCUT_DEFAULT_KEYS);
        // If no data was given in the intent (because we were started
        // as a MAIN activity), then use our default content provider.
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(getContentUri());
        }
        mCursor = createItemQuery();
        setListAdapter(createListAdapter(mCursor));
        
        animateList();
    }
    
	@Override
	protected void onResume() {
		super.onResume();
		if (mItemIdToSelect != null) {
			Log.d(cTag, "New item id = " + mItemIdToSelect);
			// see if list contains the new item
			int count = getItemCount();
			CursorAdapter adapter = (CursorAdapter) getListAdapter();
			for (int i = 0; i < count; i++)  {
				long currentItemId = adapter.getItemId(i);
				Log.d(cTag, "Current id=" + currentItemId + " pos=" + i);
				if (currentItemId == mItemIdToSelect) {
					Log.d(cTag, "Found new item - selecting");
					setSelection(i);
					break;
				}
			}
			mItemIdToSelect = null;
		}
	}
    
    
    protected void animateList() {
        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(50);
        set.addAnimation(animation);

        animation = new TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, -1.0f,Animation.RELATIVE_TO_SELF, 0.0f
        );
        animation.setDuration(100);
        set.addAnimation(animation);

        LayoutAnimationController controller =
                new LayoutAnimationController(set, 0.5f);
        ListView listView = getListView();        
        listView.setLayoutAnimation(controller);
    }
    
    /**
     * @return id of layout for this view
     */
    abstract int getContentViewResId();

    /**
     * Content type of list items.
     */
    abstract Uri getContentUri();

    /**
     * @return a cursor selecting the items to display in the list.
     */
    abstract Cursor createItemQuery();

    abstract ListAdapter createListAdapter(Cursor cursor);

    /**
     * Generate a model object for the item at the current cursor position.
     */
    abstract T readItem(Cursor cursor);

    abstract int getCurrentViewMenuId();

	abstract String getItemName();

	
    /**
     * Permanently delete the selected item.
     */
    protected void deleteItem() {
        mCursor.moveTo(getSelectedItemPosition());
        mCursor.deleteRow();
    }
    
    /**
     * @return Number of items in the list.
     */
    protected final int getItemCount() {
    	return mCursor.count();
    }
    
    /**
     * The intent to insert a new item in this list.
     * Default to an insert action on the list type which
     * is all you need most of the time.
     */
    protected Intent getInsertIntent() {
    	return new Intent(Intent.INSERT_ACTION, getContentUri());
    }
    
    private final void insertItem() {
        // Launch activity to insert a new item
        startSubActivity(getInsertIntent(), NEW_ITEM);
    }
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode,
			String data, Bundle extras) {
    	Log.d(cTag, "Got resultCode " + resultCode + " with data " + data);
    	switch (requestCode) {
	    	case NEW_ITEM:
	        	if (resultCode == Activity.RESULT_OK) {
	    			if (data != null) {
	    				Uri uri = Uri.parse(data);
	    				mItemIdToSelect = ContentUris.parseId(uri);
	    				// need to do the actual checking and selecting
	    				// in onResume, otherwise getItemId(i) is always 0
	    				// and setSelection(i) does nothing
	    			}
	    		}
	    		break;
			default:
				Log.e(cTag, "Unknown requestCode: " + requestCode);
    	}
	}
    

    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
    	switch (event.getKeyCode()) {
    	case KeyEvent.KEYCODE_N:
    		// go to previous view
    		int prevView = getCurrentViewMenuId() - 1;
    		if (prevView < MenuUtils.INBOX_ID) {
    			prevView = MenuUtils.CONTEXT_ID;
    		}
    		MenuUtils.checkCommonItemsSelected(prevView, this, getCurrentViewMenuId());
    		return true;
		case KeyEvent.KEYCODE_M:
			// go to previous view
			int nextView = getCurrentViewMenuId() + 1;
			if (nextView > MenuUtils.CONTEXT_ID) {
				nextView = MenuUtils.INBOX_ID;
			}
			MenuUtils.checkCommonItemsSelected(nextView, this, getCurrentViewMenuId());
			return true;
    	}
		return super.onKeyDown(keyCode, event);
	}

    protected abstract boolean isTaskList();
    
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuUtils.addInsertMenuItems(menu, getItemName(), isTaskList(), this);
        MenuUtils.addAlternativeMenuItems(menu, getContentUri(), this);
        MenuUtils.addViewMenuItems(menu, getCurrentViewMenuId());
        MenuUtils.addPrefsHelpMenuItems(menu);
        
        return true;
    }
	
	abstract boolean supportsViewAction();

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final boolean haveItems = getItemCount() > 0;

        // If there are any selected items we need to generate the actions that
        // can be performed on the current selection.  This will be a combination
        // of our own specific actions along with any extensions that can be
        // found.
        if (haveItems && getSelectedItemPosition() > -1) {
            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(getContentUri(), getSelectedItemId());
            MenuUtils.addSelectedAlternativeMenuItems(menu, uri, this, supportsViewAction());
            // ... and ends with the delete command.
            MenuUtils.addDeleteMenuItem(menu);
        } else {
            menu.removeGroup(Menu.SELECTED_ALTERNATIVE);
        }

        // Make sure the delete action is disabled if there are no items.
        menu.setItemShown(MenuUtils.DELETE_ID, haveItems);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(Menu.Item item) {
        switch (item.getId()) {
	        case MenuUtils.DELETE_ID:
	            deleteItem();
	            return true;
	        case MenuUtils.INSERT_ID:
	            insertItem();
	            return true;
        }
        if (MenuUtils.checkCommonItemsSelected(item, this, getCurrentViewMenuId())) {
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri url = ContentUris.withAppendedId(getContentUri(), id);
        
        String action = getIntent().getAction();
        if (Intent.PICK_ACTION.equals(action)
                || Intent.GET_CONTENT_ACTION.equals(action)) {
            // The caller is waiting for us to return a task selected by
            // the user.  They have clicked on one, so return it now.
            setResult(RESULT_OK, url.toString());
        } else {
            // Launch activity to view/edit the currently selected item
            startActivity(getClickIntent(url));
        }
    }

    /**
     * Return the intent generated when a list item is clicked.
     * 
     * @param url type of data selected
     */ 
    protected Intent getClickIntent(Uri uri) {
    	return new Intent(Intent.EDIT_ACTION, uri);
    }
    
}
