/*
 * This is the source code of Telegram for Android v. 3.x.x
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LockController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;


public class ChatPasscodeActivity extends BaseFragment {

    private TextView titleTextView;
    private EditText passwordEditText;

    private int type;
    private long dialog_id;
    private int passcodeSetStep = 0;
    private String firstPassword;

    private final static int done_button = 1;
    private final static int pin_item = 2;

    public ChatPasscodeActivity(int type) {
        super();
        this.type = type;
    }

    public ChatPasscodeActivity(int type, long dialog_id) {
        super();
        this.type = type;
        this.dialog_id = dialog_id;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {
        if (type != 3) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        }
        actionBar.setAllowOverlayTitle(false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    if (passcodeSetStep == 0) {
                        processNext();
                    } else if (passcodeSetStep == 1) {
                        processDone();
                    }
                } else if (id == pin_item) {
                    updateDropDownTextView();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        if (type != 0) {
            ActionBarMenu menu = actionBar.createMenu();
            menu.addItemWithWidth(done_button, R.drawable.ic_done, AndroidUtilities.dp(56));

            titleTextView = new TextView(context);
            titleTextView.setTextColor(0xff757575);
            if (type == 1) {
                titleTextView.setText(LocaleController.getString("EnterNewPasscode", R.string.EnterNewPasscode));
            } else {
                titleTextView.setText(LocaleController.getString("EnterNewFirstPasscode", R.string.EnterNewFirstPasscode));
            }
            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            titleTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            frameLayout.addView(titleTextView);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) titleTextView.getLayoutParams();
            layoutParams.width = LayoutHelper.WRAP_CONTENT;
            layoutParams.height = LayoutHelper.WRAP_CONTENT;
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
            layoutParams.topMargin = AndroidUtilities.dp(38);
            titleTextView.setLayoutParams(layoutParams);

            passwordEditText = new EditText(context);
            passwordEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            passwordEditText.setTextColor(0xff000000);
            passwordEditText.setMaxLines(1);
            passwordEditText.setLines(1);
            passwordEditText.setGravity(Gravity.CENTER_HORIZONTAL);
            passwordEditText.setSingleLine(true);
            if (type == 1) {
                passcodeSetStep = 0;
                passwordEditText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            } else {
                passcodeSetStep = 1;
                passwordEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            }
            passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            passwordEditText.setTypeface(Typeface.DEFAULT);
            AndroidUtilities.clearCursorDrawable(passwordEditText);
            frameLayout.addView(passwordEditText);
            layoutParams = (FrameLayout.LayoutParams) passwordEditText.getLayoutParams();
            layoutParams.topMargin = AndroidUtilities.dp(90);
            layoutParams.height = AndroidUtilities.dp(36);
            layoutParams.leftMargin = AndroidUtilities.dp(40);
            layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
            layoutParams.rightMargin = AndroidUtilities.dp(40);
            layoutParams.width = LayoutHelper.MATCH_PARENT;
            passwordEditText.setLayoutParams(layoutParams);
            passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if (passcodeSetStep == 0) {
                        processNext();
                        return true;
                    } else if (passcodeSetStep == 1) {
                        processDone();
                        return true;
                    }
                    return false;
                }
            });
            passwordEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (passwordEditText.length() == 4) {
                        if (type == 2/* && UserConfig.passcodeType == 0*/) {
                            processDone();
                        } else if (type == 1 /*&& currentPasswordType == 0*/) {
                            if (passcodeSetStep == 0) {
                                processNext();
                            } else if (passcodeSetStep == 1) {
                                processDone();
                            }
                        }
                    }
                }
            });

            passwordEditText.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                public void onDestroyActionMode(ActionMode mode) {
                }

                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    return false;
                }
            });

            if (type == 1) {
                actionBar.setTitle(LocaleController.getString("PasscodePIN", R.string.PasscodePIN));
            } else {
                actionBar.setTitle(LocaleController.getString("Passcode", R.string.Passcode));
            }

            updateDropDownTextView();
        }

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (type != 0) {
            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    if (passwordEditText != null) {
                        passwordEditText.requestFocus();
                        AndroidUtilities.showKeyboard(passwordEditText);
                    }
                }
            }, 200);
        }
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen && type != 0) {
            AndroidUtilities.showKeyboard(passwordEditText);
        }
    }

    private void updateDropDownTextView() {
        if (type == 1 || type == 2) {
            InputFilter[] filterArray = new InputFilter[1];
            filterArray[0] = new InputFilter.LengthFilter(4);
            passwordEditText.setFilters(filterArray);
            passwordEditText.setInputType(InputType.TYPE_CLASS_PHONE);
            passwordEditText.setKeyListener(DigitsKeyListener.getInstance("1234567890"));
        }
        passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
    }

    private void processNext() {
        if (passwordEditText.getText().length() == 0 || passwordEditText.getText().length() != 4) {
            onPasscodeError();
            return;
        }

        String password = passwordEditText.getText().toString();
        if (!checkDuplicatePassword(password)) {
            return;
        }

        actionBar.setTitle(LocaleController.getString("PasscodePIN", R.string.PasscodePIN));

        titleTextView.setText(LocaleController.getString("ReEnterYourPasscode", R.string.ReEnterYourPasscode));
        firstPassword = passwordEditText.getText().toString();
        passwordEditText.setText("");
        passcodeSetStep = 1;
    }

    private boolean checkDuplicatePassword(String password) {
        if (!LockController.getInstance().isPasswordExist(password)) {
            return true;
        }

        try {
            Toast.makeText(getParentActivity(), LocaleController.getString("DuplicatePasscode", R.string.DuplicatePasscode), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
        AndroidUtilities.shakeView(titleTextView, 2, 0);

        passcodeSetStep = 0;

        passwordEditText.setText("");
        onPasscodeError();
        return false;
    }

    private void processDone() {
        if (passwordEditText.getText().length() == 0) {
            onPasscodeError();
            return;
        }
        if (type == 1) {
            if (!firstPassword.equals(passwordEditText.getText().toString())) {
                try {
                    Toast.makeText(getParentActivity(), LocaleController.getString("PasscodeDoNotMatch", R.string.PasscodeDoNotMatch), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
                AndroidUtilities.shakeView(titleTextView, 2, 0);
                passwordEditText.setText("");
                return;
            }

            String password = passwordEditText.getText().toString();
            if (!checkDuplicatePassword(password)) {
                return;
            }

            finishFragment();

            LockController.getInstance().lockDialog(dialog_id, password);

            passwordEditText.clearFocus();
            AndroidUtilities.hideKeyboard(passwordEditText);
        } else if (type == 2) {
            String password = passwordEditText.getText().toString();

            Long dialog_id = LockController.getInstance().getLockedDialogId(password);
            if (dialog_id == null) {
                passwordEditText.setText("");
                onPasscodeError();
                return;
            }

            passwordEditText.clearFocus();
            AndroidUtilities.hideKeyboard(passwordEditText);

            Bundle args = new Bundle();
            int enc_id = (int) (dialog_id >> 32);
            args.putInt("enc_id", enc_id);
            presentFragment(new ChatActivity(args), true);
        }
    }

    private void onPasscodeError() {
        if (getParentActivity() == null) {
            return;
        }
        Vibrator v = (Vibrator) getParentActivity().getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(200);
        }
        AndroidUtilities.shakeView(titleTextView, 2, 0);
    }
}
