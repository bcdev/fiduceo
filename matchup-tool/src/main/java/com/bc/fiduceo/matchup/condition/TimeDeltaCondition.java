/*
 * Copyright (C) 2016 Brockmann Consult GmbH
 * This code was developed for the EC project "Fidelity and Uncertainty in
 * Climate Data Records from Earth Observations (FIDUCEO)".
 * Grant Agreement: 638822
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * A copy of the GNU General Public License should have been supplied along
 * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package com.bc.fiduceo.matchup.condition;

import com.bc.fiduceo.matchup.MatchupSet;
import com.bc.fiduceo.matchup.Sample;
import com.bc.fiduceo.matchup.SampleSet;

import java.util.ArrayList;
import java.util.List;

/* The XML template for this condition class looks like:

    <time-delta>
        <time-delta-seconds>
            300
        </time-delta-seconds>
    </time-delta>

 */

class TimeDeltaCondition implements Condition {

    private final long maxTimeDeltaInMillis;

    TimeDeltaCondition(long maxTimeDeltaInMillis) {
        this.maxTimeDeltaInMillis = maxTimeDeltaInMillis;
    }

    @Override
    public void apply(MatchupSet matchupSet, ConditionEngineContext context) {
        final List<SampleSet> sourceSamples = matchupSet.getSampleSets();
        final List<SampleSet> targetSamples = new ArrayList<>();
        for (final SampleSet sampleSet : sourceSamples) {
            final Sample primary = sampleSet.getPrimary();
            final Sample secondary = sampleSet.getSecondary();
            final long actualTimeDelta = Math.abs(primary.time - secondary.time);
            if (actualTimeDelta <= maxTimeDeltaInMillis) {
                targetSamples.add(sampleSet);
            }
        }
        matchupSet.setSampleSets(targetSamples);
        sourceSamples.clear();
    }

    public long getMaxTimeDeltaInMillis() {
        return maxTimeDeltaInMillis;
    }
}
