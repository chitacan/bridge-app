package com.chitacan.bridge;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AddFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AddFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class AddFragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private EditText mServerName = null;
    private EditText mServerHost = null;
    private EditText mServerPort = null;

    private InputMethodManager mImm;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AddFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AddFragment newInstance(String param1, String param2) {
        AddFragment fragment = new AddFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    public AddFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        mImm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);
        mServerName = (EditText) view.findViewById(R.id.edit_server_name);
        mServerHost = (EditText) view.findViewById(R.id.edit_server_host);
        mServerPort = (EditText) view.findViewById(R.id.edit_server_port);
        view.findViewById(R.id.btn_add).setOnClickListener(this);

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
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
    public void onResume() {
        super.onResume();
        mServerName.requestFocus();
        mImm.showSoftInput(mServerName, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem add = menu.findItem(R.id.action_add);
        MenuItem dis = menu.findItem(R.id.action_disconnect);

        if (add != null) add.setVisible(false);
        if (dis != null) dis.setVisible(false);

        ActionBar actionBar = getActivity().getActionBar();
        actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        actionBar.setTitle("Add Server");

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btn_add:
                if (addServer()) {
                    mImm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
                    getFragmentManager().popBackStack();
                }
                break;
        }
    }

    private boolean addServer() {
        String name = mServerName.getText().toString();
        String host = mServerHost.getText().toString();
        String port = mServerPort.getText().toString();

        if (name.isEmpty()) {
            Toast.makeText(getActivity(), "Name is required", Toast.LENGTH_LONG).show();
            mServerName.requestFocus();
            return false;
        }

        if (host.isEmpty()) {
            Toast.makeText(getActivity(), "Hostname is required", Toast.LENGTH_LONG).show();
            mServerHost.requestFocus();
            return false;
        }

        int p = port.length() == 0 ? 80 : Integer.parseInt(port);

        ContentValues values = new ContentValues();

        values.put(ServerProvider.SERVER_NAME, name);
        values.put(ServerProvider.SERVER_Host, host );
        values.put(ServerProvider.SERVER_PORT, p);

        Uri uri = getActivity().getContentResolver().insert(ServerProvider.CONTENT_URI, values);

        return true;
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
        public void onFragmentInteraction(Uri uri);
    }

}
