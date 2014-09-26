package com.chitacan.bridge;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.chitacan.bridge.dummy.DummyContent;

import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * A fragment representing a list of Items.
 * <p />
 * <p />
 */
public class ClientFragment extends ListFragment implements Callback<List<RestKit.Client>> {

    private String mServerName;
    private String mServerHost;
    private int mServerPort;
    private ClientListAdapter mAdapter;

    private OnFragmentInteractionListener mListener;

    // TODO: Rename and change types of parameters
    public static ClientFragment newInstance(Bundle bundle) {
        ClientFragment fragment = new ClientFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ClientFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mServerName = getArguments().getString("name");
            mServerHost = getArguments().getString("host");
            mServerPort = getArguments().getInt("port");
        }

        setHasOptionsMenu(true);

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(Util.createUrl(mServerHost, mServerPort, null))
                .build();

        RestKit.BridgeAPI api = restAdapter.create(RestKit.BridgeAPI.class);
        api.listClients(this);

        bindService();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        ActionBar actionbar = getActivity().getActionBar();
        actionbar.setTitle(mServerName);
        actionbar.setDisplayHomeAsUpEnabled(true);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(getActivity());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (mIsBound && mService != null) {
            RestKit.Client client = (RestKit.Client) l.getItemAtPosition(position);

            Bundle bundle = getArguments();
            bundle.putString("clientId", client.value);

            try {
                Message msg = Message.obtain(null, BridgeService.MSG_CREATE_BRIDGE);
                msg.setData(bundle);
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mListener.onFragmentInteraction(DummyContent.ITEMS.get(position).id);
        }
    }

    @Override
    public void success(List<RestKit.Client> clients, Response response) {
        mAdapter = new ClientListAdapter(getActivity(), clients);
        setListAdapter(mAdapter);
    }

    @Override
    public void failure(RetrofitError error) {
        error.printStackTrace();
        Toast.makeText(getActivity(), "Rest API error", Toast.LENGTH_LONG).show();
        getListView().postDelayed(new Runnable() {
            @Override
            public void run() {
                NavUtils.navigateUpFromSameTask(getActivity());
            }
        }, 1500);
    }

    /**
    * This interface must be implemented by activities that contain this
    * fragment to allow an interaction in this fragment to be communicated
    * to the activity and potentially other fragments contained in that
    * activity.
    * <p>
    * See the Android Training lesson <a href=
    * "http://developer.android.com/training/basics/fragments/communicating.html"
    * >Communicating with Other Fragments</a> for more information.
    */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(String id);
    }

    private class ClientListAdapter extends ArrayAdapter<RestKit.Client> {

        public ClientListAdapter(Context context, List<RestKit.Client> clients) {
            super(context, 0, clients);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = LayoutInflater.from(getActivity()).inflate(android.R.layout.simple_list_item_2, null);

            TextView name  = (TextView) v.findViewById(android.R.id.text1);
            TextView value = (TextView) v.findViewById(android.R.id.text2);

            RestKit.Client client = getItem(position);
            name .setText(client.name);
            value.setText(client.value);

            return v;
        }
    }

    private boolean mIsBound = false;

    private void bindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        Intent intent = new Intent(getActivity(), BridgeService.class);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    private void unbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, BridgeService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            // Detach our existing connection.
            getActivity().unbindService(mConnection);
            mIsBound = false;
        }
    }


    class Incominghandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BridgeService.MSG_STATUS_BRIDGE:
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    private Messenger mService;
    private final Messenger mMessenger = new Messenger(new Incominghandler());

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = new Messenger(service);

            try {
                Message msg = Message.obtain(null, BridgeService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

                msg = Message.obtain(null, BridgeService.MSG_STATUS_BRIDGE);
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };
}
