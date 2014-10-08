package com.chitacan.bridge;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.chitacan.bridge.dummy.DummyContent;
import com.squareup.otto.Subscribe;

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
        inflater.inflate(R.menu.client, menu);

        String prefix = getString(R.string.title_client_prefix);
        Spannable title = new SpannableString(prefix + mServerName);
        title.setSpan(
                new ForegroundColorSpan(getResources().getColor(R.color.title_prefixcolor)),
                0,
                prefix.length(),
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
        );

        ActionBar actionBar = getActivity().getActionBar();
        actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        actionBar.setTitle(title);
        actionBar.setDisplayHomeAsUpEnabled(true);
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
            case R.id.action_connect:
                getArguments().remove("clientId");
                BusProvider.getInstance().post(new BridgeEvent(BridgeEvent.CREATE, getArguments()));
                setListShown(false);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        int top  = (int) getResources().getDimension(R.dimen.lvPrimaryPaddingTop);
        int side = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
        getView().setPadding(side, top, side, 0);
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
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        RestKit.Client client = (RestKit.Client) l.getItemAtPosition(position);
        Bundle bundle = getArguments();
        bundle.putString("clientId", client.value);
        BusProvider.getInstance().post(new BridgeEvent(BridgeEvent.CREATE, bundle));

        setListShown(false);

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

        setEmptyText("No connected clients :(");
    }

    @Override
    public void failure(RetrofitError error) {
        if (error.isNetworkError())
            Toast.makeText(getActivity(), "Can't connect to server.", Toast.LENGTH_LONG).show();
        else {
            error.printStackTrace();
            Toast.makeText(getActivity(), "Rest API error", Toast.LENGTH_LONG).show();
        }
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
            if (convertView == null)
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.lv_client_item, parent, false);

            TextView  name  = (TextView)  convertView.findViewById(R.id.lv_item_name);
            TextView  value = (TextView)  convertView.findViewById(R.id.lv_item_id);
            ImageView icon  = (ImageView) convertView.findViewById(R.id.lv_item_icon);

            RestKit.Client client = getItem(position);

            name .setText(client.hostInfo.hostname);
            value.setText(client.value);
            icon .setImageResource(getIconId(client.hostInfo.type));

            return convertView;
        }

        private int getIconId(String type) {
            if (type.startsWith("Darwin"))
                return R.drawable.ic_fa_apple;

            if (type.startsWith("Windows"))
                return R.drawable.ic_fa_windows;

            return R.drawable.ic_fa_linux;
        }
    }

    @Subscribe
    public void bridgeEvent(BridgeEvent event) {
        switch(event.type) {
            case BridgeEvent.CREATED:
                NavUtils.navigateUpFromSameTask(getActivity());
                break;
            case BridgeEvent.ERROR:
                // TODO: show error Dialog??
                setListShown(true);
                Toast.makeText(getActivity(), "Bridge Error", Toast.LENGTH_LONG).show();
                BusProvider.getInstance().post(new BridgeEvent(BridgeEvent.REMOVE));
                break;
        }
    }
}
