// Copyright (c) 2014 Eugene Zhuk.
// Use of this source code is governed by the MIT license that can be found
// in the LICENSE file.

package com.ezhuk.cast.tests;

import android.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListView;
import android.widget.TextView;

import com.ezhuk.cast.DevicesFragment;
import com.ezhuk.cast.MainActivity;
import com.ezhuk.cast.R;

import java.lang.reflect.Array;


public class MainActivityTest
        extends ActivityInstrumentationTestCase2<MainActivity> {
    private MainActivity mMainActivity;
    private String[] mDrawerArray;
    private ListView mDrawerList;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mMainActivity = getActivity();
        mDrawerArray = mMainActivity.getResources().getStringArray(R.array.drawer_array);
        mDrawerList = (ListView) mMainActivity.findViewById(R.id.drawer_list);
    }

    public void testPreconditions() {
        assertNotNull("mMainActivity is Null", mMainActivity);
        assertNotNull("mDrawerArray is Null", mDrawerArray);
        assertNotNull("mDrawerList is Null", mDrawerList);
    }

    public void testMainActivityTestDrawer_defaultFragment() {
        Fragment fragment = mMainActivity.getFragmentManager()
                .findFragmentById(R.id.drawer_frame);
        assertEquals(DevicesFragment.class, fragment.getClass());
    }

    public void testMainActivityTestDrawer_defaultList() {
        assertEquals(mDrawerArray.length, mDrawerList.getChildCount());
        for (int i = 0; i < mDrawerArray.length; ++i) {
            assertEquals(mDrawerArray[i], ((TextView) mDrawerList
                    .getChildAt(i)).getText().toString());
        }
    }
}
