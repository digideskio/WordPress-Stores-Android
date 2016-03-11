package org.wordpress.android.stores.action;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import org.wordpress.android.stores.store.Store;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import javax.inject.Singleton;

@Singleton
public class Dispatcher {
    private final Bus mBus;
    private long mEventCounter = 0;

    public Dispatcher() {
        mBus = new Bus(ThreadEnforcer.ANY);
    }

    public void register(final Object object) {
        mBus.register(object);
        if (object instanceof Store) {
            ((Store) object).onRegister();
        }
    }

    public void unregister(final Object object) {
        mBus.unregister(object);
    }

    public long dispatch(Action action) {
        AppLog.d(T.API, "Dispatching action: " + action.getType().getClass().getSimpleName()
                + "-" + action.getType().name() + " - id: " + action.getId());
        post(action);
        return action.getId();
    }

    public <T> long dispatch(final IAction actionType, final T payload) {
        return dispatch(createAction(actionType, payload));
    }

    public long dispatch(IAction actionType) {
        return dispatch(actionType, null);
    }

    public <T> Action<T> createAction(final IAction actionType, final T payload) {
        mEventCounter += 1;
        return new Action<T>(actionType, payload, mEventCounter);
    }

    public <T> Action<T> createAction(final IAction actionType) {
        return createAction(actionType, null);
    }

    public void emitChange(final Object changeEvent) {
        mBus.post(changeEvent);
    }

    private void post(final Object event) {
        mBus.post(event);
    }
}
