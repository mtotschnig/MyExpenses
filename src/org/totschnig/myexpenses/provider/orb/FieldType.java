/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package org.totschnig.myexpenses.provider.orb;

import android.content.ContentValues;
import android.database.Cursor;

import java.lang.reflect.Field;
import java.util.Date;

public abstract class FieldType {

  public static FieldType DOUBLE = new FieldType() {
    @Override
    public Object valueFromCursor(Cursor c, int columnIndex) {
      return c.getDouble(columnIndex);
    }

    @Override
    protected void putValue(ContentValues values, String key, Object value) {
      values.put(key, (Double) value);
    }
  };
  public static FieldType FLOAT = new FieldType() {
    @Override
    public Object valueFromCursor(Cursor c, int columnIndex) {
      return c.getFloat(columnIndex);
    }

    @Override
    protected void putValue(ContentValues values, String key, Object value) {
      values.put(key, (Float) value);
    }
  };
  public static FieldType INT = new FieldType() {
    @Override
    public Object valueFromCursor(Cursor c, int columnIndex) {
      return c.getInt(columnIndex);
    }

    @Override
    protected void putValue(ContentValues values, String key, Object value) {
      values.put(key, (Integer) value);
    }
  };
  public static FieldType LONG = new FieldType() {
    @Override
    public Object valueFromCursor(Cursor c, int columnIndex) {
      return c.getLong(columnIndex);
    }

    @Override
    protected void putValue(ContentValues values, String key, Object value) {
      values.put(key, (Long) value);
    }
  };
  public static FieldType SHORT = new FieldType() {
    @Override
    public Object valueFromCursor(Cursor c, int columnIndex) {
      return c.getShort(columnIndex);
    }

    @Override
    protected void putValue(ContentValues values, String key, Object value) {
      values.put(key, (Short) value);
    }
  };
  public static FieldType STRING = new FieldType() {
    @Override
    public Object valueFromCursor(Cursor c, int columnIndex) {
      return c.getString(columnIndex);
    }

    @Override
    protected void putValue(ContentValues values, String key, Object value) {
      values.put(key, (String) value);
    }
  };
  public static FieldType BOOLEAN = new FieldType() {
    @Override
    public Object valueFromCursor(Cursor c, int columnIndex) {
      return c.getInt(columnIndex) == 1;
    }

    @Override
    protected void putValue(ContentValues values, String key, Object value) {
      values.put(key, Boolean.TRUE.equals(value) ? 1 : 0);
    }
  };
  public static FieldType DATE = new FieldType() {
    @Override
    public Object valueFromCursor(Cursor c, int columnIndex) {
      long d = c.getLong(columnIndex);
      return d == 0 ? null : new Date(d);
    }

    @Override
    protected void putValue(ContentValues values, String key, Object value) {
      values.put(key, ((Date) value).getTime());
    }
  };

  public static class ENUM extends FieldType {

    public final Class<Enum> enumType;

    public ENUM(Class<Enum> enumType) {
      this.enumType = enumType;
    }

    @Override
    public Object valueFromCursor(Cursor c, int columnIndex) {
      String name = c.getString(columnIndex);
      return name == null ? null : Enum.valueOf(enumType, name);
    }

    @Override
    protected void putValue(ContentValues values, String key, Object value) {
      values.put(key, ((Enum) value).name());
    }

  }

  public static class ENTITY extends FieldType {

    public final Class<?> clazz;

    public ENTITY(Class<?> clazz) {
      this.clazz = clazz;
    }

    @Override
    protected void putValue(ContentValues values, String key, Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    protected Object valueFromCursor(Cursor c, int columnIndex) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPrimitive() {
      return false;
    }
  }

  public boolean isPrimitive() {
    return true;
  }

  protected abstract Object valueFromCursor(Cursor c, int columnIndex);

  protected abstract void putValue(ContentValues values, String key,
      Object value);

  public static FieldType getType(Field field) {
    Class<?> clazz = field.getType();
    if (Double.class == clazz || Double.TYPE == clazz) {
      return FieldType.DOUBLE;
    } else if (Float.class == clazz || Float.TYPE == clazz) {
      return FieldType.FLOAT;
    } else if (Integer.class == clazz || Integer.TYPE == clazz) {
      return FieldType.INT;
    } else if (Long.class == clazz || Long.TYPE == clazz) {
      return FieldType.LONG;
    } else if (Short.class == clazz || Short.TYPE == clazz) {
      return FieldType.SHORT;
    } else if (Boolean.class == clazz || Boolean.TYPE == clazz) {
      return FieldType.BOOLEAN;
    } else if (String.class == clazz) {
      return FieldType.STRING;
    } else if (Date.class == clazz) {
      return FieldType.DATE;
    } else if (clazz.isEnum()) {
      return new ENUM((Class<Enum>) clazz);
    }
    throw new IllegalArgumentException("Field [" + field
        + "] has unsupported type.");
  }

  public Object getValueFromCursor(Cursor c, String columnName) {
    int columnIndex = c.getColumnIndexOrThrow(columnName);
    return valueFromCursor(c, columnIndex);
  }

  public void setValue(ContentValues values, String key, Object value) {
    if (value == null) {
      values.putNull(key);
    } else {
      putValue(values, key, value);
    }
  }
}
