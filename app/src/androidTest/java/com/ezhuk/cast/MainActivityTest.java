// Copyright (c) 2014 Eugene Zhuk.
// Use of this source code is governed by the MIT license that can be found
// in the LICENSE file.

package com.ezhuk.cast;

import android.support.v7.app.ActionBar;
import android.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListView;
import android.widget.TextView;


public class MainActivityTest
        extends ActivityInstrumentationTestCase2<MainActivity> {
    private MainActivity mMainActivity;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mMainActivity = getActivity();
    }

    public void testPreconditions() {
        assertNotNull("mMainActivity is Null", mMainActivity);
    }

    public void testMainActivityTest_ActionBar() {
        final ActionBar actionBar = mMainActivity.getSupportActionBar();
        assertNotNull("actionBar is Null", actionBar);

        final int displayOptions = actionBar.getDisplayOptions();
        assertTrue(0 != (displayOptions & ActionBar.DISPLAY_HOME_AS_UP));
        assertTrue(0 != (displayOptions & ActionBar.DISPLAY_SHOW_HOME));
    }

    public void testMainActivityTest_DrawerLayout() {
        final DrawerLayout drawerLayout = (DrawerLayout) mMainActivity
                .findViewById(R.id.drawer_layout);
        assertNotNull("drawerLayout is Null", drawerLayout);
    }

    public void testMainActivityTest_DrawerList() {
        final String[] drawerArray = mMainActivity.getResources()
                .getStringArray(R.array.drawer_array);
        assertNotNull("drawerArray is Null", drawerArray);

        final ListView drawerList = (ListView) mMainActivity
                .findViewById(R.id.drawer_list);
        assertNotNull("drawerList is Null", drawerList);

        assertEquals(drawerArray.length, drawerList.getChildCount());
        for (int i = 0; drawerArray.length > i; ++i) {
            assertEquals(drawerArray[i], ((TextView) drawerList.getChildAt(i))
                    .getText().toString());
        }
    }

    public void testMainActivityTest_DrawerFragment() {
        final Fragment fragment = mMainActivity.getFragmentManager()
                .findFragmentById(R.id.drawer_frame);
        assertEquals(DevicesFragment.class, fragment.getClass());
    }
}
