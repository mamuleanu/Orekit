/* Copyright 2002-2014 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation.semianalytical.dsst.utilities;

import java.util.ArrayList;

import org.apache.commons.math3.analysis.interpolation.HermiteInterpolator;
import org.apache.commons.math3.util.FastMath;
import org.orekit.time.AbsoluteDate;

/** Interpolated short periodics coefficients.
 * <p>
 * Representation of a coefficient that need to be interpolated over time.
 * </p><p>
 * The short periodics coefficients can be interpolated for faster computation.
 * This class stores computed values of the coefficients through the method
 * {@link #addGridPoint} and gives an interpolated result through the method
 * {@link #value}.
 * </p>
 * @author Nicolas Bernard
 *
 */
public class ShortPeriodicsInterpolatedCoefficient {

    /**Values of the already computed coefficients.*/
    private ArrayList<Double> values;

    /**Grid points.*/
    private ArrayList<AbsoluteDate> abscissae;

    /**Number of points used in the interpolation.*/
    private int interpolationPoints;

    /**Index of the latest closest neighbor.*/
    private int latestClosestNeighbor;

    /**Simple constructor.
     * @param interpolationPoints number of points used in the interpolation
     */
    public ShortPeriodicsInterpolatedCoefficient(final int interpolationPoints) {
        this.interpolationPoints = interpolationPoints;
        this.abscissae = new ArrayList<AbsoluteDate>();
        this.values = new ArrayList<Double>();
        this.latestClosestNeighbor = 0;
    }

    /**Compute the value of the coefficient.
     * @param date date at which the coefficient should be computed
     * @return value of the coefficient
     */
    public double value(final AbsoluteDate date) {
        //Get the closest points from the input date
        final int[] neighbors = getNeighborsIndices(date);

        //Creation and set up of the interpolator
        final HermiteInterpolator interpolator = new HermiteInterpolator();
        for (int i : neighbors) {
            final double abscissa = abscissae.get(i).durationFrom(date);
            final double value = values.get(i);

            interpolator.addSamplePoint(abscissa, new double[]{value});
        }

        //interpolation
        return interpolator.value(0.0)[0];
    }

    /**Find the closest available points from the specified date.
     * @param date date of interest
     * @return indices corresponding to the closest points on the time scale
     */
    private int[] getNeighborsIndices(final AbsoluteDate date) {
        final int sizeofNeighborhood = FastMath.min(interpolationPoints, abscissae.size());
        final int[] neighborsIndices = new int[sizeofNeighborhood];

        //If the size of the complete sample is less than
        //the desired number of interpolation points,
        //then the entire sample is considered as the neighborhood
        if (interpolationPoints >= abscissae.size()) {
            for (int i = 0; i < sizeofNeighborhood; i++) {
                neighborsIndices[i] = i;
            }
        }

        //Else the neighborsIndices array is completed step-by-step,
        //starting with its closest neighbor
        else {
            final int closestNeighbor = getClosestNeighbor(date);

            neighborsIndices[0] = closestNeighbor;

            int i = 1;
            int lowerNeighbor = closestNeighbor - 1;
            int upperNeighbor = closestNeighbor + 1;

            while (i < interpolationPoints) {
                if (lowerNeighbor < 0) { //This means that we have reached the earliest date
                    neighborsIndices[i] = upperNeighbor;
                    upperNeighbor++;
                }
                else if (upperNeighbor >= abscissae.size()) { //This means that we have reached the latest date
                    neighborsIndices[i] = lowerNeighbor;
                    lowerNeighbor--;
                }
                else { //the choice is made between the two next neighbors
                    final double lowerNeighborDistance = FastMath.abs(abscissae.get(lowerNeighbor).durationFrom(date));
                    final double upperNeighborDistance = FastMath.abs(abscissae.get(upperNeighbor).durationFrom(date));

                    if (lowerNeighborDistance <= upperNeighborDistance) {
                        neighborsIndices[i] = lowerNeighbor;
                        lowerNeighbor--;
                    }
                    else {
                        neighborsIndices[i] = upperNeighbor;
                        upperNeighbor++;
                    }
                }

                i++;
            }
        }

        return neighborsIndices;
    }

    /**Find the closest point from a specific date amongst the available points.
     * @param date date of interest
     * @return index of the closest abscissa from the date of interest
     */
    private int getClosestNeighbor(final AbsoluteDate date) {
        //the starting point is the latest result of a call to this method.
        //Indeed, as this class is meant to be called during an integration process
        //with an input date evolving often continuously in time, there is a high
        //probability that the result will be the same as for last call of
        //this method.
        int closestNeighbor = latestClosestNeighbor;

        //case where the date is before the available points
        if (date.compareTo(abscissae.get(0)) <= 0) {
            closestNeighbor = 0;
        }
        //case where the date is after the available points
        else if (date.compareTo(abscissae.get(abscissae.size() - 1)) >= 0) {
            closestNeighbor = abscissae.size() - 1;
        }
        //general case: one is looking for the two consecutives entries that surround the input date
        //then one choose the closest one
        else {
            int lowerBorder = latestClosestNeighbor;
            int upperBorder = latestClosestNeighbor;

            final int searchDirection = date.compareTo(abscissae.get(latestClosestNeighbor));
            if (searchDirection > 0) {
                upperBorder++;
                while (date.compareTo(abscissae.get(upperBorder)) > 0) {
                    upperBorder++;
                    lowerBorder++;
                }
            }
            else {
                lowerBorder--;
                while (date.compareTo(abscissae.get(lowerBorder)) < 0) {
                    upperBorder--;
                    lowerBorder--;
                }
            }

            final double lowerDistance = FastMath.abs(date.durationFrom(abscissae.get(lowerBorder)));
            final double upperDistance = FastMath.abs(date.durationFrom(abscissae.get(upperBorder)));

            closestNeighbor = (lowerDistance < upperDistance) ? lowerBorder : upperBorder;
        }

        //The result is stored in order to speed up the next call to the function
        //Indeed, it is highly likely that the requested result will be the same
        this.latestClosestNeighbor = closestNeighbor;
        return closestNeighbor;
    }

    /** Clear the recorded values from the interpolation grid.
     */
    public void clearHistory() {
        abscissae.clear();
        values.clear();
    }

    /** Add a point to the interpolation grid.
     * @param date abscissa of the point
     * @param value value of the element
     */
    public void addGridPoint(final AbsoluteDate date, final double value) {
        //If the grid is empty, the value is directly added to both arrays
        if (abscissae.isEmpty()) {
            abscissae.add(date);
            values.add(value);
        }
        //If the grid already contains this point, only its value is changed
        else if (abscissae.contains(date)) {
            values.set(abscissae.indexOf(date), value);
        }
        //If the grid does not contain this point, the position of the point
        //in the grid is computed first
        else {
            final int closestNeighbor = getClosestNeighbor(date);
            final int index = (date.compareTo(abscissae.get(closestNeighbor)) < 0) ? closestNeighbor : closestNeighbor + 1;
            abscissae.add(index, date);
            values.add(index, value);
        }
    }
}
