/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.attributeEditor;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import io.afero.aferolab.R;
import io.afero.aferolab.widget.AferoEditText;
import io.afero.aferolab.widget.ScreenView;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;


public class AttributeEditorView extends ScreenView {

    public enum ValueEditorType {
        NONE,
        TEXT,
        NUMBER,
        NUMBER_DECIMAL,
        BOOLEAN,
        BYTES
    }

    @BindView(R.id.attribute_id_text)
    TextView mAttributeIdText;

    @BindView(R.id.attribute_label_text)
    TextView mAttributeLabelText;

    @BindView(R.id.attribute_data_type_text)
    TextView mAttributeDataTypeText;

    @BindView(R.id.attribute_timestamp_text)
    TextView mAttributeTimestampText;

    @BindView(R.id.attribute_value_options_label)
    TextView mAttributeValueOptionsLabelText;

    @BindView(R.id.attribute_value_text)
    AferoEditText mAttributeValueEditText;

    @BindView(R.id.attribute_value_seekbar)
    SeekBar mAttributeValueSeekBar;

    @BindView(R.id.attribute_value_switch)
    Switch mAttributeValueSwitch;

    @BindView(R.id.attribute_value_button)
    ImageButton mAttributeValueButton;

    @BindView(R.id.view_scrim)
    View mScrimView;

    @BindView(R.id.attribute_editor_card)
    CardView mAttributeCard;

    private PopupMenu mPopupMenu;

    private static final long ENTER_TRANSITION_DURATION = 100;
    private static final long EXIT_TRANSITION_DURATION = 100;

    private final AttributeEditorController mController = new AttributeEditorController(this);
    private final TimeInterpolator mEnterTransitionInterpolator = new OvershootInterpolator();

    private ValueEditorType mEditorType = ValueEditorType.NONE;

    private boolean mValueEditTextEnabled;

