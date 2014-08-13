// Copyright (c) 2014 Eugene Zhuk.
// Use of this source code is governed by the MIT license that can be found
// in the LICENSE file.

package com.ezhuk.cast;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class DevicesFragment extends Fragment {
    private ArrayAdapter<String> mDeviceListAdapter;

    public DevicesFragment() {
        // empty
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_devices,
                container, false);

        ListView deviceList = (ListView) rootView.findViewById(R.id.devices_list);
        MainActivity activity = (MainActivity) getActivity();
        mDeviceListAdapter = new ArrayAdapter<String>(activity, R.layout.device_item,
                activity.mDeviceNames);
        deviceList.setAdapter(mDeviceListAdapter);
        deviceList.setEmptyView(rootView.findViewById(R.id.empty_view));

        return rootView;
    }

    public void updateDevicesList() {
        mDeviceListAdapter.notifyDataSetChanged();
    }
}
