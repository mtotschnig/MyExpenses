package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.R;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
//...
public class EditTextDialog extends DialogFragment implements OnEditorActionListener {

 public interface EditTextDialogListener {
     void onFinishEditDialog(Bundle args);
 }

 private EditText mEditText;

 public static final EditTextDialog newInstance(Bundle args) {
   EditTextDialog dialogFragment = new EditTextDialog();
   dialogFragment.setArguments(args);
   return dialogFragment;
 }
   

 @Override
 public View onCreateView(LayoutInflater inflater, ViewGroup container,
         Bundle savedInstanceState) {
     Bundle args = getArguments();
     mEditText = new EditText(getActivity());
     getDialog().setTitle(args.getString("dialogTitle"));
     // Show soft keyboard automatically
     mEditText.setInputType(InputType.TYPE_CLASS_TEXT);
     mEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
     mEditText.requestFocus();
     getDialog().getWindow().setSoftInputMode(
             LayoutParams.SOFT_INPUT_STATE_VISIBLE);
     mEditText.setOnEditorActionListener(this);
     mEditText.setId(1);
     mEditText.setText(args.getString("value"));
     //input.setSingleLine();
     return mEditText;
 }

 @Override
 public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
     if (EditorInfo.IME_ACTION_DONE == actionId) {
         // Return input text to activity
         EditTextDialogListener activity = (EditTextDialogListener) getActivity();
         Bundle args = getArguments();
         String result = mEditText.getText().toString();
         if (result.equals(""))
           Toast.makeText(getActivity(),getString(R.string.no_title_given), Toast.LENGTH_LONG).show();
         else {
           args.putString("result", result);
           activity.onFinishEditDialog(args);
           this.dismiss();
           return true;
         }
     }
     return false;
 }
}