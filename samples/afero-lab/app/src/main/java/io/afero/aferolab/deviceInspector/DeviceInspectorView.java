/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.deviceInspector;

import android.animation.LayoutTransition;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.afero.aferolab.R;
import io.afero.aferolab.addDevice.AddDeviceView;
import io.afero.aferolab.attributeEditor.AttributeEditorView;
import io.afero.aferolab.databinding.ViewDeviceInspectorBinding;
import io.afero.aferolab.deviceTag.DeviceTagsView;
import io.afero.aferolab.widget.ScreenView;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.device.DeviceCollection;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.scheduler.OfflineScheduleEvent;
import io.afero.sdk.scheduler.OfflineScheduler;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.PublishSubject;


public class DeviceInspectorView extends ScreenView {

    private ViewDeviceInspectorBinding binding;

    private static final int TRANSITION_DURATION = 200;

    private DeviceInspectorController mController;
    private final AttributeAdapter mAttributeAdapter = new AttributeAdapter();
    private PublishSubject<DeviceInspectorView> mViewSubject;

    public DeviceInspectorView(@NonNull Context context) {
        super(context);
    }

    public DeviceInspectorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DeviceInspectorView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static DeviceInspectorView create(ViewGroup parent) {
        DeviceInspectorView view = (DeviceInspectorView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_device_inspector, parent, false);
        parent.addView(view);

        return view;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        binding = ViewDeviceInspectorBinding.bind(this);

        LayoutTransition lt = binding.deviceInfoCard.getLayoutTransition();
        if (lt != null) {
            lt.enableTransitionType(LayoutTransition.CHANGING);
            lt.setStartDelay(LayoutTransition.CHANGING, 0);
            lt.setStartDelay(LayoutTransition.CHANGE_APPEARING, 0);
            lt.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0);
        }

        lt = binding.attributesCard.getLayoutTransition();
        if (lt != null) {
            lt.enableTransitionType(LayoutTransition.CHANGING);
            lt.setStartDelay(LayoutTransition.CHANGING, 0);
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.deviceAttributeRecyclerView.setLayoutManager(layoutManager);
        binding.deviceAttributeRecyclerView.setAdapter(mAttributeAdapter);

        binding.deviceInfoExtraOpen.setOnClickListener(v -> onClickDeviceInfoOpen());
        binding.deviceInfoExtraClose.setOnClickListener(v -> onClickDeviceInfoClose());
        binding.deviceTagButton.setOnClickListener(v -> onClickTagsButton());
        binding.deviceReadScheduleButton.setOnClickListener(v -> onClickReadScheduleButton());
        binding.deviceWriteScheduleButton.setOnClickListener(v -> onClickWriteScheduleButton());
        binding.deviceDeleteButton.setOnClickListener(v -> onClickDelete());
        binding.wifiConnectButton.setOnClickListener(v -> onClickWifiConnect());
    }

    public void start(DeviceModel deviceModel, DeviceCollection deviceCollection, AferoClient aferoClient) {
        if (isStarted()) {
            return;
        }

        pushOnBackStack();

        if (mController == null) {
            mController = new DeviceInspectorController(this, deviceCollection, aferoClient);
        }
        mController.start(deviceModel);

        mAttributeAdapter.start(deviceModel);
        mAttributeAdapter.getViewOnClick()
                .subscribe(new Action1<View>() {
                    @Override
                    public void call(View view) {
                        int pos = binding.deviceAttributeRecyclerView.getChildAdapterPosition(view);
                        if (pos != RecyclerView.NO_POSITION) {
                            startAttributeEditor(mController.getDeviceModel(), mAttributeAdapter.getAttributeAt(pos));
                        }
                    }
                });

        startEnterTransition();
    }

    @Override
    public void stop() {
        if (isStarted()) {
            mController.stop();
            mAttributeAdapter.stop();

            startExitTransition();
        }

        removeFromBackStack();
    }

    public boolean isStarted() {
        return mController != null && mController.isStarted();
    }

    public Observable<DeviceInspectorView> getObservable() {
        if (mViewSubject == null) {
            mViewSubject = PublishSubject.create();
        }
        return mViewSubject;
    }

    public void setDeviceNameText(String name) {
        binding.deviceNameText.setText(name);
    }

    public void setDeviceStatusText(@StringRes int statusResId) {
        binding.deviceStatusText.setText(statusResId);
    }

    void onClickDeviceInfoOpen() {
        binding.deviceInfoExtraContainer.setVisibility(VISIBLE);
        binding.deviceInfoExtraOpen.setVisibility(GONE);
        binding.deviceInfoExtraClose.setVisibility(VISIBLE);
    }

