package com.commonsware.android.bitmap.iss;

import android.accessibilityservice.AccessibilityService;
import android.app.Service;
import android.content.Intent;
import android.graphics.Rect;
import android.os.IBinder;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class MyAccessibilityService extends AccessibilityService {
    private static final String TAG = "AccessiblityService";

    public MyAccessibilityService() {
    }

    @Override
    public void onServiceConnected() {
        Log.i(TAG, "onServiceConnected");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void onInterrupt() {
        /* do nothing */
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();
        String eventText = null;

        switch(eventType) {
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                eventText = "Clicked: ";
                break;
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                eventText = "Focused: ";
                break;
            case AccessibilityEvent.TYPE_ANNOUNCEMENT:
                eventText = "Announcement: ";
                break;
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                eventText = "View scrolled: ";
                break;
            case AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT:
                eventText = "Type text:";
                break;
            case AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED:
                eventText = "Type undefined:";
                break;
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:
                eventText = "Access.Focused:";
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                eventText = "Window content changed:";
                break;
            case AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION:
                eventText = "Context change type content description :";
                break;
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                eventText = "Type notification state changed :";
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                eventText = "Type window state changed :";
                break;
            default:
                eventText = "Event " + eventType + ": ";
        }

        Log.i(TAG, "onAccessiblitiyEvent " + event.toString() );

        // get the source node of the event
        AccessibilityNodeInfo nodeInfo = event.getSource();
        HuntForOemShelfThings( nodeInfo );

        // AccessibilityNodeInfo topNode = getListItemNodeInfo(nodeInfo);
        // HandleRecyclerViewNode( topNode, "1" );

        // Use the event and node information to determine
        // what action to take
        if( nodeInfo != null ) {
            Log.i(TAG, ">>   nodeInfo.className          :  " + nodeInfo.getClassName() );
            Log.i(TAG, ">>   nodeInfo.text               :  " + nodeInfo.getText() );
            Log.i(TAG, ">>   nodeInfo.contentDescription :  " + nodeInfo.getContentDescription() );

            if( nodeInfo.getClassName() == "android.support.v7.widget.RecyclerView" ) {
                Log.i(TAG, "  RecyclerView");
            }

            nodeInfo.recycle();
        }
    }

    private void HandleRecyclerViewNode( AccessibilityNodeInfo topNode, String prefix ) {
        if( topNode == null ) { return; }

        for( int i = 0; i < topNode.getChildCount(); ++i ) {
            AccessibilityNodeInfo node = topNode.getChild(i);

            if( node != null ) {
                String newPrefix = prefix + "." + i;
                Log.i(TAG, "   [" + newPrefix + "] " + node.toString() );
                HandleRecyclerViewNode(node, newPrefix);
            }
        }
    }

    private AccessibilityNodeInfo getListItemNodeInfo(AccessibilityNodeInfo source) {
        AccessibilityNodeInfo current = source;
        int level = 0;

        if( current == null ) { return null; }

        while (true) {
            String txt = current.toString();
            Log.i(TAG, "going up [" + level + "]-> " +  txt );

            AccessibilityNodeInfo parent = current.getParent();
            level++;
            if (parent == null) {
                return null;
            }

            if ("android.support.v7.widget.RecyclerView".equals(current.getClassName())) {
                return current;
            }

            // NOTE: Recycle the infos.
            AccessibilityNodeInfo oldCurrent = current;
            current = parent;
            oldCurrent.recycle();
        }
    }

    private AccessibilityNodeInfo getTopNode( AccessibilityNodeInfo current ) {
        int level = 0;

        // in case we start from null, avoid crashing in the loop
        if( current == null ) { return null; }

        while ( current != null ) {
            // Log.i(TAG, "going up [" + level + "] " +  current.toString() );

            AccessibilityNodeInfo parent = current.getParent();
            level++;

            if (parent == null) {
                // we reached the top, return last valid node
                return current;
            }

            // NOTE: Recycle the infos.
            AccessibilityNodeInfo oldCurrent = current;
            current = parent;
            oldCurrent.recycle();
        }
        return null;
    }

    private void getNodesByClass( AccessibilityNodeInfo topNode, String className ) {
        getNodesByClass( topNode, className, "0");
    }

    private void getNodesByClass ( AccessibilityNodeInfo topNode, String className, String prefix ) {
        if( topNode == null ) {
            return;
        }

        for( int i = 0; i < topNode.getChildCount(); ++i ) {
            AccessibilityNodeInfo node = topNode.getChild(i);

            if( node != null) {
                String newPrefix = prefix + "." + i;

                if (node.getClassName().equals(className) )  {
                    // Log.i(TAG, "   [" + newPrefix + "] " + node.toString() );
                    if( node.isVisibleToUser() ) {
                        Log.i(TAG, "[" + newPrefix + "] " + node.getContentDescription() );
                    } else {
                        Log.i(TAG, "== IN-visible ImageView found =====");
                    }
                }
                getNodesByClass( node, className, newPrefix );
            }
        }
    }

    private void HuntForOemShelfThings( AccessibilityNodeInfo node ) {
        // first, we go up to a top node
        AccessibilityNodeInfo topNode = getTopNode( node );

        // then we go down again, looking for any ImageView that contains a correct string in accessibility
        getNodesByClass( topNode, "android.widget.ImageView" );
    }

    @Override
    public void onCreate() {
    }
}
