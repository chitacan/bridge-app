package com.chitacan.bridge;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StatusFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class StatusFragment extends Fragment {
    private TextView mAdbPort;
    private TextView mName;
    private TextView mHost;
    private TextView mPort;
    private TextView mClientId;
    private TextView mServerConneted;
    private TextView mServerEndPoint;
    private TextView mServerStatus;
    private TextView mDaemonConnected;
    private TextView mDaemonStatus;

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
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_status, container, false);
        mAdbPort         = (TextView) v.findViewById(R.id.status_adbport);
        mName            = (TextView) v.findViewById(R.id.status_name);
        mHost            = (TextView) v.findViewById(R.id.status_host);
        mPort            = (TextView) v.findViewById(R.id.status_port);
        mClientId        = (TextView) v.findViewById(R.id.status_clientid);
        mServerConneted  = (TextView) v.findViewById(R.id.status_server_connected);
        mServerEndPoint  = (TextView) v.findViewById(R.id.status_server_endpoint);
        mServerStatus    = (TextView) v.findViewById(R.id.status_server);
        mDaemonConnected = (TextView) v.findViewById(R.id.status_daemon_connected);
        mDaemonStatus    = (TextView) v.findViewById(R.id.status_daemon);
        return v;
    }

    private void update(Bundle bundle) {
        if (bundle == null)
            return;

        mAdbPort.setText(bundle.containsKey("adbport") ? String.valueOf(bundle.getInt("adbport")) : "");
        mName.setText(bundle.containsKey("name") ? bundle.getString("name") : "");
        mHost.setText(bundle.containsKey("host") ? bundle.getString("host") : "");
        mPort.setText(bundle.containsKey("port") ? String.valueOf(bundle.getInt("port")) : "");
        mClientId.setText(bundle.containsKey("clientId") ? bundle.getString("clientId") : "");
        mServerConneted.setText(bundle.containsKey("server_connected") ? String.valueOf(bundle.getBoolean("server_connected")) : "");
        mServerEndPoint.setText(bundle.containsKey("server_endpoint") ? bundle.getString("server_endpoint") : "");
        mServerStatus.setText(bundle.containsKey("server_status") ? bundle.getString("server_status") : "");
        mDaemonConnected.setText(bundle.containsKey("daemon_connected") ? String.valueOf(bundle.getBoolean("daemon_connected")) : "");
        mDaemonStatus.setText(bundle.containsKey("daemon_status") ? bundle.getString("daemon_status") : "");
    }
}
