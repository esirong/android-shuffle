package org.dodgybits.shuffle.android.synchronisation.tracks;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.Map;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.model.EntityBuilder;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.Task.Builder;
import org.dodgybits.shuffle.android.core.model.persistence.EntityPersister;
import org.dodgybits.shuffle.android.core.util.DateUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

/**
 * @author Morten Nielsen
 */
public final class TaskSynchronizer extends Synchronizer<Task> {
    private static final String cTag = "TaskSynchronizer";

    
    private final String mTracksUrl;

    public TaskSynchronizer( 
            EntityPersister<Task> persister,
            TracksSynchronizer tracksSynchronizer, 
            WebClient client, 
            Context context, 
            int basePercent,
            String tracksUrl) {
        super(persister, tracksSynchronizer, client, context, basePercent);

        this.mTracksUrl = tracksUrl;
    }

    @Override
    protected EntityBuilder<Task> createBuilder() {
        return Task.newBuilder();
    }

    @Override
    protected void verifyLocalEntities(Map<Id, Task> localEntities) {

        LinkedList<Id> tasksWithoutContext = new LinkedList<Id>();
        for(Task t : localEntities.values()) {
            if(!t.getContextId().isInitialised()) {
                tasksWithoutContext.add(t.getLocalId());
            }
        }
        if (tasksWithoutContext.size() > 0) {
            mTracksSynchronizer.postSyncMessage(R.string.cannotSyncTasksWithoutContext);
            for(Id id : tasksWithoutContext) {
                localEntities.remove(id);
            }
        }
    }

    @Override
    protected String readingRemoteText() {
        return mContext.getString(R.string.readingRemoteTasks);
    }

    @Override
    protected String processingText() {
        return mContext.getString(R.string.processingTasks);
    }

    @Override
    protected String readingLocalText() {
        return mContext.getString(R.string.readingLocalTasks);
    }

    @Override
    protected String stageFinishedText() {
        return mContext.getString(R.string.doneWithTasks);
    }


    protected Task createMergedLocalEntity(Task localTask, Task newTask) {
        Builder builder = Task.newBuilder();
        builder.mergeFrom(localTask);
        builder
            .setDescription(newTask.getDescription())
            .setDetails(newTask.getDetails())
            .setContextId(newTask.getContextId())
            .setProjectId(newTask.getProjectId())
            .setModifiedDate(newTask.getModifiedDate())
            .setStartDate(newTask.getStartDate())
            .setDueDate(newTask.getDueDate())
            .setAllDay(newTask.isAllDay())
            .setTracksId(newTask.getTracksId());
        return builder.build();
    }

    protected String createDocumentForEntity(Task task) {
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();

        try {
            serializer.setOutput(writer);
            //serializer.startDocument("UTF-8", true);


            serializer.startTag("", "todo");
            if (task.isComplete()) {
                String completedDateStr = DateUtils.formatIso8601Date(task.getModifiedDate());
                serializer.startTag("", "completed-at").attribute("", "type", "datetime").text(completedDateStr).endTag("", "completed-at");
            }
            
            Id contextId = findTracksIdByContextId(task.getContextId());
            if (contextId.isInitialised()) {
                serializer.startTag("", "context-id").attribute("", "type", "integer").text(contextId.toString()).endTag("", "context-id");
            }
            String createdDateStr = DateUtils.formatIso8601Date(task.getCreatedDate());
            serializer.startTag("", "created-at").attribute("", "type", "datetime").text(createdDateStr).endTag("", "created-at");
            serializer.startTag("", "description").text(task.getDescription()).endTag("", "description");
            if (task.getDueDate() != 0) {
                String dueDateStr = DateUtils.formatIso8601Date(task.getDueDate());
                serializer.startTag("", "due").attribute("", "type", "datetime").text(dueDateStr).endTag("", "due");
            }

            serializer.startTag("", "notes").text(task.getDetails() != null ? task.getDetails() : "").endTag("", "notes");

            Id projectId = findTracksIdByProjectId(task.getProjectId());
            if (projectId.isInitialised()) {
                serializer.startTag("", "project-id").attribute("", "type", "integer").text(projectId.toString()).endTag("", "project-id");
            }
            
            if (task.getStartDate() != 0L) {
                serializer.startTag("", "show-from").attribute("", "type", "datetime").text(DateUtils.formatIso8601Date(task.getStartDate())).endTag("", "show-from");
            }
            
            serializer.startTag("", "state").text(task.isComplete() ? "completed" : "active").endTag("", "state");
            
            String updatedDateStr = DateUtils.formatIso8601Date(task.getModifiedDate());
            serializer.startTag("", "updated-at").attribute("", "type", "datetime").text(updatedDateStr).endTag("", "updated-at");


            serializer.endTag("", "todo");
            // serializer.endDocument();
            serializer.flush();
        } catch (IOException ignored) {
            Log.d(cTag, "Failed to serialize task", ignored);
        }
        Log.d(cTag, writer.toString());

        return writer.toString();
    }

