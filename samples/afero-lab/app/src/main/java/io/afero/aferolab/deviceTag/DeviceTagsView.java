/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.deviceTag;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.afero.aferolab.R;
import io.afero.aferolab.widget.ScreenView;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceTagCollection;
import rx.Observable;
import rx.functions.Action1;
import rx.subjects.PublishSubject;


public class DeviceTagsView extends ScreenView {

    @BindView(R.id.tag_recycler_view)
    RecyclerView mTagListView;


    private DeviceTagController mController;
    private DeviceTagAdapter mAdapter;
    private final PublishSubject<DeviceTagCollection.Tag> mOnClickDeviceSubject = PublishSubject.create();

    public static DeviceTagsView create(View creatorView) {
        ViewGroup parent = ScreenView.getRootContainer(creatorView);
        DeviceTagsView view = (DeviceTagsView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_device_tags, parent, false);
        parent.addView(view);

        return view;
    }

    public DeviceTagsView(@NonNull Context context) {
        super(context);
    }

    public DeviceTagsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DeviceTagsView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mTagListView.setLayoutManager(layoutManager);

        DividerItemDecoration dividerDecoration = new DividerItemDecoration(getContext(), layoutManager.getOrientation());
        dividerDecoration.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.device_list_divider));
        mTagListView.addItemDecoration(dividerDecoration);
    }

    public void start(DeviceModel deviceModel) {
        pushOnBackStack();

        mController = new DeviceTagController(this, deviceModel);

        mAdapter = new DeviceTagAdapter(deviceModel);
        mTagListView.setAdapter(mAdapter);

        mAdapter.getViewOnClick().subscribe(
                new Action1<View>() {
                    @Override
                    public void call(View view) {
                        int itemPosition = mTagListView.getChildLayoutPosition(view);
                        if (itemPosition != RecyclerView.NO_POSITION) {
                            mController.editTag(mAdapter.getTagAt(itemPosition));
                        }
                    }
                });
    }

    @Override
    public void stop() {
        mAdapter.stop();
        super.stop();
    }

    @OnClick(R.id.add_tag_button)
    void onClickAddTag() {
        mController.addTag();
    }

    Observable<TagEditor.Result> openTagEditor(DeviceTagCollection.Tag tag) {
        return new TagEditor(this, R.string.wifi_password_dialog_title).start(tag);
    }

}
