package com.chitacan.bridge;

import android.app.Activity;
import android.os.Bundle;


public class ClientActivity extends Activity implements ClientFragment.OnFragmentInteractionListener {

    private static final String TAG = "ClientActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, ClientFragment.newInstance(getIntent().getExtras()))
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Util.denotify(this);
        Util.registerRejectReceiver(this, TAG);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Util.unregisterRejectReceiver(this, TAG);
    }

    @Override
    public void onFragmentInteraction(String id) {

    }
}
