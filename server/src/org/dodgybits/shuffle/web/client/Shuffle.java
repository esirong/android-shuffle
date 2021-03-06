package org.dodgybits.shuffle.web.client;

import gwtupload.client.IUploader;
import gwtupload.client.SingleUploader;

import org.dodgybits.shuffle.web.client.gin.ShuffleGinjector;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootLayoutPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Shuffle implements EntryPoint, IUploader.OnFinishUploaderHandler {

    interface GlobalResources extends ClientBundle {
        @NotStrict
        @Source("global.css")
        CssResource css();
    }

    /**
     * This method constructs the application user interface by instantiating
     * controls and hooking up event handler.
     */
    public void onModuleLoad() {
        // Inject global styles.
        GWT.<GlobalResources> create(GlobalResources.class).css()
                .ensureInjected();

        ShuffleGinjector ginjector = GWT.create(ShuffleGinjector.class);
        Main main = ginjector.getMain();

        // Get rid of scrollbars, and clear out the window's built-in margin,
        // because we want to take advantage of the entire client area.
        Window.enableScrolling(false);
        Window.setMargin("0px");

        RootLayoutPanel.get().add(main);
        
//        addUploader();
    }

    private void addUploader() {
        // Create a new uploader panel and attach it to the document
        SingleUploader defaultUploader = new SingleUploader();
        defaultUploader.setServletPath("/shuffle/restore");
        RootLayoutPanel.get().add(defaultUploader);

        // Add a finish handler which will load the image once the upload
        // finishes
        defaultUploader.addOnFinishUploadHandler(this);
    }

    public void onFinish(IUploader uploader) {
        // DO SOMETHING on done...

    }

    //  
    // /**
    // * The message displayed to the user when the server cannot be reached or
    // * returns an error.
    // */
    // private static final String SERVER_ERROR = "An error occurred while "
    // + "attempting to contact the server. Please check your network "
    // + "connection and try again.";
    //
    // /**
    // * Create a remote service proxy to talk to the server-side Greeting
    // service.
    // */
    // private final GreetingServiceAsync greetingService = GWT
    // .create(GreetingService.class);
    //
    // /**
    // * This is the entry point method.
    // */
    // public void onModuleLoad() {
    // final Button sendButton = new Button("Send");
    // final TextBox nameField = new TextBox();
    // nameField.setText("GWT User");
    //
    // // We can add style names to widgets
    // sendButton.addStyleName("sendButton");
    //
    // // Add the nameField and sendButton to the RootPanel
    // // Use RootPanel.get() to get the entire body element
    // RootPanel.get("nameFieldContainer").add(nameField);
    // RootPanel.get("sendButtonContainer").add(sendButton);
    //
    // // Focus the cursor on the name field when the app loads
    // nameField.setFocus(true);
    // nameField.selectAll();
    //
    // // Create the popup dialog box
    // final DialogBox dialogBox = new DialogBox();
    // dialogBox.setText("Remote Procedure Call");
    // dialogBox.setAnimationEnabled(true);
    // final Button closeButton = new Button("Close");
    // // We can set the id of a widget by accessing its Element
    // closeButton.getElement().setId("closeButton");
    // final Label textToServerLabel = new Label();
    // final HTML serverResponseLabel = new HTML();
    // VerticalPanel dialogVPanel = new VerticalPanel();
    // dialogVPanel.addStyleName("dialogVPanel");
    // dialogVPanel.add(new HTML("<b>Sending name to the server:</b>"));
    // dialogVPanel.add(textToServerLabel);
    // dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
    // dialogVPanel.add(serverResponseLabel);
    // dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
    // dialogVPanel.add(closeButton);
    // dialogBox.setWidget(dialogVPanel);
    //
    // // Add a handler to close the DialogBox
    // closeButton.addClickHandler(new ClickHandler() {
    // public void onClick(ClickEvent event) {
    // dialogBox.hide();
    // sendButton.setEnabled(true);
    // sendButton.setFocus(true);
    // }
    // });
    //
    // // Create a handler for the sendButton and nameField
    // class MyHandler implements ClickHandler, KeyUpHandler {
    // /**
    // * Fired when the user clicks on the sendButton.
    // */
    // public void onClick(ClickEvent event) {
    // sendNameToServer();
    // }
    //
    // /**
    // * Fired when the user types in the nameField.
    // */
    // public void onKeyUp(KeyUpEvent event) {
    // if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
    // sendNameToServer();
    // }
    // }
    //
    // /**
    // * Send the name from the nameField to the server and wait for a response.
    // */
    // private void sendNameToServer() {
    // sendButton.setEnabled(false);
    // String textToServer = nameField.getText();
    // textToServerLabel.setText(textToServer);
    // serverResponseLabel.setText("");
    // greetingService.greetServer(textToServer, new AsyncCallback<String>() {
    // public void onFailure(Throwable caught) {
    // // Show the RPC error message to the user
    // dialogBox.setText("Remote Procedure Call - Failure");
    // serverResponseLabel.addStyleName("serverResponseLabelError");
    // serverResponseLabel.setHTML(SERVER_ERROR);
    // dialogBox.center();
    // closeButton.setFocus(true);
    // }
    //
    // public void onSuccess(String result) {
    // dialogBox.setText("Remote Procedure Call");
    // serverResponseLabel.removeStyleName("serverResponseLabelError");
    // serverResponseLabel.setHTML(result);
    // dialogBox.center();
    // closeButton.setFocus(true);
    // }
    // });
    // }
    // }
    //
    // // Add a handler to send the name to the server
    // MyHandler handler = new MyHandler();
    // sendButton.addClickHandler(handler);
    // nameField.addKeyUpHandler(handler);
    // }
}
