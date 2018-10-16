package eltos.simpledialogfragment.form;

import android.view.WindowManager;

public class SimpleFormDialogWithoutDefaultFocus extends SimpleFormDialog {
  @Override
  protected void onDialogShown() {
    // resize dialog when keyboard is shown to prevent fields from hiding behind the keyboard
    if (getDialog() != null && getDialog().getWindow() != null) {
      getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    setPositiveButtonEnabled(posButtonEnabled());
  }
}
