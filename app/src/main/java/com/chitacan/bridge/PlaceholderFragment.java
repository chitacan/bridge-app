package com.chitacan.bridge;

/**
 * Created by chitacan on 2014. 8. 20..
 */

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment implements View.OnClickListener{

    private Extender mExtender   = null;
    private TextView mStatus     = null;

    private EditText mLocalPortNumber  = null;
    private EditText mRemotePortNumber = null;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            String status = (String) msg.obj;
            mStatus.setText(status);
        }
    };
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static PlaceholderFragment newInstance(int sectionNumber) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public PlaceholderFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        rootView.findViewById(R.id.btn_start).setOnClickListener(this);
        rootView.findViewById(R.id.btn_stop) .setOnClickListener(this);
        mStatus = (TextView) rootView.findViewById(R.id.section_status);
        mRemotePortNumber = (EditText) rootView.findViewById(R.id.edit_remote_port);
        mLocalPortNumber  = (EditText) rootView.findViewById(R.id.edit_local_port);
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start:
                startExtender();
                break;
            case R.id.btn_stop:
                stopExtender();
                break;
        }
    }

    private void stopExtender() {
        if (mExtender != null)
            mExtender.interrupt();
        mExtender = null;
    }

    private void startExtender() {
        String remotePort = mRemotePortNumber.getText().toString();
        String localPort  = mLocalPortNumber .getText().toString();

        if (remotePort.length() == 0 || localPort.length() == 0) {
            Toast.makeText(getActivity(), "no remote port number", Toast.LENGTH_LONG).show();
            return;
        }
        if (mExtender == null || !mExtender.isAlive()) {
            mExtender = new Extender(
                    "127.0.0.1",    Integer.parseInt(localPort),
                    "redribbon.io", Integer.parseInt(remotePort)
            );
            mExtender.setHandler(mHandler);
            mExtender.start();
        }
    }
}
