/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.calendar;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.provider.CalendarContract;

import org.totschnig.myexpenses.util.Utils;

@SuppressLint({ "NewApi", "InlinedApi" })
public final class CalendarContractCompat {
    /**
     * True if we have to use the legacy API found on 2.x and 3.x
     */
    public static final boolean legacyApi = !Utils.hasApiLevel(Build.VERSION_CODES.ICE_CREAM_SANDWICH);

    public static String AUTHORITY,
      EXTRA_EVENT_BEGIN_TIME,
      EXTRA_EVENT_END_TIME,
      EXTRA_EVENT_ALL_DAY,
      CALLER_IS_SYNCADAPTER,
      ACCOUNT_TYPE_LOCAL,
      ACTION_HANDLE_CUSTOM_EVENT,
      EXTRA_CUSTOM_APP_URI;
    public static Uri CONTENT_URI;
    static {
        if (legacyApi) {
          try {
            //noinspection PrivateApi
            AUTHORITY = (String) Class.forName("android.provider.Calendar").getField("AUTHORITY").get(null);
          } catch (Exception e) {
            AUTHORITY = "com.android.calendar";
          }
          EXTRA_EVENT_BEGIN_TIME = "beginTime";
          EXTRA_EVENT_END_TIME = "endTime";
          EXTRA_EVENT_ALL_DAY = "allDay";
          CONTENT_URI = Uri.parse("content://" + AUTHORITY);
          CALLER_IS_SYNCADAPTER = "caller_is_syncadapter";
          ACCOUNT_TYPE_LOCAL = "LOCAL";
        } else {
          AUTHORITY = CalendarContract.AUTHORITY;
          EXTRA_EVENT_BEGIN_TIME = CalendarContract.EXTRA_EVENT_BEGIN_TIME;
          EXTRA_EVENT_END_TIME = CalendarContract.EXTRA_EVENT_END_TIME;
          EXTRA_EVENT_ALL_DAY = CalendarContract.EXTRA_EVENT_ALL_DAY;
          CONTENT_URI = CalendarContract.CONTENT_URI;
          CALLER_IS_SYNCADAPTER = CalendarContract.CALLER_IS_SYNCADAPTER;
          ACCOUNT_TYPE_LOCAL = CalendarContract.ACCOUNT_TYPE_LOCAL;
        }
        if (android.os.Build.VERSION.SDK_INT>=16) {
          ACTION_HANDLE_CUSTOM_EVENT = CalendarContract.ACTION_HANDLE_CUSTOM_EVENT;
          EXTRA_CUSTOM_APP_URI = CalendarContract.EXTRA_CUSTOM_APP_URI;
        }
    }

    /**
     * This utility class cannot be instantiated
     */
    private CalendarContractCompat() {}

    public static final class Calendars implements BaseColumns {
      public static final String ACCOUNT_NAME,
          ACCOUNT_TYPE,
          CALENDAR_COLOR,
          CALENDAR_DISPLAY_NAME,
          OWNER_ACCOUNT,
          NAME,
          CALENDAR_LOCATION,
          CALENDAR_ACCESS_LEVEL,
          VISIBLE,
          SYNC_EVENTS;
      public static Uri CONTENT_URI;
      public static final int CAL_ACCESS_NONE ,
          CAL_ACCESS_FREEBUSY ,
          CAL_ACCESS_READ ,
          CAL_ACCESS_RESPOND ,
          CAL_ACCESS_OVERRIDE ,
          CAL_ACCESS_CONTRIBUTOR ,
          CAL_ACCESS_EDITOR ,
          CAL_ACCESS_OWNER ,
          CAL_ACCESS_ROOT;
      static {
        if (legacyApi) {
          ACCOUNT_NAME =  "_sync_account";
          ACCOUNT_TYPE = "_sync_account_type";
          CALENDAR_COLOR = "color";
          CALENDAR_DISPLAY_NAME = "displayName";
          OWNER_ACCOUNT = "ownerAccount";
          CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/calendars");
          NAME = "name";
          CALENDAR_LOCATION = "location";
          VISIBLE = "selected";
          SYNC_EVENTS = "sync_events";
          CALENDAR_ACCESS_LEVEL = "access_level";
          CAL_ACCESS_NONE = 0;
          CAL_ACCESS_FREEBUSY = 100;
          CAL_ACCESS_READ = 200;
          CAL_ACCESS_RESPOND = 300;
          CAL_ACCESS_OVERRIDE = 400;
          CAL_ACCESS_CONTRIBUTOR = 500;
          CAL_ACCESS_EDITOR = 600;
          CAL_ACCESS_OWNER = 700;
          CAL_ACCESS_ROOT = 800;
        } else {
          ACCOUNT_NAME =  CalendarContract.Calendars.ACCOUNT_NAME;
          ACCOUNT_TYPE = CalendarContract.Calendars.ACCOUNT_TYPE;
          CALENDAR_COLOR = CalendarContract.Calendars.CALENDAR_COLOR;
          CALENDAR_DISPLAY_NAME = CalendarContract.Calendars.CALENDAR_DISPLAY_NAME;
          OWNER_ACCOUNT = CalendarContract.Calendars.OWNER_ACCOUNT;
          CONTENT_URI = CalendarContract.Calendars.CONTENT_URI;
          NAME = CalendarContract.Calendars.NAME;
          CALENDAR_LOCATION = CalendarContract.Calendars.CALENDAR_LOCATION;
          VISIBLE = CalendarContract.Calendars.VISIBLE;
          SYNC_EVENTS = CalendarContract.Calendars.SYNC_EVENTS;
          CALENDAR_ACCESS_LEVEL = CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL;
          CAL_ACCESS_NONE = CalendarContract.Calendars.CAL_ACCESS_NONE;
          CAL_ACCESS_FREEBUSY = CalendarContract.Calendars.CAL_ACCESS_FREEBUSY;
          CAL_ACCESS_READ = CalendarContract.Calendars.CAL_ACCESS_READ;
          CAL_ACCESS_RESPOND = CalendarContract.Calendars.CAL_ACCESS_RESPOND;
          CAL_ACCESS_OVERRIDE = CalendarContract.Calendars.CAL_ACCESS_OVERRIDE;
          CAL_ACCESS_CONTRIBUTOR = CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR;
          CAL_ACCESS_EDITOR = CalendarContract.Calendars.CAL_ACCESS_EDITOR;
          CAL_ACCESS_OWNER = CalendarContract.Calendars.CAL_ACCESS_OWNER;
          CAL_ACCESS_ROOT = CalendarContract.Calendars.CAL_ACCESS_ROOT;
        }
      }
        /**
         * This utility class cannot be instantiated
         */
        private Calendars() {}
    }

