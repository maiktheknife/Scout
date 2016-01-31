package de.mm.android.longitude.fragment;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.mm.android.longitude.R;
import de.mm.android.longitude.base.BaseFragment;
import de.mm.android.longitude.model.ContactData;
import de.mm.android.longitude.recyclerview.ContactRecyclerAdapter;

/**
 * Created by Max on 31.05.2015.
 */
public class ContactListFragment extends BaseFragment implements UpdateAble, SwipeRefreshLayout.OnRefreshListener, AppBarLayout.OnOffsetChangedListener {
    private static final String ARG_CONTACTS = "contacts";
    private static final String ARG_LOCATION = "location";
    private static final String TAG = ContactListFragment.class.getSimpleName();

    public interface IContactFragment {
        void onContactRefreshClicked();
        void onContactClicked(final int which);
        void onContactPoked(final int which);
        void onContactDeleted(final int which);
        void onContactConfirmed(final int which, final boolean isAccepted);
        void onPokeAllClicked();
    }

    public static ContactListFragment newInstance(@NonNull ArrayList<ContactData> data, @NonNull Location location) {
        ContactListFragment f = new ContactListFragment();
        Bundle b = new Bundle();
        b.putParcelableArrayList(ARG_CONTACTS, data);
        if (location == null) {
            Log.w(TAG, "newInstance with null Location");
        }
        b.putParcelable(ARG_LOCATION, location);
        f.setArguments(b);
        return f;
    }

    private IContactFragment listener;
    private ContactRecyclerAdapter adapter;

    @Bind(R.id.f_contacts_appbar)
    AppBarLayout appBarLayout;

    @Bind(R.id.f_contacts_swipe_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    /* LifeCycle */

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof IContactFragment) {
            listener = (IContactFragment) getActivity();
        } else {
            throw new IllegalStateException(getActivity().getClass().getName() + " must implement " + IContactFragment.class.getName());
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.f_contacts, container, false);
        ButterKnife.bind(this, view);

        List<ContactData> contactList;
        Location myLocation;
        if (getArguments() != null && getArguments().containsKey(ARG_CONTACTS) && getArguments().containsKey(ARG_LOCATION)) {
            Log.d(TAG, "onCreateView: with Argument");
            contactList = getArguments().getParcelableArrayList(ARG_CONTACTS);
            myLocation = getArguments().getParcelable(ARG_LOCATION);
        } else if (savedInstanceState != null) {
            Log.d(TAG, "onCreateView: with Bundle");
            contactList = savedInstanceState.getParcelableArrayList(ARG_CONTACTS);
            myLocation =  savedInstanceState.getParcelable(ARG_LOCATION);
        } else {
            throw new IllegalStateException("Keine Argumente und kein SavedState... dann ist halt Feierabend");
        }

        Log.d(TAG, "onCreateView: " + contactList.size() + " " + myLocation);

        view.findViewById(R.id.f_contacts_fab).setOnClickListener(v -> listener.onPokeAllClicked());

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.mipmap.ic_drawer);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) view.findViewById(R.id.f_contacts_collapsing_toolbar);
        collapsingToolbar.setTitle(getString(R.string.title_contacts));

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.accentColor, R.color.accentColorDark, R.color.primaryColor);

        appBarLayout.addOnOffsetChangedListener(this);

        adapter = new ContactRecyclerAdapter(getActivity(), myLocation, new ContactRecyclerAdapter.MyRecyclerViewHolder.CardActionListener(){
            @Override
            public void onCardClicked(int position) {
                Log.d(TAG, "onCardClicked: " + position);
                listener.onContactClicked(contactList.get(position).getPerson_id());
            }

            @Override
            public void onCardPoked(int position) {
                Log.d(TAG, "onCardPoked: " + position);
                listener.onContactPoked(contactList.get(position).getPerson_id());
            }

            @Override
            public void onCardDeleted(int position) {
                Log.d(TAG, "onCardDeleted: " + position);
                listener.onContactDeleted(contactList.get(position).getPerson_id());
            }

            @Override
            public void onCardConfirmed(int position, boolean isAccepted) {
                Log.d(TAG, "onCardPoked: " + position + " " + isAccepted);
                listener.onContactConfirmed(contactList.get(position).getPerson_id(), isAccepted);
            }
        });
        adapter.addItems(contactList);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.f_contacts_scrollableview);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
        outState.putParcelableArrayList(ARG_CONTACTS, adapter.getItems());
        outState.putParcelable(ARG_LOCATION, adapter.getLocation());
    }

    @Override
    public void onResume() {
        appBarLayout.addOnOffsetChangedListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        appBarLayout.removeOnOffsetChangedListener(this);
        super.onPause();
    }

    /* AppBarLayout.OnOffsetChangedListener */

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        swipeRefreshLayout.setEnabled(verticalOffset == 0);
    }

    /* SwipeRefreshLayout.OnRefreshListener */

    @Override
    public void onRefresh() {
        if (isInetAvailable) {
            listener.onContactRefreshClicked();
        } else {
            showMessage(R.string.error_noNetworkConnectionFound);
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    /* UpdateAble */

    @Override
    public void onConnectivityUpdate(boolean isConnected) {
        isInetAvailable = isConnected;
    }

    @Override
    public void onDataReceived(@Nullable List<ContactData> data) {
        Log.d(TAG, "onDataReceived");
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
        if (data == null) {
            return;
        }
        Log.d(TAG, "onContactUpdate: " + data);
        adapter.clear();
        adapter.addItems(data);
    }

    /* Update via Activity */

    public void onPersonConfirmed(int confirmedPersonId, boolean isAccepted) {
        Log.d(TAG, "onPersonConfirmed: " + confirmedPersonId + " " + isAccepted);
//        swipeRefreshLayout.post(() -> swipeRefreshLayout.setRefreshing(true));

//        for (int i = 0; i < contactList.size(); i++) {
//            if (contactList.get(i).getPerson_id() == confirmedPersonId) {
//                if (isAccepted) {
//                    Log.d(TAG, "onPersonConfirmed.accepted = true");
////                    adapter.getItem(i).setAccepted(true);
//                    adapter.notifyItemChanged(i);
//                } else {
//                    Log.d(TAG, "onPersonConfirmed.accepted = false");
//                    contactList.remove(i);
//                    adapter.removeItem(i);
//                }
//                break;
//            }
//        }
    }

    public void onPersonDeleted(int deletedPersonId) {
        Log.d(TAG, "onPersonDeleted: " + deletedPersonId);
//        swipeRefreshLayout.post(() -> swipeRefreshLayout.setRefreshing(true));

//        for (int i = 0; i < contactList.size(); i++) {
//            if (contactList.get(i).getPerson_id() == deletedPersonId) {
//                contactList.remove(i);
//                adapter.removeItem(i);
//                break;
//            }
//        }
    }

}