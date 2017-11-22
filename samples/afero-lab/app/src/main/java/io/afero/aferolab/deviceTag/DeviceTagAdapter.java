package io.afero.aferolab.deviceTag;


import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

import static android.support.v7.util.SortedList.INVALID_POSITION;

class DeviceTagAdapter extends RecyclerView.Adapter<DeviceTagAdapter.ViewHolder> {

    private final SortedList<DeviceTagCollection.Tag> mTags = new SortedList<>(
            DeviceTagCollection.Tag.class,
            new SortedListAdapterCallback<DeviceTagCollection.Tag>(this) {
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

                @Override
                public boolean areContentsTheSame(DeviceTagCollection.Tag oldItem, DeviceTagCollection.Tag newItem) {
                    String oldKey = oldItem.getKey() != null ? oldItem.getKey() : "";
                    String newKey = newItem.getKey() != null ? newItem.getKey() : "";
                    String oldValue = oldItem.getValue() != null ? oldItem.getValue() : "";
                    String newValue = newItem.getValue() != null ? newItem.getValue() : "";

                    return oldKey.equals(newKey) && oldValue.equals(newValue);
                }

                @Override
                public boolean areItemsTheSame(DeviceTagCollection.Tag item1, DeviceTagCollection.Tag item2) {
                    return item1.getId().equals(item2.getId());
                }
            });

    private final Action1<DeviceTagCollection.Tag> mUpdateTagAction = new Action1<DeviceTagCollection.Tag>() {
        @Override
        public void call(DeviceTagCollection.Tag tag) {
            addOrUpdateTag(tag);
        }
    };

    private final PublishSubject<View> mOnClickViewSubject = PublishSubject.create();

    private Subscription mUpdateSubscription;
    private Subscription mRemoveSubscription;


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
                                return !tagEvent.action.equals(DeviceTagCollection.TagAction.DELETE);
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

    private Subscription updateTags(Observable<DeviceTagCollection.Tag> tags) {
        return tags.onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mUpdateTagAction);
    }

    private Subscription removeTags(Observable<DeviceTagCollection.Tag> tags) {
        return tags.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<DeviceTagCollection.Tag>() {
                    @Override
                    public void call(DeviceTagCollection.Tag tag) {
                        removeTag(tag);
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
        DeviceTagCollection.Tag tag;
        synchronized (mTags) {
            tag = mTags.get(position);
        }
        holder.update(tag);
    }

    @Override
    public int getItemCount() {
        return mTags.size();
    }

    DeviceTagCollection.Tag getTagAt(int itemPosition) {
        return mTags.get(itemPosition);
    }

    void addOrUpdateTag(DeviceTagCollection.Tag tag) {
        synchronized (mTags) {
            int tagIndex = getTagIndexById(tag.getId());
            if (tagIndex != INVALID_POSITION) {
                mTags.updateItemAt(tagIndex, tag);
            } else {
                mTags.add(tag);
            }
        }
    }

    void removeTag(DeviceTagCollection.Tag tag) {
        synchronized (mTags) {
            int tagIndex = getTagIndexById(tag.getId());
            if (tagIndex != INVALID_POSITION) {
                mTags.removeItemAt(tagIndex);
            }
        }
    }

    void updateTag(DeviceTagCollection.Tag tag) {
        synchronized (mTags) {
            int tagIndex = getTagIndexById(tag.getId());
            if (tagIndex != INVALID_POSITION) {
                mTags.updateItemAt(tagIndex, tag);
            }
        }
    }

    private int getTagIndexById(String tagId) {
        synchronized (mTags) {
            for (int i = 0, n = mTags.size(); i < n; ++i) {
                DeviceTagCollection.Tag tag = mTags.get(i);
                if (tag.getId().equals(tagId)) {
                    return i;
                }
            }
        }

        return INVALID_POSITION;
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
