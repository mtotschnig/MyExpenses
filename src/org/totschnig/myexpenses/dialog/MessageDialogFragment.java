package org.totschnig.myexpenses.dialog;

import java.io.Serializable;

import org.totschnig.myexpenses.MyApplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.ContextThemeWrapper;

public class MessageDialogFragment extends DialogFragment implements OnClickListener {

  public static final MessageDialogFragment newInstance(
      int title,
      int message,
      int yesCommand,
      Serializable yesTag) {
    return newInstance(title, message,yesCommand, yesTag, 0,null);
  }
  public static final MessageDialogFragment newInstance(
      int title,
      int message,
      int yesCommand,
      Serializable yesTag,
      int noCommand,
      Serializable noTag) {
    return newInstance(title, MyApplication.getInstance().getString(message),
        yesCommand, yesTag, android.R.string.yes, noCommand, noTag, android.R.string.no);
  }
  public static final MessageDialogFragment newInstance(
      int title,
      CharSequence message,
      int yesCommand,
      Serializable yesTag) {
    return newInstance(title, message, yesCommand, yesTag,
        android.R.string.yes, android.R.string.no);
  }
  public static final MessageDialogFragment newInstance(
      int title,
      CharSequence message,
      int yesCommand,
      Serializable yesTag,
      int yesButton,
      int noButton) {
    return newInstance(title,message,yesCommand,yesTag,yesButton,0,null,noButton);
  }
  public static final MessageDialogFragment newInstance(
      int title,
      CharSequence message,
      int yesCommand,
      Serializable yesTag,
      int yesButton,
      int noCommand,
      Serializable noTag,
      int noButton) {
    MessageDialogFragment dialogFragment = new MessageDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putInt("title", title);
    bundle.putCharSequence("message", message);
    bundle.putInt("yesCommand", yesCommand);
    bundle.putInt("noCommand", noCommand);
    bundle.putSerializable("yesTag", yesTag);
    bundle.putSerializable("noTag", noTag);
    bundle.putInt("yesButton",yesButton);
    bundle.putInt("noButton",noButton);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Bundle bundle = getArguments();
    Activity ctx  = getActivity();
    //Applying the dark/light theme only works starting from 11, below, the dialog uses a dark theme
    //this is necessary only when we are called from one of the transparent activities,
    //but does not harm in the other cases
    Context wrappedCtx = Build.VERSION.SDK_INT > 10 ?
        new ContextThemeWrapper(ctx, MyApplication.getThemeId()) : ctx;
    return new AlertDialog.Builder(wrappedCtx)
      .setTitle(bundle.getInt("title"))
      .setMessage(bundle.getCharSequence("message"))
      .setNegativeButton(bundle.getInt("noButton"),this)
      .setPositiveButton(bundle.getInt("yesButton"),this)
      .create();
  }
  public void onCancel (DialogInterface dialog) {
    Bundle bundle = getArguments();
    int noCommand = bundle.getInt("noCommand");
    if (noCommand != 0)
      ((MessageDialogListener) getActivity())
        .dispatchCommand(noCommand, bundle.getSerializable("noTag"));
    else
      ((MessageDialogListener) getActivity()).cancelDialog();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    Bundle bundle = getArguments();
    if (which == AlertDialog.BUTTON_POSITIVE)
      ((MessageDialogListener) getActivity())
      .dispatchCommand(bundle.getInt("yesCommand"), bundle.getSerializable("yesTag"));
    else {
      onCancel(dialog);
    }
  }
  public interface MessageDialogListener {
    boolean dispatchCommand(int command, Object tag);
    void cancelDialog();
  }
}