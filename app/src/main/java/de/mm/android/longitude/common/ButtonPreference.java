package de.mm.android.longitude.common;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;

import de.mm.android.longitude.R;

/**
 * Created by Max on 12.04.2015.
 */
public class ButtonPreference extends Preference {

    public interface ClickListener{
        void onClick();
    }

    private ClickListener listener;

    public ButtonPreference(Context context) {
        super(context);
        init();
    }

    public ButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ButtonPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWidgetLayoutResource(R.layout.preference);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        view.findViewById(R.id.preference_button).setOnClickListener(v -> listener.onClick());
    }

    public void setOnButtonClickListener(ClickListener listener){
        this.listener = listener;
    }

}

