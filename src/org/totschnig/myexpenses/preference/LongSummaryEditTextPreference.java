package org.totschnig.myexpenses.preference;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class LongSummaryEditTextPreference extends EditTextPreference
{
    public LongSummaryEditTextPreference(Context ctx, AttributeSet attrs, int defStyle)
    {
        super(ctx, attrs, defStyle);        
    }

    public LongSummaryEditTextPreference(Context ctx, AttributeSet attrs)
    {
        super(ctx, attrs);  
    }

    @Override
    protected void onBindView(View view)
    {       
        super.onBindView(view);

        TextView summary= (TextView)view.findViewById(android.R.id.summary);
        summary.setMaxLines(10);
    }
}