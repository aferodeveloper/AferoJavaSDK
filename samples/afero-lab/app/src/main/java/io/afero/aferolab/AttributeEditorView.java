package io.afero.aferolab;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;


public class AttributeEditorView extends FrameLayout {

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

    @BindView(R.id.view_scrim)
    View mScrimView;

    @BindView(R.id.attribute_editor_card)
    CardView mAttributeCard;

    private static final long ENTER_TRANSITION_DURATION = 100;
    private static final long EXIT_TRANSITION_DURATION = 100;

    private final AttributeEditorController mController = new AttributeEditorController(this);

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
                mController.onAttributeValueSliderChanged(getAttributeValueSliderProportion());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

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

    public void setAttributeValueEditTextConfig(int inputType, String digits) {
        mAttributeValueEditText.setInputType(inputType);
        if (digits != null) {
            mAttributeValueEditText.setKeyListener(DigitsKeyListener.getInstance(digits));
        }
    }

    public void setAttributeValueText(String valueText) {
        mAttributeValueEditText.setText(valueText != null ? valueText : "");
    }

    public void setAttributeValueEditEnabled(boolean enabled) {
        mAttributeValueEditText.setEnabled(enabled);
        mAttributeValueSeekBar.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    public void setAttributeNumericEditEnabled(boolean enabled) {
        mAttributeValueSeekBar.setVisibility(mAttributeValueEditText.isEnabled() && enabled ? View.VISIBLE : View.GONE);
    }

    public void setAttributeValueSliderProportion(double proportion) {
        mAttributeValueSeekBar.setProgress((int)Math.round(proportion * (double)mAttributeValueSeekBar.getMax()));
    }

    public double getAttributeValueSliderProportion() {
        return (double)mAttributeValueSeekBar.getProgress() / (double)mAttributeValueSeekBar.getMax();
    }

    private void startEnterTransition() {
        setVisibility(VISIBLE);
        mScrimView.setAlpha(0);
        mScrimView.animate().alpha(1).setDuration(ENTER_TRANSITION_DURATION);
        mAttributeCard.setAlpha(0);
        mAttributeCard.setScaleY(.1f);
        mAttributeCard.animate().scaleY(1).alpha(1)
                .setInterpolator(new OvershootInterpolator())
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
}
