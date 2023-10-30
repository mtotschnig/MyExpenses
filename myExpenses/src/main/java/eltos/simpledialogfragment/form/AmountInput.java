package eltos.simpledialogfragment.form;

import android.os.Parcel;

import java.math.BigDecimal;

import androidx.annotation.Nullable;

public class AmountInput extends FormElement<AmountInput, AmountInputViewHolder> {
  int fractionDigits;
  @Nullable
  BigDecimal amount;
  @Nullable BigDecimal max;
  String maxExceededError;
  @Nullable BigDecimal min;
  String underMinError;
  @Nullable Boolean withTypeSwitch;

  protected AmountInput(String resultKey) {
    super(resultKey);
  }

  public AmountInput fractionDigits(int i) {
    this.fractionDigits = i;
    return this;
  }

  public AmountInput amount(BigDecimal amount) {
    this.amount = amount;
    return this;
  }

  public AmountInput max(BigDecimal amount, String maxExceededError) {
    this.max = amount;
    this.maxExceededError = maxExceededError;
    return this;
  }


  public AmountInput min(BigDecimal amount, String underMinError) {
    this.min = amount;
    this.underMinError = underMinError;
    return this;
  }

  public AmountInput withTypeSwitch(Boolean withTypeSwitch) {
    this.withTypeSwitch = withTypeSwitch;
    return this;
  }

  public static AmountInput plain(String resultKey) {
    return new AmountInput(resultKey);
  }

  @Override
  public AmountInputViewHolder buildViewHolder() {
    return new AmountInputViewHolder(this);
  }


  protected AmountInput(Parcel in) {
    super(in);
    fractionDigits = in.readInt();
    String val = in.readString();
    if (val != null) {
      amount = new BigDecimal(val);
    }
    val = in.readString();
    if (val != null) {
      max = new BigDecimal(val);
    }
    maxExceededError = in.readString();
    val = in.readString();
    if (val != null) {
      min = new BigDecimal(val);
    }
    underMinError = in.readString();
    switch (in.readInt()) {
      case 1 -> withTypeSwitch = true;
      case -1 -> withTypeSwitch = false;
      default -> withTypeSwitch = null;
    }
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeInt(fractionDigits);
    dest.writeString(amount == null ? null : amount.toString());
    dest.writeString(max == null ? null : max.toString());
    dest.writeString(maxExceededError);
    dest.writeString(min == null ? null : min.toString());
    dest.writeString(underMinError);
    dest.writeInt(withTypeSwitch == null ? 0 : (withTypeSwitch? 1 : -1));
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<AmountInput> CREATOR = new Creator<>() {
    @Override
    public AmountInput createFromParcel(Parcel in) {
      return new AmountInput(in);
    }

    @Override
    public AmountInput[] newArray(int size) {
      return new AmountInput[size];
    }
  };
}
