package org.totschnig.myexpenses.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.totschnig.myexpenses.R;

import java.util.ArrayList;

public class ColorAdapter extends ArrayAdapter<Integer> {
  public ColorAdapter(Context context, int selectedColor) {
    super(context, android.R.layout.simple_spinner_item, setupColors(context.getResources(), selectedColor));
    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
  }

  private static ArrayList<Integer> setupColors(Resources resources, int selectedColor) {
    ArrayList<Integer> colors = new ArrayList<>();
    colors.add(resources.getColor(R.color.material_red));
    colors.add(resources.getColor(R.color.material_pink));
    colors.add(resources.getColor(R.color.material_purple));
    colors.add(resources.getColor(R.color.material_deep_purple));
    colors.add(resources.getColor(R.color.material_indigo));
    colors.add(resources.getColor(R.color.material_blue));
    colors.add(resources.getColor(R.color.material_light_blue));
    colors.add(resources.getColor(R.color.material_cyan));
    colors.add(resources.getColor(R.color.material_teal));
    colors.add(resources.getColor(R.color.material_green));
    colors.add(resources.getColor(R.color.material_light_green));
    colors.add(resources.getColor(R.color.material_lime));
    colors.add(resources.getColor(R.color.material_yellow));
    colors.add(resources.getColor(R.color.material_amber));
    colors.add(resources.getColor(R.color.material_orange));
    colors.add(resources.getColor(R.color.material_deep_orange));
    colors.add(resources.getColor(R.color.material_brown));
    colors.add(resources.getColor(R.color.material_grey));
    colors.add(resources.getColor(R.color.material_blue_grey));
    if (colors.indexOf(selectedColor) == -1) {
      colors.add(selectedColor);
    }
    return colors;
  }

  @NonNull
  @Override
  public View getView(int position, View convertView, @NonNull ViewGroup parent) {
    TextView tv = (TextView) super.getView(position, convertView, parent);
    setColor(tv, getItem(position));
    return tv;
  }

  @Override
  public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
    TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
    setColor(tv, getItem(position));
    return tv;
  }

  private void setColor(TextView tv, int color) {
    tv.setBackgroundColor(color);
    tv.setText("");
    tv.setContentDescription(getContext().getString(R.string.color));
  }
}
