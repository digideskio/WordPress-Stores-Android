package org.wordpress.android.stores.action;

public class Action<T> {
    private final IAction mActionType;
    private final T mPayload;
    private final long mId;

    // Package private constructor
    Action(IAction actionType, T payload, long id) {
        mActionType = actionType;
        mPayload = payload;
        mId = id;
    }

    public IAction getType() {
        return mActionType;
    }

    public T getPayload() {
        return mPayload;
    }

    public long getId() {
        return mId;
    }
}
