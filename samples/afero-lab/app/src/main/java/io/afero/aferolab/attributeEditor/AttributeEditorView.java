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
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;

import io.afero.aferolab.R;
import io.afero.aferolab.databinding.ViewAttributeEditorBinding;
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

    private ViewAttributeEditorBinding binding;

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
        binding = ViewAttributeEditorBinding.bind(this);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                stop();
            }
        });

        binding.attributeValueSeekbar.setMax(Integer.MAX_VALUE);
        binding.attributeValueSeekbar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        binding.attributeValueSwitch.setOnCheckedChangeListener(mSwitchChangeListener);
        binding.attributeValueButton.setOnClickListener(v -> onClickAttributeButton());
        binding.attributeValueText.setOnEditorActionListener((textView, actionId, event) -> onAttributeValueEditorAction(textView, actionId, event));
    }

    public void start(DeviceModel deviceModel, DeviceProfile.Attribute attribute) {
        if (!isStarted()) {
            pushOnBackStack();

            binding.attributeValueText.setEnabled(false);
            binding.attributeValueText.setVisibility(VISIBLE);
            binding.attributeValueSeekbar.setVisibility(View.GONE);
            binding.attributeValueSwitch.setVisibility(View.GONE);
            binding.attributeValueButton.setVisibility(View.GONE);
            binding.attributeValueText.setFilters(new InputFilter[]{});
            binding.attributeValueText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            binding.attributeValueOptionsLabel.setText("");

            mController.start(deviceModel, attribute);

            startEnterTransition();
        }
    }

    public void stop() {
        if (isStarted()) {
            mController.stop();
            binding.attributeValueText.hideKeyboard();
            mPopupMenu = null;
            startExitTransition();
        }

        removeFromBackStack();
    }

    public boolean isStarted() {
        return mController.isStarted();
    }

    public void setEditorEnabled(boolean enabled) {
        binding.attributeValueButton.setEnabled(enabled);
        binding.attributeValueText.setEnabled(enabled && mValueEditTextEnabled);
        binding.attributeValueSeekbar.setEnabled(enabled);
        binding.attributeValueSwitch.setEnabled(enabled);
    }

    public void addEnumItem(String label, String value) {
        binding.attributeValueButton.setVisibility(View.VISIBLE);

        if (mPopupMenu == null) {
            mPopupMenu = new PopupMenu(getContext(), binding.attributeValueButton, Gravity.BOTTOM);
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
        binding.attributeValueOptionsLabel.setText(label);
    }

    public void setAttributeIdText(int id) {
        binding.attributeIdText.setText(Integer.toString(id));
    }

    public void setAttributeLabelText(@Nullable String label) {
        binding.attributeLabelText.setText(label != null ? label : "");
    }

    public void setAttributeDataTypeText(@Nullable String s) {
        binding.attributeDataTypeText.setText(s != null ? s : "-");
    }

    public void setAttributeTimestampText(@Nullable String s) {
        binding.attributeTimestampText.setText(s != null ? s : "-");
    }

    public void setAttributeValueText(String valueText) {
        binding.attributeValueText.setText(valueText != null ? valueText : "");
    }

    public void setAttributeValueSwitch(boolean value) {
        binding.attributeValueSwitch.setOnCheckedChangeListener(null);
        binding.attributeValueSwitch.setChecked(value);
        binding.attributeValueSwitch.setOnCheckedChangeListener(mSwitchChangeListener);
    }

    public void setAttributeValueEditorType(ValueEditorType editorType) {
        mEditorType = editorType;

        int numberInputType = 0;

        switch (mEditorType) {
            case NONE:
                break;

            case TEXT:
                mValueEditTextEnabled = true;
                binding.attributeValueText.setEnabled(true);
                break;

            case NUMBER_DECIMAL:
                numberInputType |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
            case NUMBER:
                numberInputType |= InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
                mValueEditTextEnabled = true;
                binding.attributeValueText.setEnabled(true);
                binding.attributeValueText.setInputType(numberInputType);
                binding.attributeValueSeekbar.setVisibility(View.VISIBLE);
                break;

            case BOOLEAN:
                binding.attributeValueSwitch.setVisibility(VISIBLE);
                break;

            case BYTES:
                mValueEditTextEnabled = true;
                binding.attributeValueText.setEnabled(true);
                binding.attributeValueText.setFilters(new InputFilter[]{new CharacterInputFilter("0123456789ABCDEF")});
                binding.attributeValueText.setInputType(binding.attributeValueText.getInputType() | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
                break;
        }
    }

    public void setAttributeValueSliderMax(int max) {
        binding.attributeValueSeekbar.setMax(max);
    }

    public void setAttributeValueSliderProportion(double proportion) {
        binding.attributeValueSeekbar.setOnSeekBarChangeListener(null);
        binding.attributeValueSeekbar.setProgress((int) Math.round(proportion * (double) binding.attributeValueSeekbar.getMax()));
        binding.attributeValueSeekbar.setOnSeekBarChangeListener(mSeekBarChangeListener);
    }

    public double getAttributeValueSliderProportion() {
        return Math.min((double) binding.attributeValueSeekbar.getProgress() / (double) binding.attributeValueSeekbar.getMax(), 1.0);
    }

    void onClickAttributeButton() {
        mPopupMenu.show();
    }

    boolean onAttributeValueEditorAction(TextView textView, int actionId, KeyEvent event) {

        if (AferoEditText.isDone(actionId, event)) {
            mController.onAttributeValueTextEditorChanged(textView.getText().toString());
            binding.attributeValueText.hideKeyboard();
        }

        return true;
    }

    private void startEnterTransition() {
        setVisibility(VISIBLE);
        binding.viewScrim.setAlpha(0);
        binding.viewScrim.animate().alpha(1).setDuration(ENTER_TRANSITION_DURATION);
        binding.attributeEditorCard.setAlpha(0);
        binding.attributeEditorCard.setScaleY(.1f);
        binding.attributeEditorCard.animate().scaleY(1).alpha(1)
                .setInterpolator(mEnterTransitionInterpolator)
                .setDuration(ENTER_TRANSITION_DURATION);
    }

    private void startExitTransition() {
        binding.viewScrim.animate().alpha(0).setDuration(EXIT_TRANSITION_DURATION);
        binding.attributeEditorCard.animate().scaleY(.1f).alpha(0)
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
