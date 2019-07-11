package eltos.simpledialogfragment.form;

import android.os.Parcel;

import java.math.BigDecimal;

import androidx.annotation.Nullable;

public class AmountEdit  extends FormElement<AmountEdit, AmountEditViewHolder> {
  int fractionDigits;
  @Nullable
  BigDecimal amount;
  @Nullable BigDecimal max;
  String maxExceededError;
  @Nullable BigDecimal min;
  String underMinError;

  protected AmountEdit(String resultKey) {
    super(resultKey);
  }

  public AmountEdit fractionDigits(int i) {
    this.fractionDigits = i;
    return this;
  }

  public AmountEdit amount(BigDecimal amount) {
    this.amount = amount;
    return this;
  }

  public AmountEdit max(BigDecimal amount, String maxExceededError) {
    this.max = amount;
    this.maxExceededError = maxExceededError;
    return this;
  }


  public AmountEdit min(BigDecimal amount, String underMinError) {
    this.min = amount;
    this.underMinError = underMinError;
    return this;
  }

  public static AmountEdit plain(String resultKey) {
    return new AmountEdit(resultKey);
  }

  @Override
  public AmountEditViewHolder buildViewHolder() {
    return new AmountEditViewHolder(this);
  }


  protected AmountEdit(Parcel in) {
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
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<AmountEdit> CREATOR = new Creator<AmountEdit>() {
    @Override
    public AmountEdit createFromParcel(Parcel in) {
      return new AmountEdit(in);
    }

    @Override
    public AmountEdit[] newArray(int size) {
      return new AmountEdit[size];
    }
  };
}
