package io.afero.aferolab.deviceTag;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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

class DeviceTagAdapter extends RecyclerView.Adapter<DeviceTagAdapter.ViewHolder> {

    private final Vector<DeviceTagCollection.Tag> mTags = new Vector<>();
    private final HashMap<String, DeviceTagCollection.Tag> mTagMap = new HashMap<>();
    private final PublishSubject<View> mOnClickViewSubject = PublishSubject.create();

    private Comparator<DeviceTagCollection.Tag> mSortComparator =
            new Comparator<DeviceTagCollection.Tag>() {
                @Override
                public int compare(DeviceTagCollection.Tag a, DeviceTagCollection.Tag b) {
                    String aKey = a.getKey();
                    String bKey = b.getKey();

                    if (aKey == null && bKey == null) {
                        return 0;
                    }

                    if (aKey == null) {
                        return -1;
                    }

                    if (bKey == null) {
                        return 1;
                    }

                    return a.getKey().compareTo(b.getKey());
                }
            };

    private Subscription mUpdateSubscription;
    private Subscription mRemoveSubscription;


    private final Action1<DeviceTagCollection.Tag> mAddTagAction = new Action1<DeviceTagCollection.Tag>() {
        @Override
        public void call(DeviceTagCollection.Tag tag) {
            synchronized (mTags) {

                int tagIndex = indexOf(tag);

                if (tagIndex != -1) {
                    updateTagAt(tagIndex, tag);
                } else {
                    tagIndex = Collections.binarySearch(mTags, tag, mSortComparator);
                    if (tagIndex < 0) {
                        tagIndex = -tagIndex - 1;
                    }

                    mTags.add(tagIndex, tag);
                    mTagMap.put(tag.getId(), tag);

                    notifyItemInserted(tagIndex);
                }
            }
        }
    };

    DeviceTagAdapter(DeviceModel deviceModel) {

        Func1<DeviceTagCollection.TagEvent, DeviceTagCollection.Tag> tagEventToTag =
                new Func1<DeviceTagCollection.TagEvent, DeviceTagCollection.Tag>() {
                    @Override
                    public DeviceTagCollection.Tag call(DeviceTagCollection.TagEvent tagEvent) {
                        return tagEvent.tag;
                    }
                };

        mUpdateSubscription = updateTags(Observable.from(deviceModel.getTags())
                .concatWith(deviceModel.getTagObservable()
                        .filter(new Func1<DeviceTagCollection.TagEvent, Boolean>() {
                            @Override
                            public Boolean call(DeviceTagCollection.TagEvent tagEvent) {
                                return tagEvent.action.equals(DeviceTagCollection.TagAction.ADD) ||
                                        tagEvent.action.equals(DeviceTagCollection.TagAction.UPDATE);
                            }
                        })
                        .map(tagEventToTag)
                ));

        mRemoveSubscription = removeTags(deviceModel.getTagObservable()
                .filter(new Func1<DeviceTagCollection.TagEvent, Boolean>() {
                    @Override
                    public Boolean call(DeviceTagCollection.TagEvent tagEvent) {
                        return tagEvent.action.equals(DeviceTagCollection.TagAction.DELETE);
                    }
                })
                .map(tagEventToTag)
        );
    }

    public void stop() {
        mUpdateSubscription = RxUtils.safeUnSubscribe(mUpdateSubscription);
        mRemoveSubscription = RxUtils.safeUnSubscribe(mRemoveSubscription);
    }

    Observable<View> getViewOnClick() {
        return mOnClickViewSubject;
    }

    private int indexOf(DeviceTagCollection.Tag tag) {
        DeviceTagCollection.Tag t = mTagMap.get(tag.getId());
        return t != null ? mTags.indexOf(t) : -1;
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
                                mTagMap.remove(tag.getId());
                                notifyItemRemoved(i);
                            }
                        }
                    }
                });
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        DeviceTagItemView tagView = (DeviceTagItemView) LayoutInflater.from(
                parent.getContext()).inflate(R.layout.view_tag_list_item, parent, false);
        tagView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnClickViewSubject.onNext(view);
            }
        });

        return new ViewHolder(tagView);
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

    void removeTag(DeviceTagCollection.Tag tag) {
        tag = mTagMap.get(tag.getId());
        int tagIndex = mTags.indexOf(tag);

        if (tagIndex >= 0) {
            mTags.remove(tagIndex);
            notifyItemRemoved(tagIndex);
        }
    }

    void updateTag(DeviceTagCollection.Tag tag) {
        DeviceTagCollection.Tag oldTag = mTagMap.get(tag.getId());
        int tagIndex = mTags.indexOf(oldTag);

        if (tagIndex >= 0) {
            updateTagAt(tagIndex, tag);
        }
    }

    private void updateTagAt(int tagIndex, DeviceTagCollection.Tag tag) {
        mTags.remove(tagIndex);

        int newIndex = Collections.binarySearch(mTags, tag, mSortComparator);
        if (newIndex < 0) {
            newIndex = -newIndex - 1;
        }

        mTags.add(newIndex, tag);
        mTagMap.put(tag.getId(), tag);

        if (newIndex != tagIndex) {
            notifyItemMoved(tagIndex, newIndex);
        } else {
            notifyItemChanged(tagIndex);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ViewHolder(View view) {
            super(view);
        }

        void update(DeviceTagCollection.Tag tag) {
            DeviceTagItemView tagView = (DeviceTagItemView) itemView;
            tagView.update(tag);
        }
    }
}
