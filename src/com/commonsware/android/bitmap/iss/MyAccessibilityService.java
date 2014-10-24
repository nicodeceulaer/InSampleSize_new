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
        Log.i(TAG, "onAccessiblitiyEvent " + event.toString() );
        // get the source node of the event
        AccessibilityNodeInfo nodeInfo = event.getSource();

        if( nodeInfo != null ) {
            HuntForImageViews( nodeInfo );
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
        while ( current != null ) {
            AccessibilityNodeInfo parent = current.getParent();
            if (parent == null) {
                return current; // top reached
            }

            // NOTE: Recycle the infos.
            AccessibilityNodeInfo oldCurrent = current;
            current = parent;
            oldCurrent.recycle();
        }
        return null; // empty tree
    }

    private void getNodesByClass( AccessibilityNodeInfo topNode, String className ) {
        getNodesByClass( topNode, className, "0");
    }

    private void getNodesByClass ( AccessibilityNodeInfo topNode, String className, String prefix ) {
        if( topNode == null ) { return; }

        for( int i = 0; i < topNode.getChildCount(); ++i ) {
            AccessibilityNodeInfo node = topNode.getChild(i);

            if( node != null) {
                String newPrefix = prefix + "." + i;
                if (node.getClassName().equals(className) )  {
                    if( node.isVisibleToUser() ) {
                        Log.i(TAG, "[" + newPrefix + "] " + node.getContentDescription() );
                    }
                }
                getNodesByClass( node, className, newPrefix );
            }
        }
    }

    private void HuntForImageViews( AccessibilityNodeInfo node ) {
        // first, we go up to a top node
        AccessibilityNodeInfo topNode = getTopNode( node );

        // then we go down again, looking for any ImageView that contains a correct string in accessibility
        getNodesByClass( topNode, "android.widget.ImageView" );
    }

    @Override
    public void onCreate() {
    }
}
