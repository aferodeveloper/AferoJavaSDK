package io.afero.aferolab.wifiSetup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.ButterKnife;
import io.afero.aferolab.R;
import io.afero.hubby.WifiSSIDEntry;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.softhub.DeviceWifiSetup;
import io.afero.sdk.utils.RxUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;


class WifiSSIDListAdapter extends ArrayAdapter<WifiSSIDEntry> {
    private final DeviceWifiSetup mWifiSetup;
    private final PublishSubject<WifiSSIDListAdapter> mSubject = PublishSubject.create();
    private PublishSubject<WifiSSIDListAdapter> mStartSubject;
    private Subscription mGetWifiSSIDListSubscription;

    private static class ViewHolder {
        View topItemDivider;
        ImageView networkConnectedIndicator;
        TextView networkNameView;
        WifiBarsView networkBarsView;
        int barsIconResId;
    }

    public WifiSSIDListAdapter(Context context, DeviceWifiSetup ws) {
        super(context, R.layout.view_wifi_ssid);
        mWifiSetup = ws;
    }

    public Observable<WifiSSIDListAdapter> start() {

        mStartSubject = PublishSubject.create();

        mGetWifiSSIDListSubscription = mWifiSetup.getWifiSSIDList()
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<WifiSSIDEntry>() {
                    @Override
                    public void onCompleted() {
                        AfLog.e("WifiSSIDListAdapter.start.onCompleted");
                        mStartSubject.onNext(WifiSSIDListAdapter.this);
                        mStartSubject.onCompleted();
                        mStartSubject = null;
                    }

                    @Override
                    public void onError(Throwable e) {
                        AfLog.d("WifiSSIDListAdapter.onError");
                        e.printStackTrace();
                        mStartSubject.onError(e);
                        mStartSubject = null;
                    }

                    @Override
                    public void onNext(WifiSSIDEntry wifiSSIDEntry) {
                        AfLog.d("WifiSSIDListAdapter.start.onNext: " + wifiSSIDEntry.getSSID());

                        // Only add non hidded ssids to avoid showing empty rows
                        if(!wifiSSIDEntry.getSSID().trim().isEmpty()) {
                            add(wifiSSIDEntry);
                        }
                    }
                });

        return mStartSubject;
    }

    public void stop() {
        mGetWifiSSIDListSubscription = RxUtils.safeUnSubscribe(mGetWifiSSIDListSubscription);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            view = inflater.inflate(R.layout.view_wifi_ssid, parent, false);
        }

        WifiSSIDEntry entry = getItem(position);
        ViewHolder vh = getViewHolder(view, entry);

        vh.topItemDivider.setVisibility(position == 0 ? View.VISIBLE : View.GONE);


        vh.networkNameView.setText(entry.getSSID());

        vh.networkConnectedIndicator.setVisibility(entry.isConnected() ? View.VISIBLE : View.GONE);

        vh.networkBarsView.setStatus(entry.getRSSI(), entry.isSecure());

        return view;
    }

    private ViewHolder getViewHolder(View view, WifiSSIDEntry entry) {
        ViewHolder vh = (ViewHolder)view.getTag();

        if (vh == null) {
            vh = new ViewHolder();
            view.setTag(vh);
            vh.topItemDivider = view.findViewById(R.id.top_item_divider);
            vh.networkConnectedIndicator = ButterKnife.findById(view, R.id.network_connected);
            vh.networkNameView = ButterKnife.findById(view, R.id.network_name);
            vh.networkBarsView = ButterKnife.findById(view, R.id.network_wifi_bars);
        }

        return vh;
    }
}
