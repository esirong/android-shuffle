/*
 * Copyright (C) 2008 The Android Open Source Project
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

package org.dodgybits.shuffle.android.widget;

import static org.dodgybits.shuffle.android.core.util.Constants.cIdType;
import static org.dodgybits.shuffle.android.core.util.Constants.cPackage;
import static org.dodgybits.shuffle.android.core.util.Constants.cStringType;

import java.util.HashMap;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.TaskQuery;
import org.dodgybits.shuffle.android.core.model.persistence.ContextPersister;
import org.dodgybits.shuffle.android.core.model.persistence.DefaultEntityCache;
import org.dodgybits.shuffle.android.core.model.persistence.EntityCache;
import org.dodgybits.shuffle.android.core.model.persistence.ProjectPersister;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.view.ContextIcon;
import org.dodgybits.shuffle.android.list.config.StandardTaskQueries;
import org.dodgybits.shuffle.android.persistence.provider.Shuffle;
import org.dodgybits.shuffle.android.preference.model.Preferences;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

/**
 * A widget provider.  We have a string that we pull from a preference in order to show
 * the configuration settings and the current time when the widget was updated.  We also
 * register a BroadcastReceiver for time-changed and timezone-changed broadcasts, and
 * update then too.
 */
public class WidgetProvider extends AppWidgetProvider {
    // log tag
    private static final String cTag = "WidgetProvider";

    private static final HashMap<String,Integer> sIdCache = new HashMap<String,Integer>();
    
    @Override
    public void onUpdate(android.content.Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(cTag, "onUpdate");
        final int N = appWidgetIds.length;
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            String prefKey = Preferences.getWidgetQueryKey(appWidgetId);
            String queryName = Preferences.getWidgetQuery(context, prefKey);
            updateAppWidget(context, appWidgetManager, appWidgetId, queryName);
        }
    }
    
    @Override
    public void onDeleted(android.content.Context context, int[] appWidgetIds) {
        Log.d(cTag, "onDeleted");
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        Editor editor = Preferences.getEditor(context);
        for (int i=0; i<N; i++) {
            String prefKey = Preferences.getWidgetQueryKey(appWidgetIds[i]);
            editor.remove(prefKey);
        }
        editor.commit();
    }

    @Override
    public void onEnabled(android.content.Context context) {
    }

    @Override
    public void onDisabled(android.content.Context context) {
    }

    static void updateAppWidget(android.content.Context androidContext, AppWidgetManager appWidgetManager,
            int appWidgetId, String queryName) {
        Log.d(cTag, "updateAppWidget appWidgetId=" + appWidgetId + " queryName=" + queryName);

        TaskPersister taskPersister = new TaskPersister(androidContext.getContentResolver());
        ProjectPersister projectPersister = new ProjectPersister(androidContext.getContentResolver());
        EntityCache<Project> projectCache = new DefaultEntityCache<Project>(projectPersister);
        ContextPersister contextPersister = new ContextPersister(androidContext.getContentResolver());
        EntityCache<Context> contextCache = new DefaultEntityCache<Context>(contextPersister);
        
        RemoteViews views = new RemoteViews(androidContext.getPackageName(), R.layout.widget);
        
        TaskQuery query = StandardTaskQueries.getQuery(queryName);
        if (query == null) return;
        
        int titleId = getIdentifier(androidContext, "title_" + queryName, cStringType);
        views.setTextViewText(R.id.title, androidContext.getString(titleId));
        
        Intent intent = new Intent(Intent.ACTION_INSERT, Shuffle.Tasks.CONTENT_URI);
        PendingIntent pendingIntent = PendingIntent.getActivity(androidContext, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.add_task, pendingIntent);
        
        Cursor taskCursor = androidContext.getContentResolver().query(
                Shuffle.Tasks.CONTENT_URI, 
                Shuffle.Tasks.cFullProjection, 
                query.getSelection(), 
                query.getSelectionArgs(), 
                query.getSortOrder());
        
        for (int taskCount = 1; taskCount <= 4; taskCount++) {
            Task task = null;
            Project project = null;
            Context context = null;
            if (taskCursor.moveToNext()) {
                task = taskPersister.read(taskCursor);
                project = projectCache.findById(task.getProjectId());
                context = contextCache.findById(task.getContextId());
            }
            
            int descriptionViewId = getIdIdentifier(androidContext, "description_" + taskCount);
            views.setTextViewText(descriptionViewId, task != null ? task.getDescription() : "");
            views.setInt(descriptionViewId, "setLines", project == null ? 2 : 1);
            
            int projectViewId = getIdIdentifier(androidContext, "project_" + taskCount);
            views.setViewVisibility(projectViewId, project == null ? View.GONE : View.VISIBLE);
            views.setTextViewText(projectViewId, project != null ? project.getName() : "");
            
            int contextIconId = getIdIdentifier(androidContext, "context_icon_" + taskCount);
            String iconName = context != null ? context.getIconName() : null;
            ContextIcon icon = ContextIcon.createIcon(iconName, androidContext.getResources());
            if (icon != ContextIcon.NONE) {
                views.setImageViewResource(contextIconId, icon.smallIconId);
                views.setViewVisibility(contextIconId, View.VISIBLE);
            } else {
                views.setViewVisibility(contextIconId, View.INVISIBLE);
            }
            
            if (task != null) {
                Uri.Builder builder = Shuffle.Tasks.CONTENT_URI.buildUpon();
                ContentUris.appendId(builder, task.getLocalId().getId());
                Uri taskUri = builder.build();
                intent = new Intent(Intent.ACTION_EDIT, taskUri);
                Log.d(cTag, "Adding pending event for viewing uri " + taskUri);
                pendingIntent = PendingIntent.getActivity(androidContext, 0, intent, 0);
                views.setOnClickPendingIntent(descriptionViewId, pendingIntent);
                views.setOnClickPendingIntent(projectViewId, pendingIntent);
                views.setOnClickPendingIntent(contextIconId, pendingIntent);
            }
        }
        taskCursor.close();
                
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    static int getIdIdentifier(android.content.Context context, String name) {
        Integer id = sIdCache.get(name);
        if (id == null) {
            id = getIdentifier(context, name, cIdType);
            sIdCache.put(name, id);
        }
        Log.d(cTag, "Got id " + id + " for resource " + name);
        return id;
    }
    
    static int getIdentifier(android.content.Context context, String name, String type) {
        int id = context.getResources().getIdentifier(
                    name, type, cPackage);
        Log.d(cTag, "Got id " + id + " for resource " + name);
        return id;
    }
    
}

