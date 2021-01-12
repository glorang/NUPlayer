/*
 * Copyright 2021 Geert Lorang
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

package be.lorang.nuplayer.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * This class represents a list of Programs, together they form the Catalog
 * This is a Singleton class that will keep the entire catalog in memory
 */
public class ProgramList {

    // Singleton instance
    private static ProgramList instance = null;

    private List<Program> mPrograms = new ArrayList<Program>();

    private ProgramList() {}

    public static ProgramList getInstance() {
        if(instance == null) {
            instance = new ProgramList();
        }
        return instance;
    }

    public List<Program> getPrograms() { return mPrograms; }
    public void addProgram(Program p) {
        mPrograms.add(p);
    }

    public void sort() {
        mPrograms.sort(Comparator.comparing(Program::getTitle));
    }

    public List<Program> getFavorites() {
        List<Program> result = new ArrayList<Program>();
        for(Program program : mPrograms) {
            if(program.isFavorite()) {
                result.add(program);
            }
        }
        result.sort(Comparator.comparing(Program::getTitle));
        return result;
    }

    public List<Program> getTimeLimitedSeries() {
        List<Program> result = new ArrayList<Program>();
        for(Program program : mPrograms) {
            if(program.isTimeLimited()) {
                result.add(program);
            }
        }
        result.sort(Comparator.comparing(Program::getTitle));
        return result;
    }

    public List<Program> search(String searchText) {
        List<Program> result = new ArrayList<Program>();
        for(Program program : mPrograms) {
            if(program.getTitle().toLowerCase().contains(searchText.toLowerCase())
                    || program.getDescription().toLowerCase().contains(searchText.toLowerCase())) {
                result.add(program);
            }
        }
        result.sort(Comparator.comparing(Program::getTitle));
        return result;
    }

}