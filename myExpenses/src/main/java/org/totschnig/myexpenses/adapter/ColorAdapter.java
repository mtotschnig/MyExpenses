package org.totschnig.myexpenses.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.totschnig.myexpenses.R;

import java.util.ArrayList;

public class ColorAdapter extends BaseAdapter {
  private ArrayList<Integer> colors;
  private Context context;
  private LayoutInflater layoutInflater;
  public ColorAdapter(Context context, int selectedColor) {
    this.context = context;
    this.layoutInflater = LayoutInflater.from(context);
    colors = setupColors(context.getResources(), selectedColor);
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

  @Override
  public int getCount() {
    return colors.size();
  }

  @Override
  public Object getItem(int position) {
    return colors.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @NonNull
  @Override
  public View getView(int position, View convertView, @NonNull ViewGroup parent) {
    View view;
    if (convertView == null) {
      view = layoutInflater.inflate(R.layout.color_spinner_item, parent, false);
    } else {
      view = convertView;
    }
    Integer color =  (Integer) getItem(position);
    if (color != null) {
      view.setBackgroundColor(color);
    }
    return view;
  }

  @Override
  public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
    View view = layoutInflater.inflate(R.layout.color_spinner_dropdown_item, parent, false);
    Integer color =  (Integer) getItem(position);
    if (color != null) {
      view.findViewById(R.id.color).setBackgroundColor(color);
    }
    return view;
  }

  public int getPosition(int selectedColor) {
    return colors.indexOf(selectedColor);
  }
}
