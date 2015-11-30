package de.mm.android.longitude.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.mm.android.longitude.R;

public class LoginFragment extends Fragment {
	private static final String TAG = LoginFragment.class.getSimpleName();

	public interface ILoginCallback {
		void onSignInPressed();
		void onLoginPressed(String name);
    }

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

	private ILoginCallback callback;

    @Bind(R.id.f_intro_name_layout)
    TextInputLayout textInputLayout;
    @Bind(R.id.f_intro_name)
	EditText nameText;
    @Bind(R.id.f_intro_login)
	Button loginBtn;

	/* LifeCycle */

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof ILoginCallback) {
            callback = (ILoginCallback) getActivity();
        } else {
            throw new IllegalStateException(getActivity().getClass().getName() + " must implement ILoginFragment");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.f_login, container, false);
        ButterKnife.bind(this, v);

        nameText = (EditText) v.findViewById(R.id.f_intro_name);
        nameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                int size = nameText.getText().toString().length();
                if (size == 0) {
                    textInputLayout.setEnabled(true);
                    textInputLayout.setError(getString(R.string.error_field_is_empty));
                } else {
                    textInputLayout.setEnabled(false);
                    textInputLayout.setError(null);
                }
            }
        });
        nameText.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loginBtn.performClick();
            }
            return true;
        });

        v.findViewById(R.id.f_intro_signin).setOnClickListener(view -> callback.onSignInPressed());
        loginBtn.setOnClickListener(onClickEvent -> doLogin());
        return v;
    }

    private void doLogin(){
        if (nameText.getText() != null && !nameText.getText().toString().isEmpty()) {
            callback.onLoginPressed(nameText.getText().toString());
        }
    }

    public void enableStepTwo(CharSequence userName) {
        Log.d(TAG, "enableStepTwo: " + userName);
        nameText.setEnabled(true);
        nameText.setText(userName);
        loginBtn.setEnabled(true);
    }

}