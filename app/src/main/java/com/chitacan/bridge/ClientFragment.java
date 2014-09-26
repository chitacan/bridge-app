package com.chitacan.bridge;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
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
public class ClientFragment extends ListFragment implements Callback<List<API.Client>> {

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

        API.BridgeService service = restAdapter.create(API.BridgeService.class);
        service.listClients(this);
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
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mListener.onFragmentInteraction(DummyContent.ITEMS.get(position).id);
        }
    }

    @Override
    public void success(List<API.Client> clients, Response response) {
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

    private class ClientListAdapter extends ArrayAdapter<API.Client> {

        public ClientListAdapter(Context context, List<API.Client> clients) {
            super(context, 0, clients);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = LayoutInflater.from(getActivity()).inflate(android.R.layout.simple_list_item_2, null);

            TextView name  = (TextView) v.findViewById(android.R.id.text1);
            TextView value = (TextView) v.findViewById(android.R.id.text2);

            API.Client client = getItem(position);
            name .setText(client.name);
            value.setText(client.value);

            return v;
        }
    }
}