    protected Task parseSingleEntity(XmlPullParser parser) throws ParseException {
        Task task = null;
        final Builder builder = Task.newBuilder();
        builder.setTimezone("UTC");
        
        try {
            int eventType = parser.getEventType();
            
            while (eventType != XmlPullParser.END_DOCUMENT && task == null) {
                final String name = parser.getName();
                
                long startDate = 0L;
                long dueDate = 0L;

                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (name.equalsIgnoreCase("description")) {
                            builder.setDescription(parser.nextText());
                        } else if (name.equalsIgnoreCase("id")) {
                            Id tracksId = Id.create(Long.parseLong(parser.nextText()));
                            builder.setTracksId(tracksId);
                        } else if (name.equalsIgnoreCase("updated-at")) {
                            String dateStr = parser.nextText();
                            long modifiedDate = DateUtils.parseIso8601Date(dateStr);
                            builder.setModifiedDate(modifiedDate);
                        } else if (name.equalsIgnoreCase("context-id")) {
                            String tokenValue = parser.nextText();
                            if (!TextUtils.isEmpty(tokenValue)) {
                                Id tracksId = Id.create(Long.parseLong(tokenValue));
                                Id contextId = findContextIdByTracksId(tracksId);
                                builder.setContextId(contextId);
                            }
                        } else if (name.equalsIgnoreCase("project-id")) {
                            String tokenValue = parser.nextText();
                            if (!TextUtils.isEmpty(tokenValue)) {
                                Id tracksId = Id.create(Long.parseLong(tokenValue));
                                Id projectId = findProjectIdByTracksId(tracksId);
                                builder.setProjectId(projectId);
                            }
                        } else if (name.equalsIgnoreCase("notes")) {
                            builder.setDetails(parser.nextText());
                        } else if (name.equalsIgnoreCase("created-at")) {
                            String dateStr = parser.nextText();
                            if (!TextUtils.isEmpty(dateStr)) {
                                long created = DateUtils.parseIso8601Date(dateStr);
                                builder.setCreatedDate(created);
                            }
                        } else if (name.equalsIgnoreCase("due")) {
                            String dateStr = parser.nextText();
                            if (!TextUtils.isEmpty(dateStr)) {
                                dueDate = DateUtils.parseIso8601Date(dateStr);
                                builder.setDueDate(dueDate);
                            }
                        } else if (name.equalsIgnoreCase("show-from")) {
                            String dateStr = parser.nextText();
                            if (!TextUtils.isEmpty(dateStr)) {
                                startDate = DateUtils.parseIso8601Date(dateStr);
                                builder.setStartDate(startDate);
                            }
                        }
                        break;
                        
                    case XmlPullParser.END_TAG:
                        if (name.equalsIgnoreCase("todo")) {
                            boolean allDay = startDate > 0L || dueDate > 0L;
                            builder.setAllDay(allDay);
                            task = builder.build();
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (IOException e) {
            throw new ParseException("Unable to parse task:" + e.getMessage(), 0);
        } catch (XmlPullParserException e) {
            throw new ParseException("Unable to parse task:" + e.getMessage(), 0);
        }
        return task;
    }

    @Override
    protected String createEntityUrl(Task task) {
        return mTracksUrl + "/todos/" + task.getTracksId() + ".xml";
    }

    @Override
    protected String endIndexTag() {
        return "todos";
    }

    @Override
    protected String entityIndexUrl() {
        return mTracksUrl + "/todos.xml";
    }

}