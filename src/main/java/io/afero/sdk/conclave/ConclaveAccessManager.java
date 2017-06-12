/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.conclave;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.api.AferoClientAPI;
import io.afero.sdk.client.afero.models.ConclaveAccessBody;
import io.afero.sdk.client.afero.models.ConclaveAccessDetails;
import io.afero.sdk.utils.RxUtils;
import rx.Observable;
import rx.subjects.PublishSubject;

public class ConclaveAccessManager {

    private PublishSubject<ConclaveAccessDetails> mCADSubject = PublishSubject.create();
    private HubbyConclaveAccessObserver mObserver;
    private ConclaveAccessDetails mConclaveAccessDetails;
    private final AferoClient mAferoClient;


    public ConclaveAccessManager(AferoClient aferoClient) {
        mObserver = new HubbyConclaveAccessObserver(this);
        mAferoClient = aferoClient;
    }

    public Observable<ConclaveAccessDetails> getObservable() {
        return mCADSubject;
    }

    public void resetAccess() {
        mConclaveAccessDetails = null;
    }

    public Observable<ConclaveAccessDetails> getAccess(String clientId) {
        if (mConclaveAccessDetails == null) {
            return mAferoClient.postConclaveAccess(clientId)
                    .doOnNext(new ConclaveAccessAction(this));
        }
        return rx.Observable.just(mConclaveAccessDetails);
    }

    public void updateAccess(String clientId) {
        mAferoClient.postConclaveAccess(clientId)
                .subscribe(mObserver);
    }

    private void onNewConclaveAccessDetails(ConclaveAccessDetails cad) {
        mConclaveAccessDetails = cad;
        mCADSubject.onNext(cad);
    }

    private static class ConclaveAccessAction extends RxUtils.WeakAction1<ConclaveAccessDetails,ConclaveAccessManager> {

        public ConclaveAccessAction(ConclaveAccessManager strongRef) {
            super(strongRef);
        }

        @Override
        public void call(ConclaveAccessManager cam, ConclaveAccessDetails cad) {
            cam.onNewConclaveAccessDetails(cad);
        }
    };

    private static class HubbyConclaveAccessObserver extends RxUtils.WeakObserver<ConclaveAccessDetails,ConclaveAccessManager> {

        public HubbyConclaveAccessObserver(ConclaveAccessManager strongRef) {
            super(strongRef);
        }

        @Override
        public void onCompleted(ConclaveAccessManager cam) {

        }

        @Override
        public void onError(ConclaveAccessManager cam, Throwable e) {
        }

        @Override
        public void onNext(ConclaveAccessManager cam, ConclaveAccessDetails cad) {
            cam.onNewConclaveAccessDetails(cad);
        }
    }

    public ConclaveAccessDetails getConclaveAccessDetails() {
        return mConclaveAccessDetails;
    }


}
