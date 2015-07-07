/*
 * Copyright 2015 Palantir Technologies, Inc.
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
 */

package com.palantir.jacoco;

public enum CoverageRealm {
    FILE("sourcefile", "fileThreshold"),
    CLASS("class", "classThreshold"),
    PACKAGE("package", "packageThreshold"),
    REPORT("report", "reportThreshold");

    /** The name of the XML tag (in the Jacoco coverage XML report ) for this realm. */
    public final String tagName;
    /** The configuration DSL name for this realm. */
    public final String realmName;

    CoverageRealm(String tagName, String realmName) {
        this.tagName = tagName;
        this.realmName = realmName;
    }
}
