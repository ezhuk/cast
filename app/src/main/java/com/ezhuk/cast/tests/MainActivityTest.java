// Copyright (c) 2014 Eugene Zhuk.
// Use of this source code is governed by the MIT license that can be found
// in the LICENSE file.

package com.ezhuk.cast.tests;

import android.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;

import com.ezhuk.cast.DevicesFragment;
import com.ezhuk.cast.MainActivity;
import com.ezhuk.cast.R;


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

    public void testMainActivityTestDrawer_defaultFragment() {
        Fragment fragment = mMainActivity.getFragmentManager()
                .findFragmentById(R.id.drawer_frame);
        assertEquals(DevicesFragment.class, fragment.getClass());
    }
}
