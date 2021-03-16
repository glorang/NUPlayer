/*
 * Copyright 2021 Geert Lorang
 * Copyright 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package be.lorang.nuplayer.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import android.os.Handler;
import android.os.ResultReceiver;
import android.text.InputType;
import android.widget.Toast;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.services.AuthService;
import be.lorang.nuplayer.services.CatalogService;

import java.util.List;

public class LoginActivity extends FragmentActivity {

    private static final int EMAIL = 1;
    private static final int PASSWORD = 2;
    private static final int SUBMIT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (null == savedInstanceState) {
            GuidedStepSupportFragment.addAsRoot(this, new FirstStepFragment(), android.R.id.content);
        }
    }

    public static class FirstStepFragment extends GuidedStepSupportFragment {

        private String loginID = "";
        private String password = "";

        @Override
        public int onProvideTheme() {
            return R.style.Theme_Leanback_GuidedStep;
        }

        @Override
        @NonNull
        public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
            String title = getString(R.string.pref_title_screen_signin);
            String description = getString(R.string.pref_title_login_description);
            Drawable icon = getActivity().getDrawable(R.drawable.ic_logo);
            return new GuidanceStylist.Guidance(title, description, "", icon);
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            GuidedAction enterEmail = new GuidedAction.Builder(getContext())
                    .id(EMAIL)
                    .title(getString(R.string.pref_title_email))
                    .descriptionEditable(true)
                    .build();
            GuidedAction enterPassword = new GuidedAction.Builder(getContext())
                    .id(PASSWORD)
                    .title(getString(R.string.pref_title_password))
                    .descriptionEditable(true)
                    .descriptionInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT)
                    .build();
            GuidedAction login = new GuidedAction.Builder(getContext())
                    .id(SUBMIT)
                    .title(getString(R.string.pref_title_login))
                    .build();
            actions.add(enterEmail);
            actions.add(enterPassword);
            actions.add(login);
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == EMAIL) {
                loginID = action.getDescription().toString();
            }

            if (action.getId() == PASSWORD) {
                password = action.getDescription().toString();
            }

            if (action.getId() == SUBMIT) {

                if(loginID.length() == 0) {
                    Toast.makeText(getActivity(), getString(R.string.pref_title_empty_username), Toast.LENGTH_SHORT).show();
                    return;
                }

                if(password.length() == 0) {
                    Toast.makeText(getActivity(), getString(R.string.pref_title_empty_password), Toast.LENGTH_SHORT).show();
                    return;
                }

                // authenticate
                Intent loginIntent = new Intent(getActivity(), AuthService.class);
                loginIntent.putExtra("loginID", loginID);
                loginIntent.putExtra("password", password);

                loginIntent.putExtra(CatalogService.BUNDLED_LISTENER, new ResultReceiver(new Handler()) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);

                        if(resultData.getString("MSG", "").length() > 0) {
                            Toast.makeText(getActivity(), resultData.getString("MSG"), Toast.LENGTH_SHORT).show();
                        }

                        if (resultCode == Activity.RESULT_OK) {
                            getActivity().finishAfterTransition();
                        }
                    }
                });

                getActivity().startService(loginIntent);

            }
        }
    }
}