package com.chitacan.bridge;


import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StatusFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class StatusFragment extends Fragment {

    private class StatusItem {
        String type;
        String title;
        String value;
        String key;
        boolean hasPref = false;

        public StatusItem(String type, String title) {
            this(type, title, null);
        }

        public StatusItem(String type, String title, String value) {
            this(type, title, value, null, false);
        }

        public StatusItem(String type, String title, String value, String key) {
            this(type, title, value, key, false);
        }

        public StatusItem(String type, String title, String value, String key, boolean hasPref) {
            this.type = type;
            this.title = title;
            this.value= value;
            this.key = key;
            this.hasPref = hasPref;
        }

        public boolean isHeader() {
            if (type == null) return false;
            if (type.equals("header")) return true;

            return false;
        }
    }

    private final ArrayList<StatusItem> mList = new ArrayList<StatusItem> ();

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment StatusFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static StatusFragment newInstance() {
        StatusFragment fragment = new StatusFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }
    public StatusFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }

        mList.add(new StatusItem("header", "Daemon"));
        mList.add(new StatusItem(null, "ADBD port", null, "adbport", true));
        mList.add(new StatusItem(null, "Status", null, "daemon_status"));
        mList.add(new StatusItem(null, "connected", null, "daemon_connected"));

        mList.add(new StatusItem("header", "Server"));
        mList.add(new StatusItem(null, "Name", null, "name"));
        mList.add(new StatusItem(null, "Endpoint", null, "server_endpoint"));
        mList.add(new StatusItem(null, "Status", null, "server_status"));
        mList.add(new StatusItem(null, "connected", null, "server_connected"));
        mList.add(new StatusItem(null, "client ID", null, "clientId"));
    }

    @Override
    public void onResume() {
        BusProvider.getInstance().register(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        BusProvider.getInstance().unregister(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_status, container, false);
        ListView lv = (ListView) v.findViewById(R.id.status);
        lv.setAdapter(new StatusListAdapter(getActivity()));
        return v;
    }

    private void update(Bundle bundle) {
        if (bundle == null)
            return;

        for (StatusItem item : mList) {
            if (item.key == null) continue;

            item.value = String.valueOf(bundle.get(item.key));
        }
    }

    @Subscribe
    public void bridgeEvent(BridgeEvent event) {
        switch(event.type) {
            case BridgeEvent.STATUS:
                update(event.bundle);
                break;
            case BridgeEvent.ERROR:
                // TODO: show error Dialog??
                Toast.makeText(getActivity(), "Bridge Error", Toast.LENGTH_LONG).show();
                BusProvider.getInstance().post(new BridgeEvent(BridgeEvent.REMOVE));
                break;
        }
    }

    private class StatusListAdapter extends BaseAdapter {

        private final LayoutInflater mInflater;

        public StatusListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public boolean isEnabled(int position) {
            return !mList.get(position).isHeader();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            StatusItem item = mList.get(position);
            boolean isHeader = item.isHeader();
            convertView = getConvertView(convertView, parent, isHeader);

            if (isHeader) {
                TextView title = (TextView) convertView.findViewById(R.id.lv_list_hdr);
                title.setText(item.title);
            } else {
                TextView title = (TextView) convertView.findViewById(R.id.lv_item_header);
                TextView desc  = (TextView) convertView.findViewById(R.id.lv_item_subtext);
                View pref = convertView.findViewById(R.id.button);
                View divider = convertView.findViewById(R.id.vertical_divider);

                title.setText(item.title);
                desc.setText(item.value);
                pref.setVisibility(item.hasPref ? View.VISIBLE : View.GONE);
                divider.setVisibility(item.hasPref ? View.VISIBLE : View.GONE);
            }

            return convertView;
        }

        private View getConvertView(View convertView, ViewGroup parent, boolean isHeader) {
            int layout = isHeader ? R.layout.lv_header : R.layout.lv_item;
            if (convertView == null)
                return mInflater.inflate(layout, parent, false);

            boolean inflateNew = isHeader
                    ? (convertView.getId() == R.id.item)
                    : (convertView.getId() == R.id.header);

            if (inflateNew)
                return mInflater.inflate(layout, parent, false);

            return convertView;
        }
    }
}