    void onClickDeviceInfoClose() {
        binding.deviceInfoExtraContainer.setVisibility(GONE);
        binding.deviceInfoExtraOpen.setVisibility(VISIBLE);
        binding.deviceInfoExtraClose.setVisibility(GONE);
    }

    void onClickTagsButton() {
        DeviceTagsView tagsView = DeviceTagsView.create(this);
        tagsView.start(mController.getDeviceModel());
    }

    void onClickReadScheduleButton() {
        AfLog.d("Read Scedules");

        OfflineScheduler os = new OfflineScheduler();
        os.start(mController.getDeviceModel());
        os.readFromDevice();
        os.getScheduleEvents()
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<OfflineScheduleEvent>() {
                    @Override
                    public void call(OfflineScheduleEvent event) {
                        AfLog.d("Got event " + event);

                        if (event.hasCompactDayRepresentation()) {
                            // Event supports multiple weekdays
                            for (int day = 1; day <= 7; day++) {
                                AfLog.d("Day " + event.hasDay(day));
                            }
                        } else {
                            // Event only supports one day
                            event.getDay();
                        }
                    }
                });
    }


    void onClickWriteScheduleButton() {
        Toast toast = Toast.makeText(getContext(), "See code for example", Toast.LENGTH_SHORT);
        toast.show();

//        OfflineScheduleEvent event = new OfflineScheduleEvent(true);
//        event.setId(59002);
//        event.setDay(OfflineScheduleEvent.SUNDAY);
//        event.setDay(OfflineScheduleEvent.MONDAY);
//        event.setDay(OfflineScheduleEvent.WEDNESDAY);
//        event.setDay(OfflineScheduleEvent.THURSDAY);
//        event.setDay(OfflineScheduleEvent.SATURDAY);
//
//        event.setHour(18);
//        event.setMinute(25);
//
//        final int value = 1;
//        AttributeValue av = new AttributeValue(Integer.toString(value), AttributeValue.DataType.SINT8);
//        event.addAttributeValue(1, av);
//
//        OfflineScheduler os = new OfflineScheduler();
//
//
//        os.start(mController.getDeviceModel());
//
//        os.addEvent(event);
//        os.writeToDevice();
//        os.writeMasterSwitchFlag(true);
    }

    void onClickDelete() {
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.dialog_message_remove_device)
                .setCancelable(true)
                .setPositiveButton(R.string.button_title_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mController.deleteDevice();
                    }
                })
                .show();
    }

    void onClickWifiConnect() {
        mController.onWifiConnect();
    }

    void onCompleted() {
        mViewSubject.onCompleted();
        mViewSubject = null;
    }

    void showProgress() {
        binding.deviceInspectorProgress.getRoot().setVisibility(View.VISIBLE);
    }

    void hideProgress() {
        binding.deviceInspectorProgress.getRoot().setVisibility(View.GONE);
    }

    private void startEnterTransition() {
        setVisibility(VISIBLE);
        binding.viewScrim.setAlpha(0);
        binding.viewScrim.animate().alpha(1).setDuration(TRANSITION_DURATION);
        binding.deviceInfoCard.setTranslationX(getWidth());
        binding.deviceInfoCard.animate().translationX(0).setDuration(TRANSITION_DURATION);
        binding.attributesCard.setTranslationX(getWidth());
        binding.attributesCard.animate().translationX(0).setDuration(TRANSITION_DURATION);
    }

    private void startExitTransition() {
        binding.viewScrim.animate().alpha(0).setDuration(TRANSITION_DURATION);
        binding.deviceInfoCard.animate().translationX(getWidth()).setDuration(TRANSITION_DURATION);
        binding.attributesCard.animate().translationX(getWidth())
                .setDuration(TRANSITION_DURATION)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mAttributeAdapter.clear();
                        setVisibility(INVISIBLE);
                    }
                });
    }

    private void startAttributeEditor(DeviceModel deviceModel, DeviceProfile.Attribute attribute) {
        AttributeEditorView view = getRootView().findViewById(R.id.attribute_editor);
        view.start(deviceModel, attribute);

    }

    public void showWifiSetup(boolean isVisible) {
        binding.wifiConnectButton.setVisibility(isVisible ? VISIBLE : GONE);
    }

    public void enableWifiSetup(boolean isEnabled) {
        binding.wifiConnectButton.setEnabled(isEnabled);
    }
}
