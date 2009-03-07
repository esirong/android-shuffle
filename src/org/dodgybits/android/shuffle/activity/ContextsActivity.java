package org.dodgybits.android.shuffle.activity;

import org.dodgybits.android.shuffle.activity.config.ContextListConfig;
import org.dodgybits.android.shuffle.activity.config.ListConfig;
import org.dodgybits.android.shuffle.model.Context;
import org.dodgybits.android.shuffle.provider.Shuffle;
import org.dodgybits.android.shuffle.util.BindingUtils;
import org.dodgybits.android.shuffle.view.ContextView;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

/**
 * Display list of contexts with task children.
 */
public class ContextsActivity extends AbstractDrilldownListActivity<Context> {

	private static final String cTag = "ContextsActivity";

	@Override
	protected void onResume() {
		super.onResume();
		
		Cursor cursor = getContentResolver().query(getDrilldownListConfig().getListContentUri(), 
				Shuffle.Contexts.cFullTaskProjection, null, null, null);
		mTaskCountArray = BindingUtils.readCountArray(cursor);
		cursor.close();
	}
	
	@Override
	protected ListConfig<Context> createListConfig()
	{
		return new ContextListConfig();
	}
	
	@Override
	protected void deleteChildren(int groupId) {
		getContentResolver().delete(getDrilldownListConfig().getChildContentUri(), 
				Shuffle.Tasks.CONTEXT_ID + " = ?", new String[] {String.valueOf(groupId)});
	}

	@Override
	protected Cursor createItemQuery() {
		Log.d(cTag, "Creating a cursor over all contexts");
		return managedQuery(getIntent().getData(), Shuffle.Contexts.cFullProjection,
				null, null, 
				Shuffle.Contexts.NAME + " ASC");
	}
	
	@Override
	protected ListAdapter createListAdapter(Cursor cursor) {
		ListAdapter adapter =
				new SimpleCursorAdapter(this,
						android.R.layout.simple_list_item_1, cursor,
						new String[] { Shuffle.Contexts.NAME },
						new int[] { android.R.id.text1 }) {
			
			public View getView(int position, View convertView, ViewGroup parent) {
				Cursor cursor = (Cursor)getItem(position);
				Context context = BindingUtils.readContext(cursor);
				ContextView contextView;
				if (convertView instanceof ContextView) {
					contextView = (ContextView) convertView;
				} else {
					contextView = new ContextView(parent.getContext());
				}
				contextView.setTaskCountArray(mTaskCountArray);
				contextView.updateView(context);
				return contextView;
			}

		};
		return adapter;
	}

	@Override
	protected int getSelectedItemChildCount() {
		final long groupId = getSelectedItemId();
		return mTaskCountArray.get((int)groupId);
	}
	
    /**
     * Return the intent generated when a list item is clicked.
     * 
     * @param url type of data selected
     */ 
	@Override
    protected Intent getClickIntent(Uri uri) {
    	// if a context is clicked on, show tasks for that context.
    	Intent intent = new Intent(this, ContextTasksActivity.class);
    	intent.setData(uri);
    	return intent;
    }
	


}