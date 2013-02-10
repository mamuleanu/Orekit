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
package org.orekit.forces.gravity.potential;

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;

/** Simple implementation of {@link RawSphericalHarmonicsProvider} for pulsating gravity fields.
 * @author Luc Maisonobe
 * @since 6.0
 */
class PulsatingSphericalHarmonics implements RawSphericalHarmonicsProvider {

    /** Underlying part of the field. */
    private final RawSphericalHarmonicsProvider provider;

    /** Pulsation (rad/s). */
    private final double pulsation;

    /** Cosine component of the cosine coefficients. */
    private final double[][] cosC;

    /** Sine component of the cosine coefficients. */
    private final double[][] sinC;

    /** Cosine component of the sine coefficients. */
    private final double[][] cosS;

    /** Sine component of the sine coefficients. */
    private final double[][] sinS;

    /** Simple constructor.
     * @param provider underlying part of the field
     * @param period period of the pulsation (s)
     * @param cosC cosine component of the cosine coefficients
     * @param sinC sine component of the cosine coefficients
     * @param cosS cosine component of the sine coefficients
     * @param sinS sine component of the sine coefficients
     */
    public PulsatingSphericalHarmonics(final RawSphericalHarmonicsProvider provider,
                                     final double period,
                                     final double[][] cosC, final double[][] sinC,
                                     final double[][] cosS, final double[][] sinS) {
        this.provider  = provider;
        this.pulsation = MathUtils.TWO_PI / period;
        this.cosC      = cosC;
        this.sinC      = sinC;
        this.cosS      = cosS;
        this.sinS      = sinS;
    }

    /** {@inheritDoc} */
    public int getMaxDegree() {
        return provider.getMaxDegree();
    }

    /** {@inheritDoc} */
    public int getMaxOrder() {
        return provider.getMaxOrder();
    }

    /** {@inheritDoc} */
    public double getMu() {
        return provider.getMu();
    }

    /** {@inheritDoc} */
    public double getAe() {
        return provider.getAe();
    }

    /** {@inheritDoc} */
    public AbsoluteDate getReferenceDate() {
        return provider.getReferenceDate();
    }

    /** {@inheritDoc} */
    public double getOffset(final AbsoluteDate date) {
        return provider.getOffset(date);
    }

    /** {@inheritDoc} */
    public double getRawCnm(final double dateOffset, final int n, final int m)
        throws OrekitException {

        // retrieve the underlying part of the coefficient
        double cnm = provider.getRawCnm(dateOffset, n, m);

        if (n < cosC.length && m < cosC[n].length) {
            // add pulsation
            final double alpha = pulsation * dateOffset;
            cnm += cosC[n][m] * FastMath.cos(alpha) + sinC[n][m] * FastMath.sin(alpha);
        }

        return cnm;

    }

    /** {@inheritDoc} */
    public double getRawSnm(final double dateOffset, final int n, final int m)
        throws OrekitException {

        // retrieve the constant part of the coefficient
        double snm = provider.getRawSnm(dateOffset, n, m);

        if (n < cosS.length && m < cosS[n].length) {
            // add pulsation
            final double alpha = pulsation * dateOffset;
            snm += cosS[n][m] * FastMath.cos(alpha) + sinS[n][m] * FastMath.sin(alpha);
        }

        return snm;

    }

}