    private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            mController.onAttributeValueNumberEditorChanging(getAttributeValueSliderProportion());
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mController.onAttributeValueNumberEditorChangeComplete(getAttributeValueSliderProportion());
        }
    };

    private CompoundButton.OnCheckedChangeListener mSwitchChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            mController.onAttributeValueBooleanEditorChanged(b);
        }
    };


    public AttributeEditorView(@NonNull Context context) {
        super(context);
    }

    public AttributeEditorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AttributeEditorView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                stop();
            }
        });

        mAttributeValueSeekBar.setMax(Integer.MAX_VALUE);
        mAttributeValueSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mAttributeValueSwitch.setOnCheckedChangeListener(mSwitchChangeListener);
    }

    public void start(DeviceModel deviceModel, DeviceProfile.Attribute attribute) {
        if (!isStarted()) {
            pushOnBackStack();

            mAttributeValueEditText.setEnabled(false);
            mAttributeValueEditText.setVisibility(VISIBLE);
            mAttributeValueSeekBar.setVisibility(View.GONE);
            mAttributeValueSwitch.setVisibility(View.GONE);
            mAttributeValueButton.setVisibility(View.GONE);
            mAttributeValueEditText.setFilters(new InputFilter[]{});
            mAttributeValueEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            mAttributeValueOptionsLabelText.setText("");

            mController.start(deviceModel, attribute);

            startEnterTransition();
        }
    }

    public void stop() {
        if (isStarted()) {
            mController.stop();
            mAttributeValueEditText.hideKeyboard();
            mPopupMenu = null;
            startExitTransition();
        }

        removeFromBackStack();
    }

    public boolean isStarted() {
        return mController.isStarted();
    }

    public void setEditorEnabled(boolean enabled) {
        mAttributeValueButton.setEnabled(enabled);
        mAttributeValueEditText.setEnabled(enabled && mValueEditTextEnabled);
        mAttributeValueSeekBar.setEnabled(enabled);
        mAttributeValueSwitch.setEnabled(enabled);
    }

    public void addEnumItem(String label, String value) {
        mAttributeValueButton.setVisibility(View.VISIBLE);

        if (mPopupMenu == null) {
            mPopupMenu = new PopupMenu(getContext(), mAttributeValueButton, Gravity.BOTTOM);
        }

        final MenuItem item = mPopupMenu.getMenu().add(label);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

            String itemValue;

            MenuItem.OnMenuItemClickListener init(String v) {
                itemValue = v;
                return this;
            }

            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                mController.onAttributeValueSelected(itemValue);
                return true;
            }
        }.init(value));
    }

    public void setAttributeValueEnumText(String label) {
        mAttributeValueOptionsLabelText.setText(label);
    }

    public void setAttributeIdText(int id) {
        mAttributeIdText.setText(Integer.toString(id));
    }

    public void setAttributeLabelText(@Nullable String label) {
        mAttributeLabelText.setText(label != null ? label : "");
    }

    public void setAttributeDataTypeText(@Nullable String s) {
        mAttributeDataTypeText.setText(s != null ? s : "-");
    }

    public void setAttributeTimestampText(@Nullable String s) {
        mAttributeTimestampText.setText(s != null ? s : "-");
    }

    public void setAttributeValueText(String valueText) {
        mAttributeValueEditText.setText(valueText != null ? valueText : "");
    }

    public void setAttributeValueSwitch(boolean value) {
        mAttributeValueSwitch.setOnCheckedChangeListener(null);
        mAttributeValueSwitch.setChecked(value);
        mAttributeValueSwitch.setOnCheckedChangeListener(mSwitchChangeListener);
    }

    public void setAttributeValueEditorType(ValueEditorType editorType) {
        mEditorType = editorType;

        int numberInputType = 0;

        switch (mEditorType) {
            case NONE:
                break;

            case TEXT:
                mValueEditTextEnabled = true;
                mAttributeValueEditText.setEnabled(true);
                break;

            case NUMBER_DECIMAL:
                numberInputType |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
            case NUMBER:
                numberInputType |= InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
                mValueEditTextEnabled = true;
                mAttributeValueEditText.setEnabled(true);
                mAttributeValueEditText.setInputType(numberInputType);
                mAttributeValueSeekBar.setVisibility(View.VISIBLE);
                break;

            case BOOLEAN:
                mAttributeValueSwitch.setVisibility(VISIBLE);
                break;

            case BYTES:
                mValueEditTextEnabled = true;
                mAttributeValueEditText.setEnabled(true);
                mAttributeValueEditText.setFilters(new InputFilter[]{new CharacterInputFilter("0123456789ABCDEF")});
                mAttributeValueEditText.setInputType(mAttributeValueEditText.getInputType() | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
                break;
        }
    }

    public void setAttributeValueSliderMax(int max) {
        mAttributeValueSeekBar.setMax(max);
    }

    public void setAttributeValueSliderProportion(double proportion) {
        mAttributeValueSeekBar.setOnSeekBarChangeListener(null);
        mAttributeValueSeekBar.setProgress((int) Math.round(proportion * (double) mAttributeValueSeekBar.getMax()));
        mAttributeValueSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
    }

    public double getAttributeValueSliderProportion() {
        return Math.min((double) mAttributeValueSeekBar.getProgress() / (double) mAttributeValueSeekBar.getMax(), 1.0);
    }

    @OnClick(R.id.attribute_value_button)
    void onClickAttributeButton() {
        mPopupMenu.show();
    }

    @OnEditorAction(R.id.attribute_value_text)
    boolean onAttributeValueEditorAction(TextView textView, int actionId, KeyEvent event) {

        if (AferoEditText.isDone(actionId, event)) {
            mController.onAttributeValueTextEditorChanged(textView.getText().toString());
            mAttributeValueEditText.hideKeyboard();
        }

        return true;
    }

    private void startEnterTransition() {
        setVisibility(VISIBLE);
        mScrimView.setAlpha(0);
        mScrimView.animate().alpha(1).setDuration(ENTER_TRANSITION_DURATION);
        mAttributeCard.setAlpha(0);
        mAttributeCard.setScaleY(.1f);
        mAttributeCard.animate().scaleY(1).alpha(1)
                .setInterpolator(mEnterTransitionInterpolator)
                .setDuration(ENTER_TRANSITION_DURATION);
    }

    private void startExitTransition() {
        mScrimView.animate().alpha(0).setDuration(EXIT_TRANSITION_DURATION);
        mAttributeCard.animate().scaleY(.1f).alpha(0)
                .setDuration(EXIT_TRANSITION_DURATION)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        setVisibility(INVISIBLE);
                    }
                });
    }

    private class CharacterInputFilter implements InputFilter {
        private final String mAllowedCharacters;

        CharacterInputFilter(String allowedCharacters) {
            mAllowedCharacters = allowedCharacters;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            for (int i = start; i < end; i++) {
                if (mAllowedCharacters.indexOf(source.charAt(i)) == -1) {
                    return "";
                }
            }
            return null;
        }
    }
}
