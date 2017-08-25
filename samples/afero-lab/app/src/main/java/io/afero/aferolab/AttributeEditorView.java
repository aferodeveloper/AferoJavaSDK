package io.afero.aferolab;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;


public class AttributeEditorView extends FrameLayout {

    public enum ValueEditorType {
        NONE,
        TEXT,
        NUMBER,
        NUMBER_DECIMAL,
        BOOLEAN,
        BYTES,
        ENUM
    }

    @BindView(R.id.attribute_id_text)
    TextView mAttributeIdText;

    @BindView(R.id.attribute_label_text)
    TextView mAttributeLabelText;

    @BindView(R.id.attribute_data_type_text)
    TextView mAttributeDataTypeText;

    @BindView(R.id.attribute_value_text)
    AferoEditText mAttributeValueEditText;

    @BindView(R.id.attribute_value_seekbar)
    SeekBar mAttributeValueSeekBar;

    @BindView(R.id.attribute_value_switch)
    Switch mAttributeValueSwitch;

    @BindView(R.id.view_scrim)
    View mScrimView;

    @BindView(R.id.attribute_editor_card)
    CardView mAttributeCard;

    private static final long ENTER_TRANSITION_DURATION = 100;
    private static final long EXIT_TRANSITION_DURATION = 100;

    private final AttributeEditorController mController = new AttributeEditorController(this);
    private final TimeInterpolator mEnterTransitionInterpolator = new OvershootInterpolator();

    private ValueEditorType mEditorType = ValueEditorType.NONE;


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
        mAttributeValueSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mController.onAttributeValueNumberEditorChanged(getAttributeValueSliderProportion());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mAttributeValueSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mController.onAttributeValueBooleanEditorChanged(b);
            }
        });
    }

    public void start(DeviceModel deviceModel, DeviceProfile.Attribute attribute) {
        if (!isStarted()) {
            mController.start(deviceModel, attribute);
            startEnterTransition();
        }
    }

    public void stop() {
        if (isStarted()) {
            mController.stop();
            mAttributeValueEditText.hideKeyboard();
            startExitTransition();
        }
    }

    public boolean isStarted() {
        return mController.isStarted();
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

    public void setAttributeValueEditTextInputConfig(int inputType, String allowedCharacters) {
        mAttributeValueEditText.setInputType(inputType);
        if (allowedCharacters != null) {
            mAttributeValueEditText.setFilters(new InputFilter[]{new CharacterInputFilter(allowedCharacters)});
        }
    }

    public void setAttributeValueText(String valueText) {
        mAttributeValueEditText.setText(valueText != null ? valueText : "");
    }

    public void setAttributeValueEditorType(ValueEditorType editorType) {
        mEditorType = editorType;

        mAttributeValueEditText.setEnabled(false);
        mAttributeValueEditText.setVisibility(VISIBLE);
        mAttributeValueSeekBar.setVisibility(View.GONE);
        mAttributeValueSwitch.setVisibility(View.GONE);
        mAttributeValueEditText.setFilters(new InputFilter[]{});
        mAttributeValueEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        int numberInputType = 0;

        switch (mEditorType) {
            case NONE:
                break;

            case TEXT:
                mAttributeValueEditText.setEnabled(true);
                break;

            case NUMBER_DECIMAL:
                numberInputType |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
            case NUMBER:
                numberInputType |= InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
                mAttributeValueEditText.setEnabled(true);
                mAttributeValueEditText.setInputType(numberInputType);
                mAttributeValueSeekBar.setVisibility(View.VISIBLE);
                break;

            case BOOLEAN:
                mAttributeValueSwitch.setVisibility(VISIBLE);
                break;

            case BYTES:
                mAttributeValueEditText.setEnabled(true);
                mAttributeValueEditText.setFilters(new InputFilter[]{new CharacterInputFilter("0123456789ABCDEF")});
                mAttributeValueEditText.setInputType(mAttributeValueEditText.getInputType() | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
                break;

            case ENUM:
                break;
        }
    }

    public void setAttributeValueSliderProportion(double proportion) {
        mAttributeValueSeekBar.setProgress((int) Math.round(proportion * (double) mAttributeValueSeekBar.getMax()));
    }

    public double getAttributeValueSliderProportion() {
        return (double) mAttributeValueSeekBar.getProgress() / (double) mAttributeValueSeekBar.getMax();
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
