package org.totschnig.myexpenses.viewmodel;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import timber.log.Timber;

/**
 * A lifecycle-aware observable that sends only new updates after subscription, used for events like
 * navigation and Snackbar messages.
 * <p>
 * This avoids a common problem with events: on configuration change (like rotation) an update
 * can be emitted if the observer is active. This LiveData only calls the observable if there's an
 * explicit call to setValue() or call().
 * <p>
 * Note that only one observer is going to be notified of changes.
 */
//https://github.com/googlesamples/android-architecture/blob/dev-todo-mvvm-live/todoapp/app/src/main/java/com/example/android/architecture/blueprints/todoapp/SingleLiveEvent.java
public class SingleLiveEvent<T> extends MutableLiveData<T> {

  private static final String TAG = "SingleLiveEvent";

  private final AtomicBoolean mPending = new AtomicBoolean(false);

  @MainThread
  public void observe(@NonNull LifecycleOwner owner, @NonNull final Observer<? super T> observer) {

    if (hasActiveObservers()) {
      Timber.tag(TAG).w("Multiple observers registered but only one will be notified of changes.");
    }

    // Observe the internal MutableLiveData
    super.observe(owner, t -> {
      if (mPending.compareAndSet(true, false)) {
        observer.onChanged(t);
      }
    });
  }

  @MainThread
  public void setValue(@Nullable T t) {
    mPending.set(true);
    super.setValue(t);
  }

  /**
   * Used for cases where T is Void, to make calls cleaner.
   */
  @MainThread
  public void call() {
    setValue(null);
  }
}