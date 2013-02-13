/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.propagation.integration;

import java.util.List;

import org.apache.commons.math3.ode.ContinuousOutputModel;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalPropagator;
import org.orekit.propagation.analytical.AdditionalStateProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** This class stores sequentially generated orbital parameters for
 * later retrieval.
 *
 * <p>
 * Instances of this class are built and then must be fed with the results
 * provided by {@link org.orekit.propagation.Propagator Propagator} objects
 * configured in {@link org.orekit.propagation.Propagator#setEphemerisMode()
 * ephemeris generation mode}. Once propagation is o, random access to any
 * intermediate state of the orbit throughout the propagation range is possible.
 * </p>
 * <p>
 * A typical use case is for numerically integrated orbits, which can be used by
 * algorithms that need to wander around according to their own algorithm without
 * cumbersome tight links with the integrator.
 * </p>
 * <p>
 * Another use case is for persistence, as this class is serializable.
 * </p>
 * <p>
 * As this class implements the {@link org.orekit.propagation.Propagator Propagator}
 * interface, it can itself be used in batch mode to build another instance of the
 * same type. This is however not recommended since it would be a waste of resources.
 * </p>
 * <p>
 * Note that this class stores all intermediate states along with interpolation
 * models, so it may be memory intensive.
 * </p>
 *
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @author Mathieu Rom&eacute;ro
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class IntegratedEphemeris
    extends AbstractAnalyticalPropagator implements BoundedPropagator {

    /** Mapper between raw double components and spacecraft state. */
    private final StateMapper mapper;

    /** Start date of the integration (can be min or max). */
    private final AbsoluteDate startDate;

    /** First date of the range. */
    private final AbsoluteDate minDate;

    /** Last date of the range. */
    private final AbsoluteDate maxDate;

    /** Underlying raw mathematical model. */
    private ContinuousOutputModel model;

    /** Creates a new instance of IntegratedEphemeris.
     * @param startDate Start date of the integration (can be minDate or maxDate)
     * @param minDate first date of the range
     * @param maxDate last date of the range
     * @param mapper mapper between raw double components and spacecraft state
     * @param stateData list of additional state data providers
     * @param model underlying raw mathematical model
     * @exception OrekitException if several providers have the same name
     */
    public IntegratedEphemeris(final AbsoluteDate startDate,
                               final AbsoluteDate minDate, final AbsoluteDate maxDate,
                               final StateMapper mapper,
                               final List<AdditionalStateData> stateData,
                               final ContinuousOutputModel model)
        throws OrekitException {

        super(mapper.getAttitudeProvider());

        this.startDate = startDate;
        this.minDate   = minDate;
        this.maxDate   = maxDate;
        this.mapper    = mapper;
        this.model     = model;

        // set up providers to map the final elements of the model array to additional states
        int index = 7;
        for (final AdditionalStateData data : stateData) {
            final int length = data.getAdditionalState().length;
            addAdditionalStateProvider(new LocalProvider(data.getName(), index, length));
            index += length;
        }

    }

    /** Set up the model at some interpolation date.
     * @param date desired interpolation date
     * @exception PropagationException if specified date is outside
     * of supported range
     */
    private void setInterpolationDate(final AbsoluteDate date)
        throws PropagationException {

        if (date.equals(startDate.shiftedBy(model.getInterpolatedTime()))) {
            // the current model date is already the desired one
            return;
        }

        if ((date.compareTo(minDate) < 0) || (date.compareTo(maxDate) > 0)) {
            // date is outside of supported range
            throw new PropagationException(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE,
                                           date, minDate, maxDate);
        }

        // reset interpolation model to the desired date
        model.setInterpolatedTime(date.durationFrom(startDate));

    }

    /** {@inheritDoc} */
    protected SpacecraftState basicPropagate(final AbsoluteDate date)
        throws PropagationException {
        try {
            setInterpolationDate(date);
            return mapper.mapArrayToState(model.getInterpolatedTime(), model.getInterpolatedState());
        } catch (OrekitExceptionWrapper oew) {
            if (oew.getException() instanceof PropagationException) {
                throw (PropagationException) oew.getException();
            } else {
                throw new PropagationException(oew.getException());
            }
        } catch (OrekitException oe) {
            if (oe instanceof PropagationException) {
                throw (PropagationException) oe;
            } else {
                throw new PropagationException(oe);
            }
        }
    }

    /** {@inheritDoc} */
    protected Orbit propagateOrbit(final AbsoluteDate date)
        throws PropagationException {
        return basicPropagate(date).getOrbit();
    }

    /** {@inheritDoc} */
    protected double getMass(final AbsoluteDate date) throws PropagationException {
        return basicPropagate(date).getMass();
    }

    /** {@inheritDoc} */
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        return propagate(date).getPVCoordinates(frame);
    }

    /** Get the first date of the range.
     * @return the first date of the range
     */
    public AbsoluteDate getMinDate() {
        return minDate;
    }

    /** Get the last date of the range.
     * @return the last date of the range
     */
    public AbsoluteDate getMaxDate() {
        return maxDate;
    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state)
        throws PropagationException {
        throw new PropagationException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    public SpacecraftState getInitialState() throws PropagationException {
        return basicPropagate(getMinDate());
    }

    /** Local provider for additional state data. */
    private class LocalProvider implements AdditionalStateProvider {

        /** Name of the additional state. */
        private final String name;

        /** Index of the first element in the global integrated array. */
        private final int startIndex;

        /** Length of the additional state array. */
        private final int length;

        /** Simple constructor.
         * @param name name of the additional state
         * @param startIndex index of the first element in the global integrated array
         * @param length length of the additional state array
         */
        public LocalProvider(final String name, final int startIndex, final int length) {
            this.name       = name;
            this.startIndex = startIndex;
            this.length     = length;
        }

        /** {@inheritDoc} */
        public String getName() {
            return name;
        }

        /** {@inheritDoc} */
        public double[] getAdditionalState(final SpacecraftState state)
            throws PropagationException {
            try {

                // set the model date
                setInterpolationDate(state.getDate());

                // extract the part of the interpolated array corresponding to the additional state
                final double[] additionalState = new double[length];
                System.arraycopy(model.getInterpolatedState(), startIndex, additionalState, 0, length);

                return additionalState;

            } catch (OrekitExceptionWrapper oew) {
                if (oew.getException() instanceof PropagationException) {
                    throw (PropagationException) oew.getException();
                } else {
                    throw new PropagationException(oew.getException());
                }
            } catch (OrekitException oe) {
                if (oe instanceof PropagationException) {
                    throw (PropagationException) oe;
                } else {
                    throw new PropagationException(oe);
                }
            }
        }

    }

}