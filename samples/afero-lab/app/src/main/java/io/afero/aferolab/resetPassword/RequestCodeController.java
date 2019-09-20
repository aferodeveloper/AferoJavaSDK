/*
 * Copyright (c) 2014-2019 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.resetPassword;

import io.afero.aferolab.BuildConfig;
import io.afero.sdk.client.afero.AferoClient;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;

class RequestCodeController {

    private RequestCodeView view;
    private AferoClient aferoClient;

    RequestCodeController(RequestCodeView requestCodeView, AferoClient aferoClient) {
        this.view = requestCodeView;
        this.aferoClient = aferoClient;
    }

    public void start() {

    }

    public void stop() {

    }

    void onClickRequestCode(String email) {
        view.showProgress();
        aferoClient.sendPasswordRecoveryEmail(email, BuildConfig.APPLICATION_ID, "ANDROID")
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<Void>() {
                @Override
                public void onCompleted() {
                    view.hideProgress();
                    gotoPasswordResetView();
                }

                @Override
                public void onError(Throwable e) {
                    view.hideProgress();
                    view.showError(e, aferoClient.getStatusCode(e));
                }

                @Override
                public void onNext(Void v) {

                }
            });
    }

    void onClickAlreadyHaveCode() {
        gotoPasswordResetView();
    }

    private void gotoPasswordResetView() {
        ResetPasswordView.create(view.getRootView()).start(aferoClient);
        view.stop();
    }
}
