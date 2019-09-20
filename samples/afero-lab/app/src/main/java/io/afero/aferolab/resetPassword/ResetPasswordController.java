/*
 * Copyright (c) 2014-2019 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.resetPassword;

import io.afero.sdk.client.afero.AferoClient;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;

class ResetPasswordController {

    private ResetPasswordView view;
    private AferoClient aferoClient;

    ResetPasswordController(ResetPasswordView resetPasswordView, AferoClient aferoClient) {
        this.view = resetPasswordView;
        this.aferoClient = aferoClient;
    }

    public void start() {

    }

    public void stop() {

    }

    void onClickResetPassword(String code, String password) {
        view.showProgress();
        aferoClient.resetPasswordWithCode(code, password)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<Void>() {
                @Override
                public void onCompleted() {
                    view.stop();
                }

                @Override
                public void onError(Throwable e) {
                    view.hideProgress();
                    view.showError(e, aferoClient.getStatusCode(e));
                }

                @Override
                public void onNext(Void aVoid) {

                }
            });
    }
}
