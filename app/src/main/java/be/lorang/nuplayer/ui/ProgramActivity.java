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
import android.os.Bundle;
import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.Program;
import com.google.gson.Gson;

public class ProgramActivity extends Activity {

    private Program program;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get passed Program object
        String programJson = getIntent().getExtras().getString("PROGRAM_OBJECT");
        if(programJson != null) {
            program = new Gson().fromJson(programJson, Program.class);
        }

        setContentView(R.layout.fragment_program);
    }

    public Program getProgram() { return program;}
}
