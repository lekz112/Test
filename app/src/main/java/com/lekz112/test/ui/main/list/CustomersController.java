package com.lekz112.test.ui.main.list;

import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewFlipper;

import com.bluelinelabs.conductor.Controller;
import com.bluelinelabs.conductor.RouterTransaction;
import com.lekz112.test.R;
import com.lekz112.test.di.util.ControllerInjection;
import com.lekz112.test.service.Customer;
import com.lekz112.test.service.network.ReservationService;
import com.lekz112.test.ui.OnItemClickListener;
import com.lekz112.test.ui.main.tables.TablesController;
import com.lekz112.test.ui.util.Titleable;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;

public class CustomersController extends Controller implements Titleable, OnItemClickListener {

    private static final int PROGRESS = 0;
    private static final int CONTENT = 1;
    private static final int ERROR = 2;

    @Bind(R.id.list_view_flipper)
    ViewFlipper viewFlipper;
    @Bind(R.id.list_content)
    RecyclerView recyclerView;
    @Bind(R.id.list_swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;

    @Inject
    ReservationService reservationService;
    @Inject
    CustomersAdapter customersAdapter;

    private Disposable customersSubscription = Disposables.disposed();

    public CustomersController() {
        // Do not destroy this when we push another controller
        setRetainViewMode(RetainViewMode.RETAIN_DETACH);
    }

    @NonNull
    @Override
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        ControllerInjection.inject(this);
        View view = inflater.inflate(R.layout.view_list, container, false);
        ButterKnife.bind(this, view);

        customersAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(customersAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        swipeRefreshLayout.setOnRefreshListener(this::loadCustomers);
        // Start loading
        loadCustomers();

        return view;
    }

    @Override
    protected void onDestroyView(@NonNull View view) {
        ButterKnife.unbind(this);
        customersSubscription.dispose();
        super.onDestroyView(view);
    }

    private void loadCustomers() {
        viewFlipper.setDisplayedChild(PROGRESS);
        customersSubscription.dispose();
        customersSubscription = reservationService.getCustomers()
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate(() -> swipeRefreshLayout.setRefreshing(false))
                .subscribe(customers -> {
                    viewFlipper.setDisplayedChild(CONTENT);
                    customersAdapter.setCustomersList(customers);
                }, error -> viewFlipper.setDisplayedChild(ERROR));
    }

    @Override
    public String getTitle() {
        return getActivity().getString(R.string.customers_title);
    }

    @Override
    public void onItemClick(int position) {
        Customer customer = customersAdapter.getCustomer(position);
        TablesController tablesController = new TablesController();
        tablesController.setCustomer(customer);
        getRouter().pushController(RouterTransaction.with(tablesController));

    }

    @OnClick(R.id.list_button_retry)
    public void onRetryClick() {
        loadCustomers();
    }

}
