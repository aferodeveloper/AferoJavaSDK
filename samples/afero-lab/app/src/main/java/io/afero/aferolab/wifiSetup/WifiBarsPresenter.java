package io.afero.aferolab.wifiSetup;


import io.afero.hubby.Hubby;

class WifiBarsPresenter {

    private final WifiBarsView mView;

    WifiBarsPresenter(WifiBarsView wifiBarsView) {
        mView = wifiBarsView;
    }

    void setStatus(int rssi, boolean isSecure) {
        final int bars = Hubby.convertRSSIToBars(rssi);
        mView.showStatus(bars, isSecure);
    }
}
