package io.afero.aferolab.deviceTag;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import io.afero.aferolab.R;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceTagCollection;
import io.afero.sdk.utils.RxUtils;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

public class DeviceTagAdapter extends RecyclerView.Adapter<DeviceTagAdapter.ViewHolder> {

    private final DeviceModel mDeviceModel;
    private final Vector<DeviceTagCollection.Tag> mTags = new Vector<>();
    private final PublishSubject<View> mOnClickViewSubject = PublishSubject.create();
    private final Func1<DeviceTagCollection.TagEvent, DeviceTagCollection.Tag> mTagEventToTag =
            new Func1<DeviceTagCollection.TagEvent, DeviceTagCollection.Tag>() {
                @Override
                public DeviceTagCollection.Tag call(DeviceTagCollection.TagEvent tagEvent) {
                    return tagEvent.tag;
                }
            };

    private Comparator<DeviceTagCollection.Tag> mSortComparator =
            new Comparator<DeviceTagCollection.Tag>() {
                @Override
                public int compare(DeviceTagCollection.Tag a, DeviceTagCollection.Tag b) {
                    return a.getKey().compareTo(b.getKey());
                }
            };

    private Subscription mUpdateSubscription;
    private Subscription mRemoveSubscription;


    private final Action1<DeviceTagCollection.Tag> mAddTagAction = new Action1<DeviceTagCollection.Tag>() {
        @Override
        public void call(DeviceTagCollection.Tag tag) {
            synchronized (mTags) {
                int i = mTags.indexOf(tag);
                if (i != -1) {
                    mTags.remove(i);

                    int newIndex = Collections.binarySearch(mTags, tag, mSortComparator);
                    if (newIndex < 0) {
                        newIndex = -newIndex - 1;
                    }

                    mTags.add(newIndex, tag);

                    if (newIndex != i) {
                        notifyItemMoved(i, newIndex);
                    } else {
                        notifyItemChanged(i);
                    }
                } else {
                    i = Collections.binarySearch(mTags, tag, mSortComparator);
                    if (i < 0) {
                        i = -i - 1;
                    }

                    mTags.add(i, tag);
                    notifyItemInserted(i);
                }
            }
        }
    };

    DeviceTagAdapter(DeviceModel deviceModel) {
        mDeviceModel = deviceModel;

        mUpdateSubscription = updateTags(Observable.from(deviceModel.getTags())
                .concatWith(deviceModel.getTagObservable()
                        .filter(new Func1<DeviceTagCollection.TagEvent, Boolean>() {
                            @Override
                            public Boolean call(DeviceTagCollection.TagEvent tagEvent) {
                                return tagEvent.action.equals(DeviceTagCollection.TagAction.ADD) ||
                                        tagEvent.action.equals(DeviceTagCollection.TagAction.UPDATE);
                            }
                        })
                        .map(mTagEventToTag)
                ));

        mRemoveSubscription = removeTags(deviceModel.getTagObservable()
                .filter(new Func1<DeviceTagCollection.TagEvent, Boolean>() {
                    @Override
                    public Boolean call(DeviceTagCollection.TagEvent tagEvent) {
                        return tagEvent.action.equals(DeviceTagCollection.TagAction.DELETE);
                    }
                })
                .map(mTagEventToTag)
        );
    }

    public void stop() {
        mUpdateSubscription = RxUtils.safeUnSubscribe(mUpdateSubscription);
        mRemoveSubscription = RxUtils.safeUnSubscribe(mRemoveSubscription);
    }

    Observable<View> getViewOnClick() {
        return mOnClickViewSubject;
    }

    public int indexOf(DeviceTagCollection.Tag tag) {
        return mTags.indexOf(tag);
    }

    private Subscription updateTags(Observable<DeviceTagCollection.Tag> tags) {
        return tags.onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mAddTagAction);
    }

    private Subscription removeTags(Observable<DeviceTagCollection.Tag> tags) {
        return tags.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<DeviceTagCollection.Tag>() {
                    @Override
                    public void call(DeviceTagCollection.Tag tag) {
                        synchronized (mTags) {
                            int i = indexOf(tag);
                            if (i != -1) {
                                mTags.remove(i);
                                notifyItemRemoved(i);
                            }
                        }
                    }
                });
    }

    private boolean isDevicePresentable(DeviceModel deviceModel) {
        return deviceModel.getPresentation() != null;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_tag_list_item, parent, false);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnClickViewSubject.onNext(view);
            }
        });

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DeviceTagCollection.Tag tag = mTags.get(position);
        holder.update(tag);
    }

    @Override
    public int getItemCount() {
        return mTags.size();
    }

    DeviceTagCollection.Tag getTagAt(int itemPosition) {
        return mTags.get(itemPosition);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ViewHolder(View view) {
            super(view);
        }

        void update(DeviceTagCollection.Tag tag) {
            ((DeviceTagItemView) itemView).update(tag);
        }
    }
}