    public static final class Events implements BaseColumns {
      public static final String CALENDAR_ID,
          TITLE,
          DESCRIPTION,
          EVENT_LOCATION,
          DTSTART,
          DTEND,
          DURATION,
          EVENT_TIMEZONE,
          HAS_ALARM,
          RRULE,
          CUSTOM_APP_PACKAGE,
          CUSTOM_APP_URI,
          ALL_DAY;
      public static final Uri CONTENT_URI;
      static {
        if (legacyApi) {
          CALENDAR_ID = "calendar_id";
          TITLE = "title";
          DESCRIPTION = "description";
          EVENT_LOCATION = "eventLocation";
          DTSTART = "dtstart";
          DTEND = "dtend";
          DURATION = "duration";
          EVENT_TIMEZONE = "eventTimezone";
          HAS_ALARM = "hasAlarm";
          RRULE = "rrule";
          ALL_DAY = "allDay";
          CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/events");
        } else {
          CALENDAR_ID = CalendarContract.Events.CALENDAR_ID;
          TITLE = CalendarContract.Events.TITLE;
          DESCRIPTION = CalendarContract.Events.DESCRIPTION;
          EVENT_LOCATION = CalendarContract.Events.EVENT_LOCATION;
          DTSTART = CalendarContract.Events.DTSTART;
          DTEND = CalendarContract.Events.DTEND;
          DURATION = CalendarContract.Events.DURATION;
          EVENT_TIMEZONE = CalendarContract.Events.EVENT_TIMEZONE;
          HAS_ALARM = CalendarContract.Events.HAS_ALARM;
          RRULE = CalendarContract.Events.RRULE;
          ALL_DAY = CalendarContract.Events.ALL_DAY;
          CONTENT_URI = CalendarContract.Events.CONTENT_URI;
        }
        if (android.os.Build.VERSION.SDK_INT>=16) {
          CUSTOM_APP_PACKAGE = CalendarContract.Events.CUSTOM_APP_PACKAGE;
          CUSTOM_APP_URI = CalendarContract.Events.CUSTOM_APP_URI;
        } else {
          CUSTOM_APP_PACKAGE = "customAppPackage";
          CUSTOM_APP_URI = "customAppUri";
        }
      }
    }

    /**
     * Fields and helpers for interacting with Instances. An instance is a
     * single occurrence of an event including time zone specific start and end
     * days and minutes. The instances table is not writable and only provides a
     * way to query event occurrences.
     */
    public static final class Instances {
      public static String BEGIN, END, EVENT_ID, TITLE;
      public static final Uri CONTENT_URI;
      static {
        if (legacyApi) {
          CONTENT_URI = Uri.parse("content://" + AUTHORITY +
              "/instances/when");
          BEGIN = "begin";
          END = "end";
          EVENT_ID = "event_id";
          TITLE = "title";
        } else {
          CONTENT_URI = CalendarContract.Instances.CONTENT_URI;
          BEGIN = CalendarContract.Instances.BEGIN;
          END = CalendarContract.Instances.END;
          EVENT_ID = CalendarContract.Instances.EVENT_ID;
          TITLE = CalendarContract.Instances.TITLE;
        }
      }
        /**
         * This utility class cannot be instantiated
         */
        private Instances() {}
    }
}
