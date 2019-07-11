/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.setupwizardlib.template;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.android.setupwizardlib.TemplateLayout;
import com.android.setupwizardlib.view.NavigationBar;

/**
 * A mixin to require the a scrollable container (BottomScrollView, RecyclerView or ListView) to
 * be scrolled to bottom, making sure that the user sees all content above and below the fold.
 */
public class RequireScrollMixin implements Mixin {

    /* static section */

    /**
     * Listener for when the require-scroll state changes. Note that this only requires the user to
     * scroll to the bottom once - if the user scrolled to the bottom and back-up, scrolling to
     * bottom is not required again.
     */
    public interface OnRequireScrollStateChangedListener {

        /**
         * Called when require-scroll state changed.
         *
         * @param scrollNeeded True if the user should be required to scroll to bottom.
         */
        void onRequireScrollStateChanged(boolean scrollNeeded);
    }

    /**
     * A delegate to detect scrollability changes and to scroll the page. This provides a layer
     * of abstraction for BottomScrollView, RecyclerView and ListView. The delegate should call
     * {@link #notifyScrollabilityChange(boolean)} when the view scrollability is changed.
     */
    interface ScrollHandlingDelegate {

        /**
         * Starts listening to scrollability changes at the target scrollable container.
         */
        void startListening();

        /**
         * Scroll the page content down by one page.
         */
        void pageScrollDown();
    }

    /* non-static section */

    @NonNull
    private final TemplateLayout mTemplateLayout;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private boolean mRequiringScrollToBottom = false;

    // Whether the user have seen the more button yet.
    private boolean mEverScrolledToBottom = false;

    private ScrollHandlingDelegate mDelegate;

    @Nullable
    private OnRequireScrollStateChangedListener mListener;

    /**
     * @param templateLayout The template containing this mixin
     */
    public RequireScrollMixin(@NonNull TemplateLayout templateLayout) {
        mTemplateLayout = templateLayout;
    }

    /**
     * Sets the delegate to handle scrolling. The type of delegate should depend on whether the
     * scrolling view is a BottomScrollView, RecyclerView or ListView.
     */
    public void setScrollHandlingDelegate(@NonNull ScrollHandlingDelegate delegate) {
        mDelegate = delegate;
    }

    /**
     * Listen to require scroll state changes. When scroll is required,
     * {@link OnRequireScrollStateChangedListener#onRequireScrollStateChanged(boolean)} is called
     * with {@code true}, and vice versa.
     */
    public void setOnRequireScrollStateChangedListener(
            @Nullable OnRequireScrollStateChangedListener listener) {
        mListener = listener;
    }

    /**
     * @return The scroll state listener previously set, or {@code null} if none is registered.
     */
    public OnRequireScrollStateChangedListener getOnRequireScrollStateChangedListener() {
        return mListener;
    }

    /**
     * Creates an {@link OnClickListener} which if scrolling is required, will scroll the page down,
     * and if scrolling is not required, delegates to the wrapped {@code listener}. Note that you
     * should call {@link #requireScroll()} as well in order to start requiring scrolling.
     *
     * @param listener The listener to be invoked when scrolling is not needed and the user taps on
     *                 the button. If {@code null}, the click listener will be a no-op when scroll
     *                 is not required.
     * @return A new {@link OnClickListener} which will scroll the page down or delegate to the
     *         given listener depending on the current require-scroll state.
     */
    public OnClickListener createOnClickListener(@Nullable final OnClickListener listener) {
        return new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mRequiringScrollToBottom) {
                    mDelegate.pageScrollDown();
                } else if (listener != null) {
                    listener.onClick(view);
                }
            }
        };
    }

    /**
     * Coordinate with the given navigation bar to require scrolling on the page. The more button
     * will be shown instead of the next button while scrolling is required.
     */
    public void requireScrollWithNavigationBar(@NonNull final NavigationBar navigationBar) {
        setOnRequireScrollStateChangedListener(
                new OnRequireScrollStateChangedListener() {
                    @Override
                    public void onRequireScrollStateChanged(boolean scrollNeeded) {
                        navigationBar.getMoreButton()
                                .setVisibility(scrollNeeded ? View.VISIBLE : View.GONE);
                        navigationBar.getNextButton()
                                .setVisibility(scrollNeeded ? View.GONE : View.VISIBLE);
                    }
                });
        navigationBar.getMoreButton().setOnClickListener(createOnClickListener(null));
        requireScroll();
    }

    /**
     * @see #requireScrollWithButton(Button, CharSequence, OnClickListener)
     */
    public void requireScrollWithButton(
            @NonNull Button button,
            @StringRes int moreText,
            @Nullable OnClickListener onClickListener) {
        requireScrollWithButton(button, button.getContext().getText(moreText), onClickListener);
    }

    /**
     * Use the given {@code button} to require scrolling. When scrolling is required, the button
     * label will change to {@code moreText}, and tapping the button will cause the page to scroll
     * down.
     *
     * <p>Note: Calling {@link View#setOnClickListener} on the button after this method will remove
     * its link to the require-scroll mechanism. If you need to do that, obtain the click listener
     * from {@link #createOnClickListener(OnClickListener)}.
     *
     * <p>Note: The normal button label is taken from the button's text at the time of calling this
     * method. Calling {@link android.widget.TextView#setText} after calling this method causes
     * undefined behavior.
     *
     * @param button The button to use for require scroll. The button's "normal" label is taken from
     *               the text at the time of calling this method, and the click listener of it will
     *               be replaced.
     * @param moreText The button label when scroll is required.
     * @param onClickListener The listener for clicks when scrolling is not required.
     */
    public void requireScrollWithButton(
            @NonNull final Button button,
            final CharSequence moreText,
            @Nullable OnClickListener onClickListener) {
        final CharSequence nextText = button.getText();
        button.setOnClickListener(createOnClickListener(onClickListener));
        setOnRequireScrollStateChangedListener(new OnRequireScrollStateChangedListener() {
            @Override
            public void onRequireScrollStateChanged(boolean scrollNeeded) {
                button.setText(scrollNeeded ? moreText : nextText);
            }
        });
        requireScroll();
    }

    /**
     * @return True if scrolling is required. Note that this mixin only requires the user to
     * scroll to the bottom once - if the user scrolled to the bottom and back-up, scrolling to
     * bottom is not required again.
     */
    public boolean isScrollingRequired() {
        return mRequiringScrollToBottom;
    }

    /**
     * Start requiring scrolling on the layout. After calling this method, this mixin will start
     * listening to scroll events from the scrolling container, and call
     * {@link OnRequireScrollStateChangedListener} when the scroll state changes.
     */
    public void requireScroll() {
        mDelegate.startListening();
    }

    /**
     * {@link ScrollHandlingDelegate} should call this method when the scrollability of the
     * scrolling container changed, so this mixin can recompute whether scrolling should be
     * required.
     *
     * @param canScrollDown True if the view can scroll down further.
     */
    void notifyScrollabilityChange(boolean canScrollDown) {
        if (canScrollDown == mRequiringScrollToBottom) {
            // Already at the desired require-scroll state
            return;
        }
        if (canScrollDown) {
            if (!mEverScrolledToBottom) {
                postScrollStateChange(true);
                mRequiringScrollToBottom = true;
            }
        } else {
            postScrollStateChange(false);
            mRequiringScrollToBottom = false;
            mEverScrolledToBottom = true;
        }
    }

    private void postScrollStateChange(final boolean scrollNeeded) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onRequireScrollStateChanged(scrollNeeded);
                }
            }
        });
    }
}
