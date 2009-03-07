package org.dodgybits.android.shuffle.activity;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.android.shuffle.activity.config.AbstractTaskListConfig;
import org.dodgybits.android.shuffle.activity.config.ListConfig;
import org.dodgybits.android.shuffle.model.Task;
import org.dodgybits.android.shuffle.provider.Shuffle;
import org.dodgybits.android.shuffle.util.MenuUtils;

import android.content.ContextWrapper;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.SimpleCursorAdapter;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class TabbedDueActionsActivity extends AbstractTaskListActivity {
	private static final String cTag = "TabbedDueActionsActivity";

	private TabHost mTabHost;
	private int mMode = Shuffle.Tasks.DAY_MODE;
	
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.tabbed_due_tasks);
        
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();
        mTabHost.addTab(createTabSpec(
        			R.string.day_button_title, 
        			String.valueOf(Shuffle.Tasks.DAY_MODE),
        			android.R.drawable.ic_menu_day));
        mTabHost.addTab(createTabSpec(
        			R.string.week_button_title, 
        			String.valueOf(Shuffle.Tasks.WEEK_MODE),
        			android.R.drawable.ic_menu_week));
        mTabHost.addTab(createTabSpec(
        			R.string.month_button_title, 
        			String.valueOf(Shuffle.Tasks.MONTH_MODE),
        			android.R.drawable.ic_menu_month));
        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {

			public void onTabChanged(String tabId) {
				Log.d(cTag, "Switched to tab: " + tabId);
				// Android issue 302: First tabId is null
				if (tabId == null) tabId = String.valueOf(Shuffle.Tasks.DAY_MODE);
				mMode = Integer.parseInt(tabId); 
				updateCursor();
			}
        	
        });
        
    }
    
	@Override
	protected ListConfig<Task> createListConfig()
	{
		return new AbstractTaskListConfig() {

			public Uri getListContentUri() {
				return Shuffle.Tasks.cDueTasksContentURI.buildUpon().appendPath(String.valueOf(mMode)).build();
			}

		    public int getCurrentViewMenuId() {
				return MenuUtils.CALENDAR_ID;
		    }
		    
		    public String createTitle(ContextWrapper context)
		    {
				return context.getString(R.string.title_calendar, getSelectedPeriod());
		    }
			
		};
	}
    	
    private TabSpec createTabSpec(int tabTitleRes, String tagId, int iconId) {
        TabSpec tabSpec = mTabHost.newTabSpec(tagId);
        tabSpec.setContent(R.id.task_list);
        String tabName = getString(tabTitleRes);
        tabSpec.setIndicator(tabName, this.getResources().getDrawable(iconId));
        return tabSpec;
    }
    
	private void updateCursor() {
    	Cursor cursor = createItemQuery();
    	SimpleCursorAdapter adapter = (SimpleCursorAdapter)getListAdapter();
    	adapter.changeCursor(cursor);
    	setTitle(getListConfig().createTitle(this));
	}    
	
	private String getSelectedPeriod() {
		String result = null;
		switch (mMode) {
		case Shuffle.Tasks.DAY_MODE:
			result = getString(R.string.day_button_title).toLowerCase();
			break;
		case Shuffle.Tasks.WEEK_MODE:
			result = getString(R.string.week_button_title).toLowerCase();
			break;
		case Shuffle.Tasks.MONTH_MODE:
			result = getString(R.string.month_button_title).toLowerCase();
			break;
		}
		return result;
	}
	

}
